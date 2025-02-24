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
 *   Feb 21, 2025 (david): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_DATE_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_FLOAT_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_INTEGER_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_STRING_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_TIME_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_ZONED_DATE_TIME_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasDateInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.hasTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isDateDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isIntegerOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isOneOfBaseTypes;
import static org.knime.core.expressions.SignatureUtils.isString;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.isTimeDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isZonedDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.knime.core.expressions.Computer.TemporalAccessorComputer;
import org.knime.core.expressions.Computer.TemporalAmountComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.TimeBasedGranularityUnit;

/**
 * Implementation of built-in functions that manipulate temporal data such as dates, times, durations, and periods.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
public final class TemporalFunctions {

    private static final String JAVADOC_URL_DATE_FORMAT =
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html";

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // seconds and subseconds optional
    private static final DateTimeFormatter DEFAULT_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    // seconds and subseconds optional
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // zone id optional, but offset required
    private static final DateTimeFormatter DEFAULT_ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    private static final String TEMPORAL_META_CATEGORY_NAME = "Temporal";

    public static final OperatorCategory CATEGORY_PARSE_FORMAT =
        new OperatorCategory(TEMPORAL_META_CATEGORY_NAME, "Parsing & Formatting", """
                Functions for parsing and formatting temporal data to and from strings, such as dates, times, \
                and date- and time-based durations.
                """);

    public static final OperatorCategory CATEGORY_CREATE_EXTRACT =
        new OperatorCategory(TEMPORAL_META_CATEGORY_NAME, "Creation & Extraction", """
                Functions for creating and extracting temporal data, such as dates, times, and intervals.
                """);

    /**
     * Implementation of a function that parses a string into a {@link TemporalAccessor} type, such as a
     * {@link LocalDate}.
     *
     * @param <T> the type of {@link TemporalAccessor} to parse the string into, such as {@link LocalDate}.
     * @param temporalType the class of the {@link TemporalAccessor} type to parse the string into.
     * @param temporalQuery the {@link TemporalQuery} to use to parse the string into the {@link TemporalAccessor}.
     * @param temporalArgName the name of the argument that contains the string to parse.
     * @param defaultFormat the default {@link DateTimeFormatter} to use if the format is not specified.
     * @return the implementation for the function.
     */
    private static <T extends TemporalAccessor> Function<Arguments<Computer>, Computer> parseTemporalAccessorImpl(
        final Class<T> temporalType, final TemporalQuery<T> temporalQuery, final String temporalArgName,
        final DateTimeFormatter defaultFormat) {

        return args -> {
            var formatStringComputer = (StringComputer)args.get("format", StringComputer.ofConstant(null));

            ComputerResultSupplier<Optional<T>> valueSupplier = ctx -> {
                // first check if the format is good, or just use the default if not provided
                var formatStringOrNull = formatStringComputer.compute(ctx);
                DateTimeFormatter formatter;
                try {
                    formatter = Optional.ofNullable(formatStringOrNull)//
                        .map(DateTimeFormatter::ofPattern) //
                        .orElse(defaultFormat);
                } catch (IllegalArgumentException e) {
                    ctx.addWarning("Unparseable temporal format '%s'".formatted(formatStringOrNull));
                    return Optional.empty();
                }

                var dateString = ((StringComputer)args.get(temporalArgName)).compute(ctx);
                T parsed;
                try {
                    parsed = formatter.parse(dateString, temporalQuery);
                } catch (DateTimeParseException e) {
                    ctx.addWarning("Temporal string '%s' did not match format".formatted(dateString));
                    return Optional.empty();
                }

                return Optional.of(parsed);
            };

            // now we can create the computer, but we need to exhaustively check all possible types that could be
            // T. A little messy but oh well.
            if (temporalType == LocalDate.class) {
                return LocalDateComputer.of( //
                    ctx -> (LocalDate)valueSupplier.apply(ctx).get(), //
                    ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
                );
            } else if (temporalType == LocalTime.class) {
                return LocalTimeComputer.of( //
                    ctx -> (LocalTime)valueSupplier.apply(ctx).get(), //
                    ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
                );
            } else if (temporalType == LocalDateTime.class) {
                return LocalDateTimeComputer.of( //
                    ctx -> (LocalDateTime)valueSupplier.apply(ctx).get(), //
                    ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
                );
            } else if (temporalType == ZonedDateTime.class) {
                return ZonedDateTimeComputer.of( //
                    ctx -> (ZonedDateTime)valueSupplier.apply(ctx).get(), //
                    ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
                );
            } else {
                throw new IllegalArgumentException("Unsupported temporal type: " + temporalType);
            }
        };
    }

    public static final ExpressionFunction PARSE_LOCAL_DATE = functionBuilder() //
        .name("parse_date") //
        .description("""
                Parses a string into a `LOCAL_DATE` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                (`yyyy-MM-dd`) is used.

                If the format string is invalid, or the provided date string does not match the provided \
                format, the function returns `MISSING` and a warning is emitted. The function also returns \
                `MISSING` if the date string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_date("1970-01-01")` returns `1970-01-01`
                * `parse_date("01/01/1970", "dd/MM/yyyy")` returns `1970-01-01`
                * `parse_date("invalid date")` returns `MISSING`
                * `parse_date("01/01/1970", "invalid format")` returns `MISSING`
                """) //
        .keywords("parse", "date", "local_date") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("date", "The string to parse into a date.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the date. If not specified, the default format is used.",
                isString()) //
        ) //
        .returnType("A `LOCAL_DATE` representing the provided string argument", RETURN_LOCAL_DATE_MISSING,
            args -> OPT_LOCAL_DATE) //
        .impl(
            TemporalFunctions.parseTemporalAccessorImpl(LocalDate.class, LocalDate::from, "date", DEFAULT_DATE_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_LOCAL_TIME = functionBuilder() //
        .name("parse_time") //
        .description("""
                Parses a string into a `LOCAL_TIME` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                is used, in which the hours and minutes are required but but seconds and subseconds are \
                optional. See the examples for more information.

                If the format string is invalid, or the provided time string does not match the provided \
                format, the function returns `MISSING` and a warning is emitted. The function also returns \
                `MISSING` if the time string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_time("12:34")` returns `12:34:00`
                * `parse_time("12:34:56")` returns `12:34:56`
                * `parse_time("12:34:56.789")` returns `12:34:56.789`
                * `parse_time("00.00.00", "HH.mm.ss")` returns `00:00:00`
                * `parse_time("00:00:00", "invalid pattern")` returns `MISSING`
                * `parse_time("invalid time")` returns `MISSING`
                """) //
        .keywords("parse", "time", "local_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("time", "The string to parse into a time.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the time. If not specified, the default format is used.",
                isString()) //
        ) //
        .returnType("A `LOCAL_TIME` representing the provided string argument", RETURN_LOCAL_TIME_MISSING,
            args -> OPT_LOCAL_TIME) //
        .impl(
            TemporalFunctions.parseTemporalAccessorImpl(LocalTime.class, LocalTime::from, "time", DEFAULT_TIME_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_LOCAL_DATE_TIME = functionBuilder() //
        .name("parse_datetime") //
        .description("""
                Parses a string into a `LOCAL_DATE_TIME` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                is used, which is the default format for `parse_date` and the default format for `parse_time` \
                concatenated together, separated by a T. See the examples for more information.

                If the format string is invalid, or the provided date time string does not match the provided \
                format, the function returns `MISSING` and a warning is emitted. The function also returns \
                `MISSING` if the date time string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_datetime("1970-01-01T00:00:00")` returns `1970-01-01T00:00:00`
                * `parse_datetime("1970-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")` returns `1970-01-01T00:00:00`
                * `parse_datetime("1970-01-01T00:00:00", "invalid format")` returns `MISSING`
                * `parse_datetime("invalid datetime")` returns `MISSING`
                """) //
        .keywords("parse", "datetime", "local_date_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("datetime", "The string to parse into a date time.", isStringOrOpt()), //
            optarg("format",
                "The format to use for parsing the date time. If not specified, the default format is used.",
                isString()) //
        ) //
        .returnType("A `LOCAL_DATE_TIME` representing the provided string argument", RETURN_LOCAL_DATE_TIME_MISSING,
            args -> OPT_LOCAL_DATE_TIME) //
        .impl(TemporalFunctions.parseTemporalAccessorImpl(LocalDateTime.class, LocalDateTime::from, "datetime",
            DEFAULT_DATE_TIME_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_ZONED_DATE_TIME = functionBuilder() //
        .name("parse_zoned_datetime") //
        .description("""
                Parses a string into a `ZONED_DATE_TIME` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                is used, which is the default format for `parse_date_time` with the addition of a required \
                offset and optional zone ID (where `Z` is shorthand for UTC). See the examples for more \
                information.

                If the format string is invalid, or the provided zoned date time string does not match the \
                provided format, the function returns `MISSING` and a warning is emitted. The function also \
                returns `MISSING` if the zoned date time string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_zoned_datetime("1970-01-01T00:00:00Z")` returns `1970-01-01T00:00:00Z`
                * `parse_zoned_datetime("1970-01-01 00:00:00Z", "yyyy-MM-dd HH:mm:ssX")` returns `1970-01-01T00:00:00Z`
                * `parse_zoned_datetime("1970-01-01T00:00:00+01:00")` returns `1970-01-01T00:00:00+01:00`
                * `parse_zoned_datetime("1970-01-01T00:00:00+01:00[Europe/Paris]")` returns \
                `1970-01-01T00:00:00+01:00[Europe/Paris]`
                * `parse_zoned_datetime("1970-01-01T00:00:00Z", "invalid format")` returns `MISSING`
                * `parse_zoned_datetime("invalid zoned datetime")` returns `MISSING`
                """) //
        .keywords("parse", "datetime", "zoned_date_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("datetime", "The string to parse into a zoned date time.", isStringOrOpt()), //
            optarg("format",
                "The format to use for parsing the zoned date time. If not specified, the default format is used.",
                isString()) //
        ) //
        .returnType("A `ZONED_DATE_TIME` representing the provided string argument", RETURN_ZONED_DATE_TIME_MISSING,
            args -> OPT_ZONED_DATE_TIME) //
        .impl(TemporalFunctions.parseTemporalAccessorImpl(ZonedDateTime.class, ZonedDateTime::from, "datetime",
            DEFAULT_ZONED_DATE_TIME_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_TIME_DURATION = functionBuilder() //
        .name("parse_time_duration") //
        .description("""
                Parses a string into a `TIME_DURATION`.

                The function supports three formats: `short`, `long`, and `iso`. The `short` and `long` formats \
                are used for parsing time durations in a human-readable format, such as `1h` or `1 hour`. The \
                `iso` format is used for parsing time durations in the ISO-8601 format, such as `PT1H`. If the \
                format is not specified, `iso` is used by default.

                If the provided format is not one of the supported formats, the function returns `MISSING` and a \
                warning is emitted. The function also returns `MISSING` if the duration string is missing or \
                does not match the provided format.
                """) //
        .examples("""
                * `parseduration("PT1H")` returns `PT1H`
                * `parseduration("1h 2m 3.5s", "short")` returns `PT1H2M3.5S`
                * `parseduration("1 hour 2 minutes 3.5 seconds", "long")` returns `PT1H2M3.5S`
                * `parseduration("invalid duration")` returns `MISSING`
                * `parseduration("PT1H", "invalid format")` returns `MISSING`
                """) //
        .keywords("parse", "duration", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The string to parse into a time duration.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the duration.", isString()) //
        ) //
        .returnType("A `TIME_DURATION` representing the provided string argument", RETURN_TIME_DURATION_MISSING,
            args -> OPT_TIME_DURATION) //
        .impl(TemporalFunctions::parseTimeDurationImpl) //
        .build();

    private static Computer parseTimeDurationImpl(final Arguments<Computer> args) {
        var styleComputer = (StringComputer)args.get("format", StringComputer.ofConstant("iso"));

        ComputerResultSupplier<Optional<Duration>> valueSupplier = ctx -> {
            Map<String, Predicate<String>> durationCheckersByType = Map.of( //
                "long", TemporalFunctions::isLongFormDuration, //
                "short", TemporalFunctions::isShortormDuration, //
                "iso", TemporalFunctions::isIsoFormDuration //
            );

            var style = styleComputer.compute(ctx);
            if (!durationCheckersByType.containsKey(style)) {
                ctx.addWarning("Unknown duration format '%s'. Allowed formats are: %s.".formatted(style,
                    durationCheckersByType.keySet().stream().collect(Collectors.joining(", "))));
                return Optional.empty();
            }

            var durationString = ((StringComputer)args.get("duration")).compute(ctx).trim();

            if (!durationCheckersByType.get(style).test(durationString)) {
                ctx.addWarning("Duration string '%s' did not match format '%s'".formatted(durationString, style));
                return Optional.empty();
            }

            try {
                return Optional.of(DurationPeriodFormatUtils.parseDuration(durationString));
            } catch (DateTimeParseException ex) {
                ctx.addWarning("Invalid time duration string '%s'".formatted(durationString));
                return Optional.empty();
            }
        };

        return TimeDurationComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction PARSE_DATE_DURATION = functionBuilder() //
        .name("parse_date_duration") //
        .description("""
                Parses a string into a `DATE_DURATION`.

                The function supports three formats: `short`, `long`, and `iso`. The `short` and `long` formats \
                are used for parsing date durations in a human-readable format, such as `1y` or `1 year`. The \
                `iso` format is used for parsing date durations in the ISO-8601 format, such as `P1Y`. If the \
                format is not specified, `iso` is used by default.

                If the provided format is not one of the supported formats, the function returns `MISSING` and a \
                warning is emitted. The function also returns `MISSING` if the duration string is missing or \
                does not match the provided format.
                """) //
        .examples("""
                * `parse_date_duration("P1Y")` returns `P1Y`
                * `parse_date_duration("1y", "short")` returns `P1Y`
                * `parse_date_duration("1 year", "long")` returns `P1Y`
                * `parse_date_duration("1 year")` returns `MISSING`
                """) //
        .keywords("parse", "period", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The string to parse into a date duration.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the duration.", isString()) //
        ) //
        .returnType("A `DATE_DURATION` representing the provided string argument", RETURN_DATE_DURATION_MISSING,
            args -> OPT_DATE_DURATION) //
        .impl(TemporalFunctions::parseDateDurationImpl) //
        .build();

    private static Computer parseDateDurationImpl(final Arguments<Computer> args) {
        var styleComputer = (StringComputer)args.get("format", StringComputer.ofConstant("iso"));

        ComputerResultSupplier<Optional<Period>> valueSupplier = ctx -> {
            Map<String, Predicate<String>> periodCheckersByType = Map.of( //
                "long", TemporalFunctions::isLongFormPeriod, //
                "short", TemporalFunctions::isShortFormPeriod, //
                "iso", TemporalFunctions::isIsoFormPeriod //
            );

            var style = styleComputer.compute(ctx);
            if (!periodCheckersByType.containsKey(style)) {
                ctx.addWarning("Unknown period style '%s'".formatted(style));
                return Optional.empty();
            }

            var periodString = ((StringComputer)args.get("duration")).compute(ctx).trim();

            if (!periodCheckersByType.get(style).test(periodString)) {
                ctx.addWarning("Period string '%s' did not match style '%s'".formatted(periodString, style));
                return Optional.empty();
            }

            try {
                return Optional.of(DurationPeriodFormatUtils.parsePeriod(periodString));
            } catch (DateTimeParseException ex) {
                ctx.addWarning("Invalid date duration string '%s'".formatted(periodString));
                return Optional.empty();
            }
        };

        return DateDurationComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    private static Function<Arguments<Computer>, Computer> formatTemporalImpl(final String temporalArgument) {
        return args -> {
            ComputerResultSupplier<Optional<String>> valueSupplier = ctx -> {
                var formatString = ((StringComputer)args.get("format")).compute(ctx);
                var temporal = ((TemporalAccessorComputer)args.get(temporalArgument)).compute(ctx);

                DateTimeFormatter formatter;
                try {
                    formatter = DateTimeFormatter.ofPattern(formatString);
                } catch (IllegalArgumentException e) {
                    ctx.addWarning("Unparseable temporal format '%s'".formatted(formatString));
                    return Optional.empty();
                }

                String formatted;
                try {
                    formatted = formatter.format(temporal);
                } catch (DateTimeException e) {
                    ctx.addWarning("Could not format temporal value: %s".formatted(e.getMessage()));
                    return Optional.empty();
                }

                return Optional.of(formatted);
            };

            return StringComputer.of( //
                ctx -> valueSupplier.apply(ctx).get(), //
                ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
            );
        };
    }

    public static final ExpressionFunction FORMAT_LOCAL_DATE = functionBuilder() //
        .name("format_local_date") //
        .description("""
                Formats a `LOCAL_DATE` to a string using the provided format. See [here](%s) for more \
                information about the format patterns.

                If the format string is invalid, or the provided date cannot be formatted using the provided
                format, the function returns `MISSING` and a warning is emitted. The function also returns `MISSING`
                if the input string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_local_date(parse_date("1970-01-01"), "dd/MM/yyyy")` returns `01/01/1970`
                * `format_local_date(parse_date("1970-01-01"), "invalid format")` returns `MISSING`
                * `format_local_date(parse_date("invalid date"), "dd/MM/yyyy")` returns `MISSING`
                * `format_local_date(parse_date("1970-01-01"), "HH:mm:ss")` returns `MISSING`
                """) //
        .keywords("format", "date", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("date", "The date to format as a string.", isLocalDateOrOpt()), //
            arg("format", "The format to use for formatting the temporal value.", isString()) //
        ) //
        .returnType("A string representing the provided `LOCAL_DATE`", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalFunctions.formatTemporalImpl("date")) //
        .build();

    public static final ExpressionFunction FORMAT_LOCAL_TIME = functionBuilder() //
        .name("format_local_time") //
        .description("""
                Formats a `LOCAL_TIME` to a string using the provided format. See [here](%s) for more \
                information about the format patterns.

                If the format string is invalid, or the provided time cannot be formatted using the provided
                format, the function returns `MISSING` and a warning is emitted. The function also returns `MISSING`
                if the input string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_temporal(parsetime("12:34"), "HH:mm:ss")` returns `12:34:00`
                * `format_temporal(parsetime("12:34"), "invalid format")` returns `MISSING`
                * `format_temporal(parsetime("invalid time"), "HH:mm:ss")` returns `MISSING`
                * `format_temporal(parsetime("12:34"), "dd/MM/yyyy")` returns `MISSING`
                """) //
        .keywords("format", "time", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("time", "The time value to format as a string.", isLocalTimeOrOpt()), //
            arg("format", "The format to use.", isString()) //
        ) //
        .returnType("A string representing the provided `LOCAL_TIME`", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalFunctions.formatTemporalImpl("time")) //
        .build();

    public static final ExpressionFunction FORMAT_LOCAL_DATE_TIME = functionBuilder() //
        .name("format_local_date_time") //
        .description("""
                Formats a `LOCAL_DATE_TIME` to a string using the provided format. See [here](%s) for more \
                information about the format patterns.

                If the format string is invalid, or the provided date time cannot be formatted using the provided
                format, the function returns `MISSING` and a warning is emitted. The function also returns `MISSING`
                if the input string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_local_date_time($["my_datetime_col"], "dd/MM/yyyy HH:mm:ss")` returns \
                `01/01/1970 00:00:00`
                * `format_local_date_time($["my_datetime_col"], "invalid format")` returns `MISSING`
                * `format_local_date_time($["my_datetime_col"], "dd/MM/yyyy HH:mm:ss")` returns `MISSING`
                * `format_local_date_time($["my_datetime_col"], "zzz")` returns `MISSING`
                """) //
        .keywords("format", "datetime", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("datetime", "The date time value to format as a string.", isLocalDateTimeOrOpt()), //
            arg("format", "The format to use.", isString()) //
        ) //
        .returnType("A string representing the provided `LOCAL_DATE_TIME`", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalFunctions.formatTemporalImpl("datetime")) //
        .build();

    public static final ExpressionFunction FORMAT_ZONED_DATE_TIME = functionBuilder() //
        .name("format_zoned_date_time") //
        .description("""
                Formats a `ZONED_DATE_TIME` to a string using the provided format. See [here](%s) for more \
                information about the format patterns.

                If the format string is invalid, or the provided zoned date time cannot be formatted using the provided
                format, the function returns `MISSING` and a warning is emitted. The function also returns `MISSING`
                if the input string is missing.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_zoned_date_time($["my_zoned_col"], "dd/MM/yyyy HH:mm:ss")` returns \
                `01/01/1970 00:00:00`
                * `format_zoned_date_time($["my_zoned_col"], "invalid format")` returns `MISSING`
                * `format_zoned_date_time($["my_zoned_col"], "dd/MM/yyyy HH:mm:ss")` returns `MISSING`
                """) //
        .keywords("format", "datetime", "zone", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("datetime", "The zoned date time value to format as a string.", isZonedDateTimeOrOpt()), //
            arg("format", "The format to use.", isString()) //
        ) //
        .returnType("A string representing the provided `ZONED_DATE_TIME`", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalFunctions.formatTemporalImpl("datetime")) //
        .build();

    private static final String ILLEGAL_DURATION_FORMAT_WARNING =
        "Unknown format '%s'. Allowed are: 'iso', 'long', and 'short'";

    private static Function<Arguments<Computer>, Computer> formatIntervalImpl(final String intervalArgument) {
        return args -> {
            ComputerResultSupplier<Optional<String>> valueSupplier = ctx -> {
                var style = ((StringComputer)args.get("format")).compute(ctx);
                var temporalAmount = ((TemporalAmountComputer)args.get(intervalArgument)).compute(ctx);

                if (temporalAmount instanceof Duration d) {
                    return Optional.ofNullable(switch (style) {
                        case "short" -> DurationPeriodFormatUtils.formatDurationShort(d);
                        case "long" -> DurationPeriodFormatUtils.formatDurationLong(d);
                        case "iso" -> d.toString();
                        default -> {
                            ctx.addWarning(ILLEGAL_DURATION_FORMAT_WARNING);
                            yield null;
                        }
                    });
                } else if (temporalAmount instanceof Period p) {
                    return Optional.ofNullable(switch (style) {
                        case "short" -> DurationPeriodFormatUtils.formatPeriodShort(p);
                        case "long" -> DurationPeriodFormatUtils.formatPeriodLong(p);
                        case "iso" -> p.toString();
                        default -> {
                            ctx.addWarning(ILLEGAL_DURATION_FORMAT_WARNING);
                            yield null;
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Unsupported temporal amount type: " + temporalAmount);
                }
            };

            return StringComputer.of( //
                ctx -> valueSupplier.apply(ctx).get(), //
                ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
            );
        };
    }

    public static final ExpressionFunction FORMAT_TIME_DURATION = functionBuilder() //
        .name("format_time_duration") //
        .description("""
                Formats a `TIME_DURATION` to a string.

                The function supports three formats: `short`, `long`, and `iso`. The `short` and `long` formats \
                are used for formatting time durations in a human-readable format, such as `1h` or `1 hour`. The \
                `iso` format is used for formatting time durations in the ISO-8601 format, such as `PT1H`. If the \
                format is not specified, `iso` is used by default.

                If the provided format is not one of the supported formats, the function returns `MISSING` and a \
                warning is emitted. The function also returns `MISSING` if the duration is missing.
                """) //
        .examples("""
                * `format_time_duration(parseduration("PT1H"), "short")` returns `1h`
                * `format_time_duration(parseduration("PT1H"), "long")` returns `1 hour`
                * `format_time_duration(parseduration("PT1H"), "iso")` returns `PT1H`
                * `format_time_duration(parseduration("PT1H"), "invalid format")` returns `MISSING`
                """) //
        .keywords("format", "interval", "duration") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The duration to format as a string.", isTimeDurationOrOpt()), //
            arg("format", "The format to use for formatting the interval.", isString()) //
        ) //
        .returnType("A string representing the provided duration", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalFunctions.formatIntervalImpl("duration")) //
        .build();

    public static final ExpressionFunction FORMAT_DATE_DURATION = functionBuilder() //
        .name("format_date_duration") //
        .description("""
                Formats a `DATE_DURATION` to a string.

                The function supports three formats: `short`, `long`, and `iso`. The `short` and `long` formats \
                are used for formatting date durations in a human-readable format, such as `1y` or `1 year`. The \
                `iso` format is used for formatting date durations in the ISO-8601 format, such as `P1Y`. If the \
                format is not specified, `iso` is used by default.

                If the provided format is not one of the supported formats, the function returns `MISSING` and a \
                warning is emitted. The function also returns `MISSING` if the duration is missing.
                """) //
        .examples("""
                * `format_date_duration(parse_date_duration("P1Y"), "short")` returns `1y`
                * `format_date_duration(parse_date_duration("P1Y"), "long")` returns `1 year`
                * `format_date_duration(parse_date_duration("P1Y"), "iso")` returns `P1Y`
                * `format_date_duration(parse_date_duration("P1Y"), "invalid format")` returns `MISSING`
                """) //
        .keywords("format", "interval", "duration", "period") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The duration to format as a string.", isDateDurationOrOpt()), //
            arg("format", "The format to use for formatting the interval.", isString()) //
        ) //
        .returnType("A string representing the provided duration", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalFunctions.formatIntervalImpl("duration")) //
        .build();

    public static final ExpressionFunction MAKE_DATE = functionBuilder() //
        .name("make_date") //
        .description("""
                Creates a `LOCAL_DATE` from the provided year, month, and day values.

                If the provided year, month, or day is missing, the function returns `MISSING`. If the provided \
                year, month, or day is invalid (e.g. the month is greater than 12), the function returns `MISSING` \
                and a warning is emitted.
                """) //
        .examples("""
                * `makedate(1970, 1, 1)` returns `1970-01-01`
                * `makedate(1970, 1, 32)` returns `MISSING`
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
        .impl(TemporalFunctions::makeDateImpl) //
        .build();

    private static Computer makeDateImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<LocalDate>> valueSupplier = ctx -> {
            var year = (int)((IntegerComputer)args.get("year")).compute(ctx);
            var month = (int)((IntegerComputer)args.get("month")).compute(ctx);
            var day = (int)((IntegerComputer)args.get("day")).compute(ctx);

            try {
                return Optional.of(LocalDate.of(year, month, day));
            } catch (DateTimeException e) {
                ctx.addWarning("Invalid date values");
                return Optional.empty();
            }
        };

        return LocalDateComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction MAKE_TIME = functionBuilder() //
        .name("make_time") //
        .description("""
                Creates a `LOCAL_TIME` from the provided hour, minute, second, and nanosecond values.

                If any of the provided values are missing, the function returns `MISSING`. If the provided hour, \
                minute, second, or nanosecond is invalid (e.g. the hour is greater than 23), the function returns \
                `MISSING` and a warning is emitted.

                Note that the seconds and nanoseconds are optional. If not provided, they default to 0.
                """) //
        .examples("""
                * `maketime(0, 0, 0, 0)` returns `00:00:00`
                * `maketime(24, 0, 0, 0)` returns `MISSING`
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
        .impl(TemporalFunctions::makeTimeImpl) //
        .build();

    private static Computer makeTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<LocalTime>> valueSupplier = ctx -> {
            var hour = (int)((IntegerComputer)args.get("hour")).compute(ctx);
            var minute = (int)((IntegerComputer)args.get("minute")).compute(ctx);
            var second = (int)((IntegerComputer)args.get("second", IntegerComputer.ofConstant(0))).compute(ctx);
            var nanosecond = (int)((IntegerComputer)args.get("nanosecond", IntegerComputer.ofConstant(0))).compute(ctx);

            try {
                return Optional.of(LocalTime.of(hour, minute, second, nanosecond));
            } catch (DateTimeException e) {
                ctx.addWarning("Invalid time values");
                return Optional.empty();
            }
        };

        return LocalTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

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
        .impl(TemporalFunctions::makeDateTimeImpl) //
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

    public static final ExpressionFunction MAKE_ZONED = functionBuilder() //
        .name("make_zoned") //
        .description("""
                Creates a `ZONED_DATE_TIME` from the provided date, time, and zone values.

                If the provided date, time, or zone is missing, the function returns `MISSING`. If the provided \
                zone is invalid, the function returns `MISSING` and a warning is emitted.
                """) //
        .examples("""
                * `make_zoned(make_date(1970, 1, 1), make_time(0, 0), 'UTC')` returns `1970-01-01T00:00:00Z`
                * `make_zoned(make_date(1970, 1, 1), make_time(0, 0), 'Europe/Berlin')` \
                  returns `1970-01-01T00:00:00+01:00[Europe/Berlin]`
                * `make_zoned(make_date(1970, 1, 1), make_time(0, 0), 'Invalid/Zone')` returns `MISSING`
                """) //
        .keywords("make", "datetime", "zoned_date_time", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("datetime", "The date to use for the zoned date time.", isLocalDateTimeOrOpt()), //
            arg("zone", "The zone to use for the zoned date time.", isStringOrOpt()) //
        ) //
        .returnType("A `ZONED_DATE_TIME` representing the provided date, time, and zone values",
            RETURN_ZONED_DATE_TIME_MISSING, args -> OPT_ZONED_DATE_TIME) //
        .impl(TemporalFunctions::makeZonedDateTimeImpl) //
        .build();

    private static Computer makeZonedDateTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<ZonedDateTime>> valueSupplier = ctx -> {
            var datetime = ((LocalDateTimeComputer)args.get("datetime")).compute(ctx);
            var zoneIdString = ((StringComputer)args.get("zone")).compute(ctx);

            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(zoneIdString);
            } catch (DateTimeException ex) {
                ctx.addWarning("Invalid zone id '%s'".formatted(zoneIdString));
                return Optional.empty();
            }

            return Optional.of(ZonedDateTime.of(datetime, zoneId));
        };

        return ZonedDateTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction MAKE_TIME_DURATION = functionBuilder() //
        .name("make_time_duration") //
        .description("""
                Creates a `TIME_DURATION` from the provided hours, minutes, seconds, and nanoseconds.

                The seconds and nanoseconds are optional, and default to 0 if not provided. However, if any
                of the provided values are missing, the function returns `MISSING`. If the provided values would
                result in a duration that cannot be represented (i.e. if it would total to more than `MAX_INTEGER`
                or less than `MIN_INTEGER` seconds), the function returns `MISSING` and a warning is emitted.
                """) //
        .examples("""
                * `makeduration(1, 2, 3, 4)` returns `PT1H2M3.000000004S`
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
            RETURN_TIME_DURATION_MISSING, args -> OPT_TIME_DURATION) //
        .impl(TemporalFunctions::makeDurationImpl) //
        .build();

    private static Computer makeDurationImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<Duration>> valueSupplier = ctx -> {
            var hours = ((IntegerComputer)args.get("hours")).compute(ctx);
            var minutes = ((IntegerComputer)args.get("minutes")).compute(ctx);
            var seconds = ((IntegerComputer)args.get("seconds", IntegerComputer.ofConstant(0))).compute(ctx);
            var nanoseconds = ((IntegerComputer)args.get("nanoseconds", IntegerComputer.ofConstant(0))).compute(ctx);

            try {
                return Optional.of(Duration.ofHours(hours) //
                    .plusMinutes(minutes) //
                    .plusSeconds(seconds) //
                    .plusNanos(nanoseconds));
            } catch (ArithmeticException e) {
                ctx.addWarning("Duration overflow: the values provided are either too large or too small");
                return Optional.empty();
            }
        };

        return TimeDurationComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction MAKE_DATE_DURATION = functionBuilder() //
        .name("make_date_duration") //
        .description("""
                Creates a `DATE_DURATION` from the provided year, month, and day values.

                If any of the provided values are missing, the function returns `MISSING`. If any of the provided \
                values is greater than `2^31 - 1` or less than `-2^31`, the function returns `MISSING` and a warning \
                is emitted.
                """) //
        .examples("""
                * `makeperiod(1, 2, 3)` returns `P1Y2M3D`
                * `makeperiod(1, 2, MAX_INTEGER)` returns `MISSING`
                """) //
        .keywords("make", "period", "interval", "create") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("years", "The years to use for the period.", isIntegerOrOpt()), //
            arg("months", "The months to use for the period.", isIntegerOrOpt()), //
            arg("days", "The days to use for the period.", isIntegerOrOpt()) //
        ) //
        .returnType("A `DATE_DURATION` representing the provided year, month, and day values",
            RETURN_DATE_DURATION_MISSING, args -> OPT_DATE_DURATION) //
        .impl(TemporalFunctions::makePeriodImpl) //
        .build();

    private static Computer makePeriodImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<Period>> valueSupplier = ctx -> {
            try {
                var years = Math.toIntExact(((IntegerComputer)args.get("years")).compute(ctx));
                var months = Math.toIntExact(((IntegerComputer)args.get("months")).compute(ctx));
                var days = Math.toIntExact(((IntegerComputer)args.get("days")).compute(ctx));

                return Optional.of(Period.of(years, months, days));
            } catch (ArithmeticException ex) {
                throw new ExpressionEvaluationException("Period values are too large", ex);
            }
        };

        return DateDurationComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    private static final Function<Arguments<Computer>, Computer> extractTemporalFieldImpl(final TemporalField field) {
        return args -> {
            IntegerComputerResultSupplier value = ctx -> {
                var temporal = ((TemporalAccessorComputer)args.get("temporal")).compute(ctx);

                try {
                    return temporal.getLong(field);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException("Field '%s' is too large to be represented as an INTEGER"
                        .formatted(field.getDisplayName(Locale.ROOT)), ex);
                }
            };

            return IntegerComputer.of( //
                value, //
                anyMissing(args) //
            );
        };
    }

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
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.YEAR)) //
        .build();

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
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.MONTH_OF_YEAR)) //
        .build();

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
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.DAY_OF_MONTH)) //
        .build();

    public static final ExpressionFunction EXTRACT_HOUR = functionBuilder() //
        .name("extract_hour") //
        .description("""
                Extracts the hour of day from a temporal value with time information.

                The hour is represented as an integer between 0 and 23.
                """) //
        .examples("""
                * `extract_hour(parse_time("00:00:00"))` returns `0`
                """) //
        .keywords("extract", "hour") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the hour from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the hour of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.HOUR_OF_DAY)) //
        .build();

    public static final ExpressionFunction EXTRACT_MINUTE = functionBuilder() //
        .name("extract_minute") //
        .description("""
                Extracts the minute of hour from a temporal value with time information.

                The minute is represented as an integer between 0 and 59.
                """) //
        .examples("""
                * `extract_minute(parse_time("00:00:00"))` returns `0`
                """) //
        .keywords("extract", "minute") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the minute from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the minute of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.MINUTE_OF_HOUR)) //
        .build();

    public static final ExpressionFunction EXTRACT_SECOND = functionBuilder() //
        .name("extract_second") //
        .description("""
                Extracts the second of minute from a temporal value with time information.

                The second is represented as an integer between 0 and 59.
                """) //
        .examples("""
                * `extract_second(parse_time("00:00:00"))` returns `0`
                """) //
        .keywords("extract", "second") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the second from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the second of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.SECOND_OF_MINUTE)) //
        .build();

    public static final ExpressionFunction EXTRACT_NANOSECOND = functionBuilder() //
        .name("extract_nanosecond") //
        .description("""
                Extracts the nanosecond of second from a temporal value.

                The nanosecond is represented as an integer between 0 and 999_999_999.
                """) //
        .examples("""
                * `extract_nanosecond(parsetime("00:00:00"))` returns `0`
                """) //
        .keywords("extract", "nanosecond") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The temporal value to extract the nanosecond from.", hasTimeInformationOrOpt()) //
        ) //
        .returnType("An integer representing the nanosecond of the provided temporal value", RETURN_INTEGER_MISSING,
            args -> INTEGER(anyOptional(args))) //
        .impl(TemporalFunctions.extractTemporalFieldImpl(ChronoField.NANO_OF_SECOND)) //
        .build();

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
            arg("temporal", "The temporal value to extract the date from.",
                isOneOfBaseTypes(LOCAL_DATE_TIME, ZONED_DATE_TIME)) //
        ) //
        .returnType("A `LOCAL_DATE` representing the date part of the provided temporal value",
            RETURN_LOCAL_DATE_MISSING, args -> LOCAL_DATE(anyOptional(args))) //
        .impl(TemporalFunctions::extractDateImpl) //
        .build();

    private static Computer extractDateImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<LocalDate> valueSupplier = ctx -> {
            var temporal = ((TemporalAccessorComputer)args.get("temporal")).compute(ctx);
            return LocalDate.from(temporal);
        };

        return LocalDateComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

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
            arg("temporal", "The temporal value to extract the time from.",
                isOneOfBaseTypes(LOCAL_DATE_TIME, ZONED_DATE_TIME)) //
        ) //
        .returnType("A `LOCAL_TIME` representing the time part of the provided temporal value",
            RETURN_LOCAL_TIME_MISSING, args -> LOCAL_TIME(anyOptional(args))) //
        .impl(TemporalFunctions::extractTimeImpl) //
        .build();

    private static Computer extractTimeImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<LocalTime> valueSupplier = ctx -> {
            var temporal = ((TemporalAccessorComputer)args.get("temporal")).compute(ctx);
            return LocalTime.from(temporal);
        };

        return LocalTimeComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction EXTRACT_DATETIME = functionBuilder() //
        .name("extract_datetime") //
        .description("""
                Extracts the date and time part of a `ZONED_DATE_TIME` value.

                If the provided temporal value is missing, the function returns `MISSING`.
                """) //
        .examples("""
                * `extract_datetime(parse_zoned("1970-01-01T00:00:00Z"))` returns `1970-01-01T00:00:00`
                """) //
        .keywords("extract", "datetime") //
        .category(CATEGORY_CREATE_EXTRACT) //
        .args( //
            arg("temporal", "The `ZONED_DATE_TIME` value to extract the date and time from.", isZonedDateTimeOrOpt()) //
        ) //
        .returnType("A `LOCAL_DATE_TIME` representing the date and time part of the provided temporal value",
            RETURN_LOCAL_DATE_TIME_MISSING, args -> LOCAL_DATE_TIME(anyOptional(args))) //
        .impl(TemporalFunctions::extractDateTimeImpl) //
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
        .impl(TemporalFunctions.convertDurationImpl(TimeBasedGranularityUnit.HOURS)) //
        .build();

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
        .impl(TemporalFunctions.convertDurationImpl(TimeBasedGranularityUnit.MINUTES)) //
        .build();

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
        .impl(TemporalFunctions.convertDurationImpl(TimeBasedGranularityUnit.SECONDS)) //
        .build();

    private static boolean isShortormDuration(final String durationString) {
        return !durationString.isBlank() && Pattern.compile("(\\d+\\s*h)?\\s*(\\d+\\s*m)?\\s*(\\d(.\\d+)?\\s*s)?") //
            .matcher(durationString) //
            .matches();
    }

    private static boolean isLongFormDuration(final String durationString) {
        return !durationString.isBlank()
            && Pattern.compile("(\\d+\\s*hours?)?\\s*(\\d+\\s*minutes?)?\\s*(\\d(.\\d+)?\\s*seconds?)?") //
                .matcher(durationString) //
                .matches();
    }

    private static boolean isIsoFormDuration(final String durationString) {
        try {
            Duration.parse(durationString);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean isLongFormPeriod(final String periodString) {
        return !periodString.isBlank() && Pattern.compile("(\\d+\\s*years?)?\\s*(\\d+\\s*months?)?\\s*(\\d+\\s*days?)?") //
            .matcher(periodString) //
            .matches();
    }

    private static boolean isShortFormPeriod(final String periodString) {
        return !periodString.isBlank() && Pattern.compile("(\\d+\\s*y)?\\s*(\\d+\\s*M)?\\s*(\\d+\\s*d)?") //
            .matcher(periodString) //
            .matches();
    }

    private static boolean isIsoFormPeriod(final String periodString) {
        try {
            Period.parse(periodString);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
