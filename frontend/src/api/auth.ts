import request from '../utils/request'
import type { ApiResult } from '@/types/api'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone: string
  email: string
}

export interface UserInfo {
  id: number
  username: string
  role: string
  vipLevel: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userInfo: UserInfo
}

export function login(data: LoginRequest) {
  return request.post<ApiResult<LoginResponse>>('/api/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<ApiResult<LoginResponse>>('/api/auth/register', data)
}

export function refreshToken(refreshToken: string) {
  return request.post<ApiResult<LoginResponse>>('/api/auth/refresh', { refreshToken })
}
