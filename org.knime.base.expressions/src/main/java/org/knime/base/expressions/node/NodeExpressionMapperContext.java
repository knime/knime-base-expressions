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
 *   Aug 20, 2024 (kampmann): created
 */
package org.knime.base.expressions.node;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.knime.base.expressions.ExpressionMapperFactory.ExpressionMapperContext;
import org.knime.base.expressions.ExpressionRunnerUtils;
import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.FlowVarAccess;
import org.knime.core.expressions.Computer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * Simple implementation of {@link ExpressionMapperContext} that maps flow variables to computers and resolves
 * aggregations that have been evaluated using {@link ExpressionRunnerUtils#evaluateAggregations}.
 */
public class NodeExpressionMapperContext implements ExpressionMapperContext {

    private final Function<VariableType<?>[], Map<String, FlowVariable>> m_getFlowVariable;

    /**
     * @param getFlowVariable
     */
    public NodeExpressionMapperContext(final Function<VariableType<?>[], Map<String, FlowVariable>> getFlowVariable) {
        m_getFlowVariable = getFlowVariable;
    }

    @Override
    public Optional<Computer> flowVariableToComputer(final FlowVarAccess flowVariableAccess) {
        return Optional.ofNullable(
            m_getFlowVariable.apply(ExpressionRunnerUtils.SUPPORTED_FLOW_VARIABLE_TYPES).get(flowVariableAccess.name()))
            .map(ExpressionRunnerUtils::computerForFlowVariable);
    }

    @Override
    public Optional<Computer> aggregationToComputer(final AggregationCall aggregationCall) {
        return Optional.ofNullable(ExpressionRunnerUtils.getAggregationResultComputer(aggregationCall));
    }
}
