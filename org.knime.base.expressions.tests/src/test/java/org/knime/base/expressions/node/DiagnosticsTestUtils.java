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
 *   Aug 18, 2025 (benjaminwilhelm): created
 */
package org.knime.base.expressions.node;

import java.util.Map;

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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
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
 * Shared logic for diagnostics tests.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class DiagnosticsTestUtils {

    private DiagnosticsTestUtils() {
    }

    private static DataColumnSpec col(final String name, final DataType type) {
        return new DataColumnSpecCreator(name, type).createSpec();
    }

    /** Test table specification with different data types. */
    public static final DataTableSpec TABLE_SPECS = new DataTableSpec( //
        col("int", IntCell.TYPE), //
        col("long", LongCell.TYPE), //
        col("double", DoubleCell.TYPE), //
        col("string", StringCell.TYPE), //
        col("bool", BooleanCell.TYPE), //
        col("unsupported", DenseBitVectorCell.TYPE) //
    );

    /** Test flow variables. */
    public static final Map<String, FlowVariable> FLOW_VARIABLES = Map.of( //
        "int_flow_var", new FlowVariable("int_flow_var", IntType.INSTANCE, 42), //
        "long_flow_var", new FlowVariable("long_flow_var", LongType.INSTANCE, 42L), //
        "double_flow_var", new FlowVariable("double_flow_var", DoubleType.INSTANCE, 3.14), //
        "string_flow_var", new FlowVariable("string_flow_var", StringType.INSTANCE, "foo"), //
        "bool_flow_var", new FlowVariable("bool_flow_var", BooleanType.INSTANCE, true), //
        "unsupported_flow_var", new FlowVariable("unsupported_flow_var", IntArrayType.INSTANCE) // no value needed
    );

    /**
     * Creates a mocked {@link WorkflowControl} with the given flow variables. Does not set any input data or specs.
     *
     * @param flowVariables the flow variables to return from the mocked
     * @return a mocked {@link WorkflowControl} that returns the given flow variables
     */
    public static WorkflowControl getWorkflowControl(final Map<String, FlowVariable> flowVariables) {

        var flowObjectStack = Mockito.mock(FlowObjectStack.class);
        Mockito.when(flowObjectStack.getAllAvailableFlowVariables()).thenReturn(flowVariables);

        var workflowControl = Mockito.mock(WorkflowControl.class);
        Mockito.when(workflowControl.getFlowObjectStack()).thenReturn(flowObjectStack);
        return workflowControl;
    }

    /**
     * Creates a mocked {@link WorkflowControl} with the given table specs and flow variables.
     *
     * @param tableSpecs the table specs to return from the mocked
     * @param flowVariables the flow variables to return from the mocked
     * @return a mocked {@link WorkflowControl} that returns the given table specs and flow variables
     */
    public static WorkflowControl getWorkflowControl(final PortObjectSpec tableSpecs,
        final Map<String, FlowVariable> flowVariables) {

        var flowObjectStack = Mockito.mock(FlowObjectStack.class);
        Mockito.when(flowObjectStack.getAllAvailableFlowVariables()).thenReturn(flowVariables);

        var workflowControl = Mockito.mock(WorkflowControl.class);
        Mockito.when(workflowControl.getInputSpec()).thenReturn(new PortObjectSpec[]{tableSpecs});
        Mockito.when(workflowControl.getInputData()).thenReturn(new PortObject[]{null});
        Mockito.when(workflowControl.getFlowObjectStack()).thenReturn(flowObjectStack);
        return workflowControl;
    }
}
