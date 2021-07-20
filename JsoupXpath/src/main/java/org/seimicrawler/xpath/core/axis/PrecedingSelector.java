package org.seimicrawler.xpath.core.axis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.XValue;
import org.seimicrawler.xpath.util.CommonUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * the preceding axis contains all nodes in the same document as the context node that are before the context
 * node in document order, excluding any ancestors and excluding attribute nodes and namespace nodes
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/27.
 */
public class PrecedingSelector implements AxisSelector {
    /**
     * assign name
     *
     * @return name
     */
    @Override
    public String name() {
        return "preceding";
    }

    /**
     * @param context
     * @return res
     */
    @Override
    public XValue apply(Elements context) {
        Elements preceding = new Elements();
        List<Element> total = new LinkedList<>();
        for (Element el : context) {
            Elements p = el.parents();
            for (Element pe : p) {
                Elements ps = CommonUtil.precedingSibling(pe);
                if (ps == null) {
                    continue;
                }
                total.addAll(ps);
            }
            Elements ps = CommonUtil.precedingSibling(el);
            if (ps == null) {
                continue;
            }
            total.addAll(ps);
        }
        preceding.addAll(total);
        return XValue.create(preceding);
    }
}
