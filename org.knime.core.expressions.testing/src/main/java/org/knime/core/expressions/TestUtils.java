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
 *   Apr 5, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.ColumnId;
import org.knime.core.expressions.Ast.FlowVarAccess;
import org.knime.core.expressions.Ast.RowId;
import org.knime.core.expressions.Ast.RowIndex;
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
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin Germany
 */
public final class TestUtils {

    /**
     * Dummy execution start time for tests, set to 2024-04-05T12:00:00+02:00[Europe/Berlin]. Used by the
     * {@link TestUtils#DUMMY_EVAL_CTX}
     */
    public static final ZonedDateTime DUMMY_EXECUTION_START_TIME =
        ZonedDateTime.parse("2024-04-05T12:00:00+02:00[Europe/Berlin]");

    /**
     * Dummy {@link EvaluationContext} that ignores warnings and has the execution start time
     * {@link TestUtils#DUMMY_EXECUTION_START_TIME}
     */
    public static final EvaluationContext DUMMY_EVAL_CTX = EvaluationContext.of(DUMMY_EXECUTION_START_TIME, w -> {
    });

    /** Function that maps from a {@link ColumnId} to its {{@link ColumnId}} */
    public static Function<ColumnAccess, ColumnId> COLUMN_ID = ColumnAccess::columnId;

    /**
     * Function that maps from a {@link ColumnId} to its name. The names For the (un-named) {@link RowId} and
     * {@link RowIndex} columns, "ROW_INDEX" and "ROW_ID" are returned, respectively.
     */
    public static final Function<ColumnId, String> COLUMN_NAME = columnId -> switch (columnId.type()) {
        case NAMED -> columnId.name();
        case ROW_INDEX -> "ROW_INDEX";
        case ROW_ID -> "ROW_ID";
    };

    /** Function that maps from a {@link FlowVarAccess} to its name */
    public static Function<FlowVarAccess, String> FLOW_VAR_NAME = FlowVarAccess::name;

    private TestUtils() {
    }

    /**
     * @param <T> the enum type
     * @param values the enum values to search
     * @return a function that maps from a string to an optional enum value
     */
    public static final <T extends Enum<T>> Function<String, Optional<T>> enumFinderAsFunction(final T[] values) {
        return name -> Arrays.stream(values).filter(t -> t.name().equals(name)).findFirst();
    }

    /**
     * @param <O> the output type
     * @param values the enum values to search
     * @param outputType the output type
     * @return a function that maps from a string to an optional enum value
     */
    public static final <T extends Enum<T>, O> Function<String, Optional<O>> enumFinderAsFunction(final T[] values,
        final Class<O> outputType) {

        return enumFinderAsFunction(values).andThen(o -> o.map(outputType::cast));
    }

    /**
     * @param <T> the enum type
     * @param values the enum values to search
     * @return a map from a string to an enum value
     */
    public static final <T extends Enum<T>> Map<String, T> enumFinderAsMap(final T[] values) {
        return Arrays.stream(values).collect(Collectors.toMap(e -> e.name(), e -> e));
    }

    /**
     * @param <T> the enum type
     * @param <O> the output type
     * @param values the enum values to search
     * @param outputType the output type
     * @return a map to an optional enum value
     */
    public static final <T extends Enum<T>, O> Map<String, O> enumFinderAsMap(final T[] values,
        final Class<O> outputType) {

        return enumFinderAsMap(values).entrySet().stream() //
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> outputType.cast(entry.getValue())));
    }

    /**
     * @param functions an array of {@link ExpressionFunction}
     * @return a map that maps from the name to the function
     */
    public static Map<String, ExpressionFunction> functionsMappingFromArray(final ExpressionFunction[] functions) {
        return Arrays.stream(functions).collect(Collectors.toMap(ExpressionFunction::name, f -> f));
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @return a consumer that checks that the argument computer evaluates to MISSING
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc) {
        return c -> {
            assertInstanceOf(Computer.class, c, computerDesc + " should eval to Computer");
            try {
                assertTrue(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should be missing");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (BOOLEAN)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final boolean expected) {
        return c -> {
            assertInstanceOf(BooleanComputer.class, c, computerDesc + " should eval to BOOLEAN");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((BooleanComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (INTEGER)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final long expected) {
        return c -> {
            assertInstanceOf(IntegerComputer.class, c, computerDesc + " should eval to INTEGER");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((IntegerComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (FLOAT)
     * @param tolerance absolute tolerance for floating point values
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final double expected,
        final double tolerance) {
        return c -> {
            assertInstanceOf(FloatComputer.class, c, computerDesc + " should eval to FLOAT");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((FloatComputer)c).compute(DUMMY_EVAL_CTX), tolerance,
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (FLOAT)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final double expected) {
        return computerResultChecker(computerDesc, expected, 0);
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (STRING)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final String expected) {
        return c -> {
            assertInstanceOf(StringComputer.class, c, computerDesc + " should eval to STRING");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((StringComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (DURATION)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final Duration expected) {
        return c -> {
            assertInstanceOf(TimeDurationComputer.class, c, computerDesc + " should eval to TIME_DURATION");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((TimeDurationComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (PERIOD)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final Period expected) {
        return c -> {
            assertInstanceOf(DateDurationComputer.class, c, computerDesc + " should eval to DATE_DURATION");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((DateDurationComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (LOCAL_DATE)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final LocalDate expected) {
        return c -> {
            assertInstanceOf(LocalDateComputer.class, c, computerDesc + " should eval to LOCAL_DATE");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((LocalDateComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (LOCAL_TIME)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final LocalTime expected) {
        return c -> {
            assertInstanceOf(LocalTimeComputer.class, c, computerDesc + " should eval to LOCAL_TIME");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((LocalTimeComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (LOCAL_DATE_TIME)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final LocalDateTime expected) {
        return c -> {
            assertInstanceOf(LocalDateTimeComputer.class, c, computerDesc + " should eval to LOCAL_DATE_TIME");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((LocalDateTimeComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @param expected the expected value (ZONED_DATE_TIME)
     * @return a consumer that checks that the argument computer evaluates to the expected value
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc, final ZonedDateTime expected) {
        return c -> {
            assertInstanceOf(ZonedDateTimeComputer.class, c, computerDesc + " should eval to ZONED_DATE_TIME");
            try {
                assertFalse(c.isMissing(DUMMY_EVAL_CTX), computerDesc + " should not be missing");
                assertEquals(expected, ((ZonedDateTimeComputer)c).compute(DUMMY_EVAL_CTX),
                    computerDesc + " should eval correctly");
            } catch (ExpressionEvaluationException ex) {
                // SONAR wants to add exec to signature but this would require special interface instead of Consumer
                fail("unexpected evaluation error", ex); // NOSONAR
            }
        };
    }
}
