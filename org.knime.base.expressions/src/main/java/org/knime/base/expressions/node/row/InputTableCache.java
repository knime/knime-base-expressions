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
 *   Sep 11, 2024 (benjamin): created
 */
package org.knime.base.expressions.node.row;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;

/**
 * Cache for input tables that are used in expression dialogs. The cache stores a full table and creates a copy of the
 * table with a given number of rows on demand.
 *
 * Note that this cache should not be used for many different tables with different row counts, as it will keep all of
 * them in memory.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class InputTableCache implements AutoCloseable {

    private final BufferedDataTable m_fullTable;

    // TODO(AP-23302) instead of holding one table per row count, we could also hold a single table slice it on demand
    // using a virtual operation
    private final Map<Long, BufferedDataTable> m_cachedTables = new HashMap<>();

    private ExecutionContext m_exec;

    private IDataRepository m_dataRepository;

    private IWriteFileStoreHandler m_fileStoreHandler;

    /**
     * Creates a new input table cache for the given full table.
     *
     * @param fullTable the full table to cache
     * @param exec the execution context to create new cached tables with
     */
    public InputTableCache(final BufferedDataTable fullTable, final ExecutionContext exec) {
        m_fullTable = fullTable;
        m_exec = exec;
        m_dataRepository = Node.invokeGetDataRepository(m_exec);
        m_fileStoreHandler = new NotInWorkflowWriteFileStoreHandler(UUID.randomUUID(), m_dataRepository);
    }

    /** @return the number of rows in the full table */
    public long getFullRowCount() {
        return m_fullTable.size();
    }

    /**
     * Returns a table with the given number of rows. If the table with the given number of rows is not already cached,
     * a new table is created by copying the full table.
     *
     * @param numRows the number of rows of the table to return
     * @return a table with the given number of rows
     */
    public synchronized BufferedDataTable getTable(final long numRows) {
        if (m_cachedTables.containsKey(numRows)) {
            return m_cachedTables.get(numRows);
        } else {
            try {
                var table = ExpressionRunnerUtils.copyToColumnarTable(m_fullTable, numRows, m_exec, m_dataRepository,
                    m_fileStoreHandler);
                m_cachedTables.put(numRows, table);
                return table;
            } catch (CanceledExecutionException ex) {
                throw new IllegalStateException("Input table preparation for expression cancelled by the user", ex);
            }
        }
    }

    @Override
    public void close() {
        // Clear the temporary tables
        for (var table : m_cachedTables.values()) {
            m_exec.clearTable(table);
        }
        m_cachedTables.clear();

        // Clear the file stores
        m_fileStoreHandler.clearAndDispose();
    }
}
