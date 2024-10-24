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
package org.knime.base.expressions.node.row.filter;

import static org.knime.core.webui.node.view.table.RowHeightPersistorUtil.LEGACY_CUSTOM_ROW_HEIGHT_COMPACT;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.base.expressions.node.ExpressionNodeDialogUtils;
import org.knime.base.expressions.node.ExpressionNodeScriptingInputOutputModelUtils;
import org.knime.base.expressions.node.FunctionCatalogData;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettingsSerializer;
import org.knime.core.webui.node.dialog.defaultdialog.setting.selection.SelectionMode;
import org.knime.core.webui.node.view.table.TableViewUtil;
import org.knime.core.webui.node.view.table.TableViewViewSettings;
import org.knime.core.webui.node.view.table.TableViewViewSettings.RowHeightMode;
import org.knime.core.webui.node.view.table.TableViewViewSettings.VerticalPaddingMode;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.node.view.table.data.TableViewInitialDataImpl;
import org.knime.core.webui.page.Page;
import org.knime.scripting.editor.GenericInitialDataBuilder;
import org.knime.scripting.editor.ScriptingNodeSettingsService;
import org.knime.scripting.editor.WorkflowControl;

// TODO(AP-23188): find an abstraction for all node dialogs of different expression nodes

/**
 * The node dialog implementation of the Expression filter node.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction")
final class ExpressionRowFilterNodeDialog implements NodeDialog {

    @Override
    public Page getPage() {
        return ExpressionNodeDialogUtils.expressionPageBuilder("row-filter.html") //
            .addResource( //
                ExpressionNodeDialogUtils::getTableViewResource, //
                ExpressionNodeDialogUtils.TABLE_VIEW_RESOURCE) //
            .build();
    }

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Set.of(SettingsType.MODEL);
    }

    @Override
    public NodeSettingsService getNodeSettingsService() {
        var workflowControl = new WorkflowControl(NodeContext.getContext().getNodeContainer());

        var initialDataBuilder = GenericInitialDataBuilder.createDefaultInitialDataBuilder(NodeContext.getContext()) //
            .addDataSupplier("inputObjects", //
                () -> ExpressionNodeScriptingInputOutputModelUtils.getTableInputObjects( //
                    workflowControl.getInputInfo() //
                )) //
            .addDataSupplier("flowVariables", () -> {
                var flowVariables = Optional.ofNullable(workflowControl.getFlowObjectStack()) //
                    .map(stack -> stack.getAllAvailableFlowVariables().values()) //
                    .orElseGet(List::of);
                return ExpressionNodeScriptingInputOutputModelUtils.getFlowVariableInputs(flowVariables);
            }) //
            .addDataSupplier("outputObjects", ExpressionNodeScriptingInputOutputModelUtils::getOutputObjects) //
            .addDataSupplier("functionCatalog", () -> FunctionCatalogData.BUILT_IN) //
            .addDataSupplier("columnNames", ExpressionNodeDialogUtils.getColumnNamesSupplier(workflowControl));

        return new ScriptingNodeSettingsService( //
            ExpressionRowFilterSettings::new, //
            initialDataBuilder //
        );
    }

    /**
     *
     */
    public static class OutputPreviewTableInitialDataRpcSupplier {

        AtomicReference<BufferedDataTable> m_table;

        private TableViewDataService m_tableViewDataService;

        OutputPreviewTableInitialDataRpcSupplier(final AtomicReference<BufferedDataTable> table,
            final TableViewDataService tableViewDataService) {
            this.m_table = table;
            m_tableViewDataService = tableViewDataService;
        }

        /**
         * @return the initial data for the output table
         */
        public String getInitialData() {
            if (m_table.get() == null) {
                // This can happen if no input table is connected to the node
                return null;
            }

            var tab = new TableViewViewSettings(m_table.get().getSpec());
            tab.m_title = null;
            tab.m_enableGlobalSearch = false;
            tab.m_showTableSize = false;
            tab.m_enablePagination = false;
            tab.m_rowHeightMode = RowHeightMode.CUSTOM;
            tab.m_verticalPaddingMode = VerticalPaddingMode.COMPACT;
            tab.m_customRowHeight = LEGACY_CUSTOM_ROW_HEIGHT_COMPACT;
            tab.m_selectionMode = SelectionMode.OFF;
            tab.m_showOnlySelectedRowsConfigurable = false;
            tab.m_enableColumnSearch = false;
            try {
                return new DefaultNodeSettingsSerializer<>().serialize(
                    Map.of("result", new TableViewInitialDataImpl(tab, m_table::get, m_tableViewDataService)));
            } catch (IOException e) {
                NodeLogger.getLogger(this.getClass()).error("Failed to serialize the initial data", e);
                return null;
            }
        }
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {

        final AtomicReference<BufferedDataTable> previewTable = new AtomicReference<>();
        var tableId = "previewTable.dummyId";
        var outputPreviewTableDataService = TableViewUtil.createTableViewDataService(previewTable::get, null, tableId);

        Runnable cleanUpTableViewDataService =
            () -> TableViewUtil.deactivateTableViewDataService(outputPreviewTableDataService, tableId);

        var scriptingService = new ExpressionRowFilterNodeScriptingService(previewTable, cleanUpTableViewDataService);

        return Optional.of(RpcDataService.builder() //
            .addService("ScriptingService", scriptingService.getJsonRpcService()) //
            .addService(OutputPreviewTableInitialDataRpcSupplier.class.getSimpleName(), //
                new OutputPreviewTableInitialDataRpcSupplier(previewTable, outputPreviewTableDataService)) //
            .addService(TableViewDataService.class.getSimpleName(), outputPreviewTableDataService) //
            .onDeactivate(scriptingService::onDeactivate) //
            .onDeactivate(cleanUpTableViewDataService) //
            .build()); //
    }

    @Override
    public boolean canBeEnlarged() {
        return true;
    }
}
