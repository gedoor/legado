import { formatDate } from "@vueuse/shared";

export const isLegadoUrl = (/** @type {string} */ url) =>
  /,\s*\{/.test(url) ||
  !(
    url.startsWith("http") ||
    url.startsWith("data:") ||
    url.startsWith("blob:")
  );
/**
 * @param {string} src
 */
export function getImageFromLegado(src) {
  return (
    (import.meta.env.VITE_API || location.origin) +
    "/image?path=" +
    encodeURIComponent(src) +
    "&url=" +
    encodeURIComponent(sessionStorage.getItem("bookUrl")) +
    "&width=" +
    useBookStore().config.readWidth
  );
}
// @ts-ignore
export const dateFormat = (/** @type {number} */ t) => {
  let time = new Date().getTime();
  let offset = Math.floor((time - t) / 1000);
  let str = "";

  if (offset <= 30) {
    str = "刚刚";
  } else if (offset < 60) {
    str = offset + "秒前";
  } else if (offset < 3600) {
    str = Math.floor(offset / 60) + "分钟前";
  } else if (offset < 86400) {
    str = Math.floor(offset / 3600) + "小时前";
  } else if (offset < 2592000) {
    str = Math.floor(offset / 86400) + "天前";
  } else {
    str = formatDate(new Date(t), "YYYY-MM-DD");
  }
  return str;
};
