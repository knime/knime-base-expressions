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

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.InsertionMode;
import org.knime.base.expressions.node.WithIndexExpressionException;
import org.knime.base.expressions.node.variable.ExpressionFlowVariableSettings.FlowVariableTypeNames;
import org.knime.core.expressions.Ast.FlowVarAccess;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionCompileException;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
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
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
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
        m_settings = new ExpressionFlowVariableSettings();
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
        // Note that this method is similar to ExpressionFlowVariableNodeScriptingService#getFlowVariableDiagnostic but
        // it throws an InvalidSettingsException instead of returning a Diagnostic.
        // Additionally, it checks for correct flow variable replacement

        // Add existing flow variables
        var availableFlowVariables =
            new HashMap<>(getAvailableInputFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES));

        for (int i = 0; i < m_settings.getNumScripts(); i++) {
            var expression = m_settings.getScripts().get(i);
            var name = m_settings.getActiveOutputFlowVariables().get(i);

            validateName(name, m_settings.getFlowVariableInsertionModes().get(i),
                availableFlowVariables.containsKey(name));

            try {
                var ast = Expressions.parse(expression);
                var inferredType = Expressions.inferTypes(ast, //
                    ExpressionFlowVariableNodeModel::columnTypeResolver, //
                    ExpressionRunnerUtils.flowVarToTypeForTypeInference(availableFlowVariables) //
                );

                var columnAccesses = ExpressionRunnerUtils.collectColumnAccesses(ast);
                if (!columnAccesses.isEmpty()) {
                    // Note that other column accesses cause errors during type inference
                    throw new InvalidSettingsException("Expression " + (i + 1)
                        + " refers to ROW_ID, ROW_INDEX, or ROW_NUMBER, but no rows are available.");
                }

                if (inferredType.isOptional()) {
                    throw new InvalidSettingsException(
                        "Expression " + (i + 1) + " evaluates to a type that can be MISSING. "
                            + "Use the missing coalescing operator '??' to define a default value.");
                }
                var variableType = ExpressionRunnerUtils.mapValueTypeToVariableType(inferredType);
                if (variableType.isError()) {
                    throw new InvalidSettingsException("Expression " + (i + 1)
                        + " evaluates to a type that is not supported by flow variables: " + inferredType + ".");
                }

                // Add the flow variable to the map for type checking of the later expressions
                // NB: The value is not important for type checking
                availableFlowVariables.put(name, new FlowVariable(name, variableType.getValue()));
            } catch (ExpressionCompileException e) {
                throw new InvalidSettingsException( //
                    "Error in Expression " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }

    private static void validateName(final String name, final InsertionMode insertionMode, final boolean exists)
        throws InvalidSettingsException {
        if (name == null || name.isEmpty()) {
            throw new InvalidSettingsException("The flow variable name must not be empty.");
        }

        boolean isReplace = insertionMode == InsertionMode.REPLACE_EXISTING;

        if (exists && !isReplace) {
            throw new InvalidSettingsException(
                "The flow variable " + name + " already exists. Choose 'replace' to replace the flow variable.");
        }
        if (!exists && isReplace) {
            throw new InvalidSettingsException(
                "The flow variable " + name + " does not exist. Choose 'append' to create a new flow variable.");
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        var messageBuilder = createMessageBuilder();
        var numScripts = m_settings.getNumScripts();
        var names = m_settings.getActiveOutputFlowVariables();

        // Apply expressions to flow variables
        List<FlowVariable> resultVariables;
        try {
            resultVariables = applyFlowVariableExpressions( //
                m_settings.getScripts(), //
                names, //
                m_settings.getReturnTypes(), //
                getAvailableFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES), //
                i -> exec.setProgress( //
                    1.0 * i / m_settings.getNumScripts(), //
                    () -> "Evaluating flow variable \"" + names.get(i) + "\" (" + (i + 1) + "/" + numScripts + ")." //
                ), //
                (i, warningMessage) -> messageBuilder.addTextIssue("Expression " + (i + 1) + ": " + warningMessage) //
            );
        } catch (WithIndexExpressionException e) {
            throw e.toKNIMEException();
        }

        // Push flow variables in reverse, so they appear on the stack
        // in the same order as the expressions in the dialog
        for (int i = resultVariables.size() - 1; i >= 0; i--) {
            pushFlowVariableObj(resultVariables.get(i));
        }

        // Set warning message if there are any issues
        var issueCount = messageBuilder.getIssueCount();
        if (issueCount > 0) {
            var message = messageBuilder //
                .withSummary(
                    issueCount + " warning" + (issueCount == 1 ? "" : "s") + " occured while evaluating expression.") //
                .build() //
                .orElse(null);
            setWarning(message);
        }

        exec.setProgress(1);
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * Applies the given expressions to the flow variables and returns a list of output variables.
     *
     * @param expressions the expressions to apply
     * @param names the names of the output variables
     * @param existingVariables a map of the existing flow variables
     * @param updateProgress a consumer that is called each time before an expression is evaluated with the index of the
     *            expression that is being evaluated
     * @param setWarning a consumer that is called if the evaluation of an expression produces a warning with the index
     *            of the expression and the warning message
     * @return a list of output variables
     * @throws ExpressionCompileException if an expression cannot be compiled
     * @throws WithIndexExpressionException
     */
    static List<FlowVariable> applyFlowVariableExpressions( //
        final List<String> expressions, //
        final List<String> names, //
        final List<FlowVariableTypeNames> returnTypes, //
        final Map<String, FlowVariable> existingVariables, //
        final IntConsumer updateProgress, //
        final BiConsumer<Integer, String> setWarning //
    ) throws ExpressionCompileException, WithIndexExpressionException {
        var availableFlowVariables = new HashMap<String, FlowVariable>(existingVariables);
        var outputVariables = new ArrayList<FlowVariable>();
        var executionStartTime = ZonedDateTime.now();

        for (int i = 0; i < expressions.size(); i++) {
            updateProgress.accept(i);

            var expression = expressions.get(i);
            var name = names.get(i);

            // Prepare - parse and infer types
            var ast = Expressions.parse(expression);
            Expressions.inferTypes(ast, //
                ExpressionFlowVariableNodeModel::columnTypeResolver,
                ExpressionRunnerUtils.flowVarToTypeForTypeInference(availableFlowVariables) //
            );

            // Evaluate
            var resultComputer = Expressions.evaluate( //
                ast, //
                column -> Optional.empty(), //
                flowVar -> toComputer(availableFlowVariables, flowVar), //
                aggregation -> Optional.empty() //
            );

            final var finalI = i;
            var outputVariable = createFlowVariableFromComputer( //
                finalI, //
                name, //
                resultComputer, //
                returnTypes.get(finalI), //
                EvaluationContext.of(executionStartTime, warning -> setWarning.accept(finalI, warning)) //
            );

            availableFlowVariables.put(name, outputVariable);
            outputVariables.add(outputVariable);
        }

        return deduplicateOutputVariables(outputVariables);
    }

    private static FlowVariable createFlowVariableFromComputer(final int index, final String name,
        final Computer computer, final FlowVariableTypeNames returnType, final EvaluationContext ctx)
        throws WithIndexExpressionException {

        try {
            if (computer instanceof IntegerComputer c) {
                return createFlowVariableFromIntegerComputer(index, name, returnType, ctx, c);
            } else if (computer instanceof StringComputer c) {
                return new FlowVariable(name, StringType.INSTANCE, c.compute(ctx));
            } else if (computer instanceof FloatComputer c) {
                return new FlowVariable(name, DoubleType.INSTANCE, c.compute(ctx));
            } else if (computer instanceof BooleanComputer c) {
                return new FlowVariable(name, BooleanType.INSTANCE, c.compute(ctx));
            } else {
                throw new IllegalStateException("Unexpected computer type: " + computer);
            }
        } catch (ExpressionEvaluationException e) {
            throw WithIndexExpressionException.forEvaluationException(index, e);
        }
    }

    private static FlowVariable createFlowVariableFromIntegerComputer(final int index, final String name,
        final FlowVariableTypeNames returnType, final EvaluationContext ctx, final IntegerComputer c)
        throws ExpressionEvaluationException, WithIndexExpressionException {
        var computedValue = c.compute(ctx);

        if (returnType == FlowVariableTypeNames.INTEGER) {
            if (computedValue < Integer.MIN_VALUE || computedValue > Integer.MAX_VALUE) {
                throw WithIndexExpressionException.forResultOutOfRange(computedValue, index);
            }

            return new FlowVariable(name, IntType.INSTANCE, (int)computedValue);
        } else {
            return new FlowVariable(name, LongType.INSTANCE, computedValue);
        }
    }

    private static List<FlowVariable> deduplicateOutputVariables(final List<FlowVariable> outputVariables) {

        Map<String, FlowVariable> deduplicatedMap = new LinkedHashMap<>();

        for (FlowVariable variable : outputVariables) {
            if (deduplicatedMap.containsKey(variable.getName())) {
                deduplicatedMap.remove(variable.getName());
            }
            deduplicatedMap.put(variable.getName(), variable);
        }

        return new ArrayList<>(deduplicatedMap.values());
    }

    /** Helper to return a failure message on column access */
    static ReturnResult<ValueType> columnTypeResolver(@SuppressWarnings("unused") final String colName) {
        return ReturnResult.failure("No row values are available. Use '$$' to access flow variables.");
    }

    private static Optional<Computer> toComputer(final Map<String, FlowVariable> flowVariables,
        final FlowVarAccess flowVarAccess) {
        return Optional.ofNullable(flowVariables.get(flowVarAccess.name())) //
            .map(ExpressionRunnerUtils::computerForFlowVariable);
    }

    /**
     * Helper to push a {@link FlowVariable} object to the flow variable stack because
     * NodeModel#pushFlowVariable(FlowVariable) is package private.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void pushFlowVariableObj(final FlowVariable flowVariable) {
        // NB: This is a hack because NodeModel#pushFlowVariable(FlowVariable) is package private
        pushFlowVariable(flowVariable.getName(), (VariableType)flowVariable.getVariableType(),
            flowVariable.getValue(flowVariable.getVariableType()));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ExpressionFlowVariableSettings().validate(settings);
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
