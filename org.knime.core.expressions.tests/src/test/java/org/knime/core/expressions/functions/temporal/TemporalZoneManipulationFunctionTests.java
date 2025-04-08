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

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misZonedDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.knime.core.expressions.functions.FunctionTestBuilder;

/**
 * Tests for {@link TemporalZoneManipulationFunctions}
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class TemporalZoneManipulationFunctionTests {

    private static final LocalDate TEST_DATE = LocalDate.of(2001, 2, 3);

    private static final LocalTime TEST_TIME = LocalTime.of(12, 34, 56);

    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(TEST_DATE, TEST_TIME);

    private static final ZonedDateTime TEST_ZONED_ID = ZonedDateTime.of(TEST_DATE_TIME, ZoneId.of("Europe/Paris"));

    @TestFactory
    List<DynamicNode> convertZone() {
        return new FunctionTestBuilder(TemporalZoneManipulationFunctions.CONVERT_TO_ZONE) //
            .typing("ZONED_DATE_TIME, STRING", List.of(ZONED_DATE_TIME, STRING), OPT_ZONED_DATE_TIME) //
            .typing("OPT ZONED_DATE_TIME, STRING", List.of(OPT_ZONED_DATE_TIME, STRING), OPT_ZONED_DATE_TIME) //
            .illegalArgs("Not a zoned datetime", List.of(LOCAL_DATE_TIME, STRING)) //
            .impl("valid zoned datetime", List.of(arg(TEST_ZONED_ID), arg("America/New_York")),
                ZonedDateTime.of(TEST_DATE_TIME.minusHours(6), ZoneId.of("America/New_York"))) //
            .impl("missing zoned datetime", List.of(misZonedDateTime(), arg("America/New_York"))) //
            .impl("zone id is case insensitive", List.of(arg(TEST_ZONED_ID), arg("aMeRiCa/NeW_YoRk")),
                ZonedDateTime.of(TEST_DATE_TIME.minusHours(6), ZoneId.of("America/New_York"))) //
            .errors("wall time shifted out of valid range",
                List.of(arg(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("UTC"))), arg("America/New_York")),
                ".*range.*") //
            .missingAndWarns("invalid zone", List.of(arg(TEST_ZONED_ID), arg("invalid")),
                "Invalid time zone ID: invalid.") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> replaceZone() {
        return new FunctionTestBuilder(TemporalZoneManipulationFunctions.REPLACE_ZONE) //
            .typing("ZONED_DATE_TIME, STRING", List.of(ZONED_DATE_TIME, STRING), OPT_ZONED_DATE_TIME) //
            .typing("OPT ZONED_DATE_TIME, STRING", List.of(OPT_ZONED_DATE_TIME, STRING), OPT_ZONED_DATE_TIME) //
            .illegalArgs("Not a zoned datetime", List.of(LOCAL_DATE_TIME, STRING)) //
            .impl("valid zoned datetime", List.of(arg(TEST_ZONED_ID), arg("America/New_York")),
                ZonedDateTime.of(TEST_DATE_TIME, ZoneId.of("America/New_York"))) //
            .impl("missing zoned datetime", List.of(misZonedDateTime(), arg("America/New_York"))) //
            .impl("zone id is case insensitive", List.of(arg(TEST_ZONED_ID), arg("aMeRiCa/NeW_YoRk")),
                ZonedDateTime.of(TEST_DATE_TIME, ZoneId.of("America/New_York"))) //
            .missingAndWarns("invalid zone", List.of(arg(TEST_ZONED_ID), arg("invalid")),
                "Invalid time zone ID: invalid.") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> hasSameInstant() {
        var testZoneId = ZoneId.of("America/Araguaina"); // GMT-3

        return new FunctionTestBuilder(TemporalZoneManipulationFunctions.IS_SAME_INSTANT) //
            .typing("ZONED_DATE_TIME × 2", List.of(ZONED_DATE_TIME, ZONED_DATE_TIME), BOOLEAN) //
            .typing("OPT ZONED_DATE_TIME × 2", List.of(OPT_ZONED_DATE_TIME, OPT_ZONED_DATE_TIME), BOOLEAN) //
            .illegalArgs("Not a zoned datetime", List.of(LOCAL_DATE_TIME, ZONED_DATE_TIME)) //
            .impl("identical inputs", List.of(arg(TEST_ZONED_ID), arg(TEST_ZONED_ID)), true) //
            .impl("different wall same instant",
                List.of(arg(TEST_ZONED_ID), arg(TEST_ZONED_ID.withZoneSameInstant(testZoneId))), true) //
            .impl("same wall different instants",
                List.of(arg(TEST_ZONED_ID), arg(TEST_ZONED_ID.withZoneSameLocal(testZoneId))), false) //
            .impl("difference wall different instants",
                List.of(arg(TEST_ZONED_ID), arg(TEST_ZONED_ID.withZoneSameLocal(testZoneId).plusHours(-6))), false) //
            .impl("both missing", List.of(misZonedDateTime(), misZonedDateTime()), true) //
            .impl("first missing", List.of(misZonedDateTime(), arg(TEST_ZONED_ID)), false) //
            .impl("second missing", List.of(arg(TEST_ZONED_ID), misZonedDateTime()), false) //
            .tests();
    }
}
