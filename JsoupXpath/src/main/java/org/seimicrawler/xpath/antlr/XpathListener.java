// Generated from resources/Xpath.g4 by ANTLR 4.7.2
package org.seimicrawler.xpath.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link XpathParser}.
 */
public interface XpathListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link XpathParser#main}.
     *
     * @param ctx the parse tree
     */
    void enterMain(XpathParser.MainContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#main}.
     *
     * @param ctx the parse tree
     */
    void exitMain(XpathParser.MainContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#locationPath}.
     *
     * @param ctx the parse tree
     */
    void enterLocationPath(XpathParser.LocationPathContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#locationPath}.
     *
     * @param ctx the parse tree
     */
    void exitLocationPath(XpathParser.LocationPathContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#absoluteLocationPathNoroot}.
     *
     * @param ctx the parse tree
     */
    void enterAbsoluteLocationPathNoroot(XpathParser.AbsoluteLocationPathNorootContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#absoluteLocationPathNoroot}.
     *
     * @param ctx the parse tree
     */
    void exitAbsoluteLocationPathNoroot(XpathParser.AbsoluteLocationPathNorootContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#relativeLocationPath}.
     *
     * @param ctx the parse tree
     */
    void enterRelativeLocationPath(XpathParser.RelativeLocationPathContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#relativeLocationPath}.
     *
     * @param ctx the parse tree
     */
    void exitRelativeLocationPath(XpathParser.RelativeLocationPathContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#step}.
     *
     * @param ctx the parse tree
     */
    void enterStep(XpathParser.StepContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#step}.
     *
     * @param ctx the parse tree
     */
    void exitStep(XpathParser.StepContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#axisSpecifier}.
     *
     * @param ctx the parse tree
     */
    void enterAxisSpecifier(XpathParser.AxisSpecifierContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#axisSpecifier}.
     *
     * @param ctx the parse tree
     */
    void exitAxisSpecifier(XpathParser.AxisSpecifierContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#nodeTest}.
     *
     * @param ctx the parse tree
     */
    void enterNodeTest(XpathParser.NodeTestContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#nodeTest}.
     *
     * @param ctx the parse tree
     */
    void exitNodeTest(XpathParser.NodeTestContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#predicate}.
     *
     * @param ctx the parse tree
     */
    void enterPredicate(XpathParser.PredicateContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#predicate}.
     *
     * @param ctx the parse tree
     */
    void exitPredicate(XpathParser.PredicateContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#abbreviatedStep}.
     *
     * @param ctx the parse tree
     */
    void enterAbbreviatedStep(XpathParser.AbbreviatedStepContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#abbreviatedStep}.
     *
     * @param ctx the parse tree
     */
    void exitAbbreviatedStep(XpathParser.AbbreviatedStepContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterExpr(XpathParser.ExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitExpr(XpathParser.ExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#primaryExpr}.
     *
     * @param ctx the parse tree
     */
    void enterPrimaryExpr(XpathParser.PrimaryExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#primaryExpr}.
     *
     * @param ctx the parse tree
     */
    void exitPrimaryExpr(XpathParser.PrimaryExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#functionCall}.
     *
     * @param ctx the parse tree
     */
    void enterFunctionCall(XpathParser.FunctionCallContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#functionCall}.
     *
     * @param ctx the parse tree
     */
    void exitFunctionCall(XpathParser.FunctionCallContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#unionExprNoRoot}.
     *
     * @param ctx the parse tree
     */
    void enterUnionExprNoRoot(XpathParser.UnionExprNoRootContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#unionExprNoRoot}.
     *
     * @param ctx the parse tree
     */
    void exitUnionExprNoRoot(XpathParser.UnionExprNoRootContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#pathExprNoRoot}.
     *
     * @param ctx the parse tree
     */
    void enterPathExprNoRoot(XpathParser.PathExprNoRootContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#pathExprNoRoot}.
     *
     * @param ctx the parse tree
     */
    void exitPathExprNoRoot(XpathParser.PathExprNoRootContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#filterExpr}.
     *
     * @param ctx the parse tree
     */
    void enterFilterExpr(XpathParser.FilterExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#filterExpr}.
     *
     * @param ctx the parse tree
     */
    void exitFilterExpr(XpathParser.FilterExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#orExpr}.
     *
     * @param ctx the parse tree
     */
    void enterOrExpr(XpathParser.OrExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#orExpr}.
     *
     * @param ctx the parse tree
     */
    void exitOrExpr(XpathParser.OrExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#andExpr}.
     *
     * @param ctx the parse tree
     */
    void enterAndExpr(XpathParser.AndExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#andExpr}.
     *
     * @param ctx the parse tree
     */
    void exitAndExpr(XpathParser.AndExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#equalityExpr}.
     *
     * @param ctx the parse tree
     */
    void enterEqualityExpr(XpathParser.EqualityExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#equalityExpr}.
     *
     * @param ctx the parse tree
     */
    void exitEqualityExpr(XpathParser.EqualityExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#relationalExpr}.
     *
     * @param ctx the parse tree
     */
    void enterRelationalExpr(XpathParser.RelationalExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#relationalExpr}.
     *
     * @param ctx the parse tree
     */
    void exitRelationalExpr(XpathParser.RelationalExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#additiveExpr}.
     *
     * @param ctx the parse tree
     */
    void enterAdditiveExpr(XpathParser.AdditiveExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#additiveExpr}.
     *
     * @param ctx the parse tree
     */
    void exitAdditiveExpr(XpathParser.AdditiveExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#multiplicativeExpr}.
     *
     * @param ctx the parse tree
     */
    void enterMultiplicativeExpr(XpathParser.MultiplicativeExprContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#multiplicativeExpr}.
     *
     * @param ctx the parse tree
     */
    void exitMultiplicativeExpr(XpathParser.MultiplicativeExprContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#unaryExprNoRoot}.
     *
     * @param ctx the parse tree
     */
    void enterUnaryExprNoRoot(XpathParser.UnaryExprNoRootContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#unaryExprNoRoot}.
     *
     * @param ctx the parse tree
     */
    void exitUnaryExprNoRoot(XpathParser.UnaryExprNoRootContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#qName}.
     *
     * @param ctx the parse tree
     */
    void enterQName(XpathParser.QNameContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#qName}.
     *
     * @param ctx the parse tree
     */
    void exitQName(XpathParser.QNameContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#functionName}.
     *
     * @param ctx the parse tree
     */
    void enterFunctionName(XpathParser.FunctionNameContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#functionName}.
     *
     * @param ctx the parse tree
     */
    void exitFunctionName(XpathParser.FunctionNameContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#variableReference}.
     *
     * @param ctx the parse tree
     */
    void enterVariableReference(XpathParser.VariableReferenceContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#variableReference}.
     *
     * @param ctx the parse tree
     */
    void exitVariableReference(XpathParser.VariableReferenceContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#nameTest}.
     *
     * @param ctx the parse tree
     */
    void enterNameTest(XpathParser.NameTestContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#nameTest}.
     *
     * @param ctx the parse tree
     */
    void exitNameTest(XpathParser.NameTestContext ctx);

    /**
     * Enter a parse tree produced by {@link XpathParser#nCName}.
     *
     * @param ctx the parse tree
     */
    void enterNCName(XpathParser.NCNameContext ctx);

    /**
     * Exit a parse tree produced by {@link XpathParser#nCName}.
     *
     * @param ctx the parse tree
     */
    void exitNCName(XpathParser.NCNameContext ctx);
}