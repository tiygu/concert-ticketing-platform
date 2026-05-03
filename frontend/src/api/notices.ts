import request from '../utils/request'
import type { ApiResult } from '@/types/api'

export interface NoticeItem {
  id: number;
  title: string;
  content: string;
  publishTime: string | null;
  status: string;
  statusText: string;
  publisherId: number | null;
  createdAt: string;
  updatedAt: string;
}

export function getNotices() {
  return request.get<ApiResult<NoticeItem[]>>('/api/notices')
}

export function getAdminNotices() {
  return request.get<ApiResult<NoticeItem[]>>('/api/admin/notices')
}

export function createNotice(data: { title: string; content?: string; publishTime?: string }) {
  return request.post<ApiResult<NoticeItem>>('/api/admin/notices', data)
}

export function updateNotice(id: number, data: { title: string; content?: string; status?: string; publishTime?: string }) {
  return request.put<ApiResult<NoticeItem>>(`/api/admin/notices/${id}`, data)
}

export function deleteNotice(id: number) {
  return request.delete<ApiResult<null>>(`/api/admin/notices/${id}`)
}
