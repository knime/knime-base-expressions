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
 *   Apr 8, 2025 (benjaminwilhelm): created
 */
package org.knime.core.expressions.functions.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.expressions.functions.temporal.TemporalFunctionUtils.parseZoneIdCaseInsensitive;

import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for TemporalFunctionUtils#parseZoneIdCaseInsensitive(String).
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class ZoneIdParserTest {

    @Test
    void noOverlappingZoneIdsIgnoringCase() {
        assertEquals(ZoneId.getAvailableZoneIds().size(), TemporalFunctionUtils.LOWER_CASE_ZONE_IDS.size(),
            "expect same number of zone ids in lower-case zone id map");
    }

    // --------------------------------------------------------------------------------------------
    // 1. Test valid zone IDs, ensuring they parse successfully.
    //    This covers region-based, offset-based, and prefix-based forms.
    // --------------------------------------------------------------------------------------------

    /**
     * Provides known-valid zone IDs in various cases (to test case-insensitivity), along with their expected canonical
     * IDs once parsed.
     */
    static Stream<Arguments> validZoneIdsProvider() {
        // The left side is the input, and the right side is the expected final ZoneId#getId()
        return Stream.of( //
            Arguments.of("America/New_York", "America/New_York"), // Region-based, normal case
            Arguments.of("europe/berlin", "Europe/Berlin"), // Region-based, lowercase => canonical is "Europe/Berlin"
            Arguments.of("AsIa/TokYo", "Asia/Tokyo"), // Region-based, mixed case => canonical is "Asia/Tokyo"
            Arguments.of("+02:00", "+02:00"), // Offset-based => remains "+02:00"
            Arguments.of("-5", "-05:00"), // Offset-based => remains "-05:00"
            Arguments.of("UtC+07:15", "UTC+07:15"), // With prefix "UTC" => remains "UTC+07:15"
            Arguments.of("gMt-3", "GMT-03:00"), // With prefix "GMT" => remains "GMT-03:00"
            Arguments.of("ut+0", "UT"), // With prefix "UT" => might canonicalize to "UT+00:00"
            Arguments.of("UTC", "UTC"), // "UTC" alone => remains "UTC"
            Arguments.of("gMT", "GMT"), // "GMT" alone, weird casing => canonical is "GMT"
            Arguments.of("z", "Z") // "z" for Zulu time
        );
    }

    @ParameterizedTest
    @MethodSource("validZoneIdsProvider")
    @DisplayName("Should parse valid zone IDs successfully, ignoring case and match expected IDs")
    void shouldParseValidZoneIds(final String input, final String expected) {
        Optional<ZoneId> result = parseZoneIdCaseInsensitive(input);
        assertTrue(result.isPresent(),
            () -> "Expected zone ID to be parsed, but got Optional.empty for input: " + input);
        assertEquals(expected, result.get().getId(),
            () -> "Expected canonical ID " + expected + " but got " + result.get().getId() + " for input: " + input);
    }

    // --------------------------------------------------------------------------------------------
    // 2. Test invalid zone IDs to ensure they return Optional.empty.
    // --------------------------------------------------------------------------------------------

    /** Provides invalid zone IDs (unsupported format, etc.) */
    static Stream<String> invalidZoneIdsProvider() {
        return Stream.of( //
            "", // empty string
            "NotARealZone", //
            "UTC+XY", // Malformed offset
            "GMT+2400", // Malformed offset
            "America_Chicago" // Underscore instead of slash
        );
    }

    @ParameterizedTest
    @MethodSource("invalidZoneIdsProvider")
    @DisplayName("Should return empty for invalid zone IDs")
    void shouldReturnEmptyForInvalidZoneIds(final String zoneId) {
        Optional<ZoneId> result = parseZoneIdCaseInsensitive(zoneId);
        assertFalse(result.isPresent(),
            () -> "Expected zone ID to fail parsing, but got present Optional for input: " + zoneId);
    }
}
