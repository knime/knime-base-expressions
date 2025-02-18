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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntSupplier;

import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;

/**
 * An example table for benchmarking with 10 rows. The last row contains only missing values.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class BenchmarkTable {

    /** Number of rows in the table. */
    public static final int NUM_ROWS = 10;

    static final double[] SALES = {50.2, 20.1, 30.3, 40.4, 10.5, 60.6, 70.7, 80.8, 90.9};

    static final int[] REVENUE = {10, 20, 30, 40, 50, 60, 70, 80, 90};

    static final int[] AMOUNT = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    static final double[] GROWTH_RATE = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};

    static final String[] STATUS =
        {"invalid", "valid", "active", "inactive", "invalid", "invalid", "active", "inactive", "active"};

    static final String[] CATEGORY =
        {"special", "normal", "special", "normal", "special", "normal", "special", "normal", "special"};

    static final String[] ROW_ID = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};

    /**
     * Column name to type mapper for the example table.
     *
     * @param columnName the name of the column
     * @return the type of the column
     */
    public static ReturnResult<ValueType> columnToType(final String columnName) {
        return switch (columnName) {
            case "sales" -> ReturnResult.success(ValueType.FLOAT);
            case "revenue" -> ReturnResult.success(ValueType.INTEGER);
            case "amount" -> ReturnResult.success(ValueType.INTEGER);
            case "growth_rate" -> ReturnResult.success(ValueType.FLOAT);
            case "status" -> ReturnResult.success(ValueType.STRING);
            case "category" -> ReturnResult.success(ValueType.STRING);
            default -> ReturnResult.failure("Unknown column " + columnName);
        };
    }

    /**
     * Column access to computer mapper for the example table.
     *
     * @param rowIdx the row index supplier
     * @return the mapper
     */
    public static Function<ColumnAccess, Optional<Computer>> columnToComputer(final IntSupplier rowIdx) {
        return columnAccess -> switch (columnAccess.columnId().type()) {
            case NAMED -> columnComputer(columnAccess.columnId().name(), rowIdx);
            case ROW_ID -> computer(ROW_ID, rowIdx);
            case ROW_INDEX -> rowIndexComputer(rowIdx);
        };
    }

    /**
     * Aggregation call to computer mapper for the example table.
     *
     * @param aggregationCall the aggregation
     * @return the computer
     */
    public static Optional<Computer> aggregationToComputer(final AggregationCall aggregationCall) {
        // We just return some value for now because the aggregation is done outside of the expression framework
        if (ValueType.INTEGER.equals(Expressions.getInferredType(aggregationCall).baseType())) {
            return Optional.of(IntegerComputer.of(ctx -> 42, ctx -> false));
        } else if (ValueType.FLOAT.equals(Expressions.getInferredType(aggregationCall).baseType())) {
            return Optional.of(FloatComputer.of(ctx -> 42.2, ctx -> false));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Computer> columnComputer(final String columnName, final IntSupplier rowIdx) {
        return switch (columnName) {
            case "sales" -> computer(SALES, rowIdx);
            case "revenue" -> computer(REVENUE, rowIdx);
            case "amount" -> computer(AMOUNT, rowIdx);
            case "growth_rate" -> computer(GROWTH_RATE, rowIdx);
            case "status" -> computer(STATUS, rowIdx);
            case "category" -> computer(CATEGORY, rowIdx);
            default -> Optional.empty();
        };
    }

    private static Optional<Computer> computer(final double[] values, final IntSupplier rowIdx) {
        return Optional.of(FloatComputer.of(ctx -> values[rowIdx.getAsInt()], ctx -> rowIdx.getAsInt() >= 9));
    }

    private static Optional<Computer> computer(final int[] values, final IntSupplier rowIdx) {
        return Optional.of(IntegerComputer.of(ctx -> values[rowIdx.getAsInt()], ctx -> rowIdx.getAsInt() >= 9));
    }

    private static Optional<Computer> computer(final String[] values, final IntSupplier rowIdx) {
        return Optional.of(StringComputer.of(ctx -> values[rowIdx.getAsInt()], ctx -> rowIdx.getAsInt() >= 9));
    }

    private static Optional<Computer> rowIndexComputer(final IntSupplier rowIdx) {
        return Optional.of(IntegerComputer.of(ctx -> rowIdx.getAsInt(), ctx -> false));
    }
}
