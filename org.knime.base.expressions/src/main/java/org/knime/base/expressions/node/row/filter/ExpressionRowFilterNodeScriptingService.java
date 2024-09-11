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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.row.InputTableCache;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.scripting.editor.ScriptingService;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
final class ExpressionRowFilterNodeScriptingService extends ScriptingService {

    private static final int PREVIEW_MAX_ROWS = 1000;

    /**
     * Cached function for mapping column access to output types for checking the expression types. Use
     * {@link #getColumnToTypeMapper()} to access this!
     */
    private Function<String, ReturnResult<ValueType>> m_columnToType;

    private InputTableCache m_inputTableCache;

    private final AtomicReference<BufferedDataTable> m_outputBufferTableReference;

    private final Runnable m_cleanUpTableViewDataService;

    ExpressionRowFilterNodeScriptingService(final AtomicReference<BufferedDataTable> outputTableRef,
        final Runnable cleanUpTableViewDataService) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains);
        m_outputBufferTableReference = outputTableRef;
        m_cleanUpTableViewDataService = cleanUpTableViewDataService;
        m_inputTableCache = new InputTableCache((BufferedDataTable)getWorkflowControl().getInputData()[0]);
    }

    @Override
    public RpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    @Override
    public void onDeactivate() {
        m_columnToType = null;
        m_inputTableCache = null;
    }

    private synchronized Function<String, ReturnResult<ValueType>> getColumnToTypeMapper() {
        if (m_columnToType == null) {
            var spec = (DataTableSpec)getWorkflowControl().getInputSpec()[0];
            m_columnToType = ExpressionRunnerUtils.columnToTypesForTypeInference(spec);
        }
        return m_columnToType;
    }

    public final class ExpressionNodeRpcService extends RpcService {

        @Override
        protected String getCodeSuggestion(final String userPrompt, final String currentCode) throws IOException {
            // NB: The AI button is disabled if the input is not available
            return ExpressionCodeAssistant.generateCode( //
                userPrompt, //
                currentCode, //
                getWorkflowControl().getInputSpec(), //
                getSupportedFlowVariables() //
            );
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

            List<ExpressionDiagnostic> diagnostics = new ArrayList<>();

            try {
                var ast = getPreparedExpression(expression);

                var inferredType = Expressions.getInferredType(ast);

                if (!ValueType.BOOLEAN.equals(inferredType)) {
                    diagnostics.add(ExpressionDiagnostic.withSameMessage(
                        "The full expression must return the value type BOOLEAN "
                            + "in order to filter out rows for which the filter expression evaluates to false.",
                        DiagnosticSeverity.ERROR, Expressions.getTextLocation(ast)));
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

            var nodeContainer = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
            var executionContext = nodeContainer.createExecutionContext();

            try {
                var inColTable = m_inputTableCache.getTable(numPreviewRows);
                var outputTable = ExpressionRowFilterNodeModel.applyFilterExpression( //
                    script, //
                    inColTable, //
                    getSupportedFlowVariablesMap(), //
                    executionContext, //
                    this::handleWarningMessage //
                );

                updateTablePreview(outputTable, (int)inColTable.size());
            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because canceling the execution should not be possible.", e);
            }
        }

        private void updateTablePreview(final BufferedDataTable outputTable, final int numPreviewRows) {
            m_outputBufferTableReference.set(outputTable);
            m_cleanUpTableViewDataService.run();
            updateOutputTable(numPreviewRows, m_inputTableCache.getFullRowCount());
        }

        private void handleWarningMessage(final String warningMessage) {
            // TODO(AP-23152) show warning next to the expression (only the first one)
            addConsoleOutputEvent(new ConsoleText(formatWarning(warningMessage), true));
        }

        private static String formatWarning(final String warningText) {
            // TODO: is this actually how we want to do it?
            return "⚠️  \u001b[47m\u001b[30m%s\u001b[0m%n".formatted(warningText);
        }
    }
}
