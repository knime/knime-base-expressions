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

import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_DURATION_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_DATE_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_PERIOD_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_STRING_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_ZONED_DATE_TIME_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.isString;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQuery;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.DurationComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.PeriodComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;

/**
 * Implementation of built-in functions that manipulate temporal data such as dates, times, durations, and periods.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
public final class TemporalFunctions {

    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    // seconds and subseconds optional
    private static final DateTimeFormatter DEFAULT_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    // seconds and subseconds optional
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // zone id optional, but offset required
    private static final DateTimeFormatter DEFAULT_ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    private static final String TEMPORAL_META_CATEGORY_NAME = "Temporal";

    public static final OperatorCategory CATEGORY_PARSE_FORMAT =
        new OperatorCategory(TEMPORAL_META_CATEGORY_NAME, "Parsing", """
                Functions for parsing and formatting temporal data to and from strings, such as dates, times, \
                durations, and periods.
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
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return Optional.empty();
                }

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
                    ctx -> valueSupplier.apply(ctx).isEmpty() //
                );
            } else if (temporalType == LocalTime.class) {
                return LocalTimeComputer.of( //
                    ctx -> (LocalTime)valueSupplier.apply(ctx).get(), //
                    ctx -> valueSupplier.apply(ctx).isEmpty() //
                );
            } else if (temporalType == LocalDateTime.class) {
                return LocalDateTimeComputer.of( //
                    ctx -> (LocalDateTime)valueSupplier.apply(ctx).get(), //
                    ctx -> valueSupplier.apply(ctx).isEmpty() //
                );
            } else if (temporalType == ZonedDateTime.class) {
                return ZonedDateTimeComputer.of( //
                    ctx -> (ZonedDateTime)valueSupplier.apply(ctx).get(), //
                    ctx -> valueSupplier.apply(ctx).isEmpty() //
                );
            } else {
                throw new IllegalArgumentException("Unsupported temporal type: " + temporalType);
            }
        };
    }

    public static final ExpressionFunction PARSE_LOCAL_DATE = functionBuilder() //
        .name("parse_date") //
        .description("Parses a string into a LOCAL_DATE.") //
        .examples("""
                * `parsedate("1970-01-01")` returns `1970-01-01`
                * `parsedate("01/01/1970", "dd/MM/yyyy")` returns `1970-01-01`
                * `parsedate("01/01/1970", "yyyy-MM-dd")` returns `MISSING`
                * `parsedate("01/01/1970")` returns `MISSING`
                """) //
        .keywords("parse", "date", "local_date") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("date", "The string to parse into a date.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the date. If not specified, the default format is used.",
                isString()) //
        ) //
        .returnType("A `LOCAL_DATE` representing the provided string argument", RETURN_LOCAL_DATE_MISSING,
            args -> LOCAL_DATE(anyOptional(args))) //
        .impl(
            TemporalFunctions.parseTemporalAccessorImpl(LocalDate.class, LocalDate::from, "date", DEFAULT_DATE_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_LOCAL_TIME = functionBuilder() //
        .name("parse_time") //
        .description("Parses a string into a LOCAL_TIME.") //
        .examples("""
                * `parsetime("00:00:00")` returns `00:00:00`
                * `parsetime("00:00:00", "HH:mm:ss")` returns `00:00:00`
                * `parsetime("00:00:00", "dd/MM/yyyy")` returns `MISSING`
                * `parsetime("00:00:00")` returns `MISSING`
                """) //
        .keywords("parse", "time", "local_time") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("time", "The string to parse into a time.", isStringOrOpt()), //
            optarg("format", "The format to use for parsing the time. If not specified, the default format is used.",
                isString()) //
        ) //
        .returnType("A `LOCAL_TIME` representing the provided string argument", RETURN_LOCAL_TIME_MISSING,
            args -> LOCAL_TIME(anyOptional(args))) //
        .impl(
            TemporalFunctions.parseTemporalAccessorImpl(LocalTime.class, LocalTime::from, "time", DEFAULT_TIME_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_LOCAL_DATE_TIME = functionBuilder() //
        .name("parse_datetime") //
        .description("Parses a string into a LOCAL_DATE_TIME.") //
        .examples("""
                * `parsedatetime("1970-01-01T00:00:00")` returns `1970-01-01T00:00:00`
                * `parsedatetime("1970-01-01T00:00:00", "yyyy-MM-dd'T'HH:mm:ss")` returns `1970-01-01T00:00:00`
                * `parsedatetime("1970-01-01T00:00:00", "dd/MM/yyyy")` returns `MISSING`
                * `parsedatetime("1970-01-01T00:00:00")` returns `MISSING`
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
            args -> LOCAL_DATE_TIME(anyOptional(args))) //
        .impl(TemporalFunctions.parseTemporalAccessorImpl(LocalDateTime.class, LocalDateTime::from, "datetime",
            DEFAULT_DATE_TIME_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_ZONED_DATE_TIME = functionBuilder() //
        .name("parse_zoned_datetime") //
        .description("Parses a string into a ZONED_DATE_TIME.") //
        .examples("""
                * `parsezoneddatetime("1970-01-01T00:00:00Z")` returns `1970-01-01T00:00:00Z`
                * `parsezoneddatetime("1970-01-01T00:00:00Z", "yyyy-MM-dd'T'HH:mm:ssX")` returns `1970-01-01T00:00:00Z`
                * `parsezoneddatetime("1970-01-01T00:00:00Z", "dd/MM/yyyy")` returns `MISSING`
                * `parsezoneddatetime("1970-01-01T00:00:00Z")` returns `MISSING`
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
            args -> ZONED_DATE_TIME(anyOptional(args))) //
        .impl(TemporalFunctions.parseTemporalAccessorImpl(ZonedDateTime.class, ZonedDateTime::from, "datetime",
            DEFAULT_ZONED_DATE_TIME_FORMAT)) //
        .build();

    public static final ExpressionFunction PARSE_DURATION = functionBuilder() //
        .name("parse_duration") //
        .description("Parses a string into a DURATION.") //
        .examples("""
                * `parseduration("PT1H")` returns `PT1H`
                * `parseduration("1h", "short")` returns `PT1H`
                * `parseduration("1 hour", "long")` returns `PT1H`
                * `parseduration("1 hour")` returns `MISSING`
                """) //
        .keywords("parse", "duration", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("duration", "The string to parse into a duration.", isStringOrOpt()), //
            optarg("style", "The style to use for parsing the duration. If not specified, the default style is used.",
                isString()) //
        ) //
        .returnType("A `DURATION` representing the provided string argument", RETURN_DURATION_MISSING,
            args -> DURATION(anyOptional(args))) //
        .impl(TemporalFunctions::parseDurationImpl) //
        .build();

    private static Computer parseDurationImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<Duration>> valueSupplier = ctx -> {
            if (anyMissing(args).applyAsBoolean(ctx)) {
                return Optional.empty();
            }

            var durationString = ((StringComputer)args.get("duration")).compute(ctx);
            try {
                var interval = Interval.parseHumanReadableOrIso(durationString);
                if (interval instanceof TimeInterval ti) {
                    return Optional.of(ti.asDuration());
                } else {
                    ctx.addWarning("Invalid duration string '%s'".formatted(durationString));
                    return Optional.empty();
                }
            } catch (IllegalArgumentException e) {
                ctx.addWarning("Invalid duration string '%s'".formatted(durationString));
                return Optional.empty();
            }
        };

        return DurationComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction PARSE_PERIOD = functionBuilder() //
        .name("parse_period") //
        .description("Parses a string into a PERIOD.") //
        .examples("""
                * `parseperiod("P1Y")` returns `P1Y`
                * `parseperiod("1y", "short")` returns `P1Y`
                * `parseperiod("1 year", "long")` returns `P1Y`
                * `parseperiod("1 year")` returns `MISSING`
                """) //
        .keywords("parse", "period", "interval") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("period", "The string to parse into a period.", isStringOrOpt()), //
            optarg("style", "The style to use for parsing the period. If not specified, the default style is used.",
                isString()) //
        ) //
        .returnType("A `PERIOD` representing the provided string argument", RETURN_PERIOD_MISSING,
            args -> PERIOD(anyOptional(args))) //
        .impl(TemporalFunctions::parsePeriodImpl) //
        .build();

    private static Computer parsePeriodImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<Period>> valueSupplier = ctx -> {
            if (anyMissing(args).applyAsBoolean(ctx)) {
                return Optional.empty();
            }

            var periodString = ((StringComputer)args.get("period")).compute(ctx);
            try {
                var interval = Interval.parseHumanReadableOrIso(periodString);
                if (interval instanceof DateInterval di) {
                    return Optional.of(di.asPeriod());
                } else {
                    ctx.addWarning("Invalid period string '%s'".formatted(periodString));
                    return Optional.empty();
                }
            } catch (IllegalArgumentException e) {
                ctx.addWarning("Invalid period string '%s'".formatted(periodString));
                return Optional.empty();
            }
        };

        return PeriodComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction FORMAT_TEMPORAL = functionBuilder() //
        .name("format_temporal") //
        .description("Formats a temporal value to a string.") //
        .examples("""
                * `format_temporal(parsedate("1970-01-01"), "dd/MM/yyyy")` returns `01/01/1970`
                """) //
        .keywords("format", "temporal", "date", "time", "zone") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("temporal", "The temporal value to format as a string.", isStringOrOpt()), //
            arg("format", "The format to use for formatting the temporal value.", isString()) //
        ) //
        .returnType("A string representing the provided temporal value", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalFunctions::formatTemporalImpl) //
        .build();

    private static Computer formatTemporalImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<String>> valueSupplier = ctx -> {
            if (anyMissing(args).applyAsBoolean(ctx)) {
                return Optional.empty();
            }

            var formatString = ((StringComputer)args.get("format")).compute(ctx);
            var temporal = extractTemporalValueFromComputer(args.get("temporal"), ctx);

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
            ctx -> valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction FORMAT_INTERVAL = functionBuilder() //
        .name("format_interval") //
        .description("Formats an interval to a string.") //
        .examples("""
                * `format_interval(parseduration("PT1H"), "short")` returns `1h`
                * `format_interval(parseduration("PT1H"), "long")` returns `1 hour`
                * `format_interval(parseperiod("P1Y"), "short")` returns `1y`
                * `format_interval(parseperiod("P1Y"), "long")` returns `1 year`
                """) //
        .keywords("format", "interval", "duration", "period") //
        .category(CATEGORY_PARSE_FORMAT) //
        .args( //
            arg("interval", "The interval to format as a string.", isStringOrOpt()), //
            arg("style", "The style to use for formatting the interval.", isString()) //
        ) //
        .returnType("A string representing the provided interval", RETURN_STRING_MISSING,
            args -> STRING(anyOptional(args))) //
        .impl(TemporalFunctions::formatIntervalImpl) //
        .build();

    private static Computer formatIntervalImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<String>> valueSupplier = ctx -> {
            if (anyMissing(args).applyAsBoolean(ctx)) {
                return Optional.empty();
            }

            var style = ((StringComputer)args.get("style")).compute(ctx);
            var temporalAmount = extractTemporalAmountFromComputer(args.get("interval"), ctx);

            if (temporalAmount instanceof Duration d) {
                var timeInterval = TimeInterval.fromDuration(d);

                return Optional.ofNullable(switch (style) {
                    case "short" -> timeInterval.toShortHumanReadableString();
                    case "long" -> timeInterval.toLongHumanReadableString();
                    case "iso" -> timeInterval.toISOString();
                    default -> {
                        ctx.addWarning("Unknown interval style '%s'".formatted(style));
                        yield null;
                    }
                });
            } else if (temporalAmount instanceof Period p) {
                var dateInterval = DateInterval.fromPeriod(p);

                return Optional.ofNullable(switch (style) {
                    case "short" -> dateInterval.toShortHumanReadableString();
                    case "long" -> dateInterval.toLongHumanReadableString();
                    case "iso" -> dateInterval.toISOString();
                    default -> {
                        ctx.addWarning("Unknown interval style '%s'".formatted(style));
                        yield null;
                    }
                });
            } else {
                throw new IllegalArgumentException("Unsupported temporal amount type: " + temporalAmount);
            }
        };

        return StringComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> valueSupplier.apply(ctx).isEmpty() //
        );
    }

    private static TemporalAccessor extractTemporalValueFromComputer(final Computer computer,
        final EvaluationContext ctx) throws ExpressionEvaluationException {
        if (computer instanceof LocalDateComputer ldc) {
            return ldc.compute(ctx);
        } else if (computer instanceof LocalTimeComputer ltc) {
            return ltc.compute(ctx);
        } else if (computer instanceof LocalDateTimeComputer ldcc) {
            return ldcc.compute(ctx);
        } else if (computer instanceof ZonedDateTimeComputer zdtc) {
            return zdtc.compute(ctx);
        } else {
            throw new IllegalArgumentException("Unsupported temporal type: " + computer);
        }
    }

    private static TemporalAmount extractTemporalAmountFromComputer(final Computer computer,
        final EvaluationContext ctx) throws ExpressionEvaluationException {
        if (computer instanceof DurationComputer dc) {
            return dc.compute(ctx);
        } else if (computer instanceof PeriodComputer pc) {
            return pc.compute(ctx);
        } else {
            throw new IllegalArgumentException("Unsupported temporal amount type: " + computer);
        }
    }
}
