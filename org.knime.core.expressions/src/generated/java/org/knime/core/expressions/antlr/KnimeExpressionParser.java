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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, LINE_COMMENT=7, WHITESPACE=8, 
		BOOLEAN=9, INTEGER=10, FLOAT=11, STRING=12, MISSING=13, MATHS_CONSTANT=14, 
		PLUS=15, MINUS=16, MULTIPLY=17, DIVIDE=18, FLOOR_DIVIDE=19, EXPONENTIATE=20, 
		MODULO=21, LESS_THAN=22, LESS_THAN_EQUAL=23, GREATER_THAN=24, GREATER_THAN_EQUAL=25, 
		EQUAL=26, NOT_EQUAL=27, AND=28, OR=29, NOT=30, MISSING_FALLBACK=31, AGGREGATION_IDENTIFIER=32, 
		FUNCTION_IDENTIFIER=33, COLUMN_IDENTIFIER=34, FLOW_VAR_IDENTIFIER=35, 
		NAMED_ARGUMENT_IDENTIFIER=36;
	public static final int
		RULE_fullExpr = 0, RULE_atom = 1, RULE_expr = 2, RULE_functionArgs = 3, 
		RULE_aggregationArgs = 4, RULE_positionalAggregationArgs = 5, RULE_namedAggregationArgs = 6, 
		RULE_namedAggregationArg = 7;
	private static String[] makeRuleNames() {
		return new String[] {
			"fullExpr", "atom", "expr", "functionArgs", "aggregationArgs", "positionalAggregationArgs", 
			"namedAggregationArgs", "namedAggregationArg"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'$$['", "']'", "'$['", "'('", "')'", "','", null, null, null, 
			null, null, null, "'MISSING'", null, "'+'", "'-'", "'*'", "'/'", "'//'", 
			"'**'", "'%'", "'<'", "'<='", "'>'", "'>='", null, null, "'and'", "'or'", 
			"'not'", "'??'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, "LINE_COMMENT", "WHITESPACE", 
			"BOOLEAN", "INTEGER", "FLOAT", "STRING", "MISSING", "MATHS_CONSTANT", 
			"PLUS", "MINUS", "MULTIPLY", "DIVIDE", "FLOOR_DIVIDE", "EXPONENTIATE", 
			"MODULO", "LESS_THAN", "LESS_THAN_EQUAL", "GREATER_THAN", "GREATER_THAN_EQUAL", 
			"EQUAL", "NOT_EQUAL", "AND", "OR", "NOT", "MISSING_FALLBACK", "AGGREGATION_IDENTIFIER", 
			"FUNCTION_IDENTIFIER", "COLUMN_IDENTIFIER", "FLOW_VAR_IDENTIFIER", "NAMED_ARGUMENT_IDENTIFIER"
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
			setState(16);
			expr(0);
			setState(17);
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
		public TerminalNode MATHS_CONSTANT() { return getToken(KnimeExpressionParser.MATHS_CONSTANT, 0); }
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
			setState(19);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 32256L) != 0)) ) {
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
		public TerminalNode COLUMN_IDENTIFIER() { return getToken(KnimeExpressionParser.COLUMN_IDENTIFIER, 0); }
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
	public static class AggregationCallContext extends ExprContext {
		public Token name;
		public AggregationArgsContext aggregationArgs() {
			return getRuleContext(AggregationArgsContext.class,0);
		}
		public TerminalNode AGGREGATION_IDENTIFIER() { return getToken(KnimeExpressionParser.AGGREGATION_IDENTIFIER, 0); }
		public AggregationCallContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterAggregationCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitAggregationCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitAggregationCall(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionCallContext extends ExprContext {
		public Token name;
		public TerminalNode FUNCTION_IDENTIFIER() { return getToken(KnimeExpressionParser.FUNCTION_IDENTIFIER, 0); }
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
		public TerminalNode FLOW_VAR_IDENTIFIER() { return getToken(KnimeExpressionParser.FLOW_VAR_IDENTIFIER, 0); }
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
			setState(62);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case FLOW_VAR_IDENTIFIER:
				{
				_localctx = new FlowVarAccessContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(30);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case FLOW_VAR_IDENTIFIER:
					{
					setState(22);
					((FlowVarAccessContext)_localctx).shortName = match(FLOW_VAR_IDENTIFIER);
					}
					break;
				case T__0:
					{
					setState(24); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(23);
						match(T__0);
						}
						}
						setState(26); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==T__0 );
					setState(28);
					((FlowVarAccessContext)_localctx).longName = match(STRING);
					setState(29);
					match(T__1);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case T__2:
			case COLUMN_IDENTIFIER:
				{
				_localctx = new ColAccessContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(40);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case COLUMN_IDENTIFIER:
					{
					setState(32);
					((ColAccessContext)_localctx).shortName = match(COLUMN_IDENTIFIER);
					}
					break;
				case T__2:
					{
					setState(34); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(33);
						match(T__2);
						}
						}
						setState(36); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==T__2 );
					setState(38);
					((ColAccessContext)_localctx).longName = match(STRING);
					setState(39);
					match(T__1);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case FUNCTION_IDENTIFIER:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(42);
				((FunctionCallContext)_localctx).name = match(FUNCTION_IDENTIFIER);
				setState(43);
				match(T__3);
				setState(45);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 65498349082L) != 0)) {
					{
					setState(44);
					functionArgs();
					}
				}

				setState(47);
				match(T__4);
				}
				break;
			case AGGREGATION_IDENTIFIER:
				{
				_localctx = new AggregationCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(48);
				((AggregationCallContext)_localctx).name = match(AGGREGATION_IDENTIFIER);
				setState(49);
				match(T__3);
				setState(50);
				aggregationArgs();
				setState(51);
				match(T__4);
				}
				break;
			case MINUS:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(53);
				((UnaryOpContext)_localctx).op = match(MINUS);
				setState(54);
				expr(9);
				}
				break;
			case NOT:
				{
				_localctx = new UnaryOpContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(55);
				((UnaryOpContext)_localctx).op = match(NOT);
				setState(56);
				expr(5);
				}
				break;
			case T__3:
				{
				_localctx = new ParenthesisedExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(57);
				match(T__3);
				setState(58);
				((ParenthesisedExprContext)_localctx).inner = expr(0);
				setState(59);
				match(T__4);
				}
				break;
			case BOOLEAN:
			case INTEGER:
			case FLOAT:
			case STRING:
			case MISSING:
			case MATHS_CONSTANT:
				{
				_localctx = new AtomExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(61);
				atom();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(87);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(85);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(64);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(65);
						((BinaryOpContext)_localctx).op = match(MISSING_FALLBACK);
						setState(66);
						expr(12);
						}
						break;
					case 2:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(67);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(68);
						((BinaryOpContext)_localctx).op = match(EXPONENTIATE);
						setState(69);
						expr(10);
						}
						break;
					case 3:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(70);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(71);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 3014656L) != 0)) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(72);
						expr(9);
						}
						break;
					case 4:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(73);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(74);
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
						setState(75);
						expr(8);
						}
						break;
					case 5:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(76);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(77);
						((BinaryOpContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 264241152L) != 0)) ) {
							((BinaryOpContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(78);
						expr(7);
						}
						break;
					case 6:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(79);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(80);
						((BinaryOpContext)_localctx).op = match(AND);
						setState(81);
						expr(5);
						}
						break;
					case 7:
						{
						_localctx = new BinaryOpContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(82);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(83);
						((BinaryOpContext)_localctx).op = match(OR);
						setState(84);
						expr(4);
						}
						break;
					}
					} 
				}
				setState(89);
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
		enterRule(_localctx, 6, RULE_functionArgs);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			expr(0);
			setState(95);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(91);
					match(T__5);
					setState(92);
					expr(0);
					}
					} 
				}
				setState(97);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			}
			setState(99);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(98);
				match(T__5);
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
	public static class AggregationArgsContext extends ParserRuleContext {
		public PositionalAggregationArgsContext positionalAggregationArgs() {
			return getRuleContext(PositionalAggregationArgsContext.class,0);
		}
		public NamedAggregationArgsContext namedAggregationArgs() {
			return getRuleContext(NamedAggregationArgsContext.class,0);
		}
		public AggregationArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregationArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterAggregationArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitAggregationArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitAggregationArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AggregationArgsContext aggregationArgs() throws RecognitionException {
		AggregationArgsContext _localctx = new AggregationArgsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_aggregationArgs);
		int _la;
		try {
			setState(113);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOLEAN:
			case INTEGER:
			case FLOAT:
			case STRING:
			case MISSING:
			case MATHS_CONSTANT:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(101);
				positionalAggregationArgs();
				setState(104);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(102);
					match(T__5);
					setState(103);
					namedAggregationArgs();
					}
					break;
				}
				setState(107);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__5) {
					{
					setState(106);
					match(T__5);
					}
				}

				}
				}
				break;
			case NAMED_ARGUMENT_IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(109);
				namedAggregationArgs();
				setState(111);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__5) {
					{
					setState(110);
					match(T__5);
					}
				}

				}
				}
				break;
			default:
				throw new NoViableAltException(this);
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
	public static class PositionalAggregationArgsContext extends ParserRuleContext {
		public List<AtomContext> atom() {
			return getRuleContexts(AtomContext.class);
		}
		public AtomContext atom(int i) {
			return getRuleContext(AtomContext.class,i);
		}
		public PositionalAggregationArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_positionalAggregationArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterPositionalAggregationArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitPositionalAggregationArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitPositionalAggregationArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PositionalAggregationArgsContext positionalAggregationArgs() throws RecognitionException {
		PositionalAggregationArgsContext _localctx = new PositionalAggregationArgsContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_positionalAggregationArgs);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(115);
			atom();
			setState(120);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(116);
					match(T__5);
					setState(117);
					atom();
					}
					} 
				}
				setState(122);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
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
	public static class NamedAggregationArgsContext extends ParserRuleContext {
		public List<NamedAggregationArgContext> namedAggregationArg() {
			return getRuleContexts(NamedAggregationArgContext.class);
		}
		public NamedAggregationArgContext namedAggregationArg(int i) {
			return getRuleContext(NamedAggregationArgContext.class,i);
		}
		public NamedAggregationArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedAggregationArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterNamedAggregationArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitNamedAggregationArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitNamedAggregationArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedAggregationArgsContext namedAggregationArgs() throws RecognitionException {
		NamedAggregationArgsContext _localctx = new NamedAggregationArgsContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_namedAggregationArgs);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			namedAggregationArg();
			setState(128);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(124);
					match(T__5);
					setState(125);
					namedAggregationArg();
					}
					} 
				}
				setState(130);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
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
	public static class NamedAggregationArgContext extends ParserRuleContext {
		public Token argName;
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public TerminalNode NAMED_ARGUMENT_IDENTIFIER() { return getToken(KnimeExpressionParser.NAMED_ARGUMENT_IDENTIFIER, 0); }
		public NamedAggregationArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedAggregationArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).enterNamedAggregationArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KnimeExpressionListener ) ((KnimeExpressionListener)listener).exitNamedAggregationArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof KnimeExpressionVisitor ) return ((KnimeExpressionVisitor<? extends T>)visitor).visitNamedAggregationArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedAggregationArgContext namedAggregationArg() throws RecognitionException {
		NamedAggregationArgContext _localctx = new NamedAggregationArgContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_namedAggregationArg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(131);
			((NamedAggregationArgContext)_localctx).argName = match(NAMED_ARGUMENT_IDENTIFIER);
			setState(132);
			atom();
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
		"\u0004\u0001$\u0087\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0004\u0002\u0019\b\u0002\u000b\u0002\f\u0002\u001a"+
		"\u0001\u0002\u0001\u0002\u0003\u0002\u001f\b\u0002\u0001\u0002\u0001\u0002"+
		"\u0004\u0002#\b\u0002\u000b\u0002\f\u0002$\u0001\u0002\u0001\u0002\u0003"+
		"\u0002)\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002.\b\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002?\b\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0005\u0002V\b\u0002\n\u0002\f\u0002Y\t\u0002"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0005\u0003^\b\u0003\n\u0003\f\u0003"+
		"a\t\u0003\u0001\u0003\u0003\u0003d\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0003\u0004i\b\u0004\u0001\u0004\u0003\u0004l\b\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004p\b\u0004\u0003\u0004r\b\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0005\u0005w\b\u0005\n\u0005\f\u0005z\t\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0005\u0006\u007f\b\u0006\n\u0006\f\u0006"+
		"\u0082\t\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0000\u0001"+
		"\u0004\b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0000\u0004\u0001\u0000\t"+
		"\u000e\u0002\u0000\u0011\u0013\u0015\u0015\u0001\u0000\u000f\u0010\u0001"+
		"\u0000\u0016\u001b\u0099\u0000\u0010\u0001\u0000\u0000\u0000\u0002\u0013"+
		"\u0001\u0000\u0000\u0000\u0004>\u0001\u0000\u0000\u0000\u0006Z\u0001\u0000"+
		"\u0000\u0000\bq\u0001\u0000\u0000\u0000\ns\u0001\u0000\u0000\u0000\f{"+
		"\u0001\u0000\u0000\u0000\u000e\u0083\u0001\u0000\u0000\u0000\u0010\u0011"+
		"\u0003\u0004\u0002\u0000\u0011\u0012\u0005\u0000\u0000\u0001\u0012\u0001"+
		"\u0001\u0000\u0000\u0000\u0013\u0014\u0007\u0000\u0000\u0000\u0014\u0003"+
		"\u0001\u0000\u0000\u0000\u0015\u001e\u0006\u0002\uffff\uffff\u0000\u0016"+
		"\u001f\u0005#\u0000\u0000\u0017\u0019\u0005\u0001\u0000\u0000\u0018\u0017"+
		"\u0001\u0000\u0000\u0000\u0019\u001a\u0001\u0000\u0000\u0000\u001a\u0018"+
		"\u0001\u0000\u0000\u0000\u001a\u001b\u0001\u0000\u0000\u0000\u001b\u001c"+
		"\u0001\u0000\u0000\u0000\u001c\u001d\u0005\f\u0000\u0000\u001d\u001f\u0005"+
		"\u0002\u0000\u0000\u001e\u0016\u0001\u0000\u0000\u0000\u001e\u0018\u0001"+
		"\u0000\u0000\u0000\u001f?\u0001\u0000\u0000\u0000 )\u0005\"\u0000\u0000"+
		"!#\u0005\u0003\u0000\u0000\"!\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000"+
		"\u0000$\"\u0001\u0000\u0000\u0000$%\u0001\u0000\u0000\u0000%&\u0001\u0000"+
		"\u0000\u0000&\'\u0005\f\u0000\u0000\')\u0005\u0002\u0000\u0000( \u0001"+
		"\u0000\u0000\u0000(\"\u0001\u0000\u0000\u0000)?\u0001\u0000\u0000\u0000"+
		"*+\u0005!\u0000\u0000+-\u0005\u0004\u0000\u0000,.\u0003\u0006\u0003\u0000"+
		"-,\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000./\u0001\u0000\u0000"+
		"\u0000/?\u0005\u0005\u0000\u000001\u0005 \u0000\u000012\u0005\u0004\u0000"+
		"\u000023\u0003\b\u0004\u000034\u0005\u0005\u0000\u00004?\u0001\u0000\u0000"+
		"\u000056\u0005\u0010\u0000\u00006?\u0003\u0004\u0002\t78\u0005\u001e\u0000"+
		"\u00008?\u0003\u0004\u0002\u00059:\u0005\u0004\u0000\u0000:;\u0003\u0004"+
		"\u0002\u0000;<\u0005\u0005\u0000\u0000<?\u0001\u0000\u0000\u0000=?\u0003"+
		"\u0002\u0001\u0000>\u0015\u0001\u0000\u0000\u0000>(\u0001\u0000\u0000"+
		"\u0000>*\u0001\u0000\u0000\u0000>0\u0001\u0000\u0000\u0000>5\u0001\u0000"+
		"\u0000\u0000>7\u0001\u0000\u0000\u0000>9\u0001\u0000\u0000\u0000>=\u0001"+
		"\u0000\u0000\u0000?W\u0001\u0000\u0000\u0000@A\n\u000b\u0000\u0000AB\u0005"+
		"\u001f\u0000\u0000BV\u0003\u0004\u0002\fCD\n\n\u0000\u0000DE\u0005\u0014"+
		"\u0000\u0000EV\u0003\u0004\u0002\nFG\n\b\u0000\u0000GH\u0007\u0001\u0000"+
		"\u0000HV\u0003\u0004\u0002\tIJ\n\u0007\u0000\u0000JK\u0007\u0002\u0000"+
		"\u0000KV\u0003\u0004\u0002\bLM\n\u0006\u0000\u0000MN\u0007\u0003\u0000"+
		"\u0000NV\u0003\u0004\u0002\u0007OP\n\u0004\u0000\u0000PQ\u0005\u001c\u0000"+
		"\u0000QV\u0003\u0004\u0002\u0005RS\n\u0003\u0000\u0000ST\u0005\u001d\u0000"+
		"\u0000TV\u0003\u0004\u0002\u0004U@\u0001\u0000\u0000\u0000UC\u0001\u0000"+
		"\u0000\u0000UF\u0001\u0000\u0000\u0000UI\u0001\u0000\u0000\u0000UL\u0001"+
		"\u0000\u0000\u0000UO\u0001\u0000\u0000\u0000UR\u0001\u0000\u0000\u0000"+
		"VY\u0001\u0000\u0000\u0000WU\u0001\u0000\u0000\u0000WX\u0001\u0000\u0000"+
		"\u0000X\u0005\u0001\u0000\u0000\u0000YW\u0001\u0000\u0000\u0000Z_\u0003"+
		"\u0004\u0002\u0000[\\\u0005\u0006\u0000\u0000\\^\u0003\u0004\u0002\u0000"+
		"][\u0001\u0000\u0000\u0000^a\u0001\u0000\u0000\u0000_]\u0001\u0000\u0000"+
		"\u0000_`\u0001\u0000\u0000\u0000`c\u0001\u0000\u0000\u0000a_\u0001\u0000"+
		"\u0000\u0000bd\u0005\u0006\u0000\u0000cb\u0001\u0000\u0000\u0000cd\u0001"+
		"\u0000\u0000\u0000d\u0007\u0001\u0000\u0000\u0000eh\u0003\n\u0005\u0000"+
		"fg\u0005\u0006\u0000\u0000gi\u0003\f\u0006\u0000hf\u0001\u0000\u0000\u0000"+
		"hi\u0001\u0000\u0000\u0000ik\u0001\u0000\u0000\u0000jl\u0005\u0006\u0000"+
		"\u0000kj\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000lr\u0001\u0000"+
		"\u0000\u0000mo\u0003\f\u0006\u0000np\u0005\u0006\u0000\u0000on\u0001\u0000"+
		"\u0000\u0000op\u0001\u0000\u0000\u0000pr\u0001\u0000\u0000\u0000qe\u0001"+
		"\u0000\u0000\u0000qm\u0001\u0000\u0000\u0000r\t\u0001\u0000\u0000\u0000"+
		"sx\u0003\u0002\u0001\u0000tu\u0005\u0006\u0000\u0000uw\u0003\u0002\u0001"+
		"\u0000vt\u0001\u0000\u0000\u0000wz\u0001\u0000\u0000\u0000xv\u0001\u0000"+
		"\u0000\u0000xy\u0001\u0000\u0000\u0000y\u000b\u0001\u0000\u0000\u0000"+
		"zx\u0001\u0000\u0000\u0000{\u0080\u0003\u000e\u0007\u0000|}\u0005\u0006"+
		"\u0000\u0000}\u007f\u0003\u000e\u0007\u0000~|\u0001\u0000\u0000\u0000"+
		"\u007f\u0082\u0001\u0000\u0000\u0000\u0080~\u0001\u0000\u0000\u0000\u0080"+
		"\u0081\u0001\u0000\u0000\u0000\u0081\r\u0001\u0000\u0000\u0000\u0082\u0080"+
		"\u0001\u0000\u0000\u0000\u0083\u0084\u0005$\u0000\u0000\u0084\u0085\u0003"+
		"\u0002\u0001\u0000\u0085\u000f\u0001\u0000\u0000\u0000\u0010\u001a\u001e"+
		"$(->UW_chkoqx\u0080";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}