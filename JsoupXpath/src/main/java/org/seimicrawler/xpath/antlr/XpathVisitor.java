// Generated from resources/Xpath.g4 by ANTLR 4.7.2
package org.seimicrawler.xpath.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link XpathParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface XpathVisitor<T> extends ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by {@link XpathParser#main}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitMain(XpathParser.MainContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#locationPath}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitLocationPath(XpathParser.LocationPathContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#absoluteLocationPathNoroot}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAbsoluteLocationPathNoroot(XpathParser.AbsoluteLocationPathNorootContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#relativeLocationPath}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitRelativeLocationPath(XpathParser.RelativeLocationPathContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#step}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitStep(XpathParser.StepContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#axisSpecifier}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAxisSpecifier(XpathParser.AxisSpecifierContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#nodeTest}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNodeTest(XpathParser.NodeTestContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#predicate}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitPredicate(XpathParser.PredicateContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#abbreviatedStep}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAbbreviatedStep(XpathParser.AbbreviatedStepContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#expr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitExpr(XpathParser.ExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#primaryExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitPrimaryExpr(XpathParser.PrimaryExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#functionCall}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitFunctionCall(XpathParser.FunctionCallContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#unionExprNoRoot}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitUnionExprNoRoot(XpathParser.UnionExprNoRootContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#pathExprNoRoot}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitPathExprNoRoot(XpathParser.PathExprNoRootContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#filterExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitFilterExpr(XpathParser.FilterExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#orExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitOrExpr(XpathParser.OrExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#andExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAndExpr(XpathParser.AndExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#equalityExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitEqualityExpr(XpathParser.EqualityExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#relationalExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitRelationalExpr(XpathParser.RelationalExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#additiveExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAdditiveExpr(XpathParser.AdditiveExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#multiplicativeExpr}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitMultiplicativeExpr(XpathParser.MultiplicativeExprContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#unaryExprNoRoot}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitUnaryExprNoRoot(XpathParser.UnaryExprNoRootContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#qName}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitQName(XpathParser.QNameContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#functionName}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitFunctionName(XpathParser.FunctionNameContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#variableReference}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitVariableReference(XpathParser.VariableReferenceContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#nameTest}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNameTest(XpathParser.NameTestContext ctx);

    /**
     * Visit a parse tree produced by {@link XpathParser#nCName}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNCName(XpathParser.NCNameContext ctx);
}