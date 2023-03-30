package me.ag2s.epublib.epub;

import java.io.OutputStream;

import me.ag2s.epublib.domain.Resource;

@SuppressWarnings("unused")
public interface HtmlProcessor {

    void processHtmlResource(Resource resource, OutputStream out);
}
