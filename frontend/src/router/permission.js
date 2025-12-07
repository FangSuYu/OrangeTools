import router from '@/router' // 建议使用 @/router 引用，确保路径正确
import { useUserStore } from '@/stores/modules/user'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 1. 配置 NProgress
NProgress.configure({ showSpinner: false })

// 2. 白名单：不需要登录就能访问的页面
const whiteList = ['/login', '/404', '/500']

// 3. 全局前置守卫
router.beforeEach(async (to, from, next) => {
  // 3.1 开启进度条
  NProgress.start()

  // 3.2 【新增】动态设置网页标题
  const appTitle = 'OrangeTools'
  if (to.meta.title) {
    document.title = `${to.meta.title} - ${appTitle}`
  } else {
    document.title = appTitle
  }

  // 3.3 获取 Token
  const userStore = useUserStore()
  const hasToken = userStore.token

  if (hasToken) {
    if (to.path === '/login') {
      // 已登录，去登录页 -> 踢回首页
      next({ path: '/' })
      NProgress.done()
    } else {
      // 已登录，去其他页 -> 放行
      // (未来可以在这里加 getUserInfo 的逻辑)
      next()
    }
  } else {
    /* 没有 Token */
    if (whiteList.indexOf(to.path) !== -1) {
      // 在白名单 -> 放行
      next()
    } else {
      // 不在白名单 -> 踢回登录页
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

// 4. 全局后置钩子
router.afterEach(() => {
  NProgress.done()
})
