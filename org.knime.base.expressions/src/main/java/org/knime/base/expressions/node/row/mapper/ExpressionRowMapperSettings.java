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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.base.expressions.ExpressionRunnerUtils.ColumnInsertionMode;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.functions.BuiltInFunctions;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.scripting.editor.GenericSettingsIOManager;
import org.knime.scripting.editor.ScriptingNodeSettings;

/**
 * Settings for an Expression Row Mapper node, with loading, saving, and validation.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
public class ExpressionRowMapperSettings extends ScriptingNodeSettings implements GenericSettingsIOManager {

    /**
     * The script shown in a new expression node.
     */
    public static final String DEFAULT_SCRIPT = """
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
    public static final String DEFAULT_CREATED_COLUMN = "New Column";

    /**
     * The default output mode for the expression node.
     */
    public static final ColumnInsertionMode DEFAULT_OUTPUT_MODE = ColumnInsertionMode.APPEND;

    private static final String CFG_KEY_SCRIPT = "script";

    private static final String CFG_KEY_CREATED_COLUMN = "createdColumn";

    private static final String CFG_KEY_REPLACED_COLUMN = "replacedColumn";

    private static final String CFG_KEY_OUTPUT_MODE = "columnOutputMode";

    private static final String CFG_KEY_LANGUAGE_VERSION = "languageVersion";

    private static final String CFG_KEY_BUILTIN_FUNCTIONS_VERSION = "builtinFunctionsVersion";

    private static final String CFG_KEY_BUILTIN_AGGREGATIONS_VERSION = "builtinAggregationsVersion";

    private static final String CFG_KEY_ADDITIONAL_SCRIPTS = "additionalScripts";

    private static final String CFG_KEY_ADDITIONAL_OUTPUT_MODES = "additionalOutputModes";

    private static final String CFG_KEY_ADDITIONAL_CREATED_COLUMNS = "additionalCreatedColumns";

    private static final String CFG_KEY_ADDITIONAL_REPLACED_COLUMNS = "additionalReplacedColumns";

    private static final String JSON_KEY_SCRIPTS = "scripts";

    private static final String JSON_KEY_OUTPUT_MODES = "outputModes";

    private static final String JSON_KEY_CREATED_COLUMNS = "createdColumns";

    private static final String JSON_KEY_REPLACED_COLUMNS = "replacedColumns";

    private static final String JSON_KEY_LANGUAGE_VERSION = CFG_KEY_LANGUAGE_VERSION;

    private static final String JSON_KEY_FUNCTION_VERSION = CFG_KEY_BUILTIN_FUNCTIONS_VERSION;

    private static final String JSON_KEY_AGGREGATION_VERSION = CFG_KEY_BUILTIN_AGGREGATIONS_VERSION;

    private static final String JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES =
        "settingsAreOverriddenByFlowVariable";

    private List<ColumnInsertionMode> m_outputModes;

    private List<String> m_createdColumns;

    private List<String> m_replacedColumns;

    private int m_languageVersion;

    private int m_builtinFunctionsVersion;

    private int m_builtinAggregationsVersion;

    private List<String> m_scripts;

    /**
     * Create a new ExpressionNodeSettings object with the default script. The replacement column has to be specified so
     * that the frontend can display the correct column name in the drop-down box for replacement columns, but if you
     * don't anticipate it being used (e.g. because you're creating this for a node model, not a dialogue) then it can
     * be null.
     *
     * @param defaultReplacementColumn
     */
    public ExpressionRowMapperSettings(final String defaultReplacementColumn) {
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
    public ExpressionRowMapperSettings(final String script, final ColumnInsertionMode outputMode,
        final String createdColumn, final String replacedColumn) {

        super(SettingsType.MODEL);

        // Set to the latest version by default
        // The version will be overwritten if we load an older version from the model settings
        this.m_languageVersion = Expressions.LANGUAGE_VERSION;
        this.m_builtinFunctionsVersion = BuiltInFunctions.FUNCTIONS_VERSION;
        this.m_builtinAggregationsVersion = BuiltInAggregations.AGGREGATIONS_VERSION;

        this.m_scripts = new ArrayList<>(Arrays.asList(script));
        this.m_outputModes = new ArrayList<>(Arrays.asList(outputMode));
        this.m_createdColumns = new ArrayList<>(Arrays.asList(createdColumn));
        this.m_replacedColumns = new ArrayList<>(Arrays.asList(replacedColumn));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_languageVersion = settings.getInt(CFG_KEY_LANGUAGE_VERSION);
        m_builtinFunctionsVersion = settings.getInt(CFG_KEY_BUILTIN_FUNCTIONS_VERSION);
        m_builtinAggregationsVersion = settings.getInt(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION);

        if (settings.containsKey(CFG_KEY_ADDITIONAL_SCRIPTS)) {
            m_scripts = new ArrayList<>(Arrays.asList(settings.getStringArray(CFG_KEY_ADDITIONAL_SCRIPTS)));
            m_scripts.add(0, settings.getString(CFG_KEY_SCRIPT));

            m_outputModes = Arrays.stream(settings.getStringArray(CFG_KEY_ADDITIONAL_OUTPUT_MODES))
                .map(ColumnInsertionMode::valueOf).collect(Collectors.toList());
            m_outputModes.add(0, ColumnInsertionMode.valueOf(settings.getString(CFG_KEY_OUTPUT_MODE)));

            m_createdColumns =
                new ArrayList<>(Arrays.asList(settings.getStringArray(CFG_KEY_ADDITIONAL_CREATED_COLUMNS)));
            m_createdColumns.add(0, settings.getString(CFG_KEY_CREATED_COLUMN));

            m_replacedColumns =
                new ArrayList<>(Arrays.asList(settings.getStringArray(CFG_KEY_ADDITIONAL_REPLACED_COLUMNS)));
            m_replacedColumns.add(0, settings.getString(CFG_KEY_REPLACED_COLUMN));
        } else {
            m_scripts = new ArrayList<>();
            m_scripts.add(settings.getString(CFG_KEY_SCRIPT));

            m_outputModes = new ArrayList<>();
            m_outputModes.add(ColumnInsertionMode.valueOf(settings.getString(CFG_KEY_OUTPUT_MODE)));

            m_createdColumns = new ArrayList<>();
            m_createdColumns.add(settings.getString(CFG_KEY_CREATED_COLUMN));

            m_replacedColumns = new ArrayList<>();
            m_replacedColumns.add(settings.getString(CFG_KEY_REPLACED_COLUMN));
        }

        if (Stream.of(m_scripts, m_outputModes, m_createdColumns, m_replacedColumns).mapToInt(List::size).distinct()
            .count() != 1) {

            throw new InvalidSettingsException("Different number of scripts, output modes, created columns, "
                + "or replaced columns found in model settings.");
        }
    }

    /**
     * @return the unmodifiable list of all column output modes
     */
    public List<ColumnInsertionMode> getColumnInsertionModes() {
        return Collections.unmodifiableList(m_outputModes);
    }

    /**
     * @return the unmodifiable list of all created columns
     */
    public List<String> getCreatedColumns() {
        return Collections.unmodifiableList(m_createdColumns);
    }

    /**
     * @return the unmodifiable list of all columns
     */
    public List<String> getReplacedColumns() {
        return Collections.unmodifiableList(m_replacedColumns);
    }

    /**
     * @return the unmodifiable list of all scripts
     */
    public List<String> getScripts() {
        return Collections.unmodifiableList(m_scripts);
    }

    /**
     * Get the list of columns that are being actively used, i.e. for each editor, if the output mode is APPEND, return
     * the column that will be appended, and if the output mode is REPLACE_EXISTING, return the column that would be
     * replaced.
     *
     * @return the unmodifiable list of columns that are being actively used
     */
    public List<String> getActiveOutputColumns() {
        return IntStream.range(0, getNumScripts()) //
            .mapToObj(i -> m_outputModes.get(i) == ColumnInsertionMode.REPLACE_EXISTING ? m_replacedColumns.get(i)
                : m_createdColumns.get(i)) //
            .toList();
    }

    /**
     * @return the number of scripts
     */
    public int getNumScripts() {
        return m_scripts.size();
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KEY_SCRIPT, m_scripts.get(0));
        settings.addString(CFG_KEY_OUTPUT_MODE, m_outputModes.get(0).name());
        settings.addString(CFG_KEY_CREATED_COLUMN, m_createdColumns.get(0));
        settings.addString(CFG_KEY_REPLACED_COLUMN, m_replacedColumns.get(0));

        settings.addInt(CFG_KEY_LANGUAGE_VERSION, m_languageVersion);
        settings.addInt(CFG_KEY_BUILTIN_FUNCTIONS_VERSION, m_builtinFunctionsVersion);
        settings.addInt(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION, m_builtinAggregationsVersion);

        settings.addStringArray(CFG_KEY_ADDITIONAL_SCRIPTS,
            m_scripts.subList(1, m_scripts.size()).toArray(new String[0]));
        settings.addStringArray(CFG_KEY_ADDITIONAL_OUTPUT_MODES,
            m_outputModes.subList(1, m_outputModes.size()).stream().map(Enum::name).toArray(String[]::new));
        settings.addStringArray(CFG_KEY_ADDITIONAL_CREATED_COLUMNS,
            m_createdColumns.subList(1, m_createdColumns.size()).toArray(new String[0]));
        settings.addStringArray(CFG_KEY_ADDITIONAL_REPLACED_COLUMNS,
            m_replacedColumns.subList(1, m_replacedColumns.size()).toArray(new String[0]));
    }

    @Override
    public Map<String, Object> convertNodeSettingsToMap(final Map<SettingsType, NodeAndVariableSettingsRO> settings)
        throws InvalidSettingsException {

        loadSettingsFrom(settings);

        var configOverWrittenByFlowVars =
            isEditorConfigurationOverwrittenByFlowVariable(settings.get(m_scriptSettingsType));

        return Map.of( //
            JSON_KEY_SCRIPTS, m_scripts, //
            JSON_KEY_OUTPUT_MODES, m_outputModes, //
            JSON_KEY_CREATED_COLUMNS, m_createdColumns, //
            JSON_KEY_REPLACED_COLUMNS, m_replacedColumns, //
            JSON_KEY_LANGUAGE_VERSION, m_languageVersion, //
            JSON_KEY_FUNCTION_VERSION, m_builtinFunctionsVersion, //
            JSON_KEY_AGGREGATION_VERSION, m_builtinAggregationsVersion, //
            JSON_KEY_ARE_SETTINGS_OVERRIDDEN_BY_FLOW_VARIABLES, configOverWrittenByFlowVars //
        );
    }

    @SuppressWarnings("unchecked") // these casts are fine if the settings are correct
    @Override
    public void writeMapToNodeSettings(final Map<String, Object> data,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {

        m_scripts = (List<String>)data.get(JSON_KEY_SCRIPTS);
        m_outputModes = ((List<String>)data.get(JSON_KEY_OUTPUT_MODES)).stream().map(ColumnInsertionMode::valueOf)
            .collect(Collectors.toList());
        m_createdColumns = (List<String>)data.get(JSON_KEY_CREATED_COLUMNS);
        m_replacedColumns = (List<String>)data.get(JSON_KEY_REPLACED_COLUMNS);
        m_languageVersion = (int)data.get(JSON_KEY_LANGUAGE_VERSION);
        m_builtinFunctionsVersion = (int)data.get(JSON_KEY_FUNCTION_VERSION);
        m_builtinAggregationsVersion = (int)data.get(JSON_KEY_AGGREGATION_VERSION);

        saveSettingsTo(settings);
        copyVariableSettings(previousSettings, settings);
    }

    private static boolean isEditorConfigurationOverwrittenByFlowVariable(final NodeAndVariableSettingsRO settings) {
        return isOverriddenByFlowVariable(settings, CFG_KEY_SCRIPT)
            || isOverriddenByFlowVariable(settings, CFG_KEY_OUTPUT_MODE)
            || isOverriddenByFlowVariable(settings, CFG_KEY_CREATED_COLUMN)
            || isOverriddenByFlowVariable(settings, CFG_KEY_REPLACED_COLUMN)
            || isOverriddenByFlowVariable(settings, CFG_KEY_ADDITIONAL_SCRIPTS)
            || isOverriddenByFlowVariable(settings, CFG_KEY_ADDITIONAL_OUTPUT_MODES)
            || isOverriddenByFlowVariable(settings, CFG_KEY_ADDITIONAL_CREATED_COLUMNS)
            || isOverriddenByFlowVariable(settings, CFG_KEY_ADDITIONAL_REPLACED_COLUMNS);
    }
}
