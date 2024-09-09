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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionDiagnostic;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
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
         * List of diagnostics for each editor, hence a 2D list.
         *
         * @param expressions
         * @param newFlowVariableNames
         * @return list of diagnostics for each editor, i.e. a list of a lists of diagnostics
         */
        public List<List<ExpressionDiagnostic>> getFlowVariableDiagnostics(final String[] expressions,
            final String[] newFlowVariableNames) {
            // Note that this method is similar to ExpressionFlowVariableNodeModel#validateFlowVariablesExpressions but
            // it collects Diagnostic objects instead of throwing an exception.

            List<List<ExpressionDiagnostic>> diagnostics = new ArrayList<>();

            // Add existing flow variables
            var availableFlowVariables = new HashMap<>(getSupportedFlowVariablesMap());

            for (int i = 0; i < expressions.length; i++) {
                var expression = expressions[i];
                var name = newFlowVariableNames[i];

                try {
                    var ast = Expressions.parse(expression);
                    var inferredType = Expressions.inferTypes(ast, //
                        ExpressionFlowVariableNodeModel::columnTypeResolver, //
                        fvName -> ExpressionFlowVariableNodeModel.toValueType(availableFlowVariables, fvName) //
                    );
                    if (inferredType.isOptional()) {
                        // Show an error if the full expression might evaluate to MISSING; this is not supported
                        diagnostics.add(List.of(
                            new ExpressionDiagnostic("The expression must evaluate to a value different from MISSING.",
                                DiagnosticSeverity.ERROR, Expressions.getTextLocation(ast))));
                    } else {
                        diagnostics.add(List.of());
                        ExpressionFlowVariableNodeModel.putFlowVariableForTypeCheck(availableFlowVariables, name,
                            inferredType);
                    }
                } catch (ExpressionCompileException ex) {
                    diagnostics.add(ExpressionDiagnostic.fromException(ex));
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
