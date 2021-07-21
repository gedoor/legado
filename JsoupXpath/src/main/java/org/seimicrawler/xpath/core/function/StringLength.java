package org.seimicrawler.xpath.core.function;

import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;

import java.util.List;

/**
 * number string-length(string?)
 * The string-length returns the number of characters in the string (see [3.6 Strings]). If the argument is
 * omitted, it defaults to the context node converted to a string, in other words the string-value of the context node.
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/27.
 */
public class StringLength implements Function {
    @Override
    public String name() {
        return "string-length";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        if (params == null || params.size() == 0) {
            return XValue.create(0);
        }
        return XValue.create(params.get(0).asString().length());
    }
}
