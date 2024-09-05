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
package org.knime.base.expressions.node.row.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.expressions.InsertionMode;
import org.knime.base.expressions.node.ExpressionVersionSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.scripting.editor.GenericSettingsIOManager;
import org.knime.scripting.editor.ScriptingNodeSettings;

/**
 * Settings for an Expression Row Mapper node, with loading, saving, and validation.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
class ExpressionRowMapperSettings extends ScriptingNodeSettings implements GenericSettingsIOManager {

    /**
     * The script shown in a new expression node.
     */
    static final String DEFAULT_SCRIPT = """
            # Examples:
            # 1. Calculate the sine of values in column "My Column":
            #  sin($["My Column"])
            # 2. Divide column values by a flow variable:
            #  $["My Column"] / $$["My Flow Variable"]
            # 3. Concatenate strings using the + operator:
            #  substring($["firstname"], 1, 1) + ". " + $["lastname"]
            # 4. Difference between adjacent rows:
            #  $["My Column"] - $["My Column", -1]
            #
            # If you need help, try the "Ask K-AI" button,
            # or have a look at the node description!
            """;

    /**
     * The default name of the column that is created by the expression when in append mode.
     */
    static final String DEFAULT_CREATED_COLUMN = "New Column";

    /**
     * The default output mode for the expression node.
     */
    static final InsertionMode DEFAULT_OUTPUT_MODE = InsertionMode.APPEND;

    private static final String CFG_KEY_SCRIPT = "script";

    private static final String CFG_KEY_CREATED_COLUMN = "createdColumn";

    private static final String CFG_KEY_REPLACED_COLUMN = "replacedColumn";

    private static final String CFG_KEY_OUTPUT_MODE = "columnOutputMode";

    private static final String CFG_KEY_ADDITIONAL_EXPRESSIONS = "additionalExpressions";

    private static final String JSON_KEY_SCRIPTS = "scripts";

    private static final String JSON_KEY_OUTPUT_MODES = "outputModes";

    private static final String JSON_KEY_CREATED_COLUMNS = "createdColumns";

    private static final String JSON_KEY_REPLACED_COLUMNS = "replacedColumns";

    private static final String JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES =
        "settingsAreOverriddenByFlowVariable";

    private List<InsertionMode> m_outputModes;

    private List<String> m_createdColumns;

    private List<String> m_replacedColumns;

    private ExpressionVersionSettings m_versionSettings;

    private List<String> m_scripts;

    /**
     * Create a new ExpressionNodeSettings object with the default script. The replacement column has to be specified so
     * that the frontend can display the correct column name in the drop-down box for replacement columns, but if you
     * don't anticipate it being used (e.g. because you're creating this for a node model, not a dialogue) then it can
     * be null.
     *
     * @param defaultReplacementColumn
     */
    ExpressionRowMapperSettings(final String defaultReplacementColumn) {
        this(DEFAULT_SCRIPT, DEFAULT_OUTPUT_MODE, DEFAULT_CREATED_COLUMN, defaultReplacementColumn);
    }

    /**
     * Create a new ExpressionNodeSettings object with the specified script, output mode, created column, and replaced
     * column.
     *
     * Assumes a single script.
     *
     * @param script
     * @param outputMode
     * @param createdColumn
     * @param replacedColumn
     */
    ExpressionRowMapperSettings(final String script, final InsertionMode outputMode, final String createdColumn,
        final String replacedColumn) {

        super(SettingsType.MODEL);

        this.m_versionSettings = new ExpressionVersionSettings();

        this.m_scripts = new ArrayList<>(Arrays.asList(script));
        this.m_outputModes = new ArrayList<>(Arrays.asList(outputMode));
        this.m_createdColumns = new ArrayList<>(Arrays.asList(createdColumn));
        this.m_replacedColumns = new ArrayList<>(Arrays.asList(replacedColumn));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_versionSettings.loadSettingsFrom(settings);

        m_scripts = new ArrayList<>();
        m_outputModes = new ArrayList<>();
        m_createdColumns = new ArrayList<>();
        m_replacedColumns = new ArrayList<>();

        m_scripts.add(settings.getString(CFG_KEY_SCRIPT));
        m_outputModes.add(InsertionMode.valueOf(settings.getString(CFG_KEY_OUTPUT_MODE)));
        m_createdColumns.add(settings.getString(CFG_KEY_CREATED_COLUMN));
        m_replacedColumns.add(settings.getString(CFG_KEY_REPLACED_COLUMN));

        if (settings.containsKey(CFG_KEY_ADDITIONAL_EXPRESSIONS)) {
            var additionalExpressionsConfig = settings.getConfig(CFG_KEY_ADDITIONAL_EXPRESSIONS);

            for (var key = 0; additionalExpressionsConfig.containsKey(Integer.toString(key)); key++) {
                var additionalExpressionConfig = additionalExpressionsConfig.getConfig(Integer.toString(key));

                m_scripts.add(additionalExpressionConfig.getString(CFG_KEY_SCRIPT));
                m_outputModes.add(InsertionMode.valueOf(additionalExpressionConfig.getString(CFG_KEY_OUTPUT_MODE)));
                m_createdColumns.add(additionalExpressionConfig.getString(CFG_KEY_CREATED_COLUMN));
                m_replacedColumns.add(additionalExpressionConfig.getString(CFG_KEY_REPLACED_COLUMN));
            }
        }
    }

    /**
     * @return the unmodifiable list of all column output modes
     */
    List<InsertionMode> getColumnInsertionModes() {
        return Collections.unmodifiableList(m_outputModes);
    }

    /**
     * @return the unmodifiable list of all created columns
     */
    List<String> getCreatedColumns() {
        return Collections.unmodifiableList(m_createdColumns);
    }

    /**
     * @return the unmodifiable list of all columns
     */
    List<String> getReplacedColumns() {
        return Collections.unmodifiableList(m_replacedColumns);
    }

    /**
     * @return the unmodifiable list of all scripts
     */
    List<String> getScripts() {
        return Collections.unmodifiableList(m_scripts);
    }

    /**
     * Get the list of columns that are being actively used, i.e. for each editor, if the output mode is APPEND, return
     * the column that will be appended, and if the output mode is REPLACE_EXISTING, return the column that would be
     * replaced.
     *
     * @return the unmodifiable list of columns that are being actively used
     */
    List<String> getActiveOutputColumns() {
        return IntStream.range(0, getNumScripts()) //
            .mapToObj(i -> m_outputModes.get(i) == InsertionMode.REPLACE_EXISTING ? m_replacedColumns.get(i)
                : m_createdColumns.get(i)) //
            .toList();
    }

    /**
     * @return the number of scripts
     */
    int getNumScripts() {
        return m_scripts.size();
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KEY_SCRIPT, m_scripts.get(0));
        settings.addString(CFG_KEY_OUTPUT_MODE, m_outputModes.get(0).name());
        settings.addString(CFG_KEY_CREATED_COLUMN, m_createdColumns.get(0));
        settings.addString(CFG_KEY_REPLACED_COLUMN, m_replacedColumns.get(0));

        m_versionSettings.saveSettingsTo(settings);

        var additionalExprsConfigs = settings.addConfig(CFG_KEY_ADDITIONAL_EXPRESSIONS);

        for (int i = 0; i < getNumScripts() - 1; ++i) {
            var singleExprConfig = additionalExprsConfigs.addConfig(Integer.toString(i));

            singleExprConfig.addString(CFG_KEY_SCRIPT, m_scripts.get(i + 1));
            singleExprConfig.addString(CFG_KEY_OUTPUT_MODE, m_outputModes.get(i + 1).name());
            singleExprConfig.addString(CFG_KEY_CREATED_COLUMN, m_createdColumns.get(i + 1));
            singleExprConfig.addString(CFG_KEY_REPLACED_COLUMN, m_replacedColumns.get(i + 1));
        }
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
            JSON_KEY_SCRIPTS, m_scripts, //
            JSON_KEY_OUTPUT_MODES, m_outputModes, //
            JSON_KEY_CREATED_COLUMNS, m_createdColumns, //
            JSON_KEY_REPLACED_COLUMNS, m_replacedColumns, //
            JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES, configOverWrittenByFlowVars //
        ));

        return settingsMap;
    }

    @SuppressWarnings("unchecked") // these casts are fine if the settings are correct
    @Override
    public void writeMapToNodeSettings(final Map<String, Object> data,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {

        m_scripts = (List<String>)data.get(JSON_KEY_SCRIPTS);
        m_outputModes = ((List<String>)data.get(JSON_KEY_OUTPUT_MODES)).stream().map(InsertionMode::valueOf)
            .collect(Collectors.toList());
        m_createdColumns = (List<String>)data.get(JSON_KEY_CREATED_COLUMNS);
        m_replacedColumns = (List<String>)data.get(JSON_KEY_REPLACED_COLUMNS);

        m_versionSettings.writeMapToNodeSettings(data);

        saveSettingsTo(settings);
        copyVariableSettings(previousSettings, settings);
    }

    private static boolean isEditorConfigurationOverwrittenByFlowVariable(final NodeAndVariableSettingsRO settings) {
        // Check if the first expression (at the root level of the settings) is overwritten by a flow variable
        boolean settingsOverridden = isSingleExprOverwritten(settings);

        // Check if one of the additional expressions is overwritten by a flow variable
        var additionalExprsVariables = getVariableOverwriteSubtree(settings, CFG_KEY_ADDITIONAL_EXPRESSIONS);
        if (additionalExprsVariables.isPresent()) {
            for (var key : additionalExprsVariables.get().getVariableSettingsIterable()) {
                settingsOverridden |= getVariableOverwriteSubtree(additionalExprsVariables.get(), key)
                    .map(ExpressionRowMapperSettings::isSingleExprOverwritten) //
                    .orElse(false);
            }
        }
        return settingsOverridden;
    }

    /** @return the variable settings for the given key, or an empty optional if the key is not present */
    private static Optional<VariableSettingsRO> getVariableOverwriteSubtree(final VariableSettingsRO settings,
        final String key) {
        try {
            return Optional.ofNullable(settings.getVariableSettings(key));
        } catch (InvalidSettingsException ex) { // NOSONAR
            // Note: We don't care about the exception here, we are using exceptions for control flow because there is
            // no other way to check if a key is present
            return Optional.empty();
        }
    }

    /**
     * @return <code>true</code> iff at least one of the expression defining settings is overwritten in the given
     *         variable settings tree
     */
    private static boolean isSingleExprOverwritten(final VariableSettingsRO settings) {
        return isOverriddenByFlowVariable(settings, CFG_KEY_SCRIPT)
            || isOverriddenByFlowVariable(settings, CFG_KEY_OUTPUT_MODE)
            || isOverriddenByFlowVariable(settings, CFG_KEY_CREATED_COLUMN)
            || isOverriddenByFlowVariable(settings, CFG_KEY_REPLACED_COLUMN);
    }
}
