package org.seimicrawler.xpath.core.axis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.XValue;

import java.util.LinkedList;
import java.util.List;

/**
 * the ancestor-or-self axis contains the context node and the ancestors of the context node;
 * thus, the ancestor axis will always include the root node
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/26.
 */
public class AncestorOrSelfSelector implements AxisSelector {
    @Override
    public String name() {
        return "ancestor-or-self";
    }

    @Override
    public XValue apply(Elements context) {
        List<Element> total = new LinkedList<>();
        for (Element el : context) {
            total.addAll(el.parents());
            //include self
            total.add(el);
        }
        return XValue.create(new Elements(total));
    }
}
