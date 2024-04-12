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
 *   Feb 13, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import static org.knime.core.expressions.Ast.binaryOp;
import static org.knime.core.expressions.Ast.booleanConstant;
import static org.knime.core.expressions.Ast.columnAccess;
import static org.knime.core.expressions.Ast.floatConstant;
import static org.knime.core.expressions.Ast.flowVarAccess;
import static org.knime.core.expressions.Ast.functionCall;
import static org.knime.core.expressions.Ast.integerConstant;
import static org.knime.core.expressions.Ast.missingConstant;
import static org.knime.core.expressions.Ast.stringConstant;
import static org.knime.core.expressions.Ast.unaryOp;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.knime.core.expressions.Ast.BinaryOperator;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.Expressions.SyntaxError;
import org.knime.core.expressions.antlr.KnimeExpressionBaseVisitor;
import org.knime.core.expressions.antlr.KnimeExpressionLexer;
import org.knime.core.expressions.antlr.KnimeExpressionParser;
import org.knime.core.expressions.antlr.KnimeExpressionParser.BinaryOpContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.ColAccessContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.FlowVarAccessContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.FullExprContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.FunctionCallContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.ParenthesisedExprContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.UnaryOpContext;

/**
 * Expression language parser that makes use of a ANTLR4 generated parser.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class Parser {

    private static final Pattern ESC_SEQUENCE_PATTERN = Pattern.compile("\\\\([abfnrtv\"'\\\\\\n]|u([0-9A-Fa-f]{4}))");

    private Parser() {
    }

    private static final ThrowingErrorListener ERROR_LISTENER = new ThrowingErrorListener();

    private static final ExpressionToAstVisitor EXPRESSION_TO_AST_VISITOR = new ExpressionToAstVisitor();

    static Ast parse(final String expression) throws SyntaxError {
        try {
            var lexer = new KnimeExpressionLexer(CharStreams.fromString(expression));
            var parser = new KnimeExpressionParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(ERROR_LISTENER);
            return parser.fullExpr().accept(EXPRESSION_TO_AST_VISITOR);
        } catch (RuntimeSyntaxError ex) { // NOSONAR: RuntimeSyntaxError is just a wrapper
            throw ex.m_cause;
        }
    }

    /** Can be set for the parser to handle errors */
    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
            final int charPositionInLine, final String msg, final RecognitionException e) {
            throw new RuntimeSyntaxError(new Expressions.SyntaxError(msg));
        }
    }

    /** Wrapper for a {@link SyntaxError} that is a {@link RuntimeException} */
    private static final class RuntimeSyntaxError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        final SyntaxError m_cause;

        RuntimeSyntaxError(final SyntaxError cause) {
            m_cause = cause;
        }
    }

    private static final class ExpressionToAstVisitor extends KnimeExpressionBaseVisitor<Ast> {

        @Override
        public Ast visitFullExpr(final FullExprContext ctx) {
            return ctx.expr().accept(this);
        }

        @Override
        public Ast visitUnaryOp(final UnaryOpContext ctx) {
            var arg = ctx.getChild(1).accept(this);
            var op = mapUnaryOperator(ctx.op);
            return unaryOp(op, arg);
        }

        @Override
        public Ast visitBinaryOp(final BinaryOpContext ctx) {
            var arg1 = ctx.getChild(0).accept(this);
            var arg2 = ctx.getChild(2).accept(this);
            var op = mapBinaryOperator(ctx.op);
            return binaryOp(op, arg1, arg2);
        }

        @Override
        public Ast visitColAccess(final ColAccessContext ctx) {
            var col = ctx.shortName != null ? ctx.shortName.getText() : parseStringLiteral(ctx.longName.getText());
            return columnAccess(col);
        }

        @Override
        public Ast visitFlowVarAccess(final FlowVarAccessContext ctx) {
            var name = ctx.shortName != null ? ctx.shortName.getText() : parseStringLiteral(ctx.longName.getText());
            return flowVarAccess(name);
        }

        @Override
        public Ast visitFunctionCall(final FunctionCallContext ctx) {
            var name = ctx.name.getText();
            var argsContext = ctx.functionArgs();
            var args = //
                argsContext == null //
                    ? List.<Ast> of() //
                    : argsContext.expr().stream().map(a -> a.accept(this)).toList();
            return functionCall(name, args);
        }

        @Override
        public Ast visitParenthesisedExpr(final ParenthesisedExprContext ctx) {
            // Ignore the parentheses terminal symbols
            return ctx.inner.accept(this);
        }

        @Override
        public Ast visitErrorNode(final ErrorNode node) {
            throw syntaxError(node.getText());
        }

        @Override
        public Ast visitTerminal(final TerminalNode node) {
            var symbol = node.getSymbol();
            return switch (symbol.getType()) {
                case KnimeExpressionParser.BOOLEAN -> //
                        booleanConstant(Boolean.parseBoolean(symbol.getText()));
                case KnimeExpressionParser.INTEGER -> //
                        integerConstant(Long.parseLong(symbol.getText().replace("_", "")));
                case KnimeExpressionParser.FLOAT -> //
                        floatConstant(Double.parseDouble(symbol.getText().replace("_", "")));
                case KnimeExpressionParser.STRING -> //
                        stringConstant(parseStringLiteral(symbol.getText()));
                case KnimeExpressionParser.MISSING -> //
                        missingConstant();
                default -> throw syntaxError("Unexpected terminal value: " + symbol.getText());
            };
        }

        /** Parse the string from the text. Removes starting and ending quotes, replaces escape sequences. */
        private static String parseStringLiteral(final String text) {
            // Crop of the double or single quotes
            var content = text.substring(1, text.length() - 1);

            // Replace escape sequences
            return ESC_SEQUENCE_PATTERN.matcher(content)
                .replaceAll(matchResult -> Matcher.quoteReplacement(escapeSequenceMapping(matchResult.group(1))));
        }

        /** @return the value that an escape sequence should be resolved to */
        private static String escapeSequenceMapping(final String escapeSequence) { // NOSONAR - not too complex
            return switch (escapeSequence.charAt(0)) {
                case '\n' -> ""; // \<newline> -> <newline ignored>
                case '\\' -> "\\"; // \\ -> \
                case '\'' -> "'"; // \' -> '
                case '"' -> "\""; // \" -> "
                case 'b' -> "\b"; // \b -> <ASCII Backspace (BS)>
                case 'f' -> "\f"; // \f -> <ASCII Formfeed (FF)>
                case 'n' -> "\n"; // \n -> <ASCII Linefeed (LF)>
                case 'r' -> "\r"; // \r -> <ASCII Carriage Return (CR)>
                case 't' -> "\t"; // \t -> <ASCII Horizontal Tab (TAB)>
                case 'u' -> "" + (char)Integer.parseInt(escapeSequence.substring(1), 16); // unicode sequence
                default -> throw syntaxError("Invalid escape sequence: '\\" + escapeSequence + "'");
            };
        }

        /** Maps a token for a unary operation to the Ast operator */
        private static UnaryOperator mapUnaryOperator(final Token op) {
            return switch (op.getType()) {
                case KnimeExpressionParser.MINUS -> UnaryOperator.MINUS;
                case KnimeExpressionParser.NOT -> UnaryOperator.NOT;
                default -> throw syntaxError("Unexpected unary operator: " + op.getType());
            };
        }

        /** Maps a token for a binary operation to the Ast operator */
        private static BinaryOperator mapBinaryOperator(final Token op) { // NOSONAR - not too complex
            return switch (op.getType()) {
                // Arithmetic
                case KnimeExpressionParser.PLUS -> BinaryOperator.PLUS;
                case KnimeExpressionParser.MINUS -> BinaryOperator.MINUS;
                case KnimeExpressionParser.MULTIPLY -> BinaryOperator.MULTIPLY;
                case KnimeExpressionParser.DIVIDE -> BinaryOperator.DIVIDE;
                case KnimeExpressionParser.FLOOR_DIVIDE -> BinaryOperator.FLOOR_DIVIDE;
                case KnimeExpressionParser.EXPONENTIATE -> BinaryOperator.EXPONENTIAL;
                case KnimeExpressionParser.MODULO -> BinaryOperator.REMAINDER;
                // Comparison
                case KnimeExpressionParser.LESS_THAN -> BinaryOperator.LESS_THAN;
                case KnimeExpressionParser.LESS_THAN_EQUAL -> BinaryOperator.LESS_THAN_EQUAL;
                case KnimeExpressionParser.GREATER_THAN -> BinaryOperator.GREATER_THAN;
                case KnimeExpressionParser.GREATER_THAN_EQUAL -> BinaryOperator.GREATER_THAN_EQUAL;
                case KnimeExpressionParser.EQUAL -> BinaryOperator.EQUAL_TO;
                case KnimeExpressionParser.NOT_EQUAL -> BinaryOperator.NOT_EQUAL_TO;
                // Logical
                case KnimeExpressionParser.AND -> BinaryOperator.CONDITIONAL_AND;
                case KnimeExpressionParser.OR -> BinaryOperator.CONDITIONAL_OR;

                default -> throw syntaxError("Unexpected binary operator: " + op.getType());
            };
        }

        /** Create a {@link RuntimeSyntaxError} to throw (will result in a {@link SyntaxError}) */
        private static RuntimeSyntaxError syntaxError(final String message) {
            // TODO(AP-22027) include information about the location of the error
            return new RuntimeSyntaxError(new SyntaxError(message));
        }
    }
}
