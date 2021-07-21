package org.seimicrawler.xpath.core.axis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.XValue;

import java.util.LinkedList;
import java.util.List;

/**
 * the following-sibling-one JsoupXpath自定义扩展,比较常用
 *
 * @author https://github.com/hermitmmll
 * @since 2018/3/27.
 */
public class FollowingSiblingOneSelector implements AxisSelector {
    /**
     * assign name
     *
     * @return name
     */
    @Override
    public String name() {
        return "following-sibling-one";
    }

    /**
     * @param context
     * @return res
     */
    @Override
    public XValue apply(Elements context) {
        List<Element> total = new LinkedList<>();
        for (Element el : context) {
            if (el.nextElementSibling() != null) {
                total.add(el.nextElementSibling());
            }
        }
        Elements newContext = new Elements();
        newContext.addAll(total);
        return XValue.create(newContext);
    }
}
