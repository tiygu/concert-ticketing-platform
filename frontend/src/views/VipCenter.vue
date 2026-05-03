<template>
  <UserLayout>
    <div class="max-w-4xl mx-auto">
      <div class="mb-8">
        <p class="text-neon-pink text-sm font-bold tracking-widest mb-2">VIP CENTER</p>
        <h1 class="font-orbitron text-4xl font-black">VIP 权益中心</h1>
        <p class="text-gray-400 mt-2">浏览可用的VIP专属权益套餐，选择您感兴趣的权益进行预约</p>
      </div>

      <LoadingOverlay v-if="loading" />
      <div v-else-if="packages.length" class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div v-for="pkg in packages" :key="pkg.id" class="glass-card rounded-2xl p-6">
          <div class="flex justify-between items-start mb-4">
            <div>
              <h3 class="text-xl font-bold">{{ pkg.packageName }}</h3>
              <p class="text-xs text-gray-400 mt-1">所需等级: V{{ pkg.userLevelRequired }}</p>
            </div>
            <StatusTag :type="pkg.stock === null || (pkg.stock - pkg.bookedCount) > 0 ? 'success' : 'danger'" :label="pkg.stock === null || (pkg.stock - pkg.bookedCount) > 0 ? '可预约' : '已满' " />
          </div>
          <p class="text-sm text-gray-400 mb-4 line-clamp-3">{{ pkg.benefits || '权益详情请查看套餐说明' }}</p>
          <div class="text-sm text-gray-500 mb-4 space-y-1">
            <p v-if="pkg.usageLimit">使用限制: {{ pkg.usageLimit }}</p>
            <p v-if="pkg.validPeriod">有效期: {{ pkg.validPeriod }}</p>
            <p v-if="pkg.stock !== null">名额: {{ pkg.bookedCount || 0 }} / {{ pkg.stock }}</p>
          </div>

          <div v-if="bookingPkgId === pkg.id" class="space-y-3">
            <div>
              <label class="block text-xs text-gray-400 mb-1">使用日期</label>
              <input v-model="bookingDate" type="date" class="input-dark" />
            </div>
            <div class="flex gap-2">
              <BaseButton variant="primary" size="sm" :loading="submittingBooking" @click="submitBooking(pkg.id)">确认预约</BaseButton>
              <BaseButton variant="ghost" size="sm" @click="bookingPkgId = 0">取消</BaseButton>
            </div>
          </div>
          <BaseButton
            v-else
            variant="primary"
            :disabled="pkg.stock !== null && (pkg.stock - (pkg.bookedCount || 0)) <= 0"
            @click="startBooking(pkg.id)"
          >
            立即预约
          </BaseButton>
        </div>
      </div>
      <EmptyState v-else description="暂无可用的VIP套餐" />
    </div>
  </UserLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAvailableVipPackages, type VipPackageItem } from '../api/vipPackages'
import { createVipBooking } from '../api/vipBookings'
import { useToast } from '../composables/useToast'
import UserLayout from '../components/UserLayout.vue'
import StatusTag from '../components/StatusTag.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingOverlay from '../components/LoadingOverlay.vue'
import BaseButton from '../components/BaseButton.vue'

const toast = useToast()
const packages = ref<VipPackageItem[]>([])
const loading = ref(true)
const bookingPkgId = ref(0)
const bookingDate = ref(new Date().toISOString().slice(0, 10))
const submittingBooking = ref(false)

function startBooking(id: number) {
  bookingPkgId.value = id
  bookingDate.value = new Date().toISOString().slice(0, 10)
}

async function submitBooking(packageId: number) {
  submittingBooking.value = true
  try {
    await createVipBooking({ packageId, useDate: bookingDate.value })
    toast.success('预约成功，请等待管理员审核')
    bookingPkgId.value = 0
  } catch { toast.error('预约失败') }
  finally { submittingBooking.value = false }
}

onMounted(async () => {
  loading.value = true
  try { packages.value = (await getAvailableVipPackages()).data.data || [] }
  catch { toast.error('加载失败') }
  finally { loading.value = false }
})
</script>
