<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>公告管理</h1>
        </div>
        <el-button type="primary" size="large" @click="openCreateDialog">新增公告</el-button>
      </section>

      <el-card class="table-card" shadow="never">
        <el-table v-loading="loading" :data="notices" row-key="id" class="show-table">
          <el-table-column label="标题" min-width="180" prop="title" show-overflow-tooltip />
          <el-table-column label="内容" min-width="240" prop="content" show-overflow-tooltip />
          <el-table-column label="发布时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.publishTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row)" effect="light" round>{{ displayStatus(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
              <el-button type="danger" link @click="confirmDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-dialog v-model="dialogVisible" :title="editingNotice ? '编辑公告' : '新增公告'" width="600px" @closed="resetForm">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="show-form">
          <el-form-item label="公告标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入公告标题" />
          </el-form-item>
          <el-form-item label="公告内容" prop="content">
            <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
          </el-form-item>
          <el-form-item label="发布时间" prop="publishTime">
            <el-date-picker
              v-model="form.publishTime"
              type="datetime"
              placeholder="选择发布时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="full-input"
            />
          </el-form-item>
          <el-form-item v-if="editingNotice" label="状态" prop="status">
            <el-select v-model="form.status" class="full-input">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="已归档" value="ARCHIVED" />
            </el-select>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
        </template>
      </el-dialog>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { createNotice, deleteNotice, getAdminNotices, updateNotice, type NoticeItem } from '../../api/notices'

interface NoticeForm {
  title: string
  content: string
  publishTime: string
  status: string
}

const notices = ref<NoticeItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingNotice = ref<NoticeItem | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<NoticeForm>({
  title: '',
  content: '',
  publishTime: '',
  status: 'DRAFT'
})

const rules: FormRules<NoticeForm> = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

function displayStatus(notice: NoticeItem) {
  const status = notice.status || ''
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'ARCHIVED') return '已归档'
  return '草稿'
}

function statusTagType(notice: NoticeItem) {
  const status = notice.status || ''
  if (status === 'PUBLISHED') return 'success'
  if (status === 'ARCHIVED') return 'warning'
  return 'info'
}

function formatDate(value: string | null) {
  if (!value) return '未设置'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

function normalizeDateTime(value: string | null) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number: number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function fetchNotices() {
  loading.value = true
  try {
    const response = await getAdminNotices()
    notices.value = response.data.data || []
  } catch {
    notices.value = []
    ElMessage.error('公告列表加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingNotice.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(notice: NoticeItem) {
  editingNotice.value = notice
  form.title = notice.title || ''
  form.content = notice.content || ''
  form.publishTime = normalizeDateTime(notice.publishTime)
  form.status = notice.status || 'DRAFT'
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.publishTime = ''
  form.status = 'DRAFT'
  nextTick(() => formRef.value?.clearValidate())
}

async function submitForm() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: { title: string; content?: string; publishTime?: string; status?: string } = {
      title: form.title.trim(),
      content: form.content.trim()
    }
    if (form.publishTime) {
      payload.publishTime = form.publishTime
    }

    if (editingNotice.value) {
      payload.status = form.status
      await updateNotice(editingNotice.value.id, payload)
      ElMessage.success('公告更新成功')
    } else {
      await createNotice(payload)
      ElMessage.success('公告创建成功')
    }
    dialogVisible.value = false
    await fetchNotices()
  } catch {
    ElMessage.error(editingNotice.value ? '公告更新失败' : '公告创建失败')
  } finally {
    submitting.value = false
  }
}

async function confirmDelete(notice: NoticeItem) {
  try {
    await ElMessageBox.confirm(`确认删除公告“${notice.title}”？`, '删除公告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteNotice(notice.id)
    ElMessage.success('公告已删除')
    await fetchNotices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchNotices)
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

.table-card {
  max-width: 1180px;
  margin: 0 auto;
  border: 0;
  border-radius: 18px;
}

.show-table {
  width: 100%;
}

.show-form {
  padding-top: 8px;
}

.full-input {
  width: 100%;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
