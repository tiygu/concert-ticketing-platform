import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '../api/auth'
import { login as loginApi, register as registerApi, refreshToken as refreshApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const userInfo = ref<UserInfo | null>(null)

  // Load userInfo from localStorage on init
  const stored = localStorage.getItem('userInfo')
  if (stored) {
    try {
      userInfo.value = JSON.parse(stored)
    } catch {
      localStorage.removeItem('userInfo')
    }
  }

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  function saveToStorage(token: string, refresh: string, user: UserInfo) {
    accessToken.value = token
    refreshToken.value = refresh
    userInfo.value = user
    localStorage.setItem('accessToken', token)
    localStorage.setItem('refreshToken', refresh)
    localStorage.setItem('userInfo', JSON.stringify(user))
  }

  async function login(username: string, password: string): Promise<boolean> {
    try {
      const res = await loginApi({ username, password })
      if (res.data.code === 200 && res.data.data) {
        const d = res.data.data
        saveToStorage(d.accessToken, d.refreshToken, d.userInfo)
        return true
      }
      return false
    } catch {
      return false
    }
  }

  async function register(data: { username: string; password: string; phone: string; email: string }): Promise<boolean> {
    try {
      const res = await registerApi(data)
      if (res.data.code === 200 && res.data.data) {
        const d = res.data.data
        saveToStorage(d.accessToken, d.refreshToken, d.userInfo)
        return true
      }
      return false
    } catch {
      return false
    }
  }

  async function refreshAccessToken(): Promise<boolean> {
    if (!refreshToken.value) return false
    try {
      const res = await refreshApi(refreshToken.value)
      if (res.data.code === 200 && res.data.data) {
        const d = res.data.data
        saveToStorage(d.accessToken, d.refreshToken, d.userInfo)
        return true
      }
      return false
    } catch {
      return false
    }
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    userInfo.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  function initFromStorage() {
    const token = localStorage.getItem('accessToken')
    const refresh = localStorage.getItem('refreshToken')
    const user = localStorage.getItem('userInfo')
    if (token) accessToken.value = token
    if (refresh) refreshToken.value = refresh
    if (user) {
      try {
        userInfo.value = JSON.parse(user)
      } catch {
        localStorage.removeItem('userInfo')
      }
    }
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    isAdmin,
    login,
    register,
    refreshAccessToken,
    logout,
    initFromStorage
  }
})
