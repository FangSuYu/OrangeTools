import { createApp } from 'vue'
import { createPinia } from 'pinia'

// 1. 引入 Element Plus 和 它的 CSS 样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 【新增】引入全局样式
import '@/styles/index.scss'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 2. 告诉 Vue 使用 Element Plus
app.use(ElementPlus)

app.mount('#app')
