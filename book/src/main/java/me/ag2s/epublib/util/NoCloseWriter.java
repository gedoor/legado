package me.ag2s.epublib.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Writer with the close() disabled.
 * We write multiple documents to a ZipOutputStream.
 * Some of the formatters call a close() after writing their data.
 * We don't want them to do that, so we wrap regular Writers in this NoCloseWriter.
 *
 * @author paul
 */
@SuppressWarnings("unused")
public class NoCloseWriter extends Writer {

  private final Writer writer;

  public NoCloseWriter(Writer writer) {
    this.writer = writer;
  }

  @Override
  public void close() {
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    writer.write(cbuf, off, len);
  }
}
