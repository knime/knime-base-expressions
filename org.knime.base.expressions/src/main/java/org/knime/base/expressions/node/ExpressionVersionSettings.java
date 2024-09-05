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
 *   Sep 3, 2024 (kampmann): created
 */
package org.knime.base.expressions.node;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.functions.BuiltInFunctions;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Settings for the expression language version. This is used to manage the language version and the built-in functions
 * and aggregations version.
 */
public class ExpressionVersionSettings {

    private static final String CFG_KEY_LANGUAGE_VERSION = "languageVersion";

    private static final String CFG_KEY_BUILTIN_FUNCTIONS_VERSION = "builtinFunctionsVersion";

    private static final String CFG_KEY_BUILTIN_AGGREGATIONS_VERSION = "builtinAggregationsVersion";

    private static final String JSON_KEY_LANGUAGE_VERSION = CFG_KEY_LANGUAGE_VERSION;

    private static final String JSON_KEY_FUNCTION_VERSION = CFG_KEY_BUILTIN_FUNCTIONS_VERSION;

    private static final String JSON_KEY_AGGREGATION_VERSION = CFG_KEY_BUILTIN_AGGREGATIONS_VERSION;

    private int m_languageVersion;

    private int m_builtinFunctionsVersion;

    private int m_builtinAggregationsVersion;

    /** Create version settings with the current version. */
    public ExpressionVersionSettings() {

        // Set to the latest version by default
        // The version will be overwritten if we load an older version from the model settings
        this.m_languageVersion = Expressions.LANGUAGE_VERSION;
        this.m_builtinFunctionsVersion = BuiltInFunctions.FUNCTIONS_VERSION;
        this.m_builtinAggregationsVersion = BuiltInAggregations.AGGREGATIONS_VERSION;
    }

    /** @param settings */
    public void loadSettingsFrom(final ConfigRO settings) {
        this.m_languageVersion = settings.getInt(CFG_KEY_LANGUAGE_VERSION, this.m_languageVersion);
        this.m_builtinFunctionsVersion =
            settings.getInt(CFG_KEY_BUILTIN_FUNCTIONS_VERSION, this.m_builtinFunctionsVersion);
        this.m_builtinAggregationsVersion =
            settings.getInt(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION, this.m_builtinAggregationsVersion);
    }

    /** @param settings */
    public void saveSettingsTo(final ConfigWO settings) {
        settings.addInt(CFG_KEY_LANGUAGE_VERSION, m_languageVersion);
        settings.addInt(CFG_KEY_BUILTIN_FUNCTIONS_VERSION, m_builtinFunctionsVersion);
        settings.addInt(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION, m_builtinAggregationsVersion);
    }

    /** @return the settings map with the version added */
    public Map<String, Object> getVersionSettingsMap() {

        var versionSettingsMap = new HashMap<String, Object>();
        versionSettingsMap.put(JSON_KEY_LANGUAGE_VERSION, m_languageVersion);
        versionSettingsMap.put(JSON_KEY_FUNCTION_VERSION, m_builtinFunctionsVersion);
        versionSettingsMap.put(JSON_KEY_AGGREGATION_VERSION, m_builtinAggregationsVersion);

        return versionSettingsMap;
    }

    /** @param data the data to write to the settings */
    public void writeMapToNodeSettings(final Map<String, Object> data) {

        m_languageVersion = (int)data.get(JSON_KEY_LANGUAGE_VERSION);
        m_builtinFunctionsVersion = (int)data.get(JSON_KEY_FUNCTION_VERSION);
        m_builtinAggregationsVersion = (int)data.get(JSON_KEY_AGGREGATION_VERSION);

    }

    /** @return the builtinAggregationsVersion */
    public int getBuiltinAggregationsVersion() {
        return m_builtinAggregationsVersion;
    }

    /** @return the builtinFunctionsVersion */
    public int getBuiltinFunctionsVersion() {
        return m_builtinFunctionsVersion;
    }

    /** @return the languageVersion */
    public int getLanguageVersion() {
        return m_languageVersion;
    }
}
