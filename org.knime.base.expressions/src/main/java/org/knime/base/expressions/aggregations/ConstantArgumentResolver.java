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
 *   Aug 2, 2024 (kampmann): created
 */
package org.knime.base.expressions.aggregations;

import org.knime.core.data.DataTableSpec;
import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ConstantAst;

/**
 * This class provides utility methods to resolve arguments for aggregations.
 *
 * @author Tobias Kampmann, TNG, Schwerte, Germany
 */
public final class ConstantArgumentResolver {

    static final String COLUMN = "column";

    static final String IGNORE_NAN = "ignore_nan";

    static final String IGNORE_MISSING = "ignore_missing";

    private ConstantArgumentResolver() {
    }

    /**
     * Extracts the column name from the arguments and resolves it to a column index in the table spec. Throws an
     * exception if the column name is not found in the table spec or if the argument is not a string.
     *
     * @param arguments
     * @param tableSpec
     * @return the column index
     */
    public static int resolveColumnIndex(final Arguments<ConstantAst> arguments, final DataTableSpec tableSpec) {

        if (!arguments.has(COLUMN)) {
            throw new IllegalStateException(
                "Implementation error - missing argument for column name. Details: " + arguments);
        }
        var columnArgument = (Ast.StringConstant)arguments.get(COLUMN);

        return tableSpec.findColumnIndex(columnArgument.value());
    }

    /**
     * @param arguments
     * @param name
     * @param defaultValue
     * @return the value of the argument or the default value if the argument
     */
    public static boolean resolveOptionalBoolean(final Arguments<ConstantAst> arguments, final String name,
        final boolean defaultValue) {

        if (!arguments.has(name)) {
            return defaultValue;
        }

        var argument = (Ast.BooleanConstant)arguments.get(name);
        return argument.value();

    }

    /**
     * @param arguments
     * @param name
     * @param defaultValue
     * @return the value of the argument or the default value if the argument
     */
    public static long resolveOptionalInteger(final Arguments<ConstantAst> arguments, final String name,
        final long defaultValue) {

        if (!arguments.has(name)) {
            return defaultValue;
        }
        var argument = (Ast.IntegerConstant)arguments.get(name);
        return argument.value();
    }

}
