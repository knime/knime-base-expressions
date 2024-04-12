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

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static org.knime.core.expressions.Ast.UnaryOperator.NOT;
import static org.knime.core.expressions.AstTestUtils.BOOL;
import static org.knime.core.expressions.AstTestUtils.COL;
import static org.knime.core.expressions.AstTestUtils.FLOAT;
import static org.knime.core.expressions.AstTestUtils.FUN;
import static org.knime.core.expressions.AstTestUtils.INT;
import static org.knime.core.expressions.AstTestUtils.MIS;
import static org.knime.core.expressions.AstTestUtils.OP;
import static org.knime.core.expressions.AstTestUtils.STR;
import static org.knime.core.expressions.TestUtils.computerResultChecker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
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
        Typing.inferTypes(ast, FIND_TEST_COLUMN.andThen(c -> c.map(TestColumn::type)), TEST_FUNCTIONS);
        var result = Evaluation.evaluate(ast, FIND_TEST_COLUMN.andThen(c -> c.map(TestColumn::computer)));
        assertNotNull(result, "should output result");
        params.m_resultChecker.accept(result);
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

            // === Function calls

            FN_PLUS_100(FUN("PLUS_100_FN", INT(10)), 110), //
        ;

        private final Ast m_expression;

        private final Consumer<Computer> m_resultChecker;

        private ExecutionTest(final Ast expression) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toExpression());
        }

        private ExecutionTest(final Ast expression, final boolean expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toExpression(), expected);
        }

        private ExecutionTest(final Ast expression, final long expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toExpression(), expected);
        }

        private ExecutionTest(final Ast expression, final double expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toExpression(), expected);
        }

        private ExecutionTest(final Ast expression, final String expected) {
            m_expression = expression;
            m_resultChecker = computerResultChecker(expression.toExpression(), expected);
        }
    }

    private static final Function<ColumnAccess, Optional<TestColumn>> FIND_TEST_COLUMN =
        colAccess -> Arrays.stream(TestColumn.values()).filter(t -> t.name().equals(colAccess.name())).findFirst();

    private static final BooleanSupplier THROWING_BOOL_SUPPLIER = () -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static final LongSupplier THROWING_LONG_SUPPLIER = () -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static final DoubleSupplier THROWING_DOUBLE_SUPPLIER = () -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static final Supplier<String> THROWING_STRING_SUPPLIER = () -> {
        throw new AssertionError("should not call compute on missing values");
    };

    private static enum TestColumn {
            BOOLEAN(ValueType.OPT_BOOLEAN, BooleanComputer.of(() -> true, () -> false)), //
            INTEGER(ValueType.OPT_INTEGER, IntegerComputer.of(() -> 100, () -> false)), //
            FLOAT(ValueType.OPT_FLOAT, FloatComputer.of(() -> 10.5, () -> false)), //
            STRING(ValueType.OPT_STRING, StringComputer.of(() -> "column value", () -> false)), //
            BOOLEAN_MISSING(ValueType.OPT_BOOLEAN, BooleanComputer.of(THROWING_BOOL_SUPPLIER, () -> true)), //
            INTEGER_MISSING(ValueType.OPT_INTEGER, IntegerComputer.of(THROWING_LONG_SUPPLIER, () -> true)), //
            FLOAT_MISSING(ValueType.OPT_FLOAT, FloatComputer.of(THROWING_DOUBLE_SUPPLIER, () -> true)), //
            STRING_MISSING(ValueType.OPT_STRING, StringComputer.of(THROWING_STRING_SUPPLIER, () -> true)), //
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

    private static final Function<String, Optional<ExpressionFunction>> TEST_FUNCTIONS =
        TestUtils.functionsMappingFromArray(TestFunctions.values());

    private static enum TestFunctions implements ExpressionFunction {
            PLUS_100_FN(List.of(ValueType.INTEGER), ValueType.INTEGER,
                (c) -> IntegerComputer.of(() -> ((IntegerComputer)c.get(0)).compute() + 100, () -> false)), //
        ;

        private final Map<List<ValueType>, ValueType> m_argsToOutputs;

        private final Function<List<Computer>, Computer> m_apply;

        private TestFunctions(final List<ValueType> args, final ValueType output,
            final Function<List<Computer>, Computer> apply) {
            this(Map.of(args, output), apply);
        }

        private TestFunctions(final Map<List<ValueType>, ValueType> argsToOutput,
            final Function<List<Computer>, Computer> apply) {
            m_argsToOutputs = argsToOutput;
            m_apply = apply;
        }

        @Override
        public Optional<ValueType> returnType(final List<ValueType> argTypes) {
            return Optional.ofNullable(m_argsToOutputs.get(argTypes));
        }

        @Override
        public Computer apply(final List<Computer> args) {
            return m_apply.apply(args);
        }

        @Override
        public Description description() {
            throw new IllegalStateException("Should not be called during function evaluation");
        }
    }
}
