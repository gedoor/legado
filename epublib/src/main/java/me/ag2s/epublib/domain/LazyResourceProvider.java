package me.ag2s.epublib.domain;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author jake
 */
public interface LazyResourceProvider {

  InputStream getResourceStream(String href) throws IOException;
}
