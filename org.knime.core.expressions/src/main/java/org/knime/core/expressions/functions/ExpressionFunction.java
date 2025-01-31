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
 *   Apr 4, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.AstContext;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.NamedExpressionOperator;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;

/**
 * The definition and implementation of a function that can be used in expressions.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public interface ExpressionFunction extends NamedExpressionOperator {

    /**
     * @return the identifier of the function. Should be in
     *         <a href="https://en.wikipedia.org/wiki/Snake_case">snake_case</a>.
     */
    @Override
    String name();

    /**
     * @return a description of the function
     */
    @Override
    OperatorDescription description();

    /**
     * Infer the return type of the argument types.
     *
     * @param argTypes the types of the input arguments
     * @return the return type or an error message if the function is not applicable to the arguments
     */
    ReturnResult<ValueType> returnType(Arguments<ValueType> argTypes);

    /**
     * Apply the function on the given arguments. Note that the arguments are guaranteed to be the appropriate computers
     * for one of the allowed argument types. Must return a computer that fits the {@link #returnType(Arguments)} for
     * these arguments. The <code>compute</code> and <code>isMissing</code> methods of the arguments must only be called
     * from the resulting computer and must not be called more than once.
     *
     * @param args the arguments
     * @return a computer that applies the function to the input arguments
     */
    Computer apply(Arguments<Computer> args, AstContext context);
}
