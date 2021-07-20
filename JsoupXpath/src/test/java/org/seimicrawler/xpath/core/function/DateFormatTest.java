package org.seimicrawler.xpath.core.function;

import org.junit.Test;
import org.seimicrawler.xpath.core.XValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @description: TODO <br/>
 * @create: 2019-01-21 21:07
 * @author: zzldn@163.com
 * @since JDK1.8
 **/

public class DateFormatTest {

    @Test
    public void defaultTest() {
        List<XValue> params = new LinkedList<>();
        params.add(XValue.create("2019-01-21 19:05:42"));
        params.add(XValue.create("yyyy-MM-dd HH:mm:ss"));
        FormatDate formatDate = new FormatDate();
        XValue value = formatDate.call(null, params);
        System.out.println(value.asDate());
    }

    @Test
    public void defaultTimeTest() {
        List<XValue> params = new LinkedList<>();
        params.add(XValue.create("19:05:42"));
        params.add(XValue.create("HH:mm:ss"));
        FormatDate formatDate = new FormatDate();
        XValue value = formatDate.call(null, params);
        System.out.println(value.asDate());
    }

    @Test
    public void localTest() {
        List<XValue> params = new LinkedList<>();
        params.add(XValue.create("1/21/2019 07:05:42 AM"));
        params.add(XValue.create("MM/dd/yyyy hh:mm:ss aa"));
        params.add(XValue.create(Locale.ENGLISH.toString()));
        FormatDate formatDate = new FormatDate();
        XValue value = formatDate.call(null, params);
        System.out.println(value.asDate());
    }
}
