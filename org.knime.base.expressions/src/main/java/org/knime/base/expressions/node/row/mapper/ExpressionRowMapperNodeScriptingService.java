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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.InsertionMode;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.ExpressionDiagnosticResult;
import org.knime.base.expressions.node.row.InputTableCache;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ColumnId.ColumnIdType;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.scripting.editor.ScriptingService;
import org.knime.scripting.editor.WorkflowControl;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class ExpressionRowMapperNodeScriptingService extends ScriptingService {

    private static final int PREVIEW_MAX_ROWS = 1000;

    private final AtomicReference<BufferedDataTable> m_outputBufferTableReference;

    private InputTableCache m_inputTableCache;

    private final Runnable m_cleanUpTableViewDataService;

    ExpressionRowMapperNodeScriptingService(final AtomicReference<BufferedDataTable> outputTableRef,
        final Runnable cleanUpTableViewDataService) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains);
        m_outputBufferTableReference = outputTableRef;
        m_cleanUpTableViewDataService = cleanUpTableViewDataService;
        m_inputTableCache = new InputTableCache((BufferedDataTable)getWorkflowControl().getInputData()[0]);
    }

    /** Constructor for testing with a mocked workflow control */
    ExpressionRowMapperNodeScriptingService(final AtomicReference<BufferedDataTable> outputTableRef,
        final Runnable cleanUpTableViewDataService, final WorkflowControl workflowControl) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains, workflowControl);
        m_outputBufferTableReference = outputTableRef;
        m_cleanUpTableViewDataService = cleanUpTableViewDataService;
        m_inputTableCache = new InputTableCache((BufferedDataTable)workflowControl.getInputData()[0]);
    }

    @Override
    public ExpressionNodeRpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    @Override
    public void onDeactivate() {
        m_inputTableCache = null;
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
         * Checks if the expression accesses columns that are not yet available, but that will be added by a later
         * expression.
         *
         * @param ast
         * @param expressionIndex
         * @param allAppendedColumnNames the column names that are appended. NOT the ones that were passed into the
         *            node.
         * @return
         */
        private static List<ExpressionDiagnostic> getPrematureAccessDiagnostics(final Ast ast,
            final int expressionIndex, final List<String> allAppendedColumnNames) {

            List<ExpressionDiagnostic> diagnostics = new ArrayList<>();

            var accessedColumns = ExpressionRunnerUtils.collectColumnAccesses(ast) //
                .stream() //
                .filter(access -> access.columnId().type() == ColumnIdType.NAMED) //
                .toList();

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

        private static Map<String, ReturnResult<ValueType>> constructColumnToTypeMap(final DataTableSpec spec) {
            var typeResolver = ExpressionRunnerUtils.columnToTypesForTypeInference(spec);
            var columnToTypeMap = new HashMap<String, ReturnResult<ValueType>>();
            for (var columnSpec : spec) {
                var columnName = columnSpec.getName();
                columnToTypeMap.put(columnName, typeResolver.apply(columnName));
            }
            return columnToTypeMap;
        }

        private static ReturnResult<ValueType> invalidExpressionType(final int expressionIdx, final String columnName) {
            return ReturnResult.failure("Expression %d that outputs column '%s' has errors. Fix Expression %d."
                .formatted(expressionIdx + 1, columnName, expressionIdx + 1));
        }

        /**
         * List of diagnostics results for each editor.
         *
         * @param expressions
         * @param allNewColumnNames the names of the all output columns. Must align with the expressions.
         * @return list of diagnostics results for each editor, i.e. a list of diagnostics and the return type.
         */
        public List<ExpressionDiagnosticResult> getRowMapperDiagnostics(final String[] expressions,
            final String[] allNewColumnNames) {

            var spec = (DataTableSpec)getWorkflowControl().getInputSpec()[0];
            if (spec == null) {
                // No input table, so no columns
                return Collections.nCopies(expressions.length,
                    new ExpressionDiagnosticResult(ExpressionDiagnostic.NO_INPUT_CONNECTED_DIAGNOSTICS, "UNKNOWN"));
            }

            List<ExpressionDiagnosticResult> diagnostics = new ArrayList<>();

            // Handle the available columns - columnToTypeMap gets updated with appended and replaced columns
            var columnToTypeMap = constructColumnToTypeMap(spec);
            Function<String, ReturnResult<ValueType>> columnToTypeMapper =
                columnName -> columnToTypeMap.getOrDefault(columnName,
                    ReturnResult.failure("No column with the name '%s' is available.".formatted(columnName)));
            var flowVarToTypeMapper =
                ExpressionRunnerUtils.flowVarToTypeForTypeInference(getSupportedFlowVariablesMap());

            // Only the names of the columns that are appended (not replaced)
            var appendedColumnNames = Arrays.stream(allNewColumnNames) //
                .map(name -> columnToTypeMap.containsKey(name) ? null : name) //
                .toList();

            for (int i = 0; i < expressions.length; ++i) {
                var expression = expressions[i];
                var currentOutputColumnName = allNewColumnNames[i];

                List<ExpressionDiagnostic> diagnosticsForThisExpression = new ArrayList<>();

                var successfulInferredTypeName = "UNKNOWN";

                try {
                    var ast = Expressions.parse(expression);

                    // Check if the expression refers to any columns that are appended in the future
                    var prematureAccessDiagnostics = getPrematureAccessDiagnostics(ast, i, appendedColumnNames);
                    diagnosticsForThisExpression.addAll(prematureAccessDiagnostics);

                    // if prematureAccessDiagnostics are present, type inference will fail,
                    // so infer a failure and continue to next expression
                    if (!prematureAccessDiagnostics.isEmpty()) {
                        columnToTypeMap.put(currentOutputColumnName, invalidExpressionType(i, currentOutputColumnName));
                        continue;
                    }

                    var inferredType = Expressions.inferTypes(ast, columnToTypeMapper, flowVarToTypeMapper);

                    if (ValueType.MISSING.equals(inferredType)) {
                        // Output type "MISSING" is not supported, hence error
                        diagnosticsForThisExpression.add(ExpressionDiagnostic.withSameMessage( //
                            "The full expression must not evaluate to MISSING.", //
                            DiagnosticSeverity.ERROR, //
                            Expressions.getTextLocation(ast) //
                        ));
                    }

                    successfulInferredTypeName = inferredType.baseType().name();
                    columnToTypeMap.put(currentOutputColumnName, ReturnResult.success(inferredType));
                } catch (ExpressionCompileException ex) {
                    // If there is an error in the expression, we still want to indicate that when accessing this column
                    // in another expression. Therefore, we put an appropriate error message into the map
                    columnToTypeMap.put(currentOutputColumnName, invalidExpressionType(i, currentOutputColumnName));

                    diagnosticsForThisExpression.addAll(ExpressionDiagnostic.fromException(ex));
                } finally {
                    diagnostics
                        .add(new ExpressionDiagnosticResult(diagnosticsForThisExpression, successfulInferredTypeName));
                }
            }

            return diagnostics;
        }

        public void runExpression(final List<String> scripts, final int numPreviewRows,
            final List<String> columnInsertionModesString, final List<String> columnNames)
            throws ExpressionCompileException, VirtualTableIncompatibleException {

            if (numPreviewRows > PREVIEW_MAX_ROWS) {
                throw new IllegalArgumentException("Number of preview rows must be at most 1000");
            }

            var warnings = new ExpressionDiagnostic[scripts.size()];

            var nodeContainer = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
            var executionContext = nodeContainer.createExecutionContext();
            try {
                var inColTable = m_inputTableCache.getTable(numPreviewRows);
                var outputTable = ExpressionRowMapperNodeModel.applyMapperExpressions( //
                    scripts, //
                    ExpressionRowMapperNodeModel.getColumnPositions( //
                        columnInsertionModesString.stream().map(InsertionMode::valueOf).toList(), //
                        columnNames //
                    ), //
                    inColTable, //
                    getSupportedFlowVariablesMap(), //
                    executionContext, //
                    ExpressionDiagnostic.getWarningMessageHandler(warnings) //
                );

                updateTablePreview(outputTable);

                sendEvent("updateWarnings", warnings);

            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because canceling the execution should not be possible.", e);
            }
        }

        private void updateTablePreview(final BufferedDataTable outputTable) {
            m_outputBufferTableReference.set(outputTable);
            m_cleanUpTableViewDataService.run();
            updateOutputTable((int)outputTable.size(), m_inputTableCache.getFullRowCount());
        }

    }
}
