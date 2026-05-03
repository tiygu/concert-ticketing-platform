<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>订单管理</h1>
        </div>
        <el-select
          v-model="statusFilter"
          class="status-select"
          placeholder="按状态筛选"
          clearable
          @change="triggerSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </section>

      <el-card class="table-card" shadow="never">
        <el-table v-loading="loading" :data="orders" row-key="id" class="order-table">
          <el-table-column label="订单号" min-width="160" prop="orderNo" show-overflow-tooltip />
          <el-table-column label="用户名" min-width="120" prop="username" show-overflow-tooltip />
          <el-table-column label="演出名称" min-width="180" prop="showName" show-overflow-tooltip />
          <el-table-column label="座位" min-width="100" prop="seatNumber" />
          <el-table-column label="金额" min-width="100">
            <template #default="{ row }">
              <span class="amount">¥{{ Number(row.amount).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="票种" width="100">
            <template #default="{ row }">
              {{ ticketTypeLabel(row.ticketType) }}
            </template>
          </el-table-column>
          <el-table-column label="支付状态" width="120">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.payStatus)" effect="light" round>
                {{ row.statusText || payStatusLabel(row.payStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" min-width="160">
            <template #default="{ row }">{{ formatDate(row.orderTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.payStatus === 'PENDING'">
                <el-button type="primary" link @click="handleApprove(row)">通过</el-button>
                <el-button type="danger" link @click="handleReject(row)">驳回</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            background
            layout="total, prev, pager, next"
            @current-change="fetchOrders"
          />
        </div>
      </el-card>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getAdminOrders, auditOrder, type AdminOrderItem } from '../../api/orders'

const orders = ref<AdminOrderItem[]>([])
const loading = ref(false)
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

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
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

function triggerSearch() {
  page.value = 1
  fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    const response = await getAdminOrders({
      page: page.value,
      size: pageSize.value,
      status: statusFilter.value || undefined
    })
    const result = response.data.data
    orders.value = result.list || []
    total.value = result.total || 0
    page.value = result.page || page.value
    pageSize.value = result.size || pageSize.value
  } catch {
    orders.value = []
    total.value = 0
    ElMessage.error('订单列表加载失败')
  } finally {
    loading.value = false
  }
}

async function handleApprove(order: AdminOrderItem) {
  try {
    await ElMessageBox.confirm(`确认通过订单“${order.orderNo}”？`, '通过订单', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await auditOrder(order.id, { action: 'APPROVE' })
    ElMessage.success('订单已通过')
    await fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

async function handleReject(order: AdminOrderItem) {
  try {
    const { value: reason } = await ElMessageBox.prompt(`请输入驳回订单“${order.orderNo}”的理由（可选）：`, '驳回订单', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      type: 'warning',
      inputPlaceholder: '驳回理由'
    })
    await auditOrder(order.id, { action: 'REJECT', reason })
    ElMessage.success('订单已驳回')
    await fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(fetchOrders)
</script>

<style scoped>
.manage-page {
  min-height: 100vh;
  padding: 32px clamp(18px, 4vw, 56px);
  color: #1f2937;
  background: #f5f7fb;
}

.toolbar {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: space-between;
  max-width: 1180px;
  margin: 0 auto 22px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #e94560;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 32px;
}

.status-select {
  width: 200px;
}

.table-card {
  max-width: 1180px;
  margin: 0 auto;
  border: 0;
  border-radius: 18px;
}

.order-table {
  width: 100%;
}

.amount {
  color: #e94560;
  font-weight: 600;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-select {
    width: 100%;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
