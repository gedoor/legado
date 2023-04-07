import { defineStore } from "pinia";
import { emptyBookSource, emptyRssSource } from "@utils/souce";

const isBookSource = /bookSource/i.test(location.href);
const emptySource = isBookSource ? emptyBookSource : emptyRssSource;

export const useSourceStore = defineStore("source", {
  state: () => {
    return {
      /** @type {import("@/source").BookSoure[]} */
      bookSources: [], // 临时存放所有书源,
      /** @type {import("@/source").RssSource[]} */
      rssSources: [], // 临时存放所有订阅源
      errorPushSources: [], // 保存到阅读app出错的源
      /** @type {import("@/source").Source} */
      currentSource: emptySource, // 当前编辑的源
      currentTab: localStorage.getItem("tabName") || "editTab",
      editTabSource: {}, // 生成序列化的json数据
      isDebuging: false,
    };
  },
  getters: {
    sources: (state) => (isBookSource ? state.bookSources : state.rssSources),
    currentSourceUrl: (state) =>
      isBookSource
        ? state.currentSource.bookSourceUrl
        : state.currentSource.sourceUrl,
    searchKey: (state) =>
      isBookSource
        ? state.currentSource.ruleSearch.checkKeyWord || "我的"
        : null,
  },
  actions: {
    startDebug() {
      this.currentTab = "editDebug";
      this.isDebuging = true;
    },
    debugFinish() {
      this.isDebuging = false;
    },

    //拉取源后保存
    saveSources(data) {
      if (isBookSource) {
        this.bookSources = data;
      } else {
        this.rssSources = data;
      }
    },
    //删除源
    deleteSources(data) {
      let sources = isBookSource ? this.bookSources : this.rssSources;
      data.forEach((source) => {
        let index = sources.indexOf(source);
        if (index > -1) sources.splice(index, 1);
      });
    },
    //保存当前编辑源
    saveCurrentSource() {
      let source = this.currentSource,
        sources,
        searchKey;
      if (isBookSource) {
        sources = this.bookSources;
        searchKey = "bookSourceUrl";
      } else {
        sources = this.rssSources;
        searchKey = "sourceUrl";
      }
      let index = sources.findIndex(
        (element) => element[searchKey] === source[searchKey]
      );
      //去掉响应 toRaw?
      source = JSON.parse(JSON.stringify(source));
      if (index > -1) {
        sources.splice(index, 1, source);
      } else {
        sources.push(source);
      }
    },
    // 更改当前编辑的源
    changeCurrentSource(source) {
      const newContent = JSON.stringify(source);
      this.currentSource = JSON.parse(newContent);
    },
    async setPushReturnSources(returnSoures) {
      if (isBookSource) {
        // @ts-ignore
        this.errorPushSources = this.sources.filter((source) =>
          returnSoures.every(
            (item) => item.bookSourceUrl !== source.bookSourceUrl
          )
        );
      } else {
        // @ts-ignore
        this.errorPushSources = this.sources.filter((source) =>
          returnSoures.every((item) => item.sourceUrl !== source.sourceUrl)
        );
      }
    },
    // update editTab tabName and editTab info
    changeTabName(tabName) {
      this.currentTab = tabName;
      localStorage.setItem("tabName", tabName);
    },
    changeEditTabSource(source) {
      const newContent = JSON.stringify(source);
      this.editTabSource = JSON.parse(newContent);
    },
    editHistory(history) {
      let historyObj;
      if (localStorage.getItem("history")) {
        historyObj = JSON.parse(localStorage.getItem("history"));
        historyObj.new.push(history);
        if (historyObj.new.length > 50) {
          historyObj.new.shift();
        }
        if (historyObj.old.length > 50) {
          historyObj.old.shift();
        }
        localStorage.setItem("history", JSON.stringify(historyObj));
      } else {
        const arr = { new: [history], old: [] };
        localStorage.setItem("history", JSON.stringify(arr));
      }
    },
    editHistoryUndo() {
      if (localStorage.getItem("history")) {
        let historyObj = JSON.parse(localStorage.getItem("history"));
        historyObj.old.push(this.currentSource);
        if (historyObj.new.length) {
          this.currentSource = historyObj.new.pop();
        }
        localStorage.setItem("history", JSON.stringify(historyObj));
      }
    },
    clearAllHistory() {
      localStorage.setItem("history", JSON.stringify({ new: [], old: [] }));
    },
    clearEdit() {
      this.editTabSource = {};
      this.currentSource = emptySource;
    },

    // clear all source
    clearAllSource() {
      this.bookSources = [];
      this.rssSources = [];
    },
  },
});
