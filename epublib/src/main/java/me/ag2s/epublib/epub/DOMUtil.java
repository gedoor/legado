package me.ag2s.epublib.epub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import me.ag2s.epublib.util.StringUtil;

/**
 * Utility methods for working with the DOM.
 *
 * @author paul
 */
// package
class DOMUtil {

    /**
     * First tries to get the attribute value by doing an getAttributeNS on the element, if that gets an empty element it does a getAttribute without namespace.
     *
     * @param element   element
     * @param namespace namespace
     * @param attribute attribute
     * @return String Attribute
     */
    public static String getAttribute(Element element, String namespace,
                                      String attribute) {
        String result = element.getAttributeNS(namespace, attribute);
        if (StringUtil.isEmpty(result)) {
            result = element.getAttribute(attribute);
        }
        return result;
    }

    /**
     * Gets all descendant elements of the given parentElement with the given namespace and tagname and returns their text child as a list of String.
     *
     * @param parentElement parentElement
     * @param namespace     namespace
     * @param tagName       tagName
     * @return List<String>
     */
    public static List<String> getElementsTextChild(Element parentElement,
                                                    String namespace, String tagName) {
        NodeList elements = parentElement
                .getElementsByTagNameNS(namespace, tagName);
        //ArrayList 初始化时指定长度提高性能
        List<String> result = new ArrayList<>(elements.getLength());
        for (int i = 0; i < elements.getLength(); i++) {
            result.add(getTextChildrenContent((Element) elements.item(i)));
        }
        return result;
    }

    /**
     * Finds in the current document the first element with the given namespace and elementName and with the given findAttributeName and findAttributeValue.
     * It then returns the value of the given resultAttributeName.
     *
     * @param document            document
     * @param namespace           namespace
     * @param elementName         elementName
     * @param findAttributeName   findAttributeName
     * @param findAttributeValue  findAttributeValue
     * @param resultAttributeName resultAttributeName
     * @return String value
     */
    public static String getFindAttributeValue(Document document,
                                               String namespace, String elementName, String findAttributeName,
                                               String findAttributeValue, String resultAttributeName) {
        NodeList metaTags = document.getElementsByTagNameNS(namespace, elementName);
        for (int i = 0; i < metaTags.getLength(); i++) {
            Element metaElement = (Element) metaTags.item(i);
            if (findAttributeValue
                    .equalsIgnoreCase(metaElement.getAttribute(findAttributeName))
                    && StringUtil
                    .isNotBlank(metaElement.getAttribute(resultAttributeName))) {
                return metaElement.getAttribute(resultAttributeName);
            }
        }
        return null;
    }

    /**
     * Gets the first element that is a child of the parentElement and has the given namespace and tagName
     *
     * @param parentElement parentElement
     * @param namespace     namespace
     * @param tagName       tagName
     * @return Element
     */
    public static NodeList getElementsByTagNameNS(Element parentElement,
                                                  String namespace, String tagName) {
        NodeList nodes = parentElement.getElementsByTagNameNS(namespace, tagName);
        if (nodes.getLength() != 0) {
            return nodes;
        }
        nodes = parentElement.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes;
    }
    /**
     * Gets the first element that is a child of the parentElement and has the given namespace and tagName
     *
     * @param parentElement parentElement
     * @param namespace     namespace
     * @param tagName       tagName
     * @return Element
     */
    public static NodeList getElementsByTagNameNS(Document parentElement,
                                                  String namespace, String tagName) {
        NodeList nodes = parentElement.getElementsByTagNameNS(namespace, tagName);
        if (nodes.getLength() != 0) {
            return nodes;
        }
        nodes = parentElement.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes;
    }

    /**
     * Gets the first element that is a child of the parentElement and has the given namespace and tagName
     *
     * @param parentElement parentElement
     * @param namespace     namespace
     * @param tagName       tagName
     * @return Element
     */
    public static Element getFirstElementByTagNameNS(Element parentElement,
                                                     String namespace, String tagName) {
        NodeList nodes = parentElement.getElementsByTagNameNS(namespace, tagName);
        if (nodes.getLength() != 0) {
            return (Element) nodes.item(0);
        }
        nodes = parentElement.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    /**
     * The contents of all Text nodes that are children of the given parentElement.
     * The result is trim()-ed.
     * <p>
     * The reason for this more complicated procedure instead of just returning the data of the firstChild is that
     * when the text is Chinese characters then on Android each Characater is represented in the DOM as
     * an individual Text node.
     *
     * @param parentElement parentElement
     * @return String value
     */
    public static String getTextChildrenContent(Element parentElement) {
        if (parentElement == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        NodeList childNodes = parentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if ((node == null) ||
                    (node.getNodeType() != Node.TEXT_NODE)) {
                continue;
            }
            result.append(((Text) node).getData());
        }
        return result.toString().trim();
    }

}
