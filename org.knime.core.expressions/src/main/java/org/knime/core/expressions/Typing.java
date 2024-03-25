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
import static org.knime.core.expressions.ValueType.STRING;

import java.util.Optional;
import java.util.function.Function;

import org.knime.core.expressions.Ast.BinaryOp;
import org.knime.core.expressions.Ast.BinaryOperator;
import org.knime.core.expressions.Ast.BooleanConstant;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.FloatConstant;
import org.knime.core.expressions.Ast.FunctionCall;
import org.knime.core.expressions.Ast.IntegerConstant;
import org.knime.core.expressions.Ast.MissingConstant;
import org.knime.core.expressions.Ast.StringConstant;
import org.knime.core.expressions.Ast.UnaryOp;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.Expressions.ExpressionError;
import org.knime.core.expressions.Expressions.MissingColumnError;
import org.knime.core.expressions.Expressions.TypingError;

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

    static ValueType inferTypes(final Ast root, final Function<ColumnAccess, Optional<ValueType>> columnType)
        throws TypingError, MissingColumnError { // NOSONAR: Sonar is wrong
        try {
            return Ast.putDataRecursive(root, TYPE_DATA_KEY, new TypingVisitor(columnType));
        } catch (TypingError | MissingColumnError ex) {
            throw ex;
        } catch (ExpressionError ex) {
            // Note this cannot happen because the visitor only throws the errors above
            throw new IllegalStateException(ex);
        }
    }

    static ValueType getType(final Ast node) {
        Object type = node.data(TYPE_DATA_KEY);
        if (type instanceof ValueType astType) {
            return astType;
        } else {
            throw new IllegalArgumentException("The node " + node + " has no type.");
        }
    }

    private static final class TypingVisitor implements Ast.AstVisitor<ValueType, ExpressionError> {

        private final Function<ColumnAccess, Optional<ValueType>> m_columnType;

        TypingVisitor(final Function<ColumnAccess, Optional<ValueType>> columnType) {
            m_columnType = columnType;
        }

        @Override
        public ValueType visit(final MissingConstant missingConstant) throws ExpressionError {
            return MISSING;
        }

        @Override
        public ValueType visit(final BooleanConstant node) throws ExpressionError {
            return BOOLEAN;
        }

        @Override
        public ValueType visit(final IntegerConstant node) throws ExpressionError {
            return INTEGER;
        }

        @Override
        public ValueType visit(final FloatConstant node) throws ExpressionError {
            return FLOAT;
        }

        @Override
        public ValueType visit(final StringConstant node) throws ExpressionError {
            return STRING;
        }

        @Override
        public ValueType visit(final ColumnAccess node) throws ExpressionError {
            return m_columnType.apply(node).orElseThrow(() -> new MissingColumnError(node.name()));
        }

        @Override
        public ValueType visit(final BinaryOp node) throws ExpressionError { // NOSONAR - this method is not too complex
            var op = node.op();
            var t1 = getType(node.arg1());
            var t2 = getType(node.arg2());

            if (op == BinaryOperator.PLUS && isAnyString(t1, t2) && !isAnyMissing(t1, t2)) {
                return STRING;
            } else if (op.isArithmetic() && isAllNumeric(t1, t2)) {
                // Arithmetic operation
                return arithmeticType(op, t1, t2);
            } else if (op.isOrderingComparison() && isAllNumeric(t1, t2)) {
                // Ordering comparison
                return BOOLEAN;
            } else if (op.isEqualityComparison()) {
                // Equality comparison
                checkEqualityTypes(t1, t2);
                return BOOLEAN;
            } else if (op.isLogical() && isAllBoolean(t1, t2)) {
                // Logical operation
                return BOOLEAN(t1.isOptional() || t2.isOptional());
            } else {
                throw new TypingError(
                    "Operator '" + op.symbol() + "' is not applicable for " + t1 + " and " + t2 + ".");
            }
        }

        @Override
        public ValueType visit(final UnaryOp node) throws ExpressionError {
            var op = node.op();
            var type = getType(node.arg());

            if (op == UnaryOperator.MINUS && isNumeric(type)) {
                return type;
            } else if (op == UnaryOperator.NOT && BOOLEAN.equals(type.baseType())) {
                return type;
            } else {
                throw new TypingError("Operator '" + op.symbol() + "' is not applicable for " + type + ".");
            }
        }

        @Override
        public ValueType visit(final FunctionCall node) throws ExpressionError {
            throw new IllegalStateException("functions are not yet implemented");
        }

        private static ValueType arithmeticType(final BinaryOperator op, final ValueType typeA, final ValueType typeB)
            throws TypingError {
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
                throw new TypingError(
                    "Operator '" + op.symbol() + "' is not applicable for " + baseTypeA + " and " + baseTypeB + ".");
            } else if (INTEGER.equals(baseTypeA) && INTEGER.equals(baseTypeB)) {
                // Both INTEGER
                return INTEGER(optional);
            } else {
                // At least one FLOAT
                return FLOAT(optional);
            }
        }

        /** @throws TypingError if the given types cannot be compared with an equality operator */
        private static void checkEqualityTypes(final ValueType typeA, final ValueType typeB) throws TypingError {
            if (typeA.baseType().equals(typeB.baseType())) {
                // Same type or one is the missing type extension of the other
                return;
            }
            if (MISSING.equals(typeA) || MISSING.equals(typeB)) {
                // Any type can be compared with MISSING
                return;
            }
            if (isNumeric(typeA) && isNumeric(typeB)) {
                // All numbers can be compared with each other
                return;
            }
            throw new TypingError("Equality comparison is not applicable for " + typeA + " and " + typeB + ".");
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
}
