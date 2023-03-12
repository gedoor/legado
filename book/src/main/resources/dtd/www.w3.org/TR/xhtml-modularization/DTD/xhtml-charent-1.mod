<!-- ...................................................................... -->
<!-- XHTML Character Entities Module  ......................................... -->
<!-- file: xhtml-charent-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-charent-1.mod,v 1.1 2010/07/29 13:42:46 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ENTITIES XHTML Character Entities 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-charent-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Character Entities for XHTML

     This module declares the set of character entities for XHTML,
     including the Latin 1, Symbol and Special character collections.
-->

<!ENTITY % xhtml-lat1
    PUBLIC "-//W3C//ENTITIES Latin 1 for XHTML//EN"
           "xhtml-lat1.ent" >
%xhtml-lat1;

<!ENTITY % xhtml-symbol
    PUBLIC "-//W3C//ENTITIES Symbols for XHTML//EN"
           "xhtml-symbol.ent" >
%xhtml-symbol;

<!ENTITY % xhtml-special
    PUBLIC "-//W3C//ENTITIES Special for XHTML//EN"
           "xhtml-special.ent" >
%xhtml-special;

<!-- end of xhtml-charent-1.mod -->
