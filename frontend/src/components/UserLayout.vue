<template>
  <div class="min-h-screen bg-[linear-gradient(135deg,#0a0a0f_0%,#1a0a2e_50%,#0f0f23_100%)] text-white">
    <header class="relative z-10 glass-card border-b border-white/10 sticky top-0">
      <div class="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="w-10 h-10 rounded-full bg-gradient-to-br from-pink-500 to-purple-600 flex items-center justify-center">
            <span class="text-white font-bold">T</span>
          </div>
          <div>
            <h1 class="font-orbitron text-xl font-bold neon-glow">STAR TICKET</h1>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <slot name="nav">
            <router-link v-if="isAdmin" to="/admin/statistics" class="text-sm text-gray-300 hover:text-cyan-400 transition-colors">数据统计</router-link>
            <router-link to="/profile" class="text-sm text-gray-300 hover:text-cyan-400 transition-colors">个人中心</router-link>
            <router-link to="/orders" class="text-sm text-gray-300 hover:text-cyan-400 transition-colors">我的订单</router-link>
            <router-link to="/vip" class="text-sm text-gray-300 hover:text-cyan-400 transition-colors">VIP权益</router-link>
            <router-link to="/vip/bookings" class="text-sm text-gray-300 hover:text-cyan-400 transition-colors">我的预约</router-link>
            <button class="text-sm text-red-400 hover:text-red-300 transition-colors ml-2" @click="logout">退出</button>
          </slot>
        </div>
      </div>
    </header>
    <main class="relative z-10 max-w-7xl mx-auto px-6 py-8">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>
