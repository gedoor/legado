import { createWebHashHistory, createRouter } from 'vue-router'
import { bookRoutes } from './bookRouter'
import { sourceRoutes } from './sourceRouter'

const router = createRouter({
  //   history: createWebHistory(process.env.BASE_URL),
  history: createWebHashHistory(),
  routes: [bookRoutes, sourceRoutes].flat(),
})

router.afterEach(to => {
  if (to.name == 'shelf') document.title = '书架'
})

import { createApp } from 'vue'
import App from '@/App.vue'
import store, { useBookStore } from '@/store'

//  init pinia instance
createApp(App).use(store)
router.beforeEach((to, from, next) => {
  // 自动加载阅读配置
  useBookStore()
    .loadWebConfig()
    .then(() => next())
    .catch(() => next())
})

export default router
