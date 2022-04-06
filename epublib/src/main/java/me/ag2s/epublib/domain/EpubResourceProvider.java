package me.ag2s.epublib.domain;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import me.ag2s.epublib.zip.ZipEntry;
import me.ag2s.epublib.zip.ZipFile;

/**
 * @author jake
 */
public class EpubResourceProvider implements LazyResourceProvider {

  private final Context context;
  private final Uri uri;

  /**
   * @param context
   * @param uri
   */
  public EpubResourceProvider(Context context, Uri uri) {
    this.context = context;
    this.uri = uri;
  }


  @Override
  public InputStream getResourceStream(String href) throws IOException {
    ZipFile zipFile = new ZipFile(context, uri);
    ZipEntry zipEntry = zipFile.getEntry(href);
    if (zipEntry == null) {
      zipFile.close();
      throw new IllegalStateException(
              "Cannot find entry " + href + " in epub file " + uri.toString());
    }
    return new ResourceInputStream(zipFile.getInputStream(zipEntry), zipFile);
  }
}
