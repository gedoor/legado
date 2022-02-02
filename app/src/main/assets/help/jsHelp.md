# js变量和函数

书源规则中使用js可访问以下变量
> java 变量-当前类  
> baseUrl 变量-当前url,String  
> result 变量-上一步的结果  
> book 变量-[书籍类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/Book.kt)  
> chapter 变量-[当前目录类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookChapter.kt)  
> source 变量-[基础书源类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BaseSource.kt)  
> cookie 变量-[cookie操作类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/http/CookieStore.kt)  
> cache 变量-[缓存操作类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/CacheManager.kt)  
> title 变量-当前标题,String  
> src 内容,源码  
> nextChapterUrl 变量 下一章节url  

## 当前类对象的可使用的部分方法

### [AnalyzeUrl](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt) 部分函数
> js中通过java.调用,只在`登录检查JS`规则中有效
```
initUrl() //重新解析url,可以用于登录检测js登录后重新解析url重新访问
getHeaderMap().putAll(source.getHeaderMap(true)) //重新设置登录头
getStrResponse( jsStr: String? = null, sourceRegex: String? = null) //返回访问结果,文本类型,书源内部重新登录后可调用此方法重新返回结果
getResponse(): Response //返回访问结果,网络朗读引擎采用的是这个,调用登录后在调用这方法可以重新访问,参考阿里云登录检测
```

### [AnalyzeRule](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt) 部分函数
* 获取文本/文本列表
> `mContent` 待解析源代码，默认为当前页面  
> `isUrl` 链接标识，默认为`false`
```
java.getString(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false)
java.getStringList(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false)
```
* 设置解析内容
```
java.setContent(content: Any?, baseUrl: String? = null):
```
* 获取Element/Element列表
> 如果要改变解析源代码，请先使用`java.setContent`
```
java.getElement(ruleStr: String)
java.getElements(ruleStr: String)
```

### [js扩展类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/JsExtensions.kt) 部分函数
* 变量存取
```
java.get(key)
java.put(key, value)
```
* 网络请求
```
java.ajax(urlStr)
java.ajaxAll(urlList: Array<String>): Array<StrResponse?>
```
* 调试
```
java.log(msg)
java.logType(var)
```
* 缓存网络文件
```
获取
java.cacheFile(url)
java.cacheFile(url,saveTime)
执行内容
eval(String(java.cacheFile(url)))
删除缓存文件
cache.delete(java.md5Encode16(url))
```
* 获取网络zip文件里面的数据
```
java.getZipStringContent(url: String, path: String)
```
* base64
> flags参数可省略，默认Base64.NO_WRAP，查看[flags参数说明](https://blog.csdn.net/zcmain/article/details/97051870)
```
java.base64Decode(str: String, flags: Int)
java.base64Encode(str: String, flags: Int)
```
* 文件
>  所有对于文件的读写删操作都是相对路径,只能操作阅读缓存/android/data/{package}/cache/内的文件
```
//文件下载,content为十六进制字符串,url用于生成文件名，返回文件路径
downloadFile(content: String, url: String): String
//文件解压,zipPath为压缩文件路径，返回解压路径
unzipFile(zipPath: String): String
//文件夹内所有文件读取
getTxtInFolder(unzipPath: String): String
//读取文本文件
readTxtFile(path: String): String
//删除文件
deleteFile(path: String) 
```
****
> [常见加密解密算法介绍](https://www.yijiyong.com/algorithm/encryption/01-intro.html)
> [相关概念](https://blog.csdn.net/OrangeJack/article/details/82913804)  
* AES
```
* @param data 传入的原始数据
* @param key AES加密的key
* @param transformation AES加密的方式 例如AES/ECB/PKCS5Padding
* @param iv ECB模式的偏移向量
java.aesDecodeToString(str: String, key: String, transformation: String, iv: String)

java.aesBase64DecodeToString(str: String, key: String, transformation: String, iv: String)

java.aesEncodeToString(str: String, key: String, transformation: String, iv: String)

java.aesEncodeToBase64String(str: String, key: String, transformation: String, iv: String)
```
* 3DES
```
* @param data 被加密的字符串
* @param key 密钥
* @param mode 模式 ECB/CBC/CFB/OFB/CTR
* @param padding 补码方式 NoPadding/PKCS5Padding/
* @param iv 加盐
java.tripleDESEncodeBase64Str(data: String,key: String,mode: String,padding: String,iv: String): String?

java.tripleDESDecodeStr(data: String,key: String,mode: String,padding: String,iv: String): String?
```
* 摘要
```
* @param data 被摘要数据
* @param algorithm 签名算法 MD5/SHA1/SHA256/SHA512
java.digestHex(data: String,algorithm: String,): String?

java.digestBase64Str(data: String,algorithm: String,): String?
```
* md5
```
java.md5Encode(str)
java.md5Encode16(str)
```

## book对象的可用属性
> 使用方法: 在js中或{{}}中使用book.属性的方式即可获取.如在正文内容后加上 ##{{book.name+"正文卷"+title}} 可以净化 书名+正文卷+章节名称（如 我是大明星正文卷第二章我爸是豪门总裁） 这一类的字符.
```
bookUrl // 详情页Url(本地书源存储完整文件路径)
tocUrl // 目录页Url (toc=table of Contents)
origin // 书源URL(默认BookType.local)
originName //书源名称 or 本地书籍文件名
name // 书籍名称(书源获取)
author // 作者名称(书源获取)
kind // 分类信息(书源获取)
customTag // 分类信息(用户修改)
coverUrl // 封面Url(书源获取)
customCoverUrl // 封面Url(用户修改)
intro // 简介内容(书源获取)
customIntro // 简介内容(用户修改)
charset // 自定义字符集名称(仅适用于本地书籍)
type // 0:text 1:audio
group // 自定义分组索引号
latestChapterTitle // 最新章节标题
latestChapterTime // 最新章节标题更新时间
lastCheckTime // 最近一次更新书籍信息的时间
lastCheckCount // 最近一次发现新章节的数量
totalChapterNum // 书籍目录总数
durChapterTitle // 当前章节名称
durChapterIndex // 当前章节索引
durChapterPos // 当前阅读的进度(首行字符的索引位置)
durChapterTime // 最近一次阅读书籍的时间(打开正文的时间)
canUpdate // 刷新书架时更新书籍信息
order // 手动排序
originOrder //书源排序
variable // 自定义书籍变量信息(用于书源规则检索书籍信息)
 ```

## chapter对象的部分可用属性
> 使用方法: 在js中或{{}}中使用chapter.属性的方式即可获取.如在正文内容后加上 ##{{chapter.title+chapter.index}} 可以净化 章节标题+序号(如 第二章 天仙下凡2) 这一类的字符.
 ```
 url // 章节地址
 title // 章节标题
 baseUrl //用来拼接相对url
 bookUrl // 书籍地址
 index // 章节序号
 resourceUrl // 音频真实URL
 tag //
 start // 章节起始位置
 end // 章节终止位置
 variable //变量
 ```
 
## source对象的部分可用函数
* 获取书源url
```
source.getKey()
```
* 书源变量存取
```
source.setVariable(variable: String?)
source.getVariable()
```

* 登录头操作
```
source.getLoginHeader()
source.getLoginHeaderMap().get(key: String)
source.putLoginHeader(header: String)
source.removeLoginHeader()
```
* 用户登录信息操作
> 使用`登录UI`规则，并成功登录，阅读自动加密保存登录UI规则中除type为button的信息
```
source.getLoginInfo()
source.getLoginInfoMap().get(key: String)
source.removeLoginInfo()
```
## cookie对象的部分可用函数
```
获取全部cookie
cookie.getCookie(url)
获取cookie某一键值
cookie.getKey(url,key)
删除cookie
cookie.removeCookie(key)
```

## cache对象的部分可用函数
> saveTime单位:秒，可省略  
> 保存至数据库和缓存文件(50M)，保存的内容较大时请使用`getFile putFile`
```
保存
cache.put(key, value , saveTime)
读取数据库
cache.get(key)
删除
cache.delete(key)
缓存文件内容
cache.putFile(key, value, saveTime)
读取文件内容
cache.getFile(key)
```