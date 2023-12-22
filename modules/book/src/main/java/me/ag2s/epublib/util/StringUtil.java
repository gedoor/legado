package me.ag2s.epublib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Various String utility functions.
 * <p>
 * Most of the functions herein are re-implementations of the ones in apache
 * commons StringUtils. The reason for re-implementing this is that the
 * functions are fairly simple and using my own implementation saves the
 * inclusion of a 200Kb jar file.
 *
 * @author paul.siegmann
 */
public class StringUtil {

    /**
     * Changes a path containing '..', '.' and empty dirs into a path that
     * doesn't. X/foo/../Y is changed into 'X/Y', etc. Does not handle invalid
     * paths like "../".
     *
     * @param path path
     * @return the normalized path
     */
    public static String collapsePathDots(String path) {
        String[] stringParts = path.split("/");
        List<String> parts = new ArrayList<>(Arrays.asList(stringParts));
        for (int i = 0; i < parts.size() - 1; i++) {
            String currentDir = parts.get(i);
            if (currentDir.length() == 0 || currentDir.equals(".")) {
                parts.remove(i);
                i--;
            } else if (currentDir.equals("..")) {
                parts.remove(i - 1);
                parts.remove(i - 1);
                i -= 2;
            }
        }
        StringBuilder result = new StringBuilder();
        if (path.startsWith("/")) {
            result.append('/');
        }
        for (int i = 0; i < parts.size(); i++) {
            result.append(parts.get(i));
            if (i < (parts.size() - 1)) {
                result.append('/');
            }
        }
        return result.toString();
    }

    /**
     * Whether the String is not null, not zero-length and does not contain of
     * only whitespace.
     *
     * @param text text
     * @return Whether the String is not null, not zero-length and does not contain of
     */
    public static boolean isNotBlank(String text) {
        return !isBlank(text);
    }

    /**
     * Whether the String is null, zero-length and does contain only whitespace.
     *
     * @return Whether the String is null, zero-length and does contain only whitespace.
     */
    public static boolean isBlank(String text) {
        if (isEmpty(text)) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the given string is null or zero-length.
     *
     * @param text the input for this method
     * @return Whether the given string is null or zero-length.
     */
    public static boolean isEmpty(String text) {
        return (text == null) || (text.length() == 0);
    }

    /**
     * Whether the given source string ends with the given suffix, ignoring
     * case.
     *
     * @param source source
     * @param suffix suffix
     * @return Whether the given source string ends with the given suffix, ignoring case.
     */
    public static boolean endsWithIgnoreCase(String source, String suffix) {
        if (isEmpty(suffix)) {
            return true;
        }
        if (isEmpty(source)) {
            return false;
        }
        if (suffix.length() > source.length()) {
            return false;
        }
        return source.substring(source.length() - suffix.length())
                .toLowerCase().endsWith(suffix.toLowerCase());
    }

    public static boolean startsWithIgnoreCase(String source, String prefix) {
        if (isEmpty(prefix)) {
            return true;
        }
        if (isEmpty(source)) {
            return false;
        }
        if (prefix.length() > source.length()) {
            return false;
        }
        return source.substring(0, prefix.length())
                .toLowerCase().startsWith(prefix.toLowerCase());
    }

    /**
     * If the given text is null return "", the original text otherwise.
     *
     * @param text text
     * @return If the given text is null "", the original text otherwise.
     */
    public static String defaultIfNull(String text) {
        return defaultIfNull(text, "");
    }

    /**
     * If the given text is null return "", the given defaultValue otherwise.
     *
     * @param text         d
     * @param defaultValue d
     * @return If the given text is null "", the given defaultValue otherwise.
     */
    public static String defaultIfNull(String text, String defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        return text;
    }

    /**
     * Null-safe string comparator
     *
     * @param text1 d
     * @param text2 d
     * @return whether the two strings are equal
     */
    public static boolean equals(String text1, String text2) {
        if (text1 == null) {
            return (text2 == null);
        }
        return text1.equals(text2);
    }

    /**
     * Pretty toString printer.
     *
     * @param keyValues d
     * @return a string representation of the input values
     */
    public static String toString(Object... keyValues) {
        StringBuilder result = new StringBuilder();
        result.append('[');
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(keyValues[i]);
            result.append(": ");
            Object value = null;
            if ((i + 1) < keyValues.length) {
                value = keyValues[i + 1];
            }
            if (value == null) {
                result.append("<null>");
            } else {
                result.append('\'');
                result.append(value);
                result.append('\'');
            }
        }
        result.append(']');
        return result.toString();
    }

    public static int hashCode(String... values) {
        int result = 31;
        for (String value : values) {
            result ^= String.valueOf(value).hashCode();
        }
        return result;
    }

    /**
     * Gives the substring of the given text before the given separator.
     * <p>
     * If the text does not contain the given separator then the given text is
     * returned.
     *
     * @param text      d
     * @param separator d
     * @return the substring of the given text before the given separator.
     */
    public static String substringBefore(String text, char separator) {
        if (isEmpty(text)) {
            return text;
        }
        int sepPos = text.indexOf(separator);
        if (sepPos < 0) {
            return text;
        }
        return text.substring(0, sepPos);
    }

    /**
     * Gives the substring of the given text before the last occurrence of the
     * given separator.
     * <p>
     * If the text does not contain the given separator then the given text is
     * returned.
     *
     * @param text      d
     * @param separator d
     * @return the substring of the given text before the last occurrence of the given separator.
     */
    public static String substringBeforeLast(String text, char separator) {
        if (isEmpty(text)) {
            return text;
        }
        int cPos = text.lastIndexOf(separator);
        if (cPos < 0) {
            return text;
        }
        return text.substring(0, cPos);
    }

    /**
     * Gives the substring of the given text after the last occurrence of the
     * given separator.
     * <p>
     * If the text does not contain the given separator then "" is returned.
     *
     * @param text      d
     * @param separator d
     * @return the substring of the given text after the last occurrence of the given separator.
     */
    public static String substringAfterLast(String text, char separator) {
        if (isEmpty(text)) {
            return text;
        }
        int cPos = text.lastIndexOf(separator);
        if (cPos < 0) {
            return "";
        }
        return text.substring(cPos + 1);
    }

    /**
     * Gives the substring of the given text after the given separator.
     * <p>
     * If the text does not contain the given separator then "" is returned.
     *
     * @param text the input text
     * @param c    the separator char
     * @return the substring of the given text after the given separator.
     */
    public static String substringAfter(String text, char c) {
        if (isEmpty(text)) {
            return text;
        }
        int cPos = text.indexOf(c);
        if (cPos < 0) {
            return "";
        }
        return text.substring(cPos + 1);
    }

    public static String formatHtml(String text) {
        StringBuilder body = new StringBuilder();
        for (String s : text.split("\\r?\\n")) {
            s = s.replaceAll("^\\s+|\\s+$", "");
            if (s.length() > 0) {
                //段落为一张图片才认定为图片章节/漫画并启用多看单图优化，否则认定为普通文字夹杂着的图片文字。
                if (s.matches("(?i)^<img\\s([^>]+)/?>$")) {
                    body.append(s.replaceAll("(?i)^<img\\s([^>]+)/?>$",
                            "<div class=\"duokan-image-single\"><img class=\"picture-80\" $1/></div>"));
                } else {
                    body.append("<p>").append(s).append("</p>");
                }
            }
        }
        return body.toString();
    }
}
