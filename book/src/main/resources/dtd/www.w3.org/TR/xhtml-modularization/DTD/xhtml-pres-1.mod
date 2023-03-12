<!-- ...................................................................... -->
<!-- XHTML Presentation Module ............................................ -->
<!-- file: xhtml-pres-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-pres-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Presentation 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-pres-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Presentational Elements

     This module defines elements and their attributes for
     simple presentation-related markup.
-->

<!ENTITY % xhtml-inlpres.module "INCLUDE" >
<![%xhtml-inlpres.module;[
<!ENTITY % xhtml-inlpres.mod
     PUBLIC "-//W3C//ELEMENTS XHTML Inline Presentation 1.0//EN"
            "xhtml-inlpres-1.mod" >
%xhtml-inlpres.mod;]]>

<!ENTITY % xhtml-blkpres.module "INCLUDE" >
<![%xhtml-blkpres.module;[
<!ENTITY % xhtml-blkpres.mod
     PUBLIC "-//W3C//ELEMENTS XHTML Block Presentation 1.0//EN"
            "xhtml-blkpres-1.mod" >
%xhtml-blkpres.mod;]]>

<!-- end of xhtml-pres-1.mod -->
