import { createRouter, createWebHistory } from 'vue-router'
/* 引入 Layout */
import Layout from '@/layout/index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          name: 'Dashboard',
          meta: { title: '首页', icon: 'House' }
        },
        {
          path: 'tools/course', // 注意路径不要以 / 开头，因为是子路由
          name: 'CourseTool',
          component: () => import('@/views/tools/course/index.vue'),
          meta: { title: '课表统计助手', icon: 'DataAnalysis' } // 这里定义图标
        },
        {
          path: 'community/feedback',
          name: 'Feedback',
          component: () => import('@/views/community/feedback/index.vue'),
          meta: { title: '需求许愿墙', icon: 'ChatLineSquare' }
        },
        {
          path: 'community/contributors',
          name: 'Contributors',
          component: () => import('@/views/community/contributors/index.vue'),
          meta: { title: '贡献者墙', icon: 'Medal' }
        },
        {
          path: '/profile', // 访问路径
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
          meta: { title: '个人中心', icon: 'User' },
          hidden: true // 【关键】加上这个属性，侧边栏就不会渲染出这个菜单
        }
      ]
    },
    //【新增】登录注册页 (全屏，不带侧边栏)
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/index.vue'), // 我们马上创建这个文件
      meta: { title: '登录 - OrangeTools' }
    }
  ]
})

export default router
