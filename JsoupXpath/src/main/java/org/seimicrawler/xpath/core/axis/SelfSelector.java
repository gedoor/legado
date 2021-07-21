package org.seimicrawler.xpath.core.axis;

import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.AxisSelector;
import org.seimicrawler.xpath.core.XValue;

/**
 * the self axis contains just the context node itself
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/2/28.
 */
public class SelfSelector implements AxisSelector {
    @Override
    public String name() {
        return "self";
    }

    @Override
    public XValue apply(Elements es) {
        return XValue.create(es);
    }
}
