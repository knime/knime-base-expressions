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

import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.arg;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isString;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isStringOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.optarg;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.ValueType;

/**
 * Implementation of built-in functions that manipulate strings.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class StringFunctions {

    private StringFunctions() {
    }

    /** The "String Manipulation" category */
    public static final FunctionCategory CATEGORY =
        new FunctionCategory("String Manipulation", "Functions that operate on strings");

    /** Compare two strings */
    public static final ExpressionFunction COMPARE = functionBuilder() //
        .name("compare") //
        .description("Compares two strings lexicographically") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the first string", isStringOrOpt()), //
            arg("y", "the second string", isStringOrOpt()) //
        ) //
        .returnType("-1 if x < y, 0 if x == y, and 1 if x > y", "INTEGER?",
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::compareImpl) //
        .build();

    private static Computer compareImpl(final List<Computer> args) {
        var c1 = (StringComputer)args.get(0);
        var c2 = (StringComputer)args.get(1);
        return IntegerComputer.of(() -> c1.compute().compareTo(c2.compute()), () -> c1.isMissing() || c2.isMissing());
    }

    /** Check if one string contains another string */
    public static final ExpressionFunction CONTAINS = functionBuilder() //
        .name("contains") //
        .description("Checks if a string contains another string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("search", "the search term", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for ignore case (root locale)", isString()) //
        ) //
        .returnType("true if str contains the search term, false otherwise", "BOOLEAN?",
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::containsImpl) //
        .build();

    private static Computer containsImpl(final List<Computer> args) {
        var c1 = (StringComputer)args.get(0);
        var c2 = (StringComputer)args.get(1);

        // Modifiers
        final BooleanSupplier ignoreCase;
        if (args.size() == 3) {
            // modifier present
            var modifier = (StringComputer)args.get(2);
            ignoreCase = () -> modifier.compute().contains("i");
        } else {
            ignoreCase = () -> false;
        }

        return BooleanComputer.of(() -> {
            if (ignoreCase.getAsBoolean()) {
                return c1.compute().toLowerCase(Locale.ROOT).contains(c2.compute().toLowerCase(Locale.ROOT));
            } else {
                return c1.compute().contains(c2.compute());
            }
        }, () -> c1.isMissing() || c2.isMissing());
    }
}
