package me.ag2s.epublib.epub;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.domain.Author;
import me.ag2s.epublib.domain.EpubBook;
import me.ag2s.epublib.domain.Identifier;
import me.ag2s.epublib.domain.MediaType;
import me.ag2s.epublib.domain.MediaTypes;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.domain.TOCReference;
import me.ag2s.epublib.domain.TableOfContents;
import me.ag2s.epublib.util.ResourceUtil;
import me.ag2s.epublib.util.StringUtil;

/**
 * Writes the ncx document as defined by namespace http://www.daisy.org/z3986/2005/ncx/
 *
 * @author Ag2S20150909
 */

public class NCXDocumentV3 {
    public static final String NAMESPACE_XHTML = "http://www.w3.org/1999/xhtml";
    public static final String NAMESPACE_EPUB = "http://www.idpf.org/2007/ops";
    public static final String LANGUAGE = "en";
    @SuppressWarnings("unused")
    public static final String PREFIX_XHTML = "html";
    public static final String NCX_ITEM_ID = "htmltoc";
    public static final String DEFAULT_NCX_HREF = "toc.xhtml";
    public static final String V3_NCX_PROPERTIES = "nav";
    public static final MediaType V3_NCX_MEDIATYPE = MediaTypes.XHTML;

    private static final String TAG = NCXDocumentV3.class.getName();

    private interface XHTMLTgs {
        String html = "html";
        String head = "head";
        String title = "title";
        String meta = "meta";
        String link = "link";
        String body = "body";
        String h1 = "h1";
        String h2 = "h2";
        String nav = "nav";
        String ol = "ol";
        String li = "li";
        String a = "a";
        String span = "span";
    }

    private interface XHTMLAttributes {
        String xmlns = "xmlns";
        String xmlns_epub = "xmlns:epub";
        String lang = "lang";
        String xml_lang = "xml:lang";
        String rel = "rel";
        String type = "type";
        String epub_type = "epub:type";//nav的必须属性
        String id = "id";
        String role = "role";
        String href = "href";
        String http_equiv = "http-equiv";
        String content = "content";

    }

    private interface XHTMLAttributeValues {
        String Content_Type = "Content-Type";
        String HTML_UTF8 = "text/html; charset=utf-8";
        String lang = "en";
        String epub_type = "toc";
        String role_toc = "doc-toc";

    }


    /**
     * 解析epub的目录文件
     *
     * @param book       Book
     * @param epubReader epubreader
     * @return Resource
     */
    @SuppressWarnings("unused")
    public static Resource read(EpubBook book, EpubReader epubReader) {
        Resource ncxResource = null;
        if (book.getSpine().getTocResource() == null) {
            Log.e(TAG, "Book does not contain a table of contents file");
            return null;
        }
        try {
            ncxResource = book.getSpine().getTocResource();
            if (ncxResource == null) {
                return null;
            }
            //一些epub 3 文件没有按照epub3的标准使用删除掉ncx目录文件
            if (ncxResource.getHref().endsWith(".ncx")) {
                Log.v(TAG, "该epub文件不标准，使用了epub2的目录文件");
                return NCXDocumentV2.read(book, epubReader);
            }
            Log.d(TAG, ncxResource.getHref());

            Document ncxDocument = ResourceUtil.getAsDocument(ncxResource);
            Log.d(TAG, ncxDocument.getNodeName());

            Element navMapElement = (Element) ncxDocument.getElementsByTagName(XHTMLTgs.nav).item(0);
            if (navMapElement == null) {
                Log.d(TAG, "epub3目录文件未发现nav节点，尝试使用epub2的规则解析");
                return NCXDocumentV2.read(book, epubReader);
            }
            navMapElement = (Element) navMapElement.getElementsByTagName(XHTMLTgs.ol).item(0);
            Log.d(TAG, navMapElement.getTagName());

            TableOfContents tableOfContents = new TableOfContents(
                    readTOCReferences(navMapElement.getChildNodes(), book));
            Log.d(TAG, tableOfContents.toString());
            book.setTableOfContents(tableOfContents);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return ncxResource;
    }

    private static List<TOCReference> doToc(Node n, EpubBook book) {
        if (n == null || n.getNodeType() != Document.ELEMENT_NODE) {
            return new ArrayList<>();
        }

        Element el = (Element) n;
        Node node = el.getElementsByTagName(XHTMLTgs.ol).item(0);

        if (node == null || node.getNodeType() != Document.ELEMENT_NODE) {
            return new ArrayList<>();
        }

        return readTOCReferences(node.getChildNodes(), book);
    }


    static List<TOCReference> readTOCReferences(NodeList navpoints,
                                                EpubBook book) {
        if (navpoints == null) {
            return new ArrayList<>();
        }
        //Log.d(TAG, "readTOCReferences:navpoints.getLength()" + navpoints.getLength());
        List<TOCReference> result = new ArrayList<>(navpoints.getLength());
        for (int i = 0; i < navpoints.getLength(); i++) {
            Node node = navpoints.item(i);
            //如果该node是null,或者不是Element,跳出本次循环
            if (node == null || node.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) node;
            //如果该Element的name为”li“,将其添加到目录结果
            if (el.getTagName().equals(XHTMLTgs.li)) {
                result.add(readTOCReference(el, book));
            }

        }


        return result;
    }


    static TOCReference readTOCReference(Element navpointElement, EpubBook book) {
        //章节的名称
        String label = readNavLabel(navpointElement);
        //Log.d(TAG, "label:" + label);
        String tocResourceRoot = StringUtil
                .substringBeforeLast(book.getSpine().getTocResource().getHref(), '/');
        if (tocResourceRoot.length() == book.getSpine().getTocResource().getHref()
                .length()) {
            tocResourceRoot = "";
        } else {
            tocResourceRoot = tocResourceRoot + "/";
        }

        String reference = StringUtil
                .collapsePathDots(tocResourceRoot + readNavReference(navpointElement));
        String href = StringUtil
                .substringBefore(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
        String fragmentId = StringUtil
                .substringAfter(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
        Resource resource = book.getResources().getByHref(href);
        if (resource == null) {
            Log.e(TAG, "Resource with href " + href + " in NCX document not found");
        }
//        Log.v(TAG, "label:" + label);
//        Log.v(TAG, "href:" + href);
//        Log.v(TAG, "fragmentId:" + fragmentId);

        //父级目录
        TOCReference result = new TOCReference(label, resource, fragmentId);
        //解析子级目录
        List<TOCReference> childTOCReferences = doToc(navpointElement, book);
        //readTOCReferences(
        //navpointElement.getChildNodes(), book);
        result.setChildren(childTOCReferences);
        return result;
    }

    /**
     * 获取目录节点的href
     *
     * @param navpointElement navpointElement
     * @return String
     */
    private static String readNavReference(Element navpointElement) {
        //https://www.w3.org/publishing/epub/epub-packages.html#sec-package-nav
        //父级节点必须是 "li"
        //Log.d(TAG, "readNavReference:" + navpointElement.getTagName());

        Element contentElement = DOMUtil
                .getFirstElementByTagNameNS(navpointElement, "", XHTMLTgs.a);
        if (contentElement == null) {
            return null;
        }
        String result = DOMUtil
                .getAttribute(contentElement, "", XHTMLAttributes.href);
        try {
            result = URLDecoder.decode(result, Constants.CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
        }

        return result;

    }

    /**
     * 获取目录节点里面的章节名
     *
     * @param navpointElement navpointElement
     * @return String
     */
    private static String readNavLabel(Element navpointElement) {
        //https://www.w3.org/publishing/epub/epub-packages.html#sec-package-nav
        //父级节点必须是 "li"
        //Log.d(TAG, "readNavLabel:" + navpointElement.getTagName());
        String label;
        Element labelElement = DOMUtil.getFirstElementByTagNameNS(navpointElement, "", "a");
        assert labelElement != null;
        label = labelElement.getTextContent();
        if (StringUtil.isNotBlank(label)) {
            return label;
        } else {
            labelElement = DOMUtil.getFirstElementByTagNameNS(navpointElement, "", "span");
        }
        assert labelElement != null;
        label = labelElement.getTextContent();
        //如果通过 a 标签无法获取章节列表,则是无href章节名
        return label;

    }

    public static Resource createNCXResource(EpubBook book)
            throws IllegalArgumentException, IllegalStateException, IOException {
        return createNCXResource(book.getMetadata().getIdentifiers(),
                book.getTitle(), book.getMetadata().getAuthors(),
                book.getTableOfContents());
    }

    public static Resource createNCXResource(List<Identifier> identifiers,
                                             String title, List<Author> authors, TableOfContents tableOfContents)
            throws IllegalArgumentException, IllegalStateException, IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        XmlSerializer out = EpubProcessorSupport.createXmlSerializer(data);
        write(out, identifiers, title, authors, tableOfContents);

        Resource resource = new Resource(NCX_ITEM_ID, data.toByteArray(),
                DEFAULT_NCX_HREF, V3_NCX_MEDIATYPE);
        resource.setProperties(V3_NCX_PROPERTIES);
        return resource;
    }

    /**
     * Generates a resource containing an xml document containing the table of contents of the book in ncx format.
     *
     * @param xmlSerializer the serializer used
     * @param book          the book to serialize
     * @throws IOException              IOException
     * @throws IllegalStateException    IllegalStateException
     * @throws IllegalArgumentException IllegalArgumentException
     */
    public static void write(XmlSerializer xmlSerializer, EpubBook book)
            throws IllegalArgumentException, IllegalStateException, IOException {
        write(xmlSerializer, book.getMetadata().getIdentifiers(), book.getTitle(),
                book.getMetadata().getAuthors(), book.getTableOfContents());
    }

    /**
     * 写入
     *
     * @param serializer      serializer
     * @param identifiers     identifiers
     * @param title           title
     * @param authors         authors
     * @param tableOfContents tableOfContents
     */
    @SuppressWarnings("unused")
    public static void write(XmlSerializer serializer,
                             List<Identifier> identifiers, String title, List<Author> authors,
                             TableOfContents tableOfContents) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startDocument(Constants.CHARACTER_ENCODING, false);
        serializer.setPrefix(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAMESPACE_XHTML);
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.html);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, XHTMLAttributes.xmlns_epub, NAMESPACE_EPUB);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, XHTMLAttributes.xml_lang, XHTMLAttributeValues.lang);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, XHTMLAttributes.lang, LANGUAGE);
        //写入头部head标签
        writeHead(title, serializer);
        //body开始
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.body);
        //h1开始
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.h1);
        serializer.text(title);
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.h1);
        //h1关闭
        //nav开始
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.nav);
        serializer.attribute("", XHTMLAttributes.epub_type, XHTMLAttributeValues.epub_type);
        serializer.attribute("", XHTMLAttributes.id, XHTMLAttributeValues.epub_type);
        serializer.attribute("", XHTMLAttributes.role, XHTMLAttributeValues.role_toc);
        //h2开始
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.h2);
        serializer.text("目录");
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.h2);


        writeNavPoints(tableOfContents.getTocReferences(), 1, serializer);


        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.nav);

        //body关闭
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.body);


        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.html);
        serializer.endDocument();

    }

    private static int writeNavPoints(List<TOCReference> tocReferences,
                                      int playOrder,
                                      XmlSerializer serializer) throws IOException {
        writeOlStart(serializer);
        for (TOCReference tocReference : tocReferences) {
            if (tocReference.getResource() == null) {
                playOrder = writeNavPoints(tocReference.getChildren(), playOrder,
                        serializer);
                continue;
            }


            writeNavPointStart(tocReference, serializer);

            playOrder++;
            if (!tocReference.getChildren().isEmpty()) {
                playOrder = writeNavPoints(tocReference.getChildren(), playOrder,
                        serializer);
            }

            writeNavPointEnd(tocReference, serializer);
        }
        writeOlSEnd(serializer);
        return playOrder;
    }

    private static void writeNavPointStart(TOCReference tocReference, XmlSerializer serializer) throws IOException {
        writeLiStart(serializer);
        String title = tocReference.getTitle();
        String href = tocReference.getCompleteHref();
        if (StringUtil.isNotBlank(href)) {
            writeLabel(title, href, serializer);
        } else {
            writeLabel(title, serializer);
        }
    }

    @SuppressWarnings("unused")
    private static void writeNavPointEnd(TOCReference tocReference,
                                         XmlSerializer serializer) throws IOException {
        writeLiEnd(serializer);
    }

    protected static void writeLabel(String title, String href, XmlSerializer serializer) throws IOException {
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.a);
        serializer.attribute("", XHTMLAttributes.href, href);
        //attribute必须在Text之前设置。
        serializer.text(title);
        //serializer.attribute(NAMESPACE_XHTML, XHTMLAttributes.href, href);
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.a);
    }

    protected static void writeLabel(String title, XmlSerializer serializer) throws IOException {
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.span);
        serializer.text(title);
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.span);
    }

    private static void writeLiStart(XmlSerializer serializer) throws IOException {
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.li);
        Log.d(TAG, "writeLiStart");
    }

    private static void writeLiEnd(XmlSerializer serializer) throws IOException {
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.li);
        Log.d(TAG, "writeLiEND");
    }

    private static void writeOlStart(XmlSerializer serializer) throws IOException {
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.ol);
        Log.d(TAG, "writeOlStart");
    }

    private static void writeOlSEnd(XmlSerializer serializer) throws IOException {
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.ol);
        Log.d(TAG, "writeOlEnd");
    }

    private static void writeHead(String title, XmlSerializer serializer) throws IOException {
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.head);
        //title
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.title);
        serializer.text(StringUtil.defaultIfNull(title));
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.title);
        //link
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.link);
        serializer.attribute("", XHTMLAttributes.rel, "stylesheet");
        serializer.attribute("", XHTMLAttributes.type, "text/css");
        serializer.attribute("", XHTMLAttributes.href, "css/style.css");
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.link);

        //meta
        serializer.startTag(NAMESPACE_XHTML, XHTMLTgs.meta);
        serializer.attribute("", XHTMLAttributes.http_equiv, XHTMLAttributeValues.Content_Type);
        serializer.attribute("", XHTMLAttributes.content, XHTMLAttributeValues.HTML_UTF8);
        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.meta);

        serializer.endTag(NAMESPACE_XHTML, XHTMLTgs.head);
    }


}
