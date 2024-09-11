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

import static org.knime.base.expressions.ExpressionRunnerUtils.columnToTypesForTypeInference;
import static org.knime.base.expressions.ExpressionRunnerUtils.flowVarToTypeForTypeInference;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.node.NodeExpressionMapperContext;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
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
import org.knime.core.table.virtual.spec.SourceTableProperties.CursorType;

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
            columnToTypesForTypeInference(inSpec), //
            flowVarToTypeForTypeInference(availableFlowVariables) //
        );
        return ast;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (ExpressionRowFilterSettings.DEFAULT_SCRIPT.equals(m_settings.getScript())) {
            throw new InvalidSettingsException("The expression node has not yet been configured. Enter an expression.");
        }

        var lastOutputSpec = inSpecs[0];

        return new DataTableSpec[]{lastOutputSpec};
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
     */
    public static BufferedDataTable applyFilterExpression( //
        final String expression, //
        final BufferedDataTable inputTable, //
        final Map<String, FlowVariable> availableFlowVariables, //
        final ExecutionContext exec, //
        final Consumer<String> setWarning //
    ) throws ExpressionCompileException, CanceledExecutionException, VirtualTableIncompatibleException {
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
        var exprContext = new NodeExpressionMapperContext(availableFlowVariables);

        // We must avoid using inRefTable.getVirtualTable() directly. Doing so would result in building upon the
        // transformation of the input table, instead of initiating a new fragment. This approach leads to complications
        // when loading the virtual table, as it would attempt to resolve the sources of the input table. By creating a
        // new ColumnarVirtualTable, we establish a new SourceTableTransform that references the input table. This
        // ensures that the input table itself acts as the source, providing a clean slate for transformations.
        // Note that the CursorType is irrelevant because the transform gets re-sourced for running the comp graph.
        var inputVirtualTable = new ColumnarVirtualTable(inRefTable.getId(), inRefTable.getSchema(), CursorType.BASIC);

        var filteredTable = ExpressionRunnerUtils.filterTableByExpression(inputVirtualTable, ast, inputTable.size(),
            setWarning::accept, exprContext);

        var materializeProgress = exec.createSubProgress(0.34);
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
