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
package org.knime.base.expressions.node.row.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.base.expressions.node.DiagnosticsTestUtils.FLOW_VARIABLES;
import static org.knime.base.expressions.node.DiagnosticsTestUtils.TABLE_SPECS;
import static org.knime.base.expressions.node.DiagnosticsTestUtils.getWorkflowControl;

import org.junit.jupiter.api.Test;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.row.filter.ExpressionRowFilterNodeScriptingService.ExpressionNodeRpcService;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl;

/**
 * Test getRowFilterDiagnostics of ExpressionRowFilterNodeScriptingService.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("static-method")
final class ExpressionRowFilterNodeScriptingDiagnosticsTest {

    private static ExpressionNodeRpcService createService(final WorkflowControl workflowControl) {
        return new ExpressionRowFilterNodeScriptingService(workflowControl).getJsonRpcService();
    }

    @Test
    void testNoError() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("($int - 100) + $$int_flow_var > 0");

        assertEquals(0, diagnostics.size(), "Expect no diagnostics.");
    }

    @Test
    void testNoInput() {
        var service = createService(getWorkflowControl(null, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("($int - 100) + $$int_flow_var");

        assertEquals(1, diagnostics.size(), "Expected 1 expressions to be analyzed.");
        assertTrue(diagnostics.get(0).message().contains("No input"),
            "Expected \"No input...\" error message, got \"" + diagnostics.get(0).message() + "\".");
    }

    @Test
    void testInactiveInput() {
        var service = createService(getWorkflowControl(InactiveBranchPortObjectSpec.INSTANCE, FLOW_VARIABLES));
        var result = service.getRowFilterDiagnostics("$int > 1");
        assertEquals(1, result.size(), "Expected one error for inactive branches.");
        assertEquals(DiagnosticSeverity.ERROR, result.get(0).severity(),
            "Expected error severity for inactive branches.");
        assertEquals("The input connection is inactive. Connect an active table.", result.get(0).message(),
            "Expected inactive branches error message.");
    }

    @Test
    void testSyntaxError() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("($int - ) + $$int_flow_var");

        assertFalse(diagnostics.isEmpty(), "Expected error in the expression.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertTrue(diagnostics.get(0).message().contains("mismatched input"),
            "Expected syntax error in the expression.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for syntax error.");
    }

    @Test
    void testMissingColumn() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("$mis_col");

        assertFalse(diagnostics.isEmpty(), "Expected error for missing column.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for missing column.");
        assertEquals("No column with the name 'mis_col' is available.", diagnostics.get(0).message(),
            "Expected missing column error message.");
    }

    @Test
    void testUnsupportedColumnType() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("$unsupported + 1");

        assertFalse(diagnostics.isEmpty(), "Expected error for unsupported column type.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for unsupported column type.");
        assertEquals( //
            "Columns of the type '" + DenseBitVectorCell.TYPE.getName() + "' are not supported in expressions.", //
            diagnostics.get(0).message(), //
            "Expected unsupported column type error message." //
        );
    }

    @Test
    void testNullFlowVariable() {
        var vars = new java.util.HashMap<>(FLOW_VARIABLES);
        vars.put("null_var", new FlowVariable("null_var", StringType.INSTANCE, null));
        var service = createService(getWorkflowControl(TABLE_SPECS, vars));
        var diagnostics = service.getRowFilterDiagnostics("$$null_var = 'x'");

        assertFalse(diagnostics.isEmpty(), "Expected error for null flow variable.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for null flow variable.");
        assertEquals("The STRING flow variable 'null_var' has the value null. This is not supported by expressions.",
            diagnostics.get(0).message(), "Expected null flow variable error message.");
    }

    @Test
    void testExpressionEvaluatesToInt() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("$int + 1");

        assertFalse(diagnostics.isEmpty(), "Expected error for expression evaluating to MISSING.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for expression evaluating to MISSING.");
        assertEquals( //
            "The expression evaluates to INTEGER| MISSING. "
                + "It should evaluate to BOOLEAN in order to filter out rows for which the filter expression "
                + "evaluates to false.", //
            diagnostics.get(0).message(), //
            "Expected MISSING evaluation error message." //
        );
    }

    @Test
    void testExpressionEvaluatesToOptBoolean() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("if(TRUE, TRUE, MISSING)");

        assertFalse(diagnostics.isEmpty(), "Expected error for expression evaluating to MISSING.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for expression evaluating to MISSING.");
        assertEquals( //
            "The expression evaluates to BOOLEAN | MISSING. "
                + "Use the missing coalescing operator '??' to define if rows that evaluate to MISSING "
                + "should be included or excluded.", //
            diagnostics.get(0).message(), //
            "Expected MISSING evaluation error message." //
        );
    }
}
