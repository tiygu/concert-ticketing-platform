<template>
  <el-config-provider :locale="zhCn">
    <main v-loading="loading" class="order-detail-page" element-loading-background="rgba(26, 26, 46, 0.72)">
      <el-result v-if="error" icon="error" title="订单不存在" sub-title="该订单可能已被删除或链接无效">
        <template #extra>
          <el-button type="primary" @click="goOrders">返回订单列表</el-button>
        </template>
      </el-result>

      <div v-else-if="order" class="detail-shell">
        <div class="detail-header">
          <h1>订单详情</h1>
          <el-tag :type="payStatusType(order.payStatus)" effect="dark" size="large" round>
            {{ order.statusText || payStatusLabel(order.payStatus) }}
          </el-tag>
        </div>

        <section class="detail-card">
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">订单号</span>
              <strong>{{ order.orderNo }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">演出名称</span>
              <strong>{{ order.showName }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">座位</span>
              <strong>{{ order.seatNumber }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">票种</span>
              <strong>{{ ticketTypeLabel(order.ticketType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">金额</span>
              <strong class="amount">¥{{ Number(order.amount).toFixed(2) }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">下单时间</span>
              <strong>{{ formatDate(order.orderTime) }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">支付时间</span>
              <strong>{{ order.payTime ? formatDate(order.payTime) : '—' }}</strong>
            </div>
            <div class="detail-field">
              <span class="field-label">过期时间</span>
              <strong>{{ order.expireTime ? formatDate(order.expireTime) : '—' }}</strong>
            </div>
          </div>
        </section>

        <div class="action-bar">
          <el-button @click="goOrders">返回订单列表</el-button>
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </div>
      </div>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getOrderDetail, type OrderItem } from '../api/orders'

const route = useRoute()
const router = useRouter()

const order = ref<OrderItem | null>(null)
const loading = ref(false)
const error = ref(false)

function payStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REJECTED: '已驳回'
  }
  return map[status] || status
}

function payStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PAID: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger',
    REJECTED: 'danger'
  }
  return (map[status] || 'info') as 'warning' | 'success' | 'info' | 'danger'
}

function ticketTypeLabel(type: string) {
  const map: Record<string, string> = {
    REGULAR: '普通票',
    VIP: 'VIP票'
  }
  return map[type] || type
}

function formatDate(value: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

async function fetchOrder() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    error.value = true
    return
  }

  loading.value = true
  error.value = false
  try {
    const res = await getOrderDetail(id)
    order.value = res.data.data
  } catch {
    order.value = null
    error.value = true
  } finally {
    loading.value = false
  }
}

function goOrders() {
  router.push('/orders')
}

function goHome() {
  router.push('/')
}

onMounted(fetchOrder)
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  padding: 40px clamp(16px, 5vw, 72px);
  color: #eee;
  background:
    radial-gradient(circle at 76% 0%, rgba(255, 215, 0, 0.18), transparent 28%),
    radial-gradient(circle at 12% 18%, rgba(233, 69, 96, 0.28), transparent 32%),
    linear-gradient(135deg, #1a1a2e 0%, #16213e 56%, #0c0d1f 100%);
}

.detail-shell {
  max-width: 800px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.detail-header h1 {
  margin: 0;
  color: #fff;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(28px, 5vw, 42px);
}

.detail-card {
  padding: clamp(22px, 4vw, 42px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 58px rgba(0, 0, 0, 0.24);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px 32px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  color: rgba(255, 215, 0, 0.82);
  font-size: 13px;
  font-weight: 800;
}

.detail-field strong {
  color: #fff;
  font-size: 16px;
  line-height: 1.45;
}

.amount {
  color: #ffd700 !important;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

@media (max-width: 560px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>