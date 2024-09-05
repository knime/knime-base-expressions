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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.CoreUIPlugin;
import org.knime.core.webui.page.FromFilePageBuilder;
import org.knime.core.webui.page.Page;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

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
     * The resource name for the table view.
     */
    public static final String TABLE_VIEW_RESOURCE = "TableView.js";

    /**
     * The resource name for the flow variable view.
     */
    public static final String FLOW_VARIABLE_VIEW_RESOURCE = "FlowVariableView.js";

    /**
     * @param entryPoint the entry point of the expression node dialog
     * @return a page builder for the expression node dialog
     */
    public static FromFilePageBuilder expressionPageBuilder(final String entryPoint) {
        return Page //
            .builder(ExpressionNodeDialogUtils.class, "js-src/dist", entryPoint) //
            .addResourceDirectory("assets") //
            .addResourceDirectory("monacoeditorwork");
    }

    /**
     * Convenience function to directly get the table view resource.
     *
     * @return an input stream to the table view resource
     */
    public static InputStream getTableViewResource() {
        return getCoreUIResource(TABLE_VIEW_RESOURCE);
    }

    /**
     * Convenience function to directly get the flow variable view resource.
     *
     * @return an input stream to the flow variable view resource
     */
    public static InputStream getFlowVariableViewResource() {
        return getCoreUIResource(FLOW_VARIABLE_VIEW_RESOURCE);
    }

    /** Get a resource from the js-src/dist folder of the core UI plugin. */
    private static InputStream getCoreUIResource(final String nameOfResource) {
        try {
            return Files.newInputStream(ExpressionNodeDialogUtils.getAbsoluteBasePath(CoreUIPlugin.class, null,
                "js-src/dist/" + nameOfResource));
        } catch (IOException e) {
            NodeLogger.getLogger(ExpressionNodeDialogUtils.class).error("Failed to load " + nameOfResource, e);
            return null;
        }
    }

    private static Path getAbsoluteBasePath(final Class<?> clazz, final String bundleID, final String baseDir) {
        if (clazz != null) {
            return getAbsoluteBasePath(FrameworkUtil.getBundle(clazz), baseDir);
        } else {
            return getAbsoluteBasePath(Platform.getBundle(bundleID), baseDir);
        }
    }

    private static Path getAbsoluteBasePath(final Bundle bundle, final String baseDir) {
        var bundleUrl = bundle.getEntry(".");
        try {
            // must not use url.toURI() -- FileLocator leaves spaces in the URL (see eclipse bug 145096)
            // -- taken from TableauHyperActivator.java line 158
            var url = FileLocator.toFileURL(bundleUrl);
            return Paths.get(new URI(url.getProtocol(), url.getFile(), null)).resolve(baseDir).normalize();
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Failed to resolve the directory " + baseDir, ex);
        }
    }

}
