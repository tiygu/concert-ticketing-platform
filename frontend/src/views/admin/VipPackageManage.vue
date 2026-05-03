<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>VIP套餐管理</h1>
        </div>
        <el-button type="primary" size="large" @click="openCreateDialog">新增套餐</el-button>
      </section>

      <el-card class="table-card" shadow="never">
        <el-table v-loading="loading" :data="packages" row-key="id" class="show-table">
          <el-table-column label="套餐名称" min-width="160" prop="packageName" show-overflow-tooltip />
          <el-table-column label="权益描述" min-width="200" prop="benefits" show-overflow-tooltip />
          <el-table-column label="所需等级" width="100" prop="userLevelRequired" />
          <el-table-column label="库存/已订" width="120">
            <template #default="{ row }">{{ row.stock }} / {{ row.bookedCount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
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

      <el-dialog v-model="dialogVisible" :title="editingPackage ? '编辑套餐' : '新增套餐'" width="600px" @closed="resetForm">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="show-form">
          <el-form-item label="套餐名称" prop="packageName">
            <el-input v-model="form.packageName" placeholder="请输入套餐名称" />
          </el-form-item>
          <el-form-item label="权益描述" prop="benefits">
            <el-input v-model="form.benefits" type="textarea" :rows="4" placeholder="请输入权益描述" />
          </el-form-item>
          <el-form-item label="使用限制" prop="usageLimit">
            <el-input v-model="form.usageLimit" placeholder="请输入使用限制" />
          </el-form-item>
          <el-form-item label="有效期" prop="validPeriod">
            <el-input v-model="form.validPeriod" placeholder="请输入有效期" />
          </el-form-item>
          <el-form-item label="所需等级" prop="userLevelRequired">
            <el-input-number v-model="form.userLevelRequired" :min="1" :precision="0" class="full-input" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :precision="0" class="full-input" />
          </el-form-item>
          <el-form-item v-if="editingPackage" label="状态" prop="status">
            <el-select v-model="form.status" class="full-input">
              <el-option label="生效中" value="ACTIVE" />
              <el-option label="已下架" value="INACTIVE" />
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
import { createVipPackage, deleteVipPackage, getVipPackages, updateVipPackage, type VipPackageItem } from '../../api/vipPackages'

interface PackageForm {
  packageName: string
  benefits: string
  usageLimit: string
  validPeriod: string
  userLevelRequired: number
  stock: number
  status: string
}

const packages = ref<VipPackageItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingPackage = ref<VipPackageItem | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<PackageForm>({
  packageName: '',
  benefits: '',
  usageLimit: '',
  validPeriod: '',
  userLevelRequired: 1,
  stock: 0,
  status: 'ACTIVE'
})

const rules: FormRules<PackageForm> = {
  packageName: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  userLevelRequired: [{ required: true, message: '请输入所需等级', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }]
}

function displayStatus(pkg: VipPackageItem) {
  const status = pkg.status || ''
  if (status === 'ACTIVE') return '生效中'
  if (status === 'INACTIVE') return '已下架'
  return status
}

function statusTagType(pkg: VipPackageItem) {
  const status = pkg.status || ''
  if (status === 'ACTIVE') return 'success'
  return 'info'
}

async function fetchPackages() {
  loading.value = true
  try {
    const response = await getVipPackages()
    packages.value = response.data.data || []
  } catch {
    packages.value = []
    ElMessage.error('套餐列表加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingPackage.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(pkg: VipPackageItem) {
  editingPackage.value = pkg
  form.packageName = pkg.packageName || ''
  form.benefits = pkg.benefits || ''
  form.usageLimit = pkg.usageLimit || ''
  form.validPeriod = pkg.validPeriod || ''
  form.userLevelRequired = Number(pkg.userLevelRequired || 1)
  form.stock = Number(pkg.stock || 0)
  form.status = pkg.status || 'ACTIVE'
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function resetForm() {
  form.packageName = ''
  form.benefits = ''
  form.usageLimit = ''
  form.validPeriod = ''
  form.userLevelRequired = 1
  form.stock = 0
  form.status = 'ACTIVE'
  nextTick(() => formRef.value?.clearValidate())
}

async function submitForm() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: { packageName: string; benefits?: string; usageLimit?: string; validPeriod?: string; userLevelRequired?: number; stock?: number; status?: string } = {
      packageName: form.packageName.trim(),
      benefits: form.benefits.trim(),
      usageLimit: form.usageLimit.trim(),
      validPeriod: form.validPeriod.trim(),
      userLevelRequired: form.userLevelRequired,
      stock: form.stock
    }

    if (editingPackage.value) {
      payload.status = form.status
      await updateVipPackage(editingPackage.value.id, payload)
      ElMessage.success('套餐更新成功')
    } else {
      await createVipPackage(payload)
      ElMessage.success('套餐创建成功')
    }
    dialogVisible.value = false
    await fetchPackages()
  } catch {
    ElMessage.error(editingPackage.value ? '套餐更新失败' : '套餐创建失败')
  } finally {
    submitting.value = false
  }
}

async function confirmDelete(pkg: VipPackageItem) {
  try {
    await ElMessageBox.confirm(`确认删除套餐“${pkg.packageName}”？`, '删除套餐', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteVipPackage(pkg.id)
    ElMessage.success('套餐已删除')
    await fetchPackages()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchPackages)
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
