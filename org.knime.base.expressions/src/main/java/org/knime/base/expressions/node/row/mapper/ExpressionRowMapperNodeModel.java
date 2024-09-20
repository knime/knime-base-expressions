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
package org.knime.base.expressions.node.row.mapper;

import static org.knime.base.expressions.ExpressionRunnerUtils.columnToTypesForTypeInference;
import static org.knime.base.expressions.ExpressionRunnerUtils.flowVarToTypeForTypeInference;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.knime.base.expressions.ExpressionMapperFactory;
import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.base.expressions.ExpressionRunnerUtils.NewColumnPosition;
import org.knime.base.expressions.InsertionMode;
import org.knime.base.expressions.node.NodeExpressionMapperContext;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.columnar.table.VirtualTableExtensionTable;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.expressions.Ast;
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
import org.knime.core.table.virtual.expression.Exec;
import org.knime.core.table.virtual.spec.SourceTableProperties.CursorType;

/**
 * The node model for the Expression node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // the columnar table API is not public yet
final class ExpressionRowMapperNodeModel extends NodeModel {

    private final ExpressionRowMapperSettings m_settings;

    public ExpressionRowMapperNodeModel() {
        super(1, 1);

        // TODO(AP-23250) The `null` parameter sets the initial replacement flow variable in case the node
        // replaces a flow variable. In the dialog the scripting service is set up where the initial replacement
        // column/flow variable is correctly set but the frontend gets this setting here.

        m_settings = new ExpressionRowMapperSettings(null);
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

    /**
     * Takes a single script, some settings, and an input table specification, and returns what the new specification
     * would look like if the script were to be applied to the input table.
     *
     * @param inputSpec the input table specification
     * @param outputMode whether to replace an existing column or add a new one
     * @param outputColumn the name of the column to be created/replaced
     * @param expression the expression to be applied
     * @param indexInScripts the index of the script in the list of scripts. Useful for error messages
     * @return the table specification after the script has been applied
     * @throws InvalidSettingsException if anything is wrong with the script or the settings
     */
    private DataTableSpec computeTableSpecAfterScriptApplied(final DataTableSpec inputSpec,
        final InsertionMode outputMode, final String outputColumn, final String expression, final int indexInScripts)
        throws InvalidSettingsException {
        var availableFlowVariables =
            getAvailableInputFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES);

        try {
            var ast = getPreparedExpression(expression, inputSpec, availableFlowVariables);
            var outputType = Expressions.getInferredType(ast);
            if (ValueType.MISSING.equals(outputType)) {
                throw new InvalidSettingsException(
                    "Expression %d evaluates to MISSING. Enter an expression that has an output type."
                        .formatted(indexInScripts + 1));
            }
            var outputDataSpec = Exec.valueTypeToDataSpec(outputType);
            var outputColumnSpec =
                ExpressionMapperFactory.primitiveDataSpecToDataColumnSpec(outputDataSpec.spec(), outputColumn);

            if (outputMode == InsertionMode.REPLACE_EXISTING) {
                var columnIndex = inputSpec.findColumnIndex(outputColumn);
                if (columnIndex == -1) {
                    throw new InvalidSettingsException("The output column '" + outputColumn + "' of Expression "
                        + (indexInScripts + 1) + " does not exist in the input table. "
                        + "Choose an existing column or choose to append a column.");
                }
                return new DataTableSpecCreator(inputSpec) //
                    .replaceColumn(columnIndex, outputColumnSpec) //
                    .createSpec(); //
            } else if (!inputSpec.containsName(outputColumn)) {
                return new DataTableSpecCreator(inputSpec).addColumns(outputColumnSpec) //
                    .createSpec();
            } else {
                throw new InvalidSettingsException(
                    "The output column '%s' of Expression %d exists in the input table. ".formatted(outputColumn,
                        indexInScripts + 1) + "Choose another column name or choose replacing the existing column.");
            }
        } catch (final ExpressionCompileException e) {
            throw new InvalidSettingsException(
                "Error in Expression %d: %s".formatted(indexInScripts + 1, e.getMessage()), e);
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getNumScripts() == 1
            && ExpressionRowMapperSettings.DEFAULT_SCRIPT.equals(m_settings.getScripts().get(0))) {
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

    static List<NewColumnPosition> getColumnPositions(final List<InsertionMode> insertionModes,
        final List<String> activeOutputColumns) {
        return IntStream.range(0, insertionModes.size()) //
            .mapToObj(
                i -> new ExpressionRunnerUtils.NewColumnPosition(insertionModes.get(i), activeOutputColumns.get(i))) //
            .toList();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        var messageBuilder = createMessageBuilder();

        var outputTable = applyMapperExpressions(m_settings.getScripts(), //
            getColumnPositions(m_settings.getColumnInsertionModes(), m_settings.getActiveOutputColumns()), //
            inData[0], //
            getAvailableFlowVariables(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES), //
            exec, //
            (i, warningMessage) -> messageBuilder.addTextIssue("Expression " + (i + 1) + ": " + warningMessage) //
        );

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

        return new BufferedDataTable[]{outputTable};
    }

    /**
     * Applies the given expressions to the table.
     *
     * @param expressions the expressions to apply
     * @param newColumnPositions the positions of the new columns
     * @param inputTable the input table
     * @param availableFlowVariables the available flow variables
     * @param exec the execution context
     * @param setWarning a consumer that is called if the evaluation of an expression produces a warning with the index
     *            of the expression and the warning message
     * @return the output table
     *
     * @throws ExpressionCompileException if an expression cannot be compiled
     * @throws CanceledExecutionException if the execution is canceled
     * @throws VirtualTableIncompatibleException
     */
    static BufferedDataTable applyMapperExpressions( //
        final List<String> expressions, //
        final List<NewColumnPosition> newColumnPositions, //
        final BufferedDataTable inputTable, //
        final Map<String, FlowVariable> availableFlowVariables, //
        final ExecutionContext exec, //
        final BiConsumer<Integer, String> setWarning //
    ) throws ExpressionCompileException, CanceledExecutionException, VirtualTableIncompatibleException {
        var exprContext = new NodeExpressionMapperContext(availableFlowVariables);
        var numberOfExpressions = expressions.size();
        var nextInputTable = inputTable;

        for (int i = 0; i < numberOfExpressions; ++i) {
            var subExec = exec.createSubExecutionContext(1.0 / numberOfExpressions);

            var newColumnPosition = newColumnPositions.get(i);

            // Parse the expression and infer the types
            var expression =
                getPreparedExpression(expressions.get(i), nextInputTable.getDataTableSpec(), availableFlowVariables);

            // Create a reference table for the input table
            var inRefTable =
                ExpressionRunnerUtils.createReferenceTable(nextInputTable, subExec.createSubExecutionContext(0.33));

            // Pre-evaluate the aggregations
            // NB: We use the inRefTable because it is guaranteed to be a columnar table
            ExpressionRunnerUtils.evaluateAggregations(expression, inRefTable.getBufferedTable(),
                subExec.createSubProgress(0.33));

            // Evaluate the expression and materialize the result
            final var finalI = i;
            EvaluationContext ctx = warning -> setWarning.accept(finalI, warning);
            var expressionResult = ExpressionRunnerUtils.applyAndMaterializeExpression(inRefTable, expression,
                newColumnPosition.columnName(), exec, subExec.createSubProgress(0.34), exprContext, ctx);

            // We must avoid using inRefTable.getVirtualTable() directly. Doing so would result in building upon the
            // transformation of the input table, instead of initiating a new fragment. This leads to complications
            // when loading the virtual table, as it attempts to resolve the sources of the input table. By creating a
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
                    nextInputTable.size(), Node.invokeGetDataRepository(exec).generateNewID());

            nextInputTable = outputExtensionTable.create(exec);
        }
        return nextInputTable;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ExpressionRowMapperSettings(null).validate(settings);
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
