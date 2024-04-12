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
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isIntegerOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isNumericOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.optarg;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.vararg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.stream.IntStream;

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

    /** Index of the maximum of multiple numbers */
    public static final ExpressionFunction ARGMAX = functionBuilder() //
        .name("argmax") //
        .description("""
                The index of the maximum of a list of numbers.
                If there are multiple copies of the max value, return the index of the first one.
                """) //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("input1", "first number", isNumericOrOpt()), //
            arg("input2", "second number", isNumericOrOpt()), //
            vararg("more", "additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("the index of the largest number in the arguments", //
            "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::argmaxImpl) //
        .build();

    private static Computer argmaxImpl(final List<Computer> args) {
        // are all the arguments integers?
        boolean allInts = args.stream().allMatch(IntegerComputer.class::isInstance);

        LongSupplier value = () -> {
            if (allInts) {
                var computedArgs = args.stream().map(c -> toInteger(c).compute()).toArray(Long[]::new);

                // One indexing
                return 1 + IntStream.range(0, computedArgs.length) //
                    .reduce((a, b) -> computedArgs[a] < computedArgs[b] ? b : a) //
                    .getAsInt();
            } else {
                var computedArgs = args.stream().map(c -> toFloat(c).compute()).toArray(Double[]::new);

                // One indexing
                return 1 + IntStream.range(0, computedArgs.length) //
                    .reduce((a, b) -> computedArgs[a] < computedArgs[b] ? b : a) //
                    .getAsInt();
            }
        };

        return IntegerComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    /** Index of the minimum of multiple numbers */
    public static final ExpressionFunction ARGMIN = functionBuilder() //
        .name("argmin") //
        .description("""
                The index of the minimum of a list of numbers.
                If there are multiple copies of the min value, return the index of the first one.
                """) //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("input1", "first number", isNumericOrOpt()), //
            arg("input2", "second number", isNumericOrOpt()), //
            vararg("more", "additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("the index of the smallest number in the arguments", //
            "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::argminImpl) //
        .build();

    private static Computer argminImpl(final List<Computer> args) {
        // are all the arguments integers?
        boolean allInts = args.stream().allMatch(IntegerComputer.class::isInstance);

        LongSupplier value = () -> {
            if (allInts) {
                var computedArgs = args.stream().map(c -> toInteger(c).compute()).toArray(Long[]::new);

                // One indexing
                return 1 + IntStream.range(0, computedArgs.length) //
                    .reduce((a, b) -> computedArgs[a] > computedArgs[b] ? b : a) //
                    .getAsInt();
            } else {
                var computedArgs = args.stream().map(c -> toFloat(c).compute()).toArray(Double[]::new);

                // One indexing
                return 1 + IntStream.range(0, computedArgs.length) //
                    .reduce((a, b) -> computedArgs[a] > computedArgs[b] ? b : a) //
                    .getAsInt();
            }
        };

        return IntegerComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
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

    /** The tangent of one number */
    public static final ExpressionFunction TAN = functionBuilder() //
        .name("tan") //
        .description("The tangent of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the tangent of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::tanImpl) //
        .build();

    private static Computer tanImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.tan(c.compute()), c::isMissing);
    }

    /** The arcsine of one number */
    public static final ExpressionFunction ASIN = functionBuilder() //
        .name("asin") //
        .description("The arcsine of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the arcsine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::asinImpl) //
        .build();

    private static Computer asinImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.asin(c.compute()), c::isMissing);
    }

    /** The arccosine of one number */
    public static final ExpressionFunction ACOS = functionBuilder() //
        .name("acos") //
        .description("The arccosine of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the arccosine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::acosImpl) //
        .build();

    private static Computer acosImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.acos(c.compute()), c::isMissing);
    }

    /** The arctan of one number */
    public static final ExpressionFunction ATAN = functionBuilder() //
        .name("atan") //
        .description("The arctangent of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the arctangent of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::atanImpl) //
        .build();

    private static Computer atanImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.atan(c.compute()), c::isMissing);
    }

    /** The arctan of two numbers */
    public static final ExpressionFunction ATAN2 = functionBuilder() //
        .name("atan2") //
        .description("The arctangent of two numbers") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("y", "the number", isNumericOrOpt()), //
            arg("x", "the number", isNumericOrOpt()) //
        ) //
        .returnType("the arctangent of y and x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::atan2Impl) //
        .build();

    private static Computer atan2Impl(final List<Computer> args) {
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        var y = toFloat(args.get(0));
        var x = toFloat(args.get(1));
        return FloatComputer.of(() -> Math.atan2(y.compute(), x.compute()), isMissing);
    }

    /** Hyperbolic sine of one number */
    public static final ExpressionFunction SINH = functionBuilder() //
        .name("sinh") //
        .description("The hyperbolic sine of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the hyerbolic sine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::sinhImpl) //
        .build();

    private static Computer sinhImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.sinh(c.compute()), c::isMissing);
    }

    /** Hyperbolic cosine of one number */
    public static final ExpressionFunction COSH = functionBuilder() //
        .name("cosh") //
        .description("The hyperbolic cosine of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the hyerbolic cosine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::coshImpl) //
        .build();

    // TODO(AP-22272) warn if argument < 1
    private static Computer coshImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.cosh(c.compute()), c::isMissing);
    }

    /** Hyperbolic tangent of one number */
    public static final ExpressionFunction TANH = functionBuilder() //
        .name("tanh") //
        .description("The hyperbolic tangent of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the hyerbolic tangent of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::tanhImpl) //
        .build();

    private static Computer tanhImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.tanh(c.compute()), c::isMissing);
    }

    /** Hyperbolic arcsine of one number */
    public static final ExpressionFunction ASINH = functionBuilder() //
        .name("asinh") //
        .description("The hyperbolic arcsine of a number") //
        .keywords("arcsinh", "arsinh") //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the hyerbolic arcsine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::asinhImpl) //
        .build();

    private static Computer asinhImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));

        DoubleSupplier value = () -> {
            var cC = c.compute();

            return Math.log(cC + Math.sqrt(cC * cC + 1));
        };

        return FloatComputer.of(value, c::isMissing);
    }

    /** Hyperbolic arccosine of one number */
    public static final ExpressionFunction ACOSH = functionBuilder() //
        .name("acosh") //
        .description("The hyperbolic arccosine of a number") //
        .keywords("arccosh", "arcosh") //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the hyerbolic arccosine of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::acoshImpl) //
        .build();

    private static Computer acoshImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));

        DoubleSupplier value = () -> {
            var cC = c.compute();

            return Math.log(cC + Math.sqrt(cC * cC - 1));
        };

        return FloatComputer.of(value, c::isMissing);
    }

    /** Hyperbolic arctangent of one number */
    public static final ExpressionFunction ATANH = functionBuilder() //
        .name("atanh") //
        .description("The hyperbolic arctangent of a number") //
        .keywords("arctanh", "artanh") //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the hyerbolic arctangent of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::atanhImpl) //
        .build();

    // TODO(AP-22272) warn if |arg| > 1
    private static Computer atanhImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));

        DoubleSupplier value = () -> {
            var cC = c.compute();
            return 0.5 * Math.log((cC + 1.0) / (1.0 - cC));
        };

        return FloatComputer.of(value, c::isMissing);
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

    // TODO(AP-22272) warn if arg <= 0 (goes for all following log functions except log1p)
    private static Computer lnImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.log(c.compute()), c::isMissing);
    }

    /** The base-10 logarithm of one number */
    public static final ExpressionFunction LOG10 = functionBuilder() //
        .name("log10") //
        .description("The base-10 logarithm of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the base-10 logarithm of x", "FLOAT?", args -> FLOAT(anyOptional(args)))
        .impl(MathFunctions::log10Impl) //
        .build();

    private static Computer log10Impl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.log10(c.compute()), c::isMissing);
    }

    /** The base-2 logarithm of one number */
    public static final ExpressionFunction LOG2 = functionBuilder() //
        .name("log2") //
        .description("The base-2 logarithm of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the base-2 logarithm of x", "FLOAT?", args -> FLOAT(anyOptional(args)))
        .impl(MathFunctions::log2Impl) //
        .build();

    private static Computer log2Impl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.log(c.compute()) / Math.log(2), c::isMissing);
    }

    /** The base-n logarithm of one number */
    public static final ExpressionFunction LOG_BASE = functionBuilder() //
        .name("log") //
        .description("The logarithm of a number, with the given base") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the number", isNumericOrOpt()), //
            arg("b", "the base", isNumericOrOpt()) //
        ) //
        .returnType("the base logarithm of x base b", "FLOAT?", args -> FLOAT(anyOptional(args)))
        .impl(MathFunctions::logBaseImpl) //
        .build();

    private static Computer logBaseImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        var b = toFloat(args.get(1));

        DoubleSupplier value = () -> {
            var bC = b.compute();
            var cC = c.compute();

            return (Math.abs(bC) < 2 * Double.MIN_VALUE) ? Float.NaN : (Math.log(cC) / Math.log(bC));
        };

        BooleanSupplier isMissing = () -> c.isMissing() || b.isMissing();

        return FloatComputer.of(value, isMissing);
    }

    /** natural log of a number plus 1, i.e. ln(1+x) */
    public static final ExpressionFunction LOG1P = functionBuilder() //
        .name("log1p") //
        .description("The natural logarithm of one plus a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("natural log of (1+x)", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::log1pImpl) //
        .build();

    private static Computer log1pImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.log1p(c.compute()), c::isMissing);
    }

    /** exponent of a number */
    public static final ExpressionFunction EXP = functionBuilder() //
        .name("exp") //
        .description("e raised to the power of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("exponent of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::expImpl) //
        .build();

    private static Computer expImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.exp(c.compute()), c::isMissing);
    }

    /** For two numbers, x to the power of y */
    public static final ExpressionFunction POW = functionBuilder() //
        .name("pow") //
        .description("One number raised to the power of another") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the base", isNumericOrOpt()), //
            arg("y", "the exponent", isNumericOrOpt()) //
        ) //
        .returnType("x to the power of y", "INTEGER? | FLOAT?",
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::powImpl) //
        .build();

    private static Computer powImpl(final List<Computer> args) {
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        if (args.stream().allMatch(IntegerComputer.class::isInstance)) {
            var x = toInteger(args.get(0));
            var y = toInteger(args.get(1));
            return IntegerComputer.of(() -> (long)Math.pow(x.compute(), y.compute()), isMissing);
        } else {
            var x = toFloat(args.get(0));
            var y = toFloat(args.get(1));
            return FloatComputer.of(() -> Math.pow(x.compute(), y.compute()), isMissing);
        }
    }

    /** square root of a number */
    public static final ExpressionFunction SQRT = functionBuilder() //
        .name("sqrt") //
        .description("square root of a number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("square root of x", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::sqrtImpl) //
        .build();

    private static Computer sqrtImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.sqrt(c.compute()), c::isMissing);
    }

    /** One number modulo another */
    public static final ExpressionFunction MOD = functionBuilder() //
        .name("mod") //
        .description("One number modulo another") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the numerator", isNumericOrOpt()), //
            arg("y", "the divisor", isNumericOrOpt()) //
        ) //
        .returnType("x modulo y", "INTEGER? | FLOAT?",
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::modImpl) //
        .build();

    // TODO(AP-22272) warn when dividing by zero
    private static Computer modImpl(final List<Computer> args) {
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        if (args.stream().allMatch(IntegerComputer.class::isInstance)) {
            return IntegerComputer.of(() -> {
                var x = toInteger(args.get(0)).compute();
                var y = toInteger(args.get(1)).compute();

                return y == 0 ? 0 : (x % y);
            }, isMissing);
        } else {
            var x = toFloat(args.get(0));
            var y = toFloat(args.get(1));
            return FloatComputer.of(() -> (x.compute() % y.compute()), isMissing);
        }
    }

    /** number from radians to degrees */
    public static final ExpressionFunction DEGREES = functionBuilder() //
        .name("degrees") //
        .description("Convert a number from radians to degrees") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("x in degrees", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::degreesImpl) //
        .build();

    private static Computer degreesImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.toDegrees(c.compute()), c::isMissing);
    }

    /** number from degrees to radians */
    public static final ExpressionFunction RADIANS = functionBuilder() //
        .name("radians") //
        .description("Convert a number from degrees to radians") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("x in radians", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::radiansImpl) //
        .build();

    private static Computer radiansImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return FloatComputer.of(() -> Math.toRadians(c.compute()), c::isMissing);
    }

    /** floor of number */
    public static final ExpressionFunction FLOOR = functionBuilder() //
        .name("floor") //
        .description("Round x to nearest smaller integer") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("nearest integer less than x", "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::floorImpl) //
        .build();

    private static Computer floorImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return IntegerComputer.of(() -> (long)Math.floor(c.compute()), c::isMissing);
    }

    /** ceil of number */
    public static final ExpressionFunction CEIL = functionBuilder() //
        .name("ceil") //
        .description("Round x to nearest larger integer") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("nearest integer greater than x", "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::ceilImpl) //
        .build();

    private static Computer ceilImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return IntegerComputer.of(() -> (int)Math.ceil(c.compute()), c::isMissing);
    }

    /** truncate number, i.e. round towards zero */
    public static final ExpressionFunction TRUNC = functionBuilder() //
        .name("trunc") //
        .description("Round x towards zero") //
        .keywords("rounddown") //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("nearest integer closer to zero than x", "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::truncImpl) //
        .build();

    private static Computer truncImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return IntegerComputer.of( //
            () -> BigDecimal.valueOf(c.compute()).setScale(0, RoundingMode.DOWN).longValue(), //
            c::isMissing //
        );
    }

    /** round towards nearest integer (n + 0.5 rounds to nearest closer to zero) */
    public static final ExpressionFunction ROUNDHALFDOWN = functionBuilder() //
        .name("roundhalfdown") //
        .description("Round x to n decimal places. In case of ambiguity, round to the one closer to zero.") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the number", isNumericOrOpt()), //
            optarg("n", "precision", isIntegerOrOpt()) //
        ) //
        .returnType("nearest integer to x", "INTEGER? | FLOAT?",
            args -> (args.length == 1) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::roundHalfDownImpl) //
        .build();

    private static Computer roundHalfDownImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        if (args.size() == 1) {
            // Return integer
            return IntegerComputer.of( //
                () -> BigDecimal.valueOf(c.compute()).setScale(0, RoundingMode.HALF_DOWN).longValue(), //
                isMissing //
            );
        } else {
            return FloatComputer.of(() -> {
                int scale = (int)toInteger(args.get(1)).compute();
                return BigDecimal.valueOf(c.compute()).setScale(scale, RoundingMode.HALF_DOWN).doubleValue();
            }, isMissing);
        }
    }

    /** round towards nearest integer (n + 0.5 rounds to nearest further from zero) */
    public static final ExpressionFunction ROUNDHALFUP = functionBuilder() //
        .name("roundhalfup") //
        .description("Round x to n decimal places. In case of ambiguity, round to the one further from zero.") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the number", isNumericOrOpt()), //
            optarg("n", "precision", isIntegerOrOpt()) //
        ) //
        .returnType("nearest integer to x", "INTEGER? | FLOAT?",
            args -> (args.length == 1) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::roundHalfUpImpl) //
        .build();

    private static Computer roundHalfUpImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        if (args.size() == 1) {
            // Return integer
            return IntegerComputer.of( //
                () -> BigDecimal.valueOf(c.compute()).setScale(0, RoundingMode.HALF_UP).longValue(), //
                isMissing //
            );
        } else {
            return FloatComputer.of(() -> {
                int scale = (int)toInteger(args.get(1)).compute();
                return BigDecimal.valueOf(c.compute()).setScale(scale, RoundingMode.HALF_UP).doubleValue();
            }, isMissing);
        }
    }

    /** round towards nearest integer (n + 0.5 rounds to nearest even) */
    public static final ExpressionFunction ROUNDHALFEVEN = functionBuilder() //
        .name("round") //
        .description("Round x to n decimal places. In case of ambiguity, round to the one that is even.") //
        .keywords("roundhalfeven") //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the number", isNumericOrOpt()), //
            optarg("n", "precision", isIntegerOrOpt()) //
        ) //
        .returnType("nearest integer to x", "INTEGER? | FLOAT?",
            args -> (args.length == 1) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::roundevenImpl) //
        .build();

    private static Computer roundevenImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        if (args.size() == 1) {
            // Return integer
            return IntegerComputer.of( //
                () -> BigDecimal.valueOf(c.compute()).setScale(0, RoundingMode.HALF_EVEN).longValue(), //
                isMissing //
            );
        } else {
            return FloatComputer.of(() -> {
                int scale = (int)toInteger(args.get(1)).compute();
                return BigDecimal.valueOf(c.compute()).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
            }, isMissing);
        }
    }

    /** The sign of one number */
    public static final ExpressionFunction SIGN = functionBuilder() //
        .name("sign") //
        .description("The sign of the number") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args(arg("x", "the number", isNumericOrOpt())) //
        .returnType("the sign of x", "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::signImpl) //
        .build();

    private static Computer signImpl(final List<Computer> args) {
        var c = toFloat(args.get(0));
        return IntegerComputer.of(() -> (int)Math.signum(c.compute()), c::isMissing);
    }

    /** The mean of multiple numbers */
    public static final ExpressionFunction AVERAGE = functionBuilder() //
        .name("average") //
        .description("The mean value of a list of numbers") //
        .keywords("mean") //
        .category(CATEGORY.name()) //
        .args( //
            arg("input1", "first number", isNumericOrOpt()), //
            arg("input2", "second number", isNumericOrOpt()), //
            vararg("more", "additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("the mean of the arguments", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::averageImpl) //
        .build();

    private static Computer averageImpl(final List<Computer> args) {
        var floatArgs = args.stream().map(c -> toFloat(c)).toArray(FloatComputer[]::new);

        return FloatComputer.of( //
            () -> Arrays.stream(floatArgs).mapToDouble(c -> c.compute()).average().getAsDouble(), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    /** The median of multiple numbers */
    public static final ExpressionFunction MEDIAN = functionBuilder() //
        .name("median") //
        .description("The median value of a list of numbers") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("input1", "first number", isNumericOrOpt()), //
            arg("input2", "second number", isNumericOrOpt()), //
            vararg("more", "additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("the median of the arguments", "FLOAT?", args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::medianImpl) //
        .build();

    private static Computer medianImpl(final List<Computer> args) {
        DoubleSupplier value = () -> {
            // Because we use ::compute we need to do this inside the DoubleSupplier
            var sortedFloatArgs = args.stream().map(c -> toFloat(c).compute()) //
                .sorted().toArray(Double[]::new);

            if (sortedFloatArgs.length % 2 == 0) {
                return 0.5
                    * (sortedFloatArgs[sortedFloatArgs.length / 2] + sortedFloatArgs[sortedFloatArgs.length / 2 - 1]);
            } else {
                return sortedFloatArgs[sortedFloatArgs.length / 2];
            }
        };

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return FloatComputer.of(value, isMissing);
    }

    /** Binomial coefficient nCr of two numbers */
    public static final ExpressionFunction BINOMIAL = functionBuilder() //
        .name("binomial") //
        .description("Binomial coefficient nCr = n!/r!(n-r)!") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("n", "n", isIntegerOrOpt()), //
            arg("r", "r", isIntegerOrOpt()) //
        ) //
        .returnType("the binomial coefficient", "INTEGER?", args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::binomialImpl) //
        .build();

    // TODO(AP-22272) warn on invalid input
    private static Computer binomialImpl(final List<Computer> args) {
        LongSupplier value = () -> {
            long n = toInteger(args.get(0)).compute();
            long r = toInteger(args.get(1)).compute();

            // 0c0 needs special handling
            if (n == 0 && r == 0) {
                return 1;
            }

            if (r > n || r < 0 || n < 0) {
                return 0; // or raise error?
            }

            // nCr == nC(n-r), and our iterative algorithm is linear in r, so
            // if n-r < r, use that instead.
            r = (n - r) < r ? (n - r) : r;

            // Take advantage of the fact that (n+1)C(r+1) = (n/r)nCr, which
            // means we can start with (n-r)C0=1 and just multiply it by
            //
            // ([n]/[r])([n-1]/[k-1])([n-2]/[k-2])...([n-k+1]/[1])
            //
            // Except! If we switch the multiplication order of the denominator
            // (start with 1 instead of r) we can ensure that everything is an
            // integer the whole way, so no floating point. Nice!
            long ret = 1;

            for (int d = 1; d <= r; d++) {
                ret *= n;
                ret /= d;

                --n;
            }

            return ret;
        };

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return IntegerComputer.of(value, isMissing);
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

    private static IntegerComputer toInteger(final Computer computer) {
        if (computer instanceof IntegerComputer i) {
            return i;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }
}
