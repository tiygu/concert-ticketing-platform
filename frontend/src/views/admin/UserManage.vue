<template>
  <AdminLayout title="用户管理">
    <div class="mb-6">
      <input v-model="keyword" class="input-dark max-w-sm" placeholder="搜索用户名或手机号" @keyup.enter="search" />
    </div>

    <div class="glass-card rounded-2xl overflow-hidden">
      <BaseTable :columns="columns" :data="users" :loading="loading">
        <template #cell-role="{ value }">
          <StatusTag :type="value === 'ADMIN' ? 'purple' : 'info'" :label="value === 'ADMIN' ? '管理员' : '普通用户'" />
        </template>
        <template #cell-status="{ value }">
          <StatusTag :type="value === 'ACTIVE' ? 'success' : 'danger'" :label="value === 'ACTIVE' ? '正常' : '已禁用'" />
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button v-if="(row as any).status === 'ACTIVE'" class="text-red-400 hover:underline text-sm" @click="toggleStatus((row as any).id, 'DISABLED')">禁用</button>
            <button v-else class="text-green-400 hover:underline text-sm" @click="toggleStatus((row as any).id, 'ACTIVE')">启用</button>
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
import { getAdminUsers, updateUserStatus } from '../../api/users'
import { useToast } from '../../composables/useToast'
import { useGlobalConfirm } from '../../composables/useConfirm'
import AdminLayout from '../../components/AdminLayout.vue'
import BaseTable from '../../components/BaseTable.vue'
import BasePagination from '../../components/BasePagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const toast = useToast()
const confirm = useGlobalConfirm()

const users = ref<any[]>([])
const loading = ref(true)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'username', label: '用户名' },
  { key: 'role', label: '角色' },
  { key: 'phone', label: '手机号' },
  { key: 'email', label: '邮箱' },
  { key: 'vipLevel', label: 'VIP等级' },
  { key: 'status', label: '状态' },
  { key: 'actions', label: '操作' }
]

async function load() {
  loading.value = true
  try {
    const res = await getAdminUsers({ page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined as any })
    const d = res.data.data as any
    users.value = d.records || []
    total.value = d.total || 0
  } catch { toast.error('用户列表加载失败') }
  finally { loading.value = false }
}

async function toggleStatus(id: number, status: string) {
  const label = status === 'DISABLED' ? '禁用' : '启用'
  const ok = await confirm.open(`确定${label}该用户吗？`, `${label}用户`)
  if (!ok) return
  try {
    await updateUserStatus(id, status)
    toast.success(`用户已${label}`)
    await load()
  } catch { toast.error('操作失败') }
}

function search() { page.value = 1; load() }
function goPage(p: number) { page.value = p; load() }

onMounted(load)
</script>
