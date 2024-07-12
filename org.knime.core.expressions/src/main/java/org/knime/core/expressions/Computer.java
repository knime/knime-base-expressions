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

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

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
     */
    boolean isMissing(EvaluationContext ctx);

    /** {@link Computer} for {@link ValueType#BOOLEAN} and {@link ValueType#OPT_BOOLEAN} */
    interface BooleanComputer extends Computer {

        /**
         * @param ctx a {@link EvaluationContext} to report warnings
         * @return the result of the expression evaluation
         */
        boolean compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link BooleanComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link BooleanComputer}
         */
        static BooleanComputer of(final ToBooleanFunction<EvaluationContext> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new BooleanComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public boolean compute(final EvaluationContext ctx) {
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
         */
        long compute(EvaluationContext ctx);

        /**
         * Helper method to create an {@link IntegerComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return an {@link IntegerComputer}
         */
        static IntegerComputer of(final ToLongFunction<EvaluationContext> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new IntegerComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public long compute(final EvaluationContext ctx) {
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
         */
        double compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link FloatComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link FloatComputer}
         */
        static FloatComputer of(final ToDoubleFunction<EvaluationContext> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new FloatComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public double compute(final EvaluationContext ctx) {
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
         */
        String compute(EvaluationContext ctx);

        /**
         * Helper method to create a {@link StringComputer}.
         *
         * @param value a supplier for the {@link #compute(EvaluationContext)} result
         * @param missing a supplier that returns {@code true} if the result {@link #isMissing(EvaluationContext)}
         * @return a {@link StringComputer}
         */
        static StringComputer of(final Function<EvaluationContext, String> value,
            final ToBooleanFunction<EvaluationContext> missing) {

            return new StringComputer() {

                @Override
                public boolean isMissing(final EvaluationContext ctx) {
                    return missing.applyAsBoolean(ctx);
                }

                @Override
                public String compute(final EvaluationContext ctx) {
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
    static Computer createTypedResultComputer(final Function<EvaluationContext, Computer> computerSupplier,
        final ValueType returnType) {

        ToBooleanFunction<EvaluationContext> isMissing = ctx -> computerSupplier.apply(ctx).isMissing(ctx);

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
