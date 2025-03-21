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
 *   Apr 8, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.DateDurationComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.TestUtils;
import org.knime.core.expressions.ValueType;

/**
 * Builder for dynamic tests for an {@link ExpressionFunction}. Add tests via {@link #typing}, {@link #illegalArgs}, and
 * {@link #impl}. Run the tests by returning the result of {@link #tests()} from a {@link TestFactory}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin Germany
 */
public final class FunctionTestBuilder {

    private static final EvaluationContext DUMMY_WML = w -> {
    };

    // == Arguments for testing functions ==

    /**
     * An argument for tests. Can create computers that allow access to the value only once and only when it is "open".
     */
    public static final class TestingArgument {

        private final Object m_value;

        private final boolean m_isMissing;

        private boolean m_open = false;

        private boolean m_accessedValue = false;

        private boolean m_accessedIsMissing = false;

        private TestingArgument(final Object value, final boolean isMissing) {
            m_value = value;
            m_isMissing = isMissing;
        }

        private void setOpen() {
            m_open = true;
        }

        private void resetAccessed() {
            this.m_accessedValue = false;
            this.m_accessedIsMissing = false;
        }

        private boolean isMissing() {
            if (!m_open) {
                return fail("isMissing called during setup");
            } else if (m_accessedIsMissing) {
                return fail("isMissing called multiple times");
            } else {
                m_accessedIsMissing = true;
                return m_isMissing;
            }
        }

        /**
         * @param ctx unused parameter
         */
        private boolean isMissing(final EvaluationContext ctx) {
            return this.isMissing();
        }

        private Object getValue() {
            if (!m_open) {
                return fail("compute called during setup");
            } else if (m_isMissing) {
                return fail("compute called for missing value");
            } else if (m_accessedValue) {
                return fail("compute called multiple times");
            } else {
                m_accessedValue = true;
                return m_value;
            }
        }

        private Computer computer() {
            // TODO(AP-24022): Use pattern matching for exhaustive switch
            if (m_value instanceof Boolean) {
                return BooleanComputer.of(ctx -> (Boolean)getValue(), this::isMissing);
            } else if (m_value instanceof Long) {
                return IntegerComputer.of(ctx -> (Long)getValue(), this::isMissing);
            } else if (m_value instanceof Double) {
                return FloatComputer.of(ctx -> (Double)getValue(), this::isMissing);
            } else if (m_value instanceof String) {
                return StringComputer.of(ctx -> (String)getValue(), this::isMissing);
            } else if (m_value instanceof LocalDate) {
                return LocalDateComputer.of(ctx -> (LocalDate)getValue(), this::isMissing);
            } else if (m_value instanceof LocalTime) {
                return LocalTimeComputer.of(ctx -> (LocalTime)getValue(), this::isMissing);
            } else if (m_value instanceof LocalDateTime) {
                return LocalDateTimeComputer.of(ctx -> (LocalDateTime)getValue(), this::isMissing);
            } else if (m_value instanceof ZonedDateTime) {
                return ZonedDateTimeComputer.of(ctx -> (ZonedDateTime)getValue(), this::isMissing);
            } else if (m_value instanceof Duration) {
                return TimeDurationComputer.of(ctx -> (Duration)getValue(), this::isMissing);
            } else if (m_value instanceof Period) {
                return DateDurationComputer.of(ctx -> (Period)getValue(), this::isMissing);
            } else {
                return ctx -> isMissing();
            }
        }
    }

    /**
     * @param value
     * @return a BOOLEAN {@link TestingArgument}
     */
    public static TestingArgument arg(final boolean value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return an INTEGER {@link TestingArgument}
     */
    public static TestingArgument arg(final long value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a FLOAT {@link TestingArgument}
     */
    public static TestingArgument arg(final double value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a STRING {@link TestingArgument}
     */
    public static TestingArgument arg(final String value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a LOCAL_DATE {@link TestingArgument}
     */
    public static TestingArgument arg(final LocalDate value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a LOCAL_TIME {@link TestingArgument}
     */
    public static TestingArgument arg(final LocalTime value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a LOCAL_DATE_TIME {@link TestingArgument}
     */
    public static TestingArgument arg(final LocalDateTime value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a ZONED_DATE_TIME {@link TestingArgument}
     */
    public static TestingArgument arg(final ZonedDateTime value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a TIME_DURATION {@link TestingArgument}
     */
    public static TestingArgument arg(final Duration value) {
        return new TestingArgument(value, false);
    }

    /**
     * @param value
     * @return a DATE_DURATION {@link TestingArgument}
     */
    public static TestingArgument arg(final Period value) {
        return new TestingArgument(value, false);
    }

    /** @return a MISSING {@link TestingArgument} */
    public static TestingArgument mis() {
        return new TestingArgument(null, true);
    }

    /** @return a BOOLEAN {@link TestingArgument} that is missing */
    public static TestingArgument misBoolean() {
        return new TestingArgument(false, true);
    }

    /** @return an INTEGER {@link TestingArgument} that is missing */
    public static TestingArgument misInteger() {
        return new TestingArgument(1L, true);
    }

    /** @return a FLOAT {@link TestingArgument} that is missing */
    public static TestingArgument misFloat() {
        return new TestingArgument(1.0, true);
    }

    /** @return a STRING {@link TestingArgument} that is missing */
    public static TestingArgument misString() {
        return new TestingArgument("", true);
    }

    /** @return a LOCAL_DATE {@link TestingArgument} that is missing */
    public static TestingArgument misLocalDate() {
        return new TestingArgument(LocalDate.now(), true);
    }

    /** @return a LOCAL_TIME {@link TestingArgument} that is missing */
    public static TestingArgument misLocalTime() {
        return new TestingArgument(LocalTime.now(), true);
    }

    /** @return a LOCAL_DATE_TIME {@link TestingArgument} that is missing */
    public static TestingArgument misLocalDateTime() {
        return new TestingArgument(LocalDateTime.now(), true);
    }

    /** @return a ZONED_DATE_TIME {@link TestingArgument} that is missing */
    public static TestingArgument misZonedDateTime() {
        return new TestingArgument(ZonedDateTime.now(), true);
    }

    /** @return a TIME_DURATION {@link TestingArgument} that is missing */
    public static TestingArgument misDuration() {
        return new TestingArgument(Duration.ZERO, true);
    }

    /** @return a DATE_DURATION {@link TestingArgument} that is missing */
    public static TestingArgument misPeriod() {
        return new TestingArgument(Period.ZERO, true);
    }

    // ====

    private final ExpressionFunction m_function;

    private final List<DynamicTest> m_typingTests;

    private final List<DynamicTest> m_illegalArgsTests;

    private final List<DynamicTest> m_implTests;

    private final List<DynamicTest> m_warningTests;

    private final List<DynamicTest> m_evaluationErrorTests;

    /** @param function the function that should be tested */
    public FunctionTestBuilder(final ExpressionFunction function) {
        m_function = function;
        m_typingTests = new ArrayList<>();
        m_illegalArgsTests = new ArrayList<>();
        m_implTests = new ArrayList<>();
        m_warningTests = new ArrayList<>();
        m_evaluationErrorTests = new ArrayList<>();
    }

    /**
     * Add a test that checks that the return type is inferred correctly for the given argument types.
     *
     * @param name display name of the test
     * @param positionalArgTypes
     * @param namedArgTypes
     * @param expectedReturn the expected return type of {@link ExpressionFunction#returnType(Arguments)}
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder typing(final String name, final List<ValueType> positionalArgTypes,
        final Map<String, ValueType> namedArgTypes, final ValueType expectedReturn) {

        var args = m_function.signature(positionalArgTypes, namedArgTypes);

        m_typingTests.add(DynamicTest.dynamicTest(name, () -> {
            assertTrue(args.isOk(), "typing test: arguments should match signature");
            var returnType = m_function.returnType(args.getValue());
            assertTrue(returnType.isOk(),
                "should fit arguments: " + (returnType.isError() ? returnType.getErrorMessage() : ""));
            assertEquals(expectedReturn, returnType.getValue(),
                "should return correct type for " + args.getValue().toString());
        }));
        return this;
    }

    /**
     * @param name
     * @param positionalArgTypes
     * @param expectedReturn
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder typing(final String name, final List<ValueType> positionalArgTypes,
        final ValueType expectedReturn) {
        return typing(name, positionalArgTypes, Map.of(), expectedReturn);
    }

    /**
     * Add a test that checks that the function does not accept the given argument types.
     *
     * TODO(AP-23143): Test error messages in case of manual argument checks
     *
     * @param name display name of the test
     * @param positionalArgTypes
     * @param namedArgTypes
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder illegalArgs(final String name, final List<ValueType> positionalArgTypes,
        final Map<String, ValueType> namedArgTypes) {

        var invalidArgs = m_function.signature(positionalArgTypes, namedArgTypes);

        m_illegalArgsTests.add(DynamicTest.dynamicTest(name, () -> {

            var returnType = invalidArgs.flatMap(m_function::returnType);
            assertTrue(invalidArgs.isError() || returnType.isError(), "should not fit arguments.  " + //
                (invalidArgs.isOk() ? invalidArgs.getValue().toString() : invalidArgs.getErrorMessage()) + " -> " //
                + (returnType.isOk() ? returnType.getValue().toString() : returnType.getErrorMessage()) + " ("
                + m_function.name() + ": " + m_function.description().arguments() + " -> "
                + m_function.description().returnType() + ").");
        }));
        return this;
    }

    /**
     * @param name
     * @param positionalArgTypes
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder illegalArgs(final String name, final List<ValueType> positionalArgTypes) {
        return illegalArgs(name, positionalArgTypes, Map.of());
    }

    /**
     * Add a test that checks that the function evaluates to "MISSING".
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(Computer.class, c, m_function.name() + " should eval to Computer");
            try {
                assertTrue(c.isMissing(DUMMY_WML), m_function.name() + " should be missing");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs) {
        return impl(name, positionalArgs, Map.of());
    }

    /**
     * Add a test that checks that the function evaluates to the expected BOOLEAN value;
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final boolean expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(BooleanComputer.class, c, m_function.name() + " should eval to BOOLEAN");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((BooleanComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final boolean expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * Add a test that checks that the function evaluates to the expected INTEGER value;
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final long expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(IntegerComputer.class, c, m_function.name() + " should eval to INTEGER");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((IntegerComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final long expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * Add a test that checks that the function evaluates to the expected FLOAT value;
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final double expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(FloatComputer.class, c, m_function.name() + " should eval to FLOAT");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((FloatComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final double expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * Add a test that checks that the function evaluates to the expected STRING value;
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final String expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(StringComputer.class, c, m_function.name() + " should eval to STRING");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((StringComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final String expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final LocalDate expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(LocalDateComputer.class, c, m_function.name() + " should eval to LOCAL_DATE");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((LocalDateComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final LocalDate expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final LocalTime expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(LocalTimeComputer.class, c, m_function.name() + " should eval to LOCAL_TIME");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((LocalTimeComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final LocalTime expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final LocalDateTime expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(LocalDateTimeComputer.class, c, m_function.name() + " should eval to LOCAL_DATE_TIME");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((LocalDateTimeComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final LocalDateTime expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final ZonedDateTime expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(ZonedDateTimeComputer.class, c, m_function.name() + " should eval to ZONED_DATE_TIME");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((ZonedDateTimeComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final ZonedDateTime expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final Duration expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(TimeDurationComputer.class, c, m_function.name() + " should eval to TIME_DURATION");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((TimeDurationComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Duration expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final Period expected) {
        return impl(name, positionalArgs, namedArgs, c -> {
            assertInstanceOf(DateDurationComputer.class, c, m_function.name() + " should eval to DATE_DURATION");
            try {
                assertFalse(c.isMissing(DUMMY_WML), m_function.name() + " should not be missing");
                positionalArgs.forEach(TestingArgument::resetAccessed);
                namedArgs.values().forEach(TestingArgument::resetAccessed);
                assertEquals(expected, ((DateDurationComputer)c).compute(DUMMY_WML),
                    m_function.name() + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        });
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Period expected) {
        return impl(name, positionalArgs, Map.of(), expected);
    }

    /**
     * Add a test that checks that the function evaluates to the expected FLOAT value (with specified tolerance)
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @param tolerance absolute tolerance for floating point values
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder implWithTolerance(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final double expected, final double tolerance) {
        return impl(name, positionalArgs, namedArgs,
            TestUtils.computerResultChecker(m_function.name(), expected, tolerance));
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @param tolerance
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder implWithTolerance(final String name, final List<TestingArgument> positionalArgs,
        final double expected, final double tolerance) {
        return implWithTolerance(name, positionalArgs, Map.of(), expected, tolerance);
    }

    /**
     * Add a test that checks that the function evaluates to the expected FLOAT value (with tolerance of 1e-10);
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder implWithTolerance(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final double expected) {

        return implWithTolerance(name, positionalArgs, namedArgs, expected, 1e-10);
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expected
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder implWithTolerance(final String name, final List<TestingArgument> positionalArgs,
        final double expected) {
        return implWithTolerance(name, positionalArgs, Map.of(), expected, 1e-10);
    }

    /**
     * Add a test that checks that the function evaluates such that the result checker succeeds.
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param resultChecker a consumer that checks if the returned computer evaluates to the correct value
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder impl(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final Consumer<Computer> resultChecker) {

        m_implTests.add(DynamicTest.dynamicTest("impl - " + name, () -> {

            var args = m_function.signature(positionalArgs, namedArgs);

            if (args.isError()) {
                fail("Arguments do not match signature: " + args.getErrorMessage());
            }
            var result = m_function.apply(args.getValue().map(TestingArgument::computer));
            // NB: we open the args after calling apply to make sure that apply does not access the computers
            positionalArgs.forEach(TestingArgument::setOpen);
            namedArgs.values().forEach(TestingArgument::setOpen);
            resultChecker.accept(result);
        }));
        return this;
    }

    /**
     * A test that checks that the given arguments produce a warning.
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expectedWarningPattern a pattern that the warning message should match
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder warns(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final String expectedWarningPattern) {
        m_warningTests.add(DynamicTest.dynamicTest("warns - " + name, () -> {

            var args = m_function.signature(positionalArgs, namedArgs)
                .orElseThrow(cause -> new IllegalStateException("Arguments should match signature: " + cause));

            var resultComputer = m_function.apply(args.map(TestingArgument::computer));

            args.stream().forEach(TestingArgument::setOpen);

            var warnings = new ArrayList<String>();
            EvaluationContext ctx = w -> warnings.add(w);

            // Compute the computer
            if (!resultComputer.isMissing(ctx)) {
                // TODO(AP-24022): use pattern matching for exhaustive case switches
                if (resultComputer instanceof IntegerComputer ic) {
                    ic.compute(ctx);
                } else if (resultComputer instanceof FloatComputer fc) {
                    fc.compute(ctx);
                } else if (resultComputer instanceof BooleanComputer bc) {
                    bc.compute(ctx);
                } else if (resultComputer instanceof StringComputer sc) {
                    sc.compute(ctx);
                } else if (resultComputer instanceof LocalDateComputer ldc) {
                    ldc.compute(ctx);
                } else if (resultComputer instanceof LocalTimeComputer ltc) {
                    ltc.compute(ctx);
                } else if (resultComputer instanceof LocalDateTimeComputer ldtc) {
                    ldtc.compute(ctx);
                } else if (resultComputer instanceof ZonedDateTimeComputer zdtc) {
                    zdtc.compute(ctx);
                } else if (resultComputer instanceof TimeDurationComputer tdc) {
                    tdc.compute(ctx);
                } else if (resultComputer instanceof DateDurationComputer ddc) {
                    ddc.compute(ctx);
                } else {
                    fail("unexpected computer type");
                }
            }

            assertFalse(warnings.isEmpty(), "expected warnings");
            assertEquals(1, warnings.size(), "testing multiple warnings at once is not a good idea");
            assertThat(warnings.get(0)).as("Must match expected pattern").matches(expectedWarningPattern);
            assertThat(warnings.get(0)).as("Warning messages must have end punctuation").endsWith(".");
            assertThat(warnings.get(0)).as("Too many dots were appended").doesNotEndWith("..");
        }));

        return this;

    }

    /**
     * @param name
     * @param positionalArgs
     * @param expectedWarningPattern a pattern that the warning message should match
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder warns(final String name, final List<TestingArgument> positionalArgs,
        final String expectedWarningPattern) {
        return warns(name, positionalArgs, Map.of(), expectedWarningPattern);
    }

    /**
     * A test that checks that the given arguments produce MISSING result and a warning. Actually a shorthand for
     * calling both {@link #impl} and {@link #warns} with the same arguments, so it adds two tests.
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expectedWarningPattern a pattern that the warning message should match
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder missingAndWarns(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final String expectedWarningPattern) {

        impl(name, positionalArgs, namedArgs);

        // we need to copy the arguments otherwise the tests will think we called isMissing and compute
        // twice, and fail.
        var copiedPositionalArgs = positionalArgs.stream() //
            .map(arg -> new TestingArgument(arg.m_value, arg.m_isMissing)) //
            .toList();
        var copiedNamedArgs = namedArgs.entrySet().stream() //
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> new TestingArgument(e.getValue().m_value, e.getValue().m_isMissing)));

        warns(name, copiedPositionalArgs, copiedNamedArgs, expectedWarningPattern);

        return this;
    }

    /**
     * @param name
     * @param positionalArgs
     * @param expectedWarningPattern a pattern that the warning message should match
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder missingAndWarns(final String name, final List<TestingArgument> positionalArgs,
        final String expectedWarningPattern) {
        return missingAndWarns(name, positionalArgs, Map.of(), expectedWarningPattern);
    }

    /**
     * A test that checks that evaluating the given arguments produces an error. The arguments must not result in
     * MISSING.
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param expectedErrorPattern a pattern that the error message should match
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder errors(final String name, final List<TestingArgument> positionalArgs,
        final String expectedErrorPattern) {
        return errors(name, positionalArgs, Map.of(), expectedErrorPattern);
    }

    /**
     * A test that checks that evaluating the given arguments produces an error. The arguments must not result in
     * MISSING.
     *
     * @param name display name of the test
     * @param positionalArgs
     * @param namedArgs
     * @param expectedErrorPattern a pattern that the error message should match
     * @return <code>this</code> for chaining
     */
    public FunctionTestBuilder errors(final String name, final List<TestingArgument> positionalArgs,
        final Map<String, TestingArgument> namedArgs, final String expectedErrorPattern) {
        m_evaluationErrorTests.add(DynamicTest.dynamicTest("errors - " + name, () -> {

            var args = m_function.signature(positionalArgs, namedArgs)
                .orElseThrow(cause -> new IllegalStateException("Arguments should match signature: " + cause));

            var result = m_function.apply(args.map(TestingArgument::computer));
            args.stream().forEach(TestingArgument::setOpen);

            ExpressionEvaluationException exception;
            try {
                assertFalse(result.isMissing(DUMMY_WML), "expected error but was missing");

                // TODO(AP-24022): use pattern matching for exhaustive case switches
                if (result instanceof IntegerComputer ic) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> ic.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof FloatComputer fc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> fc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof BooleanComputer bc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> bc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof StringComputer sc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> sc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof LocalDateComputer ldc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> ldc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof LocalTimeComputer ltc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> ltc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof LocalDateTimeComputer ldtc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> ldtc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof ZonedDateTimeComputer zdtc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> zdtc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof TimeDurationComputer tdc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> tdc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else if (result instanceof DateDurationComputer ddc) {
                    exception = assertThrows(ExpressionEvaluationException.class, () -> ddc.compute(DUMMY_WML),
                        "evaluation should throw an error");
                } else {
                    fail("unexpected computer type");
                    return;
                }
            } catch (ExpressionEvaluationException ex) {
                // if isMissing is throwing the exception, this is okay but not necessary
                exception = ex;
            }
            assertThat(exception).hasMessageMatching(expectedErrorPattern);
            assertThat(exception).as("Warning messages must have end punctuation").hasMessageEndingWith(".");
            assertThat(exception.getMessage()).as("Too many dots were appended").doesNotEndWith("..");
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
        if (!m_implTests.isEmpty()) {
            tests.add(DynamicContainer.dynamicContainer("impl", m_implTests));
        }
        if (!m_warningTests.isEmpty()) {
            tests.add(DynamicContainer.dynamicContainer("warns", m_warningTests));
        }
        if (!m_evaluationErrorTests.isEmpty()) {
            tests.add(DynamicContainer.dynamicContainer("errors", m_evaluationErrorTests));
        }
        return tests;
    }

}
