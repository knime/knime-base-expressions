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

import static org.knime.core.expressions.SimplifiedSignatureUtils.arg;
import static org.knime.core.expressions.SimplifiedSignatureUtils.isAnything;
import static org.knime.core.expressions.SimplifiedSignatureUtils.isBoolean;
import static org.knime.core.expressions.SimplifiedSignatureUtils.vararg;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.knime.core.expressions.Arguments;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;

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

    /** Argument identifier */
    private static final String IF_CONDITION = "condition_1";

    private static final String IF_TRUE_BRANCH = "value_1";

    private static final String IF_ELSE = "additional_conditions__value_if_all_false";

    /** The "if*" function */
    public static final ExpressionFunction IF = functionBuilder() //
        .name("if") //
        .description(
            """
                    **Conditional expression:**  \s
                    `if(condition_1, value_1, ...<condition_N, value_N>, value_if_all_false)`  \s

                    The first expression after a fulfilled condition will be returned. \
                    If no condition evaluates to `TRUE` the `else` case, i.e. `value_if_all_false` will be returned.   \
                    Conditions need to be boolean expressions and all `value_N` expressions have to return the same type. \
                    Integers will be automatically cast to floats if necessary.  \s

                    **Examples**  \s
                    ```  \s
                    if(  \s
                    \t $customer_id < 100,  #condition_1  \s
                    \t $customer_name + " is an early customer", #value_1  \s
                    \t $customer_id < 1000, #condtion_2  \s
                    \t $customer_name + " is a mid customer", #value_2 and cond. 1 false  \s
                    \t $customre_name + " is a late customer", #value_if_all_false  \s
                    )
                    ```
                      \s
                    The simplest case has no additional conditions (`if(condition_1, value_1, value_if_all_false)`) and this function becomes a classical *if-then-else*:  \s
                    ```
                    if(  \s
                    \t $customer_id < 100,  #if / condition_1 \s
                    \t $customer_name + " is an early customer", #then / value_1  \s
                    \t $customer_name + " is a late customer", #else / value_if_all_false  \s
                    )
                    ```
                    """ //
        ) //
        .keywords("conditional") //
        .category(CATEGORY.name()) //
        .args( //
            arg(IF_CONDITION, "Boolean condition. See how to chain multiple conditions in description below.",
                isBoolean()), //
            arg(IF_TRUE_BRANCH, "Expression if condition 1 is `TRUE`.", isAnything()), //
            vararg(IF_ELSE,
                "Pairs of conditions and related expressions executed when the condition evaluates to `TRUE`. "
                    + "Last argument is the mandatory default (\"else\" case) to be returned when no condition is fulfilled.",
                isAnything()) //
        ) //
        .returnType("Result of the expression belonging to the first matched condition",
            "Common return type of conditional expressions", ControlFlowFunctions::ifReturnType) //
        .impl(ControlFlowFunctions::ifImpl) //
        .build();

    private static ReturnResult<ValueType> ifReturnType(final Arguments<ValueType> arguments) {

        // At this point we already validated the arguments, so we can safely assume that the arguments are present
        // and the types are correct. Nevertheless, the vararg is not checked for the correct number of arguments.
        var elseIf = arguments.getVariableArgument();
        if (elseIf.isEmpty()) {
            return ReturnResult.failure("If statements must have at least one else branch.");
        }
        if (elseIf.size() % 2 == 0) {
            return ReturnResult
                .failure("Parameter " + IF_ELSE + " has to be a series of n [condition,value] pairs (n can be 0) + "
                    + "followed by a mandatory else value. Please provide at least one else value.");
        }

        var firstBranch = arguments.get("value_1");

        ArrayList<ValueType> branchExpressions = new ArrayList<>();
        branchExpressions.add(firstBranch);

        for (int i = 1; i < elseIf.size() - 1; i += 2) {
            branchExpressions.add(elseIf.get(i));
        }
        branchExpressions.add(elseIf.get(elseIf.size() - 1));

        return calculateReturnTypeFromBranchExpressionValues(branchExpressions);

    }

    private static ReturnResult<ValueType> ifReturnTypeFromComputer(final Arguments<Computer> arguments) {
        return ifReturnType(arguments.map(Computer::getReturnTypeFromComputer));
    }

    private static Computer computeMatchingBranchIf(final Arguments<Computer> arguments, final EvaluationContext ctx) {
        var condition = arguments.get(IF_CONDITION);

        if (((BooleanComputer)condition).compute(ctx)) {
            return arguments.get(IF_TRUE_BRANCH);
        }

        var elseIf = arguments.getVariableArgument();

        for (int i = 0; i < elseIf.size() - 1; i += 2) {
            if (((BooleanComputer)elseIf.get(i)).compute(ctx)) {
                return elseIf.get(i + 1);
            }
        }
        return elseIf.get(elseIf.size() - 1);
    }

    private static Computer ifImpl(final Arguments<Computer> arguments) {
        return Computer.createTypedResultComputer(ctx -> computeMatchingBranchIf(arguments, ctx),
            ifReturnTypeFromComputer(arguments).getValue());
    }

    /** Argument identifier for switch */
    private static final String SWITCH_EXPRESSION = "expression";

    private static final String SWITCH_CASE = "case_1";

    private static final String SWITCH_VALUE = "value_1";

    private static final String SWITCH_DEFAULT = "value_if_none_matched";

    /** The "switch" function: switch(value, A, exprInCaseA, B, exprInCaseB, ...) */
    public static final ExpressionFunction SWITCH = functionBuilder() //
        .name("switch") //
        .description("""
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

                **Example**  \s
                ```  \s
                switch($customer_name,  \s
                \t "Elon", 0, #case_1, value_1  \s
                \t "Mark", 1, #case_2, value_2  \s
                \t "Jeff", 2, #case_3, value_3  \s
                \t 100, #value_if_none_matched / default  \s
                )
                ```
                """ //

        ) // isOneOfBaseTypes(STRING,BOOLEAN)
        .keywords("conditional") //
        .category(CATEGORY.name()) //
        .args( //
            arg(SWITCH_EXPRESSION, "Value to switch on. Only accepts string or integer types.", isAnything()), //
            arg(SWITCH_CASE, "Case to check equality against `expression`.", isAnything()), //
            arg(SWITCH_VALUE, "Expression to execute, when `case_1` matches the `expression`.", isAnything()), //
            vararg(SWITCH_DEFAULT,
                "Optional pairs of `case_N` values to check for equality with the `expression` and `value_N` expressions. "
                    + "The last argument can be an optional `value_if_none_matched` expression to be applied as a default when no match is found. "
                    + "If no `value_if_none_matched` expression is provided, the default is `MISSING`.",
                isAnything()) //
        ) //
        .returnType("Result of the expression belonging to the first matched case",
            "Common return type of case expressions", ControlFlowFunctions::switchReturnType) //
        .impl(ControlFlowFunctions::switchImpl) //
        .build();

    private static ReturnResult<ValueType> switchReturnType(final Arguments<ValueType> arguments) {

        var switchExpression = arguments.get(SWITCH_EXPRESSION);
        if (switchExpression.baseType() != STRING && switchExpression.baseType() != INTEGER
            && switchExpression.baseType() != MISSING) {
            return ReturnResult.failure("Switch expression, i.e., the value to switch on, must be "
                + "of type STRING or INTEGER. First argument is of type " + switchExpression.baseType() + ".");
        }

        if (arguments.get(SWITCH_CASE).baseType() != switchExpression.baseType()) {
            return ReturnResult.failure("Case and switch expressions must have the same type. "
                + "Switch expression is of type " + switchExpression.baseType() + " and first case is of type "
                + arguments.get(SWITCH_CASE).baseType() + ".");
        }

        var cases = arguments.getVariableArgument();
        for (int i = 0; i < cases.size() - 1; i += 2) {
            if (cases.get(i) == MISSING) {
                continue;
            }
            if (cases.get(i).baseType() != switchExpression.baseType()) {
                return ReturnResult.failure("Case and switch expressions must have the same type. "
                    + "Switch expression is of type " + switchExpression.baseType() + " and case " + i + 1
                    + " is of type " + arguments.get(SWITCH_CASE).baseType() + ".");
            }
        }

        ArrayList<ValueType> branchExpressions = new ArrayList<>();
        branchExpressions.add(arguments.get(SWITCH_VALUE));

        int defaultIndex = cases.size() - (cases.size() % 2);
        for (int i = 1; i < defaultIndex; i += 2) {
            branchExpressions.add(cases.get(i));
        }

        boolean hasDefaultCase = cases.size() % 2 == 1;
        if (hasDefaultCase) {
            branchExpressions.add(cases.get(cases.size() - 1));
        }

        return calculateReturnTypeFromBranchExpressionValues(branchExpressions)
            .map(type -> hasDefaultCase ? type : type.optionalType());
    }

    private static ReturnResult<ValueType> switchReturnTypeFromComputer(final Arguments<Computer> arguments) {
        return switchReturnType(arguments.map(Computer::getReturnTypeFromComputer));
    }

    private static Computer switchImpl(final Arguments<Computer> arguments) {
        return Computer.createTypedResultComputer(ctx -> computeMatchingCaseSwitch(arguments, ctx),
            switchReturnTypeFromComputer(arguments).getValue());
    }

    private static Computer computeMatchingCaseSwitch(final Arguments<Computer> arguments,
        final EvaluationContext ctx) {

        Computer switchExpression = arguments.get(SWITCH_EXPRESSION);
        Computer firstCase = arguments.get(SWITCH_CASE);
        var cases = arguments.getVariableArgument();

        if (switchExpression.isMissing(ctx)) {
            if (firstCase.isMissing(ctx)) {
                return arguments.get(SWITCH_VALUE);
            }
            for (int i = 0; i < cases.size() - 1; i += 2) {
                if (cases.get(i).isMissing(ctx)) {
                    return cases.get(i + 1);
                }
            }
        } else if (switchExpression instanceof StringComputer stringComputer) {
            String evaluatedSwitchValue = stringComputer.compute(ctx);

            if (!firstCase.isMissing(ctx) && firstCase instanceof StringComputer stringComputerToCompare
                && evaluatedSwitchValue.equals(stringComputerToCompare.compute(ctx))) {
                return arguments.get(SWITCH_VALUE);
            }

            for (int i = 0; i < cases.size() - 1; i += 2) {
                if (!cases.get(i).isMissing(ctx) && cases.get(i) instanceof StringComputer stringComputerToCompare
                    && evaluatedSwitchValue.equals(stringComputerToCompare.compute(ctx))) {
                    return cases.get(i + 1);
                }
            }
        } else if (switchExpression instanceof IntegerComputer integerComputer) {
            long evaluatedSwitchValue = integerComputer.compute(ctx);

            if (!firstCase.isMissing(ctx) && firstCase instanceof IntegerComputer integerComputerToCompare
                && integerComputerToCompare.compute(ctx) == evaluatedSwitchValue) {
                return arguments.get(SWITCH_VALUE);
            }

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

}
