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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.knime.base.expressions.node.ExpressionNodeModel.ColumnInsertionMode;
import org.knime.base.expressions.node.ExpressionNodeModel.NewColumnPosition;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.scripting.editor.InputOutputModel;
import org.knime.scripting.editor.ScriptingService;

/**
 *
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionNodeScriptingService extends ScriptingService {

    private static final int DIALOG_PREVIEW_NUM_ROWS = 10;

    static final Set<VariableType<?>> SUPPORTED_FLOW_VARIABLE_TYPES =
        Set.of(BooleanType.INSTANCE, DoubleType.INSTANCE, IntType.INSTANCE, LongType.INSTANCE, StringType.INSTANCE);

    ExpressionNodeScriptingService() {
        super(null, (flowVar) -> SUPPORTED_FLOW_VARIABLE_TYPES.contains(flowVar.getVariableType()));
    }

    @Override
    public RpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    public final class ExpressionNodeRpcService extends RpcService {

        @Override
        public InputOutputModel getFlowVariableInputs() {
            return ExpressionNodeScriptingInputOutputModelUtils.getFlowVariableInputs(getFlowVariables());
        }

        @Override
        public List<InputOutputModel> getInputObjects() {
            return ExpressionNodeScriptingInputOutputModelUtils.getInputObjects(getWorkflowControl().getInputInfo());
        }

        @Override
        public List<InputOutputModel> getOutputObjects() {
            return ExpressionNodeScriptingInputOutputModelUtils.getOutputObjects();
        }

        @Override
        protected String getCodeSuggestion(final String userPrompt, final String currentCode) throws IOException {
            // TODO(code-assistant) implement
            return null;
        }

        public void runExpression(final String expression) {
            var inTable = (BufferedDataTable)getWorkflowControl().getInputData()[0];
            if (inTable == null) {
                throw new IllegalStateException("Input table not available");
            }

            var numRows = (int) Math.min(DIALOG_PREVIEW_NUM_ROWS, inTable.size());
            var result = new String[numRows];
            try (var expressionResultTable = ExpressionNodeModel.applyExpression(inTable, expression,
                new NewColumnPosition(ColumnInsertionMode.APPEND, "result"), () -> 1)) {
                try (var cursor =
                    expressionResultTable.cursor(TableFilter.filterRangeOfRows(0, numRows))) {
                    var resultIdx = expressionResultTable.getDataTableSpec().getNumColumns() - 1;
                    for (var i = 0; i < numRows && cursor.canForward(); i++) {
                        result[i] = cursor.forward().getValue(resultIdx).materializeDataCell().toString();
                    }
                }
            }
            addConsoleOutputEvent(new ConsoleText(formatResult(result), false));
        }

        private static String formatResult(final String[] result) {
            var sb = new StringBuilder();
            sb.append("Result on the first ").append(result.length).append(" rows:").append('\n');
            for (var value : result) {
                sb.append('\t').append(value).append('\n');
            }
            sb.append('\n');
            return sb.toString();
        }
    }
}
