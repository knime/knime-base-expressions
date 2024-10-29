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
 *   Sep 3, 2024 (kampmann): created
 */
package org.knime.base.expressions.node;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.knime.core.expressions.ExpressionCompileError;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.TextRange;

/**
 * Represents a diagnostic message for an expression.
 *
 * @param message the message that describes the diagnostic, which will be displayed in the expression on hover
 * @param shortMessage a short version of the message, which will be displayed under the expression
 * @param severity the severity of the diagnostic as defined here
 * @param location the location in the expression where the diagnostic applies
 */
public record ExpressionDiagnostic(String message, String shortMessage, DiagnosticSeverity severity,
    TextRange location) {

    public static final List<ExpressionDiagnostic> NO_INPUT_CONNECTED_DIAGNOSTICS = List.of(withSameMessage( //
        "No input table available. Connect a node first.", //
        DiagnosticSeverity.ERROR, //
        null //
    ));

    /**
     * @param error the error to convert to a diagnostic
     * @return a diagnostic for the given error
     */
    public static ExpressionDiagnostic fromError(final ExpressionCompileError error) {
        return switch (error.type()) {
            case SYNTAX -> new ExpressionDiagnostic( //
                error.message(), //
                "The expression has a syntax error.", //
                DiagnosticSeverity.ERROR, //
                error.location() //
                ); //
            default -> ExpressionDiagnostic.withSameMessage( //
                error.message(), //
                DiagnosticSeverity.ERROR, //
                error.location() //
                ); //
        };
    }

    /**
     * @param exception the exception to extract the diagnostics from
     * @return diagnostics extracted from the given exception
     */
    public static List<ExpressionDiagnostic> fromException(final ExpressionCompileException exception) {
        return exception.getErrors().stream().map(ExpressionDiagnostic::fromError).toList();
    }

    /**
     * Create a diagnostic with the message and shortMessage being the same.
     *
     * @param message
     * @param severity
     * @param location
     * @return a diagnostic with the same message and shortMessage
     */
    public static ExpressionDiagnostic withSameMessage(final String message, final DiagnosticSeverity severity,
        final TextRange location) {
        return new ExpressionDiagnostic(message, message, severity, location);
    }

    /**
     * Convenience function to create a handler that stores the warning at the given index. The warnings array must be
     * large enough to store the warning at the given index. If the index is out of bounds or the array already contains
     * a warning at the given index, the handler does nothing.
     *
     * @param warnings the array to store the warnings in
     * @return a handler that stores the warning at the given index in the array
     */
    public static BiConsumer<Integer, String> getWarningMessageHandler(final ExpressionDiagnostic[] warnings) {
        return (i, warningMessage) -> {

            if (i >= warnings.length || warnings[i] != null) { // already has a warning
                return;
            }
            warnings[i] = ExpressionDiagnostic.withSameMessage( //
                warningMessage, //
                DiagnosticSeverity.WARNING, //
                null //
            );
        };
    }

    /**
     * Convenience function to create a handler that stores the warning just for a single editor. The warnings array
     * must have at least one entry.
     *
     * @param warnings the array to store the warning in
     * @return a handler that stores the warning of the first editor in the array
     */
    public static Consumer<String> getSingleWarningMessageHandler(final ExpressionDiagnostic[] warnings) {
        var warningHandler = getWarningMessageHandler(warnings);
        return warningMessage -> warningHandler.accept(0, warningMessage);
    }

    /**
     * Represents the severity of a diagnostic message.
     */
    public enum DiagnosticSeverity {
            /** The diagnostic represents an error. */
            ERROR,
            /** The diagnostic represents a warning. */
            WARNING,
    }
}
