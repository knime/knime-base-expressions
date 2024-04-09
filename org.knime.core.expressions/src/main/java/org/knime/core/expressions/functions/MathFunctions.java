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

import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.allBaseTypesMatch;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.arg;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isNumericOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.vararg;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;

/**
 * Implementation of built-in functions that do math.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class MathFunctions {

    private MathFunctions() {
    }

    /** The "Math" category */
    public static final FunctionCategory CATEGORY = new FunctionCategory("Math", "Math functions");

    /** The maximum of multiple numbers */
    public static final ExpressionFunction MAX = functionBuilder() //
        .name("max") //
        .description("The maximum value of a list of numbers") //
        .keywords("maximum") //
        .category(CATEGORY.name()) //
        .args( //
            arg("input1", "first number", isNumericOrOpt()), //
            arg("input2", "second number", isNumericOrOpt()), //
            vararg("more", "additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("the largest number of the arguments", "INTEGER? | FLOAT?",
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::maxImpl) //
        .build();

    private static Computer maxImpl(final List<Computer> args) {
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);
        if (args.stream().allMatch(IntegerComputer.class::isInstance)) {
            return IntegerComputer
                .of(() -> args.stream().mapToLong(c -> ((IntegerComputer)c).compute()).max().getAsLong(), isMissing);
        } else {
            var floatArgs = args.stream().map(c -> toFloat(c)).toArray(FloatComputer[]::new);
            return FloatComputer.of(() -> Arrays.stream(floatArgs).mapToDouble(c -> c.compute()).max().getAsDouble(),
                isMissing);
        }
    }

    /** The minimum of multiple numbers */
    public static final ExpressionFunction MIN = functionBuilder() //
        .name("min") //
        .description("The minimum value of a list of numbers") //
        .keywords("minimum") //
        .category(CATEGORY.name()) //
        .args( //
            arg("input1", "first number", isNumericOrOpt()), //
            arg("input2", "second number", isNumericOrOpt()), //
            vararg("more", "additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("the smallest number of the arguments", "INTEGER? | FLOAT?",
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::minImpl) //
        .build();

    private static Computer minImpl(final List<Computer> args) {
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);
        if (args.stream().allMatch(IntegerComputer.class::isInstance)) {
            return IntegerComputer
                .of(() -> args.stream().mapToLong(c -> ((IntegerComputer)c).compute()).min().getAsLong(), isMissing);
        } else {
            var floatArgs = args.stream().map(c -> toFloat(c)).toArray(FloatComputer[]::new);
            return FloatComputer.of(() -> Arrays.stream(floatArgs).mapToDouble(c -> c.compute()).min().getAsDouble(),
                isMissing);
        }
    }

    /** The absolute value of a number */
    public static final ExpressionFunction ABS = functionBuilder() //
        .name("abs") //
        .description("The absolute value of a number") //
        .keywords("absolute") //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the absolute value of x", "INTEGER? | FLOAT?", args -> args[0]) //
        .impl(MathFunctions::absImpl) //
        .build();

    private static Computer absImpl(final List<Computer> args) {
        if (args.get(0) instanceof IntegerComputer c) {
            return IntegerComputer.of(() -> Math.abs(c.compute()), c::isMissing);
        } else if (args.get(0) instanceof FloatComputer c) {
            return FloatComputer.of(() -> Math.abs(c.compute()), c::isMissing);
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    /** The natural logarithm of one number */
    public static final ExpressionFunction LN = functionBuilder() //
        .name("ln") //
        .description("The natural logarithm of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the natural logarithm of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::lnImpl) //
        .build();

    private static Computer lnImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.log(c.compute()), c::isMissing);
    }

    /** The sine of one number */
    public static final ExpressionFunction SIN = functionBuilder() //
        .name("sin") //
        .description("The sine of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the sine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::sinImpl) //
        .build();

    private static Computer sinImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.sin(c.compute()), c::isMissing);
    }

    /** The cosine of one number */
    public static final ExpressionFunction COS = functionBuilder() //
        .name("cos") //
        .description("The cosine of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the cosine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::cosImpl) //
        .build();

    private static Computer cosImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.cos(c.compute()), c::isMissing);
    }

    // ======================= UTILITIES ==============================

    private static FloatComputer toFloat(final Computer computer) {
        if (computer instanceof FloatComputer c) {
            return c;
        } else if (computer instanceof IntegerComputer c) {
            return FloatComputer.of(c::compute, c::isMissing);
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }
}
