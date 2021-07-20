package org.seimicrawler.xpath.core.function;

import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;
import org.seimicrawler.xpath.exception.XpathParserException;

import java.util.List;

/**
 * bool not(bool)
 *
 * @author github.com/hermitmmll
 * @since 2018/4/3.
 */
public class Not implements Function {
    @Override
    public String name() {
        return "not";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        if (params.size() == 1) {
            return XValue.create(!params.get(0).asBoolean());
        } else {
            throw new XpathParserException("error param in not(bool) function.Please check.");
        }
    }
}
