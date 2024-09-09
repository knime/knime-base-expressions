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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.scripting.editor.ScriptingService;

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

    @Override
    public RpcService getJsonRpcService() {
        return new ExpressionNodeRpcService();
    }

    @Override
    public void onDeactivate() {
        //  nothing to do
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

        /**
         * List of diagnostics for each editor, hence a 2D list.
         *
         * @param expressions
         * @param newFlowVariableNames the names of the appended flow variables. Guaranteed to have the same length and
         *            order as the expressions. Some elements are null, for expressions that replaced instead of
         *            appending.
         * @return list of diagnostics for each editor, i.e. a list of a lists of diagnostics
         */
        public List<List<ExpressionDiagnostic>> getFlowVariableDiagnostics(final String[] expressions,
            final String[] newFlowVariableNames) {
            // Note that this method is similar to ExpressionFlowVariableNodeModel#validateFlowVariablesExpressions but
            // it collects Diagnostic objects instead of throwing an exception.

            List<List<ExpressionDiagnostic>> diagnostics = new ArrayList<>();

            var availableFlowVariables = new HashMap<>(getSupportedFlowVariablesMap());

            for (int i = 0; i < expressions.length; i++) {
                var expression = expressions[i];
                List<ExpressionDiagnostic> diagnosticsForThisExpression = new ArrayList<>();

                try {
                    var ast = Expressions.parse(expression);

                    var prematureAccessDiagnostics = getPrematureAccessDiagnostics( //
                        ast, //
                        i, //
                        Arrays.asList(newFlowVariableNames) //
                    );
                    diagnosticsForThisExpression.addAll(prematureAccessDiagnostics);

                    // if prematureAccessDiagnostics are present, type inference will fail; continue to next expression
                    if (!prematureAccessDiagnostics.isEmpty()) {
                        continue;
                    }

                    var inferredType = Expressions.inferTypes( //
                        ast, //
                        ExpressionFlowVariableNodeModel::columnTypeResolver, //
                        fvName -> ExpressionFlowVariableNodeModel.toValueType(availableFlowVariables, fvName) //
                    );
                    if (inferredType.isOptional()) {
                        // Show an error if the full expression might evaluate to MISSING; this is not supported
                        diagnosticsForThisExpression.add(ExpressionDiagnostic.withSameMessage( //
                            "The full expression must not evaluate to MISSING.", //
                            DiagnosticSeverity.ERROR, //
                            Expressions.getTextLocation(ast) //
                        ));
                    } else {
                        ExpressionFlowVariableNodeModel.putFlowVariableForTypeCheck( //
                            availableFlowVariables, //
                            newFlowVariableNames[i], //
                            inferredType //
                        );
                    }
                } catch (ExpressionCompileException ex) {
                    diagnosticsForThisExpression.addAll(ExpressionDiagnostic.fromException(ex));
                } finally {
                    diagnostics.add(diagnosticsForThisExpression);
                }
            }

            return diagnostics;
        }

        public void runFlowVariableExpression(final List<String> expressions, final List<String> newFlowVariableNames)
            throws ExpressionCompileException {

            var resultVariables = ExpressionFlowVariableNodeModel.applyFlowVariableExpressions( //
                expressions, //
                newFlowVariableNames, //
                getSupportedFlowVariablesMap(), //
                i -> {
                }, // we do not show the progress
                this::handleWarningMessage //
            );
            m_outputFlowVariablesReference.set(resultVariables);

            sendEvent("updatePreview", null);

        }

        private void handleWarningMessage(final int i, final String warningMessage) {
            // TODO(AP-23152) do not use the console output
            addConsoleOutputEvent(new ConsoleText("Expression " + (i + 1) + ": " + warningMessage + "\n", false));
        }
    }
}
