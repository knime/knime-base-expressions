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

import static org.knime.base.expressions.ExpressionRunnerUtils.flowVarToTypeForTypeInference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.InsertionMode;
import org.knime.base.expressions.node.NodeExpressionMapperContext;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;

/**
 * The node model for the Flow variable Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
final class ExpressionFlowVariableNodeModel extends NodeModel {

    private final ExpressionFlowVariableSettings m_settings;

    ExpressionFlowVariableNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});

        // TODO(AP-23250) The `null` parameter sets the initial replacement flow variable in case the node
        // replaces a flow variable. In the dialog the scripting service is set up where the initial replacement
        // column/flow variable is correctly set but the frontend gets this setting here.

        m_settings = new ExpressionFlowVariableSettings(null);
    }

    /** @return the typed Ast for the configured expression */
    private Ast getPreparedExpression(final String script, final Map<String, FlowVariable> addedFlowVariables)
        throws ExpressionCompileException {

        var currentTemporaryFlowVariables = getTemporaryFlowVariables(addedFlowVariables);

        var ast = Expressions.parse(script);
        Expressions.inferTypes(ast,
            name -> ReturnResult.failure(
                "Accessing a column is not allowed for the flow variable node. This is an implementation error."),
            flowVarToTypeForTypeInference(currentTemporaryFlowVariables));

        return ast;
    }

    /**
     * {@inheritDoc}
     *
     * Evaluating and pushing a single flow variable takes roughly 2µs. There have to be unrealistically many flow
     * variables to make this a performance issue, so we can make the flow variables available even while only
     * configuring this node.
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        if (ExpressionFlowVariableSettings.DEFAULT_SCRIPT.equals(m_settings.getScripts().get(0))) {
            throw new InvalidSettingsException(
                "The flow variable expression node has not yet been configured. Enter an expression.");
        }

        validateFlowVariablesExpressions();

        return new PortObjectSpec[]{FlowVariablePortObject.INSTANCE.getSpec()};
    }

    private void validateFlowVariablesExpressions() throws InvalidSettingsException {

        Map<String, FlowVariable> addedFlowVariables = new HashMap<>();
        Predicate<String> flowVariableExists =
            name -> getAvailableInputFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES)
                .containsKey(name) || addedFlowVariables.containsKey(name);

        for (int i = 0; i < m_settings.getNumScripts(); ++i) {

            String newName = m_settings.getActiveOutputFlowVariables().get(i);

            boolean isReplace = m_settings.getFlowVariableInsertionModes().get(i) == InsertionMode.REPLACE_EXISTING;
            boolean exists = flowVariableExists.test(newName);

            if (exists && !isReplace) {
                throw new InvalidSettingsException(
                    "The flow variable " + newName + " already exists. Choose 'replace' to replace the flow variable.");
            }

            if (!exists && isReplace) {
                throw new InvalidSettingsException(
                    "The flow variable " + newName + " does not exist. Choose 'append' to create a new flow variable.");
            }

            Ast expression;
            try {
                expression = getPreparedExpression(m_settings.getScripts().get(i), addedFlowVariables);
            } catch (ExpressionCompileException e) {
                throw new InvalidSettingsException(e);
            }

            if (!Expressions.collectColumnAccesses(expression).isEmpty()) {
                throw new InvalidSettingsException(
                    "The flow variable expression node cannot access columns. Use the Expression node instead.");
            }

            if (!isReplace) {
                var variableType =
                    ExpressionRunnerUtils.mapValueTypeToVariableType(Expressions.getInferredType(expression));
                addedFlowVariables.put( //
                    newName, //
                    new FlowVariable(newName, variableType) // values are not needed, no evaluation
                );
            }
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        pushFlowVariables(exec);
        exec.setProgress(1);

        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * @param exec the execution context, can be null -> no progress reported
     * @throws CanceledExecutionException thrown when the user cancels the execution
     * @throws ExpressionCompileException thrown when the expression cannot be compiled
     */
    private void pushFlowVariables(final ExecutionContext exec)
        throws CanceledExecutionException, ExpressionCompileException {

        // TODO(AP-23274): add expression dependency
        var messageBuilder = createMessageBuilder();
        EvaluationContext ctx = messageBuilder::addTextIssue;

        Map<String, FlowVariable> addedFlowVariables = new HashMap<>();

        for (int i = 0; i < m_settings.getNumScripts(); ++i) {

            String newName = m_settings.getActiveOutputFlowVariables().get(i);

            final int finalI = i + 1;
            if (exec != null) {
                exec.setProgress( //
                    1.0 * i / m_settings.getNumScripts(), //
                    () -> "Evaluating flow variable \"" + newName + //
                        "\" (" + (finalI) + "/" + m_settings.getNumScripts() + ").");
                exec.checkCanceled();
            }

            boolean isReplace = m_settings.getFlowVariableInsertionModes().get(i) == InsertionMode.REPLACE_EXISTING;

            var expression = getPreparedExpression(m_settings.getScripts().get(i), addedFlowVariables);

            var evaluatedExpression = evaluateFlowVariableExpression(expression, addedFlowVariables);

            if (evaluatedExpression instanceof IntegerComputer c) {
                pushFlowVariable(newName, LongType.INSTANCE, c.compute(ctx));
                if (!isReplace) {
                    addedFlowVariables.put(newName, new FlowVariable(newName, LongType.INSTANCE, c.compute(ctx)));
                }
            }
            if (evaluatedExpression instanceof StringComputer c) {
                pushFlowVariable(newName, StringType.INSTANCE, c.compute(ctx));
                if (!isReplace) {
                    addedFlowVariables.put(newName, new FlowVariable(newName, StringType.INSTANCE, c.compute(ctx)));
                }
            }
            if (evaluatedExpression instanceof FloatComputer c) {
                pushFlowVariable(newName, DoubleType.INSTANCE, c.compute(ctx));
                if (!isReplace) {
                    addedFlowVariables.put(newName, new FlowVariable(newName, DoubleType.INSTANCE, c.compute(ctx)));
                }
            }
            if (evaluatedExpression instanceof BooleanComputer c) {
                pushFlowVariable(newName, BooleanType.INSTANCE, c.compute(ctx));
                if (!isReplace) {
                    addedFlowVariables.put(newName, new FlowVariable(newName, BooleanType.INSTANCE, c.compute(ctx)));
                }
            }

        }

        var issueCount = messageBuilder.getIssueCount();
        if (issueCount > 0) {
            var message = messageBuilder //
                .withSummary(
                    issueCount + " warning" + (issueCount == 1 ? "" : "s") + " occured while evaluating expression.") //
                .build() //
                .orElse(null);
            setWarning(message);
        }
    }

    private Computer evaluateFlowVariableExpression(final Ast expression,
        final Map<String, FlowVariable> addedFlowVariables) throws ExpressionCompileException {

        var currentTemporaryFlowVariables = getTemporaryFlowVariables(addedFlowVariables);
        var exprContext = new NodeExpressionMapperContext(types -> currentTemporaryFlowVariables);

        return Expressions.evaluate( //
            expression, //
            column -> Optional.empty(), //
            exprContext::flowVariableToComputer, //
            aggregation -> Optional.empty());

    }

    private Map<String, FlowVariable> getTemporaryFlowVariables(final Map<String, FlowVariable> addedFlowVariables) {

        Map<String, FlowVariable> availableFlowVariables = new HashMap<>();
        availableFlowVariables
            .putAll(getAvailableInputFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES));
        availableFlowVariables.putAll(addedFlowVariables);

        return availableFlowVariables;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ExpressionFlowVariableSettings(null).validate(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void reset() {
        // nothing to do
    }

}
