package org.seimicrawler.xpath.exception;

/**
 * 无法合并多个表达式的解析结果
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2017/12/5.
 */
public class XpathMergeValueException extends RuntimeException {
    public XpathMergeValueException(String message) {
        super(message);
    }
}
