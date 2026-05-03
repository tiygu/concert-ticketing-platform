import request from '../utils/request'

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

export interface ApiResult<T> {
  code: number
  message: string
  data: T
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
