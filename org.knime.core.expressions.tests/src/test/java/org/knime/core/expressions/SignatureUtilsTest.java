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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.matchSignature;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.SignatureUtils.vararg;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.core.expressions.SignatureUtils.Arg;

/**
 * Tests for the {@link SignatureUtils} class.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class SignatureUtilsTest {

    private static final String ARG_NAME_INTEGER = "arg1";

    private static final String ARG_NAME_STRING = "arg2";

    private static final String OPT_ARG_NAME_FLOAT = "opt_arg1";

    private static final String OPT_ARG_NAME_STRING = "opt_arg2";

    private static final String VAR_ARG_NAME = "var_arg";

    private static final Arg ARG_INTEGER = arg(ARG_NAME_INTEGER, "", SignatureUtils.isInteger());

    private static final Arg ARG_STRING = arg(ARG_NAME_STRING, "", SignatureUtils.isString());

    private static final Arg OPT_ARG_FLOAT = optarg(OPT_ARG_NAME_FLOAT, "", SignatureUtils.isFloat());

    private static final Arg OPT_ARG_STRING = optarg(OPT_ARG_NAME_STRING, "", SignatureUtils.isString());

    private static final Arg VAR_ARG_BOOLEAN = vararg(VAR_ARG_NAME, "", SignatureUtils.isBoolean());

    @ParameterizedTest
    @EnumSource(SingatureMatchCases.class)
    void testMatchSignature(final SingatureMatchCases params) {
        var result = matchSignature(params.m_signature, params.m_positionalArgs, params.m_namedArgs);
        assertTrue(result.isOk(), () -> "Expected result to be present but is " + result.getErrorMessage());
        assertEquals(params.m_expected, result.getValue(), "Expected arguments to match");
    }

    private enum SingatureMatchCases {
            EMPTY_SIGNATURE( //
                List.of(), //
                List.of(), Map.of(), //
                new Arguments<>(Map.of(), List.of()) //
            ), //
            TWO_REQUIRED_ONLY_POS( //
                List.of(ARG_INTEGER, ARG_STRING), //
                List.of("value1", "value2"), Map.of(), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1", ARG_NAME_STRING, "value2"), List.of()) //
            ), //
            TWO_REQUIRED_ONLY_NAMED( //
                List.of(ARG_INTEGER, ARG_STRING), //
                List.of(), Map.of(ARG_NAME_INTEGER, "value1", ARG_NAME_STRING, "value2"), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1", ARG_NAME_STRING, "value2"), List.of()) //
            ), //
            TWO_REQUIRED_ONE_POS_ONE_NAMED( //
                List.of(ARG_INTEGER, ARG_STRING), //
                List.of("value1"), Map.of(ARG_NAME_STRING, "value2"), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1", ARG_NAME_STRING, "value2"), List.of()) //
            ), //
            OPTIONAL_ARGUMENT_OMITTED( //
                List.of(OPT_ARG_FLOAT), //
                List.of(), Map.of(), //
                new Arguments<>(Map.of(), List.of()) //
            ), //
            OPTIONAL_ARGUMENT_PROVIDED_AS_POS( //
                List.of(OPT_ARG_FLOAT), //
                List.of("value_opt"), Map.of(), //
                new Arguments<>(Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), List.of()) //
            ), //
            OPTIONAL_ARGUMENT_PROVIDED_AS_NAMED( //
                List.of(OPT_ARG_FLOAT), //
                List.of(), Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), //
                new Arguments<>(Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), List.of()) //
            ), //
            REQUIRED_AND_OPTIONAL_PROVIDED( //
                List.of(ARG_INTEGER, OPT_ARG_FLOAT), //
                List.of("value1"), Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1", OPT_ARG_NAME_FLOAT, "value_opt"), List.of()) //
            ), //
            REQUIRED_AND_OPTIONAL_OMITTED( //
                List.of(ARG_INTEGER, OPT_ARG_FLOAT), //
                List.of("value1"), Map.of(), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1"), List.of()) //
            ), //
            REQUIRED_AND_VAR_PROVIDED( //
                List.of(ARG_INTEGER, VAR_ARG_BOOLEAN), //
                List.of("value1", "value_var1", "value_var2"), Map.of(), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1"), List.of("value_var1", "value_var2")) //
            ), //
            REQUIRED_OPTIONAL_AND_VAR_PROVIDED( //
                List.of(ARG_INTEGER, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT), //
                List.of("value1", "value_var1", "value_var2"), Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1", OPT_ARG_NAME_FLOAT, "value_opt"),
                    List.of("value_var1", "value_var2")) //
            ), //
            REQUIRED_OPTIONAL_AND_VAR_ONLY_REQUIRED_PROVIDED( //
                List.of(ARG_INTEGER, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT), //
                List.of("value1"), Map.of(), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1"), List.of()) //
            ), //
            REQUIRED_OPTIONAL_AND_VAR_VAR_OMITTED( //
                List.of(ARG_INTEGER, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT), //
                List.of("value1"), Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1", OPT_ARG_NAME_FLOAT, "value_opt"), List.of()) //
            ), //
            REQUIRED_OPTIONAL_AND_VAR_OPT_OMITTED( //
                List.of(ARG_INTEGER, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT), //
                List.of("value1", "var_val1", "var_val2", "var_val3"), Map.of(), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, "value1"), List.of("var_val1", "var_val2", "var_val3")) //
            ), //
            ONLY_VAR_PROVIDED( //
                List.of(VAR_ARG_BOOLEAN), //
                List.of("value_var1", "value_var2"), Map.of(), //
                new Arguments<>(Map.of(), List.of("value_var1", "value_var2")) //
            ), //
            ALL_ARG_TYPES_PROVIDED( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT, OPT_ARG_STRING), //
                List.of("value1", "value2", "value_var1"), Map.of(OPT_ARG_NAME_FLOAT, "value_opt"), //
                new Arguments<>(
                    Map.of(ARG_NAME_INTEGER, "value1", ARG_NAME_STRING, "value2", OPT_ARG_NAME_FLOAT, "value_opt"),
                    List.of("value_var1")) //
            ), //
            OPTIONAL_AND_VAR_NO_ARG_PROVIDED( //
                List.of(OPT_ARG_FLOAT, VAR_ARG_BOOLEAN), //
                List.of(), Map.of(), //
                new Arguments<>(Map.of(), List.of()) //
            ), //
        ;

        private final List<Arg> m_signature;

        private final List<String> m_positionalArgs;

        private final Map<String, String> m_namedArgs;

        private final Arguments<String> m_expected;

        SingatureMatchCases(final List<Arg> signature, final List<String> positionalArgs,
            final Map<String, String> namedArgs, final Arguments<String> expected) {
            m_signature = signature;
            m_positionalArgs = positionalArgs;
            m_namedArgs = namedArgs;
            m_expected = expected;

        }
    }

    @ParameterizedTest
    @EnumSource(SingatureMatchFailureCases.class)
    void testMatchSignatureFailures(final SingatureMatchFailureCases params) {
        var result = matchSignature(params.m_signauture, params.m_positionalArgs, params.m_namedArgs);
        assertTrue(result.isError(), "Expected result to be empty");
        var message = result.getErrorMessage();
        for (int i = 0; i < params.m_expectedErrorHints.length; i++) {
            var expectedHint = params.m_expectedErrorHints[i];
            assertTrue(message.contains(expectedHint), () -> "Expected error message to match. " //
                + "Expected: " + expectedHint + ", got: " + message);
        }
    }

    private enum SingatureMatchFailureCases {
            EMPTY_SIGNATURE_POS_ARG_GIVEN( //
                List.of(), //
                List.of("value1"), Map.of(), //
                "Too many arguments", "0 arguments" //
            ), //
            EMPTY_SIGNATURE_NAMED_ARG_GIVEN( //
                List.of(), //
                List.of(), Map.of(ARG_NAME_INTEGER, "value1"), //
                "No argument", ARG_NAME_INTEGER //
            ), //
            MISSING_REQUIRED_ARG( //
                List.of(ARG_INTEGER, ARG_STRING), //
                List.of("value1"), Map.of(), //
                "Missing required argument", ARG_NAME_STRING), //
            MISSING_REQUIRED_ARG_EMPTY( //
                List.of(ARG_INTEGER), //
                List.of(), Map.of(), //
                "Missing required argument", ARG_NAME_INTEGER), //
            UNKNOWN_POS_ARG( //
                List.of(ARG_INTEGER), //
                List.of("value1", "value2"), Map.of(), //
                "Too many arguments", "1 argument" //
            ), //
            UNKOWN_NAMED_ARGS( //
                List.of(ARG_INTEGER), //
                List.of(), Map.of(ARG_NAME_INTEGER, "value1", ARG_NAME_STRING, "value2"), //
                "No argument with identifier", ARG_NAME_STRING //
            ), //
            NAMED_ARG_FOR_ALREADY_MATCHED_ARG( //
                List.of(ARG_INTEGER), //
                List.of("value1"), Map.of(ARG_NAME_INTEGER, "value2"), //
                ARG_NAME_INTEGER, "was provided twice" //
            ), //
            EXTRA_POSITIONAL_ARGUMENT( //
                List.of(ARG_INTEGER, OPT_ARG_FLOAT), //
                List.of("value1", "value2", "extra_value"), Map.of(), //
                "Too many arguments", "2 arguments" //
            ), //
            UNKNOWN_NAMED_ARG( //
                List.of(ARG_INTEGER, OPT_ARG_FLOAT), //
                List.of("value1"), Map.of("unknown_arg", "value_unknown"), //
                "No argument with identifier 'unknown_arg'" //
            ), //
        ;

        private final List<Arg> m_signauture;

        private final List<String> m_positionalArgs;

        private final Map<String, String> m_namedArgs;

        private final String[] m_expectedErrorHints;

        SingatureMatchFailureCases(final List<Arg> signauture, final List<String> positionalArgs,
            final Map<String, String> namedArgs, final String... expectedErrorHints) {
            m_signauture = signauture;
            m_positionalArgs = positionalArgs;
            m_namedArgs = namedArgs;
            m_expectedErrorHints = expectedErrorHints;

        }
    }

    @ParameterizedTest
    @EnumSource(ValidSignatureCases.class)
    void testCheckSignatures(final ValidSignatureCases testCase) {
        assertDoesNotThrow(() -> SignatureUtils.checkSignature(testCase.m_signature),
            "Expected signature to be valid.");
    }

    enum ValidSignatureCases {
            REQUIRED_ONLY(List.of(ARG_INTEGER, ARG_STRING)), //
            REQUIRED_AND_OPTIONAL(List.of(ARG_INTEGER, ARG_STRING, OPT_ARG_FLOAT)), //
            REQUIRED_VAR_AND_OPTIONAL(List.of(ARG_INTEGER, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT)), //
            REQUIRED_AND_VAR(List.of(ARG_INTEGER, VAR_ARG_BOOLEAN)), //
            NO_ARGS(List.of()), //
            OPTIONAL_ONLY(List.of(OPT_ARG_FLOAT)), //
            VAR_AND_OPTIONAL(List.of(VAR_ARG_BOOLEAN, OPT_ARG_FLOAT)), //
        ;

        private final List<Arg> m_signature;

        private ValidSignatureCases(final List<Arg> signature) {
            m_signature = signature;
        }
    }

    @ParameterizedTest
    @EnumSource(InvalidSignatureCases.class)
    void testCheckInvalidSignatures(final InvalidSignatureCases testCase) {
        var thrown =
            assertThrows(IllegalArgumentException.class, () -> SignatureUtils.checkSignature(testCase.m_signature),
                "Expected invalid signature to throw exception.");
        assertEquals(testCase.m_expectedMessage, thrown.getMessage(), "Expected exception message to match.");
    }

    enum InvalidSignatureCases {
            OPTIONAL_BEFORE_VAR(List.of(ARG_INTEGER, OPT_ARG_FLOAT, VAR_ARG_BOOLEAN),
                "Optional arguments must be at the end."), //
            REQUIRED_AFTER_OPTIONAL(List.of(OPT_ARG_FLOAT, ARG_INTEGER),
                "All required arguments must be at the beginning."), //
            REQUIRED_AFTER_VAR(List.of(VAR_ARG_BOOLEAN, ARG_INTEGER),
                "All required arguments must be at the beginning."), //
            MULTIPLE_VAR_ARGS(List.of(ARG_INTEGER, VAR_ARG_BOOLEAN, VAR_ARG_BOOLEAN),
                "Only one variadic argument is allowed."), //
        ;

        private final List<Arg> m_signature;

        private final String m_expectedMessage;

        InvalidSignatureCases(final List<Arg> signature, final String expectedMessage) {
            m_signature = signature;
            m_expectedMessage = expectedMessage;
        }
    }

    @ParameterizedTest
    @EnumSource(CheckTypesCases.class)
    void testCheckTypes(final CheckTypesCases testCase) {
        var result = SignatureUtils.checkTypes(testCase.m_signature, testCase.m_arguments);

        if (testCase.m_expectedErrorMessage == null) {
            assertTrue(result.isOk(), () -> "Expected success but got error: " + result.getErrorMessage());
        } else {
            assertTrue(result.isError(), () -> "Expected error but got success.");
            assertEquals(testCase.m_expectedErrorMessage, result.getErrorMessage(), //
                "Expected error message to match.");
        }
    }

    enum CheckTypesCases {
            MATCH_ALL_NAMED_ARGS( //
                List.of(ARG_INTEGER, ARG_STRING), //
                new Arguments<>(Map.of(ARG_NAME_INTEGER, ValueType.INTEGER, ARG_NAME_STRING, ValueType.STRING),
                    List.of()), //
                null //
            ), //
            MATCH_NAMED_AND_VAR_ARGS( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN), //
                new Arguments<>(Map.of( //
                    ARG_NAME_INTEGER, ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), List.of(ValueType.BOOLEAN, ValueType.BOOLEAN)), //
                null //
            ), //
            MISMATCHED_NAMED_ARG_TYPE( //
                List.of(ARG_INTEGER, ARG_STRING), //
                new Arguments<>(Map.of( //
                    ARG_NAME_INTEGER, ValueType.FLOAT, //
                    ARG_NAME_STRING, ValueType.STRING), //
                    List.of()), //
                "Argument (1, '" + ARG_NAME_INTEGER + "') is not of the expected type: INTEGER but got FLOAT." //
            ), //
            MISMATCHED_VAR_ARG_TYPE( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN), //
                new Arguments<>(Map.of( //
                    ARG_NAME_INTEGER, ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), //
                    List.of(ValueType.BOOLEAN, ValueType.STRING)), //
                "Argument (4, 'variableArguments') is not of the expected type: BOOLEAN but got STRING." //
            ), //
            MISMATCHED_OPT_ARG_AFTER_MISSING_VARIADIC( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT, OPT_ARG_STRING), //
                new Arguments<>(Map.of( //
                    OPT_ARG_NAME_FLOAT, ValueType.INTEGER, //
                    OPT_ARG_NAME_STRING, ValueType.STRING, //
                    ARG_NAME_INTEGER, ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), List.of()), //
                "Argument (3, '" + OPT_ARG_NAME_FLOAT + "') is not of the expected type: FLOAT but got INTEGER." //
            ), //
            MISMATCHED_OPT_ARG_AFTER_MANY_VARIADIC( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT, OPT_ARG_STRING), //
                new Arguments<>(Map.of( //
                    OPT_ARG_NAME_FLOAT, ValueType.INTEGER, //
                    OPT_ARG_NAME_STRING, ValueType.STRING, //
                    ARG_NAME_INTEGER, ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), //
                    List.of(ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN)), //
                "Argument (6, '" + OPT_ARG_NAME_FLOAT + "') is not of the expected type: FLOAT but got INTEGER." //
            ), //
            MISMATCHED_VAR_ARG_BUT_OPT_ARG_ERRS_FIRST( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT, OPT_ARG_STRING), //
                new Arguments<>(Map.of( //
                    OPT_ARG_NAME_FLOAT, ValueType.INTEGER, //
                    OPT_ARG_NAME_STRING, ValueType.STRING, //
                    ARG_NAME_INTEGER, ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), //
                    List.of(ValueType.BOOLEAN, ValueType.STRING, ValueType.BOOLEAN)), //
                "Argument (6, '" + OPT_ARG_NAME_FLOAT + "') is not of the expected type: FLOAT but got INTEGER." //
            ), //
            MISMATCHED_VAR_ARG_OPT_ARG_CORRECT( //
                List.of(ARG_INTEGER, ARG_STRING, VAR_ARG_BOOLEAN, OPT_ARG_FLOAT, OPT_ARG_STRING), //
                new Arguments<>(Map.of( //
                    OPT_ARG_NAME_FLOAT, ValueType.FLOAT, //
                    OPT_ARG_NAME_STRING, ValueType.STRING, //
                    ARG_NAME_INTEGER, ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), //
                    List.of(ValueType.BOOLEAN, ValueType.STRING, ValueType.BOOLEAN)), //
                "Argument (4, 'variableArguments') is not of the expected type: BOOLEAN but got STRING." //
            ), //
            UNKNOWN_NAMED_ARG( //
                List.of(ARG_INTEGER, ARG_STRING), //
                new Arguments<>(Map.of( //
                    "unknown_arg", ValueType.INTEGER, //
                    ARG_NAME_STRING, ValueType.STRING), //
                    List.of()), //
                "Argument not found: unknown_arg" //
            ), //
        ;

        final List<Arg> m_signature;

        final Arguments<ValueType> m_arguments;

        final String m_expectedErrorMessage;

        CheckTypesCases(final List<Arg> signature, final Arguments<ValueType> arguments,
            final String expectedErrorMessage) {
            m_signature = signature;
            m_arguments = arguments;
            m_expectedErrorMessage = expectedErrorMessage;
        }
    }
}
