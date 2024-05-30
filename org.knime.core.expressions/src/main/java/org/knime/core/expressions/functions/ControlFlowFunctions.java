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
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ValueType;
import org.knime.core.expressions.EvaluationContext;

/**
 * Implementation of built-in functions that control the flow.
 *
 * @author Tobias Kampmann, TNG, Schwerte, Germany
 */

public final class ControlFlowFunctions {

    private ControlFlowFunctions() {
    }

    /** The "Control flow" category */
    public static final OperatorCategory CATEGORY = new OperatorCategory("Control", "Control flow functions");

    /** The "if*" function */
    public static final ExpressionFunction IF = functionBuilder() //
        .name("if") //
        .description(
            """
                    **Conditional expression:**  \s
                    `if(conditionA, exprIfATrue, ...<condition,exprIfTrue>, exprIfAllFalse)`  \s

                    The first expression after a fulfilled condition will be returned. \
                    If no condition evaluates to `True` the `else` case, i.e. `exprIfAllFalse` will be returned.   \
                    Conditions needs to be boolean expressions and all branch expressions must have the same type. \
                    If integer expressions are used, they will be casted to float expressions if necessary.  \s

                    Example:  \s
                    ```  \s
                    if(  \s
                    \t $customer_id < 100,  #conditionA  \s
                    \t $customer_name + " is an early customer", #exprIfATrue  \s
                    \t $customer_id < 1000, #condtionB  \s
                    \t $customer_name + " is a mid customer", #exprIfBTrueAFalse  \s
                    \t $customre_name + " is a late customer", #exprIfAllFalse  \s
                    )
                    ```
                      \s
                    The simplest case has no additional conditions (`if(condition, exprIfTrue, else)`) and this function becomes a classical *if-then-else*:  \s
                    ```
                    if(  \s
                    \t $customer_id < 100,  #condition  \s
                    \t $customer_name + " is an early customer", #exprIfTrue  \s
                    \t $customer_name + " is a late customer", #else  \s
                    )
                    ```
                    """ //
        ) //
        .keywords("conditional") //
        .category(CATEGORY.name()) //
        .args( //
            arg("condition", "Boolean condition", isBoolean()), //
            arg("exprIfTrue", "Expression if condition is `True`", isAnything()), //
            vararg("additionalConditionsAndDefaultElseCase",
                "Pairs of conditions and related expressions " + "executed when the condition evaluates to `True`. "
                    + "Last argument is the mandatory default when no conditions are fulfilled",
                isAnything()) //
        ) //
        .returnType("Result of the if expression", "Common return type of conditional expressions",
            ControlFlowFunctions::ifReturnType) //
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

    private static Computer computeMatchingBranchIf(final List<Computer> arguments, final EvaluationContext wml) {
        for (int i = 0; i < arguments.size() - 1; i += 2) {
            if (((BooleanComputer)arguments.get(i)).compute(wml)) {
                return arguments.get(i + 1);
            }
        }
        return arguments.get(arguments.size() - 1);
    }

    private static Computer ifImpl(final List<Computer> arguments) {
        return Computer.createTypedResultComputer(wml -> computeMatchingBranchIf(arguments, wml),
            ifReturnType(arguments));
    }

    /** The "switch" function: switch(value, A, exprInCaseA, B, exprInCaseB, ...) */
    public static final ExpressionFunction SWITCH = functionBuilder() //
        .name("switch") //
        .description("""
                **Switch expression:**  \s
                `switch(value, caseA, exprInCaseA, caseB, exprInCaseB, ..., optional defaultExpr)`  \s

                The switch expression allows you to execute different expressions based on the value of a given input. \
                It compares the 'value' against each provided 'case' in order until a match is found. \
                The 'value' and 'case' must have the same type (either string or boolean). \
                If a match is found, the corresponding 'exprInCase' is executed and returned. \
                If no match is found and a default expression is provided, the default case is returned. \
                Otherwise, the function returns 'MISSING'. \
                All eventually returning case expressions must have the same type. \
                If integer expressions are used, they will be casted to float expressions if necessary.  \s

                Example:  \s
                ```  \s
                switch($customer_name,  \s
                \t "Elon", 0, #caseA, exprInCaseA  \s
                \t "Mark", 1, #caseB, exprInCaseB  \s
                \t "Jeff", 2, #caseC, exprInCaseC  \s
                \t 100, #default  \s
                )
                ```
                """ //

        ) //
        .keywords("conditional") //
        .category(CATEGORY.name()) //
        .args( //
            arg("value", "Value to switch on. Only accepts string or boolean types", isOneOfBaseTypes(STRING, INTEGER)), //
            arg("firstCase", "Case to check equality against", isOneOfBaseTypes(STRING, INTEGER)), //
            arg("firstCaseExpr", "Expression to execute, when this `case` matches the `value`", isAnything()), //
            vararg("moreCasesAndDefault",
                "Optional pairs of values to check for equality of the 'value' against the provided 'cases'. "
                    + "The last argument can be an optional default expression to be applied when no match is found. "
                    + "If the default case is missing, 'MISSING' is returned "
                    + "and the switch expression returns an optional type.",
                isAnything()) //
        ) //
        .returnType("Result of the switch expression", "Common return type of case expressions",
            ControlFlowFunctions::switchReturnType) //
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
            return wml -> true;
        }
        return Computer.createTypedResultComputer(wml -> computeMatchingCaseSwitch(arguments, wml),
            returnType.baseType());
    }

    private static Computer computeMatchingCaseSwitch(final List<Computer> arguments,
        final EvaluationContext wml) { // NOSONAR

        Computer computerToSwitchOn = arguments.get(0);
        final boolean hasDefaultCase = arguments.size() % 2 == 0;

        if (computerToSwitchOn.isMissing(wml)) {
            for (int i = 1; i < arguments.size() - 1; i += 2) {
                if (arguments.get(i).isMissing(wml)) {
                    return arguments.get(i + 1);
                }
            }
        } else if (computerToSwitchOn instanceof StringComputer stringComputer) {
            String evaluatedSwitchValue = stringComputer.compute(wml);
            for (int i = 1; i < arguments.size() - 1; i += 2) {
                if (arguments.get(i).isMissing(wml)) {
                    continue;
                }
                if (arguments.get(i) instanceof StringComputer stringComputerToCompare
                    && evaluatedSwitchValue.equals(stringComputerToCompare.compute(wml))) {
                    return arguments.get(i + 1);
                }
            }
        } else if (computerToSwitchOn instanceof IntegerComputer integerComputer) {
            long evaluatedSwitchValue = integerComputer.compute(wml);
            for (int i = 1; i < arguments.size() - 1; i += 2) {
                if (arguments.get(i).isMissing(wml)) {
                    continue;
                }
                if (arguments.get(i) instanceof IntegerComputer integerComputerToCompare
                    && integerComputerToCompare.compute(wml) == evaluatedSwitchValue) {
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
