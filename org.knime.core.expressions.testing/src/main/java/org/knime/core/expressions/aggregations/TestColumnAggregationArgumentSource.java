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
 *   May 27, 2024 (benjamin): created
 */
package org.knime.core.expressions.aggregations;

import static org.junit.jupiter.api.Assertions.fail;
import static org.knime.core.expressions.aggregations.ArgumentsBuilder.args;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;

/**
 * Provides arguments for testing that a consumer can provide a Computer for each built-in aggregation.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public class TestColumnAggregationArgumentSource implements ArgumentsProvider {

    /** Test columns used in the testing aggregations */
    public static final Map<String, ValueType> TEST_COLUMNS =
        Map.of("INT_COL", ValueType.INTEGER, "FLOAT_COL", ValueType.FLOAT, "STRING_COL", ValueType.STRING);

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
        return BuiltInAggregations.BUILT_IN_AGGREGATIONS.stream() //
            .map(TestColumnAggregationArgumentSource::getTestAstFor) //
            .map(Arguments::of);
    }

    private static Ast.AggregationCall getTestAstFor(final ColumnAggregation agg) {
        var args = getTestArgsFor(agg);
        var ast = Ast.aggregationCall(agg.name(), args);
        try {
            Expressions.inferTypes(ast, n -> ReturnResult.fromNullable(TEST_COLUMNS.get(n), "Column does not exist"),
                n -> ReturnResult.failure("There are no flow variables"));
        } catch (ExpressionCompileException ex) {
            fail("Failed to infer types for " + ast, ex); // NOSONAR - the method cannot throw to be usable in map
        }
        return ast;
    }

    private static org.knime.core.expressions.Arguments<Ast.ConstantAst> getTestArgsFor(final ColumnAggregation call) {
        if (BuiltInAggregations.MAX.equals(call)) {
            return args().p(Ast.stringConstant("INT_COL")).build();
        } else if (BuiltInAggregations.MIN.equals(call)) {
            return args().p(Ast.stringConstant("FLOAT_COL")).build();
        } else if (BuiltInAggregations.SUM.equals(call)) {
            return args().p(Ast.stringConstant("INT_COL")).build();
        } else if (BuiltInAggregations.MEAN.equals(call)) {
            return args().p(Ast.stringConstant("FLOAT_COL")).build();
        } else if (BuiltInAggregations.COUNT.equals(call)) {
            return args().p(Ast.stringConstant("STRING_COL")).n("ignore_missing", Ast.booleanConstant(true)).build();
        } else if (BuiltInAggregations.MEDIAN.equals(call)) {
            return args().p(Ast.stringConstant("INT_COL")).build();
        } else if (BuiltInAggregations.STD_DEV.equals(call)) {
            return args().p(Ast.stringConstant("FLOAT_COL")).build();
        } else if (BuiltInAggregations.VARIANCE.equals(call)) {
            return args().p(Ast.stringConstant("FLOAT_COL")).build();
        }
        return fail("No test arguments for aggregation " + call.name());
    }
}
