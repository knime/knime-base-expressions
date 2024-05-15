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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionNodeModel.NodeExpressionEvaluationContext;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableExtensionTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.v2.RowRead;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.ExpressionCompileError;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.TextRange;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.WarningMessageListener;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.VariableType;
import org.knime.scripting.editor.InputOutputModel;
import org.knime.scripting.editor.ScriptingService;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionNodeScriptingService extends ScriptingService {

    private static final int DIALOG_PREVIEW_NUM_ROWS = 10;

    /**
     * Cached function for mapping column access to output types for checking the expression types. Use
     * {@link #getColumnToTypeMapper()} to access this!
     */
    private Function<ColumnAccess, Optional<ValueType>> m_columnToType;

    /**
     * Cached input table for executing the expression.
     */
    private ReferenceTable m_inputTable;

    private NodeExpressionEvaluationContext m_exprContext = new NodeExpressionEvaluationContext(
        types -> getWorkflowControl().getFlowObjectStack().getAvailableFlowVariables(types));

    ExpressionNodeScriptingService() {
        super(null,
            flowVar -> new HashSet<VariableType<?>>(
                new HashSet<>(Arrays.asList(ExpressionNodeModel.SUPPORTED_FLOW_VARIABLE_TYPES)))
                    .contains(flowVar.getVariableType()));
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

    synchronized Function<ColumnAccess, Optional<ValueType>> getColumnToTypeMapper() {
        if (m_columnToType == null) {
            var spec = (DataTableSpec)getWorkflowControl().getInputSpec()[0];
            m_columnToType = ExpressionNodeModel.columnToTypesForTypeInference(spec);
        }
        return m_columnToType;
    }

    synchronized ReferenceTable getInputTable() {
        if (m_inputTable == null) {
            var inTable = (BufferedDataTable)getWorkflowControl().getInputData()[0];
            if (inTable == null) {
                throw new IllegalStateException("Input table not available");
            }

            var nodeContainer = (NativeNodeContainer)NodeContext.getContext().getNodeContainer();
            m_inputTable = ExpressionRunnerUtils.createReferenceTable(inTable, nodeContainer.createExecutionContext());
        }
        return m_inputTable;
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
            // NB: The AI button is disabled if the input is not available
            return ExpressionCodeAssistant.generateCode( //
                userPrompt, //
                currentCode, //
                getWorkflowControl().getInputSpec(), //
                getFlowVariables() //
            );
        }

        public FunctionCatalogData getFunctionCatalog() {
            return FunctionCatalogData.BUILT_IN;
        }

        public List<Diagnostic> getDiagnostics(final String expression) {
            try {
                var ast = Expressions.parse(expression);
                Expressions.inferTypes(ast, getColumnToTypeMapper(), m_exprContext::flowVariableToType);
                return List.of();
            } catch (ExpressionCompileException ex) {
                return Diagnostic.fromException(ex);
            }
        }

        public void runExpression(final String expression) {
            // Apply the expression on the input table using a ColumnarVirtualTable

            List<String> warnings = new ArrayList<>();
            WarningMessageListener wml = warning -> warnings.add(warning);

            var inputTable = getInputTable();
            var numRows = (int)Math.min(DIALOG_PREVIEW_NUM_ROWS, inputTable.getBufferedTable().size());
            var expressionResult = ExpressionRunnerUtils.applyExpression( //
                inputTable.getVirtualTable().slice(0, numRows), //
                expression, //
                "result", // column name is irrelevant
                m_exprContext, //
                wml);

            var result = new String[numRows];
            try (var expressionResultTable =
                new VirtualTableExtensionTable(new ReferenceTable[]{inputTable}, expressionResult, numRows, 0)) {
                try (var cursor = expressionResultTable.cursor(TableFilter.filterRangeOfRows(0, numRows))) {
                    var resultIdx = expressionResultTable.getDataTableSpec().getNumColumns() - 1;
                    for (var i = 0; i < numRows && cursor.canForward(); i++) {
                        result[i] = getRowResult(cursor.forward(), resultIdx);
                    }
                }
            }

            addConsoleOutputEvent(new ConsoleText(formatResult(result), false));

            for (var warning : warnings) {
                addConsoleOutputEvent(new ConsoleText(formatWarning(warning), true));
            }
        }

        private static String getRowResult(final RowRead rowRead, final int resultIdx) {
            return rowRead.isMissing(resultIdx) //
                ? "MISSING" //
                : rowRead.getValue(resultIdx).materializeDataCell().toString();
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
