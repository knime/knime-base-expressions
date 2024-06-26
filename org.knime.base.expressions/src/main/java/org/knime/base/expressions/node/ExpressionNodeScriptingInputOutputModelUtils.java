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
import java.util.Optional;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.scripting.editor.InputOutputModel;
import org.knime.scripting.editor.WorkflowControl.InputPortInfo;

import com.google.common.base.Preconditions;

/**
 * Utilities for providing the {@link InputOutputModel} for the scripting editor dialog.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionNodeScriptingInputOutputModelUtils {

    // escapeQuotes is a HandleBars helper registered in the frontend
    private static final String COLUMN_ALIAS_TEMPLATE = "$[\"{{{escapeDblQuotes subItems.[0]}}}\"]";

    private static final String FLOWVAR_ALIAS_TEMPLATE = "$$[\"{{{escapeDblQuotes subItems.[0]}}}\"]";

    private ExpressionNodeScriptingInputOutputModelUtils() {
        // utility class
    }

    static InputOutputModel getFlowVariableInputs(final Collection<FlowVariable> flowVariables) {
        return InputOutputModel.createFromFlowVariables( //
            flowVariables, //
            null, //
            FLOWVAR_ALIAS_TEMPLATE, //
            null, // no required import
            false, // no multiple selection
            type -> Optional.ofNullable(ExpressionRunnerUtils.mapVariableToValueType(type)).map(ValueType::toString)
                .orElse(type.toString()) // try to convert to an expression type, otherwise fallback to type name
        );
    }

    static List<InputOutputModel> getInputObjects(final InputPortInfo[] inputPorts) {

        Preconditions.checkArgument(inputPorts.length == 1, "expected one input port");

        final var spec = inputPorts[0].portSpec();
        if (spec != null) {
            Preconditions.checkArgument(spec instanceof DataTableSpec, "expected data table spec");
            return List.of(InputOutputModel.createFromTableSpec( //
                "Input table", //
                (DataTableSpec)spec, //
                null, //
                COLUMN_ALIAS_TEMPLATE, //
                false, // no multiple selection
                null, // no required import
                type -> Optional.ofNullable(ExpressionRunnerUtils.mapDataTypeToValueType(type)) //
                    .map(ValueType::baseType) //
                    .map(ValueType::toString) //
                    .orElse(null) // extract type name, or null if unsupported
            ));
        } else {
            return List.of(InputOutputModel.createForNonAvailableTable( //
                "Input table", //
                null, //
                COLUMN_ALIAS_TEMPLATE, //
                null, // no required import
                false // no multiple selection
            ));
        }
    }

    static List<InputOutputModel> getOutputObjects() {
        return List.of(InputOutputModel.createForNonAvailableTable("Output table", null, null, null, false));
    }
}
