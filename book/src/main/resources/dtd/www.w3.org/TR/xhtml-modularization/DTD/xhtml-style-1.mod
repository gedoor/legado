<!-- ...................................................................... -->
<!-- XHTML Document Style Sheet Module  ................................... -->
<!-- file: xhtml-style-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-style-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//DTD XHTML Style Sheets 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-style-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Style Sheets

        style

     This module declares the style element type and its attributes,
     used to embed style sheet information in the document head element.
-->

<!-- style: Style Sheet Information .................... -->

<!ENTITY % style.element  "INCLUDE" >
<![%style.element;[
<!ENTITY % style.content  "( #PCDATA )" >
<!ENTITY % style.qname  "style" >
<!ELEMENT %style.qname;  %style.content; >
<!-- end of style.element -->]]>

<!ENTITY % style.attlist  "INCLUDE" >
<![%style.attlist;[
<!ATTLIST %style.qname;
      %XHTML.xmlns.attrib;
      %id.attrib;
      %title.attrib;
      %I18n.attrib;
      xml:space    ( preserve )             #FIXED 'preserve'
      type         %ContentType.datatype;   #REQUIRED
      media        %MediaDesc.datatype;     #IMPLIED
>
<!-- end of style.attlist -->]]>

<!-- end of xhtml-style-1.mod -->
