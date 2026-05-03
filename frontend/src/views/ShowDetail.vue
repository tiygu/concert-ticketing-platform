<template>
  <UserLayout v-if="!error">
    <LoadingOverlay v-if="loading" />

    <template v-else-if="show">
      <div class="glass-card rounded-2xl p-6 mb-6">
        <div class="flex flex-col md:flex-row gap-6">
          <div class="w-full md:w-48 h-64 rounded-xl bg-gradient-to-br from-purple-600 to-pink-500 flex items-center justify-center flex-shrink-0">
            <img v-if="resolveCover(show.coverImage)" :src="resolveCover(show.coverImage)" class="w-full h-full object-cover rounded-xl" />
            <span v-else class="text-white text-5xl font-bold">LIVE</span>
          </div>
          <div class="flex-1">
            <StatusTag :type="tagType(show)" :label="labelText(show)" class="mb-2" />
            <h2 class="text-2xl font-bold mb-2">{{ show.showName || show.title }}</h2>
            <p class="text-gray-400">{{ formatDate(show.showTime) }}</p>
            <p class="text-gray-400">{{ show.venue }}</p>
            <div class="text-right mt-2">
              <p class="text-sm text-gray-400">票价范围</p>
              <p class="text-3xl font-bold text-neon-cyan font-orbitron">{{ show.priceRange || '¥' + show.ticketPrice }}</p>
            </div>
            <div class="grid grid-cols-4 gap-3 mt-4">
              <div v-for="z in zones" :key="z.name" class="p-3 rounded-lg bg-white/5 text-center">
                <p class="text-xs text-gray-400">{{ z.name }}</p>
                <p class="text-lg font-bold" :class="z.color">¥{{ z.price }}</p>
                <p class="text-xs" :class="z.stock > 10 ? 'text-green-400' : 'text-red-400'">剩余 {{ z.stock }} 张</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Seat Map + Order -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 glass-card rounded-2xl p-6">
          <h3 class="text-lg font-bold mb-4">在线选座</h3>
          <div class="stage-glow rounded-t-3xl p-4 mb-6 text-center">
            <div class="w-full h-12 bg-gradient-to-b from-purple-500/30 to-transparent rounded-t-2xl flex items-center justify-center border-t-2 border-purple-400/50">
              <span class="font-bold tracking-widest">舞台 STAGE</span>
            </div>
          </div>
          <div class="grid grid-cols-[repeat(13,minmax(0,1fr))] gap-1 mb-6 justify-items-center">
            <template v-for="row in seatRows" :key="row.label">
              <span class="text-xs text-gray-500 self-center">{{ row.label }}</span>
              <button
                v-for="s in row.seats"
                :key="s.id"
                class="w-7 h-7 rounded text-[10px] transition-all duration-200"
                :class="seatClass(s)"
                :disabled="!s.available"
                @click="toggleSeat(s)"
              >{{ (s.seatNumber || '').slice(-2) }}</button>
            </template>
          </div>
          <div class="flex justify-center gap-5 text-sm">
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-gray-600 inline-block" /> 已售</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-cyan-400/40 border border-cyan-400/50 inline-block" /> 可选</span>
            <span class="flex items-center gap-1.5"><span class="w-4 h-4 rounded bg-gradient-to-br from-pink-500 to-purple-600 inline-block" /> 已选</span>
          </div>
        </div>

        <div class="glass-card rounded-2xl p-6">
          <h3 class="text-lg font-bold mb-4">订单信息</h3>
          <div v-if="selectedSeats.length === 0" class="text-gray-400 text-sm mb-4">请选择座位</div>
          <div v-else class="space-y-2 mb-4 max-h-48 overflow-y-auto">
            <div v-for="s in selectedSeats" :key="s.id" class="flex justify-between text-sm bg-white/5 rounded-lg p-3">
              <span>{{ s.seatNumber }}</span>
              <span class="text-neon-cyan font-bold">¥{{ s.price }}</span>
            </div>
          </div>
          <div class="border-t border-white/10 pt-4 mb-4">
            <div class="flex justify-between mb-2"><span class="text-gray-400">座位数量</span><span class="font-bold">{{ selectedSeats.length }}</span></div>
            <div class="flex justify-between text-xl"><span>总计</span><span class="font-bold text-neon-cyan font-orbitron">¥{{ totalPrice }}</span></div>
          </div>
          <BaseButton variant="primary" size="lg" class="w-full" :disabled="selectedSeats.length === 0" :loading="submitting" @click="handleBuy">提交订单</BaseButton>
          <p class="text-xs text-gray-500 mt-3 text-center">请在15分钟内完成支付，超时将释放座位</p>
        </div>
      </div>

      <!-- Reviews -->
      <div class="glass-card rounded-2xl p-6 mt-6">
        <h3 class="text-lg font-bold mb-4">观众评价</h3>
        <div v-if="reviews.length" class="space-y-4">
          <div v-for="r in reviews" :key="r.id" class="border-b border-white/5 pb-4 last:border-0">
            <div class="flex items-center gap-3 mb-2">
              <span class="text-sm font-bold text-white">{{ r.username }}</span>
              <StarRating :model-value="r.rating" :readonly="true" />
              <span class="text-xs text-gray-500">{{ formatDate(r.createdAt) }}</span>
            </div>
            <p class="text-sm text-gray-400">{{ r.content }}</p>
          </div>
        </div>
        <EmptyState v-else description="暂无评价" />
      </div>
    </template>

    <div v-else class="min-h-screen flex items-center justify-center">
      <div class="glass-card rounded-2xl p-8 text-center max-w-md">
        <p class="text-5xl mb-4 opacity-30">✘</p>
        <h2 class="text-xl font-bold mb-2">演出不存在</h2>
        <p class="text-gray-400 mb-6">该演出可能已下架或删除</p>
        <BaseButton variant="primary" @click="router.push('/')">返回首页</BaseButton>
      </div>
    </div>
  </UserLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getShow, type ShowItem } from '../api/shows'
import { getShowSeats, createOrder, type SeatItem } from '../api/orders'
import { getShowReviews, type ReviewItem } from '../api/reviews'
import { useToast } from '../composables/useToast'
import { useGlobalConfirm } from '../composables/useConfirm'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import StarRating from '../components/StarRating.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'
import BaseButton from '../components/BaseButton.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const confirm = useGlobalConfirm()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const show = ref<ShowItem | null>(null)
const seats = ref<SeatItem[]>([])
const reviews = ref<ReviewItem[]>([])
const loading = ref(true)
const error = ref(false)
const selectedSeats = ref<SeatItem[]>([])
const submitting = ref(false)

const totalPrice = computed(() => selectedSeats.value.reduce((s, x) => s + x.price, 0))

const zones = computed(() => {
  const prices = [...new Set(seats.value.map(s => s.price))].sort((a, b) => b - a)
  const names = ['VIP区', 'A区', 'B区', 'C区']
  const colors = ['text-yellow-400', 'text-purple-400', 'text-blue-400', 'text-cyan-400']
  return prices.map((p, i) => ({ name: names[i] || `${i+1}区`, price: p, color: colors[i] || 'text-white', stock: seats.value.filter(s => s.price === p && s.available).length }))
})

const seatRows = computed(() => {
  const rows = new Map<string, SeatItem[]>()
  for (const s of seats.value) {
    const row = s.seatNumber?.match(/[A-Za-z]+/)?.[0] || 'X'
    if (!rows.has(row)) rows.set(row, [])
    rows.get(row)!.push(s)
  }
  return Array.from(rows.entries()).sort(([a], [b]) => a.localeCompare(b)).map(([label, seatList]) => ({ label, seats: seatList.sort((a, b) => (a.seatNumber || '').localeCompare(b.seatNumber || '')) }))
})

function seatClass(s: SeatItem) {
  if (selectedSeats.value.find(x => x.id === s.id)) return 'bg-gradient-to-br from-pink-500 to-purple-600 shadow-lg shadow-pink-500/50'
  if (!s.available) return 'bg-gray-600 cursor-not-allowed'
  return 'bg-cyan-400/20 border border-cyan-400/50 hover:scale-110 hover:shadow-[0_0_12px] hover:shadow-neon-cyan/60'
}

function toggleSeat(s: SeatItem) {
  if (!s.available) return
  const idx = selectedSeats.value.findIndex(x => x.id === s.id)
  if (idx >= 0) selectedSeats.value.splice(idx, 1)
  else if (selectedSeats.value.length < 4) selectedSeats.value.push(s)
  else toast.warning('最多选择4个座位')
}

async function handleBuy() {
  if (selectedSeats.value.length === 0) return
  const ok = await confirm.open(`确认购买 ${selectedSeats.value.length} 个座位，总计 ¥${totalPrice.value}？`, '确认购票')
  if (!ok) return
  submitting.value = true
  try {
    const ticket = selectedSeats.value[0]
    await createOrder({ showId: ticket.showId, ticketId: ticket.id })
    toast.success('购票成功')
    router.push('/orders')
  } catch { toast.error('购票失败') } finally { submitting.value = false }
}

function resolveCover(img: string | null) {
  if (!img) return ''
  if (/^(https?:)?\/\//.test(img) || img.startsWith('data:')) return img
  if (img.startsWith('/uploads/')) return `${apiBaseUrl}${img}`
  return img
}

function labelText(s: ShowItem) {
  const st = (s.statusText || s.status || '').toUpperCase()
  if (st.includes('ON_SALE')) return '售票中'
  if (st.includes('UPCOMING')) return '即将开售'
  if (st.includes('ENDED')) return '已结束'
  return s.statusText || s.status || '未知'
}

function tagType(s: ShowItem) {
  const st = labelText(s)
  if (st === '售票中') return 'success' as const
  if (st === '即将开售') return 'warning' as const
  return 'info' as const
}

function formatDate(v: string | null) {
  if (!v) return ''
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id || Number.isNaN(id)) { error.value = true; loading.value = false; return }
  try {
    const [showRes, seatsRes, reviewsRes] = await Promise.all([getShow(id), getShowSeats(id), getShowReviews(id)])
    show.value = showRes.data.data
    seats.value = seatsRes.data.data || []
    reviews.value = reviewsRes.data.data || []
  } catch { error.value = true } finally { loading.value = false }
})
</script>
