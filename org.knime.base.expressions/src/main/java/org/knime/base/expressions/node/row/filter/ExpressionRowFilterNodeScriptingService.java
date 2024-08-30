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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.NodeExpressionMapperContext;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionCompileError;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.TextRange;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.VariableType;
import org.knime.scripting.editor.ScriptingService;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionRowFilterNodeScriptingService extends ScriptingService {

    private static final int PREVIEW_MAX_ROWS = 1000;

    /**
     * Cached function for mapping column access to output types for checking the expression types. Use
     * {@link #getColumnToTypeMapper()} to access this!
     */
    private Function<String, ReturnResult<ValueType>> m_columnToType;

    /**
     * Cached input table for executing the expression.
     */
    private ReferenceTable m_inputTable;

    /**
     * Cached row count of the input table. In case of a row-based table a columnar-based table is created on the fly
     * for the first {@link PREVIEW_MAX_ROWS} rows, thus loosing the information about the original input table row
     * count
     */
    private long m_inputTableRowCount;

    private final AtomicReference<BufferedDataTable> m_outputBufferTableReference;

    private final boolean m_inputTableIsAvailable;

    private final Runnable m_cleanUpTableViewDataService;

    ExpressionRowFilterNodeScriptingService(final AtomicReference<BufferedDataTable> outputTableRef,
        final Runnable cleanUpTableViewDataService) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains);

        var inputData = getWorkflowControl().getInputData();
        m_inputTableIsAvailable = inputData.length > 0 && inputData[0] != null;

        if (m_inputTableIsAvailable) {
            m_outputBufferTableReference = outputTableRef;
            m_outputBufferTableReference.set(getInputTable().getBufferedTable());
        } else {
            m_outputBufferTableReference = outputTableRef;
        }

        m_cleanUpTableViewDataService = cleanUpTableViewDataService;
    }

    @Override
    public RpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    @Override
    public void onDeactivate() {
        m_columnToType = null;
        m_inputTable = null;
    }

    synchronized Function<String, ReturnResult<ValueType>> getColumnToTypeMapper() {
        if (m_columnToType == null) {
            var spec = (DataTableSpec)getWorkflowControl().getInputSpec()[0];
            m_columnToType = ExpressionRunnerUtils.columnToTypesForTypeInference(spec);
        }
        return m_columnToType;
    }

    synchronized ReferenceTable getInputTable() {

        if (m_inputTable == null) {
            var inTable = (BufferedDataTable)getWorkflowControl().getInputData()[0];
            if (inTable == null) {
                throw new IllegalStateException("Input table not available");
            }
            m_inputTableRowCount = inTable.size();

            var nodeContainer = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();

            var executionContext = nodeContainer.createExecutionContext();

            try {
                // Progress isn't used in this context so we can pass whatever and don't expect it to be canceled
                m_inputTable = ExpressionRunnerUtils.createReferenceTable(inTable, executionContext,
                    executionContext.createSubProgress(1), PREVIEW_MAX_ROWS);
            } catch (CanceledExecutionException ex) {
                throw new IllegalStateException("Input table preparation for expression cancelled by the user", ex);
            }
        }
        return m_inputTable;
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

        private Map<String, FlowVariable> getAvailableFlowVariables(final VariableType<?>[] types) {
            var flowObjectStack = getWorkflowControl().getFlowObjectStack();
            if (flowObjectStack != null) {
                return flowObjectStack.getAvailableFlowVariables(types);
            } else {
                return Map.of();
            }
        }

        /** @return the typed Ast for the configured expression */
        private Ast getPreparedExpression(final String script) throws ExpressionCompileException {

            var ast = Expressions.parse(script);
            var flowVarToTypeMapper = ExpressionRunnerUtils.flowVarToTypeForTypeInference(
                getAvailableFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES));

            Expressions.inferTypes(ast, getColumnToTypeMapper(), flowVarToTypeMapper);

            return ast;
        }

        /**
         * List of diagnostics.
         *
         * @param expression the expression to check.
         * @return list of diagnostics.
         */
        public List<Diagnostic> getRowFilterDiagnostics(final String expression) {

            List<Diagnostic> diagnostics = new ArrayList<>();

            try {
                var ast = getPreparedExpression(expression);

                var inferredType = Expressions.getInferredType(ast);

                if (!ValueType.BOOLEAN.equals(inferredType)) {
                    diagnostics.add(new Diagnostic(
                        "The full expression must return the value type BOOLEAN "
                            + "in order to filter out rows for which the filter expression evaluates to false.",
                        DiagnosticSeverity.ERROR, Expressions.getTextLocation(ast)));
                }
            } catch (ExpressionCompileException ex) {
                diagnostics.addAll(Diagnostic.fromException(ex));
            }

            return diagnostics;
        }

        public void runRowFilterExpression(final String script, int numPreviewRows) {

            if (numPreviewRows > PREVIEW_MAX_ROWS) {
                throw new IllegalArgumentException("Number of preview rows must be at most 1000");
            }

            var inputTable = getInputTable();

            final Ast expression;
            try {
                // Prepare the expression
                expression = getPreparedExpression(script);

            } catch (ExpressionCompileException ex) {
                NodeLogger.getLogger(ExpressionRowFilterNodeScriptingService.class)
                    .debug("Error while running expression in dialog. This should not happen because the "
                        + "run button is disabled if the expression is invalid: " + ex.getMessage(), ex);
                addConsoleOutputEvent(new ConsoleText("Error: " + ex.getMessage(), true));

                throw new IllegalStateException(
                    "Implementation error: Error while running expression in dialog: '%s'".formatted(ex.getMessage()),
                    ex);
            }
            // NB: We use the inRefTable because it is guaranteed to be a columnar table
            try {
                ExpressionRunnerUtils.evaluateAggregations(expression, inputTable.getBufferedTable(),
                    new ExecutionMonitor(), numPreviewRows);
            } catch (CanceledExecutionException ex) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because canceling the execution should not be possible.", ex);
            }

            List<String> warnings = new ArrayList<>();
            EvaluationContext evaluationContext = warnings::add;
            numPreviewRows = (int)Math.min(numPreviewRows, m_inputTableRowCount);

            var slicedInputTable = inputTable.getVirtualTable().slice(0, numPreviewRows);

            var exprContext = new NodeExpressionMapperContext(this::getAvailableFlowVariables);
            var filteredTable = ExpressionRunnerUtils.filterTableByExpression(slicedInputTable, expression,
                numPreviewRows, evaluationContext, exprContext);

            var exec = ((NativeNodeContainer)NodeContext.getContext().getNodeContainer()).createExecutionContext();
            BufferedDataTable outputTable;
            try {
                outputTable = ColumnarVirtualTableMaterializer.materializer() //
                    .sources(inputTable.getSources()) //
                    .materializeRowKey(true) //
                    .progress((rowIndex, rowKey) -> {
                    }) //
                    .executionContext(exec) //
                    .tableIdSupplier(Node.invokeGetDataRepository(exec)::generateNewID) //
                    .materialize(filteredTable) //
                    .getBufferedTable();
            } catch (VirtualTableIncompatibleException e) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because the table is guaranteed to be compatible.", e);
            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("Preview evaluation cancelled by the user, which should be impossible",
                    e);
            }
            m_outputBufferTableReference.set(outputTable);
            m_cleanUpTableViewDataService.run();
            updateOutputTable(numPreviewRows, m_inputTableRowCount);

            for (var warning : warnings) {
                addConsoleOutputEvent(new ConsoleText(formatWarning(warning), true));
            }

        }

        public record Diagnostic(String message, DiagnosticSeverity severity, TextRange location) {
            static Diagnostic fromError(final ExpressionCompileError error) {
                return new Diagnostic(error.createMessage(), DiagnosticSeverity.ERROR, error.location());
            }

            static List<Diagnostic> fromException(final ExpressionCompileException exception) {
                return exception.getErrors().stream().map(Diagnostic::fromError).toList();
            }
        }

        public enum DiagnosticSeverity {
                ERROR, WARNING, INFORMATION, HINT;
        }

        private static String formatWarning(final String warningText) {
            // TODO: is this actually how we want to do it?
            return "⚠️  \u001b[47m\u001b[30m%s\u001b[0m%n".formatted(warningText);
        }
    }
}
