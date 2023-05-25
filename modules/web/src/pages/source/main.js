import { createApp } from "vue";
import App from "@/App.vue";
import sourceRouter from "@/router";
import store from "@/store";

createApp(App).use(store).use(sourceRouter).mount("#app");
