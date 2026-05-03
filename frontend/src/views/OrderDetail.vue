<template>
  <UserLayout v-if="!error">
    <LoadingOverlay v-if="loading" />
    <template v-else-if="order">
      <div class="glass-card rounded-2xl p-6 max-w-2xl mx-auto">
        <div class="flex justify-between items-start mb-6">
          <div>
            <StatusTag :type="stType(order.payStatus)" :label="stText(order.payStatus)" />
            <h2 class="text-2xl font-bold mt-2">{{ order.showName }}</h2>
            <p class="text-gray-400 mt-1">订单号: {{ order.orderNo }}</p>
          </div>
          <p class="text-3xl font-bold text-neon-cyan font-orbitron">¥{{ order.amount }}</p>
        </div>

        <div class="grid grid-cols-2 gap-4 border-t border-white/10 pt-4 mb-6">
          <div><p class="text-xs text-gray-400">座位</p><p class="font-bold">{{ order.seatNumber || '未指定' }}</p></div>
          <div><p class="text-xs text-gray-400">票种</p><p class="font-bold">{{ order.ticketType || '普通票' }}</p></div>
          <div><p class="text-xs text-gray-400">下单时间</p><p class="font-bold">{{ formatDate(order.orderTime) }}</p></div>
          <div><p class="text-xs text-gray-400">支付时间</p><p class="font-bold">{{ formatDate(order.payTime) || '—' }}</p></div>
        </div>

        <div class="flex gap-3">
          <BaseButton v-if="order.payStatus === 'PENDING'" variant="primary" @click="handlePay">立即支付</BaseButton>
          <BaseButton v-if="order.payStatus === 'PENDING'" variant="danger" @click="handleCancel">取消订单</BaseButton>
          <BaseButton v-if="order.payStatus === 'COMPLETED'" variant="success" @click="handleComplete">确认完成</BaseButton>
          <BaseButton variant="ghost" @click="router.push('/orders')">返回列表</BaseButton>
        </div>
      </div>
    </template>

    <div v-else class="min-h-60 flex items-center justify-center">
      <div class="glass-card rounded-2xl p-8 text-center max-w-md">
        <p class="text-5xl mb-4 opacity-30">✘</p>
        <h2 class="text-xl font-bold mb-2">订单不存在</h2>
        <BaseButton variant="primary" class="mt-4" @click="router.push('/')">返回首页</BaseButton>
      </div>
    </div>
  </UserLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder, type OrderItem } from '../api/orders'
import { useToast } from '../composables/useToast'
import { useGlobalConfirm } from '../composables/useConfirm'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'
import BaseButton from '../components/BaseButton.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const confirm = useGlobalConfirm()

const order = ref<OrderItem | null>(null)
const loading = ref(true)
const error = ref(false)

function stText(s: string) {
  const m: Record<string, string> = { PENDING: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已驳回' }
  return m[s] || s
}
function stType(s: string) {
  const m: Record<string, 'warning'|'success'|'info'|'danger'> = { PENDING: 'warning', PAID: 'success', COMPLETED: 'info', CANCELLED: 'danger', REJECTED: 'danger' }
  return m[s] || 'info'
}
function formatDate(v: string | null) {
  if (!v) return ''
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false })
}

async function load() {
  const id = Number(route.params.id)
  if (!id || Number.isNaN(id)) { error.value = true; loading.value = false; return }
  try { order.value = (await getOrderDetail(id)).data.data } catch { error.value = true } finally { loading.value = false }
}

async function handlePay() {
  const id = Number(route.params.id)
  try { await payOrder(id); toast.success('支付成功'); await load() } catch { toast.error('支付失败') }
}

async function handleCancel() {
  const ok = await confirm.open('确定取消订单？', '取消订单')
  if (!ok) return
  const id = Number(route.params.id)
  try { await cancelOrder(id); toast.success('订单已取消'); await load() } catch { toast.error('取消失败') }
}

function handleComplete() { toast.info('功能开发中，敬请期待') }

onMounted(load)
</script>
