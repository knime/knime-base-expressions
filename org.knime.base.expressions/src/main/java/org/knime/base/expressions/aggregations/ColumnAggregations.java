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
 *   May 23, 2024 (benjamin): created
 */
package org.knime.base.expressions.aggregations;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.v2.RowRead;
import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ToBooleanFunction;
import org.knime.core.expressions.aggregations.BuiltInAggregations;

/**
 * A collection of column aggregation implementations that operate on {@link RowRead}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ColumnAggregations {

    private ColumnAggregations() {
    }

    /** Interface for an aggregation implementation that operates on {@link RowRead}. */
    public interface Aggregation {

        /** @param row the next row to add */
        void addRow(RowRead row);

        /** @return a computer that returns the result of the aggregation */
        Computer createResultComputer();
    }

    /**
     * Returns the implementation of the aggregation for the given aggregation call.
     *
     * @param aggregationCall the aggregation call
     * @param tableSpec the table spec of the table
     * @return the implementation of the aggregation
     */
    public static Aggregation getAggregationImplementationFor(final AggregationCall aggregationCall,
        final DataTableSpec tableSpec) { // NOSONAR the number of returns here is fine

        var columnAggregation = aggregationCall.aggregation();

        if (BuiltInAggregations.MAX.equals(columnAggregation)) {
            return MaxColumnAggregationImpl.maxAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.MIN.equals(columnAggregation)) {
            return MinColumnAggregationImpl.minAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.AVERAGE.equals(columnAggregation)) {
            return AverageColumnAggregationImpl.averageAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.MEDIAN.equals(columnAggregation)) {
            return MedianColumnAggregationImpl.medianAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.SUM.equals(columnAggregation)) {
            return SumColumnAggregationImpl.sumAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.VARIANCE.equals(columnAggregation)) {
            return VarianceColumnAggregationImpl.varianceAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.STD_DEV.equals(columnAggregation)) {
            return StdDevColumnAggregationImpl.stddevAggregation(aggregationCall.args(), tableSpec);
        } else if (BuiltInAggregations.COUNT.equals(columnAggregation)) {
            return CountColumnAggregationImpl.countAggregation(aggregationCall.args(), tableSpec);
        } else {
            throw new UnsupportedOperationException("Aggregation " + columnAggregation.name() + " is not supported.");
        }
    }

    /**
     * Utility method to create a missing value that adds a warning to the context.
     *
     * @param warning the warning text
     * @return a function that always returns {@code true}
     */
    public static ToBooleanFunction<EvaluationContext> missingWithWarning(final String warning) {
        return ctx -> {
            ctx.addWarning(warning);
            return true;
        };
    }
}
