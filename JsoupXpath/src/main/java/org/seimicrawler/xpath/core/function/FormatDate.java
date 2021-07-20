package org.seimicrawler.xpath.core.function;

import org.apache.commons.lang3.time.FastDateFormat;
import org.seimicrawler.xpath.core.Function;
import org.seimicrawler.xpath.core.Scope;
import org.seimicrawler.xpath.core.XValue;
import org.seimicrawler.xpath.exception.XpathParserException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Function: string format-date(string, string, string)
 * The format-date function returns Date object
 * The first parameter is the date and time
 * The second parameter is the time format of the first parameter.
 * The third parameter is not required, and is required if the date format of the first parameter requires that the time zone must be specified
 * For example, format-date("1999/04/01","yyyy/MM/dd") returns Date Object, and format-date("1999/04/01 07:55:23 pm","yyyy/MM/dd hh:mm:ss a",'en') returns Date Object.
 *
 * @author github.com/zzldn@163.com
 * @since 2019/1/22.
 */
public class FormatDate implements Function {
    @Override
    public String name() {
        return "format-date";
    }

    @Override
    public XValue call(Scope scope, List<XValue> params) {
        String value = params.get(0).asString();
        String patten = params.get(1).asString();
        try {
            if (params.size() > 2 && null != params.get(2)) {
                final Locale locale = Locale.forLanguageTag(params.get(2).asString());
                final SimpleDateFormat format = new SimpleDateFormat(patten, locale);
                return XValue.create(format.parse(value));
            }
            return XValue.create(FastDateFormat.getInstance(patten).parse(value));
        } catch (ParseException e) {
            throw new XpathParserException("date format exception!", e);
        }

    }
}
