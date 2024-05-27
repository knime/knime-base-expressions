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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast;

/**
 * A builder for creating arguments for aggregations. This builder simplifies the process of defining positional and
 * named arguments.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ArgumentsBuilder {

    private ArgumentsBuilder() {
    }

    /** Interface for the second builder stage */
    public interface RequiresNamedArgs {

        /**
         * Adds a named argument to the arguments list.
         *
         * @param name
         * @param argument
         * @return the next stage of the builder
         */
        RequiresNamedArgs n(String name, Ast.ConstantAst argument);

        /**
         * Builds the arguments.
         *
         * @return the arguments
         */
        Arguments<Ast.ConstantAst> build();
    }

    /** Interface for the first builder stage */
    public interface RequiresPositionalArgs extends RequiresNamedArgs {

        /**
         * Adds a positional argument to the arguments list.
         *
         * @param argument
         * @return the next stage of the builder
         */
        RequiresPositionalArgs p(Ast.ConstantAst argument);
    }

    /**
     * @return a new builder for creating arguments
     */
    public static RequiresPositionalArgs args() {
        return new ArgsBuilderImpl();
    }

    // NOTE: The interfaces define the stages
    private static class ArgsBuilderImpl implements RequiresPositionalArgs {

        private final List<Ast.ConstantAst> m_positionalArguments = new ArrayList<>();

        private final Map<String, Ast.ConstantAst> m_namedArguments = new HashMap<>();

        @Override
        public RequiresPositionalArgs p(final Ast.ConstantAst argument) {
            m_positionalArguments.add(argument);
            return this;
        }

        @Override
        public RequiresNamedArgs n(final String name, final Ast.ConstantAst argument) {
            m_namedArguments.put(name, argument);
            return this;
        }

        @Override
        public Arguments<Ast.ConstantAst> build() {
            return new Arguments<>(m_positionalArguments, m_namedArguments);
        }
    }
}
