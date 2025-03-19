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
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misDuration;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misInteger;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalDate;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalDateTime;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalTime;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misString;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misZonedDateTime;

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
 * Tests for {@link TemporalCreateExtractFunctions}
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class TemporalCreateExtractFunctionTests {

    private static final LocalDate TEST_DATE = LocalDate.of(2001, 2, 3);

    private static final LocalTime TEST_TIME = LocalTime.of(12, 34, 56);

    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(TEST_DATE, TEST_TIME);

    private static final ZonedDateTime TEST_ZONED_ID = ZonedDateTime.of(TEST_DATE_TIME, ZoneId.of("Europe/Paris"));

    private static final Duration TEST_DURATION = Duration.ofHours(1).plusMinutes(2).plusSeconds(3);

    private static final Period TEST_PERIOD = Period.of(1, 2, 3);

    @TestFactory
    List<DynamicNode> makeDate() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.MAKE_DATE) //
            .typing("INTEGER x 3", List.of(INTEGER, INTEGER, INTEGER), OPT_LOCAL_DATE) //
            .typing("OPT INTEGER x 3", List.of(OPT_INTEGER, OPT_INTEGER, OPT_INTEGER), OPT_LOCAL_DATE) //
            .typing("mix of opt and non-opt", List.of(OPT_INTEGER, INTEGER, OPT_INTEGER), OPT_LOCAL_DATE) //
            .illegalArgs("1st arg not INTEGER", List.of(STRING, INTEGER, INTEGER)) //
            .illegalArgs("2nd arg not INTEGER", List.of(INTEGER, STRING, INTEGER)) //
            .illegalArgs("3rd arg not INTEGER", List.of(INTEGER, INTEGER, STRING)) //
            .impl("valid date", List.of(arg(2001), arg(2), arg(3)), TEST_DATE) //
            .missingAndWarns("invalid date", List.of(arg(2001), arg(2), arg(30))) //
            .missingAndWarns("invalid month", List.of(arg(2001), arg(13), arg(1))) //
            .missingAndWarns("negative date", List.of(arg(2001), arg(1), arg(-1))) //
            .missingAndWarns("negative month", List.of(arg(2001), arg(-1), arg(1))) //
            .errors("overflow", List.of(arg(1L + Integer.MAX_VALUE), arg(1), arg(1)), ".*overflow.*") //
            .impl("missing argument", List.of(misInteger(), arg(2), arg(3))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> makeTime() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.MAKE_TIME) //
            .typing("INTEGER x 2", List.of(INTEGER, INTEGER), OPT_LOCAL_TIME) //
            .typing("INTEGER x 3", List.of(INTEGER, INTEGER, INTEGER), OPT_LOCAL_TIME) //
            .typing("OPT INTEGER x 3", List.of(OPT_INTEGER, OPT_INTEGER, OPT_INTEGER), OPT_LOCAL_TIME) //
            .typing("INTEGER x 4", List.of(INTEGER, INTEGER, INTEGER, INTEGER), OPT_LOCAL_TIME) //
            .typing("mix of opt and non-opt", List.of(OPT_INTEGER, INTEGER, OPT_INTEGER), OPT_LOCAL_TIME) //
            .illegalArgs("1st arg not INTEGER", List.of(STRING, INTEGER, INTEGER)) //
            .illegalArgs("2nd arg not INTEGER", List.of(INTEGER, STRING, INTEGER)) //
            .illegalArgs("3rd arg not INTEGER", List.of(INTEGER, INTEGER, STRING)) //
            .illegalArgs("4th arg not INTEGER", List.of(INTEGER, INTEGER, INTEGER, STRING)) //
            .impl("valid time", List.of(arg(12), arg(34)), TEST_TIME.withSecond(0)) //
            .impl("valid time with seconds", List.of(arg(12), arg(34), arg(56)), TEST_TIME) //
            .impl("valid time with nanos", List.of(arg(12), arg(34), arg(56), arg(78)), TEST_TIME.withNano(78)) //
            .impl("missing nanos default to 0", List.of(arg(12), arg(34), arg(56), misInteger()), TEST_TIME) //
            .impl("missing seconds default to 0", List.of(arg(12), arg(34), misInteger()), TEST_TIME.withSecond(0)) //
            .missingAndWarns("invalid second", List.of(arg(12), arg(34), arg(60))) //
            .missingAndWarns("invalid minute", List.of(arg(12), arg(60), arg(56))) //
            .missingAndWarns("invalid hour", List.of(arg(24), arg(34), arg(56))) //
            .missingAndWarns("invalid nano", List.of(arg(12), arg(34), arg(56), arg(1_000_000_000))) //
            .missingAndWarns("negative second", List.of(arg(12), arg(34), arg(-1))) //
            .missingAndWarns("negative minute", List.of(arg(12), arg(-1), arg(56))) //
            .missingAndWarns("negative hour", List.of(arg(-1), arg(34), arg(56))) //
            .missingAndWarns("negative nano", List.of(arg(12), arg(34), arg(56), arg(-1))) //
            .errors("overflow", List.of(arg(1L + Integer.MAX_VALUE), arg(1), arg(1)), ".*overflow.*") //
            .impl("missing argument", List.of(misInteger(), arg(34), arg(56))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> makeDateTime() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.MAKE_DATETIME) //
            .typing("LOCAL_DATE, LOCAL_TIME", List.of(LOCAL_DATE, LOCAL_TIME), LOCAL_DATE_TIME) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_TIME), OPT_LOCAL_DATE_TIME) //
            .typing("First opt, second not", List.of(OPT_LOCAL_DATE, LOCAL_TIME), OPT_LOCAL_DATE_TIME) //
            .typing("Second opt, first not", List.of(LOCAL_DATE, OPT_LOCAL_TIME), OPT_LOCAL_DATE_TIME) //
            .illegalArgs("1st arg not LOCAL_DATE", List.of(STRING, LOCAL_TIME)) //
            .illegalArgs("2nd arg not LOCAL_TIME", List.of(LOCAL_DATE, STRING)) //
            .impl("valid datetime", List.of(arg(TEST_DATE), arg(TEST_TIME)), TEST_DATE_TIME) //
            .impl("missing date", List.of(misLocalDate(), arg(TEST_TIME))) //
            .impl("missing time", List.of(arg(TEST_DATE), misLocalTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> makeZonedDateTime() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.MAKE_ZONED) //
            .typing("LOCAL_DATE_TIME, STRING", List.of(LOCAL_DATE_TIME, STRING), OPT_ZONED_DATE_TIME) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_STRING), OPT_ZONED_DATE_TIME) //
            .typing("First opt, second not", List.of(OPT_LOCAL_DATE_TIME, STRING), OPT_ZONED_DATE_TIME) //
            .typing("Second opt, first not", List.of(LOCAL_DATE_TIME, OPT_STRING), OPT_ZONED_DATE_TIME) //
            .illegalArgs("1st arg not LOCAL_DATE_TIME", List.of(STRING, STRING)) //
            .illegalArgs("2nd arg not STRING", List.of(LOCAL_DATE_TIME, LOCAL_TIME)) //
            .impl("valid zoned datetime", List.of(arg(TEST_DATE_TIME), arg("Europe/Paris")), TEST_ZONED_ID) //
            .impl("missing datetime", List.of(misLocalDateTime(), arg("Europe/Paris"))) //
            .impl("missing zone", List.of(arg(TEST_DATE_TIME), misString())) //
            .missingAndWarns("invalid zone", List.of(arg(TEST_DATE_TIME), arg("invalid"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> makeTimeDuration() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.MAKE_TIME_DURATION) //
            .typing("INTEGER x 3", List.of(INTEGER, INTEGER, INTEGER), TIME_DURATION) //
            .typing("OPT INTEGER x 3", List.of(OPT_INTEGER, OPT_INTEGER, OPT_INTEGER), OPT_TIME_DURATION) //
            .typing("mix of opt and non-opt", List.of(OPT_INTEGER, INTEGER, OPT_INTEGER), OPT_TIME_DURATION) //
            .illegalArgs("1st arg not INTEGER", List.of(STRING, INTEGER, INTEGER)) //
            .illegalArgs("2nd arg not INTEGER", List.of(INTEGER, STRING, INTEGER)) //
            .illegalArgs("3rd arg not INTEGER", List.of(INTEGER, INTEGER, STRING)) //
            .impl("valid duration", List.of(arg(1), arg(2), arg(3)), TEST_DURATION) //
            .impl("missing argument", List.of(misInteger(), arg(2), arg(3))) //
            .errors("duration overflow", List.of(arg(Long.MAX_VALUE), arg(1), arg(1)), ".*too large.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> makeDateDuration() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.MAKE_DATE_DURATION) //
            .typing("INTEGER x 3", List.of(INTEGER, INTEGER, INTEGER), DATE_DURATION) //
            .typing("OPT INTEGER x 3", List.of(OPT_INTEGER, OPT_INTEGER, OPT_INTEGER), OPT_DATE_DURATION) //
            .typing("mix of opt and non-opt", List.of(OPT_INTEGER, INTEGER, OPT_INTEGER), OPT_DATE_DURATION) //
            .illegalArgs("1st arg not INTEGER", List.of(STRING, INTEGER, INTEGER)) //
            .illegalArgs("2nd arg not INTEGER", List.of(INTEGER, STRING, INTEGER)) //
            .illegalArgs("3rd arg not INTEGER", List.of(INTEGER, INTEGER, STRING)) //
            .impl("valid period", List.of(arg(1), arg(2), arg(3)), TEST_PERIOD) //
            .impl("missing argument", List.of(misInteger(), arg(2), arg(3))) //
            .errors("period overflow", List.of(arg((long)Integer.MAX_VALUE + 1), arg(0), arg(0)), ".*too large.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractYear() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_YEAR) //
            .typing("LOCAL_DATE", List.of(LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE", List.of(OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME)) //
            .impl("valid date", List.of(arg(TEST_DATE)), 2001) //
            .impl("missing date", List.of(misLocalDate())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractMonth() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_MONTH) //
            .typing("LOCAL_DATE", List.of(LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE", List.of(OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME)) //
            .impl("valid date", List.of(arg(TEST_DATE)), 2) //
            .impl("missing date", List.of(misLocalDate())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractDayOfMonth() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_DAY_OF_MONTH) //
            .typing("LOCAL_DATE", List.of(LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE", List.of(OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME)) //
            .impl("valid date", List.of(arg(TEST_DATE)), 3) //
            .impl("missing date", List.of(misLocalDate())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractHour() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_HOUR) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME), INTEGER) //
            .typing("OPT LOCAL_TIME", List.of(OPT_LOCAL_TIME), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE)) //
            .impl("valid time", List.of(arg(TEST_TIME)), 12) //
            .impl("missing time", List.of(misLocalTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractMinute() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_MINUTE) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME), INTEGER) //
            .typing("OPT LOCAL_TIME", List.of(OPT_LOCAL_TIME), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE)) //
            .impl("valid time", List.of(arg(TEST_TIME)), 34) //
            .impl("missing time", List.of(misLocalTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractSecond() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_SECOND) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME), INTEGER) //
            .typing("OPT LOCAL_TIME", List.of(OPT_LOCAL_TIME), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE)) //
            .impl("valid time", List.of(arg(TEST_TIME)), 56) //
            .impl("missing time", List.of(misLocalTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractNanosecond() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_NANOSECOND) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME), INTEGER) //
            .typing("OPT LOCAL_TIME", List.of(OPT_LOCAL_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE)) //
            .impl("valid datetime", List.of(arg(TEST_DATE_TIME)), 0) //
            .impl("valid datetime", List.of(arg(TEST_DATE_TIME.plusNanos(123456789))), 123456789) //
            .impl("missing datetime", List.of(misLocalDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractDate() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_DATE) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), LOCAL_DATE) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_LOCAL_DATE) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), LOCAL_DATE) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_LOCAL_DATE) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME)) //
            .impl("valid datetime", List.of(arg(TEST_DATE_TIME)), TEST_DATE) //
            .impl("valid zoned", List.of(arg(TEST_ZONED_ID)), TEST_DATE) //
            .impl("missing datetime", List.of(misLocalDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractTime() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_TIME) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), LOCAL_TIME) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_LOCAL_TIME) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), LOCAL_TIME) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_LOCAL_TIME) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE)) //
            .impl("valid datetime", List.of(arg(TEST_DATE_TIME)), TEST_TIME) //
            .impl("valid zoned", List.of(arg(TEST_ZONED_ID)), TEST_TIME) //
            .impl("missing datetime", List.of(misLocalDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractDateTime() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.EXTRACT_DATETIME) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), LOCAL_DATE_TIME) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_LOCAL_DATE_TIME) //
            .illegalArgs("Not a zoned datetime", List.of(LOCAL_DATE)) //
            .impl("valid zoned", List.of(arg(TEST_ZONED_ID)), TEST_DATE_TIME) //
            .impl("missing zoned", List.of(misZonedDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toHours() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.TO_HOURS) //
            .typing("TIME_DURATION", List.of(TIME_DURATION), FLOAT) //
            .typing("OPT_TIME_DURATION", List.of(OPT_TIME_DURATION), OPT_FLOAT) //
            .illegalArgs("Not a duration", List.of(LOCAL_DATE)) //
            .impl("valid duration", List.of(arg(TEST_DURATION)), 1 + 2 / 60.0 + 3 / 3600.0) //
            .impl("missing duration", List.of(misDuration())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toMinutes() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.TO_MINUTES) //
            .typing("TIME_DURATION", List.of(TIME_DURATION), FLOAT) //
            .typing("OPT_TIME_DURATION", List.of(OPT_TIME_DURATION), OPT_FLOAT) //
            .illegalArgs("Not a duration", List.of(LOCAL_DATE)) //
            .implWithTolerance("valid duration", List.of(arg(TEST_DURATION)), 1 * 60.0 + 2 + 3 / 60.0) //
            .impl("missing duration", List.of(misDuration())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toSeconds() {
        return new FunctionTestBuilder(TemporalCreateExtractFunctions.TO_SECONDS) //
            .typing("TIME_DURATION", List.of(TIME_DURATION), FLOAT) //
            .typing("OPT_TIME_DURATION", List.of(OPT_TIME_DURATION), OPT_FLOAT) //
            .illegalArgs("Not a duration", List.of(LOCAL_DATE)) //
            .implWithTolerance("valid duration", List.of(arg(TEST_DURATION)), 1 * 3600.0 + 2 * 60.0 + 3) //
            .impl("missing duration", List.of(misDuration())) //
            .tests();
    }
}
