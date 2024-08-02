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
package org.knime.core.expressions.aggregations;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.SignatureUtils;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.ValueType;

/**
 * A builder for {@link ColumnAggregation}s. Start building with {@link #aggregationBuilder()}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public class AggregationBuilder {
    /**
     * @return a builder for {@link ColumnAggregation}.
     */
    public static RequiresName aggregationBuilder() {
        return name -> description -> keywords -> category -> args -> (returnDesc, returnType,
            returnTypeMapping) -> new FinalStage(name, description, keywords, category, args, returnDesc, returnType,
                returnTypeMapping);
    }

    // NOTE: Sonar suggest use unicode-aware char classes but we only support ASCII names for functions
    private static final Pattern SCREAMING_SNAKE_CASE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$"); // NOSONAR

    // ====================== BUILDER ===========================

    interface RequiresName {
        /**
         * @param name the {@link ColumnAggregation#name()}
         * @return the next stage of the builder
         */
        RequiresDescription name(String name);
    }

    interface RequiresDescription {
        /**
         * @param description the {@link OperatorDescription#description()}
         * @return the next stage of the builder
         */
        RequiresKeywords description(String description);
    }

    interface RequiresKeywords {
        /**
         * @param keywords the {@link OperatorDescription#keywords()}
         * @return the next stage of the builder
         */
        RequiresCategory keywords(String... keywords);
    }

    interface RequiresCategory {
        /**
         * @param category the {@link OperatorDescription#category()}
         * @return the next stage of the builder
         */
        RequiresArgs category(String category);
    }

    interface RequiresArgs {
        /**
         * @param args the argument declarations
         * @return the next stage of the builder
         */
        RequiresReturnType args(Arg... args);
    }

    /** Type helper for the function mapping from input arguments to the output type. */
    public interface ReturnTypeMapper {
        /**
         * Infer the return type of the aggregation applied on the given arguments.
         *
         * @param arguments the arguments of the aggregation. The value of the arguments can be considered because all
         *            arguments are constant.
         * @param columnType a function that can be used to look up the type of a column by its name
         * @return the return type as a {@link ReturnResult}, either success or failure depending on whether the
         *         arguments are valid.
         */
        ReturnResult<ValueType> returnType(Arguments<ConstantAst> arguments,
            Function<String, ReturnResult<ValueType>> columnType);
    }

    interface RequiresReturnType {
        /**
         * @param returnDesc the {@link OperatorDescription#returnDescription()}
         * @param returnType the {@link OperatorDescription#returnType()}
         * @param returnTypeMapping a function mapping from input arguments to the return type
         * @return the next stage of the builder
         */
        FinalStage returnType(String returnDesc, String returnType, ReturnTypeMapper returnTypeMapping);
    }

    record FinalStage( // NOSONAR - equals and hashCode are not important for this record
        String name, String description, String[] keywords, String category, Arg[] args, String returnDesc,
        String returnType, ReturnTypeMapper returnTypeMapping) {

        public ColumnAggregation build() {
            // Check that the name is screaming snake case
            if (!SCREAMING_SNAKE_CASE_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException("Aggregation name must be screaming snake case");
            }

            var argsList = List.of(args);
            SignatureUtils.checkSignature(argsList);

            var desc = new OperatorDescription(name, description, Arg.toOperatorDescription(argsList), returnType,
                returnDesc, List.of(keywords), category, OperatorDescription.FUNCTION_ENTRY_TYPE);

            return new AggregationImpl(name, desc, argsList, returnTypeMapping);
        }
    }

    private static final class AggregationImpl implements ColumnAggregation {

        private final String m_name;

        private final OperatorDescription m_desc;

        private final List<Arg> m_signature;

        private final ReturnTypeMapper m_returnTypeMapping; // NOSONAR

        AggregationImpl(final String name, final OperatorDescription desc, final List<Arg> signature,
            final ReturnTypeMapper returnTypeMapping) {
            m_name = name;
            m_desc = desc;
            m_signature = signature;
            m_returnTypeMapping = returnTypeMapping;
        }

        @Override
        public String name() {
            return m_name;
        }

        @Override
        public OperatorDescription description() {
            return m_desc;
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(m_signature, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ConstantAst> arguments,
            final Function<String, ReturnResult<ValueType>> columnType) {

            return m_returnTypeMapping.returnType(arguments, columnType);
        }
    }
}
