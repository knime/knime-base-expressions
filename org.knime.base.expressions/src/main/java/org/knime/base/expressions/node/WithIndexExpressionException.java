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
 *   Feb 3, 2025 (benjamin): created
 */
package org.knime.base.expressions.node;

import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;

/**
 * An exception that happens while evaluating multiple expressions and that has information about which expression
 * caused the issue.
 *
 * <p>
 * This class defines two concrete exception types:
 * <ul>
 * <li>{@link ExpressionResultOutOfRangeException} for cases where an expression's result is out of range for an
 * integer.</li>
 * <li>{@link ExpressionWithIndexEvaluationException} for wrapping an {@link ExpressionEvaluationException} that
 * occurred while evaluating an expression.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Use the provided static factory methods to create instances:
 * <ul>
 * <li>{@link #forResultOutOfRange(long, int)}</li>
 * <li>{@link #forEvaluationException(int, ExpressionEvaluationException)}</li>
 * </ul>
 * </p>
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public abstract sealed class WithIndexExpressionException extends Exception {

    /**
     * Creates an exception indicating that the result of an expression is outside the range of integer numbers.
     * <p>
     * This exception is intended for cases where an expression evaluation results in a value that cannot be represented
     * as an integer.
     * </p>
     *
     * @param value the result value which is out of the valid integer range
     * @param expressionIndex the 0-based index of the expression that caused the error
     * @return an instance of {@link ExpressionResultOutOfRangeException}
     */
    public static WithIndexExpressionException forResultOutOfRange(final long value, final int expressionIndex) {
        return new ExpressionResultOutOfRangeException(value, expressionIndex);
    }

    /**
     * Creates an exception that wraps an {@link ExpressionEvaluationException} encountered while evaluating an
     * expression.
     * <p>
     * This exception associates the evaluation error with the corresponding expression index.
     * </p>
     *
     * @param expressionIndex the 0-based index of the expression that caused the error
     * @param cause the underlying {@link ExpressionEvaluationException} thrown during expression evaluation
     * @return an instance of {@link ExpressionWithIndexEvaluationException}
     */
    public static WithIndexExpressionException forEvaluationException(final int expressionIndex,
        final ExpressionEvaluationException cause) {
        return new ExpressionWithIndexEvaluationException(expressionIndex, cause);
    }

    private static final long serialVersionUID = 1L;

    private final int m_expressionIndex;

    private WithIndexExpressionException(final String message, final int expressionIndex) {
        super(message);
        m_expressionIndex = expressionIndex;
    }

    private WithIndexExpressionException(final String message, final int expressionIndex,
        final ExpressionEvaluationException cause) {
        super(message, cause);
        m_expressionIndex = expressionIndex;
    }

    /** @return the index of the expression that caused the error (0-based) */
    public int getExpressionIndex() {
        return m_expressionIndex;
    }

    /**
     * Converts this exception to a {@link KNIMEException} that encapsulates a message with both a summary and potential
     * resolutions.
     *
     * @return a {@link KNIMEException} representing this exception.
     */
    public KNIMEException toKNIMEException() {
        return KNIMEException.of(Message.fromSummaryWithResolution(getMessage(), getResolutions()), this);
    }

    /**
     * Returns a user interface message for this exception.
     * <p>
     * This message is intended to be displayed directly in the dialog next to the expression with the error. It omits
     * the index of the expression that caused the error, as that information is presented separately in contexts where
     * individual expressions are not listed.
     * </p>
     *
     * @return a UI message describing the error in a user-friendly manner.
     */
    public abstract String getUIMessage();

    /**
     * Returns potential resolutions for this exception.
     * <p>
     * Resolutions are suggestions on how to resolve the issue that caused the exception.
     * </p>
     *
     * @return an array of resolutions, which might be empty if no resolutions are available.
     */
    public abstract String[] getResolutions();

    /**
     * Exception thrown when the result of an expression is outside the range of integer numbers.
     */
    public static final class ExpressionResultOutOfRangeException extends WithIndexExpressionException {

        private static final long serialVersionUID = 1L;

        private final long m_value;

        private ExpressionResultOutOfRangeException(final long value, final int expressionIndex) {
            super("The result " + value + " of expression " + (expressionIndex + 1)
                + " is outside the range of integer numbers.", expressionIndex);
            m_value = value;
        }

        @Override
        public String getUIMessage() {
            // Note: The first resolution is appended to the message.
            return "Result " + m_value + " is outside of the range of integer numbers. " + getResolutions()[0];
        }

        @Override
        public String[] getResolutions() {
            return new String[]{"Use the output type Long."};
        }
    }

    /**
     * Exception thrown when an {@link ExpressionEvaluationException} occurs while evaluating an expression.
     */
    public static final class ExpressionWithIndexEvaluationException extends WithIndexExpressionException {

        private static final long serialVersionUID = 1L;

        private final ExpressionEvaluationException m_cause;

        private ExpressionWithIndexEvaluationException(final int expressionIndex,
            final ExpressionEvaluationException cause) {
            super("Error evaluating expression " + (expressionIndex + 1) + ": " + cause.getMessage(), expressionIndex,
                cause);
            m_cause = cause;
        }

        @Override
        public String getUIMessage() {
            return m_cause.getMessage();
        }

        @Override
        public String[] getResolutions() {
            return new String[0];
        }
    }
}
