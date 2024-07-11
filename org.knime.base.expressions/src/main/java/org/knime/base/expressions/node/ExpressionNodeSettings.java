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
 *   Jan 11, 2024 (benjamin): created
 */
package org.knime.base.expressions.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.knime.core.webui.node.dialog.NodeSettingsService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;

/**
 * Settings of the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
final class ExpressionNodeSettings {

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

    public static final String DEFAULT_CREATED_COLUMN = "New Column";

    public static final String DEFAULT_REPLACED_COLUMN = "";

    public static final ColumnInsertionMode DEFAULT_OUTPUT_MODE = ColumnInsertionMode.APPEND;

    public static final int DEFAULT_NUM_ADDITIONAL_SCRIPTS = 0;

    public static final String CFG_KEY_SCRIPT = "script";

    public static final String CFG_KEY_CREATED_COLUMN = "createdColumn";

    public static final String CFG_KEY_REPLACED_COLUMN = "replacedColumn";

    public static final String CFG_KEY_OUTPUT_MODE = "columnOutputMode";

    public static final String CFG_KEY_LANGUAGE_VERSION = "languageVersion";

    public static final String CFG_KEY_BUILTIN_FUNCTIONS_VERSION = "builtinFunctionsVersion";

    public static final String CFG_KEY_BUILTIN_AGGREGATIONS_VERSION = "builtinAggregationsVersion";

    public static final String CFG_KEY_ADDITIONAL_SCRIPTS = "additionalScripts";

    public static final String CFG_KEY_ADDITIONAL_OUTPUT_MODES = "additionalOutputModes";

    public static final String CFG_KEY_ADDITIONAL_CREATED_COLUMNS = "additionalCreatedColumns";

    public static final String CFG_KEY_ADDITIONAL_REPLACED_COLUMNS = "additionalReplacedColumns";

    public static final String JSON_KEY_SCRIPTS = "scripts";

    public static final String JSON_KEY_OUTPUT_MODES = "outputModes";

    public static final String JSON_KEY_CREATED_COLUMNS = "createdColumns";

    public static final String JSON_KEY_REPLACED_COLUMNS = "replacedColumns";

    private List<ColumnInsertionMode> m_outputModes;

    private List<String> m_createdColumns;

    private List<String> m_replacedColumns;

    private int m_languageVersion;

    private int m_builtinFunctionsVersion;

    private int m_builtinAggregationsVersion;

    private List<String> m_scripts;

    ExpressionNodeSettings() {
        this(DEFAULT_SCRIPT, DEFAULT_OUTPUT_MODE, DEFAULT_CREATED_COLUMN, DEFAULT_REPLACED_COLUMN);
    }

    ExpressionNodeSettings(final String script, final ColumnInsertionMode outputMode, final String createdColumn,
        final String replacedColumn) {

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

    static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // Just try to load the settings - if this works they are valid
        new ExpressionNodeSettings().loadModelSettings(settings);
    }

    public void loadModelSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

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

            throw new InvalidSettingsException(
                "Different number of scripts, output modes, created columns, or replaced columns found in model settings.");
        }
    }

    public void readModelSettingsFromJson(final ObjectNode settingsJson) {
        m_languageVersion = settingsJson.get(CFG_KEY_LANGUAGE_VERSION).asInt();
        m_builtinFunctionsVersion = settingsJson.get(CFG_KEY_BUILTIN_FUNCTIONS_VERSION).asInt();
        m_builtinAggregationsVersion = settingsJson.get(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION).asInt();

        m_scripts =
            Streams.stream(settingsJson.get(JSON_KEY_SCRIPTS)).map(JsonNode::asText).collect(Collectors.toList());
        m_outputModes = Streams.stream(settingsJson.get(JSON_KEY_OUTPUT_MODES)).map(JsonNode::asText)
            .map(ColumnInsertionMode::valueOf).collect(Collectors.toList());
        m_createdColumns = Streams.stream(settingsJson.get(JSON_KEY_CREATED_COLUMNS)).map(JsonNode::asText)
            .collect(Collectors.toList());
        m_replacedColumns = Streams.stream(settingsJson.get(JSON_KEY_REPLACED_COLUMNS)).map(JsonNode::asText)
            .collect(Collectors.toList());
    }

    public void writeModelSettingsToJson(final ObjectNode settingsJson) {
        settingsJson.put(CFG_KEY_LANGUAGE_VERSION, m_languageVersion);
        settingsJson.put(CFG_KEY_BUILTIN_FUNCTIONS_VERSION, m_builtinFunctionsVersion);
        settingsJson.put(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION, m_builtinAggregationsVersion);

        m_scripts.forEach(script -> settingsJson.withArray(JSON_KEY_SCRIPTS).add(script));
        m_outputModes.forEach(mode -> settingsJson.withArray(JSON_KEY_OUTPUT_MODES).add(mode.name()));
        m_createdColumns.forEach(column -> settingsJson.withArray(JSON_KEY_CREATED_COLUMNS).add(column));
        m_replacedColumns.forEach(column -> settingsJson.withArray(JSON_KEY_REPLACED_COLUMNS).add(column));
    }

    public List<ColumnInsertionMode> getColumnInsertionModes() {
        return Collections.unmodifiableList(m_outputModes);
    }

    public List<String> getCreatedColumns() {
        return Collections.unmodifiableList(m_createdColumns);
    }

    public List<String> getReplacedColumns() {
        return Collections.unmodifiableList(m_replacedColumns);
    }

    public List<String> getScripts() {
        return Collections.unmodifiableList(m_scripts);
    }

    public List<String> getActiveOutputColumns() {
        return IntStream.range(0, getNumScripts()) //
            .mapToObj(i -> m_outputModes.get(i) == ColumnInsertionMode.REPLACE_EXISTING ? m_replacedColumns.get(i)
                : m_createdColumns.get(i)) //
            .collect(Collectors.toList());
    }

    public int getNumScripts() {
        return m_scripts.size();
    }

    public void saveModelSettingsTo(final NodeSettingsWO settings) {
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

    static NodeSettingsService createNodeSettingsService() {
        return new ExpressionNodeSettingsService();
    }
}
