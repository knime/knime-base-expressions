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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

    public static final OperatorCategory AGGREGATION_CATEGORY =
        new OperatorCategory("Aggregation", "Aggregation functions");

    public static final List<OperatorCategory> BUILT_IN_CATEGORIES = List.of(AGGREGATION_CATEGORY);

    // Helper constants

    private static final String COLUMN_ARG_ID = "column";

    // Aggregation implementations

    /** Aggregation that returns the maximum value of a column. */
    public static final ColumnAggregation MAX = AggregationBuilder.aggregationBuilder() //
        .name("COLUMN_MAX") //
        .description("") // TODO
        .keywords("") // TODO
        .category(AGGREGATION_CATEGORY.name()) //
        .args(new Argument(COLUMN_ARG_ID, "COLUMN", "The name of the column to aggregate")) //
        .returnType("The maximum value of the column", "INTEGER | FLOAT", BuiltInAggregations::maxReturnType) //
        .build();

    private static Optional<ValueType> maxReturnType(final Arguments<ConstantAst> arguments,
        final Function<String, Optional<ValueType>> columnType) {
        return Argument.matchSignature(MAX.description().arguments(), arguments) //
            .filter(args -> args.size() == 1) // must be only 1 argument
            .map(args -> args.get(COLUMN_ARG_ID)) // type of the column argument
            .map(arg -> arg instanceof Ast.StringConstant colName ? colName.value() : null) // get the column name
            .flatMap(columnType::apply) // get the type of the column
            .filter(ValueType::isNumericOrOpt); // must be numeric
    }

    /** Built-in aggregations */
    public static final List<ColumnAggregation> BUILT_IN_AGGREGATIONS = List.of( //
        MAX //
    );

    private static final Map<String, ColumnAggregation> BUILT_IN_AGGREGATIONS_MAP =
        BUILT_IN_AGGREGATIONS.stream().collect(Collectors.toMap(ColumnAggregation::name, f -> f));

    /** A function mapping from function name to the built-in {@link ColumnAggregation column aggregation} */
    public static final Function<String, Optional<ColumnAggregation>> BUILT_IN_AGGREGATIONS_GETTER =
        name -> Optional.ofNullable(BUILT_IN_AGGREGATIONS_MAP.get(name));
}
