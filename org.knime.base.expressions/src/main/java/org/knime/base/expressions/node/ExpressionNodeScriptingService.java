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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.ExpressionRunnerUtils.ColumnInsertionMode;
import org.knime.base.expressions.node.ExpressionNodeModel.NodeExpressionMapperContext;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionCompileError;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.MathConstantValue;
import org.knime.core.expressions.NamedExpressionOperator;
import org.knime.core.expressions.TextRange;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.functions.BuiltInFunctions;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
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

    private static final int PREVIEW_MAX_ROWS = 1000;

    private static final Set<VariableType<?>> SUPPORTED_FLOW_VARIABLE_TYPES_SET =
        Arrays.stream(ExpressionNodeModel.SUPPORTED_FLOW_VARIABLE_TYPES).collect(Collectors.toSet());

    /**
     * Cached function for mapping column access to output types for checking the expression types. Use
     * {@link #getColumnToTypeMapper()} to access this!
     */
    private Function<String, Optional<ValueType>> m_columnToType;

    /**
     * Cached input table for executing the expression.
     */
    private ReferenceTable m_inputTable;

    private AtomicReference<BufferedDataTable> m_outputBufferTableReference;

    private final Runnable m_cleanUpTableViewDataService;

    ExpressionNodeScriptingService(final AtomicReference<BufferedDataTable> outputTableRef,
        final Runnable cleanUpTableViewDataService) {
        super(null, flowVar -> SUPPORTED_FLOW_VARIABLE_TYPES_SET.contains(flowVar.getVariableType()));

        m_outputBufferTableReference = outputTableRef;
        m_outputBufferTableReference.set(getInputTable().getBufferedTable());
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

    synchronized Function<String, Optional<ValueType>> getColumnToTypeMapper() {
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

        public List<Map<String, Serializable>> getMathConstants() {
            return Arrays.stream(MathConstantValue.values()) //
                .map(constant -> Map.of( //
                    "name", constant.name(), //
                    "type", constant.type().toString(), //
                    "documentation", constant.documentation(), //
                    "value", constant.value()) //
                ) //
                .toList();
        }

        private Map<String, FlowVariable> getAvailableFlowVariables(final VariableType<?>[] types) {
            return getWorkflowControl().getFlowObjectStack().getAvailableFlowVariables(types);
        }

        /**
         * Parses and type-checks the expression.
         *
         * @return an expression that is ready to be executed
         */
        private Ast getPreparedExpression(final String script) throws ExpressionCompileException {
            var ast = Expressions.parse(script);
            var flowVarToTypeMapper = ExpressionNodeModel.flowVarToTypeForTypeInference(
                getAvailableFlowVariables(ExpressionNodeModel.SUPPORTED_FLOW_VARIABLE_TYPES));
            Expressions.inferTypes(ast, getColumnToTypeMapper(), flowVarToTypeMapper);
            return ast;
        }

        public List<Diagnostic> getDiagnostics(final String expression) {
            try {
                getPreparedExpression(expression);
                return List.of();
            } catch (ExpressionCompileException ex) {
                return Diagnostic.fromException(ex);
            }
        }

        public void runExpression(final String script, int numPreviewRows, final String columnInsertionModeString,
            final String columnName) {

            if (numPreviewRows > PREVIEW_MAX_ROWS) {
                throw new IllegalArgumentException("Number of preview rows must be at most 1000");
            }

            final Ast expression;
            try {
                expression = getPreparedExpression(script);
            } catch (ExpressionCompileException ex) {
                NodeLogger.getLogger(ExpressionNodeScriptingService.class)
                    .debug("Error while running expression in dialog. This should not happen because the "
                        + "run button is disabled if the expression is invalid: " + ex.getMessage(), ex);
                addConsoleOutputEvent(new ConsoleText("Error: " + ex.getMessage(), true));
                return;
            }

            var inputTable = getInputTable();

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
            var exprContext = new NodeExpressionMapperContext(this::getAvailableFlowVariables);
            var slicedInputTable = inputTable.getVirtualTable().slice(0, numPreviewRows);

            var expressionResult = ExpressionRunnerUtils.applyExpression( //
                slicedInputTable, //
                numPreviewRows, //
                expression, //
                columnName, //
                exprContext, //
                evaluationContext);

            updateTablePreview(inputTable, slicedInputTable, expressionResult, columnName, columnInsertionModeString,
                numPreviewRows);

            for (var warning : warnings) {
                addConsoleOutputEvent(new ConsoleText(formatWarning(warning), true));
            }
        }

        public String getDocumentationContext() {
            StringBuilder sb = new StringBuilder();

            sb.append("# Available math constants\n");

            for (var constant : MathConstantValue.values()) {
                sb.append("\n## " + constant.name() + "\n");
                sb.append("### Type\n");
                sb.append(constant.type() + "\n");
                sb.append("### Description\n");
                sb.append(constant.documentation() + "\n");
            }

            sb.append("\n# Available functions\n\n");

            sb.append("In general, except where otherwise stated, any function that "
                + "is given a `MISSING` value as an argument should return `MISSING`.\n\n");

            sb.append("You can ONLY use these functions listed here:\n\n");

            sb.append("# Functions");

            sb.append(generateSubdocumentation(BuiltInFunctions.BUILT_IN_FUNCTIONS, "Function"));

            sb.append("\n# Column aggregations\n\n");
            sb.append(
                "The column argument is always required and must be a column name formatted as a string literal.\n");
            sb.append("All other arguments are optional and have default values, which can be passed positionally\n");
            sb.append("or by their name in the format `argname=value`. Optional arguments must be literals.\n\n");
            sb.append("You can ONLY use these aggregations listed here:\n\n");

            sb.append(generateSubdocumentation(BuiltInAggregations.BUILT_IN_AGGREGATIONS, "Aggregation"));

            return applyIndent(sb.toString(), 4);
        }

        private static String generateSubdocumentation(final Collection<? extends NamedExpressionOperator> operators,
            final String typeName) {
            StringBuilder sb = new StringBuilder();

            for (var operator : operators) {
                sb.append("").append("\n");
                sb.append("## " + typeName + " `" + operator.name() + "(" + operator.description().arguments().stream()
                    .map(arg -> arg.name()).collect(Collectors.joining(", ")) + ")`\n");
                sb.append("### Arguments\n");
                for (var arg : operator.description().arguments()) {
                    sb.append("- " + arg.name() + " [" + arg.type() + "]: " + arg.description() + "\n");
                }
                sb.append("### Returns\n");
                sb.append("[" + operator.description().returnType() + "] " + operator.description().returnDescription()
                    + "\n");
                sb.append("### Description\n");
                sb.append(getShortDescription(operator.description().description()) + "\n");
            }

            return sb.toString();
        }

        private static String getShortDescription(final String description) {
            // We basically want to extract the first paragraph of the description

            return description.split("\n\n")[0];
        }

        private static String applyIndent(final String s, final int indent) {
            return Arrays.stream(s.split("\n")).map(line -> " ".repeat(indent) + line)
                .collect(Collectors.joining("\n"));
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
            final String columnInsertionModeString, final int numRows) {

            try {
                var context =
                    ((NativeNodeContainer)NodeContext.getContext().getNodeContainer()).createExecutionContext();

                var newColumnPosition = new ExpressionRunnerUtils.NewColumnPosition(
                    ColumnInsertionMode.valueOf(columnInsertionModeString), columnName);
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
                updateOutputTable(numRows);
            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("Preview evaluation cancelled by the user.", e);
            } catch (VirtualTableIncompatibleException e) {
                throw new IllegalStateException("This is an implementation error. Must not happen "
                    + "because the table is guaranteed to be compatible.", e);
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
