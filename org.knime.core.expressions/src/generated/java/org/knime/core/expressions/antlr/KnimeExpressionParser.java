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
		IDENTIFIER=29, COLUMN_IDENTIFIER=30, FLOW_VAR_IDENTIFIER=31, FLOW_VARIABLE_ACCESS_START=32, 
		COLUMN_ACCESS_START=33, ACCESS_END=34, COMMA=35, BRACKET_OPEN=36, BRACKET_CLOSE=37;
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
			"'**'", "'%'", "'<'", "'<='", "'>'", "'>='", "'='", "'=='", null, null, 
			null, null, "'??'", null, null, null, "'$$['", "'$['", "']'", "','", 
			"'('", "')'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WHITESPACE", "LINE_COMMENT", "BOOLEAN", "INTEGER", "FLOAT", "STRING", 
			"MISSING", "ROW_INDEX", "ROW_NUMBER", "ROW_ID", "PLUS", "MINUS", "MULTIPLY", 
			"DIVIDE", "FLOOR_DIVIDE", "EXPONENTIATE", "MODULO", "LESS_THAN", "LESS_THAN_EQUAL", 
			"GREATER_THAN", "GREATER_THAN_EQUAL", "EQUAL", "DBL_EQUAL", "NOT_EQUAL", 
			"AND", "OR", "NOT", "MISSING_FALLBACK", "IDENTIFIER", "COLUMN_IDENTIFIER", 
			"FLOW_VAR_IDENTIFIER", "FLOW_VARIABLE_ACCESS_START", "COLUMN_ACCESS_START", 
			"ACCESS_END", "COMMA", "BRACKET_OPEN", "BRACKET_CLOSE"
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
			setState(65);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
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
				_localctx = new FunctionOrAggregationCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(50);
				((FunctionOrAggregationCallContext)_localctx).name = match(IDENTIFIER);
				setState(51);
				match(BRACKET_OPEN);
				setState(53);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 85496698872L) != 0)) {
					{
					setState(52);
					arguments();
					}
				}

				setState(55);
				match(BRACKET_CLOSE);
				}
				break;
			case 5:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(56);
				((UnaryOpContext)_localctx).op = match(MINUS);
				setState(57);
				expr(9);
				}
				break;
			case 6:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(58);
				((UnaryOpContext)_localctx).op = match(NOT);
				setState(59);
				expr(5);
				}
				break;
			case 7:
				{
				_localctx = new ParenthesisedExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(60);
				match(BRACKET_OPEN);
				setState(61);
				((ParenthesisedExprContext)_localctx).inner = expr(0);
				setState(62);
				match(BRACKET_CLOSE);
				}
				break;
			case 8:
				{
				_localctx = new AtomExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(64);
				atom();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(90);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(88);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(67);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(68);
						((BinaryOpContext)_localctx).op = match(MISSING_FALLBACK);
						setState(69);
						expr(12);
						}
						break;
					case 2:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(70);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(71);
						((BinaryOpContext)_localctx).op = match(EXPONENTIATE);
						setState(72);
						expr(10);
						}
						break;
					case 3:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(73);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(74);
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
						setState(75);
						expr(9);
						}
						break;
					case 4:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(76);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(77);
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
						setState(78);
						expr(8);
						}
						break;
					case 5:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(79);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(80);
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
						setState(81);
						expr(7);
						}
						break;
					case 6:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(82);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(83);
						((BinaryOpContext)_localctx).op = match(AND);
						setState(84);
						expr(5);
						}
						break;
					case 7:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(85);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(86);
						((BinaryOpContext)_localctx).op = match(OR);
						setState(87);
						expr(4);
						}
						break;
					}
					} 
				}
				setState(92);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
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
			setState(95);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(93);
				namedArgument();
				}
				break;
			case 2:
				{
				setState(94);
				positionalArgument();
				}
				break;
			}
			setState(104);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(97);
					match(COMMA);
					setState(100);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
					case 1:
						{
						setState(98);
						namedArgument();
						}
						break;
					case 2:
						{
						setState(99);
						positionalArgument();
						}
						break;
					}
					}
					} 
				}
				setState(106);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			}
			setState(108);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(107);
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
			setState(110);
			((NamedArgumentContext)_localctx).argName = match(IDENTIFIER);
			setState(111);
			match(EQUAL);
			setState(112);
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
			setState(114);
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
		"\u0004\u0001%u\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002\u0002"+
		"\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002\u0005"+
		"\u0007\u0005\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0004\u0002\u0015\b\u0002\u000b\u0002"+
		"\f\u0002\u0016\u0001\u0002\u0001\u0002\u0003\u0002\u001b\b\u0002\u0001"+
		"\u0002\u0001\u0002\u0004\u0002\u001f\b\u0002\u000b\u0002\f\u0002 \u0001"+
		"\u0002\u0001\u0002\u0004\u0002%\b\u0002\u000b\u0002\f\u0002&\u0001\u0002"+
		"\u0003\u0002*\b\u0002\u0001\u0002\u0003\u0002-\b\u0002\u0001\u0002\u0003"+
		"\u00020\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003"+
		"\u00026\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003"+
		"\u0002B\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002Y\b"+
		"\u0002\n\u0002\f\u0002\\\t\u0002\u0001\u0003\u0001\u0003\u0003\u0003`"+
		"\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003e\b\u0003\u0005"+
		"\u0003g\b\u0003\n\u0003\f\u0003j\t\u0003\u0001\u0003\u0003\u0003m\b\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0000\u0001\u0004\u0006\u0000\u0002\u0004\u0006\b\n\u0000"+
		"\u0004\u0001\u0000\u0003\n\u0002\u0000\r\u000f\u0011\u0011\u0001\u0000"+
		"\u000b\f\u0001\u0000\u0012\u0018\u0088\u0000\f\u0001\u0000\u0000\u0000"+
		"\u0002\u000f\u0001\u0000\u0000\u0000\u0004A\u0001\u0000\u0000\u0000\u0006"+
		"_\u0001\u0000\u0000\u0000\bn\u0001\u0000\u0000\u0000\nr\u0001\u0000\u0000"+
		"\u0000\f\r\u0003\u0004\u0002\u0000\r\u000e\u0005\u0000\u0000\u0001\u000e"+
		"\u0001\u0001\u0000\u0000\u0000\u000f\u0010\u0007\u0000\u0000\u0000\u0010"+
		"\u0003\u0001\u0000\u0000\u0000\u0011\u001a\u0006\u0002\uffff\uffff\u0000"+
		"\u0012\u001b\u0005\u001f\u0000\u0000\u0013\u0015\u0005 \u0000\u0000\u0014"+
		"\u0013\u0001\u0000\u0000\u0000\u0015\u0016\u0001\u0000\u0000\u0000\u0016"+
		"\u0014\u0001\u0000\u0000\u0000\u0016\u0017\u0001\u0000\u0000\u0000\u0017"+
		"\u0018\u0001\u0000\u0000\u0000\u0018\u0019\u0005\u0006\u0000\u0000\u0019"+
		"\u001b\u0005\"\u0000\u0000\u001a\u0012\u0001\u0000\u0000\u0000\u001a\u0014"+
		"\u0001\u0000\u0000\u0000\u001bB\u0001\u0000\u0000\u0000\u001c0\u0005\u001e"+
		"\u0000\u0000\u001d\u001f\u0005!\u0000\u0000\u001e\u001d\u0001\u0000\u0000"+
		"\u0000\u001f \u0001\u0000\u0000\u0000 \u001e\u0001\u0000\u0000\u0000 "+
		"!\u0001\u0000\u0000\u0000!\"\u0001\u0000\u0000\u0000\",\u0005\u0006\u0000"+
		"\u0000#%\u0005#\u0000\u0000$#\u0001\u0000\u0000\u0000%&\u0001\u0000\u0000"+
		"\u0000&$\u0001\u0000\u0000\u0000&\'\u0001\u0000\u0000\u0000\')\u0001\u0000"+
		"\u0000\u0000(*\u0005\f\u0000\u0000)(\u0001\u0000\u0000\u0000)*\u0001\u0000"+
		"\u0000\u0000*+\u0001\u0000\u0000\u0000+-\u0005\u0004\u0000\u0000,$\u0001"+
		"\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000"+
		".0\u0005\"\u0000\u0000/\u001c\u0001\u0000\u0000\u0000/\u001e\u0001\u0000"+
		"\u0000\u00000B\u0001\u0000\u0000\u00001B\u0005\u001d\u0000\u000023\u0005"+
		"\u001d\u0000\u000035\u0005$\u0000\u000046\u0003\u0006\u0003\u000054\u0001"+
		"\u0000\u0000\u000056\u0001\u0000\u0000\u000067\u0001\u0000\u0000\u0000"+
		"7B\u0005%\u0000\u000089\u0005\f\u0000\u00009B\u0003\u0004\u0002\t:;\u0005"+
		"\u001b\u0000\u0000;B\u0003\u0004\u0002\u0005<=\u0005$\u0000\u0000=>\u0003"+
		"\u0004\u0002\u0000>?\u0005%\u0000\u0000?B\u0001\u0000\u0000\u0000@B\u0003"+
		"\u0002\u0001\u0000A\u0011\u0001\u0000\u0000\u0000A/\u0001\u0000\u0000"+
		"\u0000A1\u0001\u0000\u0000\u0000A2\u0001\u0000\u0000\u0000A8\u0001\u0000"+
		"\u0000\u0000A:\u0001\u0000\u0000\u0000A<\u0001\u0000\u0000\u0000A@\u0001"+
		"\u0000\u0000\u0000BZ\u0001\u0000\u0000\u0000CD\n\u000b\u0000\u0000DE\u0005"+
		"\u001c\u0000\u0000EY\u0003\u0004\u0002\fFG\n\n\u0000\u0000GH\u0005\u0010"+
		"\u0000\u0000HY\u0003\u0004\u0002\nIJ\n\b\u0000\u0000JK\u0007\u0001\u0000"+
		"\u0000KY\u0003\u0004\u0002\tLM\n\u0007\u0000\u0000MN\u0007\u0002\u0000"+
		"\u0000NY\u0003\u0004\u0002\bOP\n\u0006\u0000\u0000PQ\u0007\u0003\u0000"+
		"\u0000QY\u0003\u0004\u0002\u0007RS\n\u0004\u0000\u0000ST\u0005\u0019\u0000"+
		"\u0000TY\u0003\u0004\u0002\u0005UV\n\u0003\u0000\u0000VW\u0005\u001a\u0000"+
		"\u0000WY\u0003\u0004\u0002\u0004XC\u0001\u0000\u0000\u0000XF\u0001\u0000"+
		"\u0000\u0000XI\u0001\u0000\u0000\u0000XL\u0001\u0000\u0000\u0000XO\u0001"+
		"\u0000\u0000\u0000XR\u0001\u0000\u0000\u0000XU\u0001\u0000\u0000\u0000"+
		"Y\\\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000Z[\u0001\u0000\u0000"+
		"\u0000[\u0005\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000]`\u0003"+
		"\b\u0004\u0000^`\u0003\n\u0005\u0000_]\u0001\u0000\u0000\u0000_^\u0001"+
		"\u0000\u0000\u0000`h\u0001\u0000\u0000\u0000ad\u0005#\u0000\u0000be\u0003"+
		"\b\u0004\u0000ce\u0003\n\u0005\u0000db\u0001\u0000\u0000\u0000dc\u0001"+
		"\u0000\u0000\u0000eg\u0001\u0000\u0000\u0000fa\u0001\u0000\u0000\u0000"+
		"gj\u0001\u0000\u0000\u0000hf\u0001\u0000\u0000\u0000hi\u0001\u0000\u0000"+
		"\u0000il\u0001\u0000\u0000\u0000jh\u0001\u0000\u0000\u0000km\u0005#\u0000"+
		"\u0000lk\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000m\u0007\u0001"+
		"\u0000\u0000\u0000no\u0005\u001d\u0000\u0000op\u0005\u0016\u0000\u0000"+
		"pq\u0003\u0004\u0002\u0000q\t\u0001\u0000\u0000\u0000rs\u0003\u0004\u0002"+
		"\u0000s\u000b\u0001\u0000\u0000\u0000\u000f\u0016\u001a &),/5AXZ_dhl";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}