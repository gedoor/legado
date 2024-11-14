/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/data/entities */
export type BaseBook = {
  name: string
  author: string
  bookUrl: string
  kind?: string
  wordCount?: string
  variable?: string
  /** 忽略序列化
    infoHtml?: string
    tocHtml?: string
    */
}
export type Book = BaseBook & {
  // 目录页Url (toc=table of Contents)
  tocUrl: string
  // 书源URL(默认BookType.local)
  origin: string
  //书源名称 or 本地书籍文件名
  originName: string
  // 分类信息(用户修改)
  customTag?: string
  // 封面Url(书源获取)
  coverUrl?: string
  // 封面Url(用户修改)
  customCoverUrl?: string
  // 简介内容(书源获取)
  intro?: string
  // 简介内容(用户修改)
  customnumberro?: string
  // 自定义字符集名称(仅适用于本地书籍)
  charset?: string
  // 类型详见BookType
  type: number
  // 自定义分组索引号
  group: number
  // 最新章节标题
  latestChapterTitle?: string
  // 最新章节标题更新时间
  latestChapterTime: number
  // 最近一次更新书籍信息的时间
  lastCheckTime: number
  // 最近一次发现新章节的数量
  lastCheckCount: number
  // 书籍目录总数
  totalChapterNum: number
  // 当前章节名称
  durChapterTitle?: string
  // 当前章节索引
  durChapterIndex: number
  // 当前阅读的进度(首行字符的索引位置)
  durChapterPos: number
  // 最近一次阅读书籍的时间(打开正文的时间)
  durChapterTime: number
  // 刷新书架时更新书籍信息
  canUpdate: boolean
  // 手动排序
  order: number
  //书源排序
  originOrder: number
  //阅读设置
  readConfig?: ReadConfig
  //同步时间
  syncTime: number
}
export type SeachBook = BaseBook & {
  /** 书源 */
  origin: string
  originName: string
  /** BookType */
  type: number
  coverUrl?: string
  intro?: string
  latestChapterTitle?: string
  /** 目录页Url (toc=table of Contents) */
  tocUrl: string
  time: number
  originOrder: number
  chapterWordCountText?: string
  chapterWordCount: number0
  respondTime: number
}
export type BookProgress = Pick<
  Book,
  | 'name'
  | 'author'
  | 'durChapterIndex'
  | 'durChapterPos'
  | 'durChapterTime'
  | 'durChapterTitle'
>

export type BookChapter = {
  url: string // 章节地址
  title: string // 章节标题
  isVolume: boolean // 是否是卷名
  baseUrl: string // 用来拼接相对url
  bookUrl: string // 书籍地址
  index: number // 章节序号
  isVip: boolean // 是否VIP
  isPay: boolean // 是否已购买
  resourceUrl?: string // 音频真实URL
  tag?: string // 更新时间或其他章节附加信息
  start?: number // 章节起始位置
  end?: number // 章节终止位置
  startFragmentId?: string //EPUB书籍当前章节的fragmentId
  endFragmentId?: string //EPUB书籍下一章节的fragmentId
  variable?: string //变量
}
