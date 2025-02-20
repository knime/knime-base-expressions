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
 */
package org.knime.core.expressions;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.knime.core.expressions.Ast.BinaryOperator.CONDITIONAL_AND;
import static org.knime.core.expressions.Ast.BinaryOperator.CONDITIONAL_OR;
import static org.knime.core.expressions.Ast.BinaryOperator.DIVIDE;
import static org.knime.core.expressions.Ast.BinaryOperator.EQUAL_TO;
import static org.knime.core.expressions.Ast.BinaryOperator.FLOOR_DIVIDE;
import static org.knime.core.expressions.Ast.BinaryOperator.GREATER_THAN;
import static org.knime.core.expressions.Ast.BinaryOperator.GREATER_THAN_EQUAL;
import static org.knime.core.expressions.Ast.BinaryOperator.LESS_THAN;
import static org.knime.core.expressions.Ast.BinaryOperator.LESS_THAN_EQUAL;
import static org.knime.core.expressions.Ast.BinaryOperator.MISSING_FALLBACK;
import static org.knime.core.expressions.Ast.BinaryOperator.MULTIPLY;
import static org.knime.core.expressions.Ast.BinaryOperator.NOT_EQUAL_TO;
import static org.knime.core.expressions.Ast.BinaryOperator.PLUS;
import static org.knime.core.expressions.Ast.BinaryOperator.REMAINDER;
import static org.knime.core.expressions.Ast.UnaryOperator.MINUS;
import static org.knime.core.expressions.Ast.UnaryOperator.NOT;
import static org.knime.core.expressions.AstTestUtils.AGG;
import static org.knime.core.expressions.AstTestUtils.BOOL;
import static org.knime.core.expressions.AstTestUtils.COL;
import static org.knime.core.expressions.AstTestUtils.FLOAT;
import static org.knime.core.expressions.AstTestUtils.FLOW;
import static org.knime.core.expressions.AstTestUtils.FUN;
import static org.knime.core.expressions.AstTestUtils.INT;
import static org.knime.core.expressions.AstTestUtils.MIS;
import static org.knime.core.expressions.AstTestUtils.OP;
import static org.knime.core.expressions.AstTestUtils.ROW_ID;
import static org.knime.core.expressions.AstTestUtils.ROW_INDEX;
import static org.knime.core.expressions.AstTestUtils.STR;
import static org.knime.core.expressions.SignatureUtils.arg;
import static org.knime.core.expressions.SignatureUtils.hasType;
import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_BOOLEAN;
import static org.knime.core.expressions.ValueType.OPT_DATE_DURATION;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.OPT_LOCAL_TIME;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.OPT_TIME_DURATION;
import static org.knime.core.expressions.ValueType.OPT_ZONED_DATE_TIME;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.core.expressions.Ast.BinaryOperator;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class TypingTest {

    @ParameterizedTest
    @EnumSource(TypingTestCase.class)
    void test(final TypingTestCase params) throws Exception {
        var ast = params.m_expression;
        var outputType = Typing.inferTypes(ast, TEST_COLUMN_TO_TYPE, TEST_FLOWVARIABLE_TO_TYPE);
        assertEquals(params.m_expectedType, outputType, "should fit output type");
        assertEquals(params.m_expectedType, Expressions.getInferredType(ast), "should fit output type");
        assertChildrenHaveTypes(ast);
    }

    private static enum TypingTestCase {

            // === Constants

            CONSTANT_MISSING(MIS(), MISSING), //
            CONSTANT_BOOLEAN(BOOL(true), BOOLEAN), //
            CONSTANT_INTEGER(INT(100), INTEGER), //
            CONSTANT_FLOAT(FLOAT(1.0), FLOAT), //
            CONSTANT_STRING(STR("Foo"), STRING), //

            // === Column access

            INTEGER_COLUMN(COL("i"), INTEGER), //
            OPTIONAL_STRING_COLUMN(COL("s?"), OPT_STRING), //
            ROW_INDEX(ROW_INDEX(), INTEGER), //
            ROW_ID(ROW_ID(), STRING), //

            // === Flow variable access

            INTEGER_FLOW_VARIABLE(FLOW("i"), INTEGER), //
            OPTIONAL_STRING_FLOW_VARIABLE(FLOW("s?"), OPT_STRING), //

            // === Arithmetic Operations

            // Unary Ops
            NEGATION_INTEGER(OP(MINUS, INT(-100)), INTEGER), //
            NEGATION_FLOAT(OP(MINUS, FLOAT(-100.5)), FLOAT), //
            NEGATION_TIME_DURATION(OP(MINUS, COL("d")), TIME_DURATION), //
            NEGATION_DATE_DURATION(OP(MINUS, COL("p")), DATE_DURATION), //
            // optional
            NEGATION_OPTIONAL_INTEGER(OP(MINUS, COL("i?")), OPT_INTEGER), //
            NEGATION_OPTIONAL_FLOAT(OP(MINUS, COL("f?")), OPT_FLOAT), //
            NEGATION_OPTIONAL_TIME_DURATION(OP(MINUS, COL("d?")), OPT_TIME_DURATION), //
            NEGATION_OPTIONAL_DATE_DURATION(OP(MINUS, COL("p?")), OPT_DATE_DURATION), //

            // Binary Ops
            SUM_OF_TWO_INTEGERS(OP(INT(10), PLUS, INT(20)), INTEGER), //
            SUM_OF_INTEGER_AND_FLOAT(OP(INT(10), PLUS, FLOAT(20.1)), FLOAT), //
            MULTIPLICATION_INT_FLOAT(OP(INT(10), MULTIPLY, FLOAT(2.5)), FLOAT), //
            MODULO_TWO_INTEGERS(OP(INT(10), REMAINDER, INT(3)), INTEGER), //
            DIVISION_OF_TWO_INTEGERS(OP(INT(10), DIVIDE, INT(20)), FLOAT), //
            DIVISION_OF_INTEGER_AND_FLOAT(OP(INT(10), DIVIDE, FLOAT(20.1)), FLOAT), //
            DIVISION_OF_TWO_FLOATS(OP(FLOAT(10.0), MULTIPLY, FLOAT(2.0)), FLOAT), //
            FLOOR_DIVISION(OP(INT(10), FLOOR_DIVIDE, INT(20)), INTEGER), //

            // === Time binary operators (non-optional)

            // Time binary operators (temporalamount - temporalamount)
            DIFFERENCE_TIME_DURATION(OP(COL("d"), BinaryOperator.MINUS, COL("d")), TIME_DURATION), //
            DIFFERENCE_DATE_DURATION(OP(COL("p"), BinaryOperator.MINUS, COL("p")), DATE_DURATION), //

            // Time binary operators (temporalamount - temporalamount, both optional)
            DIFFERENCE_OPT_TIME_DURATION(OP(COL("d?"), BinaryOperator.MINUS, COL("d?")), OPT_TIME_DURATION), //
            DIFFERENCE_OPT_DATE_DURATION(OP(COL("p?"), BinaryOperator.MINUS, COL("p?")), OPT_DATE_DURATION), //

            // Time binary operators (temporalamount - temporalamount, one optional)
            DIFFERENCE_TIME_DURATION_OPT(OP(COL("d"), BinaryOperator.MINUS, COL("d?")), OPT_TIME_DURATION), //
            DIFFERENCE_DATE_DURATION_OPT(OP(COL("p"), BinaryOperator.MINUS, COL("p?")), OPT_DATE_DURATION), //

            // Time binary operators (temporal - temporal)
            DIFFERENCE_LOCAL_DATE(OP(COL("ld"), BinaryOperator.MINUS, COL("ld")), DATE_DURATION), //
            DIFFERENCE_LOCAL_TIME(OP(COL("lt"), BinaryOperator.MINUS, COL("lt")), TIME_DURATION), //
            DIFFERENCE_LOCAL_DATE_TIME(OP(COL("ldt"), BinaryOperator.MINUS, COL("ldt")), TIME_DURATION), //
            DIFFERENCE_ZONED_DATE_TIME(OP(COL("zdt"), BinaryOperator.MINUS, COL("zdt")), TIME_DURATION), //

            // Time binary operators (temporal - temporal, both optional)
            DIFFERENCE_OPT_LOCAL_DATE(OP(COL("ld?"), BinaryOperator.MINUS, COL("ld?")), OPT_DATE_DURATION), //
            DIFFERENCE_OPT_LOCAL_TIME(OP(COL("lt?"), BinaryOperator.MINUS, COL("lt?")), OPT_TIME_DURATION), //
            DIFFERENCE_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), BinaryOperator.MINUS, COL("ldt?")), OPT_TIME_DURATION), //
            DIFFERENCE_OPT_ZONED_DATE_TIME(OP(COL("zdt?"), BinaryOperator.MINUS, COL("zdt?")), OPT_TIME_DURATION), //

            // Time binary operators (temporal - temporalamount)
            DIFFERENCE_LOCAL_TIME_TIME_DURATION(OP(COL("lt"), BinaryOperator.MINUS, COL("d")), LOCAL_TIME), //
            DIFFERENCE_LOCAL_DATE_TIME_TIME_DURATION(OP(COL("ldt"), BinaryOperator.MINUS, COL("d")), LOCAL_DATE_TIME), //
            DIFFERENCE_ZONED_DATE_TIME_TIME_DURATION(OP(COL("zdt"), BinaryOperator.MINUS, COL("d")), ZONED_DATE_TIME), //
            DIFFERENCE_LOCAL_DATE_DATE_DURATION(OP(COL("ld"), BinaryOperator.MINUS, COL("p")), LOCAL_DATE), //
            DIFFERENCE_LOCAL_DATE_TIME_DATE_DURATION(OP(COL("ldt"), BinaryOperator.MINUS, COL("p")), LOCAL_DATE_TIME), //
            DIFFERENCE_ZONED_DATE_TIME_DATE_DURATION(OP(COL("zdt"), BinaryOperator.MINUS, COL("p")), ZONED_DATE_TIME), //

            // Time binary operators (temporal - temporalamount, one optional)
            DIFFERENCE_LOCAL_DATE_OPT_DATE_DURATION(OP(COL("ld"), BinaryOperator.MINUS, COL("p?")), OPT_LOCAL_DATE), //
            DIFFERENCE_LOCAL_TIME_OPT_TIME_DURATION(OP(COL("lt"), BinaryOperator.MINUS, COL("d?")), OPT_LOCAL_TIME), //
            DIFFERENCE_LOCAL_DATE_TIME_OPT_TIME_DURATION(OP(COL("ldt"), BinaryOperator.MINUS, COL("d?")),
                OPT_LOCAL_DATE_TIME), //
            DIFFERENCE_ZONED_DATE_TIME_OPT_TIME_DURATION(OP(COL("zdt"), BinaryOperator.MINUS, COL("d?")),
                OPT_ZONED_DATE_TIME), //
            DIFFERENCE_OPT_LOCAL_DATE_DATE_DURATION(OP(COL("ld?"), BinaryOperator.MINUS, COL("p")), OPT_LOCAL_DATE), //
            DIFFERENCE_OPT_LOCAL_TIME_TIME_DURATION(OP(COL("lt?"), BinaryOperator.MINUS, COL("d")), OPT_LOCAL_TIME), //
            DIFFERENCE_OPT_LOCAL_DATE_TIME_TIME_DURATION(OP(COL("ldt?"), BinaryOperator.MINUS, COL("d")),
                OPT_LOCAL_DATE_TIME), //
            DIFFERENCE_OPT_ZONED_DATE_TIME_TIME_DURATION(OP(COL("zdt?"), BinaryOperator.MINUS, COL("d")),
                OPT_ZONED_DATE_TIME), //

            // Time binary operators (temporalamount + temporalamount)
            SUM_DATE_DURATION(OP(COL("p"), BinaryOperator.PLUS, COL("p")), DATE_DURATION), //
            SUM_TIME_DURATION(OP(COL("d"), BinaryOperator.PLUS, COL("d")), TIME_DURATION), //

            // Time binary operators (temporalamount + temporalamount, both optional)
            SUM_OPT_DATE_DURATION(OP(COL("p?"), BinaryOperator.PLUS, COL("p?")), OPT_DATE_DURATION), //
            SUM_OPT_TIME_DURATION(OP(COL("d?"), BinaryOperator.PLUS, COL("d?")), OPT_TIME_DURATION), //

            // Time binary operators (temporalamount + temporalamount, one optional)
            SUM_DATE_DURATION_MIXED(OP(COL("p"), BinaryOperator.PLUS, COL("p?")), OPT_DATE_DURATION), //
            SUM_TIME_DURATION_MIXED(OP(COL("d"), BinaryOperator.PLUS, COL("d?")), OPT_TIME_DURATION), //

            // Time binary operators (temporal + temporalamount)
            SUM_LOCAL_TIME_TIME_DURATION(OP(COL("lt"), BinaryOperator.PLUS, COL("d")), LOCAL_TIME), //
            SUM_LOCAL_DATE_TIME_TIME_DURATION(OP(COL("ldt"), BinaryOperator.PLUS, COL("d")), LOCAL_DATE_TIME), //
            SUM_ZONED_DATE_TIME_TIME_DURATION(OP(COL("zdt"), BinaryOperator.PLUS, COL("d")), ZONED_DATE_TIME), //
            SUM_LOCAL_DATE_DATE_DURATION(OP(COL("ld"), BinaryOperator.PLUS, COL("p")), LOCAL_DATE), //
            SUM_LOCAL_DATE_TIME_DATE_DURATION(OP(COL("ldt"), BinaryOperator.PLUS, COL("p")), LOCAL_DATE_TIME), //
            SUM_ZONED_DATE_TIME_DATE_DURATION(OP(COL("zdt"), BinaryOperator.PLUS, COL("p")), ZONED_DATE_TIME), //

            // Time binary operators (temporal + temporalamount, one optional)
            SUM_LOCAL_DATE_OPT_DATE_DURATION(OP(COL("ld"), BinaryOperator.PLUS, COL("p?")), OPT_LOCAL_DATE), //
            SUM_LOCAL_TIME_OPT_TIME_DURATION(OP(COL("lt"), BinaryOperator.PLUS, COL("d?")), OPT_LOCAL_TIME), //

            // Time binary operators (temporal + temporalamount, both optional)
            SUM_LOCAL_DATE_DATE_DURATION_BOTH_OPT(OP(COL("ld?"), BinaryOperator.PLUS, COL("p")), OPT_LOCAL_DATE), //
            SUM_LOCAL_TIME_TIME_DURATION_BOTH_OPT(OP(COL("lt?"), BinaryOperator.PLUS, COL("d")), OPT_LOCAL_TIME), //

            // Time binary operators (temporalamount * scalar)
            MULTIPLY_TIME_DURATION(OP(COL("d"), BinaryOperator.MULTIPLY, INT(10)), TIME_DURATION), //
            MULTIPLY_DATE_DURATION(OP(COL("p"), BinaryOperator.MULTIPLY, INT(10)), DATE_DURATION), //
            MULTIPLY_TIME_DURATION_INTEGER_FIRST(OP(INT(10), BinaryOperator.MULTIPLY, COL("d")), TIME_DURATION), //
            MULTIPLY_DATE_DURATION_INTEGER_FIRST(OP(INT(10), BinaryOperator.MULTIPLY, COL("p")), DATE_DURATION), //

            // Time binary operators (temporalamount * scalar, one optional)
            MULTIPLY_OPT_TIME_DURATION(OP(COL("d?"), BinaryOperator.MULTIPLY, INT(10)), OPT_TIME_DURATION), //
            MULTIPLY_OPT_DATE_DURATION(OP(COL("p?"), BinaryOperator.MULTIPLY, INT(10)), OPT_DATE_DURATION), //
            MULTIPLY_OPT_TIME_DURATION_INTEGER_FIRST(OP(COL("d"), BinaryOperator.MULTIPLY, COL("i?")),
                OPT_TIME_DURATION), //
            MULTIPLY_OPT_DATE_DURATION_INTEGER_FIRST(OP(COL("p"), BinaryOperator.MULTIPLY, COL("i?")),
                OPT_DATE_DURATION), //

            // Time binary operators (temporalamount * scalar, both optional)
            MULTIPLY_OPT_TIME_DURATION_BOTH_OPT(OP(COL("d?"), BinaryOperator.MULTIPLY, COL("i?")), OPT_TIME_DURATION), //
            MULTIPLY_OPT_DATE_DURATION_BOTH_OPT(OP(COL("p?"), BinaryOperator.MULTIPLY, COL("i?")), OPT_DATE_DURATION), //

            // binary operators - optional
            SUM_OF_INTEGER_AND_OPT_INTEGER(OP(INT(10), PLUS, COL("i?")), OPT_INTEGER), //
            SUM_OF_TWO_OPT_INTEGER(OP(COL("i?"), PLUS, COL("i?")), OPT_INTEGER), //
            DIVISION_OF_OPTIONAL_INTEGERS(OP(INT(10), DIVIDE, COL("i?")), OPT_FLOAT), //
            FLOOR_DIVISION_OPTIONAL(OP(INT(10), FLOOR_DIVIDE, COL("i?")), OPT_INTEGER), //

            // === MISSING Fallback Operator

            FALLBACK_NO_MISSING_INTEGER(OP(INT(10), MISSING_FALLBACK, INT(-100)), INTEGER), //
            FALLBACK_NO_MISSING_FLOAT(OP(FLOAT(10), MISSING_FALLBACK, FLOAT(-100)), FLOAT), //
            FALLBACK_INT_WITH_FLOAT(OP(INT(10), MISSING_FALLBACK, FLOAT(-100)), FLOAT), //
            FALLBACK_FLOAT_WITH_INT(OP(FLOAT(10), MISSING_FALLBACK, INT(-100)), FLOAT), //
            FALLBACK_INT_WITH_FLOAT_FIRST_OPT(OP(COL("i?"), MISSING_FALLBACK, FLOAT(-100)), FLOAT), //
            FALLBACK_FLOAT_WITH_INT_FIRST_OPT(OP(COL("f?"), MISSING_FALLBACK, INT(-100)), FLOAT), //
            FALLBACK_INT_WITH_FLOAT_SECOND_OPT(OP(INT(10), MISSING_FALLBACK, COL("f?")), FLOAT), //
            FALLBACK_FLOAT_WITH_INT_SECOND_OPT(OP(FLOAT(10), MISSING_FALLBACK, COL("i?")), FLOAT), //
            FALLBACK_NO_MISSING_STRING(OP(STR("10"), MISSING_FALLBACK, STR("-100")), STRING), //
            FALLBACK_NO_MISSING_BOOLEAN(OP(BOOL(true), MISSING_FALLBACK, BOOL(false)), BOOLEAN), //

            FALLBACK_FIRST_MISSING(OP(MIS(), MISSING_FALLBACK, INT(10)), INTEGER), //
            FALLBACK_SECOND_MISSING(OP(INT(10), MISSING_FALLBACK, MIS()), INTEGER), //
            FALLBACK_SAME_MIXED_TYPES(OP(STR("s"), MISSING_FALLBACK, COL("s?")), STRING),
            FALLBACK_SAME_OPTIONAL_TYPES(OP(COL("s?"), MISSING_FALLBACK, COL("s?")), OPT_STRING),
            FALLBACK_INT_WITH_FLOAT_BOTH_OPT(OP(COL("i?"), MISSING_FALLBACK, (COL("f?"))), OPT_FLOAT), //
            FALLBACK_FLOAT_WITH_INT_BOTH_OPT(OP(COL("f?"), MISSING_FALLBACK, (COL("i?"))), OPT_FLOAT), //

            // === Comparison Operations

            // Ordering
            GREATER_THAN_INTEGER(OP(INT(5), GREATER_THAN, INT(20)), BOOLEAN), //
            LESS_THAN_INT_FLOAT(OP(INT(5), LESS_THAN, FLOAT(10.0)), BOOLEAN), //
            GREATER_THAN_EQ_OPT_INT(OP(COL("i?"), GREATER_THAN_EQUAL, INT(10)), BOOLEAN), //
            LESS_THAN_EQ_BOTH_OPT(OP(COL("i?"), LESS_THAN_EQUAL, COL("f?")), BOOLEAN), //

            // Time equality
            EQ_LOCAL_DATE(OP(COL("ld"), EQUAL_TO, COL("ld")), BOOLEAN), //
            NEQ_LOCAL_DATE(OP(COL("ld"), NOT_EQUAL_TO, COL("ld")), BOOLEAN), //
            EQ_LOCAL_TIME(OP(COL("lt"), EQUAL_TO, COL("lt")), BOOLEAN), //
            NEQ_LOCAL_TIME(OP(COL("lt"), NOT_EQUAL_TO, COL("lt")), BOOLEAN), //
            EQ_LOCAL_DATE_TIME(OP(COL("ldt"), EQUAL_TO, COL("ldt")), BOOLEAN), //
            NEQ_LOCAL_DATE_TIME(OP(COL("ldt"), NOT_EQUAL_TO, COL("ldt")), BOOLEAN), //
            EQ_TIME_DURATION(OP(COL("d"), EQUAL_TO, COL("d")), BOOLEAN), //
            NEQ_TIME_DURATION(OP(COL("d"), NOT_EQUAL_TO, COL("d")), BOOLEAN), //
            EQ_DATE_DURATION(OP(COL("p"), EQUAL_TO, COL("p")), BOOLEAN), //
            NEQ_DATE_DURATION(OP(COL("p"), NOT_EQUAL_TO, COL("p")), BOOLEAN), //

            // Time equality with two optional cols
            EQ_OPT_LOCAL_DATE(OP(COL("ld?"), EQUAL_TO, COL("ld?")), BOOLEAN), //
            NEQ_OPT_LOCAL_DATE(OP(COL("ld?"), NOT_EQUAL_TO, COL("ld?")), BOOLEAN), //
            EQ_OPT_LOCAL_TIME(OP(COL("lt?"), EQUAL_TO, COL("lt?")), BOOLEAN), //
            NEQ_OPT_LOCAL_TIME(OP(COL("lt?"), NOT_EQUAL_TO, COL("lt?")), BOOLEAN), //
            EQ_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), EQUAL_TO, COL("ldt?")), BOOLEAN), //
            NEQ_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), NOT_EQUAL_TO, COL("ldt?")), BOOLEAN), //

            // Time equality with one optional and one non-optional col
            EQ_LOCAL_DATE_MIXED_OPT(OP(COL("ld?"), EQUAL_TO, COL("ld")), BOOLEAN), //
            NEQ_LOCAL_DATE_MIXED_OPT(OP(COL("ld"), NOT_EQUAL_TO, COL("ld?")), BOOLEAN), //
            EQ_LOCAL_DATE_TIME_MIXED_OPT(OP(COL("ldt"), EQUAL_TO, COL("ldt?")), BOOLEAN), //
            NEQ_LOCAL_DATE_TIME_MIXED_OPT(OP(COL("ldt?"), NOT_EQUAL_TO, COL("ldt")), BOOLEAN), //
            EQ_LOCAL_TIME_MIXED_OPT(OP(COL("lt?"), EQUAL_TO, COL("lt")), BOOLEAN), //
            NEQ_LOCAL_TIME_MIXED_OPT(OP(COL("lt"), NOT_EQUAL_TO, COL("lt?")), BOOLEAN), //

            // Time ordering
            GREATER_THAN_LOCAL_DATE(OP(COL("ld"), GREATER_THAN, COL("ld")), BOOLEAN), //
            LESS_THAN_LOCAL_DATE(OP(COL("ld"), LESS_THAN, COL("ld")), BOOLEAN), //
            GREATER_THAN_EQ_LOCAL_DATE(OP(COL("ld"), GREATER_THAN_EQUAL, COL("ld")), BOOLEAN), //
            LESS_THAN_EQ_LOCAL_DATE(OP(COL("ld"), LESS_THAN_EQUAL, COL("ld")), BOOLEAN), //
            GREATER_THAN_LOCAL_TIME(OP(COL("lt"), GREATER_THAN, COL("lt")), BOOLEAN), //
            LESS_THAN_LOCAL_TIME(OP(COL("lt"), LESS_THAN, COL("lt")), BOOLEAN), //
            GREATER_THAN_EQ_LOCAL_TIME(OP(COL("lt"), GREATER_THAN_EQUAL, COL("lt")), BOOLEAN), //
            LESS_THAN_EQ_LOCAL_TIME(OP(COL("lt"), LESS_THAN_EQUAL, COL("lt")), BOOLEAN), //
            GREATER_THAN_LOCAL_DATE_TIME(OP(COL("ldt"), GREATER_THAN, COL("ldt")), BOOLEAN), //
            LESS_THAN_LOCAL_DATE_TIME(OP(COL("ldt"), LESS_THAN, COL("ldt")), BOOLEAN), //
            GREATER_THAN_EQ_LOCAL_DATE_TIME(OP(COL("ldt"), GREATER_THAN_EQUAL, COL("ldt")), BOOLEAN), //
            LESS_THAN_EQ_LOCAL_DATE_TIME(OP(COL("ldt"), LESS_THAN_EQUAL, COL("ldt")), BOOLEAN), //
            GREATER_THAN_ZONED_DATE_TIME(OP(COL("zdt"), GREATER_THAN, COL("zdt")), BOOLEAN), //
            LESS_THAN_ZONED_DATE_TIME(OP(COL("zdt"), LESS_THAN, COL("zdt")), BOOLEAN), //
            GREATER_THAN_EQ_ZONED_DATE_TIME(OP(COL("zdt"), GREATER_THAN_EQUAL, COL("zdt")), BOOLEAN), //
            LESS_THAN_EQ_ZONED_DATE_TIME(OP(COL("zdt"), LESS_THAN_EQUAL, COL("zdt")), BOOLEAN), //
            GREATER_THAN_TIME_DURATION(OP(COL("d"), GREATER_THAN, COL("d")), BOOLEAN), //
            LESS_THAN_TIME_DURATION(OP(COL("d"), LESS_THAN, COL("d")), BOOLEAN), //
            GREATER_THAN_EQ_TIME_DURATION(OP(COL("d"), GREATER_THAN_EQUAL, COL("d")), BOOLEAN), //
            LESS_THAN_EQ_TIME_DURATION(OP(COL("d"), LESS_THAN_EQUAL, COL("d")), BOOLEAN), //

            // Time ordering with optional cols
            GREATER_THAN_OPT_LOCAL_DATE(OP(COL("ld?"), GREATER_THAN, COL("ld?")), BOOLEAN), //
            LESS_THAN_OPT_LOCAL_DATE(OP(COL("ld?"), LESS_THAN, COL("ld?")), BOOLEAN), //
            GREATER_THAN_EQ_OPT_LOCAL_DATE(OP(COL("ld?"), GREATER_THAN_EQUAL, COL("ld?")), BOOLEAN), //
            LESS_THAN_EQ_OPT_LOCAL_DATE(OP(COL("ld?"), LESS_THAN_EQUAL, COL("ld?")), BOOLEAN), //
            GREATER_THAN_OPT_LOCAL_TIME(OP(COL("lt?"), GREATER_THAN, COL("lt?")), BOOLEAN), //
            LESS_THAN_OPT_LOCAL_TIME(OP(COL("lt?"), LESS_THAN, COL("lt?")), BOOLEAN), //
            GREATER_THAN_EQ_OPT_LOCAL_TIME(OP(COL("lt?"), GREATER_THAN_EQUAL, COL("lt?")), BOOLEAN), //
            LESS_THAN_EQ_OPT_LOCAL_TIME(OP(COL("lt?"), LESS_THAN_EQUAL, COL("lt?")), BOOLEAN), //
            GREATER_THAN_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), GREATER_THAN, COL("ldt?")), BOOLEAN), //
            LESS_THAN_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), LESS_THAN, COL("ldt?")), BOOLEAN), //
            GREATER_THAN_EQ_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), GREATER_THAN_EQUAL, COL("ldt?")), BOOLEAN), //
            LESS_THAN_EQ_OPT_LOCAL_DATE_TIME(OP(COL("ldt?"), LESS_THAN_EQUAL, COL("ldt?")), BOOLEAN), //
            GREATER_THAN_OPT_ZONED_DATE_TIME(OP(COL("zdt?"), GREATER_THAN, COL("zdt?")), BOOLEAN), //
            LESS_THAN_OPT_ZONED_DATE_TIME(OP(COL("zdt?"), LESS_THAN, COL("zdt?")), BOOLEAN), //
            GREATER_THAN_EQ_OPT_ZONED_DATE_TIME(OP(COL("zdt?"), GREATER_THAN_EQUAL, COL("zdt?")), BOOLEAN), //
            LESS_THAN_EQ_OPT_ZONED_DATE_TIME(OP(COL("zdt?"), LESS_THAN_EQUAL, COL("zdt?")), BOOLEAN), //
            GREATER_THAN_OPT_TIME_DURATION(OP(COL("d?"), GREATER_THAN, COL("d?")), BOOLEAN), //
            LESS_THAN_OPT_TIME_DURATION(OP(COL("d?"), LESS_THAN, COL("d?")), BOOLEAN), //
            GREATER_THAN_EQ_OPT_TIME_DURATION(OP(COL("d?"), GREATER_THAN_EQUAL, COL("d?")), BOOLEAN), //
            LESS_THAN_EQ_OPT_TIME_DURATION(OP(COL("d?"), LESS_THAN_EQUAL, COL("d?")), BOOLEAN), //

            // Couple of extra ordering tests with one optional and one non-optional col
            GREATER_THAN_LOCAL_DATE_TIME_MIXED_OPT(OP(COL("ldt?"), GREATER_THAN, COL("ldt")), BOOLEAN), //
            LESS_THAN_LOCAL_DATE_TIME_MIXED_OPT(OP(COL("ldt?"), LESS_THAN, COL("ldt")), BOOLEAN), //
            GREATER_THAN_EQ_LOCAL_DATE_TIME_MIXED_OPT(OP(COL("ldt?"), GREATER_THAN_EQUAL, COL("ldt")), BOOLEAN), //
            LESS_THAN_EQ_LOCAL_DATE_TIME_MIXED_OPT(OP(COL("ldt?"), LESS_THAN_EQUAL, COL("ldt")), BOOLEAN), //
            GREATER_THAN_ZONED_DATE_TIME_MIXED_OPT(OP(COL("zdt?"), GREATER_THAN, COL("zdt")), BOOLEAN), //
            LESS_THAN_ZONED_DATE_TIME_MIXED_OPT(OP(COL("zdt?"), LESS_THAN, COL("zdt")), BOOLEAN), //
            GREATER_THAN_EQ_ZONED_DATE_TIME_MIXED_OPT(OP(COL("zdt?"), GREATER_THAN_EQUAL, COL("zdt")), BOOLEAN), //
            LESS_THAN_EQ_ZONED_DATE_TIME_MIXED_OPT(OP(COL("zdt?"), LESS_THAN_EQUAL, COL("zdt")), BOOLEAN), //
            GREATER_THAN_TIME_DURATION_MIXED_OPT(OP(COL("d?"), GREATER_THAN, COL("d")), BOOLEAN), //
            LESS_THAN_TIME_DURATION_MIXED_OPT(OP(COL("d?"), LESS_THAN, COL("d")), BOOLEAN), //
            GREATER_THAN_EQ_TIME_DURATION_MIXED_OPT(OP(COL("d?"), GREATER_THAN_EQUAL, COL("d")), BOOLEAN), //
            LESS_THAN_EQ_TIME_DURATION_MIXED_OPT(OP(COL("d?"), LESS_THAN_EQUAL, COL("d")), BOOLEAN), //

            // Comparing times and durations to MISSING
            EQ_LOCAL_DATE_MISSING(OP(COL("ld"), EQUAL_TO, MIS()), BOOLEAN), //
            NEQ_LOCAL_DATE_MISSING(OP(COL("ld"), NOT_EQUAL_TO, MIS()), BOOLEAN), //
            EQ_LOCAL_TIME_MISSING(OP(COL("lt"), EQUAL_TO, MIS()), BOOLEAN), //
            NEQ_LOCAL_TIME_MISSING(OP(COL("lt"), NOT_EQUAL_TO, MIS()), BOOLEAN), //
            EQ_LOCAL_DATE_TIME_MISSING(OP(COL("ldt"), EQUAL_TO, MIS()), BOOLEAN), //
            NEQ_LOCAL_DATE_TIME_MISSING(OP(COL("ldt"), NOT_EQUAL_TO, MIS()), BOOLEAN), //
            EQ_ZONED_DATE_TIME_MISSING(OP(COL("zdt"), EQUAL_TO, MIS()), BOOLEAN), //
            NEQ_ZONED_DATE_TIME_MISSING(OP(COL("zdt"), NOT_EQUAL_TO, MIS()), BOOLEAN), //
            EQ_TIME_DURATION_MISSING(OP(COL("d"), EQUAL_TO, MIS()), BOOLEAN), //
            NEQ_TIME_DURATION_MISSING(OP(COL("d"), NOT_EQUAL_TO, MIS()), BOOLEAN), //
            EQ_DATE_DURATION_MISSING(OP(COL("p"), EQUAL_TO, MIS()), BOOLEAN), //
            NEQ_DATE_DURATION_MISSING(OP(COL("p"), NOT_EQUAL_TO, MIS()), BOOLEAN), //

            // Equality
            EQUALITY_BOTH_STRING(OP(STR("a"), EQUAL_TO, STR("b")), BOOLEAN), //
            EQUALITY_BOTH_INT(OP(INT(10), NOT_EQUAL_TO, INT(20)), BOOLEAN), //
            EQUALITY_INT_FLOAT(OP(INT(10), EQUAL_TO, FLOAT(10.1)), BOOLEAN), //
            // optional
            EQUALITY_FLOAT_AND_OPT_FLOAT(OP(COL("f?"), EQUAL_TO, FLOAT(10.1)), BOOLEAN), //
            EQUALITY_INT_AND_OPT_FLOAT(OP(INT(10), EQUAL_TO, COL("f?")), BOOLEAN), //
            EQUALITY_BOTH_OPT_INT(OP(COL("i?"), NOT_EQUAL_TO, COL("i?")), BOOLEAN), //
            EQUALITY_STRING_AND_OPT_STRING(OP(STR("foo"), EQUAL_TO, COL("s?")), BOOLEAN), //
            // missing
            EQUALITY_INT_MISSING(OP(INT(10), EQUAL_TO, MIS()), BOOLEAN), //
            EQUALITY_MISSING_OPT_STRING(OP(MIS(), EQUAL_TO, COL("s?")), BOOLEAN), //
            EQUALITY_MISSING_MISSING(OP(MIS(), EQUAL_TO, MIS()), BOOLEAN), //

            // === Logical Operations

            LOGICAL_AND_TWO_BOOLEANS(OP(BOOL(true), CONDITIONAL_AND, BOOL(false)), BOOLEAN), //
            LOGICAL_OR_OPT_BOOLEAN(OP(COL("b?"), CONDITIONAL_OR, BOOL(true)), OPT_BOOLEAN), //
            LOGICAL_AND_OPT_BOOLEAN(OP(BOOL(true), CONDITIONAL_AND, COL("b?")), OPT_BOOLEAN), //
            LOGICAL_NOT_BOOLEAN(OP(NOT, BOOL(false)), BOOLEAN), //
            LOGICAL_NOT_OPTIONAL_BOOLEAN(OP(NOT, COL("b?")), OPT_BOOLEAN), //

            // === String Concatenation
            STRING_CONCAT_TWO_STRINGS(OP(STR("Hello, "), PLUS, STR("World!")), STRING), //
            STRING_CONCAT_OPT_STRING(OP(STR("Hello, "), PLUS, COL("s?")), STRING), //
            STRING_CONCAT_TWO_OPT_STRING(OP(COL("s?"), PLUS, COL("s?")), STRING), //
            STRING_CONCAT_INTEGER_AND_STRING(OP(INT(10), PLUS, STR("foo")), STRING), //
            STRING_CONCAT_STRING_AND_OPT_FLOAT(OP(STR("bar"), PLUS, COL("f?")), STRING), //
            STRING_CONCAT_STRING_AND_BOOL(OP(STR("bar"), PLUS, BOOL(true)), STRING), //

            // === Function calls
            FUNCTION_CALL(FUN(TestFunctions.INT_TO_FLOAT_FN, INT(1)), FLOAT), //
            FUNCTION_CALL_NO_ARGS(FUN(TestFunctions.FN_WITH_NO_ARGS), MISSING), //

            // === Aggregation calls
            AGG_CALL_WITH_INT_ARG_I(AGG(TestAggregations.RETURN_42_WITH_COL_TYPE, STR("i")), INTEGER), //
            AGG_CALL_WITH_INT_ARG_OPT_F(AGG(TestAggregations.RETURN_42_WITH_COL_TYPE, STR("f?")), OPT_FLOAT), //
            AGG_CALL_WITH_NAMED_ARG(
                AGG(TestAggregations.EXPECT_NAMED_ARG, List.of(), Map.of("named_arg_id", FLOAT(2.0))), MISSING), //

            // === Complex Expressions
            NESTED_BINARY_OPS(OP(OP(INT(2), PLUS, COL("i?")), MULTIPLY, FLOAT(4.0)), OPT_FLOAT), //
        ;

        private final Ast m_expression;

        private final ValueType m_expectedType;

        private TypingTestCase(final Ast expression, final ValueType expectedType) {
            m_expression = expression;
            m_expectedType = expectedType;
        }
    }

    @ParameterizedTest
    @EnumSource(TypingErrorTestCase.class)
    void testError(final TypingErrorTestCase params) {
        var ast = params.m_expression;
        var typingError = assertThrows(ExpressionCompileException.class,
            () -> Typing.inferTypes(ast, TEST_COLUMN_TO_TYPE, TEST_FLOWVARIABLE_TO_TYPE),
            "should fail type inferrence");
        var errorMessage = typingError.getMessage();
        for (var expectedSubstring : params.m_expectedErrorSubstrings) {
            assertTrue(errorMessage.toLowerCase(Locale.ROOT).contains(expectedSubstring.toLowerCase(Locale.ROOT)),
                "error should contain '" + expectedSubstring + "', got '" + errorMessage + "'");
        }
    }

    private static enum TypingErrorTestCase {
            // === Arithmetic Operations
            ARITHMETICS_ON_BOOLEANS(OP(BOOL(true), PLUS, BOOL(false)), "+", BOOLEAN.name()), //
            ARITHMETICS_ON_STRING_AND_BOOLEAN(OP(STR("foo"), DIVIDE, COL("b?")), "/", STRING.name(),
                OPT_BOOLEAN.name()), //
            ARITHMETICS_ON_INT_AND_BOOLEAN(OP(INT(5), PLUS, BOOL(false)), "+", INTEGER.name(), BOOLEAN.name()), //
            ARITHMETICS_ON_INT_MISSING(OP(INT(5), PLUS, MIS()), "+", INTEGER.name(), MISSING.name()), //
            FLOOR_DIVISION_ON_FLOAT(OP(FLOAT(10.1), FLOOR_DIVIDE, FLOAT(2)), "//", FLOAT.name()), //
            FLOOR_DIVISION_ON_INT_AND_FLOAT(OP(INT(2), FLOOR_DIVIDE, FLOAT(2.0)), "//", FLOAT.name(), INTEGER.name()), //
            NEGATE_STRING(OP(MINUS, STR("foo")), "-", STRING.name()), //
            NEGATE_MISSING(OP(MINUS, MIS()), "-", MISSING.name()), //
            DURATION_PLUS_TIMELIKE(OP(COL("d"), PLUS, COL("lt")), "must be first"), //
            PERIOD_PLUS_DATELIKE(OP(COL("p"), PLUS, COL("ld")), "must be first"), //
            SUBTRACT_TWO_DIFFERENT_TEMPORAL_TYPES(OP(COL("ldt"), BinaryOperator.MINUS, COL("zdt")),
                LOCAL_DATE_TIME.name(), ZONED_DATE_TIME.name()), //
            PERIOD_MINUS_DATELIKE(OP(COL("p"), BinaryOperator.MINUS, COL("ld")), "must be first"), //
            DURATION_MINUS_TIMELIKE(OP(COL("d"), BinaryOperator.MINUS, COL("lt")), "must be first"), //
            MULTIPLY_DATE_DURATION_FLOAT(OP(COL("p"), MULTIPLY, FLOAT(2.0)), DATE_DURATION.name(), FLOAT.name()), //
            MULTIPLY_TIME_DURATION_FLOAT(OP(COL("d"), MULTIPLY, FLOAT(2.0)), TIME_DURATION.name(), FLOAT.name()), //
            DIVIDE_DATE_DURATION_INTEGER(OP(COL("p"), DIVIDE, INT(2)), DATE_DURATION.name(), INTEGER.name()), //
            DIVIDE_TIME_DURATION_INTEGER(OP(COL("d"), DIVIDE, INT(2)), TIME_DURATION.name(), INTEGER.name()), //
            DURATION_MINUS_PERIOD(OP(COL("d"), BinaryOperator.MINUS, COL("p")), "different type"), //
            PERIOD_MINUS_DURATION(OP(COL("p"), BinaryOperator.MINUS, COL("d")), "different type"), //
            DURATION_PLUS_PERIOD(OP(COL("d"), PLUS, COL("p")), "different type"), //
            PERIOD_PLUS_DURATION(OP(COL("p"), PLUS, COL("d")), "different type"), //
            LOCAL_DATE_PLUS_DURATION(OP(COL("ld"), PLUS, COL("d")), LOCAL_DATE.name(), TIME_DURATION.name()), //
            LOCAL_DATE_MINUS_DURATION(OP(COL("ld"), BinaryOperator.MINUS, COL("d")), LOCAL_DATE.name(),
                TIME_DURATION.name()), //
            LOCAL_TIME_PLUS_PERIOD(OP(COL("lt"), PLUS, COL("p")), LOCAL_TIME.name(), DATE_DURATION.name()), //
            LOCAL_TIME_MINUS_PERIOD(OP(COL("lt"), BinaryOperator.MINUS, COL("p")), LOCAL_TIME.name(),
                DATE_DURATION.name()), //

            // === Comparison Operations
            ORDERING_ON_INT_AND_BOOLEAN(OP(INT(100), GREATER_THAN, BOOL(false)), ">", INTEGER.name(), BOOLEAN.name()), //
            ORDERING_ON_STRING(OP(STR("a"), LESS_THAN, STR("b")), "<", STRING.name()), //
            ORDERING_ON_INT_AND_MISSING(OP(INT(20), LESS_THAN, MIS()), "<", INTEGER.name(), MISSING.name()), //
            ORDERING_ON_DATE_DURATIONS(OP(COL("p"), GREATER_THAN, COL("p")), DATE_DURATION.name()), //
            EQUALITY_ON_STRING_AND_BOOLEAN(OP(STR("a"), NOT_EQUAL_TO, BOOL(false)), "!=", STRING.name(),
                BOOLEAN.name()), //
            EQUALITY_ON_INT_AND_STRING(OP(INT(20), EQUAL_TO, STR("bar")), "==", INTEGER.name(), STRING.name()), //
            EQUALITY_ON_ZONED_AND_ZONED(OP(COL("zdt"), EQUAL_TO, COL("zdt")), ZONED_DATE_TIME.name()), //

            // === Logical Operations
            LOGICAL_ON_INTEGER(OP(INT(10), CONDITIONAL_AND, INT(20)), "and", INTEGER.name()), //
            LOGICAL_ON_BOOL_AND_FLOAT(OP(BOOL(false), CONDITIONAL_OR, FLOAT(10.1)), "or", FLOAT.name(), BOOLEAN.name()), //
            LOGICAL_ON_MISSING_AND_BOOL(OP(MIS(), CONDITIONAL_AND, BOOL(false)), "and", MISSING.name(), BOOLEAN.name()), //
            LOGICAL_NOT_ON_STRING(OP(NOT, STR("foo")), "not", STRING.name()), //
            LOGICAL_NOT_ON_MISSING(OP(NOT, MIS()), "not", MISSING.name()), //

            // === MISSING Fallback Operator
            FALLBACK_BOTH_MISSING(OP(MIS(), MISSING_FALLBACK, MIS()), "one", "must", "not", MISSING.name()),
            FALLBACK_NOT_SAME_TYPE(OP(INT(0), MISSING_FALLBACK, BOOL(false)), "must", "compatible"),
            FALLBACK_NOT_SAME_OPTIONAL_TYPES(OP(COL("f?"), MISSING_FALLBACK, COL("s?")), "must", "compatible"),
            FALLBACK_NOT_SAME_MIXED_TYPES(OP(INT(0), MISSING_FALLBACK, COL("s?")), "must", "compatible"),

            // === String Concatenation
            STRING_CONCAT_STRING_AND_MISSING(OP(STR("foo"), PLUS, MIS()), "+", STRING.name(), MISSING.name()), //
            STRING_CONCAT_MISSING_AND_STRING(OP(MIS(), PLUS, STR("foo")), "+", STRING.name(), MISSING.name()), //

            // === Function calls
            FUNCTION_CALL_WRONG_ARG_TYPES(FUN(TestFunctions.INT_TO_FLOAT_FN, FLOAT(1.0)), "INT_TO_FLOAT_FN",
                FLOAT.name()), //

            // === Aggregation calls
            AGG_CALL_WRONG_ARG_TYPES(AGG(TestAggregations.RETURN_42_WITH_COL_TYPE, STR("s?")),
                "RETURN_42_WITH_COL_TYPE", "not", "numeric"), //
        ;

        private final Ast m_expression;

        private final String[] m_expectedErrorSubstrings;

        private TypingErrorTestCase(final Ast expression, final String... expectedErrorSubstrings) {
            m_expression = expression;
            m_expectedErrorSubstrings = expectedErrorSubstrings;
        }
    }

    @Test
    void testMissingColumn() {
        var colName = "not_a_column";
        var ast = OP(INT(10), PLUS, COL(colName));
        var exception = assertThrows(ExpressionCompileException.class,
            () -> Typing.inferTypes(ast, TEST_COLUMN_TO_TYPE, TEST_FLOWVARIABLE_TO_TYPE),
            "should fail type inferrence");
        var errors = exception.getErrors();

        System.out.println(errors);

        assertEquals(1, errors.size(), "should be one error");
        assertEquals(ExpressionCompileError.CompileErrorType.MISSING_COLUMN, errors.get(0).type(),
            "should be missing column error type");
        var errorMessage = errors.get(0).message();
        assertTrue(errorMessage.toLowerCase(Locale.ROOT).contains(colName.toLowerCase(Locale.ROOT)),
            "error message should contain column name '" + colName + "', was '" + errorMessage + "'");
    }

    private static final Map<String, ValueType> TEST_TYPES = Map.ofEntries( //
        entry("b", BOOLEAN), entry("b?", OPT_BOOLEAN), //
        entry("i", INTEGER), entry("i?", OPT_INTEGER), //
        entry("f", FLOAT), entry("f?", OPT_FLOAT), //
        entry("s", STRING), entry("s?", OPT_STRING), //
        entry("d", TIME_DURATION), entry("d?", OPT_TIME_DURATION), //
        entry("p", DATE_DURATION), entry("p?", OPT_DATE_DURATION), //
        entry("ld", LOCAL_DATE), entry("ld?", OPT_LOCAL_DATE), //
        entry("lt", LOCAL_TIME), entry("lt?", OPT_LOCAL_TIME), //
        entry("ldt", LOCAL_DATE_TIME), entry("ldt?", OPT_LOCAL_DATE_TIME), //
        entry("zdt", ZONED_DATE_TIME), entry("zdt?", OPT_ZONED_DATE_TIME) //
    );

    private static final Function<String, ReturnResult<ValueType>> TEST_COLUMN_TO_TYPE =
        c -> ReturnResult.fromNullable(TEST_TYPES.get(c), "col " + c + " missing");

    private static final Function<String, ReturnResult<ValueType>> TEST_FLOWVARIABLE_TO_TYPE =
        c -> ReturnResult.fromNullable(TEST_TYPES.get(c), "var " + c + " missing");

    private static void assertChildrenHaveTypes(final Ast astWithTypes) {
        for (var child : astWithTypes.children()) {
            assertNotNull(Expressions.getInferredType(astWithTypes), "should have inferred type");
            assertChildrenHaveTypes(child);
        }
    }

    private static enum TestFunctions implements ExpressionFunction {
            FN_WITH_NO_ARGS(Map.of(), MISSING), //
            INT_TO_FLOAT_FN(Map.of("arg1", INTEGER), FLOAT), //
            FN_WITH_NO_XXXX(Map.of(), MISSING); // here to test error message - note similarity to FN_WITH_NO_ARGS

        private final ValueType m_output;

        private final List<Arg> m_signature;

        private TestFunctions(final Map<String, ValueType> args, final ValueType output) {
            m_output = output;
            m_signature = args.entrySet().stream() //
                .map(e -> arg(e.getKey(), "", hasType(e.getValue()))) //
                .toList();
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(m_signature, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            return SignatureUtils.checkTypes(m_signature, argTypes).map(valid -> m_output);
        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            return fail("Should not be called during type inference");
        }

        @Override
        public OperatorDescription description() {
            return fail("Should not be called during type inference");
        }
    }
}
