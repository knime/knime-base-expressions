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
 *   Apr 23, 2024 (tobias): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.DATE_DURATION;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.LOCAL_DATE;
import static org.knime.core.expressions.ValueType.LOCAL_DATE_TIME;
import static org.knime.core.expressions.ValueType.LOCAL_TIME;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_BOOLEAN;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.ValueType.TIME_DURATION;
import static org.knime.core.expressions.ValueType.ZONED_DATE_TIME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.Computer.ComputerResultSupplier;
import org.knime.core.expressions.Computer.DateDurationComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.LocalDateComputer;
import org.knime.core.expressions.Computer.LocalDateTimeComputer;
import org.knime.core.expressions.Computer.LocalTimeComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Computer.TimeDurationComputer;
import org.knime.core.expressions.Computer.ZonedDateTimeComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.OperatorDescription;
import org.knime.core.expressions.OperatorDescription.Argument;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.ValueType.NativeValueType;

/**
 * Implementation of built-in functions that control the flow.
 *
 * @author Tobias Kampmann, TNG, Schwerte, Germany
 */

public final class ControlFlowFunctions {

    private ControlFlowFunctions() {
    }

    /** The "Control flow" category */
    public static final OperatorCategory CATEGORY = new OperatorCategory("Condition", """
            The "Condition" category in KNIME Expression language provides functions that enable conditional logic
            within expressions. These functions allow users to define conditions and return specific values based on
            those conditions, thus making expressions more dynamic and context-sensitive.
            """);

    /** The "if*" function */
    public static final ExpressionFunction IF = new IfFunction();

    private static final class IfFunction implements ExpressionFunction {

        private static final String DESCRIPTION = """
                **Conditional expression:**  \s
                `if(condition_1, value_1, ...<condition_N, value_N>, value_if_all_false)`  \s

                The first expression after a fulfilled condition will be returned. \
                If no condition evaluates to `TRUE` the `else` case, i.e. `value_if_all_false` will be returned.   \
                Conditions need to be boolean expressions and all `value_N` expressions have to return the same type. \
                Integers will be automatically cast to floats if necessary.  \s

                Note that `MISSING` conditions are considered to be `FALSE` for the purposes of this function.
                """;

        private static final String EXAMPLES = """
                ```
                if(  \s
                \t $customer_id < 100,  #condition_1  \s
                \t $customer_name + " is an early customer", #value_1  \s
                \t $customer_id < 1000, #condtion_2  \s
                \t $customer_name + " is a mid customer", #value_2 and cond. 1 false  \s
                \t $customer_name + " is a late customer", #value_if_all_false  \s
                )
                ```

                The simplest case has no additional conditions (`if(condition_1, value_1, value_if_all_false)`) and this
                function becomes a classical *if-then-else*:  \s
                ```
                if(  \s
                \t $customer_id < 100,  #if / condition_1 \s
                \t $customer_name + " is an early customer", #then / value_1  \s
                \t $customer_name + " is a late customer", #else / value_if_all_false  \s
                )
                ```
                    """;

        @Override
        public String name() {
            return "if";
        }

        @Override
        public OperatorDescription description() {
            return new OperatorDescription( //
                name(), //
                DESCRIPTION, //
                EXAMPLES, //
                List.of(
                    new Argument("condition_1", OPT_BOOLEAN.name(),
                        "Boolean condition. See how to chain multiple conditions in description below."),
                    new Argument("value_1", "ANYTHING", "The first condition to check"),
                    new Argument("additional_conditions__value_if_all_false", "ANYTHING", """
                            Pairs of conditions and related expressions executed when the condition evaluates to
                            `TRUE`. Last argument is the mandatory default ("else" case) to be returned when no
                            condition is fulfilled.""") //
                ), //
                "Common return type of conditional expressions", //
                "Result of the expression belonging to the first matched condition", //
                List.of("conditional"), //
                CATEGORY.fullName(), //
                "function" //
            );
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            if (!namedArguments.isEmpty()) {
                return ReturnResult.failure("Named arguments are not supported");
            } else if (positionalArguments.size() < 3) {
                return ReturnResult.failure("At least three arguments are required");
            } else if (positionalArguments.size() % 2 == 0) {
                return ReturnResult.failure("Arguments must be in the form of [condition, value] pairs "
                    + "followed by a value if all conditions are FALSE");
            }
            return ReturnResult.success(new Arguments<>(Map.of(), positionalArguments));
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> argTypes) {
            var args = argTypes.getVariableArgument();

            // Conditions (note that the last argument is the else case)
            for (int i = 0; i < args.size() - 1; i += 2) {
                var condition = args.get(i);
                if (!BOOLEAN.equals(condition.baseType())) {
                    return ReturnResult
                        .failure("Argument " + (i + 1) + " is not a BOOLEAN but is expected to be a condition");
                }
            }

            // Branch expressions
            var branchExpressions = new ArrayList<ValueType>();
            for (int i = 1; i < args.size(); i += 2) {
                branchExpressions.add(args.get(i));
            }
            branchExpressions.add(args.get(args.size() - 1)); // add else case

            return calculateReturnTypeFromBranchExpressionValues(branchExpressions);
        }

        @Override
        public Computer apply(final Arguments<Computer> arguments) {
            return createTypedResultComputer(ctx -> computeMatchingBranchIf(arguments, ctx),
                ifReturnTypeFromComputer(arguments).getValue());
        }

        private ReturnResult<ValueType> ifReturnTypeFromComputer(final Arguments<Computer> arguments) {
            return returnType(arguments.map(Computer::getReturnTypeFromComputer));
        }

        private static Computer computeMatchingBranchIf(final Arguments<Computer> arguments,
            final EvaluationContext ctx) throws ExpressionEvaluationException {
            var args = arguments.getVariableArgument();
            for (int i = 0; i < args.size() - 1; i += 2) {
                if (!args.get(i).isMissing(ctx) && ((BooleanComputer)args.get(i)).compute(ctx)) {
                    return args.get(i + 1);
                }
            }
            return args.get(args.size() - 1);
        }
    }

    /** The "switch*" function */
    public static final ExpressionFunction SWITCH = new SwitchFn();

    private static final class SwitchFn implements ExpressionFunction {

        private static final String DESCRIPTION = """
                **Switch expression:**  \s
                `switch(expression, case_1, value_1, case_2, value_2,..., optional value_if_none_matched)`  \s

                The switch function executes different expressions based on the value of a given input. \
                It compares the `value` against each provided `case` in order until a match is found. \
                The `expression` and `case` must have the same type (either string or integer). \
                If a match is found, the corresponding `value_N` is executed and returned. \
                If no match is found the `value_if_none_matched` expression is returned, if provided. \
                Otherwise, the function returns `MISSING`. \
                All `value_N` expressions have to return the same type. \
                If integer expressions are used, they will be cast to float expressions if necessary.  \s
                """;

        private static final String EXAMPLE = """
                ```  \s
                switch($customer_name,  \s
                \t "Elon", 0, #case_1, value_1  \s
                \t "Mark", 1, #case_2, value_2  \s
                \t "Jeff", 2, #case_3, value_3  \s
                \t 100, #value_if_none_matched / default  \s
                )
                """;

        private static final String SWITCH_EXPRESSION = "expression";

        @Override
        public String name() {
            return "switch";
        }

        @Override
        public OperatorDescription description() {
            return new OperatorDescription( //
                name(), //
                DESCRIPTION, //
                EXAMPLE, //
                List.of( //
                    new Argument(SWITCH_EXPRESSION, "STRING | INTEGER | MISSING", "Value to switch on."), //
                    new Argument("case_1", "STRING | INTEGER | MISSING",
                        "Case to check equality against `expression`."), //
                    new Argument("value_1", "ANYTHING",
                        "Expression to execute, when `case_1` matches the `expression`."), //
                    new Argument("value_if_none_matched", "ANYTHING", """
                            Optional pairs of `case_N` values to check for equality with the `expression` and `value_N`
                            expressions. The last argument can be an optional `value_if_none_matched` expression to be
                            applied as a default when no match is found. If no `value_if_none_matched` expression is
                            provided, the default is `MISSING`.""") //
                ), //
                "Common return type of case expressions", //
                "Result of the expression belonging to the first matched case", //
                List.of("conditional"), //
                CATEGORY.fullName(), //
                "function" //
            );
        }

        @Override
        public <T> ReturnResult<Arguments<T>> signature(final List<T> positionalArguments,
            final Map<String, T> namedArguments) {
            if (!namedArguments.isEmpty()) {
                return ReturnResult.failure("Named arguments are not supported");
            } else if (positionalArguments.size() < 3) {
                return ReturnResult.failure("At least three arguments are required");
            }
            return ReturnResult.success(new Arguments<T>(Map.of(SWITCH_EXPRESSION, positionalArguments.get(0)),
                positionalArguments.subList(1, positionalArguments.size())));
        }

        @Override
        public ReturnResult<ValueType> returnType(final Arguments<ValueType> arguments) {
            var switchExpression = arguments.get(SWITCH_EXPRESSION);
            if (switchExpression.baseType() != STRING && switchExpression.baseType() != INTEGER
                && switchExpression.baseType() != MISSING) {
                return ReturnResult.failure("Switch expression, i.e., the value to switch on, must be "
                    + "of type STRING or INTEGER. First argument is of type " + switchExpression.baseType() + ".");
            }

            var cases = arguments.getVariableArgument();

            // Case expressions (last one is the default case if odd number of arguments)
            for (int i = 0; i < cases.size() - 1; i += 2) {
                var caseType = cases.get(i);
                if (!MISSING.equals(caseType) && !switchExpression.baseType().equals(caseType.baseType())) {
                    return ReturnResult.failure("Case and switch expressions must have the same type. "
                        + "Switch expression is of type " + switchExpression.baseType() + " and case " + (i + 1)
                        + " is of type " + caseType.baseType() + ".");
                }
            }

            // Values
            var branchExpressions = new ArrayList<ValueType>();
            for (int i = 1; i < cases.size(); i += 2) {
                branchExpressions.add(cases.get(i));
            }
            boolean hasDefaultCase = cases.size() % 2 == 1;
            if (hasDefaultCase) {
                branchExpressions.add(cases.get(cases.size() - 1));
            }

            return calculateReturnTypeFromBranchExpressionValues(branchExpressions)
                .map(type -> hasDefaultCase ? type : type.optionalType());
        }

        @Override
        public Computer apply(final Arguments<Computer> arguments) {
            return createTypedResultComputer(ctx -> computeMatchingCaseSwitch(arguments, ctx),
                switchReturnTypeFromComputer(arguments).getValue());
        }

        private ReturnResult<ValueType> switchReturnTypeFromComputer(final Arguments<Computer> arguments) {
            return returnType(arguments.map(Computer::getReturnTypeFromComputer));
        }

        private static Computer computeMatchingCaseSwitch(final Arguments<Computer> arguments,
            final EvaluationContext ctx) throws ExpressionEvaluationException {

            Computer switchExpression = arguments.get(SWITCH_EXPRESSION);
            var cases = arguments.getVariableArgument();

            if (switchExpression.isMissing(ctx)) {
                for (int i = 0; i < cases.size() - 1; i += 2) {
                    if (cases.get(i).isMissing(ctx)) {
                        return cases.get(i + 1);
                    }
                }
            } else if (switchExpression instanceof StringComputer stringComputer) {
                String evaluatedSwitchValue = stringComputer.compute(ctx);
                for (int i = 0; i < cases.size() - 1; i += 2) {
                    if (!cases.get(i).isMissing(ctx) && cases.get(i) instanceof StringComputer stringComputerToCompare
                        && evaluatedSwitchValue.equals(stringComputerToCompare.compute(ctx))) {
                        return cases.get(i + 1);
                    }
                }
            } else if (switchExpression instanceof IntegerComputer integerComputer) {
                long evaluatedSwitchValue = integerComputer.compute(ctx);
                for (int i = 0; i < cases.size() - 1; i += 2) {
                    if (!cases.get(i).isMissing(ctx) && cases.get(i) instanceof IntegerComputer integerComputerToCompare
                        && integerComputerToCompare.compute(ctx) == evaluatedSwitchValue) {
                        return cases.get(i + 1);
                    }
                }
            }

            final boolean hasDefaultCase = cases.size() % 2 != 0;
            return hasDefaultCase ? cases.get(cases.size() - 1) : missing -> true;
        }
    }

    // Utilities that are re-used by both control flow functions

    private static ReturnResult<ValueType>
        calculateReturnTypeFromBranchExpressionValues(final List<ValueType> expressions) {
        if (expressions.stream().allMatch(type -> type == MISSING)) {
            return ReturnResult.failure("All branch expressions are missing. "
                + "At least one branch has to be present to infer the return type for conditional functions.");
        }

        if (!expressions.stream().allMatch(isCompatibleTo(expressions.get(0)))) {
            return ReturnResult.failure("All branch expressions must have the same type.");
        }

        ValueType commonReturnBaseType =
            expressions.stream().filter(type -> type != MISSING).findFirst().orElse(MISSING);
        if (expressions.stream().anyMatch(type -> type.baseType() == FLOAT)) {
            commonReturnBaseType = ValueType.FLOAT;
        }

        if (expressions.stream().anyMatch(ValueType::isOptional)) {
            return ReturnResult.success(commonReturnBaseType.optionalType());
        } else {
            return ReturnResult.success(commonReturnBaseType);
        }
    }

    private static Predicate<ValueType> isCompatibleTo(final ValueType typeToCompare) {
        return typeToCheck -> ValueType.hasSameBaseType(typeToCompare, typeToCheck)
            || (ValueType.isNumericOrOpt(typeToCompare) && ValueType.isNumericOrOpt(typeToCheck))
            || typeToCheck == MISSING || typeToCompare == MISSING;
    }

    /**
     * Helper method to create a Typed Computer from a Computer Supplier and the intended return type {@link ValueType}.
     * This is helpful when the computer supplier needs to compute in order to supply the resulting computer.
     *
     * Note that the return type FLOAT supports {@link IntegerComputer}. In this case, the result value is cast to 64bit
     * floating point which can lead to loss of precision.
     *
     * @param computerSupplier a supplier for the computer that computes the result and therefore needs to be delayed
     * @param returnType the intended return type of the computer; will be cast to the baseType if necessary
     * @return a {@link Computer} of the {@link ValueType} of return type
     */
    private static Computer createTypedResultComputer(final ComputerResultSupplier<Computer> computerSupplier,
        final ValueType returnType) {

        BooleanComputerResultSupplier isMissing = ctx -> computerSupplier.apply(ctx).isMissing(ctx);

        if (returnType.baseType() == BOOLEAN) {
            return BooleanComputer.of(ctx -> ((BooleanComputer)computerSupplier.apply(ctx)).compute(ctx), isMissing);
        }
        if (returnType.baseType() == INTEGER) {
            return IntegerComputer.of(ctx -> ((IntegerComputer)computerSupplier.apply(ctx)).compute(ctx), isMissing);
        }
        if (returnType.baseType() == FLOAT) {
            return FloatComputer.of(ctx -> Computer.toFloat(computerSupplier.apply(ctx)).compute(ctx), isMissing);
        }
        if (returnType.baseType() == STRING) {
            return StringComputer.of(ctx -> ((StringComputer)computerSupplier.apply(ctx)).compute(ctx), isMissing);
        }
        if (returnType.baseType() == LOCAL_DATE) {
            return LocalDateComputer.of(ctx -> ((LocalDateComputer)computerSupplier.apply(ctx)).compute(ctx),
                isMissing);
        }
        if (returnType.baseType() == LOCAL_TIME) {
            return LocalTimeComputer.of(ctx -> ((LocalTimeComputer)computerSupplier.apply(ctx)).compute(ctx),
                isMissing);
        }
        if (returnType.baseType() == LOCAL_DATE_TIME) {
            return LocalDateTimeComputer.of(ctx -> ((LocalDateTimeComputer)computerSupplier.apply(ctx)).compute(ctx),
                isMissing);
        }
        if (returnType.baseType() == ZONED_DATE_TIME) {
            return ZonedDateTimeComputer.of(ctx -> ((ZonedDateTimeComputer)computerSupplier.apply(ctx)).compute(ctx),
                isMissing);
        }
        if (returnType.baseType() == TIME_DURATION) {
            return TimeDurationComputer.of(ctx -> ((TimeDurationComputer)computerSupplier.apply(ctx)).compute(ctx),
                isMissing);
        }
        if (returnType.baseType() == DATE_DURATION) {
            return DateDurationComputer.of(ctx -> ((DateDurationComputer)computerSupplier.apply(ctx)).compute(ctx),
                isMissing);
        }

        var commaSeparatedNames = Arrays.stream(NativeValueType.values()) //
            .map(ValueType::name) //
            .collect(Collectors.joining(", "));
        throw new IllegalStateException("Type of Expression is unknown: " + returnType + " not in "
            + commaSeparatedNames + ". This in an implementation error.");
    }
}
