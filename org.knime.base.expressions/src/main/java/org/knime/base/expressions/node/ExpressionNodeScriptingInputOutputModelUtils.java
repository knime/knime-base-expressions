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
 *   Jan 12, 2024 (benjamin): created
 */
package org.knime.base.expressions.node;

import java.util.Collection;
import java.util.List;

import org.knime.base.expressions.ColumnInputUtils;
import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel.InputOutputModelSubItem;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel.InputOutputModelSubItemType;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl.InputPortInfo;

import com.google.common.base.Preconditions;

/**
 * Utilities for providing the {@link InputOutputModel} for the scripting editor dialog.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ExpressionNodeScriptingInputOutputModelUtils {

    private static final List<InputOutputModelSubItem> ROW_INFO_SUB_ITEMS = List.of( //
        new InputOutputModelSubItem( //
            "ROW_NUMBER", InputOutputModelSubItemType.fromDisplayName(ValueType.INTEGER.name()), true, "$[ROW_NUMBER]"),
        new InputOutputModelSubItem( //
            "ROW_INDEX", InputOutputModelSubItemType.fromDisplayName(ValueType.INTEGER.name()), true, "$[ROW_INDEX]"),
        new InputOutputModelSubItem( //
            "ROW_ID", InputOutputModelSubItemType.fromDisplayName(ValueType.STRING.name()), true, "$[ROW_ID]"));

    // escapeDblQuotes is a Handlebars.js helper registered in the frontend
    private static final String COLUMN_ALIAS_TEMPLATE = """
            {{~#if subItems.[0].insertionText~}}
                {{ subItems.[0].insertionText }}
            {{~else~}}
                $[" {{~{ escapeDblQuotes subItems.[0].name }~}} "]
            {{~/if~}}
            """;

    private static final String FLOWVAR_ALIAS_TEMPLATE = """
            {{~#if subItems.[0].insertionText~}}
                {{ subItems.[0].insertionText }}
            {{~else~}}
                $$[" {{~{ escapeDblQuotes subItems.[0].name }~}} "]
            {{~/if~}}
            """;

    private ExpressionNodeScriptingInputOutputModelUtils() {
        // utility class
    }

    /**
     * @param flowVariables
     * @return the {@link InputOutputModel} for the flow variables
     */
    public static InputOutputModel getFlowVariableInputs(final Collection<FlowVariable> flowVariables) {
        return InputOutputModel.flowVariables() //
            .subItemCodeAliasTemplate(FLOWVAR_ALIAS_TEMPLATE) //
            .subItems( //
                flowVariables, //
                type -> ExpressionRunnerUtils.mapVariableToValueType(type).toString(), //
                ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains //
            ) //
            .build();
    }

    /**
     * Return the single input object for expression nodes with a single input table. The resulting list will always
     * have length 1.
     *
     * @param inputPorts (a list of size 1)
     * @return a list of size 1 of {@link InputOutputModel} for the input objects
     */
    public static List<InputOutputModel> getTableInputObjects(final InputPortInfo[] inputPorts) {
        Preconditions.checkArgument(inputPorts.length == 1, "expected one input port");
        final var spec = inputPorts[0].portSpec();

        // expecting an input table
        if (spec instanceof DataTableSpec dataTablespec) {
            return List.of(InputOutputModel.table() //
                .name("Input table") //
                .subItemCodeAliasTemplate(COLUMN_ALIAS_TEMPLATE) //
                .subItems(ROW_INFO_SUB_ITEMS) //
                .subItems( //
                    dataTablespec, //
                    type -> ColumnInputUtils.mapDataTypeToValueType(type).baseType().name(), //
                    type -> ColumnInputUtils.mapDataTypeToValueType(type) != null //
                ) //
                .build() //
            ); //
        } else {
            return List.of(InputOutputModel.table() //
                .name("Input table") //
                .subItemCodeAliasTemplate(COLUMN_ALIAS_TEMPLATE) //
                .build() //
            );
        }
    }
}
