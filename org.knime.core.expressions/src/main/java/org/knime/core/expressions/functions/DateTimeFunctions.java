package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasTimePartOrIsOpt;
import static org.knime.core.expressions.SignatureUtils.isDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isPeriodOrOpt;
import static org.knime.core.expressions.SignatureUtils.isString;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.DURATION;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.PERIOD;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.DurationComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.PeriodComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.time.node.manipulate.datetimeround.DateRoundNodeSettings;
import org.knime.time.node.manipulate.datetimeround.DateRoundNodeSettings.RoundDatePrecision;
import org.knime.time.node.manipulate.datetimeround.DateRoundingUtil;
import org.knime.time.node.manipulate.datetimeround.TimeRoundNodeSettings;
import org.knime.time.node.manipulate.datetimeround.TimeRoundingUtil;
import org.knime.time.util.DurationPeriodFormatUtils;

/**
 * Implementation of built-in functions that manipulate date and times.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
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

    public static final ExpressionFunction PARSE_TIME = functionBuilder() //
        .name("parse_time") //
        .description("""
                The node parses String values into Time values.
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
        .returnType("A local time", RETURN_LOCAL_TIME, args -> LOCAL_TIME(anyOptional(args)))//
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

    public static final ExpressionFunction ROUND_TIME = functionBuilder() //
        .name("round_time") //
        .description("""
                The node rounds Time values to the nearest unit.
                """) //
        .examples("""
                * `round_time(time("12:12"), "HOURS")` returns LocalTime(12, 0)
                """) //
        .keywords("round", "time") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("time", "Time to round", hasTimePartOrIsOpt()), //
            optarg("precision", "Precision to round to", isDurationOrOpt()), //
            optarg("strategy", "Rounding strategy: 'FIRST', 'NEAREST', 'LAST'", isStringOrOpt()) //
        ) //
        .returnType("A local time", RETURN_LOCAL_TIME, args -> LOCAL_TIME(anyOptional(args)))//
        .impl(DateTimeFunctions::roundTime) //
        .build();

    private static Computer roundTime(final Arguments<Computer> args) {
        var timeComputer = args.get("time");

        if (timeComputer instanceof LocalTimeComputer) {
            return LocalTimeComputer.of( //
                ctx -> (LocalTime)roundTimeBasedTemporal(args, "time", ctx), //
                ctx -> roundTemporalIsMissing(args, "time", ctx) //
            );
        } else if (timeComputer instanceof LocalDateTimeComputer) {
            return LocalDateTimeComputer.of( //
                ctx -> (LocalDateTime)roundTimeBasedTemporal(args, "time", ctx), //
                ctx -> roundTemporalIsMissing(args, "time", ctx) //
            );
        } else if (timeComputer instanceof ZonedDateTimeComputer) {
            return ZonedDateTimeComputer.of( //
                ctx -> (ZonedDateTime)roundTimeBasedTemporal(args, "time", ctx), //
                ctx -> roundTemporalIsMissing(args, "time", ctx) //
            );
        } else {
            throw FunctionUtils.calledWithIllegalArgs();
        }
    }

    public static final ExpressionFunction ROUND_DATE = functionBuilder() //
        .name("round_date") //
        .description("""
                The node rounds Date values to the nearest unit.
                """) //
        .examples("""
                * `round_date(date("2021-01-01"), "MONTHS")` returns LocalDate(2021-01-01)
                """) //
        .keywords("round", "date") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("date", "Date to round", hasTimePartOrIsOpt()), //
            optarg("precision", "Precision to round to: " + RoundDatePrecision.values(), isStringOrOpt()), //
            optarg("strategy", "Rounding strategy: 'FIRST', 'NEAREST', 'LAST'", isStringOrOpt()), //
            optarg("shift_mode", "Shift mode: 'PREVIOUS','THIS', 'NEXT'", isStringOrOpt()), //
            optarg("dayOrWeekday", "If set to weekday rounding will exclude weekends", isStringOrOpt()) //
        ) //
        .returnType("A local date", RETURN_LOCAL_TIME, args -> LOCAL_TIME(anyOptional(args)))//
        .impl(DateTimeFunctions::roundDate) //
        .build();

    private static Computer roundDate(final Arguments<Computer> args) {
        var dateComputer = args.get("date");

        if (dateComputer instanceof LocalDateComputer) {
            return LocalDateComputer.of( //
                ctx -> (LocalDate)roundDateBasedTemporal(args, "date", ctx), //
                ctx -> roundTemporalIsMissing(args, "date", ctx) //
            );
        } else if (dateComputer instanceof LocalDateTimeComputer) {
            return LocalDateTimeComputer.of( //
                ctx -> (LocalDateTime)roundDateBasedTemporal(args, "date", ctx), //
                ctx -> roundTemporalIsMissing(args, "date", ctx) //
            );
        } else if (dateComputer instanceof ZonedDateTimeComputer) {
            return ZonedDateTimeComputer.of( //
                ctx -> (ZonedDateTime)roundDateBasedTemporal(args, "date", ctx), //
                ctx -> roundTemporalIsMissing(args, "date", ctx) //
            );
        } else {
            throw FunctionUtils.calledWithIllegalArgs();
        }
    }

    public static final ExpressionFunction PARSE_PERIOD = functionBuilder() //
        .name("parse_period") //
        .description("""
                The node parses String values into Period values.
                """) //
        .examples("""
                * `period("P1Y2M3D")` returns Period.of(1, 2, 3)
                """) //
        .keywords("") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("string", "String to parse to a period", isStringOrOpt()) //
        ) //
        .returnType("A period", "Period", args -> PERIOD(anyOptional(args)))//
        .impl(DateTimeFunctions::parsePeriod) //
        .build();

    private static Computer parsePeriod(final Arguments<Computer> args) {
        var string = toString(args.get("string"));

        return PeriodComputer.of( //
            ctx -> {
                var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
                return ((DateInterval)parsedInterval).asPeriod();
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
                    return !(parsedInterval instanceof DateInterval);
                } catch (IllegalArgumentException e) {
                    return true;
                }
            });
    }

    public static final ExpressionFunction PARSE_DURATION = functionBuilder() //
        .name("parse_duration") //
        .description("""
                The node parses String values into Duration values.
                """) //
        .examples("""
                * `duration("PT12H")` returns Duration.ofHours(12)
                """) //
        .keywords("") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("string", "String to parse to a duration", isStringOrOpt()) //
        ) //
        .returnType("A duration", "Duration", args -> DURATION(anyOptional(args)))//
        .impl(DateTimeFunctions::parseDuration) //
        .build();

    private static Computer parseDuration(final Arguments<Computer> args) {
        var string = toString(args.get("string"));

        return DurationComputer.of( //
            ctx -> {
                var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
                return ((TimeInterval)parsedInterval).asDuration();
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                try {
                    var parsedInterval = Interval.parseHumanReadableOrIso(string.compute(ctx));
                    return !(parsedInterval instanceof TimeInterval);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return true;
                }
            });
    }

    public static final ExpressionFunction FORMAT_PERIOD = functionBuilder() //
        .name("format_period") //
        .description("""
                The node formats Period values into String values.
                """) //
        .examples("""
                * `format_period(period("P1Y2M3D"))` returns "P1Y2M3D"
                """) //
        .keywords("") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("period", "Period to format", isPeriodOrOpt()), //
            optarg("format", "Format of the period string. If not specified, use ISO.", isString()) //
        ) //
        .returnType("A string", "String", args -> STRING(anyOptional(args)))//
        .impl(DateTimeFunctions::formatPeriod) //
        .build();

    private static Computer formatPeriod(final Arguments<Computer> args) {
        var period = toPeriod(args.get("period"));

        return StringComputer.of( //
            ctx -> {
                var format = args.has("format") //
                    ? toString(args.get("format")).compute(ctx) //
                    : "iso";

                return switch (format) {
                    case "iso" -> period.compute(ctx).toString();
                    case "short" -> DurationPeriodFormatUtils.formatPeriodShort(period.compute(ctx));
                    case "long" -> DurationPeriodFormatUtils.formatPeriodLong(period.compute(ctx));
                    default -> throw new IllegalStateException(
                        "Unknown format: " + format + ". This should never happen.");
                };
            }, //
            ctx -> {
                if (anyMissing(args).applyAsBoolean(ctx)) {
                    return true;
                }

                var format = args.has("format") //
                    ? toString(args.get("format")).compute(ctx) //
                    : "iso";

                return !List.of("iso", "short", "long").contains(format);
            });
    }

    public static final ExpressionFunction FORMAT_DURATION = functionBuilder().name("format_duration") //
        .description("""
                The node formats Duration values into String values.
                """) //
        .examples("""
                * `format_duration(duration("PT12H"))` returns "PT12H"
                """) //
        .keywords("") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("duration", "Duration to format", isDurationOrOpt()), //
            optarg("format", "Format of the duration string. If not specified, use ISO.", isString()) //
        ) //
        .returnType("A string", "String", args -> STRING(anyOptional(args))) //
        .impl(DateTimeFunctions::formatDuration) //
        .build();

    private static Computer formatDuration(final Arguments<Computer> args) {
        var duration = toDuration(args.get("duration"));

        return StringComputer.of(ctx -> {
            var format = args.has("format") //
                ? toString(args.get("format")).compute(ctx) //
                : "iso";

            return switch (format) {
                case "iso" -> duration.compute(ctx).toString();
                case "short" -> DurationPeriodFormatUtils.formatDurationShort(duration.compute(ctx));
                case "long" -> DurationPeriodFormatUtils.formatDurationLong(duration.compute(ctx));
                default -> throw new IllegalStateException("Unknown format: " + format + ". This should never happen.");
            };
        }, ctx -> {
            if (anyMissing(args).applyAsBoolean(ctx)) {
                return true;
            }

            var format = args.has("format") //
                ? toString(args.get("format")).compute(ctx) //
                : "iso";

            return !List.of("iso", "short", "long").contains(format);
        });
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

    private static Computer defaultTimeRoundStrategyComputer =
        StringComputer.of(ctx -> TimeRoundNodeSettings.TimeRoundingStrategy.FIRST_POINT_IN_TIME.name(), ctx -> false);

    private static Computer defaultDateRoundStrategyComputer =
        StringComputer.of(ctx -> DateRoundNodeSettings.DateRoundingStrategy.FIRST.name(), ctx -> false);

    private static Computer defaultTimeShiftModeComputer =
        StringComputer.of(ctx -> DateRoundNodeSettings.ShiftMode.THIS.name(), ctx -> false);

    private static Computer defaultDayOrWWeekdayComputer =
        StringComputer.of(ctx -> DateRoundNodeSettings.DayOrWeekday.DAY.name(), ctx -> false);

    private static Computer zeroDurationComputer = DurationComputer.of(ctx -> Duration.ZERO, ctx -> false);

    private static Computer oneHourDurationComputer = DurationComputer.of(ctx -> Duration.ofHours(1), ctx -> false);

    private static Computer defaultDatePrecisionComputer =
        StringComputer.of(ctx -> RoundDatePrecision.MONTH.name(), ctx -> false);

    /**
     * Helper function to round time-based temporal values. Assumption: The arguments 'strategy' and 'precision' are
     * present, where precision is a Duration.
     *
     * @param args arguments to the function
     * @param temporalArgumentName name of the temporal argument to round
     * @param ctx evaluation context
     * @return the rounded temporal value
     */
    private static final Temporal roundTimeBasedTemporal(final Arguments<Computer> args,
        final String temporalArgumentName, final EvaluationContext ctx) {
        var temporalComputer = args.get(temporalArgumentName);
        var strategyComputer = toString(args.get("strategy", defaultTimeRoundStrategyComputer));
        var precisionComputer = toDuration(args.get("precision", oneHourDurationComputer));

        var timeToRound = Computer.computeTemporal(temporalComputer, ctx);
        var strategy = TimeRoundNodeSettings.TimeRoundingStrategy.valueOf(strategyComputer.compute(ctx));
        var precision = precisionComputer.compute(ctx);

        return TimeRoundingUtil.roundTimeBasedTemporal(timeToRound, strategy, precision);
    }

    /**
     * Helper function to round time-based temporal values. Assumption: The arguments 'strategy' and 'precision' are
     * present, where precision is a Period.
     *
     * @param args arguments to the function
     * @param temporalArgumentName name of the temporal argument to round
     * @param ctx evaluation context
     * @return true if the temporal argument is missing or if the strategy or precision is missing
     */
    private static final Temporal roundDateBasedTemporal(final Arguments<Computer> args,
        final String temporalArgumentName, final EvaluationContext ctx) {

        var temporalComputer = args.get(temporalArgumentName);
        var strategyComputer = toString(args.get("strategy", defaultDateRoundStrategyComputer));
        var precisionComputer = toString(args.get("precision", defaultDatePrecisionComputer));
        var shiftModeComputer = toString(args.get("shift_mode", defaultTimeShiftModeComputer));
        var dayOrWeekdayComputer = toString(args.get("dayOrWeekday", defaultDayOrWWeekdayComputer));

        var dateToRound = Computer.computeTemporal(temporalComputer, ctx);
        var strategy = DateRoundNodeSettings.DateRoundingStrategy.valueOf(strategyComputer.compute(ctx));
        var precision = DateRoundNodeSettings.RoundDatePrecision.valueOf(precisionComputer.compute(ctx));
        var shiftMode = DateRoundNodeSettings.ShiftMode.valueOf(shiftModeComputer.compute(ctx));
        var dayOrWeekday = DateRoundNodeSettings.DayOrWeekday.valueOf(dayOrWeekdayComputer.compute(ctx));

        return DateRoundingUtil.roundDateBasedTemporal(dateToRound, strategy, precision, shiftMode, dayOrWeekday);
    }

    /**
     * Helper function to determine if the temporal argument is missing or if the strategy or precision is missing.
     *
     * @param args arguments to the function
     * @param temporalArgumentName name of the temporal argument to round
     * @param ctx evaluation context
     * @return true if the temporal argument is missing or if the strategy or precision is missing
     */
    private static final boolean roundTemporalIsMissing(final Arguments<Computer> args,
        final String temporalArgumentName, final EvaluationContext ctx) {
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
}