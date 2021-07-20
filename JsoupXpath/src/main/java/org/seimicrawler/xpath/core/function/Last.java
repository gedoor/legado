package org.seimicrawler.xpath.core.function;

import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;

import java.util.List;

/**
 * Function: number last()
 * The last function returns a number equal to the context size from the expression evaluation context.
 * e.g.
 * para[last()] selects the last para child of the context node
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/27.
 */
public class Last implements Function {
    @Override
    public String name() {
        return "last";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        return XValue.create(-1);
    }
}
