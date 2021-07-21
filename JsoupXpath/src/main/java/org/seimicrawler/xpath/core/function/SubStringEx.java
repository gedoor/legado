package org.seimicrawler.xpath.core.function;

import org.apache.commons.lang3.StringUtils;
import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;

import java.util.List;

/**
 * JsoupXpath扩展函数(统一使用 ‘jx’ 域)，保持使用习惯java开发者习惯相同，第一个数字为起始索引(且索引从0开始)，第二个数字为结束索引
 * <p>
 * StringUtils.substring(null, *, *)    = null
 * StringUtils.substring("", * ,  *)    = "";
 * StringUtils.substring("abc", 0, 2)   = "ab"
 * StringUtils.substring("abc", 2, 0)   = ""
 * StringUtils.substring("abc", 2, 4)   = "c"
 * StringUtils.substring("abc", 2.13, 3.7)   = "c"
 * StringUtils.substring("abc", 4, 6)   = ""
 * StringUtils.substring("abc", 2, 2)   = ""
 * StringUtils.substring("abc", -2, -1) = "b"
 * StringUtils.substring("abc", -4, 2)  = "ab"
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/3/26.
 */

public class SubStringEx implements Function {
    @Override
    public String name() {
        return "substring-ex";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        String target = params.get(0).asString();
        int start = params.get(1).asLong().intValue();
        if (params.get(2) != null) {
            int end = params.get(2).asLong().intValue();
            return XValue.create(StringUtils.substring(target, start, end));
        }
        return XValue.create(StringUtils.substring(target, start));
    }
}
