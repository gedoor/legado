# 更新日志
* 关注公众号 **[开源阅读]()** 菜单•软件下载 提前享受新版本。
* 关注合作公众号 **[小说拾遗]()** 获取好看的小说。
* 旧版数据导入教程：先在旧版阅读(2.x)中进行备份，然后在新版阅读(3.x)【我的】->【备份与恢复】，选择【导入旧版本数据】。

**2020/11/06**
* 详情页菜单添加拷贝URL
* 解决一些书名太长缓存报错的bug
* 添加备份搜索记录
* 替换编辑界面添加正则学习教程
* 去除解析目录时拼接相对url,提升解析速度
* 自动分段优化 by [tumuyan](https://github.com/tumuyan)
* web支持图片显示 by [六月](https://github.com/Celeter)

**2020/11/01**
* 导入本地添加智能扫描,菜单-智能扫描,扫描当前文件夹包括子文件夹下所有文件

**2020/10/30**
* 修复bug
* 优化Android 11文件选择,本地导入

**2020/10/28**
* 修复SDK 30使用TTS问题

**2020/10/27**
* 点击书籍分组可显示书籍数量
* 升级到SDK30
* 修复8.0不显示默认背景图片的bug
* 添加排版命名

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

**2020/10/21**
* 默认分组无书籍时自动隐藏
* 自定义翻页按键支持多个按键

**2020/10/19**
* 优化分组管理
* 修复预下载没有保存的bug

**2020/10/18**
* 优化分组管理,默认分组可以重命名了
* 修复书架空白的bug,是constraintlayout库新版本的bug
* 修复分组和崩溃bug

**2020/10/16**
* 修复排版导入背景失败bug
* 修改默认度逍遥per为5003,需要重新导入默认
* 优化规则解析

**2020/10/14**
* 优化替换规则编辑界面
* 修复网格书架间距变大bug
* 其它一些优化,bug修复

**2020/10/13**
* 更新android studio 到 4.1
* 书架整理增加滑动选择

**2020/10/12**
* 优化预下载,防止同时下载太多卡顿

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
* 修复bug

**2020/10/02**
* 优化规则解析
* 优化正文搜索
* 翻页动画跟随背景
* 双击发现折叠发现,再次双击滚动至顶端
* 修复bug

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

**2020/09/24**
* 修复规则解析bug

**2020/09/21**
* 修复规则解析bug
* 换源时无最新章节信息时可加载详情页来获取(默认关闭)

**2020/09/20**
* 优化正文搜索
* 阅读界面信息添加书名

**2020/09/18**
* 解决正文替换{{title}}问题
* 修复共用布局配置不能读取的问题
* 添加自定义源分组功能 by KKL369
* 解决跨进程调用ReaderProvider出现CursorIndexOutOfBoundsException问题

**2020/09/17**
* 优化正文搜索文字颜色
* 优化书源校验 by KKL369
* 缓存导出到webDav by 10bits
* 导入的字体在字体选择界面显示

**2020/09/15**
* 修复导入排版字体重复报错的bug
* 添加正文搜索 by [h11128](https://github.com/h11128)

**2020/09/12**
* web看书同步最新章
* web写源增加图片样式等规则
* 正文规则可以使用@get:{title}获取目录标题,js里使用title

**2020/09/11**
* 修复一些bug
* 背景配置自由添加

**2020/09/10**
* 修复自动换源的bug
* 修复保存主题的bug
* 书源排序，分享，注释优化 by [h11128](https://github.com/h11128)

**2020/09/09**
* 修复主题导入导出bug
* 优化分屏模式状态栏
* 书源基本属性增加“书源注释”

**2020/09/08**
* 页眉页脚跟随背景
* 主题导入导出

**2020/09/07**
* 订阅源和替换规则添加滑动选择
* 修复排版配置导入导出
* 订阅界面添加下载文件功能

**2020/09/06**
* 优化翻页
* EInk模式独立背景
* 阅读排版配置导入导出,包括背景和字体,支持网络导入

**2020/09/03**
* 修复替换中的回车消失的bug
* 所有内容恢复htmlFormat, 在想其它办法解决丢失一些内容的问题
* 图片(漫画)支持导出

**2020/09/02**
* 搜索url支持put,get,js里使用java.put,java.get

* 正文合并后替换规则支持所有规则写法,包括js

**2020/09/01**
* 导入书源列表添加全不选
* 详情页菜单添加清理缓存,清理当前书籍缓存
* 修复滑动选择,选择数量不更新的bug
* 字体跟随背景,每个背景对应一个字体
* 优化图片下载

**功能介绍**
* 书源调试
  - 调试搜索>>输入关键字，如：`系统`
  - 调试发现>>输入发现URL，如：`月票榜::https://www.qidian.com/rank/yuepiao?page={{page}}`
  - 调试详情页>>输入详情页URL，如：`https://m.qidian.com/book/1015609210`
  - 调试目录页>>输入目录页URL，如：`++https://www.zhaishuyuan.com/read/30394`
  - 调试正文页>>输入正文页URL，如：`--https://www.zhaishuyuan.com/chapter/30394/20940996`
* 修改订阅中自动添加style的情景
  订阅源的内容规则中存在`<style>`或`style=`时，直接显示内容规则的原始内容
* 请求头,支持http代理,socks4 socks5代理设置 by [10bits](https://github.com/10bits)
```
socks5代理
{
  "proxy":"socks5://127.0.0.1:1080"
}
http代理
{
  "proxy":"http://127.0.0.1:1080"
}
支持代理服务器验证
{
  "proxy":"socks5://127.0.0.1:1080@用户名@密码"
}
注意:这些请求头是无意义的,会被忽略掉
```
* 对于搜索重定向的源，可以使用此方法获得重定向后的url
```
<js>
var url='https://www.yooread.net/e/search/index.php,'+JSON.stringify({
"method":"POST",
"body":"show=title&tempid=1&keyboard="+key
});
String(java.connect(url).raw().request().url())
</js>
```