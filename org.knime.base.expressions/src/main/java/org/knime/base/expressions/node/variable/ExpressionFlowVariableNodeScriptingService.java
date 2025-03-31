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
package org.knime.base.expressions.node.variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionCodeAssistant.ExpressionType;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.ExpressionDiagnosticResult;
import org.knime.base.expressions.node.WithIndexExpressionException;
import org.knime.base.expressions.node.variable.ExpressionFlowVariableSettings.FlowVariableTypeNames;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.ExpressionCompileException;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.scripting.editor.CodeGenerationRequest;
import org.knime.scripting.editor.InputOutputModel;
import org.knime.scripting.editor.ScriptingService;
import org.knime.scripting.editor.WorkflowControl;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
final class ExpressionFlowVariableNodeScriptingService extends ScriptingService {

    private final AtomicReference<List<FlowVariable>> m_outputFlowVariablesReference;

    ExpressionFlowVariableNodeScriptingService(final AtomicReference<List<FlowVariable>> outputFlowVariablesReference) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains);

        m_outputFlowVariablesReference = outputFlowVariablesReference;

    }

    ExpressionFlowVariableNodeScriptingService(final AtomicReference<List<FlowVariable>> outputFlowVariablesReference,
        final WorkflowControl workflowControl) {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains, workflowControl);

        m_outputFlowVariablesReference = outputFlowVariablesReference;

    }

    @Override
    public ExpressionNodeRpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    @Override
    public void onDeactivate() {
        //  nothing to do
    }

    public final class ExpressionNodeRpcService extends RpcService {

        @Override
        protected CodeGenerationRequest getCodeSuggestionRequest(final String userPrompt, final String currentCode,
            final InputOutputModel[] inputModels) {
            return ExpressionCodeAssistant.createCodeGenerationRequest(ExpressionType.VARIABLE, userPrompt, currentCode, inputModels);
        }

        /**
         * Get diagnostics for the given expression which represent a flow variable accessed that doesn't exist yet, but
         * will at some point in the future.
         *
         * @param ast
         * @param expressionIndex the index of this expression in the list of all expressions, starting from 0.
         * @param allAppendedFlowVariableNames the flow variable names that are appended. NOT the ones that were passed
         *            into the node.
         * @return
         */
        private static List<ExpressionDiagnostic> getPrematureAccessDiagnostics(final Ast ast,
            final int expressionIndex, final List<String> allAppendedFlowVariableNames) {

            List<ExpressionDiagnostic> diagnostics = new ArrayList<>();

            var accessedFlowVars = ExpressionRunnerUtils.collectFlowVariableAccesses(ast);

            // Find any flow variables that are accessed now but appended later
            var flowVarsAccessedEarly = accessedFlowVars.stream() //
                .filter(fvAccess -> allAppendedFlowVariableNames.contains(fvAccess.name())) //
                .filter(fvAccess -> !allAppendedFlowVariableNames.subList(0, expressionIndex).contains(fvAccess.name()))
                .toList();

            for (var flowVar : flowVarsAccessedEarly) {
                String flowVarName = flowVar.name();
                String errorMessage = "The flow variable '" + flowVarName
                    + "' was used before it was appended by Expression "
                    + (allAppendedFlowVariableNames.indexOf(flowVarName) + 1) + ". Try reordering your expressions.";
                diagnostics.add(ExpressionDiagnostic.withSameMessage( //
                    errorMessage, //
                    DiagnosticSeverity.ERROR, //
                    Expressions.getTextLocation(flowVar) //
                ));
            }

            return diagnostics;
        }

        private static boolean appendsInvalidFlowVariable(final String appendedFlowVariable) {
            return appendedFlowVariable.isBlank() || appendedFlowVariable.startsWith("knime");
        }

        private static ReturnResult<FlowVariable> invalidExpressionType(final int expressionIdx,
            final String columnName) {
            return ReturnResult.failure("Expression %d that outputs flow variable '%s' has errors. Fix Expression %d."
                .formatted(expressionIdx + 1, columnName, expressionIdx + 1));
        }

        /**
         * List of diagnostics results for each editor.
         *
         * @param expressions
         * @param allNewFlowVariableNames the names of all output flow variables. Guaranteed to have the same length and
         *            order as the expressions.
         * @return list of diagnostics results for each editor, i.e. a list of diagnostics and the return type.
         */
        public List<ExpressionDiagnosticResult> getFlowVariableDiagnostics(final String[] expressions,
            final String[] allNewFlowVariableNames) {
            // Note that this method is similar to ExpressionFlowVariableNodeModel#validateFlowVariablesExpressions but
            // it collects Diagnostic objects instead of throwing an exception.

            List<ExpressionDiagnosticResult> diagnostics = new ArrayList<>();
            var availableFlowVariables = new HashMap<String, ReturnResult<FlowVariable>>(getFullFlowVariablesMap());

            // Only the names of the flow variables that are appended (not replaced)
            var appendedFlowVariableNames = Arrays.stream(allNewFlowVariableNames) //
                .map(name -> availableFlowVariables.containsKey(name) ? null : name) //
                .toList();

            for (int i = 0; i < expressions.length; i++) {
                var expression = expressions[i];
                List<ExpressionDiagnostic> diagnosticsForThisExpression = new ArrayList<>();

                var inferredType = ValueType.MISSING;
                try {
                    var ast = Expressions.parse(expression);

                    var prematureAccessDiagnostics = getPrematureAccessDiagnostics( //
                        ast, //
                        i, //
                        appendedFlowVariableNames //
                    );
                    diagnosticsForThisExpression.addAll(prematureAccessDiagnostics);

                    // if prematureAccessDiagnostics are present, type inference will fail;
                    // so infer a failure and continue to next expression
                    if (!prematureAccessDiagnostics.isEmpty()) {
                        availableFlowVariables.put(allNewFlowVariableNames[i],
                            invalidExpressionType(i, allNewFlowVariableNames[i]));
                        continue;
                    }

                    inferredType = Expressions.inferTypes( //
                        ast, //
                        ExpressionFlowVariableNodeModel::columnTypeResolver, //
                        fvName -> toValueType(availableFlowVariables, fvName) //
                    );

                    // Note: we only collect special column accesses here, as normal column accesses would have
                    // already thrown an error while inferring the type
                    diagnosticsForThisExpression.addAll( //
                        ExpressionRunnerUtils.collectColumnAccesses(ast) //
                            .stream() //
                            .map(specialColAccess -> ExpressionDiagnostic.withSameMessage( //
                                "No rows are available.", //
                                DiagnosticSeverity.ERROR, //
                                Expressions.getTextLocation(specialColAccess) //
                            )) //
                            .toList() //
                    );

                    if (appendedFlowVariableNames.get(i) != null
                        && appendsInvalidFlowVariable(appendedFlowVariableNames.get(i))) {
                        // this error will also be caught by the frontend - here we just
                        // need to make sure the diagnostics don't choke...
                        continue;
                    }

                    if (inferredType.isOptional()) {
                        // Show an error if the full expression might evaluate to MISSING; this is not supported
                        diagnosticsForThisExpression.add(ExpressionDiagnostic.withSameMessage( //
                            "The expression evaluates to a type that can be MISSING. "
                                + "Use the missing coalescing operator '??' to define a default value.", //
                            DiagnosticSeverity.ERROR, //
                            Expressions.getTextLocation(ast) //
                        ));
                    } else {
                        availableFlowVariables.put(allNewFlowVariableNames[i],
                            ReturnResult.success(new FlowVariable(allNewFlowVariableNames[i],
                                ExpressionRunnerUtils.mapValueTypeToVariableType(inferredType))));

                    }
                } catch (ExpressionCompileException ex) {
                    availableFlowVariables.put(allNewFlowVariableNames[i],
                        invalidExpressionType(i, allNewFlowVariableNames[i]));
                    diagnosticsForThisExpression.addAll(ExpressionDiagnostic.fromException(ex));
                } finally {
                    diagnostics
                        .add(new ExpressionDiagnosticResult(diagnosticsForThisExpression, inferredType.toString()));
                }
            }

            return diagnostics;
        }

        public void runFlowVariableExpression(final List<String> expressions, final List<String> newFlowVariableNames,
            final List<String> outputReturnType) throws ExpressionCompileException {

            var warnings = new ExpressionDiagnostic[expressions.size()];

            try {
                var resultVariables = ExpressionFlowVariableNodeModel.applyFlowVariableExpressions( //
                    expressions, //
                    newFlowVariableNames, //
                    outputReturnType.stream().map(FlowVariableTypeNames::getByTypeName).toList(), //
                    getSupportedFlowVariablesMap(), //
                    i -> {
                    }, // we do not show the progress
                    ExpressionDiagnostic.getWarningMessageHandler(warnings) //
                );
                m_outputFlowVariablesReference.set(resultVariables);
                sendEvent("updatePreview", null);

            } catch (WithIndexExpressionException e) { // NOSONAR - we send the message to the frontend
                warnings[e.getExpressionIndex()] =
                    ExpressionDiagnostic.withSameMessage(e.getUIMessage(), DiagnosticSeverity.ERROR, null);
                sendEvent("updatePreview", "Preview cannot be shown, because Expression " + (e.getExpressionIndex() + 1)
                    + " could not be evaluated.");
            }

            sendEvent("updateWarnings", warnings);
        }

        /**
         * Get a map of all flow variables (inclusive unsupported ones)
         *
         * @return a map of flow variables
         */
        private Map<String, ReturnResult<FlowVariable>> getFullFlowVariablesMap() {
            var stack = getWorkflowControl().getFlowObjectStack();

            return stack.getAllAvailableFlowVariables().entrySet().stream().map(e -> {
                var v = e.getValue();
                return Map.entry(e.getKey(), ReturnResult.success(v));
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        }

        /** Helper to get the ValueType of a flow variable from the given map (use for type inference) */
        static ReturnResult<ValueType> toValueType(final Map<String, ReturnResult<FlowVariable>> flowVariables,
            final String name) {
            var flowVariableResult = flowVariables.get(name);

            if (flowVariableResult == null) {
                return ReturnResult.failure("No flow variable with the name '" + name + "' is available.");
            }

            return flowVariableResult //
                .flatMap(flowVariable -> ReturnResult.fromNullable(
                    ExpressionRunnerUtils.mapVariableToValueType(flowVariable.getVariableType()),
                    "Flow variables of the type '" + flowVariable.getVariableType()
                        + "' are not supported in expressions."));
        }

    }
}
