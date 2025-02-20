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
 *   Jul 8, 2024 (david): created
 */
package org.knime.core.expressions;

import java.util.Arrays;
import java.util.stream.Stream;

import org.knime.core.expressions.ValueType.NativeValueType;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class ReturnTypeDescriptions {

    private ReturnTypeDescriptions() {
    }

    /** Return type description for function returning non-optional string */
    public static final String RETURN_STRING = "STRING";

    /** Return type description for function returning non-optional boolean */
    public static final String RETURN_BOOLEAN = "BOOLEAN";

    /** Return type description for function returning non-optional integer */
    public static final String RETURN_INTEGER = "INTEGER";

    /** Return type description for function returning non-optional float */
    public static final String RETURN_FLOAT = "FLOAT";

    /** Return type description for function returning non-optional date */
    public static final String RETURN_LOCAL_DATE = NativeValueType.LOCAL_DATE.name();

    /** Return type description for function returning non-optional time */
    public static final String RETURN_LOCAL_TIME = NativeValueType.LOCAL_TIME.name();

    /** Return type description for function returning non-optional date-time */
    public static final String RETURN_LOCAL_DATE_TIME = NativeValueType.LOCAL_DATE_TIME.name();

    /** Return type description for function returning non-optional zoned date-time */
    public static final String RETURN_ZONED_DATE_TIME = NativeValueType.ZONED_DATE_TIME.name();

    /** Return type description for function returning non-optional time-based duration */
    public static final String RETURN_TIME_DURATION = NativeValueType.TIME_DURATION.name();

    /** Return type description for function returning non-optional date-based */
    public static final String RETURN_DATE_DURATION = NativeValueType.DATE_DURATION.name();

    /** Return type description for function returning non-optional numeric type */
    public static final String RETURN_INTEGER_FLOAT = union(RETURN_INTEGER, RETURN_FLOAT);

    /** Return type description for function returning optional boolean */
    public static final String RETURN_BOOLEAN_MISSING = optUnion(RETURN_BOOLEAN);

    /** Return type description for function returning optional integer */
    public static final String RETURN_INTEGER_MISSING = optUnion(RETURN_INTEGER);

    /** Return type description for function returning optional float */
    public static final String RETURN_FLOAT_MISSING = optUnion(RETURN_FLOAT);

    /** Return type description for function returning optional string */
    public static final String RETURN_STRING_MISSING = optUnion(RETURN_STRING);

    /** Return type description for function returning optional date */
    public static final String RETURN_LOCAL_DATE_MISSING = optUnion(RETURN_LOCAL_DATE);

    /** Return type description for function returning optional time */
    public static final String RETURN_LOCAL_TIME_MISSING = optUnion(RETURN_LOCAL_TIME);

    /** Return type description for function returning optional date-time */
    public static final String RETURN_LOCAL_DATE_TIME_MISSING = optUnion(RETURN_LOCAL_DATE_TIME);

    /** Return type description for function returning optional zoned date-time */
    public static final String RETURN_ZONED_DATE_TIME_MISSING = optUnion(RETURN_ZONED_DATE_TIME);

    /** Return type description for function returning optional time-based duration */
    public static final String RETURN_TIME_DURATION_MISSING = optUnion(RETURN_TIME_DURATION);

    /** Return type description for function returning optional date-based duration */
    public static final String RETURN_DATE_DURATION_MISSING = optUnion(RETURN_DATE_DURATION);

    /** Return type description for function returning optional numeric type */
    public static final String RETURN_INTEGER_FLOAT_MISSING = optUnion(RETURN_INTEGER, RETURN_FLOAT);

    private static String union(final String... types) {
        return String.join(" | ", types);
    }

    private static String optUnion(final String... types) {
        var typesWithMissing = Stream.concat(Arrays.stream(types), Stream.of("MISSING"));
        return union(typesWithMissing.toArray(String[]::new));
    }
}
