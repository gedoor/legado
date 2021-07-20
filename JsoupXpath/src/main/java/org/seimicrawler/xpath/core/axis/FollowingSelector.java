package org.seimicrawler.xpath.core.axis;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.XValue;
import org.seimicrawler.xpath.util.CommonUtil;

import java.util.LinkedList;
import java.util.List;


/**
 * the following axis contains all nodes in the same document as the context node that are after the context node in
 * document order, excluding any descendants and excluding attribute nodes and namespace nodes
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/26.
 */
public class FollowingSelector implements AxisSelector {
    @Override
    public String name() {
        return "following";
    }

    @Override
    public XValue apply(Elements context) {
        List<Element> total = new LinkedList<>();
        for (Element el : context) {
            Elements p = el.parents();
            for (Element pe : p) {
                Elements fs = CommonUtil.followingSibling(pe);
                if (fs == null) {
                    continue;
                }
                for (Element pse : fs) {
                    //include pse
                    total.addAll(pse.getAllElements());
                }
            }
            Elements fs = CommonUtil.followingSibling(el);
            if (fs == null) {
                continue;
            }
            for (Element se : fs) {
                total.addAll(se.getAllElements());
            }
        }
        return XValue.create(new Elements(total));
    }
}
