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

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.STRING;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.knime.core.expressions.Computer;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.WarningMessageListener;
import org.knime.core.expressions.functions.ExpressionFunctionBuilder.Arg.ArgKind;

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
        return name -> description -> keywords -> category -> args -> (returnDesc, returnType,
            returnTypeMapping) -> impl -> new FinalStage(name, description, keywords, category, args, returnDesc,
                returnType, returnTypeMapping, impl);
    }

    // ====================== PUBLIC UTILITIES ===========================

    /**
     * Check if any of the argument types is optional.
     *
     * @param types the argument types
     * @return <code>true</code> if at least one type is optional
     */
    public static boolean anyOptional(final ValueType... types) {
        return Arrays.stream(types).anyMatch(ValueType::isOptional);
    }

    /**
     * Factory for isMissing argument to computers. Returns an Predicate<WarningMessageListener> that returns true iff
     * at least one if the arguments is missing.
     *
     * @param values
     * @return the predicate
     */
    public static Predicate<WarningMessageListener> anyMissing(final Collection<Computer> values) {
        return wml -> values.stream().anyMatch(c -> c.isMissing(wml));
    }

    /**
     * Check if the {@link ValueType#baseType()} of all arguments match the given predicate.
     *
     * @param matcher the predicate
     * @param types the argument types
     * @return <code>true</code> if the {@link ValueType#baseType()} of all arguments match the predicate
     */
    public static boolean allBaseTypesMatch(final Predicate<ValueType> matcher, final ValueType... types) {
        return Arrays.stream(types).map(ValueType::baseType).allMatch(matcher);
    }

    /**
     * Creates a {@link ArgKind#REQUIRED} argument.
     *
     * @param name
     * @param description
     * @param matcher
     * @return the argument
     */
    public static Arg arg(final String name, final String description, final ArgMatcher matcher) {
        return new Arg(name, description, matcher, ArgKind.REQUIRED);
    }

    /**
     * Creates a {@link ArgKind#OPTIONAL} argument.
     *
     * @param name
     * @param description
     * @param matcher
     * @return the argument
     */
    public static Arg optarg(final String name, final String description, final ArgMatcher matcher) {
        return new Arg(name, description, matcher, ArgKind.OPTIONAL);
    }

    /**
     * Creates a {@link ArgKind#VAR} argument.
     *
     * @param name
     * @param description
     * @param matcher
     * @return the argument
     */
    public static Arg vararg(final String name, final String description, final ArgMatcher matcher) {
        return new Arg(name, description, matcher, ArgKind.VAR);
    }

    /** @return an {@link ArgMatcher} that matches all numeric non-optional types */
    public static ArgMatcher isNumeric() {
        return new ArgMatcherImpl("INTEGER | FLOAT", ValueType::isNumeric);
    }

    /** @return an {@link ArgMatcher} that matches all numeric types (optional or not) */
    public static ArgMatcher isNumericOrOpt() {
        return new ArgMatcherImpl("INTEGER? | FLOAT?", ValueType::isNumericOrOpt);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#INTEGER} */
    public static ArgMatcher isInteger() {
        return hasType(INTEGER);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#INTEGER} and {@link ValueType#OPT_INTEGER} */
    public static ArgMatcher isIntegerOrOpt() {
        return hasBaseType(INTEGER);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#FLOAT} */
    public static ArgMatcher isFloat() {
        return hasType(FLOAT);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#FLOAT} and {@link ValueType#OPT_FLOAT} */
    public static ArgMatcher isFloatOrOpt() {
        return hasBaseType(FLOAT);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#STRING} */
    public static ArgMatcher isString() {
        return hasType(STRING);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#STRING} and {@link ValueType#OPT_STRING} */
    public static ArgMatcher isStringOrOpt() {
        return hasBaseType(STRING);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#BOOLEAN} */
    public static ArgMatcher isBoolean() {
        return hasType(BOOLEAN);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#BOOLEAN} and {@link ValueType#OPT_BOOLEAN} */
    public static ArgMatcher isBooleanOrOpt() {
        return hasBaseType(BOOLEAN);
    }

    /** @return an {@link ArgMatcher} that matches any type (missing or otherwise) */
    public static ArgMatcher isAnything() {
        return new ArgMatcherImpl("ANY", arg -> true);
    }

    /**
     * @param types the types to match
     * @return an {@link ArgMatcher} that matches any of the given types
     */
    public static ArgMatcher isOneOfBaseTypes(final ValueType... types) {
        return new ArgMatcherImpl("ANY OF " + Arrays.toString(types),
            arg -> Arrays.stream(types).anyMatch(validArg -> validArg.baseType().equals(arg.baseType())));
    }

    /**
     * @param type the exact type to match
     * @return an {@link ArgMatcher} that matches only the given type
     */
    public static ArgMatcher hasType(final ValueType type) {
        return new ArgMatcherImpl(type.name(), type::equals);
    }

    /**
     * @param baseType the base type to match
     * @return an {@link ArgMatcher} that matches all types that have the given {@link ValueType#baseType()}
     */
    public static ArgMatcher hasBaseType(final ValueType baseType) {
        return new ArgMatcherImpl(baseType.optionalType().name(), arg -> baseType.equals(arg.baseType()));
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

    interface RequiresReturnType {
        /**
         * @param returnDesc the {@link OperatorDescription#returnDescription()}
         * @param returnType the {@link OperatorDescription#returnType()}
         * @param returnTypeMapping a function mapping from legal input argument types to the return type
         * @return the next stage of the builder
         */
        RequiresImpl returnType(String returnDesc, String returnType,
            Function<ValueType[], ValueType> returnTypeMapping);
    }

    interface RequiresImpl {
        /**
         * @param impl the implementation of the function ({@link ExpressionFunction#apply(List)}
         * @return the next stage of the builder
         */
        FinalStage impl(Function<List<Computer>, Computer> impl);
    }

    record FinalStage( // NOSONAR - equals and hashCode are not important for this record
        String name, String description, String[] keywords, String category, Arg[] args, String returnDesc,
        String returnType, Function<ValueType[], ValueType> returnTypeMapping,
        Function<List<Computer>, Computer> impl) {

        public ExpressionFunction build() {
            // Check that the name is snake_case
            if (!SNAKE_CASE_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException("Function name must be snake case");
            }

            if (Arrays.stream(args).limit(args.length - 1L).anyMatch(a -> a.kind != ArgKind.REQUIRED)) {
                throw new IllegalArgumentException("Only the last argument can be optional or variable");
            }

            var argsDesc = Arrays.stream(args) //
                .map(a -> new OperatorDescription.Argument(a.name, a.matcher.allowed(), a.description)) //
                .toList();
            var desc = new OperatorDescription(name, description, argsDesc, returnType, returnDesc, List.of(keywords),
                category);

            return new FunctionImpl(name, desc, argTypes -> {
                // Check if the args match the definition
                if (!argsMatch(argTypes.toArray(ValueType[]::new))) {
                    return Optional.empty();
                }

                // Compute the return type
                return Optional.ofNullable(returnTypeMapping.apply(argTypes.toArray(ValueType[]::new)));
            }, impl);
        }

        private boolean argsMatch(final ValueType[] argTypes) {
            // Check if the number or args is correct
            var numDefined = args.length;
            var numArgs = argTypes.length;
            var correctArgCount = switch (args[numDefined - 1].kind) {
                case REQUIRED -> numArgs == numDefined;
                case OPTIONAL -> numArgs == numDefined || numArgs == numDefined - 1;
                case VAR -> numArgs >= numDefined - 1; // note: count of vararg could be 0
            };

            // Checks if arg at location i matches definition at i or the last definition
            IntPredicate argMatches = i -> args[Math.min(i, args.length - 1)].matcher.matches(argTypes[i]);
            return correctArgCount && IntStream.range(0, numArgs).allMatch(argMatches);
        }
    }

    // ====================== RESULT IMPLEMENTATION ===========================

    private static final class FunctionImpl implements ExpressionFunction {

        private String m_name;

        private OperatorDescription m_description;

        private Function<List<ValueType>, Optional<ValueType>> m_typeMapping;

        private Function<List<Computer>, Computer> m_impl;

        FunctionImpl(final String name, final OperatorDescription description,
            final Function<List<ValueType>, Optional<ValueType>> typeMapping,
            final Function<List<Computer>, Computer> impl) {
            m_name = name;
            m_description = description;
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
        public Optional<ValueType> returnType(final List<ValueType> argTypes) {
            return m_typeMapping.apply(argTypes);
        }

        @Override
        public Computer apply(final List<Computer> args) {
            return m_impl.apply(args);
        }
    }

    // ====================== TYPES FOR ARGUMENTS ===========================

    /**
     * Declaration of a function argument
     *
     * @param name the argument name
     * @param description
     * @param matcher
     * @param kind
     */
    public record Arg(String name, String description, ArgMatcher matcher, ArgKind kind) {

        /** Kind of a function argument */
        public enum ArgKind {
                /** standard arguments */
                REQUIRED,
                /** arguments that can be omitted (only allowed at the last position) */
                OPTIONAL,
                /** arguments that can occur multiple times (or never) */
                VAR;
        }
    }

    /**
     * Checks if the type of an argument matches a predicate via {@link #matches(ValueType)} and displays this rule as a
     * String via {@link #allowed()}.
     */
    public interface ArgMatcher {

        /**
         * @param type the argument type
         * @return if the argument type matches the predicate
         */
        boolean matches(ValueType type);

        /** @return a String representation of the types accepted by this matcher */
        String allowed();
    }

    private record ArgMatcherImpl(String allowed, Predicate<ValueType> matcher) implements ArgMatcher {

        @Override
        public boolean matches(final ValueType type) {
            return matcher.test(type);
        }
    }
}
