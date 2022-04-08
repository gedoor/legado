package me.ag2s.epublib.util;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.domain.MediaType;
import me.ag2s.epublib.domain.MediaTypes;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.epub.EpubProcessorSupport;

/**
 * Various resource utility methods
 *
 * @author paul
 */
public class ResourceUtil {
    /**
     * 快速创建HTML类型的Resource
     *
     * @param title 章节的标题
     * @param txt   章节的正文
     * @param model html模板
     * @return 返回Resource
     */
    public static Resource createChapterResource(String title, String txt, String model, String href) {
        if (title.contains("\n")) {
            title = "<span class=\"chapter-sequence-number\">" + title.replaceFirst("\\s*\\n\\s*", "</span><br />");
        } else {
            title = title.replaceFirst("\\s+", "</span><br />");
            if (title.contains("</span>"))
                title = "<span class=\"chapter-sequence-number\">" + title;
        }
        String html = model.replace("{title}", title)
                .replace("{content}", StringUtil.formatHtml(txt));
        return new Resource(html.getBytes(), href);
    }

    public static Resource createPublicResource(String name, String author, String intro, String kind, String wordCount, String model, String href) {
        String html = model.replace("{name}", name)
                .replace("{author}", author)
                .replace("{kind}", kind == null ? "" : kind)
                .replace("{wordCount}", wordCount == null ? "" : wordCount)
                .replace("{intro}", StringUtil.formatHtml(intro == null ? "" : intro));
        return new Resource(html.getBytes(), href);
    }

    /**
     * 快速从File创建Resource
     *
     * @param file File
     * @return Resource
     * @throws IOException IOException
     */

    @SuppressWarnings("unused")
    public static Resource createResource(File file) throws IOException {
        if (file == null) {
            return null;
        }
        MediaType mediaType = MediaTypes.determineMediaType(file.getName());
        byte[] data = IOUtil.toByteArray(new FileInputStream(file));
        return new Resource(data, mediaType);
    }


    /**
     * 创建一个只带标题的HTMl类型的Resource,常用于封面页，大卷页
     *
     * @param title v
     * @param href  v
     * @return a resource with as contents a html page with the given title.
     */
    @SuppressWarnings("unused")
    public static Resource createResource(String title, String href) {
        String content =
                "<html><head><title>" + title + "</title></head><body><h1>" + title
                        + "</h1></body></html>";
        return new Resource(null, content.getBytes(), href, MediaTypes.XHTML,
                Constants.CHARACTER_ENCODING);
    }

    /**
     * Creates a resource out of the given zipEntry and zipInputStream.
     *
     * @param zipEntry       v
     * @param zipInputStream v
     * @return a resource created out of the given zipEntry and zipInputStream.
     * @throws IOException v
     */
    public static Resource createResource(ZipEntry zipEntry,
                                          ZipInputStream zipInputStream) throws IOException {
        return new Resource(zipInputStream, zipEntry.getName());

    }

    public static Resource createResource(ZipEntry zipEntry,
                                          InputStream zipInputStream) throws IOException {
        return new Resource(zipInputStream, zipEntry.getName());

    }

    /**
     * Converts a given string from given input character encoding to the requested output character encoding.
     *
     * @param inputEncoding  v
     * @param outputEncoding v
     * @param input          v
     * @return the string from given input character encoding converted to the requested output character encoding.
     * @throws UnsupportedEncodingException v
     */
    @SuppressWarnings("unused")
    public static byte[] recode(String inputEncoding, String outputEncoding,
                                byte[] input) throws UnsupportedEncodingException {
        return new String(input, inputEncoding).getBytes(outputEncoding);
    }

    /**
     * Gets the contents of the Resource as an InputSource in a null-safe manner.
     */
    @SuppressWarnings("unused")
    public static InputSource getInputSource(Resource resource)
            throws IOException {
        if (resource == null) {
            return null;
        }
        Reader reader = resource.getReader();
        if (reader == null) {
            return null;
        }
        return new InputSource(reader);
    }


    /**
     * Reads parses the xml therein and returns the result as a Document
     */
    public static Document getAsDocument(Resource resource)
            throws SAXException, IOException {
        return getAsDocument(resource,
                EpubProcessorSupport.createDocumentBuilder());
    }

    /**
     * Reads the given resources inputstream, parses the xml therein and returns the result as a Document
     *
     * @param resource        v
     * @param documentBuilder v
     * @return the document created from the given resource
     * @throws UnsupportedEncodingException v
     * @throws SAXException                 v
     * @throws IOException                  v
     */
    public static Document getAsDocument(Resource resource,
                                         DocumentBuilder documentBuilder)
            throws UnsupportedEncodingException, SAXException, IOException {
        InputSource inputSource = getInputSource(resource);
        if (inputSource == null) {
            return null;
        }
        return documentBuilder.parse(inputSource);
    }
}
