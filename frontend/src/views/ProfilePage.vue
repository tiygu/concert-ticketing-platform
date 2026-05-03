<template>
  <el-config-provider :locale="zhCn">
    <main v-loading="loading" class="detail-page" element-loading-background="rgba(26, 26, 46, 0.72)">
      <el-result v-if="error" icon="error" title="加载失败" sub-title="无法获取个人信息，请稍后重试">
        <template #extra>
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </template>
      </el-result>

      <article v-else-if="profile" class="detail-shell">
        <section class="detail-content">
          <div class="title-row">
            <div>
              <p class="eyebrow">USER PROFILE</p>
              <h1>个人中心</h1>
            </div>
            <el-tag :type="profile.status === 'ACTIVE' ? 'success' : 'danger'" effect="dark" size="large" round>
              {{ profile.status === 'ACTIVE' ? '正常' : '已禁用' }}
            </el-tag>
          </div>

          <div class="profile-layout">
            <div class="profile-left">
              <h2 class="section-title">基本信息</h2>
              <div class="metadata-grid">
                <div class="metadata-item">
                  <span>用户名</span>
                  <strong>{{ profile.username }}</strong>
                </div>
                <div class="metadata-item">
                  <span>角色</span>
                  <strong>{{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</strong>
                </div>
                <div class="metadata-item">
                  <span>VIP等级</span>
                  <strong>V{{ profile.vipLevel }}</strong>
                </div>
                <div class="metadata-item">
                  <span>积分</span>
                  <strong>{{ profile.points }}</strong>
                </div>
                <div class="metadata-item full-width">
                  <span>注册时间</span>
                  <strong>{{ formatDetailDate(profile.createdAt) }}</strong>
                </div>
              </div>
            </div>

            <div class="profile-right">
              <h2 class="section-title">联系方式</h2>
              <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="edit-form">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="form.phone" placeholder="请输入手机号" size="large" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" :loading="submitting" class="save-btn" @click="submitForm">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <el-button class="back-button" size="large" @click="goHome">返回首页</el-button>
        </section>
      </article>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getProfile, updateProfile, type UserProfile } from '../api/users'

const router = useRouter()

const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const error = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  phone: '',
  email: ''
})

const rules: FormRules = {
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

function formatDetailDate(value: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const dateText = date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
  const timeText = date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
  return `${dateText.replace(/\//g, '年').replace('年', '年').replace('年', '年')} ${timeText}`.replace(/年(\d{2})年(\d{2})/, '年$1月$2日')
}

async function fetchProfile() {
  loading.value = true
  error.value = false
  try {
    const response = await getProfile()
    profile.value = response.data.data
    form.phone = profile.value.phone || ''
    form.email = profile.value.email || ''
  } catch {
    profile.value = null
    error.value = true
  } finally {
    loading.value = false
  }
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
    const data: { phone?: string; email?: string } = {}
    if (form.phone) data.phone = form.phone
    if (form.email) data.email = form.email

    await updateProfile(data)
    ElMessage.success('个人信息更新成功')
    await fetchProfile()
  } catch {
    ElMessage.error('个人信息更新失败')
  } finally {
    submitting.value = false
  }
}

function goHome() {
  router.push('/')
}

onMounted(fetchProfile)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 40px clamp(16px, 5vw, 72px);
  color: #eee;
  background:
    radial-gradient(circle at 76% 0%, rgba(255, 215, 0, 0.18), transparent 28%),
    radial-gradient(circle at 12% 18%, rgba(233, 69, 96, 0.28), transparent 32%),
    linear-gradient(135deg, #1a1a2e 0%, #16213e 56%, #0c0d1f 100%);
}

.detail-shell {
  max-width: 1120px;
  margin: 0 auto;
}

.detail-content {
  padding: clamp(22px, 4vw, 42px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 58px rgba(0, 0, 0, 0.24);
}

.title-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 40px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #ffd700;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.24em;
}

h1 {
  margin: 0;
  color: #fff;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(34px, 6vw, 64px);
  line-height: 1.08;
}

.profile-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  margin-bottom: 40px;
}

.section-title {
  margin: 0 0 24px;
  color: #ffd700;
  font-size: 22px;
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metadata-item {
  min-height: 96px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.16);
}

.metadata-item.full-width {
  grid-column: 1 / -1;
}

.metadata-item span {
  display: block;
  margin-bottom: 10px;
  color: rgba(255, 215, 0, 0.82);
  font-size: 13px;
  font-weight: 800;
}

.metadata-item strong {
  color: #fff;
  font-size: 17px;
  line-height: 1.45;
}

.edit-form {
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.16);
}

:deep(.el-form-item__label) {
  color: rgba(255, 215, 0, 0.82) !important;
  font-weight: 800;
}

:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.05);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}

:deep(.el-input__inner) {
  color: #fff;
}

.save-btn {
  width: 100%;
  margin-top: 12px;
}

.back-button {
  margin-top: 10px;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

@media (max-width: 560px) {
  .title-row {
    flex-direction: column;
  }

  .metadata-grid {
    grid-template-columns: 1fr;
  }
}
</style>
