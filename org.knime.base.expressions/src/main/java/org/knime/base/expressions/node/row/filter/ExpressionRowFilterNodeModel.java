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
package org.knime.base.expressions.node.row.filter;

import static org.knime.base.expressions.ExpressionRunnerUtils.flowVarToTypeForTypeInference;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.knime.base.expressions.ColumnInputUtils;
import org.knime.base.expressions.ExpressionEvaluationRuntimeException;
import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.NodeExpressionAdditionalInputs;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.ExpressionCompileException;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.Expressions;
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

/**
 * The node model for the row filter expression node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction") // the columnar table API is not public yet
final class ExpressionRowFilterNodeModel extends NodeModel {

    private final ExpressionRowFilterSettings m_settings;

    ExpressionRowFilterNodeModel() {
        super(1, 1);
        m_settings = new ExpressionRowFilterSettings();
    }

    /** @return the typed Ast for the configured expression */
    private static Ast getPreparedExpression(final String expression, final DataTableSpec inSpec,
        final Map<String, FlowVariable> availableFlowVariables) throws ExpressionCompileException {

        var ast = Expressions.parse(expression);
        Expressions.inferTypes(ast, //
            ColumnInputUtils.columnToTypesForTypeInference(inSpec), //
            flowVarToTypeForTypeInference(availableFlowVariables) //
        );
        return ast;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (ExpressionRowFilterSettings.DEFAULT_SCRIPT.equals(m_settings.getScript())) {
            throw new InvalidSettingsException("The expression node has not yet been configured. Enter an expression.");
        }

        var inputSpec = inSpecs[0];

        try {
            var ast = getPreparedExpression(m_settings.getScript(), inputSpec,
                getAvailableInputFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES));
            var outputType = Expressions.getInferredType(ast);

            if (ValueType.OPT_BOOLEAN.equals(outputType)) {
                throw new InvalidSettingsException("The expression evaluates to BOOLEAN | MISSING. "
                    + "Use the missing coalescing operator '??' to define if rows that evaluate to MISSING "
                    + "should be included or excluded. ");
            } else if (!ValueType.BOOLEAN.equals(outputType)) {
                throw new InvalidSettingsException("The expression evaluates to " + outputType.name() + ". "
                    + "It should evaluate to BOOLEAN in order to filter out rows for which the "
                    + "filter expression evaluates to false.");
            }
        } catch (final ExpressionCompileException e) {
            throw new InvalidSettingsException("Error in Expression: %s".formatted(e.getMessage()), e);
        }

        return new DataTableSpec[]{inputSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        var messageBuilder = createMessageBuilder();

        var outputTable = applyFilterExpression( //
            m_settings.getScript(), //
            inData[0], //
            getAvailableFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES), //
            exec, //
            messageBuilder::addTextIssue //
        );

        var issueCount = messageBuilder.getIssueCount();
        if (issueCount > 0) {
            var message = messageBuilder //
                .withSummary(
                    issueCount + " warning" + (issueCount == 1 ? "" : "s") + " occured while evaluating expression") //
                .build() //
                .orElse(null);
            setWarning(message);
        }

        return new BufferedDataTable[]{outputTable};
    }

    /**
     * Applies the given filter expression to the input table and returns the resulting table.
     *
     * @param expression the filter expression
     * @param inputTable the input table
     * @param availableFlowVariables the available flow variables
     * @param exec the execution context
     * @param setWarning a consumer for setting warnings
     * @return the filtered table
     * @throws ExpressionCompileException if the expression could not be compiled
     * @throws CanceledExecutionException if the execution was cancelled
     * @throws VirtualTableIncompatibleException
     * @throws ExpressionEvaluationException
     */
    public static BufferedDataTable applyFilterExpression( //
        final String expression, //
        final BufferedDataTable inputTable, //
        final Map<String, FlowVariable> availableFlowVariables, //
        final ExecutionContext exec, //
        final Consumer<String> setWarning //
    ) throws ExpressionCompileException, CanceledExecutionException, VirtualTableIncompatibleException,
        ExpressionEvaluationException {
        var numRows = inputTable.size();
        exec.setProgress(0, "Evaluating expression");

        // Parse the expression and infer the types
        var ast = getPreparedExpression(expression, inputTable.getDataTableSpec(), availableFlowVariables);

        // Create a reference table for the input table
        var inRefTable = ExpressionRunnerUtils.createReferenceTable(inputTable, exec.createSubExecutionContext(0.33));

        // Pre-evaluate the aggregations
        // NB: We use the inRefTable because it is guaranteed to be a columnar table
        ExpressionRunnerUtils.evaluateAggregations(ast, inRefTable.getBufferedTable(), exec.createSubProgress(0.33));

        // Evaluate the expression and materialize the result
        var additionalInputs = new NodeExpressionAdditionalInputs(availableFlowVariables);

        var filteredTable = ExpressionRunnerUtils.filterTableByExpression(inRefTable.getVirtualTable(), ast,
            inputTable.size(), setWarning::accept, additionalInputs);

        var materializeProgress = exec.createSubProgress(0.34);
        try {
            return ColumnarVirtualTableMaterializer.materializer() //
                .sources(inRefTable.getSources()) //
                .materializeRowKey(true) //
                .progress((rowIndex, rowKey) -> materializeProgress.setProgress(rowIndex / (double)numRows, //
                    () -> "Evaluating expression (row %d of %s)".formatted(rowIndex + 1, numRows)) //
                ) //
                .executionContext(exec) //
                .tableIdSupplier(Node.invokeGetDataRepository(exec)::generateNewID) //
                .materialize(filteredTable) //
                .getBufferedTable();
        } catch (ExpressionEvaluationRuntimeException e) { // NOSONAR - throwing only the cause is intended
            throw e.getCause();
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ExpressionRowFilterSettings().validate(settings);
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
