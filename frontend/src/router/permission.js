import router from './index'
import { useUserStore } from '@/stores/modules/user'
import NProgress from 'nprogress' // 引入进度条
import 'nprogress/nprogress.css' // 引入进度条样式

// 配置 NProgress (关闭右上角的螺旋加载动画，只留进度条)
NProgress.configure({ showSpinner: false })

// 白名单：不需要登录就能访问的页面
const whiteList = ['/login', '/404']

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 1. 开启进度条
  NProgress.start()

  // 2. 获取 Token (必须在守卫里获取 store，否则会报错)
  const userStore = useUserStore()
  const hasToken = userStore.token

  if (hasToken) {
    if (to.path === '/login') {
      // 2.1 如果已登录，又想去登录页 -> 强制踢回首页
      next({ path: '/' })
      NProgress.done() // 结束进度条（因为不会触发 afterEach 了）
    } else {
      // 2.2 已登录，去其他页面 -> 放行
      // TODO: 这里以后会加“获取用户信息”的逻辑，现在先直接放行
      next()
    }
  } else {
    /* 没有 Token */
    if (whiteList.indexOf(to.path) !== -1) {
      // 3.1 去白名单页面 (登录、注册) -> 放行
      next()
    } else {
      // 3.2 去受保护页面 -> 踢回登录页，并带上 redirect 参数
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

// 全局后置钩子
router.afterEach(() => {
  // 4. 关闭进度条
  NProgress.done()
})
