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
 *   Apr 8, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.SignatureUtils;
import org.knime.core.expressions.SignatureUtils.Arg;
import org.knime.core.expressions.ValueType;

/**
 * A builder for {@link ExpressionFunction}s. Start building with {@link #functionBuilder()}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ExpressionFunctionBuilder {

    // NOTE: Sonar suggest use unicode-aware char classes but we only support ASCII names for functions
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$"); // NOSONAR

    private ExpressionFunctionBuilder() {
    }

    /**
     * @return a builder for {@link ExpressionFunction}.
     */
    public static RequiresName functionBuilder() {
        return name -> description -> examples -> keywords -> category -> args -> (returnDesc, returnType,
            returnTypeMapping) -> impl -> new FinalStage(name, description, examples, keywords, category, args,
                returnDesc, returnType, returnTypeMapping, impl);
    }

    // ====================== PUBLIC UTILITIES ===========================

    /**
     * Check if any of the argument types is optional.
     *
     * @param types the argument types
     * @return <code>true</code> if at least one type is optional
     */
    public static boolean anyOptional(final Arguments<ValueType> types) {
        return types.anyMatch(v -> v.isOptional());
    }

    /**
     * Factory for isMissing argument to computers. Returns an Predicate<EvaluationContext> that returns true iff at
     * least one if the arguments is missing.
     *
     * @param values
     * @return the predicate
     */
    public static BooleanComputerResultSupplier anyMissing(final Arguments<Computer> values) {
        return ctx -> values.anyMatch(c -> c.isMissing(ctx));
    }

    /**
     * Check if the {@link ValueType#baseType()} of all arguments match the given predicate.
     *
     * @param matcher the predicate
     * @param types the argument types
     * @return <code>true</code> if the {@link ValueType#baseType()} of all arguments match the predicate
     */
    public static boolean allBaseTypesMatch(final Predicate<ValueType> matcher, final Arguments<ValueType> types) {
        return types.map(ValueType::baseType).allMatch(matcher::test);
    }

    // ====================== BUILDER ===========================

    interface RequiresName {
        /**
         * @param name the {@link ExpressionFunction#name()}
         * @return the next stage of the builder
         */
        RequiresDescription name(String name);
    }

    interface RequiresDescription {
        /**
         * @param description the {@link OperatorDescription#description()}
         * @return the next stage of the builder
         */
        RequiresExamples description(String description);
    }

    interface RequiresExamples {
        /**
         * @param examples the {@link OperatorDescription#examples()}
         * @return the next stage of the builder
         */
        RequiresKeywords examples(String examples);
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
        RequiresArgs category(OperatorCategory category);
    }

    interface RequiresArgs {
        /**
         * @param args the argument declarations
         * @return the next stage of the builder
         */
        RequiresReturnType args(Arg... args);
    }

    interface RequiresReturnType {
        /**
         * @param returnDesc the {@link OperatorDescription#returnDescription()}
         * @param returnType the {@link OperatorDescription#returnType()}
         * @param returnTypeMapping a function mapping from legal input argument types to the return type
         * @return the next stage of the builder
         */
        RequiresImpl returnType(String returnDesc, String returnType,
            Function<Arguments<ValueType>, ValueType> returnTypeMapping);
    }

    interface RequiresImpl {
        /**
         * @param impl the implementation of the function ({@link ExpressionFunction#apply(Arguments)}
         * @return the next stage of the builder
         */
        FinalStage impl(Function<Arguments<Computer>, Computer> impl);
    }

    record FinalStage( // NOSONAR - equals and hashCode are not important for this record
        String name, String description, String examples, String[] keywords, OperatorCategory category, Arg[] args,
        String returnDesc, String returnType, Function<Arguments<ValueType>, ValueType> returnTypeMapping,
        Function<Arguments<Computer>, Computer> impl) {

        public ExpressionFunction build() {
            // Check that the name is snake_case
            if (!SNAKE_CASE_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException("Function name must be snake case.");
            }

            var argsList = List.of(args);
            SignatureUtils.checkSignature(argsList);

            var argsDesc = Arg.toOperatorDescription(argsList);
            var desc = new OperatorDescription( //
                name, description, examples, //
                argsDesc, returnType, returnDesc, //
                List.of(keywords), category.fullName(), OperatorDescription.FUNCTION_ENTRY_TYPE //
            );

            Function<Arguments<ValueType>, ReturnResult<ValueType>> typeMappingAndCheck = argTypes -> SignatureUtils
                .checkTypes(argsList, argTypes).map(valid -> returnTypeMapping.apply(argTypes));

            return new FunctionImpl(name, desc, List.of(args), typeMappingAndCheck, impl);
        }
    }

    // ====================== RESULT IMPLEMENTATION ===========================

    private static final class FunctionImpl implements ExpressionFunction {

        private final String m_name;

        private final OperatorDescription m_description;

        private final List<Arg> m_signature;

        private final Function<Arguments<ValueType>, ReturnResult<ValueType>> m_typeMapping;

        private final Function<Arguments<Computer>, Computer> m_impl;

        FunctionImpl(final String name, final OperatorDescription description, final List<Arg> signature,
            final Function<Arguments<ValueType>, ReturnResult<ValueType>> typeMapping,
            final Function<Arguments<Computer>, Computer> impl) {
            m_name = name;
            m_description = description;
            m_signature = signature;
            m_typeMapping = typeMapping;
            m_impl = impl;
        }

        @Override
        public String name() {
            return m_name;
        }

        @Override
        public OperatorDescription description() {
            return m_description;
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            return SignatureUtils.matchSignature(m_signature, positionalArguments, namedArguments);
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            return m_typeMapping.apply(argTypes);
        }

        @Override
        public Computer apply(final Arguments<Computer> args) {
            return m_impl.apply(args);
        }
    }
}
