<template>
  <AdminLayout title="数据统计">
    <!-- Revenue -->
    <div class="glass-card rounded-2xl p-6 mb-6">
      <h3 class="text-lg font-bold mb-4">营收统计</h3>
      <div class="mb-4">
        <input v-model="dateRange.start" type="date" class="input-dark w-auto inline-block mr-2" />
        <span class="text-gray-400 mr-2">至</span>
        <input v-model="dateRange.end" type="date" class="input-dark w-auto inline-block mr-4" />
        <BaseButton variant="primary" size="sm" @click="loadRevenue">查询</BaseButton>
      </div>
      <div class="grid grid-cols-3 gap-4 mb-6">
        <div class="stat-card rounded-xl p-4"><p class="text-gray-400 text-sm">总订单数</p><p class="text-3xl font-bold font-orbitron">{{ revenue.totalOrders }}</p></div>
        <div class="stat-card rounded-xl p-4"><p class="text-gray-400 text-sm">售票总数</p><p class="text-3xl font-bold font-orbitron">{{ revenue.totalTickets }}</p></div>
        <div class="stat-card rounded-xl p-4"><p class="text-gray-400 text-sm">总收入 (¥)</p><p class="text-3xl font-bold font-orbitron text-neon-cyan">{{ (revenue.totalRevenue || 0).toFixed(2) }}</p></div>
      </div>
      <div ref="revenueChartRef" style="width:100%;height:400px" />
    </div>

    <!-- Stock -->
    <div class="glass-card rounded-2xl p-6">
      <h3 class="text-lg font-bold mb-4">库存统计</h3>
      <div class="mb-4">
        <select v-model="selectedShowId" class="input-dark max-w-xs" @change="loadStock">
          <option value="">选择演出</option>
          <option v-for="s in shows" :key="s.id" :value="s.id">{{ s.showName || s.title }}</option>
        </select>
        <BaseButton variant="primary" size="sm" class="ml-3" @click="loadStock">查询</BaseButton>
      </div>
      <div ref="stockChartRef" style="width:100%;height:400px;margin-bottom:20px" />
      <table class="w-full text-sm">
        <thead class="bg-white/5 text-gray-400"><tr>
          <th class="text-left p-3">票种</th><th class="text-left p-3">票价类型</th><th class="text-left p-3">单价</th><th class="text-left p-3">总库存</th><th class="text-left p-3">已售</th><th class="text-left p-3">剩余</th><th class="text-left p-3">售出率</th>
        </tr></thead>
        <tbody class="divide-y divide-white/5">
          <tr v-for="d in stock.details" :key="d.ticketType + d.priceType">
            <td class="p-3"><span :class="d.ticketType === 'VIP' ? 'tag-purple' : 'tag-info'" class="px-2 py-1 rounded text-xs">{{ d.ticketType === 'VIP' ? 'VIP票' : '普通票' }}</span></td>
            <td class="p-3">{{ d.priceType }}</td><td class="p-3">¥{{ d.price?.toFixed?.(2) ?? d.price }}</td>
            <td class="p-3">{{ d.total }}</td><td class="p-3">{{ d.sold }}</td><td class="p-3">{{ d.stock }}</td>
            <td class="p-3">
              <div class="flex items-center gap-2">
                <div class="flex-1 h-2 bg-white/10 rounded-full overflow-hidden">
                  <div class="h-full bg-gradient-to-r from-pink-500 to-purple-600 rounded-full" :style="{ width: (d.soldPercentage || 0) + '%' }" />
                </div>
                <span class="text-xs text-gray-400 w-12">{{ (d.soldPercentage || 0).toFixed(1) }}%</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getRevenueReport, getStockStats, type RevenueReportResponse, type StockStatsResponse } from '../../api/statistics'
import { getShows, type ShowItem } from '../../api/shows'
import { useToast } from '../../composables/useToast'
import AdminLayout from '../../components/AdminLayout.vue'
import BaseButton from '../../components/BaseButton.vue'

const toast = useToast()

const revenueChartRef = ref<HTMLElement>()
const stockChartRef = ref<HTMLElement>()
let revenueChart: echarts.ECharts | null = null
let stockChart: echarts.ECharts | null = null

const shows = ref<ShowItem[]>([])
const selectedShowId = ref('')

const dateRange = reactive({ start: '2026-01-01', end: '2026-12-31' })
const revenue = ref<RevenueReportResponse>({ totalOrders: 0, totalTickets: 0, totalRevenue: 0, details: [] })
const stock = ref<StockStatsResponse | any>({ showId: 0, showName: '', details: [] })

async function loadRevenue() {
  try {
    const res = await getRevenueReport({ startDate: dateRange.start, endDate: dateRange.end })
    revenue.value = res.data.data
    renderRevenueChart()
  } catch { toast.error('营收数据加载失败') }
}

async function loadStock() {
  if (!selectedShowId.value) return
  try {
    const res = await getStockStats(Number(selectedShowId.value))
    stock.value = res.data.data
    renderStockChart()
  } catch { toast.error('库存数据加载失败') }
}

function renderRevenueChart() {
  if (!revenueChartRef.value) return
  if (!revenueChart) {
    revenueChart = echarts.init(revenueChartRef.value)
  }
  const details = revenue.value.details || []
  revenueChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: '#fff' }, data: ['订单数', '售票数', '收入'] },
    xAxis: { type: 'category', data: details.map(d => d.showName), axisLabel: { color: '#888' }, axisLine: { lineStyle: { color: '#333' } } },
    yAxis: [{ type: 'value', name: '数量', nameTextStyle: { color: '#888' }, axisLabel: { color: '#888' }, splitLine: { lineStyle: { color: '#222' } } },
      { type: 'value', name: '收入(¥)', nameTextStyle: { color: '#888' }, axisLabel: { color: '#888' }, splitLine: { show: false } }],
    series: [
      { name: '订单数', type: 'bar', data: details.map(d => d.orderCount), itemStyle: { color: '#8338ec' } },
      { name: '售票数', type: 'bar', data: details.map(d => d.ticketCount), itemStyle: { color: '#3a86ff' } },
      { name: '收入', type: 'line', yAxisIndex: 1, data: details.map(d => d.revenue), itemStyle: { color: '#00f5ff' }, lineStyle: { color: '#00f5ff' } }
    ]
  })
}

function renderStockChart() {
  if (!stockChartRef.value) return
  if (!stockChart) {
    stockChart = echarts.init(stockChartRef.value)
  }
  const details = stock.value.details || []
  stockChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: details.map((d: any) => ({ name: d.ticketType + '-' + d.priceType, value: d.sold })),
      label: { color: '#fff' }
    }]
  })
}

onMounted(async () => {
  try { shows.value = (await getShows({ page: 1, pageSize: 1000 })).data.data?.records || [] }
  catch { /* ignore */ }
  await loadRevenue()
})
</script>
