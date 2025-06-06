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
 *   Aug 14, 2024 (tobias): created
 */
package org.knime.base.expressions.node.row.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.knime.base.expressions.ColumnInputUtils;
import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionCodeAssistant.ExpressionType;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.row.InputTableCache;
import org.knime.base.expressions.node.row.OutputTablePreview;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.ExpressionCompileException;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.scripting.editor.CodeGenerationRequest;
import org.knime.scripting.editor.InputOutputModel;
import org.knime.scripting.editor.ScriptingService;
import org.knime.scripting.editor.WorkflowControl;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
final class ExpressionRowFilterNodeScriptingService extends ScriptingService {

    private static final int PREVIEW_MAX_ROWS = 1000;

    private final OutputTablePreview m_tablePreview;

    /**
     * Cached function for mapping column access to output types for checking the expression types. Use
     * {@link #getColumnToTypeMapper()} to access this!
     */
    private Function<String, ReturnResult<ValueType>> m_columnToType;

    private InputTableCache m_inputTableCache;

    private ExecutionContext m_exec;

    ExpressionRowFilterNodeScriptingService(final OutputTablePreview tablePreview) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains);
        m_tablePreview = tablePreview;
        var nodeContainer = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
        m_exec = nodeContainer.createExecutionContext();
        m_inputTableCache = new InputTableCache((BufferedDataTable)getWorkflowControl().getInputData()[0], m_exec);
    }

    /** For testing with a mocked {@link WorkflowControl} only */
    ExpressionRowFilterNodeScriptingService(final WorkflowControl workflowControl) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains, workflowControl);
        m_tablePreview = null;
    }

    @Override
    public ExpressionNodeRpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    @Override
    public void onDeactivate() {
        m_tablePreview.clearTables(m_exec);
        m_columnToType = null;
        if (m_inputTableCache != null) {
            m_inputTableCache.close();
            m_inputTableCache = null;
        }
    }

    private synchronized Function<String, ReturnResult<ValueType>> getColumnToTypeMapper() {
        if (m_columnToType == null) {
            var spec = (DataTableSpec)getWorkflowControl().getInputSpec()[0];
            m_columnToType = ColumnInputUtils.columnToTypesForTypeInference(spec);
        }
        return m_columnToType;
    }

    public final class ExpressionNodeRpcService extends RpcService {

        @Override
        protected CodeGenerationRequest getCodeSuggestionRequest(final String userPrompt, final String currentCode,
            final InputOutputModel[] inputModels) {
            return ExpressionCodeAssistant.createCodeGenerationRequest(ExpressionType.FILTER, userPrompt, currentCode, inputModels);
        }

        /** @return the typed Ast for the configured expression */
        private Ast getPreparedExpression(final String script) throws ExpressionCompileException {

            var ast = Expressions.parse(script);
            var flowVarToTypeMapper =
                ExpressionRunnerUtils.flowVarToTypeForTypeInference(getSupportedFlowVariablesMap());

            Expressions.inferTypes(ast, getColumnToTypeMapper(), flowVarToTypeMapper);

            return ast;
        }

        /**
         * List of diagnostics.
         *
         * @param expression the expression to check.
         * @return list of diagnostics.
         */
        public List<ExpressionDiagnostic> getRowFilterDiagnostics(final String expression) {

            if ((DataTableSpec)getWorkflowControl().getInputSpec()[0] == null) {
                // No input table, so no column
                return ExpressionDiagnostic.NO_INPUT_CONNECTED_DIAGNOSTICS;
            }

            List<ExpressionDiagnostic> diagnostics = new ArrayList<>();

            try {
                var ast = getPreparedExpression(expression);

                var inferredType = Expressions.getInferredType(ast);

                if (!ValueType.BOOLEAN.equals(inferredType)) {
                    String message;
                    if (ValueType.OPT_BOOLEAN.equals(inferredType)) {
                        message = "The expression evaluates to BOOLEAN | MISSING. "
                            + "Use the missing coalescing operator '??' to define if rows that evaluate to MISSING "
                            + "should be included or excluded.";
                    } else {
                        message = "The expression evaluates to " + inferredType.name() + ". "
                            + "It should evaluate to BOOLEAN in order to filter out rows for which the "
                            + "filter expression evaluates to false.";
                    }
                    diagnostics.add(ExpressionDiagnostic.withSameMessage(message, DiagnosticSeverity.ERROR,
                        Expressions.getTextLocation(ast)));
                }
            } catch (ExpressionCompileException ex) {
                diagnostics.addAll(ExpressionDiagnostic.fromException(ex));
            }

            return diagnostics;
        }

        public void runRowFilterExpression(final String script, final int numPreviewRows)
            throws ExpressionCompileException, VirtualTableIncompatibleException {

            if (numPreviewRows > PREVIEW_MAX_ROWS) {
                throw new IllegalArgumentException("Number of preview rows must be at most 1000");
            }

            var warnings = new ExpressionDiagnostic[1];

            try {
                var inColTable = m_inputTableCache.getTable(numPreviewRows);
                var outputTable = ExpressionRowFilterNodeModel.applyFilterExpression( //
                    script, //
                    inColTable, //
                    getSupportedFlowVariablesMap(), //
                    m_exec, //
                    ExpressionDiagnostic.getSingleWarningMessageHandler(warnings) //
                );

                m_tablePreview.updateTables(List.of(outputTable), m_exec);
                updateOutputTable((int)m_tablePreview.numRows(), m_inputTableCache.getFullRowCount());

                if (warnings[0] != null) {
                    sendEvent("updateWarning", warnings[0]);
                }

            } catch (ExpressionEvaluationException e) {
                // TODO(AP-23937) - update the frontend to show the error
                throw new IllegalStateException(e.getMessage(), e);
            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because canceling the execution should not be possible.", e);
            }
        }
    }
}
