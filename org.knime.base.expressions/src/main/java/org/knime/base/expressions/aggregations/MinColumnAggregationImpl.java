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
import org.knime.core.data.LongValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class MinColumnAggregationImpl {

    private static final boolean IGNORE_NAN_DEFAULT = false;

    private MinColumnAggregationImpl() {
    }

    static Aggregation minAggregation(final Arguments<ConstantAst> arguments, final DataTableSpec tableSpec) {
        var columnIdx = ConstantArgumentResolver.resolveColumnIndex(arguments, tableSpec);

        var ignoreNaN = ConstantArgumentResolver.resolveOptionalBoolean(arguments, "ignore_nan", IGNORE_NAN_DEFAULT);

        var columnType = tableSpec.getColumnSpec(columnIdx).getType();

        if (columnType.isCompatible(LongValue.class)) {
            return new MinIntegerAggregation(columnIdx);
        } else if (columnType.isCompatible(DoubleValue.class)) {
            return new MinFloatAggregation(columnIdx, ignoreNaN);
        } else {
            throw new IllegalStateException("Implementation error - unsupported column type: %s".formatted(columnType));
        }
    }

    @SuppressWarnings("squid:S3052") // Allow redundant initialisations for clarity
    private static final class MinFloatAggregation extends AbstractAggregation {

        private final boolean m_ignoreNaN;

        private double m_min = Double.POSITIVE_INFINITY;

        private boolean m_anyValuesNaN = false;

        private boolean m_allValuesNaN = true;

        private MinFloatAggregation(final int columnIdx, final boolean ignoreNaN) {
            super(columnIdx);

            this.m_ignoreNaN = ignoreNaN;
        }

        @Override
        protected void addNonMissingRow(final RowRead row) {
            var value = ((DoubleValue)row.getValue(m_columnIdx)).getDoubleValue();

            m_anyValuesNaN = m_anyValuesNaN || Double.isNaN(value);
            m_allValuesNaN = m_allValuesNaN && Double.isNaN(value);

            if (m_ignoreNaN && Double.isNaN(value)) {
                return;
            }

            if (value < m_min) {
                m_min = value;
            }
        }

        @Override
        public Computer createResultComputer() {
            boolean shouldWarn = (m_ignoreNaN && m_allValuesNaN) || m_isMissing;
            if (shouldWarn) {
                return FloatComputer.of(ctx -> Double.NaN,
                    missingWithWarning("COLUMN_MIN returned MISSING because all values were either MISSING or NaN."));
            }

            if (!m_ignoreNaN && m_anyValuesNaN) {
                return FloatComputer.of(ctx -> Double.NaN, ctx -> m_isMissing);
            } else {
                return FloatComputer.of(ctx -> m_min, ctx -> m_isMissing || m_allValuesNaN);
            }
        }
    }

    private static final class MinIntegerAggregation extends AbstractAggregation {

        private long m_min = Long.MAX_VALUE;

        private MinIntegerAggregation(final int columnIdx) {
            super(columnIdx);
        }

        @Override
        protected void addNonMissingRow(final RowRead row) {
            var value = ((LongValue)row.getValue(m_columnIdx)).getLongValue();
            if (value < m_min) {
                m_min = value;
            }
        }

        @Override
        public Computer createResultComputer() {
            boolean shouldWarn = m_isMissing;
            if (shouldWarn) {
                return IntegerComputer.of(ctx -> 0,
                    missingWithWarning("COLUMN_MIN returned MISSING because all values were MISSING."));
            }

            return IntegerComputer.of(ctx -> m_min, ctx -> m_isMissing);
        }
    }
}
