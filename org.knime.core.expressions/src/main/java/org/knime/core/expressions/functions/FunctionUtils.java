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
 *   Apr 8, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import java.util.function.Supplier;

import org.knime.core.expressions.ExpressionEvaluationException;

/**
 * Utilities for implementing functions.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class FunctionUtils {

    private FunctionUtils() {
    }

    static IllegalStateException calledWithIllegalArgs() {
        return new IllegalStateException("Implementation error: called function with unsupported arguments.");
    }

    /**
     * Convert a long to an int, throwing an exception if the value is out of range for an int.
     *
     * @param value the long value
     * @param message the exception message
     * @param formattingArgs the formatting arguments for the message. Will be forwarded to
     *            {@link String#format(String, Object...)} along with the message. Can be empty.
     * @return the int value
     * @throws ExpressionEvaluationException if the value is out of range
     */
    public static int toIntExact(final long value, final String message, final Object... formattingArgs)
        throws ExpressionEvaluationException {
        return toIntExact(value, () -> String.format(message, formattingArgs));
    }

    /**
     * Convert a long to an int, throwing an exception if the value is out of range for an int.
     *
     * @param value the long value
     * @param messageSupplier a supplier for the exception message. Will only be called if the value is out of range.
     * @return the int value
     * @throws ExpressionEvaluationException if the value is out of range
     */
    public static int toIntExact(final long value, final Supplier<String> messageSupplier)
        throws ExpressionEvaluationException {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException e) { // NOSONAR we don't need to rethrow this exception
            throw new ExpressionEvaluationException(messageSupplier.get());
        }
    }

    /**
     * Convert a long to an int, clamping the value to the range of an int.
     *
     * @param value the long value
     * @return the int value
     */
    public static int toIntClamped(final long value) {
        return clamped(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Clamp the given long value to the given range. Note: if both boundary args are ints, the result is an int.
     *
     * @param value the value to clamp
     * @param min the minimum value as int
     * @param max the maximum value as int
     * @return the clamped value as int
     */
    public static int clamped(final long value, final int min, final int max) {
        return (int)clamped(value, (long)min, (long)max);
    }

    /**
     * Clamp the given long value to the given range.
     *
     * @param value the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return the clamped value
     */
    public static long clamped(final long value, final long min, final long max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }
}
