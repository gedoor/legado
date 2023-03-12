<!-- ...................................................................... -->
<!-- XHTML Inline Style Module  ........................................... -->
<!-- file: xhtml-inlstyle-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-inlstyle-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ENTITIES XHTML Inline Style 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-inlstyle-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Inline Style

     This module declares the 'style' attribute, used to support inline
     style markup. This module must be instantiated prior to the XHTML
     Common Attributes module in order to be included in %Core.attrib;.
-->

<!ENTITY % style.attrib
     "style        CDATA                    #IMPLIED"
>


<!ENTITY % Core.extra.attrib
     "%style.attrib;"
>

<!-- end of xhtml-inlstyle-1.mod -->
