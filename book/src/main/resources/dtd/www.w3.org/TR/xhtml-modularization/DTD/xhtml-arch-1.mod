<!-- ...................................................................... -->
<!-- XHTML Base Architecture Module  ...................................... -->
<!-- file: xhtml-arch-1.mod

     This is XHTML, a reformulation of HTML as a modular XML application.
     Copyright 1998-2005 W3C (MIT, ERCIM, Keio), All Rights Reserved.
     Revision: $Id: xhtml-arch-1.mod,v 1.1 2010/07/29 13:42:46 bertails Exp $ SMI

     This DTD module is identified by the PUBLIC and SYSTEM identifiers:

       PUBLIC "-//W3C//ELEMENTS XHTML Base Architecture 1.0//EN"
       SYSTEM "http://www.w3.org/MarkUp/DTD/xhtml-arch-1.mod"

     Revisions:
     (none)
     ....................................................................... -->

<!-- This optional module includes declarations that enable XHTML to be used
     as a base architecture according to the 'Architectural Forms Definition
     Requirements' (Annex A.3, ISO/IEC 10744, 2nd edition). For more information
     on use of architectural forms, see the HyTime web site at:

         http://www.hytime.org/
-->

<?IS10744 ArcBase xhtml ?>

<!NOTATION xhtml PUBLIC "-//W3C//NOTATION AFDR ARCBASE XHTML 1.1//EN" >

<!-- Entity declaration for associated Architectural DTD
-->
<!ENTITY xhtml-arch.dtd
      PUBLIC "-//W3C//DTD XHTML Architecture 1.1//EN"
             "xhtml11-arch.dtd" >

<?IS10744:arch xhtml
    public-id       =  "-//W3C//NOTATION AFDR ARCBASE XHTML 1.1//EN"
    dtd-public-id   =  "-//W3C//DTD XHTML 1.1//EN"
    dtd-system-id   =  "xhtml11.dtd"
    doc-elem-form   =  "html"
    form-att        =  "html"
    renamer-att     =  "htnames"
    suppressor-att  =  "htsupp"
    data-ignore-att =  "htign"
    auto            =  "ArcAuto"
    options         =  "HtModReq HtModOpt"
    HtModReq        =  "Framework Text Hypertext Lists Structure"
    HtModOpt        =  "Standard"
?>

<!-- end of xhtml-arch-1.mod -->
