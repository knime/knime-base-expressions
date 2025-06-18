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
package org.knime.base.expressions.node.variable;

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
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.scripting.editor.GenericSettingsIOManager;
import org.knime.scripting.editor.ScriptingNodeSettings;

/**
 * Settings for an Expression Flow Variable node, with loading, saving, and validation.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
class ExpressionFlowVariableSettings extends ScriptingNodeSettings implements GenericSettingsIOManager {

    /**
     * The script shown in a new expression node.
     */
    static final String DEFAULT_SCRIPT = """
            # Write a flow variable expression, e.g.
            #  substring($$["firstname"], 1, 1) + ". " + $$["lastname"]
            # See node description for full syntax
            """;

    /**
     * The default name of the flow variable that is created by the expression when in append mode.
     */
    static final String DEFAULT_CREATED_FLOW_VARIABLE = "New Flow Variable";

    /**
     * The default value of the replacement variable option. An empty string means that nothing is selected, causing the
     * auto-selection to be triggered.
     */
    static final String DEFAULT_REPLACEMENT_FLOW_VARIABLE = "";

    /**
     * The default name of the return type of a flow variable expression In case of an expression returning an
     * expression language type `INTEGER` the user can configure the return type to be `Number (integer)` or `Number
     * (double)`
     */
    static final FlowVariableTypeNames DEFAULT_FLOW_VARIABLE_RETURN_TYPE = FlowVariableTypeNames.UNKNOWN;

    /**
     * The default output mode for the expression node.
     */

    // REMOVE unnecessary differentiation between first and additional
    static final InsertionMode DEFAULT_OUTPUT_MODE = InsertionMode.APPEND;

    private static final String CFG_KEY_ADDITIONAL_EXPRESSIONS = "additionalExpressions";

    private static final String CFG_KEY_SCRIPT = "script";

    private static final String CFG_KEY_OUTPUT_MODE = "outputMode";

    private static final String CFG_KEY_CREATED_FLOW_VARIABLE = "createdFlowVariable";

    private static final String CFG_KEY_REPLACED_FLOW_VARIABLE = "replacedFlowVariable";

    private static final String CFG_KEY_RETURN_TYPES = "flowVariableReturnType";

    private static final String JSON_KEY_SCRIPTS = "scripts";

    private static final String JSON_KEY_OUTPUT_MODES = "flowVariableOutputModes";

    private static final String JSON_KEY_CREATED_FLOW_VARIABLES = "createdFlowVariables";

    private static final String JSON_KEY_REPLACED_FLOW_VARIABLES = "replacedFlowVariables";

    private static final String JSON_KEY_RETURN_TYPES = "flowVariableReturnTypes";

    private static final String JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES =
        "settingsAreOverriddenByFlowVariable";

    private List<InsertionMode> m_outputModes;

    private List<String> m_createdFlowVariables;

    private List<String> m_replacedFlowVariables;

    private List<String> m_scripts;

    enum FlowVariableTypeNames {
            STRING("String"), //
            LONG("Long"), //
            INTEGER("Integer"), //
            BOOLEAN("Boolean"), //
            DOUBLE("Double"), //
            UNKNOWN("Unknown");

        private final String m_typeName;

        FlowVariableTypeNames(final String typeName) {
            m_typeName = typeName;
        }

        @Override
        public String toString() {
            return m_typeName;
        }

        /**
         * @return the typeName
         */
        public String getTypeName() {
            return m_typeName;
        }

        public static FlowVariableTypeNames getByTypeName(final String typeName) {
            return Arrays.stream(values()).filter(v -> v.m_typeName.equals(typeName)).findFirst().orElse(UNKNOWN);
        }
    }

    private List<FlowVariableTypeNames> m_flowVariableReturnTypes;

    private ExpressionVersionSettings m_versionSettings;

    /** Create a new ExpressionNodeSettings object with the default script. */
    ExpressionFlowVariableSettings() {
        this(DEFAULT_SCRIPT, DEFAULT_OUTPUT_MODE, DEFAULT_CREATED_FLOW_VARIABLE, DEFAULT_FLOW_VARIABLE_RETURN_TYPE,
            DEFAULT_REPLACEMENT_FLOW_VARIABLE);
    }

    /**
     * Create a new ExpressionNodeSettings object with the specified script, output mode, created flow variable, and
     * replaced flow variable.
     *
     * Assumes a single script.
     *
     * @param script
     * @param outputMode
     * @param createdFlowVariable
     * @param replacedFlowVariable
     */
    ExpressionFlowVariableSettings(final String script, final InsertionMode outputMode,
        final String createdFlowVariable, final FlowVariableTypeNames returnType, final String replacedFlowVariable) {

        super(SettingsType.MODEL);

        this.m_versionSettings = new ExpressionVersionSettings();

        this.m_scripts = new ArrayList<>(Arrays.asList(script));
        this.m_outputModes = new ArrayList<>(Arrays.asList(outputMode));
        this.m_createdFlowVariables = new ArrayList<>(Arrays.asList(createdFlowVariable));
        this.m_replacedFlowVariables = new ArrayList<>(Arrays.asList(replacedFlowVariable));
        this.m_flowVariableReturnTypes = new ArrayList<>(Arrays.asList(returnType));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_versionSettings.loadSettingsFrom(settings);

        m_scripts = new ArrayList<>();
        m_outputModes = new ArrayList<>();
        m_createdFlowVariables = new ArrayList<>();
        m_replacedFlowVariables = new ArrayList<>();
        m_flowVariableReturnTypes = new ArrayList<>();

        if (settings.containsKey(CFG_KEY_ADDITIONAL_EXPRESSIONS)) {
            var additionalExpressionsConfig = settings.getConfig(CFG_KEY_ADDITIONAL_EXPRESSIONS);

            for (var key = 0; additionalExpressionsConfig.containsKey(Integer.toString(key)); key++) {
                var additionalExpressionConfig = additionalExpressionsConfig.getConfig(Integer.toString(key));

                m_scripts.add(additionalExpressionConfig.getString(CFG_KEY_SCRIPT));
                m_outputModes.add(InsertionMode.valueOf(additionalExpressionConfig.getString(CFG_KEY_OUTPUT_MODE)));
                m_createdFlowVariables.add(additionalExpressionConfig.getString(CFG_KEY_CREATED_FLOW_VARIABLE));
                m_replacedFlowVariables.add(additionalExpressionConfig.getString(CFG_KEY_REPLACED_FLOW_VARIABLE));
                m_flowVariableReturnTypes.add(
                    FlowVariableTypeNames.getByTypeName(additionalExpressionConfig.getString(CFG_KEY_RETURN_TYPES)));
            }
        }
    }

    /**
     * @return the unmodifiable list of all flow variable output modes
     */
    List<InsertionMode> getFlowVariableInsertionModes() {
        return Collections.unmodifiableList(m_outputModes);
    }

    /**
     * @return the unmodifiable list of all created flow variables
     */
    List<String> getCreatedFlowVariables() {
        return Collections.unmodifiableList(m_createdFlowVariables);
    }

    /**
     * @return the unmodifiable list of all flow variables
     */
    List<String> getReplacedFlowVariables() {
        return Collections.unmodifiableList(m_replacedFlowVariables);
    }

    /**
     * @return the unmodifiable list of all return types
     */
    List<FlowVariableTypeNames> getReturnTypes() {
        return Collections.unmodifiableList(m_flowVariableReturnTypes);
    }

    /**
     * @return the unmodifiable list of all scripts
     */
    List<String> getScripts() {
        return Collections.unmodifiableList(m_scripts);
    }

    /**
     * Get the list of flow variables that are being actively used, i.e. for each editor, if the output mode is APPEND,
     * return the flow variable that will be appended, and if the output mode is REPLACE_EXISTING, return the flow
     * variable that would be replaced.
     *
     * @return the unmodifiable list of flow variables that are being actively used
     */
    List<String> getActiveOutputFlowVariables() {
        return IntStream.range(0, getNumScripts()) //
            .mapToObj(i -> m_outputModes.get(i) == InsertionMode.REPLACE_EXISTING ? m_replacedFlowVariables.get(i)
                : m_createdFlowVariables.get(i)) //
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

        m_versionSettings.saveSettingsTo(settings);

        var additionalExprsConfigs = settings.addConfig(CFG_KEY_ADDITIONAL_EXPRESSIONS);

        for (int i = 0; i < getNumScripts(); ++i) {
            var singleExprConfig = additionalExprsConfigs.addConfig(Integer.toString(i));

            singleExprConfig.addString(CFG_KEY_SCRIPT, m_scripts.get(i));
            singleExprConfig.addString(CFG_KEY_OUTPUT_MODE, m_outputModes.get(i).name());
            singleExprConfig.addString(CFG_KEY_CREATED_FLOW_VARIABLE, m_createdFlowVariables.get(i));
            singleExprConfig.addString(CFG_KEY_REPLACED_FLOW_VARIABLE, m_replacedFlowVariables.get(i));
            singleExprConfig.addString(CFG_KEY_RETURN_TYPES, m_flowVariableReturnTypes.get(i).getTypeName());

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
            JSON_KEY_CREATED_FLOW_VARIABLES, m_createdFlowVariables, //
            JSON_KEY_REPLACED_FLOW_VARIABLES, m_replacedFlowVariables, //
            JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES, configOverWrittenByFlowVars, //
            JSON_KEY_RETURN_TYPES, m_flowVariableReturnTypes.stream() //
                .map(FlowVariableTypeNames::getTypeName).toList() //
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
        m_createdFlowVariables = (List<String>)data.get(JSON_KEY_CREATED_FLOW_VARIABLES);
        m_replacedFlowVariables = (List<String>)data.get(JSON_KEY_REPLACED_FLOW_VARIABLES);
        m_flowVariableReturnTypes =
            ((List<String>)data.get(JSON_KEY_RETURN_TYPES)).stream().map(FlowVariableTypeNames::getByTypeName).toList();

        m_versionSettings.writeMapToNodeSettings(data);

        final var extractedSettings = new NodeSettings("extracted settings");
        saveSettingsTo(extractedSettings);

        NodeSettingsCorrectionUtil.correctNodeSettingsRespectingFlowVariables(new ConfigMappings(List.of()),
            extractedSettings, previousSettings.get(SettingsType.MODEL), previousSettings.get(SettingsType.MODEL));

        extractedSettings.copyTo(settings.get(SettingsType.MODEL));

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
                    .map(ExpressionFlowVariableSettings::isSingleExprOverwritten) //
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
            || isOverriddenByFlowVariable(settings, CFG_KEY_CREATED_FLOW_VARIABLE)
            || isOverriddenByFlowVariable(settings, CFG_KEY_REPLACED_FLOW_VARIABLE);
    }
}
