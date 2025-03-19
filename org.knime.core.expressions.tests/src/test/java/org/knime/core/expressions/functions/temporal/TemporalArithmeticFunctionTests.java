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
 *   Feb 26, 2025 (david): created
 */
package org.knime.core.expressions.functions.temporal;

import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalDate;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalTime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.knime.core.expressions.functions.FunctionTestBuilder;

/**
 * Tests for {@link TemporalArithmeticFunctions}
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class TemporalArithmeticFunctionTests {

    @TestFactory
    List<DynamicNode> dateDurationBetween() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.DATE_DURATION_BETWEEN) //
            .typing("LOCAL_DATE × 2", List.of(LOCAL_DATE, LOCAL_DATE), DATE_DURATION) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_DATE), OPT_DATE_DURATION) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), DATE_DURATION) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_DATE_DURATION) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), DATE_DURATION) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_DATE_DURATION) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, LOCAL_TIME)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_DATE, LOCAL_DATE_TIME)) //
            .impl("valid dates", List.of(arg(LocalDate.of(2001, 2, 3)), arg(LocalDate.of(2001, 3, 6))),
                Period.of(0, 1, 3)) //
            .impl("valid datetimes",
                List.of(arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6)), arg(LocalDateTime.of(2001, 3, 6, 7, 8, 9))),
                Period.of(0, 1, 3)) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC"))),
                    arg(ZonedDateTime.of(2001, 3, 6, 7, 8, 9, 10, ZoneId.of("Europe/Berlin")))),
                Period.of(0, 1, 3)) //
            .impl("zoned where timezone results in an extra day",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 23, 59, 59, 0, ZoneId.of("Pacific/Midway"))),
                    arg(ZonedDateTime.of(2001, 2, 3, 23, 59, 59, 0, ZoneId.of("Pacific/Kiritimati")))),
                Period.ZERO) //
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> timeDurationBetween() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.TIME_DURATION_BETWEEN) //
            .typing("LOCAL_TIME × 2", List.of(LOCAL_TIME, LOCAL_TIME), TIME_DURATION) //
            .typing("OPT LOCAL_TIME x 2", List.of(OPT_LOCAL_TIME, OPT_LOCAL_TIME), OPT_TIME_DURATION) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), TIME_DURATION) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_TIME_DURATION) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), TIME_DURATION) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_TIME_DURATION) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE, LOCAL_DATE)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_TIME, LOCAL_DATE_TIME)) //
            .impl("valid times", List.of(arg(LocalTime.of(1, 2, 3)), arg(LocalTime.of(4, 5, 6))),
                Duration.ofHours(3).plusMinutes(3).plusSeconds(3)) //
            .impl("valid datetimes",
                List.of(arg(LocalDateTime.of(2001, 2, 3, 1, 2, 3)), arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6))),
                Duration.ofHours(3).plusMinutes(3).plusSeconds(3)) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 1, 2, 3, 0, ZoneId.of("UTC"))),
                    arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 0, ZoneId.of("Europe/Berlin")))),
                Duration.ofHours(2).plusMinutes(3).plusSeconds(3)) //
            .impl("missing time", List.of(misLocalTime(), arg(LocalTime.of(4, 5, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> yearsBetween() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.YEARS_BETWEEN) //
            .typing("LOCAL_DATE × 2", List.of(LOCAL_DATE, LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, LOCAL_TIME)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_DATE, LOCAL_DATE_TIME)) //
            .impl("valid dates", List.of(arg(LocalDate.of(2001, 2, 3)), arg(LocalDate.of(2005, 3, 6))), 4) //
            .impl("negative", List.of(arg(LocalDate.of(2005, 3, 6)), arg(LocalDate.of(2001, 2, 3))), -4) //
            .impl("valid datetime",
                List.of(arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6)), arg(LocalDateTime.of(2002, 3, 6, 7, 8, 9))), 1) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC"))),
                    arg(ZonedDateTime.of(2002, 3, 6, 7, 8, 9, 10, ZoneId.of("Europe/Berlin")))),
                1) //
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> monthsBetween() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.MONTHS_BETWEEN) //
            .typing("LOCAL_DATE × 2", List.of(LOCAL_DATE, LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, LOCAL_TIME)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_DATE, LOCAL_DATE_TIME)) //
            .impl("valid dates", List.of(arg(LocalDate.of(2001, 2, 3)), arg(LocalDate.of(2001, 3, 6))), 1) //
            .impl("negative", List.of(arg(LocalDate.of(2001, 3, 6)), arg(LocalDate.of(2001, 2, 3))), -1) //
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .impl("valid datetime",
                List.of(arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6)), arg(LocalDateTime.of(2001, 3, 6, 7, 8, 9))), 1) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC"))),
                    arg(ZonedDateTime.of(2001, 3, 6, 7, 8, 9, 10, ZoneId.of("Europe/Berlin")))),
                1) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> daysBetween() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.DAYS_BETWEEN) //
            .typing("LOCAL_DATE × 2", List.of(LOCAL_DATE, LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, LOCAL_TIME)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_DATE, LOCAL_DATE_TIME)) //
            .impl("valid dates", List.of(arg(LocalDate.of(2001, 2, 3)), arg(LocalDate.of(2001, 3, 6))), 31) //
            .impl("valid datetime",
                List.of(arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6)), arg(LocalDateTime.of(2001, 3, 6, 7, 8, 9))), 31) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC"))),
                    arg(ZonedDateTime.of(2001, 3, 6, 7, 8, 9, 10, ZoneId.of("Europe/Berlin")))),
                31) //
            .impl("negative", List.of(arg(LocalDate.of(2001, 3, 6)), arg(LocalDate.of(2001, 2, 3))), -31) //
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> addDateDuration() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.ADD_DATE_DURATION) //
            .typing("LOCAL_DATE, DATE_DURATION", List.of(LOCAL_DATE, DATE_DURATION), LOCAL_DATE) //
            .typing("OPT LOCAL_DATE, OPT DATE_DURATION", List.of(OPT_LOCAL_DATE, OPT_DATE_DURATION), OPT_LOCAL_DATE) //
            .typing("LOCAL_DATE_TIME, DATE_DURATION", List.of(LOCAL_DATE_TIME, DATE_DURATION), LOCAL_DATE_TIME) //
            .typing("OPT LOCAL_DATE_TIME, OPT DATE_DURATION", List.of(OPT_LOCAL_DATE_TIME, OPT_DATE_DURATION),
                OPT_LOCAL_DATE_TIME) //
            .typing("ZONED_DATE_TIME, DATE_DURATION", List.of(ZONED_DATE_TIME, DATE_DURATION), ZONED_DATE_TIME) //
            .typing("OPT ZONED_DATE_TIME, OPT DATE_DURATION", List.of(OPT_ZONED_DATE_TIME, OPT_DATE_DURATION),
                OPT_ZONED_DATE_TIME) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, DATE_DURATION)) //
            .illegalArgs("Not a period", List.of(LOCAL_DATE, TIME_DURATION)) //
            .impl("valid date", List.of(arg(LocalDate.of(2001, 2, 3)), arg(Period.of(1, 2, 3))),
                LocalDate.of(2002, 4, 6)) //
            .impl("valid datetime", List.of(arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6)), arg(Period.of(1, 2, 3))),
                LocalDateTime.of(2002, 4, 6, 4, 5, 6)) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC"))), arg(Period.of(1, 2, 3))),
                ZonedDateTime.of(2002, 4, 6, 4, 5, 6, 7, ZoneId.of("UTC"))) //
            .impl("missing date", List.of(misLocalDate(), arg(Period.of(1, 2, 3)))) //
            .errors("overflow", List.of(arg(LocalDate.MAX), arg(Period.of(1, 0, 0))), ".*too large.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> addTimeDuration() {
        return new FunctionTestBuilder(TemporalArithmeticFunctions.ADD_TIME_DURATION) //
            .typing("LOCAL_TIME, TIME_DURATION", List.of(LOCAL_TIME, TIME_DURATION), LOCAL_TIME) //
            .typing("OPT LOCAL_TIME, OPT TIME_DURATION", List.of(OPT_LOCAL_TIME, OPT_TIME_DURATION), OPT_LOCAL_TIME) //
            .typing("LOCAL_DATE_TIME, TIME_DURATION", List.of(LOCAL_DATE_TIME, TIME_DURATION), LOCAL_DATE_TIME) //
            .typing("OPT LOCAL_DATE_TIME, OPT TIME_DURATION", List.of(OPT_LOCAL_DATE_TIME, OPT_TIME_DURATION),
                OPT_LOCAL_DATE_TIME) //
            .typing("ZONED_DATE_TIME, TIME_DURATION", List.of(ZONED_DATE_TIME, TIME_DURATION), ZONED_DATE_TIME) //
            .typing("OPT ZONED_DATE_TIME, OPT TIME_DURATION", List.of(OPT_ZONED_DATE_TIME, OPT_TIME_DURATION),
                OPT_ZONED_DATE_TIME) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE, TIME_DURATION)) //
            .illegalArgs("Not a duration", List.of(LOCAL_TIME, DATE_DURATION)) //
            .impl("valid time",
                List.of(arg(LocalTime.of(1, 2, 3)), arg(Duration.ofHours(1).plusMinutes(2).plusSeconds(3))),
                LocalTime.of(2, 4, 6)) //
            .impl("valid datetime",
                List.of(arg(LocalDateTime.of(2001, 2, 3, 4, 5, 6)),
                    arg(Duration.ofHours(1).plusMinutes(2).plusSeconds(3))),
                LocalDateTime.of(2001, 2, 3, 5, 7, 9)) //
            .impl("valid zoned",
                List.of(arg(ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneId.of("UTC"))),
                    arg(Duration.ofHours(1).plusMinutes(2).plusSeconds(3))),
                ZonedDateTime.of(2001, 2, 3, 5, 7, 9, 7, ZoneId.of("UTC"))) //
            .impl("missing time", List.of(misLocalTime(), arg(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)))) //
            .errors("overflow", List.of(arg(LocalDateTime.MAX), arg(Duration.ofHours(1))), ".*too large.*") //
            .tests();
    }
}
