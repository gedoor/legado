package me.ag2s.epublib.epub;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.domain.EpubBook;
import me.ag2s.epublib.domain.Guide;
import me.ag2s.epublib.domain.GuideReference;
import me.ag2s.epublib.domain.MediaType;
import me.ag2s.epublib.domain.MediaTypes;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.domain.Resources;
import me.ag2s.epublib.domain.Spine;
import me.ag2s.epublib.domain.SpineReference;
import me.ag2s.epublib.util.ResourceUtil;
import me.ag2s.epublib.util.StringUtil;

/**
 * Reads the opf package document as defined by namespace http://www.idpf.org/2007/opf
 *
 * @author paul
 */
public class PackageDocumentReader extends PackageDocumentBase {

    private static final String TAG = PackageDocumentReader.class.getName();
    private static final String[] POSSIBLE_NCX_ITEM_IDS = new String[]{"toc",
            "ncx", "ncxtoc", "htmltoc"};
    private static final Pattern namespaceRegex = Pattern.compile(" s?mlns=\"");


    public static void read(
            Resource packageResource, EpubReader epubReader, EpubBook book,
            Resources resources)
            throws SAXException, IOException {
        /*掌上书苑有很多自制书OPF的nameSpace格式不标准，强制修复成正确的格式*/
        String string = namespaceRegex.matcher(new String(packageResource.getData()))
                .replaceAll(" xmlns=\"");
        packageResource.setData(string.getBytes());

        Document packageDocument = ResourceUtil.getAsDocument(packageResource);
        String packageHref = packageResource.getHref();

        URI packagePath;
        try {
            packagePath = new URI(packageHref);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        //resources = fixHrefs(packageHref, resources);
        readGuide(packageDocument, epubReader, book, resources);

        // Books sometimes use non-identifier ids. We map these here to legal ones
        Map<String, String> idMapping = new HashMap<>();
        String version = DOMUtil.getAttribute(packageDocument.getDocumentElement(), PREFIX_OPF, PackageDocumentBase.version);

        resources = readManifest(packageDocument, packageHref, packagePath, epubReader,
                resources, idMapping);
        book.setResources(resources);
        book.setVersion(version);
        readCover(packageDocument, packagePath, book);
        book.setMetadata(
                PackageDocumentMetadataReader.readMetadata(packageDocument));
        book.setSpine(readSpine(packageDocument, book.getResources(), idMapping));

        // if we did not find a cover page then we make the first page of the book the cover page
        if (book.getCoverPage() == null && book.getSpine().size() > 0) {
            book.setCoverPage(book.getSpine().getResource(0));
        }
    }

    /**
     * 修复一些非标准epub格式由于 opf 文件内容不全而读取不到图片的问题
     *
     * @return 修复图片路径后的一个Element列表
     * @author qianfanguojin
     */
    private static ArrayList<Element> ensureImageInfo(Resources resources,
                                                      Element manifestElement,
                                                      URI packagePath,
                                                      Document packageDocument) {
        ArrayList<Element> fixedElements = new ArrayList<>();
        HashSet<String> originItemHrefSet = new HashSet<>();
        //加入当前所有的 item 标签 并将 href 保存到集合中
        NodeList originItemElements = manifestElement
                .getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.item);
        for (int i = 0; i < originItemElements.getLength(); i++) {
            Element itemElement = (Element) originItemElements.item(i);
            fixedElements.add(itemElement);
            String href = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.href);
            try {
                href = URLDecoder.decode(packagePath.resolve(href).toString(), Constants.CHARACTER_ENCODING);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.getMessage());
            }
            originItemHrefSet.add(href);
        }

        //如果有图片资源未定义在 originItemElements ，则加入该图片信息得到 fixedElements 中
        for (Resource resource : resources.getAll()) {
            MediaType currentMediaType = resource.getMediaType();
            if (!MediaTypes.isImage(currentMediaType)) {
                continue;
            }
            String imageHref = resource.getHref();
            //确保该图片信息 resource 在原 originItemHrefSet 集合中没有出现过
            if (originItemHrefSet.contains(imageHref)) {
                continue;
            }
            Element itemEl = packageDocument.createElement("item");
            itemEl.setAttribute("id", resource.getId());
            try {
                imageHref = URLEncoder.encode(imageHref, Constants.CHARACTER_ENCODING);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.getMessage());
                continue;
            }
            itemEl.setAttribute("href", imageHref.replace("+", "%20"));
            itemEl.setAttribute("media-type", currentMediaType.getName());
            fixedElements.add(itemEl);
        }
        return fixedElements;
    }

    /**
     * Reads the manifest containing the resource ids, hrefs and mediatypes.
     *
     * @param packageDocument e
     * @param packageHref     e
     * @param epubReader      e
     * @param resources       e
     * @param idMapping       e
     * @return a Map with resources, with their id's as key.
     */
    @SuppressWarnings("unused")
    private static Resources readManifest(Document packageDocument,
                                          String packageHref,
                                          URI packagePath,
                                          EpubReader epubReader, Resources resources,
                                          Map<String, String> idMapping) {
        Element manifestElement = DOMUtil
                .getFirstElementByTagNameNS(packageDocument.getDocumentElement(),
                        NAMESPACE_OPF, OPFTags.manifest);
        Resources result = new Resources();
        if (manifestElement == null) {
            Log.e(TAG,
                    "Package document does not contain element " + OPFTags.manifest);
            return result;
        }
        List<Element> ensuredElements = ensureImageInfo(resources, manifestElement, packagePath, packageDocument);
        for (Element itemElement : ensuredElements) {
//            Element itemElement = ;
            String id = DOMUtil
                    .getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.id);
            String href = DOMUtil
                    .getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.href);

            try {
                href = URLDecoder.decode(packagePath.resolve(href).toString(), Constants.CHARACTER_ENCODING);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.getMessage());
            }
            String mediaTypeName = DOMUtil
                    .getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.media_type);
            Resource resource = resources.remove(href);
            if (resource == null) {
                Log.e(TAG, "resource with href '" + href + "' not found");
                continue;
            }
            resource.setId(id);
            //for epub3
            String properties = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.properties);
            resource.setProperties(properties);

            MediaType mediaType = MediaTypes.getMediaTypeByName(mediaTypeName);
            if (mediaType != null) {
                resource.setMediaType(mediaType);
            }
            result.add(resource);
            idMapping.put(id, resource.getId());
        }
        return result;
    }


    /**
     * Reads the book's guide.
     * Here some more attempts are made at finding the cover page.
     *
     * @param packageDocument r
     * @param epubReader      r
     * @param book            r
     * @param resources       g
     */
    @SuppressWarnings("unused")
    private static void readGuide(Document packageDocument,
                                  EpubReader epubReader, EpubBook book, Resources resources) {
        Element guideElement = DOMUtil
                .getFirstElementByTagNameNS(packageDocument.getDocumentElement(),
                        NAMESPACE_OPF, OPFTags.guide);
        if (guideElement == null) {
            return;
        }
        Guide guide = book.getGuide();
        NodeList guideReferences = guideElement
                .getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.reference);
        for (int i = 0; i < guideReferences.getLength(); i++) {
            Element referenceElement = (Element) guideReferences.item(i);
            String resourceHref = DOMUtil
                    .getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.href);
            if (StringUtil.isBlank(resourceHref)) {
                continue;
            }
            Resource resource = resources.getByHref(StringUtil
                    .substringBefore(resourceHref, Constants.FRAGMENT_SEPARATOR_CHAR));
            if (resource == null) {
                Log.e(TAG, "Guide is referencing resource with href " + resourceHref
                        + " which could not be found");
                continue;
            }
            String type = DOMUtil
                    .getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.type);
            if (StringUtil.isBlank(type)) {
                Log.e(TAG, "Guide is referencing resource with href " + resourceHref
                        + " which is missing the 'type' attribute");
                continue;
            }
            String title = DOMUtil
                    .getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.title);
            if (GuideReference.COVER.equalsIgnoreCase(type)) {
                continue; // cover is handled elsewhere
            }
            GuideReference reference = new GuideReference(resource, type, title,
                    StringUtil
                            .substringAfter(resourceHref, Constants.FRAGMENT_SEPARATOR_CHAR));
            guide.addReference(reference);
        }
    }


    /**
     * Strips off the package prefixes up to the href of the packageHref.
     * <p>
     * Example:
     * If the packageHref is "OEBPS/content.opf" then a resource href like "OEBPS/foo/bar.html" will be turned into "foo/bar.html"
     *
     * @param packageHref     f
     * @param resourcesByHref g
     * @return The stripped package href
     */
    static Resources fixHrefs(String packageHref,
                              Resources resourcesByHref) {
        int lastSlashPos = packageHref.lastIndexOf('/');
        if (lastSlashPos < 0) {
            return resourcesByHref;
        }
        Resources result = new Resources();
        for (Resource resource : resourcesByHref.getAll()) {
            if (StringUtil.isNotBlank(resource.getHref())
                    && resource.getHref().length() > lastSlashPos) {
                resource.setHref(resource.getHref().substring(lastSlashPos + 1));
            }
            result.add(resource);
        }
        return result;
    }

    /**
     * Reads the document's spine, containing all sections in reading order.
     *
     * @param packageDocument b
     * @param resources       b
     * @param idMapping       b
     * @return the document's spine, containing all sections in reading order.
     */
    private static Spine readSpine(Document packageDocument, Resources resources,
                                   Map<String, String> idMapping) {

        Element spineElement = DOMUtil
                .getFirstElementByTagNameNS(packageDocument.getDocumentElement(),
                        NAMESPACE_OPF, OPFTags.spine);
        if (spineElement == null) {
            Log.e(TAG, "Element " + OPFTags.spine
                    + " not found in package document, generating one automatically");
            return generateSpineFromResources(resources);
        }
        Spine result = new Spine();
        String tocResourceId = DOMUtil.getAttribute(spineElement, NAMESPACE_OPF, OPFAttributes.toc);
        Log.v(TAG, tocResourceId);
        result.setTocResource(findTableOfContentsResource(tocResourceId, resources));
        NodeList spineNodes = DOMUtil.getElementsByTagNameNS(packageDocument, NAMESPACE_OPF, OPFTags.itemref);
        if (spineNodes == null) {
            Log.e(TAG, "spineNodes is null");
            return result;
        }
        List<SpineReference> spineReferences = new ArrayList<>(spineNodes.getLength());
        for (int i = 0; i < spineNodes.getLength(); i++) {
            Element spineItem = (Element) spineNodes.item(i);
            String itemref = DOMUtil.getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.idref);
            if (StringUtil.isBlank(itemref)) {
                Log.e(TAG, "itemref with missing or empty idref"); // XXX
                continue;
            }
            String id = idMapping.get(itemref);
            if (id == null) {
                id = itemref;
            }

            Resource resource = resources.getByIdOrHref(id);
            if (resource == null) {
                Log.e(TAG, "resource with id '" + id + "' not found");
                continue;
            }

            SpineReference spineReference = new SpineReference(resource);
            if (OPFValues.no.equalsIgnoreCase(DOMUtil
                    .getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.linear))) {
                spineReference.setLinear(false);
            }
            spineReferences.add(spineReference);
        }
        result.setSpineReferences(spineReferences);
        return result;
    }

    /**
     * Creates a spine out of all resources in the resources.
     * The generated spine consists of all XHTML pages in order of their href.
     *
     * @param resources f
     * @return a spine created out of all resources in the resources.
     */
    private static Spine generateSpineFromResources(Resources resources) {
        Spine result = new Spine();
        List<String> resourceHrefs = new ArrayList<>(resources.getAllHrefs());
        Collections.sort(resourceHrefs, String.CASE_INSENSITIVE_ORDER);
        for (String resourceHref : resourceHrefs) {
            Resource resource = resources.getByHref(resourceHref);
            if (resource.getMediaType() == MediaTypes.NCX) {
                result.setTocResource(resource);
            } else if (resource.getMediaType() == MediaTypes.XHTML) {
                result.addSpineReference(new SpineReference(resource));
            }
        }
        return result;
    }


    /**
     * The spine tag should contain a 'toc' attribute with as value the resource id of the table of contents resource.
     * <p>
     * Here we try several ways of finding this table of contents resource.
     * We try the given attribute value, some often-used ones and finally look through all resources for the first resource with the table of contents mimetype.
     *
     * @param tocResourceId g
     * @param resources     g
     * @return the Resource containing the table of contents
     */
    static Resource findTableOfContentsResource(
            String tocResourceId,
            Resources resources
    ) {
        Resource tocResource;
        //一些epub3的文件为了兼容epub2,保留的epub2的目录文件，这里优先选择epub3的xml目录
        tocResource = resources.getByProperties("nav");
        if (tocResource != null) {
            return tocResource;
        }

        if (StringUtil.isNotBlank(tocResourceId)) {
            tocResource = resources.getByIdOrHref(tocResourceId);
        }

        if (tocResource != null) {
            return tocResource;
        }

        // get the first resource with the NCX mediatype
        tocResource = resources.findFirstResourceByMediaType(MediaTypes.NCX);

        if (tocResource == null) {
            for (String possibleNcxItemId : POSSIBLE_NCX_ITEM_IDS) {
                tocResource = resources.getByIdOrHref(possibleNcxItemId);
                if (tocResource != null) {
                    break;
                }
                tocResource = resources
                        .getByIdOrHref(possibleNcxItemId.toUpperCase());
                if (tocResource != null) {
                    break;
                }
            }
        }


        if (tocResource == null) {
            Log.e(TAG,
                    "Could not find table of contents resource. Tried resource with id '"
                            + tocResourceId + "', " + Constants.DEFAULT_TOC_ID + ", "
                            + Constants.DEFAULT_TOC_ID.toUpperCase()
                            + " and any NCX resource.");
        }
        return tocResource;
    }


    /**
     * Find all resources that have something to do with the coverpage and the cover image.
     * Search the meta tags and the guide references
     *
     * @param packageDocument s
     * @return all resources that have something to do with the coverpage and the cover image.
     */
    // package
    static Set<String> findCoverHrefs(Document packageDocument, URI packagePath) {

        Set<String> result = new HashSet<>();

        // try and find a meta tag with name = 'cover' and a non-blank id
        String coverResourceId = DOMUtil
                .getFindAttributeValue(packageDocument, NAMESPACE_OPF,
                        OPFTags.meta, OPFAttributes.name, OPFValues.meta_cover,
                        OPFAttributes.content);

        if (StringUtil.isNotBlank(coverResourceId)) {
            String coverHref = DOMUtil
                    .getFindAttributeValue(packageDocument, NAMESPACE_OPF,
                            OPFTags.item, OPFAttributes.id, coverResourceId,
                            OPFAttributes.href);
            if (StringUtil.isNotBlank(coverHref)) {
                result.add(packagePath.resolve(coverHref).toString());
            } else {
                String resolved = packagePath.resolve(coverResourceId).toString();
                result.add(
                        resolved); // maybe there was a cover href put in the cover id attribute
            }
        }
        // try and find a reference tag with type is 'cover' and reference is not blank
        String coverHref = DOMUtil
                .getFindAttributeValue(packageDocument, NAMESPACE_OPF,
                        OPFTags.reference, OPFAttributes.type, OPFValues.reference_cover,
                        OPFAttributes.href);
        if (StringUtil.isNotBlank(coverHref)) {
            result.add(packagePath.resolve(coverHref).toString());
        }
        return result;
    }

    /**
     * Finds the cover resource in the packageDocument and adds it to the book if found.
     * Keeps the cover resource in the resources map
     *
     * @param packageDocument s
     * @param book            x
     */
    private static void readCover(Document packageDocument, URI packagePath, EpubBook book) {

        Collection<String> coverHrefs = findCoverHrefs(packageDocument, packagePath);
        for (String coverHref : coverHrefs) {
            Resource resource = book.getResources().getByHref(coverHref);
            if (resource == null) {
                Log.e(TAG, "Cover resource " + coverHref + " not found");
                continue;
            }
            if (resource.getMediaType() == MediaTypes.XHTML) {
                book.setCoverPage(resource);
            } else if (MediaTypes.isBitmapImage(resource.getMediaType())) {
                book.setCoverImage(resource);
            }
        }
    }


}
