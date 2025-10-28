# [English](English.md) [中文](README.md)

[![icon_android](https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/icon_android.png)](https://play.google.com/store/apps/details?id=io.legado.play.release)
<a href="https://jb.gg/OpenSourceSupport" target="_blank">
<img width="24" height="24" src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*135yekd*_ga*OTY4Mjg4NDYzLjE2Mzk0NTE3MzQ.*_ga_9J976DJZ68*MTY2OTE2MzM5Ny4xMy4wLjE2NjkxNjMzOTcuNjAuMC4w&_ga=2.257292110.451256242.1669085120-968288463.1639451734" alt="idea"/>
</a>

<div align="center">
<img width="125" height="125" src="https://github.com/gedoor/legado/raw/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="legado"/>  
  
Legado / 开源阅读
<br>
<a href="https://gedoor.github.io" target="_blank">gedoor.github.io</a> / <a href="https://loyc.xyz/" target="_blank">loyc.xyz</a>
<br>
Legado 是一款免费的 Android 平台开源小说阅读器。
</div>

# 复刻仓库的变动
- 新增功能
    - 音频播放支持跳过片头片尾
    - 音频支持显示歌词
    - 自定义本地dns（除内置浏览器和jsoup函数访问无法接管ip获取）
    - 支持书源通过图片链接控制样式
    - 点击图片执行链接js函数
    - 支持标题显示图片(在标题规则获取)
    - 支持自动更新书源、订阅源、替换规则的订阅链接
    - 编辑源时文本支持撤销/重做
    - 新增一款专属的代码编辑器，拥有完善的代码高亮、一键格式化、搜索替换、自动补全等等功能
    - 新增一款视频播放器，书源支持视频类型
    - 本地TXT目录规则支持用js更高效的处理
    - 净化规则使用js时支持book，chapter
    - 支持书源监听加入书架，移除书架，分享按钮，保存进度等事件并进行函数回调
- 功能变动
    - 书源编辑时改回文本跟随键盘光标滚动
    - 软件内部更新链接变为本复刻仓库的gitee链接。能拥有更好的下载速度和更新体验
    - 更换软件内部一些指向原版的链接。对一些过期提示文本内容进行去除。
    - openUrl函数打开阅读自身的链接不再弹窗提示
    - 正则表达式教程不再有超链接，避免被误点
    - 本地TXT目录会优先选择排在前面的合适规则，而不是将所有规则都进行匹配
    - 单书源搜索后续不继续维持单书源（避免后面每次得切换为全部书源搜索）
    - 净化规则的js结果重新支持$符号
    - 修改封面比例同多数平台的3:4
- 功能完善
    - 订阅源支持显示识别更新状态
    - 字数显示能智能识别在线字数
    - 图片链接不计入本地字数统计
    - 状态栏音频控制支持上一首下一首
    - 优化书籍详情页平板布局
    - 优化并发率控制实现
    - 登录UI支持js构建
    - 详情页执行java.refreshTocUrl()不会再连续刷新两次详情页
    - 高亮代码限制提高到2万字符，支持let、const关键字高亮
    - 支持epub统计每章字数
    - 优化字数统计，使其更加准确
    - 允许jslib密封对象的修改，禁止未用var声明的隐性创建全局变量
    - 详情页刷新目录成功后也会移除更新失败分组
    - 更新目录时，继承章节变量等附加信息
    - 登录等界面按钮加上更优雅的点击动效，并且按钮300ms防抖
    - 软键盘输入符支持换行符、制表符等等
    - 支持鼠标在阅读小说时滑轮滚动视图
    - 授权弹窗适配暗色主题，且最多只弹窗5次
    - 书架支持网格两列布局，支持隐藏书名、叠加书名到封面
    - 书架支持紧凑列表布局（每本书4行变3行，封面尺寸更小）
    - 书架间距支持自定义
    - tts界面增加清除缓存按钮
    - 封面换源优先调用封面规则再调用书源搜索（可再次点击刷新强制书源搜索）
    - 书源发现列表获取超时时间由30秒改为60秒
    - 段评图占位符不再发送到tts
    - 删除tts规则时会二次提醒
    - 让段评图片更容易识别点击
    - 本地小说长章节拆分后首章提示无内容异常改为卷标题
    - 优化封面圆角效果，更平滑
    - 对默认启用的bitmap封面绘制结果进行缓存，避免每次都实时绘制造成卡顿
- 登录界面变动
    - 登录界面支持切换按钮，支持设定默认值，支持js改变按钮文本
    - 支持isLongClick变量来识别按钮为长按点击
    - 支持登录界面执行的函数打开书源发现界面
- 内置浏览器变动
    - 支持screen.orientation.lock在全屏时控制屏幕方向
    - 支持window.close()关闭网页
    - 支持window.run(jscode)异步执行阅读的java函数
    - 支持不静音自动播放视频
    - 支持输出web调试日志
- 订阅源变动
    - 单url订阅源支持加载内容规则
    - 订阅源分类自动分多行显示
    - 点击过的订阅源分类自动变暗
    - 支持为订阅源设定起始页
    - 单个分类时标题显示为分类名
    - 浏览器会优先读取已有的Glide图片缓存
    - 新增网页日志开关，console.log会输出到阅读日志
    - 支持函数直接打开分类界面和正文界面
    - 优化刷新按钮效果为重新访问链接再刷新
    - 新增瀑布流观看样式(对图片类订阅源更加友好舒适,没有简介文本行数限制,会提前5个进行下一页预加载)
    - 不同分类可以持有同一个播放链接的视频，避免视频在不同分类间跳转
    - 支持点击订阅源历史记录跳转到内容页
    - 支持图片类订阅源，点击直接查看全图
    - 支持源决定是否预加载
    - 支持搜索功能
- 函数变动
    - startBrowser、startBrowserAwait支持html参数
    - startBrowserAwait返回的响应的url为网页实际地址
    - source.getLoginInfoMap()在用户信息未初始化时，返回空对象而不是null
- 函数、变量新增
    - java.setConcurrent(concurrent:Sring)实时改变并发率
    - cookie.setWebCookie(url,cookie)单独给内置浏览器设置cookie
    - source.refreshExplore()刷新发现
    - source.refreshJSLib()刷新jslib
    - 目录规则新增isFromBookInfo变量，判断执行规则时是否来着详情页刷新
    - 登录url规则含有book和chapter对象
    - 新增java.ajaxTestAll函数，可用来并发测试链接
    - 新增java.openVideoPlayer函数，调用内置的播放器播放视频
    - 登录界面支持searchBook和addBook、copyText函数
- 其余杂项
    - 目录卷章会显示获取的章节信息
    - 目录页，字数显示在章节信息右侧
    - 合并音频界面两个调速按钮，界面布局调整
    - 优化源正文图片格式化规则
    - 规则编辑界面"章节更新时间"规则中文名改名为"章节信息"
    - 先执行正文规则最后执行标题规则
    - 保持text图片大小与字体汉字相同
    - 优化章节vip获取到数字0错判为true的情况
    - 新增gitee仓库同步
<br><br>

[![](https://img.shields.io/badge/-Contents:-696969.svg)](#contents) [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-主要功能-) [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-交流社区-) [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-) [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-其他-) [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-感谢-) [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-界面-)

>新用户？
>
>软件不提供内容，需要您自己手动添加，例如导入书源等。
>看看 [官方帮助文档](https://www.yuque.com/legado/wiki)，也许里面就有你要的答案。

# Function-主要功能 [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-主要功能-)
[English](English.md)

<details><summary>中文</summary>
1.自定义书源，自己设置规则，抓取网页数据，规则简单易懂，软件内有规则说明。<br>
2.列表书架，网格书架自由切换。<br>
3.书源规则支持搜索及发现，所有找书看书功能全部自定义，找书更方便。<br>
4.订阅内容,可以订阅想看的任何内容,看你想看<br>
5.支持替换净化，去除广告替换内容很方便。<br>
6.支持本地TXT、EPUB阅读，手动浏览，智能扫描。<br>
7.支持高度自定义阅读界面，切换字体、颜色、背景、行距、段距、加粗、简繁转换等。<br>
8.支持多种翻页模式，覆盖、仿真、滑动、滚动等。<br>
9.软件开源，持续优化，无广告。
</details>

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Community-交流社区 [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-交流社区-)

#### Telegram
[![Telegram-group](https://img.shields.io/badge/Telegram-%E7%BE%A4%E7%BB%84-blue)](https://t.me/yueduguanfang) [![Telegram-channel](https://img.shields.io/badge/Telegram-%E9%A2%91%E9%81%93-blue)](https://t.me/legado_channels)

#### Discord
[![Discord](https://img.shields.io/discord/560731361414086666?color=%235865f2&label=Discord)](https://discord.gg/VtUfRyzRXn)

#### Other
https://www.yuque.com/legado/wiki/community

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# API [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-)
* 阅读3.0 提供了2种方式的API：`Web方式`和`Content Provider方式`。您可以在[这里](api.md)根据需要自行调用。 
* 可通过url唤起阅读进行一键导入,url格式: legado://import/{path}?src={url}
* path类型: bookSource,rssSource,replaceRule,textTocRule,httpTTS,theme,readConfig,dictRule,[addToBookshelf](/app/src/main/java/io/legado/app/ui/association/AddToBookshelfDialog.kt)
* path类型解释: 书源,订阅源,替换规则,本地txt小说目录规则,在线朗读引擎,主题,阅读排版,添加到书架

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Other-其他 [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-其他-)
##### 免责声明
https://gedoor.github.io/Disclaimer

##### 阅读3.0
* [书源规则](https://mgz0227.github.io/The-tutorial-of-Legado/)
* [更新日志](/app/src/main/assets/updateLog.md)
* [帮助文档](/app/src/main/assets/web/help/md/appHelp.md)
* [web端书架](https://github.com/gedoor/legado_web_bookshelf)
* [web端源编辑](https://github.com/gedoor/legado_web_source_editor)

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Grateful-感谢 [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-感谢-)
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
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Interface-界面 [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-界面-)
<img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B1.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B2.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B3.jpg" width="270">
<img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B4.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B5.jpg" width="270"><img src="https://github.com/gedoor/gedoor.github.io/blob/master/static/img/legado/%E9%98%85%E8%AF%BB%E7%AE%80%E4%BB%8B6.jpg" width="270">

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>
