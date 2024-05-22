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
package org.knime.core.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.expressions.OperatorDescription.Argument.matchSignature;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.expressions.OperatorDescription.Argument;

/**
 * Tests for the {@link OperatorDescription} class.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class OperatorDescriptionTest {

    // NB: Only the names of the arguments are relevant for this test
    private static final Argument ARG_1 = new OperatorDescription.Argument("arg1", "", "");

    private static final Argument ARG_2 = new OperatorDescription.Argument("arg2", "", "");

    @Test
    void testMatchWithAllPositionalArguments() {
        var result = matchSignature( //
            List.of(ARG_1, ARG_2), //
            new Arguments<>(List.of("value1", "value2"), Map.of()) //
        );

        assertTrue(result.isPresent(), "Expected result to be present");
        assertEquals("value1", result.get().get("arg1"), "Expected 'arg1' to be 'value1'");
        assertEquals("value2", result.get().get("arg2"), "Expected 'arg2' to be 'value2'");
    }

    @Test
    void testMatchWithAllNamedArguments() {
        var result = matchSignature( //
            List.of(ARG_1, ARG_2), //
            new Arguments<>(List.of(), Map.of("arg1", "value1", "arg2", "value2")) //
        );

        assertTrue(result.isPresent(), "Expected result to be present");
        assertEquals("value1", result.get().get("arg1"), "Expected 'arg1' to be 'value1'");
        assertEquals("value2", result.get().get("arg2"), "Expected 'arg2' to be 'value2'");
    }

    @Test
    void testMatchWithMixedArguments() {
        var result = matchSignature( //
            List.of(ARG_1, ARG_2), //
            new Arguments<>(List.of("value1"), Map.of("arg2", "value2")) //
        );

        assertTrue(result.isPresent(), "Expected result to be present");
        assertEquals("value1", result.get().get("arg1"), "Expected 'arg1' to be 'value1'");
        assertEquals("value2", result.get().get("arg2"), "Expected 'arg2' to be 'value2'");
    }

    @Test
    void testMatchWithExtraPositionalArguments() {
        var result = matchSignature( //
            List.of(ARG_1), //
            new Arguments<>(List.of("value1", "value2"), Map.of()) //
        );

        assertFalse(result.isPresent(), "Expected result to be empty due to extra positional arguments");
    }

    @Test
    void testMatchWithExtraNamedArguments() {
        var result = matchSignature( //
            List.of(ARG_1), //
            new Arguments<>(List.of(), Map.of("arg1", "value1", "arg2", "value2")) //
        );

        assertFalse(result.isPresent(), "Expected result to be empty due to extra named arguments");
    }

    @Test
    void testMatchWithDuplicateNamedArguments() {
        var result = matchSignature( //
            List.of(ARG_1), //
            new Arguments<>(List.of("value1"), Map.of("arg1", "value2")) //
        );

        assertFalse(result.isPresent(), "Expected result to be empty due to duplicate named arguments");
    }

    @Test
    void testMatchWithEmptyArguments() {
        var result = matchSignature( //
            List.of(ARG_1), //
            new Arguments<>(List.of(), Map.of()) //
        );

        assertTrue(result.isPresent(), "Expected result to be present for empty arguments");
        assertTrue(result.get().isEmpty(), "Expected result map to be empty for empty arguments");
    }

    @Test
    void testMatchWithEmptySignature() {
        var result = matchSignature( //
            List.of(), //
            new Arguments<>(List.of("value1"), Map.of("arg1", "value2")) //
        );

        assertFalse(result.isPresent(), "Expected result to be empty due to empty signature");
    }
}
