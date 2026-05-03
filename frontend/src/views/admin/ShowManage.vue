<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>演出管理</h1>
        </div>
        <el-button type="primary" size="large" @click="openCreateDialog">新增演出</el-button>
      </section>

      <el-card class="table-card" shadow="never">
        <el-table v-loading="loading" :data="shows" row-key="id" class="show-table">
          <el-table-column label="封面" width="96">
            <template #default="{ row }">
              <el-image v-if="resolveCoverImage(row.coverImage)" class="cover-thumb" :src="resolveCoverImage(row.coverImage)" fit="cover">
                <template #error>
                  <div class="cover-thumb placeholder"></div>
                </template>
              </el-image>
              <div v-else class="cover-thumb placeholder"></div>
            </template>
          </el-table-column>
          <el-table-column label="演出名称" min-width="180" prop="showName" show-overflow-tooltip>
            <template #default="{ row }">{{ row.showName || row.title }}</template>
          </el-table-column>
          <el-table-column label="场馆" min-width="160" prop="venue" show-overflow-tooltip />
          <el-table-column label="时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.showTime) }}</template>
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

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            background
            layout="total, prev, pager, next"
            @current-change="fetchShows"
          />
        </div>
      </el-card>

      <el-dialog v-model="dialogVisible" :title="editingShow ? '编辑演出' : '新增演出'" width="600px" @closed="resetForm">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="show-form">
          <el-form-item label="演出标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入演出标题" />
          </el-form-item>
          <el-form-item label="场馆" prop="venue">
            <el-input v-model="form.venue" placeholder="请输入场馆" />
          </el-form-item>
          <el-form-item label="演出时间" prop="showTime">
            <el-date-picker
              v-model="form.showTime"
              type="datetime"
              placeholder="选择演出时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="full-input"
            />
          </el-form-item>
          <el-form-item label="票价" prop="ticketPrice">
            <el-input-number v-model="form.ticketPrice" :precision="2" :min="0" class="full-input" />
          </el-form-item>
          <el-form-item label="座位总数" prop="totalSeats">
            <el-input-number v-model="form.totalSeats" :min="1" :precision="0" class="full-input" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入演出描述" />
          </el-form-item>
          <el-form-item label="封面图">
            <el-upload
              v-model:file-list="uploadFiles"
              list-type="picture-card"
              :limit="1"
              accept=".jpg,.jpeg,.png"
              :auto-upload="false"
              :before-upload="beforeCoverUpload"
              :on-change="handleCoverChange"
              :on-exceed="handleUploadExceed"
            >
              <span class="upload-plus">+</span>
              <template #tip>
                <div class="upload-tip">支持 JPG/PNG，大小不超过 2MB</div>
              </template>
            </el-upload>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadProps, type UploadRawFile, type UploadUserFile } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { createShow, deleteShow, getShows, updateShow, type ShowItem } from '../../api/shows'

interface ShowForm {
  title: string
  venue: string
  showTime: string
  ticketPrice: number
  totalSeats: number
  description: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const shows = ref<ShowItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingShow = ref<ShowItem | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const formRef = ref<FormInstance>()
const uploadFiles = ref<UploadUserFile[]>([])

const form = reactive<ShowForm>({
  title: '',
  venue: '',
  showTime: '',
  ticketPrice: 0,
  totalSeats: 1,
  description: ''
})

const rules: FormRules<ShowForm> = {
  title: [{ required: true, message: '请输入演出标题', trigger: 'blur' }],
  venue: [{ required: true, message: '请输入场馆', trigger: 'blur' }],
  showTime: [{ required: true, message: '请选择演出时间', trigger: 'change' }],
  ticketPrice: [{ required: true, message: '请输入票价', trigger: 'change' }],
  totalSeats: [{ required: true, message: '请输入座位总数', trigger: 'change' }]
}

function resolveCoverImage(coverImage: string | null) {
  if (!coverImage) {
    return ''
  }
  if (/^(https?:)?\/\//.test(coverImage) || coverImage.startsWith('data:')) {
    return coverImage
  }
  if (coverImage.startsWith('/uploads/')) {
    return `${apiBaseUrl}${coverImage}`
  }
  return coverImage
}

function displayStatus(show: ShowItem) {
  const status = show.statusText || show.status || ''
  const upperStatus = status.toUpperCase()

  if (status.includes('售票') || upperStatus.includes('ON_SALE') || upperStatus.includes('SELLING')) {
    return '售票中'
  }
  if (status.includes('即将') || status.includes('开售') || upperStatus.includes('UPCOMING')) {
    return '即将开售'
  }
  if (status.includes('结束') || upperStatus.includes('ENDED') || upperStatus.includes('FINISHED')) {
    return '已结束'
  }
  return status || '未知状态'
}

function statusTagType(show: ShowItem) {
  const status = displayStatus(show)
  if (status === '售票中') {
    return 'success'
  }
  if (status === '即将开售') {
    return 'warning'
  }
  return 'info'
}

function formatDate(value: string) {
  if (!value) {
    return '时间待定'
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

function normalizeDateTime(value: string) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (number: number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function fetchShows() {
  loading.value = true
  try {
    const response = await getShows({ page: page.value, pageSize: pageSize.value })
    const result = response.data.data
    shows.value = result.records || []
    total.value = result.total || 0
    page.value = result.page || page.value
    pageSize.value = result.pageSize || pageSize.value
  } catch {
    shows.value = []
    total.value = 0
    ElMessage.error('演出列表加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingShow.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(show: ShowItem) {
  editingShow.value = show
  form.title = show.title || show.showName || ''
  form.venue = show.venue || ''
  form.showTime = normalizeDateTime(show.showTime)
  form.ticketPrice = Number(show.ticketPrice || 0)
  form.totalSeats = Number(show.totalSeats || 1)
  form.description = show.description || ''
  uploadFiles.value = []
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function resetForm() {
  form.title = ''
  form.venue = ''
  form.showTime = ''
  form.ticketPrice = 0
  form.totalSeats = 1
  form.description = ''
  uploadFiles.value = []
  nextTick(() => formRef.value?.clearValidate())
}

function validateCoverFile(rawFile: UploadRawFile) {
  const isImage = ['image/jpeg', 'image/png'].includes(rawFile.type)
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('封面图仅支持 JPG/PNG 格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('封面图大小不能超过 2MB')
    return false
  }
  return true
}

const beforeCoverUpload: UploadProps['beforeUpload'] = validateCoverFile

const handleCoverChange: UploadProps['onChange'] = (uploadFile) => {
  if (uploadFile.raw && !validateCoverFile(uploadFile.raw)) {
    uploadFiles.value = uploadFiles.value.filter((file) => file.uid !== uploadFile.uid)
  }
}

function handleUploadExceed() {
  ElMessage.warning('只能上传一张封面图')
}

function buildFormData() {
  const formData = new FormData()
  formData.append('title', form.title.trim())
  formData.append('venue', form.venue.trim())
  formData.append('showTime', form.showTime)
  formData.append('ticketPrice', String(form.ticketPrice))
  formData.append('totalSeats', String(form.totalSeats))
  formData.append('description', form.description || '')

  const cover = uploadFiles.value[0]?.raw
  if (cover) {
    formData.append('cover', cover)
  }
  return formData
}

async function submitForm() {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const formData = buildFormData()
    if (editingShow.value) {
      await updateShow(editingShow.value.id, formData)
      ElMessage.success('演出更新成功')
    } else {
      await createShow(formData)
      ElMessage.success('演出创建成功')
    }
    dialogVisible.value = false
    await fetchShows()
  } catch {
    ElMessage.error(editingShow.value ? '演出更新失败' : '演出创建失败')
  } finally {
    submitting.value = false
  }
}

async function confirmDelete(show: ShowItem) {
  try {
    await ElMessageBox.confirm(`确认删除“${show.showName || show.title}”？`, '删除演出', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteShow(show.id)
    ElMessage.success('演出已删除')
    if (shows.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await fetchShows()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchShows)
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

.cover-thumb {
  width: 60px;
  height: 40px;
  overflow: hidden;
  border-radius: 8px;
  vertical-align: middle;
}

.cover-thumb.placeholder {
  background:
    linear-gradient(135deg, rgba(233, 69, 96, 0.86), rgba(22, 33, 62, 0.9)),
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.18) 0 1px, transparent 1px 8px);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

.show-form {
  padding-top: 8px;
}

.full-input {
  width: 100%;
}

.upload-plus {
  color: #909399;
  font-size: 28px;
  line-height: 1;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
