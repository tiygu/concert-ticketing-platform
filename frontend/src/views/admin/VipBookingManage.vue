<template>
  <AdminLayout title="VIP预约管理">
    <div class="mb-4">
      <select v-model="statusFilter" class="input-dark max-w-xs" @change="load">
        <option value="">全部</option>
        <option value="PENDING">待审核</option>
        <option value="APPROVED">已通过</option>
        <option value="REJECTED">已驳回</option>
      </select>
    </div>

    <div class="glass-card rounded-2xl overflow-hidden">
      <BaseTable :columns="columns" :data="bookings" :loading="loading">
        <template #cell-auditStatus="{ value }">
          <StatusTag :type="auditTag(value as string)" :label="auditLabel(value as string)" />
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button v-if="(row as any).auditStatus === 'PENDING'" class="text-green-400 hover:underline text-sm" @click="handleAudit((row as any).id, 'APPROVE')">通过</button>
            <button v-if="(row as any).auditStatus === 'PENDING'" class="text-red-400 hover:underline text-sm" @click="handleReject((row as any).id)">驳回</button>
          </div>
        </template>
      </BaseTable>
      <div class="p-4">
        <BasePagination :current="page" :page-size="pageSize" :total="total" @change="goPage" />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminVipBookings, auditVipBooking } from '../../api/vipBookings'
import { useToast } from '../../composables/useToast'
import { useGlobalConfirm } from '../../composables/useConfirm'
import { useGlobalPrompt } from '../../composables/usePrompt'
import AdminLayout from '../../components/AdminLayout.vue'
import BaseTable from '../../components/BaseTable.vue'
import BasePagination from '../../components/BasePagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const toast = useToast()
const confirm = useGlobalConfirm()
const prompt = useGlobalPrompt()

const bookings = ref<any[]>([])
const loading = ref(true)
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const columns = [
  { key: 'username', label: '用户名' },
  { key: 'vipLevel', label: 'VIP等级' },
  { key: 'packageName', label: '套餐名称' },
  { key: 'useDate', label: '使用日期' },
  { key: 'bookingTime', label: '预约时间' },
  { key: 'auditStatus', label: '审核状态' },
  { key: 'adminReply', label: '回复' },
  { key: 'actions', label: '操作' }
]

function auditLabel(s: string) {
  const m: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return m[s] || s
}
function auditTag(s: string) {
  const m: Record<string, 'warning'|'success'|'danger'> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return m[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminVipBookings({ page: page.value, pageSize: pageSize.value, status: statusFilter.value || undefined as any })
    const d = res.data.data as any
    bookings.value = d.records || []
    total.value = d.total || 0
  } catch { toast.error('加载失败') }
  finally { loading.value = false }
}

async function handleAudit(id: number, action: string) {
  const ok = await confirm.open('确认通过该预约吗？', '审核通过')
  if (!ok) return
  try { await auditVipBooking(id, { auditStatus: action === 'APPROVE' ? 'APPROVED' : 'REJECTED' }); toast.success('审核已通过'); await load() }
  catch { toast.error('操作失败') }
}

async function handleReject(id: number) {
  const reason = await prompt.open('请输入驳回理由', '驳回预约')
  if (reason === null) return
  try { await auditVipBooking(id, { auditStatus: 'REJECTED', adminReply: reason || '' }); toast.success('审核已驳回'); await load() }
  catch { toast.error('操作失败') }
}

function goPage(p: number) { page.value = p; load() }

onMounted(load)
</script>
