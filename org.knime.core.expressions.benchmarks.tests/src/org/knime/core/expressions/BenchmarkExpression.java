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
 *   Feb 18, 2025 (benjamin): created
 */
package org.knime.core.expressions;

import java.util.function.IntFunction;

/**
 * Expression examples that can be used for benchmarking.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public enum BenchmarkExpression {

        /** Binary operations with two constants */
        SIMPLE_ARITHMETIC("1 + 2 # A basic arithmetic operation with a comment", rowIdx -> 1 + 2), //

        /** Binary operations building a deep tree */
        DEEP_AST("1" + "+1".repeat(2_000), DeepAstJavaImpl::run), //

        /** Nests a function call 200 times */
        DEEP_FN_CALLS("sqrt(".repeat(200) + "10_000" + ")".repeat(200), DeepFnCallsJavaImpl::run), //

        /** Medium complexity expression with function calls logic operators and string concatenation */
        MEDIUM_COMPLEXITY("""
                if(
                    (abs($amount) > 100 and not ($status = "invalid")) or $["category"] = "special",
                    "Approved: " + $[ROW_ID],
                    "Rejected: " + (MISSING ?? "Unknown")
                )""", //
            rowIdx -> {
                if (rowIdx >= 9) {
                    return null;
                } else if ((Math.abs(BenchmarkTable.AMOUNT[rowIdx]) > 100
                    && !BenchmarkTable.STATUS[rowIdx].equals("invalid"))
                    || BenchmarkTable.CATEGORY[rowIdx].equals("special")) {
                    return "Approved: " + BenchmarkTable.ROW_ID[rowIdx];
                } else {
                    return "Rejected: " + (null == null ? "Unknown" : null);
                }
            } //
        ), //

        /** Complex expression aggregation and windowing */
        COMPLEX_AGGREGATION("""
                COLUMN_AVERAGE("sales", ignore_nan=TRUE) +
                ($["revenue", -1] * pow(E, -$["growth_rate"])) /
                ($[ROW_NUMBER] ?? 1) +
                if($["status"] = "active", "active", "inactive") +
                "_" + $$user_name
                """, //
            rowIdx -> {
                if (rowIdx >= 9) {
                    return null;
                } else {
                    var columnAverage = 42.2; // Note: aggregations happen outside of the expression framework
                    return columnAverage
                        + (BenchmarkTable.REVENUE[rowIdx] * Math.pow(Math.E, -BenchmarkTable.GROWTH_RATE[rowIdx]))
                            / (rowIdx + 1)
                        + (BenchmarkTable.STATUS[rowIdx].equals("active") ? "active" : "inactive") + "_" + "john.doe";
                }
            } //
        ), //
    ;

    private final String m_expression;

    private final IntFunction<Object> m_javaImpl;

    BenchmarkExpression(final String expression, final IntFunction<Object> javaImpl) {
        m_expression = expression;
        m_javaImpl = javaImpl;
    }

    /** @return the benchmark expression */
    public String getExpression() {
        return m_expression;
    }

    /**
     * Run the Java implementation of the expression.
     *
     * @param rowIdx the row index
     * @return the result
     */
    public Object runJavaImpl(final int rowIdx) {
        return m_javaImpl.apply(rowIdx);
    }
}