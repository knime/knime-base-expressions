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
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ExpressionNodeSettingsService implements NodeSettingsService {

    public ExpressionNodeSettingsService() {
    }

    protected void addAdditionalSettingsToNodeSettings(final ObjectNode settingsJson,
        final Map<SettingsType, ? extends NodeSettingsWO> settings) {

        var expressionSettings = new ExpressionNodeSettings();
        expressionSettings.readModelSettingsFromJson(settingsJson);
        expressionSettings.saveModelSettingsTo(settings.get(SettingsType.MODEL));
    }

    protected void putAdditionalSettingsToJson(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs, final ObjectNode settingsJson) {
        // load settings

        try {
            var expressionSettings = new ExpressionNodeSettings();
            expressionSettings.loadModelSettings(settings.get(SettingsType.MODEL));
            expressionSettings.writeModelSettingsToJson(settingsJson);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Implementation error - Could not load expression settings", ex);
        }
    }

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {

        try {
            String scriptUsedFlowVariable = null;
            var settingsForScript = settings.get(SettingsType.MODEL);
            if (settingsForScript.isVariableSetting(ExpressionNodeSettings.CFG_KEY_SCRIPT)) {
                scriptUsedFlowVariable = settingsForScript.getUsedVariable(ExpressionNodeSettings.CFG_KEY_SCRIPT);
            }

            // Construct the JSON output
            var settingsJson = new ObjectMapper().createObjectNode() //
                .put("scriptUsedFlowVariable", scriptUsedFlowVariable);

            putAdditionalSettingsToJson(settings, specs, settingsJson);

            return settingsJson.toString();
        } catch (InvalidSettingsException e) {
            // IllegalSettings: Should not happen because we do not save invalid settings
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void toNodeSettings(final String textSettings,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {

        try {
            var settingsJson = (ObjectNode)new ObjectMapper().readTree(textSettings);
            addAdditionalSettingsToNodeSettings(settingsJson, settings);
            setVariableSettings(settingsJson, previousSettings, settings);
        } catch (JsonProcessingException e) {
            // Should not happen because the frontend gives a correct JSON settings
            throw new IllegalStateException(e);
        }

    }

    private static void setVariableSettings(final ObjectNode settingsJson,
        final Map<SettingsType, ? extends VariableSettingsRO> previousSettings,
        final Map<SettingsType, ? extends VariableSettingsWO> settings) {
        for (var settingsType : settings.keySet()) {
            copyVariableSettings(previousSettings.get(settingsType), settings.get(settingsType));
        }
    }
}
