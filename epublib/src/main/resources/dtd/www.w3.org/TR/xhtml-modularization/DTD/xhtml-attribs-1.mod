<!-- ...................................................................... -->
<!-- XHTML Common Attributes Module  ...................................... -->
<!-- file: xhtml-attribs-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-attribs-1.mod,v 1.1 2010/07/29 13:42:46 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ENTITIES XHTML Common Attributes 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-attribs-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Common Attributes

     This module declares many of the common attributes for the XHTML DTD.
     %NS.decl.attrib; is declared in the XHTML Qname module.

	 Note that this file was extended in XHTML Modularization 1.1 to 
	 include declarations of "global" versions of the attribute collections.
	 The global versions of the attributes are for use on elements in other 
	 namespaces.  The global version of "common" includes the xmlns declaration
	 for the prefixed version of the xhtml namespace.  If you are only using a
	 specific attribute or an individual attribute collection, you must also
	 include the XHTML.xmlns.attrib.prefixed PE on your elements.
-->

<!ENTITY % id.attrib
     "id           ID                       #IMPLIED"
>

<![%XHTML.global.attrs.prefixed;[
<!ENTITY % XHTML.global.id.attrib
     "%XHTML.prefix;:id           ID        #IMPLIED"
>
]]>

<!ENTITY % class.attrib
     "class        CDATA                 #IMPLIED"
>

<![%XHTML.global.attrs.prefixed;[
<!ENTITY % XHTML.global.class.attrib
     "%XHTML.prefix;:class        CDATA                 #IMPLIED"
>
]]>

<!ENTITY % title.attrib
     "title        %Text.datatype;          #IMPLIED"
>

<![%XHTML.global.attrs.prefixed;[
<!ENTITY % XHTML.global.title.attrib
     "%XHTML.prefix;:title        %Text.datatype;          #IMPLIED"
>
]]>

<!ENTITY % Core.extra.attrib "" >

<!ENTITY % Core.attrib
     "%XHTML.xmlns.attrib;
      %id.attrib;
      %class.attrib;
      %title.attrib;
      xml:space    ( preserve )             #FIXED 'preserve'
      %Core.extra.attrib;"
>

<!ENTITY % XHTML.global.core.extra.attrib "" >

<![%XHTML.global.attrs.prefixed;[

<!ENTITY % XHTML.global.core.attrib
     "%XHTML.global.id.attrib;
      %XHTML.global.class.attrib;
      %XHTML.global.title.attrib;
      %XHTML.global.core.extra.attrib;"
>
]]>

<!ENTITY % XHTML.global.core.attrib "" >


<!ENTITY % lang.attrib
     "xml:lang     %LanguageCode.datatype;  #IMPLIED"
>

<![%XHTML.bidi;[
<!ENTITY % dir.attrib
     "dir          ( ltr | rtl )            #IMPLIED"
>

<!ENTITY % I18n.attrib
     "%dir.attrib;
      %lang.attrib;"
>

<![%XHTML.global.attrs.prefixed;[
<!ENTITY XHTML.global.i18n.attrib
     "%XHTML.prefix;:dir          ( ltr | rtl )            #IMPLIED
      %lang.attrib;"
>
]]>
<!ENTITY XHTML.global.i18n.attrib "" >

]]>
<!ENTITY % I18n.attrib
     "%lang.attrib;"
>
<!ENTITY % XHTML.global.i18n.attrib
     "%lang.attrib;"
>

<!ENTITY % Common.extra.attrib "" >
<!ENTITY % XHTML.global.common.extra.attrib "" >

<!-- intrinsic event attributes declared previously
-->
<!ENTITY % Events.attrib "" >

<!ENTITY % XHTML.global.events.attrib "" >

<!ENTITY % Common.attrib
     "%Core.attrib;
      %I18n.attrib;
      %Events.attrib;
      %Common.extra.attrib;"
>

<!ENTITY % XHTML.global.common.attrib
     "%XHTML.xmlns.attrib.prefixed;
      %XHTML.global.core.attrib;
	  %XHTML.global.i18n.attrib;
	  %XHTML.global.events.attrib;
	  %XHTML.global.common.extra.attrib;"
>

<!-- end of xhtml-attribs-1.mod -->
