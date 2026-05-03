import request from '../utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface ShowItem {
  id: number;
  title: string;
  showName: string;
  venue: string;
  showTime: string;
  ticketPrice: number;
  priceRange: string;
  totalSeats: number;
  availableSeats: number;
  description: string;
  coverImage: string | null;
  seatZones: string | null;
  status: string;
  statusText: string;
  isDeleted: number;
  createdAt: string;
  updatedAt: string;
}

export function getShows(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<ApiResult<PageResult<ShowItem>>>('/api/shows', { params })
}

export function getShow(id: number) {
  return request.get<ApiResult<ShowItem>>(`/api/shows/${id}`)
}

export function createShow(formData: FormData) {
  return request.post<ApiResult<ShowItem>>('/api/admin/shows', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateShow(id: number, formData: FormData) {
  return request.put<ApiResult<ShowItem>>(`/api/admin/shows/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteShow(id: number) {
  return request.delete<ApiResult<null>>(`/api/admin/shows/${id}`)
}
