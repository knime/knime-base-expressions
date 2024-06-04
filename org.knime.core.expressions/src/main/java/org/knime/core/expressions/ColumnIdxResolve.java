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
 *   Feb 15, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Expressions.ExpressionCompileException;

/**
 * Utilities for mapping column names to column indices in an Expression {@link Ast}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class ColumnIdxResolve {

    private static final String COLUMN_IDX_DATA_KEY = "colIdx";

    private ColumnIdxResolve() {
    }

    /**
     * Resolve column indices for the given Expression {@link Ast}. Adds the field "colIdx" to all
     * {@link Ast.ColumnAccess} nodes.
     *
     * @param columnNameToIdx map a column name to the index. Should return {@link OptionalInt#empty()} for column names
     *            that do not exist in the table.
     */
    static void resolveColumnIndices(final Ast root, final Function<Ast.ColumnId, OptionalInt> columnNameToIdx)
        throws ExpressionCompileException {
        Ast.putDataRecursive(root, COLUMN_IDX_DATA_KEY, new ColumnIdxVisitor(columnNameToIdx));
    }

    static int getColumnIdx(final ColumnAccess node) {
        return (Integer)node.data(COLUMN_IDX_DATA_KEY);
    }

    private static final class ColumnIdxVisitor extends Ast.OptionalAstVisitor<Integer, ExpressionCompileException> {

        private final Function<Ast.ColumnId, OptionalInt> m_colIdx;

        public ColumnIdxVisitor(final Function<Ast.ColumnId, OptionalInt> colIdx) {
            m_colIdx = colIdx;
        }

        @Override
        public Optional<Integer> visit(final ColumnAccess node) throws ExpressionCompileException {
            return Optional.of(m_colIdx.apply(node.columnId())
                .orElseThrow(() -> new ExpressionCompileException(ExpressionCompileError.missingColumnError(node))));
        }
    }
}
