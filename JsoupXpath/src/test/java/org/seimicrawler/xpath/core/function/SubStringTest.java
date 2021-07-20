package org.seimicrawler.xpath.core.function;

import org.junit.Assert;
import org.junit.Test;
import org.seimicrawler.xpath.core.XValue;

import java.util.LinkedList;
import java.util.List;

/**
 * SubString Tester.
 *
 * @author seimimaster@gmail.com
 * @version 1.0
 */
public class SubStringTest {

    /**
     * substring("12345", 1.5, 2.6) returns "234"
     * Method: call(Element context, List<XValue> params)
     */
    @Test
    public void testCall() throws Exception {
        List<XValue> params = new LinkedList<>();
        params.add(XValue.create("12345"));
        params.add(XValue.create("1.5"));
        params.add(XValue.create("2.6"));
        SubString subStringFunc = new SubString();
        Assert.assertEquals(subStringFunc.call(null, params).asString(), "234");
    }

    @Test
    public void testZeroLength() throws Exception {
        List<XValue> params = new LinkedList<>();
        params.add(XValue.create("12345"));
        params.add(XValue.create("2"));
        params.add(XValue.create("-6"));
        SubString subStringFunc = new SubString();
        Assert.assertEquals(subStringFunc.call(null, params).asString(), "");
    }

    @Test
    public void testOneLength() throws Exception {
        List<XValue> params = new LinkedList<>();
        params.add(XValue.create("12345"));
        params.add(XValue.create("0"));
        params.add(XValue.create("1"));
        SubString subStringFunc = new SubString();
        Assert.assertEquals(subStringFunc.call(null, params).asString(), "1");
    }


} 
