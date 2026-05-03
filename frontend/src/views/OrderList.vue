<template>
  <el-config-provider :locale="zhCn">
    <main class="order-list-page">
      <div class="page-shell">
        <div class="page-header">
          <h1>我的订单</h1>
          <el-button @click="goHome">返回首页</el-button>
        </div>

        <div class="status-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.key"
            :class="['status-tab', { active: activeStatus === tab.key }]"
            @click="switchStatus(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div v-loading="loading" class="order-content" element-loading-background="rgba(26, 26, 46, 0.72)">
          <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />

          <div v-else class="order-cards">
            <div
              v-for="order in orders"
              :key="order.id"
              class="order-card"
            >
              <div class="order-card-header" @click="goDetail(order.id)">
                <span class="order-no">{{ order.orderNo }}</span>
                <el-tag :type="payStatusType(order.payStatus)" effect="dark" size="small" round>
                  {{ order.statusText || payStatusLabel(order.payStatus) }}
                </el-tag>
              </div>
              <div class="order-card-body" @click="goDetail(order.id)">
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
                <template v-if="order.payStatus === 'PENDING'">
                  <el-button type="primary" size="small" :loading="payingId === order.id" @click.stop="handlePay(order.id)">
                    立即支付
                  </el-button>
                  <el-button type="danger" text size="small" :loading="cancellingId === order.id" @click.stop="handleCancel(order.id)">
                    取消订单
                  </el-button>
                </template>
                <el-button v-else-if="order.payStatus === 'COMPLETED'" type="warning" text size="small" @click.stop="openReviewDialog(order)">
                  去评价
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <el-dialog v-model="reviewVisible" title="评价演出" width="480px" :close-on-click-modal="false" custom-class="review-dialog">
        <div class="review-form">
          <p class="review-order-info">订单: {{ selectedOrder?.orderNo }}</p>
          <p class="review-show-info">演出: {{ selectedOrder?.showName }}</p>
          <div class="rating-row">
            <span>评分：</span>
            <el-rate v-model="reviewRating" :max="5" show-score />
          </div>
          <el-input
            v-model="reviewContent"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="分享你的观演体验（1-500字）"
          />
        </div>
        <template #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="submittingReview" :disabled="!canSubmitReview" @click="submitReview">
            提交评价
          </el-button>
        </template>
      </el-dialog>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserOrders, payOrder, cancelOrder, type OrderItem } from '../api/orders'
import { createReview } from '../api/reviews'

const router = useRouter()
const orders = ref<OrderItem[]>([])
const loading = ref(false)
const activeStatus = ref('')
const payingId = ref<number | null>(null)
const cancellingId = ref<number | null>(null)

const reviewVisible = ref(false)
const selectedOrder = ref<OrderItem | null>(null)
const reviewRating = ref(0)
const reviewContent = ref('')
const submittingReview = ref(false)

const canSubmitReview = computed(() => reviewRating.value >= 1 && reviewContent.value.trim().length > 0)

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待支付' },
  { key: 'PAID', label: '已支付' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

function payStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', COMPLETED: '已完成',
    CANCELLED: '已取消', REJECTED: '已驳回'
  }
  return map[status] || status
}

function payStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning', PAID: 'success', COMPLETED: 'info',
    CANCELLED: 'danger', REJECTED: 'danger'
  }
  return (map[status] || 'info') as 'warning' | 'success' | 'info' | 'danger'
}

function formatDate(value: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false
  })
}

function switchStatus(key: string) {
  activeStatus.value = key
  fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getUserOrders(activeStatus.value || undefined)
    orders.value = res.data.data || []
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handlePay(id: number) {
  payingId.value = id
  try {
    await payOrder(id)
    ElMessage.success('支付成功')
    await fetchOrders()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '支付失败'
    ElMessage.error(msg)
  } finally {
    payingId.value = null
  }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？取消后将释放座位。', '确认取消', {
      confirmButtonText: '确定取消',
      cancelButtonText: '我再想想',
      type: 'warning'
    })
  } catch {
    return
  }
  cancellingId.value = id
  try {
    await cancelOrder(id)
    ElMessage.success('订单已取消')
    await fetchOrders()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '取消失败'
    ElMessage.error(msg)
  } finally {
    cancellingId.value = null
  }
}

function openReviewDialog(order: OrderItem) {
  selectedOrder.value = order
  reviewRating.value = 0
  reviewContent.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!selectedOrder.value) return
  submittingReview.value = true
  try {
    await createReview({
      orderId: selectedOrder.value.id,
      showId: selectedOrder.value.showId,
      rating: reviewRating.value,
      content: reviewContent.value.trim()
    })
    ElMessage.success('评价成功')
    reviewVisible.value = false
    await fetchOrders()
  } catch (e: any) {
    const msg = e?.response?.data?.message || '评价失败'
    ElMessage.error(msg)
  } finally {
    submittingReview.value = false
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

.page-shell { max-width: 900px; margin: 0 auto; }

.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;
}

.page-header h1 {
  margin: 0; color: #fff;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(28px, 5vw, 42px);
}

.status-tabs {
  display: flex; gap: 8px; margin-bottom: 24px; flex-wrap: wrap;
}

.status-tab {
  padding: 8px 18px; border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 22px; background: rgba(255, 255, 255, 0.05);
  color: rgba(238, 238, 238, 0.72); font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}

.status-tab:hover {
  border-color: rgba(255, 215, 0, 0.4); color: #ffd700;
}

.status-tab.active {
  border-color: #ffd700; background: rgba(255, 215, 0, 0.12); color: #ffd700;
}

.order-content { min-height: 200px; }

.order-cards { display: flex; flex-direction: column; gap: 16px; }

.order-card {
  padding: 20px; border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px; background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.order-card:hover { border-color: rgba(255, 215, 0, 0.35); box-shadow: 0 12px 36px rgba(0, 0, 0, 0.28); }

.order-card-header, .order-card-body { cursor: pointer; }

.order-card-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px;
}

.order-no {
  color: rgba(255, 215, 0, 0.82); font-size: 13px; font-weight: 800; letter-spacing: 0.06em;
}

.order-card-body {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 24px;
}

.order-field { display: flex; flex-direction: column; gap: 4px; }

.field-label { color: rgba(238, 238, 238, 0.55); font-size: 12px; font-weight: 600; }

.order-field strong { color: #fff; font-size: 15px; }

.amount { color: #ffd700 !important; }

.order-card-footer {
  display: flex; align-items: center; justify-content: flex-end; gap: 10px;
  margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-order-info,
.review-show-info {
  color: rgba(238, 238, 238, 0.7);
  margin: 0;
  font-size: 14px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #eee;
}

:deep(.review-dialog) {
  background: #16213e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
}

:deep(.review-dialog .el-dialog__title) {
  color: #fff;
}

:deep(.review-dialog .el-dialog__body) {
  color: #eee;
}

@media (max-width: 560px) {
  .order-card-body { grid-template-columns: 1fr; }
  .order-card-footer { flex-wrap: wrap; }
}
</style>
