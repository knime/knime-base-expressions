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

import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.arg;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isAnything;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isBoolean;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isOneOfBaseTypes;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.vararg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.OperatorCategory;
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

    /** The "if*" function */
    public static final ExpressionFunction IF = functionBuilder() //
        .name("if") //
        .description(
            """
                    **Conditional expression:**  \s
                    `if(condition_1, if_1_true, ...<condition_N,if_N_true>, if_all_false)`  \s

                    The first expression after a fulfilled condition will be returned. \
                    If no condition evaluates to `true` the `else` case, i.e. `if_all_false` will be returned.   \
                    Conditions need to be boolean expressions and all `if_…_true` expressions have to return the same type. \
                    Integers will be casted automatically to floats if necessary.  \s

                    Example:  \s
                    ```  \s
                    if(  \s
                    \t $customer_id < 100,  #condition_1  \s
                    \t $customer_name + " is an early customer", #if_1_true  \s
                    \t $customer_id < 1000, #condtion_2  \s
                    \t $customer_name + " is a mid customer", #if_2_true and cond. 1 false  \s
                    \t $customre_name + " is a late customer", #if_all_false  \s
                    )
                    ```
                      \s
                    The simplest case has no additional conditions (`if(condition_1, if_1_true, if_all_false)`) and this function becomes a classical *if-then-else*:  \s
                    ```
                    if(  \s
                    \t $customer_id < 100,  #if / condition_1 \s
                    \t $customer_name + " is an early customer", #then / if_1_true  \s
                    \t $customer_name + " is a late customer", #else / if_all_false  \s
                    )
                    ```
                    """ //
        ) //
        .keywords("conditional") //
        .category(CATEGORY.name()) //
        .args( //
            arg("condition_1", "Boolean condition. See how to chain multiple conditions in description below.",
                isBoolean()), //
            arg("if_1_true", "Expression if condition 1 is `true`.", isAnything()), //
            vararg("additional_conditions, if_all_false",
                "Pairs of conditions and related expressions executed when the condition evaluates to `true`. "
                    + "Last argument is the mandatory default (\"else\" case) to be returned when no condition is fulfilled.",
                isAnything()) //
        ) //
        .returnType("Result of the expression belonging to the first matched condition",
            "Common return type of conditional expressions", ControlFlowFunctions::ifReturnType) //
        .impl(ControlFlowFunctions::ifImpl) //
        .build();

    private static ValueType ifReturnType(final ValueType[] arguments) {
        var ifStatementHasNoElseCase = arguments.length % 2 != 1;

        if (ifStatementHasNoElseCase || arguments.length < 3) {
            // TODO(AP-22303) better message about what arguments are supported
            return null;
        }

        ArrayList<ValueType> branchExpressions = new ArrayList<>();

        for (int i = 1; i < arguments.length - 1; i += 2) {
            branchExpressions.add(arguments[i]);
        }
        branchExpressions.add(arguments[arguments.length - 1]);

        return calculateReturnTypeFromBranchExpressionValues(branchExpressions);

    }

    private static ValueType ifReturnType(final List<Computer> arguments) {
        return ifReturnType(arguments.stream().map(Computer::getReturnTypeFromComputer).toArray(ValueType[]::new));
    }

    private static Computer computeMatchingBranchIf(final List<Computer> arguments, final EvaluationContext ctx) {
        for (int i = 0; i < arguments.size() - 1; i += 2) {
            if (((BooleanComputer)arguments.get(i)).compute(ctx)) {
                return arguments.get(i + 1);
            }
        }
        return arguments.get(arguments.size() - 1);
    }

    private static Computer ifImpl(final List<Computer> arguments) {
        return Computer.createTypedResultComputer(ctx -> computeMatchingBranchIf(arguments, ctx),
            ifReturnType(arguments));
    }

    /** The "switch" function: switch(value, A, exprInCaseA, B, exprInCaseB, ...) */
    public static final ExpressionFunction SWITCH = functionBuilder() //
        .name("switch") //
        .description("""
                **Switch expression:**  \s
                `switch(value, case_1, if_1_matched, case_2, if_2_matched,..., optional if_none_matched)`  \s

                The switch function executes different expressions based on the value of a given input. \
                It compares the `value` against each provided `case` in order until a match is found. \
                The `value` and `case` must have the same type (either string or boolean). \
                If a match is found, the corresponding `if_…_matched` is executed and returned. \
                If no match is found the `if_none_matched` expression is returned, if provided. \
                Otherwise, the function returns `MISSING`. \
                All `if_…_matched` expressions have to return the same type. \
                If integer expressions are used, they will be casted to float expressions if necessary.  \s

                Example:  \s
                ```  \s
                switch($customer_name,  \s
                \t "Elon", 0, #case_1, if_1_matched  \s
                \t "Mark", 1, #case_2, if_2_matched  \s
                \t "Jeff", 2, #case_3, if_3_matched  \s
                \t 100, #if_none_matched / default  \s
                )
                ```
                """ //

        ) //
        .keywords("conditional") //
        .category(CATEGORY.name()) //
        .args( //
            arg("value", "Value to switch on. Only accepts string or boolean types.",
                isOneOfBaseTypes(STRING, INTEGER)), //
            arg("case_1", "Case to check equality against `value`.", isOneOfBaseTypes(STRING, INTEGER)), //
            arg("if_1_matched", "Expression to execute, when `case_1` matches the `value`.", isAnything()), //
            vararg("additional_cases, if_none_matched",
                "Optional pairs of `case_…` values to check for equality with the `value` and `if_…_matched` expressions. "
                    + "The last argument can be an optional `if_none_matched` expression to be applied as a default when no match is found. "
                    + "If no `if_none_matched` expression is provided, the default is `MISSING`.",
                isAnything()) //
        ) //
        .returnType("Result of the expression belonging to the first matched case",
            "Common return type of case expressions", ControlFlowFunctions::switchReturnType) //
        .impl(ControlFlowFunctions::switchImpl) //
        .build();

    private static ValueType switchReturnType(final ValueType[] arguments) {
        if (arguments.length < 3) {
            return null;
        }

        ValueType switchValue = arguments[0];
        if (switchValue.baseType() != STRING && switchValue.baseType() != INTEGER && switchValue != MISSING) {
            return null;
        }
        for (int i = 1; i < arguments.length - 1; i += 2) {
            if (arguments[i] == MISSING) {
                continue;
            }
            if (arguments[i].baseType() != switchValue.baseType()) {
                return null;
            }
        }

        boolean hasDefaultCase = arguments.length % 2 == 0;
        ArrayList<ValueType> branchExpressions = new ArrayList<>();

        for (int i = 2; i < arguments.length - (hasDefaultCase ? 1 : 0); i += 2) {
            branchExpressions.add(arguments[i]);
        }

        if (hasDefaultCase) {
            branchExpressions.add(arguments[arguments.length - 1]);
        }

        var returnType = calculateReturnTypeFromBranchExpressionValues(branchExpressions);

        if (returnType == null) {
            return null;
        }
        return hasDefaultCase ? returnType : returnType.optionalType();
    }

    private static ValueType switchReturnType(final List<Computer> arguments) {
        return switchReturnType(arguments.stream().map(Computer::getReturnTypeFromComputer).toArray(ValueType[]::new));
    }

    private static Computer switchImpl(final List<Computer> arguments) {
        var returnType = switchReturnType(arguments);
        if (returnType == null) {
            return ctx -> true;
        }
        return Computer.createTypedResultComputer(ctx -> computeMatchingCaseSwitch(arguments, ctx),
            returnType.baseType());
    }

    private static Computer computeMatchingCaseSwitch(final List<Computer> arguments,
        final EvaluationContext ctx) { // NOSONAR

        Computer computerToSwitchOn = arguments.get(0);
        final boolean hasDefaultCase = arguments.size() % 2 == 0;

        if (computerToSwitchOn.isMissing(ctx)) {
            for (int i = 1; i < arguments.size() - 1; i += 2) {
                if (arguments.get(i).isMissing(ctx)) {
                    return arguments.get(i + 1);
                }
            }
        } else if (computerToSwitchOn instanceof StringComputer stringComputer) {
            String evaluatedSwitchValue = stringComputer.compute(ctx);
            for (int i = 1; i < arguments.size() - 1; i += 2) {
                if (arguments.get(i).isMissing(ctx)) {
                    continue;
                }
                if (arguments.get(i) instanceof StringComputer stringComputerToCompare
                    && evaluatedSwitchValue.equals(stringComputerToCompare.compute(ctx))) {
                    return arguments.get(i + 1);
                }
            }
        } else if (computerToSwitchOn instanceof IntegerComputer integerComputer) {
            long evaluatedSwitchValue = integerComputer.compute(ctx);
            for (int i = 1; i < arguments.size() - 1; i += 2) {
                if (arguments.get(i).isMissing(ctx)) {
                    continue;
                }
                if (arguments.get(i) instanceof IntegerComputer integerComputerToCompare
                    && integerComputerToCompare.compute(ctx) == evaluatedSwitchValue) {
                    return arguments.get(i + 1);
                }
            }
        }

        return hasDefaultCase ? arguments.get(arguments.size() - 1) : w -> true;
    }

    private static ValueType calculateReturnTypeFromBranchExpressionValues(final List<ValueType> expressions) {
        if (expressions.stream().allMatch(type -> type == MISSING)) {
            // TODO(AP-22303) better message about what arguments are supported
            return null;
        }

        if (!expressions.stream().allMatch(isCompatibleTo(expressions.get(0)))) {
            // TODO(AP-22303) better message about what arguments are supported
            return null;
        }

        ValueType commonReturnBaseType = expressions.stream().filter(type -> type != MISSING).findFirst().orElse(MISSING);
        if (expressions.stream().anyMatch(type -> type.baseType() == FLOAT)) {
            commonReturnBaseType = ValueType.FLOAT;
        }

        if (expressions.stream().anyMatch(ValueType::isOptional)) {
            return commonReturnBaseType.optionalType();
        } else {
            return commonReturnBaseType;
        }
    }

    private static Predicate<ValueType> isCompatibleTo(final ValueType typeToCompare) {
        return typeToCheck -> ValueType.hasSameBaseType(typeToCompare, typeToCheck)
            || (ValueType.isNumericOrOpt(typeToCompare) && ValueType.isNumericOrOpt(typeToCheck))
            || typeToCheck == MISSING
            || typeToCompare == MISSING;
    }

}
