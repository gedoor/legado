import { defineStore } from "pinia";
import API from "@api";

export const useBookStore = defineStore("book", {
  state: () => {
    return {
      connectStatus: "正在连接后端服务器……",
      /**@type {"primary" | "success" |"danger"} */
      connectType: "primary",
      newConnect: true,
      /**@type {Array<{respondTime:number}>} */
      searchBooks: [],
      shelf: [],
      catalog: [],
      /**@type {{index: number,chapterPos:number}} */
      readingBook: { index: 0, chapterPos: 0 },
      popCataVisible: false,
      contentLoading: true,
      showContent: false,
      config: {
        theme: 0,
        font: 0,
        fontSize: 18,
        readWidth: 800,
        infiniteLoading: false,
        customFontName: "",
        jumpDuration: 1000,
        spacing: {
          paragraph: 1,
          line: 0.8,
          letter: 0,
        },
      },
      miniInterface: false,
      readSettingsVisible: false,
    };
  },
  getters: {
    bookProgress: (state) => {
      if (state.catalog.length == 0) return;
      // @ts-ignore
      const { index, chapterPos, bookName, bookAuthor } = state.readingBook;
      let title = state.catalog[index]?.title;
      if (!title) return;
      return {
        name: bookName,
        author: bookAuthor,
        durChapterIndex: index,
        durChapterPos: chapterPos,
        durChapterTime: new Date().getTime(),
        durChapterTitle: title,
      };
    },
    theme: (state) => {
      return state.config.theme
    },
    isNight: (state) => state.config.theme == 6,
  },
  actions: {
    setConnectStatus(connectStatus) {
      this.connectStatus = connectStatus;
    },
    setConnectType(connectType) {
      this.connectType = connectType;
    },
    setNewConnect(newConnect) {
      this.newConnect = newConnect;
    },
    addBooks(books) {
      this.shelf = books;
    },
    clearBooks() {
      this.shelf = [];
    },
    setCatalog(catalog) {
      this.catalog = catalog;
    },
    setPopCataVisible(visible) {
      this.popCataVisible = visible;
    },
    setContentLoading(loading) {
      this.contentLoading = loading;
    },
    setReadingBook(readingBook) {
      this.readingBook = readingBook;
    },
    setConfig(config) {
      this.config = Object.assign({}, this.config, config);
    },
    setReadSettingsVisible(visible) {
      this.readSettingsVisible = visible;
    },
    setShowContent(visible) {
      this.showContent = visible;
    },
    setMiniInterface(mini) {
      this.miniInterface = mini;
    },
    async setSearchBooks(books) {
      books.forEach((book) => {
        let findBook = this.shelf.find((item) => item.bookUrl == book.bookUrl);
        if (findBook === undefined) {
          this.searchBooks.push(book);
        }
      });
    },
    clearSearchBooks() {
      this.searchBooks = [];
    },
    //保存进度到app
    async saveBookProgress() {
      if (!this.bookProgress) return Promise.resolve();
      return API.saveBookProgress(this.bookProgress);
    },
    //读取阅读界面配置以初始化夜间模式 以免初次加载书架页面时闪屏
    async loadReadConfig() {
      return API.getReadConfig().then(response => response.data)
        .then(({isSuccess, data}) => 
          isSuccess && this.setConfig(JSON.parse(data))
        )
    }
  },
});
