package org.knime.core.expressions.aggregations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;

/**
 * A builder for creating dynamic tests for {@link ColumnAggregation} implementations. This builder simplifies the
 * process of defining and executing tests to verify the correct behavior of column aggregations in KNIME expressions.
 * Add tests via {@link #typing} and {@link #illegalArgs}. Run the tests by returning the result of {@link #tests()}
 * from a {@link TestFactory}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ColumnAggregationTestBuilder {

    private final ColumnAggregation m_aggregation;

    private final List<DynamicTest> m_typingTests;

    private final List<DynamicTest> m_illegalArgsTests;

    /** Map to store column names and their corresponding value types */
    private final Map<String, ValueType> m_columnTypes;

    /**
     * @param aggregation The `ColumnAggregation` implementation to be tested.
     * @param columnTypes A map of column names and their associated value types.
     */
    public ColumnAggregationTestBuilder(final ColumnAggregation aggregation, final Map<String, ValueType> columnTypes) {
        m_aggregation = aggregation;
        m_columnTypes = columnTypes;
        m_typingTests = new ArrayList<>();
        m_illegalArgsTests = new ArrayList<>();
    }

    private ReturnResult<ValueType> columnType(final String colName) {
        return ReturnResult.fromNullable(m_columnTypes.get(colName), "Column does not exist");
    }

    /**
     * Adds a test to verify the correct inference of the aggregation's return type based on the given arguments.
     *
     * @param name A descriptive name for the test.
     * @param arguments The arguments to be passed to the aggregation.
     * @param expectedReturn The expected return type of the aggregation.
     * @return This builder instance for method chaining.
     */
    public ColumnAggregationTestBuilder typing(final String name, final Arguments<Ast.ConstantAst> arguments,
        final ValueType expectedReturn) {
        m_typingTests.add(DynamicTest.dynamicTest(name, () -> {
            var returnType = m_aggregation.returnType(arguments, this::columnType);
            assertTrue(returnType.isOk(), "should fit arguments");
            assertEquals(expectedReturn, returnType.getValue(), "should return correct type for " + arguments);
        }));
        return this;
    }

    /**
     * Adds a test to verify that the aggregation does not accept the specified arguments due to invalid types or other
     * constraints.
     *
     * @param name A descriptive name for the test.
     * @param arguments The arguments that should be rejected by the aggregation.
     * @return This builder instance for method chaining.
     */
    public ColumnAggregationTestBuilder illegalArgs(final String name, final Arguments<Ast.ConstantAst> arguments) {
        m_illegalArgsTests.add(DynamicTest.dynamicTest(name, () -> {
            var returnType = m_aggregation.returnType(arguments, this::columnType);
            assertFalse(returnType.isOk(), "should not fit arguments");
        }));
        return this;
    }

    /** @return the dynamic tests */
    public List<DynamicNode> tests() {
        List<DynamicNode> tests = new ArrayList<>();
        if (!m_typingTests.isEmpty()) {
            tests.add(DynamicContainer.dynamicContainer("typing", m_typingTests));
        }
        if (!m_illegalArgsTests.isEmpty()) {
            tests.add(DynamicContainer.dynamicContainer("illegal args", m_illegalArgsTests));
        }
        return tests;
    }
}