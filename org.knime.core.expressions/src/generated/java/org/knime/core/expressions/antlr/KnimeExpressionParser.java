// Generated from KnimeExpression.g4 by ANTLR 4.13.1
package org.knime.core.expressions.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class KnimeExpressionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, WHITESPACE=8, 
		BOOLEAN=9, INTEGER=10, FLOAT=11, STRING=12, MISSING=13, PLUS=14, MINUS=15, 
		MULTIPLY=16, DIVIDE=17, FLOOR_DIVIDE=18, EXPONENTIATE=19, MODULO=20, LESS_THAN=21, 
		LESS_THAN_EQUAL=22, GREATER_THAN=23, GREATER_THAN_EQUAL=24, EQUAL=25, 
		NOT_EQUAL=26, AND=27, OR=28, NOT=29, IDENTIFIER=30;
	public static final int
		RULE_fullExpr = 0, RULE_expr = 1, RULE_functionArgs = 2;
	private static String[] makeRuleNames() {
		return new String[] {
			"fullExpr", "expr", "functionArgs"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'$$'", "'['", "']'", "'$'", "'('", "')'", "','", null, null, null, 
			null, null, "'MISSING'", "'+'", "'-'", "'*'", "'/'", "'//'", "'**'", 
			"'%'", "'<'", "'<='", "'>'", "'>='", null, null, "'and'", "'or'", "'not'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, "WHITESPACE", "BOOLEAN", 
			"INTEGER", "FLOAT", "STRING", "MISSING", "PLUS", "MINUS", "MULTIPLY", 
			"DIVIDE", "FLOOR_DIVIDE", "EXPONENTIATE", "MODULO", "LESS_THAN", "LESS_THAN_EQUAL", 
			"GREATER_THAN", "GREATER_THAN_EQUAL", "EQUAL", "NOT_EQUAL", "AND", "OR", 
			"NOT", "IDENTIFIER"
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
			setState(6);
			expr(0);
			setState(7);
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
	public static class ParenthesisedExprContext extends ExprContext {
		public ExprContext inner;
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
		public TerminalNode IDENTIFIER() { return getToken(KnimeExpressionParser.IDENTIFIER, 0); }
		public TerminalNode STRING() { return getToken(KnimeExpressionParser.STRING, 0); }
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
	public static class FunctionCallContext extends ExprContext {
		public Token name;
		public TerminalNode IDENTIFIER() { return getToken(KnimeExpressionParser.IDENTIFIER, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public FunctionCallContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FlowVarAccessContext extends ExprContext {
		public Token shortName;
		public Token longName;
		public TerminalNode IDENTIFIER() { return getToken(KnimeExpressionParser.IDENTIFIER, 0); }
		public TerminalNode STRING() { return getToken(KnimeExpressionParser.STRING, 0); }
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
	public static class AtomContext extends ExprContext {
		public TerminalNode BOOLEAN() { return getToken(KnimeExpressionParser.BOOLEAN, 0); }
		public TerminalNode INTEGER() { return getToken(KnimeExpressionParser.INTEGER, 0); }
		public TerminalNode FLOAT() { return getToken(KnimeExpressionParser.FLOAT, 0); }
		public TerminalNode STRING() { return getToken(KnimeExpressionParser.STRING, 0); }
		public TerminalNode MISSING() { return getToken(KnimeExpressionParser.MISSING, 0); }
		public AtomContext(ExprContext ctx) { copyFrom(ctx); }
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

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				{
				_localctx = new FlowVarAccessContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(10);
				match(T__0);
				setState(19);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IDENTIFIER:
					{
					setState(11);
					((FlowVarAccessContext)_localctx).shortName = match(IDENTIFIER);
					}
					break;
				case T__1:
					{
					setState(13); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(12);
						match(T__1);
						}
						}
						setState(15); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==T__1 );
					setState(17);
					((FlowVarAccessContext)_localctx).longName = match(STRING);
					setState(18);
					match(T__2);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case T__3:
				{
				_localctx = new ColAccessContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(21);
				match(T__3);
				setState(30);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IDENTIFIER:
					{
					setState(22);
					((ColAccessContext)_localctx).shortName = match(IDENTIFIER);
					}
					break;
				case T__1:
					{
					setState(24); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(23);
						match(T__1);
						}
						}
						setState(26); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==T__1 );
					setState(28);
					((ColAccessContext)_localctx).longName = match(STRING);
					setState(29);
					match(T__2);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case IDENTIFIER:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(32);
				((FunctionCallContext)_localctx).name = match(IDENTIFIER);
				setState(33);
				match(T__4);
				setState(35);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1610661426L) != 0)) {
					{
					setState(34);
					functionArgs();
					}
				}

				setState(37);
				match(T__5);
				}
				break;
			case MINUS:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(38);
				((UnaryOpContext)_localctx).op = match(MINUS);
				setState(39);
				expr(13);
				}
				break;
			case NOT:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(40);
				((UnaryOpContext)_localctx).op = match(NOT);
				setState(41);
				expr(9);
				}
				break;
			case T__4:
				{
				_localctx = new ParenthesisedExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(42);
				match(T__4);
				setState(43);
				((ParenthesisedExprContext)_localctx).inner = expr(0);
				setState(44);
				match(T__5);
				}
				break;
			case BOOLEAN:
				{
				_localctx = new AtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(46);
				match(BOOLEAN);
				}
				break;
			case INTEGER:
				{
				_localctx = new AtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(47);
				match(INTEGER);
				}
				break;
			case FLOAT:
				{
				_localctx = new AtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(48);
				match(FLOAT);
				}
				break;
			case STRING:
				{
				_localctx = new AtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(49);
				match(STRING);
				}
				break;
			case MISSING:
				{
				_localctx = new AtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(50);
				match(MISSING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(73);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(71);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(53);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(54);
						((BinaryOpContext)_localctx).op = match(EXPONENTIATE);
						setState(55);
						expr(14);
						}
						break;
					case 2:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(56);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(57);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1507328L) != 0)) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(58);
						expr(13);
						}
						break;
					case 3:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(59);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(60);
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
						setState(61);
						expr(12);
						}
						break;
					case 4:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(62);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(63);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 132120576L) != 0)) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(64);
						expr(11);
						}
						break;
					case 5:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(65);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(66);
						((BinaryOpContext)_localctx).op = match(AND);
						setState(67);
						expr(9);
						}
						break;
					case 6:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(68);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(69);
						((BinaryOpContext)_localctx).op = match(OR);
						setState(70);
						expr(8);
						}
						break;
					}
					} 
				}
				setState(75);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
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
	public static class FunctionArgsContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public FunctionArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterFunctionArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitFunctionArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitFunctionArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgsContext functionArgs() throws RecognitionException {
		FunctionArgsContext _localctx = new FunctionArgsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_functionArgs);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			expr(0);
			setState(81);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(77);
					match(T__6);
					setState(78);
					expr(0);
					}
					} 
				}
				setState(83);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			}
			setState(85);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(84);
				match(T__6);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 14);
		case 1:
			return precpred(_ctx, 12);
		case 2:
			return precpred(_ctx, 11);
		case 3:
			return precpred(_ctx, 10);
		case 4:
			return precpred(_ctx, 8);
		case 5:
			return precpred(_ctx, 7);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u001eX\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0004\u0001\u000e\b\u0001\u000b\u0001\f"+
		"\u0001\u000f\u0001\u0001\u0001\u0001\u0003\u0001\u0014\b\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0004\u0001\u0019\b\u0001\u000b\u0001\f\u0001"+
		"\u001a\u0001\u0001\u0001\u0001\u0003\u0001\u001f\b\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0003\u0001$\b\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u00014\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0005\u0001H\b\u0001\n\u0001\f\u0001K\t\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002P\b\u0002\n\u0002\f\u0002S\t"+
		"\u0002\u0001\u0002\u0003\u0002V\b\u0002\u0001\u0002\u0000\u0001\u0002"+
		"\u0003\u0000\u0002\u0004\u0000\u0003\u0002\u0000\u0010\u0012\u0014\u0014"+
		"\u0001\u0000\u000e\u000f\u0001\u0000\u0015\u001ak\u0000\u0006\u0001\u0000"+
		"\u0000\u0000\u00023\u0001\u0000\u0000\u0000\u0004L\u0001\u0000\u0000\u0000"+
		"\u0006\u0007\u0003\u0002\u0001\u0000\u0007\b\u0005\u0000\u0000\u0001\b"+
		"\u0001\u0001\u0000\u0000\u0000\t\n\u0006\u0001\uffff\uffff\u0000\n\u0013"+
		"\u0005\u0001\u0000\u0000\u000b\u0014\u0005\u001e\u0000\u0000\f\u000e\u0005"+
		"\u0002\u0000\u0000\r\f\u0001\u0000\u0000\u0000\u000e\u000f\u0001\u0000"+
		"\u0000\u0000\u000f\r\u0001\u0000\u0000\u0000\u000f\u0010\u0001\u0000\u0000"+
		"\u0000\u0010\u0011\u0001\u0000\u0000\u0000\u0011\u0012\u0005\f\u0000\u0000"+
		"\u0012\u0014\u0005\u0003\u0000\u0000\u0013\u000b\u0001\u0000\u0000\u0000"+
		"\u0013\r\u0001\u0000\u0000\u0000\u00144\u0001\u0000\u0000\u0000\u0015"+
		"\u001e\u0005\u0004\u0000\u0000\u0016\u001f\u0005\u001e\u0000\u0000\u0017"+
		"\u0019\u0005\u0002\u0000\u0000\u0018\u0017\u0001\u0000\u0000\u0000\u0019"+
		"\u001a\u0001\u0000\u0000\u0000\u001a\u0018\u0001\u0000\u0000\u0000\u001a"+
		"\u001b\u0001\u0000\u0000\u0000\u001b\u001c\u0001\u0000\u0000\u0000\u001c"+
		"\u001d\u0005\f\u0000\u0000\u001d\u001f\u0005\u0003\u0000\u0000\u001e\u0016"+
		"\u0001\u0000\u0000\u0000\u001e\u0018\u0001\u0000\u0000\u0000\u001f4\u0001"+
		"\u0000\u0000\u0000 !\u0005\u001e\u0000\u0000!#\u0005\u0005\u0000\u0000"+
		"\"$\u0003\u0004\u0002\u0000#\"\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000"+
		"\u0000$%\u0001\u0000\u0000\u0000%4\u0005\u0006\u0000\u0000&\'\u0005\u000f"+
		"\u0000\u0000\'4\u0003\u0002\u0001\r()\u0005\u001d\u0000\u0000)4\u0003"+
		"\u0002\u0001\t*+\u0005\u0005\u0000\u0000+,\u0003\u0002\u0001\u0000,-\u0005"+
		"\u0006\u0000\u0000-4\u0001\u0000\u0000\u0000.4\u0005\t\u0000\u0000/4\u0005"+
		"\n\u0000\u000004\u0005\u000b\u0000\u000014\u0005\f\u0000\u000024\u0005"+
		"\r\u0000\u00003\t\u0001\u0000\u0000\u00003\u0015\u0001\u0000\u0000\u0000"+
		"3 \u0001\u0000\u0000\u00003&\u0001\u0000\u0000\u00003(\u0001\u0000\u0000"+
		"\u00003*\u0001\u0000\u0000\u00003.\u0001\u0000\u0000\u00003/\u0001\u0000"+
		"\u0000\u000030\u0001\u0000\u0000\u000031\u0001\u0000\u0000\u000032\u0001"+
		"\u0000\u0000\u00004I\u0001\u0000\u0000\u000056\n\u000e\u0000\u000067\u0005"+
		"\u0013\u0000\u00007H\u0003\u0002\u0001\u000e89\n\f\u0000\u00009:\u0007"+
		"\u0000\u0000\u0000:H\u0003\u0002\u0001\r;<\n\u000b\u0000\u0000<=\u0007"+
		"\u0001\u0000\u0000=H\u0003\u0002\u0001\f>?\n\n\u0000\u0000?@\u0007\u0002"+
		"\u0000\u0000@H\u0003\u0002\u0001\u000bAB\n\b\u0000\u0000BC\u0005\u001b"+
		"\u0000\u0000CH\u0003\u0002\u0001\tDE\n\u0007\u0000\u0000EF\u0005\u001c"+
		"\u0000\u0000FH\u0003\u0002\u0001\bG5\u0001\u0000\u0000\u0000G8\u0001\u0000"+
		"\u0000\u0000G;\u0001\u0000\u0000\u0000G>\u0001\u0000\u0000\u0000GA\u0001"+
		"\u0000\u0000\u0000GD\u0001\u0000\u0000\u0000HK\u0001\u0000\u0000\u0000"+
		"IG\u0001\u0000\u0000\u0000IJ\u0001\u0000\u0000\u0000J\u0003\u0001\u0000"+
		"\u0000\u0000KI\u0001\u0000\u0000\u0000LQ\u0003\u0002\u0001\u0000MN\u0005"+
		"\u0007\u0000\u0000NP\u0003\u0002\u0001\u0000OM\u0001\u0000\u0000\u0000"+
		"PS\u0001\u0000\u0000\u0000QO\u0001\u0000\u0000\u0000QR\u0001\u0000\u0000"+
		"\u0000RU\u0001\u0000\u0000\u0000SQ\u0001\u0000\u0000\u0000TV\u0005\u0007"+
		"\u0000\u0000UT\u0001\u0000\u0000\u0000UV\u0001\u0000\u0000\u0000V\u0005"+
		"\u0001\u0000\u0000\u0000\n\u000f\u0013\u001a\u001e#3GIQU";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}