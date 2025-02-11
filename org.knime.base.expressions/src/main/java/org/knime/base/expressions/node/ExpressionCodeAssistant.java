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
 *   16 Aug 2023 (chaubold): created
 */
package org.knime.base.expressions.node;

import java.io.IOException;

import org.eclipse.core.runtime.Platform;
import org.knime.scripting.editor.InputOutputModel;
import org.knime.scripting.editor.InputOutputModelNameAndTypeUtils;
import org.knime.scripting.editor.InputOutputModelNameAndTypeUtils.NameAndType;
import org.knime.scripting.editor.ai.HubConnection;

/**
 * This class provides methods to generate expressions with the help of AI
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class ExpressionCodeAssistant {
    private ExpressionCodeAssistant() {
    }

    private static final String EXPRESSION_CORE_PLUGIN_VERSION = getExpressionBundleVersion();

    private static String getExpressionBundleVersion() {
        var version = Platform.getBundle("org.knime.core.expressions").getVersion();
        // NB: version.toString() contains the qualifier which we do not want
        return version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
    }

    /**
     * The type of expression for which we want AI code assistance
     */
    public enum ExpressionType {
            /**
             * The normal "row mapper" expression node
             */
            ROW("knime_expression"), //
            /**
             * The expression row filter
             */
            FILTER("knime_expression_filter"), //
            /**
             * The expression node working on variables only
             */
            VARIABLE("knime_expression_variable");

        final String m_endpoint;

        ExpressionType(final String endpoint) {
            m_endpoint = endpoint;
        }
    }

    /**
     * Query the AI to generate expressions for the given prompt
     *
     * @param type The type of expression to generate, allowing to specialize for the node type (row, filter or
     *            variable)
     * @param userPrompt The user prompt to instruct the AI what to do
     * @param oldCode The current code. Should not be null, but may be an empty string.
     * @param inputModels The input models that serve as context for the AI prompt
     *
     * @return The newly generated code
     * @throws IOException
     */
    public static String generateCode( //
        final ExpressionType type, //
        final String userPrompt, //
        final String oldCode, //
        final InputOutputModel[] inputModels //
    ) throws IOException {
        return generateCode(//
            type, //
            userPrompt, //
            oldCode, //
            new NameAndType[][]{InputOutputModelNameAndTypeUtils.getAllSupportedTableColumns(inputModels)}, //
            InputOutputModelNameAndTypeUtils.getSupportedFlowVariables(inputModels) //
        );
    }

    /**
     * Query the AI to generate expressions for the given prompt
     *
     * @param type The type of expression to generate, allowing to specialize for the node type (row, filter or
     *            variable)
     * @param userPrompt The user prompt to instruct the AI what to do
     * @param oldCode The current code. Should not be null, but may be an empty string.
     * @param inputPorts Names and Types of all input columns
     * @param flowVariables The incoming flow variables
     * @return The newly generated code
     * @throws IOException
     */
    public static String generateCode( //
        final ExpressionType type, //
        final String userPrompt, //
        final String oldCode, //
        final NameAndType[][] inputPorts, //
        final NameAndType[] flowVariables //
    ) throws IOException {
        var request = new CodeGenerationRequest(//
            oldCode, //
            userPrompt, //
            new Inputs(inputPorts, 0, flowVariables), //
            new Outputs(0, 0, 0, false), //
            EXPRESSION_CORE_PLUGIN_VERSION //
        );

        return HubConnection.INSTANCE.sendRequest("/code_generation/" + type.m_endpoint, request);
    }

    private record Outputs(long num_tables, long num_objects, long num_images, boolean has_view) {
    }

    private record Inputs(NameAndType[][] tables, long num_objects, NameAndType[] flow_variables) { // NOSONAR: we don't need hash or equals here
    }

    private record CodeGenerationRequest(String code, String user_query, Inputs inputs, Outputs outputs,
        String version) {
    }
}
