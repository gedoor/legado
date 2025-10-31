# 源规则帮助

* [阅读3.0(Legado)规则说明](https://mgz0227.github.io/The-tutorial-of-Legado/)
* [书源帮助文档](https://mgz0227.github.io/The-tutorial-of-Legado/Rule/source.html)
* [订阅源帮助文档](https://mgz0227.github.io/The-tutorial-of-Legado/Rule/rss.html)
* 辅助键盘❓中可插入URL参数模板,打开帮助,js教程,正则教程,选择文件
* 规则标志, {{......}}内使用规则必须有明显的规则标志,没有规则标志当作js执行
```
@@ 默认规则,直接写时可以省略@@
@XPath: xpath规则,直接写时以//开头可省略@XPath
@Json: json规则,直接写时以$.开头可省略@Json
: regex规则,不可省略,只可以用在书籍列表和目录列表
```
* jsLib
> 注入JavaScript到RhinoJs引擎中，支持两种格式，可实现[函数共用](https://github.com/gedoor/legado/wiki/JavaScript%E5%87%BD%E6%95%B0%E5%85%B1%E7%94%A8)

> `JavaScript Code` 直接填写JavaScript片段  
> `{"example":"https://www.example.com/js/example.js", ...}` 自动复用已经下载的js文件

> 注意此处定义的函数可能会被多个线程同时调用，在函数里的全局变量内容将会共享使用，对其进行修改可能会出现竞争问题

* 并发率
> 并发限制，单位ms，可填写两种格式

> `1000` 访问间隔1s  
> `20/60000` 60s内访问次数20  
> `source.putConcurrent(str: String)` 更改并发率(书源的并发率规则不为空时才生效)

* 书源类型: 文件
> 对于类似知轩藏书提供文件整合下载的网站，可以在书源详情的下载URL规则获取文件链接

> 通过截取下载链接或文件响应头头获取文件信息，获取失败会自动拼接`书名` `作者`和下载链接的`UrlOption`的`type`字段

> 压缩文件解压缓存会在下次启动后自动清理，不会占用额外空间  

* CookieJar
> 启用后会自动保存每次返回头中的Set-Cookie中的值，适用于验证码图片一类需要session的网站

* 登录UI
> 不使用内置webView登录网站，需要使用`登录URL`规则实现登录逻辑，可使用`登录检查JS`检查登录结果  
> 版本20221113重要更改：按钮支持调用`登录URL`规则里面的函数，必须实现`login`函数
```
规则填写示范
[
    {
        "name": "telephone",
        "type": "text",
        "default": "123"
    },
    {
        "name": "password",
        "type": "password"
    },
    {
        "name": "注册",
        "type": "button",
        "action": "http://www.yooike.com/xiaoshuo/#/register?title=%E6%B3%A8%E5%86%8C"
    },
    {
        "name": "获取验证码",
        "type": "button",
        "action": "getVerificationCode()",
        "style": {
            "layout_flexGrow": 0,
            "layout_flexShrink": 1,
            "layout_alignSelf": "auto",
            "layout_flexBasisPercent": -1,
            "layout_wrapBefore": false
        }
    },
    {
        "name": "评论开关",
        "type": "toggle",
        "chars": ["❎", "☑️"],
        "default": "☑️"
    },
    {
        "name": "显示书名",
        "viewName": "book?.name||'未获取到书名'",
        "type": "button"
    }
]
```
* 登录URL
> 可填写登录链接或者实现登录UI的登录逻辑的JavaScript  
变量`isLongClick`为true时表示为按钮长按点击
```
示范填写
function login() {
    java.log("模拟登录请求");
    java.log(source.getLoginInfoMap());
}
function getVerificationCode() {
    java.log("登录UI按钮：获取到手机号码"+result.get("telephone"))
}

登录按钮函数获取登录信息
result.get("telephone")
login函数获取登录信息
source.getLoginInfo()
source.getLoginInfoMap().get("telephone")
source登录相关方法,可在js内通过source.调用,可以参考阿里云语音登录
login()
getHeaderMap(hasLoginHeader: Boolean = false)
getLoginHeader(): String?
getLoginHeaderMap(): Map<String, String>?
putLoginHeader(header: String)
removeLoginHeader()
setVariable(variable: String?)
getVariable(): String?
AnalyzeUrl相关函数,js中通过java.调用
initUrl() //重新解析url,可以用于登录检测js登录后重新解析url重新访问
getHeaderMap().putAll(source.getHeaderMap(true)) //重新设置登录头
getStrResponse( jsStr: String? = null, sourceRegex: String? = null) //返回访问结果,文本类型,书源内部重新登录后可调用此方法重新返回结果
getResponse(): Response //返回访问结果,网络朗读引擎采用的是这个,调用登录后在调用这方法可以重新访问,参考阿里云登录检测
```

* 发现url格式
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

* 请求头,支持http代理,socks4 socks5代理设置
> 注意请求头的key是区分大小写的  
> 正确格式 User-Agent Referer  
> 错误格式 user-agent referer
```
socks5代理
{
  "proxy":"socks5://127.0.0.1:1080"
}
http代理
{
  "proxy":"http://127.0.0.1:1080"
}
支持http代理服务器验证
{
  "proxy":"http://127.0.0.1:1080@用户名@密码"
}
注意:这些请求头是无意义的,会被忽略掉
```

* url添加js参数,解析url时执行,可在访问url时处理url,例
```
https://www.baidu.com,{"js":"java.headerMap.put('xxx', 'yyy')"}
https://www.baidu.com,{"js":"java.url=java.url+'yyyy'"}
```

* 增加js方法，用于重定向拦截
  * `java.get(urlStr: String, headers: Map<String, String>)`
  * `java.post(urlStr: String, body: String, headers: Map<String, String>)`
* 对于搜索重定向的源，可以使用此方法获得重定向后的url
```
(()=>{
  if(page==1){
    let url='https://www.yooread.net/e/search/index.php,'+JSON.stringify({
    "method":"POST",
    "body":"show=title&tempid=1&keyboard="+key
    });
    return source.put('surl',String(java.connect(url).raw().request().url()));
  } else {
    return source.get('surl')+'&page='+(page-1)
  }
})()
或者
(()=>{
  let base='https://www.yooread.net/e/search/';
  if(page==1){
    let url=base+'index.php';
    let body='show=title&tempid=1&keyboard='+key;
    return base+source.put('surl',java.post(url,body,{}).header("Location"));
  } else {
    return base+source.get('surl')+'&page='+(page-1);
  }
})()
```

* 图片链接支持修改headers
```
let options = {
"headers": {"User-Agent": "xxxx","Referrer":baseUrl,"Cookie":"aaa=vbbb;"}
};
'<img src="'+src+","+JSON.stringify(options)+'">'
```

* 字体解析使用
> 使用方法,在正文替换规则中使用,原理根据f1字体的字形数据到f2中查找字形对应的编码
```
<js>
(function(){
  var b64=String(src).match(/ttf;base64,([^\)]+)/);
  if(b64){
    var f1 = java.queryTTF(b64[1]);
    var f2 = java.queryTTF("https://alanskycn.gitee.io/teachme/assets/font/Source Han Sans CN Regular.ttf");
    // return java.replaceFont(result, f1, f2);
    return java.replaceFont(result, f1, f2, true); // 过滤掉f1中不存在的字形
  }
  return result;
})()
</js>
```

* 购买操作
> 可直接填写链接或者JavaScript，如果执行结果是网络链接将会自动打开浏览器,js返回true自动刷新目录和当前章节

* 回调操作
> 先启用事件监听按钮，然后软件触发事件时会执行回调规则的js代码。如果js返回true会消费事件，之后软件部分原本操作不会再执行。  
`event`变量值对应的事件名称，目前的事件有
```js
"shareBook" //详情页分享按钮
"clickBookName" //详情页点击书名
"clickAuthor" //详情页点击作者
"clickCustomButton" //书源自定义按钮
"clearCache" //详情页清理缓存
"addBookShelf" //添加到书架
"delBookShelf" //移除书架
"saveRead" //保存阅读进度
"startRead" //开始阅读
"endRead" //结束阅读
"startShelfRefresh" //开始书架刷新
"endShelfRefresh" //结束书架刷新
```

* 图片解密
> 适用于图片需要二次解密的情况，直接填写JavaScript，返回解密后的`ByteArray`  
> 部分变量说明：java（仅支持[js扩展类](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/JsExtensions.kt)），result为待解密图片的`ByteArray`，src为图片链接

```js
java.createSymmetricCrypto("AES/CBC/PKCS5Padding", key, iv).decrypt(result)
```

```js
function decodeImage(data, key) {
  var input = new Packages.java.io.ByteArrayInputStream(data)
  var out = new Packages.java.io.ByteArrayOutputStream()
  var byte
  while ((byte = input.read()) != -1) {
    out.write(byte ^ key)
  }
  return out.toByteArray()
}

decodeImage(result, key)
```

* 封面解密
> 同图片解密 其中result为待解密封面的`inputStream`

```js
java.createSymmetricCrypto("AES/CBC/PKCS5Padding", key, iv).decrypt(result)
```

```js
function decodeImage(data, key) {
  var out = new Packages.java.io.ByteArrayOutputStream()
  var byte
  while ((byte = data.read()) != -1) {
    out.write(byte ^ key)
  }
  return out.toByteArray()
}

decodeImage(result, key)
```

* 音频书籍获取歌词
> 正文内容第一行作为音频播放链接，其余内容作为歌词显示

```js
result = "https://example.mp3" + "\n" + "[00:00.00][by:花丸的蜜柑面包]"
```

* 网页JS
> 本地html中的额外支持的js函数

> `window.close()` 关闭浏览器界面  
> `screen.orientation.lock()` 全屏后可控制屏幕方向  

> 异步执行阅读函数，并返回字符串结果

```js
window.run("java.toast('执行成功');'成功'")
.then(r=>alert(r))
.catch(e=>alert("执行出错:"+e));
```

* 图片控制
> 在线书籍的图片链接中含有"js"键时，点击图片会执行一次键值的函数  
> 加载图片时，执行结果作为图片链接  

> "style"键值控制单个图片的样式  
> 目前支持"text"、"full"、"single"、"left"、"right"  
> "TEXT"且处于段尾时，占1.5个字符位  

```
https://www.baidu.com/img/flexible/logo/pc/result.png,{
  "js": "if (book) java.toast('这是'+book.name+'的图');result",
  "style": "right"
}
```

* 订阅源
> 内置浏览器额外支持的函数和超链  

```js
`opensorturl://${encodeURIComponent("https://example.com")}` //超链接打开分类界面
`openrssurl://${encodeURIComponent("https://example.com")}` //超链接打开正文界面
window.openui("sort", {"分类":"https://example.com"}) //函数打开分类界面
window.openui("rss", {"正文":"https://example.com"}) //函数打开正文界面
```
