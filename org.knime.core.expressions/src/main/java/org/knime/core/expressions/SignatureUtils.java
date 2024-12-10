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
 *   Aug 19, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.expressions.SignatureUtils.Arg.ArgKind;

/**
 * Utility class to work with signatures that are defined by a list of {@link Arg arguments} with associated types.
 * <P>
 * NOTE: The expression framework allows operators to handle arbitrary arguments. However, it suffices for most
 * operators to define the signature as used here.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Tobias Kampmann, TNG, Schwerte, Germany
 */
public final class SignatureUtils {

    private SignatureUtils() {
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
    // TODO(AP-23139): we should define the default in the function definition.
    // Right now the defaults are all over the place.
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
        return new ArgMatcherImpl(ReturnTypeDescriptions.RETURN_INTEGER_FLOAT, ValueType::isNumeric);
    }

    /** @return an {@link ArgMatcher} that matches all numeric types (optional or not) */
    public static ArgMatcher isNumericOrOpt() {
        return new ArgMatcherImpl(ReturnTypeDescriptions.RETURN_INTEGER_FLOAT_MISSING, ValueType::isNumericOrOpt);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#INTEGER} */
    public static ArgMatcher isInteger() {
        return hasType(ValueType.INTEGER);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#INTEGER} and {@link ValueType#OPT_INTEGER} */
    public static ArgMatcher isIntegerOrOpt() {
        return hasBaseType(ValueType.INTEGER);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#FLOAT} */
    public static ArgMatcher isFloat() {
        return hasType(ValueType.FLOAT);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#FLOAT} and {@link ValueType#OPT_FLOAT} */
    public static ArgMatcher isFloatOrOpt() {
        return hasBaseType(ValueType.FLOAT);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#STRING} */
    public static ArgMatcher isString() {
        return hasType(ValueType.STRING);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#STRING} and {@link ValueType#OPT_STRING} */
    public static ArgMatcher isStringOrOpt() {
        return hasBaseType(ValueType.STRING);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#BOOLEAN} */
    public static ArgMatcher isBoolean() {
        return hasType(ValueType.BOOLEAN);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#BOOLEAN} and {@link ValueType#OPT_BOOLEAN} */
    public static ArgMatcher isBooleanOrOpt() {
        return hasBaseType(ValueType.BOOLEAN);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#DATE} */
    public static ArgMatcher isTemporal() {
        return isOneOfTypes(ValueType.LOCAL_DATE, ValueType.LOCAL_DATE_TIME, ValueType.LOCAL_TIME,
            ValueType.ZONED_DATE_TIME);
    }

    /** @return an {@link ArgMatcher} that matches {@link ValueType#DATE} and {@link ValueType#OPT_LOCAL_DATE} */
    public static ArgMatcher isTemporalOrOpt() {
        return isOneOfBaseTypes(ValueType.LOCAL_DATE, ValueType.LOCAL_DATE_TIME, ValueType.LOCAL_TIME,
            ValueType.ZONED_DATE_TIME);
    }

    /** @return an {@link ArgMatcher} that matches non-optional Temporal that contains a date. */
    public static ArgMatcher hasDatePart() {
        return isOneOfTypes(ValueType.LOCAL_DATE, ValueType.LOCAL_DATE_TIME, ValueType.ZONED_DATE_TIME);
    }

    /** @return an {@link ArgMatcher} that matches optional Temporal that contains a date */
    public static ArgMatcher hasDatePartOrIsOpt() {
        return isOneOfBaseTypes(ValueType.LOCAL_DATE, ValueType.LOCAL_DATE_TIME, ValueType.ZONED_DATE_TIME);
    }

    /** @return an {@link ArgMatcher} that matches optional Temporal that contains a time */
    public static ArgMatcher hasTimePartOrIsOpt() {
        return isOneOfBaseTypes(ValueType.LOCAL_DATE_TIME, ValueType.LOCAL_TIME, ValueType.ZONED_DATE_TIME);
    }

    /** @return an {@link ArgMatcher} that matches any type (missing or otherwise) */
    public static ArgMatcher isAnything() {
        return new ArgMatcherImpl("ANYTHING", arg -> true);
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
     * @param types
     * @return an {@link ArgMatcher} that matches any of the given types
     */
    public static ArgMatcher isOneOfTypes(final ValueType... types) {
        var argListWithMissingOnlyOnce = Arrays.stream(types).map(ValueType::optionalType).toArray(ValueType[]::new);
        return new ArgMatcherImpl("ANY OF " + argListWithMissingOnlyOnce,
            arg -> Arrays.stream(types).anyMatch(validArg -> validArg.equals(arg)));
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

    /**
     * Declaration of an operator argument. Use {@link #arg}, {@link #optarg} or {@link #vararg} to create an argument.
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
                /** arguments that can be omitted */
                OPTIONAL,
                /** arguments that can occur multiple times (or never) */
                VAR;
        }

        /** @return if its an optional argument */
        public boolean isOptional() {
            return kind == ArgKind.OPTIONAL;
        }

        /** @return if its a variable argument */
        public boolean isVariable() {
            return kind == ArgKind.VAR;
        }

        /** @return if its a required argument */
        public boolean isRequired() {
            return kind == ArgKind.REQUIRED;
        }

        /** @return a description of the argument */
        public OperatorDescription.Argument toOperatorDescription() {
            return new OperatorDescription.Argument(name, matcher.allowed(), description);
        }

        /**
         * @param signature the signature to convert
         * @return the signature as a list of {@link OperatorDescription.Argument}
         */
        public static List<OperatorDescription.Argument> toOperatorDescription(final List<Arg> signature) {
            return signature.stream().map(Arg::toOperatorDescription).toList();
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

    /**
     * Simple implementation of {@link ArgMatcher}.
     *
     * @param allowed string representation of the allowed types
     * @param matcher the predicate to match the types
     */
    public record ArgMatcherImpl(String allowed, Predicate<ValueType> matcher) implements ArgMatcher {

        @Override
        public boolean matches(final ValueType type) {
            return matcher.test(type);
        }
    }

    /**
     * Checks if the given signature is valid. The signature is valid if all required arguments are at the beginning and
     * there is at most one variable argument.
     *
     * @param signature the signature to check
     * @throws IllegalArgumentException if the signature is invalid
     */
    public static void checkSignature(final List<Arg> signature) {
        // start with required because this allows everything to follow
        var lastArgKind = ArgKind.REQUIRED;

        for (var arg : signature) {
            if (arg.isRequired() && lastArgKind != ArgKind.REQUIRED) {
                throw new IllegalArgumentException("All required arguments must be at the beginning.");
            }
            if (arg.isVariable() && lastArgKind == ArgKind.VAR) {
                throw new IllegalArgumentException("Only one variadic argument is allowed.");
            }
            if (arg.isVariable() && lastArgKind == ArgKind.OPTIONAL) {
                throw new IllegalArgumentException("Optional arguments must be at the end.");
            }
            lastArgKind = arg.kind();
        }
    }

    /**
     * Match the positional and named arguments to the signature. Canonical way to create proper Arguments from input
     * parameters. If the arguments do not match the signature, return an error message.
     *
     * Note that the signature must list all required arguments first, followed by at most one variadic arguments,
     * followed by optional arguments. Use {@link #checkSignature(List)} to ensure this.
     *
     * @param <T> the type of the arguments
     * @param signature the signature of the function
     * @param positionalArguments the positional arguments to match against the signature (in order)
     * @param namedArguments the named arguments to match against the signature (by name)
     * @return the matched arguments or an error message if the arguments do not match the signature
     */
    public static <T> ReturnResult<Arguments<T>> matchSignature(final List<Arg> signature,
        final List<T> positionalArguments, final Map<String, T> namedArguments) {

        OptionalInt positionOfVarArgument =
            IntStream.range(0, signature.size()).filter(i -> signature.get(i).isVariable()).findFirst();

        int maxLoopIterations = positionOfVarArgument.isPresent() ? positionOfVarArgument.getAsInt() : signature.size();

        if (positionOfVarArgument.isEmpty() && positionalArguments.size() > maxLoopIterations) {
            return ReturnResult.failure("Too many arguments. Expected at most " + signature.size() + " argument"
                + (signature.size() == 1 ? "" : "s") + " ("
                + signature.stream().map(Arg::name).collect(Collectors.joining(", ")) + ") but got "
                + positionalArguments.size() + ".");
        }

        var argumentsMap = new LinkedHashMap<String, T>();

        // Resolving the positional arguments without the variadic argument
        for (var i = 0; i < Math.min(maxLoopIterations, positionalArguments.size()); i++) {
            argumentsMap.put(signature.get(i).name(), positionalArguments.get(i));
        }

        // resolving the variadic argument
        var varargs = new ArrayList<T>();
        if (!positionalArguments.isEmpty() && positionOfVarArgument.isPresent()) {
            for (var i = maxLoopIterations; i < positionalArguments.size(); i++) {
                varargs.add(positionalArguments.get(i));
            }
        }

        // resolving the named arguments, optional arguments must come after the variadic argument
        // and must be named in that case.
        var validArgumentIds = signature.stream().map(Arg::name).collect(Collectors.toSet());

        for (final var entry : namedArguments.entrySet()) {
            var name = entry.getKey();
            if (argumentsMap.containsKey(name)) {
                return ReturnResult.failure("Argument '" + name + "' was provided twice.");
            }
            if (!validArgumentIds.contains(name)) {
                return ReturnResult
                    .failure("No argument with identifier '" + name + "' found in the function signature.");
            }
            argumentsMap.put(name, entry.getValue());
        }

        // check if all required arguments are present
        for (var arg : signature) {
            if (!arg.isOptional() && !arg.isVariable() && !argumentsMap.containsKey(arg.name())) {
                return ReturnResult.failure("Missing required argument: " + arg.name() + ".");
            }
        }

        return ReturnResult.success(new Arguments<>(argumentsMap, varargs));
    }

    /**
     * Check if the arguments match the types of the signature.
     *
     * @param signature the signature of the function
     * @param arguments the arguments to check
     * @return a successful {link ReturnResult} if the arguments match the signature or an error message explaining why
     *         the arguments do not match the signature
     */
    public static ReturnResult<Void> checkTypes(final List<Arg> signature, final Arguments<ValueType> arguments) {
        var namedArguments = arguments.getNamedArguments();
        List<ValueType> variableArguments = arguments.getVariableArgument();

        int variadicArgIndex =
            IntStream.range(0, signature.size()).filter(i -> signature.get(i).isVariable()).findFirst().orElse(-1);

        for (var arg : namedArguments.entrySet()) {

            Optional<Arg> expectedArgument = signature.stream().filter(a -> a.name().equals(arg.getKey())).findFirst();

            if (expectedArgument.isEmpty()) {
                return ReturnResult.failure("Argument not found: " + arg.getKey());
            }

            if (!expectedArgument.get().matcher().matches(arg.getValue())) {

                int positionInSignature = 1;
                if (variadicArgIndex != -1 && signature.indexOf(expectedArgument.get()) > variadicArgIndex) {
                    positionInSignature += variadicArgIndex + variableArguments.size();
                } else {
                    positionInSignature += signature.indexOf(expectedArgument.get());
                }

                var isOptionalWhenItsNotAllowed = expectedArgument.get().matcher().matches(arg.getValue().baseType());
                if (isOptionalWhenItsNotAllowed) {
                    return ReturnResult.failure("Argument (" + positionInSignature + ", '" + arg.getKey()
                        + "') is optional but MISSING values are not allowed here. "
                        + "Use the nullish coalescing operator '??' to define a default value.");
                }

                return ReturnResult.failure(
                    "Argument (" + positionInSignature + ", '" + arg.getKey() + "') is not of the expected type: "
                        + expectedArgument.get().matcher().allowed() + " but got " + arg.getValue() + ".");
            }

        }

        if (variableArguments.isEmpty()) {
            return ReturnResult.success();
        }

        var variadicArgument = signature.stream().filter(Arg::isVariable).findFirst();
        if (variadicArgument.isEmpty()) {
            return ReturnResult.failure(
                "No variadic argument found in the signature, but got variable arguments: " + variableArguments + ".");
        }
        var variadicArgumentMatcher = variadicArgument.get().matcher();

        var position = 0;
        for (var arg : variableArguments) {

            position++;
            if (!variadicArgumentMatcher.matches(arg)) {
                return ReturnResult.failure("Argument (" + (variadicArgIndex + position)
                    + ", 'variableArguments') is not of the expected type: " + variadicArgumentMatcher.allowed()
                    + " but got " + arg + ".");
            }
        }

        return ReturnResult.success();
    }
}
