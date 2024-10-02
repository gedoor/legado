import { createApp } from "vue";
import App from "@/App.vue";
import sourceRouter from "@/router/sourceRouter";
import store from "@/store";

createApp(App).use(store).use(sourceRouter).mount("#app");
