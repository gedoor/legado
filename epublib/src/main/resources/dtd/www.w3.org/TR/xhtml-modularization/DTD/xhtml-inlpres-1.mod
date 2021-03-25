<!-- ...................................................................... -->
<!-- XHTML Inline Presentation Module  .................................... -->
<!-- file: xhtml-inlpres-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-inlpres-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Inline Presentation 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-inlpres-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Inline Presentational Elements

        b, big, i, small, sub, sup, tt

     This module declares the elements and their attributes used to
     support inline-level presentational markup.
-->

<!ENTITY % b.element  "INCLUDE" >
<![%b.element;[
<!ENTITY % b.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % b.qname  "b" >
<!ELEMENT %b.qname;  %b.content; >
<!-- end of b.element -->]]>

<!ENTITY % b.attlist  "INCLUDE" >
<![%b.attlist;[
<!ATTLIST %b.qname;
      %Common.attrib;
>
<!-- end of b.attlist -->]]>

<!ENTITY % big.element  "INCLUDE" >
<![%big.element;[
<!ENTITY % big.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % big.qname  "big" >
<!ELEMENT %big.qname;  %big.content; >
<!-- end of big.element -->]]>

<!ENTITY % big.attlist  "INCLUDE" >
<![%big.attlist;[
<!ATTLIST %big.qname;
      %Common.attrib;
>
<!-- end of big.attlist -->]]>

<!ENTITY % i.element  "INCLUDE" >
<![%i.element;[
<!ENTITY % i.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % i.qname  "i" >
<!ELEMENT %i.qname;  %i.content; >
<!-- end of i.element -->]]>

<!ENTITY % i.attlist  "INCLUDE" >
<![%i.attlist;[
<!ATTLIST %i.qname;
      %Common.attrib;
>
<!-- end of i.attlist -->]]>

<!ENTITY % small.element  "INCLUDE" >
<![%small.element;[
<!ENTITY % small.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % small.qname  "small" >
<!ELEMENT %small.qname;  %small.content; >
<!-- end of small.element -->]]>

<!ENTITY % small.attlist  "INCLUDE" >
<![%small.attlist;[
<!ATTLIST %small.qname;
      %Common.attrib;
>
<!-- end of small.attlist -->]]>

<!ENTITY % sub.element  "INCLUDE" >
<![%sub.element;[
<!ENTITY % sub.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % sub.qname  "sub" >
<!ELEMENT %sub.qname;  %sub.content; >
<!-- end of sub.element -->]]>

<!ENTITY % sub.attlist  "INCLUDE" >
<![%sub.attlist;[
<!ATTLIST %sub.qname;
      %Common.attrib;
>
<!-- end of sub.attlist -->]]>

<!ENTITY % sup.element  "INCLUDE" >
<![%sup.element;[
<!ENTITY % sup.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % sup.qname  "sup" >
<!ELEMENT %sup.qname;  %sup.content; >
<!-- end of sup.element -->]]>

<!ENTITY % sup.attlist  "INCLUDE" >
<![%sup.attlist;[
<!ATTLIST %sup.qname;
      %Common.attrib;
>
<!-- end of sup.attlist -->]]>

<!ENTITY % tt.element  "INCLUDE" >
<![%tt.element;[
<!ENTITY % tt.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % tt.qname  "tt" >
<!ELEMENT %tt.qname;  %tt.content; >
<!-- end of tt.element -->]]>

<!ENTITY % tt.attlist  "INCLUDE" >
<![%tt.attlist;[
<!ATTLIST %tt.qname;
      %Common.attrib;
>
<!-- end of tt.attlist -->]]>

<!-- end of xhtml-inlpres-1.mod -->
