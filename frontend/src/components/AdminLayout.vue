<template>
  <div class="min-h-screen bg-[linear-gradient(135deg,#0a0a0f_0%,#1a0a2e_50%,#0f0f23_100%)] text-white flex">
    <!-- Sidebar -->
    <aside class="w-60 min-h-screen glass-card border-r border-white/10 flex-shrink-0 flex flex-col">
      <div class="p-6 border-b border-white/10">
        <h1 class="font-orbitron text-lg font-bold neon-glow">STAR TICKET</h1>
        <p class="text-xs text-gray-400 mt-1">管理控制台</p>
      </div>
      <nav class="flex-1 p-4 space-y-1">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm transition-colors"
          :class="isActive(item.path) ? 'bg-purple-500/20 text-white' : 'text-gray-400 hover:text-white hover:bg-white/5'"
        >
          <span>{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="p-4 border-t border-white/10">
        <router-link to="/" class="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors px-4 py-2">
          <span>←</span> 返回首页
        </router-link>
      </div>
    </aside>

    <!-- Main content -->
    <div class="flex-1 flex flex-col min-w-0">
      <header class="glass-card border-b border-white/10 sticky top-0 z-10 px-6 py-4">
        <h2 class="text-xl font-bold">{{ title }}</h2>
      </header>
      <main class="flex-1 p-6">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

defineProps<{ title?: string }>()

const route = useRoute()

const menuItems = [
  { path: '/admin/statistics', label: '数据统计', icon: '📊' },
  { path: '/admin/users', label: '用户管理', icon: '👤' },
  { path: '/admin/shows', label: '演出管理', icon: '🎵' },
  { path: '/admin/orders', label: '订单管理', icon: '📋' },
  { path: '/admin/notices', label: '公告管理', icon: '📢' },
  { path: '/admin/vip/packages', label: 'VIP套餐', icon: '👑' },
  { path: '/admin/vip/bookings', label: 'VIP预约', icon: '📅' }
]

function isActive(path: string) {
  return route.path.startsWith(path)
}
</script>
