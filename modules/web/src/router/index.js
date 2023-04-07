import { createWebHashHistory, createRouter } from "vue-router";
import { bookRoutes } from "./bookRouter";
import { sourceRoutes } from "./sourceRouter";

const router = createRouter({
  //   history: createWebHistory(process.env.BASE_URL),
  history: createWebHashHistory(),
  // @ts-ignore
  routes: bookRoutes.concat(sourceRoutes),
});

router.afterEach((to) => {
  if (to.name == "shelf") document.title = "书架";
});
export default router;
