package me.ag2s.epublib.epub;

import android.util.Log;

import me.ag2s.epublib.Constants;
//import io.documentnode.minilog.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
//import org.kxml2.io.KXmlSerializer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Various low-level support methods for reading/writing epubs.
 *
 * @author paul.siegmann
 *
 */
public class EpubProcessorSupport {

  private static String TAG= EpubProcessorSupport.class.getName();

  protected static DocumentBuilderFactory documentBuilderFactory;

  static {
    init();
  }

  static class EntityResolverImpl implements EntityResolver {

    private String previousLocation;

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException {
      String resourcePath;
      if (systemId.startsWith("http:")) {
        URL url = new URL(systemId);
        resourcePath = "dtd/" + url.getHost() + url.getPath();
        previousLocation = resourcePath
            .substring(0, resourcePath.lastIndexOf('/'));
      } else {
        resourcePath =
            previousLocation + systemId.substring(systemId.lastIndexOf('/'));
      }

      if (this.getClass().getClassLoader().getResource(resourcePath) == null) {
        throw new RuntimeException(
            "remote resource is not cached : [" + systemId
                + "] cannot continue");
      }

      InputStream in = EpubProcessorSupport.class.getClassLoader()
          .getResourceAsStream(resourcePath);
      return new InputSource(in);
    }
  }


  private static void init() {
    EpubProcessorSupport.documentBuilderFactory = DocumentBuilderFactory
        .newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setValidating(false);
  }

  public static XmlSerializer createXmlSerializer(OutputStream out)
      throws UnsupportedEncodingException {
    return createXmlSerializer(
        new OutputStreamWriter(out, Constants.CHARACTER_ENCODING));
  }

  public static XmlSerializer createXmlSerializer(Writer out) {
    XmlSerializer result = null;
    try {
      /*
       * Disable XmlPullParserFactory here before it doesn't work when
       * building native image using GraalVM
       */
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       factory.setValidating(true);
       result = factory.newSerializer();

      //result = new KXmlSerializer();
      result.setFeature(
          "http://xmlpull.org/v1/doc/features.html#indent-output", true);
      result.setOutput(out);
    } catch (Exception e) {
      Log.e(TAG,
          "When creating XmlSerializer: " + e.getClass().getName() + ": " + e
              .getMessage());
    }
    return result;
  }

  /**
   * Gets an EntityResolver that loads dtd's and such from the epub4j classpath.
   * In order to enable the loading of relative urls the given EntityResolver contains the previousLocation.
   * Because of a new EntityResolver is created every time this method is called.
   * Fortunately the EntityResolver created uses up very little memory per instance.
   *
   * @return an EntityResolver that loads dtd's and such from the epub4j classpath.
   */
  public static EntityResolver getEntityResolver() {
    return new EntityResolverImpl();
  }

  public DocumentBuilderFactory getDocumentBuilderFactory() {
    return documentBuilderFactory;
  }

  /**
   * Creates a DocumentBuilder that looks up dtd's and schema's from epub4j's classpath.
   *
   * @return a DocumentBuilder that looks up dtd's and schema's from epub4j's classpath.
   */
  public static DocumentBuilder createDocumentBuilder() {
    DocumentBuilder result = null;
    try {
      result = documentBuilderFactory.newDocumentBuilder();
      result.setEntityResolver(getEntityResolver());
    } catch (ParserConfigurationException e) {
      Log.e(TAG,e.getMessage());
    }
    return result;
  }
}
