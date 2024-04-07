package me.ag2s.epublib.domain;

import android.util.Base64;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.util.StringUtil;

/**
 * All the resources that make up the book.
 * XHTML files, images and epub xml documents must be here.
 *
 * @author paul
 */
public class Resources implements Serializable {

    private static final long serialVersionUID = 2450876953383871451L;
    private static final String IMAGE_PREFIX = "image_";
    private static final String ITEM_PREFIX = "item_";
    private static final Pattern dataUriRegex = Pattern.compile("data:([\\w/\\-\\.]+);base64,(.*)");
    private int lastId = 1;

    private Map<String, Resource> resources = new HashMap<>();

    private final Map<String, Resource> resourcesById = new HashMap<>();

    /**
     * Adds a resource to the resources.
     * <p>
     * Fixes the resources id and href if necessary.
     *
     * @param resource resource
     * @return the newly added resource
     */
    public Resource add(Resource resource) {
        fixResourceHref(resource);
        fixResourceId(resource);
        this.resources.put(resource.getHref(), resource);
        resourcesById.put(resource.getId(), resource);
        return resource;
    }

    /**
     * Checks the id of the given resource and changes to a unique identifier if it isn't one already.
     *
     * @param resource resource
     */
    public void fixResourceId(Resource resource) {
        String resourceId = resource.getId();

        // first try and create a unique id based on the resource's href
        if (StringUtil.isBlank(resource.getId())) {
            resourceId = StringUtil.substringBeforeLast(resource.getHref(), '.');
            resourceId = StringUtil.substringAfterLast(resourceId, '/');
        }

        resourceId = makeValidId(resourceId, resource);

        // check if the id is unique. if not: create one from scratch
        if (StringUtil.isBlank(resourceId) || containsId(resourceId)) {
            resourceId = createUniqueResourceId(resource);
        }
        resource.setId(resourceId);
    }

    /**
     * Check if the id is a valid identifier. if not: prepend with valid identifier
     *
     * @param resource resource
     * @return a valid id
     */
    private String makeValidId(String resourceId, Resource resource) {
        if (StringUtil.isNotBlank(resourceId) && !Character
                .isJavaIdentifierStart(resourceId.charAt(0))) {
            resourceId = getResourceItemPrefix(resource) + resourceId;
        }
        return resourceId;
    }

    private String getResourceItemPrefix(Resource resource) {
        String result;
        if (MediaTypes.isBitmapImage(resource.getMediaType())) {
            result = IMAGE_PREFIX;
        } else {
            result = ITEM_PREFIX;
        }
        return result;
    }

    /**
     * Creates a new resource id that is guaranteed to be unique for this set of Resources
     *
     * @param resource resource
     * @return a new resource id that is guaranteed to be unique for this set of Resources
     */
    private String createUniqueResourceId(Resource resource) {
        int counter = lastId;
        if (counter == Integer.MAX_VALUE) {
            if (resources.size() == Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Resources contains " + Integer.MAX_VALUE
                                + " elements: no new elements can be added");
            } else {
                counter = 1;
            }
        }
        String prefix = getResourceItemPrefix(resource);
        String result = prefix + counter;
        while (containsId(result)) {
            result = prefix + (++counter);
        }
        lastId = counter;
        return result;
    }

    /**
     * Whether the map of resources already contains a resource with the given id.
     *
     * @param id id
     * @return Whether the map of resources already contains a resource with the given id.
     */
    public boolean containsId(String id) {
        if (StringUtil.isBlank(id)) {
            return false;
        }
        return resourcesById.containsKey(id);
    }

    /**
     * Gets the resource with the given id.
     *
     * @param id id
     * @return null if not found
     */
    public Resource getById(String id) {
        if (StringUtil.isBlank(id)) {
            return null;
        }
        return resourcesById.get(id);
    }

    public Resource getByProperties(String properties) {
        if (StringUtil.isBlank(properties)) {
            return null;
        }
        for (Resource resource : resources.values()) {
            if (properties.equals(resource.getProperties())) {
                return resource;
            }
        }
        return null;
    }

    /**
     * Remove the resource with the given href.
     *
     * @param href href
     * @return the removed resource, null if not found
     */
    public Resource remove(String href) {
        return resources.remove(href);
    }

    private void fixResourceHref(Resource resource) {
        if (StringUtil.isNotBlank(resource.getHref())
                && !resources.containsKey(resource.getHref())) {
            return;
        }
        if (StringUtil.isBlank(resource.getHref())) {
            if (resource.getMediaType() == null) {
                throw new IllegalArgumentException(
                        "Resource must have either a MediaType or a href");
            }
            int i = 1;
            String href = createHref(resource.getMediaType(), i);
            while (resources.containsKey(href)) {
                href = createHref(resource.getMediaType(), (++i));
            }
            resource.setHref(href);
        }
    }

    private String createHref(MediaType mediaType, int counter) {
        if (MediaTypes.isBitmapImage(mediaType)) {
            return IMAGE_PREFIX + counter + mediaType.getDefaultExtension();
        } else {
            return ITEM_PREFIX + counter + mediaType.getDefaultExtension();
        }
    }


    public boolean isEmpty() {
        return resources.isEmpty();
    }

    /**
     * The number of resources
     *
     * @return The number of resources
     */
    public int size() {
        return resources.size();
    }

    /**
     * The resources that make up this book.
     * Resources can be xhtml pages, images, xml documents, etc.
     *
     * @return The resources that make up this book.
     */
    @SuppressWarnings("unused")
    public Map<String, Resource> getResourceMap() {
        return resources;
    }

    public Collection<Resource> getAll() {
        return resources.values();
    }


    /**
     * Whether there exists a resource with the given href
     *
     * @param href href
     * @return Whether there exists a resource with the given href
     */
    public boolean notContainsByHref(String href) {
        if (StringUtil.isBlank(href)) {
            return true;
        } else {
            return !resources.containsKey(
                    StringUtil.substringBefore(href, Constants.FRAGMENT_SEPARATOR_CHAR));
        }
    }

    /**
     * Whether there exists a resource with the given href
     *
     * @param href href
     * @return Whether there exists a resource with the given href
     */
    @SuppressWarnings("unused")
    public boolean containsByHref(String href) {
        return !notContainsByHref(href);
    }

    /**
     * Sets the collection of Resources to the given collection of resources
     *
     * @param resources resources
     */
    public void set(Collection<Resource> resources) {
        this.resources.clear();
        resourcesById.clear();
        addAll(resources);
    }

    /**
     * Adds all resources from the given Collection of resources to the existing collection.
     *
     * @param resources resources
     */
    public void addAll(Collection<Resource> resources) {
        for (Resource resource : resources) {
            add(resource);
        }
    }

    /**
     * Sets the collection of Resources to the given collection of resources
     *
     * @param resources A map with as keys the resources href and as values the Resources
     */
    public void set(Map<String, Resource> resources) {
        this.resources = new HashMap<>(resources);
        resourcesById.clear();
        for (Resource resource : resources.values()) {
            resourcesById.put(resource.getId(), resource);
        }
    }


    /**
     * First tries to find a resource with as id the given idOrHref, if that
     * fails it tries to find one with the idOrHref as href.
     *
     * @param idOrHref idOrHref
     * @return the found Resource
     */
    public Resource getByIdOrHref(String idOrHref) {
        Resource resource = getById(idOrHref);
        if (resource == null) {
            resource = getByHref(idOrHref);
        }
        return resource;
    }


    /**
     * Gets the resource with the given href.
     * If the given href contains a fragmentId then that fragment id will be ignored.
     *
     * @param href href
     * @return null if not found.
     */
    public Resource getByHref(String href) {
        if (StringUtil.isBlank(href)) {
            return null;
        }
        href = StringUtil.substringBefore(href, Constants.FRAGMENT_SEPARATOR_CHAR);

        if (!StringUtil.startsWithIgnoreCase(href, "data")) {
            return resources.get(href);
        }

        Matcher dataUriMatcher = dataUriRegex.matcher(href);
        if (dataUriMatcher.find()) {
            String dataUriMediaTypeString = dataUriMatcher.group(1);
            MediaType dataUriMediaType = new MediaType(dataUriMediaTypeString, "." + StringUtil.substringAfterLast(dataUriMediaTypeString, '/'));
            byte[] dataUriData = Base64.decode(dataUriMatcher.group(2), Base64.DEFAULT);
            return new Resource(dataUriData, dataUriMediaType);
        } else {
            return resources.get(href);
        }
    }

    /**
     * Gets the first resource (random order) with the give mediatype.
     * <p>
     * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
     *
     * @param mediaType mediaType
     * @return the first resource (random order) with the give mediatype.
     */
    public Resource findFirstResourceByMediaType(MediaType mediaType) {
        return findFirstResourceByMediaType(resources.values(), mediaType);
    }

    /**
     * Gets the first resource (random order) with the give mediatype.
     * <p>
     * Useful for looking up the table of contents as it's supposed to be the only resource with NCX mediatype.
     *
     * @param mediaType mediaType
     * @return the first resource (random order) with the give mediatype.
     */
    public static Resource findFirstResourceByMediaType(
            Collection<Resource> resources, MediaType mediaType) {
        for (Resource resource : resources) {
            if (resource.getMediaType() == mediaType) {
                return resource;
            }
        }
        return null;
    }

    /**
     * All resources that have the given MediaType.
     *
     * @param mediaType mediaType
     * @return All resources that have the given MediaType.
     */
    public List<Resource> getResourcesByMediaType(MediaType mediaType) {
        List<Resource> result = new ArrayList<>();
        if (mediaType == null) {
            return result;
        }
        for (Resource resource : getAll()) {
            if (resource.getMediaType() == mediaType) {
                result.add(resource);
            }
        }
        return result;
    }

    /**
     * All Resources that match any of the given list of MediaTypes
     *
     * @param mediaTypes mediaType
     * @return All Resources that match any of the given list of MediaTypes
     */
    @SuppressWarnings("unused")
    public List<Resource> getResourcesByMediaTypes(MediaType[] mediaTypes) {
        List<Resource> result = new ArrayList<>();
        if (mediaTypes == null) {
            return result;
        }

        // this is the fastest way of doing this according to
        // http://stackoverflow.com/questions/1128723/in-java-how-can-i-test-if-an-array-contains-a-certain-value
        List<MediaType> mediaTypesList = Arrays.asList(mediaTypes);
        for (Resource resource : getAll()) {
            if (mediaTypesList.contains(resource.getMediaType())) {
                result.add(resource);
            }
        }
        return result;
    }


    /**
     * All resource hrefs
     *
     * @return all resource hrefs
     */
    public Collection<String> getAllHrefs() {
        return resources.keySet();
    }
}
