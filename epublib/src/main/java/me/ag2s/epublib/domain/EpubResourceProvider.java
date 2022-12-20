package me.ag2s.epublib.domain;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

import me.ag2s.epublib.util.zip.ZipEntryWrapper;
import me.ag2s.epublib.util.zip.ZipFileWrapper;

/**
 * @author jake
 */
public class EpubResourceProvider implements LazyResourceProvider {


  private final ZipFileWrapper zipFileWrapper;


  public EpubResourceProvider(ZipFileWrapper zipFileWrapper) {
    this.zipFileWrapper = zipFileWrapper;
  }


  @Override
  public InputStream getResourceStream(@NonNull String href) throws IOException {

    //ZipFile zipFile = new ZipFile(epubFilename);
    ZipEntryWrapper zipEntry = zipFileWrapper.getEntry(href);
    if (zipEntry == null) {
      //zipFile.close();
      throw new IllegalStateException(
              "Cannot find entry " + href + " in epub file " + zipFileWrapper);
    }
    return new ResourceInputStream(zipFileWrapper.getInputStream(zipEntry));
  }
}
