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
 *   Dec 11, 2024 (Tobias Kampmann): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misString;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 *
 * @author Tobias Kampmann
 */
final class DateTimeFunctionTests {

    @TestFactory
    List<DynamicNode> parseTime() {
        return new FunctionTestBuilder(DateTimeFunctions.PARSE_TIME) //
            .typing("STRING", List.of(STRING), LOCAL_TIME) //
            .typing("STRING?", List.of(OPT_STRING), OPT_LOCAL_TIME) //
            .illegalArgs("FLOAT", List.of(FLOAT)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("simple", List.of(arg("12:34")), LocalTime.of(12, 34)) //
            .impl("MISSING", List.of(misString())) //
            .tests();
    }

    //    @TestFactory
    //    List<DynamicNode> parsePeriod() {
    //        return new FunctionTestBuilder(DateTimeFunctions.PARSE_PERIOD) //
    //            .typing("STRING", List.of(STRING), PERIOD) //
    //            .typing("STRING?", List.of(OPT_STRING), OPT_PERIOD) //
    //            .illegalArgs("FLOAT", List.of(FLOAT)) //
    //            .illegalArgs("INTEGER", List.of(INTEGER)) //
    //            .illegalArgs("MISSING", List.of(MISSING)) //
    //            .impl("ISO", List.of(arg("P1Y2M3W4D")), Period.ofWeeks(3).plus(Period.of(1, 2, 4))) //
    //            .impl("human readable (short)", List.of(arg("1y2m3w4d")), Period.ofWeeks(3).plus(Period.of(1, 2, 4))) //
    //            .impl("human readable (long)", List.of(arg("1 year 2 months 3 weeks 4 days")),
    //                Period.ofWeeks(3).plus(Period.of(1, 2, 4))) //
    //            .impl("MISSING", List.of(misString())) //
    //            .impl("parse duration", List.of(arg("PT1H2M3S"))) //
    //            .tests();
    //    }

    //    @TestFactory
    //    List<DynamicNode> parseDuration() {
    //        return new FunctionTestBuilder(DateTimeFunctions.PARSE_DURATION) //
    //            .typing("STRING", List.of(STRING), DURATION) //
    //            .typing("STRING?", List.of(OPT_STRING), OPT_DURATION) //
    //            .illegalArgs("FLOAT", List.of(FLOAT)) //
    //            .illegalArgs("INTEGER", List.of(INTEGER)) //
    //            .illegalArgs("MISSING", List.of(MISSING)) //
    //            .impl("ISO", List.of(arg("PT1H2M3.4S")), Duration.ofMillis(3723400)) //
    //            .impl("human readable (short)", List.of(arg("1h2m3.4s")), Duration.ofMillis(3723400)) //
    //            .impl("human readable (long)", List.of(arg("1 hour 2 minutes 3.04 seconds")), Duration.ofMillis(3723040)) //
    //            .impl("MISSING", List.of(misString())) //
    //            .impl("parse period", List.of(arg("P1Y2M3W4D"))) //
    //            .tests();
    //    }

    //    @TestFactory
    //    List<DynamicNode> formatPeriod() {
    //        return new FunctionTestBuilder(DateTimeFunctions.FORMAT_PERIOD) //
    //            .typing("PERIOD", List.of(PERIOD), STRING) //
    //            .typing("PERIOD?", List.of(OPT_PERIOD), OPT_STRING) //
    //            .illegalArgs("FLOAT", List.of(FLOAT)) //
    //            .illegalArgs("INTEGER", List.of(INTEGER)) //
    //            .illegalArgs("MISSING", List.of(MISSING)) //
    //            .impl("Default should be ISO", List.of(arg(Period.ofWeeks(3).plus(Period.of(1, 2, 4)))), "P1Y2M25D") //
    //            .impl("ISO", List.of(arg(Period.ofWeeks(3).plus(Period.of(1, 2, 4))), arg("iso")), "P1Y2M25D") //
    //            .impl("human readable (short)", List.of(arg(Period.ofWeeks(3).plus(Period.of(1, 2, 4))), arg("short")),
    //                "1y 2M 25d") //
    //            .impl("human readable (long)", List.of(arg(Period.ofWeeks(3).plus(Period.of(1, 2, 4))), arg("long")),
    //                "1 year 2 months 25 days") //
    //            .impl("MISSING", List.of(misPeriod())) //
    //            .tests();
    //    }

    //    @TestFactory
    //    List<DynamicNode> formatDuration() {
    //        return new FunctionTestBuilder(DateTimeFunctions.FORMAT_DURATION) //
    //            .typing("DURATION", List.of(DURATION), STRING) //
    //            .typing("DURATION?", List.of(OPT_DURATION), OPT_STRING) //
    //            .illegalArgs("FLOAT", List.of(FLOAT)) //
    //            .illegalArgs("INTEGER", List.of(INTEGER)) //
    //            .illegalArgs("MISSING", List.of(MISSING)) //
    //            .impl("Default should be ISO", List.of(arg(Duration.ofMillis(3723400))), "PT1H2M3.4S") //
    //            .impl("ISO", List.of(arg(Duration.ofMillis(3723400)), arg("iso")), "PT1H2M3.4S") //
    //            .impl("human readable (short)", List.of(arg(Duration.ofMillis(3723400)), arg("short")), "1H 2m 3.4s") //
    //            .impl("human readable (long)", List.of(arg(Duration.ofMillis(3723400)), arg("long")),
    //                "1 hour 2 minutes 3.4 seconds") //
    //            .impl("MISSING", List.of(misDuration())) //
    //            .tests();
    //    }

    //    @TestFactory
    //    List<DynamicNode> roundTime() {
    //        return new FunctionTestBuilder(DateTimeFunctions.ROUND_TIME) //
    //            .typing("LOCAL_TIME", List.of(LOCAL_TIME), LOCAL_TIME) //
    //            .typing("LOCAL_TIME?", List.of(OPT_LOCAL_TIME), OPT_LOCAL_TIME) //
    //            .illegalArgs("FLOAT", List.of(FLOAT)) //
    //            .illegalArgs("INTEGER", List.of(INTEGER)) //
    //            .illegalArgs("MISSING", List.of(MISSING)) //
    //            .impl("round to full hour", List.of(arg(LocalTime.of(12, 34)), arg(Duration.ofHours(1))),
    //                LocalTime.of(13, 0)) //
    //            .impl("round to full minute", List.of(arg(LocalTime.of(12, 34, 56)), arg("M")), LocalTime.of(12, 35)) //
    //            .impl("round to full second", List.of(arg(LocalTime.of(12, 34, 56)), arg("S")), LocalTime.of(12, 34, 56)) //
    //            .impl("MISSING", List.of(misString())) //
    //            .tests();
    //    }
}
