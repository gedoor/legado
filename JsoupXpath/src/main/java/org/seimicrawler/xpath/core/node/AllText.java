package org.seimicrawler.xpath.core.node;

import org.jsoup.nodes.Element;
import org.seimicrawler.xpath.core.NodeTest;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;

import java.util.LinkedList;
import java.util.List;

/**
 * 获取当前节点下以及所有子孙节点中纯文本
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/26.
 */
public class AllText implements NodeTest {
    /**
     * 支持的函数名
     */
    @Override
    public String name() {
        return "allText";
    }

    /**
     * 函数具体逻辑
     *
     * @param scope 上下文
     * @return 计算好的节点
     */
    @Override
    public XValue call(Scope scope) {
        List<String> res = new LinkedList<>();
        for (Element e : scope.context()) {
            if ("script".equals(e.nodeName())) {
                res.add(e.data());
            } else {
                res.add(e.text());
            }
        }
        return XValue.create(res);
    }
}
