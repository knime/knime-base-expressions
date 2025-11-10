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
package org.knime.base.expressions.node.row.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.base.expressions.node.DiagnosticsTestUtils.FLOW_VARIABLES;
import static org.knime.base.expressions.node.DiagnosticsTestUtils.TABLE_SPECS;
import static org.knime.base.expressions.node.DiagnosticsTestUtils.getWorkflowControl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.base.expressions.ColumnOutputUtils;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.row.mapper.ExpressionRowMapperNodeScriptingService.ExpressionNodeRpcService;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.expressions.ValueType;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel.InputOutputModelSubItemType;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl;

/**
 * Test getRowMapperDiagnostics of ExpressionRowMapperNodeScriptingService.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class ExpressionRowMapperNodeScriptingDiagnosticsTest {

    private static ExpressionNodeRpcService createService(final WorkflowControl workflowControl) {
        return new ExpressionRowMapperNodeScriptingService(workflowControl).getJsonRpcService();
    }

    private static InputOutputModelSubItemType createInOutModelSubItemType(final ValueType valueType) {
        final var colSpec = ColumnOutputUtils.valueTypeToDataColumnSpec(valueType, "temp");
        return InputOutputModelSubItemType.fromColSpec(colSpec, valueType.name());
    }

    @Test
    void testMultipleExpressionsNoError() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowMapperDiagnostics( //
            new String[]{"($int - 100) + $$int_flow_var", "$out / 5.5", "$[ROW_ID] + $[ROW_INDEX] + $[ROW_NUMBER]",
                "$['long', 10] + 2 > 0"}, //
            new String[]{"out", "out2", "out3", "out4"} //
        );

        assertEquals(4, diagnostics.size(), "Expected 4 expressions to be analyzed.");
        assertEquals(List.of(), diagnostics.get(0).diagnostics(), "Expected no diagnostics for the first expression.");
        assertEquals(List.of(), diagnostics.get(1).diagnostics(), "Expected no diagnostics for the second expression.");
        assertEquals(List.of(), diagnostics.get(2).diagnostics(), "Expected no diagnostics for the third expression.");
        assertEquals(List.of(), diagnostics.get(3).diagnostics(), "Expected no diagnostics for the fourth expression.");

        assertEquals(createInOutModelSubItemType(ValueType.INTEGER), diagnostics.get(0).returnType(),
            "Expected integer type.");
        assertEquals(createInOutModelSubItemType(ValueType.FLOAT), diagnostics.get(1).returnType(),
            "Expected float type.");
        assertEquals(createInOutModelSubItemType(ValueType.STRING), diagnostics.get(2).returnType(),
            "Expected string type.");
        assertEquals(createInOutModelSubItemType(ValueType.BOOLEAN), diagnostics.get(3).returnType(),
            "Expected boolean type.");
    }

    @Test
    void testNoInput() {
        var service = createService(getWorkflowControl(null, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics( //
            new String[]{"($int - 100) + $$int_flow_var"}, //
            new String[]{"out"} //
        );

        assertEquals(1, result.size(), "Expected 1 expressions to be analyzed.");
        assertTrue(result.get(0).diagnostics().get(0).message().contains("No input"),
            "Expected \"No input...\" error message, got \"" + result.get(0).diagnostics().get(0).message() + "\".");
    }

    @Test
    void testInactiveInput() {
        var service = createService(getWorkflowControl(InactiveBranchPortObjectSpec.INSTANCE, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"$int + 1"}, new String[]{"out"});
        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertEquals(InputOutputModelSubItemType.fromDisplayName("UNKNOWN"), result.get(0).returnType(),
            "Expected no return type for inactive branches.");
        assertEquals(1, result.get(0).diagnostics().size(), "Expected one error for inactive branches.");
        var diagnostic = result.get(0).diagnostics().get(0);
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity(), "Expected error severity for inactive branches.");
        assertEquals("The input connection is inactive. Connect an active table.", diagnostic.message(),
            "Expected inactive branches error message.");
    }

    @Test
    void testSyntaxError() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"($int - ) + $$int_flow_var"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected syntax error in the expression.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for syntax error.");
    }

    @Test
    void testMissingColumn() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"$mis_col"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for missing column.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for missing column.");
        assertEquals("No column with the name 'mis_col' is available.", result.get(0).diagnostics().get(0).message(),
            "Expected missing column error message.");
    }

    @Test
    void testPrematureColumnAccess() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"$out2 + $int", "100"}, new String[]{"out1", "out2"});

        assertEquals(2, result.size(), "Expected diagnostics for two expressions.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for premature column access.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for premature column access.");
        assertEquals( //
            "The column 'out2' was used before it was appended by Expression 2. Try reordering your expressions.", //
            result.get(0).diagnostics().get(0).message(), //
            "Expected premature column access error message." //
        );
    }

    @Test
    void testUnsupportedColumnType() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"$unsupported + 1"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for unsupported column type.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for unsupported column type.");
        assertEquals( //
            "Columns of the type '" + DenseBitVectorCell.TYPE.getName() + "' are not supported in expressions.", //
            result.get(0).diagnostics().get(0).message(), //
            "Expected unsupported column type error message." //
        );
    }

    @Test
    void testNullFlowVariable() {
        var vars = new java.util.HashMap<>(FLOW_VARIABLES);
        vars.put("null_var", new FlowVariable("null_var", StringType.INSTANCE, null));
        var service = createService(getWorkflowControl(TABLE_SPECS, vars));
        var result = service.getRowMapperDiagnostics(new String[]{"$$null_var"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for null flow variable.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for null flow variable.");
        assertEquals("The STRING flow variable 'null_var' has the value null. This is not supported by expressions.",
            result.get(0).diagnostics().get(0).message(), "Expected null flow variable error message.");
    }

    @Test
    void testExpressionEvaluatesToMissing() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"MISSING"}, new String[]{"out"});

        assertEquals(1, result.size(), "Expected diagnostics for one expression.");
        assertFalse(result.get(0).diagnostics().isEmpty(), "Expected error for expression evaluating to MISSING.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).diagnostics().get(0).severity(),
            "Expected error severity for expression evaluating to MISSING.");
        assertEquals( //
            "The full expression must not evaluate to MISSING.", //
            result.get(0).diagnostics().get(0).message(), //
            "Expected MISSING evaluation error message." //
        );
    }

    @Test
    void testAccessColumnFromInvalidExpression() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result =
            service.getRowMapperDiagnostics(new String[]{"___invalid", "$out + 10"}, new String[]{"out", "out2"});

        assertEquals(2, result.size(), "Expected diagnostics for two expressions.");
        assertFalse(result.get(0).diagnostics().isEmpty(),
            "Expected error for accessing column from invalid expression.");

        assertFalse(result.get(1).diagnostics().isEmpty(),
            "Expected error for accessing column from invalid expression.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(1).diagnostics().get(0).severity(),
            "Expected error severity for accessing column from invalid expression.");
        assertEquals( //
            "Expression 1 that outputs column 'out' has errors. Fix Expression 1.", //
            result.get(1).diagnostics().get(0).message(), //
            "Expected invalid expression error message." //
        );
    }

    @Test
    void testAccessColumnFromExpressionWithPrematureColumnAccess() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var result = service.getRowMapperDiagnostics(new String[]{"$out1 + 10", "10", "$out + 10"},
            new String[]{"out", "out1", "out2"});

        assertEquals(3, result.size(), "Expected diagnostics for two expressions.");
        assertFalse(result.get(0).diagnostics().isEmpty(),
            "Expected error for accessing column that was not yet appended.");
        assertTrue(result.get(1).diagnostics().isEmpty(), "Expected second expression to be valid.");

        assertFalse(result.get(2).diagnostics().isEmpty(),
            "Expected error for accessing column from invalid expression.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(2).diagnostics().get(0).severity(),
            "Expected error severity for accessing column from invalid expression.");
        assertEquals( //
            "Expression 1 that outputs column 'out' has errors. Fix Expression 1.", //
            result.get(2).diagnostics().get(0).message(), //
            "Expected invalid expression error message." //
        );
    }
}
