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

export const useBookStore = defineStore('book', {
  state: () => {
    return {
      connectStatus: '正在连接后端服务器……',
      connectType: 'primary' as 'primary' | 'success' | 'danger',
      newConnect: true,
      searchBooks: [] as SeachBook[],
      shelf: [] as Book[],
      catalog: [] as BookChapter[],
      readingBook: {} as BaseBook & {
        chapterPos: number
        chapterIndex: number
        isSeachBook?: boolean
      },
      popCataVisible: false,
      contentLoading: true,
      showContent: false,
      config: {
        theme: -1,
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
      } as webReadConfig,
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
    configInited: state => state.config.theme !== -1,
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
    addBooks(books: Book[]) {
      this.shelf = books
    },
    clearBooks() {
      this.shelf = []
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
    setConfig(config: webReadConfig) {
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
    //保存进度到app
    async saveBookProgress() {
      if (!this.bookProgress) return Promise.resolve()
      return API.saveBookProgress(this.bookProgress)
    },
  },
})
