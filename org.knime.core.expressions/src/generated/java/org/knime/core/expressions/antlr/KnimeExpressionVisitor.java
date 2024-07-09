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
	 * Visit a parse tree produced by the {@code functionOrAggregationCall}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionOrAggregationCall(KnimeExpressionParser.FunctionOrAggregationCallContext ctx);
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
	 * Visit a parse tree produced by the {@code constant}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(KnimeExpressionParser.ConstantContext ctx);
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
	 * Visit a parse tree produced by {@link KnimeExpressionParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(KnimeExpressionParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#namedArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedArgument(KnimeExpressionParser.NamedArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link KnimeExpressionParser#positionalArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalArgument(KnimeExpressionParser.PositionalArgumentContext ctx);
}