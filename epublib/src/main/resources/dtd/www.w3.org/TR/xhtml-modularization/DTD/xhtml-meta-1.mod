<!-- ...................................................................... -->
<!-- XHTML Document Metainformation Module  ............................... -->
<!-- file: xhtml-meta-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-meta-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Metainformation 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-meta-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Meta Information

        meta

     This module declares the meta element type and its attributes,
     used to provide declarative document metainformation.
-->

<!-- meta: Generic Metainformation ..................... -->

<!ENTITY % meta.element  "INCLUDE" >
<![%meta.element;[
<!ENTITY % meta.content  "EMPTY" >
<!ENTITY % meta.qname  "meta" >
<!ELEMENT %meta.qname;  %meta.content; >
<!-- end of meta.element -->]]>

<!ENTITY % meta.attlist  "INCLUDE" >
<![%meta.attlist;[
<!ATTLIST %meta.qname;
      %XHTML.xmlns.attrib;
      %I18n.attrib;
      http-equiv   NMTOKEN                  #IMPLIED
      name         NMTOKEN                  #IMPLIED
      content      CDATA                    #REQUIRED
      scheme       CDATA                    #IMPLIED
>
<!-- end of meta.attlist -->]]>

<!-- end of xhtml-meta-1.mod -->
