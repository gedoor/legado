<!-- ...................................................................... -->
<!-- XHTML Inline Structural Module  ...................................... -->
<!-- file: xhtml-inlstruct-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-inlstruct-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Inline Structural 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-inlstruct-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Inline Structural

        br, span

     This module declares the elements and their attributes
     used to support inline-level structural markup.
-->

<!-- br: forced line break ............................. -->

<!ENTITY % br.element  "INCLUDE" >
<![%br.element;[

<!ENTITY % br.content  "EMPTY" >
<!ENTITY % br.qname  "br" >
<!ELEMENT %br.qname;  %br.content; >

<!-- end of br.element -->]]>

<!ENTITY % br.attlist  "INCLUDE" >
<![%br.attlist;[
<!ATTLIST %br.qname;
      %Core.attrib;
>
<!-- end of br.attlist -->]]>

<!-- span: generic inline container .................... -->

<!ENTITY % span.element  "INCLUDE" >
<![%span.element;[
<!ENTITY % span.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % span.qname  "span" >
<!ELEMENT %span.qname;  %span.content; >
<!-- end of span.element -->]]>

<!ENTITY % span.attlist  "INCLUDE" >
<![%span.attlist;[
<!ATTLIST %span.qname;
      %Common.attrib;
>
<!-- end of span.attlist -->]]>

<!-- end of xhtml-inlstruct-1.mod -->
