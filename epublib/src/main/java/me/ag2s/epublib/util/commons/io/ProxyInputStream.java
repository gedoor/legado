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


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import me.ag2s.epublib.util.IOUtil;

import static me.ag2s.epublib.util.IOUtil.EOF;


/**
 * A Proxy stream which acts as expected, that is it passes the method
 * calls on to the proxied stream and doesn't change which methods are
 * being called.
 * <p>
 * It is an alternative base class to FilterInputStream
 * to increase reusability, because FilterInputStream changes the
 * methods being called, such as read(byte[]) to read(byte[], int, int).
 * </p>
 * <p>
 * See the protected methods for ways in which a subclass can easily decorate
 * a stream with custom pre-, post- or error processing functionality.
 * </p>
 */
public abstract class ProxyInputStream extends FilterInputStream {

    /**
     * Constructs a new ProxyInputStream.
     *
     * @param proxy the InputStream to delegate to
     */
    public ProxyInputStream(final InputStream proxy) {
        super(proxy);
        // the proxy is stored in a protected superclass variable named 'in'
    }

    /**
     * Invokes the delegate's <code>read()</code> method.
     *
     * @return the byte read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        try {
            beforeRead(1);
            final int b = in.read();
            afterRead(b != EOF ? 1 : EOF);
            return b;
        } catch (final IOException e) {
            handleIOException(e);
            return EOF;
        }
    }

    /**
     * Invokes the delegate's <code>read(byte[])</code> method.
     *
     * @param bts the buffer to read the bytes into
     * @return the number of bytes read or EOF if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] bts) throws IOException {
        try {
            beforeRead(IOUtil.length(bts));
            final int n = in.read(bts);
            afterRead(n);
            return n;
        } catch (final IOException e) {
            handleIOException(e);
            return EOF;
        }
    }

    /**
     * Invokes the delegate's <code>read(byte[], int, int)</code> method.
     *
     * @param bts the buffer to read the bytes into
     * @param off The start offset
     * @param len The number of bytes to read
     * @return the number of bytes read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(final byte[] bts, final int off, final int len) throws IOException {
        try {
            beforeRead(len);
            final int n = in.read(bts, off, len);
            afterRead(n);
            return n;
        } catch (final IOException e) {
            handleIOException(e);
            return EOF;
        }
    }

    /**
     * Invokes the delegate's <code>skip(long)</code> method.
     *
     * @param ln the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(final long ln) throws IOException {
        try {
            return in.skip(ln);
        } catch (final IOException e) {
            handleIOException(e);
            return 0;
        }
    }

    /**
     * Invokes the delegate's <code>available()</code> method.
     *
     * @return the number of available bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        try {
            return super.available();
        } catch (final IOException e) {
            handleIOException(e);
            return 0;
        }
    }

    /**
     * Invokes the delegate's <code>close()</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        IOUtil.close(in, this::handleIOException);
    }

    /**
     * Invokes the delegate's <code>mark(int)</code> method.
     *
     * @param readlimit read ahead limit
     */
    @Override
    public synchronized void mark(final int readlimit) {
        in.mark(readlimit);
    }

    /**
     * Invokes the delegate's <code>reset()</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public synchronized void reset() throws IOException {
        try {
            in.reset();
        } catch (final IOException e) {
            handleIOException(e);
        }
    }

    /**
     * Invokes the delegate's <code>markSupported()</code> method.
     *
     * @return true if mark is supported, otherwise false
     */
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Invoked by the read methods before the call is proxied. The number
     * of bytes that the caller wanted to read (1 for the {@link #read()}
     * method, buffer length for {@link #read(byte[])}, etc.) is given as
     * an argument.
     * <p>
     * Subclasses can override this method to add common pre-processing
     * functionality without having to override all the read methods.
     * The default implementation does nothing.
     * <p>
     * Note this method is <em>not</em> called from {@link #skip(long)} or
     * {@link #reset()}. You need to explicitly override those methods if
     * you want to add pre-processing steps also to them.
     *
     * @param n number of bytes that the caller asked to be read
     * @since 2.0
     */
    @SuppressWarnings("unused")

    protected void beforeRead(final int n) {
        // no-op
    }

    /**
     * Invoked by the read methods after the proxied call has returned
     * successfully. The number of bytes returned to the caller (or -1 if
     * the end of stream was reached) is given as an argument.
     * <p>
     * Subclasses can override this method to add common post-processing
     * functionality without having to override all the read methods.
     * The default implementation does nothing.
     * <p>
     * Note this method is <em>not</em> called from {@link #skip(long)} or
     * {@link #reset()}. You need to explicitly override those methods if
     * you want to add post-processing steps also to them.
     *
     * @param n number of bytes read, or -1 if the end of stream was reached
     * @since 2.0
     */
    @SuppressWarnings("unused")
    protected void afterRead(final int n) {
        // no-op
    }

    /**
     * Handle any IOExceptions thrown.
     * <p>
     * This method provides a point to implement custom exception
     * handling. The default behavior is to re-throw the exception.
     *
     * @param e The IOException thrown
     * @throws IOException if an I/O error occurs
     * @since 2.0
     */
    protected void handleIOException(final IOException e) throws IOException {
        throw e;
    }

}
