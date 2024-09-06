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
 *   Apr 5, 2024 (benjamin): created
 */
package org.knime.base.expressions.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.expressions.ExpressionConstants;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.aggregations.ColumnAggregation;
import org.knime.core.expressions.functions.BuiltInFunctions;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @param categories a list of operator categories
 * @param functions a list of operator descriptions
 */
public record FunctionCatalogData(List<OperatorCategory> categories, List<OperatorDescription> functions) {

    public static final FunctionCatalogData BUILT_IN =
        new FunctionCatalogData(getBuiltInCategories(), getBuiltInOperators(false));

    public static final FunctionCatalogData BUILT_IN_NO_AGGREGATIONS =
        new FunctionCatalogData(getBuiltInCategories(), getBuiltInOperators(true));

    private static List<OperatorDescription> getBuiltInOperators(final boolean skipAggregations) {
        var operators = new ArrayList<OperatorDescription>();

        operators.addAll(BuiltInFunctions.BUILT_IN_FUNCTIONS.stream().map(ExpressionFunction::description).toList());
        if (!skipAggregations) {
            operators.addAll(
                BuiltInAggregations.BUILT_IN_AGGREGATIONS.stream().map(ColumnAggregation::description).toList());
        }
        operators.addAll(
            Arrays.stream(ExpressionConstants.values()).map(ExpressionConstants::toOperatorDescription).toList());

        return operators;
    }

    private static List<OperatorCategory> getBuiltInCategories() {
        var categories = new ArrayList<OperatorCategory>();

        categories.add(ExpressionConstants.CONSTANTS_CATEGORY);
        categories.addAll(BuiltInFunctions.META_CATEGORY_CONTROL);

        categories.addAll(BuiltInFunctions.META_CATEGORY_MATH);
        categories.addAll(BuiltInAggregations.BUILT_IN_CATEGORIES);

        categories.addAll(BuiltInFunctions.META_CATEGORY_STRING);

        return categories;
    }
}
