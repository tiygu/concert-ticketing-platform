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

export function getUserOrders() {
  return request.get<ApiResult<OrderItem[]>>('/api/user/orders')
}

export function getOrderDetail(id: number) {
  return request.get<ApiResult<OrderItem>>(`/api/user/orders/${id}`)
}
