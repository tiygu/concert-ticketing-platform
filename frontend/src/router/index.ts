import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import { useAuthStore } from '../stores/auth'

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
    path: '/admin/notices',
    name: 'NoticeManage',
    component: () => import('../views/admin/NoticeManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/statistics',
    name: 'Statistics',
    component: () => import('../views/admin/Statistics.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/vip/packages',
    name: 'VipPackageManage',
    component: () => import('../views/admin/VipPackageManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/vip/bookings',
    name: 'VipBookingManage',
    component: () => import('../views/admin/VipBookingManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/vip',
    name: 'VipCenter',
    component: () => import('../views/VipCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/vip/bookings',
    name: 'MyVipBookings',
    component: () => import('../views/MyVipBookings.vue'),
    meta: { requiresAuth: true }
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

// Navigation guard — uses Pinia store for consistent auth state
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const { accessToken, isAdmin } = authStore

  if (to.meta.guest && accessToken) {
    next('/')
    return
  }

  if (to.meta.requiresAuth && !accessToken) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !isAdmin) {
    next('/')
    return
  }

  next()
})

export default router
