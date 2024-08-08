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
 *   Apr 30, 2024 (david): created
 */
package org.knime.base.expressions.node;

import static org.knime.scripting.editor.SettingsServiceUtils.copyVariableSettings;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.VariableSettingsWO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Communicates settings between the node model and the web UI.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ExpressionNodeSettingsService implements NodeSettingsService {

    /**
     * Add additional settings to the node settings by reading the JSON object, which came from the frontend.
     *
     * @param settingsJson
     * @param settings
     */
    protected void addAdditionalSettingsToNodeSettings(final ObjectNode settingsJson,
        final Map<SettingsType, ? extends NodeSettingsWO> settings) {

        var expressionSettings = new ExpressionNodeSettings();
        expressionSettings.readModelSettingsFromJson(settingsJson);
        expressionSettings.saveModelSettingsTo(settings.get(SettingsType.MODEL));
    }

    /**
     * Add additional settings to the JSON object, which will be sent to the frontend.
     *
     * @param settings
     * @param settingsJson
     */
    protected void putAdditionalSettingsToJson(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final ObjectNode settingsJson) {
        // load settings

        try {
            var expressionSettings = new ExpressionNodeSettings();
            expressionSettings.loadModelSettings(settings.get(SettingsType.MODEL));
            expressionSettings.writeModelSettingsToJson(settingsJson);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Implementation error - Could not load expression settings", ex);
        }
    }

    private static boolean isSetByFlowVariable(final NodeAndVariableSettingsRO settings, final String key) {
        try {
            return settings.isVariableSetting(key) && settings.getUsedVariable(key) != null;
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(
                "Implementation error - Could not load expression settings and failed to query if "
                    + "a flow variable was used for key: " + key + " in the settings.",
                e);
        }
    }

    private static boolean isEditorConfigurationOverwrittenByFlowVariable(final NodeAndVariableSettingsRO settings) {
        return isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_SCRIPT)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_OUTPUT_MODE)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_CREATED_COLUMN)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_REPLACED_COLUMN)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_ADDITIONAL_SCRIPTS)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_ADDITIONAL_OUTPUT_MODES)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_ADDITIONAL_CREATED_COLUMNS)
            || isSetByFlowVariable(settings, ExpressionNodeSettings.CFG_KEY_ADDITIONAL_REPLACED_COLUMNS);
    }

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {

        // Construct the JSON output
        var settingsJson = new ObjectMapper().createObjectNode();
        putAdditionalSettingsToJson(settings, settingsJson);

        settingsJson.put("setByFlowVariables",
            isEditorConfigurationOverwrittenByFlowVariable(settings.get(SettingsType.MODEL)));

        return settingsJson.toString();
    }

    @Override
    public void toNodeSettings(final String textSettings,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {

        try {
            var settingsJson = (ObjectNode)new ObjectMapper().readTree(textSettings);
            addAdditionalSettingsToNodeSettings(settingsJson, settings);
            setVariableSettings(previousSettings, settings);
        } catch (JsonProcessingException e) {
            // Should not happen because the frontend gives a correct JSON settings
            throw new IllegalStateException(e);
        }

    }

    private static void setVariableSettings(final Map<SettingsType, ? extends VariableSettingsRO> previousSettings,
        final Map<SettingsType, ? extends VariableSettingsWO> settings) {
        for (var settingsType : settings.keySet()) {
            copyVariableSettings(previousSettings.get(settingsType), settings.get(settingsType));
        }
    }
}
