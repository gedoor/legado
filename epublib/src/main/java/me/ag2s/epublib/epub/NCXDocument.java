package me.ag2s.epublib.epub;

import android.util.Log;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.domain.Author;
import me.ag2s.epublib.domain.Book;
import me.ag2s.epublib.domain.Identifier;
import me.ag2s.epublib.domain.MediaTypes;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.domain.TOCReference;
import me.ag2s.epublib.domain.TableOfContents;
import me.ag2s.epublib.util.ResourceUtil;
import me.ag2s.epublib.util.StringUtil;
//import io.documentnode.minilog.Logger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
//import javax.xml.stream.FactoryConfigurationError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

/**
 * Writes the ncx document as defined by namespace http://www.daisy.org/z3986/2005/ncx/
 *
 * @author paul
 */
public class NCXDocument {

    public static final String NAMESPACE_NCX = "http://www.daisy.org/z3986/2005/ncx/";
    public static final String PREFIX_NCX = "ncx";
    public static final String NCX_ITEM_ID = "ncx";
    public static final String DEFAULT_NCX_HREF = "toc.ncx";
    public static final String PREFIX_DTB = "dtb";

    private static String TAG = NCXDocument.class.getName();

    private interface NCXTags {

        String ncx = "ncx";
        String meta = "meta";
        String navPoint = "navPoint";
        String navMap = "navMap";
        String navLabel = "navLabel";
        String content = "content";
        String text = "text";
        String docTitle = "docTitle";
        String docAuthor = "docAuthor";
        String head = "head";
    }

    private interface NCXAttributes {

        String src = "src";
        String name = "name";
        String content = "content";
        String id = "id";
        String playOrder = "playOrder";
        String clazz = "class";
        String version = "version";
    }

    private interface NCXAttributeValues {

        String chapter = "chapter";
        String version = "2005-1";

    }

    public static Resource read(Book book, EpubReader epubReader) {
        Log.d(TAG, book.getVersion());
        String version = book.getVersion();
        if (version.startsWith("2.")) {
            return NCXDocumentV2.read(book, epubReader);
        } else if (version.startsWith("3.")) {
            return NCXDocumentV3.read(book, epubReader);
        } else {
            return NCXDocumentV2.read(book, epubReader);
        }

    }

    private static List<TOCReference> readTOCReferences(NodeList navpoints,
                                                        Book book) {
        Log.d(TAG, book.getVersion());
        String version = book.getVersion();
        if (version.startsWith("2.")) {
            return NCXDocumentV2.readTOCReferences(navpoints,book);
        } else if (version.startsWith("3.")) {
            return NCXDocumentV3.readTOCReferences(navpoints,book);
        } else {
            return NCXDocumentV2.readTOCReferences(navpoints,book);
        }

    }

    static TOCReference readTOCReference(Element navpointElement, Book book) {
        Log.d(TAG, book.getVersion());
        String version = book.getVersion();
        if (version.startsWith("2.")) {
            return NCXDocumentV2.readTOCReference(navpointElement,book);
        } else if (version.startsWith("3.")) {
            return NCXDocumentV3.readTOCReference(navpointElement,book);
        } else {
            return NCXDocumentV2.readTOCReference(navpointElement,book);
        }

    }

    private static String readNavReference(Element navpointElement) {
        Element contentElement = DOMUtil
                .getFirstElementByTagNameNS(navpointElement, NAMESPACE_NCX,
                        NCXTags.content);
        String result = DOMUtil
                .getAttribute(contentElement, NAMESPACE_NCX, NCXAttributes.src);
        try {
            result = URLDecoder.decode(result, Constants.CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    private static String readNavLabel(Element navpointElement) {
        Element navLabel = DOMUtil
                .getFirstElementByTagNameNS(navpointElement, NAMESPACE_NCX,
                        NCXTags.navLabel);
        return DOMUtil.getTextChildrenContent(DOMUtil
                .getFirstElementByTagNameNS(navLabel, NAMESPACE_NCX, NCXTags.text));
    }


    public static void write(EpubWriter epubWriter, Book book,
                             ZipOutputStream resultStream) throws IOException {
        resultStream
                .putNextEntry(new ZipEntry(book.getSpine().getTocResource().getHref()));
        XmlSerializer out = EpubProcessorSupport.createXmlSerializer(resultStream);
        write(out, book);
        out.flush();
    }


    /**
     * Generates a resource containing an xml document containing the table of contents of the book in ncx format.
     *
     * @param xmlSerializer the serializer used
     * @param book          the book to serialize
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @1throws FactoryConfigurationError
     */
    public static void write(XmlSerializer xmlSerializer, Book book)
            throws IllegalArgumentException, IllegalStateException, IOException {
        write(xmlSerializer, book.getMetadata().getIdentifiers(), book.getTitle(),
                book.getMetadata().getAuthors(), book.getTableOfContents());
    }

    public static Resource createNCXResource(Book book)
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
                DEFAULT_NCX_HREF, MediaTypes.NCX);
        return resource;
    }

    public static void write(XmlSerializer serializer,
                             List<Identifier> identifiers, String title, List<Author> authors,
                             TableOfContents tableOfContents)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startDocument(Constants.CHARACTER_ENCODING, false);
        serializer.setPrefix(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAMESPACE_NCX);
        serializer.startTag(NAMESPACE_NCX, NCXTags.ncx);
//		serializer.writeNamespace("ncx", NAMESPACE_NCX);
//		serializer.attribute("xmlns", NAMESPACE_NCX);
        serializer
                .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.version,
                        NCXAttributeValues.version);
        serializer.startTag(NAMESPACE_NCX, NCXTags.head);

        for (Identifier identifier : identifiers) {
            writeMetaElement(identifier.getScheme(), identifier.getValue(),
                    serializer);
        }

        writeMetaElement("generator", Constants.EPUB4J_GENERATOR_NAME, serializer);
        writeMetaElement("depth", String.valueOf(tableOfContents.calculateDepth()),
                serializer);
        writeMetaElement("totalPageCount", "0", serializer);
        writeMetaElement("maxPageNumber", "0", serializer);

        serializer.endTag(NAMESPACE_NCX, "head");

        serializer.startTag(NAMESPACE_NCX, NCXTags.docTitle);
        serializer.startTag(NAMESPACE_NCX, NCXTags.text);
        // write the first title
        serializer.text(StringUtil.defaultIfNull(title));
        serializer.endTag(NAMESPACE_NCX, NCXTags.text);
        serializer.endTag(NAMESPACE_NCX, NCXTags.docTitle);

        for (Author author : authors) {
            serializer.startTag(NAMESPACE_NCX, NCXTags.docAuthor);
            serializer.startTag(NAMESPACE_NCX, NCXTags.text);
            serializer.text(author.getLastname() + ", " + author.getFirstname());
            serializer.endTag(NAMESPACE_NCX, NCXTags.text);
            serializer.endTag(NAMESPACE_NCX, NCXTags.docAuthor);
        }

        serializer.startTag(NAMESPACE_NCX, NCXTags.navMap);
        writeNavPoints(tableOfContents.getTocReferences(), 1, serializer);
        serializer.endTag(NAMESPACE_NCX, NCXTags.navMap);

        serializer.endTag(NAMESPACE_NCX, "ncx");
        serializer.endDocument();
    }


    private static void writeMetaElement(String dtbName, String content,
                                         XmlSerializer serializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(NAMESPACE_NCX, NCXTags.meta);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.name,
                PREFIX_DTB + ":" + dtbName);
        serializer
                .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.content,
                        content);
        serializer.endTag(NAMESPACE_NCX, NCXTags.meta);
    }

    private static int writeNavPoints(List<TOCReference> tocReferences,
                                      int playOrder,
                                      XmlSerializer serializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        for (TOCReference tocReference : tocReferences) {
            if (tocReference.getResource() == null) {
                playOrder = writeNavPoints(tocReference.getChildren(), playOrder,
                        serializer);
                continue;
            }
            writeNavPointStart(tocReference, playOrder, serializer);
            playOrder++;
            if (!tocReference.getChildren().isEmpty()) {
                playOrder = writeNavPoints(tocReference.getChildren(), playOrder,
                        serializer);
            }
            writeNavPointEnd(tocReference, serializer);
        }
        return playOrder;
    }


    private static void writeNavPointStart(TOCReference tocReference,
                                           int playOrder, XmlSerializer serializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(NAMESPACE_NCX, NCXTags.navPoint);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.id,
                "navPoint-" + playOrder);
        serializer
                .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.playOrder,
                        String.valueOf(playOrder));
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.clazz,
                NCXAttributeValues.chapter);
        serializer.startTag(NAMESPACE_NCX, NCXTags.navLabel);
        serializer.startTag(NAMESPACE_NCX, NCXTags.text);
        serializer.text(tocReference.getTitle());
        serializer.endTag(NAMESPACE_NCX, NCXTags.text);
        serializer.endTag(NAMESPACE_NCX, NCXTags.navLabel);
        serializer.startTag(NAMESPACE_NCX, NCXTags.content);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NCXAttributes.src,
                tocReference.getCompleteHref());
        serializer.endTag(NAMESPACE_NCX, NCXTags.content);
    }

    private static void writeNavPointEnd(TOCReference tocReference,
                                         XmlSerializer serializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.endTag(NAMESPACE_NCX, NCXTags.navPoint);
    }
}
