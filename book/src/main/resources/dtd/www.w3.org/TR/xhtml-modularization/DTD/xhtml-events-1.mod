<!-- ...................................................................... -->
<!-- XHTML Intrinsic Events Module  ....................................... -->
<!-- file: xhtml-events-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-events-1.mod,v 1.1 2010/07/29 13:42:47 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ENTITIES XHTML Intrinsic Events 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-events-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Intrinsic Event Attributes

     These are the event attributes defined in HTML 4,
     Section 18.2.3 "Intrinsic Events". This module must be
     instantiated prior to the Attributes Module but after
     the Datatype Module in the Modular Framework module.

    "Note: Authors of HTML documents are advised that changes
     are likely to occur in the realm of intrinsic events
     (e.g., how scripts are bound to events). Research in
     this realm is carried on by members of the W3C Document
     Object Model Working Group (see the W3C Web site at
     http://www.w3.org/ for more information)."
-->
<!-- NOTE: Because the ATTLIST declarations in this module occur
     before their respective ELEMENT declarations in other
     modules, there may be a dependency on this module that
     should be considered if any of the parameter entities used
     for element type names (eg., %a.qname;) are redeclared.
-->

<!ENTITY % Events.attrib
     "onclick      %Script.datatype;        #IMPLIED
      ondblclick   %Script.datatype;        #IMPLIED
      onmousedown  %Script.datatype;        #IMPLIED
      onmouseup    %Script.datatype;        #IMPLIED
      onmouseover  %Script.datatype;        #IMPLIED
      onmousemove  %Script.datatype;        #IMPLIED
      onmouseout   %Script.datatype;        #IMPLIED
      onkeypress   %Script.datatype;        #IMPLIED
      onkeydown    %Script.datatype;        #IMPLIED
      onkeyup      %Script.datatype;        #IMPLIED"
>

<![%XHTML.global.attrs.prefixed;[
<!ENTITY % XHTML.global.events.attrib
     "%XHTML.prefix;:onclick      %Script.datatype;        #IMPLIED
      %XHTML.prefix;:ondblclick   %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onmousedown  %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onmouseup    %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onmouseover  %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onmousemove  %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onmouseout   %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onkeypress   %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onkeydown    %Script.datatype;        #IMPLIED
      %XHTML.prefix;:onkeyup      %Script.datatype;        #IMPLIED"
>
]]>

<!-- additional attributes on anchor element
-->
<!ATTLIST %a.qname;
     onfocus      %Script.datatype;         #IMPLIED
     onblur       %Script.datatype;         #IMPLIED
>

<!-- additional attributes on form element
-->
<!ATTLIST %form.qname;
      onsubmit     %Script.datatype;        #IMPLIED
      onreset      %Script.datatype;        #IMPLIED
>

<!-- additional attributes on label element
-->
<!ATTLIST %label.qname;
      onfocus      %Script.datatype;        #IMPLIED
      onblur       %Script.datatype;        #IMPLIED
>

<!-- additional attributes on input element
-->
<!ATTLIST %input.qname;
      onfocus      %Script.datatype;        #IMPLIED
      onblur       %Script.datatype;        #IMPLIED
      onselect     %Script.datatype;        #IMPLIED
      onchange     %Script.datatype;        #IMPLIED
>

<!-- additional attributes on select element
-->
<!ATTLIST %select.qname;
      onfocus      %Script.datatype;        #IMPLIED
      onblur       %Script.datatype;        #IMPLIED
      onchange     %Script.datatype;        #IMPLIED
>

<!-- additional attributes on textarea element
-->
<!ATTLIST %textarea.qname;
      onfocus      %Script.datatype;        #IMPLIED
      onblur       %Script.datatype;        #IMPLIED
      onselect     %Script.datatype;        #IMPLIED
      onchange     %Script.datatype;        #IMPLIED
>

<!-- additional attributes on button element
-->
<!ATTLIST %button.qname;
      onfocus      %Script.datatype;        #IMPLIED
      onblur       %Script.datatype;        #IMPLIED
>

<!-- additional attributes on body element
-->
<!ATTLIST %body.qname;
      onload       %Script.datatype;        #IMPLIED
      onunload     %Script.datatype;        #IMPLIED
>

<!-- additional attributes on area element
-->
<!ATTLIST %area.qname;
      onfocus      %Script.datatype;        #IMPLIED
      onblur       %Script.datatype;        #IMPLIED
>

<!-- end of xhtml-events-1.mod -->
