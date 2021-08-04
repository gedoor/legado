package me.ag2s.epublib.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream with the close() disabled.
 * We write multiple documents to a ZipOutputStream.
 * Some of the formatters call a close() after writing their data.
 * We don't want them to do that, so we wrap regular OutputStreams in this NoCloseOutputStream.
 *
 * @author paul
 */
@SuppressWarnings("unused")
public class NoCloseOutputStream extends OutputStream {

  private final OutputStream outputStream;

  public NoCloseOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(b);
  }

  /**
   * A close() that does not call it's parent's close()
   */
  public void close() {
  }
}
