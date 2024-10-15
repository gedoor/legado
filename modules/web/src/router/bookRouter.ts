import { createWebHashHistory, createRouter } from 'vue-router'

export const bookRoutes = [
  {
    path: '/',
    name: 'shelf',
    component: () => import('../views/BookShelf.vue'),
  },
  {
    path: '/chapter',
    name: 'chapter',
    component: () => import('../views/BookChapter.vue'),
  },
]

const router = createRouter({
  // mode: "history",
  history: createWebHashHistory(),
  routes: bookRoutes,
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
