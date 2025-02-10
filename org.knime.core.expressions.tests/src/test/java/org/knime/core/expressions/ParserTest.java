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
 *   Feb 13, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.expressions.Ast.BinaryOperator.CONDITIONAL_AND;
import static org.knime.core.expressions.Ast.BinaryOperator.CONDITIONAL_OR;
import static org.knime.core.expressions.Ast.BinaryOperator.DIVIDE;
import static org.knime.core.expressions.Ast.BinaryOperator.EQUAL_TO;
import static org.knime.core.expressions.Ast.BinaryOperator.EXPONENTIAL;
import static org.knime.core.expressions.Ast.BinaryOperator.FLOOR_DIVIDE;
import static org.knime.core.expressions.Ast.BinaryOperator.GREATER_THAN;
import static org.knime.core.expressions.Ast.BinaryOperator.GREATER_THAN_EQUAL;
import static org.knime.core.expressions.Ast.BinaryOperator.LESS_THAN;
import static org.knime.core.expressions.Ast.BinaryOperator.LESS_THAN_EQUAL;
import static org.knime.core.expressions.Ast.BinaryOperator.MINUS;
import static org.knime.core.expressions.Ast.BinaryOperator.MULTIPLY;
import static org.knime.core.expressions.Ast.BinaryOperator.NOT_EQUAL_TO;
import static org.knime.core.expressions.Ast.BinaryOperator.PLUS;
import static org.knime.core.expressions.Ast.BinaryOperator.REMAINDER;
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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.functions.MathFunctions;
import org.knime.core.expressions.functions.StringFunctions;

/**
 * Tests for parsing expressions in the KNIME Expression language.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class ParserTest {

    @ParameterizedTest
    @EnumSource(ValidExpr.class)
    void testValidExpressions(final ValidExpr exprTest) throws ExpressionCompileException {
        var ast = Expressions.parse(exprTest.m_input);
        clearDataFromAst(ast);
        assertEquals(exprTest.m_expectedAst, ast, "Wrong result for expr '" + exprTest.m_input + "'");
    }

    private static void clearDataFromAst(final Ast node) {
        for (var c : node.children()) {
            clearDataFromAst(c);
        }
        node.data().clear();
    }

    enum ValidExpr {

            // Mathematical constants
            MATH_CONSTANT_E("E", FLOAT(Math.E)), //
            MATH_CONSTANT_PI("PI", FLOAT(Math.PI)), //
            MATH_CONSTANT_NaN("NaN", FLOAT(Double.NaN)), //
            MATH_CONSTANT_INFINITY("INFINITY", FLOAT(Double.POSITIVE_INFINITY)), //
            MATH_CONSTANT_MIN_INT("MIN_INTEGER", INT(Long.MIN_VALUE)), //
            MATH_CONSTANT_MAX_INT("MAX_INTEGER", INT(Long.MAX_VALUE)), //
            MATH_CONSTANT_MIN_FLOAT("MIN_FLOAT", FLOAT(-Double.MAX_VALUE)), //
            MATH_CONSTANT_MAX_FLOAT("MAX_FLOAT", FLOAT(Double.MAX_VALUE)), //
            MATH_CONSTANT_TINY_FLOAT("TINY_FLOAT", FLOAT(Double.MIN_NORMAL)), //

            // Comments
            COMMENT_AT_START("# some comment\n1\n#some other comment", INT(1)),
            COMMENT_AT_END("1 # some comment", INT(1)),

            // Column access
            COL_SHORTHAND_1("$colname", COL("colname")), //
            COL_SHORTHAND_2("$col_name12_", COL("col_name12_")), //
            COL_LONG_1("$[\"col\"]", COL("col")), //
            COL_LONG_2("$[\"my 'very\\\" special column\"]", COL("my 'very\" special column")), //
            COL_ROW_INDEX("$[ROW_INDEX]", ROW_INDEX()), //
            COL_ROW_ID("$[ROW_ID]", ROW_ID()), //

            // Column access with offsets
            COL_ACCESS_OFFSET("$[\"col\", 10]", COL("col", 10)), //
            COL_ACCESS_OFFSET_NEG("$[\"col\", -10]", COL("col", -10)), //
            COL_ACCESS_OFFSET_UNDERSCORE("$[\"col\", 10_0]", COL("col", 100)), //

            // Flow Variable Access
            FLOW_SHORTHAND_1("$$varname", FLOW("varname")), //
            FLOW_SHORTHAND_2("$$flow_name12_", FLOW("flow_name12_")), //
            FLOW_LONG_1("$$[\"flow\"]", FLOW("flow")), //
            FLOW_LONG_2("$$[\"my 'very\\\" special variable\"]", FLOW("my 'very\" special variable")), //

            // BOOLEAN Literal
            BOOL_TRUE("TRUE", BOOL(true)), //
            BOOL_FALSE("FALSE", BOOL(false)), //

            // INTEGER literal
            INT_ZERO("0", INT(0)), //
            INT_10("10", INT(10)), //
            INT_WITH_UNDERSCORE("10_200", INT(10_200)), //

            // FLOAT literal
            FLOAT_SIMPLE("0.1", FLOAT(0.1)), //
            FLOAT_STARTING_POINT(".1", FLOAT(0.1)), //
            FLOAT_UNDERSCORE("10_200.1_2_3", FLOAT(10200.123)), //
            FLOAT_EXPONENT("3e-4", FLOAT(3e-4)), //

            // STRING literal
            STRING_DQ("\"my value\"", STR("my value")), //
            STRING_DQ_MULTI_LINE("\"my \n value\"", STR("my \n value")), //
            STRING_DQ_INC_SINGLE_QUOTE("\"my ' value\"", STR("my ' value")), //
            STRING_DQ_SPECIAL_CHAR("\"Hello 世界\"", STR("Hello 世界")), //
            STRING_SQ("'my value'", STR("my value")), //
            STRING_SQ_INC_DOUBLE_QUOTE("'my \" value'", STR("my \" value")), //
            STRING_SQ_MULTI_LINE("'my \n value'", STR("my \n value")), //
            STRING_SQ_SPECIAL_CHAR("'Hello 世界'", STR("Hello 世界")), //
            // escape sequences
            STRING_DQ_ESC_NL("\"my \\\nvalue\"", STR("my value")), //
            STRING_SQ_ESC_NL("'my \\\nvalue'", STR("my value")), //
            STRING_DQ_ESC_BACKSLASH("\"my \\\\ value\"", STR("my \\ value")), //
            STRING_SQ_ESC_BACKSLASH("'my \\\\ value'", STR("my \\ value")), //
            STRING_DQ_ESC_SINGLE_QUOTE("\"my \\' value\"", STR("my ' value")), //
            STRING_SQ_ESC_SINGLE_QUOTE("'my \\' value'", STR("my ' value")), //
            STRING_DQ_ESC_DOUBLE_QUOTE("\"my \\\" value\"", STR("my \" value")), //
            STRING_SQ_ESC_DOUBLE_QUOTE("'my \\\" value'", STR("my \" value")), //
            STRING_DQ_ESC_BS("\"my \\b value\"", STR("my \b value")), //
            STRING_SQ_ESC_BS("'my \\b value'", STR("my \b value")), //
            STRING_DQ_ESC_LF("\"my \\n value\"", STR("my \n value")), //
            STRING_SQ_ESC_LF("'my \\n value'", STR("my \n value")), //
            STRING_DQ_ESC_CR("\"my \\r value\"", STR("my \r value")), //
            STRING_SQ_ESC_CR("'my \\r value'", STR("my \r value")), //
            STRING_DQ_ESC_TAB("\"my \\t value\"", STR("my \t value")), //
            STRING_SQ_ESC_TAB("'my \\t value'", STR("my \t value")), //
            STRING_DQ_MIXED_ESC("\"foo \\\\n bar\"", STR("foo \\n bar")), //
            STRING_SQ_MIXED_ESC("\"foo \\\\n bar\"", STR("foo \\n bar")), //
            // unicode by code
            STRING_DQ_UNICODE_16("\"Hello W\\u00F6rld\"", STR("Hello Wörld")), //
            STRING_SQ_UNICODE_16("'Hello W\\u00F6rld'", STR("Hello Wörld")), //
            STRING_DQ_NO_UNICODE_IF_ESCAPED("\"\\\\u2122=\\u2122\"", STR("\\u2122=\u2122")), //
            STRING_SQ_NO_UNICODE_IF_ESCAPED("'\\\\u2122=\\u2122'", STR("\\u2122=\u2122")), //
            STRING_DQ_UNICODE_CANNOT_PRODUCE_OTHER_ESC_SEQ("\"\\u005Cn\"", STR("\\n")), //
            STRING_SQ_UNICODE_CANNOT_PRODUCE_OTHER_ESC_SEQ("'\\u005Cn'", STR("\\n")), //

            // MISSING literal
            MISSING("MISSING", MIS()),

            // Unary Operators
            OP_UNARY_MINUS_1("-10", INT(-10)), //
            OP_UNARY_MINUS_2("- 10", INT(-10)), //
            OP_UNARY_NOT("not 10", OP(UnaryOperator.NOT, INT(10))), //

            // Binary Operator
            // Arithmetics
            OP_PLUS("10+20", OP(INT(10), PLUS, INT(20))), //
            OP_MINUS("10-20", OP(INT(10), MINUS, INT(20))), //
            OP_MULTIPLY("10 *20", OP(INT(10), MULTIPLY, INT(20))), //
            OP_DIVIDE("10 / 20", OP(INT(10), DIVIDE, INT(20))), //
            OP_FLOOR_DIVIDE("10 // 20", OP(INT(10), FLOOR_DIVIDE, INT(20))), //
            OP_EXPONENTIAL("10** 20", OP(INT(10), EXPONENTIAL, INT(20))), //
            OP_REMAINDER("10 % 20", OP(INT(10), REMAINDER, INT(20))), //
            // Comparison
            OP_LESS_THAN("10 < 20", OP(INT(10), LESS_THAN, INT(20))), //
            OP_LESS_THAN_EQUAL("10 <= 20", OP(INT(10), LESS_THAN_EQUAL, INT(20))), //
            OP_GREATER_THAN("10 > 20", OP(INT(10), GREATER_THAN, INT(20))), //
            OP_GREATER_THAN_EQUAL("10 >= 20", OP(INT(10), GREATER_THAN_EQUAL, INT(20))), //
            OP_EQUAL_TO_1("10 = 20", OP(INT(10), EQUAL_TO, INT(20))), //
            OP_EQUAL_TO_X("'foo' = 20", OP(STR("foo"), EQUAL_TO, INT(20))), //
            OP_EQUAL_TO_2("10 == 20", OP(INT(10), EQUAL_TO, INT(20))), //
            OP_NOT_EQUAL_TO_1("10 != 20", OP(INT(10), NOT_EQUAL_TO, INT(20))), //
            OP_NOT_EQUAL_TO_2("10 <> 20", OP(INT(10), NOT_EQUAL_TO, INT(20))), //
            // Logical
            OP_CONDITIONAL_AND("10 and 20", OP(INT(10), CONDITIONAL_AND, INT(20))), //
            OP_CONDITIONAL_OR("10 or 20", OP(INT(10), CONDITIONAL_OR, INT(20))), //

            // Operator precedence
            OP_PREC_PLUS_MULTIPLY("1+2*3", OP(INT(1), PLUS, OP(INT(2), MULTIPLY, INT(3)))), //
            OP_PREC_PLUS_MULTIPLY_EXPONENTIAL("0+1*2**3",
                OP(INT(0), PLUS, OP(INT(1), MULTIPLY, OP(INT(2), EXPONENTIAL, INT(3))))), //
            OP_PREC_MULTIPLY_NEGATE("1 * - 2", OP(INT(1), MULTIPLY, INT(-2))), //
            OP_PREC_PLUS_NEGATE("1 + - 2", OP(INT(1), PLUS, INT(-2))), //
            OP_PREC_PLUS_LESS_THAN("1 < 2 + 3", OP(INT(1), LESS_THAN, OP(INT(2), PLUS, INT(3)))), //
            OP_PREC_MULTIPLY_GREATER_THAN_EQUAL("1 >= 2 * 3",
                OP(INT(1), GREATER_THAN_EQUAL, OP(INT(2), MULTIPLY, INT(3)))), //
            OP_PREC_COMP_NOT("not 1 == 2", OP(UnaryOperator.NOT, OP(INT(1), EQUAL_TO, INT(2)))), //
            OP_PREC_NOT_AND("not 1 and 2", OP(OP(UnaryOperator.NOT, INT(1)), CONDITIONAL_AND, INT(2))), //
            OP_PREC_AND_NOT("1 and not 2", OP(INT(1), CONDITIONAL_AND, OP(UnaryOperator.NOT, INT(2)))), //
            OP_PREC_OR_NOT("1 or not 2", OP(INT(1), CONDITIONAL_OR, OP(UnaryOperator.NOT, INT(2)))), //
            OP_PREC_OR_AND("1 or 2 and 3", OP(INT(1), CONDITIONAL_OR, OP(INT(2), CONDITIONAL_AND, INT(3)))), //
            OP_PREC_AND_OR("1 and 2 or 3", OP(OP(INT(1), CONDITIONAL_AND, INT(2)), CONDITIONAL_OR, INT(3))), //

            // Operator associativity
            OP_ASSO_PLUS("10+20+30", OP(OP(INT(10), PLUS, INT(20)), PLUS, INT(30))), //
            OP_ASSO_MULTIPLY_DIVIDE("10/20*30", OP(OP(INT(10), DIVIDE, INT(20)), MULTIPLY, INT(30))), //
            OP_ASSO_EXPONENTIAL("10**20**30", OP(INT(10), EXPONENTIAL, OP(INT(20), EXPONENTIAL, INT(30)))), //
            OP_ASSO_COMPARISON("10 <= 3 > 4 < 2",
                OP(OP(OP(INT(10), LESS_THAN_EQUAL, INT(3)), GREATER_THAN, INT(4)), LESS_THAN, INT(2))), //

            // Parentheses
            PAREN_PLUS_MULTIPLY("(1+2)*3", OP(OP(INT(1), PLUS, INT(2)), MULTIPLY, INT(3))), //
            PAREN_EXPRESSION_LOGIC("(1 or 2) and 3 - (4 < 5)", OP( //
                OP(INT(1), CONDITIONAL_OR, INT(2)), //
                CONDITIONAL_AND, //
                OP(INT(3), MINUS, OP(INT(4), LESS_THAN, INT(5))) //
            )),

            // Function calls
            FUNC_SINGLE_ARG("sin(1)", FUN(MathFunctions.SIN, INT(1))), //
            FUNC_TRAILING_COMMA("max(1,2,)", FUN(MathFunctions.MAX, INT(1), INT(2))), //
            FUNC_COLUMN_ACCESS_PARM("sin($[\"col\"])", FUN(MathFunctions.SIN, COL("col"))), //
            FUNC_NESTED_EXPR("sin(1 + 2)", FUN(MathFunctions.SIN, OP(INT(1), PLUS, INT(2)))), //
            FUNC_NESTED_FUNC("sin(cos(1))", FUN(MathFunctions.SIN, FUN(MathFunctions.COS, INT(1)))), //

            // Aggregation functions
            COL_AGG("COLUMN_AVERAGE(\"column name\")", AGG(BuiltInAggregations.AVERAGE, STR("column name"))), //
            COL_AGG_NAMED_ARG("COLUMN_AVERAGE(\"column name\", ignore_nan=TRUE)",
                AGG(BuiltInAggregations.AVERAGE, List.of(STR("column name")), Map.of("ignore_nan", BOOL(true)))), //
            COL_AGG_NAMED_ARG_WITH_WHITESPACE("COLUMN_AVERAGE(\"column name\", ignore_nan =TRUE)",
                AGG(BuiltInAggregations.AVERAGE, List.of(STR("column name")), Map.of("ignore_nan", BOOL(true)))), //
            COL_AGG_ONLY_NAMED_ARGS("COLUMN_AVERAGE(column=\"column name\", ignore_nan=TRUE)",
                AGG(BuiltInAggregations.AVERAGE, List.of(),
                    Map.of("column", STR("column name"),"ignore_nan", BOOL(true)))), //
            COL_AGG_ONLY_POS_ARGS("COLUMN_AVERAGE(\"column name\", TRUE)",
                AGG(BuiltInAggregations.AVERAGE, STR("column name"), BOOL(true))), //

            // Special stuff

            // Ignoring white spaces of each kind
            IGNORE_WHITESPACES("10\n   \n+\t\n   20\n\t   \n\n", OP(INT(10), PLUS, INT(20))), //

            // Trying to confuse the parser by using known stuff in a context where they do not have their meaning
            MIXING_MISSING_IN_STR("\"MISSING\"", STR("MISSING")), //
            MIXING_NUMBERS_IN("\"1.2\"", STR("1.2")), //
            MIXING_EXPR_IN_COL_ACCESS("$[\"1 + 2\"]", COL("1 + 2")), //

            // Combined examples
            COMPLEX_1("not ($email = MISSING) or ($phone_number != MISSING and $opt_in_status == TRUE)", //
                OP( //
                    OP(UnaryOperator.NOT, OP(COL("email"), EQUAL_TO, MIS())), //
                    CONDITIONAL_OR, //
                    OP( //
                        OP(COL("phone_number"), NOT_EQUAL_TO, MIS()), //
                        CONDITIONAL_AND, //
                        OP(COL("opt_in_status"), EQUAL_TO, BOOL(true)) //
                    ) //
                )), //
            COMPLEX_2("$$discount_rate * $price > $price - $$min_savings", //
                OP( //
                    OP(FLOW("discount_rate"), MULTIPLY, COL("price")), //
                    GREATER_THAN, //
                    OP(COL("price"), MINUS, FLOW("min_savings")) //
                ) //
            ), //
            COMPLEX_3("round(average($age,$age), 0) >= 30 and lower_case($department) = \"marketing\"", //
                OP( //
                    OP( //
                        FUN(MathFunctions.ROUNDHALFEVEN, FUN(MathFunctions.AVERAGE, COL("age"), COL("age")), INT(0)), //
                        GREATER_THAN_EQUAL, //
                        INT(30) //
                    ), //
                    CONDITIONAL_AND, //
                    OP(FUN(StringFunctions.LOWER_CASE, COL("department")), EQUAL_TO, STR("marketing")) //
                ) //
            ), //
            COMPLEX_4( //
                "$status = \"Active\" and $days_since_last_order <= 30 \n or \n " //
                    + "$status = \"Inactive\" and $days_since_last_order > $$['many days']", //
                OP( //
                    OP( //
                        OP(COL("status"), EQUAL_TO, STR("Active")), //
                        CONDITIONAL_AND, //
                        OP(COL("days_since_last_order"), LESS_THAN_EQUAL, INT(30)) //
                    ), //
                    CONDITIONAL_OR, //
                    OP( //
                        OP(COL("status"), EQUAL_TO, STR("Inactive")), //
                        CONDITIONAL_AND, //
                        OP(COL("days_since_last_order"), GREATER_THAN, FLOW("many days")) //
                    ) //
                ) //
            ), //
        ;

        private final String m_input;

        private final Ast m_expectedAst;

        ValidExpr(final String input, final Ast expectedAst) {
            m_input = input;
            m_expectedAst = expectedAst;

        }
    }

    @ParameterizedTest
    @EnumSource(InvalidExpr.class)
    void testSyntaxErrors(final InvalidExpr exprTest) {
        ExpressionCompileException message = assertThrows(ExpressionCompileException.class,
            () -> Expressions.parse(exprTest.m_input), "Expected syntax error for expr '" + exprTest.m_input + "'");
        if (exprTest.m_expectedErrorMessageFragments.length > 0) {
            for (String fragment : exprTest.m_expectedErrorMessageFragments) {
                assertTrue(message.getMessage().toLowerCase().contains(fragment.toLowerCase()),
                    "Wrong error message for expr '" + exprTest.m_input + "' - Error message (" + message.getMessage()
                        + ") expected to contain '" + fragment + "'");
            }
        }
    }

    enum InvalidExpr {
            // no expression present
            EMPTY("", "The expression is empty"), //
            EMPTY_WITH_WHITESPACE(" ", "The expression is empty"), //
            EMPTY_WITH_COMMENTS("# comment", "The expression is empty"), //

            // Column access
            COL_ACCESS_INVALID_SHORTHAND("$foo@bar", "token", "recognition", "error"), //

            // INTEGER literal
            INT_LEADING_ZERO("01", "Leading", "integer"), //

            // STRING literal
            INVALID_STRING_ESC_WHITESPACE("'Hello \\ World'", "\\"), //
            INVALID_STRING_ESC_CHAR_G("'Hello \\g World'", "invalid", "escape sequence", "\\g"), //
            INVALID_STRING_ESC_NON_ESCAPED_DQ("\"Hello \\\\\" World\""), //
            INVALID_STRING_ESC_NON_ESCAPED_SQ("'Hello \\\\\' World'"), //
            INVALID_STRING_ESC_NON_ESCAPED_BS("\"\\\"", "escape sequence"), //
            INVALID_STRING_ESC_NON_COMPLETE_UNICODE("\" \\u \"", "unicode", "escape sequence"), //
            INVALID_STRING_ESC_UNICODE_INVALID_CHARS("\" \\u123z \"", "unicode", "escape sequence", "z"), //

            // mismatched input
            UNMATCHED_OPENING_PAREN("(1 + 2"), //
            UNMATCHED_CLOSING_PAREN("(1 + 2))"), //
            UNMATCHED_OPENING_BRACKET("$[\"foo\""), //
            UNMATCHED_CLOSING_BRACKET("$foo]"), //
            UNMATCHED_OPENING_SQ_STRING("'Hello"), //
            UNMATCHED_CLOSING_SQ_STRING("Hello'"), //
            UNMATCHED_OPENING_DQ_STRING("\"Hello"), //
            UNMATCHED_CLOSING_DQ_STRING("Hello\""), //

            // Invalid function calls
            FUNC_STARTING_UNDERSCORE("_func(1,2,3)", "token recognition error"), //
            FUNC_WITH_INVALID_ID("func(1,2,3)", "no", "name", "func"), //
            FUNC_WITH_POSITIONAL_AFTER_NAMED_ARGS(MathFunctions.AVERAGE.name() + "(x=100, 10)", "named", "positional"), //)

            // Invalid aggregation calls
            AGG_WITH_INVALID_ID("AB_00(1,2,3)", "AB_00"),
            AGG_WITH_POSITIONAL_AFTER_NAMED_ARGS(BuiltInAggregations.SUM.name() + "(a=100, 10)", "named", "positional"), //
            AGG_WITH_EXPR_ARG(BuiltInAggregations.SUM.name() + "(1 + 2)", "only", "literal"), //
            AGG_WITH_ROW_ID(BuiltInAggregations.SUM.name() + "($[ROW_ID])", "ROW_ID", "cannot"), //
            AGG_WITH_ROW_INDEX(BuiltInAggregations.SUM.name() + "($[ROW_INDEX])", "ROW_INDEX", "cannot"), //
            AGG_WITH_ROW_NUMBER(BuiltInAggregations.SUM.name() + "($[ROW_NUMBER])", "ROW_NUMBER", "cannot"), //

            // Trying to break it
            NOT_OP_WITHOUT_SPACE("not10"), // NB: can maybe parsed to a constant at some point
            AND_OP_WITHOUT_SPACE("10 and20"), //
        ;

        private final String m_input;

        private final String[] m_expectedErrorMessageFragments;

        private InvalidExpr(final String input, final String... expectedErrorMessageFragments) {
            m_input = input;
            m_expectedErrorMessageFragments = expectedErrorMessageFragments;
        }
    }

    @Test
    void testNewExpression() throws ExpressionCompileException {
        // A new expression with all the AST node types
        var expr = "10 + if(FALSE,-atan2(-20,2.5), parse_float('4' + $var + $$context))";
        //      0    ^    1    ^    2    ^    3    ^    4    ^    5    ^

        var ast = Parser.parse(expr);

        // if
        var functionCall = ast.children().get(1);
        assertTextLocation(5, 67, functionCall);

        // FALSE
        assertTextLocation(8, 13, functionCall.children().get(0));

        // -atan2(-20, 2.5)
        var unaryOp = functionCall.children().get(1);
        assertTextLocation(14, 29, unaryOp);
        var nestedFunctionCall = unaryOp.children().get(0);
        // atan2(-20, 2.5)
        assertTextLocation(15, 29, nestedFunctionCall);

        // -20
        var innerUnaryOp = nestedFunctionCall.children().get(0);
        assertTextLocation(21, 24, innerUnaryOp);

        // 2.5
        assertTextLocation(25, 28, nestedFunctionCall.children().get(1));

        // parse_float('4' + $var + $$context)
        var toFloatCall = functionCall.children().get(2);
        assertTextLocation(31, 66, toFloatCall);

        // '4' + $var
        var nestedBinOp = toFloatCall.children().get(0);
        assertTextLocation(43, 65, nestedBinOp);

        // '4' + $var
        assertTextLocation(43, 53, nestedBinOp.children().get(0));
        //  $$context
        assertTextLocation(56, 65, nestedBinOp.children().get(1));
    }

    @Test
    void testTextLocationWithNewlines() throws ExpressionCompileException {
        var expr = "\n10\n+20\n\n - MISSING";
        //          0      ^      1    ^    2
        var ast = Parser.parse(expr);

        assertTextLocation(1, 19, ast);

        // 10 + 20
        var plusOp = ast.children().get(0);
        assertTextLocation(1, 7, plusOp);
        // 10
        assertTextLocation(1, 3, plusOp.children().get(0));
        // 20
        assertTextLocation(5, 7, plusOp.children().get(1));

        // MISSING
        assertTextLocation(12, 19, ast.children().get(1));
    }

    private static void assertTextLocation(final int expectedStart, final int expectedStop, final Ast node) {
        assertEquals(new TextRange(expectedStart, expectedStop), Parser.getTextLocation(node),
            "should have correct location");
    }
}
