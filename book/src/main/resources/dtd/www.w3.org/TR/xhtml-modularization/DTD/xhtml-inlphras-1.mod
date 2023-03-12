<!-- ...................................................................... -->
<!-- XHTML Inline Phrasal Module  ......................................... -->
<!-- file: xhtml-inlphras-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-inlphras-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Inline Phrasal 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-inlphras-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Inline Phrasal

        abbr, acronym, cite, code, dfn, em, kbd, q, samp, strong, var

     This module declares the elements and their attributes used to
     support inline-level phrasal markup.
-->

<!ENTITY % abbr.element  "INCLUDE" >
<![%abbr.element;[
<!ENTITY % abbr.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % abbr.qname  "abbr" >
<!ELEMENT %abbr.qname;  %abbr.content; >
<!-- end of abbr.element -->]]>

<!ENTITY % abbr.attlist  "INCLUDE" >
<![%abbr.attlist;[
<!ATTLIST %abbr.qname;
      %Common.attrib;
>
<!-- end of abbr.attlist -->]]>

<!ENTITY % acronym.element  "INCLUDE" >
<![%acronym.element;[
<!ENTITY % acronym.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % acronym.qname  "acronym" >
<!ELEMENT %acronym.qname;  %acronym.content; >
<!-- end of acronym.element -->]]>

<!ENTITY % acronym.attlist  "INCLUDE" >
<![%acronym.attlist;[
<!ATTLIST %acronym.qname;
      %Common.attrib;
>
<!-- end of acronym.attlist -->]]>

<!ENTITY % cite.element  "INCLUDE" >
<![%cite.element;[
<!ENTITY % cite.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % cite.qname  "cite" >
<!ELEMENT %cite.qname;  %cite.content; >
<!-- end of cite.element -->]]>

<!ENTITY % cite.attlist  "INCLUDE" >
<![%cite.attlist;[
<!ATTLIST %cite.qname;
      %Common.attrib;
>
<!-- end of cite.attlist -->]]>

<!ENTITY % code.element  "INCLUDE" >
<![%code.element;[
<!ENTITY % code.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % code.qname  "code" >
<!ELEMENT %code.qname;  %code.content; >
<!-- end of code.element -->]]>

<!ENTITY % code.attlist  "INCLUDE" >
<![%code.attlist;[
<!ATTLIST %code.qname;
      %Common.attrib;
>
<!-- end of code.attlist -->]]>

<!ENTITY % dfn.element  "INCLUDE" >
<![%dfn.element;[
<!ENTITY % dfn.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % dfn.qname  "dfn" >
<!ELEMENT %dfn.qname;  %dfn.content; >
<!-- end of dfn.element -->]]>

<!ENTITY % dfn.attlist  "INCLUDE" >
<![%dfn.attlist;[
<!ATTLIST %dfn.qname;
      %Common.attrib;
>
<!-- end of dfn.attlist -->]]>

<!ENTITY % em.element  "INCLUDE" >
<![%em.element;[
<!ENTITY % em.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % em.qname  "em" >
<!ELEMENT %em.qname;  %em.content; >
<!-- end of em.element -->]]>

<!ENTITY % em.attlist  "INCLUDE" >
<![%em.attlist;[
<!ATTLIST %em.qname;
      %Common.attrib;
>
<!-- end of em.attlist -->]]>

<!ENTITY % kbd.element  "INCLUDE" >
<![%kbd.element;[
<!ENTITY % kbd.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % kbd.qname  "kbd" >
<!ELEMENT %kbd.qname;  %kbd.content; >
<!-- end of kbd.element -->]]>

<!ENTITY % kbd.attlist  "INCLUDE" >
<![%kbd.attlist;[
<!ATTLIST %kbd.qname;
      %Common.attrib;
>
<!-- end of kbd.attlist -->]]>

<!ENTITY % q.element  "INCLUDE" >
<![%q.element;[
<!ENTITY % q.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % q.qname  "q" >
<!ELEMENT %q.qname;  %q.content; >
<!-- end of q.element -->]]>

<!ENTITY % q.attlist  "INCLUDE" >
<![%q.attlist;[
<!ATTLIST %q.qname;
      %Common.attrib;
      cite         %URI.datatype;           #IMPLIED
>
<!-- end of q.attlist -->]]>

<!ENTITY % samp.element  "INCLUDE" >
<![%samp.element;[
<!ENTITY % samp.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % samp.qname  "samp" >
<!ELEMENT %samp.qname;  %samp.content; >
<!-- end of samp.element -->]]>

<!ENTITY % samp.attlist  "INCLUDE" >
<![%samp.attlist;[
<!ATTLIST %samp.qname;
      %Common.attrib;
>
<!-- end of samp.attlist -->]]>

<!ENTITY % strong.element  "INCLUDE" >
<![%strong.element;[
<!ENTITY % strong.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % strong.qname  "strong" >
<!ELEMENT %strong.qname;  %strong.content; >
<!-- end of strong.element -->]]>

<!ENTITY % strong.attlist  "INCLUDE" >
<![%strong.attlist;[
<!ATTLIST %strong.qname;
      %Common.attrib;
>
<!-- end of strong.attlist -->]]>

<!ENTITY % var.element  "INCLUDE" >
<![%var.element;[
<!ENTITY % var.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % var.qname  "var" >
<!ELEMENT %var.qname;  %var.content; >
<!-- end of var.element -->]]>

<!ENTITY % var.attlist  "INCLUDE" >
<![%var.attlist;[
<!ATTLIST %var.qname;
      %Common.attrib;
>
<!-- end of var.attlist -->]]>

<!-- end of xhtml-inlphras-1.mod -->
