<template>
  <UserLayout v-if="!error">
    <LoadingOverlay v-if="loading" />
    <template v-else>
      <div class="max-w-lg mx-auto">
        <div class="glass-card rounded-2xl p-6">
          <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-bold">个人中心</h1>
            <StatusTag :type="userStatus === 'ACTIVE' ? 'success' : 'danger'" :label="userStatus === 'ACTIVE' ? '正常' : '已禁用'" />
          </div>

          <div class="grid grid-cols-2 gap-4 text-sm mb-6 pb-6 border-b border-white/10">
            <div><p class="text-gray-400 text-xs">用户名</p><p class="font-bold">{{ profile?.username }}</p></div>
            <div><p class="text-gray-400 text-xs">角色</p><p class="font-bold">{{ profile?.role === 'ADMIN' ? '管理员' : '用户' }}</p></div>
            <div><p class="text-gray-400 text-xs">VIP等级</p><p class="font-bold text-yellow-400">V{{ profile?.vipLevel || 0 }}</p></div>
            <div><p class="text-gray-400 text-xs">积分</p><p class="font-bold text-neon-cyan">{{ profile?.points || 0 }}</p></div>
          </div>

          <form @submit.prevent="handleSave" class="space-y-4">
            <div>
              <label class="block text-sm text-gray-400 mb-2">手机号</label>
              <input v-model="form.phone" class="input-dark" placeholder="请输入手机号" />
              <p v-if="errors.phone" class="text-red-400 text-xs mt-1">{{ errors.phone }}</p>
            </div>
            <div>
              <label class="block text-sm text-gray-400 mb-2">邮箱</label>
              <input v-model="form.email" class="input-dark" placeholder="请输入邮箱" />
              <p v-if="errors.email" class="text-red-400 text-xs mt-1">{{ errors.email }}</p>
            </div>
            <BaseButton variant="primary" native-type="submit" :loading="submitting" class="w-full">保存修改</BaseButton>
          </form>
        </div>
      </div>
    </template>
  </UserLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getProfile, updateProfile } from '../api/users'
import { useToast } from '../composables/useToast'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'
import BaseButton from '../components/BaseButton.vue'

const toast = useToast()

const profile = ref<any>(null)
const loading = ref(true)
const error = ref(false)
const userStatus = ref('ACTIVE')
const submitting = ref(false)
const form = reactive({ phone: '', email: '' })
const errors = reactive({ phone: '', email: '' })

function validate() {
  errors.phone = form.phone.trim() ? '' : '请输入手机号'
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) ? '' : '请输入有效邮箱'
  return !errors.phone && !errors.email
}

async function handleSave() {
  if (!validate()) return
  submitting.value = true
  try {
    await updateProfile({ phone: form.phone, email: form.email })
    toast.success('个人信息更新成功')
  } catch { toast.error('个人信息更新失败') }
  finally { submitting.value = false }
}

onMounted(async () => {
  try {
    const res = await getProfile()
    profile.value = res.data.data
    userStatus.value = profile.value?.status || 'ACTIVE'
    form.phone = profile.value?.phone || ''
    form.email = profile.value?.email || ''
  } catch { error.value = true }
  finally { loading.value = false }
})
</script>
