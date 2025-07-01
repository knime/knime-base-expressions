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
 *   Apr 23, 2024 (david): created
 */
package org.knime.core.expressions;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * Interface for registering warnings emitted during expression evaluation.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public interface EvaluationContext {

    /**
     * Add a warning that happened during expression evaluation.
     *
     * Warnings should follow this format as much as possible:
     *
     * "{thing} returned {value} because {reason} [{at position/row N}]"
     *
     * @param warning the warning to add
     */
    void addWarning(final String warning);

    /**
     * Returns the node execution start time as a {@link ZonedDateTime}. This is used by functions like now().
     *
     * @return the node execution start time, not <code>null</code>
     */
    ZonedDateTime getExecutionStartTime();

    /**
     * Creates an {@link EvaluationContext} that uses the given {@link ZonedDateTime} as the execution start time and
     * forwards warnings to the given consumer.
     *
     * @param executionStartTime the execution start time, not <code>null</code>
     * @param warningConsumer a consumer for warnings, not <code>null</code>
     * @return a new {@link EvaluationContext}
     */
    static EvaluationContext of(final ZonedDateTime executionStartTime, final Consumer<String> warningConsumer) {
        return new EvaluationContext() {
            @Override
            public void addWarning(final String warning) {
                warningConsumer.accept(warning);
            }

            @Override
            public ZonedDateTime getExecutionStartTime() {
                return executionStartTime;
            }
        };
    }
}
