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

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.scripting.editor.ScriptingNodeSettingsService;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ExpressionNodeSettingsService extends ScriptingNodeSettingsService {

    public ExpressionNodeSettingsService() {
        super(SettingsType.MODEL);
    }

    @Override
    protected void addAdditionalSettingsToNodeSettings(final ObjectNode settingsJson,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        // save settings
        try {
            var columnOutputMode = settingsJson.get(ExpressionNodeSettings.CFG_KEY_OUTPUT_MODE)
                .asText(ExpressionNodeSettings.DEFAULT_OUTPUT_MODE.name());

            var outputNewColumn = settingsJson.get(ExpressionNodeSettings.CFG_KEY_CREATED_COLUMN)
                .asText(ExpressionNodeSettings.DEFAULT_CREATED_COLUMN);

            var outputReplacementColumn = settingsJson.get(ExpressionNodeSettings.CFG_KEY_REPLACED_COLUMN)
                .asText(ExpressionNodeSettings.DEFAULT_REPLACED_COLUMN);

            var modelSettings = settings.get(SettingsType.MODEL);
            modelSettings.addString(ExpressionNodeSettings.CFG_KEY_OUTPUT_MODE, columnOutputMode);
            modelSettings.addString(ExpressionNodeSettings.CFG_KEY_CREATED_COLUMN, outputNewColumn);
            modelSettings.addString(ExpressionNodeSettings.CFG_KEY_REPLACED_COLUMN, outputReplacementColumn);
        } catch (NullPointerException ex) { // NOSONAR this is nicer than 3 separate null checks
            // Can happen if we're given invalid settings by the frontend
            throw new IllegalStateException(
                "Error saving expression editor settings - this might mean the frontend gave us invalid input", //
                ex //
            );
        }
    }

    @Override
    protected void putAdditionalSettingsToJson(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs, final ObjectNode settingsJson) {
        // load settings

        var modelSettings = settings.get(SettingsType.MODEL);

        try {
            settingsJson.put(ExpressionNodeSettings.CFG_KEY_OUTPUT_MODE,
                modelSettings.getString(ExpressionNodeSettings.CFG_KEY_OUTPUT_MODE));
            settingsJson.put(ExpressionNodeSettings.CFG_KEY_CREATED_COLUMN,
                modelSettings.getString(ExpressionNodeSettings.CFG_KEY_CREATED_COLUMN));
            settingsJson.put(ExpressionNodeSettings.CFG_KEY_REPLACED_COLUMN,
                modelSettings.getString(ExpressionNodeSettings.CFG_KEY_REPLACED_COLUMN));
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Error loading expression settings. This is an implementation error", ex);
        }
    }

}
