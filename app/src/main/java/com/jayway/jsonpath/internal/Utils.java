/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath.internal;

import com.jayway.jsonpath.JsonPathException;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

public final class Utils {

    // accept a collection of objects, since all objects have toString()
    public static String join(String delimiter, String wrap, Iterable<?> objs) {
        Iterator<?> iter = objs.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append(wrap).append(iter.next()).append(wrap);
        while (iter.hasNext()) {
            buffer.append(delimiter).append(wrap).append(iter.next()).append(wrap);
        }
        return buffer.toString();
    }

    // accept a collection of objects, since all objects have toString()
    public static String join(String delimiter, Iterable<?> objs) {
        return join(delimiter, "", objs);
    }

    public static String concat(CharSequence... strings) {
        if (strings.length == 0) {
            return "";
        }
        if (strings.length == 1) {
            return strings[0].toString();
        }
        int length = 0;
        // -1 = no result, -2 = multiple results
        int indexOfSingleNonEmptyString = -1;
        for (int i = 0; i < strings.length; i++) {
            CharSequence charSequence = strings[i];
            int len = charSequence.length();
            length += len;
            if (indexOfSingleNonEmptyString != -2 && len > 0) {
                if (indexOfSingleNonEmptyString == -1) {
                    indexOfSingleNonEmptyString = i;
                } else {
                    indexOfSingleNonEmptyString = -2;
                }
            }
        }
        if (length == 0) {
            return "";
        }
        if (indexOfSingleNonEmptyString > 0) {
            return strings[indexOfSingleNonEmptyString].toString();
        }
        StringBuilder sb = new StringBuilder(length);
        for (CharSequence charSequence : strings) {
            sb.append(charSequence);
        }
        return sb.toString();

    }

    //---------------------------------------------------------
    //
    // IO
    //
    //---------------------------------------------------------

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignore) {
        }
    }

    public static String escape(String str, boolean escapeSingleQuote) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        StringWriter writer = new StringWriter(len * 2);

        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                writer.write("\\u" + hex(ch));
            } else if (ch > 0xff) {
                writer.write("\\u0" + hex(ch));
            } else if (ch > 0x7f) {
                writer.write("\\u00" + hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        writer.write('\\');
                        writer.write('b');
                        break;
                    case '\n':
                        writer.write('\\');
                        writer.write('n');
                        break;
                    case '\t':
                        writer.write('\\');
                        writer.write('t');
                        break;
                    case '\f':
                        writer.write('\\');
                        writer.write('f');
                        break;
                    case '\r':
                        writer.write('\\');
                        writer.write('r');
                        break;
                    default :
                        if (ch > 0xf) {
                            writer.write("\\u00" + hex(ch));
                        } else {
                            writer.write("\\u000" + hex(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':
                        if (escapeSingleQuote) {
                            writer.write('\\');
                        }
                        writer.write('\'');
                        break;
                    case '"':
                        writer.write('\\');
                        writer.write('"');
                        break;
                    case '\\':
                        writer.write('\\');
                        writer.write('\\');
                        break;
                    case '/':
                        writer.write('\\');
                        writer.write('/');
                        break;
                    default :
                        writer.write(ch);
                        break;
                }
            }
        }
        return writer.toString();
    }

    public static String unescape(String str) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        StringWriter writer = new StringWriter(len);
        StringBuilder unicode = new StringBuilder(4);
        boolean hadSlash = false;
        boolean inUnicode = false;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (inUnicode) {
                unicode.append(ch);
                if (unicode.length() == 4) {
                    try {
                        int value = Integer.parseInt(unicode.toString(), 16);
                        writer.write((char) value);
                        unicode.setLength(0);
                        inUnicode = false;
                        hadSlash = false;
                    } catch (NumberFormatException nfe) {
                        throw new JsonPathException("Unable to parse unicode value: " + unicode, nfe);
                    }
                }
                continue;
            }
            if (hadSlash) {
                hadSlash = false;
                switch (ch) {
                    case '\\':
                        writer.write('\\');
                        break;
                    case '\'':
                        writer.write('\'');
                        break;
                    case '\"':
                        writer.write('"');
                        break;
                    case 'r':
                        writer.write('\r');
                        break;
                    case 'f':
                        writer.write('\f');
                        break;
                    case 't':
                        writer.write('\t');
                        break;
                    case 'n':
                        writer.write('\n');
                        break;
                    case 'b':
                        writer.write('\b');
                        break;
                    case 'u':
                    {
                        inUnicode = true;
                        break;
                    }
                    default :
                        writer.write(ch);
                        break;
                }
                continue;
            } else if (ch == '\\') {
                hadSlash = true;
                continue;
            }
            writer.write(ch);
        }
        if (hadSlash) {
            writer.write('\\');
        }
        return writer.toString();
    }

    /**
     * Returns an upper case hexadecimal <code>String</code> for the given
     * character.
     *
     * @param ch The character to map.
     * @return An upper case hexadecimal <code>String</code>
     */
    public static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase();
    }

    /**
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     * <p/>
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     * <p/>
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the CharSequence.
     * That functionality is available in isBlank().</p>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Used by the indexOf(CharSequence methods) as a green implementation of indexOf.
     *
     * @param cs         the {@code CharSequence} to be processed
     * @param searchChar the {@code CharSequence} to be searched for
     * @param start      the start index
     * @return the index where the search sequence was found
     */
    static int indexOf(CharSequence cs, CharSequence searchChar, int start) {
        return cs.toString().indexOf(searchChar.toString(), start);
    }



    //---------------------------------------------------------
    //
    // Validators
    //
    //---------------------------------------------------------

    /**
     * <p>Validate that the specified argument is not {@code null};
     * otherwise throwing an exception with the specified message.
     * <p/>
     * <pre>Validate.notNull(myObject, "The object must not be null");</pre>
     *
     * @param <T>     the object type
     * @param object  the object to check
     * @param message the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message
     * @return the validated object (never {@code null} for method chaining)
     * @throws NullPointerException if the object is {@code null}
     */
    public static <T> T notNull(T object, String message, Object... values) {
        if (object == null) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return object;
    }

    /**
     * <p>Validate that the argument condition is {@code true}; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a
     * primitive number or using your own custom validation expression.</p>
     * <p/>
     * <pre>Validate.isTrue(i > 0.0, "The value must be greater than zero: %d", i);</pre>
     * <p/>
     * <p>For performance reasons, the long value is passed as a separate parameter and
     * appended to the exception message only in the case of an error.</p>
     *
     * @param expression the boolean expression to check
     * @param message
     * @throws IllegalArgumentException if expression is {@code false}
     */
    public static void isTrue(boolean expression, String message) {
        if (expression == false) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Check if one and only one condition is true; otherwise
     * throw an exception with the specified message.
     *
     * @param message     error describing message
     * @param expressions the boolean expressions to check
     * @throws IllegalArgumentException if zero or more than one expressions are true
     */
    public static void onlyOneIsTrue(final String message, final boolean... expressions) {
        if (!onlyOneIsTrueNonThrow(expressions)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static boolean onlyOneIsTrueNonThrow(final boolean... expressions) {
        int count = 0;
        for (final boolean expression : expressions) {
            if (expression && ++count > 1) {
                return false;
            }
        }
        return 1 == count;
    }

    /**
     * <p>Validate that the specified argument character sequence is
     * neither {@code null} nor a length of zero (no characters);
     * otherwise throwing an exception with the specified message.
     * <p/>
     * <pre>Validate.notEmpty(myString, "The string must not be empty");</pre>
     *
     * @param <T>     the character sequence type
     * @param chars   the character sequence to check, validated not null by this method
     * @param message the {@link String#format(String, Object...)} exception message if invalid, not null
     * @param values  the optional values for the formatted exception message, null array not recommended
     * @return the validated character sequence (never {@code null} method for chaining)
     * @throws NullPointerException     if the character sequence is {@code null}
     * @throws IllegalArgumentException if the character sequence is empty
     */
    public static <T extends CharSequence> T notEmpty(T chars, String message, Object... values) {
        if (chars == null) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        if (chars.length() == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return chars;
    }


    //---------------------------------------------------------
    //
    // Converters
    //
    //---------------------------------------------------------
    public static String toString(Object o) {
        if (null == o) {
            return null;
        }
        return o.toString();
    }

    private Utils() {
    }
}
