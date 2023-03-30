<!-- ...................................................................... -->
    <!-- XHTML Embedded Object Module  ........................................ -->
    <!-- file: xhtml-object-1.mod

         This is XHTML, a reformulation of HTML as a modular XML application.
         Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
         Revision: $Id: xhtml-object-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $ SMI

         This DTD module is identified by the PUBLIC and SYSTEM identifiers:

           PUBLIC "-//W3C//ELEMENTS XHTML Embedded Object 1.0//EN"
           SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-object-1.mod"

         Revisions:
         (none)
         ....................................................................... -->

    <!-- Embedded Objects

            object

         This module declares the object element type and its attributes, used
         to embed external objects as part of XHTML pages. In the document,
         place param elements prior to other content within the object element.

         Note that use of this module requires instantiation of the Param
         Element Module.
    -->

    <!-- object: Generic Embedded Object ................... -->

    <!ENTITY % object.element  "INCLUDE" ><![%object.element;[<!ENTITY % object.content"( #PCDATA | %Flow.mix; | %param.qname; )*"><!ENTITY % object.qname  "object" ><!ELEMENT %object.qname;  %object.content; ><!-- end of object.element -->]]>

    <!ENTITY % object.attlist  "INCLUDE" ><![%object.attlist;[<!ATTLIST %object.qname;%Common.attrib;declare      ( declare )              #IMPLIEDclassid      %URI.datatype;           #IMPLIEDcodebase     %URI.datatype;           #IMPLIEDdata         %URI.datatype;           #IMPLIEDtype         %ContentType.datatype;   #IMPLIEDcodetype     %ContentType.datatype;   #IMPLIEDarchive      %URIs.datatype;          #IMPLIEDstandby      %Text.datatype;          #IMPLIEDheight       %Length.datatype;        #IMPLIEDwidth        %Length.datatype;        #IMPLIEDname         CDATA                    #IMPLIEDtabindex     %Number.datatype;        #IMPLIED><!-- end of object.attlist -->]]>

    <!-- end of xhtml-object-1.mod -->
