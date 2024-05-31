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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.OperatorDescription.Argument;
import org.knime.core.expressions.ValueType;

/**
 * Holds the collection of all built-in {@link ColumnAggregation column aggregations}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class BuiltInAggregations {

    private BuiltInAggregations() {
    }

    /** The category for all built-in aggregations */
    public static final OperatorCategory AGGREGATION_CATEGORY =
        new OperatorCategory("Aggregation", "Aggregation functions");

    /** The list of all built-in aggregation categories */
    public static final List<OperatorCategory> BUILT_IN_CATEGORIES = List.of(AGGREGATION_CATEGORY);

    // Helper constants

    /** The name of the argument which holds the column name */
    private static final String COLUMN_ARG_ID = "column";

    // Aggregation implementations

    /** Aggregation that returns the maximum value of a column. */
    public static final ColumnAggregation MAX = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MAX") //
        .description("""
                Find the maximum value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`. The result has
                the same type as the column.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `false`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                Examples:
                * `COLUMN_MAX("col")` returns the maximum value in column `col`,
                  including `NaN` values
                * `COLUMN_MAX("col", ignore_nan=true)` returns the maximum value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MAX("col", false)` returns the maximum value in
                  column `col`, including `NaN` values
                """) //
        .keywords("maximum") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)") //
        ) //
        .returnType("The maximum value of the column", "INTEGER? | FLOAT?", BuiltInAggregations::maxReturnType) //
        .build();

    private static Optional<ValueType> maxReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(MAX.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2)); // must be only 1/2 arguments

        Optional<ValueType> columnArgType = matchedArgs //
            .map(args -> args.get(COLUMN_ARG_ID)) // type of the column argument
            .map(arg -> arg instanceof Ast.StringConstant colName ? colName.value() : null) // get the column name
            .flatMap(columnType::apply) // get the type of the column
            .filter(ValueType::isNumericOrOpt); // must be numeric

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return ignoreNaNArgValid ? columnArgType : Optional.empty();
    }

    /** Aggregation that returns the minimum value of a column. */
    public static final ColumnAggregation MIN = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MIN") //
        .description("""
                Find the minimum value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`. The result has
                the same type as the column.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `false`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                Examples:
                * `COLUMN_MIN("col")` returns the minimum value in column `col`,
                  including `NaN` values
                * `COLUMN_MIN("col", ignore_nan=true)` returns the minimum value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MIN("col", false)` returns the minimum value in
                  column `col`, including `NaN` values
                """) //
        .keywords("minimum") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)") //
        ) //
        .returnType("The minimum value of the column", "INTEGER? | FLOAT?", BuiltInAggregations::minReturnType) //
        .build();

    private static Optional<ValueType> minReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(MIN.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2)); // must be only 1/2 arguments

        Optional<ValueType> columnArgType = extractColumnType(matchedArgs, columnType) //
            .filter(ValueType::isNumericOrOpt); // must be numeric

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return ignoreNaNArgValid ? columnArgType : Optional.empty();
    }

    /** Aggregation that returns the mean value of a column. */
    public static final ColumnAggregation MEAN = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MEAN") //
        .description("""
                Find the mean value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `false`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                Examples:
                * `COLUMN_MEAN("col")` returns the mean value in column `col`,
                  including `NaN` values
                * `COLUMN_MEAN("col", ignore_nan=true)` returns the mean value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MEAN("col", false)` returns the mean value in column `col`,
                  including `NaN` values
                """) //
        .keywords("average", "avg") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)") //
        ) //
        .returnType("The mean value of the column", "FLOAT?", BuiltInAggregations::meanReturnType) //
        .build();

    private static Optional<ValueType> meanReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(MEAN.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2));

        var columnArgValid = extractColumnType(matchedArgs, columnType) //
            .filter(ValueType::isNumericOrOpt) // must be numeric
            .isPresent();

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return (ignoreNaNArgValid && columnArgValid) //
            ? Optional.of(ValueType.OPT_FLOAT) //
            : Optional.empty();
    }

    /** Aggregation that returns the median value of a column. */
    public static final ColumnAggregation MEDIAN = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MEDIAN") //
        .description("""
                Find the median value in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `false`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                Examples:
                * `COLUMN_MEDIAN("col")` returns the median value in column `col`,
                  including `NaN` values
                * `COLUMN_MEDIAN("col", ignore_nan=true)` returns the median value in
                  column `col`, ignoring `NaN` values
                * `COLUMN_MEDIAN("col", false)` returns the median value in column `col`,
                  including `NaN` values
                """) //
        .keywords("average", "avg") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)") //
        ) //
        .returnType("The median value of the column", "FLOAT", BuiltInAggregations::medianReturnType) //
        .build();

    private static Optional<ValueType> medianReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(MEDIAN.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2));

        var columnArgValid = extractColumnType(matchedArgs, columnType) //
            .filter(ValueType::isNumericOrOpt) // must be numeric
            .isPresent();

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return (ignoreNaNArgValid && columnArgValid) //
            ? Optional.of(ValueType.OPT_FLOAT) //
            : Optional.empty();
    }

    /** Aggregation that returns the sum of a column. */
    public static final ColumnAggregation SUM = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_SUM") //
        .description("""
                Find the sum of the values in a column, ignoring `MISSING` values. If
                all values are `MISSING`, the result is `MISSING`. The result has the
                same type as the column.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, and if all values are `NaN`,
                the result is 0. If it is `false`, then `NaN` values are not ignored
                and the result is `NaN` if any value in the column is `NaN`.

                Examples:
                * `COLUMN_SUM("col")` returns the sum of the values in column `col`,
                  including `NaN` values
                * `COLUMN_SUM("col", ignore_nan=true)` returns the sum of the values in
                  column `col`, ignoring `NaN` values
                * `COLUMN_SUM("col", false)` returns the sum of the values in column `col`,
                  including `NaN` values
                """) //
        .keywords("sum", "total") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)") //
        ) //
        .returnType("The sum of the column", "INTEGER | FLOAT", BuiltInAggregations::sumReturnType) //
        .build();

    private static Optional<ValueType> sumReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(SUM.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2));

        Optional<ValueType> columnArgType = extractColumnType(matchedArgs, columnType) //
            .filter(ValueType::isNumericOrOpt); // must be numeric

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return ignoreNaNArgValid ? columnArgType : Optional.empty();
    }

    /** Aggregation that returns the variance of a column. */
    public static final ColumnAggregation VARIANCE = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_VARIANCE") //
        .description("""
                Find the variance of the values in a column, ignoring `MISSING` values.
                If all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `false`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                Examples:
                * `COLUMN_VARIANCE("col")` returns the variance of the values in column `col`,
                  including `NaN` values
                * `COLUMN_VARIANCE("col", ignore_nan=true)` returns the variance of the values in
                  column `col`, ignoring `NaN` values
                * `COLUMN_VARIANCE("col", false)` returns the variance of the values in column `col`,
                  including `NaN` values
                """) //
        .keywords("var", "variation") //
        .category(AGGREGATION_CATEGORY.name()) //
        .args( //
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)") //
        ) //
        .returnType("The variance of the column", "FLOAT?", BuiltInAggregations::varianceReturnType) //
        .build();

    private static Optional<ValueType> varianceReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(VARIANCE.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2));

        var columnArgValid = extractColumnType(matchedArgs, columnType) //
            .filter(ValueType::isNumericOrOpt) // must be numeric
            .isPresent();

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return (ignoreNaNArgValid && columnArgValid) //
            ? Optional.of(ValueType.OPT_FLOAT) //
            : Optional.empty();
    }

    /** Aggregation that returns the standard deviation of a column. */
    public static final ColumnAggregation STD_DEV = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_STDDEV") //
        .description("""
                Find the standard deviation of the values in a column, ignoring
                `MISSING` values. If all values are `MISSING`, the result is `MISSING`.

                The `ignore_nan` option can be used to ignore `NaN` values. If it is
                set to `true`, `NaN` values are ignored, but if all values are `NaN`,
                the result is `MISSING`. If it is `false`, then `NaN` values are not
                ignored and the result is `NaN` if any value in the column is `NaN`.

                There is also an optional argument for the delta degrees of freedom
                (ddof) to use for the calculation. By default it is set to 0, but this
                can be changed to 1 to calculate the corrected sample standard deviation.
                See [wikipedia:standard_deviation](https://en.wikipedia.org/wiki/Standard_deviation).
                Other integer values are also accepted.

                Examples:
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
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_nan", "BOOLEAN", "Whether to skip `NaN` values (defaults to `false`)"), //
            new Argument("ddof", "INTEGER", "The delta degrees of freedom to use (defaults to 0)") //
        ) //
        .returnType("The variance of the column", "FLOAT?", BuiltInAggregations::stddevReturnType) //
        .build();

    private static Optional<ValueType> stddevReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(STD_DEV.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2));

        var columnArgValid = extractColumnType(matchedArgs, columnType) //
            .filter(ValueType::isNumericOrOpt) // must be numeric
            .isPresent();

        boolean ignoreNaNArgValid = optionalArgumentIsValid(matchedArgs, "ignore_nan", Ast.BooleanConstant.class);

        return (ignoreNaNArgValid && columnArgValid) //
            ? Optional.of(ValueType.OPT_FLOAT) //
            : Optional.empty();
    }

    /** Aggregation that counts the values in a column. */
    public static final ColumnAggregation COUNT = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_COUNT") //
        .description("""
                Count the number of values in a column, with or without `MISSING` values.

                The `ignore_missing` option can be used to skip `MISSING` values. If it is
                set to `true`, `MISSING` values are ignored. If it is `false`, then `MISSING`
                values are not ignored and are counted as well.

                Examples:
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
            new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate"), //
            new Argument("ignore_missing", "BOOLEAN", "Whether to skip `MISSING` values (defaults to `false`)") //
        ) //
        .returnType("The number of values in the column", "INTEGER", BuiltInAggregations::countReturnType) //
        .build();

    private static Optional<ValueType> countReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {

        var matchedArgs = Argument.matchSignature(COUNT.description().arguments(), arguments) //
            .filter(hasNtoMArguments(1, 2));

        var columnArgValid = extractColumnType(matchedArgs, columnType).isPresent();

        boolean ignoreMissingArgValid =
            optionalArgumentIsValid(matchedArgs, "ignore_missing", Ast.BooleanConstant.class);

        return (ignoreMissingArgValid && columnArgValid) //
            ? Optional.of(ValueType.INTEGER) //
            : Optional.empty();
    }

    /**
     * Extracts the column type from the given arguments. If the column name argument is missing, has the wrong type, or
     * the given column doesn't exist, the result is an empty optional. Otherwise the optional contains the type of the
     * column.
     *
     * @param matchedArgs the arguments that have been matched to the call signature using
     *            {@link Argument#matchSignature}.
     * @param columnTypeMapper a function that maps a column name to the column type.
     * @return an optional containing the column type if everything about the column argument is valid, otherwise an
     *         empty optional.
     */
    private static Optional<ValueType> extractColumnType(final Optional<Map<String, ConstantAst>> matchedArgs, // NOSONAR - optional here is needed
        final Function<String, Optional<ValueType>> columnTypeMapper) { // as we're handling an optional from elsewhere

        return matchedArgs //
            .map(args -> args.get(COLUMN_ARG_ID)) // type of the column argument
            .filter(Ast.StringConstant.class::isInstance) // must be a string literal
            .map(Ast.StringConstant.class::cast) // cast to string literal Ast node
            .map(Ast.StringConstant::value) // get the column name
            .flatMap(columnTypeMapper::apply); // get the type of the column
    }

    /**
     * Checks if an optional argument is valid. If the argument isn't provided, it's considered to be valid. If the
     * argument is present, it must be of the given type.
     *
     * @param matchedArgs the arguments that have been matched to the call signature using
     *            {@link Argument#matchSignature}.
     * @param argName the name of the argument to check.
     * @param argType the class of the argument type (e.g. Ast.BooleanConstant.class for a boolean).
     * @return true if the argument is missing or of the correct type, false otherwise.
     */
    private static <T extends Ast.ConstantAst> boolean optionalArgumentIsValid(
        final Optional<Map<String, ConstantAst>> matchedArgs, final String argName, final Class<T> argType) { // NOSONAR - optional here is needed
        // because we're processing an optional from elsewhere

        var arg = matchedArgs //
            .map(args -> args.get(argName)); //

        if (arg.isEmpty()) {
            return true;
        }

        return arg //
            .filter(argType::isInstance) //
            .isPresent();
    }

    /** Returns a predicate that checks if the number of arguments is between n and m. */
    private static Predicate<Map<String, ConstantAst>> hasNtoMArguments(final int n, final int m) {
        return args -> args.size() >= n && args.size() <= m;
    }

    /** Built-in aggregations */
    public static final List<ColumnAggregation> BUILT_IN_AGGREGATIONS = List.of( //
        MAX, //
        MIN, //
        MEAN, //
        MEDIAN, //
        SUM, //
        VARIANCE, //
        STD_DEV, //
        COUNT //
    );

    public static final Map<String, ColumnAggregation> BUILT_IN_AGGREGATIONS_MAP = Collections.unmodifiableMap( //
        BUILT_IN_AGGREGATIONS.stream().collect(Collectors.toMap(ColumnAggregation::name, f -> f)) //
    );

}
