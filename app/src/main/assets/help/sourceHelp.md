# 源规则帮助

* [书源帮助文档](https://alanskycn.gitee.io/teachme/Rule/source.html)
* [订阅源帮助文档](https://alanskycn.gitee.io/teachme/Rule/rss.html)

* 规则标志, {{......}}内使用规则必须有明显的规则标志,没有规则标志当作js执行
  * @@ 默认规则,直接写时可以省略@@
  * @XPath: xpath规则,直接写时以//开头可省略@XPath
  * @Json: json规则,直接写时以$.开头可省略@Json
  * : regex规则,不可省略,只可以用在书籍列表和目录列表
  
* js 变量和函数
  * java 变量-当前类
  * baseUrl 变量-当前url,String
  * result 变量-上一步的结果
  * book 变量-书籍类,方法见 io.legado.app.data.entities.Book
  * cookie 变量-cookie操作类,方法见 io.legado.app.help.http.CookieStore
  * cache 变量-缓存操作类,方法见 io.legado.app.help.CacheManager
  * chapter 变量-当前目录类,方法见 io.legado.app.data.entities.BookChapter
  * title 变量-当前标题,String