package me.ag2s.epublib.domain;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper class for closing a AndroidZipFile object when the InputStream derived
 * from it is closed.
 *
 * @author ttopalov
 */
public class ResourceInputStream extends FilterInputStream {

    //private final ZipFile zipFile;

    /**
     * Constructor.
     *
     * @param in The InputStream object.
     */
    public ResourceInputStream(InputStream in) {
        super(in);
        //this.zipFile = zipFile;
    }

    @Override
    public void close() throws IOException {
        super.close();

        //zipFile.close();
    }
}
