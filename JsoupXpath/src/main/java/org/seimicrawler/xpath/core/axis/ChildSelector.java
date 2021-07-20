package org.seimicrawler.xpath.core.axis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.XValue;

/**
 * the child axis contains the children of the context node
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/26.
 */
public class ChildSelector implements AxisSelector {
    @Override
    public String name() {
        return "child";
    }

    @Override
    public XValue apply(Elements context) {
        Elements childs = new Elements();
        for (Element el : context) {
            childs.addAll(el.children());
        }
        return XValue.create(childs);
    }
}
