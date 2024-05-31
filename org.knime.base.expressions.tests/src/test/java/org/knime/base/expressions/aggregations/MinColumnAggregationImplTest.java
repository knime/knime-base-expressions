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
 *   May 27, 2024 (benjamin): created
 */
package org.knime.base.expressions.aggregations;

import static org.knime.base.expressions.aggregations.AggregationTestUtils.listOf;
import static org.knime.core.expressions.AstTestUtils.BOOL;

import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class MinColumnAggregationImplTest {

    @TestFactory
    List<DynamicNode> min() {
        return new AggregationTestUtils.AggregationTestBuilder(MinColumnAggregationImpl::minAggregation) //
            .implInt("int", listOf(1, -10, 10, 5), -10) //
            .implLong("long", listOf(1L, -10L, 10L, 5L), -10L) //
            .implLong("longMissing", listOf(1L, null, 5L), 1L) //
            .implLong("longOnlyMissing", listOf(null, null), null) //
            .implLong("longMinValue", listOf(Long.MIN_VALUE, 0L), Long.MIN_VALUE) //
            .implLong("longMaxValue", listOf(Long.MAX_VALUE, Long.MAX_VALUE), Long.MAX_VALUE) //
            .implDouble("double", listOf(1.0, -0.1, 2.2, 0.1), -0.1) //
            .implDouble("doubleMissing", listOf(1.0, null, 5.4), 1.0) //
            .implDouble("doubleOnlyMissing", listOf(null, null), null) //
            .implDouble("doubleNaN", listOf(Double.NaN, 1.0), Double.NaN) //
            .implDouble("doubleOnlyNaN", listOf(Double.NaN, Double.NaN), Double.NaN) //
            .implDouble("doubleNaNIgnore", listOf(Double.NaN, 1.0), List.of(BOOL(true)), 1.0) //
            .implDouble("doubleOnlyNaNIgnore", listOf(Double.NaN, Double.NaN), List.of(BOOL(true)), Double.NaN) //
            .implDouble("doubleNoNaNIgnore", List.of(1.0, 2.0), List.of(BOOL(true)), 1.0) //
            .unsupportedTypeString("string") //
            .tests();
    }
}
