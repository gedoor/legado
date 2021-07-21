package org.seimicrawler.xpath.exception;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2017/12/5.
 */
public class XpathParserException extends RuntimeException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public XpathParserException(String message) {
        super(message);
    }

    public XpathParserException(String message, Throwable e) {
        super(message, e);
    }
}
