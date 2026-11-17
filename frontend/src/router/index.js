import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import Layout from '../views/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/books',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '数据统计', roles: ['admin'] }
      },
      {
        path: 'books',
        name: 'BookList',
        component: () => import('../views/BookList.vue'),
        meta: { title: '图书检索' }
      },
      {
        path: 'book-manage',
        name: 'BookManage',
        component: () => import('../views/BookManage.vue'),
        meta: { title: '图书管理', roles: ['admin'] }
      },
      {
        path: 'categories',
        name: 'CategoryManage',
        component: () => import('../views/CategoryManage.vue'),
        meta: { title: '分类管理', roles: ['admin'] }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('../views/UserManage.vue'),
        meta: { title: '用户管理', roles: ['admin'] }
      },
      {
        path: 'borrows',
        name: 'BorrowManage',
        component: () => import('../views/BorrowManage.vue'),
        meta: { title: '借阅管理', roles: ['admin'] }
      },
      {
        path: 'reservations',
        name: 'ReservationManage',
        component: () => import('../views/ReservationManage.vue'),
        meta: { title: '预约队列', roles: ['admin'] }
      },
      {
        path: 'notices',
        name: 'NoticeList',
        component: () => import('../views/NoticeList.vue'),
        meta: { title: '系统公告' }
      },
      {
        path: 'logs',
        name: 'OperationLog',
        component: () => import('../views/OperationLog.vue'),
        meta: { title: '操作日志', roles: ['admin'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/books' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页；角色不满足时拦截并提示
router.beforeEach(to => {
  const store = useUserStore()
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '图书管理系统'
  if (to.path !== '/login' && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && store.isLoggedIn) {
    return store.isAdmin ? '/dashboard' : '/books'
  }
  if (to.meta.roles && !to.meta.roles.includes(store.role)) {
    ElMessage.error('无权访问该页面')
    return store.isAdmin ? '/dashboard' : '/books'
  }
  return true
})

export default router
