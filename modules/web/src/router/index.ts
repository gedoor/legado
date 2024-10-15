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

export default router
