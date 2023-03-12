package me.ag2s.epublib.epub;


/**
 * Functionality shared by the PackageDocumentReader and the PackageDocumentWriter
 *
 * @author paul
 *
 */
public class PackageDocumentBase {

  public static final String BOOK_ID_ID = "duokan-book-id";
  public static final String NAMESPACE_OPF = "http://www.idpf.org/2007/opf";
  public static final String NAMESPACE_DUBLIN_CORE = "http://purl.org/dc/elements/1.1/";
  public static final String PREFIX_DUBLIN_CORE = "dc";
  //public static final String PREFIX_OPF = "opf";
  //在EPUB3标准中，packge前面没有opf头，一些epub阅读器也不支持opf头。
  //Some Epub Reader not reconize op:packge,So just let it empty;
  public static final String PREFIX_OPF = "";
  //添加 version 变量来区分Epub文件的版本
  //Add the version field to distinguish the version of EPUB file
  public static final String version="version";
  public static final String dateFormat = "yyyy-MM-dd";

  protected interface DCTags {

    String title = "title";
    String creator = "creator";
    String subject = "subject";
    String description = "description";
    String publisher = "publisher";
    String contributor = "contributor";
    String date = "date";
    String type = "type";
    String format = "format";
    String identifier = "identifier";
    String source = "source";
    String language = "language";
    String relation = "relation";
    String coverage = "coverage";
    String rights = "rights";
  }

  protected interface DCAttributes {

    String scheme = "scheme";
    String id = "id";
  }

  protected interface OPFTags {

    String metadata = "metadata";
    String meta = "meta";
    String manifest = "manifest";
    String packageTag = "package";
    String itemref = "itemref";
    String spine = "spine";
    String reference = "reference";
    String guide = "guide";
    String item = "item";
  }

  protected interface OPFAttributes {

    String uniqueIdentifier = "unique-identifier";
    String idref = "idref";
    String name = "name";
    String content = "content";
    String type = "type";
    String href = "href";
    String linear = "linear";
    String event = "event";
    String role = "role";
    String file_as = "file-as";
    String id = "id";
    String media_type = "media-type";
    String title = "title";
    String toc = "toc";
    String version = "version";
    String scheme = "scheme";
    String property = "property";
    //add for epub3
    /**
     * add for epub3
     */
    String properties="properties";
  }

  protected interface OPFValues {

    String meta_cover = "cover";
    String reference_cover = "cover";
    String no = "no";
    String generator = "generator";
    String duokan = "duokan-body-font";
  }
}