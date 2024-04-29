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

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin Germany
 */
public final class TestUtils {

    // Listener that just ignores any warnings
    private static final WarningMessageListener DUMMY_WML = w -> {
    };

    private TestUtils() {
    }

    /**
     * @param functions an array of {@link ExpressionFunction}
     * @return a function that maps from the name to the function
     */
    public static Function<String, Optional<ExpressionFunction>>
        functionsMappingFromArray(final ExpressionFunction[] functions) {
        var map = Arrays.stream(functions).collect(Collectors.toMap(ExpressionFunction::name, f -> f));
        return name -> Optional.ofNullable(map.get(name));
    }

    /**
     * @param computerDesc a string about what the computer represents for assertion messages
     * @return a consumer that checks that the argument computer evaluates to MISSING
     */
    public static Consumer<Computer> computerResultChecker(final String computerDesc) {
        return c -> {
            assertInstanceOf(Computer.class, c, computerDesc + " should eval to Computer");
            assertTrue(c.isMissing(DUMMY_WML), computerDesc + " should be missing");
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
            assertFalse(c.isMissing(DUMMY_WML), computerDesc + " should not be missing");
            assertEquals(expected, ((BooleanComputer)c).compute(DUMMY_WML), computerDesc + " should eval correctly");
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
            assertFalse(c.isMissing(DUMMY_WML), computerDesc + " should not be missing");
            assertEquals(expected, ((IntegerComputer)c).compute(DUMMY_WML), computerDesc + " should eval correctly");
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
            assertFalse(c.isMissing(DUMMY_WML), computerDesc + " should not be missing");
            assertEquals(expected, ((FloatComputer)c).compute(DUMMY_WML), tolerance,
                computerDesc + " should eval correctly");
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
            assertFalse(c.isMissing(DUMMY_WML), computerDesc + " should not be missing");
            assertEquals(expected, ((StringComputer)c).compute(DUMMY_WML), computerDesc + " should eval correctly");
        };
    }
}
