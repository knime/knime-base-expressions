// Generated from KnimeExpression.g4 by ANTLR 4.13.1
package org.knime.core.expressions.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link KnimeExpressionParser}.
 */
public interface KnimeExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link KnimeExpressionParser#fullExpr}.
	 * @param ctx the parse tree
	 */
	void enterFullExpr(KnimeExpressionParser.FullExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link KnimeExpressionParser#fullExpr}.
	 * @param ctx the parse tree
	 */
	void exitFullExpr(KnimeExpressionParser.FullExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesisedExpr}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterParenthesisedExpr(KnimeExpressionParser.ParenthesisedExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesisedExpr}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitParenthesisedExpr(KnimeExpressionParser.ParenthesisedExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryOp}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterBinaryOp(KnimeExpressionParser.BinaryOpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryOp}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitBinaryOp(KnimeExpressionParser.BinaryOpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colAccess}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterColAccess(KnimeExpressionParser.ColAccessContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colAccess}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitColAccess(KnimeExpressionParser.ColAccessContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(KnimeExpressionParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(KnimeExpressionParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code flowVarAccess}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterFlowVarAccess(KnimeExpressionParser.FlowVarAccessContext ctx);
	/**
	 * Exit a parse tree produced by the {@code flowVarAccess}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitFlowVarAccess(KnimeExpressionParser.FlowVarAccessContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryOp}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOp(KnimeExpressionParser.UnaryOpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryOp}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOp(KnimeExpressionParser.UnaryOpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code atom}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAtom(KnimeExpressionParser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code atom}
	 * labeled alternative in {@link KnimeExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAtom(KnimeExpressionParser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link KnimeExpressionParser#functionArgs}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgs(KnimeExpressionParser.FunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link KnimeExpressionParser#functionArgs}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgs(KnimeExpressionParser.FunctionArgsContext ctx);
}