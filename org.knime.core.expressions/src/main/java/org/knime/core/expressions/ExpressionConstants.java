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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.expressions.Ast.ConstantAst;

/**
 * Enum type representing our allowed constants.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public enum ExpressionConstants {

        /** mathematical constant pi. */
        PI(Math.PI, "The ratio of a circle's circumference to its diameter"), //
        /** mathematical constant e. */
        E(Math.E, "Euler's constant, the base of the natural logarithm"), //
        /** positive infinity. */
        INFINITY(Double.POSITIVE_INFINITY, "Infinity"), //
        /** not a number */
        NaN(Double.NaN, "Not a number, indicating that a value has no defined real value, such as 0.0/0.0"), // NOSONAR NaN should be written in this way
        /** the smallest INTEGER number */
        MIN_INTEGER(Long.MIN_VALUE, "Smallest integer representable by this computer"), //
        /** the largest INTEGER number */
        MAX_INTEGER(Long.MAX_VALUE, "Largest integer representable by this computer"), //
        /** the smallest FLOAT number */
        MIN_FLOAT(-Double.MAX_VALUE, "Smallest float representable by this computer"), //
        /** the largest FLOAT number */
        MAX_FLOAT(Double.MAX_VALUE, "Largest float representable by this computer"), //
        /** the smallest positive normal FLOAT number */
        TINY_FLOAT(Double.MIN_NORMAL, "Smallest positive float value representable by this computer"), //
        /** true value */
        TRUE(true, "True value"), //
        /** false value */
        FALSE(false, "False value"),
        /** missing value */
        MISSING("MISSING", ValueType.MISSING, "Missing value");

    private final Object m_value;

    private final ValueType m_type;

    private final String m_documentation;

    ExpressionConstants(final long value, final String documentation) {
        this(value, ValueType.INTEGER, documentation);
    }

    ExpressionConstants(final double value, final String documentation) {
        this(value, ValueType.FLOAT, documentation);
    }

    ExpressionConstants(final boolean value, final String documentation) {
        this(value, ValueType.BOOLEAN, documentation);
    }

    ExpressionConstants(final Object value, final ValueType type, final String documentation) {
        m_value = value;
        m_type = type;
        m_documentation = documentation;
    }

    static String availableConstants() {
        return Arrays.stream(values()).map(ExpressionConstants::name).collect(Collectors.joining(", "));
    }

    /**
     * The numerical value of the constant.
     *
     * @return the value of the constant
     */
    public Object value() {
        return m_value;
    }

    /**
     * The type of the constant.
     *
     * @return the type of the constant
     */
    public ValueType type() {
        return m_type;
    }

    /**
     * The documentation of the constant.
     *
     * @return the documentation of the constant
     */
    public String documentation() {
        return m_documentation;
    }

    /**
     * Converts this constant to an {@link Ast} of the appropriate numerical type.
     *
     * @param data the data to be used for the {@link Ast}
     * @return the {@link Ast} representing this constant
     */
    public ConstantAst toAst(final Map<String, Object> data) {
        if (m_type == ValueType.INTEGER) {
            return new Ast.IntegerConstant((Long)m_value, data);
        } else if (m_type == ValueType.FLOAT) {
            return new Ast.FloatConstant((Double)m_value, data);
        } else if (m_type == ValueType.BOOLEAN) {
            return new Ast.BooleanConstant((Boolean)m_value, data);
        } else if (m_type == ValueType.MISSING) {
            return new Ast.MissingConstant(data);
        } else {
            throw new IllegalStateException("Unexpected value type in toAst: " + m_type);
        }
    }

    /**
     * Converts this constant to an {@link Ast} of the appropriate numerical type, with empty data.
     *
     * @return the {@link Ast} representing this constant
     */
    public Ast toAst() {
        return toAst(new HashMap<>());
    }

    /**
     * Converts this constant to an {@link OperatorDescription}. Arguments and return type are null.
     *
     * @return the {@link OperatorDescription} representing this constant
     */
    public OperatorDescription toOperatorDescription() {
        return new OperatorDescription(name(), documentation(), null, null, type().name(), null, List.of("constant"),
            CONSTANTS_CATEGORY.name(), OperatorDescription.CONSTANT_ENTRY_TYPE);
    }

    /** Category for all constants */
    public static final OperatorCategory CONSTANTS_CATEGORY = new OperatorCategory("Constants", """
            The "Constants" category in KNIME Expression language includes
            predefined constants such as PI, Infinity, and NaN.
            """);
}
