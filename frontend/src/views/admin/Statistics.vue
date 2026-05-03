<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>数据统计</h1>
        </div>
      </section>

      <div class="statistics-container">
        <!-- Revenue Section -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>营收统计</span>
            </div>
          </template>
          <!-- Date range picker -->
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="loadRevenue"
          />
          
          <!-- Summary cards -->
          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="8">
              <el-statistic title="总订单数" :value="revenue.totalOrders" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="售票总数" :value="revenue.totalTickets" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="总收入 (¥)" :value="(revenue.totalRevenue || 0).toFixed(2)" />
            </el-col>
          </el-row>
          
          <!-- Revenue bar chart -->
          <div ref="revenueChartRef" style="width: 100%; height: 400px; margin-top: 20px"></div>
        </el-card>
        
        <!-- Stock Section -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>库存统计</span>
            </div>
          </template>
          <!-- Show selector -->
          <el-select v-model="selectedShowId" placeholder="选择演出" @change="loadStock" filterable>
            <el-option v-for="s in shows" :key="s.id" :label="s.showName || s.title" :value="s.id" />
          </el-select>
          
          <!-- Stock donut chart -->
          <div ref="stockChartRef" style="width: 100%; height: 400px; margin-top: 20px"></div>
          
          <!-- Stock detail table -->
          <el-table :data="stock.details" style="margin-top: 20px" border>
            <el-table-column prop="ticketType" label="票种">
              <template #default="{ row }">
                <el-tag :type="row.ticketType === 'VIP' ? 'warning' : 'info'">{{ row.ticketType === 'VIP' ? 'VIP票' : '普通票' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priceType" label="票价类型" />
            <el-table-column prop="price" label="单价">
              <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="total" label="总库存" />
            <el-table-column prop="sold" label="已售" />
            <el-table-column prop="stock" label="剩余" />
            <el-table-column prop="soldPercentage" label="售出率(%)">
              <template #default="{ row }">
                <el-progress :percentage="Number((row.soldPercentage || 0).toFixed(1))" :color="customColors" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getRevenueReport, getStockStats, type RevenueReportResponse, type StockStatsResponse, type RevenueDetailItem } from '../../api/statistics'
import { getShows, type ShowItem } from '../../api/shows'

// Revenue
const dateRange = ref<[string, string]>(['2026-01-01', '2026-12-31'])
const revenue = ref<RevenueReportResponse>({ totalOrders: 0, totalTickets: 0, totalRevenue: 0, details: [] })
const revenueChartRef = ref<HTMLDivElement>()
let revenueChart: echarts.ECharts | null = null

// Stock
const shows = ref<ShowItem[]>([])
const selectedShowId = ref<number | null>(null)
const stock = ref<StockStatsResponse>({ showId: 0, showName: '', totalStock: 0, totalSold: 0, totalCapacity: 0, soldPercentage: 0, details: [] })
const stockChartRef = ref<HTMLDivElement>()
let stockChart: echarts.ECharts | null = null

const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 },
]

async function loadShows() {
  try {
    const res = await getShows({ page: 1, pageSize: 100 })
    shows.value = res.data?.data?.records || []
  } catch { /* ignore */ }
}

async function loadRevenue() {
  const [startDate, endDate] = dateRange.value || ['', '']
  try {
    const res = await getRevenueReport({ startDate, endDate })
    const data = (res.data as { data?: RevenueReportResponse }).data || res.data
    revenue.value = data as RevenueReportResponse || { totalOrders: 0, totalTickets: 0, totalRevenue: 0, details: [] }
    await nextTick()
    renderRevenueChart()
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err?.response?.data?.message || '加载营收数据失败')
  }
}

async function loadStock() {
  if (!selectedShowId.value) return
  try {
    const res = await getStockStats(selectedShowId.value)
    const data = (res.data as { data?: StockStatsResponse }).data || res.data
    stock.value = data as StockStatsResponse || { showId: 0, showName: '', totalStock: 0, totalSold: 0, totalCapacity: 0, soldPercentage: 0, details: [] }
    await nextTick()
    renderStockChart()
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err?.response?.data?.message || '加载库存数据失败')
  }
}

function renderRevenueChart() {
  if (!revenueChartRef.value) return
  if (!revenueChart) {
    revenueChart = echarts.init(revenueChartRef.value)
  }
  const names = revenue.value.details.map((d: RevenueDetailItem) => d.showName || `演出#${d.showId}`)
  const revenues = revenue.value.details.map((d: RevenueDetailItem) => d.revenue)
  const counts = revenue.value.details.map((d: RevenueDetailItem) => d.orderCount)
  
  revenueChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入 (¥)', '订单数'] },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
    yAxis: [
      { type: 'value', name: '收入 (¥)' },
      { type: 'value', name: '订单数' }
    ],
    series: [
      { name: '收入 (¥)', type: 'bar', data: revenues, yAxisIndex: 0, itemStyle: { color: '#409EFF' } },
      { name: '订单数', type: 'line', data: counts, yAxisIndex: 1, itemStyle: { color: '#67C23A' } }
    ]
  })
}

function renderStockChart() {
  if (!stockChartRef.value) return
  if (!stockChart) {
    stockChart = echarts.init(stockChartRef.value)
  }
  stockChart.setOption({
    title: { text: stock.value.showName, left: 'center' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '已售', value: stock.value.totalSold, itemStyle: { color: '#67C23A' } },
        { name: '剩余', value: stock.value.totalStock, itemStyle: { color: '#E6A23C' } }
      ]
    }]
  })
}

function handleResize() {
  revenueChart?.resize()
  stockChart?.resize()
}

onMounted(() => {
  loadShows()
  loadRevenue()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  revenueChart?.dispose()
  stockChart?.dispose()
})
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

.statistics-container {
  max-width: 1180px;
  margin: 0 auto;
}

.section-card {
  margin-bottom: 24px;
  border: 0;
  border-radius: 18px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}
</style>
