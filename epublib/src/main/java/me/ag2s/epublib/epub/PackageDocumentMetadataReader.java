package me.ag2s.epublib.epub;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import me.ag2s.epublib.domain.Author;
import me.ag2s.epublib.domain.Date;
import me.ag2s.epublib.domain.Identifier;
import me.ag2s.epublib.domain.Metadata;
import me.ag2s.epublib.util.StringUtil;

/**
 * Reads the package document metadata.
 *
 * In its own separate class because the PackageDocumentReader became a bit large and unwieldy.
 *
 * @author paul
 */
// package
class PackageDocumentMetadataReader extends PackageDocumentBase {

  private static final String TAG= PackageDocumentMetadataReader.class.getName();

  public static Metadata readMetadata(Document packageDocument) {
    Metadata result = new Metadata();
    Element metadataElement = DOMUtil
        .getFirstElementByTagNameNS(packageDocument.getDocumentElement(),
            NAMESPACE_OPF, OPFTags.metadata);
    if (metadataElement == null) {
      Log.e(TAG,"Package does not contain element " + OPFTags.metadata);
      return result;
    }
    result.setTitles(DOMUtil
        .getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.title));
    result.setPublishers(DOMUtil
        .getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.publisher));
    result.setDescriptions(DOMUtil
        .getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.description));
    result.setRights(DOMUtil
        .getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.rights));
    result.setTypes(DOMUtil
        .getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.type));
    result.setSubjects(DOMUtil
        .getElementsTextChild(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.subject));
    result.setIdentifiers(readIdentifiers(metadataElement));
    result.setAuthors(readCreators(metadataElement));
    result.setContributors(readContributors(metadataElement));
    result.setDates(readDates(metadataElement));
    result.setOtherProperties(readOtherProperties(metadataElement));
    result.setMetaAttributes(readMetaProperties(metadataElement));
    Element languageTag = DOMUtil
        .getFirstElementByTagNameNS(metadataElement, NAMESPACE_DUBLIN_CORE,
            DCTags.language);
    if (languageTag != null) {
      result.setLanguage(DOMUtil.getTextChildrenContent(languageTag));
    }

    return result;
  }

  /**
   * consumes meta tags that have a property attribute as defined in the standard. For example:
   * &lt;meta property="rendition:layout"&gt;pre-paginated&lt;/meta&gt;
   * @param metadataElement metadataElement
   * @return Map<QName, String>
   */
  private static Map<QName, String> readOtherProperties(
      Element metadataElement) {
    Map<QName, String> result = new HashMap<>();

    NodeList metaTags = metadataElement.getElementsByTagName(OPFTags.meta);
    for (int i = 0; i < metaTags.getLength(); i++) {
      Node metaNode = metaTags.item(i);
      Node property = metaNode.getAttributes()
          .getNamedItem(OPFAttributes.property);
      if (property != null) {
        String name = property.getNodeValue();
        String value = metaNode.getTextContent();
        result.put(new QName(name), value);
      }
    }

    return result;
  }

  /**
   * consumes meta tags that have a property attribute as defined in the standard. For example:
   * &lt;meta property="rendition:layout"&gt;pre-paginated&lt;/meta&gt;
   * @param metadataElement metadataElement
   * @return Map<String, String>
   */
  private static Map<String, String> readMetaProperties(
      Element metadataElement) {
    Map<String, String> result = new HashMap<>();

    NodeList metaTags = metadataElement.getElementsByTagName(OPFTags.meta);
    for (int i = 0; i < metaTags.getLength(); i++) {
      Element metaElement = (Element) metaTags.item(i);
      String name = metaElement.getAttribute(OPFAttributes.name);
      String value = metaElement.getAttribute(OPFAttributes.content);
      result.put(name, value);
    }

    return result;
  }

  private static String getBookIdId(Document document) {
    Element packageElement = DOMUtil
        .getFirstElementByTagNameNS(document.getDocumentElement(),
            NAMESPACE_OPF, OPFTags.packageTag);
    if (packageElement == null) {
      return null;
    }
    return packageElement
        .getAttributeNS(NAMESPACE_OPF, OPFAttributes.uniqueIdentifier);
  }

  private static List<Author> readCreators(Element metadataElement) {
    return readAuthors(DCTags.creator, metadataElement);
  }

  private static List<Author> readContributors(Element metadataElement) {
    return readAuthors(DCTags.contributor, metadataElement);
  }

  private static List<Author> readAuthors(String authorTag,
      Element metadataElement) {
    NodeList elements = metadataElement
        .getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, authorTag);
    List<Author> result = new ArrayList<>(elements.getLength());
    for (int i = 0; i < elements.getLength(); i++) {
      Element authorElement = (Element) elements.item(i);
      Author author = createAuthor(authorElement);
      if (author != null) {
        result.add(author);
      }
    }
    return result;

  }

  private static List<Date> readDates(Element metadataElement) {
    NodeList elements = metadataElement
        .getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, DCTags.date);
    List<Date> result = new ArrayList<>(elements.getLength());
    for (int i = 0; i < elements.getLength(); i++) {
      Element dateElement = (Element) elements.item(i);
      Date date;
      try {
        date = new Date(DOMUtil.getTextChildrenContent(dateElement),
            dateElement.getAttributeNS(NAMESPACE_OPF, OPFAttributes.event));
        result.add(date);
      } catch (IllegalArgumentException e) {
        Log.e(TAG,e.getMessage());
      }
    }
    return result;

  }

  private static Author createAuthor(Element authorElement) {
    String authorString = DOMUtil.getTextChildrenContent(authorElement);
    if (StringUtil.isBlank(authorString)) {
      return null;
    }
    int spacePos = authorString.lastIndexOf(' ');
    Author result;
    if (spacePos < 0) {
      result = new Author(authorString);
    } else {
      result = new Author(authorString.substring(0, spacePos),
          authorString.substring(spacePos + 1));
    }
    result.setRole(
        authorElement.getAttributeNS(NAMESPACE_OPF, OPFAttributes.role));
    return result;
  }


  private static List<Identifier> readIdentifiers(Element metadataElement) {
    NodeList identifierElements = metadataElement
        .getElementsByTagNameNS(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
    if (identifierElements.getLength() == 0) {
      Log.e(TAG,"Package does not contain element " + DCTags.identifier);
      return new ArrayList<>();
    }
    String bookIdId = getBookIdId(metadataElement.getOwnerDocument());
    List<Identifier> result = new ArrayList<>(
            identifierElements.getLength());
    for (int i = 0; i < identifierElements.getLength(); i++) {
      Element identifierElement = (Element) identifierElements.item(i);
      String schemeName = identifierElement
          .getAttributeNS(NAMESPACE_OPF, DCAttributes.scheme);
      String identifierValue = DOMUtil
          .getTextChildrenContent(identifierElement);
      if (StringUtil.isBlank(identifierValue)) {
        continue;
      }
      Identifier identifier = new Identifier(schemeName, identifierValue);
      if (identifierElement.getAttribute("id").equals(bookIdId)) {
        identifier.setBookId(true);
      }
      result.add(identifier);
    }
    return result;
  }
}
