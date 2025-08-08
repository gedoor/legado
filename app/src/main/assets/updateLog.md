# 更新日志

* 关注公众号 **[开源阅读]** 菜单•软件下载 提前享受新版本。
* 关注合作公众号 **[小说拾遗]** 获取好看的小说。

## cronet版本: 128.0.6613.40

## **必读**
# 此版本来源于阅读fork仓库 [Luoyacheng/legado](https://github.com/Luoyacheng/legado)
【温馨提醒】 *更新前一定要做好备份，以免数据丢失！*

* 阅读只是一个转码工具，不提供内容，第一次安装app，需要自己手动导入书源，可以从公众号 **[开源阅读]**
  、QQ群、酷安评论里获取由书友制作分享的书源。
* 正文出现缺字漏字、内容缺失、排版错乱等情况，有可能是净化规则或简繁转换出现问题。
* 漫画源看书显示乱码，**阅读与其他软件的源并不通用**，请导入阅读的支持的漫画源！

**2025/08/08**
* 音频播放支持跳过片头片尾
* 音频支持显示歌词
* 状态栏音频控制支持上一首下一首
* 支持自定义本地dns
* 支持书源通过图片链接控制样式
* 点击图片执行链接js函数
* 支持标题显示图片
* 订阅源能识别更新状态
* 字数显示能识别在线字数
* 支持epub统计每章字数
* 优化字数统计，使其更加准确
* 优化书籍详情页平板布局
* 优化并发率控制实现
* 新增java.setConcurrent(concurrent:Sring)实时改变并发率
* 函数startBrowser、startBrowserAwait支持html参数
* 新增isFromBookInfo变量，判断是否来源于详情页刷新
* 新增cookie.setWebCookie(url,cookie)为内置浏览器设置cookie
* 登录UI支持用js构建
* 高亮代码限制提高到2万字符，支持let、const关键字高亮
* 优化text图片大小与字体汉字相同
* 内置浏览器变动
    - 支持screen.orientation.lock在全屏时控制屏幕方向
    - 支持window.close()关闭网页
    - 支持window.run(jscode)异步执行阅读的java函数
    - 支持不静音自动播放视频
* 订阅源变动
    - 单url订阅源支持加载内容规则
    - 订阅源分类自动分多行显示
    - 点击过的订阅源分类自动变暗
    - 支持为订阅源设定起始页
    - 单个分类时标题显示为分类名
    - 浏览器会优先读取已有的Glide图片缓存
    - 新增网页日志开关，console.log会输出到阅读日志
    - 支持函数直接打开分类界面和正文界面
    - 优化刷新按钮效果为重新访问链接再刷新

**2025/03/26**
* 目前阅读被恶意注册软著，并建立了多个公众号
* 官方公众号仅有：开源阅读、开源阅读软件，其他公众号与本软件无关

**2024/10/03**
* web书架支持加载网络字体、试读非书架书籍后弹窗、自定义后端IP
* rss收藏添加分组管理
* 朗读功能添加流式播放音频、来电自动暂停播放功能
* 其它bug修复

**2024/02/27**
* 添加备用URL导出
* 更新书源制作的帮助文档链接

**2024/02/20**
* 更新cronet: 121.0.6167.180
* 更新 kotlin->1.9.22 ksp->1.0.17
* 界面绘制优化
* Bug修复和其他优化

----

* [2023年日志](https://github.com/gedoor/legado/blob/record2023/app/src/main/assets/updateLog.md)
* [2022年日志](https://github.com/gedoor/legado/blob/record2022/app/src/main/assets/updateLog.md)
* [2021年日志](https://github.com/gedoor/legado/blob/record2021/app/src/main/assets/updateLog.md)
