import ajax from "./axios";
import { ElMessage } from "element-plus/es";

/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/api */
/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/web */

let legado_http_origin
let legado_webSocket_origin

const setLeagdoHttpUrl = (http_url) => {
  let legado_webSocket_port;
  const { protocol, hostname, port } = new URL(http_url);
  if (!protocol.startsWith("http"))
    throw new Error("unexpect protocol:" + http_url);
  ajax.defaults.baseURL = http_url;
  legado_http_origin = http_url;
  if (port !== "") {
    legado_webSocket_port = Number(port) + 1;
  } else {
    legado_webSocket_port = protocol.startsWith("https:") ? "444" : "81";
  }
  legado_webSocket_origin = 
    `${protocol.startsWith("https:") ? "wss://" : "ws://"}${hostname}:${legado_webSocket_port}`;

  console.info("legado_server_config:");
  console.table({legado_http_origin, legado_webSocket_origin});
};

// 手动初始化 阅读web服务地址
setLeagdoHttpUrl(ajax.defaults.baseURL);

const testLeagdoHttpUrlConnection = async (http_url) => {
  const {data = {}} = await ajax.get("/getReadConfig", {
    baseURL: http_url,
    timeout: 3000
  })
  // 返回结果应该是JSON 并有键值isSuccess
  try {
    if ("isSuccess" in data) return data.data
    throw new Error("ReadConfig后端返回格式错误" )
  } catch {
    throw new Error("ReadConfig后端返回格式错误" )
  }
}

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

// 书架API
// Http
/** @returns {Promise<import("axios").AxiosResponse<{isSuccess: boolean, data: string, errorMsg:string}>>} */
const getReadConfig = () => ajax.get("/getReadConfig", {timeout: 3000});
const saveReadConfig = (config) => ajax.post("/saveReadConfig", config);

const saveBookProgress = (bookProgress) =>
  ajax.post("/saveBookProgress", bookProgress);

const saveBookProgressWithBeacon = (bookProgress) => {
  if (!bookProgress) return;
  // 常规请求可能会被取消 使用Fetch keep-alive 或者 navigator.sendBeacon
  navigator.sendBeacon(
    `${legado_http_origin}/saveBookProgress`,
    JSON.stringify(bookProgress)
  );
};

const getBookShelf = () => ajax.get("/getBookshelf");

const getChapterList = (/** @type {string} */ bookUrl) =>
  ajax.get("/getChapterList?url=" + encodeURIComponent(bookUrl));

const getBookContent = (
  /** @type {string} */ bookUrl,
  /** @type {number} */ chapterIndex
) =>
  ajax.get(
    "/getBookContent?url=" +
      encodeURIComponent(bookUrl) +
      "&index=" +
      chapterIndex
  );

// webSocket
const search = (
  /** @type {string} */ searchKey,
  /** @type {(data: string) => void} */ onReceive,
  /** @type {() => void} */ onFinish
) => {

  const url = `${legado_webSocket_origin}/searchBook`;
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

// 源编辑API
// Http
const getSources = () =>
  isBookSource ? ajax.get("/getBookSources") : ajax.get("/getRssSources");

const saveSource = (data) =>
  isBookSource
    ? ajax.post("/saveBookSource", data)
    : ajax.post("/saveRssSource", data);

const saveSources = (data) =>
  isBookSource
    ? ajax.post("/saveBookSources", data)
    : ajax.post("/saveRssSources", data);

const deleteSource = (data) =>
  isBookSource
    ? ajax.post("/deleteBookSources", data)
    : ajax.post("/deleteRssSources", data);

// webSocket
const debug = (
  /** @type {string} */ sourceUrl,
  /** @type {string} */ searchKey,
  /** @type {(data: string) => void} */ onReceive,
  /** @type {() => void} */ onFinish
) => {

  const url = `${legado_webSocket_origin}/${
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

/**
 * 从阅读获取需要特定处理的书籍封面
 * @param {string} coverUrl
 */
const getProxyCoverUrl = (coverUrl) => {
  if(coverUrl.startsWith(legado_http_origin)) return coverUrl
  return legado_http_origin + "/cover?path=" + encodeURIComponent(coverUrl);
}
/**
 * 从阅读获取需要特定处理的图片
 * @param {string} src
 * @param {number|`${number}`} width
 */
const getProxyImageUrl = (src, width) => {
  if (src.startsWith(legado_http_origin)) return src
  return (
    legado_http_origin +
    "/image?path=" +
    encodeURIComponent(src) +
    "&url=" +
    encodeURIComponent(sessionStorage.getItem("bookUrl")) +
    "&width=" +
    width
  );
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

  testLeagdoHttpUrlConnection,
  setLeagdoHttpUrl,
  legado_http_origin,
};
