package me.ag2s.epublib.epub;

import android.util.Log;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.ag2s.epublib.domain.EpubBook;
import me.ag2s.epublib.domain.MediaTypes;
import me.ag2s.epublib.domain.Resource;
import me.ag2s.epublib.util.IOUtil;

/**
 * Generates an epub file. Not thread-safe, single use object.
 *
 * @author paul
 */
public class EpubWriter {

    // package
    static final String EMPTY_NAMESPACE_PREFIX = "";
    private static final String TAG = EpubWriter.class.getName();
    private BookProcessor bookProcessor;

    private EpubWriterProcessor epubWriterProcessor;

    public EpubWriter() {
        this(BookProcessor.IDENTITY_BOOKPROCESSOR);
        this.epubWriterProcessor = new EpubWriterProcessor();
        // 写入MimeType、Container，初始化TOCResource整体为1
        // 写入PackageDocument 为1
        // 关闭流 为1
        this.epubWriterProcessor.setTotalProgress(3);
    }


    public EpubWriter(BookProcessor bookProcessor) {
        this.bookProcessor = bookProcessor;
    }

    public EpubWriter setCallback(EpubWriterProcessor.Callback callback) {
        epubWriterProcessor.setCallback(callback);
        return this;
    }

    public void write(EpubBook book, OutputStream out) throws IOException {
        if (Objects.nonNull(this.epubWriterProcessor.getCallback())) {
            epubWriterProcessor.getCallback().onStart(book);
        }
        epubWriterProcessor.setTotalProgress(epubWriterProcessor.getTotalProgress() + book.getResources().size());
        book = processBook(book);
        ZipOutputStream resultStream = new ZipOutputStream(out);
        //resultStream.setLevel(ZipOutputStream.STORED);
        writeMimeType(resultStream);
        writeContainer(resultStream);
        initTOCResource(book);
        epubWriterProcessor.updateCurrentProgress(1);
        writeResources(book, resultStream);
        writePackageDocument(book, resultStream);
        epubWriterProcessor.updateCurrentProgress(epubWriterProcessor.getCurrentProgress() + 1);
        resultStream.close();
        epubWriterProcessor.updateCurrentProgress(epubWriterProcessor.getCurrentProgress() + 1);
        if (Objects.nonNull(epubWriterProcessor.getCallback())) {
            epubWriterProcessor.getCallback().onEnd(book);
        }
    }

    private EpubBook processBook(EpubBook book) {
        if (bookProcessor != null) {
            book = bookProcessor.processBook(book);
        }
        return book;
    }

    private void initTOCResource(EpubBook book) {
        Resource tocResource;
        try {
            if (book.isEpub3()) {
                tocResource = NCXDocumentV3.createNCXResource(book);
            } else {
                tocResource = NCXDocumentV2.createNCXResource(book);
            }

            Resource currentTocResource = book.getSpine().getTocResource();
            if (currentTocResource != null) {
                book.getResources().remove(currentTocResource.getHref());
            }
            book.getSpine().setTocResource(tocResource);
            book.getResources().add(tocResource);
        } catch (Exception ex) {
            Log.e(TAG, "Error writing table of contents: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }


    private void writeResources(EpubBook book, ZipOutputStream resultStream) {
        for (Resource resource : book.getResources().getAll()) {
            writeResource(resource, resultStream);
            epubWriterProcessor.updateCurrentProgress(epubWriterProcessor.getCurrentProgress() + 1);
        }
    }

    /**
     * Writes the resource to the resultStream.
     *
     * @param resource     resource
     * @param resultStream resultStream
     */
    private void writeResource(Resource resource, ZipOutputStream resultStream) {
        if (resource == null) {
            return;
        }
        try {
            resultStream.putNextEntry(new ZipEntry("OEBPS/" + resource.getHref()));
            InputStream inputStream = resource.getInputStream();

            IOUtil.copy(inputStream, resultStream);
            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private void writePackageDocument(EpubBook book, ZipOutputStream resultStream) throws IOException {
        resultStream.putNextEntry(new ZipEntry("OEBPS/content.opf"));
        XmlSerializer xmlSerializer = EpubProcessorSupport.createXmlSerializer(resultStream);
        PackageDocumentWriter.write(this, xmlSerializer, book);
        xmlSerializer.flush();
//		String resultAsString = result.toString();
//		resultStream.write(resultAsString.getBytes(Constants.ENCODING));
    }

    /**
     * Writes the META-INF/container.xml file.
     *
     * @param resultStream resultStream
     * @throws IOException IOException
     */
    private void writeContainer(ZipOutputStream resultStream) throws IOException {
        resultStream.putNextEntry(new ZipEntry("META-INF/container.xml"));
        Writer out = new OutputStreamWriter(resultStream);
        out.write("<?xml version=\"1.0\"?>\n");
        out.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n");
        out.write("\t<rootfiles>\n");
        out.write("\t\t<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n");
        out.write("\t</rootfiles>\n");
        out.write("</container>");
        out.flush();
    }

    /**
     * Stores the mimetype as an uncompressed file in the ZipOutputStream.
     *
     * @param resultStream resultStream
     * @throws IOException IOException
     */
    private void writeMimeType(ZipOutputStream resultStream) throws IOException {
        ZipEntry mimetypeZipEntry = new ZipEntry("mimetype");
        mimetypeZipEntry.setMethod(ZipEntry.STORED);
        byte[] mimetypeBytes = MediaTypes.EPUB.getName().getBytes();
        mimetypeZipEntry.setSize(mimetypeBytes.length);
        mimetypeZipEntry.setCrc(calculateCrc(mimetypeBytes));
        resultStream.putNextEntry(mimetypeZipEntry);
        resultStream.write(mimetypeBytes);
    }

    private long calculateCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    String getNcxId() {
        return "ncx";
    }

    String getNcxHref() {
        return "toc.ncx";
    }

    String getNcxMediaType() {
        return MediaTypes.NCX.getName();
    }


    @SuppressWarnings("unused")
    public BookProcessor getBookProcessor() {
        return bookProcessor;
    }

    @SuppressWarnings("unused")
    public void setBookProcessor(BookProcessor bookProcessor) {
        this.bookProcessor = bookProcessor;
    }

}
