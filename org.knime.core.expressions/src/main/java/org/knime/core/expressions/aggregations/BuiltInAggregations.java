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
package org.knime.core.expressions.aggregations;

import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.isBoolean;
import static org.knime.core.expressions.SignatureUtils.isInteger;
import static org.knime.core.expressions.SignatureUtils.optarg;
import static org.knime.core.expressions.ValueType.STRING;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ReturnTypeDescriptions;
import org.knime.core.expressions.SignatureUtils;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.ValueType;

/**
 * Holds the collection of all built-in {@link ColumnAggregation column aggregations}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class BuiltInAggregations {

    private static final String COLUMN_MUST_BE_NUMERIC = "Column data type must be numeric.";

    /**
     * The major version of the built-in aggregations. This version number should be incremented whenever incompatible
     * changes are introduced to the built-in aggregations. Incompatible changes include, but are not limited to,
     * removing aggregations, changing signatures (while not supporting the old signature), or altering aggregation
     * behaviors in a way that could break existing expressions.
     */
    public static final int AGGREGATIONS_VERSION = 1;

    private BuiltInAggregations() {
    }

    /** The category for all built-in aggregations */
    public static final OperatorCategory AGGREGATION_CATEGORY = new OperatorCategory("Math – Aggregate columns", """
            The "Math – Aggregate Columns" category in KNIME Expression language includes functions that perform
            aggregations over all rows of a column. These functions are distinct from other aggregations as they
            operate across the entire column rather than individual values or subsets. They are essential for
            summarizing and analyzing data at the column level within expressions. Note that aggregation functions
            utilize the entire column as provided in the input. They do not use the output of the expression
            if applied on a column configured to be replaced.
            """);

    /** The list of all built-in aggregation categories */
    public static final List<OperatorCategory> BUILT_IN_CATEGORIES = List.of(AGGREGATION_CATEGORY);

    // Helper constants
    private static final String COLUMN_ARG_ID = "column";

    private static final Arg COLUMN_ARG = arg(COLUMN_ARG_ID, "The name of the column to aggregate",
        new SignatureUtils.ArgMatcherImpl("COLUMN", STRING::equals));

    private static final String IGNORE_NAN_ARG_ID = "ignore_nan";

    private static final Arg IGNORE_NAN_ARG =
        optarg(IGNORE_NAN_ARG_ID, "Whether to skip `NaN` values (defaults to `FALSE`)", isBoolean());

    private static final String COLUMN_ARG_MUST_BE_STRING_ERR = "Column argument must be a string.";

    private static final String IGNORE_NAN_MUST_BE_BOOLEAN = "ignore_nan must be a boolean.";

    // Aggregation implementations
    /** Aggregation that returns the maximum value of a column. */
    public static final ColumnAggregation MAX = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MAX") //
        .description("""
                Find the maximum value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`. The result has
                the same type as the column.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `FALSE`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.
                """) //
        .examples(
            """
                * `COLUMN_MAX("col")` returns the maximum value in column `col`,
                  including `NaN` values
                * `COLUMN_MAX("col", ignore_nan=true)` returns the maximum value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MAX("col", false)` returns the maximum value in
                  column `col`, including `NaN` values
                """) //
        .keywords("maximum", "max") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args(COLUMN_ARG, IGNORE_NAN_ARG) //
        .returnType("The maximum value of the column", ReturnTypeDescriptions.RETURN_INTEGER_FLOAT_MISSING,
            BuiltInAggregations::maxReturnType) //
        .build();

    private static ReturnResult<ValueType> maxReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC);
    }

    /** Aggregation that returns the minimum value of a column. */
    public static final ColumnAggregation MIN = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MIN") //
        .description("""
                Find the minimum value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`. The result has
                the same type as the column.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `FALSE`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.
                """) //
        .examples(
            """
                * `COLUMN_MIN("col")` returns the minimum value in column `col`,
                  including `NaN` values
                * `COLUMN_MIN("col", ignore_nan=true)` returns the minimum value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MIN("col", false)` returns the minimum value in
                  column `col`, including `NaN` values
                """) //
        .keywords("minimum") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args(COLUMN_ARG, IGNORE_NAN_ARG) //
        .returnType("The minimum value of the column", ReturnTypeDescriptions.RETURN_INTEGER_FLOAT_MISSING,
            BuiltInAggregations::minReturnType) //
        .build();

    private static ReturnResult<ValueType> minReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC);
    }

    /** Aggregation that returns the mean value of a column. */
    public static final ColumnAggregation AVERAGE = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_AVERAGE") //
        .description("""
                Find the mean value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `FALSE`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.
                """) //
        .examples(
            """
                * `COLUMN_AVERAGE("col")` returns the mean value in column `col`,
                  including `NaN` values
                * `COLUMN_AVERAGE("col", ignore_nan=true)` returns the mean value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_AVERAGE("col", false)` returns the mean value in column `col`,
                  including `NaN` values
                """) //
        .keywords("column_mean", "avg") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args(COLUMN_ARG, IGNORE_NAN_ARG) //
        .returnType("The mean value of the column", ReturnTypeDescriptions.RETURN_FLOAT_MISSING,
            BuiltInAggregations::meanReturnType) //
        .build();

    private static ReturnResult<ValueType> meanReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        // TODO(AP-23168) replace some of these checks with the usage of SignatureUtils.checkTypes
        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC) //
            .map(type -> ValueType.OPT_FLOAT);
    }

    /** Aggregation that returns the median value of a column. */
    public static final ColumnAggregation MEDIAN = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MEDIAN") //
        .description("""
                Find the median value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `FALSE`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.
                """) //
        .examples(
            """
                * `COLUMN_MEDIAN("col")` returns the median value in column `col`,
                  including `NaN` values
                * `COLUMN_MEDIAN("col", ignore_nan=true)` returns the median value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MEDIAN("col", false)` returns the median value in column `col`,
                  including `NaN` values
                """) //
        .keywords("average", "avg") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args(COLUMN_ARG, IGNORE_NAN_ARG) //
        .returnType("The median value of the column", ReturnTypeDescriptions.RETURN_FLOAT_MISSING,
            BuiltInAggregations::medianReturnType) //
        .build();

    private static ReturnResult<ValueType> medianReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC) //
            .map(type -> ValueType.OPT_FLOAT);
    }

    /** Aggregation that returns the sum of a column. */
    public static final ColumnAggregation SUM = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_SUM") //
        .description("""
                Find the sum of the values in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`. The result has the
                same type as the column.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, and if all values are `NaN`,
                the result is 0. If it is `FALSE`, then `NaN` values are not ignored
                and the result is `NaN` if any value in the column is `NaN`.
                """) //
        .examples(
            """
                * `COLUMN_SUM("col")` returns the sum of the values in column `col`,
                  including `NaN` values
                * `COLUMN_SUM("col", ignore_nan=true)` returns the sum of the values in
                  column `col`, ignoring `NaN` values
                * `COLUMN_SUM("col", false)` returns the sum of the values in column `col`,
                  including `NaN` values
                """) //
        .keywords("sum", "total") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args(COLUMN_ARG, IGNORE_NAN_ARG) //
        .returnType("The sum of the column", ReturnTypeDescriptions.RETURN_INTEGER_FLOAT,
            BuiltInAggregations::sumReturnType) //
        .build();

    private static ReturnResult<ValueType> sumReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC);
    }

    /** Aggregation that returns the variance of a column. */
    public static final ColumnAggregation VARIANCE = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_VARIANCE") //
        .description("""
                Find the variance of the values in a column, ignoring `MISSING` values.
                If all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `FALSE`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.
                """) //
        .examples(
            """
                * `COLUMN_VARIANCE("col")` returns the variance of the values in column `col`,
                  including `NaN` values
                * `COLUMN_VARIANCE("col", ignore_nan=true)` returns the variance of the values in
                  column `col`, ignoring `NaN` values
                * `COLUMN_VARIANCE("col", false)` returns the variance of the values in column `col`,
                  including `NaN` values
                """) //
        .keywords("var", "variation") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args(COLUMN_ARG, IGNORE_NAN_ARG) //
        .returnType("The variance of the column", ReturnTypeDescriptions.RETURN_FLOAT_MISSING,
            BuiltInAggregations::varianceReturnType) //
        .build();

    private static ReturnResult<ValueType> varianceReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC) //
            .map(type -> ValueType.OPT_FLOAT);
    }

    /** Aggregation that returns the standard deviation of a column. */
    public static final ColumnAggregation STD_DEV = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_STDDEV") //
        .description("""
                Find the standard deviation of the values in a column, ignoring
                `MISSING` values. If all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `TRUE`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `FALSE`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                There is also an optional argument for the delta degrees of freedom
                (ddof) to use for the calculation. By default it is set to 0, but this
                can be changed to 1 to calculate the corrected sample standard deviation.
                See [wikipedia:standard_deviation](https://en.wikipedia.org/wiki/Standard_deviation).
                Other integer values are also accepted.
                """) //
        .examples(
            """
                * `COLUMN_STDDEV("col")` returns the standard deviation of the values in column `col`,
                  including `NaN` values
                * `COLUMN_STDDEV("col", ignore_nan=true)` returns the standard deviation of the values in
                  column `col`, ignoring `NaN` values
                * `COLUMN_STDDEV("col", false)` returns the standard deviation of the values in column `col`,
                  including `NaN` values
                * `COLUMN_STDDEV("col", ddof=1)` returns the corrected sample standard deviation of the values in
                  column `col`, including `NaN` values
                """) //
        .keywords("standard deviation", "std") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            COLUMN_ARG, //
            IGNORE_NAN_ARG, //
            optarg("ddof", "The delta degrees of freedom to use (defaults to 0)", isInteger()) //
        ) //
        .returnType("The standard deviation of the column", ReturnTypeDescriptions.RETURN_FLOAT_MISSING,
            BuiltInAggregations::stddevReturnType) //
        .build();

    private static ReturnResult<ValueType> stddevReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 3), "Should have 1-3 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType(IGNORE_NAN_ARG_ID, Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .filter(optArgHasType("ddof", Ast.IntegerConstant.class), "ddof must be an integer") //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .filter(ValueType::isNumericOrOpt, COLUMN_MUST_BE_NUMERIC) //
            .map(type -> ValueType.OPT_FLOAT);
    }

    /** Aggregation that counts the values in a column. */
    public static final ColumnAggregation COUNT = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_COUNT") //
        .description("""
                Count the number of values in a column, with or without `MISSING` values.

                The `ignore_missing` option can be used to skip `MISSING` values. If it is
                set to `TRUE`, `MISSING` values are ignored. If it is `FALSE`, then `MISSING`
                values are not ignored and are counted as well.
                """) //
        .examples(
            """
                * `COLUMN_COUNT("col")` returns the number of values in column `col`,
                  including `MISSING` values
                * `COLUMN_COUNT("col", ignore_missing=true)` returns the number of
                  values in column `col`, ignoring `MISSING` values
                * `COLUMN_COUNT("col", false) - COLUMN_COUNT("col", true)` returns the
                  number of `MISSING`values in column `col`
                """) //
        .keywords("number", "rows") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            COLUMN_ARG, //
            optarg("ignore_missing", "Whether to skip `MISSING` values (defaults to `FALSE`)", isBoolean()) //
        ) //
        .returnType("The number of values in the column", ReturnTypeDescriptions.RETURN_INTEGER,
            BuiltInAggregations::countReturnType) //
        .build();

    private static ReturnResult<ValueType> countReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnTypeMapper) {

        return ReturnResult.success(arguments.getNamedArguments()) //
            .filter(hasNtoMArguments(1, 2), "Should have 1-2 arguments") //
            .filter(columnArgumentIsString(), COLUMN_ARG_MUST_BE_STRING_ERR) //
            .filter(optArgHasType("ignore_missing", Ast.BooleanConstant.class), IGNORE_NAN_MUST_BE_BOOLEAN) //
            .map(args -> args.get(COLUMN_ARG_ID)) //
            .map(Ast.StringConstant.class::cast) //
            .map(Ast.StringConstant::value) //
            .flatMap(columnTypeMapper::apply) //
            .map(arg -> ValueType.INTEGER); //
    }

    private static Predicate<Map<String, ConstantAst>> hasNtoMArguments(final int n, final int m) {
        return args -> args.size() >= n && args.size() <= m;
    }

    private static Predicate<Map<String, ConstantAst>> columnArgumentIsString() {
        return args -> args.get(COLUMN_ARG_ID) instanceof Ast.StringConstant;
    }

    private static Predicate<Map<String, ConstantAst>> optArgHasType(final String argName,
        final Class<? extends ConstantAst> argType) {

        return args -> args.get(argName) == null || argType.isInstance(args.get(argName));
    }

    /** Built-in aggregations */
    public static final List<ColumnAggregation> BUILT_IN_AGGREGATIONS = List.of( //
        MAX, //
        MIN, //
        AVERAGE, //
        MEDIAN, //
        SUM, //
        VARIANCE, //
        STD_DEV, //
        COUNT //
    );

    /** Map of built-in aggregations by name */
    public static final Map<String, ColumnAggregation> BUILT_IN_AGGREGATIONS_MAP = Collections.unmodifiableMap( //
        BUILT_IN_AGGREGATIONS.stream().collect(Collectors.toMap(ColumnAggregation::name, f -> f)) //
    );

}
