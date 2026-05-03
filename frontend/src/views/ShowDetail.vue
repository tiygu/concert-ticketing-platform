<template>
  <el-config-provider :locale="zhCn">
    <main v-loading="loading" class="detail-page" element-loading-background="rgba(26, 26, 46, 0.72)">
      <el-result v-if="error" icon="error" title="演出不存在" sub-title="该演出可能已下架或链接无效">
        <template #extra>
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </template>
      </el-result>

      <article v-else-if="show" class="detail-shell">
        <section class="hero-card">
          <el-image v-if="coverUrl" class="hero-image" :src="coverUrl" fit="cover">
            <template #error>
              <div class="hero-placeholder">LIVE SHOW</div>
            </template>
          </el-image>
          <div v-else class="hero-placeholder">LIVE SHOW</div>
        </section>

        <section class="detail-content">
          <div class="title-row">
            <div>
              <p class="eyebrow">CONCERT DETAIL</p>
              <h1>{{ show.showName || show.title }}</h1>
            </div>
            <el-tag :type="statusTagType(show)" effect="dark" size="large" round>
              {{ displayStatus(show) }}
            </el-tag>
          </div>

          <div class="metadata-grid">
            <div class="metadata-item">
              <span>日期时间</span>
              <strong>{{ formatDetailDate(show.showTime) }}</strong>
            </div>
            <div class="metadata-item">
              <span>场馆</span>
              <strong>📍 {{ show.venue }}</strong>
            </div>
            <div class="metadata-item">
              <span>票价</span>
              <strong>{{ formatPrice(show.ticketPrice) }}</strong>
            </div>
            <div class="metadata-item">
              <span>座位</span>
              <strong>{{ show.availableSeats }} / {{ show.totalSeats }}</strong>
            </div>
          </div>

          <section class="content-section">
            <h2>演出介绍</h2>
            <p class="description">{{ show.description || '暂无演出介绍' }}</p>
          </section>

          <section class="content-section">
            <h2>座位分区</h2>
            <pre v-if="formattedSeatZones" class="seat-zones">{{ formattedSeatZones }}</pre>
            <div v-else class="empty-zone">座位信息暂未配置</div>
          </section>

          <el-button class="back-button" size="large" @click="goHome">返回首页</el-button>
        </section>
      </article>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getShow, type ShowItem } from '../api/shows'

const route = useRoute()
const router = useRouter()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const show = ref<ShowItem | null>(null)
const loading = ref(false)
const error = ref(false)

const coverUrl = computed(() => resolveCoverImage(show.value?.coverImage || null))
const formattedSeatZones = computed(() => formatSeatZones(show.value?.seatZones || null))

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

function displayStatus(item: ShowItem) {
  const status = item.statusText || item.status || ''
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

function statusTagType(item: ShowItem) {
  const status = displayStatus(item)
  if (status === '售票中') {
    return 'success'
  }
  if (status === '即将开售') {
    return 'warning'
  }
  return 'info'
}

function formatDetailDate(value: string) {
  if (!value) {
    return '时间待定'
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

function formatPrice(value: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatSeatZones(value: string | null) {
  if (!value) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

async function fetchShow() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    error.value = true
    return
  }

  loading.value = true
  error.value = false
  try {
    const response = await getShow(id)
    show.value = response.data.data
  } catch {
    show.value = null
    error.value = true
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/')
}

onMounted(fetchShow)
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

.hero-card {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 28px;
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.34);
}

.hero-image,
.hero-placeholder {
  width: 100%;
  height: min(50vw, 400px);
  min-height: 240px;
}

.hero-placeholder {
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.86);
  font-size: clamp(34px, 7vw, 82px);
  font-weight: 900;
  letter-spacing: 0.16em;
  background:
    linear-gradient(120deg, rgba(233, 69, 96, 0.92), rgba(22, 33, 62, 0.96)),
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.13) 0 1px, transparent 1px 14px);
}

.detail-content {
  margin-top: 32px;
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

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 34px 0;
}

.metadata-item {
  min-height: 96px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.16);
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

.content-section {
  margin-top: 28px;
}

.content-section h2 {
  margin: 0 0 14px;
  color: #ffd700;
  font-size: 22px;
}

.description {
  margin: 0;
  color: rgba(238, 238, 238, 0.84);
  font-size: 16px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.seat-zones,
.empty-zone {
  margin: 0;
  padding: 18px;
  overflow-x: auto;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  color: #f7f0d0;
  background: rgba(0, 0, 0, 0.28);
}

.seat-zones {
  font-family: 'Cascadia Code', 'Consolas', monospace;
  line-height: 1.6;
  white-space: pre-wrap;
}

.empty-zone {
  color: rgba(238, 238, 238, 0.68);
}

.back-button {
  margin-top: 30px;
}

@media (max-width: 900px) {
  .metadata-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
