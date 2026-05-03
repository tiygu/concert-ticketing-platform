<template>
  <el-config-provider :locale="zhCn">
    <main class="my-bookings-page">
      <section class="page-header">
        <h1>我的预约</h1>
        <el-radio-group v-model="statusFilter" class="status-filter">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="PENDING">待审核</el-radio-button>
          <el-radio-button label="APPROVED">已通过</el-radio-button>
          <el-radio-button label="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
      </section>

      <section v-loading="loading" class="bookings-section">
        <div v-if="filteredBookings.length" class="booking-list">
          <el-card v-for="booking in filteredBookings" :key="booking.id" class="booking-card" shadow="never">
            <div class="card-header">
              <h2>{{ booking.packageName }}</h2>
              <el-tag :type="auditStatusType(booking.auditStatus)" effect="light" round>
                {{ booking.auditStatusText || booking.auditStatus }}
              </el-tag>
            </div>

            <div class="booking-meta">
              <p><span>使用日期</span>{{ booking.useDate }}</p>
              <p><span>预约时间</span>{{ formatDate(booking.bookingTime) }}</p>
              <p><span>权益内容</span>{{ booking.benefits }}</p>
            </div>

            <div v-if="booking.adminReply && (booking.auditStatus === 'REJECTED' || booking.auditStatus === 'APPROVED')" class="admin-reply" :class="booking.auditStatus.toLowerCase()">
              <strong>管理员回复：</strong>
              <p>{{ booking.adminReply }}</p>
            </div>
          </el-card>
        </div>
        <el-empty v-else-if="!loading" description="暂无预约记录" />
      </section>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getMyVipBookings, type VipBookingItem } from '../api/vipBookings'

const bookings = ref<VipBookingItem[]>([])
const loading = ref(false)
const statusFilter = ref('')

const filteredBookings = computed(() => {
  if (!statusFilter.value) return bookings.value
  return bookings.value.filter(b => b.auditStatus === statusFilter.value)
})

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
    const response = await getMyVipBookings()
    bookings.value = response.data.data || []
  } catch (error: any) {
    bookings.value = []
    ElMessage.error(error.response?.data?.message || '预约记录加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchBookings)
</script>

<style scoped>
.my-bookings-page {
  min-height: 100vh;
  padding: 48px clamp(20px, 5vw, 72px);
  background: #f5f7fb;
  color: #1f2937;
}

.page-header {
  max-width: 800px;
  margin: 0 auto 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header h1 {
  margin: 0;
  color: #111827;
  font-size: 32px;
  font-weight: 800;
}

.bookings-section {
  max-width: 800px;
  margin: 0 auto;
  min-height: 300px;
}

.booking-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.booking-card {
  border: 0;
  border-radius: 16px;
  background: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.booking-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.booking-meta p {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.booking-meta span {
  flex: 0 0 70px;
  color: #9ca3af;
}

.admin-reply {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
}

.admin-reply.rejected {
  background: #fef2f2;
  color: #991b1b;
}

.admin-reply.approved {
  background: #f0fdf4;
  color: #166534;
}

.admin-reply strong {
  display: block;
  margin-bottom: 4px;
}

.admin-reply p {
  margin: 0;
}

@media (max-width: 640px) {
  .status-filter {
    display: flex;
    flex-wrap: wrap;
  }
  .status-filter :deep(.el-radio-button) {
    flex: 1;
  }
  .status-filter :deep(.el-radio-button__inner) {
    width: 100%;
  }
}
</style>
