import request from '../utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface UserManageItem {
  id: number; username: string; role: string; phone: string | null;
  email: string | null; vipLevel: number; points: number;
  status: string; statusText: string; createdAt: string; updatedAt: string;
}

export interface UserProfile {
  id: number; username: string; role: string; phone: string | null;
  email: string | null; vipLevel: number; points: number;
  status: string; createdAt: string;
}

export function getAdminUsers(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<ApiResult<PageResult<UserManageItem>>>('/api/admin/users', { params })
}

export function updateUserStatus(id: number, status: string) {
  return request.put<ApiResult<null>>(`/api/admin/users/${id}/status`, { status })
}

export function getProfile() {
  return request.get<ApiResult<UserProfile>>('/api/user/profile')
}

export function updateProfile(data: { phone?: string; email?: string }) {
  return request.put<ApiResult<UserProfile>>('/api/user/profile', data)
}
