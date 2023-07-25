import ajax from "./axios";
import { ElMessage } from "element-plus/es";

/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/api */
/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/web */

const { hostname, port } = new URL(import.meta.env.VITE_API || location.origin);

const isSourecEditor = /source/i.test(location.href);
const APIExceptionHandler = (error) => {
  if (isSourecEditor) {
    ElMessage({
      message: "后端错误，检查网络或者阅读app",
      type: "error",
    });
  }
  throw error;
};
ajax.interceptors.response.use((response) => response, APIExceptionHandler);

// Http
const getReadConfig = () => ajax.get("/getReadConfig");
const saveReadConfig = (config) => ajax.post("/saveReadConfig", config);

const saveBookProgress = (bookProgress) =>
  ajax.post("/saveBookProgress", bookProgress);

const saveBookProgressWithBeacon = (bookProgress) => {
  if (!bookProgress) return;
  // 常规请求可能会被取消 使用Fetch keep-alive 或者 navigator.sendBeacon
  navigator.sendBeacon(
    `${import.meta.env.VITE_API || location.origin}/saveBookProgress`,
    JSON.stringify(bookProgress),
  );
};

const getBookShelf = () => ajax.get("/getBookshelf");

const getChapterList = (/** @type {string} */ bookUrl) =>
  ajax.get("/getChapterList?url=" + encodeURIComponent(bookUrl));

const getBookContent = (
  /** @type {string} */ bookUrl,
  /** @type {number} */ chapterIndex,
) =>
  ajax.get(
    "/getBookContent?url=" +
      encodeURIComponent(bookUrl) +
      "&index=" +
      chapterIndex,
  );

const search = (
  /** @type {string} */ searchKey,
  /** @type {(data: string) => void} */ onReceive,
  /** @type {() => void} */ onFinish,
) => {
  // webSocket
  const url = `ws://${hostname}:${Number(port) + 1}/searchBook`;
  const socket = new WebSocket(url);

  socket.onopen = () => {
    socket.send(`{"key":"${searchKey}"}`);
  };
  socket.onmessage = ({ data }) => onReceive(data);

  socket.onclose = () => {
    onFinish();
  };
};

const saveBook = (book) => ajax.post("/saveBook", book);
const deleteBook = (book) => ajax.post("/deleteBook", book);

const isBookSource = /bookSource/i.test(location.href);

// Http
const getSources = () =>
  isBookSource ? ajax.get("getBookSources") : ajax.get("getRssSources");

const saveSource = (data) =>
  isBookSource
    ? ajax.post("saveBookSource", data)
    : ajax.post("saveRssSource", data);

const saveSources = (data) =>
  isBookSource
    ? ajax.post("saveBookSources", data)
    : ajax.post("saveRssSources", data);

const deleteSource = (data) =>
  isBookSource
    ? ajax.post("deleteBookSources", data)
    : ajax.post("deleteRssSources", data);

const debug = (
  /** @type {string} */ sourceUrl,
  /** @type {string} */ searchKey,
  /** @type {(data: string) => void} */ onReceive,
  /** @type {() => void} */ onFinish,
) => {
  // webSocket
  const url = `ws://${hostname}:${Number(port) + 1}/${
    isBookSource ? "bookSource" : "rssSource"
  }Debug`;

  const socket = new WebSocket(url);

  socket.onopen = () => {
    socket.send(JSON.stringify({ tag: sourceUrl, key: searchKey }));
  };
  socket.onmessage = ({ data }) => onReceive(data);

  socket.onclose = () => {
    ElMessage({
      message: "调试已关闭！",
      type: "info",
    });
    onFinish();
  };
};

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
};
