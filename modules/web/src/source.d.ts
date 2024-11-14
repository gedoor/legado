/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/data/entities */
type BaseSource = {
  /**
   * 并发率
   */
  concurrentRate?: string
  /**
   * 登录地址
   */
  loginUrl?: string

  /**
   * 登录UI
   */
  loginUi?: string

  /**
   * 请求头
   */
  header?: string

  /**
   * 启用cookieJar
   */
  enabledCookieJar?: boolean

  /**
   * js库
   */
  jsLib?: string
}
type BookSoure = BaseSource & {
  // 地址，包括 http/https
  bookSourceUrl: string
  // 名称
  bookSourceName: string
  // 分组
  bookSourceGroup?: string
  // 类型，0 文本，1 音频, 2 图片, 3 文件（指的是类似知轩藏书只提供下载的网站）
  bookSourceType: number
  // 详情页url正则
  bookUrlPattern?: string
  // 手动排序编号
  customOrder: number
  // 是否启用
  enabled: boolean
  // 启用发现
  enabledExplore: boolean
  // 登录检测js
  loginCheckJs?: string
  // 封面解密js
  coverDecodeJs?: string
  // 注释
  bookSourceComment?: string
  // 自定义变量说明
  variableComment?: string
  // 最后更新时间，用于排序
  lastUpdateTime: number
  // 响应时间，用于排序
  respondTime: number
  // 智能排序的权重
  weight: number
  // 发现url
  exploreUrl?: string
  // 发现筛选规则
  exploreScreen?: string
  // 发现规则
  ruleExplore?: ExploreRule
  // 搜索url
  searchUrl?: string
  // 搜索规则
  ruleSearch?: SearchRule
  // 书籍信息页规则
  ruleBookInfo?: BookInfoRule
  // 目录页规则
  ruleToc?: TocRule
  // 正文页规则
  ruleContent?: ContentRule
  // 段评规则
  ruleReview?: ReviewRule
}
type RuleSearch = {
  checkKeyWord?: string
  [prop: string]: string
}
/* type ExploreRule = {
    [prop:string]: string
}
type BookInfoRule = {
    [prop:string]: string
}
type TocRule = {
    [prop:string]: string
}
type ContentRule = {
    [prop:string]: string
}
type ReviewRule = {
    [prop:string]: string
} */
type RssSource = BaseSource & {
  sourceUrl: string
  // 名称
  sourceName: string
  // 图标
  sourceIcon: string
  // 分组
  sourceGroup?: string
  // 注释
  sourceComment?: string
  // 是否启用
  enabled: boolean
  // 自定义变量说明
  variableComment?: string
  /**登录检测js**/
  loginCheckJs?: string
  /**封面解密js**/
  coverDecodeJs?: string
  /**分类Url**/
  sortUrl?: string
  /**是否单url源**/
  singleUrl: boolean
  /*列表规则*/
  /**列表样式,0,1,2**/
  articleStyle: number
  /**列表规则**/
  ruleArticles?: string
  /**下一页规则**/
  ruleNextPage?: string
  /**标题规则**/
  ruleTitle?: string
  /**发布日期规则**/
  rulePubDate?: string
  /*webView规则*/
  /**描述规则**/
  ruleDescription?: string
  /**图片规则**/
  ruleImage?: string
  /**链接规则**/
  ruleLink?: string
  /**正文规则**/
  ruleContent?: string
  /**正文url白名单**/
  contentWhitelist?: string
  /**正文url黑名单**/
  contentBlacklist?: string
  /**
   * 跳转url拦截,
   * js, 返回true拦截,js变量url,可以通过js打开url,比如调用阅读搜索,添加书架等,简化规则写法,不用webView js注入
   * **/
  shouldOverrideUrlLoading?: string
  /**webView样式**/
  style?: string
  enableJs: boolean
  loadWithBaseUrl: boolean
  /**注入js**/
  injectJs?: string
  /*其它规则*/
  /**最后更新时间，用于排序**/
  lastUpdateTime: number
  customOrder: number
}
type Source = BookSoure | RssSource

export { Source, BookSoure, RssSource }
