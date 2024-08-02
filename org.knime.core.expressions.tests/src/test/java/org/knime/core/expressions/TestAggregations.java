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
 *   May 23, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.aggregations.ColumnAggregation;

/**
 * Test aggregations for the expression engine.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
enum TestAggregations implements ColumnAggregation {
        /** Requires column name as arg and return 42 with the same type (must be numeric) */
        RETURN_42_WITH_COL_TYPE( //
            (args, columnType) -> {
                if (args.get("column") instanceof Ast.StringConstant colName) {
                    return columnType.apply(colName.value()).filter(ValueType::isNumericOrOpt,
                        "RETURN_42_WITH_COL_TYPE not numeric");
                }

                return ReturnResult.failure("Invalid arguments to aggregation RETURN_42_WITH_COL_TYPE");

            }, //
            call -> {
                if (ValueType.INTEGER.equals(Typing.getType(call).baseType())) {
                    return Optional.of(IntegerComputer.of(ctx -> 42, ctx -> false));
                } else if (ValueType.FLOAT.equals(Typing.getType(call).baseType())) {
                    return Optional.of(FloatComputer.of(ctx -> 42.0, ctx -> false));
                } else {
                    return Optional.empty();
                }
            }, //
            List.of(SignatureUtils.arg("column", "", SignatureUtils.isAnything())) //
        ), //
        /** Test function to test named args. Returns MISSING. */
        EXPECT_NAMED_ARG( //
            (args, columnType) -> ReturnResult.success(ValueType.MISSING), //
            call -> {
                return Optional.of(ctx -> true);
            }, //
            List.of(SignatureUtils.arg("named_arg_id", "", SignatureUtils.isFloat())) //
        ), //
        /** Exists only to check that error message for unknown aggregation suggests similar aggregation names */
        RETURN_42_WITH_COL_TXXX((args, columnType) -> ReturnResult.failure("some error"), call -> Optional.empty(),
            List.of());

    public static final Map<String, ColumnAggregation> TEST_AGGREGATIONS =
        TestUtils.enumFinderAsMap(TestAggregations.values(), ColumnAggregation.class);

    public static final Function<AggregationCall, Optional<Computer>> TEST_AGGREGATIONS_COMPUTER = agg -> TestUtils
        .enumFinderAsFunction(TestAggregations.values()).apply(agg.aggregation().name()).flatMap(t -> t.computer(agg));

    private final BiFunction<Arguments<ConstantAst>, Function<String, ReturnResult<ValueType>>, ReturnResult<ValueType>> m_returnType;

    private final Function<AggregationCall, Optional<Computer>> m_computer;

    private final List<Arg> m_arguments;

    private TestAggregations(
        final BiFunction<Arguments<ConstantAst>, Function<String, ReturnResult<ValueType>>, ReturnResult<ValueType>> returnType,
        final Function<AggregationCall, Optional<Computer>> computer, final List<Arg> arguments) {
        m_returnType = returnType;
        m_computer = computer;
        m_arguments = arguments;
    }

    @Override
    public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
        final Map<String, T> namedArguments) {
        return SignatureUtils.matchSignature(m_arguments, positionalArguments, namedArguments);
    }

    @Override
    public ReturnResult<ValueType> returnType(final Arguments<ConstantAst> arguments,
        final Function<String, ReturnResult<ValueType>> columnType) {
        return m_returnType.apply(arguments, columnType);
    }

    public Optional<Computer> computer(final AggregationCall call) {
        return m_computer.apply(call);
    }

    @Override
    public OperatorDescription description() {
        return new OperatorDescription(this.name(), "Test aggregation", Arg.toOperatorDescription(m_arguments),
            "Some return type", "Some return description", List.of(), "Test category",
            OperatorDescription.FUNCTION_ENTRY_TYPE);
    }
}