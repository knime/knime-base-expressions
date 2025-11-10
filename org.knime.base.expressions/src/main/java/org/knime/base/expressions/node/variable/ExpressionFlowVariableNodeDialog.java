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
 *   Aug 14, 2024 (tobias): created
 */
package org.knime.base.expressions.node.variable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.knime.base.expressions.node.ExpressionNodeDialogUtils;
import org.knime.base.expressions.node.ExpressionNodeScriptingInputOutputModelUtils;
import org.knime.base.expressions.node.FunctionCatalogData;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.scripting.GenericInitialDataBuilder;
import org.knime.core.webui.node.dialog.scripting.ScriptingNodeSettingsService;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl;
import org.knime.core.webui.node.view.flowvariable.FlowVariableViewUtil;
import org.knime.core.webui.node.view.table.TableViewUtil;
import org.knime.core.webui.node.view.table.TableViewViewSettings;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.page.Page;

/**
 * The node dialog implementation of the Expression filter node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionFlowVariableNodeDialog implements NodeDialog {

    @Override
    public Page getPage() {
        return ExpressionNodeDialogUtils.createExpressionPage("flow-variable.html");
    }

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Set.of(SettingsType.MODEL);
    }

    @Override
    public NodeSettingsService getNodeSettingsService() {
        var workflowControl = new WorkflowControl(NodeContext.getContext().getNodeContainer());

        var initialDataBuilder = GenericInitialDataBuilder.createDefaultInitialDataBuilder(NodeContext.getContext()) //
            .addDataSupplier("inputObjects", List::of) //
            .addDataSupplier("flowVariables", () -> {
                var flowVariables = Optional.ofNullable(workflowControl.getFlowObjectStack()) //
                    .map(stack -> stack.getAllAvailableFlowVariables().values()) //
                    .orElseGet(List::of);
                return ExpressionNodeScriptingInputOutputModelUtils.getFlowVariableInputs(flowVariables);
            }) //
            .addDataSupplier("functionCatalog", () -> FunctionCatalogData.BUILT_IN_NO_AGGREGATIONS);

        return new ScriptingNodeSettingsService( //
            ExpressionFlowVariableSettings::new, //
            initialDataBuilder //
        );
    }

    public static final class FlowVariablePreviewInitialDataSupplier {

        final AtomicReference<List<FlowVariable>> m_flowVariables;

        private FlowVariablePreviewInitialDataSupplier(final AtomicReference<List<FlowVariable>> flowVariables) {
            m_flowVariables = flowVariables;
        }

        public String getInitialData() {
            final var settingsSupplier = getSettingsSupplier();
            final var bufferedTableSupplier = getBufferedTableSupplier(m_flowVariables.get());
            return TableViewUtil //
                .createInitialDataService(settingsSupplier, bufferedTableSupplier, null, "flowvariableview") //
                .getInitialData();
        }

        /**
         * {@link TableViewViewSettings#m_enablePagination} needs to be enabled to disable lazy fetching of data. There
         * are missing handlers in the {@link DataService}. {@link RpcDataService} does not support named and unnamed
         * handlers at the same time. Look {@link TableViewDataService} for all missing methods that should be in the
         * {@link DataService}.
         */
        private static Supplier<TableViewViewSettings> getSettingsSupplier() {
            var flowVariableViewSettings = FlowVariableViewUtil.getSettings();
            flowVariableViewSettings.m_enablePagination = true;
            return () -> flowVariableViewSettings;
        }

        private static Supplier<BufferedDataTable>
            getBufferedTableSupplier(final Collection<FlowVariable> flowVariables) {
            return () -> FlowVariableViewUtil.getBufferedTable(flowVariables);
        }

    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {

        AtomicReference<List<FlowVariable>> flowVariablesReference = new AtomicReference<>(List.of());

        var scriptingService = new ExpressionFlowVariableNodeScriptingService(flowVariablesReference);

        return Optional.of(RpcDataService.builder() //
            .addService("ScriptingService", scriptingService.getJsonRpcService()) //
            .addService(FlowVariablePreviewInitialDataSupplier.class.getSimpleName(), //
                new FlowVariablePreviewInitialDataSupplier(flowVariablesReference)) //
            .onDeactivate(scriptingService::onDeactivate) //
            .build()); //
    }

    @Override
    public boolean canBeEnlarged() {
        return true;
    }

}
