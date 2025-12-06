import { createApp } from 'vue'
import { createPinia } from 'pinia'

// 1. 先引入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 2. 引入我们的全局样式
// 这一行非常重要！它包含了 reset.scss 和 theme.scss (定义了所有变量)
import '@/styles/index.scss'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
