import request from '../utils/request'

export interface RevenueDetailItem {
  showId: number
  showName: string
  orderCount: number
  ticketCount: number
  revenue: number
}

export interface RevenueReportResponse {
  totalOrders: number
  totalTickets: number
  totalRevenue: number
  details: RevenueDetailItem[]
}

export interface StockDetailItem {
  ticketId: number
  ticketType: string
  priceType: string
  price: number
  stock: number
  sold: number
  total: number
  soldPercentage: number
}

export interface StockStatsResponse {
  showId: number
  showName: string
  totalStock: number
  totalSold: number
  totalCapacity: number
  soldPercentage: number
  details: StockDetailItem[]
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export function getRevenueReport(params: { startDate?: string; endDate?: string }) {
  return request.get<ApiResult<RevenueReportResponse>>('/api/admin/statistics/revenue', { params })
}

export function getStockStats(showId: number) {
  return request.get<ApiResult<StockStatsResponse>>('/api/admin/statistics/stock', { params: { showId } })
}
