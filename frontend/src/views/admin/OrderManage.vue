<template>
  <AdminLayout title="订单管理">
    <div class="mb-4">
      <select v-model="statusFilter" class="input-dark max-w-xs" @change="load">
        <option value="">全部订单</option>
        <option value="PENDING">待支付</option>
        <option value="PAID">已支付</option>
        <option value="COMPLETED">已完成</option>
        <option value="CANCELLED">已取消</option>
        <option value="REJECTED">已驳回</option>
      </select>
    </div>

    <div class="glass-card rounded-2xl overflow-hidden">
      <BaseTable :columns="columns" :data="orders" :loading="loading">
        <template #cell-payStatus="{ value }">
          <StatusTag :type="payTag((value as string) || '')" :label="payLabel((value as string) || '')" />
        </template>
        <template #cell-ticketType="{ value }">
          <StatusTag :type="(value as string) === 'VIP' ? 'purple' : 'info'" :label="((value as string) || 'REGULAR')" />
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button v-if="(row as any).payStatus === 'PENDING'" class="text-green-400 hover:underline text-sm" @click="handleAudit((row as any).id, 'APPROVE')">通过</button>
            <button v-if="(row as any).payStatus === 'PENDING'" class="text-red-400 hover:underline text-sm" @click="handleReject((row as any).id)">驳回</button>
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
import { getAdminOrders, auditOrder } from '../../api/orders'
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

const orders = ref<any[]>([])
const loading = ref(true)
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const columns = [
  { key: 'orderNo', label: '订单号' },
  { key: 'username', label: '用户名' },
  { key: 'showName', label: '演出名称' },
  { key: 'seatNumber', label: '座位' },
  { key: 'amount', label: '金额' },
  { key: 'ticketType', label: '票种' },
  { key: 'payStatus', label: '支付状态' },
  { key: 'orderTime', label: '下单时间' },
  { key: 'actions', label: '操作' }
]

function payLabel(s: string) {
  const m: Record<string, string> = { PENDING: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已驳回' }
  return m[s] || s
}
function payTag(s: string) {
  const m: Record<string, 'warning'|'success'|'info'|'danger'> = { PENDING: 'warning', PAID: 'success', COMPLETED: 'info', CANCELLED: 'danger', REJECTED: 'danger' }
  return m[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminOrders({ page: page.value, pageSize: pageSize.value, status: statusFilter.value || undefined as any })
    const d = res.data.data as any
    orders.value = d.records || []
    total.value = d.total || 0
  } catch { toast.error('订单列表加载失败') }
  finally { loading.value = false }
}

async function handleAudit(id: number, action: string) {
  const ok = await confirm.open('确认通过该订单吗？', '审核通过')
  if (!ok) return
  try { await auditOrder(id, { action }); toast.success('订单已通过'); await load() }
  catch { toast.error('操作失败') }
}

async function handleReject(id: number) {
  const reason = await prompt.open('请输入驳回理由', '驳回订单')
  if (reason === null) return
  try { await auditOrder(id, { action: 'REJECT', reason }); toast.success('订单已驳回'); await load() }
  catch { toast.error('操作失败') }
}

function goPage(p: number) { page.value = p; load() }

onMounted(load)
</script>
