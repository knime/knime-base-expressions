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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.ExpressionCodeAssistant;
import org.knime.base.expressions.node.ExpressionNodeDiagnosticsUtils.Diagnostic;
import org.knime.base.expressions.node.ExpressionNodeDiagnosticsUtils.DiagnosticSeverity;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.scripting.editor.ScriptingService;

/**
 * {@link ScriptingService} implementation for the Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionFlowVariableNodeScriptingService extends ScriptingService {

    /**
     * This should never be called. Fails with an error message to identify implementation errors
     */
    private Function<String, ReturnResult<ValueType>> m_columnToType = name -> ReturnResult
        .failure("Flow variable expressions do not allow column accesses. Please use the expression node.");

    ExpressionFlowVariableNodeScriptingService() {
        super(null, ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES_SET::contains);
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

        private Map<String, FlowVariable> getAvailableFlowVariables(final VariableType<?>[] types) {
            var flowObjectStack = getWorkflowControl().getFlowObjectStack();
            if (flowObjectStack != null) {
                return flowObjectStack.getAvailableFlowVariables(types);
            } else {
                return Map.of();
            }
        }

        /**
         * Parses and type-checks the expression.
         *
         * @param script the expression to parse
         * @param additionalFlowVariableNames the names of the additional columns that are available (from previous
         *            expression editors in the same node)
         * @param additionalFlowVariableTypes the types of the additional columns that are available (from previous
         *            expression editors in the same node)
         *
         * @return an expression that is ready to be executed
         */
        private Ast getPreparedExpression(final String script, final List<String> additionalFlowVariableNames,
            final List<ValueType> additionalFlowVariableTypes) throws ExpressionCompileException {

            var ast = Expressions.parse(script);

            Map<String, FlowVariable> flowVars =
                getAvailableFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES);

            Function<String, ReturnResult<ValueType>> flowVarToTypeMapper = name -> {

                if (additionalFlowVariableNames.contains(name)) {
                    return ReturnResult
                        .success(additionalFlowVariableTypes.get(additionalFlowVariableNames.indexOf(name)));
                } else {
                    return ReturnResult.fromNullable(flowVars.get(name), //
                        "No flow variable with the name '" + name + "' is available.") //
                        .map(FlowVariable::getVariableType) //
                        .flatMap(type -> ReturnResult.fromNullable(ExpressionRunnerUtils.mapVariableToValueType(type),
                            "Flow variables of the type '" + type + "' are not supported"));
                }
            };

            Expressions.inferTypes(ast, m_columnToType, flowVarToTypeMapper);
            return ast;
        }

        /**
         * List of diagnostics for each editor, hence a 2D list.
         *
         * @param expressions
         * @param newFlowVariableNames
         * @return list of diagnostics for each editor, i.e. a list of a lists of diagnostics
         */
        public List<List<Diagnostic>> getFlowVariableDiagnostics(final String[] expressions,
            final String[] newFlowVariableNames) {
            List<ValueType> inferredFlowVariableTypes = new ArrayList<>();
            List<String> additionalFlowVariableNames = new ArrayList<>();
            List<List<Diagnostic>> diagnostics = new ArrayList<>();

            for (int i = 0; i < expressions.length; ++i) {
                var expression = expressions[i];

                try {
                    var ast = getPreparedExpression(expression, additionalFlowVariableNames, inferredFlowVariableTypes);

                    var inferredType = Expressions.getInferredType(ast);
                    inferredFlowVariableTypes.add(inferredType);

                    if (ValueType.MISSING.equals(inferredType)) {
                        // Show an error if the full expression has the output type "MISSING"; this is not supported
                        diagnostics
                            .add(List.of(new Diagnostic("The full expression must not have the value type MISSING.",
                                DiagnosticSeverity.ERROR, Expressions.getTextLocation(ast))));
                    } else {
                        diagnostics.add(List.of());
                    }
                    additionalFlowVariableNames.add(newFlowVariableNames[i]);

                } catch (ExpressionCompileException ex) {
                    // If there is an error in the expression, we still want to be able to continue with the other
                    // expression diagnostics, so add a missing type to the list of inferred types and continue
                    inferredFlowVariableTypes.add(ValueType.MISSING);

                    diagnostics.add(Diagnostic.fromException(ex));
                }
            }

            return diagnostics;
        }

        public void runFlowVariableExpression(final String[] expressions, final String[] newFlowVariableNames) {
            throw new UnsupportedOperationException("Preview not implemented yet.");
        }

        private static String formatWarning(final String warningText) {
            // TODO: is this actually how we want to do it?
            return "⚠️  \u001b[47m\u001b[30m%s\u001b[0m%n".formatted(warningText);
        }
    }
}
