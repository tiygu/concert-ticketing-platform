import request from '../utils/request'

export interface VipPackageItem {
  id: number;
  packageName: string;
  benefits: string;
  usageLimit: string;
  validPeriod: string;
  userLevelRequired: number;
  stock: number;
  bookedCount: number;
  status: string;
  statusText: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export function getVipPackages() {
  return request.get<ApiResult<VipPackageItem[]>>('/api/admin/vip/packages')
}

export function getAvailableVipPackages() {
  return request.get<ApiResult<VipPackageItem[]>>('/api/user/vip/packages')
}

export function createVipPackage(data: { packageName: string; benefits?: string; usageLimit?: string; validPeriod?: string; userLevelRequired?: number; stock?: number }) {
  return request.post<ApiResult<VipPackageItem>>('/api/admin/vip/packages', data)
}

export function updateVipPackage(id: number, data: { packageName?: string; benefits?: string; usageLimit?: string; validPeriod?: string; userLevelRequired?: number; stock?: number; status?: string }) {
  return request.put<ApiResult<VipPackageItem>>(`/api/admin/vip/packages/${id}`, data)
}

export function deleteVipPackage(id: number) {
  return request.delete<ApiResult<null>>(`/api/admin/vip/packages/${id}`)
}
