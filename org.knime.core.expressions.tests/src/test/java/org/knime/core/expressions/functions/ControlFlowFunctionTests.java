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
 *   Apr 24, 2024 (tobias): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_BOOLEAN;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.mis;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misBoolean;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misInteger;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misString;

import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests for {@link ControlFlowFunctions}.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class ControlFlowFunctionTests {

    /** A large long value for which <code>(double)(LARGE_NUMBER) != LARGE_NUMBER</code> */
    private static final long LARGE_NUMBER = 9007199254740995L;

    @TestFactory
    List<DynamicNode> ifFunction() {
        return new FunctionTestBuilder(ControlFlowFunctions.IF) //
            .typing("integer", List.of(BOOLEAN, INTEGER, INTEGER), INTEGER) //
            .typing("integer/float mix", List.of(BOOLEAN, INTEGER, FLOAT), FLOAT) //
            .typing("float/integer mix", List.of(BOOLEAN, FLOAT, INTEGER), FLOAT) //
            .typing("optional/float mix", List.of(BOOLEAN, OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .typing("float/optional mix", List.of(BOOLEAN, FLOAT, OPT_FLOAT), OPT_FLOAT) //
            .typing("integer/optional float mix", List.of(BOOLEAN, INTEGER, OPT_FLOAT), OPT_FLOAT) //
            .typing("float/optional integer mix", List.of(BOOLEAN, FLOAT, OPT_INTEGER), OPT_FLOAT) //
            .typing("string", List.of(BOOLEAN, STRING, STRING), STRING)
            .typing("boolean", List.of(BOOLEAN, BOOLEAN, BOOLEAN), BOOLEAN)
            .typing("MISSING else case", List.of(BOOLEAN, STRING, MISSING), OPT_STRING) //
            .typing("conditions can be optional (1 condition)", List.of(OPT_BOOLEAN, STRING, STRING), STRING) //
            .typing("conditions can be optional (2 conditions)", List.of(BOOLEAN, STRING, OPT_BOOLEAN, STRING, STRING),
                STRING) //
            .illegalArgs("incompatible return expressions string/float", List.of(BOOLEAN, STRING, FLOAT)) //
            .illegalArgs("incompatible return expressions string/integer", List.of(BOOLEAN, STRING, INTEGER)) //
            .illegalArgs("incompatible return expressions integer/boolean", List.of(BOOLEAN, INTEGER, BOOLEAN)) //
            .illegalArgs("too few arguments", List.of(BOOLEAN, STRING))//
            .illegalArgs("even number of arguments/ missing else case", List.of(BOOLEAN, STRING, STRING, STRING)) //
            .illegalArgs("second condition not boolean", List.of(BOOLEAN, FLOAT, FLOAT, FLOAT, FLOAT)) //
            .impl("integer", List.of(arg(true), arg(2), arg(-1)), 2) //
            .impl("float", List.of(arg(true), arg(1.4), arg(1.5)), 1.4) //
            .impl("missing integer true case", List.of(arg(false), arg(1), misInteger())) //
            .impl("missing integer false case", List.of(arg(true), misInteger(), arg(1))) //
            .impl("basic: true branch", List.of(arg(true), arg("true branch"), arg("false branch")), "true branch") //
            .impl("basic: false branch", List.of(arg(false), arg("true branch"), arg("false branch")), "false branch") //
            .impl("multi extendend: true branch",
                List.of(arg(true), arg("true branch"), arg(false), arg("false branch"), arg("else branch")),
                "true branch") //
            .impl("multi extendend: true branch (superflous false branch)",
                List.of(arg(true), arg("true branch"), arg(true), arg("false branch"), arg("else branch")),
                "true branch") //
            .impl("multi: false branch",
                List.of(arg(false), arg("true branch"), arg(true), arg("false branch"), arg("else branch")),
                "false branch") //
            .impl("multi: else branch",
                List.of(arg(false), arg("true branch"), arg(false), arg("false branch"), arg("else branch")),
                "else branch") //
            .impl("multi: many conditions",
                List.of(arg(true), arg("true branch"), arg(true), arg("false branch"), arg(true), arg("fake branch"),
                    arg(true), arg("some name branch"), arg("else branch")),
                "true branch") //
            .impl("MISSING else case", List.of(arg(true), arg("true branch"), mis()), "true branch") //
            .impl("MISSING counts as false (1 condition)",
                List.of(misBoolean(), arg("true branch"), arg("false branch")), "false branch") //
            .impl("MISSING counts as false (2 conditions)", List.of(arg(false), arg(0), misBoolean(), arg(1), arg(3)),
                3) //
            .impl("large integer must not lose precision", List.of(arg(true), arg(LARGE_NUMBER), arg(20)), LARGE_NUMBER) //
            .impl("integer cast to float", List.of(arg(true), arg(LARGE_NUMBER), arg(20.0)), (double)LARGE_NUMBER) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> switchFunction() {
        return new FunctionTestBuilder(ControlFlowFunctions.SWITCH) //
            .typing("integer", List.of(STRING, STRING, INTEGER, INTEGER), INTEGER) //
            .typing("missing default, integer", List.of(STRING, STRING, INTEGER), OPT_INTEGER) //
            .typing("integer/float mix", List.of(STRING, STRING, INTEGER, FLOAT), FLOAT) //
            .typing("float/integer mix", List.of(STRING, STRING, FLOAT, INTEGER), FLOAT) //
            .typing("optional/float mix", List.of(STRING, STRING, OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .typing("float/optional mix missing default", List.of(STRING, STRING, FLOAT, OPT_FLOAT), OPT_FLOAT) //
            .typing("integer/optional float mix", List.of(STRING, STRING, INTEGER, OPT_FLOAT), OPT_FLOAT) //
            .typing("float/optional integer mix", List.of(STRING, STRING, FLOAT, OPT_INTEGER), OPT_FLOAT) //
            .typing("string", List.of(INTEGER, INTEGER, STRING), OPT_STRING) //
            .typing("first case missing", List.of(OPT_STRING, MISSING, INTEGER), OPT_INTEGER) //
            .typing("optinal string case", List.of(OPT_STRING, STRING, INTEGER, OPT_STRING, INTEGER, INTEGER), INTEGER) //
            .typing("missing string case", List.of(OPT_STRING, STRING, INTEGER, MISSING, INTEGER, INTEGER), INTEGER) //
            .typing("MISSING case expression", List.of(OPT_STRING, STRING, MISSING, INTEGER), OPT_INTEGER) //
            .illegalArgs("no arguments", List.of())//
            .illegalArgs("incompatible return expressions", List.of(INTEGER, INTEGER, FLOAT, STRING))//
            .illegalArgs("two few Arguments", List.of(STRING, INTEGER)) //
            .illegalArgs("no float as case/value", List.of(FLOAT, FLOAT, STRING, STRING)) //
            .illegalArgs("no boolean as case/value", List.of(BOOLEAN, BOOLEAN, STRING, STRING)) //
            .illegalArgs("mixing case types", List.of(STRING, STRING, INTEGER, INTEGER, INTEGER, INTEGER)) //
            .illegalArgs("mixing first case type", List.of(STRING, INTEGER, INTEGER))
            .illegalArgs("MISSING as type of value", List.of(MISSING, STRING, INTEGER, STRING, INTEGER)) //
            .impl("integer with default",
                List.of(arg(1), arg(2), arg(false), arg(3), arg(false), arg(1), arg(true), arg(false)), true) //
            .impl("integer with default matching first case",
                List.of(arg(1), arg(1), arg(true), arg(2), arg(false), arg(false)), true) //
            .impl("integer without default", List.of(arg(1), arg(2), arg(false), arg(3), arg(false), arg(1), arg(true)),
                true) //
            .impl("string matching",
                List.of(arg("This will be matched"), arg("firstCase"), arg(false), arg("secondCase"), arg(false),
                    arg("This will be matched"), arg(true), arg(false)),
                true) //
            .impl("string not matching; with default case",
                List.of(arg("This wont be matched"), arg("firstCase"), arg(false), arg("secondCase"), arg(false),
                    arg("thirdCase"), arg(false), arg(true)),
                true) //
            .impl("string not matching; without default case -> result missing",
                List.of(arg("This wont be matched"), arg("firstCase"), arg(false), arg("secondCase"), arg(false),
                    arg("thirdCase"), arg(false))) //
            .impl("optional string case #1", List.of(misString(), arg("a"), arg(1), arg("b"), arg(2), arg(3)), 3) //
            .impl("optional string case #2", List.of(arg("b"), misString(), arg(1), arg("b"), arg(2), arg(3)), 2) //
            .impl("missing string case #1", List.of(misString(), arg("a"), arg(false), mis(), arg(true), arg(false)),
                true) //
            .impl("missing string case #2", List.of(arg("b"), arg("a"), arg(1), mis(), arg(2), arg(3)), 3) //
            .impl("MISSING case expression", List.of(misString(), arg("a"), arg("column is a"), mis())) //
            .impl("large integer must not lose precision",
                List.of(arg("a"), arg("a"), arg(LARGE_NUMBER), arg("b"), arg(20)), LARGE_NUMBER) //
            .impl("integer cast to float", List.of(arg("a"), arg("a"), arg(LARGE_NUMBER), arg("b"), arg(20.0)),
                (double)LARGE_NUMBER) //
            .tests();
    }
}