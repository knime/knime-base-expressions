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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class TemporalFunctionUtils {

    private TemporalFunctionUtils() {
    }

    public static final String TEMPORAL_META_CATEGORY_NAME = "Temporal";

    /**
     * Package private - only intended for use by this file and by tests.
     *
     * <p>
     * The list of supported zone ids, converted to lower case for a quick case-insensitive lookup. Note that not
     * everything that is supported by {@link ZoneId#of(String)} is included here, only the region-based ones.
     * Offset-based ones aren't included here.
     * </p>
     */
    static final Map<String, ZoneId> LOWER_CASE_ZONE_IDS = ZoneId.getAvailableZoneIds().stream() //
        .collect(Collectors.toUnmodifiableMap(k -> k.toLowerCase(Locale.ROOT), ZoneId::of));

    /**
     * Parses the given zone id, ignoring its case. The given ZoneId may be region or offset based.
     *
     * @param zoneId the zone id to parse
     * @return the parsed zone id, or empty if the zone id is not valid
     */
    public static Optional<ZoneId> parseZoneIdCaseInsensitive(final String zoneId) {
        // Since LOWER_CASE_ZONE_IDS only includes geographical zone ids, we
        // try ZoneId.of iff the zone id is not found in the map.
        return Optional.ofNullable(LOWER_CASE_ZONE_IDS.get(zoneId.toLowerCase(Locale.ROOT))) //
            .or(() -> {
                try {
                    return Optional.of(ZoneId.of(zoneId.toUpperCase(Locale.ROOT)));
                } catch (DateTimeException e) { // NOSONAR don't need to log or rethrow
                    return Optional.empty();
                }
            });
    }

    public static final String URL_TIMEZONE_LIST = "https://en.wikipedia.org/wiki/List_of_tz_database_time_zones";
}
