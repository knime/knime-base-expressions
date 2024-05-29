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
 *   May 29, 2024 (david): created
 */
package org.knime.core.expressions;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.expressions.ValueType.NativeValueType;

/**
 * Enum type representing our allowed mathematical constants.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public enum MathsConstantValue {

        PI(Math.PI), //
        E(Math.E), //
        INF(Double.POSITIVE_INFINITY), //
        NaN(Double.NaN), // NOSONAR NaN should be written in this way
        MIN_INT(Long.MIN_VALUE), //
        MAX_INT(Long.MAX_VALUE), //
        MIN_FLOAT(-Double.MAX_VALUE), //
        MAX_FLOAT(Double.MAX_VALUE), //
        MIN_POSITIVE_FLOAT(Double.MIN_VALUE), //
        MIN_NORMAL_FLOAT(Double.MIN_NORMAL); //

    private final Number m_value;

    private final NativeValueType m_type;

    MathsConstantValue(final long value) {
        m_value = value;
        m_type = NativeValueType.INTEGER;
    }

    MathsConstantValue(final double value) {
        m_value = value;
        m_type = NativeValueType.FLOAT;
    }

    /**
     * The numerical value of the constant.
     *
     * @return the value of the constant
     */
    public Number value() {
        return m_value;
    }

    /**
     * The type of the constant.
     *
     * @return the type of the constant
     */
    public NativeValueType type() {
        return m_type;
    }

    /**
     * Converts this constant to an {@link Ast} of the appropriate numerical type.
     *
     * @param data the data to be used for the {@link Ast}
     * @return the {@link Ast} representing this constant
     */
    public Ast toAst(final Map<String, Object> data) {
        return switch (m_type) {
            case INTEGER -> new Ast.IntegerConstant(m_value.longValue(), data);
            case FLOAT -> new Ast.FloatConstant(m_value.doubleValue(), data);
            default -> throw new IllegalStateException("Unexpected value type in toAst: " + m_type);
        };
    }

    /**
     * Converts this constant to an {@link Ast} of the appropriate numerical type, with empty data.
     *
     * @return the {@link Ast} representing this constant
     */
    public Ast toAst() {
        return toAst(new HashMap<>());
    }
}
