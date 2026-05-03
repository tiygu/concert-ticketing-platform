<template>
  <AdminLayout title="公告管理">
    <div class="mb-4 flex justify-end">
      <BaseButton variant="primary" @click="openDialog()">新增公告</BaseButton>
    </div>

    <div class="glass-card rounded-2xl overflow-hidden">
      <BaseTable :columns="columns" :data="notices" :loading="loading">
        <template #cell-title="{ value }"><span class="text-cyan-400">{{ value as string }}</span></template>
        <template #cell-status="{ value }">
          <StatusTag :type="statusTagType(value as string)" :label="statusLabel(value as string)" />
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button class="text-cyan-400 hover:underline text-sm" @click="openDialog(row as any)">编辑</button>
            <button class="text-red-400 hover:underline text-sm" @click="handleDelete((row as any).id)">删除</button>
          </div>
        </template>
      </BaseTable>
    </div>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '新增公告'" width="600px">
      <form @submit.prevent="handleSave" class="space-y-4">
        <div>
          <label class="block text-sm text-gray-400 mb-1">标题</label>
          <input v-model="form.title" class="input-dark" placeholder="请输入公告标题" />
        </div>
        <div>
          <label class="block text-sm text-gray-400 mb-1">内容</label>
          <textarea v-model="form.content" class="input-dark resize-none" rows="6" placeholder="请输入公告内容" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1">发布时间</label>
            <input v-model="form.publishTime" type="datetime-local" class="input-dark" />
          </div>
          <div v-if="editingId">
            <label class="block text-sm text-gray-400 mb-1">状态</label>
            <select v-model="form.status" class="input-dark">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">已发布</option>
              <option value="ARCHIVED">已归档</option>
            </select>
          </div>
        </div>
      </form>
      <template #footer>
        <BaseButton variant="ghost" @click="dialogVisible = false">取消</BaseButton>
        <BaseButton variant="primary" :loading="saving" @click="handleSave">{{ editingId ? '更新' : '发布' }}</BaseButton>
      </template>
    </BaseDialog>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAdminNotices, createNotice, updateNotice, deleteNotice } from '../../api/notices'
import { useToast } from '../../composables/useToast'
import { useGlobalConfirm } from '../../composables/useConfirm'
import AdminLayout from '../../components/AdminLayout.vue'
import BaseTable from '../../components/BaseTable.vue'
import BaseButton from '../../components/BaseButton.vue'
import BaseDialog from '../../components/BaseDialog.vue'
import StatusTag from '../../components/StatusTag.vue'

const toast = useToast()
const confirm = useGlobalConfirm()

const notices = ref<any[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ title: '', content: '', publishTime: '', status: 'PUBLISHED' })

const columns = [
  { key: 'title', label: '标题' },
  { key: 'content', label: '内容' },
  { key: 'publishTime', label: '发布时间' },
  { key: 'status', label: '状态' },
  { key: 'actions', label: '操作' }
]

function statusLabel(s: string) {
  const m: Record<string, string> = { PUBLISHED: '已发布', DRAFT: '草稿', ARCHIVED: '已归档' }
  return m[s] || s
}
function statusTagType(s: string) {
  const m: Record<string, 'success'|'warning'|'info'> = { PUBLISHED: 'success', DRAFT: 'warning', ARCHIVED: 'info' }
  return m[s] || 'info'
}

async function load() {
  loading.value = true
  try { notices.value = (await getAdminNotices()).data.data || [] }
  catch { toast.error('公告列表加载失败') }
  finally { loading.value = false }
}

function openDialog(row?: any) {
  editingId.value = row ? row.id : null
  form.title = row ? (row.title || '') : ''
  form.content = row ? (row.content || '') : ''
  form.publishTime = row ? (row.publishTime || '').slice(0, 16) : ''
  form.status = row ? (row.status || 'PUBLISHED') : 'PUBLISHED'
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const data: any = { title: form.title, content: form.content, publishTime: form.publishTime, status: form.status }
    if (editingId.value) {
      await updateNotice(editingId.value, data)
      toast.success('公告更新成功')
    } else {
      await createNotice(data)
      toast.success('公告发布成功')
    }
    dialogVisible.value = false
    await load()
  } catch { toast.error('保存失败') }
  finally { saving.value = false }
}

async function handleDelete(id: number) {
  const ok = await confirm.open('确定删除该公告吗？', '删除确认')
  if (!ok) return
  try { await deleteNotice(id); toast.success('公告已删除'); await load() }
  catch { toast.error('删除失败') }
}

onMounted(load)
</script>
