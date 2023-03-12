<!-- ...................................................................... -->
<!-- XHTML Table Module  .................................................. -->
<!-- file: xhtml-table-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-table-1.mod,v 1.1 2010/07/29 13:42:48 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Tables 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-table-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- Tables

        table, caption, thead, tfoot, tbody, colgroup, col, tr, th, td

     This module declares element types and attributes used to provide
     table markup similar to HTML 4, including features that enable
     better accessibility for non-visual user agents.
-->

<!-- declare qualified element type names:
-->
<!ENTITY % table.qname  "table" >
<!ENTITY % caption.qname  "caption" >
<!ENTITY % thead.qname  "thead" >
<!ENTITY % tfoot.qname  "tfoot" >
<!ENTITY % tbody.qname  "tbody" >
<!ENTITY % colgroup.qname  "colgroup" >
<!ENTITY % col.qname  "col" >
<!ENTITY % tr.qname  "tr" >
<!ENTITY % th.qname  "th" >
<!ENTITY % td.qname  "td" >

<!-- The frame attribute specifies which parts of the frame around
     the table should be rendered. The values are not the same as
     CALS to avoid a name clash with the valign attribute.
-->
<!ENTITY % frame.attrib
     "frame        ( void
                   | above
                   | below
                   | hsides
                   | lhs
                   | rhs
                   | vsides
                   | box
                   | border )               #IMPLIED"
>

<!-- The rules attribute defines which rules to draw between cells:

     If rules is absent then assume:

       "none" if border is absent or border="0" otherwise "all"
-->
<!ENTITY % rules.attrib
     "rules        ( none
                   | groups
                   | rows
                   | cols
                   | all )                  #IMPLIED"
>

<!-- horizontal alignment attributes for cell contents
-->
<!ENTITY % CellHAlign.attrib
     "align        ( left
                   | center
                   | right
                   | justify
                   | char )                 #IMPLIED
      char         %Character.datatype;     #IMPLIED
      charoff      %Length.datatype;        #IMPLIED"
>

<!-- vertical alignment attribute for cell contents
-->
<!ENTITY % CellVAlign.attrib
     "valign       ( top
                   | middle
                   | bottom
                   | baseline )             #IMPLIED"
>

<!-- scope is simpler than axes attribute for common tables
-->
<!ENTITY % scope.attrib
     "scope        ( row
                   | col
                   | rowgroup
                   | colgroup )             #IMPLIED"
>

<!-- table: Table Element .............................. -->

<!ENTITY % table.element  "INCLUDE" >
<![%table.element;[
<!ENTITY % table.content
     "( %caption.qname;?, ( %col.qname;* | %colgroup.qname;* ),
      (( %thead.qname;?, %tfoot.qname;?, %tbody.qname;+ ) | ( %tr.qname;+ )))"
>
<!ELEMENT %table.qname;  %table.content; >
<!-- end of table.element -->]]>

<!ENTITY % table.attlist  "INCLUDE" >
<![%table.attlist;[
<!ATTLIST %table.qname;
      %Common.attrib;
      summary      %Text.datatype;          #IMPLIED
      width        %Length.datatype;        #IMPLIED
      border       %Pixels.datatype;        #IMPLIED
      %frame.attrib;
      %rules.attrib;
      cellspacing  %Length.datatype;        #IMPLIED
      cellpadding  %Length.datatype;        #IMPLIED
>
<!-- end of table.attlist -->]]>

<!-- caption: Table Caption ............................ -->

<!ENTITY % caption.element  "INCLUDE" >
<![%caption.element;[
<!ENTITY % caption.content
     "( #PCDATA | %Inline.mix; )*"
>
<!ELEMENT %caption.qname;  %caption.content; >
<!-- end of caption.element -->]]>

<!ENTITY % caption.attlist  "INCLUDE" >
<![%caption.attlist;[
<!ATTLIST %caption.qname;
      %Common.attrib;
>
<!-- end of caption.attlist -->]]>

<!-- thead: Table Header ............................... -->

<!-- Use thead to duplicate headers when breaking table
     across page boundaries, or for static headers when
     tbody sections are rendered in scrolling panel.
-->

<!ENTITY % thead.element  "INCLUDE" >
<![%thead.element;[
<!ENTITY % thead.content  "( %tr.qname; )+" >
<!ELEMENT %thead.qname;  %thead.content; >
<!-- end of thead.element -->]]>

<!ENTITY % thead.attlist  "INCLUDE" >
<![%thead.attlist;[
<!ATTLIST %thead.qname;
      %Common.attrib;
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of thead.attlist -->]]>

<!-- tfoot: Table Footer ............................... -->

<!-- Use tfoot to duplicate footers when breaking table
     across page boundaries, or for static footers when
     tbody sections are rendered in scrolling panel.
-->

<!ENTITY % tfoot.element  "INCLUDE" >
<![%tfoot.element;[
<!ENTITY % tfoot.content  "( %tr.qname; )+" >
<!ELEMENT %tfoot.qname;  %tfoot.content; >
<!-- end of tfoot.element -->]]>

<!ENTITY % tfoot.attlist  "INCLUDE" >
<![%tfoot.attlist;[
<!ATTLIST %tfoot.qname;
      %Common.attrib;
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of tfoot.attlist -->]]>

<!-- tbody: Table Body ................................. -->

<!-- Use multiple tbody sections when rules are needed
     between groups of table rows.
-->

<!ENTITY % tbody.element  "INCLUDE" >
<![%tbody.element;[
<!ENTITY % tbody.content  "( %tr.qname; )+" >
<!ELEMENT %tbody.qname;  %tbody.content; >
<!-- end of tbody.element -->]]>

<!ENTITY % tbody.attlist  "INCLUDE" >
<![%tbody.attlist;[
<!ATTLIST %tbody.qname;
      %Common.attrib;
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of tbody.attlist -->]]>

<!-- colgroup: Table Column Group ...................... -->

<!-- colgroup groups a set of col elements. It allows you
     to group several semantically-related columns together.
-->

<!ENTITY % colgroup.element  "INCLUDE" >
<![%colgroup.element;[
<!ENTITY % colgroup.content  "( %col.qname; )*" >
<!ELEMENT %colgroup.qname;  %colgroup.content; >
<!-- end of colgroup.element -->]]>

<!ENTITY % colgroup.attlist  "INCLUDE" >
<![%colgroup.attlist;[
<!ATTLIST %colgroup.qname;
      %Common.attrib;
      span         %Number.datatype;        '1'
      width        %MultiLength.datatype;   #IMPLIED
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of colgroup.attlist -->]]>

<!-- col: Table Column ................................. -->

<!-- col elements define the alignment properties for
     cells in one or more columns.

     The width attribute specifies the width of the
     columns, e.g.

       width="64"        width in screen pixels
       width="0.5*"      relative width of 0.5

     The span attribute causes the attributes of one
     col element to apply to more than one column.
-->

<!ENTITY % col.element  "INCLUDE" >
<![%col.element;[
<!ENTITY % col.content  "EMPTY" >
<!ELEMENT %col.qname;  %col.content; >
<!-- end of col.element -->]]>

<!ENTITY % col.attlist  "INCLUDE" >
<![%col.attlist;[
<!ATTLIST %col.qname;
      %Common.attrib;
      span         %Number.datatype;        '1'
      width        %MultiLength.datatype;   #IMPLIED
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of col.attlist -->]]>

<!-- tr: Table Row ..................................... -->

<!ENTITY % tr.element  "INCLUDE" >
<![%tr.element;[
<!ENTITY % tr.content  "( %th.qname; | %td.qname; )+" >
<!ELEMENT %tr.qname;  %tr.content; >
<!-- end of tr.element -->]]>

<!ENTITY % tr.attlist  "INCLUDE" >
<![%tr.attlist;[
<!ATTLIST %tr.qname;
      %Common.attrib;
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of tr.attlist -->]]>

<!-- th: Table Header Cell ............................. -->

<!-- th is for header cells, td for data,
     but for cells acting as both use td
-->

<!ENTITY % th.element  "INCLUDE" >
<![%th.element;[
<!ENTITY % th.content
     "( #PCDATA | %Flow.mix; )*"
>
<!ELEMENT %th.qname;  %th.content; >
<!-- end of th.element -->]]>

<!ENTITY % th.attlist  "INCLUDE" >
<![%th.attlist;[
<!ATTLIST %th.qname;
      %Common.attrib;
      abbr         %Text.datatype;          #IMPLIED
      axis         CDATA                    #IMPLIED
      headers      IDREFS                   #IMPLIED
      %scope.attrib;
      rowspan      %Number.datatype;        '1'
      colspan      %Number.datatype;        '1'
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of th.attlist -->]]>

<!-- td: Table Data Cell ............................... -->

<!ENTITY % td.element  "INCLUDE" >
<![%td.element;[
<!ENTITY % td.content
     "( #PCDATA | %Flow.mix; )*"
>
<!ELEMENT %td.qname;  %td.content; >
<!-- end of td.element -->]]>

<!ENTITY % td.attlist  "INCLUDE" >
<![%td.attlist;[
<!ATTLIST %td.qname;
      %Common.attrib;
      abbr         %Text.datatype;          #IMPLIED
      axis         CDATA                    #IMPLIED
      headers      IDREFS                   #IMPLIED
      %scope.attrib;
      rowspan      %Number.datatype;        '1'
      colspan      %Number.datatype;        '1'
      %CellHAlign.attrib;
      %CellVAlign.attrib;
>
<!-- end of td.attlist -->]]>

<!-- end of xhtml-table-1.mod -->
