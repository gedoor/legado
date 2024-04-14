# [English](English.md) [中文](README.md)

[![icon_android](https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/icon_android.png)](https://play.google.com/store/apps/details?id=io.legado.play.release)
<a href="https://jb.gg/OpenSourceSupport" target="_blank">
<img width="24" height="24" src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*135yekd*_ga*OTY4Mjg4NDYzLjE2Mzk0NTE3MzQ.*_ga_9J976DJZ68*MTY2OTE2MzM5Ny4xMy4wLjE2NjkxNjMzOTcuNjAuMC4w&_ga=2.257292110.451256242.1669085120-968288463.1639451734" alt="idea"/>
</a>
<div align="center">
<img width="125" height="125" src="https://github.com/gedoor/legado/raw/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="legado"/>  
  
Legado / 开源阅读
<br>
<a href="https://gedoor.github.io" target="_blank">gedoor.github.io</a> / <a href="https://www.legado.top/" target="_blank">legado.top</a>
<br>
Legado is a free and open source novel reader for Android.
</div>

[![](https://img.shields.io/badge/-Contents:-696969.svg)](#contents) [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-) [![](https://img.shields.io/badge/-Download-F5F5F5.svg)](#Download-) [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-) [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-) [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-) [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-) [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-)

>New user?
>
>The software does not provide content, you need to add it manually, such as importing book sources, etc. 
>Take a look at [official help documentation](https://www.yuque.com/legado/wiki)，Maybe there's an answer you need inside.

# Function [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-)

You can customize the book source, set your own rules, and capture web page data. The rules are simple and easy to understand. There are rules in the software. List bookshelf, grid bookshelf switch freely. The book source rules support search and discovery, and all the functions of finding books and reading books are all customized, making it easier to find books.
* Custom ebook sources, set your own rules to capture web data, the rules are simple and easy to understand, the software has a rule description.
* eBook sources rules support search and discovery, all find books and read books function all custom, find books more convenient.
* Schedule updating your library for new chapters.
* Online reading from web sources that can be imported in bulk
* Local reading of Auto-download episodes.
* Local reading of TXT or EPUB files
* ebook Wishlist
* Big text viewer. You can open eBook and txt in 1GB size
* Automatic text replacement for removing ad in content
* List bookshelf, grid bookshelf free to switch.
* Subscription content, you can subscribe to any content you want to see, see what you want to see
* A configurable reader with fonts, background, page transitions mode and other settings
* Timer. Set interval time to listen ebook, time up, ebook  turn off completely.
* TTS book reader. tts can optionally be install“smartvoice-4.1.0” or ”Speech Services by Google“  Give your baby a storybook to listen to and teach your baby to talk, 
* Dark mode and E-Ink mode support and Web service support
* Create backups to local or WebDav server
* Decentralization web3
* Support replacement purification, it is very convenient to remove the content of advertisement replacement.
* Support local TXT, EPUB reading, manual browsing, intelligent scanning.
* Support highly customized reading interface, switch font, color, background, line spacing, paragraph spacing, bold, simplified and traditional conversion.
* Support multiple page turning modes, covering, emulating, sliding, scrolling, etc.


<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>

# Download [![](https://img.shields.io/badge/-Download-F5F5F5.svg)](#Download-)

#### Android

* [Releases](https://github.com/gedoor/legado/releases/latest)
* [Google play - $1.99](https://play.google.com/store/apps/details?id=io.legado.play.release)
* [Coolapk](https://www.coolapk.com/apk/io.legado.app.release)
* [\#Beta](https://kunfei.lanzoui.com/b0f810h4b)
* [IzzyOnDroid F-Droid Repository](https://apt.izzysoft.de/fdroid/index/apk/io.legado.app.release)


#### IOS

* Stopped(No release) - [Github](https://github.com/gedoor/YueDuFlutter)

<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>

# Community [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-)

#### Telegram

[![Telegram-group](https://img.shields.io/badge/Telegram-group-blue)](https://t.me/yueduguanfang) [![Telegram-channel](https://img.shields.io/badge/Telegram-channel-blue)](https://t.me/legado_channels)

#### Discord

[![Discord](https://img.shields.io/discord/560731361414086666?color=%235865f2&label=Discord)](https://discord.gg/VtUfRyzRXn)

#### Other

https://www.yuque.com/legado/wiki/community

<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>

# API [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-)

* Legado 3.0 The API is provided in 2 ways: `Web way` and `Content Provider way`. You can call it yourself as needed in [here](api.md). 
* One-click import by url recall reading, url format: legado://import/{path}?src={url}
* Path Type: bookSource,rssSource,replaceRule,textTocRule,httpTTS,theme,readConfig,dictRule,addToBookshelf
* path type explanation: Book source, subscription source, replacement rules, local txt novel directory rules, online reading engine, theme, reading layout, [add to bookshelf](/app/src/main/java/io/legado/app/ui/association/AddToBookshelfDialog.kt)

<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>

# Other [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-)

##### Disclaimers

https://gedoor.github.io/Disclaimer

##### Legado 3.0

* [eBook sources rules](https://mgz0227.github.io/The-tutorial-of-Legado/)
* [Update Log](/app/src/main/assets/updateLog.md)
* [Help Documentation](/app/src/main/assets/web/help/md/appHelp.md)
* [web bookshelf](https://github.com/gedoor/legado_web_bookshelf)
* [web source editor](https://github.com/gedoor/legado_web_source_editor)

<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>

# Grateful [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-)

> * org.jsoup:jsoup
> * cn.wanghaomiao:JsoupXpath
> * com.jayway.jsonpath:json-path
> * com.github.gedoor:rhino-android
> * com.squareup.okhttp3:okhttp
> * com.github.bumptech.glide:glide
> * org.nanohttpd:nanohttpd
> * org.nanohttpd:nanohttpd-websocket
> * cn.bingoogolapple:bga-qrcode-zxing
> * com.jaredrummler:colorpicker
> * org.apache.commons:commons-text
> * io.noties.markwon:core
> * io.noties.markwon:image-glide
> * com.hankcs:hanlp
> * com.positiondev.epublib:epublib-core

<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>

# Interface [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-)

<img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B1.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B2.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B3.jpg" width="270">
<img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B4.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B5.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B6.jpg" width="270">

<a href="#readme">
    <img src="https://img.shields.io/badge/-Top-orange.svg" alt="#" align="right">
</a>
