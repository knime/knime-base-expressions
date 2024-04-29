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
 *   May 6, 2024 (benjamin): created
 */
package org.knime.base.expressions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.knime.base.expressions.ExpressionMapperFactory.ExpressionEvaluationContext;
import org.knime.core.data.columnar.ColumnarTableBackend;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTables;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.expressions.WarningMessageListener;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;

/**
 * Utility methods to work with expressions on Columnar virtual tables.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // Expressions API is not yet public
public final class ExpressionRunnerUtils {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExpressionRunnerUtils.class);

    /**
     * What should happen with new columns, whether they're appended at the end or replace an existing column.
     */
    public enum ColumnInsertionMode {
            APPEND, REPLACE_EXISTING
    }

    /**
     * Specifies where and with which name a new column should be added to a table
     *
     * @param mode The {@link ColumnInsertionMode}
     * @param columnName The name of the new column (also of the column to replace if that mode is selected)
     */
    public record NewColumnPosition(ColumnInsertionMode mode, String columnName) {
    }

    private ExpressionRunnerUtils() {
    }

    /**
     * Construct an output table containing the expression result based on the new column position settings
     *
     * @param inputTable the input of the node
     * @param expressionResult the result of applying the expression
     *            ({@link #applyAndMaterializeExpression(ReferenceTable, String, String, ExecutionContext)})
     * @param columnInsertionMode settings of how the expression column should be inserted in the output table
     * @return the outputTable
     */
    public static ColumnarVirtualTable constructOutputTable(final ColumnarVirtualTable inputTable,
        final ColumnarVirtualTable expressionResult, final NewColumnPosition columnInsertionMode) {
        if (columnInsertionMode.mode() == ColumnInsertionMode.APPEND) {
            return inputTable.append(expressionResult);
        } else {
            var inputSpec = inputTable.getSchema().getSourceSpec();
            var matchingColumnIndices = inputSpec.columnsToIndices(columnInsertionMode.columnName());
            if (matchingColumnIndices.length == 0) {
                throw new IllegalStateException("Cannot replace column with name '" + columnInsertionMode.columnName()
                    + "', no such column available.");
            }
            int replacedColIdx = matchingColumnIndices[0];
            // +1 for RowID
            List<Integer> allColumnIndices = rangeList(inputSpec.getNumColumns() + 1);

            // keep all but replaced, append new column at the end
            List<Integer> columnIndicesWithoutOldColumn = rangeList(allColumnIndices.size());
            columnIndicesWithoutOldColumn.remove(replacedColIdx + 1); // +1 to skip RowID
            var tableWithAppendedResult =
                inputTable.selectColumns(columnIndicesWithoutOldColumn.stream().mapToInt(i -> i).toArray())
                    .append(expressionResult);

            // move appended (=last) column to the position of the column to replace
            // init with column indices of input (without the removed column)
            List<Integer> columnIndicesWithNewColumnAtPositionOfOld = rangeList(allColumnIndices.size() - 1);
            // +1 to skip RowID
            columnIndicesWithNewColumnAtPositionOfOld.add(replacedColIdx + 1, columnIndicesWithoutOldColumn.size());
            return tableWithAppendedResult
                .selectColumns(columnIndicesWithNewColumnAtPositionOfOld.stream().mapToInt(i -> i).toArray());
        }
    }

    private static List<Integer> rangeList(final int endExclusive) {
        return new ArrayList<>(IntStream.range(0, endExclusive).boxed().toList());
    }

    /**
     * Create a {@link ReferenceTable} from the given table. Copies the table to the columnar format if necessary.
     *
     * @param table
     * @param exec an {@link ExecutionContext} that is used to create a new columnar container if the table is not
     *            columnar
     * @return a {@link ReferenceTable} that can be used for a {@link ColumnarVirtualTable}
     */
    public static ReferenceTable createReferenceTable(final BufferedDataTable table, final ExecutionContext exec) {
        var uuid = UUID.randomUUID();
        try {
            return ReferenceTables.createReferenceTable(uuid, table);
        } catch (VirtualTableIncompatibleException ex) {
            // Fallback for the row-based backend
            LOGGER.debug("Copying table to columnar format to be compatible with expressions", ex);
            try (var container =
                new ColumnarTableBackend().create(exec, table.getDataTableSpec(), DataContainerSettings.getDefault(),
                    Node.invokeGetDataRepository(exec), Node.invokeGetFileStoreHandler(exec));
                    var writeCursor = container.createCursor();
                    var readCursor = table.cursor()) {
                while (readCursor.canForward()) {
                    writeCursor.forward().setFrom(readCursor.forward());
                }
                return ReferenceTables.createReferenceTable(uuid, container.finish());
            } catch (IOException e) {
                throw new IllegalStateException("Copying row-based table failed.", e);
            } catch (VirtualTableIncompatibleException e) {
                // This cannot happen because we explicitly create a columnar table
                throw new IllegalStateException(e);
            }

        }
    }

    /**
     * Virtually apply the expression to the given input table. The output table will contain the RowIDs of the input
     * table and the expression result.
     *
     * @param exprContext
     */
    public static ColumnarVirtualTable applyExpression(final ColumnarVirtualTable input, final String expression,
        final String outputColumnName, final ExpressionEvaluationContext exprContext,
        final WarningMessageListener wml) {
        var expressionMapperFactory =
            new ExpressionMapperFactory(expression, input.getSchema(), outputColumnName, exprContext, wml);
        return input.selectColumns(0)
            .append(input.map(expressionMapperFactory, expressionMapperFactory.getInputColumnIndices()));
    }

    public static ReferenceTable applyAndMaterializeExpression( //
        final ReferenceTable refTable, //
        final String expression, //
        final String outputColumnName, //
        final ExecutionContext exec, //
        final ExpressionEvaluationContext exprContext, //
        final WarningMessageListener wml //
    ) throws CanceledExecutionException, VirtualTableIncompatibleException {
        var numRows = refTable.getBufferedTable().size();
        var expressionResultVirtual =
            applyExpression(refTable.getVirtualTable(), expression, outputColumnName, exprContext, wml);
        return ColumnarVirtualTableMaterializer.materializer() //
            .sources(refTable.getSources()) //
            .materializeRowKey(false) //
            .progress((rowIndex, rowKey) -> exec.setProgress(rowIndex / (double)numRows,
                () -> "Evaluating expression on row " + (rowIndex + 1) + " of " + numRows)) //
            .executionContext(exec) //
            .tableIdSupplier(Node.invokeGetDataRepository(exec)::generateNewID) //
            .materialize(expressionResultVirtual);
    }
}
