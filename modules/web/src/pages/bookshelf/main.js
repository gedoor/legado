import { createApp } from 'vue'
import App from '@/App.vue'
import bookRouter from '@/router/bookRouter'
import store from '@/store'
import 'element-plus/theme-chalk/dark/css-vars.css'

createApp(App).use(store).use(bookRouter).mount('#app')

// 同步Element PLUS 夜间模式
watch(
  () => useBookStore().isNight,
  isNight => {
    if (isNight) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  },
)
