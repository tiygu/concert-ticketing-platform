<template>
  <UserLayout>
    <!-- Hero -->
    <div class="grid grid-cols-1 md:grid-cols-[1fr_380px] gap-8 items-end mb-8">
      <div>
        <p class="text-neon-pink text-sm font-bold tracking-widest mb-2">LIVE CONCERT TICKETS</p>
        <h1 class="font-orbitron text-5xl md:text-7xl font-black leading-tight tracking-tight">演唱會票務</h1>
        <p class="text-gray-400 text-lg mt-4 max-w-xl">发现热门演出，快速查看余票与票价，锁定你的下一场现场记忆。</p>
      </div>
      <div class="relative">
        <span class="absolute left-4 top-1/2 -translate-y-1/2 text-neon-pink text-xl">⌕</span>
        <input
          v-model="keyword"
          class="input-dark pl-11 rounded-full"
          placeholder="搜索演出名称或场馆"
          @keyup.enter="triggerSearch"
        />
      </div>
    </div>

    <!-- Notices -->
    <section class="mb-10">
      <h2 class="text-2xl font-bold text-white mb-6">最新公告</h2>
      <LoadingOverlay v-if="noticesLoading" />
      <div v-else-if="notices.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="n in notices" :key="n.id" class="glass-card rounded-2xl p-5 hover:-translate-y-1 transition-all duration-300 hover:shadow-lg hover:shadow-purple-500/20">
          <div class="flex justify-between items-start gap-3 mb-3">
            <h3 class="text-lg font-bold text-neon-cyan">{{ n.title }}</h3>
            <span class="text-xs text-gray-500 flex-shrink-0">{{ formatDate(n.publishTime) }}</span>
          </div>
          <p class="text-sm text-gray-400 leading-relaxed line-clamp-3">{{ n.content }}</p>
        </div>
      </div>
      <EmptyState v-else description="暂无公告" />
    </section>

    <!-- Shows -->
    <section>
      <LoadingOverlay v-if="loading" />
      <div v-else-if="shows.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="s in shows"
          :key="s.id"
          class="glass-card rounded-2xl overflow-hidden cursor-pointer hover:scale-[1.02] hover:border-neon-cyan/40 transition-all duration-300 group"
          @click="goToShow(s.id)"
        >
          <div class="aspect-video bg-gradient-to-br from-pink-500/80 to-purple-600/80 flex items-center justify-center relative overflow-hidden">
            <img v-if="resolveCover(s.coverImage)" :src="resolveCover(s.coverImage)" class="w-full h-full object-cover" />
            <span v-else class="text-white text-3xl font-bold tracking-widest opacity-80">LIVE</span>
          </div>
          <div class="p-5">
            <div class="flex justify-between items-start gap-3 mb-3">
              <h2 class="text-xl font-bold text-white leading-snug">{{ s.showName || s.title }}</h2>
              <StatusTag :type="statusTagType(s)" :label="displayStatus(s)" />
            </div>
            <div class="space-y-2 text-sm text-gray-400 mb-4">
              <p class="flex justify-between"><span class="text-neon-cyan/80 font-bold">场馆</span>{{ s.venue }}</p>
              <p class="flex justify-between"><span class="text-neon-cyan/80 font-bold">时间</span>{{ formatDate(s.showTime) }}</p>
              <p class="flex justify-between"><span class="text-neon-cyan/80 font-bold">票价</span>{{ formatPrice(s) }}</p>
            </div>
            <div class="flex justify-between items-center pt-3 border-t border-white/10">
              <span class="text-xs text-neon-cyan/80 font-bold">可售座位</span>
              <span class="text-white font-bold">{{ s.availableSeats }} / {{ s.totalSeats }}</span>
            </div>
          </div>
        </div>
      </div>
      <EmptyState v-else description="暂无匹配的演出" />

      <div class="mt-8 flex justify-center">
        <BasePagination
          v-if="total > 0"
          :current="page"
          :page-size="pageSize"
          :total="total"
          @change="goToPage"
        />
      </div>
    </section>
  </UserLayout>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getShows, type ShowItem } from '../api/shows'
import { getNotices, type NoticeItem } from '../api/notices'
import { useToast } from '../composables/useToast'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'
import BasePagination from '../components/BasePagination.vue'

const router = useRouter()
const toast = useToast()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const shows = ref<ShowItem[]>([])
const notices = ref<NoticeItem[]>([])
const loading = ref(false)
const noticesLoading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(9)
const total = ref(0)
let searchTimer: ReturnType<typeof setTimeout> | undefined

function resolveCover(coverImage: string | null) {
  if (!coverImage) return ''
  if (/^(https?:)?\/\//.test(coverImage) || coverImage.startsWith('data:')) return coverImage
  if (coverImage.startsWith('/uploads/')) return `${apiBaseUrl}${coverImage}`
  return coverImage
}

function displayStatus(s: ShowItem) {
  const st = (s.statusText || s.status || '').toUpperCase()
  if (st.includes('ON_SALE') || st.includes('SELLING') || st.includes('售票')) return '售票中'
  if (st.includes('UPCOMING') || st.includes('即将')) return '即将开售'
  if (st.includes('ENDED') || st.includes('FINISHED') || st.includes('结束')) return '已结束'
  return s.statusText || s.status || '未知'
}

function statusTagType(s: ShowItem) {
  const st = displayStatus(s)
  if (st === '售票中') return 'success'
  if (st === '即将开售') return 'warning'
  return 'info'
}

function formatDate(value: string | null) {
  if (!value) return '时间待定'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

function formatPrice(s: ShowItem) {
  if (s.priceRange) return s.priceRange.startsWith('¥') ? s.priceRange : `¥${s.priceRange}`
  return `¥${Number(s.ticketPrice || 0).toFixed(2)}`
}

function triggerSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  page.value = 1
  fetchShows()
}

async function fetchShows() {
  loading.value = true
  try {
    const res = await getShows({ page: page.value, pageSize: pageSize.value, keyword: keyword.value.trim() || undefined })
    const r = res.data.data
    shows.value = r.records || []
    total.value = r.total || 0
  } catch {
    toast.error('演出列表加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchNotices() {
  noticesLoading.value = true
  try {
    const res = await getNotices()
    notices.value = res.data.data || []
  } catch { /* ignore */ } finally {
    noticesLoading.value = false
  }
}

function goToShow(id: number) { router.push(`/shows/${id}`) }
function goToPage(p: number) { page.value = p; fetchShows() }

watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 1; fetchShows() }, 400)
})

onMounted(() => { fetchShows(); fetchNotices() })
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>
