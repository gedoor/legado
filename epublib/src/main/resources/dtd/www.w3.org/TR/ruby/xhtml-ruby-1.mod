<!-- ...................................................................... -->
<!-- XHTML Ruby Module .................................................... -->
<!-- file: xhtml-ruby-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1999-2001 W3C (MIT, INRIA, Keio), All Rights Reserved.
     Revision: $Id: xhtml-ruby-1.mod,v 4.0 2001/04/03 23:14:33 altheim Exp $

     This module is based on the W3C Ruby Annotation Specification:

        http://www.w3.org/TR/ruby

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Ruby 1.0//EN"
       SYSTEM "http://www.w3.org/TR/ruby/xhtml-ruby-1.mod"

     ...................................................................... -->

<!-- Ruby Elements

        ruby, rbc, rtc, rb, rt, rp

     This module declares the elements and their attributes used to
     support ruby annotation markup.
-->

<!-- declare qualified element type names:
-->
<!ENTITY % ruby.qname  "ruby" >
<!ENTITY % rbc.qname  "rbc" >
<!ENTITY % rtc.qname  "rtc" >
<!ENTITY % rb.qname  "rb" >
<!ENTITY % rt.qname  "rt" >
<!ENTITY % rp.qname  "rp" >

<!-- rp fallback is included by default.
-->
<!ENTITY % Ruby.fallback "INCLUDE" >
<!ENTITY % Ruby.fallback.mandatory "IGNORE" >

<!-- Complex ruby is included by default; it may be 
     overridden by other modules to ignore it.
-->
<!ENTITY % Ruby.complex "INCLUDE" >

<!-- Fragments for the content model of the ruby element -->
<![%Ruby.fallback;[
<![%Ruby.fallback.mandatory;[
<!ENTITY % Ruby.content.simple 
     "( %rb.qname;, %rp.qname;, %rt.qname;, %rp.qname; )"
>
]]>
<!ENTITY % Ruby.content.simple 
     "( %rb.qname;, ( %rt.qname; | ( %rp.qname;, %rt.qname;, %rp.qname; ) ) )"
>
]]>
<!ENTITY % Ruby.content.simple "( %rb.qname;, %rt.qname; )" >

<![%Ruby.complex;[
<!ENTITY % Ruby.content.complex 
     "| ( %rbc.qname;, %rtc.qname;, %rtc.qname;? )"
>
]]>
<!ENTITY % Ruby.content.complex "" >

<!-- Content models of the rb and the rt elements are intended to
     allow other inline-level elements of its parent markup language,
     but it should not include ruby descendent elements. The following
     parameter entity %NoRuby.content; can be used to redefine
     those content models with minimum effort.  It's defined as
     '( #PCDATA )' by default.
-->
<!ENTITY % NoRuby.content "( #PCDATA )" >

<!-- one or more digits (NUMBER) -->
<!ENTITY % Number.datatype "CDATA" >

<!-- ruby element ...................................... -->

<!ENTITY % ruby.element  "INCLUDE" >
<![%ruby.element;[
<!ENTITY % ruby.content
     "( %Ruby.content.simple; %Ruby.content.complex; )"
>
<!ELEMENT %ruby.qname;  %ruby.content; >
<!-- end of ruby.element -->]]>

<![%Ruby.complex;[
<!-- rbc (ruby base component) element ................. -->

<!ENTITY % rbc.element  "INCLUDE" >
<![%rbc.element;[
<!ENTITY % rbc.content
     "(%rb.qname;)+"
>
<!ELEMENT %rbc.qname;  %rbc.content; >
<!-- end of rbc.element -->]]>

<!-- rtc (ruby text component) element ................. -->

<!ENTITY % rtc.element  "INCLUDE" >
<![%rtc.element;[
<!ENTITY % rtc.content
     "(%rt.qname;)+"
>
<!ELEMENT %rtc.qname;  %rtc.content; >
<!-- end of rtc.element -->]]>
]]>

<!-- rb (ruby base) element ............................ -->

<!ENTITY % rb.element  "INCLUDE" >
<![%rb.element;[
<!-- %rb.content; uses %NoRuby.content; as its content model,
     which is '( #PCDATA )' by default. It may be overridden
     by other modules to allow other inline-level elements
     of its parent markup language, but it should not include
     ruby descendent elements.
-->
<!ENTITY % rb.content "%NoRuby.content;" >
<!ELEMENT %rb.qname;  %rb.content; >
<!-- end of rb.element -->]]>

<!-- rt (ruby text) element ............................ -->

<!ENTITY % rt.element  "INCLUDE" >
<![%rt.element;[
<!-- %rt.content; uses %NoRuby.content; as its content model,
     which is '( #PCDATA )' by default. It may be overridden
     by other modules to allow other inline-level elements
     of its parent markup language, but it should not include
     ruby descendent elements.
-->
<!ENTITY % rt.content "%NoRuby.content;" >

<!ELEMENT %rt.qname;  %rt.content; >
<!-- end of rt.element -->]]>

<!-- rbspan attribute is used for complex ruby only ...... -->
<![%Ruby.complex;[
<!ENTITY % rt.attlist  "INCLUDE" >
<![%rt.attlist;[
<!ATTLIST %rt.qname;
      rbspan         %Number.datatype;      "1"
>
<!-- end of rt.attlist -->]]>
]]>

<!-- rp (ruby parenthesis) element ..................... -->

<![%Ruby.fallback;[
<!ENTITY % rp.element  "INCLUDE" >
<![%rp.element;[
<!ENTITY % rp.content
     "( #PCDATA )"
>
<!ELEMENT %rp.qname;  %rp.content; >
<!-- end of rp.element -->]]>
]]>

<!-- Ruby Common Attributes

     The following optional ATTLIST declarations provide an easy way
     to define common attributes for ruby elements.  These declarations
     are ignored by default.

     Ruby elements are intended to have common attributes of its
     parent markup language.  For example, if a markup language defines
     common attributes as a parameter entity %attrs;, you may add
     those attributes by just declaring the following parameter entities

         <!ENTITY % Ruby.common.attlists  "INCLUDE" >
         <!ENTITY % Ruby.common.attrib  "%attrs;" >

     before including the Ruby module.
-->

<!ENTITY % Ruby.common.attlists  "IGNORE" >
<![%Ruby.common.attlists;[
<!ENTITY % Ruby.common.attrib  "" >

<!-- common attributes for ruby ........................ -->

<!ENTITY % Ruby.common.attlist  "INCLUDE" >
<![%Ruby.common.attlist;[
<!ATTLIST %ruby.qname;
      %Ruby.common.attrib;
>
<!-- end of Ruby.common.attlist -->]]>

<![%Ruby.complex;[
<!-- common attributes for rbc ......................... -->

<!ENTITY % Rbc.common.attlist  "INCLUDE" >
<![%Rbc.common.attlist;[
<!ATTLIST %rbc.qname;
      %Ruby.common.attrib;
>
<!-- end of Rbc.common.attlist -->]]>

<!-- common attributes for rtc ......................... -->

<!ENTITY % Rtc.common.attlist  "INCLUDE" >
<![%Rtc.common.attlist;[
<!ATTLIST %rtc.qname;
      %Ruby.common.attrib;
>
<!-- end of Rtc.common.attlist -->]]>
]]>

<!-- common attributes for rb .......................... -->

<!ENTITY % Rb.common.attlist  "INCLUDE" >
<![%Rb.common.attlist;[
<!ATTLIST %rb.qname;
      %Ruby.common.attrib;
>
<!-- end of Rb.common.attlist -->]]>

<!-- common attributes for rt .......................... -->

<!ENTITY % Rt.common.attlist  "INCLUDE" >
<![%Rt.common.attlist;[
<!ATTLIST %rt.qname;
      %Ruby.common.attrib;
>
<!-- end of Rt.common.attlist -->]]>

<![%Ruby.fallback;[
<!-- common attributes for rp .......................... -->

<!ENTITY % Rp.common.attlist  "INCLUDE" >
<![%Rp.common.attlist;[
<!ATTLIST %rp.qname;
      %Ruby.common.attrib;
>
<!-- end of Rp.common.attlist -->]]>
]]>
]]>

<!-- end of xhtml-ruby-1.mod -->
