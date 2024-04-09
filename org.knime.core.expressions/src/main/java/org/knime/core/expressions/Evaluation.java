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
 *   Mar 20, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.STRING;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

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
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Expressions.MissingColumnError;

/**
 * Implementation of expression evaluation based on {@link Computer}.
 *
 * @author Tobias Pietzsch
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class Evaluation {

    private Evaluation() {
    }

    private static final Computer MISSING_CONSTANT_COMPUTER = () -> true;

    static Computer evaluate(final Ast expression, final Function<ColumnAccess, Optional<Computer>> columnToComputer)
        throws MissingColumnError {
        return expression.accept(new ComputerFactory(columnToComputer));
    }

    private static final class EvaluationImplementationError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EvaluationImplementationError(final String message) {
            super(message + " (this is an implementation error)");
        }
    }

    private static final class ComputerFactory implements Ast.AstVisitor<Computer, MissingColumnError> {

        private final Function<ColumnAccess, Optional<Computer>> m_columnToComputer;

        public ComputerFactory(final Function<ColumnAccess, Optional<Computer>> columnToComputer) {
            m_columnToComputer = columnToComputer;
        }

        @Override
        public Computer visit(final ColumnAccess node) throws MissingColumnError {
            return m_columnToComputer.apply(node).orElseThrow(() -> new MissingColumnError(node.name()));
        }

        @Override
        public Computer visit(final MissingConstant node) {
            return MISSING_CONSTANT_COMPUTER;
        }

        @Override
        public Computer visit(final BooleanConstant node) {
            return BooleanComputer.of(node::value, () -> false);
        }

        @Override
        public IntegerComputer visit(final IntegerConstant node) {
            return IntegerComputer.of(node::value, () -> false);
        }

        @Override
        public Computer visit(final FloatConstant node) {
            return FloatComputer.of(node::value, () -> false);
        }

        @Override
        public Computer visit(final StringConstant node) {
            return StringComputer.of(node::value, () -> false);
        }

        @Override
        public Computer visit(final UnaryOp node) throws MissingColumnError {
            var arg = node.arg().accept(this);

            var outType = Typing.getType(node);
            if (BOOLEAN.equals(outType.baseType())) {
                return Boolean.unary(node.op(), arg);
            } else if (INTEGER.equals(outType.baseType())) {
                return Integer.unary(node.op(), (IntegerComputer)arg);
            } else if (FLOAT.equals(outType.baseType())) {
                return Float.unary(node.op(), Float.cast(arg));
            }
            throw new EvaluationImplementationError("Unknown output type " + outType.name() + " for unary operation");
        }

        @Override
        public Computer visit(final BinaryOp node) throws MissingColumnError {
            var arg1 = node.arg1().accept(this);
            var arg2 = node.arg2().accept(this);

            var outType = Typing.getType(node);
            if (BOOLEAN.equals(outType.baseType())) {
                return Boolean.binary(node.op(), arg1, arg2);
            } else if (INTEGER.equals(outType.baseType())) {
                return Integer.binary(node.op(), arg1, arg2);
            } else if (FLOAT.equals(outType.baseType())) {
                return Float.binary(node.op(), Float.cast(arg1), Float.cast(arg2));
            } else if (STRING.equals(outType.baseType())) {
                return Strings.binary(node.op(), arg1, arg2);
            }

            throw new EvaluationImplementationError("Unknown output type " + outType.name() + " for binary operation");
        }

        @Override
        public Computer visit(final FunctionCall node) throws MissingColumnError {
            // Create computers for the arguments
            var argComputers = new ArrayList<Computer>(node.args().size());
            for (var arg : node.args()) {
                argComputers.add(arg.accept(this));
            }

            // Apply the function
            return Typing.getFunctionImpl(node).apply(argComputers);
        }
    }

    // Computer implementations for native types

    private static class Boolean {

        static BooleanComputer unary(final UnaryOperator op, final Computer arg) {
            if (op == UnaryOperator.NOT && arg instanceof BooleanComputer boolArg) {
                var a = toKleenesLogicComputer(boolArg);
                return fromKleenesLogicSupplier(() -> KleenesLogic.not(a.get()));
            } else {
                throw unsupportedOutputForOpError(op, BOOLEAN);
            }
        }

        static BooleanComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            if (op.isOrderingComparison()) {
                return comparison(op, arg1, arg2);
            } else if (op.isEqualityComparison()) {
                return equality(op, arg1, arg2);
            } else if (op.isLogical()) {
                return logical(op, arg1, arg2);
            }
            throw unsupportedOutputForOpError(op, BOOLEAN);
        }

        private static BooleanComputer comparison( // NOSONAR - this method is complex but still clear
            final BinaryOperator op, final Computer arg1, final Computer arg2) {
            BooleanSupplier anyMissing = () -> arg1.isMissing() || arg2.isMissing();
            BooleanSupplier bothMissing = () -> arg1.isMissing() && arg2.isMissing();

            BooleanSupplier value;
            if (arg1 instanceof FloatComputer || arg2 instanceof FloatComputer) {
                // One is FLOAT -> we do the comparison for FLOAT
                var a1 = Float.cast(arg1);
                var a2 = Float.cast(arg2);
                value = switch (op) { // NOSONAR
                    case LESS_THAN -> () -> !anyMissing.getAsBoolean() && a1.compute() < a2.compute();
                    case LESS_THAN_EQUAL -> () -> bothMissing.getAsBoolean()
                        || (!anyMissing.getAsBoolean() && a1.compute() <= a2.compute());
                    case GREATER_THAN -> () -> !anyMissing.getAsBoolean() && a1.compute() > a2.compute();
                    case GREATER_THAN_EQUAL -> () -> bothMissing.getAsBoolean()
                        || (!anyMissing.getAsBoolean() && a1.compute() >= a2.compute());
                    default -> throw new EvaluationImplementationError(
                        "Binary operator " + op + " is not a comparison");
                };
            } else {
                // Both are INTEGER
                var a1 = (IntegerComputer)arg1;
                var a2 = (IntegerComputer)arg2;
                value = switch (op) {
                    case LESS_THAN -> () -> !anyMissing.getAsBoolean() && a1.compute() < a2.compute();
                    case LESS_THAN_EQUAL -> () -> !anyMissing.getAsBoolean() && a1.compute() <= a2.compute();
                    case GREATER_THAN -> () -> !anyMissing.getAsBoolean() && a1.compute() > a2.compute();
                    case GREATER_THAN_EQUAL -> () -> !anyMissing.getAsBoolean() && a1.compute() >= a2.compute();
                    default -> throw new EvaluationImplementationError(
                        "Binary operator " + op + " is not a comparison");
                };
            }
            return BooleanComputer.of(value, () -> false);
        }

        private static BooleanComputer equality( // NOSONAR - this method is complex but still clear
            final BinaryOperator op, final Computer arg1, final Computer arg2) {
            BooleanSupplier valuesEqual;
            if (arg1 == MISSING_CONSTANT_COMPUTER || arg2 == MISSING_CONSTANT_COMPUTER) {
                // One of the values guaranteed to be MISSING -> Only equal if both missing, values are irrelevant
                valuesEqual = () -> false;
            } else if (arg1 instanceof BooleanComputer a1 && arg2 instanceof BooleanComputer a2) {
                valuesEqual = () -> a1.compute() == a2.compute();
            } else if (arg1 instanceof StringComputer a1 && arg2 instanceof StringComputer a2) {
                valuesEqual = () -> Objects.equals(a1.compute(), a2.compute());
            } else if (arg1 instanceof FloatComputer || arg2 instanceof FloatComputer) {
                // NB: Cast Integer to float if necessary
                var a1 = Float.cast(arg1);
                var a2 = Float.cast(arg2);
                valuesEqual = () -> a1.compute() == a2.compute(); // NOSONAR - we want the equality test here
            } else if (arg1 instanceof IntegerComputer a1 && arg2 instanceof IntegerComputer a2) {
                valuesEqual = () -> a1.compute() == a2.compute();
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not equality comparable");
            }

            BooleanSupplier equal = () -> (arg1.isMissing() && arg2.isMissing()) // both missing -> true
                || (!arg1.isMissing() && !arg2.isMissing() && valuesEqual.getAsBoolean()); // any missing -> false

            return switch (op) {
                case EQUAL_TO -> BooleanComputer.of(equal, () -> false);
                case NOT_EQUAL_TO -> BooleanComputer.of(() -> !equal.getAsBoolean(), () -> false);
                default -> throw new EvaluationImplementationError(
                    "Binary operator " + op + " is not a equality check");
            };
        }

        private static BooleanComputer logical(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            var a1 = toKleenesLogicComputer((BooleanComputer)arg1);
            var a2 = toKleenesLogicComputer((BooleanComputer)arg2);

            return switch (op) {
                case CONDITIONAL_AND -> fromKleenesLogicSupplier(() -> KleenesLogic.and(a1.get(), a2.get()));
                case CONDITIONAL_OR -> fromKleenesLogicSupplier(() -> KleenesLogic.or(a1.get(), a2.get()));
                default -> throw new EvaluationImplementationError("Binary operator " + op + " is not logical");
            };

        }

        private static Supplier<KleenesLogic> toKleenesLogicComputer(final BooleanComputer c) {
            return () -> {
                if (c.isMissing()) {
                    return KleenesLogic.UNKNOWN;
                } else if (c.compute()) {
                    return KleenesLogic.TRUE;
                } else {
                    return KleenesLogic.FALSE;
                }
            };
        }

        private static BooleanComputer fromKleenesLogicSupplier(final Supplier<KleenesLogic> logicSupplier) {
            return BooleanComputer.of( //
                () -> logicSupplier.get() == KleenesLogic.TRUE, //
                () -> logicSupplier.get() == KleenesLogic.UNKNOWN //
            );
        }
    }

    private static class Integer {

        static IntegerComputer unary(final UnaryOperator op, final IntegerComputer arg) {
            LongSupplier value = switch (op) {
                case MINUS -> () -> -arg.compute();
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return IntegerComputer.of(value, arg::isMissing);
        }

        static IntegerComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            var a1 = (IntegerComputer)arg1;
            var a2 = (IntegerComputer)arg2;
            LongSupplier value = switch (op) {
                case PLUS -> () -> a1.compute() + a2.compute();
                case MINUS -> () -> a1.compute() - a2.compute();
                case MULTIPLY -> () -> a1.compute() * a2.compute();
                case FLOOR_DIVIDE -> safeFloorDivide(a1, a2);
                case EXPONENTIAL -> () -> (long)Math.pow(a1.compute(), a2.compute());
                case REMAINDER -> safeRemainder(a1, a2);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return IntegerComputer.of(value, () -> a1.isMissing() || a2.isMissing());
        }

        static LongSupplier safeFloorDivide(final IntegerComputer a1, final IntegerComputer a2) {
            return () -> {
                var divisor = a2.compute();
                if (divisor == 0) {
                    return 0;
                }
                return a1.compute() / divisor;
            };
        }

        static LongSupplier safeRemainder(final IntegerComputer a1, final IntegerComputer a2) {
            return () -> {
                var divisor = a2.compute();
                if (divisor == 0) {
                    return 0;
                }
                return a1.compute() % divisor;
            };
        }
    }

    private static class Float {

        static FloatComputer cast(final Computer computer) {
            if (computer instanceof IntegerComputer intComputer) {
                return FloatComputer.of(intComputer::compute, intComputer::isMissing);
            } else {
                return (FloatComputer)computer;
            }
        }

        static FloatComputer unary(final UnaryOperator op, final FloatComputer arg) {
            DoubleSupplier value = switch (op) {
                case MINUS -> () -> -arg.compute();
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return FloatComputer.of(value, arg::isMissing);
        }

        static FloatComputer binary(final BinaryOperator op, final FloatComputer arg1, final FloatComputer arg2) {
            DoubleSupplier value = switch (op) {
                case PLUS -> () -> arg1.compute() + arg2.compute();
                case MINUS -> () -> arg1.compute() - arg2.compute();
                case MULTIPLY -> () -> arg1.compute() * arg2.compute();
                case DIVIDE -> () -> arg1.compute() / arg2.compute();
                case EXPONENTIAL -> () -> Math.pow(arg1.compute(), arg2.compute());
                case REMAINDER -> () -> arg1.compute() % arg2.compute();
                default -> throw unsupportedOutputForOpError(op, FLOAT);
            };
            return FloatComputer.of(value, () -> arg1.isMissing() || arg2.isMissing());
        }
    }

    private static class Strings {
        static Supplier<String> stringRepr(final Computer computer) {
            Supplier<String> value;
            if (computer instanceof BooleanComputer c) {
                value = () -> c.compute() ? "true" : "false";
            } else if (computer instanceof IntegerComputer c) {
                value = () -> Long.toString(c.compute());
            } else if (computer instanceof FloatComputer c) {
                value = () -> Double.toString(c.compute());
            } else if (computer instanceof StringComputer c) {
                value = c::compute;
            } else {
                throw new EvaluationImplementationError(
                    "Argument of " + computer.getClass() + " cannot be cast to STRING");
            }
            return () -> computer.isMissing() ? "MISSING" : value.get();
        }

        static StringComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            if (op != BinaryOperator.PLUS) {
                throw unsupportedOutputForOpError(op, STRING);
            }
            var a1 = Strings.stringRepr(arg1);
            var a2 = Strings.stringRepr(arg2);
            return StringComputer.of(() -> a1.get() + a2.get(), () -> false);
        }
    }

    private static EvaluationImplementationError unsupportedOutputForOpError(final Object operator,
        final ValueType outputType) {
        return new EvaluationImplementationError("Output of operator " + operator + " cannot be " + outputType.name());
    }
}
