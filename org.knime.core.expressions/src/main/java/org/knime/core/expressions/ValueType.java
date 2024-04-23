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
 */
package org.knime.core.expressions;

import java.util.Objects;

/**
 * The type of an expression value.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public interface ValueType {

    /**
     * Type of "MISSING" values. Note that the {@link #baseType()} of {@link #MISSING} is itself and therefore, not
     * optional.
     */
    ValueType MISSING = new MissingValueType();

    /** Type for "BOOLEAN" values "true" and "false" */
    ValueType BOOLEAN = NativeValueType.BOOLEAN;

    /** Type for "INTEGER" values (equivalent to Java long) */
    ValueType INTEGER = NativeValueType.INTEGER;

    /** Type for "FLOAT" values (equivalent to Java double) */
    ValueType FLOAT = NativeValueType.FLOAT;

    /** Type for "STRING" values */
    ValueType STRING = NativeValueType.STRING;

    /** Type for values of {@link #BOOLEAN} that might be "MISSING" */
    ValueType OPT_BOOLEAN = new OptionalValueType(NativeValueType.BOOLEAN);

    /** Type for values of {@link #INTEGER} that might be "MISSING" */
    ValueType OPT_INTEGER = new OptionalValueType(NativeValueType.INTEGER);

    /** Type for values of {@link #FLOAT} that might be "MISSING" */
    ValueType OPT_FLOAT = new OptionalValueType(NativeValueType.FLOAT);

    /** Type for values of {@link #STRING} that might be "MISSING" */
    ValueType OPT_STRING = new OptionalValueType(NativeValueType.STRING);

    /**
     * Helper to create {@link #BOOLEAN} or {@link #OPT_BOOLEAN} types.
     *
     * @param optional if the resulting type should permit "MISSING" values
     * @return {@link #BOOLEAN} or {@link #OPT_BOOLEAN}
     */
    static ValueType BOOLEAN(final boolean optional) { // NOSONAR - naming this utility the same is useful
        return optional ? OPT_BOOLEAN : BOOLEAN;
    }

    /**
     * Helper to create {@link #INTEGER} or {@link #OPT_INTEGER} types.
     *
     * @param optional if the resulting type should permit "MISSING" values
     * @return {@link #INTEGER} or {@link #OPT_INTEGER}
     */
    static ValueType INTEGER(final boolean optional) { // NOSONAR - naming this utility the same is useful
        return optional ? OPT_INTEGER : INTEGER;
    }

    /**
     * Helper to create {@link #FLOAT} or {@link #OPT_FLOAT} types.
     *
     * @param optional if the resulting type should permit "MISSING" values
     * @return {@link #FLOAT} or {@link #OPT_FLOAT}
     */
    static ValueType FLOAT(final boolean optional) { // NOSONAR - naming this utility the same is useful
        return optional ? OPT_FLOAT : FLOAT;
    }

    /**
     * Helper to create {@link #STRING} or {@link #OPT_STRING} types.
     *
     * @param optional if the resulting type should permit "MISSING" values
     * @return {@link #STRING} or {@link #OPT_STRING}
     */
    static ValueType STRING(final boolean optional) { // NOSONAR - naming this utility the same is useful
        return optional ? OPT_STRING : STRING;
    }

    /**
     * Helper to check equality of two types, including optional types.
     *
     * @param type1 type 1
     * @param type2 type 2
     * @return <code>true</code> if the base-types are equal
     */
    static boolean hasSameBaseType(final ValueType type1, final ValueType type2) {
        return type1.baseType().equals(type2.baseType());
    }

    /**
     * Helper to check if the given type is numeric and not optional.
     *
     * @param type the type to check
     * @return <code>true</code> if the type is a numeric type (either {@link #FLOAT} or {@link #INTEGER})
     */
    static boolean isNumeric(final ValueType type) {
        return type.equals(FLOAT) || type.equals(INTEGER);
    }

    /**
     * Helper to check if the given type is numeric (optional or not).
     *
     * @param type the type to check
     * @return <code>true</code> if the base-type is a numeric type (either {@link #FLOAT} or {@link #INTEGER})
     */
    static boolean isNumericOrOpt(final ValueType type) {
        return isNumeric(type.baseType());
    }

    /**
     * @return a unique but simple name of the type
     */
    String name();

    /**
     * @return <code>true</code> if the type permits "MISSING" values
     */
    boolean isOptional();

    /**
     * @return the complementary type that is not optional or the same type if this type is not optional. For the
     *         returned type, {@link #isOptional()} will be <code>false</code> (except for {@link #MISSING}).
     */
    ValueType baseType();

    /**
     * @return the complementary optional type or the same type if this type is optional. For the returned type,
     *         {@link #isOptional()} will be <code>true</code>.
     */
    ValueType optionalType();

    /** Implementation of {@link ValueType} for the native types "BOOLEAN", "INTEGER, "FLOAT", and "STRING". */
    @SuppressWarnings("hiding")
    enum NativeValueType implements ValueType {
            BOOLEAN, INTEGER, FLOAT, STRING;

        @Override
        public boolean isOptional() {
            return false;
        }

        @Override
        public ValueType baseType() {
            return this;
        }

        @Override
        public ValueType optionalType() {
            return new OptionalValueType(this);
        }
    }

    /**
     * Implementation of {@link ValueType} for "MISSING" values. Note that this implementation is special because the
     * {@link #baseType()} and {@link #optionalType()} are the same.
     */
    final class MissingValueType implements ValueType {

        private MissingValueType() {
            // Singleton
        }

        @Override
        public String name() {
            return "MISSING";
        }

        @Override
        public boolean isOptional() {
            return true;
        }

        @Override
        public ValueType baseType() {
            return this;
        }

        @Override
        public ValueType optionalType() {
            return this;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    /** Wrapper for any non-optional {@link ValueType} to make it optional */
    final class OptionalValueType implements ValueType {

        private final ValueType m_baseType;

        OptionalValueType(final ValueType baseType) {
            m_baseType = baseType;
        }

        @Override
        public String name() {
            return m_baseType.name() + "?";
        }

        @Override
        public boolean isOptional() {
            return true;
        }

        @Override
        public ValueType baseType() {
            return m_baseType;
        }

        @Override
        public ValueType optionalType() {
            return this;
        }

        @Override
        public String toString() {
            return name();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof OptionalValueType optValType) {
                return m_baseType.equals(optValType.m_baseType);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(17, m_baseType);
        }
    }
}
