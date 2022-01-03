package me.ag2s.epublib.util.commons.io;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ag2s.epublib.util.IOUtil;


/**
 * Character stream that handles all the necessary Voodoo to figure out the
 * charset encoding of the XML document within the stream.
 * <p>
 * IMPORTANT: This class is not related in any way to the org.xml.sax.XMLReader.
 * This one IS a character stream.
 * </p>
 * <p>
 * All this has to be done without consuming characters from the stream, if not
 * the XML parser will not recognized the document as a valid XML. This is not
 * 100% true, but it's close enough (UTF-8 BOM is not handled by all parsers
 * right now, XmlStreamReader handles it and things work in all parsers).
 * </p>
 * <p>
 * The XmlStreamReader class handles the charset encoding of XML documents in
 * Files, raw streams and HTTP streams by offering a wide set of constructors.
 * </p>
 * <p>
 * By default the charset encoding detection is lenient, the constructor with
 * the lenient flag can be used for a script (following HTTP MIME and XML
 * specifications). All this is nicely explained by Mark Pilgrim in his blog, <a
 * href="http://diveintomark.org/archives/2004/02/13/xml-media-types">
 * Determining the character encoding of a feed</a>.
 * </p>
 * <p>
 * Originally developed for <a href="http://rome.dev.java.net">ROME</a> under
 * Apache License 2.0.
 * </p>
 *
 * //@seerr XmlStreamWriter
 * @since 2.0
 */
public class XmlStreamReader extends Reader {
    private static final int BUFFER_SIZE = IOUtil.DEFAULT_BUFFER_SIZE;

    private static final String UTF_8 = "UTF-8";

    private static final String US_ASCII = "US-ASCII";

    private static final String UTF_16BE = "UTF-16BE";

    private static final String UTF_16LE = "UTF-16LE";

    private static final String UTF_32BE = "UTF-32BE";

    private static final String UTF_32LE = "UTF-32LE";

    private static final String UTF_16 = "UTF-16";

    private static final String UTF_32 = "UTF-32";

    private static final String EBCDIC = "CP1047";

    private static final ByteOrderMark[] BOMS = new ByteOrderMark[] {
            ByteOrderMark.UTF_8,
            ByteOrderMark.UTF_16BE,
            ByteOrderMark.UTF_16LE,
            ByteOrderMark.UTF_32BE,
            ByteOrderMark.UTF_32LE
    };

    // UTF_16LE and UTF_32LE have the same two starting BOM bytes.
    private static final ByteOrderMark[] XML_GUESS_BYTES = new ByteOrderMark[] {
            new ByteOrderMark(UTF_8,    0x3C, 0x3F, 0x78, 0x6D),
            new ByteOrderMark(UTF_16BE, 0x00, 0x3C, 0x00, 0x3F),
            new ByteOrderMark(UTF_16LE, 0x3C, 0x00, 0x3F, 0x00),
            new ByteOrderMark(UTF_32BE, 0x00, 0x00, 0x00, 0x3C,
                    0x00, 0x00, 0x00, 0x3F, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x6D),
            new ByteOrderMark(UTF_32LE, 0x3C, 0x00, 0x00, 0x00,
                    0x3F, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x6D, 0x00, 0x00, 0x00),
            new ByteOrderMark(EBCDIC,   0x4C, 0x6F, 0xA7, 0x94)
    };

    private final Reader reader;

    private final String encoding;

    private final String defaultEncoding;

    /**
     * Returns the default encoding to use if none is set in HTTP content-type,
     * XML prolog and the rules based on content-type are not adequate.
     * <p>
     * If it is NULL the content-type based rules are used.
     *
     * @return the default encoding to use.
     */
    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Creates a Reader for a File.
     * <p>
     * It looks for the UTF-8 BOM first, if none sniffs the XML prolog charset,
     * if this is also missing defaults to UTF-8.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with
     * the lenient parameter for details.
     *
     * @param file File to create a Reader from.
     * @throws IOException thrown if there is a problem reading the file.
     */
    @SuppressWarnings("unused")
    public XmlStreamReader(final File file) throws IOException {
        this(new FileInputStream(Objects.requireNonNull(file)));
    }

    /**
     * Creates a Reader for a raw InputStream.
     * <p>
     * It follows the same logic used for files.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with
     * the lenient parameter for details.
     *
     * @param inputStream InputStream to create a Reader from.
     * @throws IOException thrown if there is a problem reading the stream.
     */
    public XmlStreamReader(final InputStream inputStream) throws IOException {
        this(inputStream, true);
    }

    /**
     * Creates a Reader for a raw InputStream.
     * <p>
     * It follows the same logic used for files.
     * <p>
     * If lenient detection is indicated and the detection above fails as per
     * specifications it then attempts the following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and
     * tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never
     * thrown.
     *
     * @param inputStream InputStream to create a Reader from.
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @throws IOException thrown if there is a problem reading the stream.
     * @throws XmlStreamReaderException thrown if the charset encoding could not
     *         be determined according to the specs.
     */
    public XmlStreamReader(final InputStream inputStream, final boolean lenient) throws IOException {
        this(inputStream, lenient, null);
    }

    /**
     * Creates a Reader for a raw InputStream.
     * <p>
     * It follows the same logic used for files.
     * <p>
     * If lenient detection is indicated and the detection above fails as per
     * specifications it then attempts the following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and
     * tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never
     * thrown.
     *
     * @param inputStream InputStream to create a Reader from.
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @param defaultEncoding The default encoding
     * @throws IOException thrown if there is a problem reading the stream.
     * @throws XmlStreamReaderException thrown if the charset encoding could not
     *         be determined according to the specs.
     */
    public XmlStreamReader(final InputStream inputStream, final boolean lenient, final String defaultEncoding)
            throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        this.defaultEncoding = defaultEncoding;
        final BOMInputStream bom = new BOMInputStream(new BufferedInputStream(inputStream, BUFFER_SIZE), false, BOMS);
        final BOMInputStream pis = new BOMInputStream(bom, true, XML_GUESS_BYTES);
        this.encoding = doRawStream(bom, pis, lenient);
        this.reader = new InputStreamReader(pis, encoding);
    }

    /**
     * Creates a Reader using the InputStream of a URL.
     * <p>
     * If the URL is not of type HTTP and there is not 'content-type' header in
     * the fetched data it uses the same logic used for Files.
     * <p>
     * If the URL is a HTTP Url or there is a 'content-type' header in the
     * fetched data it uses the same logic used for an InputStream with
     * content-type.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with
     * the lenient parameter for details.
     *
     * @param url URL to create a Reader from.
     * @throws IOException thrown if there is a problem reading the stream of
     *         the URL.
     */
    @SuppressWarnings("unused")
    public XmlStreamReader(final URL url) throws IOException {
        this(Objects.requireNonNull(url, "url").openConnection(), null);
    }

    /**
     * Creates a Reader using the InputStream of a URLConnection.
     * <p>
     * If the URLConnection is not of type HttpURLConnection and there is not
     * 'content-type' header in the fetched data it uses the same logic used for
     * files.
     * <p>
     * If the URLConnection is a HTTP Url or there is a 'content-type' header in
     * the fetched data it uses the same logic used for an InputStream with
     * content-type.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with
     * the lenient parameter for details.
     *
     * @param conn URLConnection to create a Reader from.
     * @param defaultEncoding The default encoding
     * @throws IOException thrown if there is a problem reading the stream of
     *         the URLConnection.
     */
    public XmlStreamReader(final URLConnection conn, final String defaultEncoding) throws IOException {
        Objects.requireNonNull(conn, "conm");
        this.defaultEncoding = defaultEncoding;
        final boolean lenient = true;
        final String contentType = conn.getContentType();
        final InputStream inputStream = conn.getInputStream();
        final BOMInputStream bom = new BOMInputStream(new BufferedInputStream(inputStream, BUFFER_SIZE), false, BOMS);
        final BOMInputStream pis = new BOMInputStream(bom, true, XML_GUESS_BYTES);
        if (conn instanceof HttpURLConnection || contentType != null) {
            this.encoding = processHttpStream(bom, pis, contentType, lenient);
        } else {
            this.encoding = doRawStream(bom, pis, lenient);
        }
        this.reader = new InputStreamReader(pis, encoding);
    }

    /**
     * Creates a Reader using an InputStream and the associated content-type
     * header.
     * <p>
     * First it checks if the stream has BOM. If there is not BOM checks the
     * content-type encoding. If there is not content-type encoding checks the
     * XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     * <p>
     * It does a lenient charset encoding detection, check the constructor with
     * the lenient parameter for details.
     *
     * @param inputStream InputStream to create the reader from.
     * @param httpContentType content-type header to use for the resolution of
     *        the charset encoding.
     * @throws IOException thrown if there is a problem reading the file.
     */
    public XmlStreamReader(final InputStream inputStream, final String httpContentType)
            throws IOException {
        this(inputStream, httpContentType, true);
    }

    /**
     * Creates a Reader using an InputStream and the associated content-type
     * header. This constructor is lenient regarding the encoding detection.
     * <p>
     * First it checks if the stream has BOM. If there is not BOM checks the
     * content-type encoding. If there is not content-type encoding checks the
     * XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     * <p>
     * If lenient detection is indicated and the detection above fails as per
     * specifications it then attempts the following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and
     * tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never
     * thrown.
     *
     * @param inputStream InputStream to create the reader from.
     * @param httpContentType content-type header to use for the resolution of
     *        the charset encoding.
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @param defaultEncoding The default encoding
     * @throws IOException thrown if there is a problem reading the file.
     * @throws XmlStreamReaderException thrown if the charset encoding could not
     *         be determined according to the specs.
     */
    public XmlStreamReader(final InputStream inputStream, final String httpContentType,
                           final boolean lenient, final String defaultEncoding) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        this.defaultEncoding = defaultEncoding;
        final BOMInputStream bom = new BOMInputStream(new BufferedInputStream(inputStream, BUFFER_SIZE), false, BOMS);
        final BOMInputStream pis = new BOMInputStream(bom, true, XML_GUESS_BYTES);
        this.encoding = processHttpStream(bom, pis, httpContentType, lenient);
        this.reader = new InputStreamReader(pis, encoding);
    }

    /**
     * Creates a Reader using an InputStream and the associated content-type
     * header. This constructor is lenient regarding the encoding detection.
     * <p>
     * First it checks if the stream has BOM. If there is not BOM checks the
     * content-type encoding. If there is not content-type encoding checks the
     * XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     * <p>
     * If lenient detection is indicated and the detection above fails as per
     * specifications it then attempts the following:
     * <p>
     * If the content type was 'text/html' it replaces it with 'text/xml' and
     * tries the detection again.
     * <p>
     * Else if the XML prolog had a charset encoding that encoding is used.
     * <p>
     * Else if the content type had a charset encoding that encoding is used.
     * <p>
     * Else 'UTF-8' is used.
     * <p>
     * If lenient detection is indicated an XmlStreamReaderException is never
     * thrown.
     *
     * @param inputStream InputStream to create the reader from.
     * @param httpContentType content-type header to use for the resolution of
     *        the charset encoding.
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @throws IOException thrown if there is a problem reading the file.
     * @throws XmlStreamReaderException thrown if the charset encoding could not
     *         be determined according to the specs.
     */
    public XmlStreamReader(final InputStream inputStream, final String httpContentType,
                           final boolean lenient) throws IOException {
        this(inputStream, httpContentType, lenient, null);
    }

    /**
     * Returns the charset encoding of the XmlStreamReader.
     *
     * @return charset encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Invokes the underlying reader's <code>read(char[], int, int)</code> method.
     * @param buf the buffer to read the characters into
     * @param offset The start offset
     * @param len The number of bytes to read
     * @return the number of characters read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final char[] buf, final int offset, final int len) throws IOException {
        return reader.read(buf, offset, len);
    }

    /**
     * Closes the XmlStreamReader stream.
     *
     * @throws IOException thrown if there was a problem closing the stream.
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Process the raw stream.
     *
     * @param bom BOMInputStream to detect byte order marks
     * @param pis BOMInputStream to guess XML encoding
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @return the encoding to be used
     * @throws IOException thrown if there is a problem reading the stream.
     */
    private String doRawStream(final BOMInputStream bom, final BOMInputStream pis, final boolean lenient)
            throws IOException {
        final String bomEnc      = bom.getBOMCharsetName();
        final String xmlGuessEnc = pis.getBOMCharsetName();
        final String xmlEnc = getXmlProlog(pis, xmlGuessEnc);
        try {
            return calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc);
        } catch (final XmlStreamReaderException ex) {
            if (lenient) {
                return doLenientDetection(null, ex);
            }
            throw ex;
        }
    }

    /**
     * Process a HTTP stream.
     *
     * @param bom BOMInputStream to detect byte order marks
     * @param pis BOMInputStream to guess XML encoding
     * @param httpContentType The HTTP content type
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @return the encoding to be used
     * @throws IOException thrown if there is a problem reading the stream.
     */
    private String processHttpStream(final BOMInputStream bom, final BOMInputStream pis, final String httpContentType,
                                     final boolean lenient) throws IOException {
        final String bomEnc = bom.getBOMCharsetName();
        final String xmlGuessEnc = pis.getBOMCharsetName();
        final String xmlEnc = getXmlProlog(pis, xmlGuessEnc);
        try {
            return calculateHttpEncoding(httpContentType, bomEnc, xmlGuessEnc, xmlEnc, lenient);
        } catch (final XmlStreamReaderException ex) {
            if (lenient) {
                return doLenientDetection(httpContentType, ex);
            }
            throw ex;
        }
    }

    /**
     * Do lenient detection.
     *
     * @param httpContentType content-type header to use for the resolution of
     *        the charset encoding.
     * @param ex The thrown exception
     * @return the encoding
     * @throws IOException thrown if there is a problem reading the stream.
     */
    private String doLenientDetection(String httpContentType,
                                      XmlStreamReaderException ex) throws IOException {
        if (httpContentType != null && httpContentType.startsWith("text/html")) {
            httpContentType = httpContentType.substring("text/html".length());
            httpContentType = "text/xml" + httpContentType;
            try {
                return calculateHttpEncoding(httpContentType, ex.getBomEncoding(),
                        ex.getXmlGuessEncoding(), ex.getXmlEncoding(), true);
            } catch (final XmlStreamReaderException ex2) {
                ex = ex2;
            }
        }
        String encoding = ex.getXmlEncoding();
        if (encoding == null) {
            encoding = ex.getContentTypeEncoding();
        }
        if (encoding == null) {
            encoding = defaultEncoding == null ? UTF_8 : defaultEncoding;
        }
        return encoding;
    }

    /**
     * Calculate the raw encoding.
     *
     * @param bomEnc BOM encoding
     * @param xmlGuessEnc XML Guess encoding
     * @param xmlEnc XML encoding
     * @return the raw encoding
     * @throws IOException thrown if there is a problem reading the stream.
     */
    String calculateRawEncoding(final String bomEnc, final String xmlGuessEnc,
                                final String xmlEnc) throws IOException {

        // BOM is Null
        if (bomEnc == null) {
            if (xmlGuessEnc == null || xmlEnc == null) {
                return defaultEncoding == null ? UTF_8 : defaultEncoding;
            }
            if (xmlEnc.equals(UTF_16) &&
                    (xmlGuessEnc.equals(UTF_16BE) || xmlGuessEnc.equals(UTF_16LE))) {
                return xmlGuessEnc;
            }
            return xmlEnc;
        }

        // BOM is UTF-8
        if (bomEnc.equals(UTF_8)) {
            if (xmlGuessEnc != null && !xmlGuessEnc.equals(UTF_8)) {
                final String msg = MessageFormat.format(RAW_EX_1, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
            }
            if (xmlEnc != null && !xmlEnc.equals(UTF_8)) {
                final String msg = MessageFormat.format(RAW_EX_1, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
            }
            return bomEnc;
        }

        // BOM is UTF-16BE or UTF-16LE
        if (bomEnc.equals(UTF_16BE) || bomEnc.equals(UTF_16LE)) {
            if (xmlGuessEnc != null && !xmlGuessEnc.equals(bomEnc)) {
                final String msg = MessageFormat.format(RAW_EX_1, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
            }
            if (xmlEnc != null && !xmlEnc.equals(UTF_16) && !xmlEnc.equals(bomEnc)) {
                final String msg = MessageFormat.format(RAW_EX_1, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
            }
            return bomEnc;
        }

        // BOM is UTF-32BE or UTF-32LE
        if (bomEnc.equals(UTF_32BE) || bomEnc.equals(UTF_32LE)) {
            if (xmlGuessEnc != null && !xmlGuessEnc.equals(bomEnc)) {
                final String msg = MessageFormat.format(RAW_EX_1, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
            }
            if (xmlEnc != null && !xmlEnc.equals(UTF_32) && !xmlEnc.equals(bomEnc)) {
                final String msg = MessageFormat.format(RAW_EX_1, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
            }
            return bomEnc;
        }

        // BOM is something else
        final String msg = MessageFormat.format(RAW_EX_2, bomEnc, xmlGuessEnc, xmlEnc);
        throw new XmlStreamReaderException(msg, bomEnc, xmlGuessEnc, xmlEnc);
    }


    /**
     * Calculate the HTTP encoding.
     *
     * @param httpContentType The HTTP content type
     * @param bomEnc BOM encoding
     * @param xmlGuessEnc XML Guess encoding
     * @param xmlEnc XML encoding
     * @param lenient indicates if the charset encoding detection should be
     *        relaxed.
     * @return the HTTP encoding
     * @throws IOException thrown if there is a problem reading the stream.
     */
    String calculateHttpEncoding(final String httpContentType,
                                 final String bomEnc, final String xmlGuessEnc, final String xmlEnc,
                                 final boolean lenient) throws IOException {

        // Lenient and has XML encoding
        if (lenient && xmlEnc != null) {
            return xmlEnc;
        }

        // Determine mime/encoding content types from HTTP Content Type
        final String cTMime = getContentTypeMime(httpContentType);
        final String cTEnc  = getContentTypeEncoding(httpContentType);
        final boolean appXml  = isAppXml(cTMime);
        final boolean textXml = isTextXml(cTMime);

        // Mime type NOT "application/xml" or "text/xml"
        if (!appXml && !textXml) {
            final String msg = MessageFormat.format(HTTP_EX_3, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
            throw new XmlStreamReaderException(msg, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
        }

        // No content type encoding
        if (cTEnc == null) {
            if (appXml) {
                return calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc);
            }
            return defaultEncoding == null ? US_ASCII : defaultEncoding;
        }

        // UTF-16BE or UTF-16LE content type encoding
        if (cTEnc.equals(UTF_16BE) || cTEnc.equals(UTF_16LE)) {
            if (bomEnc != null) {
                final String msg = MessageFormat.format(HTTP_EX_1, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
            }
            return cTEnc;
        }

        // UTF-16 content type encoding
        if (cTEnc.equals(UTF_16)) {
            if (bomEnc != null && bomEnc.startsWith(UTF_16)) {
                return bomEnc;
            }
            final String msg = MessageFormat.format(HTTP_EX_2, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
            throw new XmlStreamReaderException(msg, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
        }

        // UTF-32BE or UTF-132E content type encoding
        if (cTEnc.equals(UTF_32BE) || cTEnc.equals(UTF_32LE)) {
            if (bomEnc != null) {
                final String msg = MessageFormat.format(HTTP_EX_1, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
                throw new XmlStreamReaderException(msg, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
            }
            return cTEnc;
        }

        // UTF-32 content type encoding
        if (cTEnc.equals(UTF_32)) {
            if (bomEnc != null && bomEnc.startsWith(UTF_32)) {
                return bomEnc;
            }
            final String msg = MessageFormat.format(HTTP_EX_2, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
            throw new XmlStreamReaderException(msg, cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc);
        }

        return cTEnc;
    }

    /**
     * Returns MIME type or NULL if httpContentType is NULL.
     *
     * @param httpContentType the HTTP content type
     * @return The mime content type
     */
    static String getContentTypeMime(final String httpContentType) {
        String mime = null;
        if (httpContentType != null) {
            final int i = httpContentType.indexOf(";");
            if (i >= 0) {
                mime = httpContentType.substring(0, i);
            } else {
                mime = httpContentType;
            }
            mime = mime.trim();
        }
        return mime;
    }

    private static final Pattern CHARSET_PATTERN = Pattern
            .compile("charset=[\"']?([.[^; \"']]*)[\"']?");

    /**
     * Returns charset parameter value, NULL if not present, NULL if
     * httpContentType is NULL.
     *
     * @param httpContentType the HTTP content type
     * @return The content type encoding (upcased)
     */
    static String getContentTypeEncoding(final String httpContentType) {
        String encoding = null;
        if (httpContentType != null) {
            final int i = httpContentType.indexOf(";");
            if (i > -1) {
                final String postMime = httpContentType.substring(i + 1);
                final Matcher m = CHARSET_PATTERN.matcher(postMime);
                encoding = m.find() ? m.group(1) : null;
                encoding = encoding != null ? encoding.toUpperCase(Locale.ROOT) : null;
            }
        }
        return encoding;
    }

    /**
     * Pattern capturing the encoding of the "xml" processing instruction.
     */
    public static final Pattern ENCODING_PATTERN = Pattern.compile(
            "<\\?xml.*encoding[\\s]*=[\\s]*((?:\".[^\"]*\")|(?:'.[^']*'))",
            Pattern.MULTILINE);

    /**
     * Returns the encoding declared in the <?xml encoding=...?>, NULL if none.
     *
     * @param inputStream InputStream to create the reader from.
     * @param guessedEnc guessed encoding
     * @return the encoding declared in the <?xml encoding=...?>
     * @throws IOException thrown if there is a problem reading the stream.
     */
    private static String getXmlProlog(final InputStream inputStream, final String guessedEnc)
            throws IOException {
        String encoding = null;
        if (guessedEnc != null) {
            final byte[] bytes = new byte[BUFFER_SIZE];
            inputStream.mark(BUFFER_SIZE);
            int offset = 0;
            int max = BUFFER_SIZE;
            int c = inputStream.read(bytes, offset, max);
            int firstGT = -1;
            String xmlProlog = ""; // avoid possible NPE warning (cannot happen; this just silences the warning)
            while (c != -1 && firstGT == -1 && offset < BUFFER_SIZE) {
                offset += c;
                max -= c;
                c = inputStream.read(bytes, offset, max);
                xmlProlog = new String(bytes, 0, offset, guessedEnc);
                firstGT = xmlProlog.indexOf('>');
            }
            if (firstGT == -1) {
                if (c == -1) {
                    throw new IOException("Unexpected end of XML stream");
                }
                throw new IOException(
                        "XML prolog or ROOT element not found on first "
                                + offset + " bytes");
            }
            final int bytesRead = offset;
            if (bytesRead > 0) {
                inputStream.reset();
                final BufferedReader bReader = new BufferedReader(new StringReader(
                        xmlProlog.substring(0, firstGT + 1)));
                final StringBuffer prolog = new StringBuffer();
                String line;
                while ((line = bReader.readLine()) != null) {
                    prolog.append(line);
                }
                final Matcher m = ENCODING_PATTERN.matcher(prolog);
                if (m.find()) {
                    encoding = Objects.requireNonNull(m.group(1)).toUpperCase(Locale.ROOT);
                    encoding = encoding.substring(1, encoding.length() - 1);
                }
            }
        }
        return encoding;
    }

    /**
     * Indicates if the MIME type belongs to the APPLICATION XML family.
     *
     * @param mime The mime type
     * @return true if the mime type belongs to the APPLICATION XML family,
     * otherwise false
     */
    static boolean isAppXml(final String mime) {
        return mime != null &&
                (mime.equals("application/xml") ||
                        mime.equals("application/xml-dtd") ||
                        mime.equals("application/xml-external-parsed-entity") ||
                        mime.startsWith("application/") && mime.endsWith("+xml"));
    }

    /**
     * Indicates if the MIME type belongs to the TEXT XML family.
     *
     * @param mime The mime type
     * @return true if the mime type belongs to the TEXT XML family,
     * otherwise false
     */
    static boolean isTextXml(final String mime) {
        return mime != null &&
                (mime.equals("text/xml") ||
                        mime.equals("text/xml-external-parsed-entity") ||
                        mime.startsWith("text/") && mime.endsWith("+xml"));
    }

    private static final String RAW_EX_1 =
            "Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] encoding mismatch";

    private static final String RAW_EX_2 =
            "Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] unknown BOM";

    private static final String HTTP_EX_1 =
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], BOM must be NULL";

    private static final String HTTP_EX_2 =
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], encoding mismatch";

    private static final String HTTP_EX_3 =
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], Invalid MIME";

}