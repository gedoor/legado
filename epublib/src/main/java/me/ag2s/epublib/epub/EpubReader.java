package me.ag2s.epublib.epub;

import android.util.Log;

import androidx.annotation.NonNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.domain.EpubBook;
import me.ag2s.epublib.domain.MediaType;
import me.ag2s.epublib.domain.MediaTypes;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.domain.Resources;
import me.ag2s.epublib.util.ResourceUtil;
import me.ag2s.epublib.util.StringUtil;
import me.ag2s.epublib.util.zip.AndroidZipFile;
import me.ag2s.epublib.util.zip.ZipFileWrapper;

/**
 * Reads an epub file.
 *
 * @author paul
 */
@SuppressWarnings("ALL")
public class EpubReader {

    private static final String TAG = EpubReader.class.getName();
    private final BookProcessor bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR;

    public EpubBook readEpub(InputStream in) throws IOException {
        return readEpub(in, Constants.CHARACTER_ENCODING);
    }

    public EpubBook readEpub(ZipInputStream in) throws IOException {
        return readEpub(in, Constants.CHARACTER_ENCODING);
    }

    public EpubBook readEpub(ZipFile zipfile) throws IOException {
        return readEpub(zipfile, Constants.CHARACTER_ENCODING);
    }

    /**
     * Read epub from inputstream
     *
     * @param in       the inputstream from which to read the epub
     * @param encoding the encoding to use for the html files within the epub
     * @return the Book as read from the inputstream
     * @throws IOException IOException
     */
    public EpubBook readEpub(InputStream in, String encoding) throws IOException {
        return readEpub(new ZipInputStream(in), encoding);
    }


    /**
     * Reads this EPUB without loading any resources into memory.
     *
     * @param zipFile  the file to load
     * @param encoding the encoding for XHTML files
     * @return this Book without loading all resources into memory.
     * @throws IOException IOException
     */
    public EpubBook readEpubLazy(@NonNull ZipFile zipFile, @NonNull String encoding)
            throws IOException {
        return readEpubLazy(zipFile, encoding, Arrays.asList(MediaTypes.mediaTypes));
    }

    public EpubBook readEpubLazy(@NonNull AndroidZipFile zipFile, @NonNull String encoding)
            throws IOException {
        return readEpubLazy(zipFile, encoding, Arrays.asList(MediaTypes.mediaTypes));
    }

    public EpubBook readEpub(@NonNull ZipInputStream in, @NonNull String encoding) throws IOException {
        return readEpub(ResourcesLoader.loadResources(in, encoding));
    }

    public EpubBook readEpub(ZipFile in, String encoding) throws IOException {
        return readEpub(ResourcesLoader.loadResources(new ZipFileWrapper(in), encoding));
    }

    /**
     * Reads this EPUB without loading all resources into memory.
     *
     * @param zipFile         the file to load
     * @param encoding        the encoding for XHTML files
     * @param lazyLoadedTypes a list of the MediaType to load lazily
     * @return this Book without loading all resources into memory.
     * @throws IOException IOException
     */
    public EpubBook readEpubLazy(@NonNull ZipFile zipFile, @NonNull String encoding,
                                 @NonNull List<MediaType> lazyLoadedTypes) throws IOException {
        Resources resources = ResourcesLoader
                .loadResources(new ZipFileWrapper(zipFile), encoding, lazyLoadedTypes);
        return readEpub(resources);
    }

    public EpubBook readEpubLazy(@NonNull AndroidZipFile zipFile, @NonNull String encoding,
                                 @NonNull List<MediaType> lazyLoadedTypes) throws IOException {
        Resources resources = ResourcesLoader
                .loadResources(new ZipFileWrapper(zipFile), encoding, lazyLoadedTypes);
        return readEpub(resources);
    }

    public EpubBook readEpub(Resources resources) {
        return readEpub(resources, new EpubBook());
    }

    public EpubBook readEpub(Resources resources, EpubBook result) {
        if (result == null) {
            result = new EpubBook();
        }
        handleMimeType(result, resources);
        String packageResourceHref = getPackageResourceHref(resources);
        Resource packageResource = processPackageResource(packageResourceHref, result, resources);
        result.setOpfResource(packageResource);
        Resource ncxResource = processNcxResource(packageResource, result);
        result.setNcxResource(ncxResource);
        result = postProcessBook(result);
        return result;
    }

    private EpubBook postProcessBook(EpubBook book) {
        if (bookProcessor != null) {
            book = bookProcessor.processBook(book);
        }
        return book;
    }

    private Resource processNcxResource(Resource packageResource, EpubBook book) {
        Log.d(TAG, "OPF:getHref()" + packageResource.getHref());
        if (book.isEpub3()) {
            return NCXDocumentV3.read(book, this);
        } else {
            return NCXDocumentV2.read(book, this);
        }

    }

    private Resource processPackageResource(String packageResourceHref, EpubBook book,
                                            Resources resources) {
        Resource packageResource = resources.remove(packageResourceHref);
        try {
            PackageDocumentReader.read(packageResource, this, book, resources);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return packageResource;
    }

    private String getPackageResourceHref(Resources resources) {
        String defaultResult = "OEBPS/content.opf";
        String result = defaultResult;

        Resource containerResource = resources.remove("META-INF/container.xml");
        if (containerResource == null) {
            return result;
        }
        try {
            Document document = ResourceUtil.getAsDocument(containerResource);
            Element rootFileElement = (Element) ((Element) document
                    .getDocumentElement().getElementsByTagName("rootfiles").item(0))
                    .getElementsByTagName("rootfile").item(0);
            result = rootFileElement.getAttribute("full-path");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        if (StringUtil.isBlank(result)) {
            result = defaultResult;
        }
        return result;
    }

    private void handleMimeType(EpubBook result, Resources resources) {
        resources.remove("mimetype");
        //result.setResources(resources);
    }
}
