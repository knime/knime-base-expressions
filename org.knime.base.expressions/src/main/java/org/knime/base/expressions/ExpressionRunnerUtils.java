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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.expressions.ExpressionMapperFactory.ExpressionMapperContext;
import org.knime.base.expressions.aggregations.ColumnAggregations;
import org.knime.core.data.columnar.ColumnarTableBackend;
import org.knime.core.data.columnar.schema.ColumnarValueSchema;
import org.knime.core.data.columnar.schema.ColumnarValueSchemaUtils;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTableMaterializer;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTables;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.ColumnId;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;

/**
 * Utility methods to work with expressions on Columnar virtual tables.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // ColumnarVirtualTable API is not yet public
public final class ExpressionRunnerUtils {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExpressionRunnerUtils.class);

    private static final String AGGREGATION_RESULT_DATA_KEY = "aggregationResultComputer";

    /**
     * What should happen with new columns, whether they're appended at the end or replace an existing column.
     */
    public enum ColumnInsertionMode {
            /**
             * Append a new column
             */
            APPEND,
            /**
             * Replace an existing column
             */
            REPLACE_EXISTING
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
     * @param expressionResult the result of applying the expression ({@link #applyAndMaterializeExpression})
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
     * Convert no more than {@code maxRowsToConvert} rows to columnar format.
     *
     * @param table
     * @param exec an {@link ExecutionContext} that is used to create a new columnar container if the table is not
     *            columnar
     * @param progress an {@link ExecutionMonitor} that is used to report progress
     * @param maxRowsToConvert the maximum number of rows to convert to columnar format (if it's too big, we just use
     *            the table size instead)
     * @return a {@link ReferenceTable} that can be used for a {@link ColumnarVirtualTable}
     * @throws CanceledExecutionException if the execution was canceled
     */
    public static ReferenceTable createReferenceTable(final BufferedDataTable table, final ExecutionContext exec,
        final ExecutionMonitor progress, final long maxRowsToConvert) throws CanceledExecutionException {
        var uuid = UUID.randomUUID();
        try {
            return ReferenceTables.createReferenceTable(uuid, table);
        } catch (VirtualTableIncompatibleException ex) {

            // Fallback for the row-based backend
            LOGGER.debug("Copying table to columnar format to be compatible with expressions", ex);

            try (var container = new ColumnarTableBackend().create(exec, table.getDataTableSpec(),
                DataContainerSettings.builder() //
                    .withInitializedDomain(true) //
                    .withCheckDuplicateRowKeys(false) //
                    .withDomainUpdate(false).build(), //
                Node.invokeGetDataRepository(exec), Node.invokeGetFileStoreHandler(exec));
                    var writeCursor = container.createCursor();
                    var readCursor = table.cursor()) {

                long rowIndex = 0;
                while (readCursor.canForward() && rowIndex < maxRowsToConvert) {
                    rowIndex++;
                    writeCursor.forward().setFrom(readCursor.forward());

                    progress.setProgress( //
                        rowIndex / (double)table.size(), //
                        "Copying row-based table to columnar format (row %d of %d)".formatted(rowIndex, table.size()) //
                    );

                    progress.checkCanceled();
                }
                return ReferenceTables.createReferenceTable(uuid, container.finish());
            } catch (IOException e) {
                throw new IllegalStateException("Copying row-based table failed.", e);
            } catch (VirtualTableIncompatibleException e) {
                // This cannot happen because we explicitly create a columnar table
                throw new IllegalStateException(e);
            }
        } finally {
            progress.setProgress(1);
        }
    }

    /**
     * Create a {@link ReferenceTable} from the given table. Copies the table to the columnar format if necessary.
     * Converts all table rows.
     *
     * @param table
     * @param exec an {@link ExecutionContext} that is used to create a new columnar container if the table is not
     *            columnar
     * @param progress an {@link ExecutionMonitor} that is used to report progress
     * @return a {@link ReferenceTable} that can be used for a {@link ColumnarVirtualTable}
     * @throws CanceledExecutionException if the execution was canceled
     */
    public static ReferenceTable createReferenceTable(final BufferedDataTable table, final ExecutionContext exec,
        final ExecutionMonitor progress) throws CanceledExecutionException {
        return ExpressionRunnerUtils.createReferenceTable(table, exec, progress, table.size());
    }

    private static List<AggregationCall> collectAggregations(final Ast expression) {
        var acc = new ArrayList<AggregationCall>();
        collectAggregations(expression, acc);
        return acc;
    }

    /** Recursively collect all aggregation calls in the expression */
    private static void collectAggregations(final Ast expression, final List<AggregationCall> acc) {
        for (var child : expression.children()) {
            collectAggregations(child, acc);
        }
        if (expression instanceof AggregationCall agg) {
            acc.add(agg);
        }
    }

    /**
     * Evaluate the aggregations in the given expression on the given table. The result of each aggregation is stored in
     * the {@link AggregationCall} as a {@link Computer}. The computer can be retrieved using the method
     * {@link #getAggregationResultComputer}. Must be called after typing and before {@link #applyExpression}.
     *
     * @param expression the expression
     * @param table the table to evaluate the aggregations on
     * @param progress an execution monitor for progress and cancellation checks
     * @throws CanceledExecutionException if the execution was canceled
     */
    public static void evaluateAggregations(final Ast expression, final BufferedDataTable table,
        final ExecutionMonitor progress) throws CanceledExecutionException {

        evaluateAggregations(expression, table, progress, table.size());
    }

    /**
     * Evaluate the aggregations in the given expression on the given table. The result of each aggregation is stored in
     * the {@link AggregationCall} as a {@link Computer}. The computer can be retrieved using the method
     * {@link #getAggregationResultComputer}. Must be called after typing and before {@link #applyExpression}.
     *
     * @param expression the expression
     * @param table the table to evaluate the aggregations on
     * @param progress an execution monitor for progress and cancellation checks
     * @param numRowsToAggregate the number of rows to aggregate
     * @throws CanceledExecutionException if the execution was canceled
     */
    public static void evaluateAggregations(final Ast expression, final BufferedDataTable table,
        final ExecutionMonitor progress, long numRowsToAggregate) throws CanceledExecutionException {

        numRowsToAggregate = Math.min(table.size(), numRowsToAggregate);

        // Collect the aggregations for the expression
        var aggregationCalls = collectAggregations(expression);

        if (aggregationCalls.isEmpty()) {
            progress.setProgress(1);
            return;
        }

        var aggregationResults = aggregationCalls.stream().collect(Collectors.toMap(a -> a,
            a -> ColumnAggregations.getAggregationImplementationFor(a, table.getDataTableSpec())));

        // Run the aggregations on the table
        var currentRow = 0L;
        try (var cursor = table.cursor()) {
            while (cursor.canForward() && currentRow < numRowsToAggregate) {
                currentRow++;
                var row = cursor.forward();
                aggregationResults.values().forEach(a -> a.addRow(row));
                progress.setProgress( //
                    currentRow / (double)numRowsToAggregate, //
                    "Evaluating aggregations (row %d of %d)".formatted(currentRow, numRowsToAggregate) //
                );
                progress.checkCanceled();
            }
        }
        progress.setProgress(1);

        // Remember the result computer for each aggregation call
        for (var entry : aggregationResults.entrySet()) {
            var aggregationCall = entry.getKey();
            var resultComputer = entry.getValue().createResultComputer();
            aggregationCall.putData(AGGREGATION_RESULT_DATA_KEY, resultComputer);
        }
    }

    /**
     * Get the result computer for the given aggregation call. Must be called after calling
     * {@link #evaluateAggregations}.
     *
     * @param agg the aggregation call
     * @return the computer that returns the result of the aggregation
     */
    public static Computer getAggregationResultComputer(final AggregationCall agg) { // NOSONAR - not applicable to a generic Ast
        return (Computer)agg.data(AGGREGATION_RESULT_DATA_KEY);
    }

    /**
     * Virtually apply the expression to the given input table. The output table will contain the RowIDs of the input
     * table and the expression result.
     *
     * @param input the input table
     * @param numRows number of rows in the input table
     * @param expression the expression. Must have {@link Expressions#inferTypes inferred types}.
     * @param outputColumnName the name of the column that will contain the result of the expression
     * @param exprContext a context for the {@link ExpressionMapperFactory}
     * @param ctx the {@link EvaluationContext}
     * @return the result of the expression
     */
    public static ColumnarVirtualTable applyExpression(final ColumnarVirtualTable input, final long numRows,
        final Ast expression, final String outputColumnName, final ExpressionMapperContext exprContext,
        final EvaluationContext ctx) {

        var resolvedInput = resolveColumns(expression, input, numRows);

        var expressionMapperFactory =
            new ExpressionMapperFactory(expression, resolvedInput.getSchema(), outputColumnName, exprContext, ctx);
        return input.selectColumns(0)
            .append(resolvedInput.map(expressionMapperFactory, expressionMapperFactory.getInputColumnIndices()));
    }

    /**
     * Add column indices to the given {@code expression}. Returns the input {@code ColumnarVirtualTable} with
     * additional columns if required (e.g., ROW_INDEX).
     *
     * @param expression the expression
     * @param input the input table
     * @param numRows number of rows in the input table
     * @return the input {@code ColumnarVirtualTable} with additional columns if required (ROW_INDEX, offset columns,
     *         etc).
     */
    private static ColumnarVirtualTable resolveColumns(final Ast expression, final ColumnarVirtualTable input,
        final long numRows) {

        try {
            final ColumnarValueSchema inputTableSchema = input.getSchema();

            int numCols = inputTableSchema.numColumns();
            ColumnarVirtualTable modifiedInputTable = input;

            // -- append ROW_INDEX if required --
            // ----------------------------------

            final OptionalInt rowIndexColIdx;
            if (Expressions.requiresRowIndexColumn(expression)) {
                rowIndexColIdx = OptionalInt.of(numCols++);
                modifiedInputTable = modifiedInputTable.appendRowIndex("row_idx-" + UUID.randomUUID().toString());
            } else {
                rowIndexColIdx = OptionalInt.empty();
            }

            // -- append (windowing) offset columns if required --
            // ---------------------------------------------------

            // We resolveColumnIndices here to be sure that ExpressionCompileException related to
            // missing columns are thrown.
            //
            // This assigns column indices to ColumnAccess nodes with windowing offset != 0, too.
            // These are wrong and will be fixed below.
            final Function<ColumnId, OptionalInt> columnIdToIndex = columnId -> switch (columnId.type()) {
                case NAMED -> {
                    var colIdx = inputTableSchema.getSourceSpec().findColumnIndex(columnId.name());
                    yield colIdx == -1 ? OptionalInt.empty() : OptionalInt.of(colIdx + 1);
                }
                case ROW_ID -> OptionalInt.of(0);
                case ROW_INDEX -> rowIndexColIdx;
            };
            Expressions.resolveColumnIndices(expression, c -> columnIdToIndex.apply(c.columnId()));

            // Maps each windowing offset to a column index resolution function
            final Map<Long, Function<ColumnId, OptionalInt>> offsetToColumnIdToIndex = new HashMap<>();
            // for offset==0, it's just the "normal" columnIdToIndex from above
            offsetToColumnIdToIndex.put(0L, columnIdToIndex);

            // Maps windowing offset to set of ColumnIds occurring with this offset
            final Map<Long, Set<ColumnId>> offsetToColumnId = Expressions.collectColumnAccesses(expression).stream()
                .collect(groupingBy(ColumnAccess::offset, mapping(ColumnAccess::columnId, toSet())));

            // For each occurring windowing offset ...
            for (var entry : offsetToColumnId.entrySet()) {
                long offset = entry.getKey();
                if (offset == 0) {
                    continue;
                }

                // ColumnIds occurring with offset, and indices of the corresponding non-offset input column
                final ColumnId[] columnIds = entry.getValue().toArray(ColumnId[]::new);
                final int[] columnIndices = new int[columnIds.length];
                Arrays.setAll(columnIndices, i -> columnIdToIndex.apply(columnIds[i]).orElseThrow());

                // TODO (TP) Offsets further than the numRows() in either direction will always be completely missing.
                //           That case should be handled with a simple MissingColumn.
                if (offset > 0) {
                    modifiedInputTable = modifiedInputTable
                        .append(input.selectColumns(columnIndices).renameToRandomColumnNames().slice(offset, numRows));
                } else {
                    ColumnarVirtualTable appendedTable =
                        input.selectColumns(0)
                            .appendMissingValueColumns(
                                ColumnarValueSchemaUtils.selectColumns(input.getSchema(), columnIndices))
                            .dropColumns(0);

                    modifiedInputTable = modifiedInputTable.append( //
                        appendedTable.slice(0, -offset).concatenate( //
                            input.selectColumns(columnIndices).slice(0, numRows + offset) //
                        ).renameToRandomColumnNames() //
                    );
                }

                // Record appended column indices into Map<ColumnId, OptionalInt>
                final Map<ColumnId, OptionalInt> appendedColumnIdToIndex = new HashMap<>();
                for (var columnId : columnIds) {
                    appendedColumnIdToIndex.put(columnId, OptionalInt.of(numCols++));
                }
                offsetToColumnIdToIndex.put(offset,
                    id -> appendedColumnIdToIndex.getOrDefault(id, OptionalInt.empty()));
            }
            // resolveColumnIndices again, this time including offset() handling
            final Function<ColumnAccess, OptionalInt> columnAccessToIndex =
                c -> offsetToColumnIdToIndex.get(c.offset()).apply(c.columnId());
            Expressions.resolveColumnIndices(expression, columnAccessToIndex);

            return modifiedInputTable;
        } catch (ExpressionCompileException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Apply the given expression to the given table and materialize the result. The output table will contain the
     * RowIDs of the input table and the expression result.
     *
     * @param refTable the input table
     * @param expression the expression. Must have {@link Expressions#inferTypes inferred types}.
     * @param outputColumnName the name of the column that will contain the result of the expression
     * @param exec the execution context
     * @param progress an execution monitor for progress and cancellation checks. Could be a subprogress monitor, since
     *            it will go from 0-1 over the course of the execution here.
     * @param exprContext a context for the {@link ExpressionMapperFactory}
     * @param ctx the {@link EvaluationContext}
     * @return the materialized result of the expression
     * @throws CanceledExecutionException if the execution was canceled
     * @throws VirtualTableIncompatibleException if the input table is not compatible with the expression
     */
    public static ReferenceTable applyAndMaterializeExpression( //
        final ReferenceTable refTable, //
        final Ast expression, //
        final String outputColumnName, //
        final ExecutionContext exec, //
        final ExecutionMonitor progress, //
        final ExpressionMapperContext exprContext, //
        final EvaluationContext ctx //
    ) throws CanceledExecutionException, VirtualTableIncompatibleException {
        var numRows = refTable.getBufferedTable().size();
        var expressionResultVirtual =
            applyExpression(refTable.getVirtualTable(), numRows, expression, outputColumnName, exprContext, ctx);
        return ColumnarVirtualTableMaterializer.materializer() //
            .sources(refTable.getSources()) //
            .materializeRowKey(false) //
            .progress( //
                (rowIndex, rowKey) -> progress.setProgress(rowIndex / (double)numRows, //
                    () -> "Evaluating expression (row %d of %s)".formatted(rowIndex + 1, numRows)) //
            ) //
            .executionContext(exec) //
            .tableIdSupplier(Node.invokeGetDataRepository(exec)::generateNewID) //
            .materialize(expressionResultVirtual);
    }
}
