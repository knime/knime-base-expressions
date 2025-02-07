package org.knime.core.expressions.functions;

import static org.knime.core.expressions.Computer.createConstantComputer;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_HAS_DATE_PART_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_HAS_TIME_PART_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_PERIOD_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_ZONED_DATE_TIME_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasBaseType;
import static org.knime.core.expressions.SignatureUtils.hasDatePartOrIsOpt;
import static org.knime.core.expressions.SignatureUtils.hasTimePartOrIsOpt;
import static org.knime.core.expressions.SignatureUtils.isIntegerOrOpt;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.DURATION;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.PERIOD;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.DurationComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.PeriodComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ValueType;

/**
 * Implementation of built-in functions that manipulate date and times.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class DateTimeFunctions {

    private DateTimeFunctions() {
    }

    private static final String DATE_TIME_META_CATEGORY_NAME = "Date&Time";

    /** The "DateTime – General" category */
    public static final OperatorCategory CATEGORY_GENERAL =
        new OperatorCategory(DATE_TIME_META_CATEGORY_NAME, "General", """
                The "Date&Time – General" category in KNIME Expression language includes functions for manipulating and
                converting Date&Time data.
                """);

    /* ============================= *
     * PARSE/CREATE/FORMAT FUNCTIONS *
     * ============================= */

    public static final ExpressionFunction PARSE_DATE = functionBuilder() //
        .name("parse_date") //
        .description("""
                Parse String values into Date values.
                """) //
        .examples("""
                * `date("2021-01-01")` returns LocalDate(2021-01-01)
                """) //
        .keywords("match", "equals") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("string", "String to parse to a date", isStringOrOpt()), //
            optarg("format", "Format of the date string", isStringOrOpt()) //
        ) //
        .returnType("A local date", RETURN_LOCAL_DATE_MISSING, args -> LOCAL_DATE(anyOptional(args)))//
        .impl(DateTimeFunctions::parseDate) //
        .build();

    public static final ExpressionFunction CREATE_DATE = functionBuilder() //
        .name("create_date") //
        .description("""
                Create a Date value from the given year, month and day.
                """) //
        .examples("""
                * `create_date(2021, 1, 1)` returns LocalDate(2021-01-01)
                """) //
        .keywords("create", "date") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("year", "Year of the date", isIntegerOrOpt()), //
            arg("month", "Month of the date", isIntegerOrOpt()), //
            arg("day", "Day of the date", isIntegerOrOpt()) //
        ) //
        .returnType("A local date", RETURN_LOCAL_DATE_MISSING, args -> LOCAL_DATE(anyOptional(args)))//
        .impl(DateTimeFunctions::createDate) //
        .build();

    private static Computer createDate(final Arguments<Computer> args) {
        var year = args.get("year");
        var month = args.get("month");
        var day = args.get("day");

        return LocalDateComputer.of( //
            ctx -> LocalDate.of( //
                (int)toInteger(year).compute(ctx), //
                (int)toInteger(month).compute(ctx), //
                (int)toInteger(day).compute(ctx)), //
            ctx -> anyMissing(args).applyAsBoolean(ctx));
    }

    private static Computer parseDate(final Arguments<Computer> args) {
        var string = toString(args.get("string"));

        return LocalDateComputer.of( //
            ctx -> {
                var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_DATE).apply(ctx);
                return formatter.parse(string.compute(ctx), LocalDate::from);
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_DATE).apply(ctx);
                    formatter.parse(string.compute(ctx), LocalDate::from);
                    return false;
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction PARSE_TIME = functionBuilder() //
        .name("parse_time") //
        .description("""
                Parse String values into Time values.
                """) //
        .examples("""
                * `time("12:12")` returns LocalTime(12, 12)
                """) //
        .keywords("match", "equals") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("string", "String to parse to a time", isStringOrOpt()), //
            optarg("format", "Format of the time string", isStringOrOpt()) //
        ) //
        .returnType("A local time", RETURN_LOCAL_TIME_MISSING, args -> LOCAL_TIME(anyOptional(args)))//
        .impl(DateTimeFunctions::parseTime) //
        .build();

    private static Computer parseTime(final Arguments<Computer> args) {
        var string = toString(args.get("string"));

        return LocalTimeComputer.of( //
            ctx -> {
                var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_TIME).apply(ctx);
                return formatter.parse(string.compute(ctx), LocalTime::from);
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_TIME).apply(ctx);
                    formatter.parse(string.compute(ctx), LocalTime::from);
                    return false;
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction CREATE_TIME = functionBuilder() //
        .name("create_time") //
        .description("""
                Creates a Time value from the given hour, minute, second and nanosecond.
                """) //
        .examples("""
                * `create_time(12, 12, 12, 12)` returns LocalTime(12, 12, 12, 12)
                """) //
        .keywords("create", "time") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("hour", "Hour of the time", isIntegerOrOpt()), //
            arg("minute", "Minute of the time", isIntegerOrOpt()), //
            optarg("second", "Second of the time (0 if not given)", isIntegerOrOpt()), //
            optarg("nano", "Nanosecond of the time (0 if not given)", isIntegerOrOpt()) //
        ) //
        .returnType("A local time", RETURN_LOCAL_TIME_MISSING, args -> LOCAL_TIME(anyOptional(args)))//
        .impl(DateTimeFunctions::createTime) //
        .build();

    private static Computer createTime(final Arguments<Computer> args) {
        var hour = args.get("hour");
        var minute = args.get("minute");
        var second = args.get("second", ZERO_INTEGER_COMPUTER);
        var nano = args.get("nano", ZERO_INTEGER_COMPUTER);

        return LocalTimeComputer.of( //
            ctx -> LocalTime.of( //
                (int)toInteger(hour).compute(ctx), //
                (int)toInteger(minute).compute(ctx), //
                (int)toInteger(second).compute(ctx), //
                (int)toInteger(nano).compute(ctx)), //
            ctx -> anyMissing(args).applyAsBoolean(ctx));
    }

    public static final ExpressionFunction PARSE_DATE_TIME = functionBuilder() //
        .name("parse_date_time") //
        .description("""
                Parse String values into DateTime values.
                """) //
        .examples("""
                * `date_time("2021-01-01T12:12")` returns LocalDateTime("2021-01-01T12:12")
                """) //
        .keywords("match", "equals") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("string", "String to parse to a date time", isStringOrOpt()), //
            optarg("format", "Format of the date time string", isStringOrOpt()) //
        ) //
        .returnType("A local date time", "LocalDateTime", args -> LOCAL_DATE_TIME(anyOptional(args)))//
        .impl(DateTimeFunctions::parseDateTime) //
        .build();

    private static Computer parseDateTime(final Arguments<Computer> args) {
        var string = toString(args.get("string"));

        return LocalDateTimeComputer.of( //
            ctx -> {
                var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_DATE_TIME).apply(ctx);
                return formatter.parse(string.compute(ctx), LocalDateTime::from);
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_DATE_TIME).apply(ctx);
                    formatter.parse(string.compute(ctx), LocalDateTime::from);
                    return false;
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction CREATE_DATE_TIME = functionBuilder() //
        .name("create_date_time") //
        .description("""
                Create a DateTime value from the given Date and Time.
                """) //
        .examples("""
                * `create_date_time(date("2021-01-01"), time("12:12"))` returns LocalDateTime("2021-01-01T12:12")
                """) //
        .keywords("create", "datetime") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("date", "Date part of the DateTime", hasBaseType(LOCAL_DATE)), //
            arg("time", "Time part of the DateTime", hasBaseType(LOCAL_TIME)) //
        ) //
        .returnType("A local date time", "LocalDateTime", args -> LOCAL_DATE_TIME(anyOptional(args))) //
        .impl(DateTimeFunctions::createDateTime) //
        .build();

    private static Computer createDateTime(final Arguments<Computer> args) {
        var date = args.get("date");
        var time = args.get("time");

        return LocalDateTimeComputer.of( //
            ctx -> LocalDateTime.of( //
                (LocalDate)Computer.computeTemporal(date, ctx), //
                (LocalTime)Computer.computeTemporal(time, ctx)), //
            ctx -> anyMissing(args).applyAsBoolean(ctx));
    }

    public static final ExpressionFunction PARSE_ZONED_DATE_TIME = functionBuilder() //
        .name("parse_zoned_date_time") //
        .description("""
                Parse String values into ZonedDateTime values.
                """) //
        .examples(
            """
                    * `zoned_date_time("2021-01-01T12:12+01:00[Europe/Berlin]")` returns ZonedDateTime("2021-01-01T12:12+01:00[Europe/Berlin]")
                    """) //
        .keywords("match", "equals") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("string", "String to parse to a zoned date time", isStringOrOpt()), //
            optarg("format", "Format of the zoned date time string", isStringOrOpt()) //
        ) //
        .returnType("A zoned date time", "ZonedDateTime", args -> LOCAL_DATE_TIME(anyOptional(args)))//
        .impl(DateTimeFunctions::parseZonedDateTime) //
        .build();

    private static Computer parseZonedDateTime(final Arguments<Computer> args) {
        var string = toString(args.get("string"));

        return ZonedDateTimeComputer.of( //
            ctx -> {
                var formatter = createFormatter(args, DateTimeFormatter.ISO_ZONED_DATE_TIME).apply(ctx);
                return formatter.parse(string.compute(ctx), ZonedDateTime::from);
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var formatter = createFormatter(args, DateTimeFormatter.ISO_ZONED_DATE_TIME).apply(ctx);
                    formatter.parse(string.compute(ctx), ZonedDateTime::from);
                    return false;
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction CREATE_ZONED_DATE_TIME = functionBuilder() //
        .name("create_zoned_date_time") //
        .description("""
                Create a ZonedDateTime value from the given LocalDateTime and ZoneId.
                """) //
        .examples("""
                * `TODO` \
                  returns ZonedDateTime("2021-01-01T12:12+01:00[Europe/Berlin]")
                """) //
        .keywords("create", "datetime") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("local_date_time", "LocalDateTime part of the ZonedDateTime", hasBaseType(LOCAL_DATE_TIME)), //
            arg("zone_id", "ZoneId part of the ZonedDateTime", isStringOrOpt()) //
        ) //
        .returnType("A zoned date time", "ZonedDateTime", args -> LOCAL_DATE_TIME(anyOptional(args))) //
        .impl(DateTimeFunctions::createZonedDateTime) //
        .build();

    private static Computer createZonedDateTime(final Arguments<Computer> args) {
        var localDateTime = args.get("local_date_time");
        var zoneId = toString(args.get("zone_id"));

        return ZonedDateTimeComputer.of( //
            ctx -> ZonedDateTime.of( //
                (LocalDateTime)Computer.computeTemporal(localDateTime, ctx), //
                ZoneId.of(zoneId.compute(ctx))), //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    ZoneId.of(zoneId.compute(ctx));
                    return false;
                } catch (DateTimeException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction FORMAT_TEMPORAL = functionBuilder() //
        .name("format_temporal") //
        .description("""
                The node formats Temporal values into String values.
                """) //
        .examples("""
                * `format_temporal(time("12:12"))` returns "12:12"
                """) //
        .keywords("") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("temporal", "Temporal value to format", hasTimePartOrIsOpt()), //
            optarg("format", "Format of the temporal string", isStringOrOpt()) //
        ) //
        .returnType("A string", "String", args -> STRING(anyOptional(args)))//
        .impl(DateTimeFunctions::formatTemporal) //
        .build();

    private static Computer formatTemporal(final Arguments<Computer> args) {
        var temporal = args.get("temporal");

        DateTimeFormatter fallbackFormatter = fromTemporalComputer(temporal);

        return StringComputer.of( //
            ctx -> {
                var formatter = createFormatter(args, fallbackFormatter).apply(ctx);
                return formatter.format(Computer.computeTemporal(temporal, ctx));
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var formatter = createFormatter(args, DateTimeFormatter.ISO_LOCAL_TIME).apply(ctx);
                    formatter.format(Computer.computeTemporal(temporal, ctx));
                    return false;
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction CREATE_PERIOD = functionBuilder() //
        .name("create_period") //
        .description("""
                Create a Period value from the given years, months and days.
                """) //
        .examples("""
                * `create_period(1, 2, 3)` returns Period.of(1, 2, 3)
                """) //
        .keywords("create", "period") //
        .category(CATEGORY_GENERAL) //
        .args( //
            optarg("years", "Years of the period", isIntegerOrOpt()), //
            optarg("months", "Months of the period", isIntegerOrOpt()), //
            optarg("days", "Days of the period", isIntegerOrOpt()) //
        ) //
        .returnType("A period", RETURN_PERIOD_MISSING, args -> PERIOD(anyOptional(args)))//
        .impl(DateTimeFunctions::createPeriod) //
        .build();

    private static Computer createPeriod(final Arguments<Computer> args) {
        var years = toInteger(args.get("years", ZERO_INTEGER_COMPUTER));
        var months = toInteger(args.get("months", ZERO_INTEGER_COMPUTER));
        var days = toInteger(args.get("days", ZERO_INTEGER_COMPUTER));

        return PeriodComputer.of( //
            ctx -> Period.of( //
                (int)toInteger(years).compute(ctx), //
                (int)toInteger(months).compute(ctx), //
                (int)toInteger(days).compute(ctx)), //
            anyMissing(args));
    }

    //    public static final ExpressionFunction PARSE_PERIOD = functionBuilder() //
    //        .name("parse_period") //
    //        .description("""
    //                The node parses String values into Period values.
    //                """) //
    //        .examples("""
    //                * `period("P1Y2M3D")` returns Period.of(1, 2, 3)
    //                """) //
    //        .keywords("") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("string", "String to parse to a period", isStringOrOpt()) //
    //        ) //
    //        .returnType("A period", "Period", args -> PERIOD(anyOptional(args)))//
    //        .impl(DateTimeFunctions::parsePeriod) //
    //        .build();

    //    private static Computer parsePeriod(final Arguments<Computer> args) {
    //        var string = toString(args.get("string"));
    //
    //        return PeriodComputer.of( //
    //            ctx -> {
    //                var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
    //                return ((DateInterval)parsedInterval).asPeriod();
    //            }, //
    //            ctx -> {
    //                if (anyMissing(args).applyAsBoolean(ctx)) {
    //                    return true;
    //                }
    //
    //                try {
    //                    var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
    //                    return !(parsedInterval instanceof DateInterval);
    //                } catch (IllegalArgumentException e) {
    //                    return true;
    //                }
    //            });
    //    }

    public static final ExpressionFunction CREATE_DURATION = functionBuilder() //
        .name("create_duration") //
        .description("""
                Create a Duration value from the given hours, minutes, seconds and nanoseconds.
                """) //
        .examples("""
                * `create_duration(1, 2, 3, 4)` returns TODO
                """).keywords("create", "duration") //
        .category(CATEGORY_GENERAL) //
        .args( //
            optarg("hours", "Hours of the duration (0 if not given)", isIntegerOrOpt()), //
            optarg("minutes", "Minutes of the duration (0 if not given)", isIntegerOrOpt()), //
            optarg("seconds", "Seconds of the duration (0 if not given)", isIntegerOrOpt()), //
            optarg("nanos", "Nanoseconds of the duration (0 if not given)", isIntegerOrOpt()) //
        ) //
        .returnType("A duration", RETURN_DURATION_MISSING, args -> DURATION(anyOptional(args))) //
        .impl(DateTimeFunctions::createDuration) //
        .build();

    private static Computer createDuration(final Arguments<Computer> args) {
        var hours = args.get("hours", ZERO_INTEGER_COMPUTER);
        var minutes = args.get("minutes", ZERO_INTEGER_COMPUTER);
        var seconds = args.get("seconds", ZERO_INTEGER_COMPUTER);
        var nanos = args.get("nanos", ZERO_INTEGER_COMPUTER);

        return DurationComputer.of( //
            ctx -> Duration.ofHours(toInteger(hours).compute(ctx)) //
                .plusMinutes(toInteger(minutes).compute(ctx)) //
                .plusSeconds(toInteger(seconds).compute(ctx)) //
                .plusNanos(toInteger(nanos).compute(ctx)), //
            anyMissing(args));
    }

    //    public static final ExpressionFunction PARSE_DURATION = functionBuilder() //
    //        .name("parse_duration") //
    //        .description("""
    //                The node parses String values into Duration values.
    //                """) //
    //        .examples("""
    //                * `duration("PT12H")` returns Duration.ofHours(12)
    //                """) //
    //        .keywords("") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("string", "String to parse to a duration", isStringOrOpt()) //
    //        ) //
    //        .returnType("A duration", RETURN_DURATION_MISSING, args -> DURATION(anyOptional(args))) //
    //        .impl(DateTimeFunctions::parseDuration) //
    //        .build();

    //    private static Computer parseDuration(final Arguments<Computer> args) {
    //        var string = toString(args.get("string"));
    //
    //        return DurationComputer.of( //
    //            ctx -> {
    //                var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
    //                return ((TimeInterval)parsedInterval).asDuration();
    //            }, //
    //            ctx -> {
    //                if (anyMissing(args).applyAsBoolean(ctx)) {
    //                    return true;
    //                }
    //
    //                try {
    //                    var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
    //                    return !(parsedInterval instanceof TimeInterval);
    //                } catch (IllegalArgumentException e) {
    //                    return true;
    //                }
    //            } //
    //        );
    //    }

    //    public static final ExpressionFunction FORMAT_INTERVAL = functionBuilder() //
    //        .name("format_interval") //
    //        .description("""
    //                Format Period/Duration values into String values.
    //                """) //
    //        .examples("""
    //                * `format_period(period("P1Y2M3D"))` returns "P1Y2M3D"
    //                """) //
    //        .keywords("") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("interval", "Interval to format", isIntervalOrOpt()), //
    //            optarg("format", "Format of the inteveral string: either 'iso', 'short', or 'long'. Defaults to iso.",
    //                isString()) //
    //        ) //
    //        .returnType("A string", "STRING", args -> STRING(anyOptional(args)))//
    //        .impl(DateTimeFunctions::formatInterval) //
    //        .build();

    //    private static Computer formatInterval(final Arguments<Computer> args) {
    //        var intervalComputer = args.get("interval");
    //
    //        return StringComputer.of( //
    //            ctx -> {
    //                var temporalAmount = toTemporalAmount(intervalComputer, ctx);
    //
    //                var format = args.has("format") //
    //                    ? toString(args.get("format")).compute(ctx) //
    //                    : "iso";
    //
    //                return switch (format) {
    //                    case "iso" -> temporalAmount.toString();
    //                    case "short" -> DurationPeriodFormatUtils.formatTemporalAmountShort(temporalAmount);
    //                    case "long" -> DurationPeriodFormatUtils.formatTemporalAmountLong(temporalAmount);
    //                    default -> throw new IllegalStateException(
    //                        "Unknown format: " + format + ". This should never happen.");
    //                };
    //            }, //
    //            ctx -> {
    //                if (anyMissing(args).applyAsBoolean(ctx)) {
    //                    return true;
    //                }
    //
    //                var format = args.has("format") //
    //                    ? toString(args.get("format")).compute(ctx) //
    //                    : "iso";
    //
    //                return !List.of("iso", "short", "long").contains(format);
    //            } //
    //        );
    //    }

    /* ========================= *
     * PART EXTRACTION FUNCTIONS *
     * ========================= */

    //    public static final ExpressionFunction EXTRACT_INTERVAL_PART = functionBuilder() //
    //        .name("extract_interval_part") //
    //        .description("""
    //                Extracts a part of an interval value.
    //                """) //
    //        .examples("""
    //                * `extract_interval_part(duration("PT12H"), "HOURS")` returns 12
    //                """) //
    //        .keywords("extract", "duration", "period", "interval", "part") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("interval", "Interval to extract part from", hasBaseType(DURATION)), //
    //            arg("part", "Part of the interval to extract", isString()) //
    //        ) //
    //        .returnType("The extracted part of the duration", RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args))) //
    //        .impl(DateTimeFunctions::extractIntervalPart) //
    //        .build();

    //    private static Computer extractIntervalPart(final Arguments<Computer> args) {
    //        var interval = args.get("interval");
    //        var part = toString(args.get("part"));
    //
    //        return IntegerComputer.of( //
    //            ctx -> {
    //                var temporalAmount = toTemporalAmount(interval, ctx);
    //                return ExtractableIntervalField.valueOf(part.compute(ctx)).extractFieldFrom(temporalAmount);
    //            }, //
    //            ctx -> {
    //                if (anyMissing(args).applyAsBoolean(ctx)) {
    //                    return true;
    //                }
    //
    //                ExtractableIntervalField field;
    //                try {
    //                    field = ExtractableIntervalField.valueOf(part.compute(ctx));
    //                } catch (IllegalArgumentException e) {
    //                    return true;
    //                }
    //
    //                var inputDataType = interval instanceof DurationComputer //
    //                    ? DurationCellFactory.TYPE //
    //                    : PeriodCellFactory.TYPE;
    //
    //                return !field.isCompatibleWith(inputDataType);
    //            });
    //    }

    /* ==================== *
     * CONVERSION FUNCTIONS *
     * ==================== */

    //    public static final ExpressionFunction DURATION_TO_DOUBLE = functionBuilder() //
    //        .name("duration_to_double") //
    //        .description("""
    //                Converts a Duration value to a number of the specified unit.
    //                """) //
    //        .examples("""
    //                * `duration_to_double(duration("PT12H30M", "HOURS"))` returns 12.5
    //                """) //
    //        .keywords("convert", "duration", "number", "double") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("duration", "Duration to convert", hasBaseType(DURATION)), //
    //            arg("unit", "Unit to convert to", isString()) //
    //        ) //
    //        .returnType("The duration as a number of the specified unit", RETURN_FLOAT_MISSING,
    //            args -> FLOAT(anyOptional(args))) //
    //        .impl(DateTimeFunctions::durationToDouble) //
    //        .build();

    //    private static Computer durationToDouble(final Arguments<Computer> args) {
    //        var duration = toDuration(args.get("duration"));
    //        var unit = toString(args.get("unit"));
    //
    //        return FloatComputer.of( //
    //            ctx -> {
    //                var temporalAmount = duration.compute(ctx);
    //                var unitThing = AllowedUnitForDurationConversion.valueOf(unit.compute(ctx));
    //
    //                return unitThing.getConversionExact(temporalAmount);
    //            }, //
    //            anyMissing(args));
    //    }

    //    public static final ExpressionFunction DURATION_TO_INTEGER = functionBuilder() //
    //        .name("duration_to_integer") //
    //        .description("""
    //                Converts a Duration value to a number of the specified unit.
    //                """) //
    //        .examples("""
    //                * `duration_to_integer(duration("PT12H", "SECONDS"))` returns 43200
    //                """) //
    //        .keywords("convert", "duration", "number", "integer", "long") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("duration", "Duration to convert", hasBaseType(DURATION)), //
    //            arg("unit", "Unit to convert to", isString()) //
    //        ) //
    //        .returnType("The duration as a number of the specified unit", RETURN_INTEGER_MISSING,
    //            args -> INTEGER(anyOptional(args))) //
    //        .impl(DateTimeFunctions::durationToInteger) //
    //        .build();

    //    private static Computer durationToInteger(final Arguments<Computer> args) {
    //        var duration = toDuration(args.get("duration"));
    //        var unit = toString(args.get("unit"));
    //
    //        return IntegerComputer.of( //
    //            ctx -> {
    //                var temporalAmount = duration.compute(ctx);
    //                var unitThing = AllowedUnitForDurationConversion.valueOf(unit.compute(ctx));
    //
    //                return unitThing.getConversionFloored(temporalAmount);
    //            }, //
    //            anyMissing(args));
    //    }

    private static DateTimeFormatter fromTemporalComputer(final Computer c) {
        if (c instanceof LocalTimeComputer) {
            return DateTimeFormatter.ISO_LOCAL_TIME;
        } else if (c instanceof LocalDateComputer) {
            return DateTimeFormatter.ISO_LOCAL_DATE;
        } else if (c instanceof LocalDateTimeComputer) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        } else if (c instanceof ZonedDateTimeComputer) {
            return DateTimeFormatter.ISO_ZONED_DATE_TIME;
        } else {
            throw FunctionUtils.calledWithIllegalArgs();
        }
    }

    /* ================== *
     * ROUNDING FUNCTIONS *
     * ================== */

    //    public static final ExpressionFunction ROUND_TIME = functionBuilder() //
    //        .name("round_time") //
    //        .description("""
    //                The node rounds Time values to the nearest unit.
    //                """) //
    //        .examples("""
    //                * `round_time(time("12:12"), "HOURS")` returns LocalTime(12, 0)
    //                """) //
    //        .keywords("round", "time") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("time", "Time to round", hasTimePartOrIsOpt()), //
    //            optarg("precision", "Precision to round to", isDurationOrOpt()), //
    //            optarg("strategy", "Rounding strategy: 'FIRST', 'NEAREST', 'LAST'", isStringOrOpt()) //
    //        ) //
    //        .returnType("The input with the time part rounded", RETURN_HAS_TIME_PART_MISSING,
    //            createReturnTypeSupplierWithSameBaseTypeAsArg("time")) //
    //        .impl(DateTimeFunctions::roundTime) //
    //        .build();

    //    private static Computer roundTime(final Arguments<Computer> args) {
    //        var timeComputer = args.get("time");
    //
    //        if (timeComputer instanceof LocalTimeComputer) {
    //            return LocalTimeComputer.of( //
    //                ctx -> (LocalTime)roundTimeBasedTemporal(args, "time", ctx), //
    //                ctx -> roundTemporalIsMissing(args, "time", ctx) //
    //            );
    //        } else if (timeComputer instanceof LocalDateTimeComputer) {
    //            return LocalDateTimeComputer.of( //
    //                ctx -> (LocalDateTime)roundTimeBasedTemporal(args, "time", ctx), //
    //                ctx -> roundTemporalIsMissing(args, "time", ctx) //
    //            );
    //        } else if (timeComputer instanceof ZonedDateTimeComputer) {
    //            return ZonedDateTimeComputer.of( //
    //                ctx -> (ZonedDateTime)roundTimeBasedTemporal(args, "time", ctx), //
    //                ctx -> roundTemporalIsMissing(args, "time", ctx) //
    //            );
    //        } else {
    //            throw FunctionUtils.calledWithIllegalArgs();
    //        }
    //    }

    //    public static final ExpressionFunction ROUND_DATE = functionBuilder() //
    //        .name("round_date") //
    //        .description("""
    //                The node rounds Date values to the nearest unit.
    //                """) //
    //        .examples("""
    //                * `round_date(date("2021-01-01"), "MONTHS")` returns LocalDate(2021-01-01)
    //                """) //
    //        .keywords("round", "date") //
    //        .category(CATEGORY_GENERAL) //
    //        .args( //
    //            arg("date", "Date to round", hasTimePartOrIsOpt()), //
    //            optarg("precision", "Precision to round to: " + RoundDatePrecision.values(), isStringOrOpt()), //
    //            optarg("strategy", "Rounding strategy: 'FIRST', 'NEAREST', 'LAST'", isStringOrOpt()), //
    //            optarg("shift_mode", "Shift mode: 'PREVIOUS','THIS', 'NEXT'", isStringOrOpt()), //
    //            optarg("dayOrWeekday", "If set to weekday rounding will exclude weekends", isStringOrOpt()) //
    //        ) //
    //        .returnType("The input type with the date part rounded", RETURN_HAS_DATE_PART_MISSING,
    //            createReturnTypeSupplierWithSameBaseTypeAsArg("date")) //
    //        .impl(DateTimeFunctions::roundDate) //
    //        .build();

    //    private static Computer roundDate(final Arguments<Computer> args) {
    //        var dateComputer = args.get("date");
    //
    //        if (dateComputer instanceof LocalDateComputer) {
    //            return LocalDateComputer.of( //
    //                ctx -> (LocalDate)roundDateBasedTemporal(args, "date", ctx), //
    //                ctx -> roundTemporalIsMissing(args, "date", ctx) //
    //            );
    //        } else if (dateComputer instanceof LocalDateTimeComputer) {
    //            return LocalDateTimeComputer.of( //
    //                ctx -> (LocalDateTime)roundDateBasedTemporal(args, "date", ctx), //
    //                ctx -> roundTemporalIsMissing(args, "date", ctx) //
    //            );
    //        } else if (dateComputer instanceof ZonedDateTimeComputer) {
    //            return ZonedDateTimeComputer.of( //
    //                ctx -> (ZonedDateTime)roundDateBasedTemporal(args, "date", ctx), //
    //                ctx -> roundTemporalIsMissing(args, "date", ctx) //
    //            );
    //        } else {
    //            throw FunctionUtils.calledWithIllegalArgs();
    //        }
    //    }

    /* ====================== *
     * MODIFY PARTS FUNCTIONS *
     * ====================== */

    public static final ExpressionFunction MODIFY_DATE = functionBuilder() //
        .name("modify_date") //
        .description("""
                Modify the date part of a date-time by setting some of its fields
                """) //
        .examples("""
                * `modify_date(date("2021-01-01"), year=2022)` returns LocalDate(2022-01-01)
                """) //
        .keywords("modify", "date") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("date", "Date to modify", hasDatePartOrIsOpt()), //
            optarg("year", "Year to set", isIntegerOrOpt()), //
            optarg("month", "Month to set", isIntegerOrOpt()), //
            optarg("day", "Day to set", isIntegerOrOpt()) //
        ) //
        .returnType("The input type with the date part modified", RETURN_HAS_DATE_PART_MISSING,
            createReturnTypeSupplierWithSameBaseTypeAsArg("date")) //
        .impl(DateTimeFunctions::modifyDate) //
        .build();

    private static Computer modifyDate(final Arguments<Computer> args) {
        var dateComputer = args.get("date");

        Function<EvaluationContext, Temporal> valueSupplier = ctx -> {
            var date = Computer.computeTemporal(dateComputer, ctx);
            var year = Optional.ofNullable(args.has("year") ? toInteger(args.get("year")).compute(ctx) : null);
            var month = Optional.ofNullable(args.has("month") ? toInteger(args.get("month")).compute(ctx) : null);
            var day = Optional.ofNullable(args.has("day") ? toInteger(args.get("day")).compute(ctx) : null);

            date = date.with(ChronoField.YEAR, year.orElse((long)date.get(ChronoField.YEAR)));
            date = date.with(ChronoField.MONTH_OF_YEAR, month.orElse((long)date.get(ChronoField.MONTH_OF_YEAR)));
            date = date.with(ChronoField.DAY_OF_MONTH, day.orElse((long)date.get(ChronoField.DAY_OF_MONTH)));

            return date;
        };

        if (dateComputer instanceof LocalDateComputer) {
            return LocalDateComputer.of(valueSupplier.andThen(LocalDate.class::cast), anyMissing(args));
        } else if (dateComputer instanceof LocalDateTimeComputer) {
            return LocalDateTimeComputer.of(valueSupplier.andThen(LocalDateTime.class::cast), anyMissing(args));
        } else if (dateComputer instanceof ZonedDateTimeComputer) {
            return ZonedDateTimeComputer.of(valueSupplier.andThen(ZonedDateTime.class::cast), anyMissing(args));
        } else {
            throw FunctionUtils.calledWithIllegalArgs();
        }
    }

    public static final ExpressionFunction MODIFY_TIME = functionBuilder() //
        .name("modify_time") //
        .description("""
                Modify the time part of a date-time by setting some of its fields
                """) //
        .examples("""
                * `modify_time(time("12:12"), hour=13)` returns LocalTime(13:12)
                """) //
        .keywords("modify", "time") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("time", "Time to modify", hasTimePartOrIsOpt()), //
            optarg("hour", "Hour to set", isIntegerOrOpt()), //
            optarg("minute", "Minute to set", isIntegerOrOpt()), //
            optarg("second", "Second to set", isIntegerOrOpt()), //
            optarg("nano", "Nanosecond to set", isIntegerOrOpt()) //
        ) //
        .returnType("The input type with the time part modified", RETURN_HAS_TIME_PART_MISSING,
            createReturnTypeSupplierWithSameBaseTypeAsArg("time")) //
        .impl(DateTimeFunctions::modifyTime) //
        .build();

    private static Computer modifyTime(final Arguments<Computer> args) {
        var timeComputer = args.get("time");

        Function<EvaluationContext, Temporal> valueSupplier = ctx -> {
            var time = Computer.computeTemporal(timeComputer, ctx);
            var hour = Optional.ofNullable(args.has("hour") ? toInteger(args.get("hour")).compute(ctx) : null);
            var minute = Optional.ofNullable(args.has("minute") ? toInteger(args.get("minute")).compute(ctx) : null);
            var second = Optional.ofNullable(args.has("second") ? toInteger(args.get("second")).compute(ctx) : null);
            var nano = Optional.ofNullable(args.has("nano") ? toInteger(args.get("nano")).compute(ctx) : null);

            time = time.with(ChronoField.HOUR_OF_DAY, hour.orElse((long)time.get(ChronoField.HOUR_OF_DAY)));
            time = time.with(ChronoField.MINUTE_OF_HOUR, minute.orElse((long)time.get(ChronoField.MINUTE_OF_HOUR)));
            time = time.with(ChronoField.SECOND_OF_MINUTE, second.orElse((long)time.get(ChronoField.SECOND_OF_MINUTE)));
            time = time.with(ChronoField.NANO_OF_SECOND, nano.orElse((long)time.get(ChronoField.NANO_OF_SECOND)));

            return time;
        };

        if (timeComputer instanceof LocalTimeComputer) {
            return LocalTimeComputer.of(valueSupplier.andThen(LocalTime.class::cast), anyMissing(args));
        } else if (timeComputer instanceof LocalDateTimeComputer) {
            return LocalDateTimeComputer.of(valueSupplier.andThen(LocalDateTime.class::cast), anyMissing(args));
        } else if (timeComputer instanceof ZonedDateTimeComputer) {
            return ZonedDateTimeComputer.of(valueSupplier.andThen(ZonedDateTime.class::cast), anyMissing(args));
        } else {
            throw FunctionUtils.calledWithIllegalArgs();
        }
    }

    public static final ExpressionFunction MODIFY_TIME_ZONE = functionBuilder() //
        .name("modify_time_zone") //
        .description("""
                Modify the time zone part of a ZonedDateTime
                """) //
        .examples(
            """
                    * `modify_time_zone(zoned_date_time("2021-01-01T12:12+01:00[Europe/Berlin]"), "Europe/Paris")` returns ZonedDateTime("2021-01-01T12:12+01:00[Europe/Paris]")
                    """) //
        .keywords("modify", "time", "zone") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("zoned_date_time", "ZonedDateTime to modify", hasBaseType(ZONED_DATE_TIME)), //
            arg("zone_id", "ZoneId to set", isStringOrOpt()) //
        ) //
        .returnType("The input type with the time zone part modified", RETURN_ZONED_DATE_TIME_MISSING,
            args -> ZONED_DATE_TIME(anyOptional(args))) //
        .impl(DateTimeFunctions::modifyTimeZone) //
        .build();

    private static Computer modifyTimeZone(final Arguments<Computer> args) {
        var zonedDateTimeComputer = args.get("zoned_date_time");
        var zoneIdComputer = toString(args.get("zone_id"));

        return ZonedDateTimeComputer.of( //
            ctx -> {
                var zonedDateTime = (ZonedDateTime)Computer.computeTemporal(zonedDateTimeComputer, ctx);
                var zoneId = zoneIdComputer.compute(ctx);
                return zonedDateTime.withZoneSameLocal(ZoneId.of(zoneId));
            }, //
            anyMissing(args));
    }

    // ======================= UTILITIES ==============================

    private static Function<EvaluationContext, DateTimeFormatter> createFormatter(final Arguments<Computer> args,
        final DateTimeFormatter defaultFormatter) {
        return ctx -> {
            var locale = args.has("locale") //
                ? Locale.forLanguageTag(toString(args.get("locale")).compute(ctx)) //
                : Locale.ENGLISH;
            if (args.has("format")) {
                return DateTimeFormatter.ofPattern(toString(args.get("format")).compute(ctx), locale)
                    .withChronology(Chronology.ofLocale(locale));
            } else {
                return defaultFormatter.withLocale(locale);
            }
        };
    }

    private static Function<Arguments<ValueType>, ValueType>
        createReturnTypeSupplierWithSameBaseTypeAsArg(final String argThatDeterminesBaseType) {
        return args -> {
            var baseType = args.get(argThatDeterminesBaseType).baseType();
            return anyOptional(args) ? baseType.optionalType() : baseType;
        };
    }

    private static StringComputer toString(final Computer c) {
        if (c instanceof StringComputer sc) {
            return sc;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static DurationComputer toDuration(final Computer c) {
        if (c instanceof DurationComputer dc) {
            return dc;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static PeriodComputer toPeriod(final Computer c) {
        if (c instanceof PeriodComputer pc) {
            return pc;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    //    private static final Computer defaultTimeRoundStrategyComputer =
    //        createConstantComputer(TimeRoundingStrategy.FIRST_POINT_IN_TIME.name());
    //
    //    private static final Computer defaultDateRoundStrategyComputer =
    //        createConstantComputer(DateRoundingStrategy.FIRST.name());
    //
    //    private static final Computer defaultTimeShiftModeComputer = createConstantComputer(ShiftMode.THIS.name());
    //
    //    private static final Computer defaultDayOrWWeekdayComputer = createConstantComputer(DayOrWeekday.DAY.name());

    private static final Computer zeroDurationComputer = createConstantComputer(Duration.ZERO);

    private static final Computer ZERO_INTEGER_COMPUTER = createConstantComputer(0);

    private static final Computer oneHourDurationComputer = createConstantComputer(Duration.ofHours(1));

    //    private static final Computer defaultDatePrecisionComputer =
    //        createConstantComputer(RoundDatePrecision.MONTH.name());

    /**
     * Helper function to round time-based temporal values. Assumption: The arguments 'strategy' and 'precision' are
     * present, where precision is a Duration.
     *
     * @param args arguments to the function
     * @param temporalArgumentName name of the temporal argument to round
     * @param ctx evaluation context
     * @return the rounded temporal value
     */
    //    private static Temporal roundTimeBasedTemporal(final Arguments<Computer> args, final String temporalArgumentName,
    //        final EvaluationContext ctx) {
    //        var temporalComputer = args.get(temporalArgumentName);
    //        var strategyComputer = toString(args.get("strategy", defaultTimeRoundStrategyComputer));
    //        var precisionComputer = toDuration(args.get("precision", oneHourDurationComputer));
    //
    //        var timeToRound = Computer.computeTemporal(temporalComputer, ctx);
    //        var strategy = TimeRoundingStrategy.valueOf(strategyComputer.compute(ctx));
    //        var precision = precisionComputer.compute(ctx);
    //
    //        return TimeRoundingUtil.roundTimeBasedTemporal(timeToRound, strategy, precision);
    //    }

    /**
     * Helper function to round time-based temporal values. Assumption: The arguments 'strategy' and 'precision' are
     * present, where precision is a Period.
     *
     * @param args arguments to the function
     * @param temporalArgumentName name of the temporal argument to round
     * @param ctx evaluation context
     * @return true if the temporal argument is missing or if the strategy or precision is missing
     */
    //    private static Temporal roundDateBasedTemporal(final Arguments<Computer> args, final String temporalArgumentName,
    //        final EvaluationContext ctx) {
    //
    //        var temporalComputer = args.get(temporalArgumentName);
    //        var strategyComputer = toString(args.get("strategy", defaultDateRoundStrategyComputer));
    //        var precisionComputer = toString(args.get("precision", defaultDatePrecisionComputer));
    //        var shiftModeComputer = toString(args.get("shift_mode", defaultTimeShiftModeComputer));
    //        var dayOrWeekdayComputer = toString(args.get("dayOrWeekday", defaultDayOrWWeekdayComputer));
    //
    //        var dateToRound = Computer.computeTemporal(temporalComputer, ctx);
    //        var strategy = DateRoundingStrategy.valueOf(strategyComputer.compute(ctx));
    //        var precision = RoundDatePrecision.valueOf(precisionComputer.compute(ctx));
    //        var shiftMode = ShiftMode.valueOf(shiftModeComputer.compute(ctx));
    //        var dayOrWeekday = DayOrWeekday.valueOf(dayOrWeekdayComputer.compute(ctx));
    //
    //        return DateRoundingUtil.roundDateBasedTemporal(dateToRound, strategy, precision, shiftMode, dayOrWeekday);
    //    }

    /**
     * Helper function to determine if the temporal argument is missing or if the strategy or precision is missing.
     *
     * @param args arguments to the function
     * @param temporalArgumentName name of the temporal argument to round
     * @param ctx evaluation context
     * @return true if the temporal argument is missing or if the strategy or precision is missing
     */
    private static boolean roundTemporalIsMissing(final Arguments<Computer> args, final String temporalArgumentName,
        final EvaluationContext ctx) {
        if (args.get(temporalArgumentName).isMissing(ctx)) {
            return true;
        }

        if (args.has("strategy") && args.get("strategy").isMissing(ctx)) {
            return true;
        }

        if (args.has("precision") && args.get("precision").isMissing(ctx)) {
            return true;
        }

        return false;
    }

    private static IntegerComputer toInteger(final Computer computer) {
        if (computer instanceof IntegerComputer i) {
            return i;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static BooleanComputer toBoolean(final Computer computer) {
        if (computer instanceof BooleanComputer b) {
            return b;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static TemporalAmount toTemporalAmount(final Computer computer, final EvaluationContext ctx) {
        if (computer instanceof DurationComputer dc) {
            return dc.compute(ctx);
        } else if (computer instanceof PeriodComputer pc) {
            return pc.compute(ctx);
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }
}