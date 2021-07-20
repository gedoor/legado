package org.seimicrawler.xpath.util;
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

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.core.Constants;
import org.seimicrawler.xpath.core.Scope;

import java.util.Objects;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * Date: 14-3-15
 */
public class CommonUtil {

    /**
     * 获取同名元素在同胞中的index
     *
     * @param e
     * @return
     */
    public static int getElIndexInSameTags(Element e, Scope scope) {
        Elements chs = e.parent().children();
        int index = 1;
        for (Element cur : chs) {
            if (e.tagName().equals(cur.tagName()) && scope.context().contains(cur)) {
                if (e.equals(cur)) {
                    break;
                } else {
                    index += 1;
                }
            }
        }
        return index;
    }


    /**
     * 获取同胞中同名元素的数量
     *
     * @param e
     * @return
     */
    public static int sameTagElNums(Element e, Scope scope) {
        Elements context = new Elements();
        Elements els = e.parent().getElementsByTag(e.tagName());
        for (Element el : els) {
            if (scope.context().contains(el)) {
                context.add(el);
            }
        }
        return context.size();
    }

    public static int getIndexInContext(Scope scope, Element el) {
        for (int i = 0; i < scope.context().size(); i++) {
            Element tmp = scope.context().get(i);
            if (Objects.equals(tmp, el)) {
                return i + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static Elements followingSibling(Element el) {
        Elements rs = new Elements();
        Element tmp = el.nextElementSibling();
        while (tmp != null) {
            rs.add(tmp);
            tmp = tmp.nextElementSibling();
        }
        if (rs.size() > 0) {
            return rs;
        }
        return null;
    }

    public static Elements precedingSibling(Element el) {
        Elements rs = new Elements();
        Element tmp = el.previousElementSibling();
        while (tmp != null) {
            rs.add(tmp);
            tmp = tmp.previousElementSibling();
        }
        if (rs.size() > 0) {
            return rs;
        }
        return null;
    }

    public static void setSameTagIndexInSiblings(Element ori, int index) {
        if (ori == null) {
            return;
        }
        ori.attr(Constants.EL_SAME_TAG_INDEX_KEY, String.valueOf(index));
    }

    public static int getJxSameTagIndexInSiblings(Element ori) {
        String val = ori.attr(Constants.EL_SAME_TAG_INDEX_KEY);
        if (StringUtils.isBlank(val)) {
            return -1;
        }
        return Integer.parseInt(val);
    }
}
