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
import org.knime.core.data.v2.RowRead;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class CountColumnAggregationImpl {

    private static final boolean IGNORE_MISSING_DEFAULT = false;

    private CountColumnAggregationImpl() {
    }

    static Aggregation countAggregation(final Arguments<ConstantAst> arguments, final DataTableSpec tableSpec) {

        var columnIdx = ConstantArgumentResolver.resolveColumnIndex(arguments, tableSpec);

        var ignoreMissing =
            ConstantArgumentResolver.resolveOptionalBoolean(arguments, "ignore_missing", IGNORE_MISSING_DEFAULT);

        return new CountFloatAggregation(columnIdx, ignoreMissing);
    }

    @SuppressWarnings("squid:S3052") // Allow redundant initialisations for clarity
    private static final class CountFloatAggregation extends AbstractAggregation {

        private long m_count = 0;

        private final boolean m_ignoreMissing;

        private CountFloatAggregation(final int columnIdx, final boolean ignoreMissing) {
            super(columnIdx);

            m_ignoreMissing = ignoreMissing;

            // This aggregation is never missing
            m_isMissing = false;
        }

        @Override
        public void addRow(final RowRead row) {
            if (m_ignoreMissing && row.isMissing(m_columnIdx)) {
                return;
            }

            m_count++;
        }

        @Override
        protected void addNonMissingRow(final RowRead row) {
            // do nothing, addRow has this covered
        }

        @Override
        public Computer createResultComputer() {
            return Computer.IntegerComputer.of(ctx -> m_count, ctx -> m_isMissing);
        }
    }
}
