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
 *   Apr 4, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.expressions.OperatorCategory;

/**
 * Holds the collection of all built-in {@link ExpressionFunction functions} and {@link OperatorCategory function
 * categories}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class BuiltInFunctions {

    private BuiltInFunctions() {
    }

    /** Built-in function categories */
    public static final List<OperatorCategory> BUILT_IN_CATEGORIES = List.of( //
        MathFunctions.CATEGORY, //
        StringFunctions.CATEGORY //
    );

    /** Built-in functions */
    public static final List<ExpressionFunction> BUILT_IN_FUNCTIONS = List.of( //
        MathFunctions.MAX, //
        MathFunctions.MIN, //
        MathFunctions.ARGMAX, //
        MathFunctions.ARGMIN, //
        MathFunctions.ABS, //
        MathFunctions.SIN, //
        MathFunctions.COS, //
        MathFunctions.TAN, //
        MathFunctions.ASIN, //
        MathFunctions.ACOS, //
        MathFunctions.ATAN, //
        MathFunctions.ATAN2, //
        MathFunctions.SINH, //
        MathFunctions.COSH, //
        MathFunctions.TANH, //
        MathFunctions.ASINH, //
        MathFunctions.ACOSH, //
        MathFunctions.ATANH, //
        MathFunctions.LN, //
        MathFunctions.LOG10, //
        MathFunctions.LOG2, //
        MathFunctions.LOG_BASE, //
        MathFunctions.LOG1P, //
        MathFunctions.EXP, //
        MathFunctions.POW, //
        MathFunctions.SQRT, //
        MathFunctions.MOD, //
        MathFunctions.DEGREES, //
        MathFunctions.RADIANS, //
        MathFunctions.FLOOR, //
        MathFunctions.CEIL, //
        MathFunctions.TRUNC, //
        MathFunctions.ROUNDHALFDOWN, //
        MathFunctions.ROUNDHALFUP, //
        MathFunctions.ROUNDHALFEVEN, //
        MathFunctions.SIGN, //
        MathFunctions.AVERAGE, //
        MathFunctions.MEDIAN, //
        MathFunctions.BINOMIAL, //
        MathFunctions.NORMAL, //
        MathFunctions.ERROR_FUNCTION, //
        MathFunctions.IS_NAN, //
        MathFunctions.NAN_TO_MISSING, //
        StringFunctions.COMPARE, //
        StringFunctions.CONTAINS, //
        StringFunctions.STARTS_WITH, //
        StringFunctions.ENDS_WITH, //
        StringFunctions.LIKE, //
        StringFunctions.REGEX_MATCH, //
        StringFunctions.REGEX_EXTRACT, //
        StringFunctions.REGEX_REPLACE, //
        StringFunctions.REPLACE, //
        StringFunctions.REPLACE_CHARS, //
        StringFunctions.REPLACE_UMLAUTS, //
        StringFunctions.REPLACE_DIACRITICS, //
        StringFunctions.LOWER_CASE, //
        StringFunctions.UPPER_CASE, //
        StringFunctions.CAPITALIZE, //
        StringFunctions.PAD_START, //
        StringFunctions.PAD_END, //
        StringFunctions.JOIN, //
        StringFunctions.SUBSTRING, //
        StringFunctions.FIRST_CHARS, //
        StringFunctions.LAST_CHARS, //
        StringFunctions.REMOVE_CHARS, //
        StringFunctions.STRIP, //
        StringFunctions.STRIP_START, //
        StringFunctions.STRIP_END, //
        StringFunctions.REMOVE_DUPLICATE_SPACES, //
        StringFunctions.REVERSE, //
        StringFunctions.EMPTY_TO_MISSING, //
        StringFunctions.MISSING_TO_EMPTY, //
        StringFunctions.LENGTH, //
        StringFunctions.COUNT, //
        StringFunctions.COUNT_CHARS, //
        StringFunctions.FIND, //
        StringFunctions.FIND_CHARS, //
        StringFunctions.CHECKSUM_MD5, //
        StringFunctions.XML_ENCODE, //
        StringFunctions.URL_ENCODE, //
        StringFunctions.URL_DECODE, //
        StringFunctions.TO_STRING, //
        StringFunctions.PARSE_FLOAT, //
        StringFunctions.PARSE_INT, //
        StringFunctions.PARSE_BOOL, //
        ControlFlowFunctions.IF, //
        ControlFlowFunctions.SWITCH //

    );

    /** Built-in functions as map */
    public static final Map<String, ExpressionFunction> BUILT_IN_FUNCTIONS_MAP = Collections.unmodifiableMap( //
        BUILT_IN_FUNCTIONS.stream().collect(Collectors.toMap(ExpressionFunction::name, f -> f)) //
    );

}
