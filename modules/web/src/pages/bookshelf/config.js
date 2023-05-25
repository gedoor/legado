import API from "@api";
import { useBookStore } from "@/store";
import "@/assets/bookshelf.css";

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
