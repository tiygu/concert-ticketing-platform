<template>
  <el-config-provider :locale="zhCn">
    <main class="order-list-page">
      <div class="page-shell">
        <div class="page-header">
          <h1>我的订单</h1>
          <el-button @click="goHome">返回首页</el-button>
        </div>

        <div v-loading="loading" class="order-content" element-loading-background="rgba(26, 26, 46, 0.72)">
          <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />

          <div v-else class="order-cards">
            <div
              v-for="order in orders"
              :key="order.id"
              class="order-card"
              @click="goDetail(order.id)"
            >
              <div class="order-card-header">
                <span class="order-no">{{ order.orderNo }}</span>
                <el-tag :type="payStatusType(order.payStatus)" effect="dark" size="small" round>
                  {{ order.statusText || payStatusLabel(order.payStatus) }}
                </el-tag>
              </div>
              <div class="order-card-body">
                <div class="order-field">
                  <span class="field-label">演出</span>
                  <strong>{{ order.showName }}</strong>
                </div>
                <div class="order-field">
                  <span class="field-label">座位</span>
                  <strong>{{ order.seatNumber }}</strong>
                </div>
                <div class="order-field">
                  <span class="field-label">金额</span>
                  <strong class="amount">¥{{ Number(order.amount).toFixed(2) }}</strong>
                </div>
                <div class="order-field">
                  <span class="field-label">下单时间</span>
                  <strong>{{ formatDate(order.orderTime) }}</strong>
                </div>
              </div>
              <div class="order-card-footer">
                <el-button type="primary" text size="small" @click.stop="goDetail(order.id)">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getUserOrders, type OrderItem } from '../api/orders'

const router = useRouter()
const orders = ref<OrderItem[]>([])
const loading = ref(false)

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

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getUserOrders()
    orders.value = res.data.data || []
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/orders/${id}`)
}

function goHome() {
  router.push('/')
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-list-page {
  min-height: 100vh;
  padding: 40px clamp(16px, 5vw, 72px);
  color: #eee;
  background:
    radial-gradient(circle at 76% 0%, rgba(255, 215, 0, 0.18), transparent 28%),
    radial-gradient(circle at 12% 18%, rgba(233, 69, 96, 0.28), transparent 32%),
    linear-gradient(135deg, #1a1a2e 0%, #16213e 56%, #0c0d1f 100%);
}

.page-shell {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.page-header h1 {
  margin: 0;
  color: #fff;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(28px, 5vw, 42px);
}

.order-content {
  min-height: 200px;
}

.order-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.order-card:hover {
  border-color: rgba(255, 215, 0, 0.35);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.28);
}

.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.order-no {
  color: rgba(255, 215, 0, 0.82);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.order-card-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 24px;
}

.order-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  color: rgba(238, 238, 238, 0.55);
  font-size: 12px;
  font-weight: 600;
}

.order-field strong {
  color: #fff;
  font-size: 15px;
}

.amount {
  color: #ffd700 !important;
}

.order-card-footer {
  margin-top: 12px;
  text-align: right;
}

@media (max-width: 560px) {
  .order-card-body {
    grid-template-columns: 1fr;
  }
}
</style>