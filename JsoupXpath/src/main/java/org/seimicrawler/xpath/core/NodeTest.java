package org.seimicrawler.xpath.core;


/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/2/28.
 */
public interface NodeTest {
    /**
     * 支持的函数名
     */
    String name();

    /**
     * 函数具体逻辑
     *
     * @param scope 上下文
     * @return 计算好的节点
     */
    XValue call(Scope scope);
}
