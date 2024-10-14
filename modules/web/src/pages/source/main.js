import { createApp } from 'vue'
import App from '@/App.vue'
import sourceRouter from '@/router/sourceRouter'
import store from '@/store'
import 'element-plus/theme-chalk/dark/css-vars.css'

createApp(App).use(store).use(sourceRouter).mount('#app')
