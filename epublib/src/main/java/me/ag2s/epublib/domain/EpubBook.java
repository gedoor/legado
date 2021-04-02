package me.ag2s.epublib.domain;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a Book.
 * <p>
 * All resources of a Book (html, css, xml, fonts, images) are represented
 * as Resources. See getResources() for access to these.<br/>
 * A Book as 3 indexes into these Resources, as per the epub specification.<br/>
 * <dl>
 * <dt>Spine</dt>
 * <dd>these are the Resources to be shown when a user reads the book from
 * start to finish.</dd>
 * <dt>Table of Contents<dt>
 * <dd>The table of contents. Table of Contents references may be in a
 * different order and contain different Resources than the spine, and often do.
 * <dt>Guide</dt>
 * <dd>The Guide has references to a set of special Resources like the
 * cover page, the Glossary, the copyright page, etc.
 * </dl>
 * <p/>
 * The complication is that these 3 indexes may and usually do point to
 * different pages.
 * A chapter may be split up in 2 pieces to fit it in to memory. Then the
 * spine will contain both pieces, but the Table of Contents only the first.
 * <p>
 * The Content page may be in the Table of Contents, the Guide, but not
 * in the Spine.
 * Etc.
 * <p/>
 * <p>
 * Please see the illustration at: doc/schema.svg
 *
 * @author paul
 * @author jake
 */
public class EpubBook implements Serializable {

    private static final long serialVersionUID = 2068355170895770100L;

    private Resources resources = new Resources();
    private Metadata metadata = new Metadata();
    private Spine spine = new Spine();
    private TableOfContents tableOfContents = new TableOfContents();
    private final Guide guide = new Guide();
    private Resource opfResource;
    private Resource ncxResource;
    private Resource coverImage;


    private String version = "2.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEpub3() {
        return this.version.startsWith("3.");
    }

    @SuppressWarnings("UnusedReturnValue")
    public TOCReference addSection(
            TOCReference parentSection, String sectionTitle, Resource resource) {
        return addSection(parentSection, sectionTitle, resource, null);
    }

    /**
     * Adds the resource to the table of contents of the book as a child
     * section of the given parentSection
     *
     * @param parentSection parentSection
     * @param sectionTitle  sectionTitle
     * @param resource      resource
     * @param fragmentId    fragmentId
     * @return The table of contents
     */
    public TOCReference addSection(
            TOCReference parentSection, String sectionTitle, Resource resource,
            String fragmentId) {
        getResources().add(resource);
        if (spine.findFirstResourceById(resource.getId()) < 0) {
            spine.addSpineReference(new SpineReference(resource));
        }
        return parentSection.addChildSection(
                new TOCReference(sectionTitle, resource, fragmentId));
    }

    public TOCReference addSection(String title, Resource resource) {
        return addSection(title, resource, null);
    }

    /**
     * Adds a resource to the book's set of resources, table of contents and
     * if there is no resource with the id in the spine also adds it to the spine.
     *
     * @param title      title
     * @param resource   resource
     * @param fragmentId fragmentId
     * @return The table of contents
     */
    public TOCReference addSection(
            String title, Resource resource, String fragmentId) {
        getResources().add(resource);
        TOCReference tocReference = tableOfContents
                .addTOCReference(new TOCReference(title, resource, fragmentId));
        if (spine.findFirstResourceById(resource.getId()) < 0) {
            spine.addSpineReference(new SpineReference(resource));
        }
        return tocReference;
    }

    @SuppressWarnings("unused")
    public void generateSpineFromTableOfContents() {
        Spine spine = new Spine(tableOfContents);

        // in case the tocResource was already found and assigned
        spine.setTocResource(this.spine.getTocResource());

        this.spine = spine;
    }

    /**
     * The Book's metadata (titles, authors, etc)
     *
     * @return The Book's metadata (titles, authors, etc)
     */
    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }


    public void setResources(Resources resources) {
        this.resources = resources;
    }

    @SuppressWarnings("unused")
    public Resource addResource(Resource resource) {
        return resources.add(resource);
    }

    /**
     * The collection of all images, chapters, sections, xhtml files,
     * stylesheets, etc that make up the book.
     *
     * @return The collection of all images, chapters, sections, xhtml files,
     * stylesheets, etc that make up the book.
     */
    public Resources getResources() {
        return resources;
    }


    /**
     * The sections of the book that should be shown if a user reads the book
     * from start to finish.
     *
     * @return The Spine
     */
    public Spine getSpine() {
        return spine;
    }


    public void setSpine(Spine spine) {
        this.spine = spine;
    }


    /**
     * The Table of Contents of the book.
     *
     * @return The Table of Contents of the book.
     */
    public TableOfContents getTableOfContents() {
        return tableOfContents;
    }


    public void setTableOfContents(TableOfContents tableOfContents) {
        this.tableOfContents = tableOfContents;
    }

    /**
     * The book's cover page as a Resource.
     * An XHTML document containing a link to the cover image.
     *
     * @return The book's cover page as a Resource
     */
    public Resource getCoverPage() {
        Resource coverPage = guide.getCoverPage();
        if (coverPage == null) {
            coverPage = spine.getResource(0);
        }
        return coverPage;
    }


    public void setCoverPage(Resource coverPage) {
        if (coverPage == null) {
            return;
        }
        if (resources.notContainsByHref(coverPage.getHref())) {
            resources.add(coverPage);
        }
        guide.setCoverPage(coverPage);
    }

    /**
     * Gets the first non-blank title from the book's metadata.
     *
     * @return the first non-blank title from the book's metadata.
     */
    public String getTitle() {
        return getMetadata().getFirstTitle();
    }


    /**
     * The book's cover image.
     *
     * @return The book's cover image.
     */
    public Resource getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(Resource coverImage) {
        if (coverImage == null) {
            return;
        }
        if (resources.notContainsByHref(coverImage.getHref())) {
            resources.add(coverImage);
        }
        this.coverImage = coverImage;
    }

    /**
     * The guide; contains references to special sections of the book like
     * colophon, glossary, etc.
     *
     * @return The guide; contains references to special sections of the book
     * like colophon, glossary, etc.
     */
    public Guide getGuide() {
        return guide;
    }

    /**
     * All Resources of the Book that can be reached via the Spine, the
     * TableOfContents or the Guide.
     * <p/>
     * Consists of a list of "reachable" resources:
     * <ul>
     * <li>The coverpage</li>
     * <li>The resources of the Spine that are not already in the result</li>
     * <li>The resources of the Table of Contents that are not already in the
     * result</li>
     * <li>The resources of the Guide that are not already in the result</li>
     * </ul>
     * To get all html files that make up the epub file use
     * {@link #getResources()}
     *
     * @return All Resources of the Book that can be reached via the Spine,
     * the TableOfContents or the Guide.
     */
    public List<Resource> getContents() {
        Map<String, Resource> result = new LinkedHashMap<>();
        addToContentsResult(getCoverPage(), result);

        for (SpineReference spineReference : getSpine().getSpineReferences()) {
            addToContentsResult(spineReference.getResource(), result);
        }

        for (Resource resource : getTableOfContents().getAllUniqueResources()) {
            addToContentsResult(resource, result);
        }

        for (GuideReference guideReference : getGuide().getReferences()) {
            addToContentsResult(guideReference.getResource(), result);
        }

        return new ArrayList<>(result.values());
    }

    private static void addToContentsResult(Resource resource,
                                            Map<String, Resource> allReachableResources) {
        if (resource != null && (!allReachableResources
                .containsKey(resource.getHref()))) {
            allReachableResources.put(resource.getHref(), resource);
        }
    }

    public Resource getOpfResource() {
        return opfResource;
    }

    public void setOpfResource(Resource opfResource) {
        this.opfResource = opfResource;
    }

    public void setNcxResource(Resource ncxResource) {
        this.ncxResource = ncxResource;
    }

    public Resource getNcxResource() {
        return ncxResource;
    }
}

