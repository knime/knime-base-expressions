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
import org.knime.base.expressions.ExpressionRunnerUtils.InsertionMode;
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
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.scripting.editor.WorkflowControl;

/**
 * The node model for the Flow variable Expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction") // webui node dialogs are not API yet
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
    private Ast getPreparedExpression(final String script) throws ExpressionCompileException {

        var ast = Expressions.parse(script);
        Expressions.inferTypes(ast,
            name -> ReturnResult.failure(
                "Accessing a column is not allowed for the flow variable node. This is an implementation error."),
            flowVarToTypeForTypeInference(
                getAvailableInputFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES)));

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
        // FIXME: Evaluating the expressions can be in case of a lot of flow variables or very complex expressions
        // (f.i. regex) slow.
        // Do we want to do that while configure like the variable node does?

        return new PortObjectSpec[]{FlowVariablePortObject.INSTANCE.getSpec()};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        exec.setProgress(0, "Evaluating flow variables.");
        pushFlowVariables(exec);
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private void pushFlowVariables(final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException {

        var workflowControl = new WorkflowControl(NodeContext.getContext().getNodeContainer());

        int numberOfScripts = m_settings.getNumScripts();

        var messageBuilder = createMessageBuilder();
        EvaluationContext ctx = messageBuilder::addTextIssue;

        Map<String, FlowVariable> addedFlowVariables = new HashMap<>();

        Predicate<String> flowVariableExists =
            name -> workflowControl.getFlowObjectStack().getAllAvailableFlowVariables().containsKey(name)
                || addedFlowVariables.containsKey(name);

        for (int i = 0; i < numberOfScripts; ++i) {

            String newName = m_settings.getActiveOutputFlowVariables().get(i);

            boolean isReplace = m_settings.getFlowVariableInsertionModes().get(i) == InsertionMode.REPLACE_EXISTING;

            if (flowVariableExists.test(newName) && !isReplace) {
                throw new InvalidSettingsException(
                    "The flow variable " + newName + " already exists. Choose 'replace' to replace the flow variable.");
            }

            var evaluatedExpression =
                evaluateFlowVariableExpression(m_settings.getScripts().get(i), addedFlowVariables);

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

            exec.setProgress(i * 1.0 / numberOfScripts, i + ". flow variable (" + newName + ") evaluated.");
            exec.checkCanceled();
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
        exec.setProgress(1, "Flow variables evaluated.");
    }

    private Computer evaluateFlowVariableExpression(final String script,
        final Map<String, FlowVariable> addedFlowVariables) throws InvalidSettingsException {

        try {
            var expression = getPreparedExpression(script);

            if (!Expressions.collectColumnAccesses(expression).isEmpty()) {
                throw new InvalidSettingsException(
                    "The flow variable expression node cannot access columns. Use the Expression node instead.");
            }

            var exprContext = new NodeExpressionMapperContext(types -> {
                Map<String, FlowVariable> availableFlowVariables = getAvailableInputFlowVariables(types);
                availableFlowVariables.putAll(addedFlowVariables);

                return availableFlowVariables;
            });

            return Expressions.evaluate( //
                expression, //
                ExpressionFlowVariableNodeModel::dummyResolver, //
                exprContext::flowVariableToComputer, //
                ExpressionFlowVariableNodeModel::dummyResolver);
        } catch (final ExpressionCompileException e) {
            throw new InvalidSettingsException("Failed to evaluate flow variable expression: " + e.getMessage(), e);
        }
    }

    /**
     * @param c the flow variable name; unused
     */
    private static <T> Optional<Computer> dummyResolver(final T c) {
        return Optional.empty();
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
