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
 *   May 24, 2024 (benjamin): created
 */
package org.knime.base.expressions.aggregations;

import org.knime.base.expressions.aggregations.ColumnAggregations.Aggregation;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.OperatorDescription.Argument;
import org.knime.core.expressions.aggregations.BuiltInAggregations;

/**
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class MaxColumnAggregationImpl {

    private MaxColumnAggregationImpl() {
    }

    static Aggregation maxAggregation(final Arguments<ConstantAst> arguments, final DataTableSpec tableSpec) {
        // NOTE: The exception should never happen, because the return type check
        var columnName = Argument.matchSignature(BuiltInAggregations.MAX.description().arguments(), arguments) //
            .filter(args -> args.size() == 1) // must be only 1 argument
            .map(args -> args.get("column")) // type of the column argument
            .map(arg -> arg instanceof Ast.StringConstant colName ? colName.value() : null) // get the column name
            .orElseThrow(() -> new IllegalArgumentException("Invalid arguments: " + arguments));

        var columnIdx = tableSpec.findColumnIndex(columnName);
        var columnType = tableSpec.getColumnSpec(columnIdx).getType();

        if (columnType.isCompatible(LongValue.class)) {
            return new MaxIntegerAggregation(columnIdx);
        } else if (columnType.isCompatible(DoubleValue.class)) {
            return new MaxFloatAggregation(columnIdx);
        } else {
            // NOTE: This should never happen, because the return type check
            throw new IllegalArgumentException("Unsupported column type: " + columnType);
        }
    }

    private static final class MaxFloatAggregation extends AbstractAggregation {

        private double m_max = Double.MIN_VALUE;

        private MaxFloatAggregation(final int columnIdx) {
            super(columnIdx);
        }

        @Override
        protected void addNonMissingRow(final RowRead row) {
            var value = ((DoubleValue)row.getValue(m_columnIdx)).getDoubleValue();
            if (value > m_max) {
                m_max = value;
            }
        }

        @Override
        public Computer createResultComputer() {
            return Computer.FloatComputer.of(ctx -> m_max, ctx -> m_isMissing);
        }
    }

    private static final class MaxIntegerAggregation extends AbstractAggregation {

        private long m_max = Long.MIN_VALUE;

        private MaxIntegerAggregation(final int columnIdx) {
            super(columnIdx);
        }

        @Override
        protected void addNonMissingRow(final RowRead row) {
            var value = ((LongValue)row.getValue(m_columnIdx)).getLongValue();
            if (value > m_max) {
                m_max = value;
            }
        }

        @Override
        public Computer createResultComputer() {
            return Computer.IntegerComputer.of(ctx -> m_max, ctx -> m_isMissing);
        }
    }
}
