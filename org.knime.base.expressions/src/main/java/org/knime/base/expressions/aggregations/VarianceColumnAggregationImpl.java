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

import static org.knime.base.expressions.aggregations.ColumnAggregations.missingWithWarning;

import org.knime.base.expressions.aggregations.ColumnAggregations.Aggregation;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.FloatComputer;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class VarianceColumnAggregationImpl {

    private static final boolean IGNORE_NAN_DEFAULT = false;

    private VarianceColumnAggregationImpl() {
    }

    static Aggregation varianceAggregation(final Arguments<ConstantAst> arguments, final DataTableSpec tableSpec) {
        var columnIdx = ConstantArgumentResolver.resolveColumnIndex(arguments, tableSpec);

        var ignoreNaN = ConstantArgumentResolver.resolveOptionalBoolean(arguments, "ignore_nan", IGNORE_NAN_DEFAULT);

        var columnType = tableSpec.getColumnSpec(columnIdx).getType();

        if (columnType.isCompatible(DoubleValue.class)) {
            return new VarianceFloatAggregation(columnIdx, ignoreNaN);
        } else {
            throw new IllegalStateException("Implementation error - unsupported column type: %s".formatted(columnType));
        }
    }

    @SuppressWarnings("squid:S3052") // Allow redundant initializations for clarity
    private static final class VarianceFloatAggregation extends AbstractAggregation {

        private double m_runningMean = 0;

        private double m_runningMeanSq = 0;

        private long m_count = 0;

        private final boolean m_ignoreNaN;

        private boolean m_anyValuesNaN = false;

        private boolean m_allValuesNaN = true;

        private VarianceFloatAggregation(final int columnIdx, final boolean ignoreNaN) {
            super(columnIdx);

            m_ignoreNaN = ignoreNaN;
        }

        @Override
        protected void addNonMissingRow(final RowRead row) {
            var value = ((DoubleValue)row.getValue(m_columnIdx)).getDoubleValue();

            m_anyValuesNaN = m_anyValuesNaN || Double.isNaN(value);
            m_allValuesNaN = m_allValuesNaN && Double.isNaN(value);

            if (m_ignoreNaN && Double.isNaN(value)) {
                return;
            }

            m_runningMean = m_runningMean * (m_count / (m_count + 1.0)) + (value / (m_count + 1.0));
            m_runningMeanSq = m_runningMeanSq * (m_count / (m_count + 1.0)) + value * (value / (m_count + 1.0));
            m_count++;
        }

        @Override
        public Computer createResultComputer() {
            boolean shouldWarn = (m_ignoreNaN && m_allValuesNaN) || m_isMissing;
            if (shouldWarn) {
                return FloatComputer.of(ctx -> Double.NaN, missingWithWarning(
                    "COLUMN_VARIANCE returned MISSING because all values were either MISSING or NaN."));
            }

            if (!m_ignoreNaN && m_anyValuesNaN) {
                return Computer.FloatComputer.of(ctx -> Double.NaN, ctx -> m_isMissing);
            } else {
                var variance = m_runningMeanSq - m_runningMean * m_runningMean;
                return Computer.FloatComputer.of(ctx -> variance, ctx -> m_isMissing || m_allValuesNaN);
            }
        }
    }
}
