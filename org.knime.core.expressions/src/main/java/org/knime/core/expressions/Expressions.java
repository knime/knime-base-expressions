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

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.parser.ExpressionGrammar;
import org.knime.core.expressions.parser.ExpressionGrammar.Expr;
import org.rekex.parser.ParseResult;
import org.rekex.parser.PegParser;

/**
 * Utilities for working with expressions in the KNIME Expression Language.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class Expressions {

    private Expressions() {
    }

    /**
     * Parse the given expression to an {@link Ast abstract syntax tree}.
     *
     * @param expression the expression in the KNIME Expression Language.
     * @return the {@link Ast abstract syntax tree}
     * @throws SyntaxError if the input is not a syntactically valid expression
     */
    public static Ast parse(final String expression) throws SyntaxError {
        final PegParser<Expr> parser = ExpressionGrammar.parser();
        final ParseResult<Expr> result = parser.parse(expression);
        if (result instanceof ParseResult.Full<Expr> full) {
            return full.value().ast();
        } else {
            throw new SyntaxError("parse error:\n" + result);
        }
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
     * @throws MissingColumnError if the expression accesses a column that is not available
     */
    public static void resolveColumnIndices(final Ast expression, final Function<String, OptionalInt> columnNameToIdx)
        throws MissingColumnError {
        ColumnIdxResolve.resolveColumnIndices(expression, columnNameToIdx);
    }

    /**
     * Infer the type of the given expression. Adds the type as {@link Ast#data()} to each node of the syntax tree.
     *
     * @param expression the expression
     * @param columnToType a function that returns the type of a columns accessed by the expression. The function should
     *            return <code>Optional.empty()</code> if the column is not available.
     * @return the output type of the full expression
     * @throws MissingColumnError if the expression accesses a column that is not available
     * @throws TypingError if type inference failed because operations are used for incompatible types
     */
    public static ValueType inferTypes(final Ast expression,
        final Function<ColumnAccess, Optional<ValueType>> columnToType)
        throws MissingColumnError, TypingError {
        return Typing.inferTypes(expression, columnToType);
    }

    /**
     * Get the inferred output type of the given expression.
     *
     * @param expression the expression with present type information from {@link #inferTypes(Ast, Function)}
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

    // EXCEPTIONS

    /** Base class for exceptions that happen when working with expressions. */
    public abstract static sealed class ExpressionError extends Exception {

        private static final long serialVersionUID = 1L;

        ExpressionError(final String message) {
            super(message);
        }
    }

    /** Exception thrown when an expression could not be parsed. */
    public static final class SyntaxError extends ExpressionError {

        private static final long serialVersionUID = 1L;

        SyntaxError(final String message) {
            super(message);
        }
    }

    /** Exception thrown when an expression refers a column that is not available. */
    public static final class MissingColumnError extends ExpressionError {

        private static final long serialVersionUID = 1L;

        MissingColumnError(final String columnName) {
            super("The column '" + columnName + "' is not available");
        }
    }

    /**
     * Exception thrown when type inference of an expression fails (e.g. the expression is invalid because operations
     * are used on incompatible types).
     */
    public static final class TypingError extends ExpressionError {

        private static final long serialVersionUID = 1L;

        TypingError(final String message) {
            super(message);
        }
    }
}
