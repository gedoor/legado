package org.seimicrawler.xpath.core.function;

import org.apache.commons.lang3.StringUtils;
import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;

import java.util.List;

/**
 * Function: string substring(string, number, number?)
 * https://www.w3.org/TR/1999/REC-xpath-19991116/#function-substring
 * The substring function returns the substring of the first argument starting at the position specified in
 * the second argument with length specified in the third argument. For example, substring("12345",2,3) returns "234".
 * If the third argument is not specified, it returns the substring starting at the position specified in the
 * second argument and continuing to the end of the string. For example, substring("12345",2) returns "2345".
 * <p>
 * substring("12345", 1.5, 2.6) returns "234"
 * substring("12345", 0 `div` 0, 3) returns ""
 * substring("12345", 1, 0 `div` 0) returns ""
 * substring("12345", -42, 1 `div` 0) returns "12345"
 * substring("12345", -1 `div` 0, 1 `div` 0) returns ""
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/26.
 */

public class SubString implements Function {
    @Override
    public String name() {
        return "substring";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        String target = params.get(0).asString();
        int start = params.get(1).asLong().intValue();
        start = Math.max(start - 1, 0);
        if (params.get(2) != null) {
            int end = params.get(2).asLong().intValue();
            end = Math.min(start + end, target.length());
            end = Math.max(end, 0);
            return XValue.create(StringUtils.substring(target, start, end));
        }
        return XValue.create(StringUtils.substring(target, start));
    }
}
