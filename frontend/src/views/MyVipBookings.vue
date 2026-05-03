<template>
  <UserLayout>
    <h1 class="text-2xl font-bold mb-6">我的预约</h1>

    <div class="flex gap-2 mb-6">
      <button
        v-for="opt in filterOptions"
        :key="opt.value"
        class="px-4 py-2 rounded-lg text-sm transition-colors"
        :class="activeFilter === opt.value ? 'bg-purple-500/30 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'"
        @click="activeFilter = opt.value"
      >{{ opt.label }}</button>
    </div>

    <LoadingOverlay v-if="loading" />
    <template v-else-if="filteredBookings.length">
      <div class="space-y-4">
        <div v-for="b in filteredBookings" :key="b.id" class="glass-card rounded-2xl p-6 flex justify-between items-center">
          <div>
            <StatusTag :type="auditTag(b.auditStatus)" :label="auditText(b.auditStatus)" class="mb-1" />
            <h3 class="font-bold text-lg">{{ b.packageName || 'VIP套餐' }}</h3>
            <p class="text-sm text-gray-400">使用日期: {{ b.useDate }} | 预约时间: {{ formatDate(b.bookingTime) }}</p>
            <p v-if="b.adminReply" class="text-sm text-gray-500 mt-1">回复: {{ b.adminReply }}</p>
          </div>
        </div>
      </div>
    </template>
    <EmptyState v-else description="暂无预约" />
  </UserLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMyVipBookings, type VipBookingItem } from '../api/vipBookings'
import { useToast } from '../composables/useToast'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'

const toast = useToast()
const bookings = ref<(VipBookingItem & { packageName?: string })[]>([])
const loading = ref(true)
const activeFilter = ref('ALL')

const filterOptions = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

const filteredBookings = computed(() => {
  if (activeFilter.value === 'ALL') return bookings.value
  return bookings.value.filter(b => b.auditStatus === activeFilter.value)
})

function auditText(s: string) {
  const m: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return m[s] || s
}
function auditTag(s: string) {
  const m: Record<string, 'warning'|'success'|'danger'> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return m[s] || 'info'
}
function formatDate(v: string | null) {
  if (!v) return ''
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

onMounted(async () => {
  loading.value = true
  try { bookings.value = (await getMyVipBookings()).data.data || [] }
  catch { toast.error('加载失败') }
  finally { loading.value = false }
})
</script>
