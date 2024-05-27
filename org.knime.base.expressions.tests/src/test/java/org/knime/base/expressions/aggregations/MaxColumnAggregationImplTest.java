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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.base.expressions.aggregations.MaxColumnAggregationImpl.maxAggregation;
import static org.knime.base.expressions.aggregations.TestRow.tr;
import static org.knime.core.expressions.AstTestUtils.STR;
import static org.knime.core.expressions.aggregations.ArgumentsBuilder.args;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.expressions.TestUtils;

/**
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class MaxColumnAggregationImplTest {

    private static final DataTableSpec TEST_TABLE_SPEC = new DataTableSpec( //
        new DataColumnSpecCreator("LONG_COL", LongCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("INT_COL", IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("DOUBLE_COL", DoubleCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("STRING_COL", StringCell.TYPE).createSpec() //
    );

    @Test
    void testInt() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 1;
        var agg = maxAggregation(args().p(STR("INT_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, 1));
        agg.addRow(tr(colIdx, -10));
        agg.addRow(tr(colIdx, 10));
        agg.addRow(tr(colIdx, 5));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", 10).accept(result);
    }

    @Test
    void testLong() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 0;
        var agg = maxAggregation(args().p(STR("LONG_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, 1L));
        agg.addRow(tr(colIdx, -10L));
        agg.addRow(tr(colIdx, 10L));
        agg.addRow(tr(colIdx, 5L));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", 10).accept(result);
    }

    @Test
    void testLongMissing() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 0;
        var agg = maxAggregation(args().p(STR("LONG_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, 1L));
        agg.addRow(tr(colIdx));
        agg.addRow(tr(colIdx, 5L));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", 5).accept(result);
    }

    @Test
    void testLongOnlyMissing() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 0;
        var agg = maxAggregation(args().p(STR("LONG_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx));
        agg.addRow(tr(colIdx));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result").accept(result);
    }

    @Test
    void testLongMinValue() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 0;
        var agg = maxAggregation(args().p(STR("LONG_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, Long.MIN_VALUE));
        agg.addRow(tr(colIdx, Long.MIN_VALUE));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", Long.MIN_VALUE).accept(result);
    }

    @Test
    void testLongMaxValue() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 0;
        var agg = maxAggregation(args().p(STR("LONG_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, Long.MAX_VALUE));
        agg.addRow(tr(colIdx, 0));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", Long.MAX_VALUE).accept(result);
    }

    @Test
    void testDouble() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 2;
        var agg = maxAggregation(args().p(STR("DOUBLE_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, 1.0));
        agg.addRow(tr(colIdx, -0.1));
        agg.addRow(tr(colIdx, 2.2));
        agg.addRow(tr(colIdx, 0.1));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", 2.2).accept(result);
    }

    @Test
    void testDoubleMissing() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 2;
        var agg = maxAggregation(args().p(STR("DOUBLE_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx, 1.0));
        agg.addRow(tr(colIdx));
        agg.addRow(tr(colIdx, 5.4));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result", 5.4).accept(result);
    }

    @Test
    void testDoubleOnlyMissing() { // NOSONAR - computerResultChecker has the assertions
        var colIdx = 2;
        var agg = maxAggregation(args().p(STR("DOUBLE_COL")).build(), TEST_TABLE_SPEC);

        agg.addRow(tr(colIdx));
        agg.addRow(tr(colIdx));

        var result = agg.createResultComputer();
        TestUtils.computerResultChecker("aggregation result").accept(result);
    }

    @Test
    void testUnsupportedType() {
        var args = args().p(STR("STRING_COL")).build();
        assertThrows(IllegalArgumentException.class, () -> {
            maxAggregation(args, TEST_TABLE_SPEC);
        }, "unsupported column type: STRING");
    }
}
