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
 *   Aug 26, 2024 (david): created
 */
package org.knime.base.expressions.node.row.filter;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.expressions.node.ExpressionVersionSettingsUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.scripting.editor.GenericSettingsIOManager;
import org.knime.scripting.editor.ScriptingNodeSettings;

/**
 * Settings for the Expression Row Filter node, with loading, saving, and validation.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
class ExpressionRowFilterSettings extends ScriptingNodeSettings implements GenericSettingsIOManager {

    /**
     * The script shown in a new expression node.
     */
    static final String DEFAULT_SCRIPT = """
            # Examples:
            # 1. Remove every other row:
            #  $ROW_INDEX %2 = 0
            # 2. Remove rows negative values
            #  $["My Column"] > 0
            # 3. Remove rows where the difference between adjacent rows is negative:
            #  $["My Column"] - $["My Column", -1] > 0
            #
            # If you need help, try the "Ask K-AI" button,
            # or have a look at the node description!
            """;

    private static final String CFG_KEY_SCRIPT = "script";

    private static final String JSON_KEY_SCRIPT = CFG_KEY_SCRIPT;

    private static final String JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES =
        "settingsAreOverriddenByFlowVariable";

    private ExpressionVersionSettingsUtils m_versionSettings;

    private String m_script;

    /**
     * Create a new settings object with the default script.
     */
    ExpressionRowFilterSettings() {
        super(SettingsType.MODEL);

        this.m_versionSettings = new ExpressionVersionSettingsUtils();

        this.m_script = DEFAULT_SCRIPT;
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_versionSettings.loadSettingsFrom(settings);

        m_script = settings.getString(CFG_KEY_SCRIPT);
    }

    /**
     * @return the script
     */
    String getScript() {
        return m_script;
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KEY_SCRIPT, m_script);

        m_versionSettings.saveSettingsTo(settings);
    }

    @Override
    public Map<String, Object> convertNodeSettingsToMap(final Map<SettingsType, NodeAndVariableSettingsRO> settings)
        throws InvalidSettingsException {

        loadSettingsFrom(settings);

        var configOverWrittenByFlowVars =
            isEditorConfigurationOverwrittenByFlowVariable(settings.get(m_scriptSettingsType));

        Map<String, Object> settingsMap = new HashMap<>();
        settingsMap.putAll(m_versionSettings.getVersionSettingsMap());
        settingsMap.putAll(Map.of( //
            JSON_KEY_SCRIPT, m_script, //
            JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES, configOverWrittenByFlowVars //
        ));

        return settingsMap;
    }

    @Override
    public void writeMapToNodeSettings(final Map<String, Object> data,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {

        m_script = (String)data.get(JSON_KEY_SCRIPT);

        m_versionSettings.writeMapToNodeSettings(data);

        saveSettingsTo(settings);
        copyVariableSettings(previousSettings, settings);
    }

    private static boolean isEditorConfigurationOverwrittenByFlowVariable(final NodeAndVariableSettingsRO settings) {
        return isOverriddenByFlowVariable(settings, CFG_KEY_SCRIPT);
    }
}
