/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/api */
/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/web */

import type { webReadConfig } from '@/web'
import ajax from './axios'
import type {
  BaseBook,
  Book,
  BookChapter,
  BookProgress,
  SeachBook,
} from '@/book'
import type { Source } from '@/source'

export type LeagdoApiResponse<T> = {
  isSuccess: boolean
  errorMsg: string
  data: T
}

export let legado_http_entry_point = ''
export let legado_webSocket_entry_point = ''

let wsOnError: typeof WebSocket.prototype.onerror = () => {}
export const setWebsocketOnError = (
  callback: typeof WebSocket.prototype.onerror,
) => {
  //WebSocket.prototype.onerror = callback
  wsOnError = callback
}

export const setApiEntryPoint = (
  http_entry_point: string,
  webSocket_entry_point: string,
) => {
  legado_http_entry_point = http_entry_point
  legado_webSocket_entry_point = webSocket_entry_point
  ajax.defaults.baseURL = http_entry_point.toString()
}

// 书架API
// Http
const getReadConfig = async (http_url = legado_http_entry_point) => {
  const { data } = await ajax.get<LeagdoApiResponse<string>>('/getReadConfig', {
    baseURL: http_url.toString(),
    timeout: 3000,
  })
  if (data.isSuccess) {
    try {
      return JSON.parse(data.data) as webReadConfig
    } catch {}
  }
}
const saveReadConfig = (config: webReadConfig) =>
  ajax.post<LeagdoApiResponse<string>>('/saveReadConfig', config)

/** @deprecated: 使用`API.saveBookProgressWithBeacon`以确保在页面或者直接关闭的情况下保存进度 */
const saveBookProgress = (bookProgress: BookProgress) =>
  ajax.post('/saveBookProgress', bookProgress)

/**主要在直接关闭浏览器情况下可靠发送书籍进度 */
const saveBookProgressWithBeacon = (bookProgress: BookProgress) => {
  if (!bookProgress) return
  // 常规请求可能会被取消 使用Fetch keep-alive 或者 navigator.sendBeacon
  navigator.sendBeacon(
    new URL('/saveBookProgress', legado_http_entry_point),
    JSON.stringify(bookProgress),
  )
}

const getBookShelf = () => ajax.get<LeagdoApiResponse<Book[]>>('/getBookshelf')

const getChapterList = (/** @type {string} */ bookUrl: string) =>
  ajax.get<LeagdoApiResponse<BookChapter[]>>(
    '/getChapterList?url=' + encodeURIComponent(bookUrl),
  )

const getBookContent = (
  /** @type {string} */ bookUrl: string,
  /** @type {number} */ chapterIndex: number,
) =>
  ajax.get<LeagdoApiResponse<string>>(
    '/getBookContent?url=' +
      encodeURIComponent(bookUrl) +
      '&index=' +
      chapterIndex,
  )

// webSocket
const search = (
  searchKey: string,
  onReceive: (data: SeachBook[]) => void,
  onFinish: () => void,
) => {
  const socket = new WebSocket(
    new URL('/searchBook', legado_webSocket_entry_point),
  )
  socket.onerror = wsOnError

  socket.onopen = () => {
    socket.send(`{"key":"${searchKey}"}`)
  }
  socket.onmessage = ({ data }) => {
    try {
      onReceive(JSON.parse(data))
    } catch {
      onFinish()
    }
  }

  socket.onclose = () => {
    onFinish()
  }
}

const saveBook = (book: BaseBook) =>
  ajax.post<LeagdoApiResponse<string>>('/saveBook', book)
const deleteBook = (book: BaseBook) =>
  ajax.post<LeagdoApiResponse<string>>('/deleteBook', book)

const isBookSource = /bookSource/i.test(location.href)

// 源编辑API
// Http
const getSources = () =>
  isBookSource ? ajax.get('/getBookSources') : ajax.get('/getRssSources')

const saveSource = (data: Source) =>
  isBookSource
    ? ajax.post<LeagdoApiResponse<string>>('/saveBookSource', data)
    : ajax.post<LeagdoApiResponse<string>>('/saveRssSource', data)

const saveSources = (data: Source[]) =>
  isBookSource
    ? ajax.post<LeagdoApiResponse<Source[]>>('/saveBookSources', data)
    : ajax.post<LeagdoApiResponse<Source[]>>('/saveRssSources', data)

const deleteSource = (data: Source[]) =>
  isBookSource
    ? ajax.post<LeagdoApiResponse<string>>('/deleteBookSources', data)
    : ajax.post<LeagdoApiResponse<string>>('/deleteRssSources', data)

// webSocket
const debug = (
  /** @type {string} */ sourceUrl: string,
  /** @type {string} */ searchKey: string,
  /** @type {(data: string) => void} */ onReceive: (data: string) => void,
  /** @type {() => void} */ onFinish: () => void,
) => {
  const url = new URL(
    `/${isBookSource ? 'bookSource' : 'rssSource'}Debug`,
    legado_webSocket_entry_point,
  )

  const socket = new WebSocket(url)
  socket.onerror = wsOnError
  socket.onopen = () => {
    socket.send(JSON.stringify({ tag: sourceUrl, key: searchKey }))
  }
  socket.onmessage = ({ data }) => onReceive(data)

  socket.onclose = () => {
    onFinish()
  }
}

/**
 * 从阅读获取需要特定处理的书籍封面
 * @param {string} coverUrl
 */
const getProxyCoverUrl = (coverUrl: string) => {
  if (coverUrl.startsWith(legado_http_entry_point)) return coverUrl
  return new URL(
    '/cover?path=' + encodeURIComponent(coverUrl),
    legado_http_entry_point,
  ).toString()
}
/**
 * 从阅读获取需要特定处理的图片
 * @param {string} bookUrl
 * @param {string} src
 * @param {number|`${number}`} width
 */
const getProxyImageUrl = (
  bookUrl: string,
  src: string,
  width: number | `${number}`,
) => {
  if (src.startsWith(legado_http_entry_point)) return src
  return new URL(
    '/image?path=' +
      encodeURIComponent(src) +
      '&url=' +
      encodeURIComponent(bookUrl) +
      '&width=' +
      width,
    legado_http_entry_point,
  ).toString()
}

export default {
  getReadConfig,
  saveReadConfig,
  saveBookProgress,
  saveBookProgressWithBeacon,
  getBookShelf,
  getChapterList,
  getBookContent,
  search,
  saveBook,
  deleteBook,

  getSources,
  saveSources,
  saveSource,
  deleteSource,
  debug,

  getProxyCoverUrl,
  getProxyImageUrl,
}
