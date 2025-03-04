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
 *   Created on Feb 5, 2024 by benjamin
 */
package org.knime.core.expressions;

import static org.knime.core.expressions.Ast.ColumnId.ColumnIdType.ROW_INDEX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.FlowVarAccess;

/**
 * Utilities for working with expressions in the KNIME Expression Language.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class Expressions {

    /**
     * The current major version number of the KNIME Expression Language. This constant is used to track the versioning
     * of the expression language syntax and functionality. It should be incremented when there are changes that break
     * backward compatibility. Such changes include modifications that alter the execution behavior of existing
     * expressions or the introduction of new syntax that is not backward compatible.
     */
    public static final int LANGUAGE_VERSION = 1;

    private Expressions() {
    }

    /**
     * Parse the given expression to an {@link Ast abstract syntax tree}.
     *
     * @param expression the expression in the KNIME Expression Language.
     * @return the {@link Ast abstract syntax tree}
     * @throws ExpressionCompileException if the input is not a syntactically valid expression
     */
    public static Ast parse(final String expression) throws ExpressionCompileException {
        return Parser.parse(expression);
    }

    // TODO(AP-22024) remove this method from the API
    // We only use this to get the appropriate ReadAccess in Exec.
    // The caller has to provide a function which maps from the column index to the ReadAccess. However, the
    // caller could map directly from the name to the ReadAccess (maybe using another name to index mapper
    // that has to exist somewhere).
    /**
     * Resolve column indices for the given expression. Adds the column index as {@link Ast#data()} to
     * {@link ColumnAccess} nodes.
     *
     * @param expression the expression
     * @param columnNameToIdx a function that returns the index of a column accessed by the expression. The function
     *            should return <code>Optional.empty()</code> if the column is not available.
     * @throws ExpressionCompileException if the expression accesses a column that is not available
     */
    public static void resolveColumnIndices(final Ast expression,
        final Function<Ast.ColumnAccess, OptionalInt> columnNameToIdx) throws ExpressionCompileException {

        ColumnIdxResolve.resolveColumnAccessIndices(expression, columnNameToIdx);
    }

    /**
     * Infer the type of the given expression and resolve function calls. Adds the type, and function as
     * {@link Ast#data()} to each node of the syntax tree.
     *
     * @param expression the expression
     * @param columnToType a function that returns the type of a column accessed by the expression. The function should
     *            return <code>ReturnResult.failure()</code> if the column is not available or has an unsupported type.
     * @param flowVarType a function that returns the type of a flow variable accessed by the expression. The function
     *            should return <code>ReturnResult.failure()</code> if the flow variable is not available or has an
     *            unsupported type.
     * @return the output type of the full expression
     * @throws ExpressionCompileException if type inference failed because operations are used for incompatible types or
     *             a column is not available
     */
    public static ValueType inferTypes( //
        final Ast expression, //
        final Function<String, ReturnResult<ValueType>> columnToType, //
        final Function<String, ReturnResult<ValueType>> flowVarType //
    ) throws ExpressionCompileException {
        return Typing.inferTypes(expression, columnToType, flowVarType);
    }

    /**
     * Create a {@link Computer} that evaluates the given expression. The resulting {@link Computer} does not cache the
     * result but evaluates it on each access. The caller has to provide the input data for each used
     * {@link ColumnAccess} via a {@link Computer} of the appropriate type.
     *
     * @param expression the expression. Must include type information inferred by {@link #inferTypes}.
     * @param columnToComputer a function that returns the computer for column data accessed by the expression. The
     *            function should return <code>Optional.empty()</code> if the column is not available.
     * @param flowVariableToComputer a function that returns the computer for flow variable accessed by the expression.
     *            The function should return <code>Optional.empty()</code> if the flow variable is not available.
     * @param aggregationToComputer a function that returns the computer for an aggregation call
     * @return the output type of the full expression
     * @throws ExpressionCompileException if the expression accesses a column that is not available
     */
    public static Computer evaluate(final Ast expression,
        final Function<ColumnAccess, Optional<Computer>> columnToComputer,
        final Function<FlowVarAccess, Optional<Computer>> flowVariableToComputer,
        final Function<AggregationCall, Optional<Computer>> aggregationToComputer) throws ExpressionCompileException {
        return Evaluation.evaluate(expression, columnToComputer, flowVariableToComputer, aggregationToComputer);
    }

    /**
     * Get the inferred output type of the given expression.
     *
     * @param expression the expression with present type information from {@link #inferTypes}
     * @return the output type
     * @throws IllegalArgumentException if the expression is not typed
     */
    public static ValueType getInferredType(final Ast expression) {
        return Typing.getType(expression);
    }

    /**
     * Get the resolved column index of the given ColumnAccess expression.
     *
     * @param columnAccess the column access with present index information from
     *            {@link #resolveColumnIndices(Ast, Function)}
     * @return the column index
     */
    public static int getResolvedColumnIdx(final ColumnAccess columnAccess) {
        return ColumnIdxResolve.getColumnIdx(columnAccess);
    }

    /**
     * Returns {@code true} if the given {@code expression} uses the ROW_INDEX column.
     *
     * @param expression the expression to check
     * @return {@code true} if {@code expression} uses the ROW_INDEX column.
     */
    public static boolean requiresRowIndexColumn(final Ast expression) {
        return expression.accept( //
            new AstVisitors.ReducingAstVisitor<Boolean, RuntimeException>(false, Boolean::logicalOr) {
                @Override
                public Boolean visit(final Ast.ColumnAccess node) {
                    return node.columnId().type() == ROW_INDEX;
                }
            });
    }

    /**
     * Collect all {@code ColumnAccess} nodes in the given {@code expression}
     *
     * @param expression the expression to process
     * @return list of all all {@code ColumnAccess} nodes in the given {@code expression}
     */
    public static List<Ast.ColumnAccess> collectColumnAccesses(final Ast expression) {
        // TODO (TP) This should be rewritten to collect-style operation instead of reduce, once the AstVisitor API has been sorted out
        // TODO (TP) The above requiresRowIndexColumn() could use this and check whether the returned set contains(Ast.rowIndex())
        return expression.accept( //
            new AstVisitors.ReducingAstVisitor<List<Ast.ColumnAccess>, RuntimeException>(Collections.emptyList(),
                (s1, s2) -> {
                    if (s1.isEmpty()) {
                        return s2;
                    } else if (s2.isEmpty()) {
                        return s1;
                    } else {
                        var s = new ArrayList<>(s1);
                        s.addAll(s2);
                        return s;
                    }
                }) {
                @Override
                public List<Ast.ColumnAccess> visit(final Ast.ColumnAccess node) {
                    return Collections.singletonList(node);
                }
            });
    }

    /**
     * @param node an {@link Ast} node that was parsed by {@link #parse}
     * @return the {@link TextRange} of the code that was parsed to this node, or <code>null</code> if there is no text
     *         location associated
     */
    public static TextRange getTextLocation(final Ast node) {
        return Parser.getTextLocation(node);
    }
}
