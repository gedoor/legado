package org.seimicrawler.xpath;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.seimicrawler.xpath.exception.XpathSyntaxErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JXDocument Tester.
 *
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @version 1.0
 */
@RunWith(DataProviderRunner.class)
public class JXDocumentTest {

    private JXDocument underTest;

    private JXDocument doubanTest;

    private JXDocument custom;
    private final ClassLoader loader = getClass().getClassLoader();
    private final Logger logger = LoggerFactory.getLogger(JXDocumentTest.class);

    @Before
    public void before() throws Exception {
        String html = "<html><body><script>console.log('aaaaa')</script><div class='test'>some body</div><div class='xiao'>Two</div></body></html>";
        underTest = JXDocument.create(html);
        if (doubanTest == null) {
            URL t = loader.getResource("d_test.html");
            assert t != null;
            File dBook = new File(t.toURI());
            String context = FileUtils.readFileToString(dBook, StandardCharsets.UTF_8);
            doubanTest = JXDocument.create(context);
        }
        custom = JXDocument.create("<li><b>性别：</b>男</li>");
    }

    /**
     * Method: sel(String xpath)
     */
    @Test
    public void testSel() throws Exception {
        String xpath = "//script[1]/text()";
        JXNode res = underTest.selNOne(xpath);
        Assert.assertNotNull(res);
        Assert.assertEquals("console.log('aaaaa')", res.asString());
    }

    @Test
    public void testNotMatchFilter() throws Exception {
        String xpath = "//div[contains(@class,'xiao')]/text()";
        JXNode node = underTest.selNOne(xpath);
        Assert.assertEquals("Two", node.asString());
    }

    @Test
    @DataProvider(value = {
            "//a/@href",
            "//div[@class='paginator']/span[@class='next']/a/@href",
    })
    public void testXpath(String xpath) throws XpathSyntaxErrorException {
        logger.info("current xpath: {}", xpath);
        List<JXNode> rs = doubanTest.selN(xpath);
        for (JXNode n : rs) {
            if (!n.isString()) {
                int index = n.asElement().siblingIndex();
                logger.info("index = {}", index);
            }
            logger.info(n.toString());
        }
    }

    /**
     * d_test.html 来源于 https://book.douban.com/tag/%E4%BA%92%E8%81%94%E7%BD%91
     * <p>
     * 为了测试各种可能情况，ul[@class='subject-list']节点以及其下内容被复制了一份出来，并修改部分书名前缀为'T2-'以便区分
     */
    @DataProvider
    public static Object[][] dataOfXpathAndexpect() {
        return new Object[][]{
                {"//ul[@class='subject-list']/li[position()<3][last()]/div/h2/allText()", "黑客与画家 : 硅谷创业之父Paul Graham文集T2-黑客与画家 : 硅谷创业之父Paul Graham文集"},
                {"//ul[@class='subject-list']/li[first()]/div/h2/allText()", "失控 : 全人类的最终命运和结局T2-失控 : 全人类的最终命运和结局"},
                {"//ul[@class='subject-list']/li[./div/div/span[@class='pl']/num()>(1000+90*(2*50))][last()][1]/div/h2/allText()", "长尾理论长尾理论"},
                {"//ul[@class='subject-list']/li[self::li/div/div/span[@class='pl']/num()>10000][-1]/div/h2/allText()", "长尾理论长尾理论"},
                {"//ul[@class='subject-list']/li[contains(self::li/div/div/span[@class='pl']//text(),'14582')]/div/h2//text()", "黑客与画家: 硅谷创业之父Paul Graham文集T2-黑客与画家: 硅谷创业之父Paul Graham文集"},
                {"//ul[@class='subject-list']/li[contains(./div/div/span[@class='pl']//text(),'14582')]/div/h2//text()", "黑客与画家: 硅谷创业之父Paul Graham文集T2-黑客与画家: 硅谷创业之父Paul Graham文集"},
                {"//*[@id=\"subject_list\"]/ul/li[2]/div[2]/h2/a//text()", "黑客与画家: 硅谷创业之父Paul Graham文集T2-黑客与画家: 硅谷创业之父Paul Graham文集"},
                {"//ul[@class]", 3L},
                {"//a[@id]/@href", "https://www.douban.com/doumail/"},
                {"//*[@id=\"subject_list\"]/ul[1]/li[8]/div[2]/div[2]/span[3]/num()", "3734.0"},
                {"//a[@id]/@href | //*[@id=\"subject_list\"]/ul[1]/li[8]/div[2]/div[2]/span[3]/num()", "https://www.douban.com/doumail/3734.0"},
        };
    }

    @UseDataProvider("dataOfXpathAndexpect")
    @Test
    public void testXpathAndAssert(String xpath, Object expect) throws XpathSyntaxErrorException {
        logger.info("current xpath: {}", xpath);
        List<JXNode> rs = doubanTest.selN(xpath);
        if (expect instanceof String) {
            String res = StringUtils.join(rs, "");
            logger.info(res);
            Assert.assertEquals(expect, res);
        } else if (expect instanceof Number) {
            long size = (long) expect;
            Assert.assertEquals(size, rs.size());
        }
    }

    @Test
    @DataProvider(value = {
            "//ul[@class='subject-list']/li[position()<3]"
    })
    public void testJXNode(String xpath) throws XpathSyntaxErrorException {
        logger.info("current xpath: {}", xpath);
        List<JXNode> jxNodeList = doubanTest.selN(xpath);
        Set<String> expect = new HashSet<>();
        //第一个 ul 中的
        expect.add("失控: 全人类的最终命运和结局");
        expect.add("黑客与画家: 硅谷创业之父Paul Graham文集");
        //第二个 ul 中的
        expect.add("T2-失控: 全人类的最终命运和结局");
        expect.add("T2-黑客与画家: 硅谷创业之父Paul Graham文集");

        Set<String> res = new HashSet<>();
        for (JXNode node : jxNodeList) {
            if (!node.isString()) {
                String currentRes = StringUtils.join(node.sel("/div/h2/a//text()"), "");
                logger.info(currentRes);
                res.add(currentRes);
            }
        }
        Assert.assertEquals(expect, res);
    }

    @Test
    @DataProvider(value = {
            "//ul[@class='subject-list']"
    })
    public void testRecursionNode(String xpath) throws XpathSyntaxErrorException {
        logger.info("current xpath: {}", xpath);
        List<JXNode> jxNodeList = doubanTest.selN(xpath);
        logger.info("size = {}", jxNodeList.size());
        // 有两个ul，下面的是为了测试特意复制添加的
        Assert.assertEquals(2, jxNodeList.size());
    }

    @Test
    @DataProvider(value = {
            "//body/div/div/h1/text()",
            "/body/div/div/h1/text()"
    })
    public void absolutePathTest(String xpath) throws XpathSyntaxErrorException {
        logger.info("current xpath: {}", xpath);
        List<JXNode> jxNodeList = doubanTest.selN(xpath);
        logger.info("size = {}，res ={}", jxNodeList.size(), jxNodeList);
    }

    @Test
    public void testAs() throws XpathSyntaxErrorException {
        List<JXNode> jxNodeList = custom.selN("//b[contains(text(),'性别')]/parent::*/text()");
        Assert.assertEquals("男", StringUtils.join(jxNodeList, ""));
        for (JXNode jxNode : jxNodeList) {
            logger.info(jxNode.toString());
        }
    }

    /**
     * fix https://github.com/zhegexiaohuozi/JsoupXpath/issues/33
     */
//    @Test
    public void testNotObj() {
        JXDocument doc = JXDocument.createByUrl("https://www.gxwztv.com/61/61514/");
//        List<JXNode> nodes = doc.selN("//*[@id=\"chapters-list\"]/li[@style]");
        List<JXNode> nodes = doc.selN("//*[@id=\"chapters-list\"]/li[not(@style)]");
        for (JXNode node : nodes) {
            logger.info("r = {}", node);
        }
    }

    /**
     * fix https://github.com/zhegexiaohuozi/JsoupXpath/issues/34
     */
    @Test
    public void testAttrAtRoot() {
        String content = "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <a href=\"/124/124818/162585930.html\">第2章 神奇交流群</a>\n" +
                " </body>\n" +
                "</html>";
        JXDocument doc = JXDocument.create(content);
        List<JXNode> nodes = doc.selN("//@href");
        for (JXNode node : nodes) {
            logger.info("r = {}", node);
        }
    }

    @Test
    public void testA() {
        String content = "<span style=\"color: #5191ce;\" >网页设计师</span>";
        JXDocument doc = JXDocument.create(content);
        List<JXNode> nodes = doc.selN("//*[text()='网页设计师']");
        for (JXNode node : nodes) {
            logger.info("r = {}", node);
        }
    }

    /**
     * fix https://github.com/zhegexiaohuozi/JsoupXpath/issues/52
     */
    @Test
    public void fixTextBehaviorTest() {
        String html = "<p><span class=\"text-muted\">分类：</span>动漫<span class=\"split-line\"></span><span class=\"text-muted hidden-xs\">地区：</span>日本<span class=\"split-line\"></span><span class=\"text-muted hidden-xs\">年份：</span>2010</p>";
        JXDocument jxDocument = JXDocument.create(html);
        List<JXNode> jxNodes = jxDocument.selN("//text()[3]");
        String actual = StringUtils.join(jxNodes, "");
        logger.info("actual = {}", actual);
        Assert.assertEquals("2010", actual);
        List<JXNode> nodes = jxDocument.selN("//text()");
        String allText = StringUtils.join(nodes, "");
        Assert.assertEquals("分类：动漫地区：日本年份：2010", allText);
        logger.info("all = {}", allText);
    }

    /**
     * fix https://github.com/zhegexiaohuozi/JsoupXpath/issues/44
     */
    @Test
    public void fixTextElNoParentTest() {
        String test = "<div class='a'> a <div>need</div> <div class='e'> not need</div> c </div>";
        JXDocument j = JXDocument.create(test);
        List<JXNode> l = j.selN("//div[@class='a']//text()[not(ancestor::div[@class='e'])]");
        Set<String> finalRes = new HashSet<>();
        for (JXNode i : l) {
            logger.info("{}", i.toString());
            finalRes.add(i.asString());
        }
        Assert.assertFalse(finalRes.contains("not need"));
        Assert.assertTrue(finalRes.contains("need"));
        Assert.assertEquals(4, finalRes.size());
    }

    /**
     * fix https://github.com/zhegexiaohuozi/JsoupXpath/issues/53
     */
    @Test
    public void fixIssue53() {
        String content = "<li class=\"res-book-item\" data-bid=\"1018351389\" data-rid=\"1\"> \n" +
                " <div class=\"book-img-box\"> <a href=\"//book.qidian.com/info/1018351389\" target=\"_blank\" data-eid=\"qd_S04\" data-algrid=\"0.0.0\" data-bid=\"1018351389\"><img src=\"//bookcover.yuewen.com/qdbimg/349573/1018351389/150\"></a> \n" +
                " </div> \n" +
                " <div class=\"book-mid-info\"> \n" +
                "  <h4><a href=\"//book.qidian.com/info/1018351389\" target=\"_blank\" data-eid=\"qd_S05\" data-bid=\"1018351389\" data-algrid=\"0.0.0\"><cite class=\"red-kw\">我们</cite>平凡<cite class=\"red-kw\">我们</cite>忠诚</a></h4> \n" +
                "  <p class=\"author\"> <img src=\"//qidian.gtimg.com/qd/images/ico/user.f22d3.png\"><a class=\"name\" data-eid=\"qd_S06\" href=\"//my.qidian.com/author/403791004\" target=\"_blank\">巡璃</a> <em>|</em><a href=\"//www.qidian.com/duanpian\" data-eid=\"qd_S07\" target=\"_blank\">短篇</a><em>|</em><span>连载</span> </p> \n" +
                "  <p class=\"intro\"> 这是一位普通老兵的故事，这位老兵没有走上战场，也没有人歌颂他，但他的工作却是面对生与死，他是一名普通的军转干部，没有得到任何荣誉，却仍旧坚守着信仰，永远忠诚。除了他的家人，他的战友，他的故事不被任何人所知，但他的故事正是一代军人、一代军转干部的写照。所以，我来歌颂他，歌颂那一代人。 </p> \n" +
                "  <p class=\"update\"><a href=\"//read.qidian.com/chapter/YiObT_DmJpXu4xLcYRGW6w2/Ulsr6ThvJS5p4rPq4Fd4KQ2\" target=\"_blank\" data-eid=\"qd_S08\" data-bid=\"1018351389\" data-cid=\"//read.qidian.com/chapter/YiObT_DmJpXu4xLcYRGW6w2/Ulsr6ThvJS5p4rPq4Fd4KQ2\">最新更新 第一次见识到生死</a><em>·</em><span>2020-02-19</span> </p> \n" +
                " </div> \n" +
                " <div class=\"book-right-info\"> \n" +
                "  <div class=\"total\"> \n" +
                "   <p><span> 4497</span>总字数</p> \n" +
                "   <p><span> 0</span>总推荐</p> \n" +
                "  </div> \n" +
                "  <p class=\"btn\"> <a class=\"red-btn\" href=\"//book.qidian.com/info/1018351389\" data-eid=\"qd_S02\" target=\"_blank\">书籍详情</a> <a class=\"blue-btn add-book\" href=\"javascript:\" data-eid=\"qd_S03\" data-bookid=\"1018351389\" data-bid=\"1018351389\">加入书架</a> </p> \n" +
                " </div> </li>";
        JXDocument j = JXDocument.create(content);
        List<JXNode> l = j.selN("//*[text()='总字数']//text()");
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("4497", l.get(0).asString());
        Assert.assertEquals("总字数", l.get(1).asString());
    }

}
