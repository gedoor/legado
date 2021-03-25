<!-- ...................................................................... -->
<!-- XHTML Client-side Image Map Module  .................................. -->
<!-- file: xhtml-csismap-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-csismap-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Client-side Image Maps 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-csismap-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Client-side Image Maps

        area, map

     This module declares elements and attributes to support client-side
     image maps. This requires that the Image Module (or a module
     declaring the img element type) be included in the DTD.

     These can be placed in the same document or grouped in a
     separate document, although the latter isn't widely supported
-->

<!ENTITY % area.element  "INCLUDE" >
<![%area.element;[
<!ENTITY % area.content  "EMPTY" >
<!ENTITY % area.qname  "area" >
<!ELEMENT %area.qname;  %area.content; >
<!-- end of area.element -->]]>

<!ENTITY % Shape.datatype "( rect | circle | poly | default )">
<!ENTITY % Coords.datatype "CDATA" >

<!ENTITY % area.attlist  "INCLUDE" >
<![%area.attlist;[
<!ATTLIST %area.qname;
      %Common.attrib;
      href         %URI.datatype;           #IMPLIED
      shape        %Shape.datatype;         'rect'
      coords       %Coords.datatype;        #IMPLIED
      nohref       ( nohref )               #IMPLIED
      alt          %Text.datatype;          #REQUIRED
      tabindex     %Number.datatype;        #IMPLIED
      accesskey    %Character.datatype;     #IMPLIED
>
<!-- end of area.attlist -->]]>

<!-- modify anchor attribute definition list
     to allow for client-side image maps
-->
<!ATTLIST %a.qname;
      shape        %Shape.datatype;         'rect'
      coords       %Coords.datatype;        #IMPLIED
>

<!-- modify img attribute definition list
     to allow for client-side image maps
-->
<!ATTLIST %img.qname;
      usemap       %URIREF.datatype;        #IMPLIED
>

<!-- modify form input attribute definition list
     to allow for client-side image maps
-->
<!ATTLIST %input.qname;
      usemap       %URIREF.datatype;        #IMPLIED
>

<!-- modify object attribute definition list
     to allow for client-side image maps
-->
<!ATTLIST %object.qname;
      usemap       %URIREF.datatype;        #IMPLIED
>

<!-- 'usemap' points to the 'id' attribute of a <map> element,
     which must be in the same document; support for external
     document maps was not widely supported in HTML and is
     eliminated in XHTML.

     It is considered an error for the element pointed to by
     a usemap URIREF to occur in anything but a <map> element.
-->

<!ENTITY % map.element  "INCLUDE" >
<![%map.element;[
<!ENTITY % map.content
     "(( %Block.mix; ) | %area.qname; )+"
>
<!ENTITY % map.qname  "map" >
<!ELEMENT %map.qname;  %map.content; >
<!-- end of map.element -->]]>

<!ENTITY % map.attlist  "INCLUDE" >
<![%map.attlist;[
<!ATTLIST %map.qname;
      %XHTML.xmlns.attrib;
      id           ID                       #REQUIRED
      %class.attrib;
      %title.attrib;
      %Core.extra.attrib;
      %I18n.attrib;
      %Events.attrib;
>
<!-- end of map.attlist -->]]>

<!-- end of xhtml-csismap-1.mod -->
