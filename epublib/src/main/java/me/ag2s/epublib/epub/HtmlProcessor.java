package me.ag2s.epublib.epub;

import me.ag2s.epublib.domain.Resource;
import java.io.OutputStream;

public interface HtmlProcessor {

  void processHtmlResource(Resource resource, OutputStream out);
}
