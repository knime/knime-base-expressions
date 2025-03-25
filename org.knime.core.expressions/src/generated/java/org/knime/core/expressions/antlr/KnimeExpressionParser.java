// Generated from KnimeExpression.g4 by ANTLR 4.13.2
package org.knime.core.expressions.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class KnimeExpressionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WHITESPACE=1, LINE_COMMENT=2, BOOLEAN=3, INTEGER=4, FLOAT=5, STRING=6, 
		MISSING=7, ROW_INDEX=8, ROW_NUMBER=9, ROW_ID=10, PLUS=11, MINUS=12, MULTIPLY=13, 
		DIVIDE=14, FLOOR_DIVIDE=15, EXPONENTIATE=16, MODULO=17, LESS_THAN=18, 
		LESS_THAN_EQUAL=19, GREATER_THAN=20, GREATER_THAN_EQUAL=21, EQUAL=22, 
		DBL_EQUAL=23, NOT_EQUAL=24, AND=25, OR=26, NOT=27, MISSING_FALLBACK=28, 
		IF_KEYWORD=29, ELSE_KEYWORD=30, IDENTIFIER=31, COLUMN_IDENTIFIER=32, FLOW_VAR_IDENTIFIER=33, 
		FLOW_VARIABLE_ACCESS_START=34, COLUMN_ACCESS_START=35, ACCESS_END=36, 
		COMMA=37, BRACKET_OPEN=38, BRACKET_CLOSE=39, CURLY_OPEN=40, CURLY_CLOSE=41;
	public static final int
		RULE_fullExpr = 0, RULE_atom = 1, RULE_expr = 2, RULE_arguments = 3, RULE_namedArgument = 4, 
		RULE_positionalArgument = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"fullExpr", "atom", "expr", "arguments", "namedArgument", "positionalArgument"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, "'MISSING'", "'$[ROW_INDEX]'", 
			"'$[ROW_NUMBER]'", "'$[ROW_ID]'", "'+'", "'-'", "'*'", "'/'", "'//'", 
			"'**'", "'%'", "'<'", "'<='", "'>'", "'>='", "'='", "'=='", null, "'and'", 
			"'or'", "'not'", "'??'", "'if'", "'else'", null, null, null, "'$$['", 
			"'$['", "']'", "','", "'('", "')'", "'{'", "'}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WHITESPACE", "LINE_COMMENT", "BOOLEAN", "INTEGER", "FLOAT", "STRING", 
			"MISSING", "ROW_INDEX", "ROW_NUMBER", "ROW_ID", "PLUS", "MINUS", "MULTIPLY", 
			"DIVIDE", "FLOOR_DIVIDE", "EXPONENTIATE", "MODULO", "LESS_THAN", "LESS_THAN_EQUAL", 
			"GREATER_THAN", "GREATER_THAN_EQUAL", "EQUAL", "DBL_EQUAL", "NOT_EQUAL", 
			"AND", "OR", "NOT", "MISSING_FALLBACK", "IF_KEYWORD", "ELSE_KEYWORD", 
			"IDENTIFIER", "COLUMN_IDENTIFIER", "FLOW_VAR_IDENTIFIER", "FLOW_VARIABLE_ACCESS_START", 
			"COLUMN_ACCESS_START", "ACCESS_END", "COMMA", "BRACKET_OPEN", "BRACKET_CLOSE", 
			"CURLY_OPEN", "CURLY_CLOSE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "KnimeExpression.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public KnimeExpressionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FullExprContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(KnimeExpressionParser.EOF, 0); }
		public FullExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fullExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterFullExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitFullExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitFullExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FullExprContext fullExpr() throws RecognitionException {
		FullExprContext _localctx = new FullExprContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_fullExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(12);
			expr(0);
			setState(13);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AtomContext extends ParserRuleContext {
		public TerminalNode BOOLEAN() { return getToken(KnimeExpressionParser.BOOLEAN, 0); }
		public TerminalNode INTEGER() { return getToken(KnimeExpressionParser.INTEGER, 0); }
		public TerminalNode FLOAT() { return getToken(KnimeExpressionParser.FLOAT, 0); }
		public TerminalNode STRING() { return getToken(KnimeExpressionParser.STRING, 0); }
		public TerminalNode MISSING() { return getToken(KnimeExpressionParser.MISSING, 0); }
		public TerminalNode ROW_INDEX() { return getToken(KnimeExpressionParser.ROW_INDEX, 0); }
		public TerminalNode ROW_NUMBER() { return getToken(KnimeExpressionParser.ROW_NUMBER, 0); }
		public TerminalNode ROW_ID() { return getToken(KnimeExpressionParser.ROW_ID, 0); }
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_atom);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(15);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2040L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionOrAggregationCallContext extends ExprContext {
		public Token name;
		public TerminalNode BRACKET_OPEN() { return getToken(KnimeExpressionParser.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(KnimeExpressionParser.BRACKET_CLOSE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(KnimeExpressionParser.IDENTIFIER, 0); }
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public FunctionOrAggregationCallContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterFunctionOrAggregationCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitFunctionOrAggregationCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitFunctionOrAggregationCall(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesisedExprContext extends ExprContext {
		public ExprContext inner;
		public TerminalNode BRACKET_OPEN() { return getToken(KnimeExpressionParser.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(KnimeExpressionParser.BRACKET_CLOSE, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ParenthesisedExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterParenthesisedExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitParenthesisedExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitParenthesisedExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BinaryOpContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode MISSING_FALLBACK() { return getToken(KnimeExpressionParser.MISSING_FALLBACK, 0); }
		public TerminalNode EXPONENTIATE() { return getToken(KnimeExpressionParser.EXPONENTIATE, 0); }
		public TerminalNode MULTIPLY() { return getToken(KnimeExpressionParser.MULTIPLY, 0); }
		public TerminalNode DIVIDE() { return getToken(KnimeExpressionParser.DIVIDE, 0); }
		public TerminalNode MODULO() { return getToken(KnimeExpressionParser.MODULO, 0); }
		public TerminalNode FLOOR_DIVIDE() { return getToken(KnimeExpressionParser.FLOOR_DIVIDE, 0); }
		public TerminalNode PLUS() { return getToken(KnimeExpressionParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(KnimeExpressionParser.MINUS, 0); }
		public TerminalNode LESS_THAN() { return getToken(KnimeExpressionParser.LESS_THAN, 0); }
		public TerminalNode LESS_THAN_EQUAL() { return getToken(KnimeExpressionParser.LESS_THAN_EQUAL, 0); }
		public TerminalNode GREATER_THAN() { return getToken(KnimeExpressionParser.GREATER_THAN, 0); }
		public TerminalNode GREATER_THAN_EQUAL() { return getToken(KnimeExpressionParser.GREATER_THAN_EQUAL, 0); }
		public TerminalNode EQUAL() { return getToken(KnimeExpressionParser.EQUAL, 0); }
		public TerminalNode DBL_EQUAL() { return getToken(KnimeExpressionParser.DBL_EQUAL, 0); }
		public TerminalNode NOT_EQUAL() { return getToken(KnimeExpressionParser.NOT_EQUAL, 0); }
		public TerminalNode AND() { return getToken(KnimeExpressionParser.AND, 0); }
		public TerminalNode OR() { return getToken(KnimeExpressionParser.OR, 0); }
		public BinaryOpContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterBinaryOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitBinaryOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitBinaryOp(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ColAccessContext extends ExprContext {
		public Token shortName;
		public Token longName;
		public Token minus;
		public Token offset;
		public TerminalNode ACCESS_END() { return getToken(KnimeExpressionParser.ACCESS_END, 0); }
		public TerminalNode COLUMN_IDENTIFIER() { return getToken(KnimeExpressionParser.COLUMN_IDENTIFIER, 0); }
		public TerminalNode STRING() { return getToken(KnimeExpressionParser.STRING, 0); }
		public List<TerminalNode> COLUMN_ACCESS_START() { return getTokens(KnimeExpressionParser.COLUMN_ACCESS_START); }
		public TerminalNode COLUMN_ACCESS_START(int i) {
			return getToken(KnimeExpressionParser.COLUMN_ACCESS_START, i);
		}
		public TerminalNode INTEGER() { return getToken(KnimeExpressionParser.INTEGER, 0); }
		public List<TerminalNode> COMMA() { return getTokens(KnimeExpressionParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KnimeExpressionParser.COMMA, i);
		}
		public TerminalNode MINUS() { return getToken(KnimeExpressionParser.MINUS, 0); }
		public ColAccessContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterColAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitColAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitColAccess(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstantContext extends ExprContext {
		public Token constant;
		public TerminalNode IDENTIFIER() { return getToken(KnimeExpressionParser.IDENTIFIER, 0); }
		public ConstantContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitConstant(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FlowVarAccessContext extends ExprContext {
		public Token shortName;
		public Token longName;
		public TerminalNode ACCESS_END() { return getToken(KnimeExpressionParser.ACCESS_END, 0); }
		public TerminalNode FLOW_VAR_IDENTIFIER() { return getToken(KnimeExpressionParser.FLOW_VAR_IDENTIFIER, 0); }
		public TerminalNode STRING() { return getToken(KnimeExpressionParser.STRING, 0); }
		public List<TerminalNode> FLOW_VARIABLE_ACCESS_START() { return getTokens(KnimeExpressionParser.FLOW_VARIABLE_ACCESS_START); }
		public TerminalNode FLOW_VARIABLE_ACCESS_START(int i) {
			return getToken(KnimeExpressionParser.FLOW_VARIABLE_ACCESS_START, i);
		}
		public FlowVarAccessContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterFlowVarAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitFlowVarAccess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitFlowVarAccess(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AtomExprContext extends ExprContext {
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public AtomExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterAtomExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitAtomExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitAtomExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IfFunctionCallContext extends ExprContext {
		public TerminalNode IF_KEYWORD() { return getToken(KnimeExpressionParser.IF_KEYWORD, 0); }
		public TerminalNode BRACKET_OPEN() { return getToken(KnimeExpressionParser.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(KnimeExpressionParser.BRACKET_CLOSE, 0); }
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public IfFunctionCallContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterIfFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitIfFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitIfFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UnaryOpContext extends ExprContext {
		public Token op;
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(KnimeExpressionParser.MINUS, 0); }
		public TerminalNode NOT() { return getToken(KnimeExpressionParser.NOT, 0); }
		public UnaryOpContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterUnaryOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitUnaryOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitUnaryOp(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IfElseContext extends ExprContext {
		public ExprContext condition;
		public ExprContext thenBranch;
		public ExprContext elseBranch;
		public TerminalNode IF_KEYWORD() { return getToken(KnimeExpressionParser.IF_KEYWORD, 0); }
		public TerminalNode BRACKET_OPEN() { return getToken(KnimeExpressionParser.BRACKET_OPEN, 0); }
		public TerminalNode BRACKET_CLOSE() { return getToken(KnimeExpressionParser.BRACKET_CLOSE, 0); }
		public List<TerminalNode> CURLY_OPEN() { return getTokens(KnimeExpressionParser.CURLY_OPEN); }
		public TerminalNode CURLY_OPEN(int i) {
			return getToken(KnimeExpressionParser.CURLY_OPEN, i);
		}
		public List<TerminalNode> CURLY_CLOSE() { return getTokens(KnimeExpressionParser.CURLY_CLOSE); }
		public TerminalNode CURLY_CLOSE(int i) {
			return getToken(KnimeExpressionParser.CURLY_CLOSE, i);
		}
		public TerminalNode ELSE_KEYWORD() { return getToken(KnimeExpressionParser.ELSE_KEYWORD, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public IfElseContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterIfElse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitIfElse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitIfElse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 4;
		enterRecursionRule(_localctx, 4, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(83);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				{
				_localctx = new FlowVarAccessContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(26);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case FLOW_VAR_IDENTIFIER:
					{
					setState(18);
					((FlowVarAccessContext)_localctx).shortName = match(FLOW_VAR_IDENTIFIER);
					}
					break;
				case FLOW_VARIABLE_ACCESS_START:
					{
					setState(20); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(19);
						match(FLOW_VARIABLE_ACCESS_START);
						}
						}
						setState(22); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==FLOW_VARIABLE_ACCESS_START );
					setState(24);
					((FlowVarAccessContext)_localctx).longName = match(STRING);
					setState(25);
					match(ACCESS_END);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 2:
				{
				_localctx = new ColAccessContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(47);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case COLUMN_IDENTIFIER:
					{
					setState(28);
					((ColAccessContext)_localctx).shortName = match(COLUMN_IDENTIFIER);
					}
					break;
				case COLUMN_ACCESS_START:
					{
					setState(30); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(29);
						match(COLUMN_ACCESS_START);
						}
						}
						setState(32); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==COLUMN_ACCESS_START );
					setState(34);
					((ColAccessContext)_localctx).longName = match(STRING);
					setState(44);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(36); 
						_errHandler.sync(this);
						_la = _input.LA(1);
						do {
							{
							{
							setState(35);
							match(COMMA);
							}
							}
							setState(38); 
							_errHandler.sync(this);
							_la = _input.LA(1);
						} while ( _la==COMMA );
						setState(41);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==MINUS) {
							{
							setState(40);
							((ColAccessContext)_localctx).minus = match(MINUS);
							}
						}

						setState(43);
						((ColAccessContext)_localctx).offset = match(INTEGER);
						}
					}

					setState(46);
					match(ACCESS_END);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case 3:
				{
				_localctx = new ConstantContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(49);
				((ConstantContext)_localctx).constant = match(IDENTIFIER);
				}
				break;
			case 4:
				{
				_localctx = new IfElseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(50);
				match(IF_KEYWORD);
				setState(51);
				match(BRACKET_OPEN);
				setState(52);
				((IfElseContext)_localctx).condition = expr(0);
				setState(53);
				match(BRACKET_CLOSE);
				setState(54);
				match(CURLY_OPEN);
				setState(55);
				((IfElseContext)_localctx).thenBranch = expr(0);
				setState(56);
				match(CURLY_CLOSE);
				setState(57);
				match(ELSE_KEYWORD);
				setState(58);
				match(CURLY_OPEN);
				setState(59);
				((IfElseContext)_localctx).elseBranch = expr(0);
				setState(60);
				match(CURLY_CLOSE);
				}
				break;
			case 5:
				{
				_localctx = new IfFunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(62);
				match(IF_KEYWORD);
				setState(63);
				match(BRACKET_OPEN);
				setState(65);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 342120994808L) != 0)) {
					{
					setState(64);
					arguments();
					}
				}

				setState(67);
				match(BRACKET_CLOSE);
				}
				break;
			case 6:
				{
				_localctx = new FunctionOrAggregationCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(68);
				((FunctionOrAggregationCallContext)_localctx).name = match(IDENTIFIER);
				setState(69);
				match(BRACKET_OPEN);
				setState(71);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 342120994808L) != 0)) {
					{
					setState(70);
					arguments();
					}
				}

				setState(73);
				match(BRACKET_CLOSE);
				}
				break;
			case 7:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(74);
				((UnaryOpContext)_localctx).op = match(MINUS);
				setState(75);
				expr(9);
				}
				break;
			case 8:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(76);
				((UnaryOpContext)_localctx).op = match(NOT);
				setState(77);
				expr(5);
				}
				break;
			case 9:
				{
				_localctx = new ParenthesisedExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(78);
				match(BRACKET_OPEN);
				setState(79);
				((ParenthesisedExprContext)_localctx).inner = expr(0);
				setState(80);
				match(BRACKET_CLOSE);
				}
				break;
			case 10:
				{
				_localctx = new AtomExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(82);
				atom();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(108);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(106);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(85);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(86);
						((BinaryOpContext)_localctx).op = match(MISSING_FALLBACK);
						setState(87);
						expr(12);
						}
						break;
					case 2:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(88);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(89);
						((BinaryOpContext)_localctx).op = match(EXPONENTIATE);
						setState(90);
						expr(10);
						}
						break;
					case 3:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(91);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(92);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 188416L) != 0)) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(93);
						expr(9);
						}
						break;
					case 4:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(94);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(95);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS || _la==MINUS) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(96);
						expr(8);
						}
						break;
					case 5:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(97);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(98);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 33292288L) != 0)) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(99);
						expr(7);
						}
						break;
					case 6:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(100);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(101);
						((BinaryOpContext)_localctx).op = match(AND);
						setState(102);
						expr(5);
						}
						break;
					case 7:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(103);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(104);
						((BinaryOpContext)_localctx).op = match(OR);
						setState(105);
						expr(4);
						}
						break;
					}
					} 
				}
				setState(110);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentsContext extends ParserRuleContext {
		public List<NamedArgumentContext> namedArgument() {
			return getRuleContexts(NamedArgumentContext.class);
		}
		public NamedArgumentContext namedArgument(int i) {
			return getRuleContext(NamedArgumentContext.class,i);
		}
		public List<PositionalArgumentContext> positionalArgument() {
			return getRuleContexts(PositionalArgumentContext.class);
		}
		public PositionalArgumentContext positionalArgument(int i) {
			return getRuleContext(PositionalArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KnimeExpressionParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KnimeExpressionParser.COMMA, i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_arguments);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(113);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				{
				setState(111);
				namedArgument();
				}
				break;
			case 2:
				{
				setState(112);
				positionalArgument();
				}
				break;
			}
			setState(122);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(115);
					match(COMMA);
					setState(118);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
					case 1:
						{
						setState(116);
						namedArgument();
						}
						break;
					case 2:
						{
						setState(117);
						positionalArgument();
						}
						break;
					}
					}
					} 
				}
				setState(124);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			}
			setState(126);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(125);
				match(COMMA);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NamedArgumentContext extends ParserRuleContext {
		public Token argName;
		public TerminalNode EQUAL() { return getToken(KnimeExpressionParser.EQUAL, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(KnimeExpressionParser.IDENTIFIER, 0); }
		public NamedArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterNamedArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitNamedArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitNamedArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedArgumentContext namedArgument() throws RecognitionException {
		NamedArgumentContext _localctx = new NamedArgumentContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_namedArgument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(128);
			((NamedArgumentContext)_localctx).argName = match(IDENTIFIER);
			setState(129);
			match(EQUAL);
			setState(130);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PositionalArgumentContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public PositionalArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_positionalArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterPositionalArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitPositionalArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitPositionalArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PositionalArgumentContext positionalArgument() throws RecognitionException {
		PositionalArgumentContext _localctx = new PositionalArgumentContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_positionalArgument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(132);
			expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 2:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 11);
		case 1:
			return precpred(_ctx, 10);
		case 2:
			return precpred(_ctx, 8);
		case 3:
			return precpred(_ctx, 7);
		case 4:
			return precpred(_ctx, 6);
		case 5:
			return precpred(_ctx, 4);
		case 6:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001)\u0087\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0004\u0002\u0015\b\u0002\u000b"+
		"\u0002\f\u0002\u0016\u0001\u0002\u0001\u0002\u0003\u0002\u001b\b\u0002"+
		"\u0001\u0002\u0001\u0002\u0004\u0002\u001f\b\u0002\u000b\u0002\f\u0002"+
		" \u0001\u0002\u0001\u0002\u0004\u0002%\b\u0002\u000b\u0002\f\u0002&\u0001"+
		"\u0002\u0003\u0002*\b\u0002\u0001\u0002\u0003\u0002-\b\u0002\u0001\u0002"+
		"\u0003\u00020\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002B\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002H\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002T\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002"+
		"k\b\u0002\n\u0002\f\u0002n\t\u0002\u0001\u0003\u0001\u0003\u0003\u0003"+
		"r\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003w\b\u0003\u0005"+
		"\u0003y\b\u0003\n\u0003\f\u0003|\t\u0003\u0001\u0003\u0003\u0003\u007f"+
		"\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0000\u0001\u0004\u0006\u0000\u0002\u0004\u0006\b\n"+
		"\u0000\u0004\u0001\u0000\u0003\n\u0002\u0000\r\u000f\u0011\u0011\u0001"+
		"\u0000\u000b\f\u0001\u0000\u0012\u0018\u009d\u0000\f\u0001\u0000\u0000"+
		"\u0000\u0002\u000f\u0001\u0000\u0000\u0000\u0004S\u0001\u0000\u0000\u0000"+
		"\u0006q\u0001\u0000\u0000\u0000\b\u0080\u0001\u0000\u0000\u0000\n\u0084"+
		"\u0001\u0000\u0000\u0000\f\r\u0003\u0004\u0002\u0000\r\u000e\u0005\u0000"+
		"\u0000\u0001\u000e\u0001\u0001\u0000\u0000\u0000\u000f\u0010\u0007\u0000"+
		"\u0000\u0000\u0010\u0003\u0001\u0000\u0000\u0000\u0011\u001a\u0006\u0002"+
		"\uffff\uffff\u0000\u0012\u001b\u0005!\u0000\u0000\u0013\u0015\u0005\""+
		"\u0000\u0000\u0014\u0013\u0001\u0000\u0000\u0000\u0015\u0016\u0001\u0000"+
		"\u0000\u0000\u0016\u0014\u0001\u0000\u0000\u0000\u0016\u0017\u0001\u0000"+
		"\u0000\u0000\u0017\u0018\u0001\u0000\u0000\u0000\u0018\u0019\u0005\u0006"+
		"\u0000\u0000\u0019\u001b\u0005$\u0000\u0000\u001a\u0012\u0001\u0000\u0000"+
		"\u0000\u001a\u0014\u0001\u0000\u0000\u0000\u001bT\u0001\u0000\u0000\u0000"+
		"\u001c0\u0005 \u0000\u0000\u001d\u001f\u0005#\u0000\u0000\u001e\u001d"+
		"\u0001\u0000\u0000\u0000\u001f \u0001\u0000\u0000\u0000 \u001e\u0001\u0000"+
		"\u0000\u0000 !\u0001\u0000\u0000\u0000!\"\u0001\u0000\u0000\u0000\",\u0005"+
		"\u0006\u0000\u0000#%\u0005%\u0000\u0000$#\u0001\u0000\u0000\u0000%&\u0001"+
		"\u0000\u0000\u0000&$\u0001\u0000\u0000\u0000&\'\u0001\u0000\u0000\u0000"+
		"\')\u0001\u0000\u0000\u0000(*\u0005\f\u0000\u0000)(\u0001\u0000\u0000"+
		"\u0000)*\u0001\u0000\u0000\u0000*+\u0001\u0000\u0000\u0000+-\u0005\u0004"+
		"\u0000\u0000,$\u0001\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-.\u0001"+
		"\u0000\u0000\u0000.0\u0005$\u0000\u0000/\u001c\u0001\u0000\u0000\u0000"+
		"/\u001e\u0001\u0000\u0000\u00000T\u0001\u0000\u0000\u00001T\u0005\u001f"+
		"\u0000\u000023\u0005\u001d\u0000\u000034\u0005&\u0000\u000045\u0003\u0004"+
		"\u0002\u000056\u0005\'\u0000\u000067\u0005(\u0000\u000078\u0003\u0004"+
		"\u0002\u000089\u0005)\u0000\u00009:\u0005\u001e\u0000\u0000:;\u0005(\u0000"+
		"\u0000;<\u0003\u0004\u0002\u0000<=\u0005)\u0000\u0000=T\u0001\u0000\u0000"+
		"\u0000>?\u0005\u001d\u0000\u0000?A\u0005&\u0000\u0000@B\u0003\u0006\u0003"+
		"\u0000A@\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000BC\u0001\u0000"+
		"\u0000\u0000CT\u0005\'\u0000\u0000DE\u0005\u001f\u0000\u0000EG\u0005&"+
		"\u0000\u0000FH\u0003\u0006\u0003\u0000GF\u0001\u0000\u0000\u0000GH\u0001"+
		"\u0000\u0000\u0000HI\u0001\u0000\u0000\u0000IT\u0005\'\u0000\u0000JK\u0005"+
		"\f\u0000\u0000KT\u0003\u0004\u0002\tLM\u0005\u001b\u0000\u0000MT\u0003"+
		"\u0004\u0002\u0005NO\u0005&\u0000\u0000OP\u0003\u0004\u0002\u0000PQ\u0005"+
		"\'\u0000\u0000QT\u0001\u0000\u0000\u0000RT\u0003\u0002\u0001\u0000S\u0011"+
		"\u0001\u0000\u0000\u0000S/\u0001\u0000\u0000\u0000S1\u0001\u0000\u0000"+
		"\u0000S2\u0001\u0000\u0000\u0000S>\u0001\u0000\u0000\u0000SD\u0001\u0000"+
		"\u0000\u0000SJ\u0001\u0000\u0000\u0000SL\u0001\u0000\u0000\u0000SN\u0001"+
		"\u0000\u0000\u0000SR\u0001\u0000\u0000\u0000Tl\u0001\u0000\u0000\u0000"+
		"UV\n\u000b\u0000\u0000VW\u0005\u001c\u0000\u0000Wk\u0003\u0004\u0002\f"+
		"XY\n\n\u0000\u0000YZ\u0005\u0010\u0000\u0000Zk\u0003\u0004\u0002\n[\\"+
		"\n\b\u0000\u0000\\]\u0007\u0001\u0000\u0000]k\u0003\u0004\u0002\t^_\n"+
		"\u0007\u0000\u0000_`\u0007\u0002\u0000\u0000`k\u0003\u0004\u0002\bab\n"+
		"\u0006\u0000\u0000bc\u0007\u0003\u0000\u0000ck\u0003\u0004\u0002\u0007"+
		"de\n\u0004\u0000\u0000ef\u0005\u0019\u0000\u0000fk\u0003\u0004\u0002\u0005"+
		"gh\n\u0003\u0000\u0000hi\u0005\u001a\u0000\u0000ik\u0003\u0004\u0002\u0004"+
		"jU\u0001\u0000\u0000\u0000jX\u0001\u0000\u0000\u0000j[\u0001\u0000\u0000"+
		"\u0000j^\u0001\u0000\u0000\u0000ja\u0001\u0000\u0000\u0000jd\u0001\u0000"+
		"\u0000\u0000jg\u0001\u0000\u0000\u0000kn\u0001\u0000\u0000\u0000lj\u0001"+
		"\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000m\u0005\u0001\u0000\u0000"+
		"\u0000nl\u0001\u0000\u0000\u0000or\u0003\b\u0004\u0000pr\u0003\n\u0005"+
		"\u0000qo\u0001\u0000\u0000\u0000qp\u0001\u0000\u0000\u0000rz\u0001\u0000"+
		"\u0000\u0000sv\u0005%\u0000\u0000tw\u0003\b\u0004\u0000uw\u0003\n\u0005"+
		"\u0000vt\u0001\u0000\u0000\u0000vu\u0001\u0000\u0000\u0000wy\u0001\u0000"+
		"\u0000\u0000xs\u0001\u0000\u0000\u0000y|\u0001\u0000\u0000\u0000zx\u0001"+
		"\u0000\u0000\u0000z{\u0001\u0000\u0000\u0000{~\u0001\u0000\u0000\u0000"+
		"|z\u0001\u0000\u0000\u0000}\u007f\u0005%\u0000\u0000~}\u0001\u0000\u0000"+
		"\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0007\u0001\u0000\u0000\u0000"+
		"\u0080\u0081\u0005\u001f\u0000\u0000\u0081\u0082\u0005\u0016\u0000\u0000"+
		"\u0082\u0083\u0003\u0004\u0002\u0000\u0083\t\u0001\u0000\u0000\u0000\u0084"+
		"\u0085\u0003\u0004\u0002\u0000\u0085\u000b\u0001\u0000\u0000\u0000\u0010"+
		"\u0016\u001a &),/AGSjlqvz~";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}