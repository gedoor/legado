# 更新日志
* 关注公众号 **[开源阅读]** 菜单•软件下载 提前享受新版本。
* 关注合作公众号 **[小说拾遗]** 获取好看的小说。
* 旧版数据导入教程：先在旧版阅读(2.x)中进行备份，然后在新版阅读(3.x)【我的】->【备份与恢复】，选择【导入旧版本数据】。
## **必读** 
【温馨提醒】 *更新前一定要做好备份，以免数据丢失！*
* 阅读只是一个转码工具，不提供内容，第一次安装app，需要自己手动导入书源，可以从公众号 **[开源阅读]**、QQ群、酷安评论里获取由书友制作分享的书源。
* 正文出现缺字漏字、内容缺失、排版错乱等情况，有可能是净化规则出现问题。先关闭替换净化并刷新，再观察是否正常。如果正常说明净化规则存在误杀，如果关闭后仍然出现相关问题，请点击源链接查看原文与正文是否相同，如果不同，再进行反馈。
* 漫画源看书显示乱码，**阅读与其他软件的源并不通用**，请导入阅读的支持的漫画源！

**2021/07/22**
1. 非关键规则添加try防止报错中断解析
2. 添加获取封面的api
3. 获取正文api使用替换规则
4. 添加一个ronet版本,网络访问使用Chromium内核
5. web书架增加【最近一次更新书籍信息的时间】
6. 采用Flow替换LiveData,优化资源使用
7. 统一网络一键导入路径legado://import/{path}?src={url}
* path: bookSource,rssSource,replaceRule,textTocRule,httpTTS,theme,readConfig
* 添加了txt小说规则,在线朗读引擎,主题,排版 的一键导入支持,老url依然可用

**2021/07/16**
1. js扩展函数添加删除本地文件方法
2. js扩展函数对于文件的读写删操作都是相对路径,只能操作阅读缓存内的文件,/android/data/{package}/cache/...

**2021/07/15**
1. 添加js函数来修复开启js沙箱后某些书源失效。by ag2s20150909
```kotlin
/**
* 获取网络zip文件里面的数据
* @param url zip文件的链接
* @param path 所需获取文件在zip内的路径
* @return zip指定文件的数据
*/
fun getZipStringContent(url: String, path: String): String
/**
* 获取网络zip文件里面的数据
* @param url zip文件的链接
* @param path 所需获取文件在zip内的路径
* @return zip指定文件的数据
*/
fun getZipByteArrayContent(url: String, path: String): ByteArray?
```
* web服务添加一个导航页

**2021/07/11**
1. 开启JS沙箱限制
* 禁止在js里exec运行命令
* 禁止在js里通过geClass反射
* 禁止在js里创建File对象
* 禁止在js里获取Packages scope
2. 优化并修复bug

**2021/07/10**
1. 阅读界面长按菜单改回原来样式
2. 解决导入书源时重命名分组和保留名称冲突的问题

**2021/07/09**
1. 发现url添加json格式, 支持设置标签样式
* 样式属性可以搜索 [FleboxLayout子元素支持的属性介绍](https://www.jianshu.com/p/3c471953e36d)
* 样式属性可省略,有默认值
```json
[
  {
    "title": "xxx",
    "url": "",
    "style": {
      "layout_flexGrow": 0,
      "layout_flexShrink": 1,
      "layout_alignSelf": "auto",
      "layout_flexBasisPercent": -1,
      "layout_wrapBefore": false
    }
  }
]
```

**2021/07/07**
1. 默认规则新增类似`jsonPath`的索引写法 by bushixuanqi
* 格式形如 `[index,index, ...]` 或 `[!index,index, ...]` 其中`[!`开头表示筛选方式为排除，`index`可以是单个索引，也可以是区间。
* 区间格式为 `start:end` 或 `start:end:step`，其中`start`为`0`可省略，`end`为`-1`可省略。
* 索引、区间两端、区间间隔都支持负数
* 例如 `tag.div[-1, 3:-2:-10, 2]`
* 特殊用法 `tag.div[-1:0]` 可在任意地方让列表反向
2. 允许索引作为@分段后每个部分的首规则，此时相当于前面是`children`
* `head@.1@text` 与 `head@[1]@text` 与 `head@children[1]@text` 等价
3. 添加Umd格式支持 by ag2s20150909
4. 修复web页面按键重复监听的bug
5. 亮度条往中间移了一点,防止误触
6. 添加内置字典

**2021/06/29**
* 修复html格式化bug
* 订阅界面webView支持css prefers-color-scheme: dark 查询,需webView v76或更高版本
* 如webView低于v76可以用js调用activity.isNightTheme()来获取当前是否暗模式
* 修复一些书籍导出epub失败 by ag2s20150909

**2021/06/22**
* 修复隐藏未读设置不生效的bug
* 修复系统字体大小选择大时导入界面按钮显示不全的bug
* 修复听书从后台打开时不对的bug

**2021/06/20**
* viewPager2 改回 viewPager
* 添加配置导入文件规则功能 by bushixuanqi
* 文件夹分组样式优化(未完成)
* epub支持外部模板
* 修复一些bug

**2021/06/06**
* 添加自定义导出文件名
* 添加书架文件夹分组样式,未完成
* viewPager2 3层嵌套有问题,书架换回viewPager

**2021/05/29**
* 谷歌版可使用外部epub模板
* Asset文件夹下二级以内目录全文件读取，Asset->文件夹->文件
* epub元数据修改，使修改字体只对正文生效
* 修复epub模板文件的排序问题
* epub可自定义模板，模板路径为书籍导出目录的Asset文件夹，[模板范例](https://wwa.lanzoux.com/ibjBspkn05i)
```
Asset中里面必须有Text文件夹，Text文件夹里必须有chapter.html，否则导出正文会为空
chapter.html的关键字有{title}、{content}
其他html文件的关键字有{name}、{author}、{intro}、{kind}、{wordCount}
```

**2021/05/24**
* 反转目录后刷新内容
* 修复上下滑动会导致左右切换问题
* 精确搜索增加包含关键词的,比如搜索五行 五行天也显示出来, 五天行不显示

**2021/05/21**
* 添加反转目录功能
* 修复分享bug
* 详情页添加登录菜单
* 添加发现界面隐藏配置

**2021/05/16**
* 添加总是使用默认封面配置
* 添加一种语言 ptbr translation by mezysinc
* epublib 修bug by ag2s20150909

**2021/05/08**
* 预下载章节可调整数目
* 修复低版本Android使用TTS闪退。 by ag2s20150909
* 修复WebDav报错
* 优化翻页动画点击翻页

**2021/05/06**
* 修复bug
* url参数添加重置次数,retry
* 修改默认tts, 手动导入
* 升级android studio

**2021/04/30**
* epub插图,epublib优化,图片解码优化,epub读取导出优化。by ag2s20150909
* 添加高刷设置
* 其它一些优化
* pro版本被play商店下架了,先把pro设置图片背景的功能开放到所有版本,使用pro版本的可以使用备份恢复功能切换最新版本

**2021/04/16**
* 去掉google统计,解决华为手机使用崩溃的bug
* 添加规则订阅时判断重复提醒
* 添加恢复预设布局的功能, 添加一个微信读书布局作为预设布局

**2021/04/08**
* 缓存时重新检查并缓存图片
* 订阅源调试添加源码查看
* web调试不输出源码
* 修复bug
* 换源优化
--- by ag2s20150909
* 修复localBook获取书名作者名的逻辑
* 修复导出的epub的标题文字过大的bug
* 优化图片排版

**2021/04/02**
* 修复bug
* 书源调试添加源码查看
* 添加导出epub by ag2s20150909
* 换源添加是否校验作者选项

**2021/03/31**
* 优化epubLib by ag2s20150909
* 升级库,修改弃用方法
* tts引擎添加导入导出功能

**2021/03/23**
* 修复繁简转换“勐”“十”问题。使用了剥离HanLP简繁代码的民间库。APK减少6M左右
* js添加一个并发访问的方法 java.ajaxAll(urlList: Array<String>) 返回 Array<StrResponse?>
* 优化目录并发访问
* 添加自定义epublib,支持epub v3解析目录。by ag2s20150909

**2021/03/19**
* 修复图片地址参数缺少的bug
* 修复更改替换规则时多次重新加载正文导致朗读多次停顿的bug
* 修复是否使用替换默认值修改后不及时生效的bug
* 修复繁简转换“勐”“十”问题。使用了剥离HanLP简繁代码的民间库。APK减少6M左右 by hoodie13
* 百度tsn改为tts

**2021/03/15**
* 优化图片TEXT样式显示
* 图片url在解析正文时就拼接成绝对url
* 修复一些bug

**2021/03/08**
* 阅读页面停留10分钟之后自动备份进度
* 添加了针对中文的断行排版处理-by hoodie13, 需要再阅读界面设置里手动开启
* 添加朗读快捷方式
* 优化Epub解析 by hoodie13
* epub书籍增加cache by hoodie13
* 修复切换书籍或者章节时的断言崩溃问题。看漫画容易复现。 by hoodie13
* 修正增加书签alert的正文内容较多时，确定键溢出屏幕问题 by hoodie13
* 图片样式添加TEXT, 阅读界面菜单里可以选择图片样式

**2021/02/26**
* 添加反转内容功能
* 更新章节时若无目录url将自动加载详情页
* 添加变量nextChapterUrl
* 订阅跳转外部应用时提示
* 修复恢复bug
* 详情页拼接url改为重定向后的地址
* 不重复解析详情页

**2021/02/21**
* 下一页规则改为在内容规则之后执行
* 书籍导出增加编码设置和导出文件夹设置,使用替换设置
* 导入源添加等待框
* 修复一些崩溃bug

**2021/02/16**
* 修复分享内容不对的bug
* 优化主题颜色,添加透明度
* rss分类url支持js
* 打开阅读时同步阅读进度

**2021/02/09**
* 修复分组内书籍数目少于搜索线程数目，会导致搜索线程数目变低
* 修复保存书源时不更新书源时间的bug
* 订阅添加夜间模式,需启用js,还不是很完善
* 优化源导入界面

**2021/02/03**
* 排版导出文件名修改为配置名称
* 取消在线朗读下载文件检测,会导致朗读中断
* 修复其它一些bug

**2021/01/30**
* 优化阅读记录界面
* 自定义分组可以隐藏,删除按钮移到编辑对话框
* 修复其它一些bug

**2021/01/23**
* 优化书源校验,从搜索到正文全部校验
* play版可以设置背景图片
* 添加几个js方法,见io.legado.app.help.JsExtensions

**2021/01/18**
* 增加三星 S Pen 支持 by [dacer](https://github.com/dacer)
* 订阅添加阅读下载,可以从多个渠道下载
* 修复一些BUG

**2021/01/12**
* 修复bug
* 朗读时翻页防止重复发送请求 by [litcc](https://github.com/litcc)
* 换源刷新之前删除原搜索记录
* 优化web调试

**2021/01/03**
* 导出书单只保留书名与作者,导入时自动查找可用源
* 添加预加载设置
* 选择分组时只搜索分组

**2020/12/30**
* 解决文件下载异常，在线语音可正常播放 by [Celeter](https://github.com/Celeter)
* 更新默认在线朗读库, 默认id小于0方便下次更新时删除旧数据, 有重复的自己删除
* 导入导出书单
* 其它一些优化

**2020/12/27**
* 订阅添加搜索和分组
* 修复部分手机状态栏bug
* 单url订阅支持内容规则和样式

**2020/12/19**
* 书签转移到文本菜单里,会记录选择的文本和位置
* 订阅源添加单url选项,直接打开url
* 订阅源可以put,get数据

**2020/12/15**
* 修复一些引起崩溃的bug
* 修复搜书和换源可能什么分组都没有的bug
* 添加同步进度开关,默认开启,在备份与恢复里面

**2020/12/13**
* 修复bug
* 网络访问框架修改为RxHttp, 有bug及时反馈
* 优化进度同步
* 换源界面添加分组选择
* 沉浸模式时阅读界面导航栏透明

**2020/12/09**
* 修复bug
* 优化中文排序
* 优化编码识别
* 选择文字时优先选词
* 优化进度同步,进入书籍时同步,每次同步单本书,减少同步文件大小

**2020/12/04**
* 阅读进度从页数改为字数,排版变化时定位更准确
* 修改viewBinding
* 修复中文排序
* 去掉FontJs规则,可以写在替换规则里,示例可在帮助文档查看

**2020/11/18**
* 优化导航栏
* js添加java.log(msg: String)用于调试时输出消息
* js添加cookie变量,方法见io.legado.app.help.http.api.CookieManager
* js添加cache变量,可以用来存储token之类的临时值,可以设置保存时间,方法见io.legado.app.help.CacheManager
* 需要token的网站可以用js来写了,比如阿里tts

**2020/11/15**
* 正文规则添加字体规则,返回ByteArray
* js添加方法:
```
base64DecodeToByteArray(str: String?): ByteArray?
base64DecodeToByteArray(str: String?, flags: Int): ByteArray?
```

**2020/11/07**
* 详情页菜单添加拷贝URL
* 解决一些书名太长缓存报错的bug
* 添加备份搜索记录
* 替换编辑界面添加正则学习教程
* 去除解析目录时拼接相对url,提升解析速度
* 自动分段优化 by [tumuyan](https://github.com/tumuyan)
* web支持图片显示 by [六月](https://github.com/Celeter)

**2020/10/24**
* 修复选择错误的bug
* 修复长图最后一张不能滚动的bug
* js添加java.getCookie(sourceUrl:String, key:String? = null)来获取登录后的cookie by [AndyBernie](https://github.com/AndyBernie)
```
java.getCookie("http://baidu.com", null) => userid=1234;pwd=adbcd
java.getCookie("http://baidu.com", "userid") => 1234
```
* 修复简繁转换没有处理标题
* 每本书可以单独设置翻页动画,在菜单里
* 添加重新分段功能,针对每本书,在菜单里,分段代码来自[tumuyan](https://github.com/tumuyan)

**2020/10/18**
* 优化分组管理,默认分组可以重命名了
* 修复书架空白的bug,是constraintlayout库新版本的bug
* 修复分组和崩溃bug

**2020/10/11**
* 优化书源校验
* 语言切换bug修复 by [h11128](https://github.com/h11128)

**2020/10/07**
* 更新时预下载10章
* 支持更多分组
* url添加js参数,解析url时执行,可在访问url时处理url,例
```
https://www.baidu.com,{"js":"java.headerMap.put('xxx', 'yyy')"}
https://www.baidu.com,{"js":"java.url=java.url+'yyyy'"}
```

**2020/09/29**
* 增加了几个方法用于处理文件 by [Celeter](https://github.com/Celeter)
```
//文件下载,content为十六进制字符串,url用于生成文件名，返回文件路径
downloadFile(content: String, url: String): String
//文件解压,zipPath为压缩文件路径，返回解压路径
unzipFile(zipPath: String): String
//文件夹内所有文件读取
getTxtInFolder(unzipPath: String): String
```
* 增加type字段，返回16进制字符串,例:`https://www.baidu.com,{"type":"zip"}`
* 底部操作栏阴影跟随设置调节
