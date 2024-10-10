import ajax from "./axios";
import { ElMessage } from "element-plus/es";

/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/api */
/** https://github.com/gedoor/legado/tree/master/app/src/main/java/io/legado/app/web */

/**@type string */
export let legado_http_entry_point;
/**@type string */
export let legado_webSocket_entry_point;

/**
 * @param  {string|URL} http_url
 * @returns {URL}
 * @throws {Error}
 */
export const validatorHttpUrl = (http_url) => {
  try {
    const url = new URL(http_url);
    if (url.toString() === legado_http_entry_point)
      throw new Error("Please input different url: " + legado_http_entry_point);
    const { protocol } = url;
    if (!protocol.startsWith("http"))
      throw new Error("Expect http:/https: protocol but " + protocol);
    return url;
  } catch (e) {
    if (localStorage.getItem("remoteUrl") == http_url) {
      localStorage.removeItem("remoteUrl");
      console.warn("Remove remoteUrl from localStorage");
    }
    throw new Error("Fail to parse Leagdo remoteUrl " + http_url, { cause: e });
  }
};
/**
 * @param  {string|URL} http_url
 * @returns
 */
export const setLeagdoHttpUrl = (http_url) => {
  let url = new URL(location.origin); //默认当前网址的origin部分
  try {
    url = validatorHttpUrl(http_url);
  } catch (e) {
    console.warn(e);
    console.info(
      "setLeagdoHttpUrl: FallBack to location.origin: " + location.origin,
    );
  }
  const { protocol, port, href } = url;
  // websocket服务端口 为http服务端口 + 1
  let legado_webSocket_port, legado_webSocket_protocol;
  if (port !== "") {
    legado_webSocket_port = String(Number(port) + 1);
  } else {
    legado_webSocket_port = protocol.startsWith("https:") ? "444" : "81";
  }
  // websocket协议是否为加密版本
  legado_webSocket_protocol = protocol.startsWith("https:")
    ? "wss://"
    : "ws://";

  ajax.defaults.baseURL = href;
  //持久化
  localStorage.setItem("remoteUrl", href);
  legado_http_entry_point = href;

  url.protocol = legado_webSocket_protocol;
  url.port = legado_webSocket_port;
  legado_webSocket_entry_point = url.href;

  console.info("legado_api_config:");
  console.table({
    "http API入口": legado_http_entry_point,
    "webSocket API入口": legado_webSocket_entry_point,
  });
};

// 手动初始化 阅读web服务地址
setLeagdoHttpUrl(ajax.defaults.baseURL);
/**
 * @param  {string|URL|undefined} http_url
 * @returns
 */
const testLeagdoHttpUrlConnection = async (http_url = legado_http_entry_point) => {
  const { data = {} } = await ajax.get("/getReadConfig", {
    baseURL: http_url.toString(),
    timeout: 3000,
  });
  // 返回结果应该是JSON 并有键值isSuccess
  try {
    if ("isSuccess" in data) return data.data;
    throw new Error("ReadConfig后端返回格式错误");
  } catch {
    throw new Error("ReadConfig后端返回格式错误");
  }
};

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
const getReadConfig = () => ajax.get("/getReadConfig", { timeout: 3000 });
const saveReadConfig = (config) => ajax.post("/saveReadConfig", config);

const saveBookProgress = (bookProgress) =>
  ajax.post("/saveBookProgress", bookProgress);

const saveBookProgressWithBeacon = (bookProgress) => {
  if (!bookProgress) return;
  // 常规请求可能会被取消 使用Fetch keep-alive 或者 navigator.sendBeacon
  navigator.sendBeacon(
    new URL("/saveBookProgress", legado_http_entry_point),
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

// webSocket
const search = (
  /** @type {string} */ searchKey,
  /** @type {(data: string) => void} */ onReceive,
  /** @type {() => void} */ onFinish,
) => {
  const socket = new WebSocket(
    new URL("/searchBook", legado_webSocket_entry_point),
  );

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
  /** @type {() => void} */ onFinish,
) => {
  const url = new URL(
    `/${isBookSource ? "bookSource" : "rssSource"}Debug`,
    legado_webSocket_entry_point,
  );

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
  if (coverUrl.startsWith(legado_http_entry_point)) return coverUrl;
  return new URL(
    "/cover?path=" + encodeURIComponent(coverUrl),
    legado_http_entry_point,
  ).href;
};
/**
 * 从阅读获取需要特定处理的图片
 * @param {string} src
 * @param {number|`${number}`} width
 */
const getProxyImageUrl = (src, width) => {
  if (src.startsWith(legado_http_entry_point)) return src;
  return new URL(
    "/image?path=" +
      encodeURIComponent(src) +
      "&url=" +
      encodeURIComponent(sessionStorage.getItem("bookUrl")) +
      "&width=" +
      width,
    legado_http_entry_point,
  );
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

  getProxyCoverUrl,
  getProxyImageUrl,

  testLeagdoHttpUrlConnection,
};
