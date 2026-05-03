<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>VIP预约审核</h1>
        </div>
        <div class="filter-group">
          <el-select v-model="statusFilter" placeholder="审核状态" clearable @change="handleFilterChange" style="width: 160px">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </div>
      </section>

      <el-card class="table-card" shadow="never">
        <el-table v-loading="loading" :data="bookings" row-key="id" class="show-table">
          <el-table-column label="用户名" min-width="120" prop="username" show-overflow-tooltip />
          <el-table-column label="VIP等级" width="100">
            <template #default="{ row }">VIP {{ row.userVipLevel }}</template>
          </el-table-column>
          <el-table-column label="套餐名称" min-width="160" prop="packageName" show-overflow-tooltip />
          <el-table-column label="使用日期" width="120" prop="useDate" />
          <el-table-column label="预约时间" width="160">
            <template #default="{ row }">{{ formatDate(row.bookingTime) }}</template>
          </el-table-column>
          <el-table-column label="审核状态" width="100">
            <template #default="{ row }">
              <el-tag :type="auditStatusType(row.auditStatus)" effect="light" round>
                {{ row.auditStatusText || row.auditStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回复" min-width="150" prop="adminReply" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.auditStatus === 'PENDING'">
                <el-button type="primary" link @click="confirmApprove(row)">通过</el-button>
                <el-button type="danger" link @click="confirmReject(row)">驳回</el-button>
              </template>
              <span v-else class="action-placeholder">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            background
            layout="total, sizes, prev, pager, next"
            @current-change="fetchBookings"
            @size-change="handleSizeChange"
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
import { getAdminVipBookings, auditVipBooking, type AdminVipBookingItem } from '../../api/vipBookings'

const bookings = ref<AdminVipBookingItem[]>([])
const loading = ref(false)
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

function auditStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, string> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return (map[status] || 'info') as any
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function fetchBookings() {
  loading.value = true
  try {
    const response = await getAdminVipBookings({
      page: page.value,
      pageSize: pageSize.value,
      status: statusFilter.value || undefined
    })
    const result = response.data.data
    bookings.value = result.records || []
    total.value = result.total || 0
    page.value = result.page || page.value
    pageSize.value = result.pageSize || pageSize.value
  } catch (error: any) {
    bookings.value = []
    total.value = 0
    ElMessage.error(error.response?.data?.message || '预约列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  fetchBookings()
}

function handleSizeChange() {
  page.value = 1
  fetchBookings()
}

async function confirmApprove(booking: AdminVipBookingItem) {
  try {
    await ElMessageBox.confirm(`确认通过用户 ${booking.username} 对套餐“${booking.packageName}”的预约？`, '审核通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await auditVipBooking(booking.id, { auditStatus: 'APPROVED' })
    ElMessage.success('审核已通过')
    await fetchBookings()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

async function confirmReject(booking: AdminVipBookingItem) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '审核驳回', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '驳回原因不能为空'
    })
    await auditVipBooking(booking.id, { auditStatus: 'REJECTED', adminReply: reason })
    ElMessage.success('审核已驳回')
    await fetchBookings()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(fetchBookings)
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

.filter-group {
  display: flex;
  gap: 16px;
}

.table-card {
  max-width: 1180px;
  margin: 0 auto;
  border: 0;
  border-radius: 18px;
}

.show-table {
  width: 100%;
}

.action-placeholder {
  color: #9ca3af;
  padding-left: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
