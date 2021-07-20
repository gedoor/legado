package org.seimicrawler.xpath.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.exception.XpathParserException;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2017/12/5.
 */
public class XValue implements Comparable<XValue> {
    public XValue(Object val) {
        this.value = val;
    }

    public static XValue create(Object val) {
        return new XValue(val);
    }

    private Object value;

    private boolean isAttr = false;
    private boolean isExprStr = false;

    /**
     * 同类型节点的顺序
     */
    private int siblingIndex;

    /**
     * 满足特殊场景计算筛选需求
     */
    private List<XValue> xValues;

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public boolean isElements() {
        return value instanceof Elements;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isList() {
        return value instanceof List;
    }

    public boolean isDate() {
        return value instanceof Date;
    }

    public Boolean asBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && !StringUtils.isBlank(asString());
    }

    public Date asDate() {
        if (value instanceof String) {
            try {
                return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.parse((String) value);
            } catch (ParseException e) {
                throw new XpathParserException("cast to date fail. vale = " + value);
            }
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        throw new XpathParserException("cast to date fail. vale = " + value);
    }

    public Double asDouble() {
        if (value instanceof String) {
            return new BigDecimal((String) value).doubleValue();
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            throw new XpathParserException("cast to number fail. vale = " + value);
        }
    }

    public Long asLong() {
        if (value instanceof String) {
            //对于带小数点的，四舍五入
            return new BigDecimal((String) value).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            throw new XpathParserException("cast to number fail. vale = " + value);
        }
    }

    public Elements asElements() {
        return (Elements) value;
    }

    public String asString() {
        if (isElements()) {
            StringBuilder accum = new StringBuilder();
            for (Element e : asElements()) {
                accum.append(e.ownText());
            }
            return accum.toString();
        } else if (value instanceof Element && Objects.equals(((Element) value).tagName(), Constants.DEF_TEXT_TAG_NAME)) {
            return ((Element) value).ownText();
        } else if (value instanceof List) {
            return StringUtils.join((List) value, ",");
        } else {
            return String.valueOf(value).trim();
        }
    }

    public List<String> asList() {
        return (List<String>) value;
    }

    public XValue attr() {
        this.isAttr = true;
        return this;
    }

    public boolean isAttr() {
        return isAttr;
    }

    public XValue exprStr() {
        this.isExprStr = true;
        String str = StringUtils.removeStart(String.valueOf(this.value), "'");
        str = StringUtils.removeStart(str, "\"");
        str = StringUtils.removeEnd(str, "'");
        this.value = StringUtils.removeEnd(str, "\"");
        return this;
    }

    public boolean isExprStr() {
        return isExprStr;
    }

    public int getSiblingIndex() {
        return siblingIndex;
    }

    public void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    public List<XValue> getxValues() {
        return xValues;
    }

    public void setxValues(List<XValue> xValues) {
        this.xValues = xValues;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("value", value)
                .append("isAttr", isAttr)
                .append("isExprStr", isExprStr)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XValue value1 = (XValue) o;
        return Objects.equals(value, value1.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }


    @Override
    public int compareTo(XValue o) {
        if (this.equals(o)) {
            return 0;
        }
        if (o == null || o.value == null) {
            return 1;
        }
        if (this.value == null) {
            return -1;
        }

        if (isString()) {
            return asString().compareTo(o.asString());
        } else if (isNumber()) {
            return asDouble().compareTo(o.asDouble());
        } else {
            throw new XpathParserException("Unsupported comparable XValue = " + toString());
        }
    }

    public Class valType() {
        if (value == null) {
            return Object.class;
        }
        return value.getClass();
    }
}
