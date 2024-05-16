// Generated from KnimeExpression.g4 by ANTLR 4.13.1
package org.knime.core.expressions.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link KnimeExpressionParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface KnimeExpressionVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#fullExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullExpr(KnimeExpressionParser.FullExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom(KnimeExpressionParser.AtomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesisedExpr}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesisedExpr(KnimeExpressionParser.ParenthesisedExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code binaryOp}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOp(KnimeExpressionParser.BinaryOpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colAccess}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColAccess(KnimeExpressionParser.ColAccessContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aggregationCall}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregationCall(KnimeExpressionParser.AggregationCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(KnimeExpressionParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code flowVarAccess}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowVarAccess(KnimeExpressionParser.FlowVarAccessContext ctx);
	/**
	 * Visit a parse tree produced by the {@code atomExpr}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomExpr(KnimeExpressionParser.AtomExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unaryOp}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(KnimeExpressionParser.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#functionArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionArgs(KnimeExpressionParser.FunctionArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#aggregationArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregationArgs(KnimeExpressionParser.AggregationArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#positionalAggregationArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalAggregationArgs(KnimeExpressionParser.PositionalAggregationArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#namedAggregationArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAggregationArgs(KnimeExpressionParser.NamedAggregationArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#namedAggregationArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAggregationArg(KnimeExpressionParser.NamedAggregationArgContext ctx);
}