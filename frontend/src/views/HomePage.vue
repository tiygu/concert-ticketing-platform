<template>
  <el-config-provider :locale="zhCn">
    <main class="home-page">
      <section class="hero-section">
        <div class="hero-copy">
          <p class="eyebrow">LIVE CONCERT TICKETS</p>
          <h1>演唱會票務</h1>
          <p class="hero-subtitle">发现热门演出，快速查看余票与票价，锁定你的下一场现场记忆。</p>
        </div>

        <el-input
          v-model="keyword"
          class="search-input"
          clearable
          placeholder="搜索演出名称或场馆"
          size="large"
          @keyup.enter="triggerSearch"
        >
          <template #prefix>
            <span class="search-icon">⌕</span>
          </template>
        </el-input>
      </section>

      <section v-loading="noticesLoading" class="notices-section" element-loading-background="rgba(26, 26, 46, 0.72)">
        <div class="section-header">
          <h2>最新公告</h2>
        </div>
        <el-row v-if="notices.length" :gutter="24" class="notice-grid">
          <el-col v-for="notice in notices" :key="notice.id" :xs="24" :sm="12" :md="8">
            <el-card class="notice-card" shadow="never">
              <div class="notice-header">
                <h3>{{ notice.title }}</h3>
                <span class="notice-time">{{ formatDate(notice.publishTime) }}</span>
              </div>
              <p class="notice-content">{{ notice.content }}</p>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-else-if="!noticesLoading" description="暂无公告" :image-size="80" />
      </section>

      <section v-loading="loading" class="shows-shell" element-loading-background="rgba(26, 26, 46, 0.72)">
        <el-row v-if="shows.length" :gutter="24" class="show-grid">
          <el-col v-for="show in shows" :key="show.id" :xs="24" :sm="24" :md="12" :lg="8">
            <el-card class="show-card" shadow="never" @click="goToShow(show.id)">
              <div class="cover-frame">
                <el-image
                  v-if="resolveCoverImage(show.coverImage)"
                  class="cover-image"
                  :src="resolveCoverImage(show.coverImage)"
                  fit="cover"
                >
                  <template #error>
                    <div class="cover-placeholder">LIVE</div>
                  </template>
                </el-image>
                <div v-else class="cover-placeholder">LIVE</div>
              </div>

              <div class="card-content">
                <div class="card-header">
                  <h2>{{ show.showName || show.title }}</h2>
                  <el-tag :type="statusTagType(show)" effect="dark" round>
                    {{ displayStatus(show) }}
                  </el-tag>
                </div>

                <div class="show-meta">
                  <p><span>场馆</span>{{ show.venue }}</p>
                  <p><span>时间</span>{{ formatDate(show.showTime) }}</p>
                  <p><span>票价</span>{{ formatPrice(show) }}</p>
                </div>

                <div class="seat-row">
                  <span>可售座位</span>
                  <strong>{{ show.availableSeats }} / {{ show.totalSeats }}</strong>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-empty v-else-if="!loading" description="暂无匹配的演出" />

        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            background
            layout="total, prev, pager, next"
            @current-change="fetchShows"
          />
        </div>
      </section>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getShows, type ShowItem } from '../api/shows'
import { getNotices, type NoticeItem } from '../api/notices'

const router = useRouter()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const shows = ref<ShowItem[]>([])
const notices = ref<NoticeItem[]>([])
const loading = ref(false)
const noticesLoading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(9)
const total = ref(0)
let searchTimer: ReturnType<typeof window.setTimeout> | undefined

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

function formatDate(value: string | null) {
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

function formatPrice(show: ShowItem) {
  if (show.priceRange) {
    return show.priceRange.startsWith('¥') ? show.priceRange : `¥${show.priceRange}`
  }
  return `¥${Number(show.ticketPrice || 0).toFixed(2)}`
}

function scheduleSearch() {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    page.value = 1
    fetchShows()
  }, 400)
}

function triggerSearch() {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  page.value = 1
  fetchShows()
}

async function fetchShows() {
  loading.value = true
  try {
    const response = await getShows({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined
    })
    const result = response.data.data
    shows.value = result.records || []
    total.value = result.total || 0
    page.value = result.page || page.value
    pageSize.value = result.pageSize || pageSize.value
  } catch (error) {
    shows.value = []
    total.value = 0
    ElMessage.error('演出列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function fetchNotices() {
  noticesLoading.value = true
  try {
    const response = await getNotices()
    notices.value = response.data.data || []
  } catch (error) {
    notices.value = []
  } finally {
    noticesLoading.value = false
  }
}

function goToShow(id: number) {
  router.push(`/shows/${id}`)
}

watch(keyword, scheduleSearch)

onMounted(() => {
  fetchShows()
  fetchNotices()
})

onUnmounted(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 48px clamp(20px, 5vw, 72px);
  color: #eee;
  background:
    radial-gradient(circle at top left, rgba(233, 69, 96, 0.36), transparent 34%),
    radial-gradient(circle at 85% 14%, rgba(255, 215, 0, 0.22), transparent 28%),
    linear-gradient(135deg, #1a1a2e 0%, #16213e 52%, #0f1024 100%);
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 420px);
  gap: 32px;
  align-items: end;
  max-width: 1240px;
  margin: 0 auto 34px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #ffd700;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.24em;
}

.hero-copy h1 {
  margin: 0;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: clamp(42px, 8vw, 92px);
  line-height: 0.95;
  letter-spacing: -0.08em;
}

.hero-subtitle {
  max-width: 620px;
  margin: 18px 0 0;
  color: rgba(238, 238, 238, 0.76);
  font-size: 17px;
}

.search-input {
  align-self: center;
  border-radius: 999px;
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.28);
}

.search-icon {
  color: #e94560;
  font-size: 22px;
  font-weight: 800;
}

.notices-section {
  max-width: 1240px;
  margin: 0 auto 48px;
}

.section-header {
  margin-bottom: 24px;
}

.section-header h2 {
  margin: 0;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.notice-grid {
  row-gap: 20px;
}

.notice-card {
  height: 100%;
  border: 1px solid rgba(255, 215, 0, 0.15);
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(255, 215, 0, 0.08), rgba(255, 255, 255, 0.02));
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.notice-card:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
  transform: translateY(-4px);
}

.notice-card :deep(.el-card__body) {
  padding: 20px;
}

.notice-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.notice-header h3 {
  margin: 0;
  color: #ffd700;
  font-size: 18px;
  line-height: 1.4;
}

.notice-time {
  flex: 0 0 auto;
  color: rgba(238, 238, 238, 0.6);
  font-size: 13px;
}

.notice-content {
  margin: 0;
  color: rgba(238, 238, 238, 0.85);
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.shows-shell {
  min-height: 360px;
  max-width: 1240px;
  margin: 0 auto;
}

.show-grid {
  row-gap: 24px;
}

.show-card {
  height: 100%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 44px rgba(0, 0, 0, 0.22);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.show-card:hover {
  border-color: rgba(255, 215, 0, 0.46);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.34);
  transform: scale(1.02);
}

.show-card :deep(.el-card__body) {
  padding: 0;
}

.cover-frame {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #16213e;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: rgba(255, 255, 255, 0.82);
  font-size: 32px;
  font-weight: 900;
  letter-spacing: 0.18em;
  background:
    linear-gradient(135deg, rgba(233, 69, 96, 0.92), rgba(22, 33, 62, 0.92)),
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.12) 0 1px, transparent 1px 12px);
}

.card-content {
  padding: 20px;
}

.card-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.card-header h2 {
  margin: 0;
  color: #fff;
  font-size: 20px;
  line-height: 1.35;
}

.show-meta {
  display: grid;
  gap: 10px;
  margin: 18px 0;
}

.show-meta p,
.seat-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin: 0;
  color: rgba(238, 238, 238, 0.84);
  font-size: 14px;
}

.show-meta span,
.seat-row span {
  flex: 0 0 auto;
  color: rgba(255, 215, 0, 0.82);
  font-weight: 700;
}

.seat-row {
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.seat-row strong {
  color: #fff;
  font-size: 17px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 34px;
}

:deep(.el-empty__description p) {
  color: rgba(238, 238, 238, 0.72);
}

@media (max-width: 768px) {
  .home-page {
    padding: 32px 16px;
  }

  .hero-section {
    grid-template-columns: 1fr;
  }
}
</style>
