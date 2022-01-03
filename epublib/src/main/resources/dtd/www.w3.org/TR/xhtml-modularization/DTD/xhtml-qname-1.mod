<!-- ....................................................................... -->
<!-- XHTML Qname Module  ................................................... -->
<!-- file: xhtml-qname-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-qname-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ENTITIES XHTML Qualified Names 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-qname-1.mod"

     Revisions:
	   #2000-10-22: added qname declarations for ruby elements
     ....................................................................... -->

<!-- XHTML Qname (Qualified Name) Module

     This module is contained in two parts, labeled Section 'A' and 'B':

       Section A declares parameter entities to support namespace-
       qualified names, namespace declarations, and name prefixing
       for XHTML and extensions.

       Section B declares parameter entities used to provide
       namespace-qualified names for all XHTML element types:

         %applet.qname;   the xmlns-qualified name for <applet>
         %base.qname;     the xmlns-qualified name for <base>
         ...

     XHTML extensions would create a module similar to this one.
     Included in the XHTML distribution is a template module
     ('template-qname-1.mod') suitable for this purpose.
-->

<!-- Section A: XHTML XML Namespace Framework :::::::::::::::::::: -->

<!-- 1. Declare a %XHTML.prefixed; conditional section keyword, used
        to activate namespace prefixing. The default value should
        inherit '%NS.prefixed;' from the DTD driver, so that unless
        overridden, the default behaviour follows the overall DTD
        prefixing scheme.
-->
<!ENTITY % NS.prefixed "IGNORE" >
<!ENTITY % XHTML.prefixed "%NS.prefixed;" >

<!-- By default, we always permit XHTML attribute collections to have
     namespace-qualified prefixes as well.
-->
<!ENTITY % XHTML.global.attrs.prefixed "INCLUDE" >
<!-- By default, we allow the XML Schema attributes on the root
     element.
-->
<!ENTITY % XHTML.xsi.attrs "INCLUDE" >

<!-- 2. Declare a parameter entity (eg., %XHTML.xmlns;) containing
        the URI reference used to identify the XHTML namespace:
-->
<!ENTITY % XHTML.xmlns  "http://www.w3.org/1999/xhtml" >

<!-- 3. Declare parameter entities (eg., %XHTML.prefix;) containing
        the default namespace prefix string(s) to use when prefixing
        is enabled. This may be overridden in the DTD driver or the
        internal subset of an document instance. If no default prefix
        is desired, this may be declared as an empty string.

     NOTE: As specified in [XMLNAMES], the namespace prefix serves
     as a proxy for the URI reference, and is not in itself significant.
-->
<!ENTITY % XHTML.prefix  "xhtml" >

<!-- 4. Declare parameter entities (eg., %XHTML.pfx;) containing the
        colonized prefix(es) (eg., '%XHTML.prefix;:') used when
        prefixing is active, an empty string when it is not.
-->
<![%XHTML.prefixed;[
<!ENTITY % XHTML.pfx  "%XHTML.prefix;:" >
]]>
<!ENTITY % XHTML.pfx  "" >

<!-- declare qualified name extensions here ............ -->
<!ENTITY % xhtml-qname-extra.mod "" >
%xhtml-qname-extra.mod;

<!-- 5. The parameter entity %XHTML.xmlns.extra.attrib; may be
        redeclared to contain any non-XHTML namespace declaration
        attributes for namespaces embedded in XHTML. The default
        is an empty string.  XLink should be included here if used
        in the DTD.
-->
<!ENTITY % XHTML.xmlns.extra.attrib "" >

<!-- The remainder of Section A is only followed in XHTML, not extensions. -->

<!-- Declare a parameter entity %NS.decl.attrib; containing
     all XML Namespace declarations used in the DTD, plus the
     xmlns declaration for XHTML, its form dependent on whether
     prefixing is active.
-->
<!ENTITY % XHTML.xmlns.attrib.prefixed
     "xmlns:%XHTML.prefix;  %URI.datatype;   #FIXED '%XHTML.xmlns;'"
>
<![%XHTML.prefixed;[
<!ENTITY % NS.decl.attrib
     "%XHTML.xmlns.attrib.prefixed;
      %XHTML.xmlns.extra.attrib;"
>
]]>
<!ENTITY % NS.decl.attrib
     "%XHTML.xmlns.extra.attrib;"
>

<!-- Declare a parameter entity %XSI.prefix as a prefix to use for XML
     Schema Instance attributes.
-->
<!ENTITY % XSI.prefix "xsi" >

<!ENTITY % XSI.xmlns "http://www.w3.org/2001/XMLSchema-instance" >

<!-- Declare a parameter entity %XSI.xmlns.attrib as support for the
     schemaLocation attribute, since this is legal throughout the DTD.
-->
<!ENTITY % XSI.xmlns.attrib
     "xmlns:%XSI.prefix;  %URI.datatype;   #FIXED '%XSI.xmlns;'" >

<!-- This is a placeholder for future XLink support.
-->
<!ENTITY % XLINK.xmlns.attrib "" >

<!-- This is the attribute for the XML Schema namespace - XHTML
     Modularization is also expressed in XML Schema, and it needs to
	 be legal to declare the XML Schema namespace and the
	 schemaLocation attribute on the root element of XHTML family
	 documents.
-->
<![%XHTML.xsi.attrs;[
<!ENTITY % XSI.prefix "xsi" >
<!ENTITY % XSI.pfx "%XSI.prefix;:" >
<!ENTITY % XSI.xmlns "http://www.w3.org/2001/XMLSchema-instance" >

<!ENTITY % XSI.xmlns.attrib
     "xmlns:%XSI.prefix;  %URI.datatype;    #FIXED '%XSI.xmlns;'"
>
]]>
<!ENTITY % XSI.prefix "" >
<!ENTITY % XSI.pfx "" >
<!ENTITY % XSI.xmlns.attrib "" >


<!-- Declare a parameter entity %NS.decl.attrib; containing all
     XML namespace declaration attributes used by XHTML, including
     a default xmlns attribute when prefixing is inactive.
-->
<![%XHTML.prefixed;[
<!ENTITY % XHTML.xmlns.attrib
     "%NS.decl.attrib;
      %XSI.xmlns.attrib;
      %XLINK.xmlns.attrib;"
>
]]>
<!ENTITY % XHTML.xmlns.attrib
     "xmlns        %URI.datatype;           #FIXED '%XHTML.xmlns;'
      %NS.decl.attrib;
      %XSI.xmlns.attrib;
      %XLINK.xmlns.attrib;"
>

<!-- placeholder for qualified name redeclarations -->
<!ENTITY % xhtml-qname.redecl "" >
%xhtml-qname.redecl;

<!-- Section B: XHTML Qualified Names ::::::::::::::::::::::::::::: -->

<!-- 6. This section declares parameter entities used to provide
        namespace-qualified names for all XHTML element types.
-->

<!-- module:  xhtml-applet-1.mod -->
<!ENTITY % applet.qname  "%XHTML.pfx;applet" >

<!-- module:  xhtml-base-1.mod -->
<!ENTITY % base.qname    "%XHTML.pfx;base" >

<!-- module:  xhtml-bdo-1.mod -->
<!ENTITY % bdo.qname     "%XHTML.pfx;bdo" >

<!-- module:  xhtml-blkphras-1.mod -->
<!ENTITY % address.qname "%XHTML.pfx;address" >
<!ENTITY % blockquote.qname  "%XHTML.pfx;blockquote" >
<!ENTITY % pre.qname     "%XHTML.pfx;pre" >
<!ENTITY % h1.qname      "%XHTML.pfx;h1" >
<!ENTITY % h2.qname      "%XHTML.pfx;h2" >
<!ENTITY % h3.qname      "%XHTML.pfx;h3" >
<!ENTITY % h4.qname      "%XHTML.pfx;h4" >
<!ENTITY % h5.qname      "%XHTML.pfx;h5" >
<!ENTITY % h6.qname      "%XHTML.pfx;h6" >

<!-- module:  xhtml-blkpres-1.mod -->
<!ENTITY % hr.qname      "%XHTML.pfx;hr" >

<!-- module:  xhtml-blkstruct-1.mod -->
<!ENTITY % div.qname     "%XHTML.pfx;div" >
<!ENTITY % p.qname       "%XHTML.pfx;p" >

<!-- module:  xhtml-edit-1.mod -->
<!ENTITY % ins.qname     "%XHTML.pfx;ins" >
<!ENTITY % del.qname     "%XHTML.pfx;del" >

<!-- module:  xhtml-form-1.mod -->
<!ENTITY % form.qname    "%XHTML.pfx;form" >
<!ENTITY % label.qname   "%XHTML.pfx;label" >
<!ENTITY % input.qname   "%XHTML.pfx;input" >
<!ENTITY % select.qname  "%XHTML.pfx;select" >
<!ENTITY % optgroup.qname  "%XHTML.pfx;optgroup" >
<!ENTITY % option.qname  "%XHTML.pfx;option" >
<!ENTITY % textarea.qname  "%XHTML.pfx;textarea" >
<!ENTITY % fieldset.qname  "%XHTML.pfx;fieldset" >
<!ENTITY % legend.qname  "%XHTML.pfx;legend" >
<!ENTITY % button.qname  "%XHTML.pfx;button" >

<!-- module:  xhtml-hypertext-1.mod -->
<!ENTITY % a.qname       "%XHTML.pfx;a" >

<!-- module:  xhtml-image-1.mod -->
<!ENTITY % img.qname     "%XHTML.pfx;img" >

<!-- module:  xhtml-inlphras-1.mod -->
<!ENTITY % abbr.qname    "%XHTML.pfx;abbr" >
<!ENTITY % acronym.qname "%XHTML.pfx;acronym" >
<!ENTITY % cite.qname    "%XHTML.pfx;cite" >
<!ENTITY % code.qname    "%XHTML.pfx;code" >
<!ENTITY % dfn.qname     "%XHTML.pfx;dfn" >
<!ENTITY % em.qname      "%XHTML.pfx;em" >
<!ENTITY % kbd.qname     "%XHTML.pfx;kbd" >
<!ENTITY % q.qname       "%XHTML.pfx;q" >
<!ENTITY % samp.qname    "%XHTML.pfx;samp" >
<!ENTITY % strong.qname  "%XHTML.pfx;strong" >
<!ENTITY % var.qname     "%XHTML.pfx;var" >

<!-- module:  xhtml-inlpres-1.mod -->
<!ENTITY % b.qname       "%XHTML.pfx;b" >
<!ENTITY % big.qname     "%XHTML.pfx;big" >
<!ENTITY % i.qname       "%XHTML.pfx;i" >
<!ENTITY % small.qname   "%XHTML.pfx;small" >
<!ENTITY % sub.qname     "%XHTML.pfx;sub" >
<!ENTITY % sup.qname     "%XHTML.pfx;sup" >
<!ENTITY % tt.qname      "%XHTML.pfx;tt" >

<!-- module:  xhtml-inlstruct-1.mod -->
<!ENTITY % br.qname      "%XHTML.pfx;br" >
<!ENTITY % span.qname    "%XHTML.pfx;span" >

<!-- module:  xhtml-ismap-1.mod (also csismap, ssismap) -->
<!ENTITY % map.qname     "%XHTML.pfx;map" >
<!ENTITY % area.qname    "%XHTML.pfx;area" >

<!-- module:  xhtml-link-1.mod -->
<!ENTITY % link.qname    "%XHTML.pfx;link" >

<!-- module:  xhtml-list-1.mod -->
<!ENTITY % dl.qname      "%XHTML.pfx;dl" >
<!ENTITY % dt.qname      "%XHTML.pfx;dt" >
<!ENTITY % dd.qname      "%XHTML.pfx;dd" >
<!ENTITY % ol.qname      "%XHTML.pfx;ol" >
<!ENTITY % ul.qname      "%XHTML.pfx;ul" >
<!ENTITY % li.qname      "%XHTML.pfx;li" >

<!-- module:  xhtml-meta-1.mod -->
<!ENTITY % meta.qname    "%XHTML.pfx;meta" >

<!-- module:  xhtml-param-1.mod -->
<!ENTITY % param.qname   "%XHTML.pfx;param" >

<!-- module:  xhtml-object-1.mod -->
<!ENTITY % object.qname  "%XHTML.pfx;object" >

<!-- module:  xhtml-script-1.mod -->
<!ENTITY % script.qname  "%XHTML.pfx;script" >
<!ENTITY % noscript.qname  "%XHTML.pfx;noscript" >

<!-- module:  xhtml-struct-1.mod -->
<!ENTITY % html.qname    "%XHTML.pfx;html" >
<!ENTITY % head.qname    "%XHTML.pfx;head" >
<!ENTITY % title.qname   "%XHTML.pfx;title" >
<!ENTITY % body.qname    "%XHTML.pfx;body" >

<!-- module:  xhtml-style-1.mod -->
<!ENTITY % style.qname   "%XHTML.pfx;style" >

<!-- module:  xhtml-table-1.mod -->
<!ENTITY % table.qname   "%XHTML.pfx;table" >
<!ENTITY % caption.qname "%XHTML.pfx;caption" >
<!ENTITY % thead.qname   "%XHTML.pfx;thead" >
<!ENTITY % tfoot.qname   "%XHTML.pfx;tfoot" >
<!ENTITY % tbody.qname   "%XHTML.pfx;tbody" >
<!ENTITY % colgroup.qname  "%XHTML.pfx;colgroup" >
<!ENTITY % col.qname     "%XHTML.pfx;col" >
<!ENTITY % tr.qname      "%XHTML.pfx;tr" >
<!ENTITY % th.qname      "%XHTML.pfx;th" >
<!ENTITY % td.qname      "%XHTML.pfx;td" >

<!-- module:  xhtml-ruby-1.mod -->

<!ENTITY % ruby.qname    "%XHTML.pfx;ruby" >
<!ENTITY % rbc.qname     "%XHTML.pfx;rbc" >
<!ENTITY % rtc.qname     "%XHTML.pfx;rtc" >
<!ENTITY % rb.qname      "%XHTML.pfx;rb" >
<!ENTITY % rt.qname      "%XHTML.pfx;rt" >
<!ENTITY % rp.qname      "%XHTML.pfx;rp" >

<!-- Provisional XHTML 2.0 Qualified Names  ...................... -->

<!-- module:  xhtml-image-2.mod -->
<!ENTITY % alt.qname     "%XHTML.pfx;alt" >

<!-- end of xhtml-qname-1.mod -->
