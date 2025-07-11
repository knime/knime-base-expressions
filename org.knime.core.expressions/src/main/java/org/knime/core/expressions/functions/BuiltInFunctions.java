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
import org.knime.core.expressions.functions.temporal.TemporalArithmeticFunctions;
import org.knime.core.expressions.functions.temporal.TemporalCreateExtractFunctions;
import org.knime.core.expressions.functions.temporal.TemporalParseFormatFunctions;
import org.knime.core.expressions.functions.temporal.TemporalZoneManipulationFunctions;

/**
 * Holds the collection of all built-in {@link ExpressionFunction functions} and {@link OperatorCategory function
 * categories}.
 *
 * Note that new categories added here should be registered in FunctionCatalogData in org.knime.base.expressions.
 * Otherwise, the expression editor will crash at runtime without a helpful error message.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class BuiltInFunctions {

    /**
     * The major version of the built-in functions. This version number should be incremented whenever incompatible
     * changes are introduced to the built-in functions. Incompatible changes include, but are not limited to, removing
     * functions, changing signatures (while not supporting the old signature), or altering function behaviors in a way
     * that could break existing expressions.
     */
    public static final int FUNCTIONS_VERSION = 1;

    private BuiltInFunctions() {
    }

    /** List of all maths function categories */
    public static final List<OperatorCategory> META_CATEGORY_MATH = List.of( //
        MathFunctions.CATEGORY_GENERAL, //
        MathFunctions.CATEGORY_ROUND, //
        MathFunctions.CATEGORY_AGGREGATE, //
        MathFunctions.CATEGORY_TRIGONOMETRY, //
        MathFunctions.CATEGORY_DISTRIBUTIONS //
    );

    /** List of all string manipulation categories */
    public static final List<OperatorCategory> META_CATEGORY_STRING = List.of( //
        StringFunctions.CATEGORY_GENERAL, //
        StringFunctions.CATEGORY_MATCH_COMPARE, //
        StringFunctions.CATEGORY_EXTRACT_REPLACE, //
        StringFunctions.CATEGORY_CLEAN, //
        StringFunctions.CATEGORY_ENCODE //
    );

    /** List of all temporal manipulation categories */
    public static final List<OperatorCategory> META_CATEGORY_TEMPORAL = List.of( //
        TemporalParseFormatFunctions.CATEGORY_PARSE_FORMAT, //
        TemporalCreateExtractFunctions.CATEGORY_CREATE_EXTRACT, //
        TemporalArithmeticFunctions.CATEGORY_ARITHMETIC, //
        TemporalZoneManipulationFunctions.CATEGORY_ZONE_MANIPULATION //
    );

    /** List of all control flow categories */
    public static final List<OperatorCategory> META_CATEGORY_CONTROL = List.of( //
        ControlFlowFunctions.CATEGORY //
    );

    /** Built-in functions */
    public static final List<ExpressionFunction> BUILT_IN_FUNCTIONS = List.of( //
        // Condition
        ControlFlowFunctions.IF, //
        ControlFlowFunctions.SWITCH, //
        // Math – General
        MathFunctions.POW, //
        MathFunctions.SQRT, //
        MathFunctions.MOD, //
        MathFunctions.ABS, //
        MathFunctions.SIGN, //
        MathFunctions.EXP, //
        MathFunctions.LN, //
        MathFunctions.LOG10, //
        MathFunctions.LOG2, //
        MathFunctions.LOG_BASE, //
        MathFunctions.LOG1P, //
        MathFunctions.IS_NAN, //
        MathFunctions.NAN_TO_MISSING, //
        // Math – Round
        MathFunctions.ROUNDHALFUP, //
        MathFunctions.ROUNDHALFDOWN, //
        MathFunctions.ROUNDHALFEVEN, //
        MathFunctions.TRUNCATE, //
        MathFunctions.CEIL, //
        MathFunctions.FLOOR, //
        // Math – Aggregate
        MathFunctions.MAX, //
        MathFunctions.MIN, //
        MathFunctions.ARGMIN, //
        MathFunctions.ARGMAX, //
        MathFunctions.AVERAGE, //
        MathFunctions.MEDIAN, //
        MathFunctions.SUM, //
        MathFunctions.VARIANCE, //
        MathFunctions.STDDEV, //
        // Math – Trigonometry
        MathFunctions.DEGREES, //
        MathFunctions.RADIANS, //
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
        // Math – Distributions
        MathFunctions.BINOMIAL, //
        MathFunctions.NORMAL, //
        MathFunctions.ERROR_FUNCTION, //
        // String – General
        StringFunctions.LOWER_CASE, //
        StringFunctions.UPPER_CASE, //
        StringFunctions.CAPITALIZE, //
        StringFunctions.PAD_START, //
        StringFunctions.PAD_END, //
        StringFunctions.JOIN, //
        StringFunctions.REVERSE, //
        StringFunctions.EMPTY_TO_MISSING, //
        StringFunctions.MISSING_TO_EMPTY, //
        StringFunctions.TO_STRING, //
        StringFunctions.PARSE_BOOL, //
        StringFunctions.PARSE_FLOAT, //
        StringFunctions.PARSE_INT, //
        // String – Match & Compare
        StringFunctions.STARTS_WITH, //
        StringFunctions.ENDS_WITH, //
        StringFunctions.CONTAINS, //
        StringFunctions.LIKE, //
        StringFunctions.REGEX_MATCH, //
        StringFunctions.COMPARE, //
        // String – Extract & Replace
        StringFunctions.FIRST_CHARS, //
        StringFunctions.LAST_CHARS, //
        StringFunctions.SUBSTRING, //
        StringFunctions.REGEX_EXTRACT, //
        StringFunctions.REPLACE, //
        StringFunctions.REGEX_REPLACE, //
        StringFunctions.FIND, //
        StringFunctions.FIND_CHARS, //
        StringFunctions.COUNT, //
        StringFunctions.COUNT_CHARS, //
        StringFunctions.LENGTH, //
        // String – Clean
        StringFunctions.REPLACE_CHARS, //
        StringFunctions.REPLACE_DIACRITICS, //
        StringFunctions.REPLACE_UMLAUTS, //
        StringFunctions.REMOVE_CHARS, //
        StringFunctions.REMOVE_DUPLICATE_SPACES, //
        StringFunctions.STRIP, //
        StringFunctions.STRIP_START, //
        StringFunctions.STRIP_END, //
        // String – Encode
        StringFunctions.XML_ENCODE, //
        StringFunctions.URL_ENCODE, //
        StringFunctions.URL_DECODE, //
        // Temporal – Create & Extract
        TemporalCreateExtractFunctions.MAKE_DATE, //
        TemporalCreateExtractFunctions.MAKE_TIME, //
        TemporalCreateExtractFunctions.MAKE_DATETIME, //
        TemporalCreateExtractFunctions.MAKE_ZONED, //
        TemporalCreateExtractFunctions.MAKE_TIME_DURATION, //
        TemporalCreateExtractFunctions.MAKE_DATE_DURATION, //
        TemporalCreateExtractFunctions.NOW, //
        TemporalCreateExtractFunctions.TODAY, //
        TemporalCreateExtractFunctions.EXTRACT_YEAR, //
        TemporalCreateExtractFunctions.EXTRACT_MONTH, //
        TemporalCreateExtractFunctions.EXTRACT_DAY_OF_MONTH, //
        TemporalCreateExtractFunctions.EXTRACT_HOUR, //
        TemporalCreateExtractFunctions.EXTRACT_MINUTE, //
        TemporalCreateExtractFunctions.EXTRACT_SECOND, //
        TemporalCreateExtractFunctions.EXTRACT_NANOSECOND, //
        TemporalCreateExtractFunctions.EXTRACT_DATE, //
        TemporalCreateExtractFunctions.EXTRACT_TIME, //
        TemporalCreateExtractFunctions.EXTRACT_DATETIME, //
        TemporalCreateExtractFunctions.TO_HOURS, //
        TemporalCreateExtractFunctions.TO_MINUTES, //
        TemporalCreateExtractFunctions.TO_SECONDS, //
        // Temporal – Parse & Format
        TemporalParseFormatFunctions.PARSE_DATE, //
        TemporalParseFormatFunctions.PARSE_TIME, //
        TemporalParseFormatFunctions.PARSE_DATE_TIME, //
        TemporalParseFormatFunctions.PARSE_ZONED_DATE_TIME, //
        TemporalParseFormatFunctions.PARSE_TIME_DURATION, //
        TemporalParseFormatFunctions.PARSE_DATE_DURATION, //
        TemporalParseFormatFunctions.FORMAT_DATE, //
        TemporalParseFormatFunctions.FORMAT_TIME, //
        TemporalParseFormatFunctions.FORMAT_DATE_TIME, //
        TemporalParseFormatFunctions.FORMAT_DURATION, //
        // Temporal – Basic logic and arithmetic
        TemporalArithmeticFunctions.TIME_DURATION_BETWEEN, //
        TemporalArithmeticFunctions.DATE_DURATION_BETWEEN, //
        TemporalArithmeticFunctions.ADD_TIME_DURATION, //
        TemporalArithmeticFunctions.ADD_DATE_DURATION, //
        TemporalArithmeticFunctions.YEARS_BETWEEN, //
        TemporalArithmeticFunctions.MONTHS_BETWEEN, //
        TemporalArithmeticFunctions.DAYS_BETWEEN, //
        // Temporal – Timezone manipulation
        TemporalZoneManipulationFunctions.CONVERT_TO_ZONE, //
        TemporalZoneManipulationFunctions.REPLACE_ZONE, //
        TemporalZoneManipulationFunctions.IS_SAME_INSTANT //
    );

    /** Built-in functions as map */
    public static final Map<String, ExpressionFunction> BUILT_IN_FUNCTIONS_MAP = Collections.unmodifiableMap( //
        BUILT_IN_FUNCTIONS.stream().collect(Collectors.toMap(ExpressionFunction::name, f -> f)) //
    );

}
