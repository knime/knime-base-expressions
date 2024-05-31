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
 *   Jun 5, 2024 (david): created
 */
package org.knime.base.expressions.aggregations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.base.expressions.aggregations.TestRow.tr;
import static org.knime.core.expressions.AstTestUtils.STR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.knime.base.expressions.aggregations.ColumnAggregations.Aggregation;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;

/**
 * Collection of utilities for running tests on aggregations.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public class AggregationTestUtils {

    /**
     * Creates a list of elements. Unlike {@Link List#of} this method can deal with null elements.
     *
     * @param elements
     * @return the list
     */
    @SafeVarargs
    public static <T> List<T> listOf(final T... elements) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(elements)));
    }

    /**
     * Helper class to build tests for aggregations.
     *
     * In every function in this builder, a null table value or expected value is assumed to represent a missing value.
     *
     * @author David Hickey, TNG Technology Consulting GmbH
     */
    public static final class AggregationTestBuilder {

        // ======= STATIC MEMBERS =======

        /** Warning listener that throws away any warnings */
        private static final EvaluationContext DUMMY_WML = w -> {
        };

        /** Index of the long column in the test table */
        public static final int LONG_COL_IDX = 0;

        /** Index of the int column in the test table */
        public static final int INT_COL_IDX = 1;

        /** Index of the double column in the test table */
        public static final int DOUBLE_COL_IDX = 2;

        /** Index of the string column in the test table */
        public static final int STRING_COL_IDX = 3;

        /** Name of the long column in the test table */
        public static final String LONG_COL_NAME = "LONG_COL";

        /** Name of the int column in the test table */
        public static final String INT_COL_NAME = "INT_COL";

        /** Name of the double column in the test table */
        public static final String DOUBLE_COL_NAME = "DOUBLE_COL";

        /** Name of the string column in the test table */
        public static final String STRING_COL_NAME = "STRING_COL";

        /** A test table spec with columns of type long, int, double, and string. */
        public static final DataTableSpec TEST_TABLE_SPEC = new DataTableSpec( //
            new DataColumnSpecCreator(LONG_COL_NAME, LongCell.TYPE).createSpec(), //
            new DataColumnSpecCreator(INT_COL_NAME, IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator(DOUBLE_COL_NAME, DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator(STRING_COL_NAME, StringCell.TYPE).createSpec() //
        );

        // ======= INSTANCE MEMBERS =======

        private final BiFunction<Arguments<ConstantAst>, DataTableSpec, Aggregation> m_aggregationSupplier;

        private final List<DynamicTest> m_implTests = new ArrayList<>();

        private final List<DynamicTest> m_unsupportedTypeTests = new ArrayList<>();

        /** Tolerance for double comparisons in impl tests */
        private double m_doubleEqTolerance = 0.0;

        /**
         * Create a new test builder. The supplier should be a function that creates an aggregation from arguments and a
         * table spec (e.g. {@link MaxColumnAggregationImpl#maxAggregation}).
         *
         * @param aggregationSupplier the aggregation supplier function
         */
        public AggregationTestBuilder(
            final BiFunction<Arguments<ConstantAst>, DataTableSpec, Aggregation> aggregationSupplier) {

            m_aggregationSupplier = aggregationSupplier;
        }

        // ======= TOLERANCE SETTERS =======

        /**
         * Set the tolerance for double comparisons for all future tests.
         *
         * @param tolerance
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder setFutureTolerances(final double tolerance) {
            m_doubleEqTolerance = tolerance;
            return this;
        }

        /**
         * Reset the tolerance to 0 for all future tests.
         *
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder resetTolerance() {
            return setFutureTolerances(0.0);
        }

        // ======= LONG AGGREGATION TESTS =======

        /**
         * Run a test on the column of type Long with both positional and named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implLong(final String testName, final List<Long> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Map<String, ConstantAst> extraNamedArgs,
            final Object expectedResult) {

            implGeneric(testName, tableValues, extraPositionalArgs, extraNamedArgs, expectedResult, LONG_COL_IDX,
                LONG_COL_NAME);
            return this;
        }

        /**
         * Run a test on the column of type Long with named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implLong(final String testName, final List<Long> tableValues,
            final Map<String, ConstantAst> extraNamedArgs, final Object expectedResult) {

            return implLong(testName, tableValues, new ArrayList<>(), extraNamedArgs, expectedResult);
        }

        /**
         * Run a test on the column of type Long with positional arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implLong(final String testName, final List<Long> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Object expectedResult) {

            return implLong(testName, tableValues, extraPositionalArgs, new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type Long with no additional arguments.
         *
         * @param testName
         * @param tableValues
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implLong(final String testName, final List<Long> tableValues,
            final Object expectedResult) {

            return implLong(testName, tableValues, new ArrayList<>(), new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type Long, with the expectation that the aggregation will fail due to invalid
         * column type.
         *
         * @param testName
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder unsupportedTypeLong(final String testName) {
            unsupportedTypeGeneric(testName, LONG_COL_NAME);
            return this;
        }

        // ======= INT AGGREGATION TESTS =======

        /**
         * Run a test on the column of type Integer with both positional and named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implInt(final String testName, final List<Integer> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Map<String, ConstantAst> extraNamedArgs,
            final Object expectedResult) {

            implGeneric(testName, tableValues, extraPositionalArgs, extraNamedArgs, expectedResult, INT_COL_IDX,
                INT_COL_NAME);
            return this;
        }

        /**
         * Run a test on the column of type Integer with named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implInt(final String testName, final List<Integer> tableValues,
            final Map<String, ConstantAst> extraNamedArgs, final Object expectedResult) {

            return implInt(testName, tableValues, new ArrayList<>(), extraNamedArgs, expectedResult);
        }

        /**
         * Run a test on the column of type Integer with positional arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implInt(final String testName, final List<Integer> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Object expectedResult) {

            return implInt(testName, tableValues, extraPositionalArgs, new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type Integer with no additional arguments.
         *
         * @param testName
         * @param tableValues
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implInt(final String testName, final List<Integer> tableValues,
            final Object expectedResult) {

            return implInt(testName, tableValues, new ArrayList<>(), new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type Integer, with the expectation that the aggregation will fail due to invalid
         * column type.
         *
         * @param testName
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder unsupportedTypeInt(final String testName) {
            unsupportedTypeGeneric(testName, INT_COL_NAME);
            return this;
        }

        // ======= DOUBLE AGGREGATION TESTS =======

        /**
         * Run a test on the column of type Double with both positional and named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implDouble(final String testName, final List<Double> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Map<String, ConstantAst> extraNamedArgs,
            final Object expectedResult) {

            implGeneric(testName, tableValues, extraPositionalArgs, extraNamedArgs, expectedResult, DOUBLE_COL_IDX,
                DOUBLE_COL_NAME);
            return this;
        }

        /**
         * Run a test on the column of type Double with named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implDouble(final String testName, final List<Double> tableValues,
            final Map<String, ConstantAst> extraNamedArgs, final Object expectedResult) {

            return implDouble(testName, tableValues, new ArrayList<>(), extraNamedArgs, expectedResult);
        }

        /**
         * Run a test on the column of type Double with positional arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implDouble(final String testName, final List<Double> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Object expectedResult) {

            return implDouble(testName, tableValues, extraPositionalArgs, new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type Double with no additional arguments.
         *
         * @param testName
         * @param tableValues
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implDouble(final String testName, final List<Double> tableValues,
            final Object expectedResult) {

            return implDouble(testName, tableValues, new ArrayList<>(), new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type Double, with the expectation that the aggregation will fail due to invalid
         * column type.
         *
         * @param testName
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder unsupportedTypeDouble(final String testName) {
            unsupportedTypeGeneric(testName, DOUBLE_COL_NAME);
            return this;
        }

        // ======= STRING AGGREGATION TESTS =======

        /**
         * Run a test on the column of type String with both positional and named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implString(final String testName, final List<String> tableValues,
            final List<ConstantAst> extraPositionalArgs, final Map<String, ConstantAst> extraNamedArgs,
            final Object expectedResult) {

            implGeneric(testName, tableValues, extraPositionalArgs, extraNamedArgs, expectedResult, STRING_COL_IDX,
                STRING_COL_NAME);
            return this;
        }

        /**
         * Run a test on the column of type String with named arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraNamedArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implString(final String testName, final List<String> tableValues,
            final Map<String, ConstantAst> extraNamedArgs, final String expectedResult) {

            return implString(testName, tableValues, new ArrayList<>(), extraNamedArgs, expectedResult);
        }

        /**
         * Run a test on the column of type String with positional arguments.
         *
         * @param testName
         * @param tableValues
         * @param extraPositionalArgs
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implString(final String testName, final List<String> tableValues,
            final List<ConstantAst> extraPositionalArgs, final String expectedResult) {

            return implString(testName, tableValues, extraPositionalArgs, new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type String with no additional arguments.
         *
         * @param testName
         * @param tableValues
         * @param expectedResult
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder implString(final String testName, final List<String> tableValues,
            final Object expectedResult) {

            return implString(testName, tableValues, new ArrayList<>(), new HashMap<>(), expectedResult);
        }

        /**
         * Run a test on the column of type String, with the expectation that the aggregation will fail due to invalid
         * column type.
         *
         * @param testName
         * @return this, for builder-style chaining
         */
        public AggregationTestBuilder unsupportedTypeString(final String testName) {
            unsupportedTypeGeneric(testName, STRING_COL_NAME);
            return this;
        }

        /**
         * Compile the tests and end the builder. Suitable for returning from a function marked with the @TestFactory
         * annotation.
         *
         * @return the tests
         */
        public List<DynamicNode> tests() {
            return List.of( //
                DynamicContainer.dynamicContainer("implementation tests", m_implTests),
                DynamicContainer.dynamicContainer("unsupported type tests", m_unsupportedTypeTests) //
            );
        }

        // ======= GENERIC TEST IMPLEMENTATIONS =======

        /**
         * Create a test that expects the aggregation to fail due to an unsupported column type.
         *
         * @param testName the name of the test
         * @param colName the name of the column in the table spec
         */
        private void unsupportedTypeGeneric( //
            final String testName, //
            final String colName //
        ) {
            m_unsupportedTypeTests.add(DynamicTest.dynamicTest(testName, () -> {
                List<Ast.ConstantAst> allPositionalArgs = new ArrayList<>();
                allPositionalArgs.add(0, STR(colName));

                var args = new Arguments<>(allPositionalArgs, new HashMap<>());

                assertThrows(IllegalStateException.class, () -> {
                    m_aggregationSupplier.apply(args, TEST_TABLE_SPEC);
                }, "Expected an exception due to unsupported column type");
            }));
        }

        /**
         * Generic method that does the work of creating the implementation tests. Saves us a lot of code duplication.
         *
         * @param <T> the type of the values in the column to aggregate
         * @param testName the name of the test
         * @param tableValues the values of the column to aggregate. They'll be fed to the aggregation via
         *            {@link Aggregation#addRow}.
         * @param extraPositionalArgs extra positional arguments to pass to the aggregation (not including the column
         *            name)
         * @param extraNamedArgs extra named arguments to pass to the aggregation (not including the column name)
         * @param expectedResult the expected result of the aggregation's compute method
         * @param colIdx the index of the column to aggregate in the table spec
         * @param colName the name of the column to aggregate in the table spec
         */
        private <T> void implGeneric( //
            final String testName, //
            final List<T> tableValues, //
            final List<ConstantAst> extraPositionalArgs, //
            final Map<String, ConstantAst> extraNamedArgs, //
            final Object expectedResult, //
            final int colIdx, //
            final String colName //
        ) {
            m_implTests.add(DynamicTest.dynamicTest(testName, () -> {
                var allPositionalArgs = new ArrayList<>(extraPositionalArgs);
                allPositionalArgs.add(0, STR(colName));

                var args = new Arguments<>(allPositionalArgs, extraNamedArgs);

                var actualResult = populateAggRows( //
                    m_aggregationSupplier.apply(args, TEST_TABLE_SPEC), //
                    colIdx, //
                    tableValues //
                ).createResultComputer();

                if (expectedResult == null) {
                    assertTrue(actualResult.isMissing(DUMMY_WML),
                        "Expected column aggregation to return missing value. Args: %s".formatted(args));
                } else {
                    if (expectedResult instanceof Integer) {
                        // Since IntegerComputer returns a Long, we need to convert it to an int if
                        // the expected result is an int
                        var actualResultComputed = ((Long)computeGenericComputer(actualResult)).intValue();

                        assertEquals(expectedResult, actualResultComputed,
                            "Expected column aggregation to return correct result (expected %s, got %s). Args: %s"
                                .formatted(expectedResult, actualResultComputed, args));
                    } else if (expectedResult instanceof Double) {
                        // Doubles are a bit awkward because we need to handle both tolerance and NaNs

                        var actualResultComputed = ((Double)computeGenericComputer(actualResult)).doubleValue();
                        var expectedResultCasted = ((Double)expectedResult).doubleValue();

                        boolean areApproxEqual = Double.isNaN(expectedResultCasted) //
                            ? Double.isNaN(actualResultComputed) //
                            : Math.abs(expectedResultCasted - actualResultComputed) <= m_doubleEqTolerance;

                        assertTrue(areApproxEqual,
                            "Expected column aggregation to return correct result (expected %s, got %s). Args: %s"
                                .formatted(expectedResult, actualResultComputed, args));
                    } else {
                        var actualResultComputed = computeGenericComputer(actualResult);

                        assertEquals(expectedResult, actualResultComputed,
                            "Expected column aggregation to return correct result (expected %s, got %s). Args: %s"
                                .formatted(expectedResult, actualResultComputed, args));
                    }
                }
            }));
        }

        // ======= PRIVATE UTILITIES =======

        /**
         * Evaluate a generic computer by trying to cast to every known computer subtype.
         *
         * @param c the computer
         * @return the result of the computer's compute method
         */
        private static Object computeGenericComputer(final Computer c) {
            if (c instanceof IntegerComputer ic) {
                return ic.compute(DUMMY_WML);
            } else if (c instanceof FloatComputer fc) {
                return fc.compute(DUMMY_WML);
            } else if (c instanceof BooleanComputer bc) {
                return bc.compute(DUMMY_WML);
            } else if (c instanceof StringComputer sc) {
                return sc.compute(DUMMY_WML);
            } else {
                throw new IllegalArgumentException("Unsupported computer type: " + c);
            }
        }

        /**
         * Populate the aggregation with rows. It is assumed that the aggregation has the same DataTableSpec provided by
         * TEST_TABLE_SPEC, so the dtype of the list is inferred from the index provided. If the column index doesn't
         * match the data types in the provided list, you'll get a ClassCastException. If the column index is invalid,
         * you'll get an IllegalArgumentException.
         *
         * @param agg
         * @param colIdx the column that the aggregation is supposed to aggregate. We don't care about other columns in
         *            the rows.
         * @param values
         * @return
         */
        private static Aggregation populateAggRows(final Aggregation agg, final int colIdx,
            @SuppressWarnings("rawtypes") final List values) {

            for (Object value : values) {
                if (value == null) {
                    agg.addRow(tr(colIdx));
                } else if (colIdx == LONG_COL_IDX) {
                    agg.addRow(tr(colIdx, (Long)value));
                } else if (colIdx == INT_COL_IDX) {
                    agg.addRow(tr(colIdx, (Integer)value));
                } else if (colIdx == DOUBLE_COL_IDX) {
                    agg.addRow(tr(colIdx, (Double)value));
                } else if (colIdx == STRING_COL_IDX) {
                    agg.addRow(tr(colIdx, (String)value));
                } else {
                    throw new IllegalArgumentException("Invalid column index");
                }
            }

            return agg;
        }
    }
}
