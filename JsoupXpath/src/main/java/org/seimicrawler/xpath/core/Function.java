package org.seimicrawler.xpath.core;

import java.util.List;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/2/28.
 */
public interface Function {
    String name();

    XValue call(Scope scope, List<XValue> params);
}
