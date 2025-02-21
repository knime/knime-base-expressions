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

import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalDate;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalDateTime;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misLocalTime;
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
    List<DynamicNode> parseLocalDate() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_LOCAL_DATE) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_DATE) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_DATE) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_DATE) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("2nd arg not a valid pattern", List.of(arg("1970-01-01"), arg("invalid"))) //
            .missingAndWarns("1st arg not a valid date", List.of(arg("invalid"), arg("yyyy-MM-dd"))) //
            .impl("default format", List.of(arg("2001-02-03")), TEST_DATE) //
            .impl("custom format", List.of(arg("03/02/2001"), arg("dd/MM/yyyy")), TEST_DATE) //
            .impl("missing date", List.of(misLocalDate())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseLocalTime() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_LOCAL_TIME) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_TIME) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("2nd arg not a valid pattern", List.of(arg("12:34:56"), arg("invalid"))) //
            .missingAndWarns("1st arg not a valid time", List.of(arg("invalid"), arg("HH:mm:ss"))) //
            .impl("default format", List.of(arg("12:34:56")), TEST_TIME) //
            .impl("custom format", List.of(arg("12,34,56"), arg("HH,mm,ss")), TEST_TIME) //
            .impl("missing time", List.of(misLocalTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseLocalDateTime() {
        return new FunctionTestBuilder(TemporalFunctions.PARSE_LOCAL_DATE_TIME) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_DATE_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_DATE_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_DATE_TIME) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("2nd arg not a valid pattern", List.of(arg("2001-02-03T12:34:56"), arg("invalid"))) //
            .missingAndWarns("1st arg not a valid datetime", List.of(arg("invalid"), arg("yyyy-MM-dd'T'HH:mm:ss"))) //
            .impl("default format", List.of(arg("2001-02-03T12:34:56")), TEST_DATE_TIME) //
            .impl("custom format", List.of(arg("03/02/2001 12:34:56"), arg("dd/MM/yyyy HH:mm:ss")), TEST_DATE_TIME) //
            .impl("missing datetime", List.of(misLocalDateTime())) //
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
            .missingAndWarns("2nd arg not a valid pattern", List.of(arg("2001-02-03T12:34:56+01:00"), arg("invalid"))) //
            .missingAndWarns("1st arg not a valid datetime", List.of(arg("invalid"), arg("yyyy-MM-dd'T'HH:mm:ssXXX"))) //
            .impl("default format", List.of(arg("2001-02-03T12:34:56+01:00")), TEST_ZONED_OFFSET) //
            .impl("custom format", List.of(arg("03/02/2001 12:34:56 Europe/Paris"), arg("dd/MM/yyyy HH:mm:ss VV")),
                TEST_ZONED_ID) //
            .impl("missing datetime", List.of(misZonedDateTime())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalDate() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_LOCAL_DATE) //
            .typing("LOCAL_DATE", List.of(LOCAL_DATE, STRING), OPT_STRING) //
            .typing("OPT_LOCAL_DATE", List.of(OPT_LOCAL_DATE, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_DATE)) //
            .illegalArgs("1st arg not LOCAL_DATE", List.of(INTEGER, STRING)) //
            .impl("LOCAL_DATE", List.of(arg(TEST_DATE), arg("yyyy-MM-dd")), "2001-02-03") //
            .impl("missing local date", List.of(misLocalDate(), arg("yyyy-MM-dd"))) //
            .missingAndWarns("invalid format", List.of(arg(TEST_DATE), arg("invalid"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalTime() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_LOCAL_TIME) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME, STRING), OPT_STRING) //
            .typing("OPT_LOCAL_TIME", List.of(OPT_LOCAL_TIME, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_TIME)) //
            .illegalArgs("1st arg not LOCAL_TIME", List.of(INTEGER, STRING)) //
            .impl("LOCAL_TIME", List.of(arg(TEST_TIME), arg("HH:mm:ss")), "12:34:56") //
            .impl("missing local time", List.of(misLocalTime(), arg("HH:mm:ss"))) //
            .missingAndWarns("invalid format", List.of(arg(TEST_TIME), arg("QQQ"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalDateTime() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_LOCAL_DATE_TIME) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME, STRING), OPT_STRING) //
            .typing("OPT_LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_DATE_TIME)) //
            .illegalArgs("1st arg not LOCAL_DATE_TIME", List.of(INTEGER, STRING)) //
            .impl("LOCAL_DATE_TIME", List.of(arg(TEST_DATE_TIME), arg("yyyy-MM-dd'T'HH:mm:ss")), "2001-02-03T12:34:56") //
            .impl("missing local datetime", List.of(misLocalDateTime(), arg("yyyy-MM-dd'T'HH:mm:ss"))) //
            .missingAndWarns("invalid format", List.of(arg(TEST_DATE_TIME), arg("invalid"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatZonedDateTime() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_ZONED_DATE_TIME) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME, STRING), OPT_STRING) //
            .typing("OPT_ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(ZONED_DATE_TIME)) //
            .illegalArgs("1st arg not ZONED_DATE_TIME", List.of(INTEGER, STRING)) //
            .impl("ZONED_DATE_TIME", List.of(arg(TEST_ZONED_ID), arg("yyyy-MM-dd'T'HH:mm:ssVV")),
                "2001-02-03T12:34:56Europe/Paris") //
            .impl("missing zoned datetime", List.of(misZonedDateTime(), arg("yyyy-MM-dd'T'HH:mm:ssVV"))) //
            .missingAndWarns("invalid format", List.of(arg(TEST_ZONED_ID), arg("invalid"))) //
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
            .missingAndWarns("2nd arg not a valid format", List.of(arg("P1DT2H3M4S"), arg("invalid"))) //
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
            .missingAndWarns("2nd arg not a valid format", List.of(arg("P1Y2M3D"), arg("invalid"))) //
            .missingAndWarns("1st arg not a valid period", List.of(arg("invalid"), arg("iso"))) //
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
    List<DynamicNode> formatTimeDuration() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_TIME_DURATION) //
            .typing("TIME_DURATION", List.of(OPT_TIME_DURATION, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(OPT_TIME_DURATION)) //
            .illegalArgs("1st arg not TIME_DURATION", List.of(INTEGER, STRING)) //
            .impl("to iso", List.of(arg(TEST_DURATION), arg("iso")), "PT1H2M3S") //
            .impl("to long", List.of(arg(TEST_DURATION), arg("long")), "1 hour 2 minutes 3 seconds") //
            .impl("to short", List.of(arg(TEST_DURATION), arg("short")), "1H 2m 3s") //
            .impl("missing duration", List.of(FunctionTestBuilder.misDuration(), arg("iso"))) //
            .missingAndWarns("invalid format", List.of(arg(TEST_DURATION), arg("invalid"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatDateDuration() {
        return new FunctionTestBuilder(TemporalFunctions.FORMAT_DATE_DURATION) //
            .typing("DATE_DURATION", List.of(OPT_DATE_DURATION, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(OPT_DATE_DURATION)) //
            .illegalArgs("1st arg not DATE_DURATION", List.of(INTEGER, STRING)) //
            .impl("to iso", List.of(arg(TEST_PERIOD), arg("iso")), "P1Y2M3D") //
            .impl("to long", List.of(arg(TEST_PERIOD), arg("long")), "1 year 2 months 3 days") //
            .impl("to short", List.of(arg(TEST_PERIOD), arg("short")), "1y 2M 3d") //
            .impl("missing period", List.of(FunctionTestBuilder.misPeriod(), arg("iso"))) //
            .missingAndWarns("invalid format", List.of(arg(TEST_PERIOD), arg("invalid"))) //
            .tests();
    }
}
