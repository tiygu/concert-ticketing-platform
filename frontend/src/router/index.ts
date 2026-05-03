import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import HomePage from '../views/HomePage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('../views/OrderList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/users',
    name: 'UserManage',
    component: () => import('../views/admin/UserManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/ProfilePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    name: 'Home',
    component: HomePage,
    meta: { requiresAuth: true }
  },
  {
    path: '/shows/:id',
    name: 'ShowDetail',
    component: () => import('../views/ShowDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/orders',
    name: 'OrderManage',
    component: () => import('../views/admin/OrderManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/shows',
    name: 'ShowManage',
    component: () => import('../views/admin/ShowManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'OrderManage',
    component: () => import('../views/admin/OrderManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/notices',
    name: 'NoticeManage',
    component: () => import('../views/admin/NoticeManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/vip/packages',
    name: 'VipPackageManage',
    component: () => import('../views/admin/VipPackageManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterPage.vue'),
    meta: { guest: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const accessToken = localStorage.getItem('accessToken')
  const userInfoStr = localStorage.getItem('userInfo')
  let isAdmin = false

  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      isAdmin = userInfo.role === 'ADMIN'
    } catch {
      // ignore parse error
    }
  }

  // If visiting guest-only routes (login/register) while logged in, redirect to home
  if (to.meta.guest && accessToken) {
    next('/')
    return
  }

  // If route requires auth and user is not logged in
  if (to.meta.requiresAuth && !accessToken) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // If route requires admin and user is not admin
  if (to.meta.requiresAdmin && !isAdmin) {
    next('/')
    return
  }

  next()
})

export default router
