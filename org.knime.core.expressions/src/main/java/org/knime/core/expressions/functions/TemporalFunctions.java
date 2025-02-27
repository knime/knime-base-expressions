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
import static org.knime.core.expressions.SignatureUtils.hasDateAndTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.hasDateInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.hasTimeInformationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isDateDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isIntegerOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalDateTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isLocalTimeOrOpt;
import static org.knime.core.expressions.SignatureUtils.isString;
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
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.STRING;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
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
import org.knime.core.expressions.Computer.TemporalAmountComputer;
import org.knime.core.expressions.Computer.TemporalComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.SignatureUtils;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.ValueType.NativeValueType;
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

    public static final OperatorCategory CATEGORY_ARITHMETIC = new OperatorCategory(TEMPORAL_META_CATEGORY_NAME,
        "Arithmetic", "Functions for performing arithmetic operations on temporal data.");

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
            var temporalComputer = (TemporalComputer)args.get(temporalArgument);

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
        var localOrZonedDateTimeComputer = (TemporalComputer)args.get("datetime");

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
        .impl(TemporalFunctions::makeDateImpl) //
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
                    ctx.addWarning(e.getMessage().replace("dayOfMonth", "day"));
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
        .impl(TemporalFunctions::makeTimeImpl) //
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
                    ctx.addWarning(e.getMessage().replace("nanoOfSecond", "nanosecond"));
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
        .impl(TemporalFunctions::makeDurationImpl) //
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
                throw new ExpressionEvaluationException("Duration values are too large", e);
            }
        };

        return TimeDurationComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

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
        .impl(TemporalFunctions::makePeriodImpl) //
        .build();

    private static Computer makePeriodImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Period> valueSupplier = ctx -> {
            try {
                var years = Math.toIntExact(((IntegerComputer)args.get("years")).compute(ctx));
                var months = Math.toIntExact(((IntegerComputer)args.get("months")).compute(ctx));
                var days = Math.toIntExact(((IntegerComputer)args.get("days")).compute(ctx));

                return Period.of(years, months, days);
            } catch (ArithmeticException ex) {
                throw new ExpressionEvaluationException("Period values are too large", ex);
            }
        };

        return DateDurationComputer.of( //
            valueSupplier, //
            anyMissing(args) //
        );
    }

    private static final Function<Arguments<Computer>, Computer> extractTemporalFieldImpl(final TemporalField field) {
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
                * `extract_hour(parse_time("05:06:07"))` returns `5`
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
                * `extract_minute(parse_time("05:06:07"))` returns `6`
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
                * `extract_second(parse_time("05:06:07"))` returns `7`
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
                * `extract_nanosecond(parse_time("00:00:00.123456789"))` returns `123456789`
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
        .impl(TemporalFunctions::extractDateImpl) //
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
        .impl(TemporalFunctions::extractTimeImpl) //
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

    public static final ExpressionFunction DATE_DURATION_BETWEEN = new ExpressionFunction() {

        private static final List<String> ARG_NAMES = List.of("start", "end");

        private static final List<String> ARG_DESCS = List.of("The start time", "The end time");

        private static final String ARG_TYPE_DESC = Stream.of( //
            NativeValueType.LOCAL_DATE, //
            NativeValueType.LOCAL_DATE_TIME, //
            NativeValueType.ZONED_DATE_TIME //
        ) //
            .map(ValueType::name) //
            .collect(Collectors.joining(", "));

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {

            var argList = IntStream.range(0, ARG_NAMES.size()) //
                .mapToObj(i -> arg(ARG_NAMES.get(i), ARG_DESCS.get(i), hasDateInformationOrOpt())) //
                .toList();

            return SignatureUtils.matchSignature(argList, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            // Check that both arguments are of the same type and that that type has time information
            var startType = argTypes.get("start");
            var endType = argTypes.get("end");

            if (!startType.baseType().equals(endType.baseType())) {
                return ReturnResult.failure("Both arguments must be of the date type");
            }

            var startHasDateInformation = hasDateInformationOrOpt().matches(startType);
            var endHasDateInformation = hasDateInformationOrOpt().matches(endType);

            if (!startHasDateInformation || !endHasDateInformation) {
                return ReturnResult.failure("Both arguments must have date information");
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
                    have date information. The time part of the date-time values will be ignored.

                    If either of the provided dates is missing, the function returns `MISSING`.
                    """;

            var examples = """
                    * `date_duration_between(parse_date("1970-01-01"), parse_date("1970-01-02"))` returns `P1D`
                    * `date_duration_between(parse_date("1970-01-02"), parse_date("1970-01-01"))` returns `-P1D`
                    """;

            var arguments = IntStream.range(0, ARG_NAMES.size()) //
                .mapToObj(i -> new OperatorDescription.Argument(ARG_NAMES.get(i), ARG_TYPE_DESC, ARG_DESCS.get(i))) //
                .toList();

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
                var start = ((TemporalComputer)args.get("start")).compute(ctx);
                var end = ((TemporalComputer)args.get("end")).compute(ctx);

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

        private static final List<String> ARG_NAMES = List.of("start", "end");

        private static final List<String> ARG_DESCS = List.of("The start time", "The end time");

        private static final String ARG_TYPE_DESC = Stream.of( //
            NativeValueType.LOCAL_TIME, //
            NativeValueType.LOCAL_DATE_TIME, //
            NativeValueType.ZONED_DATE_TIME //
        ) //
            .map(ValueType::name) //
            .collect(Collectors.joining(", "));

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {

            var argList = IntStream.range(0, ARG_NAMES.size()) //
                .mapToObj(i -> arg(ARG_NAMES.get(i), ARG_DESCS.get(i), hasTimeInformationOrOpt())) //
                .toList();

            return SignatureUtils.matchSignature(argList, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            // Check that both arguments are of the same type and that that type has time information
            var startType = argTypes.get("start");
            var endType = argTypes.get("end");

            if (!startType.baseType().equals(endType.baseType())) {
                return ReturnResult.failure("Both arguments must be of the same type");
            }

            var startHasTimeInformation = hasTimeInformationOrOpt().matches(startType);

            if (!startHasTimeInformation) {
                return ReturnResult.failure("Both arguments must have time information");
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

            var arguments = IntStream.range(0, ARG_NAMES.size()) //
                .mapToObj(i -> new OperatorDescription.Argument(ARG_NAMES.get(i), ARG_TYPE_DESC, ARG_DESCS.get(i))) //
                .toList();

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
                var start = ((TemporalComputer)args.get("start")).compute(ctx);
                var end = ((TemporalComputer)args.get("end")).compute(ctx);

                try {
                    return Duration.between(start, end);
                } catch (ArithmeticException ex) {
                    throw new ExpressionEvaluationException("Duration is too large to be represented", ex);
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
                possible that the resulting time is too large to be represented, in which case the function
                returns `MISSING` and emits a warning.
                """) //
        .examples("""
                * `add_time_duration(parse_time("00:00:00"), parse_time_duration("PT1H"))` returns `01:00:00`
                * `add_time_duration(parse_time("00:00:00"), parse_time_duration("PT1H30M"))` returns `01:30:00`
                """) //
        .keywords("add", "time", "duration") //
        .category(CATEGORY_ARITHMETIC) //
        .args( //
            arg("start", "The time to add the duration to.", hasTimeInformationOrOpt()), //
            arg("duration", "The duration to add to the time.", isTimeDurationOrOpt()) //
        ) //
        .returnType("The input value with the duration added", hasTimeInformationOrOpt().allowed(),
            args -> anyOptional(args) ? args.get("start").optionalType() : args.get("start").baseType()) //
        .impl(TemporalFunctions::addTimeDurationImpl) //
        .build();

    private static Computer addTimeDurationImpl(final Arguments<Computer> args) {
        var startComputer = (TemporalComputer)args.get("start");

        ComputerResultSupplier<Temporal> valueSupplier = ctx -> {
            var startTime = startComputer.compute(ctx);
            var duration = ((TimeDurationComputer)args.get("duration")).compute(ctx);

            try {
                return startTime.plus(duration);
            } catch (ArithmeticException | DateTimeException ex) {
                throw new ExpressionEvaluationException("Resulting date-time is too large to be represented", ex);
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
        .keywords("add", "date", "duration") //
        .category(CATEGORY_ARITHMETIC) //
        .args( //
            arg("start", "The date-time to add the duration to.", hasDateInformationOrOpt()), //
            arg("duration", "The duration to add to the date-time.", isDateDurationOrOpt()) //
        ) //
        .returnType("The input value with the duration added", hasDateInformationOrOpt().allowed(),
            args -> anyOptional(args) ? args.get("start").optionalType() : args.get("start").baseType()) //
        .impl(TemporalFunctions::addDateDurationImpl) //
        .build();

    private static Computer addDateDurationImpl(final Arguments<Computer> args) {
        var startComputer = (TemporalComputer)args.get("start");

        ComputerResultSupplier<Temporal> valueSupplier = ctx -> {
            var startDate = startComputer.compute(ctx);
            var duration = ((DateDurationComputer)args.get("duration")).compute(ctx);

            try {
                return startDate.plus(duration);
            } catch (ArithmeticException | DateTimeException ex) {
                throw new ExpressionEvaluationException("Resulting date-time is too large to be represented", ex);
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

        private static final String ARG_TYPE = hasDateInformationOrOpt().allowed();

        private static final List<String> ARG_NAMES = List.of("start", "end");

        private static final List<String> ARG_DESCS = List.of("The start date-time", "The end date-time");

        UnitsBetweenExpressionFunction(final ChronoUnit unit, final String description, final String examples) {
            m_unit = unit;
            m_description = description;
            m_examples = examples;
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {

            var argList = IntStream.range(0, ARG_NAMES.size()) //
                .mapToObj(i -> arg(ARG_NAMES.get(i), ARG_DESCS.get(i), hasDateInformationOrOpt())) //
                .toList();

            return SignatureUtils.matchSignature(argList, positionalArguments, namedArguments);
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
                IntStream.range(0, ARG_NAMES.size()) //
                    .mapToObj(i -> new OperatorDescription.Argument(ARG_NAMES.get(i), ARG_TYPE, ARG_DESCS.get(i))) //
                    .toList(), //
                RETURN_INTEGER_MISSING, //
                "An integer representing the number of %s between the two date-times".formatted(m_unit.toString()), //
                List.of("interval", "duration", "period", "date", "difference"), //
                CATEGORY_ARITHMETIC.fullName(), //
                OperatorDescription.FUNCTION_ENTRY_TYPE //
            );
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            var startType = argTypes.get("start");
            var endType = argTypes.get("end");

            if (!startType.baseType().equals(endType.baseType())) {
                return ReturnResult.failure("Both arguments must be of the same type");
            }

            var startHasTimeInformation = hasDateInformationOrOpt().matches(startType);

            if (!startHasTimeInformation) {
                return ReturnResult.failure("Both arguments must have date information");
            }

            return ReturnResult.success(INTEGER(anyOptional(argTypes)));

        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            IntegerComputerResultSupplier value = ctx -> {
                var start = ((TemporalComputer)args.get("start")).compute(ctx);
                var end = ((TemporalComputer)args.get("end")).compute(ctx);

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
                year apart.

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
                month apart.

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
                00:00:01 on the next day are considered to be one day apart.

                If the first date is after the second date, the result will be negative.

                If either of the provided dates is missing, the function returns `MISSING`.
                """, //
        """
                * `days_between(parse_date("1970-01-01"), parse_date("1970-01-02"))` returns `1`
                * `days_between(parse_date("1970-01-02"), parse_date("1970-01-01"))` returns `-1`
                """ //
    );

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
