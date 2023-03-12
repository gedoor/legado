<!-- ...................................................................... -->
<!-- XHTML Param Element Module  ..................................... -->
<!-- file: xhtml-param-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-param-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Param Element 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-param-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Parameters for Java Applets and Embedded Objects

        param

     This module provides declarations for the param element,
     used to provide named property values for the applet
     and object elements.
-->

<!-- param: Named Property Value ....................... -->

<!ENTITY % param.element  "INCLUDE" >
<![%param.element;[
<!ENTITY % param.content  "EMPTY" >
<!ENTITY % param.qname  "param" >
<!ELEMENT %param.qname;  %param.content; >
<!-- end of param.element -->]]>

<!ENTITY % param.attlist  "INCLUDE" >
<![%param.attlist;[
<!ATTLIST %param.qname;
      %XHTML.xmlns.attrib;
      %id.attrib;
      name         CDATA                    #REQUIRED
      value        CDATA                    #IMPLIED
      valuetype    ( data | ref | object )  'data'
      type         %ContentType.datatype;   #IMPLIED
>
<!-- end of param.attlist -->]]>

<!-- end of xhtml-param-1.mod -->
