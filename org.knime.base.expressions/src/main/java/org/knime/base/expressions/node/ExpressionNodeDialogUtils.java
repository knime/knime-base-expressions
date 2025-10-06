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
 *   Sep 3, 2024 (kampmann): created
 */
package org.knime.base.expressions.node;

import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.page.Page;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.scripting.editor.GenericInitialDataBuilder.DataSupplier;
import org.knime.scripting.editor.OutputTablePreviewUtils;
import org.knime.scripting.editor.WorkflowControl;

/**
 * Utilities for the expression node dialogs.
 *
 * @author Tobias Kampmann, TNG, Germany
 */
@SuppressWarnings("restriction") // webui is not public API yet
public final class ExpressionNodeDialogUtils {

    private ExpressionNodeDialogUtils() {
        // prevent instantiation
    }

    /**
     * @param entryPoint the entry point of the expression node dialog
     * @return a page builder for the expression node dialog
     */
    public static Page createExpressionPage(final String entryPoint) {
        return Page //
            .create().fromFile().bundleClass(ExpressionNodeDialogUtils.class).basePath("js-src/dist")
            .relativeFilePath(entryPoint) //
            .addResourceDirectory("assets") //
            .addResourceDirectory("monacoeditorwork") //
            .addResources(OutputTablePreviewUtils::getCoreUIResource, "core-ui", true);
    }

    /**
     * Get a data supplier that provides the column names of the first input table.
     *
     * @param workflowControl the workflow control
     * @return the data supplier
     */
    public static DataSupplier getColumnNamesAndTypesSupplier(final WorkflowControl workflowControl) {
        return () -> Optional.ofNullable(workflowControl.getInputInfo()[0]) //
            .flatMap(info -> Optional.ofNullable(info.portSpec())) //
            .filter(DataTableSpec.class::isInstance) // can be InactiveBranchPortObjectSpec if the input is inactive
            .map(DataTableSpec.class::cast) //
            .map((spec) -> spec.stream().map(colSpec -> TypedStringChoice.fromColSpec(colSpec)).toArray()) //
            .orElseGet(() -> new TypedStringChoice[0]);
    }
}
