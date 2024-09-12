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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.base.expressions.node.ExpressionDiagnostic.DiagnosticSeverity;
import org.knime.base.expressions.node.row.mapper.ExpressionRowMapperNodeScriptingService.ExpressionNodeRpcService;
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
 * Test getRowMapperDiagnostics of ExpressionRowMapperNodeScriptingService.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class ExpressionRowMapperNodeScriptingDiagnosticsTest {

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
        return new ExpressionRowMapperNodeScriptingService(null, null, workflowControl).getJsonRpcService();
    }

    @Test
    void testMultipleExpressionsNoError() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowMapperDiagnostics( //
            new String[]{"($int - 100) + $$int_flow_var", "$out - 10", "$[ROW_ID] + $[ROW_INDEX] + $[ROW_NUMBER]",
                "$['long', 10] + 2"}, //
            new String[]{"out", "out2", "out3", "out4"} //
        );

        assertEquals(4, diagnostics.size(), "Expected 4 expressions to be analyzed.");
        assertEquals(List.of(), diagnostics.get(0), "Expected no diagnostics for the first expression.");
        assertEquals(List.of(), diagnostics.get(1), "Expected no diagnostics for the second expression.");
        assertEquals(List.of(), diagnostics.get(2), "Expected no diagnostics for the third expression.");
        assertEquals(List.of(), diagnostics.get(3), "Expected no diagnostics for the fourth expression.");
    }

    @Test
    void testSyntaxError() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics =
            service.getRowMapperDiagnostics(new String[]{"($int - ) + $$int_flow_var"}, new String[]{"out"});

        assertEquals(1, diagnostics.size(), "Expected diagnostics for one expression.");
        assertFalse(diagnostics.get(0).isEmpty(), "Expected syntax error in the expression.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).get(0).severity(),
            "Expected error severity for syntax error.");
    }

    @Test
    void testMissingColumn() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowMapperDiagnostics(new String[]{"$mis_col"}, new String[]{"out"});

        assertEquals(1, diagnostics.size(), "Expected diagnostics for one expression.");
        assertFalse(diagnostics.get(0).isEmpty(), "Expected error for missing column.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).get(0).severity(),
            "Expected error severity for missing column.");
        assertEquals("No column with the name 'mis_col' is available.", diagnostics.get(0).get(0).message(),
            "Expected missing column error message.");
    }

    @Test
    void testPrematureColumnAccess() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics =
            service.getRowMapperDiagnostics(new String[]{"$out2 + $int", "100"}, new String[]{"out1", "out2"});

        assertEquals(2, diagnostics.size(), "Expected diagnostics for two expressions.");
        assertFalse(diagnostics.get(0).isEmpty(), "Expected error for premature column access.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).get(0).severity(),
            "Expected error severity for premature column access.");
        assertEquals( //
            "The column 'out2' was used before it was appended by Expression 2. Try reordering your expressions.", //
            diagnostics.get(0).get(0).message(), //
            "Expected premature column access error message." //
        );
    }

    @Test
    void testUnsupportedColumnType() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowMapperDiagnostics(new String[]{"$unsupported + 1"}, new String[]{"out"});

        assertEquals(1, diagnostics.size(), "Expected diagnostics for one expression.");
        assertFalse(diagnostics.get(0).isEmpty(), "Expected error for unsupported column type.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).get(0).severity(),
            "Expected error severity for unsupported column type.");
        assertEquals( //
            "Columns of the type 'Bit vector' are not supported in expressions.", //
            diagnostics.get(0).get(0).message(), //
            "Expected unsupported column type error message." //
        );
    }

    @Test
    void testExpressionEvaluatesToMissing() {
        var service = createService(getWorkflowControl(TABLE_SPECS, FLOW_VARIABLES));
        var diagnostics = service.getRowMapperDiagnostics(new String[]{"MISSING"}, new String[]{"out"});

        assertEquals(1, diagnostics.size(), "Expected diagnostics for one expression.");
        assertFalse(diagnostics.get(0).isEmpty(), "Expected error for expression evaluating to MISSING.");
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.get(0).get(0).severity(),
            "Expected error severity for expression evaluating to MISSING.");
        assertEquals( //
            "The full expression must not evaluate to MISSING.", //
            diagnostics.get(0).get(0).message(), //
            "Expected MISSING evaluation error message." //
        );
    }
}
