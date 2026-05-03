import request from '../utils/request'

export interface SeatItem {
  id: number; showId: number; seatNumber: string; ticketType: string;
  priceType: string | null; price: number; pointsPrice: number | null;
  stock: number; sold: number; available: boolean;
}

export interface OrderItem {
  id: number; orderNo: string; userId: number; showId: number; ticketId: number;
  seatNumber: string; amount: number; ticketType: string; payStatus: string;
  statusText: string; orderTime: string; payTime: string | null;
  expireTime: string | null; showName: string; createdAt: string; updatedAt: string;
}

export interface ApiResult<T> { code: number; message: string; data: T; }

export function getShowSeats(showId: number) {
  return request.get<ApiResult<SeatItem[]>>(`/api/shows/${showId}/seats`)
}

export function createOrder(data: { showId: number; ticketId: number }) {
  return request.post<ApiResult<OrderItem>>('/api/user/orders', data)
}

export function getUserOrders(status?: string) {
  return request.get<ApiResult<OrderItem[]>>('/api/user/orders', { params: { status } })
}

export function getOrderDetail(id: number) {
  return request.get<ApiResult<OrderItem>>(`/api/user/orders/${id}`)
}

export function payOrder(id: number) {
  return request.put<ApiResult<OrderItem>>(`/api/user/orders/${id}/pay`)
}

export function cancelOrder(id: number) {
  return request.put<ApiResult<null>>(`/api/user/orders/${id}/cancel`)
}

export interface AdminOrderItem extends OrderItem {
  username: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export function getAdminOrders(params: { page?: number; size?: number; status?: string }) {
  return request.get<ApiResult<PageResult<AdminOrderItem>>>('/api/admin/orders', { params })
}

export function auditOrder(id: number, data: { action: string; reason?: string }) {
  return request.put<ApiResult<OrderItem>>(`/api/admin/orders/${id}/audit`, data)
}
