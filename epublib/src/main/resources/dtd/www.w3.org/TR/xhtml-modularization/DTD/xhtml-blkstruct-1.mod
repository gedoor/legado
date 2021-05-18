<!-- ...................................................................... -->
<!-- XHTML Block Structural Module  ....................................... -->
<!-- file: xhtml-blkstruct-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-blkstruct-1.mod,v 1.1 2010/07/29 13:42:46 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Block Structural 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-blkstruct-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Block Structural

        div, p

     This module declares the elements and their attributes used to
     support block-level structural markup.
-->

<!ENTITY % div.element  "INCLUDE" >
<![%div.element;[
<!ENTITY % div.content
     "( #PCDATA | %Flow.mix; )*"
>
<!ENTITY % div.qname  "div" >
<!ELEMENT %div.qname;  %div.content; >
<!-- end of div.element -->]]>

<!ENTITY % div.attlist  "INCLUDE" >
<![%div.attlist;[
<!ATTLIST %div.qname;
      %Common.attrib;
>
<!-- end of div.attlist -->]]>

<!ENTITY % p.element  "INCLUDE" >
<![%p.element;[
<!ENTITY % p.content
     "( #PCDATA | %Inline.mix; )*" >
<!ENTITY % p.qname  "p" >
<!ELEMENT %p.qname;  %p.content; >
<!-- end of p.element -->]]>

<!ENTITY % p.attlist  "INCLUDE" >
<![%p.attlist;[
<!ATTLIST %p.qname;
      %Common.attrib;
>
<!-- end of p.attlist -->]]>

<!-- end of xhtml-blkstruct-1.mod -->
