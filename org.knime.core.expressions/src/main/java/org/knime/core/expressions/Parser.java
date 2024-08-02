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

import static org.knime.core.expressions.Ast.aggregationCall;
import static org.knime.core.expressions.Ast.binaryOp;
import static org.knime.core.expressions.Ast.booleanConstant;
import static org.knime.core.expressions.Ast.columnAccess;
import static org.knime.core.expressions.Ast.floatConstant;
import static org.knime.core.expressions.Ast.flowVarAccess;
import static org.knime.core.expressions.Ast.functionCall;
import static org.knime.core.expressions.Ast.integerConstant;
import static org.knime.core.expressions.Ast.missingConstant;
import static org.knime.core.expressions.Ast.rowId;
import static org.knime.core.expressions.Ast.rowIndex;
import static org.knime.core.expressions.Ast.stringConstant;
import static org.knime.core.expressions.Ast.unaryOp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.knime.core.expressions.Ast.BinaryOperator;
import org.knime.core.expressions.Ast.ConstantAst;
import org.knime.core.expressions.Ast.FloatConstant;
import org.knime.core.expressions.Ast.IntegerConstant;
import org.knime.core.expressions.Ast.UnaryOperator;
import org.knime.core.expressions.Expressions.ExpressionCompileException;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.aggregations.ColumnAggregation;
import org.knime.core.expressions.antlr.KnimeExpressionBaseVisitor;
import org.knime.core.expressions.antlr.KnimeExpressionLexer;
import org.knime.core.expressions.antlr.KnimeExpressionParser;
import org.knime.core.expressions.antlr.KnimeExpressionParser.AtomContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.BinaryOpContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.ColAccessContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.FlowVarAccessContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.FullExprContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.FunctionOrAggregationCallContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.ParenthesisedExprContext;
import org.knime.core.expressions.antlr.KnimeExpressionParser.UnaryOpContext;
import org.knime.core.expressions.functions.BuiltInFunctions;
import org.knime.core.expressions.functions.ExpressionFunction;

/**
 * Expression language parser that makes use of a ANTLR4 generated parser.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
final class Parser {

    private static final Pattern ESC_SEQUENCE_PATTERN = Pattern.compile("\\\\(u....|.|$)", Pattern.DOTALL);

    private static final String VALID_ESCAPE_SEQUENCES = "<newline>, \\, \', \", b, n, r, t, u<4-hex-digits>";

    private static final String LOCATION_DATA_KEY = "text_location";

    private Parser() {
    }

    private static final ExpressionToAstVisitor EXPRESSION_TO_AST_VISITOR = new ExpressionToAstVisitor();

    static Ast parse(final String expression) throws ExpressionCompileException {
        return parseTreeToAst(parseToParseTree(expression));
    }

    /** @return the text location of the node or <code>null</code> if it is not set */
    static TextRange getTextLocation(final Ast node) {
        return (TextRange)node.data(LOCATION_DATA_KEY);
    }

    /**
     * Parses the expression using the lexer and parser. Throws a ExpressionCompileException if errors occur with all
     * errors.
     */
    private static FullExprContext parseToParseTree(final String expression) throws ExpressionCompileException {
        var errorListener = new CollectingErrorListener(expression);
        var lexer = new KnimeExpressionLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        var parser = new KnimeExpressionParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        var fullExpr = parser.fullExpr(); // NOSONAR: This call parses the expression and fills the errors
        if (!errorListener.m_errors.isEmpty()) {
            throw new ExpressionCompileException(errorListener.m_errors);
        }
        return fullExpr;
    }

    /** Can be set for the parser to handle errors */
    private static final class CollectingErrorListener extends BaseErrorListener {

        /** The index for the first character for each line */
        private final int[] m_lineStarts;

        private final int m_exprLength;

        private List<ExpressionCompileError> m_errors;

        public CollectingErrorListener(final String expression) {
            m_errors = new ArrayList<>();
            m_exprLength = expression.length() - 1;

            var lines = expression.split("\n", -1); // limit=-1 to include trailing empty lines
            // Initialize with length of previous lines - will sum up in-place next
            m_lineStarts = IntStream.concat( //
                IntStream.of(0), //
                Arrays.stream(lines).mapToInt(l -> l.length() + 1) //
            ).limit(lines.length).toArray();
            Arrays.parallelPrefix(m_lineStarts, (x, y) -> x + y);
        }

        // NB: on the API only provides
        private TextRange textRangeFromLineAndCharPos(final int line, final int charPosInLine) {
            if (line < 0 || line >= m_lineStarts.length) {
                // Cannot find the index for the position - mark everything
                return new TextRange(0, m_exprLength);
            }
            var idx = m_lineStarts[line] + charPosInLine;
            return new TextRange(idx, idx + 1);
        }

        private static Optional<String> getPrecedingToken(final Token offendingSymbol) {
            if (offendingSymbol.getStartIndex() == 0) {
                return Optional.empty();
            }
            return Optional.of(offendingSymbol.getInputStream()
                .getText(new Interval(offendingSymbol.getStartIndex() - 1, offendingSymbol.getStartIndex())));
        }

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
            final int charPositionInLine, final String msg, final RecognitionException e) {
            var detailedMsg = msg;

            if (offendingSymbol instanceof Token) {
                var offendingToken = (Token)offendingSymbol;
                var precedingToken = getPrecedingToken(offendingToken);
                if (offendingToken.getType() == KnimeExpressionLexer.INTEGER && precedingToken.isPresent()
                    && precedingToken.get().startsWith("0")) {
                    detailedMsg = "Leading zeros are not allowed in integers.";
                }
            }

            // recognizer can be the parser or the lexer
            // if it is the parser, we can check if the expression is empty,
            // There are no errors yet so the following should work and should not throw false positives
            if (recognizer instanceof KnimeExpressionParser parser && e instanceof InputMismatchException
                && m_errors.isEmpty()) {
                var tokens = parser.getTokenStream();
                if (tokens.size() == 1 && tokens.get(0).getType() == Recognizer.EOF) {
                    detailedMsg = "No expression present. Enter an expression.";
                }
            }

            m_errors.add(ExpressionCompileError.syntaxError(detailedMsg,
                textRangeFromLineAndCharPos(line - 1, charPositionInLine)));
        }
    }

    /**
     * Converts the parse tree (from parseToParseTree) to an {@link Ast}.
     *
     * @throws ExpressionCompileException
     */
    private static Ast parseTreeToAst(final FullExprContext parseTree) throws ExpressionCompileException {
        try {
            return parseTree.accept(EXPRESSION_TO_AST_VISITOR);
        } catch (RuntimeSyntaxError ex) { // NOSONAR: RuntimeSyntaxError is just a wrapper
            throw ex.toExpressionCompileException();
        }
    }

    /**
     * Wrapper for an {@link ExpressionCompileError} that is a {@link RuntimeException} for the
     * {@link ExpressionToAstVisitor}
     */
    private static final class RuntimeSyntaxError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        final transient ExpressionCompileError m_cause;

        RuntimeSyntaxError(final ExpressionCompileError cause) {
            m_cause = cause;
        }

        ExpressionCompileException toExpressionCompileException() {
            return new ExpressionCompileException(List.of(m_cause));
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
            // Handle unary minus on numbers
            if (op == UnaryOperator.MINUS) {
                if (arg instanceof IntegerConstant intArg) {
                    return integerConstant(-intArg.value(), createData(getLocation(ctx)));
                }
                if (arg instanceof FloatConstant floatArg) {
                    return floatConstant(-floatArg.value(), createData(getLocation(ctx)));
                }
            }
            return unaryOp(op, arg, createData(getLocation(ctx)));
        }

        @Override
        public Ast visitBinaryOp(final BinaryOpContext ctx) {
            var arg1 = ctx.getChild(0).accept(this);
            var arg2 = ctx.getChild(2).accept(this);
            var op = mapBinaryOperator(ctx.op);
            return binaryOp(op, arg1, arg2, createData(getLocation(ctx)));
        }

        @Override
        public Ast visitColAccess(final ColAccessContext ctx) {
            var col = ctx.shortName != null ? ctx.shortName.getText().substring(1) : parseStringLiteral(ctx.longName);

            var offsetSign = ctx.minus == null ? 1 : -1;
            var offsetValue = ctx.offset != null ? Long.parseLong(ctx.offset.getText().replace("_", "")) : 0;
            var offset = offsetSign * offsetValue;

            return columnAccess(col, offset, createData(getLocation(ctx)));
        }

        @Override
        public Ast visitFlowVarAccess(final FlowVarAccessContext ctx) {
            var name = ctx.shortName != null ? ctx.shortName.getText().substring(2) : parseStringLiteral(ctx.longName);
            return flowVarAccess(name, createData(getLocation(ctx)));
        }

        @Override
        public ConstantAst visitConstant(final KnimeExpressionParser.ConstantContext ctx) {
            try {
                return ExpressionConstants.valueOf(ctx.getText()).toAst(createData(getLocation(ctx)));
            } catch (IllegalArgumentException e) {
                throw syntaxError("Unexpected constant: " + ctx.getText() + "\nAvailable Constants: "
                    + ExpressionConstants.availableConstants(), getLocation(ctx));
            }
        }

        private static String getMissingExpressionOperatorErrorMessage(final String name) {

            var possibleFunctions = NamedOperatorFuzzyMatching.findMostSimilarlyNamedOperators(name,
                BuiltInFunctions.BUILT_IN_FUNCTIONS_MAP);
            var possibleAggregations = NamedOperatorFuzzyMatching.findMostSimilarlyNamedOperators(name,
                BuiltInAggregations.BUILT_IN_AGGREGATIONS_MAP);

            var allPossibleAlternatives = new ArrayList<String>();

            allPossibleAlternatives.addAll(possibleFunctions);
            allPossibleAlternatives.addAll(possibleAggregations);

            return switch (allPossibleAlternatives.size()) {
                case 0 -> "No function or aggregation with name %s.".formatted(name);
                case 1 -> "No function or aggregation with name %s. Did you mean %s?" //
                    .formatted(name, allPossibleAlternatives.get(0));
                default -> "No function or aggregation with name %s. Did you mean %s, or %s?" //
                    .formatted(name,
                        String.join(",", allPossibleAlternatives.subList(0, allPossibleAlternatives.size() - 1)), //
                        allPossibleAlternatives.get(allPossibleAlternatives.size() - 1));
            };
        }

        @Override
        public Ast visitFunctionOrAggregationCall(final FunctionOrAggregationCallContext ctx) {

            var resolvedFunction = Optional.ofNullable(BuiltInFunctions.BUILT_IN_FUNCTIONS_MAP.get(ctx.name.getText()));
            var resolvedAggregation =
                Optional.ofNullable(BuiltInAggregations.BUILT_IN_AGGREGATIONS_MAP.get(ctx.name.getText()));

            if (resolvedFunction.isEmpty() && resolvedAggregation.isEmpty()) {
                throw typingError(getMissingExpressionOperatorErrorMessage(ctx.name.getText()), getLocation(ctx));
            }

            if (!areArgumentsOrderedCorrectly(ctx)) {
                throw syntaxError("Named arguments must be after positional arguments.", getLocation(ctx));
            }

            if (resolvedFunction.isPresent() && resolvedAggregation.isPresent()) {
                throw new IllegalStateException("Ambiguous function or aggregation: " + ctx.name.getText()
                    + ". Found a matching function " + resolvedFunction.get().description().name()
                    + " and a matching aggregation " + resolvedAggregation.get().description().name()
                    + ". This ambiguity stems from the provided lists of available functions and aggregations.");
            }

            if (resolvedAggregation.isPresent()) {
                return visitAggregationCall(ctx, resolvedAggregation.get());
            }

            return visitFunctionCall(ctx, resolvedFunction.get());
        }

        private static boolean areArgumentsOrderedCorrectly(final FunctionOrAggregationCallContext ctx) {
            if (ctx.arguments() == null) {
                return true;
            }

            boolean namedArgOccured = false;
            for (int i = 0; i < ctx.arguments().getChildCount(); i++) {

                if (ctx.arguments().getChild(i) instanceof KnimeExpressionParser.NamedArgumentContext) {
                    namedArgOccured = true;
                }
                if (ctx.arguments().getChild(i) instanceof KnimeExpressionParser.PositionalArgumentContext
                    && namedArgOccured) {
                    return false;
                }
            }
            return true;
        }

        public Ast visitFunctionCall(final FunctionOrAggregationCallContext ctx, final ExpressionFunction function) {

            Map<String, Ast> namedArgs = ctx.arguments() == null ? Map.of() //
                : ctx.arguments().namedArgument().stream() //
                    .collect(Collectors.toMap(arg -> arg.argName.getText(), arg -> arg.expr().accept(this)));

            List<Ast> positionalArgs = ctx.arguments() == null ? List.of() //
                : ctx.arguments().positionalArgument().stream() //
                    .map(arg -> arg.accept(this)).toList();

            var args = function.signature(positionalArgs, namedArgs)
                .orElseThrow(cause -> syntaxError(cause, getLocation(ctx)));

            return functionCall(function, args, createData(getLocation(ctx)));
        }

        public static Ast visitAggregationCall(final FunctionOrAggregationCallContext ctx,
            final ColumnAggregation aggregation) {

            List<ConstantAst> positionalArgs = ctx.arguments() == null ? List.of() //
                : ctx.arguments().positionalArgument() //
                    .stream() //
                    .map(arg -> visitAggregationArg(arg.expr())) //
                    .toList();

            Map<String, ConstantAst> namedArgs = ctx.arguments() == null ? Map.of() //
                : ctx.arguments().namedArgument() //
                    .stream() //
                    .collect( //
                        Collectors.toMap( //
                            arg -> arg.argName.getText(), //
                            arg -> visitAggregationArg(arg.expr()) //
                        ) //
                    );

            var args = aggregation.signature(positionalArgs, namedArgs)
                .orElseThrow(cause -> syntaxError(cause, getLocation(ctx)));

            return aggregationCall(aggregation, args, createData(getLocation(ctx)));
        }

        private static ConstantAst visitAggregationArg(final KnimeExpressionParser.ExprContext expr) {
            if (isSpecialColumnAccessor(expr)) {
                throw typingError(
                    "`ROW_ID`, `ROW_INDEX` and `ROW_NUMBER` cannot be used as arguments for aggregation functions.",
                    getLocation(expr));
            }
            if (expr.accept(EXPRESSION_TO_AST_VISITOR) instanceof ConstantAst constantAst) {
                return constantAst;
            }

            throw syntaxError("Aggregation functions only allow literals as arguments.", getLocation(expr));
        }

        private static boolean isSpecialColumnAccessor(final KnimeExpressionParser.ExprContext expr) {
            return (expr.getChildCount() == 1 && expr.getChild(0) instanceof AtomContext atom
                && (atom.ROW_ID() != null || atom.ROW_INDEX() != null || atom.ROW_NUMBER() != null));
        }

        @Override
        public Ast visitParenthesisedExpr(final ParenthesisedExprContext ctx) {
            // Ignore the parentheses terminal symbols
            return ctx.inner.accept(this);
        }

        @Override
        public Ast visitErrorNode(final ErrorNode node) {
            throw syntaxError(node.getText(), getLocation(node.getSymbol()));
        }

        @Override
        public Ast visitTerminal(final TerminalNode node) {
            var symbol = node.getSymbol();
            var data = createData(getLocation(symbol));

            return switch (symbol.getType()) {
                case KnimeExpressionParser.BOOLEAN -> //
                        booleanConstant(Boolean.parseBoolean(symbol.getText()), data);
                case KnimeExpressionParser.INTEGER -> //
                        integerConstant(Long.parseLong(symbol.getText().replace("_", "")), data);
                case KnimeExpressionParser.FLOAT -> //
                        floatConstant(Double.parseDouble(symbol.getText().replace("_", "")), data);
                case KnimeExpressionParser.STRING -> //
                        stringConstant(parseStringLiteral(symbol), data);
                case KnimeExpressionParser.MISSING -> //
                        missingConstant(data);
                case KnimeExpressionParser.ROW_INDEX -> //
                        rowIndex(data);
                case KnimeExpressionParser.ROW_NUMBER -> //
                        binaryOp(BinaryOperator.PLUS, rowIndex(data), integerConstant(1));
                case KnimeExpressionParser.ROW_ID -> //
                        rowId(data);
                default -> throw syntaxError("Unexpected terminal value: " + symbol.getText() + ".",
                    getLocation(symbol));
            };
        }

        /** Parse the string from the text. Removes starting and ending quotes, replaces escape sequences. */
        private static String parseStringLiteral(final Token symbol) {
            var text = symbol.getText();

            // Crop off the double or single quotes
            var content = text.substring(1, text.length() - 1);

            // Replace escape sequences
            return ESC_SEQUENCE_PATTERN.matcher(content).replaceAll(
                matchResult -> Matcher.quoteReplacement(escapeSequenceMapping(matchResult.group(1), symbol)));
        }

        private static RuntimeSyntaxError getUnicodeSyntaxError(final String escapeCharacter, final Token symbol,
            final Character invalidCharacter) {
            String errorMessage = "Invalid unicode escape sequence: '\\" + escapeCharacter + "'.";
            if (invalidCharacter != null) {
                errorMessage += " Character '" + invalidCharacter + "' is not a hexadecimal digit (0-9, a-f, A-F).";
            } else {
                errorMessage += " Expected 4 hexadecimal digits after '\\u'.";
            }
            return syntaxError(errorMessage, getLocation(symbol));
        }

        private static RuntimeSyntaxError getEscapeSequenceSyntaxError(final String escapeCharacter,
            final Token symbol) {
            if (escapeCharacter == null) {
                return syntaxError("Incomplete escape sequence: '\\%s'. Expected %s.".formatted(escapeCharacter,
                    VALID_ESCAPE_SEQUENCES), getLocation(symbol));
            }
            return syntaxError(
                "Invalid escape sequence: '\\%s'. Expected %s.".formatted(escapeCharacter, VALID_ESCAPE_SEQUENCES),
                getLocation(symbol));
        }

        /** @return the value that an escape sequence should be resolved to */
        private static String escapeSequenceMapping(final String escapeCharacter, final Token symbol) {
            if (escapeCharacter.isEmpty()) {
                throw getEscapeSequenceSyntaxError(null, symbol);
            }

            return switch (escapeCharacter.charAt(0)) {
                case '\n' -> ""; // \<newline> -> <newline ignored>
                case '\\' -> "\\"; // \\ -> \
                case '\'' -> "'"; // \' -> '
                case '"' -> "\""; // \" -> "
                case 'b' -> "\b"; // \b -> <ASCII Backspace (BS)>
                case 'n' -> "\n"; // \n -> <ASCII Linefeed (LF)>
                case 'r' -> "\r"; // \r -> <ASCII Carriage Return (CR)>
                case 't' -> "\t"; // \t -> <ASCII Horizontal Tab (TAB)>
                case 'u' -> { // unicode sequence
                    if (escapeCharacter.length() != 5) {
                        throw getUnicodeSyntaxError(escapeCharacter, symbol, null);
                    }
                    String hexadecimalString = escapeCharacter.substring(1, 5);
                    for (int i = 0; i < hexadecimalString.length(); i++) {
                        char ch = hexadecimalString.charAt(i);
                        if (Character.digit(ch, 16) == -1) {
                            throw getUnicodeSyntaxError(escapeCharacter, symbol, ch);
                        }
                    }
                    yield "" + (char)Integer.parseInt(hexadecimalString, 16);
                }
                default -> throw getEscapeSequenceSyntaxError(escapeCharacter, symbol);
            };
        }

        /** Maps a token for a unary operation to the Ast operator */
        private static UnaryOperator mapUnaryOperator(final Token op) {
            return switch (op.getType()) {
                case KnimeExpressionParser.MINUS -> UnaryOperator.MINUS;
                case KnimeExpressionParser.NOT -> UnaryOperator.NOT;
                default -> throw syntaxError("Unexpected unary operator: " + op.getType() + ".", getLocation(op));
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
                case KnimeExpressionParser.DBL_EQUAL -> BinaryOperator.EQUAL_TO;
                case KnimeExpressionParser.NOT_EQUAL -> BinaryOperator.NOT_EQUAL_TO;
                // Logical
                case KnimeExpressionParser.AND -> BinaryOperator.CONDITIONAL_AND;
                case KnimeExpressionParser.OR -> BinaryOperator.CONDITIONAL_OR;
                case KnimeExpressionParser.MISSING_FALLBACK -> BinaryOperator.MISSING_FALLBACK;

                default -> throw syntaxError("Unexpected binary operator: " + op.getType() + ".", getLocation(op));
            };
        }

        /** Create a {@link RuntimeSyntaxError} to throw (will result in a {@link ExpressionCompileException}) */
        private static RuntimeSyntaxError syntaxError(final String message, final TextRange location) {
            return new RuntimeSyntaxError(ExpressionCompileError.syntaxError(message, location));
        }

        /** Create a {@link RuntimeSyntaxError} to throw (will result in a {@link ExpressionCompileException}) */
        private static RuntimeSyntaxError typingError(final String message, final TextRange location) {
            return new RuntimeSyntaxError(ExpressionCompileError.typingError(message, location));
        }

        /** Create a data map containing location data to attach to the Ast */
        private static Map<String, Object> createData(final TextRange location) {
            var data = new HashMap<String, Object>();
            data.put(LOCATION_DATA_KEY, location);
            return data;
        }

        /** Get the location of a rule context */
        private static TextRange getLocation(final ParserRuleContext ctx) {
            return new TextRange(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex() + 1);
        }

        /** Get the location of a token */
        private static TextRange getLocation(final Token token) {
            return new TextRange(token.getStartIndex(), token.getStopIndex() + 1);
        }
    }
}
