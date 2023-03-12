package me.ag2s.epublib.epub;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import me.ag2s.epublib.Constants;
import me.ag2s.epublib.domain.Author;
import me.ag2s.epublib.domain.Date;
import me.ag2s.epublib.domain.EpubBook;
import me.ag2s.epublib.domain.Identifier;
import me.ag2s.epublib.util.StringUtil;

public class PackageDocumentMetadataWriter extends PackageDocumentBase {

  /**
   * Writes the book's metadata.
   *
   * @param book       book
   * @param serializer serializer
   * @throws IOException              IOException
   * @throws IllegalStateException    IllegalStateException
   * @throws IllegalArgumentException IllegalArgumentException
   */
  public static void writeMetaData(EpubBook book, XmlSerializer serializer)
          throws IllegalArgumentException, IllegalStateException, IOException {
    serializer.startTag(NAMESPACE_OPF, OPFTags.metadata);
    serializer.setPrefix(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE);
    serializer.setPrefix(PREFIX_OPF, NAMESPACE_OPF);

    writeIdentifiers(book.getMetadata().getIdentifiers(), serializer);
    writeSimpleMetdataElements(DCTags.title, book.getMetadata().getTitles(),
            serializer);
    writeSimpleMetdataElements(DCTags.subject, book.getMetadata().getSubjects(),
            serializer);
    writeSimpleMetdataElements(DCTags.description,
        book.getMetadata().getDescriptions(), serializer);
    writeSimpleMetdataElements(DCTags.publisher,
        book.getMetadata().getPublishers(), serializer);
    writeSimpleMetdataElements(DCTags.type, book.getMetadata().getTypes(),
        serializer);
    writeSimpleMetdataElements(DCTags.rights, book.getMetadata().getRights(),
        serializer);

    // write authors
    for (Author author : book.getMetadata().getAuthors()) {
      serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.creator);
      serializer.attribute(NAMESPACE_OPF, OPFAttributes.role,
          author.getRelator().getCode());
      serializer.attribute(NAMESPACE_OPF, OPFAttributes.file_as,
          author.getLastname() + ", " + author.getFirstname());
      serializer.text(author.getFirstname() + " " + author.getLastname());
      serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.creator);
    }

    // write contributors
    for (Author author : book.getMetadata().getContributors()) {
      serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor);
      serializer.attribute(NAMESPACE_OPF, OPFAttributes.role,
          author.getRelator().getCode());
      serializer.attribute(NAMESPACE_OPF, OPFAttributes.file_as,
          author.getLastname() + ", " + author.getFirstname());
      serializer.text(author.getFirstname() + " " + author.getLastname());
      serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor);
    }

    // write dates
    for (Date date : book.getMetadata().getDates()) {
      serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.date);
      if (date.getEvent() != null) {
        serializer.attribute(NAMESPACE_OPF, OPFAttributes.event,
            date.getEvent().toString());
      }
      serializer.text(date.getValue());
      serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.date);
    }

    // write language
    if (StringUtil.isNotBlank(book.getMetadata().getLanguage())) {
      serializer.startTag(NAMESPACE_DUBLIN_CORE, "language");
      serializer.text(book.getMetadata().getLanguage());
      serializer.endTag(NAMESPACE_DUBLIN_CORE, "language");
    }

    // write other properties
    if (book.getMetadata().getOtherProperties() != null) {
      for (Map.Entry<QName, String> mapEntry : book.getMetadata()
          .getOtherProperties().entrySet()) {
        serializer.startTag(mapEntry.getKey().getNamespaceURI(), OPFTags.meta);
        serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX,
            OPFAttributes.property, mapEntry.getKey().getLocalPart());
        serializer.text(mapEntry.getValue());
        serializer.endTag(mapEntry.getKey().getNamespaceURI(), OPFTags.meta);

      }
    }

    // write coverimage
    if (book.getCoverImage() != null) { // write the cover image
      serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
      serializer
          .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.name,
              OPFValues.meta_cover);
      serializer
          .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content,
              book.getCoverImage().getId());
      serializer.endTag(NAMESPACE_OPF, OPFTags.meta);
    }

    // write generator
    serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
    serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.name,
        OPFValues.generator);
    serializer
        .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content,
            Constants.EPUB_GENERATOR_NAME);
    serializer.endTag(NAMESPACE_OPF, OPFTags.meta);

    // write duokan
    serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
    serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.name,
            OPFValues.duokan);
    serializer
            .attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content,
                    Constants.EPUB_DUOKAN_NAME);
    serializer.endTag(NAMESPACE_OPF, OPFTags.meta);

    serializer.endTag(NAMESPACE_OPF, OPFTags.metadata);
  }

  private static void writeSimpleMetdataElements(String tagName,
      List<String> values, XmlSerializer serializer)
      throws IllegalArgumentException, IllegalStateException, IOException {
    for (String value : values) {
      if (StringUtil.isBlank(value)) {
        continue;
      }
      serializer.startTag(NAMESPACE_DUBLIN_CORE, tagName);
      serializer.text(value);
      serializer.endTag(NAMESPACE_DUBLIN_CORE, tagName);
    }
  }


  /**
   * Writes out the complete list of Identifiers to the package document.
   * The first identifier for which the bookId is true is made the bookId identifier.
   * If no identifier has bookId == true then the first bookId identifier is written as the primary.
   *
   * @param identifiers identifiers
   * @param serializer serializer
   * @throws IllegalStateException e
   * @throws IllegalArgumentException e
   * @
   */
  private static void writeIdentifiers(List<Identifier> identifiers,
      XmlSerializer serializer)
      throws IllegalArgumentException, IllegalStateException, IOException {
    Identifier bookIdIdentifier = Identifier.getBookIdIdentifier(identifiers);
    if (bookIdIdentifier == null) {
      return;
    }

    serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
    serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, DCAttributes.id,
        BOOK_ID_ID);
    serializer.attribute(NAMESPACE_OPF, OPFAttributes.scheme,
        bookIdIdentifier.getScheme());
    serializer.text(bookIdIdentifier.getValue());
    serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);

    for (Identifier identifier : identifiers.subList(1, identifiers.size())) {
      if (identifier == bookIdIdentifier) {
        continue;
      }
      serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
      serializer.attribute(NAMESPACE_OPF, "scheme", identifier.getScheme());
      serializer.text(identifier.getValue());
      serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
    }
  }

}
