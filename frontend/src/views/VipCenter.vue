<template>
  <el-config-provider :locale="zhCn">
    <main class="vip-center-page">
      <section class="hero-section">
        <div class="hero-copy">
          <p class="eyebrow">VIP PRIVILEGES</p>
          <h1>VIP权益中心</h1>
          <p class="hero-subtitle">专属权益，尊享体验</p>
        </div>
      </section>

      <section v-loading="loading" class="packages-section">
        <div v-if="packages.length" class="package-grid">
          <el-card v-for="pkg in packages" :key="pkg.id" class="package-card" shadow="hover">
            <div class="card-header">
              <h2>{{ pkg.packageName }}</h2>
              <el-tag :type="isFull(pkg) ? 'info' : 'success'" effect="light" round>
                {{ isFull(pkg) ? '已满' : '可预约' }}
              </el-tag>
            </div>

            <div class="package-meta">
              <p class="benefits"><span>权益</span>{{ pkg.benefits }}</p>
              <p><span>所需等级</span>VIP {{ pkg.userLevelRequired }}</p>
              <p><span>有效期</span>{{ pkg.validPeriod }}</p>
              <p><span>使用限制</span>{{ pkg.usageLimit }}</p>
            </div>

            <div class="stock-row">
              <span>预约进度</span>
              <strong>{{ pkg.bookedCount }} / {{ pkg.stock }}</strong>
            </div>

            <div class="action-row">
              <el-date-picker
                v-model="useDateMap[pkg.id]"
                type="date"
                placeholder="选择使用日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled="isFull(pkg) || submittingId === pkg.id"
                class="date-picker"
              />
              <el-button
                type="primary"
                :disabled="isFull(pkg) || !useDateMap[pkg.id]"
                :loading="submittingId === pkg.id"
                @click="submitBooking(pkg)"
              >
                立即预约
              </el-button>
            </div>
          </el-card>
        </div>
        <el-empty v-else-if="!loading" description="暂无可预约的VIP权益" />
      </section>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getAvailableVipPackages, type VipPackageItem } from '../api/vipPackages'
import { createVipBooking } from '../api/vipBookings'

const packages = ref<VipPackageItem[]>([])
const loading = ref(false)
const submittingId = ref<number | null>(null)
const useDateMap = reactive<Record<number, string>>({})

function remainingStock(pkg: VipPackageItem): number {
  return (pkg.stock || 0) - (pkg.bookedCount || 0)
}

function isFull(pkg: VipPackageItem): boolean {
  return remainingStock(pkg) <= 0
}

async function fetchPackages() {
  loading.value = true
  try {
    const response = await getAvailableVipPackages()
    packages.value = response.data.data || []
  } catch (error: any) {
    packages.value = []
    ElMessage.error(error.response?.data?.message || '权益列表加载失败')
  } finally {
    loading.value = false
  }
}

async function submitBooking(pkg: VipPackageItem) {
  const useDate = useDateMap[pkg.id]
  if (!useDate) return

  submittingId.value = pkg.id
  try {
    await createVipBooking({
      packageId: pkg.id,
      useDate
    })
    ElMessage.success('预约成功，请等待管理员审核')
    useDateMap[pkg.id] = '' // clear date
    await fetchPackages() // refresh stock
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '预约失败')
  } finally {
    submittingId.value = null
  }
}

onMounted(fetchPackages)
</script>

<style scoped>
.vip-center-page {
  min-height: 100vh;
  padding: 48px clamp(20px, 5vw, 72px);
  background: #f5f7fb;
  color: #1f2937;
}

.hero-section {
  max-width: 1240px;
  margin: 0 auto 40px;
  text-align: center;
}

.eyebrow {
  margin: 0 0 10px;
  color: #e94560;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.24em;
}

.hero-copy h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(32px, 6vw, 48px);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  margin: 12px 0 0;
  color: #6b7280;
  font-size: 18px;
}

.packages-section {
  max-width: 1240px;
  margin: 0 auto;
  min-height: 300px;
}

.package-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.package-card {
  border: 0;
  border-radius: 18px;
  background: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.package-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  color: #111827;
  line-height: 1.4;
}

.package-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.package-meta p {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.package-meta span {
  flex: 0 0 70px;
  color: #9ca3af;
  font-weight: 500;
}

.benefits {
  line-height: 1.5;
}

.stock-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #f3f4f6;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 20px;
}

.stock-row span {
  color: #6b7280;
  font-size: 14px;
}

.stock-row strong {
  color: #111827;
  font-size: 16px;
}

.action-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.date-picker {
  width: 100% !important;
}

@media (max-width: 1024px) {
  .package-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .package-grid {
    grid-template-columns: 1fr;
  }
}
</style>
