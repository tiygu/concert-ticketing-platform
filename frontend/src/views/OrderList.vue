<template>
  <UserLayout>
    <h1 class="text-2xl font-bold mb-6">我的订单</h1>
    <LoadingOverlay v-if="loading" />
    <template v-else-if="orders.length">
      <div class="space-y-4">
        <div v-for="o in orders" :key="o.id" class="glass-card rounded-2xl p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="w-16 h-16 rounded-lg bg-gradient-to-br from-purple-600 to-pink-500 flex items-center justify-center flex-shrink-0">
              <span class="text-white text-xl font-bold">L</span>
            </div>
            <div>
              <StatusTag :type="statusType(o.payStatus)" :label="statusText(o.payStatus)" class="mb-1" />
              <h3 class="font-bold text-lg">{{ o.showName }}</h3>
              <p class="text-sm text-gray-400">{{ formatDate(o.orderTime) }} | 座位: {{ o.seatNumber || '未指定' }}</p>
            </div>
          </div>
          <div class="flex items-center gap-4 flex-shrink-0">
            <p class="text-xl font-bold text-neon-cyan font-orbitron">¥{{ o.amount }}</p>
            <div class="flex gap-2">
              <BaseButton v-if="o.payStatus === 'PENDING'" variant="primary" size="sm" @click="handlePay(o.id)">支付</BaseButton>
              <BaseButton v-if="o.payStatus === 'PENDING'" variant="danger" size="sm" @click="handleCancel(o.id)">取消</BaseButton>
              <BaseButton v-if="o.payStatus === 'COMPLETED'" variant="success" size="sm" @click="openReview(o)">评价</BaseButton>
              <BaseButton variant="ghost" size="sm" @click="router.push('/orders/' + o.id)">详情</BaseButton>
            </div>
          </div>
        </div>
      </div>
    </template>
    <EmptyState v-else description="暂无订单" />

    <BaseDialog v-model="reviewVisible" title="写评价">
      <div class="space-y-4">
        <div class="flex items-center gap-3">
          <span class="text-sm text-gray-400">评分</span>
          <StarRating v-model="reviewRating" :show-score="true" />
        </div>
        <div>
          <label class="block text-sm text-gray-400 mb-2">评价内容</label>
          <textarea v-model="reviewContent" class="input-dark resize-none" rows="4" maxlength="500" />
          <p class="text-xs text-gray-500 mt-1 text-right">{{ reviewContent.length }} / 500</p>
        </div>
      </div>
      <template #footer>
        <BaseButton variant="ghost" @click="reviewVisible = false">取消</BaseButton>
        <BaseButton variant="primary" :loading="submittingReview" @click="submitReview">提交</BaseButton>
      </template>
    </BaseDialog>
  </UserLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserOrders, payOrder, cancelOrder, type OrderItem } from '../api/orders'
import { createReview } from '../api/reviews'
import { useToast } from '../composables/useToast'
import { useGlobalConfirm } from '../composables/useConfirm'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import StarRating from '../components/StarRating.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'
import BaseButton from '../components/BaseButton.vue'
import BaseDialog from '../components/BaseDialog.vue'

const router = useRouter()
const toast = useToast()
const confirmDialog = useGlobalConfirm()

const orders = ref<OrderItem[]>([])
const loading = ref(true)
const reviewVisible = ref(false)
const reviewContent = ref('')
const reviewRating = ref(5)
const reviewOrderId = ref(0)
const reviewShowId = ref(0)
const submittingReview = ref(false)

function statusText(s: string) {
  const m: Record<string, string> = { PENDING: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已驳回' }
  return m[s] || s
}
function statusType(s: string) {
  const m: Record<string, 'warning'|'success'|'info'|'danger'> = { PENDING: 'warning', PAID: 'success', COMPLETED: 'info', CANCELLED: 'danger', REJECTED: 'danger' }
  return m[s] || 'info'
}
function formatDate(v: string | null) {
  if (!v) return ''
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

async function load() {
  loading.value = true
  try { orders.value = (await getUserOrders()).data.data || [] }
  catch { toast.error('订单加载失败') }
  finally { loading.value = false }
}

async function handlePay(id: number) {
  try { await payOrder(id); toast.success('支付成功'); await load() }
  catch { toast.error('支付失败') }
}

async function handleCancel(id: number) {
  const ok = await confirmDialog.open('确定要取消该订单吗？', '取消订单')
  if (!ok) return
  try { await cancelOrder(id); toast.success('订单已取消'); await load() }
  catch { toast.error('取消失败') }
}

function openReview(o: OrderItem) {
  reviewOrderId.value = o.id
  reviewShowId.value = o.showId
  reviewContent.value = ''
  reviewRating.value = 5
  reviewVisible.value = true
}

async function submitReview() {
  submittingReview.value = true
  try {
    await createReview({ orderId: reviewOrderId.value, rating: reviewRating.value, content: reviewContent.value, showId: reviewShowId.value } as any)
    toast.success('评价成功')
    reviewVisible.value = false
  } catch { toast.error('评价失败') }
  finally { submittingReview.value = false }
}

onMounted(load)
</script>
