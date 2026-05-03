<template>
  <AdminLayout title="VIP套餐管理">
    <div class="mb-4 flex justify-end">
      <BaseButton variant="primary" @click="openDialog()">新增套餐</BaseButton>
    </div>

    <div class="glass-card rounded-2xl overflow-hidden">
      <BaseTable :columns="columns" :data="packages" :loading="loading">
        <template #cell-userLevelRequired="{ value }"><span class="text-yellow-400 font-bold">V{{ value }}</span></template>
        <template #cell-status="{ value }">
          <StatusTag :type="value === 'ACTIVE' ? 'success' : 'danger'" :label="value === 'ACTIVE' ? '上架' : '下架'" />
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button class="text-cyan-400 hover:underline text-sm" @click="openDialog(row as any)">编辑</button>
            <button class="text-red-400 hover:underline text-sm" @click="handleDelete((row as any).id)">删除</button>
          </div>
        </template>
      </BaseTable>
    </div>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑套餐' : '新增套餐'" width="600px">
      <form @submit.prevent="handleSave" class="space-y-4">
        <div>
          <label class="block text-sm text-gray-400 mb-1">套餐名称</label>
          <input v-model="form.packageName" class="input-dark" placeholder="请输入套餐名称" />
        </div>
        <div>
          <label class="block text-sm text-gray-400 mb-1">权益描述</label>
          <textarea v-model="form.benefits" class="input-dark resize-none" rows="3" placeholder="请输入权益内容" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1">使用限制</label>
            <input v-model="form.usageLimit" class="input-dark" placeholder="如: 每用户限一次" />
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">有效期限</label>
            <input v-model="form.validPeriod" class="input-dark" placeholder="如: 购买后30天内" />
          </div>
        </div>
        <div class="grid grid-cols-3 gap-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1">所需等级</label>
            <input v-model.number="form.userLevelRequired" type="number" min="1" class="input-dark" />
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">库存</label>
            <input v-model.number="form.stock" type="number" min="0" class="input-dark" placeholder="留空不限" />
          </div>
          <div v-if="editingId">
            <label class="block text-sm text-gray-400 mb-1">状态</label>
            <select v-model="form.status" class="input-dark">
              <option value="ACTIVE">上架</option>
              <option value="INACTIVE">下架</option>
            </select>
          </div>
        </div>
      </form>
      <template #footer>
        <BaseButton variant="ghost" @click="dialogVisible = false">取消</BaseButton>
        <BaseButton variant="primary" :loading="saving" @click="handleSave">{{ editingId ? '更新' : '创建' }}</BaseButton>
      </template>
    </BaseDialog>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getVipPackages, createVipPackage, updateVipPackage, deleteVipPackage } from '../../api/vipPackages'
import { useToast } from '../../composables/useToast'
import { useGlobalConfirm } from '../../composables/useConfirm'
import AdminLayout from '../../components/AdminLayout.vue'
import BaseTable from '../../components/BaseTable.vue'
import BaseButton from '../../components/BaseButton.vue'
import BaseDialog from '../../components/BaseDialog.vue'
import StatusTag from '../../components/StatusTag.vue'

const toast = useToast()
const confirm = useGlobalConfirm()

const packages = ref<any[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ packageName: '', benefits: '', usageLimit: '', validPeriod: '', userLevelRequired: 1, stock: 0, status: 'ACTIVE' })

const columns = [
  { key: 'packageName', label: '套餐名称' },
  { key: 'benefits', label: '权益描述' },
  { key: 'userLevelRequired', label: '所需等级' },
  { key: 'status', label: '状态' },
  { key: 'actions', label: '操作' }
]

async function load() {
  loading.value = true
  try { packages.value = (await getVipPackages()).data.data || [] }
  catch { toast.error('套餐列表加载失败') }
  finally { loading.value = false }
}

function openDialog(row?: any) {
  editingId.value = row ? row.id : null
  form.packageName = row ? (row.packageName || '') : ''
  form.benefits = row ? (row.benefits || '') : ''
  form.usageLimit = row ? (row.usageLimit || '') : ''
  form.validPeriod = row ? (row.validPeriod || '') : ''
  form.userLevelRequired = row ? (row.userLevelRequired || 1) : 1
  form.stock = row ? (row.stock ?? 0) : 0
  form.status = row ? (row.status || 'ACTIVE') : 'ACTIVE'
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const data: any = { packageName: form.packageName, benefits: form.benefits, usageLimit: form.usageLimit, validPeriod: form.validPeriod, userLevelRequired: form.userLevelRequired, stock: form.stock, status: form.status }
    if (editingId.value) { await updateVipPackage(editingId.value, data); toast.success('套餐更新成功') }
    else { await createVipPackage(data); toast.success('套餐创建成功') }
    dialogVisible.value = false
    await load()
  } catch { toast.error('保存失败') }
  finally { saving.value = false }
}

async function handleDelete(id: number) {
  const ok = await confirm.open('确定删除该套餐吗？', '删除确认')
  if (!ok) return
  try { await deleteVipPackage(id); toast.success('套餐已删除'); await load() }
  catch { toast.error('删除失败') }
}

onMounted(load)
</script>
