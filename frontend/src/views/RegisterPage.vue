<template>
  <div class="min-h-screen flex items-center justify-center bg-[linear-gradient(135deg,#0a0a0f_0%,#1a0a2e_50%,#0f0f23_100%)]">
    <div class="relative z-10 w-full max-w-md mx-4">
      <div class="glass-card rounded-2xl p-8 animate-[fadeIn_0.5s_ease]">
        <div class="text-center mb-8">
          <div class="w-16 h-16 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center mx-auto mb-4">
            <span class="text-white text-2xl font-bold">+</span>
          </div>
          <h1 class="font-orbitron text-2xl font-bold neon-glow">STAR TICKET</h1>
          <p class="text-gray-400 text-sm mt-2">用户注册</p>
        </div>

        <form @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label class="block text-sm text-gray-400 mb-2">用户名</label>
            <input v-model="form.username" class="input-dark" placeholder="请输入用户名" />
            <p v-if="errors.username" class="text-red-400 text-xs mt-1">{{ errors.username }}</p>
          </div>

          <div>
            <label class="block text-sm text-gray-400 mb-2">密码</label>
            <input v-model="form.password" type="password" class="input-dark" placeholder="请输入密码（至少6位）" />
            <p v-if="errors.password" class="text-red-400 text-xs mt-1">{{ errors.password }}</p>
          </div>

          <div>
            <label class="block text-sm text-gray-400 mb-2">确认密码</label>
            <input v-model="form.confirmPassword" type="password" class="input-dark" placeholder="请确认密码" />
            <p v-if="errors.confirmPassword" class="text-red-400 text-xs mt-1">{{ errors.confirmPassword }}</p>
          </div>

          <div>
            <label class="block text-sm text-gray-400 mb-2">手机号</label>
            <input v-model="form.phone" class="input-dark" placeholder="请输入手机号" />
            <p v-if="errors.phone" class="text-red-400 text-xs mt-1">{{ errors.phone }}</p>
          </div>

          <div>
            <label class="block text-sm text-gray-400 mb-2">邮箱</label>
            <input v-model="form.email" type="email" class="input-dark" placeholder="请输入邮箱" />
            <p v-if="errors.email" class="text-red-400 text-xs mt-1">{{ errors.email }}</p>
          </div>

          <BaseButton variant="primary" size="lg" native-type="submit" :loading="loading" class="w-full">
            注 册
          </BaseButton>
        </form>

        <p class="text-center text-sm text-gray-400 mt-6">
          已有账号？<router-link to="/login" class="text-cyan-400 hover:underline">返回登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'
import BaseButton from '../components/BaseButton.vue'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const loading = ref(false)
const form = reactive({
  username: '', password: '', confirmPassword: '', phone: '', email: ''
})
const errors = reactive({
  username: '', password: '', confirmPassword: '', phone: '', email: ''
})

function validate(): boolean {
  errors.username = form.username.trim() ? '' : '请输入用户名'
  errors.password = form.password.length >= 6 ? '' : '密码长度不能少于6位'
  errors.confirmPassword = form.confirmPassword === form.password ? '' : '两次输入的密码不一致'
  errors.phone = form.phone.trim() ? '' : '请输入手机号'
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) ? '' : '请输入有效邮箱'
  return !errors.username && !errors.password && !errors.confirmPassword && !errors.phone && !errors.email
}

async function handleRegister() {
  if (!validate()) return
  loading.value = true
  const success = await authStore.register({
    username: form.username,
    password: form.password,
    phone: form.phone,
    email: form.email
  })
  loading.value = false
  if (success) {
    toast.success('注册成功')
    router.push('/')
  } else {
    toast.error('注册失败，用户名可能已存在')
  }
}
</script>
