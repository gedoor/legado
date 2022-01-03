<!-- ...................................................................... -->
<!-- XHTML Lists Module  .................................................. -->
<!-- file: xhtml-list-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-list-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Lists 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-list-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Lists

        dl, dt, dd, ol, ul, li

     This module declares the list-oriented element types
     and their attributes.
-->

<!ENTITY % dl.qname  "dl" >
<!ENTITY % dt.qname  "dt" >
<!ENTITY % dd.qname  "dd" >
<!ENTITY % ol.qname  "ol" >
<!ENTITY % ul.qname  "ul" >
<!ENTITY % li.qname  "li" >

<!-- dl: Definition List ............................... -->

<!ENTITY % dl.element  "INCLUDE" >
<![%dl.element;[
<!ENTITY % dl.content  "( %dt.qname; | %dd.qname; )+" >
<!ELEMENT %dl.qname;  %dl.content; >
<!-- end of dl.element -->]]>

<!ENTITY % dl.attlist  "INCLUDE" >
<![%dl.attlist;[
<!ATTLIST %dl.qname;
      %Common.attrib;
>
<!-- end of dl.attlist -->]]>

<!-- dt: Definition Term ............................... -->

<!ENTITY % dt.element  "INCLUDE" >
<![%dt.element;[
<!ENTITY % dt.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ELEMENT %dt.qname;  %dt.content; >
<!-- end of dt.element -->]]>

<!ENTITY % dt.attlist  "INCLUDE" >
<![%dt.attlist;[
<!ATTLIST %dt.qname;
      %Common.attrib;
>
<!-- end of dt.attlist -->]]>

<!-- dd: Definition Description ........................ -->

<!ENTITY % dd.element  "INCLUDE" >
<![%dd.element;[
<!ENTITY % dd.content
     "( #PCDATA | %Flow.mix; )*"
>
<!ELEMENT %dd.qname;  %dd.content; >
<!-- end of dd.element -->]]>

<!ENTITY % dd.attlist  "INCLUDE" >
<![%dd.attlist;[
<!ATTLIST %dd.qname;
      %Common.attrib;
>
<!-- end of dd.attlist -->]]>

<!-- ol: Ordered List (numbered styles) ................ -->

<!ENTITY % ol.element  "INCLUDE" >
<![%ol.element;[
<!ENTITY % ol.content  "( %li.qname; )+" >
<!ELEMENT %ol.qname;  %ol.content; >
<!-- end of ol.element -->]]>

<!ENTITY % ol.attlist  "INCLUDE" >
<![%ol.attlist;[
<!ATTLIST %ol.qname;
      %Common.attrib;
>
<!-- end of ol.attlist -->]]>

<!-- ul: Unordered List (bullet styles) ................ -->

<!ENTITY % ul.element  "INCLUDE" >
<![%ul.element;[
<!ENTITY % ul.content  "( %li.qname; )+" >
<!ELEMENT %ul.qname;  %ul.content; >
<!-- end of ul.element -->]]>

<!ENTITY % ul.attlist  "INCLUDE" >
<![%ul.attlist;[
<!ATTLIST %ul.qname;
      %Common.attrib;
>
<!-- end of ul.attlist -->]]>

<!-- li: List Item ..................................... -->

<!ENTITY % li.element  "INCLUDE" >
<![%li.element;[
<!ENTITY % li.content
     "( #PCDATA | %Flow.mix; )*"
>
<!ELEMENT %li.qname;  %li.content; >
<!-- end of li.element -->]]>

<!ENTITY % li.attlist  "INCLUDE" >
<![%li.attlist;[
<!ATTLIST %li.qname;
      %Common.attrib;
>
<!-- end of li.attlist -->]]>

<!-- end of xhtml-list-1.mod -->
