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
 *   May 27, 2024 (benjamin): created
 */
package org.knime.base.expressions.aggregations;

import static org.junit.jupiter.api.Assertions.fail;

import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;

/**
 * Minimal implementation of {@link RowRead} for testing purposes.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class TestRow implements RowRead {

    private final int m_columnIndex;

    private final boolean m_isMissing;

    private final DataValue m_value;

    /**
     * Create a test row with a missing value.
     *
     * @param columnIndex
     * @return the row
     */
    public static TestRow tr(final int columnIndex) {
        return new TestRow(columnIndex, null, true);
    }

    /**
     * Create a test row with the given value.
     *
     * @param columnIndex
     * @param value
     * @return the row
     */
    public static TestRow tr(final int columnIndex, final DataValue value) {
        return new TestRow(columnIndex, value, false);
    }

    /**
     * Create a test row with the given long value.
     *
     * @param columnIndex
     * @param value
     * @return the row
     */
    public static TestRow tr(final int columnIndex, final long value) {
        return new TestRow(columnIndex, new LongCell(value), false);
    }

    /**
     * Create a test row with the given long value.
     *
     * @param columnIndex
     * @param value
     * @return the row
     */
    public static TestRow tr(final int columnIndex, final double value) {
        return new TestRow(columnIndex, new DoubleCell(value), false);
    }

    /**
     * Create a test row with the given int value.
     *
     * @param columnIndex
     * @param value
     * @return the row
     */
    public static TestRow tr(final int columnIndex, final int value) {
        return new TestRow(columnIndex, new IntCell(value), false);
    }

    /**
     * Create a test row with the given String value.
     *
     * @param columnIndex
     * @param value
     * @return the row
     */
    public static TestRow tr(final int columnIndex, final String value) {
        return new TestRow(columnIndex, new StringCell(value), false);
    }

    /**
     * Create a test row with the given boolean value.
     *
     * @param columnIndex
     * @param value
     * @return the row
     */
    public static TestRow tr(final int columnIndex, final boolean value) {
        return new TestRow(columnIndex, value ? BooleanCell.TRUE : BooleanCell.FALSE, false);
    }

    private TestRow(final int columnIndex, final DataValue value, final boolean isMissing) {
        m_columnIndex = columnIndex;
        m_value = value;
        m_isMissing = isMissing;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends DataValue> D getValue(final int index) {
        checkIndex(index);
        return (D)m_value;
    }

    @Override
    public boolean isMissing(final int index) {
        checkIndex(index);
        return m_isMissing;
    }

    private void checkIndex(final int index) {
        if (m_columnIndex != index) {
            fail("Accesing column at index " + index + " but only column " + m_columnIndex + " is available");
        }
    }

    @Override
    public int getNumColumns() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public RowKeyValue getRowKey() {
        throw new IllegalStateException("Not implemented");
    }
}
