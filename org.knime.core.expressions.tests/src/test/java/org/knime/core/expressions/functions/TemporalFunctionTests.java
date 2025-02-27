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
package org.knime.core.expressions.functions;

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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests for {@link TemporalFunctions}
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class TemporalFunctionTests {

    private static final LocalDate TEST_DATE = LocalDate.of(2001, 2, 3);

    private static final LocalTime TEST_TIME = LocalTime.of(12, 34, 56);

    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(TEST_DATE, TEST_TIME);

    private static final ZonedDateTime TEST_ZONED_ID = ZonedDateTime.of(TEST_DATE_TIME, ZoneId.of("Europe/Paris"));

    private static final ZonedDateTime TEST_ZONED_OFFSET =
        ZonedDateTime.of(TEST_DATE_TIME, ZoneId.ofOffset("", ZoneOffset.ofHours(1)));

    private static final Duration TEST_DURATION = Duration.ofHours(1).plusMinutes(2).plusSeconds(3);

    private static final Period TEST_PERIOD = Period.of(1, 2, 3);

    @TestFactory
    List<DynamicNode> parseDate() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_DATE) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_DATE) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_DATE) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_DATE) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid date", List.of(arg("invalid"), arg("yyyy-MM-dd"))) //
            .impl("default format", List.of(arg("2001-02-03")), TEST_DATE) //
            .impl("custom format", List.of(arg("03/02/2001"), arg("dd/MM/yyyy")), TEST_DATE) //
            .impl("missing date", List.of(misString())) //
            .impl("overspecified format", List.of(arg("2001-02-03 12:00"), arg("yyyy-MM-dd HH:mm")), TEST_DATE) //
            .errors("invalid format", List.of(arg("2001-02-03"), arg("invalid")), "(?i).*?unparseable.*") //
            .errors("underspecified format", List.of(arg("2001-02-03"), arg("yyyy")), "(?i).*?underspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseTime() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_TIME) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_TIME) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid time", List.of(arg("invalid"), arg("HH:mm:ss"))) //
            .impl("default format", List.of(arg("12:34:56")), TEST_TIME) //
            .impl("custom format", List.of(arg("12,34,56"), arg("HH,mm,ss")), TEST_TIME) //
            .impl("missing time", List.of(misString())) //
            .impl("overspecified format", List.of(arg("2025 12:34:56"), arg("yyyy HH:mm:ss")), TEST_TIME) //
            .errors("invalid format", List.of(arg("12:34:56"), arg("invalid")), "(?i).*?unparseable.*") //
            .errors("underspecified format", List.of(arg("12:34:56"), arg("mm")), "(?i).*?underspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseDateTime() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_DATE_TIME) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_DATE_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_DATE_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_DATE_TIME) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid datetime", List.of(arg("invalid"), arg("yyyy-MM-dd'T'HH:mm:ss"))) //
            .impl("default format", List.of(arg("2001-02-03T12:34:56")), TEST_DATE_TIME) //
            .impl("custom format", List.of(arg("03/02/2001 12:34:56"), arg("dd/MM/yyyy HH:mm:ss")), TEST_DATE_TIME) //
            .impl("missing datetime", List.of(misString())) //
            .impl("overspecified format",
                List.of(arg("2001-02-03T12:34:56+01:00[Europe/Berlin]"), arg("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")),
                TEST_DATE_TIME) //
            .errors("invalid format", List.of(arg("2001-02-03T12:34:56"), arg("invalid")), "(?i).*?unparseable.*") //
            .errors("underspecified format", List.of(arg("2001-02-03T12:34:56"), arg("yyyy")),
                "(?i).*?underspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseZonedDateTime() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_ZONED_DATE_TIME) //
            .typing("STRING", List.of(STRING), OPT_ZONED_DATE_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_ZONED_DATE_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_ZONED_DATE_TIME) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid datetime", List.of(arg("invalid"), arg("yyyy-MM-dd'T'HH:mm:ssXXX"))) //
            .impl("default format", List.of(arg("2001-02-03T12:34:56+01:00")), TEST_ZONED_OFFSET) //
            .impl("custom format", List.of(arg("03/02/2001 12:34:56 Europe/Paris"), arg("dd/MM/yyyy HH:mm:ss VV")),
                TEST_ZONED_ID) //
            .impl("missing datetime", List.of(misString())) //
            .errors("invalid format", List.of(arg("2001-02-03T12:34:56+01:00"), arg("invalid")), "(?i).*?unparseable.*") //
            .errors("underspecified format", List.of(arg("2001-02-03T12:34:56+01:00"), arg("yyyy")),
                "(?i).*?underspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalDate() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_DATE) //
            .typing("LOCAL_DATE", List.of(LOCAL_DATE, STRING), STRING) //
            .typing("OPT_LOCAL_DATE", List.of(OPT_LOCAL_DATE, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_DATE)) //
            .illegalArgs("1st arg not LOCAL_DATE", List.of(INTEGER, STRING)) //
            .impl("LOCAL_DATE", List.of(arg(TEST_DATE), arg("yyyy-MM-dd")), "2001-02-03") //
            .impl("missing local date", List.of(misLocalDate(), arg("yyyy-MM-dd"))) //
            .impl("underspecified format", List.of(arg(TEST_DATE), arg("yyyy")), "2001") //
            .errors("invalid format", List.of(arg(TEST_DATE), arg("invalid")), "(?i).*?unparseable.*") //
            .errors("overspecified format", List.of(arg(TEST_DATE), arg("yyyy-MM-dd HH:mm:ss")),
                "(?i).*?overspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalTime() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_TIME) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME, STRING), STRING) //
            .typing("OPT_LOCAL_TIME", List.of(OPT_LOCAL_TIME, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_TIME)) //
            .illegalArgs("1st arg not LOCAL_TIME", List.of(INTEGER, STRING)) //
            .impl("LOCAL_TIME", List.of(arg(TEST_TIME), arg("HH:mm:ss")), "12:34:56") //
            .impl("missing local time", List.of(misLocalTime(), arg("HH:mm:ss"))) //
            .impl("underspecified format", List.of(arg(TEST_TIME), arg("HH")), "12") //
            .errors("invalid format", List.of(arg(TEST_TIME), arg("invalid")), "(?i).*?unparseable.*") //
            .errors("overspecified format", List.of(arg(TEST_TIME), arg("yyyy-MM-dd HH:mm:ss")),
                "(?i).*?overspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalDateTime() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_DATE_TIME) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME, STRING), STRING) //
            .typing("OPT_LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME, STRING), OPT_STRING) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME, STRING), STRING) //
            .typing("OPT_ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(ZONED_DATE_TIME)) //
            .illegalArgs("1st arg not ZONED_DATE_TIME", List.of(INTEGER, STRING)) //
            .illegalArgs("Missing format", List.of(LOCAL_DATE_TIME)) //
            .illegalArgs("1st arg not LOCAL_DATE_TIME", List.of(INTEGER, STRING)) //
            .impl("LOCAL_DATE_TIME", List.of(arg(TEST_DATE_TIME), arg("yyyy-MM-dd'T'HH:mm:ss")), "2001-02-03T12:34:56") //
            .impl("missing local datetime", List.of(misLocalDateTime(), arg("yyyy-MM-dd'T'HH:mm:ss"))) //
            .impl("ZONED_DATE_TIME", List.of(arg(TEST_ZONED_ID), arg("yyyy-MM-dd'T'HH:mm:ssVV")),
                "2001-02-03T12:34:56Europe/Paris") //
            .impl("underspecified format (local)", List.of(arg(TEST_DATE_TIME), arg("yyyy")), "2001") //
            .impl("underspecified format (zoned)", List.of(arg(TEST_ZONED_ID), arg("yyyy")), "2001") //
            .errors("overspecified format (local)", List.of(arg(TEST_DATE_TIME), arg("ZZ")), "(?i).*?overspecified.*") //
            // no test for overspecified zoned format as it is not possible to overspecify
            .impl("missing zoned datetime", List.of(misZonedDateTime(), arg("yyyy-MM-dd'T'HH:mm:ssVV"))) //
            .errors("invalid format", List.of(arg(TEST_DATE_TIME), arg("invalid")), "(?i).*?unparseable.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseTimeDuration() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_TIME_DURATION) //
            .typing("STRING", List.of(STRING), OPT_TIME_DURATION) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_TIME_DURATION) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_TIME_DURATION) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .errors("2nd arg not a valid format", List.of(arg("P1DT2H3M4S"), arg("foobar")),
                "(?i)unknown format.*?foobar.*") //
            .missingAndWarns("1st arg not a valid duration", List.of(arg("invalid"), arg("iso"))) //
            .impl("default format", List.of(arg("PT1H2M3S")), TEST_DURATION) //
            .impl("iso format", List.of(arg("PT1H2M3S"), arg("iso")), TEST_DURATION) //
            .impl("long format", List.of(arg("1 hour 2 minutes 3 seconds"), arg("long")), TEST_DURATION) //
            .impl("short format", List.of(arg("1h2m3s"), arg("short")), TEST_DURATION) //
            .missingAndWarns("long format but short duration", List.of(arg("1h"), arg("long"))) //
            .missingAndWarns("short format but long duration", List.of(arg("1 hour"), arg("short"))) //
            .missingAndWarns("iso format but short duration", List.of(arg("1h"), arg("iso"))) //
            .missingAndWarns("short format but iso duration", List.of(arg("PT1H"), arg("short"))) //
            .missingAndWarns("long format but iso duration", List.of(arg("PT1H"), arg("long"))) //
            .missingAndWarns("iso format but long duration", List.of(arg("1 hour"), arg("iso"))) //
            .impl("missing duration", List.of(FunctionTestBuilder.misDuration())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseDateDuration() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_DATE_DURATION) //
            .typing("STRING", List.of(STRING), OPT_DATE_DURATION) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_DATE_DURATION) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_DATE_DURATION) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid period", List.of(arg("invalid"), arg("iso"))) //
            .errors("2nd arg not a valid format", List.of(arg("P1Y2M3D"), arg("foobar")),
                "(?i)unknown format.*?foobar.*") //
            .impl("default format", List.of(arg("P1Y2M3D")), TEST_PERIOD) //
            .impl("iso format", List.of(arg("P1Y2M3D"), arg("iso")), TEST_PERIOD) //
            .impl("long format", List.of(arg("1 year 2 months 3 days"), arg("long")), TEST_PERIOD) //
            .impl("short format", List.of(arg("1y2M3d"), arg("short")), TEST_PERIOD) //
            .missingAndWarns("long format but short period", List.of(arg("1y"), arg("long"))) //
            .missingAndWarns("short format but long period", List.of(arg("1 year"), arg("short"))) //
            .missingAndWarns("iso format but short period", List.of(arg("1y"), arg("iso"))) //
            .missingAndWarns("short format but iso period", List.of(arg("P1Y"), arg("short"))) //
            .missingAndWarns("long format but iso period", List.of(arg("P1Y"), arg("long"))) //
            .missingAndWarns("iso format but long period", List.of(arg("1 year"), arg("iso"))) //
            .impl("missing period", List.of(FunctionTestBuilder.misPeriod())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatDuration() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_DURATION) //
            .typing("opt DATE_DURATION", List.of(OPT_DATE_DURATION, STRING), OPT_STRING) //
            .typing("opt TIME_DURATION", List.of(OPT_TIME_DURATION, STRING), OPT_STRING) //
            .typing("DATE_DURATION", List.of(DATE_DURATION, STRING), STRING) //
            .typing("TIME_DURATION", List.of(TIME_DURATION, STRING), STRING) //
            .illegalArgs("Missing format", List.of(OPT_DATE_DURATION)) //
            .illegalArgs("1st arg not a DATE/TIME_DURATION", List.of(INTEGER, STRING)) //
            .impl("to iso (period)", List.of(arg(TEST_PERIOD), arg("iso")), "P1Y2M3D") //
            .impl("to long (period)", List.of(arg(TEST_PERIOD), arg("long")), "1 year 2 months 3 days") //
            .impl("to short (period)", List.of(arg(TEST_PERIOD), arg("short")), "1y 2M 3d") //
            .impl("missing (period)", List.of(FunctionTestBuilder.misPeriod(), arg("iso"))) //
            .impl("to iso (duration)", List.of(arg(TEST_DURATION), arg("iso")), "PT1H2M3S") //
            .impl("to long (duration)", List.of(arg(TEST_DURATION), arg("long")), "1 hour 2 minutes 3 seconds") //
            .impl("to short (duration)", List.of(arg(TEST_DURATION), arg("short")), "1H 2m 3s") //
            .impl("missing duration", List.of(FunctionTestBuilder.misDuration(), arg("iso"))) //
            .errors("invalid format (period)", List.of(arg(TEST_PERIOD), arg("foobar")),
                "(?i)unknown format.*?foobar.*") //
            .errors("invalid format (duration", List.of(arg(TEST_DURATION), arg("foobar")),
                "(?i)unknown format.*?foobar.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> makeDate() {
        return new FunctionTestBuilder(TemporalFunctions.MAKE_DATE) //
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
        return new FunctionTestBuilder(TemporalFunctions.MAKE_TIME) //
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
        return new FunctionTestBuilder(TemporalFunctions.MAKE_DATETIME) //
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
        return new FunctionTestBuilder(TemporalFunctions.MAKE_ZONED) //
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
        return new FunctionTestBuilder(TemporalFunctions.MAKE_TIME_DURATION) //
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
        return new FunctionTestBuilder(TemporalFunctions.MAKE_DATE_DURATION) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_YEAR) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_MONTH) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_DAY_OF_MONTH) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_HOUR) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_MINUTE) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_SECOND) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_NANOSECOND) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME), INTEGER) //
            .typing("OPT LOCAL_TIME", List.of(OPT_LOCAL_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with time info", List.of(LOCAL_DATE)) //
            .impl("valid datetime", List.of(arg(TEST_DATE_TIME)), 0) //
            .impl("missing datetime", List.of(misLocalDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> extractDate() {
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_DATE) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_TIME) //
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
        return new FunctionTestBuilder(TemporalFunctions.EXTRACT_DATETIME) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME), LOCAL_DATE_TIME) //
            .typing("OPT ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME), OPT_LOCAL_DATE_TIME) //
            .illegalArgs("Not a zoned datetime", List.of(LOCAL_DATE)) //
            .impl("valid zoned", List.of(arg(TEST_ZONED_ID)), TEST_DATE_TIME) //
            .impl("missing zoned", List.of(misZonedDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toHours() {
        return new FunctionTestBuilder(TemporalFunctions.TO_HOURS) //
            .typing("TIME_DURATION", List.of(TIME_DURATION), FLOAT) //
            .typing("OPT_TIME_DURATION", List.of(OPT_TIME_DURATION), OPT_FLOAT) //
            .illegalArgs("Not a duration", List.of(LOCAL_DATE)) //
            .impl("valid duration", List.of(arg(TEST_DURATION)), 1 + 2 / 60.0 + 3 / 3600.0) //
            .impl("missing duration", List.of(misDuration())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toMinutes() {
        return new FunctionTestBuilder(TemporalFunctions.TO_MINUTES) //
            .typing("TIME_DURATION", List.of(TIME_DURATION), FLOAT) //
            .typing("OPT_TIME_DURATION", List.of(OPT_TIME_DURATION), OPT_FLOAT) //
            .illegalArgs("Not a duration", List.of(LOCAL_DATE)) //
            .implWithTolerance("valid duration", List.of(arg(TEST_DURATION)), 1 * 60.0 + 2 + 3 / 60.0) //
            .impl("missing duration", List.of(misDuration())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toSeconds() {
        return new FunctionTestBuilder(TemporalFunctions.TO_SECONDS) //
            .typing("TIME_DURATION", List.of(TIME_DURATION), FLOAT) //
            .typing("OPT_TIME_DURATION", List.of(OPT_TIME_DURATION), OPT_FLOAT) //
            .illegalArgs("Not a duration", List.of(LOCAL_DATE)) //
            .implWithTolerance("valid duration", List.of(arg(TEST_DURATION)), 1 * 3600.0 + 2 * 60.0 + 3) //
            .impl("missing duration", List.of(misDuration())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> dateDurationBetween() {
        return new FunctionTestBuilder(TemporalFunctions.DATE_DURATION_BETWEEN) //
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
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> timeDurationBetween() {
        return new FunctionTestBuilder(TemporalFunctions.TIME_DURATION_BETWEEN) //
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
            .impl("missing time", List.of(misLocalTime(), arg(LocalTime.of(4, 5, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> yearsBetween() {
        return new FunctionTestBuilder(TemporalFunctions.YEARS_BETWEEN) //
            .typing("LOCAL_DATE × 2", List.of(LOCAL_DATE, LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, LOCAL_TIME)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_DATE, LOCAL_DATE_TIME)) //
            .impl("valid dates", List.of(arg(LocalDate.of(2001, 2, 3)), arg(LocalDate.of(2001, 3, 6))), 0) //
            .impl("negative", List.of(arg(LocalDate.of(2001, 3, 6)), arg(LocalDate.of(2001, 2, 3))), 0) //
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> monthsBetween() {
        return new FunctionTestBuilder(TemporalFunctions.MONTHS_BETWEEN) //
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
            .tests();
    }

    @TestFactory
    List<DynamicNode> daysBetween() {
        return new FunctionTestBuilder(TemporalFunctions.DAYS_BETWEEN) //
            .typing("LOCAL_DATE × 2", List.of(LOCAL_DATE, LOCAL_DATE), INTEGER) //
            .typing("OPT LOCAL_DATE x 2", List.of(OPT_LOCAL_DATE, OPT_LOCAL_DATE), OPT_INTEGER) //
            .typing("LOCAL_DATE_TIME × 2", List.of(LOCAL_DATE_TIME, LOCAL_DATE_TIME), INTEGER) //
            .typing("OPT LOCAL_DATE_TIME x 2", List.of(OPT_LOCAL_DATE_TIME, OPT_LOCAL_DATE_TIME), OPT_INTEGER) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), INTEGER) //
            .typing("OPT ZONED_DATE_TIME x 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), OPT_INTEGER) //
            .illegalArgs("Not a temporal with date info", List.of(LOCAL_TIME, LOCAL_TIME)) //
            .illegalArgs("Two different temporal types", List.of(LOCAL_DATE, LOCAL_DATE_TIME)) //
            .impl("valid dates", List.of(arg(LocalDate.of(2001, 2, 3)), arg(LocalDate.of(2001, 3, 6))), 31) //
            .impl("negative", List.of(arg(LocalDate.of(2001, 3, 6)), arg(LocalDate.of(2001, 2, 3))), -31) //
            .impl("missing date", List.of(misLocalDate(), arg(LocalDate.of(2001, 3, 6)))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> addDateDuration() {
        return new FunctionTestBuilder(TemporalFunctions.ADD_DATE_DURATION) //
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
            .impl("missing date", List.of(misLocalDate(), arg(Period.of(1, 2, 3)))) //
            .errors("overflow", List.of(arg(LocalDate.MAX), arg(Period.of(1, 0, 0))), ".*too large.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> addTimeDuration() {
        return new FunctionTestBuilder(TemporalFunctions.ADD_TIME_DURATION) //
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
            .impl("missing time", List.of(misLocalTime(), arg(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)))) //
            .errors("overflow", List.of(arg(LocalDateTime.MAX), arg(Duration.ofHours(1))), ".*too large.*") //
            .tests();
    }
}
