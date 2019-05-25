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

public class JsonFormatter {

    private static final String INDENT = "   ";

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final int MODE_SINGLE = 100;
    private static final int MODE_DOUBLE = 101;
    private static final int MODE_ESCAPE_SINGLE = 102;
    private static final int MODE_ESCAPE_DOUBLE = 103;
    private static final int MODE_BETWEEN = 104;

    private static void appendIndent(StringBuilder sb, int count) {
        for (; count > 0; --count) sb.append(INDENT);
    }

    public static String prettyPrint(String input) {

        input = input.replaceAll("[\\r\\n]", "");

        StringBuilder output = new StringBuilder(input.length() * 2);
        int mode = MODE_BETWEEN;
        int depth = 0;

        for (int i = 0; i < input.length(); ++i) {
            char ch = input.charAt(i);

            switch (mode) {
                case MODE_BETWEEN:
                    switch (ch) {
                        case '{':
                        case '[':
                            output.append(ch);
                            output.append(NEW_LINE);
                            appendIndent(output, ++depth);
                            break;
                        case '}':
                        case ']':
                            output.append(NEW_LINE);
                            appendIndent(output, --depth);
                            output.append(ch);
                            break;
                        case ',':
                            output.append(ch);
                            output.append(NEW_LINE);
                            appendIndent(output, depth);
                            break;
                        case ':':
                            output.append(" : ");
                            break;
                        case '\'':
                            output.append(ch);
                            mode = MODE_SINGLE;
                            break;
                        case '"':
                            output.append(ch);
                            mode = MODE_DOUBLE;
                            break;
                        case ' ':
                            break;
                        default:
                            output.append(ch);
                            break;
                    }
                    break;
                case MODE_ESCAPE_SINGLE:
                    output.append(ch);
                    mode = MODE_SINGLE;
                    break;
                case MODE_ESCAPE_DOUBLE:
                    output.append(ch);
                    mode = MODE_DOUBLE;
                    break;
                case MODE_SINGLE:
                    output.append(ch);
                    switch (ch) {
                        case '\'':
                            mode = MODE_BETWEEN;
                            break;
                        case '\\':
                            mode = MODE_ESCAPE_SINGLE;
                            break;
                    }
                    break;
                case MODE_DOUBLE:
                    output.append(ch);
                    switch (ch) {
                        case '"':
                            mode = MODE_BETWEEN;
                            break;
                        case '\\':
                            mode = MODE_ESCAPE_DOUBLE;
                            break;
                    }
                    break;
            }
        }
        return output.toString();
    }
}
