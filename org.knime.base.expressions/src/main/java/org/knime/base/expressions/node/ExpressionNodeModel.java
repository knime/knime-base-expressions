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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.columnar.ColumnarTableBackend;
import org.knime.core.data.columnar.table.VirtualTableExtensionTable;
import org.knime.core.data.columnar.table.VirtualTableIncompatibleException;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable;
import org.knime.core.data.columnar.table.virtual.ExpressionMapperFactory;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTable;
import org.knime.core.data.columnar.table.virtual.reference.ReferenceTables;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.v2.ValueFactoryUtils;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.table.virtual.expression.AstType;
import org.knime.core.table.virtual.expression.Typing;

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

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        String input = m_settings.getScript();

        // We use a NotInWorkflowWriteFileStoreHandler here because we only want to deduce the type,
        // we'll never write any data in configure.
        var fsHandler = new NotInWorkflowWriteFileStoreHandler(UUID.randomUUID());
        final IntFunction<AstType> columnIndexToAstType = i -> ValueFactoryUtils
            .getValueFactory(inSpecs[0].getColumnSpec(i).getType(), fsHandler).getSpec().accept(Typing.toAstType);

        try {
            var ast = ExpressionMapperFactory.parseExpression(input, columnIndexToAstType);
            var outputType = ast.inferredType();
            var outputDataSpec = Typing.toDataSpec(outputType);
            var outputColumnSpec =
                ExpressionMapperFactory.primitiveDataSpecToDataColumnSpec(outputDataSpec.spec(), "Expression Result");

            return new DataTableSpec[]{new DataTableSpecCreator(inSpecs[0]).addColumns(outputColumnSpec).createSpec()};
        } catch (Exception e) {
            throw new InvalidSettingsException("Cannot parse expression", e);
        }
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        var dataRepo = NodeContext.getContext().getWorkflowManager().getWorkflowDataRepository();

        try (var expressionResultTable = applyExpression(inData[0], m_settings.getScript(),
            new NewColumnPosition(ColumnInsertionMode.APPEND, "Expression Result"), dataRepo::generateNewID)) {
            return new BufferedDataTable[]{expressionResultTable.create(exec)};
        }
    }

    /**
     * What should happen with new columns, whether they're appended at the end or replace an existing column.
     *
     * @since 5.3
     */
    enum ColumnInsertionMode {
            APPEND, REPLACE_EXISTING
    }

    /**
     * Specifies where and with which name a new column should be added to a table
     *
     * @param mode The {@link ColumnInsertionMode}
     * @param columnName The name of the new column (also of the column to replace if that mode is selected)
     *
     * @since 5.3
     */
    record NewColumnPosition(ColumnInsertionMode mode, String columnName) {
    }

    /**
     * TODO: move this function to {@link ColumnarTableBackend} and make it public API by adding it in the
     * InternalTableAPI and ExecutionContext.
     *
     * Creates a table by executing the provided expression on the given table, adding or replacing a new column with
     * the expression result.
     *
     * WARNING: this only works with the columnar backend at the moment!
     *
     * @param table The table to apply the expression on
     * @param expression The expression to evaluate
     * @param columnPosition Where to put the column with the expression results
     * @param tableIDSupplier provides IDs for created ContainerTables
     * @return The table with a newly computed column added at the specified position
     * @since 5.3
     * @noreference This method is not intended to be referenced by clients.
     */
    static VirtualTableExtensionTable applyExpression(final BufferedDataTable table, final String expression,
        final NewColumnPosition columnPosition, final IntSupplier tableIDSupplier) {
        ReferenceTable refTable;
        try {
            refTable = ReferenceTables.createReferenceTable(UUID.randomUUID(), table);
        } catch (VirtualTableIncompatibleException ex) {
            throw new IllegalStateException(
                "The provided table cannot be used as reference table. Please use the columnar backend.", ex);
        }
        var inColViTa = refTable.getVirtualTable();

        ColumnarVirtualTable outColViTa = inColViTa.map(expression, columnPosition.columnName());
        if (columnPosition.mode() == ColumnInsertionMode.APPEND) {
            outColViTa = inColViTa.append(outColViTa);
        } else {
            // TODO: Should we add a "replace" method to "ColumnarVirtualTable"?
            // FIXME: untested
            var matchingColumnIndices = table.getDataTableSpec().columnsToIndices(columnPosition.columnName());
            if (matchingColumnIndices.length == 0) {
                throw new IllegalStateException(
                    "Cannot replace column with name '" + columnPosition.columnName() + "', no such column available.");
            }
            int replacedColIdx = matchingColumnIndices[0];
            // +1 for RowID
            List<Integer> allColumnIndices =
                IntStream.range(0, table.getDataTableSpec().getNumColumns() + 1).boxed().collect(Collectors.toList());

            // keep all but replaced, append new column at the end
            List<Integer> columnIndicesWithoutOldColumn = new ArrayList<>(allColumnIndices);
            columnIndicesWithoutOldColumn.remove(replacedColIdx + 1); // +1 to skip RowID

            outColViTa = inColViTa.selectColumns(columnIndicesWithoutOldColumn.stream().mapToInt(i -> i).toArray())
                .append(outColViTa);

            // move appended (=last) column to the position of the column to replace
            // +1 to skip RowID
            List<Integer> columnIndicesWithNewColumnAtPositionOfOld = new ArrayList<>(allColumnIndices);
            columnIndicesWithNewColumnAtPositionOfOld.add(replacedColIdx + 1, allColumnIndices.size());

            outColViTa =
                outColViTa.selectColumns(columnIndicesWithNewColumnAtPositionOfOld.stream().mapToInt(i -> i).toArray());
        }

        return new VirtualTableExtensionTable(new ReferenceTable[]{refTable}, outColViTa, table.size(),
            tableIDSupplier.getAsInt());
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
}
