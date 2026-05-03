<template>
  <AdminLayout title="演出管理">
    <div class="mb-4 flex justify-between">
      <input v-model="keyword" class="input-dark max-w-sm" placeholder="搜索演出名称" @keyup.enter="search" />
      <BaseButton variant="primary" @click="openDialog()">新增演出</BaseButton>
    </div>

    <div class="glass-card rounded-2xl overflow-hidden">
      <BaseTable :columns="columns" :data="shows" :loading="loading">
        <template #cell-coverImage="{ value }">
          <img v-if="resolveCover(value as string)" :src="resolveCover(value as string)" class="w-12 h-8 object-cover rounded" />
          <span v-else class="text-xs text-gray-500">无</span>
        </template>
        <template #cell-status="{ value }">
          <StatusTag :type="statusTag(value as string)" :label="statusText(value as string)" />
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button class="text-cyan-400 hover:underline text-sm" @click="openDialog(row as any)">编辑</button>
            <button class="text-red-400 hover:underline text-sm" @click="handleDelete((row as any).id)">删除</button>
          </div>
        </template>
      </BaseTable>
      <div class="p-4">
        <BasePagination :current="page" :page-size="pageSize" :total="total" @change="goPage" />
      </div>
    </div>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑演出' : '新增演出'" width="600px">
      <form @submit.prevent="handleSave" class="space-y-4">
        <div>
          <label class="block text-sm text-gray-400 mb-1">演出名称</label>
          <input v-model="form.showName" class="input-dark" placeholder="请输入演出名称" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1">场馆</label>
            <input v-model="form.venue" class="input-dark" placeholder="请输入场馆" />
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">演出时间</label>
            <input v-model="form.showTime" type="datetime-local" class="input-dark" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm text-gray-400 mb-1">票价范围</label>
            <input v-model="form.priceRange" class="input-dark" placeholder="如 280-1680" />
          </div>
          <div>
            <label class="block text-sm text-gray-400 mb-1">座位总数</label>
            <input v-model="form.totalSeats" type="number" class="input-dark" placeholder="0" />
          </div>
        </div>
        <div>
          <label class="block text-sm text-gray-400 mb-1">描述</label>
          <textarea v-model="form.description" class="input-dark resize-none" rows="3" placeholder="演出简介..." />
        </div>
        <div>
          <label class="block text-sm text-gray-400 mb-1">封面图片</label>
          <input type="file" accept="image/*" @change="handleFile" class="text-sm text-gray-400" />
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
import { getShows, createShow, updateShow, deleteShow, type ShowItem } from '../../api/shows'
import { useToast } from '../../composables/useToast'
import { useGlobalConfirm } from '../../composables/useConfirm'
import AdminLayout from '../../components/AdminLayout.vue'
import BaseTable from '../../components/BaseTable.vue'
import BasePagination from '../../components/BasePagination.vue'
import BaseButton from '../../components/BaseButton.vue'
import BaseDialog from '../../components/BaseDialog.vue'
import StatusTag from '../../components/StatusTag.vue'

const toast = useToast()
const confirm = useGlobalConfirm()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const shows = ref<ShowItem[]>([])
const loading = ref(true)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const fileInput = ref<File | null>(null)
const form = reactive({ showName: '', venue: '', showTime: '', priceRange: '', totalSeats: 0, description: '' })

const columns = [
  { key: 'coverImage', label: '封面' },
  { key: 'showName', label: '演出名称' },
  { key: 'venue', label: '场馆' },
  { key: 'showTime', label: '时间' },
  { key: 'status', label: '状态' },
  { key: 'actions', label: '操作' }
]

function resolveCover(img: string | null) {
  if (!img) return ''
  if (/^(https?:)?\/\//.test(img) || img.startsWith('data:')) return img
  if (img.startsWith('/uploads/')) return `${apiBaseUrl}${img}`
  return img
}

function statusText(s: string) {
  const st = (s || '').toUpperCase()
  if (st.includes('ON_SALE')) return '售票中'
  if (st.includes('UPCOMING')) return '即将开售'
  if (st.includes('ENDED')) return '已结束'
  return s || '未知'
}
function statusTag(s: string) {
  const st = statusText(s)
  if (st === '售票中') return 'success' as const
  if (st === '即将开售') return 'warning' as const
  return 'info' as const
}

async function load() {
  loading.value = true
  try {
    const res = await getShows({ page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined as any })
    const d = res.data.data as any
    shows.value = d.records || []
    total.value = d.total || 0
  } catch { toast.error('演出列表加载失败') }
  finally { loading.value = false }
}

function openDialog(row?: ShowItem) {
  editingId.value = row ? row.id : null
  form.showName = row ? (row.showName || '') : ''
  form.venue = row ? (row.venue || '') : ''
  form.showTime = row ? (row.showTime || '').slice(0, 16) : ''
  form.priceRange = row ? (row.priceRange || '') : ''
  form.totalSeats = row ? row.totalSeats : 0
  form.description = row ? (row.description || '') : ''
  dialogVisible.value = true
}

function handleFile(e: Event) { fileInput.value = (e.target as HTMLInputElement).files?.[0] || null }

async function handleSave() {
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('showName', form.showName)
    fd.append('venue', form.venue)
    fd.append('showTime', form.showTime)
    fd.append('priceRange', form.priceRange)
    fd.append('totalSeats', String(form.totalSeats))
    fd.append('description', form.description)
    if (fileInput.value) fd.append('coverImage', fileInput.value)
    if (editingId.value) {
      await updateShow(editingId.value, fd)
      toast.success('演出更新成功')
    } else {
      await createShow(fd)
      toast.success('演出创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch { toast.error('保存失败') }
  finally { saving.value = false }
}

async function handleDelete(id: number) {
  const ok = await confirm.open('确定删除该演出吗？', '删除确认')
  if (!ok) return
  try { await deleteShow(id); toast.success('演出已删除'); await load() }
  catch { toast.error('删除失败') }
}

function search() { page.value = 1; load() }
function goPage(p: number) { page.value = p; load() }

onMounted(load)
</script>
