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

import static org.knime.core.expressions.Computer.toFloat;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_BOOLEAN;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_FLOAT_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_INTEGER_FLOAT_MISSING;
import static org.knime.core.expressions.ReturnTypeDescriptions.RETURN_INTEGER_MISSING;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.isFloatOrOpt;
import static org.knime.core.expressions.SignatureUtils.isIntegerOrOpt;
import static org.knime.core.expressions.SignatureUtils.isNumericOrOpt;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.SignatureUtils.vararg;
import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.allBaseTypesMatch;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.FloatComputerResultSupplier;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.IntegerComputerResultSupplier;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ValueType;

import com.google.common.math.LongMath;

/**
 * Implementation of built-in functions that do math.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class MathFunctions {

    private MathFunctions() {
    }

    private static final String MATH_META_CATEGORY_NAME = "Math";

    /** The "Math – General" category */
    public static final OperatorCategory CATEGORY_GENERAL = new OperatorCategory(MATH_META_CATEGORY_NAME, "General", """
            The "Math – General" category in KNIME Expression language includes essential mathematical functions for
            common calculations and operations, such as exponentiation, logarithms, modulus, and handling NaN values.
            These functions support basic mathematical computations and data transformations within expressions.
            """);

    /** The "Math – Round" category */
    public static final OperatorCategory CATEGORY_ROUND = new OperatorCategory(MATH_META_CATEGORY_NAME, "Round", """
            The "Math – Round" category in KNIME Expression language includes functions for rounding numerical values.
            These functions provide various rounding methods, such as rounding half up, half down, and half even, as
            well as truncating values, and rounding up or down to the nearest integer.
            """);

    /** The "Math – Aggregate" category */
    public static final OperatorCategory CATEGORY_AGGREGATE =
        new OperatorCategory(MATH_META_CATEGORY_NAME, "Aggregate", """
                The "Math – Aggregate" category in KNIME Expression language includes functions for aggregating
                numerical data. These functions calculate averages, medians, minimums, maximums, and identify the
                positions of minimum and maximum values.
                """);

    /** The "Math – Trigonometry" category */
    public static final OperatorCategory CATEGORY_TRIGONOMETRY =
        new OperatorCategory(MATH_META_CATEGORY_NAME, "Trigonometry", """
                The "Math – Trigonometry" category in KNIME Expression language includes functions for performing
                trigonometric calculations. These functions convert between degrees and radians, and calculate sine,
                cosine, tangent, and their respective inverse and hyperbolic functions.
                """);

    /** The "Math – Distributions" category */
    public static final OperatorCategory CATEGORY_DISTRIBUTIONS =
        new OperatorCategory(MATH_META_CATEGORY_NAME, "Distributions", """
                The "Math – Distributions" category in KNIME Expression language includes functions for working with
                statistical distributions. These functions handle binomial and normal distributions, as well as the
                error function.
                """);

    // some common argument identifiers
    private static final String PRECISION = "precision";

    /** The maximum of multiple numbers */
    public static final ExpressionFunction MAX = functionBuilder() //
        .name("max") //
        .description("""
                The maximum value of the `input` numbers. At least two arguments are required, but
                beyond that you can supply as many as you like.

                If all inputs are integers, the output is an integer. If any of the inputs
                is a float, the output is a float.

                If any `input` is `MISSING`, the result is also `MISSING`.
                If any `input` is `NaN` the result is `NaN`.
                """) //
        .examples("""
                * `max(1, 2, 3)` returns 3
                * `max(1.0, 2.0, 3.0)` returns 3.0
                * `max(1, 2.0, 3)` returns 3.0
                * `max(1, $["Missing Column"], 3)` returns `MISSING`
                * `max(1, 2, NaN)` returns `NaN`
                """) //
        .keywords("maximum") //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Largest number of the `input` arguments", RETURN_INTEGER_FLOAT_MISSING,
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::maxImpl) //
        .build();

    private static Computer maxImpl(final Arguments<Computer> args) {
        boolean allArgsAreIntegers = args.allMatch(IntegerComputer.class::isInstance);

        if (allArgsAreIntegers) {
            var intArgs = args.map(c -> toInteger(c));
            return IntegerComputer.of( //
                ctx -> argsToLongStream(intArgs, ctx).max().getAsLong(), //
                anyMissing(args) //
            );
        } else {
            var floatArgs = args.map(c -> toFloat(c));
            return FloatComputer.of( //
                ctx -> argsToDoubleStream(floatArgs, ctx).max().getAsDouble(), //
                anyMissing(args) //
            );
        }
    }

    /** The minimum of multiple numbers */
    public static final ExpressionFunction MIN = functionBuilder() //
        .name("min") //
        .description("""
                The minimum value of the `input` numbers. At least two arguments are required, but
                beyond that you can supply as many as you like.

                If all inputs are integers, the output is an integer. If any of the inputs
                is a float, the output is a float.

                If any `input` is `MISSING`, the result is also `MISSING`.
                If any `input` is `NaN` the result is `NaN`.
                """) //
        .examples("""
                * `min(1, 2, 3)` returns 1
                * `min(1.0, 2.0, 3.0)` returns 1.0
                * `min(1, 2.0, 3)` returns 1.0
                * `min(1, $["Missing Column"], 3)` returns `MISSING`
                * `min(1, 2, NaN)` returns `NaN`
                """) //
        .keywords("minimum") //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Smallest number of the arguments", RETURN_INTEGER_FLOAT_MISSING,
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args))) //
        .impl(MathFunctions::minImpl) //
        .build();

    private static Computer minImpl(final Arguments<Computer> args) {
        boolean allArgsAreIntegers = args.allMatch(IntegerComputer.class::isInstance);

        if (allArgsAreIntegers) {
            var intArgs = args.map(MathFunctions::toInteger);
            return IntegerComputer.of( //
                ctx -> argsToLongStream(intArgs, ctx).min().getAsLong(), //
                anyMissing(args) //
            );
        } else {
            var floatArgs = args.map(Computer::toFloat);
            return FloatComputer.of( //
                ctx -> argsToDoubleStream(floatArgs, ctx).min().getAsDouble(), //
                anyMissing(args) //
            );
        }
    }

    /** Index of the maximum of multiple numbers */
    public static final ExpressionFunction ARGMAX = functionBuilder() //
        .name("argmax") //
        .description(
            """
                    The position of the maximum value of the `input` numbers, starting at 1. At least two arguments are required, but
                    beyond that you can supply as many as you like. If there are multiple copies of the maximum value, the position of
                    the first one is returned.

                    If any `input` is `MISSING`, the result is also `MISSING`.
                    If any `input` is `NaN` the result is the position of the first `NaN`.
                    """) //
        .examples("""
                * `argmax(2, 4, 6)` returns 3
                * `argmax(2.0, 4.0, 6.0)` returns 3
                * `argmax(2, 4.0, 6)` returns 3
                * `argmax(1, 2, 2)` returns 2
                * `argmax(1, $["Missing Column"], 3)` returns `MISSING`
                * `argmax(1, 2, NaN)` returns 3
                """) //
        .keywords() //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Position of the largest number in the arguments", //
            RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::argmaxImpl) //
        .build();

    private static Computer argmaxImpl(final Arguments<Computer> args) {
        boolean allArgsAreIntegers = args.allMatch(IntegerComputer.class::isInstance);

        IntegerComputerResultSupplier supplier;

        if (allArgsAreIntegers) {
            supplier = ctx -> {
                var computedArgs = args.map(c -> toInteger(c).compute(ctx));

                var intStream = IntStream.range(0, computedArgs.getNumberOfArguments());

                // Add 1 here because we want to return 1-indexed values.
                return 1 + intStream.reduce(streamExtremumReducer(ExtremumType.MAXIMUM, computedArgs.toList())) //
                    .getAsInt(); //
            };
        } else {
            var floatArgs = args.map(c -> toFloat(c));

            supplier = ctx -> {
                var computedArgs = floatArgs.map(c -> c.compute(ctx));

                var intStream = IntStream.range(0, computedArgs.getNumberOfArguments());

                // Add 1 here because we want to return 1-indexed values.
                return 1 + intStream // Custom reducer produces first NaN if there are NaNs
                    .reduce(nanPropagatingStreamExtremumReducer(ExtremumType.MAXIMUM, computedArgs.toList())) //
                    .orElseThrow(() -> new IllegalStateException("Stream was empty. This is an implementation bug"));
            };
        }

        return IntegerComputer.of(supplier, anyMissing(args));
    }

    /** Index of the minimum of multiple numbers */
    public static final ExpressionFunction ARGMIN = functionBuilder() //
        .name("argmin") //
        .description(
            """
                    The position of the minimum value of the `input` numbers, starting at 1. At least two arguments are required, but
                    beyond that you can supply as many as you like. If there are multiple copies of the minimum value, the position of
                    the first one is returned.

                    If any `input` is `MISSING`, the result is also `MISSING`.
                    If any `input` is `NaN` the result is the position of the first `NaN`.
                    """) //
        .examples("""
                * `argmin(2, 4, 6)` returns 1
                * `argmin(2.0, 4.0, 6.0)` returns 1
                * `argmin(2, 4.0, 6)` returns 1
                * `argmin(1, 2, 1)` returns 1
                * `argmin(1, $["Missing Column"], 3)` returns `MISSING`
                * `argmin(1, 2, NaN)` returns 3
                """) //
        .keywords() //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Position of the smallest number in the arguments", //
            RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::argminImpl) //
        .build();

    private static Computer argminImpl(final Arguments<Computer> args) {
        boolean allArgsAreIntegers = args.allMatch(IntegerComputer.class::isInstance);

        IntegerComputerResultSupplier supplier;

        if (allArgsAreIntegers) {
            supplier = ctx -> {
                var computedArgs = args.map(c -> toInteger(c).compute(ctx));

                var intStream = IntStream.range(0, computedArgs.getNumberOfArguments());

                // Add 1 here because we want to return 1-indexed values.
                return 1 + intStream.reduce(streamExtremumReducer(ExtremumType.MINIMUM, computedArgs.toList())) //
                    .getAsInt(); //
            };
        } else {
            var floatArgs = args.map(c -> toFloat(c));

            supplier = ctx -> {
                var computedArgs = floatArgs.map(c -> c.compute(ctx));

                var intStream = IntStream.range(0, computedArgs.getNumberOfArguments());

                // Add 1 here because we want to return 1-indexed values.
                return 1 + intStream // Custom reducer produces first NaN if there are NaNs
                    .reduce(nanPropagatingStreamExtremumReducer(ExtremumType.MINIMUM, computedArgs.toList())) //
                    .orElseThrow(() -> new IllegalStateException("Stream was empty. This is an implementation bug"));
            };
        }

        return IntegerComputer.of(supplier, anyMissing(args));
    }

    /** The absolute value of a number */
    public static final ExpressionFunction ABS = functionBuilder() //
        .name("abs") //
        .description("""
                The absolute value of a number.

                If the input `x` is `MISSING`, the output is `MISSING`.
                If the input `x` is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `abs(1)` returns 1
                * `abs(-2)` returns 2
                * `abs(-3.0)` returns 3.0
                * `abs($["Missing Column"])` returns `MISSING`
                * `abs(NaN)` returns `NaN`
                """) //
        .keywords("absolute") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Absolute value of x", RETURN_INTEGER_FLOAT_MISSING, args -> args.get("x")) //
        .impl(MathFunctions::absImpl) //
        .build();

    private static Computer absImpl(final Arguments<Computer> args) {
        var x = args.get("x");

        if (x instanceof IntegerComputer intX) {
            return IntegerComputer.of(ctx -> Math.abs(intX.compute(ctx)), intX::isMissing);
        } else if (x instanceof FloatComputer floatX) {
            return FloatComputer.of(ctx -> Math.abs(floatX.compute(ctx)), floatX::isMissing);
        } else {
            throw FunctionUtils.calledWithIllegalArgs();
        }

    }

    /** The sine of one number */
    public static final ExpressionFunction SIN = functionBuilder() //
        .name("sin") //
        .description("""
                The sine of a number, with the input `x` in radians.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `sin(0)` returns 0.0
                * `sin(PI)` returns 0.0
                * `sin(PI/2)` returns 1.0
                * `sin($["Missing Column"])` returns `MISSING`
                * `sin(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "An angle in radians", isNumericOrOpt())) //
        .returnType("Sine of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::sinImpl) //
        .build();

    private static Computer sinImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.sin(c.compute(ctx)), c::isMissing);
    }

    /** The cosine of one number */
    public static final ExpressionFunction COS = functionBuilder() //
        .name("cos") //
        .description("""
                The cosine of a number, with the input `x` in radians.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `cos(0)` returns 1.0
                * `cos(PI)` returns -1.0
                * `cos(PI/2)` returns 0.0
                * `cos($["Missing Column"])` returns `MISSING`
                * `cos(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "Angle in radians", isNumericOrOpt())) //
        .returnType("Cosine of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::cosImpl) //
        .build();

    private static Computer cosImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.cos(c.compute(ctx)), c::isMissing);
    }

    /** The tangent of one number */
    public static final ExpressionFunction TAN = functionBuilder() //
        .name("tan") //
        .description("""
                The tangent of a number, with the input `x` in radians.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `tan(0)` returns 0.0
                * `tan(PI)` returns 0.0
                * `tan(PI/2)` returns `+INFINITY`
                * `tan($["Missing Column"])` returns `MISSING`
                * `tan(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "Angle in radians", isNumericOrOpt())) //
        .returnType("Tangent of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::tanImpl) //
        .build();

    private static Computer tanImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.tan(c.compute(ctx)), c::isMissing);
    }

    /** The arcsine of one number */
    public static final ExpressionFunction ASIN = functionBuilder() //
        .name("asin") //
        .description("""
                The arcsine of a number, with the output in radians.

                If the input is outside the range [-1, 1], a warning is issued and the result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `asin(0)` returns 0.0
                * `asin(1)` returns 1.570796… (approx. `PI/2`)
                * `asin($["Missing Column"])` returns `MISSING`
                * `asin(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Arcsine of x in radians", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::asinImpl) //
        .build();

    private static Computer asinImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> {
            var cC = c.compute(ctx);
            var ret = Math.asin(cC);

            if (Math.abs(cC) > 1) {
                ctx.addWarning("asin returned %s because argument is outside the range [-1, 1].".formatted(ret));
            }

            return ret;
        }, c::isMissing);
    }

    /** The arccosine of one number */
    public static final ExpressionFunction ACOS = functionBuilder() //
        .name("acos") //
        .description("""
                The arccosine of a number, with the output in radians between `-PI`
                and `+PI`.

                If the input is outside the range [-1, 1], a warning is issued and the result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `acos(0)` returns 1.570796… (approx. `PI/2`)
                * `acos(1)` returns 0.0
                * `acos($["Missing Column"])` returns `MISSING`
                * `acos(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Arccosine of x in radians", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::acosImpl) //
        .build();

    private static Computer acosImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> {
            var cC = c.compute(ctx);
            var ret = Math.acos(cC);

            if (Math.abs(cC) > 1) {
                ctx.addWarning("acos returned %s because argument is greater than 1.".formatted(ret));
            }

            return ret;
        }, c::isMissing);
    }

    /** The arctan of one number */
    public static final ExpressionFunction ATAN = functionBuilder() //
        .name("atan") //
        .description("""
                The arctangent of a number, with the output in radians between `-PI/2`
                and `+PI/2`.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `atan(0)` returns 0.0
                * `atan(1)` returns 0.785398… (approx. `PI/4`)
                * `atan($["Missing Column"])` returns `MISSING`
                * `atan(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Arctangent of x in radians", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::atanImpl) //
        .build();

    private static Computer atanImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.atan(c.compute(ctx)), c::isMissing);
    }

    /** The arctan of two numbers */
    public static final ExpressionFunction ATAN2 = functionBuilder() //
        .name("atan2") //
        .description("""
                The arctangent defined be a point (x, y) on a cartesian plane, with the output in radians between
                `-PI` and `+PI`. This differs from the atan function in that it knows the sign of both coordinates,
                so it returns the correct quadrant for the angle. For example,
                `atan2(-1, -1)` returns `-3*PI/4`, while `atan(-1/-1)` returns `PI/4`.

                If both inputs are zero, the result is `NaN` and a warning is
                issued.
                If any input is `MISSING`, the output is `MISSING`.
                If any input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `atan2(1, 1)` returns 0.785398… (approx. `PI/4`)
                * `atan2(-1, -1)` returns -2.356194… (approx. `-3*PI/4`)
                * `atan2(0, 0)` returns `NaN`
                * `atan2($["Missing Column"], …)` returns `MISSING`
                * `atan2(NaN, …)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args( //
            arg("y", "Coordinate on the y axis", isNumericOrOpt()), //
            arg("x", "Coordinate on the x axis", isNumericOrOpt()) //
        ) //
        .returnType("Arctangent of point (x, y) in radians", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::atan2Impl) //
        .build();

    private static Computer atan2Impl(final Arguments<Computer> args) {
        var y = toFloat(args.get("y"));
        var x = toFloat(args.get("x"));
        return FloatComputer.of( //
            ctx -> {
                var xC = x.compute(ctx);
                var yC = y.compute(ctx);

                if (isNearZero(xC) && isNearZero(yC)) {
                    ctx.addWarning("atan2 returned NaN because both inputs are zero.");
                    return Double.NaN;
                }

                return Math.atan2(yC, xC);
            }, //
            anyMissing(args) //
        );
    }

    /** Hyperbolic sine of one number */
    public static final ExpressionFunction SINH = functionBuilder() //
        .name("sinh") //
        .description("""
                The hyperbolic sine of a number.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `sinh(0)` returns 0.0
                * `sinh(1)` returns 1.175201…
                * `sinh($["Missing Column"])` returns `MISSING`
                * `sinh(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Hyerbolic sine of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::sinhImpl) //
        .build();

    private static Computer sinhImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.sinh(c.compute(ctx)), c::isMissing);
    }

    /** Hyperbolic cosine of one number */
    public static final ExpressionFunction COSH = functionBuilder() //
        .name("cosh") //
        .description("""
                The hyperbolic cosine of a number.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `cosh(0)` returns 1.0
                * `cosh(1)` returns 1.543080…
                * `cosh($["Missing Column"])` returns `MISSING`
                * `cosh(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Hyerbolic cosine of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::coshImpl) //
        .build();

    private static Computer coshImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.cosh(c.compute(ctx)), c::isMissing);
    }

    /** Hyperbolic tangent of one number */
    public static final ExpressionFunction TANH = functionBuilder() //
        .name("tanh") //
        .description("""
                The hyperbolic tangent of a number.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `tanh(0)` returns 0.0
                * `tanh(1)` returns 0.761594…
                * `tanh($["Missing Column"])` returns `MISSING`
                * `tanh(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Hyerbolic tangent of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::tanhImpl) //
        .build();

    private static Computer tanhImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> Math.tanh(c.compute(ctx)), c::isMissing);
    }

    /** Hyperbolic arcsine of one number */
    public static final ExpressionFunction ASINH = functionBuilder() //
        .name("asinh") //
        .description("""
                The hyperbolic arcsine of a number.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `asinh(0)` returns 0.0
                * `asinh(1)` returns 1.570796…
                * `asinh($["Missing Column"])` returns `MISSING`
                * `asinh(NaN)` returns `NaN`
                """) //
        .keywords("arcsinh", "arsinh") //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Hyerbolic arcsine of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::asinhImpl) //
        .build();

    private static Computer asinhImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));

        FloatComputerResultSupplier value = ctx -> {
            var cC = c.compute(ctx);

            return Math.log(cC + Math.sqrt(cC * cC + 1));
        };

        return FloatComputer.of(value, c::isMissing);
    }

    /** Hyperbolic arccosine of one number */
    public static final ExpressionFunction ACOSH = functionBuilder() //
        .name("acosh") //
        .description("""
                The hyperbolic arccosine of a number.

                If the input is less than 1, a warning is issued and the
                result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `acosh(1)` returns 0.0
                * `acosh(2)` returns 1.316957…
                * `acosh(.5)` returns `NaN`
                * `acosh($["Missing Column"])` returns `MISSING`
                * `acosh(NaN)` returns `NaN`
                """) //
        .keywords("arccosh", "arcosh") //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Hyerbolic arccosine of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::acoshImpl) //
        .build();

    private static Computer acoshImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));

        FloatComputerResultSupplier value = ctx -> {
            var cC = c.compute(ctx);

            if (cC < 1) {
                ctx.addWarning("acosh returned NaN because argument is less than 1.");
                return Double.NaN;
            }

            return Math.log(cC + Math.sqrt(cC * cC - 1));
        };

        return FloatComputer.of(value, c::isMissing);
    }

    /** Hyperbolic arctangent of one number */
    public static final ExpressionFunction ATANH = functionBuilder() //
        .name("atanh") //
        .description("""
                The hyperbolic arctangent of a number.

                If the input is outside the range [-1, 1], a warning is issued and the
                result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `atanh(0)` returns 0.0
                * `atanh(0.5)` returns 0.549306…
                * `atanh(.5)` returns `NaN`
                * `atanh($["Missing Column"])` returns `MISSING`
                * `atanh(NaN)` returns `NaN`
                """) //
        .keywords("arctanh", "artanh") //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Hyerbolic arctangent of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::atanhImpl) //
        .build();

    private static Computer atanhImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));

        FloatComputerResultSupplier value = ctx -> {
            var cC = c.compute(ctx);

            if (isNear(Math.abs(cC), 1)) {
                var argSign = Math.signum(cC);
                var ret = argSign * Double.POSITIVE_INFINITY;
                ctx.addWarning("atanh returned %s because argument is %s.".formatted(ret, argSign));
            } else if (Math.abs(cC) >= 1) {
                ctx.addWarning("atanh returned NaN because argument is outside the range [-1, 1].");
                return Double.NaN;
            }

            return 0.5 * Math.log((cC + 1.0) / (1.0 - cC));
        };

        return FloatComputer.of(value, c::isMissing);
    }

    /** The natural logarithm of one number */
    public static final ExpressionFunction LN = functionBuilder() //
        .name("ln") //
        .description("""
                The natural logarithm of a number.

                If the input is 0, a warning is issued and the
                result is `-INFINITY`.
                If the input is less than 0, a warning is issued and the
                result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `ln(1)` returns 0.0
                * `ln(E)` returns 1.0
                * `ln(0)` returns `-INFINITY`
                * `ln($["Missing Column"])` returns `MISSING`
                * `ln(NaN)` returns `NaN`
                """) //
        .keywords("natural log") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Natural logarithm of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::lnImpl) //
        .build();

    private static Computer lnImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> {
            var cC = c.compute(ctx);

            if (cC == 0) {
                ctx.addWarning("ln returned -INFINITY because argument is 0.");
                return Double.NEGATIVE_INFINITY;
            } else if (cC < 0) {
                ctx.addWarning("ln returned NaN because argument is less than 0.");
                return Double.NaN;
            }

            return Math.log(cC);
        }, c::isMissing);
    }

    /** The base-10 logarithm of one number */
    public static final ExpressionFunction LOG10 = functionBuilder() //
        .name("log10") //
        .description("""
                The base-10 logarithm of a number.

                If the input is 0, a warning is issued and the
                result is `-INFINITY`.
                If the input is less than 0, a warning is issued and the
                result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `log10(1)` returns 0.0
                * `log10(10)` returns 1.0
                * `log10(0)` returns `-INFINITY`
                * `log10($["Missing Column"])` returns `MISSING`
                * `log10(NaN)` returns `NaN`
                """) //
        .keywords("common log", "decimal log") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Base-10 logarithm of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::log10Impl) //
        .build();

    private static Computer log10Impl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> {
            var cC = c.compute(ctx);

            if (cC == 0) {
                ctx.addWarning("log10 returned -INFINITY because argument is 0.");
                return Double.NEGATIVE_INFINITY;
            } else if (cC < 0) {
                ctx.addWarning("log10 returned NaN because argument is less than 0.");
                return Double.NaN;
            }

            return Math.log10(cC);
        }, c::isMissing);
    }

    /** The base-2 logarithm of one number */
    public static final ExpressionFunction LOG2 = functionBuilder() //
        .name("log2") //
        .description("""
                The base-2 logarithm of a number.

                If the input is 0, a warning is issued and the
                result is `-INFINITY`.
                If the input is less than 0, a warning is issued and the
                result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `log2(1)` returns 0.0
                * `log2(2)` returns 1.0
                * `log2(0)` returns `-INFINITY`
                * `log2($["Missing Column"])` returns `MISSING`
                * `log2(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Base-2 logarithm of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::log2Impl) //
        .build();

    private static Computer log2Impl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of(ctx -> {
            var cC = c.compute(ctx);

            if (cC == 0) {
                ctx.addWarning("log2 returned -INFINITY because argument is 0.");
                return Double.NEGATIVE_INFINITY;
            } else if (cC < 0) {
                ctx.addWarning("log2 returned NaN because argument is less than 0.");
                return Double.NaN;
            }

            return Math.log(cC) / Math.log(2);
        }, c::isMissing);
    }

    /** The base-n logarithm of one number */
    public static final ExpressionFunction LOG_BASE = functionBuilder() //
        .name("log") //
        .description(
            """
                    The logarithm of a number `x`, with the given `base` > 0.
                    This is mathematically equivalent to `ln(x) / ln(base)`.

                    If either `x` is less than 0 or the `base` is less than or equal to 0, a warning is issued and the result is `NaN`.
                    If `x` is zero, a warning is issued and the result is `-INFINITY`.
                    If the `base` is 1, a warning is issued and the result is `NaN`.
                    If any input is `MISSING`, the output is `MISSING`.
                    If any input is `NaN`, the output is `NaN`.
                    """) //
        .examples("""
                * `log(1, 10)` returns 0.0
                * `log(1000, 10)` returns 3.0
                * `log(64, 2)` returns 6.0
                * `log(…, 0)` returns `NaN`
                * `log(0, …)` returns `-INFINITY`
                * `log(…, 1)` returns `NaN`
                * `log($["Missing Column"], …)` returns `MISSING`
                * `log(NaN, …)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("x", "A number", isNumericOrOpt()), //
            arg("base", "The base number", isNumericOrOpt()) //
        ) //
        .returnType("Logarithm of x with the given base", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))
        .impl(MathFunctions::logBaseImpl) //
        .build();

    private static Computer logBaseImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        var b = toFloat(args.get("base"));

        FloatComputerResultSupplier value = ctx -> {
            var base = b.compute(ctx);
            var number = c.compute(ctx);

            if (isNearZero(base) && isNearZero(number)) {
                ctx.addWarning("log returned NaN because both arguments are 0.");
                return Double.NaN;
            } else if (isNearZero(base)) {
                ctx.addWarning("log returned NaN because base is 0.");
                return Double.NaN;
            } else if (isNearZero(number)) {
                ctx.addWarning("log returned -INFINITY because first argument is 0.");
                return Double.NEGATIVE_INFINITY;
            } else if (number < 0) {
                ctx.addWarning("log returned NaN because first argument is less than 0.");
                return Double.NaN;
            } else if (base <= 0) {
                ctx.addWarning("log returned NaN because base is less than 0.");
                return Double.NaN;
            } else if (isNear(base, 1)) {
                ctx.addWarning("log returned NaN because base is 1.");
                return Double.NaN;
            }

            return Math.log(number) / Math.log(base);
        };

        return FloatComputer.of(value, anyMissing(args));
    }

    /** natural log of a number plus 1, i.e. ln(1+x) */
    public static final ExpressionFunction LOG1P = functionBuilder() //
        .name("log1p") //
        .description("""
                The natural logarithm of one plus a number.
                This is a more accurate way to compute the natural logarithm of
                numbers close to 1, as it avoids the loss of precision that can
                occur when adding 1 to a number close to 0 before taking the
                logarithm.

                If the input is equal to -1, a warning is issued
                and the result is `-INFINITY`.
                If the input is less than -1, a warning is issued
                and the result is `NaN`.
                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `log1p(0)` returns 0.0
                * `log1p(1)` returns 0.693147…
                * `log1p(0)` returns `-INFINITY`
                * `log1p($["Missing Column"])` returns `MISSING`
                * `log1p(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Natural logarithm of (1+x)", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::log1pImpl) //
        .build();

    private static Computer log1pImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of( //
            ctx -> {
                var cC = c.compute(ctx);

                if (cC == -1) {
                    ctx.addWarning("log1p returned -INFINITY because argument is -1.");
                    return Double.NEGATIVE_INFINITY;
                } else if (cC < -1) {
                    ctx.addWarning("log1p returned NaN because argument is less than -1.");
                    return Double.NaN;
                }

                return Math.log1p(cC);
            }, //
            c::isMissing //
        );
    }

    /** exponent of a number */
    public static final ExpressionFunction EXP = functionBuilder() //
        .name("exp") //
        .description("""
                The exponent of a number, i.e. Euler's constant e raised to the
                power of the number.

                If the input is `MISSING`, the output is `MISSING`.
                If the input is `NaN`, the output is `NaN`.
                """) //
        .examples("""
                * `exp(0)` returns 1.0
                * `exp(1)` returns 2.718281828459045… (approx. `E`)
                * `exp(-1)` returns 0.367879… (approx. `1/E`)
                """) //
        .keywords("exponent") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Eulers contant to the power of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::expImpl) //
        .build();

    private static Computer expImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of( //
            ctx -> Math.exp(c.compute(ctx)), //
            c::isMissing //
        );
    }

    /** For two numbers, x to the power of y */
    public static final ExpressionFunction POW = functionBuilder() //
        .name("pow") //
        .description("""
                One number raised to the power of another.

                If both arguments are integers, the result will be an integer. If
                either argument is a float, the result will be a float.

                If either argument is `MISSING`, the result will also be `MISSING`.
                If both arguments are zero, or the base is zero and the exponent is
                negative, then the result will be:
                * `NaN` if at least one argument is a float
                * 0 if both arguments are integers
                In either case, a warning will be issued.

                If any argument is `NaN` the function returns `NaN`.
                """) //
        .examples("""
                * `pow(2, 3)` returns 8
                * `pow(2.0, 3)` returns 8.0
                * `pow(16, 0.5)` returns 4.0
                """) //
        .keywords("power") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("x", "The base", isNumericOrOpt()), //
            arg("y", "The exponent", isNumericOrOpt()) //
        ) //
        .returnType("x to the power of y", RETURN_INTEGER_FLOAT_MISSING,
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args)))//
        .impl(MathFunctions::powImpl) //
        .build();

    private static Computer powImpl(final Arguments<Computer> args) {
        if (args.allMatch(IntegerComputer.class::isInstance)) {
            var x = toInteger(args.get("x"));
            var y = toInteger(args.get("y"));
            return IntegerComputer.of( //
                ctx -> {
                    var xC = x.compute(ctx);
                    var yC = y.compute(ctx);

                    if (xC == 0 && yC <= 0) {
                        ctx.addWarning("INTEGER pow returned 0 because base is zero and exponent is non-positive.");
                        return 0;
                    } else if (xC != 1 && yC < 0) {
                        return 0; // 1/x^y is always 0 for integer x!=1 and positive y
                    } else if (xC == 1) {
                        return 1; // 1^y is always 1
                    }

                    return LongMath.pow(xC, FunctionUtils.toIntExact(yC, "Exponent argument was too large."));
                }, anyMissing(args) //
            );
        } else {
            var x = toFloat(args.get("x"));
            var y = toFloat(args.get("y"));
            return FloatComputer.of( //
                ctx -> {
                    var xC = x.compute(ctx);
                    var yC = y.compute(ctx);

                    if (isNearZero(xC) && (isNearZero(yC) || yC < 0)) {
                        ctx.addWarning("FLOAT pow returned NaN because base is zero and exponent is non-positive.");
                        return Double.NaN;
                    }

                    return Math.pow(xC, yC);
                }, anyMissing(args) //
            );
        }
    }

    /** square root of a number */
    public static final ExpressionFunction SQRT = functionBuilder() //
        .name("sqrt") //
        .description("""
                The square root of a number.

                If the input is `MISSING`, the output will also be `MISSING`. If the
                input is negative, a warning will be issued and the result will be
                `NaN`.
                """) //
        .examples("""
                * `sqrt(4)` returns 2.0
                * `sqrt(-1)` returns `NaN`
                * `sqrt(NaN)` returns `NaN`
                * `sqrt(9)` returns 3.0
                """) //
        .keywords("squareroot") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Square root of x", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::sqrtImpl) //
        .build();

    private static Computer sqrtImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of( //
            ctx -> {
                var cC = c.compute(ctx);

                if (cC < 0) {
                    ctx.addWarning("sqrt returned NaN because argument is negative.");
                    return Double.NaN;
                }

                return Math.sqrt(cC); //
            }, c::isMissing //
        );
    }

    /** One number modulo another */
    public static final ExpressionFunction MOD = functionBuilder() //
        .name("mod") //
        .description("""
                One number modulo another.

                If both arguments are integers, the result will be an integer. If
                either argument is a float, the result will be a float. The result
                will always have the same sign as the numerator.

                If either argument is `MISSING`, the result will also be `MISSING`.
                If the divisor is zero, the result will be
                * 0 if both arguments are integers
                * `NaN` if either argument is a float
                A warning will be issued in either case.

                If any argument is `NaN` the function returns `NaN`.
                """) //
        .examples("""
                * `mod(5, 3)` returns 2
                * `mod(5.0, 3)` returns 2.0
                * `mod(-5, 3)` returns -2
                """) //
        .keywords("modulo", "remainder") //
        .category(CATEGORY_GENERAL) //
        .args( //
            arg("x", "The numerator", isNumericOrOpt()), //
            arg("y", "The divisor", isNumericOrOpt()) //
        ) //
        .returnType("x modulo y", RETURN_INTEGER_FLOAT_MISSING,
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args)))//
        .impl(MathFunctions::modImpl) //
        .build();

    private static Computer modImpl(final Arguments<Computer> args) {
        if (args.allMatch(IntegerComputer.class::isInstance)) {
            return IntegerComputer.of(ctx -> {
                var y = toInteger(args.get("y")).compute(ctx);

                if (y == 0) {
                    ctx.addWarning("INTEGER mod returned 0 because divisor is zero.");
                    return 0;
                }

                var x = toInteger(args.get("x")).compute(ctx);

                return x % y;
            }, anyMissing(args));
        } else {
            return FloatComputer.of( //
                ctx -> {
                    var y = toFloat(args.get("y")).compute(ctx);

                    if (isNearZero(y)) {
                        ctx.addWarning("FLOAT mod returned NaN because divisor is zero.");
                        return Double.NaN;
                    }

                    var x = toFloat(args.get("x")).compute(ctx);

                    return x % y;
                }, anyMissing(args) //
            );
        }
    }

    /** number from radians to degrees */
    public static final ExpressionFunction DEGREES = functionBuilder() //
        .name("degrees") //
        .description("""
                Convert a number from radians to degrees.

                If the input is `MISSING`, the output will also be `MISSING`.
                """) //
        .examples("""
                * `degrees(0)` returns 0.0
                * `degrees(PI)` returns 180.0
                * `degrees(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("x in degrees", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::degreesImpl) //
        .build();

    private static Computer degreesImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of( //
            ctx -> Math.toDegrees(c.compute(ctx)), //
            c::isMissing //
        );
    }

    /** number from degrees to radians */
    public static final ExpressionFunction RADIANS = functionBuilder() //
        .name("radians") //
        .description("""
                Convert a number from degrees to radians.

                If the input is `MISSING`, the output will also be `MISSING`.
                """) //
        .examples("""
                * `radians(0)` returns 0.0
                * `radians(180)` returns 3.141592… (approx. `PI`)
                * `radians(NaN)` returns `NaN`
                """) //
        .keywords() //
        .category(CATEGORY_TRIGONOMETRY) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("x in radians", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::radiansImpl) //
        .build();

    private static Computer radiansImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of( //
            ctx -> Math.toRadians(c.compute(ctx)), //
            c::isMissing //
        );
    }

    /** floor of number */
    public static final ExpressionFunction FLOOR = functionBuilder() //
        .name("floor") //
        .description("""
                Round x to nearest smaller integer.

                If the input is `MISSING` or `NaN`, the output will be `MISSING`.
                """) //
        .examples("""
                * `floor(2.5)` returns 2
                * `floor(-2.5)` returns -3
                * `floor(1.0)` returns 1
                * `floor(NaN)` returns `MISSING`
                """) //
        .keywords() //
        .category(CATEGORY_ROUND) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Nearest integer less than x", RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::floorImpl) //
        .build();

    private static Computer floorImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return IntegerComputer.of( //
            ctx -> (long)Math.floor(c.compute(ctx)), //
            ctx -> {
                if (c.isMissing(ctx)) {
                    return true;
                } else if (Double.isNaN(c.compute(ctx))) {
                    ctx.addWarning("floor returned MISSING because argument is NaN.");
                    return true;
                } else {
                    return false;
                }
            } //
        );
    }

    /** ceil of number */
    public static final ExpressionFunction CEIL = functionBuilder() //
        .name("ceil") //
        .description("""
                Round x to nearest larger integer.

                If the input is `MISSING` or `NaN`, the output will be `MISSING`.

                `ceil(NaN)` returns `MISSING`.
                """) //
        .examples("""
                * `ceil(NaN)` returns `MISSING`
                * `ceil(2.5)` returns 3
                * `ceil(-2.5)` returns -2
                * `ceil(1.0)` returns 1
                """) //
        .keywords() //
        .category(CATEGORY_ROUND) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Nearest integer greater than x", RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args)))//
        .impl(MathFunctions::ceilImpl) //
        .build();

    private static Computer ceilImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return IntegerComputer.of( //
            ctx -> (long)Math.ceil(c.compute(ctx)), //
            ctx -> {
                if (c.isMissing(ctx)) {
                    return true;
                } else if (Double.isNaN(c.compute(ctx))) {
                    ctx.addWarning("ceil returned MISSING because argument is NaN.");
                    return true;
                } else {
                    return false;
                }
            } //
        );
    }

    /** truncate number, i.e. round towards zero */
    public static final ExpressionFunction TRUNCATE = functionBuilder() //
        .name("truncate") //
        .description("""
                Round x to the nearest integer closer to zero.

                If the input is `MISSING` or `NaN`, the output will be `MISSING`.
                """) //
        .examples("""
                * `truncate(NaN)` returns `MISSING`
                * `truncate(2.5)` returns 2
                * `truncate(-2.5)` returns -2
                * `truncate(1.0)` returns 1
                """) //
        .keywords("round down") //
        .category(CATEGORY_ROUND) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Nearest integer closer to zero than x", RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args)))//
        .impl(MathFunctions::truncImpl) //
        .build();

    private static Computer truncImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return IntegerComputer.of( //
            ctx -> BigDecimal.valueOf(c.compute(ctx)).setScale(0, RoundingMode.DOWN).longValue(), //
            ctx -> {
                if (c.isMissing(ctx)) {
                    return true;
                } else if (Double.isNaN(c.compute(ctx))) {
                    ctx.addWarning("truncate returned MISSING because argument is NaN.");
                    return true;
                } else {
                    return false;
                }
            } //
        );
    }

    private static Function<Arguments<ValueType>, ValueType> roundReturnType = args -> {
        var isValueOptional = args.get("x").isOptional();
        if (args.has(PRECISION)) {
            return FLOAT(isValueOptional);
        } else {
            return INTEGER(isValueOptional);
        }
    };

    /** round towards nearest integer (n + 0.5 rounds to nearest closer to zero) */
    public static final ExpressionFunction ROUNDHALFDOWN = functionBuilder() //
        .name("roundhalfdown") //
        .description("""
                Round x to a certain number of decimal places. In case of an argument halfway
                between two possible rounded values, round to the one closer to zero.

                Negative values of precision will round to the left of the decimal, i.e.
                `roundhalfdown(123.4, -2)` will round to 100.0.

                If the number to round `x` is `MISSING`, the result will also be `MISSING`.

                If the precision argument is not specified, the result will be of type
                INTEGER, otherwise it will be of type FLOAT, even if the precision
                is zero!

                If the precision argument evaluates to `MISSING` the default value of 0 is used
                and a `FLOAT` is returned.

                `roundhalfdown(NaN)` returns `MISSING`, but if a precision is specified it will return NaN.
                The precision argument must be an integer and cannot be `NaN`.
                """) //
        .examples("""
                * Without ambiguity:
                    * `roundhalfdown(1.4)` returns 1
                    * `roundhalfdown(1.6)` returns 2
                    * `roundhalfdown(-1.44, 1)` returns -1.4
                    * `roundhalfdown(-1.4, 0)` returns -1.0
                * Halfway between two possible rounded values:
                    * `roundhalfdown(2.5)` returns 2
                    * `roundhalfdown(-2.5)` returns -2
                    * `roundhalfdown(1.65, 1)` returns 1.6
                """) //
        .keywords() //
        .category(CATEGORY_ROUND) //
        .args( //
            arg("x", "A number", isNumericOrOpt()), //
            optarg(PRECISION, "Number of decimal places in the result", isIntegerOrOpt()) //
        ) //
        .returnType("Nearest value to `x` with `precision` decimal places", RETURN_INTEGER_FLOAT_MISSING,
            roundReturnType) //
        .impl(roundImplFactory(RoundingMode.HALF_DOWN, "roundhalfdown")) //
        .build();

    /** round towards nearest integer (n + 0.5 rounds to nearest further from zero) */
    public static final ExpressionFunction ROUNDHALFUP = functionBuilder() //
        .name("roundhalfup") //
        .description("""
                Round x to a certain number of decimal places. In case of an argument halfway between
                two possible rounded values, round to the one further from zero.

                Negative values of precision will round to the left of the decimal, i.e.
                `roundhalfup(123.45, -2)` will round to 100.0.

                If the number to round `x` is `MISSING`, the result will also be `MISSING`.

                If the precision argument is not specified, the result will be of type
                INTEGER, otherwise it will be of type FLOAT, even if the precision
                is zero!

                If the precision argument evaluates to `MISSING` the default value of 0 is used
                and a `FLOAT` is returned.

                `roundhalfup(NaN)` returns `MISSING`, but if a precision is specified it will return NaN.
                The precision argument must be an integer and cannot be `NaN`.
                """) //
        .examples("""
                * Without ambiguity:
                      * `roundhalfup(1.4)` returns 1
                      * `roundhalfup(1.6)` returns 2
                      * `roundhalfup(-1.44, 1)` returns -1.4
                      * `roundhalfup(-1.4, 0)` returns -1.0
                * Halfway between two possible rounded values:
                      * `roundhalfup(2.5)` returns 3
                      * `roundhalfup(-2.5)` returns -3
                      * `roundhalfup(1.65, 1)` returns 1.7
                """) //
        .keywords() //
        .category(CATEGORY_ROUND) //
        .args( //
            arg("x", "A number", isNumericOrOpt()), //
            optarg(PRECISION, "Number of decimal places in the result", isIntegerOrOpt()) //
        ) //
        .returnType("Nearest value to `x` with `precision` decimal places", RETURN_INTEGER_FLOAT_MISSING,
            roundReturnType) //
        .impl(roundImplFactory(RoundingMode.HALF_UP, "roundhalfup")) //
        .build();

    /** round towards nearest integer (n + 0.5 rounds to nearest even) */
    public static final ExpressionFunction ROUNDHALFEVEN = functionBuilder() //
        .name("round") //
        .description("""
                Round x to a certain number of decimal places. In case of an argument halfway between
                two possible rounded values, round to the one that ends with an even
                digit.

                Negative values of precision will round to the left of the decimal, i.e.
                `round(123.45, -2)` will round to 100.0.

                If the number to round `x` is `MISSING`, the result will also be `MISSING`.

                If the precision argument is not specified, the result will be of type
                INTEGER, otherwise it will be of type FLOAT, even if the precision
                is zero!

                If the precision argument evaluates to `MISSING` the default value of 0 is used
                and a `FLOAT` is returned.

                `round(NaN)` returns `MISSING`, but if a precision is specified it will return NaN.
                The precision argument must be an integer and cannot be `NaN`.
                """) //
        .examples("""
                * Without ambiguity:
                  * `round(1.4)` returns 1
                  * `round(1.6)` returns 2
                  * `round(-1.44, 1)` returns -1.4
                  * `round(-1.4, 0)` returns -1.0

                * Halfway between two possible rounded values:
                  * `round(2.5)` returns 2
                  * `round(-2.5)` returns -2
                  * `round(3.5)` returns 4
                  * `round(-3.5)` returns -4
                  * `round(1.65, 1)` returns 1.6
                """) //
        .keywords("roundhalfeven") //
        .category(CATEGORY_ROUND) //
        .args( //
            arg("x", "A number", isNumericOrOpt()), //
            optarg(PRECISION, "Number of decimal places in the result", isIntegerOrOpt()) //
        ) //
        .returnType("Nearest value to `x` with `precision` decimal places", RETURN_INTEGER_FLOAT_MISSING,
            roundReturnType) //
        .impl(roundImplFactory(RoundingMode.HALF_EVEN, "round")) //
        .build();

    /**
     * Factory for round function implementations.
     *
     * @param mode the rounding mode
     * @return the function implementation
     */
    private static Function<Arguments<Computer>, Computer> roundImplFactory(final RoundingMode mode,
        final String functionName) {
        return args -> {
            var c = toFloat(args.get("x"));

            if (args.has(PRECISION)) {
                return FloatComputer.of(ctx -> {

                    var precisionArgument = args.get(PRECISION);

                    int scale = precisionArgument.isMissing(ctx) //
                        ? 0 //
                        : FunctionUtils.toIntExact(toInteger(precisionArgument).compute(ctx), "%s arg was too large.",
                            PRECISION);
                    double value = c.compute(ctx);

                    if (Double.isNaN(value)) {
                        ctx.addWarning("%s returned NaN because argument is NaN.".formatted(functionName));
                        return Double.NaN;
                    } else {
                        return BigDecimal.valueOf(value).setScale(scale, mode).doubleValue();
                    }
                }, c::isMissing);

            } else {
                return IntegerComputer.of( //
                    ctx -> BigDecimal.valueOf(c.compute(ctx)).setScale(0, mode).longValue(), //
                    ctx -> {
                        if (c.isMissing(ctx)) {
                            return true;
                        } else if (Double.isNaN(c.compute(ctx))) {
                            ctx.addWarning("%s returned MISSING because argument is NaN.".formatted(functionName));
                            return true;
                        }

                        return false;
                    });
            }
        };
    }

    /** The sign of one number */
    public static final ExpressionFunction SIGN = functionBuilder() //
        .name("sign") //
        .description("""
                Get the sign of a number. A positive argument causes the function to return +1,
                a negative argument causes -1, and a zero argument returns 0. If the input is
                `MISSING`, the output will also be `MISSING`. `sign(NaN)` also returns `MISSING`.
                """) //
        .examples("""
                * `sign(0)` returns 0
                * `sign(42)` returns 1
                * `sign(-0.3)` returns -1
                """) //
        .keywords() //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isNumericOrOpt())) //
        .returnType("Sign of x", RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args)))//
        .impl(MathFunctions::signImpl) //
        .build();

    private static Computer signImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return IntegerComputer.of( //
            ctx -> (int)Math.signum(c.compute(ctx)), //
            ctx -> {
                if (c.isMissing(ctx)) {
                    return true;
                } else if (Double.isNaN(c.compute(ctx))) {
                    ctx.addWarning("sign returned MISSING because argument is NaN.");
                    return true;
                } else {
                    return false;
                }
            } //
        );
    }

    /** The mean of multiple numbers */
    public static final ExpressionFunction AVERAGE = functionBuilder() //
        .name("average") //
        .description("""
                The mean value of a list of numbers. If any argument is
                `MISSING`, the result is also `MISSING`. At least two arguments
                are required, but beyond that you can supply as many as you
                like.

                If any of the numbers are `NaN`, the result will be `NaN`. `NEGATIVE_INFINITY` or
                `POSITIVE_INFINITY` will coerce the result to `NEGATIVE_INFINITY` or `POSITIVE_INFINITY` respectively.
                Mixing `NEGATIVE_INFINITY` and `POSITIVE_INFINITY` will result in `NaN`.
                """) //
        .examples("""
                * `average(2, 4, 6)` returns 4.0
                * `average(1, 2, 3, 4, 5)` returns 3.0
                * `average(1, 2, $["Missing Column"], 4, 5)` returns `MISSING`
                """) //
        .keywords("mean") //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Mean of the arguments", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::averageImpl) //
        .build();

    private static Computer averageImpl(final Arguments<Computer> args) {
        var floatArgs = args.map(Computer::toFloat);
        FloatComputerResultSupplier value = ctx -> argsToDoubleStream(floatArgs, ctx) //
            .average() //
            .getAsDouble();

        return FloatComputer.of(value, anyMissing(args));
    }

    /** The median of multiple numbers */
    public static final ExpressionFunction MEDIAN = functionBuilder() //
        .name("median") //
        .description("""
                The median value of a list of numbers. If any argument is
                `MISSING`, the result is also `MISSING`. At least two arguments
                are required, but beyond that you can supply as many as you
                like.

                Using `NEGATIVE_INFINITY` or `POSITIVE_INFINITY` as an argument affects the result as follows:
                * If the median is influenced by `NEGATIVE_INFINITY` or `POSITIVE_INFINITY`,
                  the result will be `NEGATIVE_INFINITY` or `POSITIVE_INFINITY` respectively.
                * If the median is influenced by `NEGATIVE_INFINITY` and `POSITIVE_INFINITY`, the result will be `NaN`

                If any of the numbers are `NaN`, the result will be `NaN`.
                """) //
        .examples("""
                * `median(2, 4, 6)` returns 4.0
                * `median(2, 4, 6, 1000)` returns 5.0
                * `median(1, 2, $["Missing Column"], 4, 5)` returns `MISSING`
                """) //
        .keywords() //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Median of the arguments", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::medianImpl) //
        .build();

    private static Computer medianImpl(final Arguments<Computer> args) {
        var floatArgs = args.map(Computer::toFloat);
        FloatComputerResultSupplier value = ctx -> {
            // Because we use ::compute we need to do this inside the DoubleSupplier
            var sortedFloatArgs = argsToDoubleStream(floatArgs, ctx) //
                .sorted() //
                .toArray();

            if (Double.isNaN(sortedFloatArgs[sortedFloatArgs.length - 1])) {
                // This can only run if we have an array with at least one NaN value and
                // we are not ignoring NaNs.
                //
                // (works because 'sorted' considers NaN to be > any other element so it
                // will always be last in the array)
                return Double.NaN;
            } else if (sortedFloatArgs.length % 2 == 0) {
                // Median is average of two middle elements if we have an even number of them
                return 0.5
                    * (sortedFloatArgs[sortedFloatArgs.length / 2] + sortedFloatArgs[sortedFloatArgs.length / 2 - 1]);
            } else {
                // Median is middle element if we have an odd number of them
                return sortedFloatArgs[sortedFloatArgs.length / 2];
            }
        };

        return FloatComputer.of(value, anyMissing(args));
    }

    /** The sum of multiple numbers */
    public static final ExpressionFunction SUM = functionBuilder() //
        .name("sum") //
        .description("""
                The sum of a list of numbers. If any argument is `MISSING`, the result
                is also `MISSING`. At least two arguments are required, but beyond that
                you can supply as many as you like. If all inputs are integers, the result
                will be an integer, otherwise it will be a float.

                If any of the numbers are `NaN`, the result will be `NaN`. `NEGATIVE_INFINITY` or
                `POSITIVE_INFINITY` will coerce the result to `NEGATIVE_INFINITY` or `POSITIVE_INFINITY` respectively.
                """) //
        .examples("""
                * `sum(2, 4, 6)` returns 12
                * `sum(1, 2, 3, 4, 5)` returns 15
                * `sum(1, 2, $["Missing Column"], 4, 5)` returns `MISSING`
                """) //
        .keywords("total") //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Sum of the arguments", RETURN_INTEGER_FLOAT_MISSING,
            args -> allBaseTypesMatch(INTEGER::equals, args) ? INTEGER(anyOptional(args)) : FLOAT(anyOptional(args)))//
        .impl(MathFunctions::sumImpl) //
        .build();

    private static Computer sumImpl(final Arguments<Computer> args) {
        boolean allArgsAreIntegers = args.allMatch(IntegerComputer.class::isInstance);

        if (allArgsAreIntegers) {
            var intArgs = args.map(MathFunctions::toInteger);
            return IntegerComputer.of( //
                ctx -> argsToLongStream(intArgs, ctx).sum(), //
                anyMissing(args) //
            );
        } else {
            var floatArgs = args.map(Computer::toFloat);
            return FloatComputer.of( //
                ctx -> argsToDoubleStream(floatArgs, ctx).sum(), //
                anyMissing(args) //
            );
        }
    }

    private static double variance(final double[] values) {
        double sum = 0;
        double sumSq = 0;
        for (double value : values) {
            sum += value;
            sumSq += value * value;
        }
        return (sumSq - sum * sum / values.length) / values.length;
    }

    /** The variance of multiple numbers */
    public static final ExpressionFunction VARIANCE = functionBuilder() //
        .name("variance") //
        .description("""
                The variance of a list of numbers. If any argument is `MISSING`, the result
                is also `MISSING`. At least two arguments are required, but beyond that
                you can supply as many as you like.

                If any of the numbers are `NaN`, the result will be `NaN`.
                """) //
        .examples("""
                * `variance(2, 4, 6)` returns 2.666666…
                * `variance(1, 2, $["Missing Column"], 4, 5)` returns `MISSING`
                """) //
        .keywords() //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Variance of the arguments", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::varianceImpl) //
        .build();

    private static Computer varianceImpl(final Arguments<Computer> args) {
        var floatArgs = args.map(Computer::toFloat);
        return FloatComputer.of( //
            ctx -> variance(argsToDoubles(floatArgs, ctx)), //
            anyMissing(args) //
        );
    }

    /** The standard deviation of multiple numbers */
    public static final ExpressionFunction STDDEV = functionBuilder() //
        .name("stddev") //
        .description("""
                The standard deviation of a list of numbers. If any argument is `MISSING`, the result
                is also `MISSING`. At least two arguments are required, but beyond that
                you can supply as many as you like.

                If any of the numbers are `NaN`, the result will be `NaN`.
                """) //
        .examples("""
                * `stddev(2, 4, 6)` returns 1.632993…
                * `stddev(1, 2, $["Missing Column"], 4, 5)` returns `MISSING`
                """) //
        .keywords() //
        .category(CATEGORY_AGGREGATE) //
        .args( //
            arg("input_1", "First number", isNumericOrOpt()), //
            arg("input_2", "Second number", isNumericOrOpt()), //
            vararg("…", "Additional numbers", isNumericOrOpt()) //
        ) //
        .returnType("Standard deviation of the arguments", RETURN_FLOAT_MISSING, args -> FLOAT(anyOptional(args))) //
        .impl(MathFunctions::stddevImpl) //
        .build();

    private static Computer stddevImpl(final Arguments<Computer> args) {
        var floatArgs = args.map(Computer::toFloat);
        return FloatComputer.of( //
            ctx -> Math.sqrt(variance(argsToDoubles(floatArgs, ctx))), //
            anyMissing(args) //
        );
    }

    /** Binomial coefficient nCr of two numbers */
    public static final ExpressionFunction BINOMIAL = functionBuilder() //
        .name("binomial") //
        .description("""
                The binomial coefficient, also known as "n choose r", is the number
                of ways to choose an unordered subset of r elements from a set of n
                elements. It is also the coefficient of the x^r term in the binomial
                expansion of (1 + x)^n.

                The binomial coefficient is calculated as:

                `binomial(n, r) = n! / (r! * (n-r)!)`

                where `!` denotes the factorial function.

                If either input is `MISSING`, the result will also be `MISSING`. If
                any of the following are true:
                * r < 0
                * n < 0
                * r > n
                a warning will be issued and the result will be 0.
                """) //
        .examples("""
                * `binomial(5, 3)` returns 10
                * `binomial(0, 0)` returns 1
                * `binomial(10, 0)` returns 1
                """) //
        .keywords("choose", "permutations") //
        .category(CATEGORY_DISTRIBUTIONS) //
        .args( //
            arg("n", "n", isIntegerOrOpt()), //
            arg("r", "r", isIntegerOrOpt()) //
        ) //
        .returnType("Binomial coefficient", RETURN_INTEGER_MISSING, args -> INTEGER(anyOptional(args))) //
        .impl(MathFunctions::binomialImpl) //
        .build();

    private static Computer binomialImpl(final Arguments<Computer> args) {
        IntegerComputerResultSupplier value = ctx -> {
            long n = toInteger(args.get("n")).compute(ctx);
            long r = toInteger(args.get("r")).compute(ctx);

            // 0c0 needs special handling
            if (n == 0 && r == 0) {
                return 1;
            }

            if (r > n) {
                ctx.addWarning("binomial returned zero because r > n.");
                return 0;
            }

            if (r < 0) {
                ctx.addWarning("binomial returned zero because r < 0.");
                return 0;
            }

            if (n < 0) {
                ctx.addWarning("binomial returned zero because n < 0.");
                return 0;
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

            for (long d = 1; d <= r; d++) {
                ret *= n;
                ret /= d;

                --n;
            }

            return ret;
        };

        return IntegerComputer.of(value, anyMissing(args));
    }

    /** The normal distribution */
    public static final ExpressionFunction NORMAL = functionBuilder() //
        .name("normal") //
        .description("""
                Compute the probability density function of the normal distribution
                at a given value, mean, and standard deviation.

                If the random variable is `MISSING`, the result will also be
                `MISSING`.

                If the mean is `MISSING` or is not provided, it will be treated as 0.

                If the standard deviation is `MISSING` or not provided, it will be treated as 1.
                If the standard deviation is less than or equal to 0, a
                warning will be issued and the result will be `NaN`.

                If any argument is `NaN` the function returns `NaN`.
                """) //
        .examples("""
                * `normal(1, 0)` returns 0.2419707…
                * `normal(0, 0, 1)` returns 0.398942…
                """) //
        .keywords("gaussian", "distribution", "probability") //
        .category(CATEGORY_DISTRIBUTIONS) //
        .args( //
            arg("x", "Random variable", isNumericOrOpt()), //
            optarg("mean", "Mean value of the normal distribution", isNumericOrOpt()), //
            optarg("standard_deviation", "Standard deviation of the normal distribution", isNumericOrOpt()) //
        ) //
        .returnType("Probability of the random variable for the given normal distribution.", RETURN_FLOAT_MISSING,
            args -> FLOAT(args.get("x").isOptional())) //
        .impl(MathFunctions::normalImpl) //
        .build();

    private static Computer normalImpl(final Arguments<Computer> args) {
        return FloatComputer.of(ctx -> {

            var value = toFloat(args.get("x")).compute(ctx);

            var mean = 0.0;
            if (args.has("mean") && !args.get("mean").isMissing(ctx)) {
                mean = toFloat(args.get("mean")).compute(ctx);
            }

            var standardDeviation = 1.0;
            if (args.has("standard_deviation") && !args.get("standard_deviation").isMissing(ctx)) {
                standardDeviation = toFloat(args.get("standard_deviation")).compute(ctx);
                if (isNearZero(standardDeviation) || standardDeviation < 0) {
                    ctx.addWarning("normal returned NaN because standard deviation <= 0.");
                    return Double.NaN;
                }
            }

            return Math.exp(-Math.pow(value - mean, 2) / (2 * Math.pow(standardDeviation, 2)))
                / (standardDeviation * Math.sqrt(2 * Math.PI));
        }, args.get("x")::isMissing);
    }

    /**
     * Approximation of the error function.
     */
    public static final ExpressionFunction ERROR_FUNCTION = functionBuilder() //
        .name("error_function") //
        .description("""
                Compute the value of the error function at a given point, with the
                specified mean and the standard deviation.

                The error function has the following interpretation: for a random
                variable Y that is normally distributed with mean 0 and standard
                deviation 1/√2, error_function(x) is the probability that Y falls
                in the range [−x, x]. Here error_function is the scaled error
                function, so the mean and standard deviation of the underlying
                distribution can be adjusted.

                The cumulative distribution function of the normal distribution can
                be calculated from the error function:
                `0.5 * (1 + error_function(x))`.

                If the random variable is `MISSING`, the result will also be
                `MISSING`.

                If the mean is `MISSING` or is not provided, it will be treated as 0.

                If the standard deviation is `MISSING` or not provided, it will be treated as 1/√2.
                If the standard deviation is less than or equal to 0, a
                warning will be issued and the result will be `NaN`.

                If any argument is `NaN` the function returns `NaN`.
                """) //
        .examples("""
                * `error_function(0, 0)` returns approx. 0.0
                * `error_function(1, 0)` returns 0.8427007…
                * `error_function(1, 0, 1)` returns 0.682689…
                """) //
        .keywords("gaussian", "distribution", "probability") //
        .category(CATEGORY_DISTRIBUTIONS) //
        .args( //
            arg("x", "Value at which to evaluate the error function", isNumericOrOpt()), //
            optarg("mean", "Mean of the underlying normal distribution", isNumericOrOpt()), //
            optarg("standard_deviation",
                "Standard deviation of the underlying normal distribution. If unspecified, defaults to 1/√2.",
                isNumericOrOpt()) //
        ) //
        .returnType("Probability of getting at least the value", RETURN_FLOAT_MISSING,
            args -> FLOAT(args.get("x").isOptional())) //
        .impl(MathFunctions::errorFunctionImpl) //
        .build();

    private static Computer errorFunctionImpl(final Arguments<Computer> args) {
        return FloatComputer.of(ctx -> {

            var value = toFloat(args.get("x")).compute(ctx);

            var mean = 0.0;
            if (args.has("mean") && !args.get("mean").isMissing(ctx)) {
                mean = toFloat(args.get("mean")).compute(ctx);
            }

            var standardDeviation = 1.0 / Math.sqrt(2);
            if (args.has("standard_deviation") && !args.get("standard_deviation").isMissing(ctx)) {
                standardDeviation = toFloat(args.get("standard_deviation")).compute(ctx);
                if (isNearZero(standardDeviation) || standardDeviation < 0) {
                    ctx.addWarning("error_function returned NaN because standard deviation <= 0.");
                    return Double.NaN;
                }
            }

            double scaledValue = (value - mean) / (standardDeviation * Math.sqrt(2));
            return erf(scaledValue);
        }, args.get("x")::isMissing);
    }

    /**
     * Source: Numerical Recipes in Fortran 77: The Art of Scientific Computing. Cambridge University Press, 1992
     *
     * @param x
     * @return error function result
     */
    private static double erf(final double x) {
        double t = 1 / (1 + 0.5 * Math.abs(x));
        if (x >= 0) {
            return 1 - tau(x, t);
        } else {
            return tau(-x, t) - 1;
        }
    }

    private static double tau(final double x, final double t) {
        double[] tPow = new double[10];
        tPow[0] = 1;
        for (int i = 1; i < 10; i++) {
            tPow[i] = tPow[i - 1] * t;
        }
        return t * Math.exp(-x * x - 1.26551223 + 1.00002368 * t + 0.37409196 * tPow[2] + 0.09678418 * tPow[3]
            - 0.18628806 * tPow[4] + 0.27886807 * tPow[5] - 1.13520398 * tPow[6] + 1.48851587 * tPow[7]
            - 0.82215223 * tPow[8] + 0.17087277 * tPow[9]);
    }

    /** Check if a number is NaN */
    public static final ExpressionFunction IS_NAN = functionBuilder() //
        .name("is_nan") //
        .description("""
                Check if a number is `NaN` (i.e. not a number).
                """) //
        .examples("""
                * `is_nan(0)` returns `FALSE`
                * `is_nan(sqrt(-1))` returns `TRUE`
                * `is_nan($["Missing Column"])` returns `FALSE`
                """) //
        .keywords("NaN") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isFloatOrOpt())) //
        .returnType("`TRUE` if x is `NaN`, `FALSE` otherwise", RETURN_BOOLEAN, args -> BOOLEAN)//
        .impl(MathFunctions::isNanImpl) //
        .build();

    private static Computer isNanImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return BooleanComputer.of(ctx -> !c.isMissing(ctx) && Double.isNaN(c.compute(ctx)), ctx -> false);
    }

    /** Convert NaN to MISSING */
    public static final ExpressionFunction NAN_TO_MISSING = functionBuilder() //
        .name("nan_to_missing") //
        .description("""
                Convert `NaN` to `MISSING`. If the input is not `NaN`, the output will be
                the same as the input.
                """) //
        .examples("""
                * `nan_to_missing(0)` returns 0"
                * `nan_to_missing(sqrt(-1))` returns `MISSING`
                * `nan_to_missing($["Missing Column"])` returns `MISSING`
                """) //
        .keywords("NaN") //
        .category(CATEGORY_GENERAL) //
        .args(arg("x", "A number", isFloatOrOpt())) //
        .returnType("x if x is not `NaN`, `MISSING` otherwise", RETURN_INTEGER_FLOAT_MISSING,
            args -> FLOAT(anyOptional(args)))//
        .impl(MathFunctions::nanToMissingImpl) //
        .build();

    private static Computer nanToMissingImpl(final Arguments<Computer> args) {
        var c = toFloat(args.get("x"));
        return FloatComputer.of( //
            c::compute, //
            ctx -> c.isMissing(ctx) || Double.isNaN(c.compute(ctx)) //
        );
    }

    // ======================= UTILITIES ==============================

    private static IntegerComputer toInteger(final Computer computer) {
        if (computer instanceof IntegerComputer i) {
            return i;
        }
        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static boolean isNearZero(final double d) {
        return isNear(d, 0);
    }

    private static boolean isNear(final double d, final double d2) {
        return Math.abs(d - d2) < 2 * Double.MIN_NORMAL;
    }

    private enum ExtremumType {
            MAXIMUM, MINIMUM
    }

    private static long[] argsToLongs(final Arguments<IntegerComputer> args, final EvaluationContext ctx)
        throws ExpressionEvaluationException {
        var numArgs = args.getNumberOfArguments();
        var argsList = args.toList();
        var values = new long[numArgs];
        for (int i = 0; i < numArgs; i++) {
            values[i] = argsList.get(i).compute(ctx);
        }
        return values;
    }

    private static LongStream argsToLongStream(final Arguments<IntegerComputer> args, final EvaluationContext ctx)
        throws ExpressionEvaluationException {
        return Arrays.stream(argsToLongs(args, ctx));
    }

    private static double[] argsToDoubles(final Arguments<FloatComputer> args, final EvaluationContext ctx)
        throws ExpressionEvaluationException {
        var numArgs = args.getNumberOfArguments();
        var argsList = args.toList();
        var values = new double[numArgs];
        for (int i = 0; i < numArgs; i++) {
            values[i] = argsList.get(i).compute(ctx);
        }
        return values;
    }

    private static DoubleStream argsToDoubleStream(final Arguments<FloatComputer> args, final EvaluationContext ctx)
        throws ExpressionEvaluationException {
        return Arrays.stream(argsToDoubles(args, ctx));
    }

    /**
     * Helper that creates an IntBinaryOperator that can be passed to reduce to extract the index of the largest or
     * smallest value in a stream of doubles. Notably, if the stream contains a NaN, the index of the first NaN will be
     * returned. If there are multiple of the largest or smallest value, the index of the first one will be returned.
     *
     * @param largest if true, extract the index of the largest value, otherwise extract the index of the smallest value
     * @param values
     * @return
     */
    private static IntBinaryOperator nanPropagatingStreamExtremumReducer(final ExtremumType extremum,
        final List<Double> values) {
        return (a, b) -> {
            if (Double.isNaN(values.get(a))) {
                return a;
            } else if (Double.isNaN(values.get(b))) {
                return b;
            } else if (extremum == ExtremumType.MAXIMUM) {
                return values.get(a) >= values.get(b) ? a : b;
            } else {
                return values.get(a) <= values.get(b) ? a : b;
            }
        };
    }

    /**
     * Helper that creates an IntBinaryOperator that can be passed to reduce to extract the index of the largest or
     * smallest value in a stream of longs. If there are multiple copies of the largest or smallest value, the index of
     * the first one will be returned.
     *
     * @param largest if true, extract the index of the largest value, otherwise extract the index of the smallest value
     * @param values
     * @return
     */
    private static IntBinaryOperator streamExtremumReducer(final ExtremumType extremum, final List<Long> values) {
        return (a, b) -> {
            if (extremum == ExtremumType.MAXIMUM) {
                return values.get(a) >= values.get(b) ? a : b;
            } else {
                return values.get(a) <= values.get(b) ? a : b;
            }
        };
    }
}
