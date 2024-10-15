import { defineStore } from 'pinia'
import API from '@api'
import type {
  BaseBook,
  Book,
  BookChapter,
  BookProgress,
  SeachBook,
} from '@/book'
import type { webReadConfig } from '@/web'
import { ElMessage } from 'element-plus/es'

const default_config: webReadConfig = {
  theme: 0,
  font: 0,
  fontSize: 18,
  readWidth: 800,
  infiniteLoading: false,
  customFontName: '',
  jumpDuration: 1000,
  spacing: {
    paragraph: 1,
    line: 0.8,
    letter: 0,
  },
}
let webReadConfigLoadedDate: Date | undefined

export const useBookStore = defineStore('book', {
  state: () => {
    return {
      connectStatus: '正在连接后端服务器……',
      connectType: 'primary' as 'primary' | 'success' | 'danger',
      newConnect: true,
      searchBooks: [] as SeachBook[],
      shelf: [] as Book[],
      catalog: [] as BookChapter[],
      readingBook: { chapterPos: 0, chapterIndex: 0 } as BaseBook & {
        chapterPos: number
        chapterIndex: number
        isSeachBook?: boolean
      },
      popCataVisible: false,
      contentLoading: true,
      showContent: false,
      config: default_config,
      miniInterface: false,
      readSettingsVisible: false,
    }
  },
  getters: {
    bookProgress: (state): BookProgress | undefined => {
      if (state.catalog.length == 0) return
      const { chapterIndex, chapterPos, name, author } = state.readingBook
      const title = state.catalog[chapterIndex]?.title
      if (!title) return
      return {
        name,
        author,
        durChapterIndex: chapterIndex,
        durChapterPos: chapterPos,
        durChapterTime: new Date().getTime(),
        durChapterTitle: title,
      }
    },
    theme: state => {
      return state.config.theme
    },
    isNight: state => state.config.theme == 6,
  },
  actions: {
    setConnectStatus(connectStatus: string) {
      this.connectStatus = connectStatus
    },
    setConnectType(connectType: 'primary' | 'success' | 'danger') {
      this.connectType = connectType
    },
    setNewConnect(newConnect: boolean) {
      this.newConnect = newConnect
    },
    /** 从后端加载书架书籍，优先返回内存缓存 */
    async loadBookShelf(): Promise<Book[]> {
      const fetchBookshellf_promise = API.getBookShelf().then(resp => {
        console.log('API.getBookShelf数据返回')
        const { isSuccess, data, errorMsg } = resp.data
        if (isSuccess === true) {
          if (
            this.shelf.length !== data.length &&
            this.shelf.length > 0 &&
            data.length > 0
          ) {
            ElMessage.info(`书架数据已更新`)
          }
          this.shelf = data.sort((a, b) => {
            const x = a['durChapterTime'] || 0
            const y = b['durChapterTime'] || 0
            return y - x
          })
        } else {
          if (errorMsg.includes('还没有添加小说') && this.shelf.length > 0) {
            ElMessage.info('当前书架上的书籍已经被删除')
            return (this.shelf = [])
          }
          ElMessage.error(errorMsg ?? '后端返回格式错误！')
        }
        console.log('书架数据已更新')
        return this.shelf
      })

      if (this.shelf.length > 0) {
        // bookshelf data fetched before:do not await
        console.log('返回缓存书架数据')
        return this.shelf
      } else {
        console.log('从阅读后端获取书架数据...')
        return await fetchBookshellf_promise
      }
    },
    clearBooks() {
      this.shelf = []
    },
    /** 从后端加载书籍目录，优先返回内存缓存 */
    async loadWebCatalog(
      book: typeof this.readingBook,
    ): Promise<BookChapter[]> {
      const { bookUrl, name, chapterIndex } = book
      const fetchChapterList_promise = API.getChapterList(
        book.bookUrl as string,
      ).then(res => {
        const { isSuccess, data, errorMsg } = res.data
        if (isSuccess === false) {
          ElMessage.error(errorMsg)
          throw new Error()
        }
        if (
          data.length !== this.catalog.length &&
          data.length > 0 &&
          this.catalog.length > 0
        ) {
          ElMessage.info(`书籍${name}: 章节目录已更新`)
        }
        this.setCatalog(data)
        console.log(`书籍${name}: 章节目录已更新`)
        return this.catalog
      })
      if (
        bookUrl === this.readingBook.bookUrl &&
        this.catalog.length > 0 &&
        this.catalog.length - 1 >= chapterIndex
      ) {
        console.log(`返回书籍《${name}》 缓存的章节目录`)
        return this.catalog
      } else {
        console.log(`从阅读后端获取书籍《${name}》 章节目录数据...`)
        return await fetchChapterList_promise
      }
    },
    setCatalog(catalog: BookChapter[]) {
      this.catalog = catalog
    },
    setPopCataVisible(visible: boolean) {
      this.popCataVisible = visible
    },
    setContentLoading(loading: boolean) {
      this.contentLoading = loading
    },
    setReadingBook(readingBook: typeof this.readingBook) {
      this.readingBook = readingBook
    },
    /** 只从从后端加载一次web阅读配置 */
    async loadWebConfig() {
      if (webReadConfigLoadedDate === undefined) {
        const _config = await API.getReadConfig()
        webReadConfigLoadedDate = new Date()
        console.log(
          `${this.$id}.loadWebConfig: ${webReadConfigLoadedDate.toLocaleString()}成功加载阅读配置`,
        )
        return this.setConfig(_config)
      }
      console.log(
        `${this.$id}.loadWebConfig: 已于${webReadConfigLoadedDate.toLocaleString()}成功加载`,
      )
    },
    setConfig(config?: webReadConfig) {
      this.config = Object.assign({}, this.config, config)
    },
    setReadSettingsVisible(visible: boolean) {
      this.readSettingsVisible = visible
    },
    setShowContent(visible: boolean) {
      this.showContent = visible
    },
    setMiniInterface(mini: boolean) {
      this.miniInterface = mini
    },
    async setSearchBooks(books: SeachBook[]) {
      books.forEach(book => {
        const isSeachBook = this.shelf.every(
          item => item.bookUrl !== book.bookUrl,
        )
        if (isSeachBook === true) {
          this.searchBooks.push(book)
        }
      })
    },
    clearSearchBooks() {
      this.searchBooks = []
    },
    /** 1.保存进度到app 2.修改内存中的数据*/
    async saveBookProgress() {
      if (!this.bookProgress) return Promise.resolve()
      const { bookUrl } = this.readingBook
      const shelfRaw = toRaw(this.shelf)
      const findIndex = shelfRaw.findIndex(book => book.bookUrl === bookUrl)
      if (findIndex > -1) {
        this.shelf[findIndex] = Object.assign(
          {},
          shelfRaw[findIndex],
          this.bookProgress,
        )
      }
      // 直接关闭浏览器时 http请求可能被取消
      // return API.saveBookProgress(this.bookProgress)
      return API.saveBookProgressWithBeacon(this.bookProgress)
    },
  },
})
