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
 *   Mar 25, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer.DateDurationComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.aggregations.ColumnAggregation;
import org.knime.core.expressions.functions.ExpressionFunction;
import org.knime.core.expressions.functions.ExpressionFunctionBuilder;

/**
 * Helpers to create {@link Ast}s with as few characters as possible. Only for tests.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin Germany
 */
public final class AstTestUtils {

    private AstTestUtils() {
    }

    /** @return a {@link Ast.MissingConstant} */
    public static final Ast.MissingConstant MIS() { // NOSONAR - name useful for visual clarity
        return Ast.missingConstant();
    }

    /**
     * @param value
     * @return a {@link Ast.BooleanConstant}
     */
    public static Ast.BooleanConstant BOOL(final boolean value) { // NOSONAR - name useful for visual clarity
        return Ast.booleanConstant(value);
    }

    /**
     * @param value
     * @return an {@link Ast.IntegerConstant}
     */
    public static Ast.IntegerConstant INT(final long value) { // NOSONAR - name useful for visual clarity
        return Ast.integerConstant(value);
    }

    /**
     * @param value
     * @return a {@link Ast.FloatConstant}
     */
    public static Ast.FloatConstant FLOAT(final double value) { // NOSONAR - name useful for visual clarity
        return Ast.floatConstant(value);
    }

    /**
     * @param value
     * @return a {@link Ast.StringConstant}
     */
    public static Ast.StringConstant STR(final String value) { // NOSONAR - name useful for visual clarity
        return Ast.stringConstant(value);
    }

    /**
     * @param name
     * @return a {@link Ast.ColumnAccess}
     */
    public static Ast.ColumnAccess COL(final String name) { // NOSONAR - name useful for visual clarity
        return COL(name, 0);
    }

    /**
     * Column access with an offset, accessing an adjacent row.
     *
     * @param name
     * @param offset
     * @return a {@link Ast.ColumnAccess}
     */
    public static Ast.ColumnAccess COL(final String name, final long offset) { // NOSONAR - name useful for visual clarity
        return Ast.columnAccess(name, offset);
    }

    /**
     * @return a {@link Ast.ColumnAccess}
     */
    public static Ast.ColumnAccess ROW_INDEX() { // NOSONAR - name useful for visual clarity
        return Ast.rowIndex();
    }

    /**
     * @return a {@link Ast.ColumnAccess}
     */
    public static Ast.ColumnAccess ROW_ID() { // NOSONAR - name useful for visual clarity
        return Ast.rowId();
    }

    /**
     * @param name
     * @return a {@link Ast.FlowVarAccess}
     */
    public static Ast.FlowVarAccess FLOW(final String name) { // NOSONAR - name useful for visual clarity
        return Ast.flowVarAccess(name);
    }

    /**
     * @param leftArg
     * @param op
     * @param rightArg
     * @return a {@link Ast.BinaryOp}
     */
    public static Ast.BinaryOp OP( // NOSONAR - name useful for visual clarity
        final Ast leftArg, final Ast.BinaryOperator op, final Ast rightArg) {
        return Ast.binaryOp(op, leftArg, rightArg);
    }

    /**
     * @param op
     * @param arg
     * @return an {@link Ast.UnaryOp}
     */
    public static Ast.UnaryOp OP(final Ast.UnaryOperator op, final Ast arg) { // NOSONAR - name useful for visual clarity
        return Ast.unaryOp(op, arg);
    }

    /**
     * @param name
     * @param args
     * @return a {@link Ast.FunctionCall}
     */
    public static Ast.FunctionCall FUN(final ExpressionFunction name, final Ast... args) { // NOSONAR - name useful for visual clarity
        return FUN(name, List.of(args), Map.of());
    }

    /**
     * @param expressionFunction
     * @param positionalArgs
     * @param namedArgs
     * @return a {@link Ast.FunctionCall}
     */
    public static Ast.FunctionCall FUN(final ExpressionFunction expressionFunction, final List<Ast> positionalArgs,
        final Map<String, Ast> namedArgs) { // NOSONAR - name useful for visual clarity
        var args = expressionFunction.signature(positionalArgs, namedArgs)
            .orElseThrow(cause -> new IllegalArgumentException(
                cause + " for function " + expressionFunction.name() + " with arguments " + positionalArgs + " and "
                    + namedArgs + ". But expected " + expressionFunction.description().arguments()));

        return Ast.functionCall(expressionFunction, args);
    }

    /**
     * @param name
     * @param positionalArgs
     * @return a {@link Ast.AggregationCall}
     */
    public static Ast.AggregationCall AGG(final ColumnAggregation name, final ConstantAst... positionalArgs) { // NOSONAR - name useful for visual clarity
        return AGG(name, List.of(positionalArgs), Map.of());
    }

    /**
     * @param columnAggregation
     * @param positionalArgs
     * @param namedArgs
     * @return a {@link Ast.AggregationCall}
     */
    public static Ast.AggregationCall AGG(final ColumnAggregation columnAggregation,
        final List<ConstantAst> positionalArgs, final Map<String, ConstantAst> namedArgs) { // NOSONAR - name useful for visual clarity

        var args = columnAggregation.signature(positionalArgs, namedArgs)
            .orElseThrow(cause -> new IllegalArgumentException(
                cause + " for aggregration " + columnAggregation.name() + " with arguments " + positionalArgs + " and "
                    + namedArgs + ". But expected " + columnAggregation.description().arguments()));

        return Ast.aggregationCall(columnAggregation, args);
    }

    /**
     * Create a function call that produces a {@link ValueType#LOCAL_DATE}, for testing purposes.
     *
     * @param date
     * @return a {@link Ast.FunctionCall} which produces a {@link ValueType#LOCAL_DATE} from the given ISO string
     */
    public static Ast.FunctionCall F_LOCAL_DATE(final String date) {
        return FUN(MAKE_LOCAL_DATE_FOR_TEST, STR(date));
    }

    /**
     * Create a function call that produces a missing {@link ValueType#LOCAL_DATE}, for testing purposes.
     *
     * @return a {@link Ast.FunctionCall} which produces a missing {@link ValueType#LOCAL_DATE}
     */
    public static Ast.FunctionCall F_LOCAL_DATE() {
        return FUN(MAKE_LOCAL_DATE_FOR_TEST);
    }

    /**
     * Create a function call that produces a {@link ValueType#LOCAL_TIME}, for testing purposes.
     *
     * @param time
     * @return a {@link Ast.FunctionCall} which produces a {@link ValueType#LOCAL_TIME} from the given ISO string
     */
    public static Ast.FunctionCall F_LOCAL_TIME(final String time) {
        return FUN(MAKE_LOCAL_TIME_FOR_TEST, STR(time));
    }

    /**
     * Create a function call that produces a missing {@link ValueType#LOCAL_TIME}, for testing purposes.
     *
     * @return a {@link Ast.FunctionCall} which produces a missing {@link ValueType#LOCAL_TIME}
     */
    public static Ast.FunctionCall F_LOCAL_TIME() {
        return FUN(MAKE_LOCAL_TIME_FOR_TEST);
    }

    /**
     * Create a function call that produces a {@link ValueType#LOCAL_DATE_TIME}, for testing purposes.
     *
     * @param datetime
     * @return a {@link Ast.FunctionCall} which produces a {@link ValueType#LOCAL_DATE_TIME} from the given ISO string
     */
    public static Ast.FunctionCall F_LOCAL_DATE_TIME(final String datetime) {
        return FUN(MAKE_LOCAL_DATE_TIME_FOR_TEST, STR(datetime));
    }

    /**
     * Create a function call that produces a missing {@link ValueType#LOCAL_DATE_TIME}, for testing purposes.
     *
     * @return a {@link Ast.FunctionCall} which produces a missing {@link ValueType#LOCAL_DATE_TIME}
     */
    public static Ast.FunctionCall F_LOCAL_DATE_TIME() {
        return FUN(MAKE_LOCAL_DATE_TIME_FOR_TEST);
    }

    /**
     * Create a function call that produces a {@link ValueType#ZONED_DATE_TIME}, for testing purposes.
     *
     * @param datetime
     * @return a {@link Ast.FunctionCall} which produces a {@link ValueType#ZONED_DATE_TIME} from the given ISO string
     */
    public static Ast.FunctionCall F_ZONED_DATE_TIME(final String datetime) {
        return FUN(MAKE_ZONED_DATE_TIME_FOR_TEST, STR(datetime));
    }

    /**
     * Create a function call that produces a missing {@link ValueType#ZONED_DATE_TIME}, for testing purposes.
     *
     * @return a {@link Ast.FunctionCall} which produces a missing {@link ValueType#ZONED_DATE_TIME}
     */
    public static Ast.FunctionCall F_ZONED_DATE_TIME() {
        return FUN(MAKE_ZONED_DATE_TIME_FOR_TEST);
    }

    /**
     * Create a function call that produces a {@link ValueType#TIME_DURATION}, for testing purposes.
     *
     * @param duration
     * @return a {@link Ast.FunctionCall} which produces a {@link ValueType#TIME_DURATION} from the given ISO string
     */
    public static Ast.FunctionCall F_TIME_DURATION(final String duration) {
        return FUN(MAKE_TIME_DURATION_FOR_TEST, STR(duration));
    }

    /**
     * Create a function call that produces a missing{@link ValueType#TIME_DURATION}, for testing purposes.
     *
     * @return a {@link Ast.FunctionCall} which produces a missing {@link ValueType#TIME_DURATION}
     */
    public static Ast.FunctionCall F_TIME_DURATION() {
        return FUN(MAKE_TIME_DURATION_FOR_TEST);
    }

    /**
     * Create a function call that produces a {@link ValueType#DATE_DURATION}, for testing purposes.
     *
     * @param period
     * @return a {@link Ast.FunctionCall} which produces a {@link ValueType#DATE_DURATION} from the given ISO string
     */
    public static Ast.FunctionCall F_DATE_DURATION(final String period) {
        return FUN(MAKE_DATE_DURATION_FOR_TEST, STR(period));
    }

    /**
     * Create a function call that produces a missing {@link ValueType#LOCAL_DATE}, for testing purposes.
     *
     * @return a {@link Ast.FunctionCall} which produces a missing {@link ValueType#LOCAL_DATE}
     */
    public static Ast.FunctionCall F_DATE_DURATION() {
        return FUN(MAKE_DATE_DURATION_FOR_TEST);
    }

    private static final OperatorCategory TEST_CATEGORY = new OperatorCategory("Test", "Test");

    private static final ExpressionFunction MAKE_LOCAL_DATE_FOR_TEST = ExpressionFunctionBuilder.functionBuilder() //
        .name("make_local_date_for_test") //
        .description("") //
        .examples("") //
        .keywords("") //
        .category(TEST_CATEGORY) //
        .args(SignatureUtils.optarg("date", "", SignatureUtils.isStringOrOpt())) //
        .returnType("", "", args -> LOCAL_DATE(args.getNumberOfArguments() == 0)) //
        .impl(args -> LocalDateComputer.of(ctx -> LocalDate.parse(((StringComputer)args.get("date")).compute(ctx)),
            ctx -> !args.has("date"))) //
        .build();

    private static final ExpressionFunction MAKE_LOCAL_TIME_FOR_TEST = ExpressionFunctionBuilder.functionBuilder() //
        .name("make_local_time_for_test") //
        .description("") //
        .examples("") //
        .keywords("") //
        .category(TEST_CATEGORY) //
        .args(SignatureUtils.optarg("time", "", SignatureUtils.isStringOrOpt())) //
        .returnType("", "", args -> LOCAL_TIME(args.getNumberOfArguments() == 0)) //
        .impl(args -> LocalTimeComputer.of(ctx -> LocalTime.parse(((StringComputer)args.get("time")).compute(ctx)),
            ctx -> !args.has("time"))) //
        .build();

    private static final ExpressionFunction MAKE_LOCAL_DATE_TIME_FOR_TEST = ExpressionFunctionBuilder.functionBuilder() //
        .name("make_local_date_time_for_test") //
        .description("") //
        .examples("") //
        .keywords("") //
        .category(TEST_CATEGORY) //
        .args(SignatureUtils.optarg("datetime", "", SignatureUtils.isStringOrOpt())) //
        .returnType("", "", args -> LOCAL_DATE_TIME(args.getNumberOfArguments() == 0)) //
        .impl(args -> LocalDateTimeComputer.of(
            ctx -> LocalDateTime.parse(((StringComputer)args.get("datetime")).compute(ctx)),
            ctx -> !args.has("datetime"))) //
        .build();

    private static final ExpressionFunction MAKE_ZONED_DATE_TIME_FOR_TEST = ExpressionFunctionBuilder.functionBuilder() //
        .name("make_zoned_date_time_for_test") //
        .description("") //
        .examples("") //
        .keywords("") //
        .category(TEST_CATEGORY) //
        .args(SignatureUtils.optarg("datetime", "", SignatureUtils.isStringOrOpt())) //
        .returnType("", "", args -> ZONED_DATE_TIME(args.getNumberOfArguments() == 0)) //
        .impl(args -> ZonedDateTimeComputer.of(
            ctx -> ZonedDateTime.parse(((StringComputer)args.get("datetime")).compute(ctx)),
            ctx -> !args.has("datetime"))) //
        .build();

    private static final ExpressionFunction MAKE_TIME_DURATION_FOR_TEST = ExpressionFunctionBuilder.functionBuilder() //
        .name("make_time_duration_for_test") //
        .description("") //
        .examples("") //
        .keywords("") //
        .category(TEST_CATEGORY) //
        .args(SignatureUtils.optarg("str", "", SignatureUtils.isStringOrOpt())) //
        .returnType("", "", args -> TIME_DURATION(args.getNumberOfArguments() == 0)) //
        .impl(args -> TimeDurationComputer.of(ctx -> Duration.parse(((StringComputer)args.get("str")).compute(ctx)),
            ctx -> !args.has("str"))) //
        .build();

    private static final ExpressionFunction MAKE_DATE_DURATION_FOR_TEST = ExpressionFunctionBuilder.functionBuilder() //
        .name("make_date_duration_for_test") //
        .description("") //
        .examples("") //
        .keywords("") //
        .category(TEST_CATEGORY) //
        .args(SignatureUtils.optarg("str", "", SignatureUtils.isStringOrOpt())) //
        .returnType("", "", args -> DATE_DURATION(args.getNumberOfArguments() == 0)) //
        .impl(args -> DateDurationComputer.of(ctx -> Period.parse(((StringComputer)args.get("str")).compute(ctx)),
            ctx -> !args.has("str"))) //
        .build();
}
