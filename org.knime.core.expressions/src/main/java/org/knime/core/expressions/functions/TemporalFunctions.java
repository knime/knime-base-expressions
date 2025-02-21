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
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_STRING_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_TIME_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_ZONED_DATE_TIME_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.isDateDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isString;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.isTimeDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isZonedDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
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
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.TemporalAccessorComputer;
import org.knime.core.expressions.Computer.TemporalAmountComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.OperatorCategory;
import org.knime.time.util.DurationPeriodFormatUtils;

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
    private static final <T extends TemporalAccessor> Function<Arguments<Computer>, Computer> parseTemporalAccessorImpl(
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
                * `parsedate("1970-01-01")` returns `1970-01-01`
                * `parsedate("01/01/1970", "dd/MM/yyyy")` returns `1970-01-01`
                * `parsedate("invalid date")` returns `MISSING`
                * `parsedate("01/01/1970", "invalid format")` returns `MISSING`
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
                * `parsetime("12:34")` returns `12:34:00`
                * `parsetime("12:34:56")` returns `12:34:56`
                * `parsetime("12:34:56.789")` returns `12:34:56.789`
                * `parsetime("00.00.00", "HH.mm.ss")` returns `00:00:00`
                * `parsetime("00:00:00", "invalid pattern")` returns `MISSING`
                * `parsetime("invalid time")` returns `MISSING`
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
                * `parsedatetime("1970-01-01T00:00:00")` returns `1970-01-01T00:00:00`
                * `parsedatetime("1970-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")` returns `1970-01-01T00:00:00`
                * `parsedatetime("1970-01-01T00:00:00", "invalid format")` returns `MISSING`
                * `parsedatetime("invalid datetime")` returns `MISSING`
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
                * `parsezoneddatetime("1970-01-01T00:00:00Z")` returns `1970-01-01T00:00:00Z`
                * `parsezoneddatetime("1970-01-01 00:00:00Z", "yyyy-MM-dd HH:mm:ssX")` returns `1970-01-01T00:00:00Z`
                * `parsezoneddatetime("1970-01-01T00:00:00+01:00")` returns `1970-01-01T00:00:00+01:00`
                * `parsezoneddatetime("1970-01-01T00:00:00+01:00[Europe/Paris]")` returns \
                `1970-01-01T00:00:00+01:00[Europe/Paris]`
                * `parsezoneddatetime("1970-01-01T00:00:00Z", "invalid format")` returns `MISSING`
                * `parsezoneddatetime("invalid zoned datetime")` returns `MISSING`
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
                * `format_local_date(parsedate("1970-01-01"), "dd/MM/yyyy")` returns `01/01/1970`
                * `format_local_date(parsedate("1970-01-01"), "invalid format")` returns `MISSING`
                * `format_local_date(parsedate("invalid date"), "dd/MM/yyyy")` returns `MISSING`
                * `format_local_date(parsedate("1970-01-01"), "HH:mm:ss")` returns `MISSING`
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
