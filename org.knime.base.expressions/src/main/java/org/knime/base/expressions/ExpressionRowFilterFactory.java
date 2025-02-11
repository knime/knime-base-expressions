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
 *   Feb 11, 2025 (benjamin): created
 */
package org.knime.base.expressions;

import java.util.function.BooleanSupplier;

import org.knime.base.expressions.ColumnInputUtils.RequiredColumns;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionCompileException;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.ValueType;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.virtual.spec.RowFilterTransformSpec.RowFilterFactory;

/**
 * Filters the input virtual table based on the result of the expression.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public class ExpressionRowFilterFactory implements RowFilterFactory {

    private final Ast m_ast;

    private final ValueSchema m_inputTableSchema;

    private final ExpressionAdditionalInputs m_additionalInputs;

    private final EvaluationContext m_ctx;

    private final RequiredColumns m_requiredColumns;

    private static void checkAstOutputType(final Ast ast) {
        var outputType = Expressions.getInferredType(ast);
        if (!ValueType.BOOLEAN.equals(outputType)) {
            throw new IllegalArgumentException(
                "The expression must evaluate to BOOLEAN. Got " + outputType.name() + ".");
        }
    }

    /**
     * Creates a new instance.
     *
     * @param ast the expression. Must have {@link Expressions#inferTypes inferred types}.
     * @param inputTableSchema
     * @param additionalInputs
     * @param ctx
     */
    public ExpressionRowFilterFactory(final Ast ast, final ValueSchema inputTableSchema,
        final ExpressionAdditionalInputs additionalInputs, final EvaluationContext ctx) {
        checkAstOutputType(ast);

        m_ast = ast;
        m_inputTableSchema = inputTableSchema;
        m_additionalInputs = additionalInputs;
        m_ctx = ctx;

        m_requiredColumns = RequiredColumns.of(ast);
    }

    int[] getInputColumnIndices() {
        return m_requiredColumns.columnIndices();
    }

    @Override
    public BooleanSupplier createRowFilter(final ReadAccess[] inputs) {
        BooleanComputer outputComputer;
        try {
            outputComputer = (BooleanComputer)Expressions.evaluate( //
                m_ast, //
                ColumnInputUtils.createColumnToComputerFn(m_inputTableSchema, m_requiredColumns, inputs), //
                m_additionalInputs::flowVariableToComputer, //
                m_additionalInputs::aggregationToComputer //
            );
        } catch (ExpressionCompileException ex) {
            // NB: We never use Optional.empty() for the column computer.
            throw new IllegalStateException(ex);
        }

        return () -> {
            try {
                return outputComputer.compute(m_ctx);
            } catch (ExpressionEvaluationException e) {
                throw new ExpressionEvaluationRuntimeException(e);
            }
        };
    }

}
