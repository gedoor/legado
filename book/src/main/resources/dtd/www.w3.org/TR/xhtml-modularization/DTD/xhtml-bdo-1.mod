<!-- ...................................................................... -->
<!-- XHTML BDO Element Module ............................................. -->
<!-- file: xhtml-bdo-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-bdo-1.mod,v 1.1 2010/07/29 13:42:46 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML BDO Element 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-bdo-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Bidirectional Override (bdo) Element

     This modules declares the element 'bdo', used to override the
     Unicode bidirectional algorithm for selected fragments of text.

     DEPENDENCIES:
     Relies on the conditional section keyword %XHTML.bidi; declared
     as "INCLUDE". Bidirectional text support includes both the bdo
     element and the 'dir' attribute.
-->

<!ENTITY % bdo.element  "INCLUDE" >
<![%bdo.element;[
<!ENTITY % bdo.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ENTITY % bdo.qname  "bdo" >
<!ELEMENT %bdo.qname;  %bdo.content; >
<!-- end of bdo.element -->]]>

<!ENTITY % bdo.attlist  "INCLUDE" >
<![%bdo.attlist;[
<!ATTLIST %bdo.qname;
      %Core.attrib;
	  %lang.attrib;
      dir          ( ltr | rtl )            #REQUIRED
>
]]>

<!-- end of xhtml-bdo-1.mod -->
