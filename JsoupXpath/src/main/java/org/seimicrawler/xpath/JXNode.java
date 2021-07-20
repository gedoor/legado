package org.seimicrawler.xpath;
/*
   Copyright 2014 Wang Haomiao<seimimaster@gmail.com>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.Constants;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * XPath提取后的
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2016/5/12.
 */
public class JXNode {
    private final Object value;

    public JXNode(Object val) {
        this.value = val;
    }

    public boolean isElement() {
        return value instanceof Element;
    }

    public Element asElement() {
        return (Element) value;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public String asString() {
        if (isString()) {
            return (String) value;
        } else if (isElement()) {
            Element e = (Element) value;
            if (Objects.equals(e.tagName(), Constants.DEF_TEXT_TAG_NAME)) {
                return e.ownText();
            } else {
                return e.toString();
            }
        } else {
            return String.valueOf(value);
        }
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public Double asDouble() {
        return (Double) value;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    public Boolean asBoolean() {
        return (Boolean) value;
    }

    public boolean isDate() {
        return value instanceof Date;
    }

    public Date asDate() {
        return (Date) value;
    }

    public List<JXNode> sel(String xpath) {
        if (!isElement()) {
            return null;
        }
        JXDocument doc = new JXDocument(new Elements(asElement()));
        return doc.selN(xpath);
    }

    public JXNode selOne(String xpath) {
        List<JXNode> jxNodeList = sel(xpath);
        if (jxNodeList != null && jxNodeList.size() > 0) {
            return jxNodeList.get(0);
        }
        return null;
    }

    public static JXNode create(Object val) {
        return new JXNode(val);
    }

    @Override
    public String toString() {
        return asString();
    }

    public Object value() {
        if (isElement()) {
            return asElement();
        }
        if (isBoolean()) {
            return asBoolean();
        }
        if (isNumber()) {
            return asDouble();
        }
        if (isDate()) {
            return asDate();
        }
        return asString();
    }
}
