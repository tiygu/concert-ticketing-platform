import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import HomePage from '../views/HomePage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: HomePage
  },
  {
    path: '/shows/:id',
    name: 'ShowDetail',
    component: () => import('../views/ShowDetail.vue')
  },
  {
    path: '/admin/shows',
    name: 'ShowManage',
    component: () => import('../views/admin/ShowManage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
