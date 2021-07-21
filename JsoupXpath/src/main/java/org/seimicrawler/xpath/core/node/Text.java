package org.seimicrawler.xpath.core.node;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.seimicrawler.xpath.core.Constants;
import org.seimicrawler.xpath.core.NodeTest;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;
import org.seimicrawler.xpath.util.CommonUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/2/28.
 * `text()`不再简单的返回节点下的所有文本，而是按照标准语义识别出多个文本块，返回文本块列表，如
 * ```
 * <p> one <span> two</span> three </p>
 * ```
 * - `//text()` 返回  `["one", "two", "three" ]`
 * - `//text()[2]` 返回  `["three"]`
 */
public class Text implements NodeTest {
    /**
     * 支持的函数名
     */
    @Override
    public String name() {
        return "text";
    }

    /**
     * 函数具体逻辑
     *
     * @param scope 上下文
     * @return 计算好的节点
     */
    @Override
    public XValue call(Scope scope) {
        Elements context = scope.context();
        final Elements res = new Elements();
        if (context != null && context.size() > 0) {
            if (scope.isRecursion()) {
                for (final Element e : context) {
                    final Map<String, Integer> indexMap = new HashMap<>();
                    NodeTraversor.traverse(new NodeVisitor() {
                        @Override
                        public void head(Node node, int depth) {
                            if (node instanceof TextNode) {
                                TextNode textNode = (TextNode) node;
                                String key = depth + "_" + textNode.parent().hashCode();
                                Integer index = indexMap.get(key);
                                if (index == null) {
                                    index = 1;
                                } else {
                                    index += 1;
                                }
                                indexMap.put(key, index);
                                Element data = new Element(Constants.DEF_TEXT_TAG_NAME);
                                data.text(textNode.getWholeText());
                                try {
                                    Method parent = Node.class.getDeclaredMethod("setParentNode", Node.class);
                                    parent.setAccessible(true);
                                    parent.invoke(data, textNode.parent());
                                } catch (Exception e) {
                                    //ignore
                                }
                                CommonUtil.setSameTagIndexInSiblings(data, index);
                                res.add(data);
                            }
                        }

                        @Override
                        public void tail(Node node, int depth) {

                        }
                    }, e);
                }
            } else {
                for (Element e : context) {
                    if ("script".equals(e.nodeName())) {
                        Element data = new Element(Constants.DEF_TEXT_TAG_NAME);
                        data.text(e.data());
                        CommonUtil.setSameTagIndexInSiblings(data, 1);
                        res.add(data);
                    } else {
                        List<TextNode> textNodes = e.textNodes();
                        for (int i = 0; i < textNodes.size(); i++) {
                            TextNode textNode = textNodes.get(i);
                            Element data = new Element(Constants.DEF_TEXT_TAG_NAME);
                            data.text(textNode.getWholeText());
                            CommonUtil.setSameTagIndexInSiblings(data, i + 1);
                            res.add(data);
                        }
                    }
                }
            }
        }
        return XValue.create(res);
    }
}
