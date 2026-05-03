import request from '../utils/request'
import type { ApiResult } from '@/types/api'
import type { AxiosResponse } from 'axios'

export interface ReviewItem {
  id: number
  orderId: number
  userId: number
  showId: number
  username: string
  content: string
  rating: number  // 1-5
  createdAt: string
}

export interface ReviewCreatePayload {
  orderId: number
  showId: number
  rating: number  // 1-5
  content: string
}

export async function createReview(data: ReviewCreatePayload): Promise<AxiosResponse<ApiResult<ReviewItem>>> {
  return request.post('/api/user/reviews', data)
}

export async function getShowReviews(showId: number): Promise<AxiosResponse<ApiResult<ReviewItem[]>>> {
  return request.get(`/api/shows/${showId}/reviews`)
}
