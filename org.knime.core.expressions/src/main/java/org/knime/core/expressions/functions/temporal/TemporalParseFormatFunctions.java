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
import static org.knime.core.expressions.ValueType.OPT_STRING;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.DateDurationComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.TemporalAmountComputer;
import org.knime.core.expressions.Computer.TemporalComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.functions.ExpressionFunction;
import org.knime.time.util.DurationPeriodFormatUtils;

/**
 * Implementation of built-in functions that parse and format temporal date such as dates, times, durations, and
 * periods.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
public final class TemporalParseFormatFunctions {

    private TemporalParseFormatFunctions() {
    }

    /** The Temporal - Parsing & Formatting category */
    public static final OperatorCategory CATEGORY_PARSE_FORMAT =
        new OperatorCategory(TemporalFunctionUtils.TEMPORAL_META_CATEGORY_NAME, "Parsing & Formatting", """
                Functions for parsing and formatting temporal data to and from strings, such as dates, times, \
                and date- and time-based durations.
                """);

    private static final String JAVADOC_URL_DATE_FORMAT =
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html";

    private static final String IETF_FORMAT_URL = "https://en.wikipedia.org/wiki/IETF_language_tag";

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // seconds and subseconds optional
    private static final DateTimeFormatter DEFAULT_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    // seconds and subseconds optional
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // zone id optional, but offset required
    private static final DateTimeFormatter DEFAULT_ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    // some constants for argument names etc. to enforce consistency
    private static final String DURATION_ARG = "duration";

    private static final String DATE_ARG = "date";

    private static final String TIME_ARG = "time";

    private static final String DATE_TIME_ARG = "datetime";

    private static final String FORMAT_ARG = "format";

    private static final String LOCALE_ARG = "locale";

    /**
     * Checks if the provided string is a valid date-time format pattern. If it isn't, throws an exception.
     *
     * @param format
     * @throws ExpressionEvaluationException
     */
    private static DateTimeFormatter checkDateTimeFormat(final String format) throws ExpressionEvaluationException {
        try {
            return DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException("Unparseable temporal format '%s'.".formatted(format), e);
        }
    }

    /**
     * Checks if the provided format can be used to parse the type specified by the query. An exception will be thrown
     * if the format is total nonsense that can't be compiled. Otherwise, returns true if the formatter is
     * underspecified, false otherwise.
     *
     * See also {@link #checkDateTimeFormatterAppliesToQuery(String, Temporal, TemporalQuery, String)}, which differs in
     * that it checks for an overspecifed format rather than an underspecified one.
     *
     * @param <T>
     * @param format
     * @param testData
     * @param query
     * @param targetType
     * @return
     * @throws ExpressionEvaluationException
     */
    private static <T extends TemporalAccessor> boolean checkIfFormatIsUnderSpecified(final DateTimeFormatter formatter,
        final TemporalQuery<T> query) {
        // step 1: create some formatted data that we can parse. Since we don't care about overspecified formats,
        // we can use a ZonedDateTime as our test data. This should never throw.
        var formatted = formatter.format(ZonedDateTime.now());

        // step 2: try to parse the thing we just formatted
        try {
            formatter.parse(formatted, query);
            return false;
        } catch (DateTimeParseException e) {
            // might mean that for example the type is a LocalDate, but the formatter lacks a required field.
            // In other words, the formatter is underspecified for the query.
            return true;
        }
    }

    /**
     * Checks if the provided format can be used to format the type specified by the query. An exception will be thrown
     * if the format is total nonsense that can't be compiled. Otherwise, returns true if the formatter is
     * overspecified, false otherwise.
     *
     * See also {@link #checkDateTimeParserAppliesToQuery(String, Temporal, TemporalQuery, String)}, which differs in
     * that it checks for an underspecified rather than overspecified format.
     *
     * @param <T>
     * @param format
     * @param testData
     * @param query
     * @param targetType
     * @return
     */
    private static <T extends TemporalAccessor> boolean checkIfFormatIsOverspecified(final DateTimeFormatter formatter,
        final TemporalQuery<T> query) {
        var testData = query.queryFrom(ZonedDateTime.now());

        // step 1: try to format
        try {
            formatter.format(testData);
            return false;
        } catch (DateTimeException e) {
            // might mean that for example the type is a LocalDate, but the formatter has some time fields.
            // In other words, the formatter is overspecified for the query, which means formatting doesn't
            // make sense.
            return true;
        }
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
                underspecified format (e.g. `yyyy`) will cause the function to return `MISSING` and give a warning.

                An optional locale can be provided in [IETF format](%s) to parse the date in a specific locale. The \
                default locale is `en-US`. This will affect things like month or day names, for example.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `parse_date("1970-01-01")` returns `1970-01-01`
                * `parse_date("01/01/1970", "dd/MM/yyyy")` returns `1970-01-01`
                * `parse_date("invalid date")` returns `MISSING` and a warning
                * `parse_date("1970", "yyyy")` returns `MISSING` and a warning (underspecified format)
                * `parse_date("01/01/1970", "invalid format")` causes an error (invalid format)
                """) //
        .keywords("parse", "date", "local_date") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DATE_ARG, "The string to parse into a date.", isStringOrOpt()), //
            optarg(FORMAT_ARG, "The format to use for parsing the date. If not specified, the default format is used.",
                isString()), //
            optarg(LOCALE_ARG, "The locale to use for parsing the date. If not specified, `en-US` is used.", //
                isString()) //
        ) //
        .returnType("A `LOCAL_DATE` representing the provided string argument", RETURN_LOCAL_DATE_MISSING,
            args -> OPT_LOCAL_DATE) //
        .impl(TemporalParseFormatFunctions::parseDateImpl) //
        .build();

    private static Computer parseDateImpl(final Arguments<Computer> args) {
        var dateComputer = (StringComputer)args.get(DATE_ARG);
        var localeParser = createLocaleParser(args);

        ComputerResultSupplier<Optional<LocalDate>> valueSupplier = ctx -> {
            // first we need to check the format
            Optional<String> formatString = args.has(FORMAT_ARG) //
                ? Optional.of(((StringComputer)args.get(FORMAT_ARG)).compute(ctx)) //
                : Optional.empty();

            var formatter = formatString.isPresent() //
                ? checkDateTimeFormat(formatString.get()) //
                : DEFAULT_DATE_FORMAT;

            var localeParseResult = localeParser.apply(ctx);
            if (localeParseResult.isError()) {
                ctx.addWarning(localeParseResult.getErrorMessage());
                return Optional.empty();
            }
            var locale = localeParseResult.getValue();

            var dateString = dateComputer.compute(ctx);

            try {
                return Optional.of(formatter.withLocale(locale).parse(dateString, LocalDate::from));
            } catch (DateTimeParseException e) {
                if (checkIfFormatIsUnderSpecified(formatter, LocalDate::from)) {
                    var format = formatString.get();

                    // if the format is underspecified, we give a special warning
                    ctx.addWarning("Format '%s' is underspecified for LOCAL_DATE.".formatted(format));
                } else if (formatString.isPresent()) {
                    var format = formatString.get();

                    // failed parse with custom format
                    ctx.addWarning("Date string '%s' did not match format '%s'.".formatted(dateString, format));
                } else {
                    ctx.addWarning("Date string '%s' did not match default format.".formatted(dateString));
                }
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
                underspecified format (e.g. `mm`) will cause the function to return `MISSING` and give a warning.

                An optional locale can be provided in [IETF format](%s) to parse the date in a specific locale. The \
                default locale is `en-US`. This will affect things like month or day names, for example.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `parse_time("12:34")` returns `12:34:00`
                * `parse_time("12:34:56")` returns `12:34:56`
                * `parse_time("12:34:56.789")` returns `12:34:56.789`
                * `parse_time("00.00.00", "HH.mm.ss")` returns `00:00:00`
                * `parse_time("00:00:00", "invalid pattern")` causes an error (invalid format)
                * `parse_time("invalid time")` returns `MISSING` and a warning
                * `parse_time("00", "HH")` returns `MISSING` and a warning (underspecified format)
                """) //
        .keywords("parse", "time", "local_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(TIME_ARG, "The string to parse into a time.", isStringOrOpt()), //
            optarg(FORMAT_ARG, "The format to use for parsing the time. If not specified, the default format is used.",
                isString()), //
            optarg(LOCALE_ARG, "The locale to use for parsing the time. If not specified, `en-US` is used.", //
                isString()) //
        ) //
        .returnType("A `LOCAL_TIME` representing the provided string argument", RETURN_LOCAL_TIME_MISSING,
            args -> OPT_LOCAL_TIME) //
        .impl(TemporalParseFormatFunctions::parseTimeImpl) //
        .build();

    private static Computer parseTimeImpl(final Arguments<Computer> args) {
        var timeComputer = (StringComputer)args.get(TIME_ARG);
        var localeParser = createLocaleParser(args);

        ComputerResultSupplier<Optional<LocalTime>> valueSupplier = ctx -> {
            // first we need to check the format
            Optional<String> formatString = args.has(FORMAT_ARG) //
                ? Optional.of(((StringComputer)args.get(FORMAT_ARG)).compute(ctx)) //
                : Optional.empty();

            var formatter = formatString.isPresent() //
                ? checkDateTimeFormat(formatString.get()) //
                : DEFAULT_TIME_FORMAT;

            var localeParseResult = localeParser.apply(ctx);
            if (localeParseResult.isError()) {
                ctx.addWarning(localeParseResult.getErrorMessage());
                return Optional.empty();
            }
            var locale = localeParseResult.getValue();

            var timeString = timeComputer.compute(ctx);

            try {
                return Optional.of(formatter.withLocale(locale).parse(timeString, LocalTime::from));
            } catch (DateTimeParseException e) {
                if (checkIfFormatIsUnderSpecified(formatter, LocalTime::from)) {
                    var format = formatString.get();

                    // if the format is underspecified, we give a special warning
                    ctx.addWarning("Format '%s' is underspecified for LOCAL_TIME.".formatted(format));
                } else if (formatString.isPresent()) {
                    var format = formatString.get();

                    // failed parse with custom format
                    ctx.addWarning("Time string '%s' did not match format '%s'.".formatted(timeString, format));
                } else {
                    ctx.addWarning("Time string '%s' did not match default format.".formatted(timeString));
                }
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
                discarded. However, an underspecified format (e.g. `yyyy`) will cause the function to return \
                `MISSING` and give a warning.

                An optional locale can be provided in [IETF format](%s) to parse the date in a specific locale. The \
                default locale is `en-US`. This will affect things like month or day names, for example.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `parse_datetime("1970-01-01T00:00:00")` returns `1970-01-01T00:00:00`
                * `parse_datetime("1970-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")` returns `1970-01-01T00:00:00`
                * `parse_datetime("1970-01-01T00:00:00", "invalid format")` causes an error (invalid format)
                * `parse_datetime("invalid datetime")` returns `MISSING` and a warning
                * `parse_datetime("1970", "yyyy")` causes an error (underspecified format)
                """) //
        .keywords("parse", "datetime", "local_date_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DATE_TIME_ARG, "The string to parse into a date time.", isStringOrOpt()), //
            optarg(FORMAT_ARG,
                "The format to use for parsing the date time. If not specified, the default format is used.",
                isString()), //
            optarg(LOCALE_ARG, "The locale to use for parsing the date and time. If not specified, `en-US` is used.",
                isString()) //
        ) //
        .returnType("A `LOCAL_DATE_TIME` representing the provided string argument", RETURN_LOCAL_DATE_TIME_MISSING,
            args -> OPT_LOCAL_DATE_TIME) //
        .impl(TemporalParseFormatFunctions::parseDateTimeImpl) //
        .build();

    private static Computer parseDateTimeImpl(final Arguments<Computer> args) {
        var dateTimeComputer = (StringComputer)args.get(DATE_TIME_ARG);
        var localeParser = createLocaleParser(args);

        ComputerResultSupplier<Optional<LocalDateTime>> valueSupplier = ctx -> {
            // first we need to check the format
            Optional<String> formatString = args.has(FORMAT_ARG) //
                ? Optional.of(((StringComputer)args.get(FORMAT_ARG)).compute(ctx)) //
                : Optional.empty();

            var formatter = formatString.isPresent() //
                ? checkDateTimeFormat(formatString.get()) //
                : DEFAULT_DATE_TIME_FORMAT;

            var localeParseResult = localeParser.apply(ctx);
            if (localeParseResult.isError()) {
                ctx.addWarning(localeParseResult.getErrorMessage());
                return Optional.empty();
            }
            var locale = localeParseResult.getValue();

            var dateTimeString = dateTimeComputer.compute(ctx);

            try {
                return Optional.of(formatter.withLocale(locale).parse(dateTimeString, LocalDateTime::from));
            } catch (DateTimeParseException e) {
                if (checkIfFormatIsUnderSpecified(formatter, LocalTime::from)) {
                    var format = formatString.get();

                    // if the format is underspecified, we give a special warning
                    ctx.addWarning("Format '%s' is underspecified for LOCAL_DATE_TIME.".formatted(format));
                } else if (formatString.isPresent()) {
                    var format = formatString.get();

                    // failed parse with custom format
                    ctx.addWarning(
                        "Date time string '%s' did not match format '%s'.".formatted(dateTimeString, format));
                } else {
                    ctx.addWarning("Date time string '%s' did not match default format.".formatted(dateTimeString));
                }
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
                is used, which is the default format for `parse_datetime` with the addition of a required \
                offset and optional zone ID (where `Z` is shorthand for UTC). See the examples for more \
                information.

                If the format string is invalid, or the provided zoned date time string does not match the \
                provided format, the function returns `MISSING` and a warning is emitted. The function also \
                returns `MISSING` if the zoned date time string is missing.

                Note that an underspecified format (e.g. `yyyy`) will cause the function to return `MISSING` \
                and give a warning.

                An optional locale can be provided in [IETF format](%s) to parse the date in a specific locale. The \
                default locale is `en-US`. This will affect things like month or day names, for example.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `parse_zoned_datetime("1970-01-01T00:00:00Z")` returns `1970-01-01T00:00:00Z`
                * `parse_zoned_datetime("1970-01-01 00:00:00Z", "yyyy-MM-dd HH:mm:ssX")` returns `1970-01-01T00:00:00Z`
                * `parse_zoned_datetime("1970-01-01T00:00:00+01:00")` returns `1970-01-01T00:00:00+01:00`
                * `parse_zoned_datetime("1970-01-01T00:00:00+01:00[Europe/Paris]")` returns \
                `1970-01-01T00:00:00+01:00[Europe/Paris]`
                * `parse_zoned_datetime("1970-01-01T00:00:00Z", "invalid format")` causes an error (invalid format)
                * `parse_zoned_datetime("invalid zoned datetime")` returns `MISSING`
                * `parse_zoned_datetime("1970", "yyyy")` returns `MISSING` and a warning (underspecified format)
                """) //
        .keywords("parse", "datetime", "zoned_date_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DATE_TIME_ARG, "The string to parse into a zoned date time.", isStringOrOpt()), //
            optarg(FORMAT_ARG,
                "The format to use for parsing the zoned date time. If not specified, the default format is used.",
                isString()), //
            optarg(LOCALE_ARG, "The locale to use for parsing the date and time. If not specified, `en-US` is used.",
                isString()) //
        ) //
        .returnType("A `ZONED_DATE_TIME` representing the provided string argument", RETURN_ZONED_DATE_TIME_MISSING,
            args -> OPT_ZONED_DATE_TIME) //
        .impl(TemporalParseFormatFunctions::parseZonedDateTime) //
        .build();

    private static Computer parseZonedDateTime(final Arguments<Computer> args) {
        var zonedDateTimeComputer = (StringComputer)args.get(DATE_TIME_ARG);
        var localeParser = createLocaleParser(args);

        ComputerResultSupplier<Optional<ZonedDateTime>> valueSupplier = ctx -> {
            // first we need to check the format
            Optional<String> formatString = args.has(FORMAT_ARG) //
                ? Optional.of(((StringComputer)args.get(FORMAT_ARG)).compute(ctx)) //
                : Optional.empty();

            var formatter = formatString.isPresent() //
                ? checkDateTimeFormat(formatString.get()) //
                : DEFAULT_ZONED_DATE_TIME_FORMAT;

            var localeParseResult = localeParser.apply(ctx);
            if (localeParseResult.isError()) {
                ctx.addWarning(localeParseResult.getErrorMessage());
                return Optional.empty();
            }
            var locale = localeParseResult.getValue();

            var zonedDateTimeString = zonedDateTimeComputer.compute(ctx);

            try {
                return Optional.of(formatter.withLocale(locale).parse(zonedDateTimeString, ZonedDateTime::from));
            } catch (DateTimeParseException e) {
                if (checkIfFormatIsUnderSpecified(formatter, LocalTime::from)) {
                    var format = formatString.get();

                    // if the format is underspecified, we give a special warning
                    ctx.addWarning("Format '%s' is underspecified for ZONED_DATE_TIME.".formatted(format));
                } else if (formatString.isPresent()) {
                    var format = formatString.get();

                    // failed parse with custom format
                    ctx.addWarning("Zoned date time string '%s' did not match format '%s'."
                        .formatted(zonedDateTimeString, format));
                } else {
                    ctx.addWarning(
                        "Zoned date time string '%s' did not match default format.".formatted(zonedDateTimeString));
                }
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

                The function supports three formats:
                * long format, which looks like "1 hour 2 minutes 3.5 seconds"
                * short format, which looks like "1h 2m 3.5s"
                * iso format, which looks like "PT1H2M3.5S"

                The format will be automatically detected based on the input string.
                If the given input cannot be parsed into a date duration, the function
                returns `MISSING` and emits a warning. If the input is missing, the
                function will also return `MISSING`.
                """) //
        .examples("""
                * `parse_time_duration("1 hour 2 minutes 3.5 seconds")` returns `PT1H2M3.5S`
                * `parse_time_duration("1h 2m 3.5s")` returns `PT1H2M3.5S`
                * `parse_time_duration("PT1H")` returns `PT1H`
                * `parse_time_duration("invalid duration")` returns `MISSING`
                """) //
        .keywords("parse", "duration", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DURATION_ARG, "The string to parse into a time duration.", isStringOrOpt()) //
        ) //
        .returnType("A `TIME_DURATION` representing the provided string argument", RETURN_TIME_DURATION_MISSING,
            args -> OPT_TIME_DURATION) //
        .impl(TemporalParseFormatFunctions::parseTimeDurationImpl) //
        .build();

    private static Computer parseTimeDurationImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<Duration>> valueSupplier = ctx -> {
            var durationString = ((StringComputer)args.get(DURATION_ARG)).compute(ctx).trim();

            try {
                return Optional.of(DurationPeriodFormatUtils.parseDuration(durationString));
            } catch (DateTimeParseException ex) {
                ctx.addWarning("Invalid TIME_DURATION string '%s'.".formatted(durationString));
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

                The function supports three formats:
                * long format, which looks like "1 year 2 months 3 days"
                * short format, which looks like "1y 2m 3d"
                * iso format, which looks like "P1Y2M3D"

                The format will be automatically detected based on the input string.
                If the given input cannot be parsed into a date duration, the function
                returns `MISSING` and emits a warning. If the input is missing, the
                function will also return `MISSING`.
                """) //
        .examples("""
                * `parse_date_duration("P1Y")` returns `P1Y`
                * `parse_date_duration("1y")` returns `P1Y`
                * `parse_date_duration("1 year")` returns `P1Y`
                """) //
        .keywords("parse", "period", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DURATION_ARG, "The string to parse into a date duration.", isStringOrOpt()) //
        ) //
        .returnType("A `DATE_DURATION` representing the provided string argument", RETURN_DATE_DURATION_MISSING,
            args -> OPT_DATE_DURATION) //
        .impl(TemporalParseFormatFunctions::parseDateDurationImpl) //
        .build();

    private static Computer parseDateDurationImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<Period>> valueSupplier = ctx -> {
            var periodString = ((StringComputer)args.get(DURATION_ARG)).compute(ctx).trim();

            try {
                return Optional.of(DurationPeriodFormatUtils.parsePeriod(periodString));
            } catch (DateTimeParseException ex) {
                ctx.addWarning("Invalid DATE_DURATION string '%s'.".formatted(periodString));
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
            var formatComputer = (StringComputer)args.get(FORMAT_ARG);
            var temporalComputer = (TemporalComputer)args.get(temporalArgument);
            var localeParser = createLocaleParser(args);

            ComputerResultSupplier<Optional<String>> valueSupplier = ctx -> {
                var formatString = formatComputer.compute(ctx);
                var temporal = temporalComputer.compute(ctx);

                DateTimeFormatter formatter = checkDateTimeFormat(formatString);

                var localeParseResult = localeParser.apply(ctx);
                if (localeParseResult.isError()) {
                    ctx.addWarning(localeParseResult.getErrorMessage());
                    return Optional.empty();
                }
                var locale = localeParseResult.getValue();

                String formatted;
                try {
                    formatted = formatter.withLocale(locale).format(temporal);
                } catch (DateTimeException e) {
                    if (checkIfFormatIsOverspecified(formatter, query)) {
                        // if the format is overspecified, we give a special warning
                        var typeName = Computer.getReturnTypeFromComputer(temporalComputer).baseType().name();
                        ctx.addWarning("Format '%s' overspecified for %s.".formatted(formatString, typeName));
                    } else {
                        ctx.addWarning("Could not format %s: %s.".formatted(temporalArgument, e.getMessage()));
                    }
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
        .name("format_date") //
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

                Additionally, a locale can be provided in [IETF format](%s) to format the date according to \
                the rules of a specific locale. If no locale is provided, the locale `en-US` is used. \
                If the locale provided is invalid, the function returns `MISSING` and emits a warning.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `format_date(parse_date("1970-01-01"), "dd/MM/yyyy")` returns `01/01/1970`
                * `format_date(parse_date("invalid date"), "dd/MM/yyyy")` returns `MISSING` and emits a warning
                * `format_date(parse_date("1970-01-01"), "yyyy-MM-dd HH")` returns `MISSING` and a warning \
                (overspecified format)
                * `format_date(parse_date("1970-01-01"), "invalid format")` causes an error (invalid format)
                """) //
        .keywords("format", "date", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DATE_ARG, "The date to format as a string.", isLocalDateOrOpt()), //
            arg(FORMAT_ARG, "The format to use for formatting the temporal value.", isString()), //
            optarg(LOCALE_ARG, "The locale to use for formatting the date. If not specified, `en-US` is used.",
                isString())//
        ) //
        .returnType("A string representing the provided `LOCAL_DATE`", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(formatTemporalImpl(DATE_ARG, LocalDate::from)) //
        .build();

    public static final ExpressionFunction FORMAT_TIME = functionBuilder() //
        .name("format_time") //
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

                Additionally, a locale can be provided in [IETF format](%s) to format the time according to \
                the rules of a specific locale. If no locale is provided, the locale `en-US` is used. \
                If the locale provided is invalid, the function returns `MISSING` and emits a warning.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `format_time(parse_time("12:34:56"), "HH:mm")` returns `12:34`
                * `format_time(parse_time("invalid time"), "HH:mm:ss")` returns `MISSING` and emits a warning
                * `format_time(parse_time("12:34"), "invalid format")` returns `MISSING` and a warning \
                (invalid format)
                * `format_time(parse_time("12:34"), "HH:mm dd/MM/yyyy")` causes an error (overspecified format)
                """) //
        .keywords("format", "time", "string") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(TIME_ARG, "The time value to format as a string.", isLocalTimeOrOpt()), //
            arg(FORMAT_ARG, "The format to use.", isString()), //
            optarg(LOCALE_ARG, "The locale to use for formatting the time. If not specified, `en-US` is used.",
                isString())//
        ) //
        .returnType("A string representing the provided `LOCAL_TIME`", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(formatTemporalImpl(TIME_ARG, LocalTime::from)) //
        .build();

    public static final ExpressionFunction FORMAT_DATE_TIME = functionBuilder() //
        .name("format_datetime") //
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

                Additionally, a locale can be provided in [IETF format](%s) to format the date-time according to \
                the rules of a specific locale. If no locale is provided, the locale `en-US` is used. \
                If the locale provided is invalid, the function returns `MISSING` and emits a warning.
                """.formatted(JAVADOC_URL_DATE_FORMAT, IETF_FORMAT_URL)) //
        .examples("""
                * `format_datetime($["my_datetime_col"], "dd/MM/yyyy HH:mm:ss")` returns \
                `01/01/1970 00:00:00`
                * `format_datetime($["my_zoned_col"], "VV")` returns `Europe/Paris`
                * `format_datetime($["my_datetime_col"], "invalid format")` causes an error (invalid format)
                * `format_datetime($["my_datetime_col"], "dd/MM/yyyy HH:mm:ss VV")` returns `MISSING` and a \
                warning (overspecified format)
                """) //
        .keywords("format", "datetime", "string", "zoned", "format_zoned") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DATE_TIME_ARG, "The date time value to format as a string.", hasDateAndTimeInformationOrOpt()), //
            arg(FORMAT_ARG, "The format to use.", isString()), //
            optarg(LOCALE_ARG, "The locale to use for formatting the date and time. If not specified, `en-US` is used.",
                isString()) //
        ) //
        .returnType("A string representing the provided date-time", RETURN_STRING_MISSING, args -> OPT_STRING) //
        .impl(TemporalParseFormatFunctions::formatDateTimeImpl) //
        .build();

    /**
     * Since this function can take both `LOCAL_DATE_TIME` and `ZONED_DATE_TIME` as input, we need to check which type
     * we're dealing with so that we can correctly check the format.
     */
    private static Computer formatDateTimeImpl(final Arguments<Computer> args) {
        var localOrZonedDateTimeComputer = (TemporalComputer)args.get(DATE_TIME_ARG);

        if (localOrZonedDateTimeComputer instanceof ZonedDateTimeComputer) {
            return formatTemporalImpl(DATE_TIME_ARG, ZonedDateTime::from).apply(args);
        } else if (localOrZonedDateTimeComputer instanceof LocalDateTimeComputer) {
            return formatTemporalImpl(DATE_TIME_ARG, LocalDateTime::from).apply(args);
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
                * `format_duration(parse_time_duration("PT1S"), "short")` returns `1s`
                * `format_duration(parse_time_duration("PT1H"), "invalid format")` returns `MISSING` and a warning \
                (invalid format)
                """) //
        .keywords("format", "interval", "duration", "period") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg(DURATION_ARG, "The duration to format as a string.", isDurationOrOpt()), //
            arg(FORMAT_ARG, "The format type to use: either 'short', 'long', or 'iso'.", isString()) //
        ) //
        .returnType("A string representing the provided duration", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalParseFormatFunctions::formatDurationImpl) //
        .build();

    private static final String ILLEGAL_DURATION_FORMAT_ERROR =
        "Unknown format '%s'. Allowed are: 'iso', 'long', and 'short'.";

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
            var style = ((StringComputer)args.get(FORMAT_ARG)).compute(ctx);
            var temporalAmount = ((TemporalAmountComputer)args.get(DURATION_ARG)).compute(ctx);

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

    private static ComputerResultSupplier<ReturnResult<Locale>> createLocaleParser(final Arguments<Computer> args) {
        return ctx -> {
            if (!args.has(LOCALE_ARG)) {
                return ReturnResult.success(Locale.US);
            }

            var localeString = ((StringComputer)args.get(LOCALE_ARG)).compute(ctx);

            var locale = Locale.forLanguageTag(localeString);
            var parsedLanguage = locale.getLanguage();

            if (parsedLanguage.isBlank()) {
                // a blank tag indicates either an invalid locale or an empty input. Either is illegal here
                return ReturnResult.failure("Malformed locale '%s'.".formatted(localeString));
            } else if (!ArrayUtils.contains(Locale.getISOLanguages(), parsedLanguage)) {
                return ReturnResult.failure("Invalid locale language '%s'.".formatted(parsedLanguage));
            } else {
                return ReturnResult.success(locale);
            }
        };
    }
}
