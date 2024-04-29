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

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misFloat;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misInteger;

import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests for {@link MathFunctions}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class MathFunctionTests {

    @TestFactory
    List<DynamicNode> max() {
        return new FunctionTestBuilder(MathFunctions.MAX) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .impl("INTEGER", List.of(arg(1), arg(2), arg(-1)), 2) //
            .impl("FLOAT", List.of(arg(1.2), arg(1.4), arg(1.5)), 1.5) //
            .impl("missing INTEGER", List.of(misInteger(), arg(1))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(1.5))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> min() {
        return new FunctionTestBuilder(MathFunctions.MIN) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .impl("INTEGER", List.of(arg(1), arg(2), arg(-1)), -1) //
            .impl("FLOAT", List.of(arg(1.2), arg(1.4), arg(1.5)), 1.2) //
            .impl("missing INTEGER", List.of(misInteger(), arg(1))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(1.5))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> argmax() {
        return new FunctionTestBuilder(MathFunctions.ARGMAX) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_INTEGER) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .impl("INTEGER", List.of(arg(1), arg(2), arg(-1)), 2) //
            .impl("FLOAT", List.of(arg(1.2), arg(1.4), arg(1.5)), 3) //
            .impl("multiple maxes", List.of(arg(1), arg(2), arg(2)), 2)
            .impl("multiple maxes (float)", List.of(arg(1.0), arg(2), arg(2)), 2) //
            .impl("missing INTEGER", List.of(misInteger(), arg(0))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(0.0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> argmin() {
        return new FunctionTestBuilder(MathFunctions.ARGMIN) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_INTEGER) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .impl("INTEGER", List.of(arg(1), arg(2), arg(-1)), 3) //
            .impl("FLOAT", List.of(arg(1.2), arg(1.4), arg(1.5)), 1) //
            .impl("multiple mins", List.of(arg(2), arg(1), arg(1)), 2) //
            .impl("multiple mins (float)", List.of(arg(2.0), arg(1), arg(1)), 2) //
            .impl("missing INTEGER", List.of(misInteger(), arg(0))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(0.0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> abs() {
        return new FunctionTestBuilder(MathFunctions.ABS) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(1)), Math.abs(1)) //
            .impl("FLOAT", List.of(arg(-1.2)), Math.abs(-1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> sin() {
        return new FunctionTestBuilder(MathFunctions.SIN) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.sin(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.sin(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> cos() {
        return new FunctionTestBuilder(MathFunctions.COS) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.cos(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.cos(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> tan() {
        return new FunctionTestBuilder(MathFunctions.TAN) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.tan(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.tan(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> asin() {
        return new FunctionTestBuilder(MathFunctions.ASIN) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.asin(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.asin(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("< -1", List.of(arg(-2))) //
            .warns("> 1", List.of(arg(1.001))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> acos() {
        return new FunctionTestBuilder(MathFunctions.ACOS) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.acos(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(0.7)), Math.acos(0.7)) //
            .impl("NaN", List.of(arg(1.5)), Float.NaN) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("< -1", List.of(arg(-2))) //
            .warns("> 1", List.of(arg(1.001))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> atan() {
        return new FunctionTestBuilder(MathFunctions.ATAN) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.atan(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.atan(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> atan2() {
        return new FunctionTestBuilder(MathFunctions.ATAN2) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("TOO MANY FLOATS", List.of(FLOAT, FLOAT, FLOAT)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(27), arg(3)), Math.atan2(27, 3)) //
            .implWithTolerance("FLOAT", List.of(arg(5.2), arg(1.8)), Math.atan2(5.2, 1.8)) //
            .impl("missing INTEGER", List.of(misInteger(), arg(3))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(1.5))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> sinh() {
        return new FunctionTestBuilder(MathFunctions.SINH) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.sinh(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.sinh(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> cosh() {
        return new FunctionTestBuilder(MathFunctions.COSH) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.cosh(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.cosh(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("< 1", List.of(arg(0.9))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> tanh() {
        return new FunctionTestBuilder(MathFunctions.TANH) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.tanh(1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.tanh(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> asinh() {
        return new FunctionTestBuilder(MathFunctions.ASINH) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.log(Math.sqrt(1.0 * 1.0 + 1) + 1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.log(Math.sqrt(1.2 * 1.2 + 1) + 1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> acosh() {
        return new FunctionTestBuilder(MathFunctions.ACOSH) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(3)), Math.log(3.0 + Math.sqrt(3.0 - 1) * Math.sqrt(3.0 + 1))) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.log(1.2 + Math.sqrt(1.2 - 1) * Math.sqrt(1.2 + 1))) //
            .impl("NaN", List.of(arg(0.7)), Float.NaN) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> atanh() {
        return new FunctionTestBuilder(MathFunctions.ATANH) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), 0.5 * Math.log(1.0 + 1) - 0.5 * Math.log(1 - 1.0)) //
            .implWithTolerance("FLOAT", List.of(arg(0.7)), 0.5 * Math.log(0.7 + 1) - 0.5 * Math.log(1 - 0.7)) //
            .impl("NaN", List.of(arg(1.2)), Float.NaN) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("< -1", List.of(arg(-2))) //
            .warns("> 1", List.of(arg(1.001))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> ln() {
        return new FunctionTestBuilder(MathFunctions.LN) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.log(1)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.log(1.2)) //
            .impl("Negative infinity", List.of(arg(0)), Float.NEGATIVE_INFINITY) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("<= 0", List.of(arg(0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> log10() {
        return new FunctionTestBuilder(MathFunctions.LOG10) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.log10(1)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.log10(1.2)) //
            .impl("Negative infinity", List.of(arg(0)), Float.NEGATIVE_INFINITY) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("<= 0", List.of(arg(0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> log2() {
        return new FunctionTestBuilder(MathFunctions.LOG2) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.log(1) / Math.log(2)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.log(1.2) / Math.log(2)) //
            .impl("Negative infinity", List.of(arg(0)), Float.NEGATIVE_INFINITY) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("<= 0", List.of(arg(0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> logBase() {
        return new FunctionTestBuilder(MathFunctions.LOG_BASE) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("TOO MANY FLOATS", List.of(FLOAT, FLOAT, FLOAT)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(27), arg(3)), Math.log(27) / Math.log(3)) //
            .implWithTolerance("FLOAT", List.of(arg(5.2), arg(1.8)), Math.log(5.2) / Math.log(1.8)) //
            .impl("FLOAT 0 -> NaN", List.of(arg(5.2), arg(0)), Float.NaN) //
            .impl("Negative infinity", List.of(arg(0), arg(1)), Float.NEGATIVE_INFINITY) //
            .impl("missing first INTEGER", List.of(misInteger(), arg(5))) //
            .impl("missing first FLOAT", List.of(misFloat(), arg(5.2))) //
            .impl("missing second INTEGER", List.of(arg(5), misInteger())) //
            .impl("missing second FLOAT", List.of(arg(5.2), misFloat())) //
            .warns("num <= 0", List.of(arg(0), arg(2.5))) //
            .warns("base <= 0", List.of(arg(5), arg(0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> log1p() {
        return new FunctionTestBuilder(MathFunctions.LOG1P) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.log1p(1)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.log1p(1.2)) //
            .impl("Negative infinity", List.of(arg(-1)), Float.NEGATIVE_INFINITY) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("arg <= -1", List.of(arg(-1))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> exp() {
        return new FunctionTestBuilder(MathFunctions.EXP) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(1)), Math.exp(1)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.exp(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> pow() {
        return new FunctionTestBuilder(MathFunctions.POW) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("TOO MANY FLOATS", List.of(FLOAT, FLOAT, FLOAT)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(27), arg(3)), (long)Math.pow(27, 3)) //
            .implWithTolerance("FLOAT", List.of(arg(5.2), arg(1.8)), Math.pow(5.2, 1.8)) //
            .impl("missing first INTEGER", List.of(misInteger(), arg(5))) //
            .impl("missing first FLOAT", List.of(misFloat(), arg(5.2))) //
            .impl("missing second INTEGER", List.of(arg(5), misInteger())) //
            .impl("missing second FLOAT", List.of(arg(5.2), misFloat())) //
            .warns("0^0", List.of(arg(0), arg(0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> sqrt() {
        return new FunctionTestBuilder(MathFunctions.SQRT) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(17)), Math.sqrt(17)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.sqrt(1.2)) //
            .impl("Negative input", List.of(arg(-1)), Float.NaN) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .warns("num < 0", List.of(arg(-0.5))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> mod() {
        return new FunctionTestBuilder(MathFunctions.MOD) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), FLOAT) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("TOO MANY FLOATS", List.of(FLOAT, FLOAT, FLOAT)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(27), arg(4)), 27 % 4) //
            .implWithTolerance("FLOAT", List.of(arg(5.2), arg(1.8)), 5.2 % 1.8) //
            .impl("NaN", List.of(arg(5.0), arg(0.0)), Float.NaN) //
            .impl("Zero", List.of(arg(5), arg(0)), 0) //
            .impl("Negative", List.of(arg(-10), arg(3)), -1) //
            .impl("missing first INTEGER", List.of(misInteger(), arg(5))) //
            .impl("missing first FLOAT", List.of(misFloat(), arg(5.2))) //
            .impl("missing second INTEGER", List.of(arg(5), misInteger())) //
            .impl("missing second FLOAT", List.of(arg(5.2), misFloat())) //
            .warns("divisor = 0", List.of(arg(14), arg(0))) //
            .warns("divisor = 0.0", List.of(arg(14.0), arg(0.0))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> degrees() {
        return new FunctionTestBuilder(MathFunctions.DEGREES) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(17)), Math.toDegrees(17)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.toDegrees(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> radians() {
        return new FunctionTestBuilder(MathFunctions.RADIANS) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .implWithTolerance("INTEGER", List.of(arg(17)), Math.toRadians(17)) //
            .implWithTolerance("FLOAT", List.of(arg(1.2)), Math.toRadians(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> floor() {
        return new FunctionTestBuilder(MathFunctions.FLOOR) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 17) //
            .impl("FLOAT", List.of(arg(1.9)), 1) //
            .impl("NEG FLOAT", List.of(arg(-1.9)), -2) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> ceil() {
        return new FunctionTestBuilder(MathFunctions.CEIL) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 17) //
            .impl("FLOAT", List.of(arg(1.9)), 2) //
            .impl("NEG FLOAT", List.of(arg(-1.9)), -1) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> trunc() {
        return new FunctionTestBuilder(MathFunctions.TRUNC) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 17) //
            .impl("FLOAT", List.of(arg(1.9)), 1) //
            .impl("NEG FLOAT", List.of(arg(-1.9)), -1) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> roundhalfdown() {
        return new FunctionTestBuilder(MathFunctions.ROUNDHALFDOWN) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, INTEGER), OPT_FLOAT) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, FLOAT", List.of(INTEGER, FLOAT)) //
            .illegalArgs("INTEGER, INTEGER, INTEGER", List.of(INTEGER, INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 17) //
            .impl("FLOAT", List.of(arg(1.9)), 2) //
            .impl("NEG FLOAT", List.of(arg(-1.9)), -2) //
            .impl("HALFWAY", List.of(arg(5.5)), 5) //
            .impl("HALFWAY + NEGATIVE", List.of(arg(-5.5)), -5) //
            .impl("INTEGER + PRECISION=0", List.of(arg(17), arg(0)), 17.0) //
            .impl("FLOAT + PRECISION=0", List.of(arg(1.9), arg(0)), 2.0) //
            .impl("NEG FLOAT + PRECISION=0", List.of(arg(-1.9), arg(0)), -2.0) //
            .impl("HALFWAY + PRECISION=0", List.of(arg(-1.5), arg(0)), -1.0) //
            .impl("INTEGER + PRECISION=1", List.of(arg(17), arg(1)), 17.0) //
            .impl("FLOAT + PRECISION=1", List.of(arg(1.93), arg(1)), 1.90) //
            .impl("NEG FLOAT + PRECISION=1", List.of(arg(-1.91), arg(1)), -1.9) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> roundhalfUp() {
        return new FunctionTestBuilder(MathFunctions.ROUNDHALFUP) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, INTEGER), OPT_FLOAT) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, FLOAT", List.of(INTEGER, FLOAT)) //
            .illegalArgs("INTEGER, INTEGER, INTEGER", List.of(INTEGER, INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 17) //
            .impl("FLOAT", List.of(arg(1.9)), 2) //
            .impl("NEG FLOAT", List.of(arg(-1.9)), -2) //
            .impl("HALFWAY", List.of(arg(5.5)), 6) //
            .impl("HALFWAY + NEGATIVE", List.of(arg(-5.5)), -6) //
            .impl("INTEGER + PRECISION=0", List.of(arg(17), arg(0)), 17.0) //
            .impl("FLOAT + PRECISION=0", List.of(arg(1.9), arg(0)), 2.0) //
            .impl("NEG FLOAT + PRECISION=0", List.of(arg(-1.5), arg(0)), -2.0) //
            .impl("HALFWAY + PRECISION=0", List.of(arg(-1.5), arg(0)), -2.0) //
            .impl("INTEGER + PRECISION=1", List.of(arg(17), arg(1)), 17.0) //
            .impl("FLOAT + PRECISION=1", List.of(arg(1.93), arg(1)), 1.90) //
            .impl("NEG FLOAT + PRECISION=1", List.of(arg(-1.91), arg(1)), -1.9) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> roundhalfEven() {
        return new FunctionTestBuilder(MathFunctions.ROUNDHALFEVEN) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT, INTEGER), OPT_FLOAT) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, FLOAT", List.of(INTEGER, FLOAT)) //
            .illegalArgs("INTEGER, INTEGER, INTEGER", List.of(INTEGER, INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 17) //
            .impl("FLOAT", List.of(arg(1.9)), 2) //
            .impl("NEG FLOAT", List.of(arg(-1.9)), -2) //
            .impl("HALFWAY", List.of(arg(5.5)), 6) //
            .impl("HALFWAY + NEGATIVE", List.of(arg(-4.5)), -4) //
            .impl("INTEGER + PRECISION=0", List.of(arg(17), arg(0)), 17.0) //
            .impl("FLOAT + PRECISION=0", List.of(arg(1.9), arg(0)), 2.0) //
            .impl("NEG FLOAT + PRECISION=0", List.of(arg(-1.5), arg(0)), -2.0) //
            .impl("HALFWAY + PRECISION=0", List.of(arg(-1.5), arg(0)), -2.0) //
            .impl("INTEGER + PRECISION=1", List.of(arg(17), arg(1)), 17.0) //
            .impl("FLOAT + PRECISION=1", List.of(arg(1.93), arg(1)), 1.90) //
            .impl("NEG FLOAT + PRECISION=1", List.of(arg(-1.91), arg(1)), -1.9) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> sign() {
        return new FunctionTestBuilder(MathFunctions.SIGN) //
            .typing("INTEGER", List.of(INTEGER), INTEGER) //
            .typing("FLOAT", List.of(FLOAT), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_INTEGER) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_INTEGER) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(17)), 1) //
            .impl("FLOAT", List.of(arg(1.2)), 1) //
            .impl("NEG INTEGER", List.of(arg(-17)), -1) //
            .impl("NEG FLOAT", List.of(arg(-1.2)), -1) //
            .impl("ZERO", List.of(arg(0)), 0) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> average() {
        return new FunctionTestBuilder(MathFunctions.AVERAGE) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .implWithTolerance("INTEGER", List.of(arg(1), arg(2), arg(-1)), 2.0 / 3.0) //
            .implWithTolerance("FLOAT", List.of(arg(1.2), arg(1.4), arg(1.5)), 41.0 / 30.0) //
            .impl("missing INTEGER", List.of(misInteger(), arg(1))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(1.5))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> median() {
        return new FunctionTestBuilder(MathFunctions.MEDIAN) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT+INTEGER", List.of(FLOAT, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT, FLOAT), OPT_FLOAT) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .implWithTolerance("INTEGER ODD", List.of(arg(1), arg(2), arg(-1)), 1.0) //
            .implWithTolerance("FLOAT ODD", List.of(arg(1.2), arg(1.4), arg(1.5)), 1.4) //
            .implWithTolerance("INTEGER EVEN", List.of(arg(1), arg(2), arg(-1), arg(0)), 0.5) //
            .implWithTolerance("FLOAT EVEN", List.of(arg(1.2), arg(1.4), arg(1.5), arg(-0.2)), 1.3) //
            .impl("missing INTEGER", List.of(misInteger(), arg(1))) //
            .impl("missing FLOAT", List.of(misFloat(), arg(1.5))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> binomial() {
        return new FunctionTestBuilder(MathFunctions.BINOMIAL) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), INTEGER) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_INTEGER) //
            .illegalArgs("TOO FEW INTEGERs", List.of(INTEGER)) //
            .illegalArgs("TOO MANY INTEGERs", List.of(INTEGER, INTEGER, INTEGER)) //
            .illegalArgs("FLOATS", List.of(FLOAT, FLOAT)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("27c4", List.of(arg(27), arg(4)), 17550) //
            .impl("27c23", List.of(arg(27), arg(23)), 17550) //
            .impl("12c1", List.of(arg(12), arg(1)), 12) //
            .impl("12c11", List.of(arg(12), arg(11)), 12) //
            .impl("0c0", List.of(arg(0), arg(0)), 1) //
            .impl("8c0 (r = 0)", List.of(arg(8), arg(0)), 1) //
            .impl("8c8 (r = n)", List.of(arg(8), arg(8)), 1) //
            .impl("1c(-5) (r < 0)", List.of(arg(1), arg(-5)), 0) //
            .impl("5c100 (r > n)", List.of(arg(5), arg(100)), 0) //
            .impl("(-8)c3 (n < 0)", List.of(arg(-8), arg(3)), 0) //
            .impl("missing INTEGER", List.of(misInteger(), arg(5))) //
            .warns("n < 0", List.of(arg(-1), arg(2))) //
            .warns("r < 0", List.of(arg(5), arg(-10))) //
            .warns("n  r", List.of(arg(5), arg(6))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> normal() {
        return new FunctionTestBuilder(MathFunctions.NORMAL) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("INTEGER+FLOAT", List.of(INTEGER, FLOAT, FLOAT), FLOAT)
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .implWithTolerance("peak value", List.of(arg(0), arg(0),arg(1)), 1/(Math.sqrt(2*Math.PI))) //
            .implWithTolerance("peak value without deviation", List.of(arg(0), arg(0)), 1/(Math.sqrt(2*Math.PI))) //
            .implWithTolerance("far away", List.of(arg(0), arg(-1000),arg(1)), 0) //
            .implWithTolerance("value 1,0", List.of(arg(1), arg(0)), 0.241970725,-10^-6) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> errorFunction() {
        return new FunctionTestBuilder(MathFunctions.ERROR_FUNCTION) //
            .typing("INTEGER", List.of(INTEGER, INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT, FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER, INTEGER), OPT_FLOAT) //
            .typing("INTEGER+FLOAT", List.of(INTEGER, FLOAT, FLOAT), FLOAT)
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("TOO FEW", List.of(INTEGER)) //
            .implWithTolerance("inflection point", List.of(arg(0), arg(0),arg(1)), 0.,-10^-6) //
            .implWithTolerance("peak value", List.of(arg(100), arg(0),arg(1)), 1,-10^-6) //
            .implWithTolerance("peak value without deviation", List.of(arg(100), arg(0)), 1,-10^-6) //
            .implWithTolerance("should be zero", List.of(arg(-100), arg(100)), 0,-10^-6) //
            .implWithTolerance("value 1", List.of(arg(1), arg(0)), 0.842700793,-10^-6)
            .implWithTolerance("value 2", List.of(arg(2), arg(0)), 0.995322265,-10^-6)
            .implWithTolerance("value 3", List.of(arg(3), arg(0)), 0.999977909,-10^-6)
            .tests();
    }
}
