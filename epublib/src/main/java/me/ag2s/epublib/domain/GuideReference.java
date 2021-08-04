package me.ag2s.epublib.domain;

import me.ag2s.epublib.util.StringUtil;
import java.io.Serializable;


/**
 * These are references to elements of the book's guide.
 *
 * @see Guide
 *
 * @author paul
 *
 */
public class GuideReference extends TitledResourceReference
    implements Serializable {

  private static final long serialVersionUID = -316179702440631834L;

  /**
   * the book cover(s), jacket information, etc.
   */
  public static final String COVER = "cover";

  /**
   * human-readable page with title, author, publisher, and other metadata
   */
  public static String TITLE_PAGE = "title-page";

  /**
   * Human-readable table of contents.
   * Not to be confused the epub file table of contents
   *
   */
  public static String TOC = "toc";

  /**
   * back-of-book style index
   */
  public static String INDEX = "index";
  public static String GLOSSARY = "glossary";
  public static String ACKNOWLEDGEMENTS = "acknowledgements";
  public static String BIBLIOGRAPHY = "bibliography";
  public static String COLOPHON = "colophon";
  public static String COPYRIGHT_PAGE = "copyright-page";
  public static String DEDICATION = "dedication";

  /**
   * an epigraph is a phrase, quotation, or poem that is set at the
   * beginning of a document or component.
   *
   * source: http://en.wikipedia.org/wiki/Epigraph_%28literature%29
   */
  public static String EPIGRAPH = "epigraph";

  public static String FOREWORD = "foreword";

  /**
   * list of illustrations
   */
  public static String LOI = "loi";

  /**
   * list of tables
   */
  public static String LOT = "lot";
  public static String NOTES = "notes";
  public static String PREFACE = "preface";

  /**
   * A page of content (e.g. "Chapter 1")
   */
  public static String TEXT = "text";

  private String type;

  public GuideReference(Resource resource) {
    this(resource, null);
  }

  public GuideReference(Resource resource, String title) {
    super(resource, title);
  }

  public GuideReference(Resource resource, String type, String title) {
    this(resource, type, title, null);
  }

  public GuideReference(Resource resource, String type, String title,
      String fragmentId) {
    super(resource, title, fragmentId);
    this.type = StringUtil.isNotBlank(type) ? type.toLowerCase() : null;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
