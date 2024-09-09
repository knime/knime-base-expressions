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
package org.knime.base.expressions.node.row.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.InsertionMode;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.NodeExpressionMapperContext;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTables;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.scripting.editor.ScriptingService;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionRowMapperNodeScriptingService extends ScriptingService {

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

    ExpressionRowMapperNodeScriptingService(final AtomicReference<BufferedDataTable> outputTableRef,
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

        /**
         * Parses and type-checks the expression.
         *
         * @param script the expression to parse
         * @param additionalColumnNames the names of the additional columns that are available (from previous expression
         *            editors in the same node)
         * @param additionalColumnTypes the types of the additional columns that are available (from previous expression
         *            editors in the same node)
         *
         * @return an expression that is ready to be executed
         */
        private Ast getPreparedExpression(final String script, final List<String> additionalColumnNames,
            final List<ValueType> additionalColumnTypes) throws ExpressionCompileException {

            var ast = Expressions.parse(script);
            var flowVarToTypeMapper =
                ExpressionRunnerUtils.flowVarToTypeForTypeInference(getSupportedFlowVariablesMap());

            Function<String, ReturnResult<ValueType>> columnToTypeMapper = columnName -> {
                if (additionalColumnNames.contains(columnName)) {
                    return ReturnResult.success(additionalColumnTypes.get(additionalColumnNames.indexOf(columnName)));
                } else {
                    return getColumnToTypeMapper().apply(columnName);
                }
            };

            Expressions.inferTypes(ast, columnToTypeMapper, flowVarToTypeMapper);
            return ast;
        }

        /**
         * Checks if the expression accesses columns that are not yet available, but that will be added by a later
         * expression.
         *
         * @param ast
         * @param expressionIndex
         * @param allAppendedColumnNames the colum names that are appended. NOT the ones that were passed into the node.
         * @return
         */
        private static List<ExpressionDiagnostic> getPrematureAccessDiagnostics(final Ast ast,
            final int expressionIndex, final List<String> allAppendedColumnNames) {

            List<ExpressionDiagnostic> diagnostics = new ArrayList<>();

            var accessedColumns = ExpressionRunnerUtils.collectColumnAccesses(ast);

            // Find any columns that are accessed now but appended later
            var columnsAccessedEarly = accessedColumns.stream() //
                .filter(access -> allAppendedColumnNames.contains(access.columnId().name())) //
                .filter(
                    access -> !allAppendedColumnNames.subList(0, expressionIndex).contains(access.columnId().name())) //
                .toList();

            for (var column : columnsAccessedEarly) {
                String columnName = column.columnId().name();
                String errorMessage =
                    "The column '%s' was used before it was appended by Expression %d. Try reordering your expressions."
                        .formatted(columnName, 1 + allAppendedColumnNames.indexOf(columnName));
                diagnostics.add(ExpressionDiagnostic.withSameMessage( //
                    errorMessage, //
                    DiagnosticSeverity.ERROR, //
                    Expressions.getTextLocation(column) //
                ));
            }

            return diagnostics;
        }

        /**
         * List of diagnostics for each editor, hence a 2D list.
         *
         * @param expressions
         * @param newColumnNames the names of the appended columns. Guaranteed to have the same length and order as the
         *            expressions. Some elements are null, for expressions that replaced instead of appending.
         * @return list of diagnostics for each editor, i.e. a list of a lists of diagnostics
         */
        public List<List<ExpressionDiagnostic>> getRowMapperDiagnostics(final String[] expressions,
            final String[] newColumnNames) {

            List<ValueType> inferredColumnTypes = new ArrayList<>();
            List<List<ExpressionDiagnostic>> diagnostics = new ArrayList<>();

            for (int i = 0; i < expressions.length; ++i) {
                var expression = expressions[i];

                List<ExpressionDiagnostic> diagnosticsForThisExpression = new ArrayList<>();

                try {
                    var untypedAst = Expressions.parse(expression);

                    // Check if the expression refers to any columns that are appended in the future
                    var prematureAccessDiagnostics =
                        getPrematureAccessDiagnostics(untypedAst, i, Arrays.asList(newColumnNames));
                    diagnosticsForThisExpression.addAll(prematureAccessDiagnostics);

                    // if prematureAccessDiagnostics are present, type inference will fail,
                    // so infer MISSING and continue to next expression
                    if (!prematureAccessDiagnostics.isEmpty()) {
                        inferredColumnTypes.add(ValueType.MISSING);
                        continue;
                    }

                    var ast = getPreparedExpression( //
                        expression, //
                        Arrays.asList(newColumnNames).subList(0, i), //
                        inferredColumnTypes //
                    );
                    var inferredType = Expressions.getInferredType(ast);

                    if (ValueType.MISSING.equals(inferredType)) {
                        // Output type "MISSING" is not supported, hence error
                        diagnosticsForThisExpression.add(ExpressionDiagnostic.withSameMessage( //
                            "The full expression must not evaluate to MISSING.", //
                            DiagnosticSeverity.ERROR, //
                            Expressions.getTextLocation(ast) //
                        ));
                    }

                    inferredColumnTypes.add(inferredType);
                } catch (ExpressionCompileException ex) {
                    // If there is an error in the expression, we still want to be able to continue with the other
                    // expression diagnostics, so add a missing type to the list of inferred types and continue
                    inferredColumnTypes.add(ValueType.MISSING);

                    diagnosticsForThisExpression.addAll(ExpressionDiagnostic.fromException(ex));
                } finally {
                    diagnostics.add(diagnosticsForThisExpression);
                }
            }

            return diagnostics;
        }

        public void runExpression(final String[] scripts, int numPreviewRows, final String[] columnInsertionModesString,
            final String[] columnNames) {

            if (numPreviewRows > PREVIEW_MAX_ROWS) {
                throw new IllegalArgumentException("Number of preview rows must be at most 1000");
            }

            var inputTable = getInputTable();

            List<ValueType> additionalColumnTypes = new ArrayList<>();

            for (int i = 0; i < scripts.length; ++i) {
                String script = scripts[i];
                String columnName = columnNames[i];
                List<String> additionalColumnNames = new ArrayList<>();
                String columnInsertionModeString = columnInsertionModesString[i];

                final Ast expression;
                try {
                    expression = getPreparedExpression(script, additionalColumnNames, additionalColumnTypes);

                    var inferredType = Expressions.getInferredType(expression);

                    additionalColumnTypes.add(inferredType);
                    additionalColumnNames.add(columnName);
                } catch (ExpressionCompileException ex) {
                    NodeLogger.getLogger(ExpressionRowMapperNodeScriptingService.class)
                        .debug("Error while running expression in dialog. This should not happen because the "
                            + "run button is disabled if the expression is invalid: " + ex.getMessage(), ex);
                    addConsoleOutputEvent(new ConsoleText("Error: " + ex.getMessage(), true));

                    throw new IllegalStateException(
                        "Implementation error: Error while running expression in dialog: '%s'"
                            .formatted(ex.getMessage()),
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
                numPreviewRows = (int)Math.min(numPreviewRows, inputTable.getBufferedTable().size());
                var exprContext = new NodeExpressionMapperContext(types -> getSupportedFlowVariablesMap());
                var slicedInputTable = inputTable.getVirtualTable().slice(0, numPreviewRows);

                var expressionResult = ExpressionRunnerUtils.applyExpression( //
                    slicedInputTable, //
                    numPreviewRows, //
                    expression, //
                    columnName, //
                    exprContext, //
                    evaluationContext //
                );

                var context =
                    ((NativeNodeContainer)NodeContext.getContext().getNodeContainer()).createExecutionContext();

                var outputTableVirtual = ExpressionRunnerUtils.constructOutputTable(slicedInputTable, expressionResult,
                    new ExpressionRunnerUtils.NewColumnPosition(InsertionMode.valueOf(columnInsertionModeString),
                        columnName));

                // TODO(AP-23177): reduce materialization to at most one
                BufferedDataTable outputTable;
                try {
                    outputTable = ColumnarVirtualTableMaterializer.materializer() //
                        .sources(inputTable.getSources()) //
                        .materializeRowKey(true) //
                        .progress((rowIndex, rowKey) -> {
                        }) //
                        .executionContext(context) //
                        .tableIdSupplier(Node.invokeGetDataRepository(context)::generateNewID) //
                        .materialize(outputTableVirtual) //
                        .getBufferedTable();
                } catch (VirtualTableIncompatibleException e) {
                    throw new IllegalStateException("This is an implementation error. Must not happen "
                        + "because the table is guaranteed to be compatible.", e);
                } catch (CanceledExecutionException e) {
                    throw new IllegalStateException(
                        "Preview evaluation cancelled by the user, which should be impossible", e);
                }

                updateTablePreview(inputTable, slicedInputTable, expressionResult, columnName,
                    columnInsertionModeString, numPreviewRows, m_inputTableRowCount);

                for (var warning : warnings) {
                    addConsoleOutputEvent(new ConsoleText(formatWarning(warning), true));
                }

                try {
                    inputTable = ReferenceTables.createReferenceTable(outputTable);
                } catch (VirtualTableIncompatibleException e) {
                    throw new IllegalStateException("This is an implementation error. Must not happen "
                        + "because the table is guaranteed to be compatible.", e);
                }
            }
        }

        /**
         * Updates the output preview table with the new column.
         *
         * @param inputTable the input table
         * @param slicedInputTable the sliced input table
         * @param expressionResult the result of the expression
         * @param columnName the name of the new column
         * @param columnInsertionModeString the column insertion mode
         * @param numRows
         */
        private void updateTablePreview(final ReferenceTable inputTable, final ColumnarVirtualTable slicedInputTable,
            final ColumnarVirtualTable expressionResult, final String columnName,
            final String columnInsertionModeString, final int numRows, final long totalNumRows) {

            if (!m_inputTableIsAvailable) {
                throw new IllegalStateException(
                    "Implementation error - input table not available, but preview requested");
            }

            try {
                var context =
                    ((NativeNodeContainer)NodeContext.getContext().getNodeContainer()).createExecutionContext();

                var newColumnPosition = new ExpressionRunnerUtils.NewColumnPosition(
                    InsertionMode.valueOf(columnInsertionModeString), columnName);
                var outputTable =
                    ExpressionRunnerUtils.constructOutputTable(slicedInputTable, expressionResult, newColumnPosition);

                m_outputBufferTableReference.set(ColumnarVirtualTableMaterializer.materializer() //
                    .sources(inputTable.getSources()) //
                    .materializeRowKey(true) //
                    .progress((rowIndex, rowKey) -> {
                    }) //
                    .executionContext(context) //
                    .tableIdSupplier(Node.invokeGetDataRepository(context)::generateNewID) //
                    .materialize(outputTable) //
                    .getBufferedTable());

                m_cleanUpTableViewDataService.run();
                updateOutputTable(numRows, totalNumRows);
            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("Preview evaluation cancelled by the user.", e);
            } catch (VirtualTableIncompatibleException e) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because the table is guaranteed to be compatible.", e);
            }
        }

        private static String formatWarning(final String warningText) {
            // TODO: is this actually how we want to do it?
            return "⚠️  \u001b[47m\u001b[30m%s\u001b[0m%n".formatted(warningText);
        }
    }
}
