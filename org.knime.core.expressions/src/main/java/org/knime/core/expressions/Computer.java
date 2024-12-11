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
 *   Mar 22, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.STRING;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.function.Function;

/**
 * A supplier of computation results for expressions.
 *
 * @author Tobias Pietzsch
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public interface Computer {

    /**
     * @param ctx a {@link EvaluationContext} to report warnings
     * @return <code>true</code> if the result is "MISSING"
     * @throws ExpressionEvaluationException if the expression could not be evaluated
     */
    boolean isMissing(EvaluationContext ctx) throws ExpressionEvaluationException;

    /**
     * A supplier for the boolean result of an expression. Used to write down the implementation of
     * {@link BooleanComputer#compute(EvaluationContext)} and {@link Computer#isMissing(EvaluationContext)} as a lambda
     * function.
     */
    @FunctionalInterface
    interface BooleanComputerResultSupplier {
        /**
         * Applies the computer with the given context.
         *
         * @param ctx the evaluation context
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        boolean applyAsBoolean(EvaluationContext ctx) throws ExpressionEvaluationException;
    }

    /**
     * A supplier for the integer result of an expression. Used to write down the implementation of the method
     * {@link IntegerComputer#compute(EvaluationContext)} as a lambda function.
     */
    @FunctionalInterface
    interface IntegerComputerResultSupplier {
        /**
         * Applies the computer with the given context.
         *
         * @param ctx the evaluation context
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        long applyAsLong(EvaluationContext ctx) throws ExpressionEvaluationException;
    }

    /**
     * A supplier for the float result of an expression. Used to write down the implementation of the method
     * {@link FloatComputer#compute(EvaluationContext)} as a lambda function.
     */
    @FunctionalInterface
    interface FloatComputerResultSupplier {
        /**
         * Applies the computer with the given context.
         *
         * @param ctx the evaluation context
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        double applyAsDouble(EvaluationContext ctx) throws ExpressionEvaluationException;
    }

    /**
     * A supplier for the result of an expression. Used to write down the implementation of the method
     * {@link StringComputer#compute(EvaluationContext)} as a lambda function.
     *
     * @param <O> the type of the result
     */
    @FunctionalInterface
    interface ComputerResultSupplier<O> {
        /**
         * Applies the computer with the given context.
         *
         * @param ctx the evaluation context
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        O apply(EvaluationContext ctx) throws ExpressionEvaluationException;
    }

    /** {@link Computer} for {@link ValueType#BOOLEAN} and {@link ValueType#OPT_BOOLEAN} */
    interface BooleanComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        boolean compute(EvaluationContext ctx) throws ExpressionEvaluationException;

        /**
         * Helper method to create a {@link BooleanComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link BooleanComputer}
         */
        static BooleanComputer of(final BooleanComputerResultSupplier value,
            final BooleanComputerResultSupplier missing) {

            return new BooleanComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public boolean compute(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return value.applyAsBoolean(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#INTEGER} and {@link ValueType#OPT_INTEGER} */
    interface IntegerComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        long compute(EvaluationContext ctx) throws ExpressionEvaluationException;

        /**
         * Helper method to create an {@link IntegerComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return an {@link IntegerComputer}
         */
        static IntegerComputer of(final IntegerComputerResultSupplier value,
            final BooleanComputerResultSupplier missing) {

            return new IntegerComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public long compute(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return value.applyAsLong(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#FLOAT} and {@link ValueType#OPT_FLOAT} */
    interface FloatComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        double compute(EvaluationContext ctx) throws ExpressionEvaluationException;

        /**
         * Helper method to create a {@link FloatComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link FloatComputer}
         */
        static FloatComputer of(final FloatComputerResultSupplier value, final BooleanComputerResultSupplier missing) {

            return new FloatComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public double compute(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return value.applyAsDouble(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#STRING} and {@link ValueType#OPT_STRING} */
    interface StringComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         * @throws ExpressionEvaluationException if the expression could not be evaluated
         */
        String compute(EvaluationContext ctx) throws ExpressionEvaluationException;

        /**
         * Helper method to create a {@link StringComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link StringComputer}
         */
        static StringComputer of(final ComputerResultSupplier<String> value,
            final BooleanComputerResultSupplier missing) {

            return new StringComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public String compute(final EvaluationContext ctx) throws ExpressionEvaluationException {
                    return value.apply(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#LOCAL_DATE} and {@link ValueType#OPT_LOCAL_DATE}. */
    interface LocalDateComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        LocalDate compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link LocalDateComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link LocalDateComputer}
         */
        static LocalDateComputer of(final Function<EvaluationContext, LocalDate> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new LocalDateComputer() {
                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public LocalDate compute(final EvaluationContext ctx) {
                    return value.apply(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#LOCAL_TIME} and {@link ValueType#OPT_LOCAL_TIME}. */
    interface LocalTimeComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        LocalTime compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link LocalTimeComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link LocalTimeComputer}
         */
        static LocalTimeComputer of(final Function<EvaluationContext, LocalTime> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new LocalTimeComputer() {
                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public LocalTime compute(final EvaluationContext ctx) {
                    return value.apply(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#LOCAL_DATE_TIME} and {@link ValueType#OPT_LOCAL_DATE_TIME}. */
    interface LocalDateTimeComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        LocalDateTime compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link LocalDateTimeComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link LocalDateTimeComputer}
         */
        static LocalDateTimeComputer of(final Function<EvaluationContext, LocalDateTime> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new LocalDateTimeComputer() {
                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public LocalDateTime compute(final EvaluationContext ctx) {
                    return value.apply(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#ZONED_DATE_TIME} and {@link ValueType#OPT_ZONED_DATE_TIME}. */
    interface ZonedDateTimeComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        ZonedDateTime compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link ZonedDateTimeComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link ZonedDateTimeComputer}
         */
        static ZonedDateTimeComputer of(final Function<EvaluationContext, ZonedDateTime> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new ZonedDateTimeComputer() {
                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public ZonedDateTime compute(final EvaluationContext ctx) {
                    return value.apply(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#DURATION} and {@link ValueType#OPT_DURATION}. */
    interface DurationComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        Duration compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link DurationComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link DurationComputer}
         */
        static DurationComputer of(final Function<EvaluationContext, Duration> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new DurationComputer() {
                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public Duration compute(final EvaluationContext ctx) {
                    return value.apply(ctx);
                }
            };
        }
    }

    /** {@link Computer} for {@link ValueType#PERIOD} and {@link ValueType#OPT_PERIOD}. */
    interface PeriodComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        Period compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link PeriodComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link PeriodComputer}
         */
        static PeriodComputer of(final Function<EvaluationContext, Period> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new PeriodComputer() {
                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public Period compute(final EvaluationContext ctx) {
                    return value.apply(ctx);
                }
            };
        }
    }

    /**
     * Helper method to get the return type of the computer {@link ValueType}.
     *
     * @param computer
     * @return a {@link ValueType}
     */
    static ValueType getReturnTypeFromComputer(final Computer computer) {
        if (computer instanceof BooleanComputer) {
            return BOOLEAN;
        } else if (computer instanceof FloatComputer) {
            return FLOAT;
        } else if (computer instanceof IntegerComputer) {
            return INTEGER;
        } else if (computer instanceof StringComputer) {
            return STRING;
        } else {
            return MISSING;
        }
    }

    /**
     * Helper method to create a Typed Computer from a Computer Supplier and the intended return type {@link ValueType}.
     * This is helpful when the computer supplier needs to compute in order to supply the resulting computer
     *
     * @param computerSupplier a supplier for the computer that computes the result and therefore needs to be delayed
     * @param returnType the intended return type of the computer; will be cast to the baseType if necessary
     * @return a {@link Computer} of the {@link ValueType} of return type
     */
    static Computer createTypedResultComputer(final ComputerResultSupplier<Computer> computerSupplier,
        final ValueType returnType) {

        BooleanComputerResultSupplier isMissing = ctx -> computerSupplier.apply(ctx).isMissing(ctx);

        if (returnType.baseType() == BOOLEAN) {
            return BooleanComputer.of(ctx -> ((BooleanComputer)computerSupplier.apply(ctx)).compute(ctx), // NOSONAR  - method reference is not possible due to delayed computation
                isMissing);
        }
        if (returnType.baseType() == INTEGER) {
            return IntegerComputer.of(ctx -> Math.round(toFloat(computerSupplier.apply(ctx)).compute(ctx)), isMissing);
        }
        if (returnType.baseType() == FLOAT) {
            return FloatComputer.of(ctx -> toFloat(computerSupplier.apply(ctx)).compute(ctx), isMissing);
        }
        if (returnType.baseType() == STRING) {
            return StringComputer.of(ctx -> ((StringComputer)computerSupplier.apply(ctx)).compute(ctx), // NOSONAR - method reference is not possible due to delayed computation
                isMissing);
        }

        throw new IllegalStateException("Type of Expression is unknown: " + returnType
            + " not in FLOAT, INTEGER, BOOLEAN or STRING. This in an implementation error.");
    }

    /**
     * Helper method to cast a computer to a {@link FloatComputer}, when it is an {@link IntegerComputer} or
     * {@link FloatComputer}.
     *
     * @param computer the computer to cast to a {@link FloatComputer}
     * @return the result of the computation
     * @throws IllegalStateException if the computer is not a numeric computer
     */
    static FloatComputer toFloat(final Computer computer) {
        if (computer instanceof FloatComputer c) {
            return c;
        } else if (computer instanceof IntegerComputer c) {
            return FloatComputer.of(c::compute, c::isMissing);
        }
        throw new IllegalArgumentException(
            "Cannot cast computer to FLOAT: " + computer + ". This in an implementation error.");
    }
}
