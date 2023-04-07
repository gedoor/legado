import sourceEditor from "../views/SourceEditor.vue";
import { createWebHashHistory, createRouter } from "vue-router";

export const sourceRoutes = [
  {
    path: "/bookSource",
    name: "book-home",
    component: sourceEditor,
  },
  {
    path: "/rssSource",
    name: "rss-home",
    component: sourceEditor,
  },
];

const router = createRouter({
  //   history: createWebHistory(process.env.BASE_URL),
  history: createWebHashHistory(),
  routes: sourceRoutes,
});

export default router;
