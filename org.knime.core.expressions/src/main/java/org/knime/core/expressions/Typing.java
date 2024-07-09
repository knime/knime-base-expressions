/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 6, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.STRING;

import java.util.List;
import java.util.function.Function;

import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.BinaryOp;
import org.knime.core.expressions.Ast.BinaryOperator;
import org.knime.core.expressions.Ast.BooleanConstant;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.FloatConstant;
import org.knime.core.expressions.Ast.FlowVarAccess;
import org.knime.core.expressions.Ast.FunctionCall;
import org.knime.core.expressions.Ast.IntegerConstant;
import org.knime.core.expressions.Ast.MissingConstant;
import org.knime.core.expressions.Ast.StringConstant;
import org.knime.core.expressions.Ast.UnaryOp;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.Expressions.ExpressionCompileException;

/**
 * Algorithm to infer types of an {@link Ast}.
 *
 * @author Tobias Pietzsch
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class Typing {

    private static final String TYPE_DATA_KEY = "type";

    private Typing() {
    }

    static ValueType inferTypes( //
        final Ast root, //
        final Function<String, ReturnResult<ValueType>> columnToType, //
        final Function<String, ReturnResult<ValueType>> flowVarType //
    ) throws ExpressionCompileException {
        var outputType = Ast.putDataRecursive(root, TYPE_DATA_KEY, new TypingVisitor(columnToType, flowVarType));
        if (outputType instanceof ErrorValueType errorValueType) {
            throw new ExpressionCompileException(errorValueType.m_errors);
        }
        return outputType;
    }

    static ValueType getType(final Ast node) {
        Object type = node.data(TYPE_DATA_KEY);
        if (type instanceof ValueType astType) {
            return astType;
        } else {
            throw new IllegalArgumentException("The node " + node + " has no type.");
        }
    }

    private static final class TypingVisitor implements Ast.AstVisitor<ValueType, RuntimeException> {

        private final Function<String, ReturnResult<ValueType>> m_columnType;

        private final Function<String, ReturnResult<ValueType>> m_flowVariableType;

        TypingVisitor( //
            final Function<String, ReturnResult<ValueType>> columnToType, //
            final Function<String, ReturnResult<ValueType>> flowVarType //
        ) {
            m_columnType = columnToType;
            m_flowVariableType = flowVarType;
        }

        @Override
        public ValueType visit(final MissingConstant missingConstant) {
            return MISSING;
        }

        @Override
        public ValueType visit(final BooleanConstant node) {
            return BOOLEAN;
        }

        @Override
        public ValueType visit(final IntegerConstant node) {
            return INTEGER;
        }

        @Override
        public ValueType visit(final FloatConstant node) {
            return FLOAT;
        }

        @Override
        public ValueType visit(final StringConstant node) {
            return STRING;
        }

        @Override
        public ValueType visit(final ColumnAccess node) {
            final Ast.ColumnId id = node.columnId();
            return switch (id.type()) {
                case NAMED -> m_columnType.apply(id.name())
                    .orElseGet(message -> ErrorValueType.missingColumn(node, message));
                case ROW_ID -> STRING;
                case ROW_INDEX -> INTEGER;
            };
        }

        @Override
        public ValueType visit(final FlowVarAccess node) {
            return m_flowVariableType.apply(node.name())
                .orElseGet(message -> ErrorValueType.missingFlowVariable(node, message));
        }

        @Override
        public ValueType visit(final BinaryOp node) { // NOSONAR - this method is not too complex
            var op = node.op();
            var t1 = getType(node.arg1());
            var t2 = getType(node.arg2());

            if (t1 instanceof ErrorValueType || t2 instanceof ErrorValueType) {
                return ErrorValueType.combined(List.of(t1, t2));
            } else if (op == BinaryOperator.PLUS && isAnyString(t1, t2) && !isAnyMissing(t1, t2)) {
                return STRING;
            } else if (op.isArithmetic() && isAllNumeric(t1, t2)) {
                // Arithmetic operation
                return arithmeticType(node, t1, t2);
            } else if (op.isOrderingComparison() && isAllNumeric(t1, t2)) {
                // Ordering comparison
                return BOOLEAN;
            } else if (op.isEqualityComparison()) {
                // Equality comparison
                return equalityType(node, t1, t2);
            } else if (op.isLogical() && isAllBoolean(t1, t2)) {
                // Logical operation
                return BOOLEAN(t1.isOptional() || t2.isOptional());
            } else if (op == BinaryOperator.MISSING_FALLBACK) {
                return missingFallbackTypes(node, t1, t2);
            } else {
                return ErrorValueType.binaryOpNotApplicable(node, t1, t2);
            }
        }

        @Override
        public ValueType visit(final UnaryOp node) {
            var op = node.op();
            var type = getType(node.arg());

            if (op == UnaryOperator.MINUS && isNumeric(type)) {
                return type;
            } else if (op == UnaryOperator.NOT && BOOLEAN.equals(type.baseType())) {
                return type;
            } else {
                return ErrorValueType.unaryOpNotApplicable(node, type);
            }
        }

        @Override
        public ValueType visit(final FunctionCall node) {
            var argTypes = node.args().asList().stream().map(Typing::getType).toList();

            if (argTypes.stream().anyMatch(ErrorValueType.class::isInstance)) {
                return ErrorValueType.combined(argTypes);
            }

            // TODO(AP-22303) show better error if the function is not applicable
            return node.function().returnType(argTypes)
                .orElseGet(() -> ErrorValueType.functionNotApplicable(node, argTypes));
        }

        @Override
        public ValueType visit(final AggregationCall node) throws RuntimeException {
            var resolvedAggregation = node.aggregation();

            var ret = resolvedAggregation.returnType(node.args(), m_columnType);

            if (ret.isOk()) {
                return ret.getValue();
            } else {
                return ErrorValueType.aggregationNotApplicable(node, ret.getErrorMessage());
            }
        }

        private static ValueType arithmeticType(final BinaryOp node, final ValueType typeA, final ValueType typeB) {
            var op = node.op();
            var baseTypeA = typeA.baseType();
            var baseTypeB = typeB.baseType();
            var optional = typeA.isOptional() || typeB.isOptional();

            if (op == BinaryOperator.DIVIDE) {
                // Special rule for "/" : we always return FLOAT
                return FLOAT(optional);
            } else if (op == BinaryOperator.FLOOR_DIVIDE) {
                // Special rule for "//" : only applicable to INTEGER
                if (INTEGER.equals(baseTypeA) && INTEGER.equals(baseTypeB)) {
                    return INTEGER(optional);
                }
                return ErrorValueType.binaryOpNotApplicable(node, typeA, typeB);
            } else if (INTEGER.equals(baseTypeA) && INTEGER.equals(baseTypeB)) {
                // Both INTEGER
                return INTEGER(optional);
            } else {
                // At least one FLOAT
                return FLOAT(optional);
            }
        }

        /** @throws ExpressionCompileException if the given types cannot be compared with an equality operator */
        private static ValueType equalityType(final BinaryOp node, final ValueType typeA, final ValueType typeB) {
            if (typeA.baseType().equals(typeB.baseType())) {
                // Same type or one is the missing type extension of the other
                return BOOLEAN;
            }
            if (MISSING.equals(typeA) || MISSING.equals(typeB)) {
                // Any type can be compared with MISSING
                return BOOLEAN;
            }
            if (isNumeric(typeA) && isNumeric(typeB)) {
                // All numbers can be compared with each other
                return BOOLEAN;
            }
            return ErrorValueType.binaryOpNotApplicable(node, typeA, typeB);
        }

        private static ValueType missingFallbackTypes(final BinaryOp node, final ValueType typeA,
            final ValueType typeB) { // NOSONAR: not too complex

            if (MISSING.equals(typeA) && MISSING.equals(typeB)) {
                return ErrorValueType.nullishOpNotApplicable(node, typeA, typeB);
            } else if (MISSING.equals(typeA)) {
                return typeB;
            } else if (MISSING.equals(typeB)) {
                return typeA;
            } else if ((typeA.baseType()).equals((typeB.baseType()))) {

                // result is optional iff both operands are optional
                return (typeA.isOptional() && typeB.isOptional()) //
                    ? typeA //
                    : typeA.baseType();
            } else if (isAllNumeric(typeA, typeB)) {

                // Handle the special case where we have integers and floats
                return (typeA.isOptional() && typeB.isOptional()) //
                    ? OPT_FLOAT //
                    : FLOAT;
            } else {
                return ErrorValueType.nullishOpNotApplicable(node, typeA, typeB);
            }
        }

        // Small helpers

        private static boolean isNumeric(final ValueType type) {
            var baseType = type.baseType();
            return INTEGER.equals(baseType) || FLOAT.equals(baseType);
        }

        private static boolean isAnyString(final ValueType typeA, final ValueType typeB) {
            return STRING.equals(typeA.baseType()) || STRING.equals(typeB.baseType());
        }

        private static boolean isAllNumeric(final ValueType typeA, final ValueType typeB) {
            return isNumeric(typeA) && isNumeric(typeB);
        }

        private static boolean isAllBoolean(final ValueType typeA, final ValueType typeB) {
            return BOOLEAN.equals(typeA.baseType()) && BOOLEAN.equals(typeB.baseType());
        }

        private static boolean isAnyMissing(final ValueType t1, final ValueType t2) {
            return MISSING.equals(t1) || MISSING.equals(t2);
        }
    }

    /** Placeholder value type for collecting typing errors */
    private static final class ErrorValueType implements ValueType {

        private final List<ExpressionCompileError> m_errors;

        static ErrorValueType missingColumn(final ColumnAccess node, final String errorMessage) {
            return new ErrorValueType(
                List.of(ExpressionCompileError.missingColumnError(errorMessage, Parser.getTextLocation(node))));
        }

        static ErrorValueType missingFlowVariable(final FlowVarAccess node, final String message) {
            return new ErrorValueType(
                List.of(ExpressionCompileError.missingFlowVariableError(message, Parser.getTextLocation(node))));
        }

        static ErrorValueType combined(final List<ValueType> children) {
            return new ErrorValueType( //
                children.stream() //
                    .filter(ErrorValueType.class::isInstance) //
                    .flatMap(e -> ((ErrorValueType)e).m_errors.stream()) //
                    .toList() //
            );
        }

        static ErrorValueType typingError(final String message, final Ast node) {
            return new ErrorValueType(
                List.of(ExpressionCompileError.typingError(message, Parser.getTextLocation(node))));
        }

        static ErrorValueType binaryOpNotApplicable(final BinaryOp node, final ValueType t1, final ValueType t2) {
            return typingError(
                "Operator '" + node.op().symbol() + "' is not applicable for " + t1.name() + " and " + t2.name() + ".",
                node);
        }

        static ErrorValueType unaryOpNotApplicable(final UnaryOp node, final ValueType t) {
            return typingError("Operator '" + node.op().symbol() + "' is not applicable for " + t.name() + ".", node);
        }

        static ErrorValueType functionNotApplicable(final FunctionCall node, final List<ValueType> args) {
            return typingError("The function " + node.function().name() + " is not applicable to the arguments " + args,
                node);
        }

        static ErrorValueType aggregationNotApplicable(final AggregationCall node, final String errorMessage) {
            return typingError(errorMessage, node);
        }

        static ErrorValueType nullishOpNotApplicable(final BinaryOp node, final ValueType t1, final ValueType t2) {
            return typingError("Operator '??' is not applicable for " + t1.name() + " and " + t2.name()
                + ". Types must be compatible, and at most one can be MISSING.", node);
        }

        private ErrorValueType(final List<ExpressionCompileError> errors) {
            m_errors = errors;
        }

        @Override
        public String name() {
            return "ERROR";
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        @Override
        public ValueType baseType() {
            return this;
        }

        @Override
        public ValueType optionalType() {
            return this;
        }

    }
}
