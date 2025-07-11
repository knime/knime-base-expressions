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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
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
import static org.knime.core.expressions.Ast.BinaryOperator.MISSING_FALLBACK;
import static org.knime.core.expressions.Ast.BinaryOperator.MULTIPLY;
import static org.knime.core.expressions.Ast.BinaryOperator.NOT_EQUAL_TO;
import static org.knime.core.expressions.Ast.BinaryOperator.PLUS;
import static org.knime.core.expressions.Ast.BinaryOperator.REMAINDER;
import static org.knime.core.expressions.Ast.UnaryOperator.NOT;
import static org.knime.core.expressions.AstTestUtils.BOOL;
import static org.knime.core.expressions.AstTestUtils.COL;
import static org.knime.core.expressions.AstTestUtils.FLOAT;
import static org.knime.core.expressions.AstTestUtils.FLOW;
import static org.knime.core.expressions.AstTestUtils.FUN;
import static org.knime.core.expressions.AstTestUtils.F_DATE_DURATION;
import static org.knime.core.expressions.AstTestUtils.F_LOCAL_DATE;
import static org.knime.core.expressions.AstTestUtils.F_LOCAL_DATE_TIME;
import static org.knime.core.expressions.AstTestUtils.F_LOCAL_TIME;
import static org.knime.core.expressions.AstTestUtils.F_TIME_DURATION;
import static org.knime.core.expressions.AstTestUtils.F_ZONED_DATE_TIME;
import static org.knime.core.expressions.AstTestUtils.INT;
import static org.knime.core.expressions.AstTestUtils.MIS;
import static org.knime.core.expressions.AstTestUtils.OP;
import static org.knime.core.expressions.AstTestUtils.ROW_ID;
import static org.knime.core.expressions.AstTestUtils.ROW_INDEX;
import static org.knime.core.expressions.AstTestUtils.STR;
import static org.knime.core.expressions.TestUtils.COLUMN_ID;
import static org.knime.core.expressions.TestUtils.COLUMN_NAME;
import static org.knime.core.expressions.TestUtils.computerResultChecker;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.FloatComputerResultSupplier;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.IntegerComputerResultSupplier;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class EvaluationTest {

    /** A large long value for which <code>(double)(LARGE_NUMBER) == (double)(LARGE_NUMBER+1)</code> */
    private static final long LARGE_NUMBER = 9007199254740995L;

    @ParameterizedTest
    @EnumSource(ExecutionTest.class)
    void test(final ExecutionTest params) throws Exception {
        var ast = params.m_expression;
        Typing.inferTypes(ast, //
            FIND_TEST_COLUMN.andThen(c -> ReturnResult.fromOptional(c, "col missing").map(TestColumn::type)), //
            FIND_TEST_FLOW_VARIABLE
                .andThen(c -> ReturnResult.fromOptional(c, "var missing").map(TestFlowVariable::type)));
        var result = Evaluation.evaluate( //
            ast, //
            COLUMN_ID //
                .andThen(COLUMN_NAME) //
                .andThen(FIND_TEST_COLUMN) //
                .andThen(c -> c.map(TestColumn::computer)), //
            TestUtils.FLOW_VAR_NAME.andThen(FIND_TEST_FLOW_VARIABLE).andThen(c -> c.map(TestFlowVariable::computer)), //
            TestAggregations.TEST_AGGREGATIONS_COMPUTER);
        assertNotNull(result, "should output result");
        params.m_resultChecker.accept(result);
    }

    @Test
    void testErroringFn() throws Exception {
        var ast = FUN(TestFunctions.ERRORING_FN, STR("foo bar message"));
        Typing.inferTypes(ast, //
            c -> ReturnResult.failure("no columns"), //
            f -> ReturnResult.failure("no flow variables") //
        );
        var result = Evaluation.evaluate(ast, //
            c -> fail("should not call column computer"), //
            f -> fail("should not call flow variable computer"), //
            a -> fail("should not call aggregation computer") //
        );

        assertNotNull(result, "should output result");
        var ctx = EvaluationContext.of(TestUtils.DUMMY_EXECUTION_START_TIME, c -> fail("should not warn"));
        Assertions.assertFalse(result.isMissing(ctx));

        var intResult = assertInstanceOf(IntegerComputer.class, result);
        var exception = assertThrows(ExpressionEvaluationException.class, () -> intResult.compute(ctx));
        assertEquals("foo bar message", exception.getMessage());
    }

    private static enum ExecutionTest {

            // === Constants

            // Missing
            CONSTANT_MISSING(MIS()), //
            // Boolean
            CONSTANT_BOOLEAN_TRUE(BOOL(true), true), //
            CONSTANT_BOOLEAN_FALSE(BOOL(false), false), //
            // Integer
            CONSTANT_INTEGER_ZERO(INT(0), 0), //
            CONSTANT_INTEGER_NEGATIVE(INT(-1000), -1000), //
            CONSTANT_INTEGER_MAX_VALUE(INT(Long.MAX_VALUE), Long.MAX_VALUE), //
            CONSTANT_INTEGER_MIN_VALUE(INT(Long.MIN_VALUE), Long.MIN_VALUE), //
            // Float
            CONSTANT_FLOAT_ZERO(FLOAT(0.0), 0.0), //
            CONSTANT_FLOAT_ONE(FLOAT(1), 1.0), //
            CONSTANT_FLOAT_VERY_SMALL(FLOAT(Double.MIN_VALUE), Double.MIN_VALUE), //
            CONSTANT_FLOAT_VERY_LARGE(FLOAT(Double.MAX_VALUE), Double.MAX_VALUE), //
            // String
            CONSTANT_STRING_HELLO_WORLD(STR("Hello World"), "Hello World"), //
            CONSTANT_STRING_WITH_NEWLINES(STR("Hello\nWorld"), "Hello\nWorld"), //
            CONSTANT_STRING_WITH_SPECIAL_CHARS(STR("Hello 世界"), "Hello 世界"), //

            // === Column Access

            COLUMN_BOOLEAN(COL("BOOLEAN"), true), //
            COLUMN_INTEGER(COL("INTEGER"), 100), //
            COLUMN_FLOAT(COL("FLOAT"), 10.5), //
            COLUMN_STRING(COL("STRING"), "column value"), //
            COLUMN_MISSING(COL("INTEGER_MISSING")), //
            COLUMN_ROW_INDEX(ROW_INDEX(), 99), //
            COLUMN_ROW_ID(ROW_ID(), "Row99"), //

            // === FlowVariable Access

            FLOW_VARIABLE_BOOLEAN(FLOW("BOOLEAN"), true), //
            FLOW_VARIABLE_INTEGER(FLOW("INTEGER"), 100), //
            FLOW_VARIABLE_FLOAT(FLOW("FLOAT"), 10.5), //
            FLOW_VARIABLE_STRING(FLOW("STRING"), "column value"), //
            FLOW_VARIABLE_MISSING(FLOW("INTEGER_MISSING")), //

            // === MISSING Fallback operator

            // Integer
            FALLBACK_NO_MISSING_INTEGER(OP(INT(10), MISSING_FALLBACK, INT(-100)), 10), //
            FALLBACK_FIRST_MISSING_INTEGER(OP(MIS(), MISSING_FALLBACK, INT(10)), 10), //
            FALLBACK_SECOND_MISSING_INTEGER(OP(INT(10), MISSING_FALLBACK, MIS()), 10), //
            // Float
            FALLBACK_NO_MISSING_FLOAT(OP(FLOAT(10), MISSING_FALLBACK, FLOAT(-100)), 10.0), //
            FALLBACK_FIRST_MISSING_FLOAT(OP(MIS(), MISSING_FALLBACK, FLOAT(10)), 10.0), //
            FALLBACK_SECOND_MISSING_FLOAT(OP(FLOAT(10), MISSING_FALLBACK, MIS()), 10.0), //
            // String
            FALLBACK_NO_MISSING_STRING(OP(STR("10"), MISSING_FALLBACK, STR("-100")), "10"), //
            FALLBACK_FIRST_MISSING_STRING(OP(MIS(), MISSING_FALLBACK, STR("10")), "10"), //
            FALLBACK_SECOND_MISSING_STRING(OP(STR("10"), MISSING_FALLBACK, MIS()), "10"), //
            // Boolean
            FALLBACK_NO_MISSING_BOOLEAN(OP(BOOL(true), MISSING_FALLBACK, BOOL(false)), true), //
            FALLBACK_FIRST_MISSING_BOOLEAN(OP(MIS(), MISSING_FALLBACK, BOOL(false)), false), //
            FALLBACK_SECOND_MISSING_BOOLEAN(OP(BOOL(true), MISSING_FALLBACK, MIS()), true), //
            // Mixed float and integer
            FALLBACK_NO_MISSING_INT_FLOAT(OP(INT(10), MISSING_FALLBACK, FLOAT(-100)), 10.0), //
            FALLBACK_NO_MISSING_FLOAT_INT(OP(FLOAT(10), MISSING_FALLBACK, INT(-100)), 10.0), //

            // === Arithmetic Operations

            // Addition
            ADDITION_OF_TWO_INTEGERS(OP(INT(10), PLUS, COL("INTEGER")), 110), //
            ADDITION_OF_INTEGER_AND_FLOAT(OP(INT(10), PLUS, COL("FLOAT")), 20.5), //
            ADDITION_OF_TWO_FLOATS(OP(COL("FLOAT"), PLUS, FLOAT(20.1)), 30.6), //
            ADDITION_OF_INTEGER_AND_MISSING(OP(INT(10), PLUS, COL("INTEGER_MISSING"))), //
            ADDITION_OF_FLOAT_AND_MISSING(OP(COL("INTEGER_MISSING"), PLUS, FLOAT(1.1))), //
            // Subtraction
            SUBTRACTION_OF_TWO_INTEGERS(OP(INT(10), MINUS, COL("INTEGER")), -90), //
            SUBTRACTION_OF_INTEGER_AND_FLOAT(OP(INT(11), MINUS, COL("FLOAT")), 0.5), //
            SUBTRACTION_OF_TWO_FLOATS(OP(COL("FLOAT"), MINUS, FLOAT(0.5)), 10.0), //
            SUBTRACTION_OF_INTEGER_AND_MISSING(OP(INT(10), MINUS, COL("INTEGER_MISSING"))), //
            SUBTRACTION_OF_FLOAT_AND_MISSING(OP(COL("INTEGER_MISSING"), MINUS, FLOAT(1.1))), //
            // Negation
            NEGATION_OF_INTEGER(OP(UnaryOperator.MINUS, COL("INTEGER")), -100), //
            NEGATION_OF_FLOAT(OP(UnaryOperator.MINUS, COL("FLOAT")), -10.5), //
            NEGATION_OF_MISSING(OP(UnaryOperator.MINUS, COL("INTEGER_MISSING"))), //
            // Multiplication
            MULTIPLICATION_OF_TWO_INTEGERS(OP(INT(10), MULTIPLY, COL("INTEGER")), 1000), //
            MULTIPLICATION_OF_INTEGER_AND_FLOAT(OP(INT(10), MULTIPLY, COL("FLOAT")), 105.), //
            MULTIPLICATION_OF_TWO_FLOATS(OP(COL("FLOAT"), MULTIPLY, FLOAT(20.1)), 211.05), //
            MULTIPLICATION_OF_INTEGER_AND_MISSING(OP(INT(10), MULTIPLY, COL("INTEGER_MISSING"))), //
            MULTIPLICATION_OF_FLOAT_AND_MISSING(OP(COL("INTEGER_MISSING"), MULTIPLY, FLOAT(1.1))), //
            // Division
            DIVISION_OF_TWO_INTEGERS(OP(INT(1000), DIVIDE, COL("INTEGER")), 10.0), //
            DIVISION_OF_INTEGER_AND_FLOAT(OP(COL("FLOAT"), DIVIDE, INT(10)), 1.05), //
            DIVISION_OF_TWO_FLOATS(OP(COL("FLOAT"), DIVIDE, FLOAT(2.0)), 5.25), //
            DIVISION_OF_INTEGER_AND_MISSING(OP(INT(10), DIVIDE, COL("INTEGER_MISSING"))), //
            DIVISION_OF_FLOAT_AND_MISSING(OP(COL("INTEGER_MISSING"), DIVIDE, FLOAT(1.1))), //
            // Floor division
            FLOOR_DIVISION_OF_TWO_INTEGERS(OP(INT(210), FLOOR_DIVIDE, COL("INTEGER")), 2), //
            FLOOR_DIVISION_OF_INTEGER_AND_MISSING(OP(INT(10), FLOOR_DIVIDE, COL("INTEGER_MISSING"))), //
            // Exponentiation
            EXPONENTIATION_OF_TWO_INTEGERS(OP(COL("INTEGER"), EXPONENTIAL, INT(2)), 10000), //
            EXPONENTIATION_OF_INTEGER_AND_FLOAT(OP(COL("FLOAT"), EXPONENTIAL, INT(4)), 12155.0625), //
            EXPONENTIATION_OF_TWO_FLOATS(OP(COL("FLOAT"), EXPONENTIAL, FLOAT(3.0)), 1157.625), //
            EXPONENTIATION_OF_INTEGER_AND_MISSING(OP(INT(10), EXPONENTIAL, COL("INTEGER_MISSING"))), //
            EXPONENTIATION_OF_FLOAT_AND_MISSING(OP(COL("INTEGER_MISSING"), EXPONENTIAL, FLOAT(1.1))), //
            // Modulo
            MODULO_OF_TWO_INTEGERS(OP(INT(1021), REMAINDER, COL("INTEGER")), 21), //
            MODULO_OF_INTEGER_AND_FLOAT(OP(COL("FLOAT"), REMAINDER, INT(2)), 0.5), //
            MODULO_OF_TWO_FLOATS(OP(COL("FLOAT"), REMAINDER, FLOAT(2.5)), 0.5), //
            MODULO_OF_INTEGER_AND_MISSING(OP(INT(10), REMAINDER, COL("INTEGER_MISSING"))), //
            MODULO_OF_FLOAT_AND_MISSING(OP(COL("INTEGER_MISSING"), REMAINDER, FLOAT(1.1))), //

            // = Division by zero - TODO(AP-22272) check for warnings
            // Divide
            DIVISION_BY_ZERO_FLOAT(OP(FLOAT(10.0), DIVIDE, FLOAT(0.0)), Double.POSITIVE_INFINITY), //
            DIVISION_BY_ZERO_NEG_FLOAT(OP(FLOAT(-10.0), DIVIDE, FLOAT(0.0)), Double.NEGATIVE_INFINITY), //
            DIVISION_BY_NEG_ZERO_FLOAT(OP(FLOAT(10.0), DIVIDE, FLOAT(-0.0)), Double.NEGATIVE_INFINITY), //
            DIVISION_BY_NEG_ZERO_NEG_FLOAT(OP(FLOAT(-10.0), DIVIDE, FLOAT(-0.0)), Double.POSITIVE_INFINITY), //
            DIVISION_BY_ZERO_ZERO(OP(FLOAT(0.0), DIVIDE, FLOAT(0.0)), Double.NaN), //
            DIVISION_BY_ZERO_INT(OP(INT(10), DIVIDE, INT(0)), Double.POSITIVE_INFINITY), //
            DIVISION_BY_ZERO_NEG_INT(OP(INT(-10), DIVIDE, INT(0)), Double.NEGATIVE_INFINITY), //
            DIVISION_BY_ZERO_ZERO_INT(OP(INT(0), DIVIDE, INT(0)), Double.NaN), //
            // Floor division
            FLOOR_DIVISION_BY_ZERO(OP(INT(10), FLOOR_DIVIDE, INT(0)), 0), //
            FLOOR_DIVISION_BY_ZERO_NEG(OP(INT(-10), FLOOR_DIVIDE, INT(0)), 0), //
            FLOOR_DIVISION_BY_ZERO_ZERO(OP(INT(0), FLOOR_DIVIDE, INT(0)), 0), //
            // Modulo
            MODULO_BY_ZERO_FLOAT(OP(FLOAT(10.0), REMAINDER, FLOAT(0.0)), Double.NaN), //
            MODULO_BY_ZERO_NEG_FLOAT(OP(FLOAT(-10.0), REMAINDER, FLOAT(0.0)), Double.NaN), //
            MODULO_BY_NEG_ZERO_FLOAT(OP(FLOAT(10.0), REMAINDER, FLOAT(-0.0)), Double.NaN), //
            MODULO_BY_NEG_ZERO_NEG_FLOAT(OP(FLOAT(-10.0), REMAINDER, FLOAT(-0.0)), Double.NaN), //
            MODULO_BY_ZERO_ZERO(OP(FLOAT(0.0), REMAINDER, FLOAT(0.0)), Double.NaN), //
            MODULO_BY_ZERO_INT(OP(INT(10), REMAINDER, INT(0)), 0), //
            MODULO_BY_ZERO_NEG_INT(OP(INT(-10), REMAINDER, INT(0)), 0), //
            MODULO_BY_ZERO_ZERO_INT(OP(INT(0), REMAINDER, INT(0)), 0), //

            // === Comparison Operations

            // = Ordering

            // Less than
            LESS_THAN_TWO_INTEGERS(OP(INT(10), LESS_THAN, COL("INTEGER")), true), //
            LESS_THAN_TWO_LARGE_INTEGERS(OP(INT(LARGE_NUMBER), LESS_THAN, INT(LARGE_NUMBER + 1)), true), //
            LESS_THAN_TWO_FLOAT(OP(FLOAT(1.1), LESS_THAN, FLOAT(1.1)), false), //
            LESS_THAN_INTEGER_AND_FLOAT(OP(INT(100), LESS_THAN, FLOAT(99.9)), false), //
            LESS_THAN_LARGE_WITH_CAST_TO_FLOAT(OP(INT(LARGE_NUMBER), LESS_THAN, FLOAT(LARGE_NUMBER + 1)), false), //
            LESS_THAN_INTEGER_MISSING(OP(INT(Long.MIN_VALUE), LESS_THAN, COL("INTEGER_MISSING")), false), //
            LESS_THAN_FLOAT_MISSING(OP(COL("INTEGER_MISSING"), LESS_THAN, FLOAT(100)), false), //
            // Less than or equal
            LESS_THAN_EQUAL_TWO_INTEGERS(OP(INT(10), LESS_THAN_EQUAL, COL("INTEGER")), true), //
            LESS_THAN_EQUAL_TWO_LARGE_INTEGERS(OP(INT(LARGE_NUMBER), LESS_THAN_EQUAL, INT(LARGE_NUMBER - 1)), false), //
            LESS_THAN_EQUAL_TWO_FLOAT(OP(FLOAT(1.11), LESS_THAN_EQUAL, FLOAT(1.1)), false), //
            LESS_THAN_EQUAL_INTEGER_AND_FLOAT(OP(INT(100), LESS_THAN_EQUAL, FLOAT(99.9)), false), //
            LESS_THAN_EQUAL_LARGE_WITH_CAST_TO_FLOAT(OP(INT(LARGE_NUMBER + 1), LESS_THAN_EQUAL, FLOAT(LARGE_NUMBER)),
                true), //
            LESS_THAN_EQUAL_INTEGER_MISSING(OP(INT(Long.MIN_VALUE), LESS_THAN_EQUAL, COL("INTEGER_MISSING")), false), //
            LESS_THAN_EQUAL_FLOAT_MISSING(OP(COL("INTEGER_MISSING"), LESS_THAN_EQUAL, FLOAT(100)), false), //
            LESS_THAN_EQUAL_TWO_MISSING(OP(COL("INTEGER_MISSING"), LESS_THAN_EQUAL, COL("FLOAT_MISSING")), true), //
            LESS_THAN_EQUAL_TWO_MISSING_INTEGER(OP(COL("INTEGER_MISSING"), LESS_THAN_EQUAL, COL("INTEGER_MISSING")),
                true), //
            // Greater than
            GREATER_THAN_TWO_INTEGERS(OP(INT(101), GREATER_THAN, COL("INTEGER")), true), //
            GREATER_THAN_TWO_LARGE_INTEGERS(OP(INT(LARGE_NUMBER), GREATER_THAN, INT(LARGE_NUMBER - 1)), true), //
            GREATER_THAN_TWO_FLOAT(OP(FLOAT(1.11), GREATER_THAN, FLOAT(1.1)), true), //
            GREATER_THAN_INTEGER_AND_FLOAT(OP(INT(100), GREATER_THAN, FLOAT(99.9)), true), //
            GREATER_THAN_LARGE_WITH_CAST_TO_FLOAT(OP(INT(LARGE_NUMBER + 1), GREATER_THAN, FLOAT(LARGE_NUMBER)), false), //
            GREATER_THAN_INTEGER_MISSING(OP(INT(Long.MAX_VALUE), GREATER_THAN, COL("INTEGER_MISSING")), false), //
            GREATER_THAN_FLOAT_MISSING(OP(COL("INTEGER_MISSING"), GREATER_THAN, FLOAT(100)), false), //
            // Greater than or equal
            GREATER_THAN_EQUAL_TWO_INTEGERS(OP(INT(100), GREATER_THAN_EQUAL, COL("INTEGER")), true), //
            GREATER_THAN_EQUAL_TWO_LARGE_INTEGERS(OP(INT(LARGE_NUMBER), GREATER_THAN_EQUAL, INT(LARGE_NUMBER + 1)),
                false), //
            GREATER_THAN_EQUAL_TWO_FLOAT(OP(FLOAT(1.1), GREATER_THAN_EQUAL, FLOAT(1.1)), true), //
            GREATER_THAN_EQUAL_INTEGER_AND_FLOAT(OP(INT(100), GREATER_THAN_EQUAL, FLOAT(100.001)), false), //
            GREATER_THAN_EQUAL_LARGE_WITH_CAST_TO_FLOAT(
                OP(INT(LARGE_NUMBER), GREATER_THAN_EQUAL, FLOAT(LARGE_NUMBER + 1)), true), //
            GREATER_THAN_EQUAL_INTEGER_MISSING(OP(INT(Long.MAX_VALUE), GREATER_THAN_EQUAL, COL("INTEGER_MISSING")),
                false), //
            GREATER_THAN_EQUAL_FLOAT_MISSING(OP(COL("INTEGER_MISSING"), GREATER_THAN_EQUAL, FLOAT(100)), false), //
            GREATER_THAN_EQUAL_TWO_MISSING(OP(COL("INTEGER_MISSING"), GREATER_THAN_EQUAL, COL("FLOAT_MISSING")), true), //
            GREATER_THAN_EQUAL_TWO_MISSING_INTEGER(
                OP(COL("INTEGER_MISSING"), GREATER_THAN_EQUAL, COL("INTEGER_MISSING")), true), //

            // = Equality

            EQUAL_TWO_BOOLEANS(OP(BOOL(false), EQUAL_TO, COL("BOOLEAN")), false), //
            EQUAL_TWO_INTEGERS(OP(INT(100), EQUAL_TO, COL("INTEGER")), true), //
            EQUAL_TWO_FLOAT(OP(FLOAT(10.4), EQUAL_TO, COL("FLOAT")), false), //
            EQUAL_INTEGER_AND_FLOAT(OP(FLOAT(100.0), EQUAL_TO, COL("INTEGER")), true), //
            EQUAL_STRINGS(OP(STR("column value"), EQUAL_TO, COL("STRING")), true), //
            // not
            NOT_EQUAL_TWO_BOOLEANS(OP(COL("BOOLEAN"), NOT_EQUAL_TO, BOOL(true)), false), //
            NOT_EQUAL_TWO_INTEGERS(OP(COL("INTEGER"), NOT_EQUAL_TO, INT(101)), true), //
            NOT_EQUAL_TWO_FLOAT(OP(COL("FLOAT"), NOT_EQUAL_TO, FLOAT(10.5)), false), //
            NOT_EQUAL_INTEGER_AND_FLOAT(OP(COL("INTEGER"), NOT_EQUAL_TO, FLOAT(99.99)), true), //
            NOT_EQUAL_STRINGS(OP(COL("STRING"), NOT_EQUAL_TO, STR("foo")), true), //
            // missing
            EQUAL_TWO_MISSING(OP(MIS(), EQUAL_TO, COL("INTEGER_MISSING")), true), //
            EQUAL_TWO_EXPLICIT_MISSING(OP(MIS(), EQUAL_TO, MIS()), true), //
            NOT_EQUAL_TWO_MISSING(OP(COL("INTEGER_MISSING"), NOT_EQUAL_TO, MIS()), false), //
            EQUAL_BOOL_AND_MISSING(OP(MIS(), EQUAL_TO, COL("BOOLEAN")), false), //
            EQUAL_INT_AND_MISSING(OP(COL("INTEGER"), EQUAL_TO, MIS()), false), //
            EQUAL_FLOAT_AND_MISSING(OP(MIS(), EQUAL_TO, COL("FLOAT")), false), //
            EQUAL_STRING_AND_MISSING(OP(MIS(), EQUAL_TO, COL("STRING")), false), //

            // === Logical Operations

            // And
            LOGICAL_AND_TRUE_TRUE(OP(COL("BOOLEAN"), CONDITIONAL_AND, BOOL(true)), true), //
            LOGICAL_AND_TRUE_MISSING(OP(COL("BOOLEAN"), CONDITIONAL_AND, COL("BOOLEAN_MISSING"))), //
            LOGICAL_AND_TRUE_FALSE(OP(COL("BOOLEAN"), CONDITIONAL_AND, BOOL(false)), false), //
            LOGICAL_AND_MISSING_TRUE(OP(COL("BOOLEAN_MISSING"), CONDITIONAL_AND, BOOL(true))), //
            LOGICAL_AND_MISSING_MISSING(OP(COL("BOOLEAN_MISSING"), CONDITIONAL_AND, COL("BOOLEAN_MISSING"))), //
            LOGICAL_AND_MISSING_FALSE(OP(COL("BOOLEAN_MISSING"), CONDITIONAL_AND, BOOL(false)), false), //
            LOGICAL_AND_FALSE_TRUE(OP(BOOL(false), CONDITIONAL_AND, BOOL(true)), false), //
            LOGICAL_AND_FALSE_MISSING(OP(BOOL(false), CONDITIONAL_AND, COL("BOOLEAN_MISSING")), false), //
            LOGICAL_AND_FALSE_FALSE(OP(BOOL(false), CONDITIONAL_AND, BOOL(false)), false), //
            // Or
            LOGICAL_OR_TRUE_TRUE(OP(COL("BOOLEAN"), CONDITIONAL_OR, BOOL(true)), true), //
            LOGICAL_OR_TRUE_MISSING(OP(COL("BOOLEAN"), CONDITIONAL_OR, COL("BOOLEAN_MISSING")), true), //
            LOGICAL_OR_TRUE_FALSE(OP(COL("BOOLEAN"), CONDITIONAL_OR, BOOL(false)), true), //
            LOGICAL_OR_MISSING_TRUE(OP(COL("BOOLEAN_MISSING"), CONDITIONAL_OR, BOOL(true)), true), //
            LOGICAL_OR_MISSING_MISSING(OP(COL("BOOLEAN_MISSING"), CONDITIONAL_OR, COL("BOOLEAN_MISSING"))), //
            LOGICAL_OR_MISSING_FALSE(OP(COL("BOOLEAN_MISSING"), CONDITIONAL_OR, BOOL(false))), //
            LOGICAL_OR_FALSE_TRUE(OP(BOOL(false), CONDITIONAL_OR, BOOL(true)), true), //
            LOGICAL_OR_FALSE_MISSING(OP(BOOL(false), CONDITIONAL_OR, COL("BOOLEAN_MISSING"))), //
            LOGICAL_OR_FALSE_FALSE(OP(BOOL(false), CONDITIONAL_OR, BOOL(false)), false), //
            // Not
            LOGICAL_NOT_TRUE(OP(NOT, BOOL(true)), false), //
            LOGICAL_NOT_MISSING(OP(NOT, COL("BOOLEAN_MISSING"))), //
            LOGICAL_NOT_FALE(OP(NOT, BOOL(false)), true), //

            // === String Concatenation

            STRING_CONCAT_TWO_STRINGS(OP(STR("Hello "), PLUS, STR("World!")), "Hello World!"), //
            STRING_CONCAT_STRING_AND_MISSING(OP(STR("Hello "), PLUS, COL("INTEGER_MISSING")), "Hello MISSING"), //
            STRING_CONCAT_MISSING_AND_MISSING(OP(COL("INTEGER_MISSING"), PLUS, COL("STRING_MISSING")),
                "MISSINGMISSING"), //
            STRING_CONCAT_STRING_AND_INTEGER(OP(STR("The solution is "), PLUS, INT(42)), "The solution is 42"), //
            STRING_CONCAT_STRING_AND_FLOAT(OP(FLOAT(0.0001), PLUS, STR(" is pretty small")), "1.0E-4 is pretty small"), //
            STRING_CONCAT_STRING_AND_TRUE(OP(STR("This is "), PLUS, BOOL(true)), "This is true"), //
            STRING_CONCAT_STRING_AND_FALSE(OP(BOOL(false), PLUS, STR(" it is")), "false it is"), //

            // === Function and Aggregation calls

            FN_PLUS_100(FUN(TestFunctions.PLUS_100_FN, INT(10)), 110), //
            AGG_RETURN_42_WITH_COL_TYPE_I(AstTestUtils.AGG(TestAggregations.RETURN_42_WITH_COL_TYPE, STR("INTEGER")),
                42), //
            AGG_RETURN_42_WITH_COL_TYPE_F(AstTestUtils.AGG(TestAggregations.RETURN_42_WITH_COL_TYPE, STR("FLOAT")),
                42.0), //

            // === Time types

            // Time unary operators
            NEGATE_TIME_DURATION(OP(UnaryOperator.MINUS, F_TIME_DURATION("P1D")), Duration.ofDays(-1)), //
            NEGATE_DATE_DURATION(OP(UnaryOperator.MINUS, F_DATE_DURATION("P1M")), Period.of(0, -1, 0)), //

            // Time binary operators (temporalamount - temporalamount)
            DIFFERENCE_TIME_DURATION(OP(F_TIME_DURATION("P1D"), MINUS, F_TIME_DURATION("PT1H")), Duration.ofHours(23)), //
            DIFFERENCE_DATE_DURATION(OP(F_DATE_DURATION("P1M"), MINUS, F_DATE_DURATION("P1D")), Period.of(0, 1, -1)), //

            // Time binary operators (temporal - temporal)
            DIFFERENCE_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-02"), MINUS, F_LOCAL_DATE("2019-01-01")),
                Period.of(0, 0, 1)), //
            DIFFERENCE_LOCAL_TIME(OP(F_LOCAL_TIME("12:00"), MINUS, F_LOCAL_TIME("11:00")), Duration.ofHours(1)), //
            DIFFERENCE_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), MINUS, F_LOCAL_DATE_TIME("2019-01-01T11:00")),
                Duration.ofHours(25)), //
            DIFFERENCE_ZONED_DATE_TIME(
                OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), MINUS, F_ZONED_DATE_TIME("2019-01-01T11:00+01:00")),
                Duration.ofHours(25)), //

            // Time binary operators (temporal - temporalamount)
            DIFFERENCE_LOCAL_TIME_TIME_DURATION(OP(F_LOCAL_TIME("12:00"), MINUS, F_TIME_DURATION("PT1H")),
                LocalTime.of(11, 0)), //
            DIFFERENCE_LOCAL_DATE_TIME_TIME_DURATION(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), MINUS, F_TIME_DURATION("PT1H")),
                LocalDateTime.of(2019, 1, 2, 11, 0)), //
            DIFFERENCE_ZONED_DATE_TIME_TIME_DURATION(
                OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), MINUS, F_TIME_DURATION("PT1H")),
                ZonedDateTime.of(2019, 1, 2, 11, 0, 0, 0, ZoneOffset.ofHours(1))), //
            DIFFERENCE_LOCAL_DATE_DATE_DURATION(OP(F_LOCAL_DATE("2019-01-02"), MINUS, F_DATE_DURATION("P1D")),
                LocalDate.of(2019, 1, 1)), //
            DIFFERENCE_LOCAL_DATE_TIME_DATE_DURATION(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), MINUS, F_DATE_DURATION("P1D")),
                LocalDateTime.of(2019, 1, 1, 12, 0)), //
            DIFFERENCE_ZONED_DATE_TIME_DATE_DURATION(
                OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), MINUS, F_DATE_DURATION("P1D")),
                ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1))), //

            // Time binary operators (temporalamount + temporalamount)
            SUM_TIME_DURATION(OP(F_TIME_DURATION("P1D"), PLUS, F_TIME_DURATION("PT1H")), Duration.ofHours(25)), //
            SUM_DATE_DURATION(OP(F_DATE_DURATION("P1M"), PLUS, F_DATE_DURATION("P1D")), Period.of(0, 1, 1)), //

            // Time binary operators (temporal + temporalamount)
            SUM_LOCAL_TIME_TIME_DURATION(OP(F_LOCAL_TIME("12:00"), PLUS, F_TIME_DURATION("PT1H")), LocalTime.of(13, 0)), //
            SUM_LOCAL_DATE_TIME_TIME_DURATION(OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), PLUS, F_TIME_DURATION("PT1H")),
                LocalDateTime.of(2019, 1, 2, 13, 0)), //
            SUM_ZONED_DATE_TIME_TIME_DURATION(
                OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), PLUS, F_TIME_DURATION("PT1H")),
                ZonedDateTime.of(2019, 1, 2, 13, 0, 0, 0, ZoneOffset.ofHours(1))), //
            SUM_LOCAL_DATE_DATE_DURATION(OP(F_LOCAL_DATE("2019-01-02"), PLUS, F_DATE_DURATION("P1D")),
                LocalDate.of(2019, 1, 3)), //
            SUM_LOCAL_DATE_TIME_DATE_DURATION(OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), PLUS, F_DATE_DURATION("P1D")),
                LocalDateTime.of(2019, 1, 3, 12, 0)), //
            SUM_ZONED_DATE_TIME_DATE_DURATION(
                OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), PLUS, F_DATE_DURATION("P1D")),
                ZonedDateTime.of(2019, 1, 3, 12, 0, 0, 0, ZoneOffset.ofHours(1))), //

            // Time binary operators (temporalamount * scalar)
            MULTIPLY_TIME_DURATION(OP(F_TIME_DURATION("P1D"), MULTIPLY, INT(2)), Duration.ofDays(2)), //
            MULTIPLY_DATE_DURATION(OP(F_DATE_DURATION("P1M"), MULTIPLY, INT(2)), Period.of(0, 2, 0)), //

            // Time ordering
            LT_TIME_DURATION(OP(F_TIME_DURATION("P1D"), LESS_THAN, F_TIME_DURATION("P2D")), true), //
            LT_TIME_DURATION_FALSE(OP(F_TIME_DURATION("P2D"), LESS_THAN, F_TIME_DURATION("P1D")), false), //
            LT_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-01"), LESS_THAN, F_LOCAL_DATE("2019-01-02")), true), //
            LT_LOCAL_DATE_FALSE(OP(F_LOCAL_DATE("2019-01-02"), LESS_THAN, F_LOCAL_DATE("2019-01-01")), false), //
            LT_LOCAL_TIME(OP(F_LOCAL_TIME("11:00"), LESS_THAN, F_LOCAL_TIME("12:00")), true), //
            LT_LOCAL_TIME_FALSE(OP(F_LOCAL_TIME("12:00"), LESS_THAN, F_LOCAL_TIME("11:00")), false), //
            LT_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), LESS_THAN, F_LOCAL_DATE_TIME("2019-01-02T12:00")), true), //
            LT_LOCAL_DATE_TIME_FALSE(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), LESS_THAN, F_LOCAL_DATE_TIME("2019-01-01T11:00")), false), //
            LT_ZONED_DATE_TIME(
                OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), LESS_THAN, F_ZONED_DATE_TIME("2019-01-02T12:00+01:00")),
                true), //
            LT_ZONED_DATE_TIME_FALSE(
                OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), LESS_THAN, F_ZONED_DATE_TIME("2019-01-01T11:00+01:00")),
                false), //
            LE_TIME_DURATION(OP(F_TIME_DURATION("P1D"), LESS_THAN_EQUAL, F_TIME_DURATION("P1D")), true), //
            LE_TIME_DURATION_FALSE(OP(F_TIME_DURATION("P2D"), LESS_THAN_EQUAL, F_TIME_DURATION("P1D")), false), //
            LE_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-01"), LESS_THAN_EQUAL, F_LOCAL_DATE("2019-01-01")), true), //
            LE_LOCAL_DATE_FALSE(OP(F_LOCAL_DATE("2019-01-02"), LESS_THAN_EQUAL, F_LOCAL_DATE("2019-01-01")), false), //
            LE_LOCAL_TIME(OP(F_LOCAL_TIME("11:00"), LESS_THAN_EQUAL, F_LOCAL_TIME("11:00")), true), //
            LE_LOCAL_TIME_FALSE(OP(F_LOCAL_TIME("12:00"), LESS_THAN_EQUAL, F_LOCAL_TIME("11:00")), false), //
            LE_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), LESS_THAN_EQUAL, F_LOCAL_DATE_TIME("2019-01-01T11:00")),
                true), //
            LE_LOCAL_DATE_TIME_FALSE(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), LESS_THAN_EQUAL, F_LOCAL_DATE_TIME("2019-01-01T11:00")),
                false), //
            LE_ZONED_DATE_TIME(OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), LESS_THAN_EQUAL,
                F_ZONED_DATE_TIME("2019-01-01T11:00+01:00")), true), //
            LE_ZONED_DATE_TIME_FALSE(OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), LESS_THAN_EQUAL,
                F_ZONED_DATE_TIME("2019-01-01T11:00+01:00")), false), //
            GT_TIME_DURATION(OP(F_TIME_DURATION("P2D"), GREATER_THAN, F_TIME_DURATION("P1D")), true), //
            GT_TIME_DURATION_FALSE(OP(F_TIME_DURATION("P1D"), GREATER_THAN, F_TIME_DURATION("P2D")), false), //
            GT_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-02"), GREATER_THAN, F_LOCAL_DATE("2019-01-01")), true), //
            GT_LOCAL_DATE_FALSE(OP(F_LOCAL_DATE("2019-01-01"), GREATER_THAN, F_LOCAL_DATE("2019-01-02")), false), //
            GT_LOCAL_TIME(OP(F_LOCAL_TIME("12:00"), GREATER_THAN, F_LOCAL_TIME("11:00")), true), //
            GT_LOCAL_TIME_FALSE(OP(F_LOCAL_TIME("11:00"), GREATER_THAN, F_LOCAL_TIME("12:00")), false), //
            GT_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), GREATER_THAN, F_LOCAL_DATE_TIME("2019-01-01T11:00")), true), //
            GT_LOCAL_DATE_TIME_FALSE(
                OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), GREATER_THAN, F_LOCAL_DATE_TIME("2019-01-02T12:00")), false), //
            GT_ZONED_DATE_TIME(OP(F_ZONED_DATE_TIME("2019-01-02T12:00+01:00"), GREATER_THAN,
                F_ZONED_DATE_TIME("2019-01-01T11:00+01:00")), true), //
            GT_ZONED_DATE_TIME_FALSE(OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), GREATER_THAN,
                F_ZONED_DATE_TIME("2019-01-02T12:00+01:00")), false), //
            GE_TIME_DURATION(OP(F_TIME_DURATION("P1D"), GREATER_THAN_EQUAL, F_TIME_DURATION("P1D")), true), //
            GE_TIME_DURATION_FALSE(OP(F_TIME_DURATION("P1D"), GREATER_THAN_EQUAL, F_TIME_DURATION("P2D")), false), //
            GE_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-01"), GREATER_THAN_EQUAL, F_LOCAL_DATE("2019-01-01")), true), //
            GE_LOCAL_DATE_FALSE(OP(F_LOCAL_DATE("2019-01-01"), GREATER_THAN_EQUAL, F_LOCAL_DATE("2019-01-02")), false), //
            GE_LOCAL_TIME(OP(F_LOCAL_TIME("11:00"), GREATER_THAN_EQUAL, F_LOCAL_TIME("11:00")), true), //
            GE_LOCAL_TIME_FALSE(OP(F_LOCAL_TIME("11:00"), GREATER_THAN_EQUAL, F_LOCAL_TIME("12:00")), false), //
            GE_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), GREATER_THAN_EQUAL, F_LOCAL_DATE_TIME("2019-01-01T11:00")),
                true), //
            GE_LOCAL_DATE_TIME_FALSE(
                OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), GREATER_THAN_EQUAL, F_LOCAL_DATE_TIME("2019-01-02T12:00")),
                false), //
            GE_ZONED_DATE_TIME(OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), GREATER_THAN_EQUAL,
                F_ZONED_DATE_TIME("2019-01-01T11:00+01:00")), true), //
            GE_ZONED_DATE_TIME_FALSE(OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), GREATER_THAN_EQUAL,
                F_ZONED_DATE_TIME("2019-01-02T12:00+01:00")), false), //

            // time ordering with MISSING values
            LT_TIME_DURATION_MISSING(OP(F_TIME_DURATION("P1D"), LESS_THAN, F_TIME_DURATION()), false), //
            LT_LOCAL_DATE_MISSING(OP(F_LOCAL_DATE("2019-01-01"), LESS_THAN, F_LOCAL_DATE()), false), //
            LT_LOCAL_TIME_MISSING(OP(F_LOCAL_TIME("12:00"), LESS_THAN, F_LOCAL_TIME()), false), //
            LT_LOCAL_DATE_TIME_MISSING(OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), LESS_THAN, F_LOCAL_DATE_TIME()),
                false), //
            LT_ZONED_DATE_TIME_MISSING(OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), LESS_THAN, F_ZONED_DATE_TIME()),
                false), //
            LE_TIME_DURATION_MISSING(OP(F_TIME_DURATION("P1D"), LESS_THAN_EQUAL, F_TIME_DURATION()), false), //
            LE_LOCAL_DATE_MISSING(OP(F_LOCAL_DATE("2019-01-01"), LESS_THAN_EQUAL, F_LOCAL_DATE()), false), //
            LE_LOCAL_TIME_MISSING(OP(F_LOCAL_TIME("12:00"), LESS_THAN_EQUAL, F_LOCAL_TIME()), false), //
            LE_LOCAL_DATE_TIME_MISSING(OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), LESS_THAN_EQUAL, F_LOCAL_DATE_TIME()),
                false), //
            LE_ZONED_DATE_TIME_MISSING(
                OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), LESS_THAN_EQUAL, F_ZONED_DATE_TIME()), false), //
            GT_TIME_DURATION_MISSING(OP(F_TIME_DURATION("P1D"), GREATER_THAN, F_TIME_DURATION()), false), //
            GT_LOCAL_DATE_MISSING(OP(F_LOCAL_DATE("2019-01-01"), GREATER_THAN, F_LOCAL_DATE()), false), //
            GT_LOCAL_TIME_MISSING(OP(F_LOCAL_TIME("12:00"), GREATER_THAN, F_LOCAL_TIME()), false), //
            GT_LOCAL_DATE_TIME_MISSING(OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), GREATER_THAN, F_LOCAL_DATE_TIME()),
                false), //
            GT_ZONED_DATE_TIME_MISSING(
                OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), GREATER_THAN, F_ZONED_DATE_TIME()), false), //
            GE_TIME_DURATION_MISSING(OP(F_TIME_DURATION("P1D"), GREATER_THAN_EQUAL, F_TIME_DURATION()), false), //
            GE_LOCAL_DATE_MISSING(OP(F_LOCAL_DATE("2019-01-01"), GREATER_THAN_EQUAL, F_LOCAL_DATE()), false), //
            GE_LOCAL_TIME_MISSING(OP(F_LOCAL_TIME("12:00"), GREATER_THAN_EQUAL, F_LOCAL_TIME()), false), //
            GE_LOCAL_DATE_TIME_MISSING(
                OP(F_LOCAL_DATE_TIME("2019-01-01T11:00"), GREATER_THAN_EQUAL, F_LOCAL_DATE_TIME()), false), //
            GE_ZONED_DATE_TIME_MISSING(
                OP(F_ZONED_DATE_TIME("2019-01-01T11:00+01:00"), GREATER_THAN_EQUAL, F_ZONED_DATE_TIME()), false), //
            // and a couple with both missing (should be true for leq and geq, false for lt and gt)
            LT_TIME_DURATION_MISSING_MISSING(OP(F_TIME_DURATION(), LESS_THAN, F_TIME_DURATION()), false), //
            LE_TIME_DURATION_MISSING_MISSING(OP(F_TIME_DURATION(), LESS_THAN_EQUAL, F_TIME_DURATION()), true), //
            GT_TIME_DURATION_MISSING_MISSING(OP(F_TIME_DURATION(), GREATER_THAN, F_TIME_DURATION()), false), //
            GE_TIME_DURATION_MISSING_MISSING(OP(F_TIME_DURATION(), GREATER_THAN_EQUAL, F_TIME_DURATION()), true), //
            LT_LOCAL_DATE_MISSING_MISSING(OP(F_LOCAL_DATE(), LESS_THAN, F_LOCAL_DATE()), false), //
            LE_LOCAL_DATE_MISSING_MISSING(OP(F_LOCAL_DATE(), LESS_THAN_EQUAL, F_LOCAL_DATE()), true), //
            GT_LOCAL_DATE_MISSING_MISSING(OP(F_LOCAL_DATE(), GREATER_THAN, F_LOCAL_DATE()), false), //
            GE_LOCAL_DATE_MISSING_MISSING(OP(F_LOCAL_DATE(), GREATER_THAN_EQUAL, F_LOCAL_DATE()), true), //
            LT_LOCAL_TIME_MISSING_MISSING(OP(F_LOCAL_TIME(), LESS_THAN, F_LOCAL_TIME()), false), //
            LE_LOCAL_TIME_MISSING_MISSING(OP(F_LOCAL_TIME(), LESS_THAN_EQUAL, F_LOCAL_TIME()), true), //
            GT_LOCAL_TIME_MISSING_MISSING(OP(F_LOCAL_TIME(), GREATER_THAN, F_LOCAL_TIME()), false), //
            GE_LOCAL_TIME_MISSING_MISSING(OP(F_LOCAL_TIME(), GREATER_THAN_EQUAL, F_LOCAL_TIME()), true), //

            // Time equality
            EQ_EQUAL_TIME_DURATIONS(OP(F_TIME_DURATION("P1D"), EQUAL_TO, F_TIME_DURATION("P1D")), true), //
            EQ_UNEQUAL_TIME_DURATION(OP(F_TIME_DURATION("P1D"), EQUAL_TO, F_TIME_DURATION("P2D")), false), //
            EQ_EQUAL_DATE_DURATIONS(OP(F_DATE_DURATION("P1M"), EQUAL_TO, F_DATE_DURATION("P1M")), true), //
            EQ_UNEQUAL_DATE_DURATION(OP(F_DATE_DURATION("P1M"), EQUAL_TO, F_DATE_DURATION("P2M")), false), //
            EQ_EQUAL_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-02"), EQUAL_TO, F_LOCAL_DATE("2019-01-02")), true), //
            EQ_UNEQUAL_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-02"), EQUAL_TO, F_LOCAL_DATE("2019-01-01")), false), //
            EQ_EQUAL_LOCAL_TIME(OP(F_LOCAL_TIME("12:00"), EQUAL_TO, F_LOCAL_TIME("12:00")), true), //
            EQ_UNEQUAL_LOCAL_TIME(OP(F_LOCAL_TIME("12:00"), EQUAL_TO, F_LOCAL_TIME("11:00")), false), //
            EQ_EQUAL_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), EQUAL_TO, F_LOCAL_DATE_TIME("2019-01-02T12:00")), true), //
            EQ_UNEQUAL_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), EQUAL_TO, F_LOCAL_DATE_TIME("2019-01-01T12:00")), false), //
            NEQ_EQUAL_TIME_DURATIONS(OP(F_TIME_DURATION("P1D"), NOT_EQUAL_TO, F_TIME_DURATION("P1D")), false), //
            NEQ_UNEQUAL_TIME_DURATION(OP(F_TIME_DURATION("P1D"), NOT_EQUAL_TO, F_TIME_DURATION("P2D")), true), //
            NEQ_EQUAL_DATE_DURATIONS(OP(F_DATE_DURATION("P1M"), NOT_EQUAL_TO, F_DATE_DURATION("P1M")), false), //
            NEQ_UNEQUAL_DATE_DURATION(OP(F_DATE_DURATION("P1M"), NOT_EQUAL_TO, F_DATE_DURATION("P2M")), true), //
            NEQ_EQUAL_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-02"), NOT_EQUAL_TO, F_LOCAL_DATE("2019-01-02")), false), //
            NEQ_UNEQUAL_LOCAL_DATE(OP(F_LOCAL_DATE("2019-01-02"), NOT_EQUAL_TO, F_LOCAL_DATE("2019-01-01")), true), //
            NEQ_EQUAL_LOCAL_TIME(OP(F_LOCAL_TIME("12:00"), NOT_EQUAL_TO, F_LOCAL_TIME("12:00")), false), //
            NEQ_UNEQUAL_LOCAL_TIME(OP(F_LOCAL_TIME("12:00"), NOT_EQUAL_TO, F_LOCAL_TIME("11:00")), true), //
            NEQ_EQUAL_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), NOT_EQUAL_TO, F_LOCAL_DATE_TIME("2019-01-02T12:00")), false), //
            NEQ_UNEQUAL_LOCAL_DATE_TIME(
                OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), NOT_EQUAL_TO, F_LOCAL_DATE_TIME("2019-01-01T12:00")), true), //

            // Time equality with MISSING values
            EQ_TIME_DURATION_MISSING(OP(F_TIME_DURATION("P1D"), EQUAL_TO, F_TIME_DURATION()), false), //
            EQ_DATE_DURATION_MISSING(OP(F_DATE_DURATION("P1M"), EQUAL_TO, F_DATE_DURATION()), false), //
            EQ_LOCAL_DATE_MISSING(OP(F_LOCAL_DATE("2019-01-02"), EQUAL_TO, F_LOCAL_DATE()), false), //
            EQ_LOCAL_TIME_MISSING(OP(F_LOCAL_TIME("12:00"), EQUAL_TO, F_LOCAL_TIME()), false), //
            EQ_LOCAL_DATE_TIME_MISSING(OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), EQUAL_TO, F_LOCAL_DATE_TIME()), false), //
            NEQ_TIME_DURATION_MISSING(OP(F_TIME_DURATION("P1D"), NOT_EQUAL_TO, F_TIME_DURATION()), true), //
            NEQ_DATE_DURATION_MISSING(OP(F_DATE_DURATION("P1M"), NOT_EQUAL_TO, F_DATE_DURATION()), true), //
            NEQ_LOCAL_DATE_MISSING(OP(F_LOCAL_DATE("2019-01-02"), NOT_EQUAL_TO, F_LOCAL_DATE()), true), //
            NEQ_LOCAL_TIME_MISSING(OP(F_LOCAL_TIME("12:00"), NOT_EQUAL_TO, F_LOCAL_TIME()), true), //
            NEQ_LOCAL_DATE_TIME_MISSING(OP(F_LOCAL_DATE_TIME("2019-01-02T12:00"), NOT_EQUAL_TO, F_LOCAL_DATE_TIME()),
                true), //

            // Time equality with both MISSING values
            EQ_TIME_DURATION_MISSING_MISSING(OP(F_TIME_DURATION(), EQUAL_TO, F_TIME_DURATION()), true), //
            NEQ_TIME_DURATION_MISSING_MISSING(OP(F_TIME_DURATION(), NOT_EQUAL_TO, F_TIME_DURATION()), false), //
            EQ_LOCAL_DATE_MISSING_MISSING(OP(F_LOCAL_DATE(), EQUAL_TO, F_LOCAL_DATE()), true), //
            NEQ_LOCAL_DATE_MISSING_MISSING(OP(F_LOCAL_DATE(), NOT_EQUAL_TO, F_LOCAL_DATE()), false), //
            EQ_LOCAL_TIME_MISSING_MISSING(OP(F_LOCAL_TIME(), EQUAL_TO, F_LOCAL_TIME()), true), //
            NEQ_LOCAL_TIME_MISSING_MISSING(OP(F_LOCAL_TIME(), NOT_EQUAL_TO, F_LOCAL_TIME()), false), //
            EQ_LOCAL_DATE_TIME_MISSING_MISSING(OP(F_LOCAL_DATE_TIME(), EQUAL_TO, F_LOCAL_DATE_TIME()), true), //
            NEQ_LOCAL_DATE_TIME_MISSING_MISSING(OP(F_LOCAL_DATE_TIME(), NOT_EQUAL_TO, F_LOCAL_DATE_TIME()), false), //
            EQ_DATE_DURATION_MISSING_MISSING(OP(F_DATE_DURATION(), EQUAL_TO, F_DATE_DURATION()), true), //
            NEQ_DATE_DURATION_MISSING_MISSING(OP(F_DATE_DURATION(), NOT_EQUAL_TO, F_DATE_DURATION()), false), //
        ;

        private final Ast m_expression;

        private final Consumer<Computer> m_resultChecker;

        private ExecutionTest(final Ast expression) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString());
        }

        private ExecutionTest(final Ast expression, final boolean expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final long expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final double expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final String expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final Duration expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final Period expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final LocalTime expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final LocalDate expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final LocalDateTime expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }

        private ExecutionTest(final Ast expression, final ZonedDateTime expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toString(), expected);
        }
    }

    private static final Function<String, Optional<TestColumn>> FIND_TEST_COLUMN =
        TestUtils.enumFinderAsFunction(TestColumn.values());

    private static final Function<String, Optional<TestFlowVariable>> FIND_TEST_FLOW_VARIABLE =
        TestUtils.enumFinderAsFunction(TestFlowVariable.values());

    private static final BooleanComputerResultSupplier THROWING_BOOL_SUPPLIER = ctx -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static final IntegerComputerResultSupplier THROWING_LONG_SUPPLIER = ctx -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static final FloatComputerResultSupplier THROWING_DOUBLE_SUPPLIER = ctx -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static final ComputerResultSupplier<String> THROWING_STRING_SUPPLIER = ctx -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static enum TestColumn {
            BOOLEAN(ValueType.OPT_BOOLEAN, BooleanComputer.of(ctx -> true, ctx -> false)), //
            INTEGER(ValueType.OPT_INTEGER, IntegerComputer.of(ctx -> 100, ctx -> false)), //
            FLOAT(ValueType.OPT_FLOAT, FloatComputer.of(ctx -> 10.5, ctx -> false)), //
            STRING(ValueType.OPT_STRING, StringComputer.of(ctx -> "column value", ctx -> false)), //
            BOOLEAN_MISSING(ValueType.OPT_BOOLEAN, BooleanComputer.of(THROWING_BOOL_SUPPLIER, ctx -> true)), //
            INTEGER_MISSING(ValueType.OPT_INTEGER, IntegerComputer.of(THROWING_LONG_SUPPLIER, ctx -> true)), //
            FLOAT_MISSING(ValueType.OPT_FLOAT, FloatComputer.of(THROWING_DOUBLE_SUPPLIER, ctx -> true)), //
            STRING_MISSING(ValueType.OPT_STRING, StringComputer.of(THROWING_STRING_SUPPLIER, ctx -> true)), //
            ROW_INDEX(ValueType.INTEGER, IntegerComputer.of(ctx -> 99, ctx -> false)), //
            ROW_ID(ValueType.STRING, StringComputer.of(ctx -> "Row99", ctx -> false)), //
        ;

        private final Computer m_computer;

        private final ValueType m_type;

        private TestColumn(final ValueType type, final Computer computer) {
            m_type = type;
            m_computer = computer;
        }

        Computer computer() {
            return m_computer;
        }

        ValueType type() {
            return m_type;
        }
    }

    private static enum TestFlowVariable {
            BOOLEAN(ValueType.OPT_BOOLEAN, BooleanComputer.of(ctx -> true, ctx -> false)), //
            INTEGER(ValueType.OPT_INTEGER, IntegerComputer.of(ctx -> 100, ctx -> false)), //
            FLOAT(ValueType.OPT_FLOAT, FloatComputer.of(ctx -> 10.5, ctx -> false)), //
            STRING(ValueType.OPT_STRING, StringComputer.of(ctx -> "column value", ctx -> false)), //
            BOOLEAN_MISSING(ValueType.OPT_BOOLEAN, BooleanComputer.of(THROWING_BOOL_SUPPLIER, ctx -> true)), //
            INTEGER_MISSING(ValueType.OPT_INTEGER, IntegerComputer.of(THROWING_LONG_SUPPLIER, ctx -> true)), //
            FLOAT_MISSING(ValueType.OPT_FLOAT, FloatComputer.of(THROWING_DOUBLE_SUPPLIER, ctx -> true)), //
            STRING_MISSING(ValueType.OPT_STRING, StringComputer.of(THROWING_STRING_SUPPLIER, ctx -> true)), //
        ;

        private final Computer m_computer;

        private final ValueType m_type;

        private TestFlowVariable(final ValueType type, final Computer computer) {
            m_type = type;
            m_computer = computer;
        }

        Computer computer() {
            return m_computer;
        }

        ValueType type() {
            return m_type;
        }
    }

    private static enum TestFunctions implements ExpressionFunction {
            PLUS_100_FN(List.of(ValueType.INTEGER), ValueType.INTEGER,
                args -> IntegerComputer.of(ctx -> ((IntegerComputer)args.get("arg0")).compute(ctx) + 100,
                    ctx -> false)), //
            ERRORING_FN(List.of(ValueType.STRING), ValueType.INTEGER, args -> IntegerComputer.of(ctx -> {
                throw new ExpressionEvaluationException(((StringComputer)args.get("arg0")).compute(ctx));
            }, ctx -> false)), //
        ;

        private final ValueType m_returnType;

        private final Function<Arguments<Computer>, Computer> m_apply;

        private final List<Arg> m_signature;

        private TestFunctions(final List<ValueType> args, final ValueType returnType,
            final Function<Arguments<Computer>, Computer> apply) {

            m_apply = apply;
            m_returnType = returnType;

            m_signature = IntStream.range(0, args.size())
                .mapToObj(i -> SignatureUtils.arg("arg" + i, "", SignatureUtils.hasType(args.get(i)))).toList();
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(m_signature, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            return SignatureUtils.checkTypes(m_signature, argTypes).map(r -> m_returnType);
        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            return m_apply.apply(args);
        }

        @Override
        public OperatorDescription description() {
            return fail("Should not be called in evaluation tests");
        }
    }
}
