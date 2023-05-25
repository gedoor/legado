import { createWebHashHistory, createRouter } from "vue-router";

export const bookRoutes = [
  {
    path: "/",
    name: "shelf",
    component: () => import("../views/BookShelf.vue"),
  },
  {
    path: "/chapter",
    name: "chapter",
    component: () => import("../views/BookChapter.vue"),
  },
];

const router = createRouter({
  // mode: "history",
  history: createWebHashHistory(),
  // @ts-ignore
  routes: bookRoutes,
});

export default router;
