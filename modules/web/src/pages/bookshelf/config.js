import API from "@api";
import { useBookStore } from "@/store";
import "@/assets/bookshelf.css";

/**
 * pc移动端判断
 */
const bookStore = useBookStore();
bookStore.setMiniInterface(window.innerWidth < 750);
window.onresize = () => {
  bookStore.setMiniInterface(window.innerWidth < 750);
};
/**
 * 加载配置
 */
API.getReadConfig().then((res) => {
  var data = res.data.data;
  if (data) {
    const bookStore = useBookStore();
    let config = JSON.parse(data);
    let defaultConfig = bookStore.config;
    config = Object.assign(defaultConfig, config);
    bookStore.setConfig(config);
  }
});
