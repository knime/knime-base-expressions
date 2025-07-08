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
 *   Sep 11, 2023 (benjamin): created
 */
package org.knime.base.expressions.node.jsoneditor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.scripting.editor.GenericSettingsIOManager;
import org.knime.scripting.editor.ScriptingNodeSettings;

// Copied from GenericEChartsSettings.java
@SuppressWarnings("restriction") // SettingsType is not yet public API
class JsonEditorSettings extends ScriptingNodeSettings implements GenericSettingsIOManager {

    private static final String DEFAULT_SCRIPT = """
            {
                "key1": "value1",
                "key2": {{my_flow_variable}},
            }
            """;

    private static final String CFG_KEY_SCRIPT = "script";

    private static final String JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES =
        "settingsAreOverriddenByFlowVariable";

    private static final String JSON_KEY_OVERRIDING_FLOW_VARIABLE = "scriptUsedFlowVariable";

    private String m_script;

    JsonEditorSettings() {
        this(DEFAULT_SCRIPT);
    }

    JsonEditorSettings(final String script) {
        super(SettingsType.MODEL);

        m_script = script;
    }

    @Override
    public Map<String, Object> convertNodeSettingsToMap(final Map<SettingsType, NodeAndVariableSettingsRO> settings)
        throws InvalidSettingsException {

        var nodeSettings = settings.get(m_scriptSettingsType);

        loadSettingsFrom(nodeSettings);

        Map<String, Object> ret = new HashMap<>(Map.of(CFG_KEY_SCRIPT, m_script));

        var scriptUsedFlowVariable = getOverridingFlowVariableName(nodeSettings, CFG_KEY_SCRIPT);
        if (scriptUsedFlowVariable.isPresent()) {
            ret.put(JSON_KEY_OVERRIDING_FLOW_VARIABLE, scriptUsedFlowVariable.get());
            ret.put(JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES, true);
        } else {
            ret.put(JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES, false);
        }

        return ret;
    }

    @Override
    public void writeMapToNodeSettings(final Map<String, Object> data,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {

        m_script = (String)data.get(CFG_KEY_SCRIPT);

        final var extractedSettings = new NodeSettings("extracted settings");
        copyVariableSettings(previousSettings, settings);
        saveSettingsTo(extractedSettings);

        NodeSettingsCorrectionUtil.correctNodeSettingsRespectingFlowVariables(new ConfigMappings(List.of()),
            extractedSettings, previousSettings.get(SettingsType.MODEL), previousSettings.get(SettingsType.MODEL));

        extractedSettings.copyTo(settings.get(SettingsType.MODEL));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_script = settings.getString(CFG_KEY_SCRIPT);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KEY_SCRIPT, m_script);
    }

    /**
     * @return the script
     */
    public String getScript() {
        return m_script;
    }

    /**
     * @param script the script to set
     */
    public void setScript(final String script) {
        m_script = script;
    }
}
