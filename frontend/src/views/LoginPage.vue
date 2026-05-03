<template>
  <div class="min-h-screen flex items-center justify-center bg-[linear-gradient(135deg,#0a0a0f_0%,#1a0a2e_50%,#0f0f23_100%)]">
    <!-- Particle background -->
    <canvas ref="particleCanvas" class="fixed inset-0 pointer-events-none z-0" />

    <div class="relative z-10 w-full max-w-md mx-4">
      <div class="glass-card rounded-2xl p-8 animate-[fadeIn_0.5s_ease]">
        <div class="text-center mb-8">
          <div class="w-16 h-16 rounded-full bg-gradient-to-br from-pink-500 to-purple-600 flex items-center justify-center mx-auto mb-4">
            <span class="text-white text-2xl font-bold">T</span>
          </div>
          <h1 class="font-orbitron text-2xl font-bold neon-glow">STAR TICKET</h1>
          <p class="text-gray-400 text-sm mt-2">大型演唱会票务预订与VIP服务平台</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="block text-sm text-gray-400 mb-2">用户名</label>
            <input
              v-model="form.username"
              class="input-dark"
              placeholder="请输入用户名"
              @blur="errors.username = validateField('username')"
            />
            <p v-if="errors.username" class="text-red-400 text-xs mt-1">{{ errors.username }}</p>
          </div>

          <div>
            <label class="block text-sm text-gray-400 mb-2">密码</label>
            <input
              v-model="form.password"
              type="password"
              class="input-dark"
              placeholder="请输入密码"
              @blur="errors.password = validateField('password')"
            />
            <p v-if="errors.password" class="text-red-400 text-xs mt-1">{{ errors.password }}</p>
          </div>

          <BaseButton variant="primary" size="lg" native-type="submit" :loading="loading" class="w-full">
            登 录
          </BaseButton>
        </form>

        <p class="text-center text-sm text-gray-400 mt-6">
          还没有账号？<router-link to="/register" class="text-cyan-400 hover:underline">立即注册</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'
import { useParticles } from '../composables/useParticles'
import BaseButton from '../components/BaseButton.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const particleCanvas = ref<HTMLCanvasElement>()
const { init } = useParticles(40)

const loading = ref(false)
const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [
    { required: true, message: '请输入密码' },
    { minLen: 6, message: '密码长度不能少于6位' }
  ]
}

function validateField(field: string): string {
  const val = (form as Record<string, string>)[field]
  const fieldRules = (rules as Record<string, { required?: boolean; minLen?: number; message?: string }[]>)[field]
  for (const r of fieldRules) {
    if (r.required && !val.trim()) return r.message || '此字段不能为空'
    if (r.minLen && val.length < r.minLen) return r.message || ''
  }
  return ''
}

function validate(): boolean {
  errors.username = validateField('username')
  errors.password = validateField('password')
  return !errors.username && !errors.password
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  const success = await authStore.login(form.username, form.password)
  loading.value = false
  if (success) {
    toast.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } else {
    toast.error('用户名或密码错误')
  }
}

onMounted(() => {
  if (particleCanvas.value) init(particleCanvas.value)
})
</script>
