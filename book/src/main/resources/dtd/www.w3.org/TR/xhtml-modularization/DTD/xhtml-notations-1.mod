<!-- ...................................................................... -->
<!-- XHTML Notations Module  .............................................. -->
<!-- file: xhtml-notations-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-notations-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//NOTATIONS XHTML Notations 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-notations-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Notations

     defines the following notations, many of these imported from
     other specifications and standards. When an existing FPI is
     known, it is incorporated here.
-->

<!-- XML Notations ..................................... -->
<!-- SGML and XML Notations ............................ -->

<!-- W3C XML 1.0 Recommendation -->
<!NOTATION w3c-xml
     PUBLIC "ISO 8879//NOTATION Extensible Markup Language (XML) 1.0//EN" >

<!-- XML 1.0 CDATA -->
<!NOTATION cdata
     PUBLIC "-//W3C//NOTATION XML 1.0: CDATA//EN" >

<!-- SGML Formal Public Identifiers -->
<!NOTATION fpi
     PUBLIC "ISO 8879:1986//NOTATION Formal Public Identifier//EN" >

<!-- XHTML Notations ................................... -->

<!-- Length defined for cellpadding/cellspacing -->

<!-- nn for pixels or nn% for percentage length -->
<!NOTATION length
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Length//EN" >

<!-- space-separated list of link types -->
<!NOTATION linkTypes
    PUBLIC "-//W3C//NOTATION XHTML Datatype: LinkTypes//EN" >

<!-- single or comma-separated list of media descriptors -->
<!NOTATION mediaDesc
    PUBLIC "-//W3C//NOTATION XHTML Datatype: MediaDesc//EN" >

<!-- pixel, percentage, or relative -->
<!NOTATION multiLength
    PUBLIC "-//W3C//NOTATION XHTML Datatype: MultiLength//EN" >

<!-- one or more digits (NUMBER) -->
<!NOTATION number
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Number//EN" >

<!-- integer representing length in pixels -->
<!NOTATION pixels
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Pixels//EN" >

<!-- script expression -->
<!NOTATION script
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Script//EN" >

<!-- textual content -->
<!NOTATION text
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Text//EN" >

<!-- Imported Notations ................................ -->

<!-- a single character from [ISO10646] -->
<!NOTATION character
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Character//EN" >

<!-- a character encoding, as per [RFC2045] -->
<!NOTATION charset
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Charset//EN" >

<!-- a space separated list of character encodings, as per [RFC2045] -->
<!NOTATION charsets
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Charsets//EN" >

<!-- media type, as per [RFC2045] -->
<!NOTATION contentType
    PUBLIC "-//W3C//NOTATION XHTML Datatype: ContentType//EN" >

<!-- comma-separated list of media types, as per [RFC2045] -->
<!NOTATION contentTypes
    PUBLIC "-//W3C//NOTATION XHTML Datatype: ContentTypes//EN" >

<!-- date and time information. ISO date format -->
<!NOTATION datetime
    PUBLIC "-//W3C//NOTATION XHTML Datatype: Datetime//EN" >

<!-- a language code, as per [RFC3066] -->
<!NOTATION languageCode
    PUBLIC "-//W3C//NOTATION XHTML Datatype: LanguageCode//EN" >

<!-- a Uniform Resource Identifier, see [URI] -->
<!NOTATION uri
    PUBLIC "-//W3C//NOTATION XHTML Datatype: URI//EN" >

<!-- a space-separated list of Uniform Resource Identifiers, see [URI] -->
<!NOTATION uris
    PUBLIC "-//W3C//NOTATION XHTML Datatype: URIs//EN" >

<!-- end of xhtml-notations-1.mod -->
