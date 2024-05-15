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

import static org.knime.core.expressions.Computer.toFloat;
import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.STRING;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

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
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Expressions.ExpressionCompileException;

/**
 * Implementation of expression evaluation based on {@link Computer}.
 *
 * @author Tobias Pietzsch
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class Evaluation {

    private Evaluation() {
    }

    private static final Computer MISSING_CONSTANT_COMPUTER = wml -> true;

    static Computer evaluate(final Ast expression, final Function<ColumnAccess, Optional<Computer>> columnToComputer,
        final Function<FlowVarAccess, Optional<Computer>> flowVariableToComputer) throws ExpressionCompileException {
        return expression.accept(new ComputerFactory(columnToComputer, flowVariableToComputer));
    }

    private static final class EvaluationImplementationError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EvaluationImplementationError(final String message) {
            super(message + " (this is an implementation error)");
        }
    }

    private static final class ComputerFactory implements Ast.AstVisitor<Computer, ExpressionCompileException> {

        private final Function<ColumnAccess, Optional<Computer>> m_columnToComputer;

        private final Function<FlowVarAccess, Optional<Computer>> m_flowVariableToComputer;

        public ComputerFactory(final Function<ColumnAccess, Optional<Computer>> columnToComputer,
            final Function<FlowVarAccess, Optional<Computer>> flowVariableToComputer) {
            m_columnToComputer = columnToComputer;
            m_flowVariableToComputer = flowVariableToComputer;
        }

        @Override
        public Computer visit(final ColumnAccess node) throws ExpressionCompileException {
            return m_columnToComputer.apply(node)
                .orElseThrow(() -> new ExpressionCompileException(ExpressionCompileError.missingColumnError(node)));
        }

        @Override
        public Computer visit(final FlowVarAccess node) throws ExpressionCompileException {
            return m_flowVariableToComputer.apply(node).orElseThrow(
                () -> new ExpressionCompileException(ExpressionCompileError.missingControlFlowVariableError(node)));
        }

        @Override
        public Computer visit(final MissingConstant node) {
            return MISSING_CONSTANT_COMPUTER;
        }

        @Override
        public Computer visit(final BooleanConstant node) {
            return BooleanComputer.of(wml -> node.value(), wml -> false);
        }

        @Override
        public IntegerComputer visit(final IntegerConstant node) {
            return IntegerComputer.of(wml -> node.value(), wml -> false);
        }

        @Override
        public Computer visit(final FloatConstant node) {
            return FloatComputer.of(wml -> node.value(), wml -> false);
        }

        @Override
        public Computer visit(final StringConstant node) {
            return StringComputer.of(wml -> node.value(), wml -> false);
        }

        @Override
        public Computer visit(final UnaryOp node) throws ExpressionCompileException {
            var arg = node.arg().accept(this);

            var outType = Typing.getType(node);
            if (BOOLEAN.equals(outType.baseType())) {
                return Boolean.unary(node.op(), arg);
            } else if (INTEGER.equals(outType.baseType())) {
                return Integer.unary(node.op(), (IntegerComputer)arg);
            } else if (FLOAT.equals(outType.baseType())) {
                return Float.unary(node.op(), toFloat(arg));
            }
            throw new EvaluationImplementationError("Unknown output type " + outType.name() + " for unary operation");
        }

        @Override
        public Computer visit(final BinaryOp node) throws ExpressionCompileException {
            var arg1 = node.arg1().accept(this);
            var arg2 = node.arg2().accept(this);

            var outType = Typing.getType(node);

            if (node.op() == BinaryOperator.MISSING_FALLBACK) {
                return ComputerFactory.missingFallbackOperatorImpl(outType, arg1, arg2);
            } else if (BOOLEAN.equals(outType.baseType())) {
                return Boolean.binary(node.op(), arg1, arg2);
            } else if (INTEGER.equals(outType.baseType())) {
                return Integer.binary(node.op(), arg1, arg2);
            } else if (FLOAT.equals(outType.baseType())) {
                return Float.binary(node.op(), toFloat(arg1), toFloat(arg2));
            } else if (STRING.equals(outType.baseType())) {
                return Strings.binary(node.op(), arg1, arg2);
            }

            throw new EvaluationImplementationError("Unknown output type " + outType.name() + " for binary operation");
        }

        @Override
        public Computer visit(final FunctionCall node) throws ExpressionCompileException {
            // Create computers for the arguments
            var argComputers = new ArrayList<Computer>(node.args().size());
            for (var arg : node.args()) {
                argComputers.add(arg.accept(this));
            }

            // Apply the function
            return Typing.getFunctionImpl(node).apply(argComputers);
        }

        private static Computer missingFallbackOperatorImpl(final ValueType outputType, final Computer arg1,
            final Computer arg2) {
            // Deferred evaluation to avoid calling missing during setup
            Function<WarningMessageListener, Computer> outputComputer = wml -> arg1.isMissing(wml) ? arg2 : arg1;

            // Output is missing iff both inputs are missing
            Predicate<WarningMessageListener> outputMissing = wml -> arg1.isMissing(wml) && arg2.isMissing(wml);

            // NOSONARs because it otherwise suggests a change that breaks deferred evaluation of get()
            if (BOOLEAN.equals(outputType.baseType())) {
                return BooleanComputer.of( //
                    wml -> ((BooleanComputer)outputComputer.apply(wml)).compute(wml), // NOSONAR
                    outputMissing //
                );
            } else if (STRING.equals(outputType.baseType())) {
                return StringComputer.of( //
                    wml -> ((StringComputer)outputComputer.apply(wml)).compute(wml), // NOSONAR
                    outputMissing //
                );
            } else if (INTEGER.equals(outputType.baseType())) {
                return IntegerComputer.of( //
                    wml -> ((IntegerComputer)outputComputer.apply(wml)).compute(wml), // NOSONAR
                    outputMissing //
                );
            } else if (FLOAT.equals(outputType.baseType())) {
                return FloatComputer.of( //
                    wml -> toFloat(outputComputer.apply(wml)).compute(wml), // NOSONAR
                    outputMissing //
                );
            } else {
                throw new IllegalStateException(
                    "Implementation error: this shouldn't happen if our typing check is correct");
            }
        }
    }

    // Computer implementations for native types

    private static class Boolean {

        static BooleanComputer unary(final UnaryOperator op, final Computer arg) {
            if (op == UnaryOperator.NOT && arg instanceof BooleanComputer boolArg) {
                var a = toKleenesLogicComputer(boolArg);
                return fromKleenesLogicSupplier(wml -> KleenesLogic.not(a.apply(wml)));
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
            Predicate<WarningMessageListener> anyMissing = wml -> arg1.isMissing(wml) || arg2.isMissing(wml);
            Predicate<WarningMessageListener> bothMissing = wml -> arg1.isMissing(wml) && arg2.isMissing(wml);

            Predicate<WarningMessageListener> value;
            if (arg1 instanceof FloatComputer || arg2 instanceof FloatComputer) {
                // One is FLOAT -> we do the comparison for FLOAT
                var a1 = toFloat(arg1);
                var a2 = toFloat(arg2);
                value = switch (op) { // NOSONAR
                    case LESS_THAN -> wml -> !anyMissing.test(wml) && a1.compute(wml) < a2.compute(wml);
                    case LESS_THAN_EQUAL -> wml -> bothMissing.test(wml)
                        || (!anyMissing.test(wml) && a1.compute(wml) <= a2.compute(wml));
                    case GREATER_THAN -> wml -> !anyMissing.test(wml) && a1.compute(wml) > a2.compute(wml);
                    case GREATER_THAN_EQUAL -> wml -> bothMissing.test(wml)
                        || (!anyMissing.test(wml) && a1.compute(wml) >= a2.compute(wml));
                    default -> throw new EvaluationImplementationError(
                        "Binary operator " + op + " is not a comparison");
                };
            } else {
                // Both are INTEGER
                var a1 = (IntegerComputer)arg1;
                var a2 = (IntegerComputer)arg2;
                value = switch (op) {
                    case LESS_THAN -> wml -> !anyMissing.test(wml) && a1.compute(wml) < a2.compute(wml);
                    case LESS_THAN_EQUAL -> wml -> !anyMissing.test(wml) && a1.compute(wml) <= a2.compute(wml);
                    case GREATER_THAN -> wml -> !anyMissing.test(wml) && a1.compute(wml) > a2.compute(wml);
                    case GREATER_THAN_EQUAL -> wml -> !anyMissing.test(wml) && a1.compute(wml) >= a2.compute(wml);
                    default -> throw new EvaluationImplementationError(
                        "Binary operator " + op + " is not a comparison");
                };
            }
            return BooleanComputer.of(value, wml -> false);
        }

        private static BooleanComputer equality( // NOSONAR - this method is complex but still clear
            final BinaryOperator op, final Computer arg1, final Computer arg2) {
            Predicate<WarningMessageListener> valuesEqual;
            if (arg1 == MISSING_CONSTANT_COMPUTER || arg2 == MISSING_CONSTANT_COMPUTER) {
                // One of the values guaranteed to be MISSING -> Only equal if both missing, values are irrelevant
                valuesEqual = wml -> false;
            } else if (arg1 instanceof BooleanComputer a1 && arg2 instanceof BooleanComputer a2) {
                valuesEqual = wml -> a1.compute(wml) == a2.compute(wml);
            } else if (arg1 instanceof StringComputer a1 && arg2 instanceof StringComputer a2) {
                valuesEqual = wml -> Objects.equals(a1.compute(wml), a2.compute(wml));
            } else if (arg1 instanceof FloatComputer || arg2 instanceof FloatComputer) {
                // NB: Cast Integer to float if necessary
                var a1 = toFloat(arg1);
                var a2 = toFloat(arg2);
                valuesEqual = wml -> a1.compute(wml) == a2.compute(wml); // NOSONAR - we want the equality test here
            } else if (arg1 instanceof IntegerComputer a1 && arg2 instanceof IntegerComputer a2) {
                valuesEqual = wml -> a1.compute(wml) == a2.compute(wml);
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not equality comparable");
            }

            Predicate<WarningMessageListener> equal = //
                wml -> (arg1.isMissing(wml) && arg2.isMissing(wml)) // both missing -> true
                    || (!arg1.isMissing(wml) && !arg2.isMissing(wml) && valuesEqual.test(wml)); // any missing -> false

            return switch (op) {
                case EQUAL_TO -> BooleanComputer.of(equal, wml -> false);
                case NOT_EQUAL_TO -> BooleanComputer.of(wml -> !equal.test(wml), wml -> false);
                default -> throw new EvaluationImplementationError(
                    "Binary operator " + op + " is not a equality check");
            };
        }

        private static BooleanComputer logical(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            var a1 = toKleenesLogicComputer((BooleanComputer)arg1);
            var a2 = toKleenesLogicComputer((BooleanComputer)arg2);

            return switch (op) {
                case CONDITIONAL_AND -> fromKleenesLogicSupplier(wml -> KleenesLogic.and(a1.apply(wml), a2.apply(wml)));
                case CONDITIONAL_OR -> fromKleenesLogicSupplier(wml -> KleenesLogic.or(a1.apply(wml), a2.apply(wml)));
                default -> throw new EvaluationImplementationError("Binary operator " + op + " is not logical");
            };

        }

        private static Function<WarningMessageListener, KleenesLogic> toKleenesLogicComputer(final BooleanComputer c) {
            return wml -> {
                if (c.isMissing(wml)) {
                    return KleenesLogic.UNKNOWN;
                } else if (c.compute(wml)) {
                    return KleenesLogic.TRUE;
                } else {
                    return KleenesLogic.FALSE;
                }
            };
        }

        private static BooleanComputer
            fromKleenesLogicSupplier(final Function<WarningMessageListener, KleenesLogic> logicSupplier) {
            return BooleanComputer.of( //
                wml -> logicSupplier.apply(wml) == KleenesLogic.TRUE, //
                wml -> logicSupplier.apply(wml) == KleenesLogic.UNKNOWN //
            );
        }
    }

    private static class Integer {

        static IntegerComputer unary(final UnaryOperator op, final IntegerComputer arg) {
            ToLongFunction<WarningMessageListener> value = switch (op) {
                case MINUS -> wml -> -arg.compute(wml);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return IntegerComputer.of(value, arg::isMissing);
        }

        static IntegerComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            var a1 = (IntegerComputer)arg1;
            var a2 = (IntegerComputer)arg2;
            ToLongFunction<WarningMessageListener> value = switch (op) {
                case PLUS -> wml -> a1.compute(wml) + a2.compute(wml);
                case MINUS -> wml -> a1.compute(wml) - a2.compute(wml);
                case MULTIPLY -> wml -> a1.compute(wml) * a2.compute(wml);
                case FLOOR_DIVIDE -> safeFloorDivide(a1, a2);
                case EXPONENTIAL -> (
                    final WarningMessageListener wml) -> (long)Math.pow(a1.compute(wml), a2.compute(wml));
                case REMAINDER -> safeRemainder(a1, a2);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return IntegerComputer.of(value, (final WarningMessageListener w) -> a1.isMissing(w) || a2.isMissing(w));
        }

        static ToLongFunction<WarningMessageListener> safeFloorDivide(final IntegerComputer a1,
            final IntegerComputer a2) {
            return wml -> {
                var divisor = a2.compute(wml);
                if (divisor == 0) {
                    return 0;
                }
                return a1.compute(wml) / divisor;
            };
        }

        static ToLongFunction<WarningMessageListener> safeRemainder(final IntegerComputer a1,
            final IntegerComputer a2) {
            return wml -> {
                var divisor = a2.compute(wml);
                if (divisor == 0) {
                    return 0;
                }
                return a1.compute(wml) % divisor;
            };
        }
    }

    private static class Float {

        static FloatComputer unary(final UnaryOperator op, final FloatComputer arg) {
            ToDoubleFunction<WarningMessageListener> value = switch (op) {
                case MINUS -> wml -> -arg.compute(wml);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return FloatComputer.of(value, arg::isMissing);
        }

        static FloatComputer binary(final BinaryOperator op, final FloatComputer arg1, final FloatComputer arg2) {
            ToDoubleFunction<WarningMessageListener> value = switch (op) {
                case PLUS -> wml -> arg1.compute(wml) + arg2.compute(wml);
                case MINUS -> wml -> arg1.compute(wml) - arg2.compute(wml);
                case MULTIPLY -> wml -> arg1.compute(wml) * arg2.compute(wml);
                case DIVIDE -> wml -> arg1.compute(wml) / arg2.compute(wml);
                case EXPONENTIAL -> wml -> Math.pow(arg1.compute(wml), arg2.compute(wml));
                case REMAINDER -> wml -> arg1.compute(wml) % arg2.compute(wml);
                default -> throw unsupportedOutputForOpError(op, FLOAT);
            };
            return FloatComputer.of(value, wml -> arg1.isMissing(wml) || arg2.isMissing(wml));
        }
    }

    private static class Strings {
        static Function<WarningMessageListener, String> stringRepr(final Computer computer) {
            Function<WarningMessageListener, String> value;
            if (computer instanceof BooleanComputer c) {
                value = wml -> c.compute(wml) ? "true" : "false";
            } else if (computer instanceof IntegerComputer c) {
                value = wml -> Long.toString(c.compute(wml));
            } else if (computer instanceof FloatComputer c) {
                value = wml -> Double.toString(c.compute(wml));
            } else if (computer instanceof StringComputer c) {
                value = c::compute;
            } else {
                throw new EvaluationImplementationError(
                    "Argument of " + computer.getClass() + " cannot be cast to STRING");
            }
            return wml -> computer.isMissing(wml) ? "MISSING" : value.apply(wml);
        }

        static StringComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            if (op != BinaryOperator.PLUS) {
                throw unsupportedOutputForOpError(op, STRING);
            }
            var a1 = Strings.stringRepr(arg1);
            var a2 = Strings.stringRepr(arg2);
            return StringComputer.of(wml -> a1.apply(wml) + a2.apply(wml), wml -> false);
        }
    }

    private static EvaluationImplementationError unsupportedOutputForOpError(final Object operator,
        final ValueType outputType) {
        return new EvaluationImplementationError("Output of operator " + operator + " cannot be " + outputType.name());
    }
}
