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
import static org.knime.core.expressions.SignatureUtils.hasDateAndTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isString;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.STRING;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQuery;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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
import org.knime.core.expressions.ExpressionEvaluationException;
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
     * Checks if the provided string is a valid long-form duration string. If it isn't, throws an exception.
     *
     * @param format
     * @throws ExpressionEvaluationException
     */
    private static DateTimeFormatter checkDateTimeFormat(final String format) throws ExpressionEvaluationException {
        try {
            return DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException("Unparseable temporal format '%s'".formatted(format));
        }
    }

    /**
     * Checks if the provided format can be used to parse the type specified by the query. An exception will be thrown
     * if:
     * <ol>
     * <li>The formatter is overspecified for the query, e.g. if the query is LocalDate::from and the formatter includes
     * time information.</li>
     * <li>The formatter is underspecified for the query, e.g. if the query is LocalDate::from and the formatter doesn't
     * include a date-of-month field.</li>
     * <li>The format is total nonsense that can't be compiled.</li>
     * </ol>
     * If none of that happens, then a formatter is returned, and you can be sure that any exceptions from this point
     * onwards are problems with the data and not the parser.
     *
     * See also {@link #checkDateTimeFormatterAppliesToQuery(String, Temporal, TemporalQuery, String)}, which differs in
     * that it errors for an overspecifed format rather than an underspecified one.
     *
     * @param <T>
     * @param format
     * @param testData
     * @param query
     * @param targetType
     * @return
     * @throws ExpressionEvaluationException
     */
    private static <T extends TemporalAccessor> DateTimeFormatter checkIfFormatIsUnderSpecified(final String format,
        final T testData, final TemporalQuery<T> query, final String targetType) throws ExpressionEvaluationException {

        var formatter = checkDateTimeFormat(format);

        // step 1: create some formatted data that we can parse. Since we don't care about overspecified formats, we can use a
        // ZonedDateTime as our test data. This should never throw.
        var formatted = formatter.format(ZonedDateTime.now());

        // step 2: try to parse the thing we just formatted
        try {
            formatter.parse(formatted, query);
        } catch (DateTimeParseException e) {
            // might mean that for example the type is a LocalDate, but the formatter lacks a required field.
            // In other words, the formatter is underspecified for the query.
            throw new ExpressionEvaluationException("Format is underspecified for a %s".formatted(targetType));
        }

        return formatter;
    }

    /**
     * Checks if the provided format can be used to format the type specified by the query. An exception will be thrown
     * if:
     * <ol>
     * <li>The formatter is overspecified for the query, e.g. if the query is LocalDate::from and the formatter includes
     * time information.</li>
     * <li>The format is total nonsense that can't be compiled.</li>
     * </ol>
     * If none of that happens, then a formatter is returned, and you can be sure that any exceptions from this point
     * onwards are problems with the data and not the formatter.
     *
     * See also {@link #checkDateTimeParserAppliesToQuery(String, Temporal, TemporalQuery, String)}, which differs in
     * that it will only error on an underspecified rather than overspecified format.
     *
     * @param <T>
     * @param format
     * @param testData
     * @param query
     * @param targetType
     * @return
     * @throws ExpressionEvaluationException
     */
    private static <T extends TemporalAccessor> DateTimeFormatter checkIfFormatIsOverspecified(final String format,
        final T testData, final TemporalQuery<T> query, final String targetType) throws ExpressionEvaluationException {
        var formatter = checkDateTimeFormat(format);

        // step 1: try to format
        try {
            formatter.format(testData);
        } catch (DateTimeException e) {
            // might mean that for example the type is a LocalDate, but the formatter has some time fields.
            // In other words, the formatter is overspecified for the query, which means formatting doesn't
            // make sense.
            throw new ExpressionEvaluationException("Format is overspecified for a %s".formatted(targetType));
        }

        return formatter;
    }

    public static final ExpressionFunction PARSE_DATE = functionBuilder() //
        .name("parse_date") //
        .description("""
                Parses a string into a `LOCAL_DATE` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                (`yyyy-MM-dd`) is used.

                If the format string is invalid, or the provided date string does not match the provided \
                format, the function returns `MISSING` and a warning is emitted. The function also returns \
                `MISSING` if the date string is missing.

                Note that it is possible to use an overspecified format here. For example, one may parse to a \
                `LOCAL_DATE` using a format like `yyyy-MM-dd HH:mm:ss` which includes time information, which is \
                not used by a `LOCAL_DATE`. The extra information is simply ignored and discarded. However, an \
                underspecified format (e.g. `yyyy`) will cause an error.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_date("1970-01-01")` returns `1970-01-01`
                * `parse_date("01/01/1970", "dd/MM/yyyy")` returns `1970-01-01`
                * `parse_date("invalid date")` returns `MISSING`
                * `parse_date("01/01/1970", "invalid format")` causes an error (invalid format)
                * `parse_date("1970", "yyyy")` causes an error (underspecified format)
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
        .impl(TemporalFunctions::parseDateImpl) //
        .build();

    private static Computer parseDateImpl(final Arguments<Computer> args) {
        var formatComputer = (StringComputer)args.get("format", StringComputer.ofConstant(null));
        var dateComputer = (StringComputer)args.get("date");

        ComputerResultSupplier<Optional<LocalDate>> valueSupplier = ctx -> {
            // first we need to check the format
            var format = formatComputer.compute(ctx);

            var formatter = format == null //
                ? DEFAULT_DATE_FORMAT //
                : checkIfFormatIsUnderSpecified(format, LocalDate.now(), LocalDate::from, "LOCAL_DATE");

            var dateString = dateComputer.compute(ctx);

            try {
                return Optional.of(formatter.parse(dateString, LocalDate::from));
            } catch (DateTimeParseException e) {
                ctx.addWarning("Date string '%s' did not match format".formatted(dateString));
                return Optional.empty();
            }
        };

        return LocalDateComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction PARSE_TIME = functionBuilder() //
        .name("parse_time") //
        .description("""
                Parses a string into a `LOCAL_TIME` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                is used, in which the hours and minutes are required but but seconds and subseconds are \
                optional. See the examples for more information.

                If the format string is invalid, or the provided time string does not match the provided \
                format, the function returns `MISSING` and a warning is emitted. The function also returns \
                `MISSING` if the time string is missing.

                Note that it is possible to use an overspecified format here. For example, one may parse to a \
                `LOCAL_TIME` using a format like `yyyy-MM-dd HH:mm:ss` which includes date information, which is \
                not used by a `LOCAL_TIME`. The extra information is simply ignored and discarded. However, an \
                underspecified format (e.g. `mm`) will cause an error.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_time("12:34")` returns `12:34:00`
                * `parse_time("12:34:56")` returns `12:34:56`
                * `parse_time("12:34:56.789")` returns `12:34:56.789`
                * `parse_time("00.00.00", "HH.mm.ss")` returns `00:00:00`
                * `parse_time("00:00:00", "invalid pattern")` causes an error (invalid format)
                * `parse_time("00", "HH")` causes an error (underspecified format)
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
        .impl(TemporalFunctions::parseTimeImpl) //
        .build();

    private static Computer parseTimeImpl(final Arguments<Computer> args) {
        var formatComputer = (StringComputer)args.get("format", StringComputer.ofConstant(null));
        var timeComputer = (StringComputer)args.get("time");

        ComputerResultSupplier<Optional<LocalTime>> valueSupplier = ctx -> {
            // first we need to check the format
            var format = formatComputer.compute(ctx);

            var formatter = format == null //
                ? DEFAULT_TIME_FORMAT //
                : checkIfFormatIsUnderSpecified(format, LocalTime.now(), LocalTime::from, "LOCAL_TIME");

            var timeString = timeComputer.compute(ctx);

            try {
                return Optional.of(formatter.parse(timeString, LocalTime::from));
            } catch (DateTimeParseException e) {
                ctx.addWarning("Time string '%s' did not match format".formatted(timeString));
                return Optional.empty();
            }
        };

        return LocalTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction PARSE_DATE_TIME = functionBuilder() //
        .name("parse_datetime") //
        .description("""
                Parses a string into a `LOCAL_DATE_TIME` using the provided format. See [here](%s) for more \
                information about the format patterns. If the format is not specified, the default format \
                is used, which is the default format for `parse_date` and the default format for `parse_time` \
                concatenated together, separated by a T. See the examples for more information.

                If the format string is invalid, or the provided date time string does not match the provided \
                format, the function returns `MISSING` and a warning is emitted. The function also returns \
                `MISSING` if the date time string is missing.

                Note that it is possible to use an overspecified format here. For example, one may parse to a \
                `LOCAL_DATE_TIME` using a format like `yyyy-MM-dd HH:mm:ss VV` which includes a time zone ID, \
                which is not required for a `LOCAL_DATE_TIME`. The extra information is simply ignored and \
                discarded. However, an underspecified format (e.g. `yyyy`) will cause an error.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_datetime("1970-01-01T00:00:00")` returns `1970-01-01T00:00:00`
                * `parse_datetime("1970-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")` returns `1970-01-01T00:00:00`
                * `parse_datetime("1970-01-01T00:00:00", "invalid format")` causes an error (invalid format)
                * `parse_datetime("1970", "yyyy")` causes an error (underspecified format)
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
        .impl(TemporalFunctions::parseDateTimeImpl) //
        .build();

    private static Computer parseDateTimeImpl(final Arguments<Computer> args) {
        var formatComputer = (StringComputer)args.get("format", StringComputer.ofConstant(null));
        var dateTimeComputer = (StringComputer)args.get("datetime");

        ComputerResultSupplier<Optional<LocalDateTime>> valueSupplier = ctx -> {
            // first we need to check the format
            var format = formatComputer.compute(ctx);

            var formatter = format == null //
                ? DEFAULT_DATE_TIME_FORMAT //
                : checkIfFormatIsUnderSpecified(format, LocalDateTime.now(), LocalDateTime::from, "LOCAL_DATE_TIME");

            var dateTimeString = dateTimeComputer.compute(ctx);

            try {
                return Optional.of(formatter.parse(dateTimeString, LocalDateTime::from));
            } catch (DateTimeParseException e) {
                ctx.addWarning("Date time string '%s' did not match format".formatted(dateTimeString));
                return Optional.empty();
            }
        };

        return LocalDateTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

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

                Note that an underspecified format (e.g. `yyyy`) will cause an error.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `parse_zoned_datetime("1970-01-01T00:00:00Z")` returns `1970-01-01T00:00:00Z`
                * `parse_zoned_datetime("1970-01-01 00:00:00Z", "yyyy-MM-dd HH:mm:ssX")` returns `1970-01-01T00:00:00Z`
                * `parse_zoned_datetime("1970-01-01T00:00:00+01:00")` returns `1970-01-01T00:00:00+01:00`
                * `parse_zoned_datetime("1970-01-01T00:00:00+01:00[Europe/Paris]")` returns \
                `1970-01-01T00:00:00+01:00[Europe/Paris]`
                * `parse_zoned_datetime("1970-01-01T00:00:00Z", "invalid format")` causes an error (invalid format)
                * `parse_zoned_datetime("1970", "yyyy")` causes an error (underspecified format)
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
        .impl(TemporalFunctions::parseZonedDateTime) //
        .build();

    private static Computer parseZonedDateTime(final Arguments<Computer> args) {
        var formatComputer = (StringComputer)args.get("format", StringComputer.ofConstant(null));
        var zonedDateTimeComputer = (StringComputer)args.get("datetime");

        ComputerResultSupplier<Optional<ZonedDateTime>> valueSupplier = ctx -> {
            // first we need to check the format
            var format = formatComputer.compute(ctx);

            var formatter = format == null //
                ? DEFAULT_ZONED_DATE_TIME_FORMAT //
                : checkIfFormatIsUnderSpecified(format, ZonedDateTime.now(), ZonedDateTime::from, "ZONED_DATE_TIME");

            var zonedDateTimeString = zonedDateTimeComputer.compute(ctx);

            try {
                return Optional.of(formatter.parse(zonedDateTimeString, ZonedDateTime::from));
            } catch (DateTimeParseException e) {
                ctx.addWarning("Zoned date time string '%s' did not match format".formatted(zonedDateTimeString));
                return Optional.empty();
            }
        };

        return ZonedDateTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction PARSE_TIME_DURATION = functionBuilder() //
        .name("parse_time_duration") //
        .description("""
                Parses a string into a `TIME_DURATION`.

                The function supports three formats: `short`, `long`, and `iso`. The `short` and `long` formats \
                are used for parsing time durations in a human-readable format, such as `1h` or `1 hour`. The \
                `iso` format is used for parsing time durations in the ISO-8601 format, such as `PT1H`. \

                If the provided format is not one of the supported formats, the function emits an error. \
                The function returns `MISSING` if the duration string is missing or  oes not match the \
                provided format. In the latter case a warning is emitted.
                """) //
        .examples("""
                * `parse_time_duration("PT1H")` returns `PT1H`
                * `parse_time_duration("1h 2m 3.5s", "short")` returns `PT1H2M3.5S`
                * `parse_time_duration("1 hour 2 minutes 3.5 seconds", "long")` returns `PT1H2M3.5S`
                * `parse_time_duration("invalid duration")` returns `MISSING`
                * `parse_time_duration("PT1H", "invalid format")` causes an error (invalid format)
                """) //
        .keywords("parse", "duration", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The string to parse into a time duration.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the duration: either 'long', 'short', or 'iso'.",
                isString()) //
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
                throw new ExpressionEvaluationException(ILLEGAL_DURATION_FORMAT_ERROR.formatted(style));
            }

            var durationString = ((StringComputer)args.get("duration")).compute(ctx).trim();

            if (!durationCheckersByType.get(style).test(durationString)) {
                ctx.addWarning("TIME_DURATION string '%s' did not match format '%s'".formatted(durationString, style));
                return Optional.empty();
            }

            try {
                return Optional.of(DurationPeriodFormatUtils.parseDuration(durationString));
            } catch (DateTimeParseException ex) {
                ctx.addWarning("Invalid TIME_DURATION string '%s'".formatted(durationString));
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
                `iso` format is used for parsing date durations in the ISO-8601 format, such as `P1Y`. \

                If the provided format is not one of the supported formats, the function emits an error. \
                The function returns `MISSING` if the duration string is missing or does not match the \
                provided format. In the latter case a warning is emitted.
                """) //
        .examples("""
                * `parse_date_duration("P1Y")` returns `P1Y`
                * `parse_date_duration("1y", "short")` returns `P1Y`
                * `parse_date_duration("1 year", "long")` returns `P1Y`
                * `parse_date_duration("1 year")` returns `MISSING`
                * `parse_date_duration("P1Y", "invalid format")` causes an error (invalid format)
                """) //
        .keywords("parse", "period", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The string to parse into a date duration.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the duration: either 'long', 'short', or 'iso'.",
                isString()) //
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
                throw new ExpressionEvaluationException(ILLEGAL_DURATION_FORMAT_ERROR.formatted(style));
            }

            var periodString = ((StringComputer)args.get("duration")).compute(ctx).trim();

            if (!periodCheckersByType.get(style).test(periodString)) {
                ctx.addWarning("DATE_DURATION string '%s' did not match style '%s'".formatted(periodString, style));
                return Optional.empty();
            }

            try {
                return Optional.of(DurationPeriodFormatUtils.parsePeriod(periodString));
            } catch (DateTimeParseException ex) {
                ctx.addWarning("Invalid DATE_DURATION string '%s'".formatted(periodString));
                return Optional.empty();
            }
        };

        return DateDurationComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    private static Function<Arguments<Computer>, Computer> formatTemporalImpl(final String temporalArgument,
        final TemporalQuery<TemporalAccessor> query) {
        return args -> {
            var formatComputer = (StringComputer)args.get("format");
            var temporalComputer = (TemporalAccessorComputer)args.get(temporalArgument);

            ComputerResultSupplier<Optional<String>> valueSupplier = ctx -> {
                var formatString = formatComputer.compute(ctx);
                var temporal = temporalComputer.compute(ctx);

                var typeName = Computer.getReturnTypeFromComputer(temporalComputer).name();

                DateTimeFormatter formatter = checkIfFormatIsOverspecified(formatString, temporal, query, typeName);

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

    public static final ExpressionFunction FORMAT_DATE = functionBuilder() //
        .name("format_local_date") //
        .description("""
                Formats a `LOCAL_DATE` to a string using the provided format. See [here](%s) for more \
                information about the format patterns.

                If the format string is invalid, an error is emitted. The function also returns `MISSING` \
                and emits a warning if the provided date cannot be formatted using the provided format. \
                If any input is missing, the function returns `MISSING`.

                Note that it is not required that the format include all fields of the date. For example, \
                one may format a date using the format string "yyyy" to get just the years. However, the \
                format must not include fields that are not present in the provided date, such as formatting \
                a `LOCAL_DATE` with a format that includes time information.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_local_date(parse_date("1970-01-01"), "dd/MM/yyyy")` returns `01/01/1970`
                * `format_local_date(parse_date("invalid date"), "dd/MM/yyyy")` returns `MISSING` and emits a warning
                * `format_local_date(parse_date("1970-01-01"), "invalid format")` causes an error (invalid format)
                * `format_local_date(parse_date("1970-01-01"), "yyyy-MM-dd HH")` causes an error (overspecified format)
                """) //
        .keywords("format", "date", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("date", "The date to format as a string.", isLocalDateOrOpt()), //
            arg("format", "The format to use for formatting the temporal value.", isString()) //
        ) //
        .returnType("A string representing the provided `LOCAL_DATE`", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalFunctions.formatTemporalImpl("date", LocalDate::from)) //
        .build();

    public static final ExpressionFunction FORMAT_TIME = functionBuilder() //
        .name("format_local_time") //
        .description("""
                Formats a `LOCAL_TIME` to a string using the provided format. See [here](%s) for more \
                information about the format patterns.

                If the format string is invalid, an error is emitted. The function also returns `MISSING` \
                and emits a warning if the provided time cannot be formatted using the provided format. \
                If any input is missing, the function returns `MISSING`.

                Note that it is not required that the format include all fields of the time. For example, \
                one may format a time using the format string "HH" to get just the hours. However, the \
                format must not include fields that are not present in the provided time, such as formatting \
                a `LOCAL_TIME` with a format that includes date information.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_temporal(parse_time("12:34:56"), "HH:mm")` returns `12:34`
                * `format_temporal(parse_time("invalid time"), "HH:mm:ss")` returns `MISSING` and emits a warning
                * `format_temporal(parse_time("12:34"), "invalid format")` causes an error (invalid format)
                * `format_temporal(parse_time("12:34"), "HH:mm dd/MM/yyyy")` causes an error (overspecified format)
                """) //
        .keywords("format", "time", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("time", "The time value to format as a string.", isLocalTimeOrOpt()), //
            arg("format", "The format to use.", isString()) //
        ) //
        .returnType("A string representing the provided `LOCAL_TIME`", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalFunctions.formatTemporalImpl("time", LocalTime::from)) //
        .build();

    public static final ExpressionFunction FORMAT_DATE_TIME = functionBuilder() //
        .name("format_date_time") //
        .description("""
                Formats a `LOCAL_DATE_TIME` or `ZONED_DATE_TIME` to a string using the provided format. \
                See [here](%s) for more information about the format patterns.

                If the format string is invalid, an error is emitted. The function also returns `MISSING` \
                and emits a warning if the provided date time cannot be formatted using the provided format. \
                If any input is missing, the function returns `MISSING`.

                Note that it is not required that the format include all fields of the date-time. For example, \
                one may format a date-time using the format string "yyyy" to get just the years. However, the \
                format must not include fields that are not present in the provided date-time, such as formatting \
                a `LOCAL_DATE_TIME` with a format that includes time zone information.
                """.formatted(JAVADOC_URL_DATE_FORMAT)) //
        .examples("""
                * `format_date_time($["my_datetime_col"], "dd/MM/yyyy HH:mm:ss")` returns \
                `01/01/1970 00:00:00`
                * `format_date_time($["my_zoned_col"], "VV")` returns `Europe/Paris`
                * `format_date_time($["my_datetime_col"], "invalid format")` causes an error (invalid format)
                * `format_date_time($["my_datetime_col"], "dd/MM/yyyy HH:mm:ss VV")` causes an error \
                (overspecified format)
                """) //
        .keywords("format", "datetime", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("datetime", "The date time value to format as a string.", hasDateAndTimeInformationOrOpt()), //
            arg("format", "The format to use.", isString()) //
        ) //
        .returnType("A string representing the provided date-time", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalFunctions::formatDateTimeImpl) //
        .build();

    /**
     * Since this function can take both `LOCAL_DATE_TIME` and `ZONED_DATE_TIME` as input, we need to check which type
     * we're dealing with so that we can correctly check the format.
     */
    private static Computer formatDateTimeImpl(final Arguments<Computer> args) {
        var localOrZonedDateTimeComputer = (TemporalAccessorComputer)args.get("datetime");

        if (localOrZonedDateTimeComputer instanceof ZonedDateTimeComputer) {
            return TemporalFunctions.formatTemporalImpl("datetime", ZonedDateTime::from).apply(args);
        } else if (localOrZonedDateTimeComputer instanceof LocalDateTimeComputer) {
            return TemporalFunctions.formatTemporalImpl("datetime", LocalDateTime::from).apply(args);
        } else {
            throw new IllegalArgumentException("Unsupported temporal accessor type: "
                + localOrZonedDateTimeComputer.getClass() + ". This is an implementation error.");
        }
    }

    public static final ExpressionFunction FORMAT_DURATION = functionBuilder() //
        .name("format_duration") //
        .description("""
                Formats a `DATE_DURATION` or `TIME_DURATION` to a string.

                The function supports three formats: `short`, `long`, and `iso`. The `short` and `long` formats \
                are used for formatting date durations in a human-readable format, such as `1y` or `1 year`. The \
                `iso` format is used for formatting date durations in the ISO-8601 format, such as `P1Y`. \

                If the provided format is not one of the supported formats, the function will emit an error. \
                The function also returns `MISSING` if the duration is missing.
                """) //
        .examples("""
                * `format_duration(parse_date_duration("P1Y"), "short")` returns `1y`
                * `format_duration(parse_date_duration("P1Y"), "long")` returns `1 year`
                * `format_duration(parse_date_duration("PT1H"), "iso")` returns `PT1H`
                * `format_duration(parse_time_duration("PT1H"), "invalid format")` causes an error (invalid format)
                """) //
        .keywords("format", "interval", "duration", "period") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The duration to format as a string.", isDurationOrOpt()), //
            arg("format", "The format type to use: either 'short', 'long', or 'iso'.", isString()) //
        ) //
        .returnType("A string representing the provided duration", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalFunctions::formatDurationImpl) //
        .build();

    private static final String ILLEGAL_DURATION_FORMAT_ERROR =
        "Unknown format '%s'. Allowed are: 'iso', 'long', and 'short'";

    private static final Map<String, Function<Duration, String>> DURATION_FORMATTERS = Map.of( //
        "short", DurationPeriodFormatUtils::formatDurationShort, //
        "long", DurationPeriodFormatUtils::formatDurationLong, //
        "iso", Duration::toString //
    );

    private static final Map<String, Function<Period, String>> PERIOD_FORMATTERS = Map.of( //
        "short", DurationPeriodFormatUtils::formatPeriodShort, //
        "long", DurationPeriodFormatUtils::formatPeriodLong, //
        "iso", Period::toString //
    );

    private static Computer formatDurationImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<String> valueSupplier = ctx -> {
            var style = ((StringComputer)args.get("format")).compute(ctx);
            var temporalAmount = ((TemporalAmountComputer)args.get("duration")).compute(ctx);

            Optional<Function<TemporalAmount, String>> formatter;
            if (temporalAmount instanceof Duration) {
                formatter = Optional.ofNullable(DURATION_FORMATTERS.get(style)) //
                    .map(f -> f.compose(Duration.class::cast));
            } else if (temporalAmount instanceof Period) {
                formatter = Optional.ofNullable(PERIOD_FORMATTERS.get(style)) //
                    .map(f -> f.compose(Period.class::cast));
            } else {
                throw new IllegalArgumentException(
                    "Unsupported temporal amount type: " + temporalAmount + ". This is an implementation error.");
            }

            return formatter //
                .map(f -> f.apply(temporalAmount)) //
                .orElseThrow(() -> new ExpressionEvaluationException(ILLEGAL_DURATION_FORMAT_ERROR.formatted(style)));
        };

        return StringComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

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
