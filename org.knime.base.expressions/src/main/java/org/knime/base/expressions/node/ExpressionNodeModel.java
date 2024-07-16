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
 *   Jan 11, 2024 (benjamin): created
 */
package org.knime.base.expressions.node;

import static org.knime.base.expressions.ExpressionRunnerUtils.columnToTypesForTypeInference;
import static org.knime.base.expressions.ExpressionRunnerUtils.flowVarToTypeForTypeInference;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionMapperFactory;
import org.knime.base.expressions.ExpressionMapperFactory.ExpressionMapperContext;
import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.ExpressionRunnerUtils.ColumnInsertionMode;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.columnar.table.VirtualTableExtensionTable;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.FlowVarAccess;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.table.virtual.expression.Exec;
import org.knime.core.table.virtual.spec.SourceTableProperties.CursorType;

/**
 * The node model for the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui node dialogs are not API yet
class ExpressionNodeModel extends NodeModel {

    private final ExpressionNodeSettings m_settings;

    ExpressionNodeModel() {
        super(1, 1);
        m_settings = new ExpressionNodeSettings();
    }

    static final VariableType<?>[] SUPPORTED_FLOW_VARIABLE_TYPES =
        new VariableType<?>[]{VariableType.BooleanType.INSTANCE, VariableType.DoubleType.INSTANCE,
            VariableType.LongType.INSTANCE, VariableType.IntType.INSTANCE, VariableType.StringType.INSTANCE};

    static final Set<VariableType<?>> SUPPORTED_FLOW_VARIABLE_TYPES_SET = Set.of(SUPPORTED_FLOW_VARIABLE_TYPES);

    /** @return the typed Ast for the configured expression */
    private Ast getPreparedExpression(final DataTableSpec inSpec, final String script)
        throws ExpressionCompileException {

        var ast = Expressions.parse(script);
        Expressions.inferTypes(ast, columnToTypesForTypeInference(inSpec),
            flowVarToTypeForTypeInference(getAvailableInputFlowVariables(SUPPORTED_FLOW_VARIABLE_TYPES)));
        return ast;
    }

    /**
     * Takes a single script, some settings, and an input table specification, and returns what the new specification
     * would look like if the script were to be applied to the input table.
     *
     * @param inputSpec the input table specification
     * @param outputMode whether to replace an existing column or add a new one
     * @param outputColumn the name of the column to be created/replaced
     * @param script the script to be applied
     * @param indexInScripts the index of the script in the list of scripts. Useful for error messages
     * @return the table specification after the script has been applied
     * @throws InvalidSettingsException if anything is wrong with the script or the settings
     */
    private DataTableSpec computeTableSpecAfterScriptApplied(final DataTableSpec inputSpec,
        final ColumnInsertionMode outputMode, final String outputColumn, final String script, final int indexInScripts)
        throws InvalidSettingsException {

        try {
            var ast = getPreparedExpression(inputSpec, script);
            var outputType = Expressions.getInferredType(ast);
            if (ValueType.MISSING.equals(outputType)) {
                throw new InvalidSettingsException(
                    "Expression (%d) evaluates to MISSING. Enter an expression that has an output type."
                        .formatted(indexInScripts + 1));
            }
            var outputDataSpec = Exec.valueTypeToDataSpec(outputType);
            var outputColumnSpec =
                ExpressionMapperFactory.primitiveDataSpecToDataColumnSpec(outputDataSpec.spec(), outputColumn);

            if (outputMode == ColumnInsertionMode.REPLACE_EXISTING) {
                return new DataTableSpecCreator(inputSpec) //
                    .replaceColumn(inputSpec.findColumnIndex(outputColumn), outputColumnSpec) //
                    .createSpec(); //
            } else if (!inputSpec.containsName(outputColumn)) {
                return new DataTableSpecCreator(inputSpec).addColumns(outputColumnSpec) //
                    .createSpec();
            } else {
                throw new InvalidSettingsException(
                    "The output column '%s' of Expression (%d) exists in the input table. Choose another column name."
                        .formatted(outputColumn, indexInScripts + 1));
            }
        } catch (final ExpressionCompileException e) {
            throw new InvalidSettingsException(
                "Error in Expression (%d): %s".formatted(indexInScripts + 1, e.getMessage()), e);
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getNumScripts() == 1
            && ExpressionNodeSettings.DEFAULT_SCRIPT.equals(m_settings.getScripts().get(0))) {
            throw new InvalidSettingsException("The expression node has not yet been configured. Enter an expression.");
        }

        int numberOfScripts = m_settings.getNumScripts();

        var lastOutputSpec = inSpecs[0];

        for (int i = 0; i < numberOfScripts; ++i) {
            lastOutputSpec =
                computeTableSpecAfterScriptApplied(lastOutputSpec, m_settings.getColumnInsertionModes().get(i),
                    m_settings.getActiveOutputColumns().get(i), m_settings.getScripts().get(i), i);
        }

        return new DataTableSpec[]{lastOutputSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        int numberOfScripts = m_settings.getNumScripts();

        var inTable = inData[0];

        var messageBuilder = createMessageBuilder();
        EvaluationContext ctx = messageBuilder::addTextIssue;

        for (int i = 0; i < numberOfScripts; ++i) {
            var subProgress = exec.createSubProgress(1.0 / numberOfScripts);

            var newColumnPosition = new ExpressionRunnerUtils.NewColumnPosition(
                m_settings.getColumnInsertionModes().get(i), m_settings.getActiveOutputColumns().get(i));

            // Prepare the expression
            var expression = getPreparedExpression(inTable.getDataTableSpec(), m_settings.getScripts().get(i));

            // Create a reference table for the input table
            var inRefTable =
                ExpressionRunnerUtils.createReferenceTable(inTable, exec, subProgress.createSubProgress(0.33));

            // Pre-evaluate the aggregations
            // NB: We use the inRefTable because it is guaranteed to be a columnar table
            ExpressionRunnerUtils.evaluateAggregations(expression, inRefTable.getBufferedTable(),
                subProgress.createSubProgress(0.33));

            // Evaluate the expression and materialize the result
            var exprContext = new NodeExpressionMapperContext(this::getAvailableInputFlowVariables);
            var expressionResult = ExpressionRunnerUtils.applyAndMaterializeExpression(inRefTable, expression,
                newColumnPosition.columnName(), exec, subProgress.createSubProgress(0.34), exprContext, ctx);

            // We must avoid using inRefTable.getVirtualTable() directly. Doing so would result in building upon the
            // transformation of the input table, instead of initiating a new fragment. This approach leads to complications
            // when loading the virtual table, as it would attempt to resolve the sources of the input table. By creating a
            // new ColumnarVirtualTable, we establish a new SourceTableTransform that references the input table. This
            // ensures that the input table itself acts as the source, providing a clean slate for transformations.
            // Note that the CursorType is irrelevant because the transform gets re-sourced for running the comp graph.
            var inputVirtualTable =
                new ColumnarVirtualTable(inRefTable.getId(), inRefTable.getSchema(), CursorType.BASIC);
            var output = ExpressionRunnerUtils.constructOutputTable(inputVirtualTable,
                expressionResult.getVirtualTable(), newColumnPosition);

            @SuppressWarnings("resource") // #close clears the table but we still want to keep the data for the output
            var outputExtensionTable =
                new VirtualTableExtensionTable(new ReferenceTable[]{inRefTable, expressionResult}, output,
                    inTable.size(), Node.invokeGetDataRepository(exec).generateNewID());

            inTable = outputExtensionTable.create(exec);
        }

        var issueCount = messageBuilder.getIssueCount();
        if (issueCount > 0) {
            var message = messageBuilder //
                .withSummary(
                    issueCount + " warning" + (issueCount == 1 ? "" : "s") + " occured while evaluating expression") //
                .build() //
                .orElse(null);
            setWarning(message);
        }

        return new BufferedDataTable[]{inTable};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveModelSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ExpressionNodeSettings().loadModelSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadModelSettings(settings);
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

    static class NodeExpressionMapperContext implements ExpressionMapperContext {

        private final Function<VariableType<?>[], Map<String, FlowVariable>> m_getFlowVariable;

        public NodeExpressionMapperContext(
            final Function<VariableType<?>[], Map<String, FlowVariable>> getFlowVariable) {
            m_getFlowVariable = getFlowVariable;
        }

        @Override
        public Optional<Computer> flowVariableToComputer(final FlowVarAccess flowVariableAccess) {
            return Optional
                .ofNullable(m_getFlowVariable.apply(SUPPORTED_FLOW_VARIABLE_TYPES).get(flowVariableAccess.name()))
                .map(NodeExpressionMapperContext::computerForFlowVariable);
        }

        @Override
        public Optional<Computer> aggregationToComputer(final AggregationCall aggregationCall) {
            return Optional.ofNullable(ExpressionRunnerUtils.getAggregationResultComputer(aggregationCall));
        }

        private static Computer computerForFlowVariable(final FlowVariable variable) {
            var variableType = variable.getVariableType();

            if (variableType == VariableType.BooleanType.INSTANCE) {
                return Computer.BooleanComputer.of(ctx -> variable.getValue(VariableType.BooleanType.INSTANCE),
                    ctx -> false);
            } else if (variableType == VariableType.DoubleType.INSTANCE) {
                return Computer.FloatComputer.of(ctx -> variable.getValue(VariableType.DoubleType.INSTANCE),
                    ctx -> false);
            } else if (variableType == VariableType.LongType.INSTANCE) {
                return Computer.IntegerComputer.of(ctx -> variable.getValue(VariableType.LongType.INSTANCE),
                    ctx -> false);
            } else if (variableType == VariableType.IntType.INSTANCE) {
                return Computer.IntegerComputer.of(ctx -> variable.getValue(VariableType.IntType.INSTANCE),
                    ctx -> false);
            } else if (variableType == VariableType.StringType.INSTANCE) {
                return Computer.StringComputer.of(ctx -> variable.getValue(VariableType.StringType.INSTANCE),
                    ctx -> false);
            } else {
                throw new IllegalArgumentException("Unsupported variable type: " + variableType);
            }
        }
    }
}
