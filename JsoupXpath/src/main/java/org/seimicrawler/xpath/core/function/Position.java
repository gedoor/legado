package org.seimicrawler.xpath.core.function;

import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;
import org.seimicrawler.xpath.util.CommonUtil;

import java.util.List;

/**
 * The position function returns a number equal to the context position from the expression evaluation context.
 * e.g.
 * /child::doc/child::chapter[position()=5]/child::section[position()=2] selects the second section of the fifth chapter of the doc document element
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/2/28.
 */
public class Position implements Function {
    @Override
    public String name() {
        return "position";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        return XValue.create(CommonUtil.getElIndexInSameTags(scope.singleEl(), scope.getParent()));
    }
}
