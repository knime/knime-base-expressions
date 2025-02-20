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
import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
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
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.DateDurationComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.FloatComputerResultSupplier;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.IntegerComputerResultSupplier;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;

/**
 * Implementation of expression evaluation based on {@link Computer}.
 *
 * @author Tobias Pietzsch
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class Evaluation {

    private Evaluation() {
    }

    private static final Computer MISSING_CONSTANT_COMPUTER = ctx -> true;

    static Computer evaluate( //
        final Ast expression, //
        final Function<ColumnAccess, Optional<Computer>> columnToComputer, //
        final Function<FlowVarAccess, Optional<Computer>> flowVariableToComputer, //
        final Function<AggregationCall, Optional<Computer>> aggregationToComputer //
    ) throws ExpressionCompileException {
        return expression.accept(new ComputerFactory(columnToComputer, flowVariableToComputer, aggregationToComputer));
    }

    private static final class EvaluationImplementationError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EvaluationImplementationError(final String message) {
            super(message + " (this is an implementation error).");
        }
    }

    private static final class ComputerFactory implements Ast.AstVisitor<Computer, ExpressionCompileException> {

        private final Function<ColumnAccess, Optional<Computer>> m_columnToComputer;

        private final Function<FlowVarAccess, Optional<Computer>> m_flowVariableToComputer;

        private final Function<AggregationCall, Optional<Computer>> m_aggregationToComputer;

        public ComputerFactory(final Function<ColumnAccess, Optional<Computer>> columnToComputer,
            final Function<FlowVarAccess, Optional<Computer>> flowVariableToComputer,
            final Function<AggregationCall, Optional<Computer>> aggregationToComputer) {
            m_columnToComputer = columnToComputer;
            m_flowVariableToComputer = flowVariableToComputer;
            m_aggregationToComputer = aggregationToComputer;
        }

        @Override
        public Computer visit(final ColumnAccess node) throws ExpressionCompileException {
            return m_columnToComputer.apply(node)
                .orElseThrow(() -> new ExpressionCompileException(ExpressionCompileError.missingColumnError(node)));
        }

        @Override
        public Computer visit(final FlowVarAccess node) throws ExpressionCompileException {
            return m_flowVariableToComputer.apply(node).orElseThrow(
                () -> new ExpressionCompileException(ExpressionCompileError.missingFlowVariableError(node)));
        }

        @Override
        public Computer visit(final MissingConstant node) {
            return MISSING_CONSTANT_COMPUTER;
        }

        @Override
        public Computer visit(final BooleanConstant node) {
            return BooleanComputer.ofConstant(node.value());
        }

        @Override
        public IntegerComputer visit(final IntegerConstant node) {
            return IntegerComputer.ofConstant(node.value());
        }

        @Override
        public Computer visit(final FloatConstant node) {
            return FloatComputer.ofConstant(node.value());
        }

        @Override
        public Computer visit(final StringConstant node) {
            return StringComputer.ofConstant(node.value());
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
            } else if (TIME_DURATION.equals(outType.baseType())) {
                return Durations.unary(node.op(), (TimeDurationComputer)arg);
            } else if (DATE_DURATION.equals(outType.baseType())) {
                return Periods.unary(node.op(), (DateDurationComputer)arg);
            }
            throw new EvaluationImplementationError("Unknown output type " + outType.name() + " for unary operation.");
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
            } else if (TIME_DURATION.equals(outType.baseType())) {
                // subtraction of any two TemporalAmounts except LocalDate, or two Durations
                // or multiplication/division of a Duration with an Integer, or Duration
                // plus a Duration
                return Durations.binary(node.op(), arg1, arg2);
            } else if (DATE_DURATION.equals(outType.baseType())) {
                // subtraction of any two LocalDates, or two Periods
                // or multiplication/division of a Period with an Integer
                return Periods.binary(node.op(), arg1, arg2);
            } else if (LOCAL_DATE.equals(outType.baseType())) {
                // LocalDate +/- Period
                return LocalDates.binary(node.op(), arg1, arg2);
            } else if (LOCAL_TIME.equals(outType.baseType())) {
                // LocalTime +/- Duration|Period
                return LocalTimes.binary(node.op(), arg1, arg2);
            } else if (LOCAL_DATE_TIME.equals(outType.baseType())) {
                // LocalDateTime +/- Duration|Period
                return LocalDateTimes.binary(node.op(), arg1, arg2);
            } else if (ZONED_DATE_TIME.equals(outType.baseType())) {
                // ZonedDateTime +/- Duration|Period
                return ZonedDateTimes.binary(node.op(), arg1, arg2);
            }

            throw new EvaluationImplementationError("Unknown output type " + outType.name() + " for binary operation.");
        }

        @Override
        public Computer visit(final FunctionCall node) throws ExpressionCompileException {
            var argComputers = node.args().map(arg -> arg.accept(this));
            return node.function().apply(argComputers);
        }

        @Override
        public Computer visit(final AggregationCall node) throws ExpressionCompileException {
            return m_aggregationToComputer.apply(node).orElseThrow(
                () -> new ExpressionCompileException(ExpressionCompileError.aggregationNotImplemented(node)));
        }

        private static Computer missingFallbackOperatorImpl(final ValueType outputType, final Computer arg1,
            final Computer arg2) {
            // Deferred evaluation to avoid calling missing during setup
            ComputerResultSupplier<Computer> outputComputer = ctx -> arg1.isMissing(ctx) ? arg2 : arg1;

            // Output is missing iff both inputs are missing
            BooleanComputerResultSupplier outputMissing = ctx -> arg1.isMissing(ctx) && arg2.isMissing(ctx);

            // NOSONARs because it otherwise suggests a change that breaks deferred evaluation of get()
            if (BOOLEAN.equals(outputType.baseType())) {
                return BooleanComputer.of( //
                    ctx -> ((BooleanComputer)outputComputer.apply(ctx)).compute(ctx), // NOSONAR
                    outputMissing //
                );
            } else if (STRING.equals(outputType.baseType())) {
                return StringComputer.of( //
                    ctx -> ((StringComputer)outputComputer.apply(ctx)).compute(ctx), // NOSONAR
                    outputMissing //
                );
            } else if (INTEGER.equals(outputType.baseType())) {
                return IntegerComputer.of( //
                    ctx -> ((IntegerComputer)outputComputer.apply(ctx)).compute(ctx), // NOSONAR
                    outputMissing //
                );
            } else if (FLOAT.equals(outputType.baseType())) {
                return FloatComputer.of( //
                    ctx -> toFloat(outputComputer.apply(ctx)).compute(ctx), // NOSONAR
                    outputMissing //
                );
            } else {
                throw new IllegalStateException(
                    "Implementation error: this shouldn't happen if our typing check is correct.");
            }
        }
    }

    // Computer implementations for native types

    private static class Boolean {

        static BooleanComputer unary(final UnaryOperator op, final Computer arg) {
            if (op == UnaryOperator.NOT && arg instanceof BooleanComputer boolArg) {
                var a = toKleenesLogicComputer(boolArg);
                return fromKleenesLogicSupplier(ctx -> KleenesLogic.not(a.apply(ctx)));
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

            BooleanComputerResultSupplier anyMissing = ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx);
            BooleanComputerResultSupplier bothMissing = ctx -> arg1.isMissing(ctx) && arg2.isMissing(ctx);

            // This comparator only runs if both values are non-missing.
            IntegerComputerResultSupplier comparator;
            if (arg1 instanceof FloatComputer || arg2 instanceof FloatComputer) {
                // One is FLOAT -> we do the comparison for FLOAT.
                var a1 = toFloat(arg1);
                var a2 = toFloat(arg2);

                comparator = ctx -> Double.compare(a1.compute(ctx), a2.compute(ctx));
            } else if (arg1 instanceof IntegerComputer a1 && arg2 instanceof IntegerComputer a2) {
                comparator = ctx -> Long.compare(a1.compute(ctx), a2.compute(ctx));
            } else if (arg1 instanceof TimeDurationComputer d1 && arg2 instanceof TimeDurationComputer d2) {
                comparator = ctx -> d1.compute(ctx).compareTo(d2.compute(ctx));
            } else if (arg1 instanceof LocalDateComputer ld1 && arg2 instanceof LocalDateComputer ld2) {
                comparator = ctx -> ld1.compute(ctx).compareTo(ld2.compute(ctx));
            } else if (arg1 instanceof LocalDateTimeComputer ldt1 && arg2 instanceof LocalDateTimeComputer ldt2) {
                comparator = ctx -> ldt1.compute(ctx).compareTo(ldt2.compute(ctx));
            } else if (arg1 instanceof ZonedDateTimeComputer zdt1 && arg2 instanceof ZonedDateTimeComputer zdt2) {
                comparator = ctx -> zdt1.compute(ctx).compareTo(zdt2.compute(ctx));
            } else if (arg1 instanceof LocalTimeComputer lt1 && arg2 instanceof LocalTimeComputer lt2) {
                comparator = ctx -> lt1.compute(ctx).compareTo(lt2.compute(ctx));
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not comparable.");
            }

            BooleanComputerResultSupplier value = switch (op) {
                case LESS_THAN -> ctx -> !anyMissing.applyAsBoolean(ctx) && comparator.applyAsLong(ctx) < 0;
                case LESS_THAN_EQUAL -> ctx -> bothMissing.applyAsBoolean(ctx)
                    || (!anyMissing.applyAsBoolean(ctx) && comparator.applyAsLong(ctx) <= 0);
                case GREATER_THAN -> ctx -> !anyMissing.applyAsBoolean(ctx) && comparator.applyAsLong(ctx) > 0;
                case GREATER_THAN_EQUAL -> ctx -> bothMissing.applyAsBoolean(ctx)
                    || (!anyMissing.applyAsBoolean(ctx) && comparator.applyAsLong(ctx) >= 0);
                default -> throw new EvaluationImplementationError("Binary operator " + op + " is not a comparison.");
            };

            return BooleanComputer.of(value, ctx -> false);
        }

        private static BooleanComputer equality( // NOSONAR - this method is complex but still clear
            final BinaryOperator op, final Computer arg1, final Computer arg2) {
            BooleanComputerResultSupplier valuesEqual;
            if (arg1 == MISSING_CONSTANT_COMPUTER || arg2 == MISSING_CONSTANT_COMPUTER) {
                // One of the values guaranteed to be MISSING -> Only equal if both missing, values are irrelevant
                valuesEqual = ctx -> false;
            } else if (arg1 instanceof BooleanComputer a1 && arg2 instanceof BooleanComputer a2) {
                valuesEqual = ctx -> a1.compute(ctx) == a2.compute(ctx);
            } else if (arg1 instanceof StringComputer a1 && arg2 instanceof StringComputer a2) {
                valuesEqual = ctx -> Objects.equals(a1.compute(ctx), a2.compute(ctx));
            } else if (arg1 instanceof FloatComputer || arg2 instanceof FloatComputer) {
                // NB: Cast Integer to float if necessary
                var a1 = toFloat(arg1);
                var a2 = toFloat(arg2);
                valuesEqual = ctx -> a1.compute(ctx) == a2.compute(ctx); // NOSONAR - we want the equality test here
            } else if (arg1 instanceof IntegerComputer a1 && arg2 instanceof IntegerComputer a2) {
                valuesEqual = ctx -> a1.compute(ctx) == a2.compute(ctx);
            } else if (arg1 instanceof TimeDurationComputer d1 && arg2 instanceof TimeDurationComputer d2) {
                valuesEqual = ctx -> d1.compute(ctx).equals(d2.compute(ctx));
            } else if (arg1 instanceof DateDurationComputer p1 && arg2 instanceof DateDurationComputer p2) {
                valuesEqual = ctx -> p1.compute(ctx).equals(p2.compute(ctx));
            } else if (arg1 instanceof LocalDateComputer ld1 && arg2 instanceof LocalDateComputer ld2) {
                valuesEqual = ctx -> ld1.compute(ctx).equals(ld2.compute(ctx));
            } else if (arg1 instanceof LocalTimeComputer t1 && arg2 instanceof LocalTimeComputer t2) {
                valuesEqual = ctx -> t1.compute(ctx).equals(t2.compute(ctx));
            } else if (arg1 instanceof LocalDateTimeComputer dt1 && arg2 instanceof LocalDateTimeComputer dt2) {
                valuesEqual = ctx -> dt1.compute(ctx).equals(dt2.compute(ctx));
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not equality comparable.");
            }

            BooleanComputerResultSupplier equal = //
                ctx -> (arg1.isMissing(ctx) && arg2.isMissing(ctx)) // both missing -> true
                    || (!arg1.isMissing(ctx) && !arg2.isMissing(ctx) && valuesEqual.applyAsBoolean(ctx)); // any missing -> false

            return switch (op) {
                case EQUAL_TO -> BooleanComputer.of(equal, ctx -> false);
                case NOT_EQUAL_TO -> BooleanComputer.of(ctx -> !equal.applyAsBoolean(ctx), ctx -> false);
                default -> throw new EvaluationImplementationError(
                    "Binary operator " + op + " is not a equality check.");
            };
        }

        private static BooleanComputer logical(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            var a1 = toKleenesLogicComputer((BooleanComputer)arg1);
            var a2 = toKleenesLogicComputer((BooleanComputer)arg2);

            return switch (op) {
                case CONDITIONAL_AND -> fromKleenesLogicSupplier(ctx -> KleenesLogic.and(a1.apply(ctx), a2.apply(ctx)));
                case CONDITIONAL_OR -> fromKleenesLogicSupplier(ctx -> KleenesLogic.or(a1.apply(ctx), a2.apply(ctx)));
                default -> throw new EvaluationImplementationError("Binary operator " + op + " is not logical.");
            };

        }

        private static ComputerResultSupplier<KleenesLogic> toKleenesLogicComputer(final BooleanComputer c) {
            return ctx -> {
                if (c.isMissing(ctx)) {
                    return KleenesLogic.UNKNOWN;
                } else if (c.compute(ctx)) {
                    return KleenesLogic.TRUE;
                } else {
                    return KleenesLogic.FALSE;
                }
            };
        }

        private static BooleanComputer
            fromKleenesLogicSupplier(final ComputerResultSupplier<KleenesLogic> logicSupplier) {
            return BooleanComputer.of( //
                ctx -> logicSupplier.apply(ctx) == KleenesLogic.TRUE, //
                ctx -> logicSupplier.apply(ctx) == KleenesLogic.UNKNOWN //
            );
        }
    }

    private static class Integer {

        static IntegerComputer unary(final UnaryOperator op, final IntegerComputer arg) {
            IntegerComputerResultSupplier value = switch (op) {
                case MINUS -> ctx -> -arg.compute(ctx);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return IntegerComputer.of(value, arg::isMissing);
        }

        static IntegerComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            var a1 = (IntegerComputer)arg1;
            var a2 = (IntegerComputer)arg2;
            IntegerComputerResultSupplier value = switch (op) {
                case PLUS -> ctx -> a1.compute(ctx) + a2.compute(ctx);
                case MINUS -> ctx -> a1.compute(ctx) - a2.compute(ctx);
                case MULTIPLY -> ctx -> a1.compute(ctx) * a2.compute(ctx);
                case FLOOR_DIVIDE -> safeFloorDivide(a1, a2);
                case EXPONENTIAL -> (final EvaluationContext ctx) -> (long)Math.pow(a1.compute(ctx), a2.compute(ctx));
                case REMAINDER -> safeRemainder(a1, a2);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return IntegerComputer.of(value, (final EvaluationContext w) -> a1.isMissing(w) || a2.isMissing(w));
        }

        static IntegerComputerResultSupplier safeFloorDivide(final IntegerComputer a1, final IntegerComputer a2) {
            return ctx -> {
                var divisor = a2.compute(ctx);
                if (divisor == 0) {
                    ctx.addWarning("INTEGER division returned 0 because divisor was 0.");
                    return 0;
                }
                return a1.compute(ctx) / divisor;
            };
        }

        static IntegerComputerResultSupplier safeRemainder(final IntegerComputer a1, final IntegerComputer a2) {
            return ctx -> {
                var divisor = a2.compute(ctx);
                if (divisor == 0) {
                    ctx.addWarning("INTEGER modulo returned 0 because divisor was 0.");
                    return 0;
                }
                return a1.compute(ctx) % divisor;
            };
        }
    }

    private static class Float {

        static FloatComputer unary(final UnaryOperator op, final FloatComputer arg) {
            FloatComputerResultSupplier value = switch (op) {
                case MINUS -> ctx -> -arg.compute(ctx);
                default -> throw unsupportedOutputForOpError(op, INTEGER);
            };
            return FloatComputer.of(value, arg::isMissing);
        }

        static FloatComputer binary(final BinaryOperator op, final FloatComputer arg1, final FloatComputer arg2) {
            FloatComputerResultSupplier value = switch (op) {
                case PLUS -> ctx -> arg1.compute(ctx) + arg2.compute(ctx);
                case MINUS -> ctx -> arg1.compute(ctx) - arg2.compute(ctx);
                case MULTIPLY -> ctx -> arg1.compute(ctx) * arg2.compute(ctx);
                case DIVIDE -> ctx -> {
                    var divisor = arg2.compute(ctx);
                    var returnValue = arg1.compute(ctx) / divisor;
                    if (divisor == 0) { // NOSONAR equality should be fine here
                        ctx.addWarning("FLOAT division returned %s because divisor was 0."
                            .formatted(String.valueOf(returnValue).replace("Infinity", "INFINITY")));
                    }
                    return returnValue;
                };
                case EXPONENTIAL -> ctx -> Math.pow(arg1.compute(ctx), arg2.compute(ctx));
                case REMAINDER -> ctx -> {
                    var divisor = arg2.compute(ctx);
                    var returnValue = arg1.compute(ctx) % divisor;
                    if (divisor == 0) { // NOSONAR equality should be fine here
                        ctx.addWarning("FLOAT modulo returned %s because divisor was 0."
                            .formatted(String.valueOf(returnValue).replace("Infinity", "INFINITY")));
                    }
                    return returnValue;
                };
                default -> throw unsupportedOutputForOpError(op, FLOAT);
            };
            return FloatComputer.of(value, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }
    }

    private static class Strings {
        static ComputerResultSupplier<String> stringRepr(final Computer computer) {
            return ctx -> Computer.stringRepresentation(computer, ctx);
        }

        static StringComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            if (op != BinaryOperator.PLUS) {
                throw unsupportedOutputForOpError(op, STRING);
            }
            var a1 = Strings.stringRepr(arg1);
            var a2 = Strings.stringRepr(arg2);
            return StringComputer.of(ctx -> a1.apply(ctx) + a2.apply(ctx), ctx -> false);
        }
    }

    private static class Durations {

        static TimeDurationComputer unary(final UnaryOperator op, final TimeDurationComputer arg) {
            ComputerResultSupplier<Duration> value = switch (op) {
                case MINUS -> ctx -> arg.compute(ctx).negated();
                default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
            };
            return TimeDurationComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `TIME_DURATION`", ex);
                }
            }, arg::isMissing);
        }

        static TimeDurationComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            ComputerResultSupplier<Duration> value;
            if (arg1 instanceof TimeDurationComputer d1 && arg2 instanceof TimeDurationComputer d2) {
                value = switch (op) {
                    case PLUS -> ctx -> d1.compute(ctx).plus(d2.compute(ctx));
                    case MINUS -> ctx -> d1.compute(ctx).minus(d2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
                };
            } else if (arg1 instanceof TimeDurationComputer d1 && arg2 instanceof IntegerComputer i2) {
                value = switch (op) {
                    case MULTIPLY -> ctx -> d1.compute(ctx).multipliedBy(i2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
                };
            } else if (arg1 instanceof IntegerComputer i1 && arg2 instanceof TimeDurationComputer d2) {
                value = switch (op) {
                    case MULTIPLY -> ctx -> d2.compute(ctx).multipliedBy(i1.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
                };
            } else if (arg1 instanceof LocalTimeComputer t1 && arg2 instanceof LocalTimeComputer t2) {
                value = switch (op) {
                    case MINUS -> ctx -> Duration.between(t2.compute(ctx), t1.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
                };
            } else if (arg1 instanceof LocalDateTimeComputer dt1 && arg2 instanceof LocalDateTimeComputer dt2) {
                value = switch (op) {
                    case MINUS -> ctx -> Duration.between(dt2.compute(ctx), dt1.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
                };
            } else if (arg1 instanceof ZonedDateTimeComputer zdt1 && arg2 instanceof ZonedDateTimeComputer zdt2) {
                value = switch (op) {
                    case MINUS -> ctx -> Duration.between(zdt2.compute(ctx), zdt1.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, TIME_DURATION);
                };
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not duration compatible.");
            }

            return TimeDurationComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the result of range representable by `TIME_DURATION`", ex);
                }
            }, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }
    }

    private static class Periods {

        static DateDurationComputer unary(final UnaryOperator op, final DateDurationComputer arg) {
            ComputerResultSupplier<Period> value = switch (op) {
                case MINUS -> ctx -> arg.compute(ctx).negated();
                default -> throw unsupportedOutputForOpError(op, DATE_DURATION);
            };
            return DateDurationComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `DATE_DURATION`", ex);
                }
            }, arg::isMissing);
        }

        static DateDurationComputer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            ComputerResultSupplier<Period> value;
            if (arg1 instanceof DateDurationComputer p1 && arg2 instanceof DateDurationComputer p2) {
                value = switch (op) {
                    case PLUS -> ctx -> p1.compute(ctx).plus(p2.compute(ctx));
                    case MINUS -> ctx -> p1.compute(ctx).minus(p2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, DATE_DURATION);
                };
            } else if (arg1 instanceof DateDurationComputer p1 && arg2 instanceof IntegerComputer i2) {
                value = switch (op) {
                    case MULTIPLY -> ctx -> p1.compute(ctx).multipliedBy((int)i2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, DATE_DURATION);
                };
            } else if (arg1 instanceof IntegerComputer i1 && arg2 instanceof DateDurationComputer p2) {
                value = switch (op) {
                    case MULTIPLY -> ctx -> p2.compute(ctx).multipliedBy((int)i1.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, DATE_DURATION);
                };
            } else if (arg1 instanceof LocalDateComputer ld1 && arg2 instanceof LocalDateComputer ld2) {
                value = switch (op) {
                    case MINUS -> ctx -> Period.between(ld2.compute(ctx), ld1.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, DATE_DURATION);
                };
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not period compatible.");
            }

            return DateDurationComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `DATE_DURATION`", ex);
                }
            }, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }
    }

    private static class LocalDates {

        static Computer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            ComputerResultSupplier<LocalDate> value;
            if (arg1 instanceof LocalDateComputer ld1 && arg2 instanceof DateDurationComputer p2) {
                value = switch (op) {
                    case PLUS -> ctx -> ld1.compute(ctx).plus(p2.compute(ctx));
                    case MINUS -> ctx -> ld1.compute(ctx).minus(p2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, LOCAL_DATE);
                };
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not date compatible.");
            }

            return LocalDateComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `LOCAL_DATE`", ex);
                }
            }, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }
    }

    private static class LocalTimes {

        static Computer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            ComputerResultSupplier<LocalTime> value;
            if (arg1 instanceof LocalTimeComputer lt1 && arg2 instanceof TimeDurationComputer d2) {
                value = switch (op) {
                    case PLUS -> ctx -> lt1.compute(ctx).plus(d2.compute(ctx));
                    case MINUS -> ctx -> lt1.compute(ctx).minus(d2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, LOCAL_TIME);
                };
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not time compatible.");
            }

            return LocalTimeComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `LOCAL_TIME`", ex);
                }
            }, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }
    }

    private static class LocalDateTimes {

        static Computer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            ComputerResultSupplier<LocalDateTime> value;
            if (arg1 instanceof LocalDateTimeComputer ldt1 && arg2 instanceof TimeDurationComputer d2) {
                value = switch (op) {
                    case PLUS -> ctx -> ldt1.compute(ctx).plus(d2.compute(ctx));
                    case MINUS -> ctx -> ldt1.compute(ctx).minus(d2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, LOCAL_DATE_TIME);
                };
            } else if (arg1 instanceof LocalDateTimeComputer ldt1 && arg2 instanceof DateDurationComputer p2) {
                value = switch (op) {
                    case PLUS -> ctx -> ldt1.compute(ctx).plus(p2.compute(ctx));
                    case MINUS -> ctx -> ldt1.compute(ctx).minus(p2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, LOCAL_DATE_TIME);
                };
            } else {
                throw new EvaluationImplementationError(
                    "Arguments of " + arg1.getClass() + " and " + arg2.getClass() + " are not date-time compatible.");
            }

            return LocalDateTimeComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `LOCAL_DATE_TIME`", ex);
                }
            }, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }
    }

    private static class ZonedDateTimes {

        static Computer binary(final BinaryOperator op, final Computer arg1, final Computer arg2) {
            ComputerResultSupplier<ZonedDateTime> value;
            if (arg1 instanceof ZonedDateTimeComputer zdt1 && arg2 instanceof TimeDurationComputer d2) {
                value = switch (op) {
                    case PLUS -> ctx -> zdt1.compute(ctx).plus(d2.compute(ctx));
                    case MINUS -> ctx -> zdt1.compute(ctx).minus(d2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, ZONED_DATE_TIME);
                };
            } else if (arg1 instanceof ZonedDateTimeComputer zdt1 && arg2 instanceof DateDurationComputer p2) {
                value = switch (op) {
                    case PLUS -> ctx -> zdt1.compute(ctx).plus(p2.compute(ctx));
                    case MINUS -> ctx -> zdt1.compute(ctx).minus(p2.compute(ctx));
                    default -> throw unsupportedOutputForOpError(op, ZONED_DATE_TIME);
                };
            } else {
                throw new EvaluationImplementationError("Arguments of " + arg1.getClass() + " and " + arg2.getClass()
                    + " are not zoned date-time compatible.");
            }

            return ZonedDateTimeComputer.of(ctx -> {
                try {
                    return value.apply(ctx);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException(
                        "Result was outside the range of values representable by `ZONED_DATE_TIME`", ex);
                }
            }, ctx -> arg1.isMissing(ctx) || arg2.isMissing(ctx));
        }

    }

    private static EvaluationImplementationError unsupportedOutputForOpError(final Object operator,
        final ValueType outputType) {
        return new EvaluationImplementationError(
            "Output of operator " + operator + " cannot be " + outputType.name() + ".");
    }
}
