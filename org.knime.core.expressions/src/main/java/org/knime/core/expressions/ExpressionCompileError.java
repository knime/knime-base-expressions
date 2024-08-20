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
 *   Apr 16, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import org.knime.core.expressions.Ast.AggregationCall;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Ast.FlowVarAccess;

/**
 * Represents an error that happens before running an expression on data (e.g. while parsing or running type inference).
 *
 * @param message a descriptive message of the error
 * @param type the type of the error
 * @param location the text location in the expression (can be <code>null<code> if unknown)
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public record ExpressionCompileError(String message, CompileErrorType type, TextRange location) {

    /** @return a text containing the message, type, and location of the error */
    public String createLongMessage() {
        if (location != null) {
            return type.m_title + " in expression at position " + location.start() + ": " + message;
        } else {
            return createMessage();
        }
    }

    /** @return a text containing the message and type of the error */
    public String createMessage() {
        return type.m_title + ": " + message;
    }

    static ExpressionCompileError syntaxError(final String message, final TextRange location) {
        return new ExpressionCompileError(message, CompileErrorType.SYNTAX, location);
    }

    static ExpressionCompileError typingError(final String message, final TextRange location) {
        return new ExpressionCompileError(message, CompileErrorType.TYPING, location);
    }

    static ExpressionCompileError missingColumnError(final String message, final TextRange location) {
        return new ExpressionCompileError(message, CompileErrorType.MISSING_COLUMN, location);
    }

    static ExpressionCompileError missingFlowVariableError(final String message, final TextRange location) {
        return new ExpressionCompileError(message, CompileErrorType.MISSING_FLOW_VARIABLE, location);
    }

    static ExpressionCompileError aggregationNotImplemented(final String aggregationName, final TextRange location) {
        return new ExpressionCompileError("The aggregation '" + aggregationName + "' is not implemented",
            CompileErrorType.AGG_NOT_IMPLEMENTED, location);
    }

    static ExpressionCompileError missingColumnError(final ColumnAccess node) {
        return missingColumnError("The column " + node.columnId().identifier() + " is not available",
            Parser.getTextLocation(node));
    }

    static ExpressionCompileError missingFlowVariableError(final FlowVarAccess node) {
        return missingFlowVariableError("The flowVariable '" + node.name() + "' is not available",
            Parser.getTextLocation(node));
    }

    static ExpressionCompileError aggregationNotImplemented(final AggregationCall node) {
        return aggregationNotImplemented(node.aggregation().name(), Parser.getTextLocation(node));
    }

    /** Types of compile errors */
    public enum CompileErrorType {

            /** Indicates that the expression could not be parsed because of an invalid syntax */
            SYNTAX("Syntax error"),

            /** Indicates that operations or functions are used on value types that are not supported */
            TYPING("Typing error"),

            /** Indicates that the expression tries to access a column that does not exist */
            MISSING_COLUMN("Missing column error"),

            /** Indicates that the expression tries to access a column that does not exist */
            MISSING_FLOW_VARIABLE("Missing flow variable error"),

            /**
             * Indicates that the expression tries to use an aggregation which is not implemented by the executing
             * backend
             */
            AGG_NOT_IMPLEMENTED("Aggregation not implemented error");

        private final String m_title;

        CompileErrorType(final String title) {
            m_title = title;
        }
    }
}
