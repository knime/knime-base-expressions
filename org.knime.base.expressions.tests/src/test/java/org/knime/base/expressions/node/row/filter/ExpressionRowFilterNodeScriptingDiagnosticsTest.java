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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.row.filter.ExpressionRowFilterNodeScriptingService.ExpressionNodeRpcService;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.scripting.editor.WorkflowControl;
import org.mockito.Mockito;

/**
 * Test getRowFilterDiagnostics of ExpressionRowFilterNodeScriptingService.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("static-method")
final class ExpressionRowFilterNodeScriptingDiagnosticsTest {

    private static DataColumnSpec col(final String name, final DataType type) {
        return new DataColumnSpecCreator(name, type).createSpec();
    }

    private static final DataTableSpec TABLE_SPECS = new DataTableSpec( //
        col("int", IntCell.TYPE), //
        col("long", LongCell.TYPE), //
        col("double", DoubleCell.TYPE), //
        col("string", StringCell.TYPE), //
        col("bool", BooleanCell.TYPE), //
        col("unsupported", DenseBitVectorCell.TYPE) //
    );

    private static final Map<String, FlowVariable> FLOW_VARIABLES = Map.of( //
        "int_flow_var", new FlowVariable("int_flow_var", IntType.INSTANCE, 42), //
        "long_flow_var", new FlowVariable("long_flow_var", LongType.INSTANCE, 42L), //
        "double_flow_var", new FlowVariable("double_flow_var", DoubleType.INSTANCE, 3.14), //
        "string_flow_var", new FlowVariable("string_flow_var", StringType.INSTANCE, "foo"), //
        "bool_flow_var", new FlowVariable("bool_flow_var", BooleanType.INSTANCE, true) //
    );

    private static WorkflowControl getWorkflowControl(final DataTableSpec tableSpecs,
        final Map<String, FlowVariable> flowVariables) {

        var flowObjectStack = Mockito.mock(FlowObjectStack.class);
        Mockito.when(flowObjectStack.getAllAvailableFlowVariables()).thenReturn(flowVariables);

        var workflowControl = Mockito.mock(WorkflowControl.class);
        Mockito.when(workflowControl.getInputSpec()).thenReturn(new DataTableSpec[]{tableSpecs});
        Mockito.when(workflowControl.getInputData()).thenReturn(new BufferedDataTable[]{null});
        Mockito.when(workflowControl.getFlowObjectStack()).thenReturn(flowObjectStack);
        return workflowControl;
    }

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
            "Columns of the type 'Bit vector' are not supported in expressions.", //
            diagnostics.get(0).message(), //
            "Expected unsupported column type error message." //
        );
    }

    @Test
    void testExpressionEvaluatesToNotBoolean() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowFilterDiagnostics("$int + 1");

        assertFalse(diagnostics.isEmpty(), "Expected error for expression evaluating to MISSING.");
        assertEquals(1, diagnostics.size(), "Expected exactly one diagnostic.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).severity(),
            "Expected error severity for expression evaluating to MISSING.");
        assertEquals( //
            "The full expression must return the value type BOOLEAN in order to "
                + "filter out rows for which the filter expression evaluates to false.", //
            diagnostics.get(0).message(), //
            "Expected MISSING evaluation error message." //
        );
    }
}
