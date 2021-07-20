package org.seimicrawler.xpath.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.NodeTest;
import org.seimicrawler.xpath.core.axis.AncestorOrSelfSelector;
import org.seimicrawler.xpath.core.axis.AncestorSelector;
import org.seimicrawler.xpath.core.axis.AttributeSelector;
import org.seimicrawler.xpath.core.axis.ChildSelector;
import org.seimicrawler.xpath.core.axis.DescendantOrSelfSelector;
import org.seimicrawler.xpath.core.axis.DescendantSelector;
import org.seimicrawler.xpath.core.axis.FollowingSelector;
import org.seimicrawler.xpath.core.axis.FollowingSiblingOneSelector;
import org.seimicrawler.xpath.core.axis.FollowingSiblingSelector;
import org.seimicrawler.xpath.core.axis.ParentSelector;
import org.seimicrawler.xpath.core.axis.PrecedingSelector;
import org.seimicrawler.xpath.core.axis.PrecedingSiblingOneSelector;
import org.seimicrawler.xpath.core.axis.PrecedingSiblingSelector;
import org.seimicrawler.xpath.core.axis.SelfSelector;
import org.seimicrawler.xpath.core.function.Concat;
import org.seimicrawler.xpath.core.function.Contains;
import org.seimicrawler.xpath.core.function.Count;
import org.seimicrawler.xpath.core.function.First;
import org.seimicrawler.xpath.core.function.FormatDate;
import org.seimicrawler.xpath.core.function.Last;
import org.seimicrawler.xpath.core.function.Not;
import org.seimicrawler.xpath.core.function.Position;
import org.seimicrawler.xpath.core.function.StartsWith;
import org.seimicrawler.xpath.core.function.StringLength;
import org.seimicrawler.xpath.core.function.SubString;
import org.seimicrawler.xpath.core.function.SubStringAfter;
import org.seimicrawler.xpath.core.function.SubStringAfterLast;
import org.seimicrawler.xpath.core.function.SubStringBefore;
import org.seimicrawler.xpath.core.function.SubStringBeforeLast;
import org.seimicrawler.xpath.core.function.SubStringEx;
import org.seimicrawler.xpath.core.node.AllText;
import org.seimicrawler.xpath.core.node.Html;
import org.seimicrawler.xpath.core.node.Node;
import org.seimicrawler.xpath.core.node.Num;
import org.seimicrawler.xpath.core.node.OuterHtml;
import org.seimicrawler.xpath.core.node.Text;
import org.seimicrawler.xpath.exception.NoSuchAxisException;
import org.seimicrawler.xpath.exception.NoSuchFunctionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 考虑更广泛的兼容性，替换掉 FastClasspathScanner，采用手工注册
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/2/28.
 */
public class Scanner {
    private static final Map<String, AxisSelector> axisSelectorMap = new HashMap<>();
    private static final Map<String, NodeTest> nodeTestMap = new HashMap<>();
    private static final Map<String, Function> functionMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Scanner.class);

    static {
        initAxis(AncestorOrSelfSelector.class, AncestorSelector.class, AttributeSelector.class, ChildSelector.class, DescendantOrSelfSelector.class, DescendantSelector.class, FollowingSelector.class, FollowingSiblingOneSelector.class, FollowingSiblingSelector.class, ParentSelector.class, PrecedingSelector.class, PrecedingSiblingOneSelector.class, PrecedingSiblingSelector.class, SelfSelector.class);
        initFunction(Concat.class, Contains.class, Count.class, First.class, Last.class, Not.class, Position.class, StartsWith.class, StringLength.class, SubString.class, SubStringAfter.class, SubStringBefore.class, SubStringEx.class, FormatDate.class, SubStringAfterLast.class, SubStringBeforeLast.class);
        initNode(AllText.class, Html.class, Node.class, Num.class, OuterHtml.class, Text.class);
    }

    public static AxisSelector findSelectorByName(String selectorName) {
        AxisSelector selector = axisSelectorMap.get(selectorName);
        if (selector == null) {
            throw new NoSuchAxisException("not support axis: " + selectorName);
        }
        return selector;
    }

    public static NodeTest findNodeTestByName(String nodeTestName) {
        NodeTest nodeTest = nodeTestMap.get(nodeTestName);
        if (nodeTest == null) {
            throw new NoSuchFunctionException("not support nodeTest: " + nodeTestName);
        }
        return nodeTest;
    }

    public static Function findFunctionByName(String funcName) {
        Function function = functionMap.get(funcName);
        if (function == null) {
            throw new NoSuchFunctionException("not support function: " + funcName);
        }
        return function;
    }

    public static void registerFunction(Class<? extends Function> func) {
        Function function;
        try {
            function = func.newInstance();
            functionMap.put(function.name(), function);
        } catch (Exception e) {
            logger.info(ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    public static void registerNodeTest(Class<? extends NodeTest> nodeTestClass) {
        NodeTest nodeTest;
        try {
            nodeTest = nodeTestClass.newInstance();
            nodeTestMap.put(nodeTest.name(), nodeTest);
        } catch (Exception e) {
            logger.info(ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    public static void registerAxisSelector(Class<? extends AxisSelector> axisSelectorClass) {
        AxisSelector axisSelector;
        try {
            axisSelector = axisSelectorClass.newInstance();
            axisSelectorMap.put(axisSelector.name(), axisSelector);
        } catch (Exception e) {
            logger.info(ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    public static void initAxis(Class<? extends AxisSelector>... cls) {
        for (Class<? extends AxisSelector> axis : cls) {
            registerAxisSelector(axis);
        }
    }

    public static void initFunction(Class<? extends Function>... cls) {
        for (Class<? extends Function> func : cls) {
            registerFunction(func);
        }
    }

    public static void initNode(Class<? extends NodeTest>... cls) {
        for (Class<? extends NodeTest> node : cls) {
            registerNodeTest(node);
        }
    }

}
