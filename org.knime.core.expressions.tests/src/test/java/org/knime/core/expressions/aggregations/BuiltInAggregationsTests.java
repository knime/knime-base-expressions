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
 *   May 24, 2024 (benjamin): created
 */
package org.knime.core.expressions.aggregations;

import static org.knime.core.expressions.AstTestUtils.STR;
import static org.knime.core.expressions.aggregations.ColumnAggregationTestBuilder.args;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.knime.core.expressions.ValueType;

/**
 * Tests for the built-in aggregations.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class BuiltInAggregationsTests {

    private static final String INT_COL = "intCol";

    private static final String FLOAT_COL = "floatCol";

    private static final String STR_COL = "stringCol";

    private static final Map<String, ValueType> COLUMN_TYPES = Map.of( //
        INT_COL, ValueType.OPT_INTEGER, //
        FLOAT_COL, ValueType.OPT_FLOAT, //
        STR_COL, ValueType.OPT_STRING //
    );

    @TestFactory
    List<DynamicNode> max() {
        return new ColumnAggregationTestBuilder(BuiltInAggregations.MAX, COLUMN_TYPES) //
            .typing("Integer column positional", args().p(STR(INT_COL)).build(), ValueType.OPT_INTEGER) //
            .typing("Integer column named", args().n("column", STR(INT_COL)).build(), ValueType.OPT_INTEGER) //
            .typing("Float column positional", args().p(STR(FLOAT_COL)).build(), ValueType.OPT_FLOAT) //
            .typing("Float column named", args().n("column", STR(FLOAT_COL)).build(), ValueType.OPT_FLOAT) //
            .illegalArgs("No column arg", args().build()) //
            .illegalArgs("String column", args().p(STR(STR_COL)).build()) //
            .illegalArgs("Missing column", args().p(STR("foo")).build()) //
            .tests();

    }
}
