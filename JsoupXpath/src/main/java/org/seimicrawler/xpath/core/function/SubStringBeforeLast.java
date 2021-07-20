package org.seimicrawler.xpath.core.function;

import org.apache.commons.lang3.StringUtils;
import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;

import java.util.List;

/**
 * Function: string substring-before-last(string, string)
 * <p>
 * The substring-before function returns the substring of the first argument string that precedes the
 * first occurrence of the second argument string in the first argument string, or the empty string
 * if the first argument string does not contain the second argument string.
 * For example, substring-before-last("1999/04/01","/") returns 1999/04.
 *
 * @author github.com/zzldnl
 * @since 2018/3/26.
 */
public class SubStringBeforeLast implements Function {
    @Override
    public String name() {
        return "substring-before-last";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        String target = params.get(0).asString();
        String sep = params.get(1).asString();
        return XValue.create(StringUtils.substringBeforeLast(target, sep));
    }
}
