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
 *   Sep 12, 2024 (benjamin): created
 */
package org.knime.base.expressions.node.variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.variable.ExpressionFlowVariableNodeScriptingService.ExpressionNodeRpcService;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.scripting.editor.WorkflowControl;
import org.mockito.Mockito;

/**
 * Test getRowMapperDiagnostics of ExpressionRowMapperNodeScriptingService.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class ExpressionFlowVariableNodeScriptingDiagnosticsTest {

    private static final Map<String, FlowVariable> FLOW_VARIABLES = Map.of( //
        "int_flow_var", new FlowVariable("int_flow_var", IntType.INSTANCE, 42), //
        "long_flow_var", new FlowVariable("long_flow_var", LongType.INSTANCE, 42L), //
        "double_flow_var", new FlowVariable("double_flow_var", DoubleType.INSTANCE, 3.14), //
        "string_flow_var", new FlowVariable("string_flow_var", StringType.INSTANCE, "foo"), //
        "bool_flow_var", new FlowVariable("bool_flow_var", BooleanType.INSTANCE, true), //
        "unsupported_flow_var", new FlowVariable("unsupported_flow_var", IntArrayType.INSTANCE) // no value needed
    );

    private static WorkflowControl getWorkflowControl(final Map<String, FlowVariable> flowVariables) {

        var flowObjectStack = Mockito.mock(FlowObjectStack.class);
        Mockito.when(flowObjectStack.getAllAvailableFlowVariables()).thenReturn(flowVariables);

        var workflowControl = Mockito.mock(WorkflowControl.class);
        Mockito.when(workflowControl.getFlowObjectStack()).thenReturn(flowObjectStack);
        return workflowControl;
    }

    private static ExpressionNodeRpcService createService(final WorkflowControl workflowControl) {
        return new ExpressionFlowVariableNodeScriptingService(null, workflowControl).getJsonRpcService();
    }

    @Test
    void testMultipleExpressionsNoError() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var diagnostics = service.getFlowVariableDiagnostics( //
            new String[]{"($$int_flow_var - 100) + $$int_flow_var", "$$out / 5.5", "$$string_flow_var + 'bar'",
                "$$long_flow_var + 2 > 0"}, //
            new String[]{"out", "out2", "out3", "out4"} //
        );

        assertEquals(4, diagnostics.size(), "Expected 4 expressions to be analyzed.");
        assertEquals(List.of(), diagnostics.get(0).diagnostics(), "Expected no diagnostics for the first expression.");
        assertEquals(List.of(), diagnostics.get(1).diagnostics(), "Expected no diagnostics for the second expression.");
        assertEquals(List.of(), diagnostics.get(2).diagnostics(), "Expected no diagnostics for the third expression.");
        assertEquals(List.of(), diagnostics.get(3).diagnostics(), "Expected no diagnostics for the fourth expression.");

        assertEquals(ValueType.INTEGER.name(), diagnostics.get(0).returnType(), "Expected integer type.");
        assertEquals(ValueType.FLOAT.name(), diagnostics.get(1).returnType(), "Expected float type.");
        assertEquals(ValueType.STRING.name(), diagnostics.get(2).returnType(), "Expected string type.");
        assertEquals(ValueType.BOOLEAN.name(), diagnostics.get(3).returnType(), "Expected boolean type.");
    }

    @Test
    void testSyntaxError() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result = service.getFlowVariableDiagnostics(new String[]{"(10 - ) + $$int_flow_var"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected syntax error in the expression.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for syntax error.");
    }

    @Test
    void testMissingFlowVariable() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result = service.getFlowVariableDiagnostics(new String[]{"$$mis_flow"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for missing flow variable.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for missing flow variable.");
        assertEquals("No flow variable with the name 'mis_flow' is available.",
            result.get(0).diagnostics().get(0).message(), "Expected missing flow variable error message.");
    }

    @Test
    void testPrematureFlowVariableAccess() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result = service.getFlowVariableDiagnostics(new String[]{"$$out2 + $$int_flow_var", "100"},
            new String[]{"out1", "out2"});

        assertEquals(2, result.size(), "Expected diagnostics for two expressions.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for premature flow variable access.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for premature flow variable access.");
        assertEquals( //
            "The flow variable 'out2' was used before it was appended by Expression 2. Try reordering your expressions.", //
            result.get(0).diagnostics().get(0).message(), //
            "Expected premature flow variable access error message." //
        );
    }

    @Test
    void testUnsupportedFlowVariableType() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result = service.getFlowVariableDiagnostics(new String[]{"$$int_flow_var + $$unsupported_flow_var"},
            new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for unsupported flow variable type.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for unsupported flow variable type.");
        assertEquals( //
            "Flow variables of the type 'INTARRAY' are not supported.", //
            result.get(0).diagnostics().get(0).message(), //
            "Expected unsupported flow variable type error message." //
        );
    }

    @Test
    void testNullFlowVariable() {
        var vars = new java.util.HashMap<>(FLOW_VARIABLES);
        vars.put("null_var", new FlowVariable("null_var", StringType.INSTANCE, null));
        var service = createService(getWorkflowControl(vars));
        var result = service.getFlowVariableDiagnostics(new String[]{"$$null_var"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for null flow variable.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for null flow variable.");
        assertEquals("The STRING flow variable 'null_var' has the value null. This is not supported by expressions.",
            result.get(0).diagnostics().get(0).message(), "Expected null flow variable error message.");
    }

    @Test
    void testUnsupportedColumnAccess() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var diagnostics =
            service.getFlowVariableDiagnostics(new String[]{"$$int_flow_var + $not_supported"}, new String[]{"out"});

        assertEquals(1, diagnostics.size(), "Expected diagnostics for one expression.");
        assertFalse(diagnostics.get(0).diagnostics().isEmpty(), "Expected error for unsupported column access type.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).diagnostics().get(0).severity(),
            "Expected error severity for unsupported flow variable type.");
        assertEquals( //
            "No row values are available. Use '$$' to access flow variables.", //
            diagnostics.get(0).diagnostics().get(0).message(), //
            "Expected unsupported column access type error message." //
        );
    }

    @Test
    void testUnsupportedSpecialColumnAccess() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var diagnostics =
            service.getFlowVariableDiagnostics(new String[]{"$$int_flow_var + $[ROW_NUMBER]"}, new String[]{"out"});

        assertEquals(1, diagnostics.size(), "Expected diagnostics for one expression.");
        assertFalse(diagnostics.get(0).diagnostics().isEmpty(),
            "Expected error for unsupported special column access type.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).diagnostics().get(0).severity(),
            "Expected error severity for unsupported special column access type.");
        assertEquals( //
            "No rows are available.", //
            diagnostics.get(0).diagnostics().get(0).message(), //
            "Expected unsupported special column access type error message." //
        );
    }

    @Test
    void testExpressionEvaluatesToMissing() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result = service.getFlowVariableDiagnostics(new String[]{"MISSING"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for expression evaluating to MISSING.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for expression evaluating to MISSING.");
        assertEquals( //
            "The expression evaluates to a type that can be MISSING. "
                + "Use the missing coalescing operator '??' to define a default value.", //
            result.get(0).diagnostics().get(0).message(), //
            "Expected MISSING evaluation error message." //
        );
    }

    @Test
    void testAccessFlowVariableFromInvalidExpression() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result =
            service.getFlowVariableDiagnostics(new String[]{"___invalid", "$$out + 10"}, new String[]{"out", "out2"});

        assertEquals(2, result.size(), "Expected diagnostics for two expressions.");
        assertFalse(result.get(0).diagnostics().isEmpty(),
            "Expected error for accessing flow variable from invalid expression.");

        assertFalse(result.get(1).diagnostics().isEmpty(),
            "Expected error for accessing flow variable from invalid expression.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(1).diagnostics().get(0).severity(),
            "Expected error severity for accessing flow variable from invalid expression.");
        assertEquals( //
            "Expression 1 that outputs flow variable 'out' has errors. Fix Expression 1.", //
            result.get(1).diagnostics().get(0).message(), //
            "Expected invalid expression error message." //
        );
    }

    @Test
    void testAccessFlowVariableFromExpressionWithPrematureFlowVariableAccess() {
        var service = createService(getWorkflowControl(FLOW_VARIABLES));
        var result = service.getFlowVariableDiagnostics(new String[]{"$$out1 + 10", "10", "$$out + 10"},
            new String[]{"out", "out1", "out2"});

        assertEquals(3, result.size(), "Expected diagnostics for two expressions.");
        assertFalse(result.get(0).diagnostics().isEmpty(),
            "Expected error for accessing flow variable that was not yet appended.");
        assertTrue(result.get(1).diagnostics().isEmpty(), "Expected second expression to be valid.");

        assertFalse(result.get(2).diagnostics().isEmpty(),
            "Expected error for accessing flow variable from invalid expression.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(2).diagnostics().get(0).severity(),
            "Expected error severity for accessing flow variable from invalid expression.");
        assertEquals( //
            "Expression 1 that outputs flow variable 'out' has errors. Fix Expression 1.", //
            result.get(2).diagnostics().get(0).message(), //
            "Expected invalid expression error message." //
        );
    }
}
