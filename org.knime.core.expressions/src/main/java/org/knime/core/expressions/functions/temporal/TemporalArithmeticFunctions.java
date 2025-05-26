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
 *   Mar 19, 2025 (benjaminwilhelm): created
 */
package org.knime.core.expressions.functions.temporal;

import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_DATE_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_INTEGER_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_TIME_DURATION_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasDateInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.hasTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isDateDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isTimeDurationOrOpt;
import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.DateDurationComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.IntegerComputerResultSupplier;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.TemporalComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.SignatureUtils;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * Implementation of built-in functions for performing arithmetic operations on temporal data.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
public final class TemporalArithmeticFunctions {

    private TemporalArithmeticFunctions() {
    }

    // constants for common literals, e.g. argument names
    private static final String START_ARG = "start";

    private static final String END_ARG = "end";

    private static final String DURATION_ARG = "duration";

    /** The Temporal - Arithmetic category */
    public static final OperatorCategory CATEGORY_ARITHMETIC =
        new OperatorCategory(TemporalFunctionUtils.TEMPORAL_META_CATEGORY_NAME, "Arithmetic",
            "Functions for performing arithmetic operations on temporal data.");

    public static final ExpressionFunction DATE_DURATION_BETWEEN = new ExpressionFunction() {

        private static final List<Arg> ARGS = List.of( //
            arg(START_ARG, "The start time", hasDateInformationOrOpt()), //
            arg(END_ARG, "The end time", hasDateInformationOrOpt()) //
        );

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(ARGS, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            // Check that both arguments are of the same type and that that type has time information
            var startType = argTypes.get(START_ARG);
            var endType = argTypes.get(END_ARG);

            if (!startType.baseType().equals(endType.baseType())) {
                return ReturnResult
                    .failure("Both arguments must be of the same base type, but one was %s and the other was %s"
                        .formatted(startType.baseType(), endType.baseType()));
            }

            var startHasDateInformation = hasDateInformationOrOpt().matches(startType);

            if (!startHasDateInformation) {
                return ReturnResult.failure("%s does not have date information".formatted(startType.baseType()));
            }

            return ReturnResult.success(DATE_DURATION(anyOptional(argTypes)));
        }

        @Override
        public String name() {
            return "date_duration_between";
        }

        @Override
        public OperatorDescription description() {
            var description = """
                    Calculates the `DATE_DURATION` between two date-time values of the same type that
                    have date information. The time part of the date-time values will be ignored, as
                    will any time zone information.

                    If either of the provided dates is missing, the function returns `MISSING`.
                    """;

            var examples = """
                    * `date_duration_between(parse_date("1970-01-01"), parse_date("1970-01-02"))` returns `P1D`
                    * `date_duration_between(parse_date("1970-01-02"), parse_date("1970-01-01"))` returns `-P1D`
                    """;

            var arguments = Arg.toOperatorDescription(ARGS);

            var returnDesc = "A `DATE_DURATION` representing the duration between the two date parts";

            var keywords = List.of("interval", "duration", "period", "date");

            return new OperatorDescription( //
                name(), //
                description, //
                examples, //
                arguments, //
                RETURN_DATE_DURATION_MISSING, //
                returnDesc, //
                keywords, //
                CATEGORY_ARITHMETIC.fullName(), //
                OperatorDescription.FUNCTION_ENTRY_TYPE //
            );
        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            ComputerResultSupplier<Period> valueSupplier = ctx -> {
                var start = ((TemporalComputer)args.get(START_ARG)).compute(ctx);
                var end = ((TemporalComputer)args.get(END_ARG)).compute(ctx);

                var startDate = LocalDate.from(start);
                var endDate = LocalDate.from(end);

                return Period.between(startDate, endDate);
            };

            return DateDurationComputer.of( //
                valueSupplier, //
                anyMissing(args) //
            );
        }
    };

    public static final ExpressionFunction TIME_DURATION_BETWEEN = new ExpressionFunction() {

        private static final List<Arg> ARGS = List.of( //
            arg(START_ARG, "The start time", hasTimeInformationOrOpt()), //
            arg(END_ARG, "The end time", hasTimeInformationOrOpt()) //
        );

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(ARGS, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            // Check that both arguments are of the same type and that that type has time information
            var startType = argTypes.get(START_ARG);
            var endType = argTypes.get(END_ARG);

            if (!startType.baseType().equals(endType.baseType())) {
                return ReturnResult
                    .failure("Both arguments must be of the same base type, but one was %s and the other was %s"
                        .formatted(startType.baseType(), endType.baseType()));
            }

            var startHasTimeInformation = hasTimeInformationOrOpt().matches(startType);

            if (!startHasTimeInformation) {
                return ReturnResult.failure("%s does not have time information".formatted(startType.baseType()));
            }

            return ReturnResult.success(TIME_DURATION(anyOptional(argTypes)));
        }

        @Override
        public String name() {
            return "time_duration_between";
        }

        @Override
        public OperatorDescription description() {
            var description = """
                    Calculates the `TIME_DURATION` between two date-time values of the same type that
                    have time information.

                    If either of the provided times is missing, the function returns `MISSING`.
                    """;

            var examples = """
                    * `time_duration_between(parse_time("00:00:00"), parse_time("01:00:00"))` returns `PT1H`
                    * `time_duration_between(parse_time("01:00:00"), parse_time("00:00:00"))` returns `-PT1H`
                    """;

            var arguments = Arg.toOperatorDescription(ARGS);

            var returnDesc = "A `TIME_DURATION` representing the duration between the two date-times";

            var keywords = List.of("interval", "duration", "difference");

            return new OperatorDescription( //
                name(), //
                description, //
                examples, //
                arguments, //
                RETURN_TIME_DURATION_MISSING, //
                returnDesc, //
                keywords, //
                CATEGORY_ARITHMETIC.fullName(), //
                OperatorDescription.FUNCTION_ENTRY_TYPE //
            );
        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            ComputerResultSupplier<Duration> valueSupplier = ctx -> {
                var start = ((TemporalComputer)args.get(START_ARG)).compute(ctx);
                var end = ((TemporalComputer)args.get(END_ARG)).compute(ctx);

                try {
                    return Duration.between(start, end);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException("Duration is too large to be represented.", ex);
                }
            };

            return TimeDurationComputer.of( //
                valueSupplier, //
                anyMissing(args) //
            );
        }
    };

    public static final ExpressionFunction ADD_TIME_DURATION = functionBuilder() //
        .name("add_time_duration") //
        .description("""
                Adds a `TIME_DURATION` to a date-time value with time information.

                If either the date-time or the duration is missing, the function returns `MISSING`. It is
                possible that the resulting time is too large to be represented, in which case the node execution fails
                with an error.
                """) //
        .examples("""
                * `add_time_duration(parse_time("01:02:03"), parse_time_duration("PT1H"))` returns `02:02:03`
                * `add_time_duration(parse_time("00:00:00"), parse_time_duration("PT1H30M"))` returns `01:30:00`
                * `add_time_duration(parse_datetime("1970-01-01T12:30:00"), parse_time_duration("PT1H20S"))` \
                returns `1970-01-01T12:31:20`
                """) //
        .keywords("add", "time", "duration", "interval") //
        .category(CATEGORY_ARITHMETIC) //
        .args( //
            arg(START_ARG, "The time to add the duration to.", hasTimeInformationOrOpt()), //
            arg(DURATION_ARG, "The duration to add to the time.", isTimeDurationOrOpt()) //
        ) //
        .returnType("The input value with the duration added", hasTimeInformationOrOpt().allowed(),
            args -> anyOptional(args) ? args.get(START_ARG).optionalType() : args.get(START_ARG).baseType()) //
        .impl(TemporalArithmeticFunctions::addTimeDurationImpl) //
        .build();

    private static Computer addTimeDurationImpl(final Arguments<Computer> args) {
        var startComputer = (TemporalComputer)args.get(START_ARG);

        ComputerResultSupplier<Temporal> valueSupplier = ctx -> {
            var startTime = startComputer.compute(ctx);
            var duration = ((TimeDurationComputer)args.get(DURATION_ARG)).compute(ctx);

            try {
                return startTime.plus(duration);
            } catch (ArithmeticException | DateTimeException ex) {
                throw new ExpressionEvaluationException("Resulting date-time is too large to be represented.", ex);
            }
        };

        BooleanComputerResultSupplier isMissing = anyMissing(args);

        if (startComputer instanceof LocalTimeComputer) {
            return LocalTimeComputer.of( //
                ctx -> (LocalTime)valueSupplier.apply(ctx), //
                isMissing //
            );
        } else if (startComputer instanceof LocalDateTimeComputer) {
            return LocalDateTimeComputer.of( //
                ctx -> (LocalDateTime)valueSupplier.apply(ctx), //
                isMissing //
            );
        } else if (startComputer instanceof ZonedDateTimeComputer) {
            return ZonedDateTimeComputer.of( //
                ctx -> (ZonedDateTime)valueSupplier.apply(ctx), //
                isMissing //
            );
        } else {
            throw new IllegalArgumentException(
                "Unsupported temporal type computer: " + startComputer.getClass().getName());
        }
    }

    public static final ExpressionFunction ADD_DATE_DURATION = functionBuilder() //
        .name("add_date_duration") //
        .description("""
                Adds a `DATE_DURATION` to a date-time value with date information.

                If either the date-time or the duration is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `add_date_duration(parse_date("1970-01-01"), parse_date_duration("P1D"))` returns `1970-01-02`
                * `add_date_duration(parse_date("1970-01-01"), parse_date_duration("P1M"))` returns `1970-02-01`
                """) //
        .keywords("add", "date", "duration", "interval") //
        .category(CATEGORY_ARITHMETIC) //
        .args( //
            arg(START_ARG, "The date-time to add the duration to.", hasDateInformationOrOpt()), //
            arg(DURATION_ARG, "The duration to add to the date-time.", isDateDurationOrOpt()) //
        ) //
        .returnType("The input value with the duration added", hasDateInformationOrOpt().allowed(),
            args -> anyOptional(args) ? args.get(START_ARG).optionalType() : args.get(START_ARG).baseType()) //
        .impl(TemporalArithmeticFunctions::addDateDurationImpl) //
        .build();

    private static Computer addDateDurationImpl(final Arguments<Computer> args) {
        var startComputer = (TemporalComputer)args.get(START_ARG);

        ComputerResultSupplier<Temporal> valueSupplier = ctx -> {
            var startDate = startComputer.compute(ctx);
            var duration = ((DateDurationComputer)args.get(DURATION_ARG)).compute(ctx);

            try {
                return startDate.plus(duration);
            } catch (ArithmeticException | DateTimeException ex) {
                throw new ExpressionEvaluationException("Resulting date-time is too large to be represented.", ex);
            }
        };

        BooleanComputerResultSupplier isMissing = anyMissing(args);

        if (startComputer instanceof LocalDateComputer) {
            return LocalDateComputer.of( //
                ctx -> (LocalDate)valueSupplier.apply(ctx), //
                isMissing //
            );
        } else if (startComputer instanceof LocalDateTimeComputer) {
            return LocalDateTimeComputer.of( //
                ctx -> (LocalDateTime)valueSupplier.apply(ctx), //
                isMissing //
            );
        } else if (startComputer instanceof ZonedDateTimeComputer) {
            return ZonedDateTimeComputer.of( //
                ctx -> (ZonedDateTime)valueSupplier.apply(ctx), //
                isMissing //
            );
        } else {
            throw new IllegalArgumentException(
                "Unsupported temporal type computer: " + startComputer.getClass().getName());
        }
    }

    private static class UnitsBetweenExpressionFunction implements ExpressionFunction {

        private final ChronoUnit m_unit;

        private final String m_description;

        private final String m_examples;

        private static final List<Arg> ARGS = List.of( //
            arg(START_ARG, "The start date-time", hasDateInformationOrOpt()), //
            arg(END_ARG, "The end date-time", hasDateInformationOrOpt()) //
        );

        UnitsBetweenExpressionFunction(final ChronoUnit unit, final String description, final String examples) {
            m_unit = unit;
            m_description = description;
            m_examples = examples;
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(ARGS, positionalArguments, namedArguments);
        }

        @Override
        public String name() {
            return "%s_between".formatted(m_unit.toString().toLowerCase(Locale.ROOT));
        }

        @Override
        public OperatorDescription description() {
            return new OperatorDescription( //
                name(), //
                m_description, //
                m_examples, //
                Arg.toOperatorDescription(ARGS), //
                RETURN_INTEGER_MISSING, //
                "An integer representing the number of %ss between the two date-times"
                    .formatted(m_unit.toString().toLowerCase(Locale.ROOT)), //
                List.of("interval", "duration", "period", "date", "difference"), //
                CATEGORY_ARITHMETIC.fullName(), //
                OperatorDescription.FUNCTION_ENTRY_TYPE //
            );
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            var startType = argTypes.get(START_ARG);
            var endType = argTypes.get(END_ARG);

            if (!startType.baseType().equals(endType.baseType())) {
                return ReturnResult
                    .failure("Both arguments must be of the same base type, but one was %s and the other was %s"
                        .formatted(startType.baseType(), endType.baseType()));
            }

            var startHasTimeInformation = hasDateInformationOrOpt().matches(startType);

            if (!startHasTimeInformation) {
                return ReturnResult.failure("%s does not have date information".formatted(startType.baseType()));
            }

            return ReturnResult.success(INTEGER(anyOptional(argTypes)));

        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            IntegerComputerResultSupplier value = ctx -> {
                var start = ((TemporalComputer)args.get(START_ARG)).compute(ctx);
                var end = ((TemporalComputer)args.get(END_ARG)).compute(ctx);

                return m_unit.between( //
                    LocalDate.from(start), //
                    LocalDate.from(end) //
                );
            };

            return IntegerComputer.of(value, anyMissing(args));
        }
    }

    public static final ExpressionFunction YEARS_BETWEEN = new UnitsBetweenExpressionFunction(ChronoUnit.YEARS, //
        """
                Calculates the number of calendar years between two date-time values with date information.
                23:59:59 on the last day of one year and 00:00:01 on the next day are considered to be one
                year apart. Time and timezone information is ignored.

                If the first date is after the second date, the result will be negative.

                If either of the provided dates is missing, the function returns `MISSING`.
                """, //
        """
                * `years_between(parse_date("1970-01-01"), parse_date("1971-01-01"))` returns `1`
                * `years_between(parse_date("1971-01-01"), parse_date("1970-01-01"))` returns `-1`
                """ //
    );

    public static final ExpressionFunction MONTHS_BETWEEN = new UnitsBetweenExpressionFunction(ChronoUnit.MONTHS, //
        """
                Calculates the number of calendar months between two date-time values with date information.
                23:59:59 on the last day of one month and 00:00:01 on the next day are considered to be one
                month apart. Time and timezone information is ignored.

                If the first date is after the second date, the result will be negative.

                If either of the provided dates is missing, the function returns `MISSING`.
                """, //
        """
                * `months_between(parse_date("1970-01-01"), parse_date("1970-02-01"))` returns `1`
                * `months_between(parse_date("1970-02-01"), parse_date("1970-01-01"))` returns `-1`
                """ //
    );

    public static final ExpressionFunction DAYS_BETWEEN = new UnitsBetweenExpressionFunction(ChronoUnit.DAYS, //
        """
                Calculates the number of calendar days between two date-time values with date information. The
                time part of the date-time values will be ignored, meaning that 23:59:59 on one day and
                00:00:01 on the next day are considered to be one day apart. Timezone information is similarly
                ignored.

                If the first date is after the second date, the result will be negative.

                If either of the provided dates is missing, the function returns `MISSING`.
                """, //
        """
                * `days_between(parse_date("1970-01-01"), parse_date("1970-01-02"))` returns `1`
                * `days_between(parse_date("1970-01-02"), parse_date("1970-01-01"))` returns `-1`
                """ //
    );
}
