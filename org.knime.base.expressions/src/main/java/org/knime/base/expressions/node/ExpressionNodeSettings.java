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

import org.knime.base.expressions.ExpressionRunnerUtils.ColumnInsertionMode;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.functions.BuiltInFunctions;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.scripting.editor.ScriptingNodeSettings;

/**
 * Settings of the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
final class ExpressionNodeSettings extends ScriptingNodeSettings {

    public static final String DEFAULT_SCRIPT = """
            # Welcome to the KNIME Expression Editor!
            # Here you can write expressions to manipulate data.
            # To the right you can see a function list with docs
            # for each. Here are some usage examples:
            #
            # 1. Calculate the sine of all column values:
            #  sin($["My Column"])
            # 2. Divide column values by a flow variable:
            #  $["My Column"] / $$["My Flow Variable"]
            # 3. String manipulation:
            #  substring($["firstname"], 1, 4) + $["lastname"]
            # 4. Difference between adjacent rows:
            #  $["My Column"] - $["My Column", -1]
            #
            # If you need help, try the "Ask K-AI" button!
            """;

    public static final String DEFAULT_CREATED_COLUMN = "New Column";

    public static final String DEFAULT_REPLACED_COLUMN = "";

    public static final ColumnInsertionMode DEFAULT_OUTPUT_MODE = ColumnInsertionMode.APPEND;

    public static final String CFG_KEY_CREATED_COLUMN = "createdColumn";

    public static final String CFG_KEY_REPLACED_COLUMN = "replacedColumn";

    public static final String CFG_KEY_OUTPUT_MODE = "columnOutputMode";

    public static final String CFG_KEY_LANGUAGE_VERSION = "languageVersion";

    public static final String CFG_KEY_BUILTIN_FUNCTIONS_VERSION = "builtinFunctionsVersion";

    public static final String CFG_KEY_BUILTIN_AGGREGATIONS_VERSION = "builtinAggregationsVersion";

    private ColumnInsertionMode m_outputMode;

    private String m_createdColumn;

    private String m_replacedColumn;

    private int m_languageVersion;

    private int m_builtinFunctionsVersion;

    private int m_builtinAggregationsVersion;

    ExpressionNodeSettings() {
        this(DEFAULT_SCRIPT, DEFAULT_OUTPUT_MODE, DEFAULT_CREATED_COLUMN, DEFAULT_REPLACED_COLUMN);
    }

    ExpressionNodeSettings(final String script, final ColumnInsertionMode outputMode, final String createdColumn,
        final String replacedColumn) {
        super(script, SettingsType.MODEL);

        this.m_outputMode = outputMode;
        this.m_createdColumn = createdColumn;
        this.m_replacedColumn = replacedColumn;

        // Set to the latest version by default
        // The version will be overwritten if we load an older version from the model settings
        this.m_languageVersion = Expressions.LANGUAGE_VERSION;
        this.m_builtinFunctionsVersion = BuiltInFunctions.FUNCTIONS_VERSION;
        this.m_builtinAggregationsVersion = BuiltInAggregations.AGGREGATIONS_VERSION;
    }

    static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // Just try to load the settings - if this works they are valid
        new ExpressionNodeSettings().loadViewSettings(settings);
    }

    @Override
    public void loadModelSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadModelSettings(settings);

        m_outputMode = ColumnInsertionMode.valueOf(settings.getString(CFG_KEY_OUTPUT_MODE));
        m_createdColumn = settings.getString(CFG_KEY_CREATED_COLUMN);
        m_replacedColumn = settings.getString(CFG_KEY_REPLACED_COLUMN);

        m_languageVersion = settings.getInt(CFG_KEY_LANGUAGE_VERSION);
        m_builtinFunctionsVersion = settings.getInt(CFG_KEY_BUILTIN_FUNCTIONS_VERSION);
        m_builtinAggregationsVersion = settings.getInt(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION);
    }

    public ColumnInsertionMode getColumnInsertionMode() {
        return m_outputMode;
    }

    public String getCreatedColumn() {
        return m_createdColumn;
    }

    public String getReplacedColumn() {
        return m_replacedColumn;
    }

    public String getActiveOutputColumn() {
        return m_outputMode == ColumnInsertionMode.APPEND ? m_createdColumn : m_replacedColumn;
    }

    @Override
    public void saveModelSettingsTo(final NodeSettingsWO settings) {
        super.saveModelSettingsTo(settings);

        settings.addString(CFG_KEY_OUTPUT_MODE, m_outputMode.name());
        settings.addString(CFG_KEY_CREATED_COLUMN, m_createdColumn);
        settings.addString(CFG_KEY_REPLACED_COLUMN, m_replacedColumn);

        settings.addInt(CFG_KEY_LANGUAGE_VERSION, m_languageVersion);
        settings.addInt(CFG_KEY_BUILTIN_FUNCTIONS_VERSION, m_builtinFunctionsVersion);
        settings.addInt(CFG_KEY_BUILTIN_AGGREGATIONS_VERSION, m_builtinAggregationsVersion);
    }

    static NodeSettingsService createNodeSettingsService() {
        return new ExpressionNodeSettingsService();
    }
}
