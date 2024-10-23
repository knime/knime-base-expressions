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
 *   Oct 23, 2024 (benjamin): created
 */
package org.knime.base.expressions.node.row;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettingsSerializer;
import org.knime.core.webui.node.dialog.defaultdialog.setting.selection.SelectionMode;
import org.knime.core.webui.node.view.table.RowHeightPersistorUtil;
import org.knime.core.webui.node.view.table.TableViewUtil;
import org.knime.core.webui.node.view.table.TableViewViewSettings;
import org.knime.core.webui.node.view.table.TableViewViewSettings.RowHeightMode;
import org.knime.core.webui.node.view.table.TableViewViewSettings.VerticalPaddingMode;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.node.view.table.data.TableViewInitialDataImpl;

/**
 * Provides a preview of the output table that can be shown in the node dialog.
 * <P>
 * Add the following services to the {@link RpcDataService}:
 *
 * <pre>
 * <code>
 * var tablePreview = new OutputTablePreview();
 * RpcDataService.builder() //
 *     // [...]
 *     .addService(OutputTablePreview.INITIAL_DATA_SERVICE_NAME, tablePreview) //
 *     .addService(OutputTablePreview.DATA_SERVICE_NAME, tablePreview.getTableViewDataService()) //
 *     // [...]
 * </code>
 * </pre>
 *
 * Update the preview table by calling {@link #updateTables}. Release resources by calling {@link #clearTables} when the
 * dialog is closed.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // TableViewDataService is not public API
public class OutputTablePreview {

    /** The name of the initial data service. The service is provided by objects of {@link OutputTablePreview}. */
    public static final String INITIAL_DATA_SERVICE_NAME = "OutputPreviewTableInitialDataRpcSupplier";

    /** The name of the data service. Use {@link OutputTablePreview#getTableViewDataService()} to get the service. */
    public static final String DATA_SERVICE_NAME = TableViewDataService.class.getSimpleName();

    private static final String DUMMY_TABLE_ID = "previewTable.dummyId";

    /**
     * The last table is show. The other tables can be temporary tables that are required to display the last table. All
     * tables are cleared when the preview is updated.
     */
    private List<BufferedDataTable> m_tables = List.of();

    private TableViewDataService m_tableViewDataService;

    /** Creates a new output table preview. */
    public OutputTablePreview() {
        m_tableViewDataService = TableViewUtil.createTableViewDataService(this::getTable, null, DUMMY_TABLE_ID);
    }

    /**
     * Called by the frontend client.
     *
     * @return the initial data for the table view.
     */
    public String getInitialData() {
        if (m_tables.isEmpty()) {
            // This can happen if no input table is connected to the node
            return null;
        }
        var table = m_tables.get(m_tables.size() - 1);

        var tab = new TableViewViewSettings(table.getSpec());
        tab.m_title = null;
        tab.m_enableGlobalSearch = false;
        tab.m_showTableSize = false;
        tab.m_enablePagination = false;
        tab.m_rowHeightMode = RowHeightMode.CUSTOM;
        tab.m_verticalPaddingMode = VerticalPaddingMode.COMPACT;
        tab.m_customRowHeight = RowHeightPersistorUtil.LEGACY_CUSTOM_ROW_HEIGHT_COMPACT;
        tab.m_selectionMode = SelectionMode.OFF;
        tab.m_showOnlySelectedRowsConfigurable = false;
        tab.m_enableColumnSearch = false;
        try {
            return new DefaultNodeSettingsSerializer<>()
                .serialize(Map.of("result", new TableViewInitialDataImpl(tab, this::getTable, m_tableViewDataService)));
        } catch (IOException e) {
            NodeLogger.getLogger(this.getClass()).error("Failed to serialize the initial data", e);
            return null;
        }
    }

    /**
     * @return the data service for the table
     */
    public TableViewDataService getTableViewDataService() {
        return m_tableViewDataService;
    }

    /**
     * @return the number of rows in the shown table
     */
    public long numRows() {
        return m_tables.isEmpty() ? 0 : m_tables.get(m_tables.size() - 1).size();
    }

    /**
     * Updates the table that are shown in the preview and clear the previously shown tables.
     *
     * @param tables a list of tables. The last table is shown in the preview. The others are temporary tables that are
     *            required only to display the last table. All tables are cleared when the preview is updated.
     * @param exec the execution context associated with the tables. Used to clear the tables.
     */
    public void updateTables(final List<BufferedDataTable> tables, final ExecutionContext exec) {
        m_tables.forEach(exec::clearTable);
        m_tables = tables;
        // Clean the cache such that the preview will fetch the new table
        cleanUpTableDataService();
    }

    /**
     * Clears the tables that are shown in the preview to free memory.
     *
     * @param exec the execution context associated with the tables.
     */
    public void clearTables(final ExecutionContext exec) {
        updateTables(List.of(), exec);
    }

    private BufferedDataTable getTable() {
        return m_tables.isEmpty() ? null : m_tables.get(m_tables.size() - 1);
    }

    private void cleanUpTableDataService() {
        TableViewUtil.deactivateTableViewDataService(m_tableViewDataService, DUMMY_TABLE_ID);
    }
}
