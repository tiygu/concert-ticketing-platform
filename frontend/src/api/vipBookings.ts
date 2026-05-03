import request from '../utils/request'

export interface VipBookingItem {
  id: number
  packageId: number
  packageName: string
  benefits: string
  userId: number
  bookingTime: string
  useDate: string
  auditStatus: string
  auditStatusText: string
  adminReply: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminVipBookingItem extends VipBookingItem {
  username: string
  userVipLevel: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export function createVipBooking(data: { packageId: number; useDate: string }) {
  return request.post<ApiResult<VipBookingItem>>('/api/user/vip/bookings', data)
}

export function getMyVipBookings() {
  return request.get<ApiResult<VipBookingItem[]>>('/api/user/vip/bookings')
}

export function getAdminVipBookings(params: { page: number; pageSize: number; status?: string }) {
  return request.get<ApiResult<PageResult<AdminVipBookingItem>>>('/api/admin/vip/bookings', { params })
}

export function auditVipBooking(id: number, data: { auditStatus: string; adminReply?: string }) {
  return request.put<ApiResult<AdminVipBookingItem>>(`/api/admin/vip/bookings/${id}/audit`, data)
}
