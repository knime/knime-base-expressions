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
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_FLOAT_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_INTEGER_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_TIME_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_ZONED_DATE_TIME_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasDateAndTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.hasDateInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.hasTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isIntegerOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.isTimeDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isZonedDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
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
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
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
import org.knime.core.expressions.Computer.TemporalComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.functions.ExpressionFunction;
import org.knime.time.util.TimeBasedGranularityUnit;

/**
 * Implementation of built-in functions for creating and extracting temporal data, such as dates, times, and intervals.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class TemporalCreateExtractFunctions {

    private TemporalCreateExtractFunctions() {
    }

    /** The Temporal - Creation & Extraction category */
    public static final OperatorCategory CATEGORY_CREATE_EXTRACT =
        new OperatorCategory(TemporalFunctionUtils.TEMPORAL_META_CATEGORY_NAME, "Creation & Extraction", """
                Functions for creating and extracting temporal data, such as dates, times, and intervals.
                """);

    /** Creates a `LOCAL_DATE` from year, month, day */
    public static final ExpressionFunction MAKE_DATE = functionBuilder() //
        .name("make_date") //
        .description("""
                Creates a `LOCAL_DATE` from the provided year, month, and day values.

                If the provided year, month, or day is missing, the function returns `MISSING`. If the provided \
                year, month, or day is invalid (e.g. the month is greater than 12), the function returns `MISSING` \
                and a warning is emitted.

                If any of the provided values are so big that numeric overflow would occur (i.e. they are larger than \
                2^31 - 1 or smaller than -2^31), the function throws an error.
                """) //
        .examples("""
                * `make_date(1970, 1, 1)` returns `1970-01-01`
                * `make_date(1970, 1, 32)` returns `MISSING`
                """) //
        .keywords("make", "date", "local_date", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("year", "The year to use for the date.", isIntegerOrOpt()), //
            arg("month", "The month to use for the date (1-12).", isIntegerOrOpt()), //
            arg("day", "The day to use for the date (1-31).", isIntegerOrOpt()) //
        ) //
        .returnType("A `LOCAL_DATE` representing the provided year, month, and day values", RETURN_LOCAL_DATE_MISSING,
            args -> OPT_LOCAL_DATE) //
        .impl(TemporalCreateExtractFunctions::makeDateImpl) //
        .build();

    private static Computer makeDateImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<LocalDate>> valueSupplier = ctx -> {
            try {
                var year = Math.toIntExact(((IntegerComputer)args.get("year")).compute(ctx));
                var month = Math.toIntExact(((IntegerComputer)args.get("month")).compute(ctx));
                var day = Math.toIntExact(((IntegerComputer)args.get("day")).compute(ctx));

                try {
                    return Optional.of(LocalDate.of(year, month, day));
                } catch (DateTimeException e) {
                    // A value was out of the valid range.
                    // In this case the exception message is quite clear even for the user.
                    ctx.addWarning(e.getMessage().replace("DayOfMonth", "day").replace("MonthOfYear", "month") + ".");
                    return Optional.empty();
                }
            } catch (ArithmeticException e) {
                throw new ExpressionEvaluationException("Numerical overflow encountered while creating date value.");
            }
        };

        return LocalDateComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    /** Creates a `LOCAL_TIME` from hour, minute, second, nanosecond */
    public static final ExpressionFunction MAKE_TIME = functionBuilder() //
        .name("make_time") //
        .description("""
                Creates a `LOCAL_TIME` from the provided hour, minute, second, and nanosecond values.

                If the hour or minute value are missing, the function returns `MISSING`. If any provided \
                values are invalid (e.g. the hour is greater than 23), the function returns \
                `MISSING` and a warning is emitted. Note that the seconds and nanoseconds are optional. If \
                they are `MISSING` or not provided, they will take their default values of 0.

                If any of the provided values are so big that numeric overflow would occur (i.e. they are larger than \
                2^31 - 1 or smaller than -2^31), the function throws an error.
                """) //
        .examples("""
                * `make_time(1, 2, 3, 4)` returns `01:02:03.000000004`
                * `make_time(24, 0, 0, 0)` returns `MISSING` and emits a warning
                * `make_time(1, 2)` returns `01:02:00`
                """) //
        .keywords("make", "time", "local_time", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("hour", "The hour to use for the time (0-23).", isIntegerOrOpt()), //
            arg("minute", "The minute to use for the time (0-59).", isIntegerOrOpt()), //
            optarg("second", "The second to use for the time (0-59).", isIntegerOrOpt()), //
            optarg("nanosecond", "The nanosecond to use for the time (0-999999999).", isIntegerOrOpt()) //
        ) //
        .returnType("A `LOCAL_TIME` representing the provided hour, minute, second, and nanosecond values",
            RETURN_LOCAL_TIME_MISSING, args -> OPT_LOCAL_TIME) //
        .impl(TemporalCreateExtractFunctions::makeTimeImpl) //
        .build();

    private static Computer makeTimeImpl(final Arguments<Computer> args) {
        var hourComputer = (IntegerComputer)args.get("hour");
        var minuteComputer = (IntegerComputer)args.get("minute");
        var secondComputer = (IntegerComputer)args.get("second", IntegerComputer.ofConstant(0));
        var nanoComputer = (IntegerComputer)args.get("nanosecond", IntegerComputer.ofConstant(0));

        ComputerResultSupplier<Optional<LocalTime>> valueSupplier = ctx -> {
            try {
                var hour = Math.toIntExact(hourComputer.compute(ctx));
                var minute = Math.toIntExact(minuteComputer.compute(ctx));
                var second = Math.toIntExact(secondComputer.isMissing(ctx) ? 0 : secondComputer.compute(ctx));
                var nanosecond = Math.toIntExact(nanoComputer.isMissing(ctx) ? 0 : nanoComputer.compute(ctx));

                try {
                    return Optional.of(LocalTime.of(hour, minute, second, nanosecond));
                } catch (DateTimeException e) {
                    // A value was out of the valid range.
                    // In this case the exception message is quite clear even for the user.
                    ctx.addWarning(
                        e.getMessage().replace("NanoOfSecond", "nanosecond").replace("SecondOfMinute", "second")
                            .replace("MinuteOfHour", "minute").replace("HourOfDay", "hour") + ".");
                    return Optional.empty();
                }
            } catch (ArithmeticException e) {
                throw new ExpressionEvaluationException("Numerical overflow encountered while creating time value.");
            }
        };

        return LocalTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> hourComputer.isMissing(ctx) || minuteComputer.isMissing(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    /** Creates a `LOCAL_DATE_TIME` from date, time */
    public static final ExpressionFunction MAKE_DATETIME = functionBuilder() //
        .name("make_datetime") //
        .description("""
                Creates a `LOCAL_DATE_TIME` from the provided date and time values.

                If the provided date or time is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `make_datetime(make_date(1970, 1, 1), make_time(0, 0))` returns `1970-01-01T00:00:00`
                """) //
        .keywords("make", "datetime", "local_date_time", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("date", "The date to use for the date time.", isLocalDateOrOpt()), //
            arg("time", "The time to use for the date time.", isLocalTimeOrOpt()) //
        ) //
        .returnType("A `LOCAL_DATE_TIME` representing the provided date and time values",
            RETURN_LOCAL_DATE_TIME_MISSING, args -> LOCAL_DATE_TIME(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions::makeDateTimeImpl) //
        .build();

    private static Computer makeDateTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<LocalDateTime> valueSupplier = ctx -> {
            var date = ((LocalDateComputer)args.get("date")).compute(ctx);
            var time = ((LocalTimeComputer)args.get("time")).compute(ctx);

            return LocalDateTime.of(date, time);
        };

        return LocalDateTimeComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    /** Creates a `ZONED_DATE_TIME` from date, time, zone */
    public static final ExpressionFunction MAKE_ZONED = functionBuilder() //
        .name("make_zoned") //
        .description("""
                Creates a `ZONED_DATE_TIME` from the provided datetime, and zone values.

                If the provided datetime or zone is missing, the function returns `MISSING`. If the provided \
                zone is invalid, the function returns `MISSING` and a warning is emitted.

                The provided zone is in the IANA time zone format (e.g. `Europe/Berlin` or `UTC`) and is \
                case insensitive. See [here](%s) for a list of valid time zones. Alternatively, a zone offset \
                can be provided (e.g. `+02:00`, `-5`, `UTC+07:15`, `GMT-3`, etc.).
                """.formatted(TemporalFunctionUtils.URL_TIMEZONE_LIST)) //
        .examples("""
                In these examples, $datetime is a `LOCAL_DATE_TIME` equals to `1970-01-01T00:00:00`.

                * `make_zoned($datetime, 'UTC')` returns `1970-01-01T00:00:00Z`
                * `make_zoned($datetime, 'Europe/Berlin')` returns `1970-01-01T00:00:00+01:00[Europe/Berlin]`
                * `make_zoned($datetime, 'Invalid/Zone')` returns `MISSING`
                """) //
        .keywords("make", "datetime", "zoned_date_time", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("datetime", "The date to use for the zoned date time.", isLocalDateTimeOrOpt()), //
            arg("zone", "The zone to use for the zoned date time.", isStringOrOpt()) //
        ) //
        .returnType("A `ZONED_DATE_TIME` representing the provided date, time, and zone values",
            RETURN_ZONED_DATE_TIME_MISSING, args -> OPT_ZONED_DATE_TIME) //
        .impl(TemporalCreateExtractFunctions::makeZonedDateTimeImpl) //
        .build();

    private static Computer makeZonedDateTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<ZonedDateTime>> valueSupplier = ctx -> {
            var datetime = ((LocalDateTimeComputer)args.get("datetime")).compute(ctx);
            var zoneIdString = ((StringComputer)args.get("zone")).compute(ctx);

            var zoneId = TemporalFunctionUtils.parseZoneIdCaseInsensitive(zoneIdString);
            if (zoneId.isEmpty()) {
                ctx.addWarning("Invalid zone id '%s'.".formatted(zoneIdString));
                return Optional.empty();
            }

            return Optional.of(ZonedDateTime.of(datetime, zoneId.get()));
        };

        return ZonedDateTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    /** Creates a `TIME_DURATION` from hours, minutes, seconds, nanoseconds */
    public static final ExpressionFunction MAKE_TIME_DURATION = functionBuilder() //
        .name("make_time_duration") //
        .description("""
                Creates a `TIME_DURATION` from the provided hours, minutes, seconds, and nanoseconds.

                The seconds and nanoseconds are optional, and default to 0 if `MISSING` or not provided. However, \
                if any of the other provided values are missing, the function returns `MISSING`. If the provided \
                values would result in a duration that cannot be represented (i.e. if it would total to more than \
                `MAX_INTEGER` or less than `MIN_INTEGER` seconds), the function emits an error.
                """) //
        .examples("""
                * `make_time_duration(1, 2, 3, 4)` returns `PT1H2M3.000000004S`
                * `make_time_duration(1, 2)` returns `PT1H2M`
                * `make_time_duration(1, 2, 3, MAX_INTEGER)` emits an error
                """) //
        .keywords("make", "duration", "interval", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("hours", "The hours to use for the duration.", isIntegerOrOpt()), //
            arg("minutes", "The minutes to use for the duration.", isIntegerOrOpt()), //
            optarg("seconds", "The seconds to use for the duration.", isIntegerOrOpt()), //
            optarg("nanoseconds", "The nanoseconds to use for the duration.", isIntegerOrOpt()) //
        ) //
        .returnType("A `TIME_DURATION` representing the provided hours, minutes, seconds, and nanoseconds",
            RETURN_TIME_DURATION_MISSING, args -> TIME_DURATION(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions::makeDurationImpl) //
        .build();

    private static Computer makeDurationImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Duration> valueSupplier = ctx -> {
            var hours = ((IntegerComputer)args.get("hours")).compute(ctx);
            var minutes = ((IntegerComputer)args.get("minutes")).compute(ctx);
            var seconds = ((IntegerComputer)args.get("seconds", IntegerComputer.ofConstant(0))).compute(ctx);
            var nanoseconds = ((IntegerComputer)args.get("nanoseconds", IntegerComputer.ofConstant(0))).compute(ctx);

            try {
                return Duration.ofHours(hours) //
                    .plusMinutes(minutes) //
                    .plusSeconds(seconds) //
                    .plusNanos(nanoseconds);
            } catch (ArithmeticException e) {
                throw new ExpressionEvaluationException("Duration values are too large.", e);
            }
        };

        return TimeDurationComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    /** Creates a `DATE_DURATION` from years, months, days */
    public static final ExpressionFunction MAKE_DATE_DURATION = functionBuilder() //
        .name("make_date_duration") //
        .description("""
                Creates a `DATE_DURATION` from the provided year, month, and day values.

                If any of the provided values are missing, the function returns `MISSING`. If any of the provided \
                values is greater than `2^31 - 1` or less than `-2^31`, the function emits an error.
                """) //
        .examples("""
                * `make_date_duration(1, 2, 3)` returns `P1Y2M3D`
                * `make_date_duration(1, 2, MAX_INTEGER)` emits an error.
                """) //
        .keywords("make", "period", "interval", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("years", "The years to use for the `DATE_DURATION`.", isIntegerOrOpt()), //
            arg("months", "The months to use for the `DATE_DURATION`.", isIntegerOrOpt()), //
            arg("days", "The days to use for the `DATE_DURATION`.", isIntegerOrOpt()) //
        ) //
        .returnType("A `DATE_DURATION` representing the provided year, month, and day values",
            RETURN_DATE_DURATION_MISSING, args -> DATE_DURATION(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions::makePeriodImpl) //
        .build();

    private static Computer makePeriodImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Period> valueSupplier = ctx -> {
            try {
                var years = Math.toIntExact(((IntegerComputer)args.get("years")).compute(ctx));
                var months = Math.toIntExact(((IntegerComputer)args.get("months")).compute(ctx));
                var days = Math.toIntExact(((IntegerComputer)args.get("days")).compute(ctx));

                return Period.of(years, months, days);
            } catch (ArithmeticException ex) {
                throw new ExpressionEvaluationException("Duration values are too large.", ex);
            }
        };

        return DateDurationComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    private static Function<Arguments<Computer>, Computer> extractTemporalFieldImpl(final TemporalField field) {
        return args -> {
            IntegerComputerResultSupplier value = ctx -> {
                var temporal = ((TemporalComputer)args.get("temporal")).compute(ctx);
                return temporal.getLong(field);
            };

            return IntegerComputer.of( //
                value, //
                anyMissing(args) //
            );
        };
    }

    /** Extracts the year from a date-time value with date information */
    public static final ExpressionFunction EXTRACT_YEAR = functionBuilder() //
        .name("extract_year") //
        .description("""
                Extracts the year from a date-time value with date information.

                Note that this year represents the proleptic year in the Gregorian calendar. Years before 0 A.D. \
                are represented with negative values.
                """) //
        .examples("""
                * `extract_year(parse_date("1970-01-01"))` returns `1970`
                """) //
        .keywords("extract", "year") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the year from.", hasDateInformationOrOpt()) //
        ) //
        .returnType("An integer representing the year of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.YEAR)) //
        .build();

    /** Extracts the month of year from a date-time value with date information */
    public static final ExpressionFunction EXTRACT_MONTH = functionBuilder() //
        .name("extract_month") //
        .description("""
                Extracts the month of year from a date-time value with date information.

                The month is represented as an integer between 1 and 12.
                """) //
        .examples("""
                * `extract_month(parse_date("1970-01-01"))` returns `1`
                """) //
        .keywords("extract", "month") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the month from.", hasDateInformationOrOpt()) //
        ) //
        .returnType("An integer representing the month of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.MONTH_OF_YEAR)) //
        .build();

    /** Extracts the day of month from a date-time value with date information */
    public static final ExpressionFunction EXTRACT_DAY_OF_MONTH = functionBuilder() //
        .name("extract_day_of_month") //
        .description("""
                Extracts the date of the month from a date-time value with date information.

                The day is represented as an integer between 1 and 31.
                """) //
        .examples("""
                * `extract_day_of_month(parse_date("1970-01-01"))` returns `1`
                """) //
        .keywords("extract", "day") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the day from.", hasDateInformationOrOpt()) //
        ) //
        .returnType("An integer representing the day of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.DAY_OF_MONTH)) //
        .build();

    /** Extracts the hour of day from a temporal value with time information */
    public static final ExpressionFunction EXTRACT_HOUR = functionBuilder() //
        .name("extract_hour") //
        .description("""
                Extracts the hour of day from a temporal value with time information.

                The hour is represented as an integer between 0 and 23.
                """) //
        .examples("""
                * `extract_hour(parse_time("05:06:07"))` returns `5`
                """) //
        .keywords("extract", "hour") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the hour from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the hour of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.HOUR_OF_DAY)) //
        .build();

    /** Extracts the minute of hour from a temporal value with time information */
    public static final ExpressionFunction EXTRACT_MINUTE = functionBuilder() //
        .name("extract_minute") //
        .description("""
                Extracts the minute of hour from a temporal value with time information.

                The minute is represented as an integer between 0 and 59.
                """) //
        .examples("""
                * `extract_minute(parse_time("05:06:07"))` returns `6`
                """) //
        .keywords("extract", "minute") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the minute from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the minute of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.MINUTE_OF_HOUR)) //
        .build();

    /** Extracts the second of minute from a temporal value with time information */
    public static final ExpressionFunction EXTRACT_SECOND = functionBuilder() //
        .name("extract_second") //
        .description("""
                Extracts the second of minute from a temporal value with time information.

                The second is represented as an integer between 0 and 59.
                """) //
        .examples("""
                * `extract_second(parse_time("05:06:07"))` returns `7`
                """) //
        .keywords("extract", "second") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the second from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the second of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.SECOND_OF_MINUTE)) //
        .build();

    /** Extracts the nanosecond of second from a temporal value */
    public static final ExpressionFunction EXTRACT_NANOSECOND = functionBuilder() //
        .name("extract_nanosecond") //
        .description("""
                Extracts the nanosecond of second from a temporal value.

                The nanosecond is represented as an integer between 0 and 999_999_999.
                """) //
        .examples("""
                * `extract_nanosecond(parse_time("00:00:00.123456789"))` returns `123456789`
                """) //
        .keywords("extract", "nanosecond") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the nanosecond from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the nanosecond of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(extractTemporalFieldImpl(ChronoField.NANO_OF_SECOND)) //
        .build();

    /** Extracts the date from a date-time value */
    public static final ExpressionFunction EXTRACT_DATE = functionBuilder() //
        .name("extract_date") //
        .description("""
                Extracts the date part of a `LOCAL_DATE_TIME` or `ZONED_DATE_TIME` value.

                If the provided temporal value is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `extract_date(parse_datetime("1970-01-01T00:00:00"))` returns `1970-01-01`
                """) //
        .keywords("extract", "date") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the date from.", hasDateAndTimeInformationOrOpt()) //
        ) //
        .returnType("A `LOCAL_DATE` representing the date part of the provided temporal value",
            RETURN_LOCAL_DATE_MISSING, args -> LOCAL_DATE(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions::extractDateImpl) //
        .build();

    private static Computer extractDateImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<LocalDate> valueSupplier = ctx -> {
            var temporal = ((TemporalComputer)args.get("temporal")).compute(ctx);
            return LocalDate.from(temporal);
        };

        return LocalDateComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    /** Extracts the time from a date-time value */
    public static final ExpressionFunction EXTRACT_TIME = functionBuilder() //
        .name("extract_time") //
        .description("""
                Extracts the time part of a `LOCAL_DATE_TIME` or `ZONED_DATE_TIME` value.

                If the provided temporal value is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `extract_time(parse_datetime("1970-01-01T00:00:00"))` returns `00:00:00`
                """) //
        .keywords("extract", "time") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the time from.", hasDateAndTimeInformationOrOpt()) //
        ) //
        .returnType("A `LOCAL_TIME` representing the time part of the provided temporal value",
            RETURN_LOCAL_TIME_MISSING, args -> LOCAL_TIME(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions::extractTimeImpl) //
        .build();

    private static Computer extractTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<LocalTime> valueSupplier = ctx -> {
            var temporal = ((TemporalComputer)args.get("temporal")).compute(ctx);
            return LocalTime.from(temporal);
        };

        return LocalTimeComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    /** Extracts the date-time from a zoned date-time value */
    public static final ExpressionFunction EXTRACT_DATETIME = functionBuilder() //
        .name("extract_datetime") //
        .description("""
                Extracts the date and time part of a `ZONED_DATE_TIME` value. This is equivalent to \
                removing the time zone information from the provided value. The wall time will not \
                be adjusted.

                If the provided value is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `extract_datetime(parse_zoned("1970-01-01T00:00:00Z"))` returns `1970-01-01T00:00:00`
                * `extract_datetime(parse_zoned("1970-01-01T00:00:00+01:00[Europe/Berlin]"))` \
                  returns `1970-01-01T00:00:00`
                """) //
        .keywords("extract", "datetime", "remove", "zone", "remove_zone") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The `ZONED_DATE_TIME` value to extract the date and time from.", isZonedDateTimeOrOpt()) //
        ) //
        .returnType("A `LOCAL_DATE_TIME` representing the date and time part of the provided temporal value",
            RETURN_LOCAL_DATE_TIME_MISSING, args -> LOCAL_DATE_TIME(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions::extractDateTimeImpl) //
        .build();

    private static Computer extractDateTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<LocalDateTime> valueSupplier = ctx -> {
            var temporal = ((ZonedDateTimeComputer)args.get("temporal")).compute(ctx);
            return LocalDateTime.from(temporal);
        };

        return LocalDateTimeComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    private static Function<Arguments<Computer>, Computer>
        convertDurationImpl(final TimeBasedGranularityUnit granularityUnit) {
        return args -> {
            FloatComputerResultSupplier value = ctx -> {
                var duration = ((TimeDurationComputer)args.get("duration")).compute(ctx);
                return granularityUnit.getConversionExact(duration);
            };

            return FloatComputer.of( //
                value, //
                anyMissing(args) //
            );
        };
    }

    /** Converts a `TIME_DURATION` to hours */
    public static final ExpressionFunction TO_HOURS = functionBuilder() //
        .name("to_hours") //
        .description("""
                Converts a `TIME_DURATION` to a decimal number representing the duration in hours.

                If the provided duration is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `to_hours(parse_duration("PT1H"))` returns `1.0`
                * `to_hours(parse_duration("PT1H30M"))` returns `1.5`
                """) //
        .keywords("convert", "hours") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("duration", "The duration to convert to hours.", isTimeDurationOrOpt()) //
        ) //
        .returnType("A `FLOAT` representing the duration in hours", RETURN_FLOAT_MISSING,
            args -> FLOAT(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions.convertDurationImpl(TimeBasedGranularityUnit.HOURS)) //
        .build();

    /** Converts a `TIME_DURATION` to minutes */
    public static final ExpressionFunction TO_MINUTES = functionBuilder() //
        .name("to_minutes") //
        .description("""
                Converts a `TIME_DURATION` to a decimal number representing the duration in minutes.

                If the provided duration is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `to_minutes(parse_duration("PT1H"))` returns `60.0`
                * `to_minutes(parse_duration("PT1H30M"))` returns `90.0`
                """) //
        .keywords("convert", "minutes") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("duration", "The duration to convert to minutes.", isTimeDurationOrOpt()) //
        ) //
        .returnType("A `FLOAT` representing the duration in minutes", RETURN_FLOAT_MISSING,
            args -> FLOAT(anyOptional(args))) //
        .impl(convertDurationImpl(TimeBasedGranularityUnit.MINUTES)) //
        .build();

    /** Converts a `TIME_DURATION` to seconds */
    public static final ExpressionFunction TO_SECONDS = functionBuilder() //
        .name("to_seconds") //
        .description("""
                Converts a `TIME_DURATION` to a decimal number representing the duration in seconds.

                If the provided duration is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `to_seconds(parse_duration("PT1H"))` returns `3600.0`
                * `to_seconds(parse_duration("PT1H30M"))` returns `5400.0`
                """) //
        .keywords("convert", "seconds") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("duration", "The duration to convert to seconds.", isTimeDurationOrOpt()) //
        ) //
        .returnType("A `FLOAT` representing the duration in seconds", RETURN_FLOAT_MISSING,
            args -> FLOAT(anyOptional(args))) //
        .impl(TemporalCreateExtractFunctions.convertDurationImpl(TimeBasedGranularityUnit.SECONDS)) //
        .build();
}
