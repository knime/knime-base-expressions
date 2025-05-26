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

import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_BOOLEAN;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_ZONED_DATE_TIME_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.isStringOrOpt;
import static org.knime.core.expressions.SignatureUtils.isZonedDateTimeOrOpt;
import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * Implementation of built-in functions for manipulating time zones.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
public final class TemporalZoneManipulationFunctions {

    private static final String FIRST_ARG = "first";

    private static final String SECOND_ARG = "second";

    private static final String ZONED_ARG = "zoned";

    private static final String ZONE_ARG = "zone";

    private TemporalZoneManipulationFunctions() {
    }

    /** The Temporal - Zone Manipulation category */
    public static final OperatorCategory CATEGORY_ZONE_MANIPULATION =
        new OperatorCategory(TemporalFunctionUtils.TEMPORAL_META_CATEGORY_NAME, "Zone Manipulation",
            "Functions for manipulating time zones.");

    public static final ExpressionFunction CONVERT_TO_ZONE = functionBuilder() //
        .name("convert_to_zone") //
        .description("""
                Changes the time zone of a `ZONED_DATE_TIME` value to the provided
                time zone ID, modifying the wall time (and potentially also the date)
                if necessary to keep the instant represented the same.

                Note: the wall time is the time that would be displayed on a clock. For the example
                `1970-01-01T12:34:56[Europe/Paris]`, the wall time is `12:34:56`.

                If the provided value is missing, the function returns `MISSING`.
                If the provided zone ID is invalid, the function returns `MISSING`
                and a warning is emitted. If it is impossible to convert the zone, e.g. because
                the resulting date-time would exceed the representable range, the node execution fails with an error.

                The provided zone is in the IANA time zone format (e.g. `Europe/Berlin` or `UTC`) and is \
                case insensitive. See [here](%s) for a list of valid time zones. Alternatively, a zone offset \
                can be provided (e.g. `+02:00`, `-5`, `UTC+07:15`, `GMT-3`, etc.).
                """.formatted(TemporalFunctionUtils.URL_TIMEZONE_LIST)) //
        .examples("""
                In these examples, the input value is the `ZONED_DATE_TIME` `1970-01-01T00:00:00Z`.

                * `change_zone($["input"], "Europe/Paris")`
                  returns `1970-01-01T00:00:00+01:00[Europe/Paris]`
                * `change_zone($["input"], "America/New_York")`
                  returns `1969-12-31T19:00:00-05:00[America/New_York]`
                """) //
        .keywords("change", "zone", "timezone") //
        .category(CATEGORY_ZONE_MANIPULATION) //
        .args( //
            arg(ZONED_ARG, "The `ZONED_DATE_TIME` value to change the zone of.", isZonedDateTimeOrOpt()), //
            arg(ZONE_ARG, "The ID of the new time zone.", isStringOrOpt()) //
        ) //
        .returnType("The input value with the time zone changed", RETURN_ZONED_DATE_TIME_MISSING,
            args -> OPT_ZONED_DATE_TIME) //
        .impl(TemporalZoneManipulationFunctions::convertZoneImpl) //
        .build();

    private static Computer convertZoneImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<ZonedDateTime>> valueSupplier = ctx -> {
            var zoneIdName = ((StringComputer)args.get(ZONE_ARG)).compute(ctx);

            var zoneId = TemporalFunctionUtils.parseZoneIdCaseInsensitive(zoneIdName);
            if (zoneId.isEmpty()) {
                ctx.addWarning("Invalid time zone ID: %s.".formatted(zoneIdName));
                return Optional.empty();
            }

            var zoned = ((ZonedDateTimeComputer)args.get(ZONED_ARG)).compute(ctx);

            try {
                return Optional.of(zoned.withZoneSameInstant(zoneId.get()));
            } catch (DateTimeException ex) {
                // means that we just exceeded the supported range
                throw new ExpressionEvaluationException(
                    "Adjusting the zone of this ZONED_DATE_TIME would cause it to exceed the representable range.", ex);
            }
        };

        return ZonedDateTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction REPLACE_ZONE = functionBuilder() //
        .name("replace_zone") //
        .description("""
                Changes the time zone of a `ZONED_DATE_TIME` value to the provided
                time zone ID, keeping the wall time and date the same. There are some rare instances
                where the time will change, for example when changing close to daylight saving
                transitions where the target time does not exist.

                Note: the wall time is the time that would be displayed on a clock. For the example
                `1970-01-01T12:34:56[Europe/Paris]`, the wall time is `12:34:56`.

                If the provided value is missing, the function returns `MISSING`.
                If the provided zone ID is invalid, the function returns `MISSING`
                and a warning is emitted. If it is impossible to convert the zone, e.g. because
                the resulting date-time would exceed the representable range, the node execution fails with an error.

                The provided zone is in the IANA time zone format (e.g. `Europe/Berlin` or `UTC`) and is \
                case insensitive. See [here](%s) for a list of valid time zones. Alternatively, a zone offset \
                can be provided (e.g. `+02:00`, `-5`, `UTC+07:15`, `GMT-3`, etc.).
                """) //
        .examples("""
                In these examples, the input value is the `ZONED_DATE_TIME` `1970-01-01T00:00:00Z`.

                * `replace_zone($["input"], "Europe/Paris")`
                  returns `1970-01-01T00:00:00+01:00[Europe/Paris]`
                  * `replace_zone($["input"], "America/New_York")`
                  returns `1970-01-01T00:00:00-05:00[America/New_York]`
                """) //
        .keywords("replace", "zone", "timezone") //
        .category(CATEGORY_ZONE_MANIPULATION) //
        .args( //
            arg(ZONED_ARG, "The `ZONED_DATE_TIME` value to change the zone of.", isZonedDateTimeOrOpt()), //
            arg(ZONE_ARG, "The ID of the new time zone.", isStringOrOpt()) //
        ) //
        .returnType("The input value with the time zone changed", RETURN_ZONED_DATE_TIME_MISSING,
            args -> OPT_ZONED_DATE_TIME) //
        .impl(TemporalZoneManipulationFunctions::replaceZoneImpl) //
        .build();

    private static Computer replaceZoneImpl(final Arguments<Computer> args) {
        ComputerResultSupplier<Optional<ZonedDateTime>> valueSupplier = ctx -> {
            var zoneIdName = ((StringComputer)args.get(ZONE_ARG)).compute(ctx);

            var zoneId = TemporalFunctionUtils.parseZoneIdCaseInsensitive(zoneIdName);
            if (zoneId.isEmpty()) {
                ctx.addWarning("Invalid time zone ID: %s.".formatted(zoneIdName));
                return Optional.empty();
            }

            var zoned = ((ZonedDateTimeComputer)args.get(ZONED_ARG)).compute(ctx);

            return Optional.of(zoned.withZoneSameLocal(zoneId.get()));
        };

        return ZonedDateTimeComputer.of( //
            ctx -> valueSupplier.apply(ctx).get(), //
            ctx -> anyMissing(args).applyAsBoolean(ctx) || valueSupplier.apply(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction IS_SAME_INSTANT = functionBuilder() //
        .name("is_same_instant") //
        .description("""
                Check if two `ZONED_DATE_TIME` values represent the same instant in time.

                If:
                * Both inputs are missing, the function returns `true`.
                * One input is missing, the function returns `false`.
                * The inputs are both non-missing and represent the same instant, the function returns `true`.
                * The inputs are both non-missing and represent different instants, the function returns `false`.
                """) //
        .examples("""
                * `is_same_instant(parse_zoned("1970-01-01T00:00:00Z"), parse_zoned("1970-01-01T01:00:00+01:00"))`
                  returns `true`
                * `is_same_instant(parse_zoned("1970-01-01T00:00:00Z"), parse_zoned("1970-01-01T01:00:00Z"))`
                    returns `false`
                """) //
        .keywords("zoned", "equality", "equals") //
        .category(CATEGORY_ZONE_MANIPULATION) //
        .args( //
            arg(FIRST_ARG, "The first `ZONED_DATE_TIME` value to compare.", isZonedDateTimeOrOpt()), //
            arg(SECOND_ARG, "The second `ZONED_DATE_TIME` value to compare.", isZonedDateTimeOrOpt()) //
        ) //
        .returnType("A boolean indicating if the two inputs represent the same instant", RETURN_BOOLEAN,
            args -> BOOLEAN) //
        .impl(TemporalZoneManipulationFunctions::hasSameInstantImpl) //
        .build();

    private static Computer hasSameInstantImpl(final Arguments<Computer> args) {
        var firstArg = args.get(FIRST_ARG);
        var secondArg = args.get(SECOND_ARG);

        BooleanComputerResultSupplier value = ctx -> {
            var firstMissing = firstArg.isMissing(ctx);
            var secondMissing = secondArg.isMissing(ctx);

            if (firstMissing && secondMissing) {
                return true;
            } else if (firstMissing || secondMissing) {
                return false;
            } else {
                var first = ((ZonedDateTimeComputer)firstArg).compute(ctx);
                var second = ((ZonedDateTimeComputer)secondArg).compute(ctx);

                // Note: not .equals
                return first.isEqual(second);
            }
        };

        return BooleanComputer.of(value, ctx -> false);
    }
}
