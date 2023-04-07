import { createApp } from "vue";
import App from "@/App.vue";
import bookRouter from "@/router";
import store from "@/store";

createApp(App).use(store).use(bookRouter).mount("#app");

import("./config");
