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
import org.knime.core.expressions.functions.FunctionTestBuilder;

/**
 * Tests for {@link TemporalParseFormatFunctions}
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class TemporalParseFormatFunctionTests {

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
        return new FunctionTestBuilder(TemporalParseFormatFunctions.PARSE_DATE) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_DATE) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_DATE) //
            .typing("3 STRINGS", List.of(STRING, STRING, STRING), OPT_LOCAL_DATE) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_DATE) //
            .illegalArgs("third arg must not be opt", List.of(STRING, STRING, OPT_STRING)) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid date", List.of(arg("invalid"), arg("yyyy-MM-dd")),
                ".*did not match.*") //
            .impl("default format", List.of(arg("2001-02-03")), TEST_DATE) //
            .impl("custom format", List.of(arg("03/02/2001"), arg("dd/MM/yyyy")), TEST_DATE) //
            .impl("missing date", List.of(misString())) //
            .impl("overspecified format", List.of(arg("2001-02-03 12:00"), arg("yyyy-MM-dd HH:mm")), TEST_DATE) //
            .impl("valid locale (de)", List.of(arg("2001-Februar-03"), arg("yyyy-MMMM-dd"), arg("de-DE")), TEST_DATE) //
            .impl("valid locale (en)", List.of(arg("2001-February-03"), arg("yyyy-MMMM-dd"), arg("en")), TEST_DATE) //
            .errors("invalid format", List.of(arg("2001-02-03"), arg("invalid")), "(?i).*?unparseable.*") //
            .missingAndWarns("underspecified format", List.of(arg("2001-02-03"), arg("yyyy")), ".*underspecified.*") //
            .missingAndWarns("invalid locale format", List.of(arg("2001-02-03"), arg("yyyy-MM-dd"), arg("foo_bar")),
                ".*malformed.*") //
            .missingAndWarns("invalid locale", List.of(arg("2001-02-03"), arg("yyyy-MM-dd"), arg("aq-XZ")),
                ".*invalid.*'aq'.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseTime() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.PARSE_TIME) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_TIME) //
            .typing("3 STRINGS", List.of(STRING, STRING, STRING), OPT_LOCAL_TIME) //
            .illegalArgs("third arg must not be opt", List.of(STRING, STRING, OPT_STRING)) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid time", List.of(arg("invalid"), arg("HH:mm:ss")), ".*did not match.*") //
            .impl("default format", List.of(arg("12:34:56")), TEST_TIME) //
            .impl("custom format", List.of(arg("12,34,56"), arg("HH,mm,ss")), TEST_TIME) //
            .impl("missing time", List.of(misString())) //
            // locales for times are relevant only when overspecifying the format
            .impl("valid locale (de)", List.of(arg("12:34:56 Februar"), arg("HH:mm:ss MMMM"), arg("de-DE")), TEST_TIME) //
            .impl("valid locale (en)", List.of(arg("12:34:56 February"), arg("HH:mm:ss MMMM"), arg("en")), TEST_TIME) //
            .impl("overspecified format", List.of(arg("2025 12:34:56"), arg("yyyy HH:mm:ss")), TEST_TIME) //
            .missingAndWarns("underspecified format", List.of(arg("12:34:56"), arg("mm")), ".*underspecified.*") //
            .errors("invalid format", List.of(arg("12:34:56"), arg("invalid")), "(?i).*?unparseable.*") //
            .missingAndWarns("invalid locale format", List.of(arg("2001-02-03"), arg("yyyy-MM-dd"), arg("foo_bar")),
                ".*malformed.*") //
            .missingAndWarns("invalid locale", List.of(arg("2001-02-03"), arg("yyyy-MM-dd"), arg("aq-XZ")),
                ".*invalid.*'aq'.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseDateTime() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.PARSE_DATE_TIME) //
            .typing("STRING", List.of(STRING), OPT_LOCAL_DATE_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_LOCAL_DATE_TIME) //
            .typing("3 STRINGS", List.of(STRING, STRING, STRING), OPT_LOCAL_DATE_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_LOCAL_DATE_TIME) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("third arg must not be opt", List.of(STRING, STRING, OPT_STRING)) //
            .missingAndWarns("1st arg not a valid datetime", List.of(arg("invalid"), arg("yyyy-MM-dd'T'HH:mm:ss")),
                ".*did not match.*") //
            .impl("default format", List.of(arg("2001-02-03T12:34:56")), TEST_DATE_TIME) //
            .impl("custom format", List.of(arg("03/02/2001 12:34:56"), arg("dd/MM/yyyy HH:mm:ss")), TEST_DATE_TIME) //
            .impl("missing datetime", List.of(misString())) //
            .impl("overspecified format",
                List.of(arg("2001-02-03T12:34:56+01:00[Europe/Berlin]"), arg("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")),
                TEST_DATE_TIME) //
            .impl("valid locale (de)",
                List.of(arg("2001-Februar-03 12:34:56"), arg("yyyy-MMMM-dd HH:mm:ss"), arg("de-DE")), TEST_DATE_TIME) //
            .impl("valid locale (en)",
                List.of(arg("2001-February-03 12:34:56"), arg("yyyy-MMMM-dd HH:mm:ss"), arg("en")), TEST_DATE_TIME) //
            .errors("invalid format", List.of(arg("2001-02-03T12:34:56"), arg("invalid")), "(?i).*?unparseable.*") //
            .missingAndWarns("underspecified format", List.of(arg("2001-02-03T12:34:56"), arg("yyyy")),
                ".*underspecified.*") //
            .missingAndWarns("invalid locale format",
                List.of(arg("2001-02-03 12:34"), arg("yyyy-MM-dd HH:mm"), arg("$foo_bar")), ".*malformed.*") //
            .missingAndWarns("invalid locale", List.of(arg("2001-02-03 12:34"), arg("yyyy-MM-dd HH:mm"), arg("aq-XZ")),
                "(?i).*invalid.*'aq'.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseZonedDateTime() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.PARSE_ZONED_DATE_TIME) //
            .typing("STRING", List.of(STRING), OPT_ZONED_DATE_TIME) //
            .typing("2 STRINGS", List.of(STRING, STRING), OPT_ZONED_DATE_TIME) //
            .typing("3 STRINGS", List.of(STRING, STRING, STRING), OPT_ZONED_DATE_TIME) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_ZONED_DATE_TIME) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("third arg must not be opt", List.of(STRING, STRING, OPT_STRING)) //
            .missingAndWarns("1st arg not a valid datetime", List.of(arg("invalid"), arg("yyyy-MM-dd'T'HH:mm:ssXXX")),
                ".*did not match.*") //
            .impl("default format", List.of(arg("2001-02-03T12:34:56+01:00")), TEST_ZONED_OFFSET) //
            .impl("custom format", List.of(arg("03/02/2001 12:34:56 Europe/Paris"), arg("dd/MM/yyyy HH:mm:ss VV")),
                TEST_ZONED_ID) //
            .impl("missing datetime", List.of(misString())) //
            .impl("valid locale (de)",
                List.of(arg("2001-Februar-03 12:34:56 Europe/Paris"), arg("yyyy-MMMM-dd HH:mm:ss VV"), arg("de-DE")),
                TEST_ZONED_ID) // interestingly, the geo id is not localised. Intentional in Java language spec.
            .impl("valid locale (en)",
                List.of(arg("2001-February-03 12:34:56 Europe/Paris"), arg("yyyy-MMMM-dd HH:mm:ss VV"), arg("en")),
                TEST_ZONED_ID) //
            .errors("invalid format", List.of(arg("2001-02-03T12:34:56+01:00"), arg("invalid")), "(?i).*?unparseable.*") //
            .missingAndWarns("underspecified format", List.of(arg("2001-02-03T12:34:56+01:00"), arg("yyyy")),
                ".*underspecified.*") //
            .missingAndWarns("invalid locale format", List.of(arg("2001-02-03"), arg("yyyy-MM-dd"), arg("foo_bar")),
                ".*malformed.*") //
            .missingAndWarns("invalid locale", List.of(arg("2001-02-03"), arg("yyyy-MM-dd"), arg("aq-XZ")),
                ".*invalid.*'aq'.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalDate() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.FORMAT_DATE) //
            .typing("LOCAL_DATE", List.of(LOCAL_DATE, STRING), OPT_STRING) //
            .typing("OPT_LOCAL_DATE", List.of(OPT_LOCAL_DATE, STRING), OPT_STRING) //
            .typing("Three args", List.of(LOCAL_DATE, STRING, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_DATE)) //
            .illegalArgs("1st arg not LOCAL_DATE", List.of(INTEGER, STRING)) //
            .illegalArgs("second arg must not be opt", List.of(LOCAL_DATE, OPT_STRING)) //
            .illegalArgs("3rd arg must not be opt", List.of(LOCAL_DATE, STRING, OPT_STRING)) //
            .impl("LOCAL_DATE", List.of(arg(TEST_DATE), arg("yyyy-MM-dd")), "2001-02-03") //
            .impl("missing local date", List.of(misLocalDate(), arg("yyyy-MM-dd"))) //
            .impl("underspecified format", List.of(arg(TEST_DATE), arg("yyyy")), "2001") //
            .impl("valid locale (de)", List.of(arg(TEST_DATE), arg("dd MMMM yyyy"), arg("de-DE")), "03 Februar 2001") //
            .impl("valid locale (en)", List.of(arg(TEST_DATE), arg("dd MMMM yyyy"), arg("en")), "03 February 2001") //
            .errors("invalid format", List.of(arg(TEST_DATE), arg("invalid")), "(?i).*?unparseable.*") //
            .missingAndWarns("overspecified format", List.of(arg(TEST_DATE), arg("yyyy-MM-dd HH:mm:ss")),
                ".*overspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalTime() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.FORMAT_TIME) //
            .typing("LOCAL_TIME", List.of(LOCAL_TIME, STRING), OPT_STRING) //
            .typing("OPT_LOCAL_TIME", List.of(OPT_LOCAL_TIME, STRING), OPT_STRING) //
            .typing("Three args", List.of(LOCAL_TIME, STRING, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(LOCAL_TIME)) //
            .illegalArgs("1st arg not LOCAL_TIME", List.of(INTEGER, STRING)) //
            .illegalArgs("second arg must not be opt", List.of(LOCAL_TIME, OPT_STRING)) //
            .illegalArgs("3rd arg must not be opt", List.of(LOCAL_TIME, STRING, OPT_STRING)) //
            .impl("LOCAL_TIME", List.of(arg(TEST_TIME), arg("HH:mm:ss")), "12:34:56") //
            .impl("missing local time", List.of(misLocalTime(), arg("HH:mm:ss"))) //
            .impl("underspecified format", List.of(arg(TEST_TIME), arg("HH")), "12") //
            .impl("valid locale (de)", List.of(arg(TEST_TIME), arg("a"), arg("de-DE")), "PM") //
            .impl("valid locale (jp)", List.of(arg(TEST_TIME), arg("a"), arg("ja-JP")), "午後") //
            .errors("invalid format", List.of(arg(TEST_TIME), arg("invalid")), "(?i).*?unparseable.*") //
            .missingAndWarns("overspecified format", List.of(arg(TEST_TIME), arg("yyyy-MM-dd HH:mm:ss")),
                ".*overspecified.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatLocalDateTime() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.FORMAT_DATE_TIME) //
            .typing("LOCAL_DATE_TIME", List.of(LOCAL_DATE_TIME, STRING), OPT_STRING) //
            .typing("OPT_LOCAL_DATE_TIME", List.of(OPT_LOCAL_DATE_TIME, STRING), OPT_STRING) //
            .typing("ZONED_DATE_TIME", List.of(ZONED_DATE_TIME, STRING), OPT_STRING) //
            .typing("OPT_ZONED_DATE_TIME", List.of(OPT_ZONED_DATE_TIME, STRING), OPT_STRING) //
            .typing("Three args", List.of(LOCAL_DATE_TIME, STRING, STRING), OPT_STRING) //
            .illegalArgs("Missing format", List.of(ZONED_DATE_TIME)) //
            .illegalArgs("1st arg not ZONED_DATE_TIME", List.of(INTEGER, STRING)) //
            .illegalArgs("Missing format", List.of(LOCAL_DATE_TIME)) //
            .illegalArgs("1st arg not LOCAL_DATE_TIME", List.of(INTEGER, STRING)) //
            .illegalArgs("second arg must not be opt", List.of(LOCAL_DATE_TIME, OPT_STRING)) //
            .illegalArgs("3rd arg must not be opt", List.of(LOCAL_DATE_TIME, STRING, OPT_STRING)) //
            .impl("LOCAL_DATE_TIME", List.of(arg(TEST_DATE_TIME), arg("yyyy-MM-dd'T'HH:mm:ss")), "2001-02-03T12:34:56") //
            .impl("missing local datetime", List.of(misLocalDateTime(), arg("yyyy-MM-dd'T'HH:mm:ss"))) //
            .impl("ZONED_DATE_TIME", List.of(arg(TEST_ZONED_ID), arg("yyyy-MM-dd'T'HH:mm:ssVV")),
                "2001-02-03T12:34:56Europe/Paris") //
            .impl("underspecified format (local)", List.of(arg(TEST_DATE_TIME), arg("yyyy")), "2001") //
            .impl("underspecified format (zoned)", List.of(arg(TEST_ZONED_ID), arg("yyyy")), "2001") //
            .missingAndWarns("overspecified format (local)", List.of(arg(TEST_DATE_TIME), arg("ZZ")),
                ".*overspecified.*") //
            // no test for overspecified zoned format as it is not possible to overspecify
            .impl("missing zoned datetime", List.of(misZonedDateTime(), arg("yyyy-MM-dd'T'HH:mm:ssVV"))) //
            .errors("invalid format", List.of(arg(TEST_DATE_TIME), arg("invalid")), "(?i).*?unparseable.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseTimeDuration() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.PARSE_TIME_DURATION) //
            .typing("STRING", List.of(STRING), OPT_TIME_DURATION) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_TIME_DURATION) //
            .illegalArgs("2 STRINGS", List.of(STRING, STRING)) //
            .illegalArgs("second arg must not be opt", List.of(STRING, OPT_STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .illegalArgs("2nd arg not STRING", List.of(STRING, INTEGER)) //
            .missingAndWarns("1st arg not a valid duration", List.of(arg("invalid")), ".*") //
            .impl("default format", List.of(arg("PT1H2M3S")), TEST_DURATION) //
            .impl("iso format", List.of(arg("PT1H2M3S")), TEST_DURATION) //
            .impl("long format", List.of(arg("1 hour 2 minutes 3 seconds")), TEST_DURATION) //
            .impl("short format", List.of(arg("1h2m3s")), TEST_DURATION) //
            .impl("missing duration", List.of(FunctionTestBuilder.misString())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseDateDuration() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.PARSE_DATE_DURATION) //
            .typing("STRING", List.of(STRING), OPT_DATE_DURATION) //
            .typing("OPT STRING", List.of(OPT_STRING), OPT_DATE_DURATION) //
            .illegalArgs("2 STRINGS", List.of(STRING, STRING)) //
            .illegalArgs("1st arg not STRING", List.of(INTEGER)) //
            .missingAndWarns("arg not a valid period", List.of(arg("invalid")), ".*") //
            .impl("iso format", List.of(arg("P1Y2M3D")), TEST_PERIOD) //
            .impl("long format", List.of(arg("1 year 2 months 3 days")), TEST_PERIOD) //
            .impl("short format", List.of(arg("1y2M3d")), TEST_PERIOD) //
            .impl("missing period", List.of(FunctionTestBuilder.misString())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> formatDuration() {
        return new FunctionTestBuilder(TemporalParseFormatFunctions.FORMAT_DURATION) //
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
}
