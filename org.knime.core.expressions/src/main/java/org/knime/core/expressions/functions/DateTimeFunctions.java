package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_LOCAL_TIME;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasTimePartOrIsOpt;
import static org.knime.core.expressions.SignatureUtils.isDurationOrOpt;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.time.LocalTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.DurationComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.OperatorCategory;
import org.knime.time.node.manipulate.datetimeround.TimeRoundingUtil;

/**
 * Implementation of built-in functions that manipulate date and times.
 *
 * @author Tobias Kampmann, TNG
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
            arg("precision", "Precision to round to", isDurationOrOpt()), //
            arg("strategy", "Rounding strategy: 'FIRST', 'NEAREST', 'LAST'", isStringOrOpt()) //)
        ) //
        .returnType("A local time", RETURN_LOCAL_TIME, args -> LOCAL_TIME(false))//
        .impl(DateTimeFunctions::roundTime) //
        .build();

    private static Computer roundTime(final Arguments<Computer> args) {
        var timeArgument = args.get("time");
        if (timeArgument instanceof LocalTimeComputer timeComputer) {
            return LocalTimeComputer.of(ctx -> {
                var timeToRound = timeComputer.compute(ctx);
                var strategyString = toString(args.get("strategy")).compute(ctx);
                var strategy
                return TimeRoundingUtil.roundTimeBasedTemporal(timeToRound,
                    toDuration(args.get("precision")).compute(ctx), toString(args.get("strategy")).compute(ctx));
            }, ctx -> false);
        }

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
}