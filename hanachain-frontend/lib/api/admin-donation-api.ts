import { SpringPageResponse } from '@/types/donation'
import { apiClient, ApiError, ApiResponse } from './client'
import { FdsDetailResult, FdsOverrideRequest, AdminDonationTrendResponse, CampaignCloseResponse } from '@/types/admin'

// 백엔드 DonationResponseDto와 일치하는 타입 정의
export interface AdminDonation {
  id: number
  amount: number
  message?: string
  paymentId: string
  paymentStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REFUNDED'
  paymentMethod: 'CREDIT_CARD' | 'BANK_TRANSFER' | 'VIRTUAL_ACCOUNT' | 'MOBILE_PAYMENT' | 'PAYPAL' | 'OTHER'
  anonymous: boolean
  donorName?: string
  campaignId: number
  campaignTitle: string
  creatorName?: string
  paidAt?: string
  cancelledAt?: string
  failureReason?: string
  createdAt: string

  // FDS (사기 탐지 시스템) 관련 필드
  fdsAction?: 'APPROVE' | 'MANUAL_REVIEW' | 'BLOCK'
  fdsRiskScore?: number
  fdsConfidence?: number
  fdsCheckedAt?: string
  fdsExplanation?: string
  fdsStatus?: 'PENDING' | 'SUCCESS' | 'FAILED' | 'TIMEOUT'
}

export interface AdminDonationStats {
  totalAmount: number
  totalCount: number
  completedCount: number
  pendingCount: number
  failedCount: number
  cancelledCount?: number
  averageAmount: number
  todayAmount: number
  todayCount: number
  monthAmount: number
  monthCount: number
}

export interface AdminDonationFilters {
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
  keyword?: string
}

class AdminDonationApi {
  private readonly basePath = '/donations'

  // 전체 기부 내역 조회 (관리자용 API 사용)
  async getAllDonations(filters: AdminDonationFilters = {}): Promise<SpringPageResponse<AdminDonation>> {
    try {
      const queryParams = new URLSearchParams()
      
      // 페이징 파라미터
      queryParams.append('page', String(filters.page || 0))
      queryParams.append('size', String(filters.size || 20))
      queryParams.append('sort', filters.sort || 'createdAt')
      queryParams.append('direction', filters.direction || 'desc')
      
      // 검색 키워드 추가
      if (filters.keyword) {
        queryParams.append('keyword', filters.keyword)
      }

      // 관리자 전용 API 사용 - ApiResponse 래퍼로 응답이 온다
      const response = await apiClient.get<ApiResponse<SpringPageResponse<AdminDonation>>>(
        `/donations/admin/all?${queryParams}`
      )
      return response.data || { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true, numberOfElements: 0, empty: true, pageable: { pageNumber: 0, pageSize: 20, sort: { sorted: false, unsorted: true, empty: true }, offset: 0, paged: true, unpaged: false }, sort: { sorted: false, unsorted: true, empty: true } }
    } catch (error) {
      console.error('Failed to fetch admin donations:', error)
      throw error
    }
  }

  // 기부 통계 조회 (프론트엔드에서 계산)
  async getDonationStats(): Promise<AdminDonationStats> {
    try {
      // 전체 기부 데이터를 가져와서 통계 계산
      const response = await this.getAllDonations({ page: 0, size: 1000 })
      
      // response 구조 검증
      if (!response || !response.content || !Array.isArray(response.content)) {
        console.warn('Invalid response structure:', response)
        return {
          totalAmount: 0,
          totalCount: 0,
          completedCount: 0,
          pendingCount: 0,
          failedCount: 0,
          cancelledCount: 0,
          averageAmount: 0,
          todayAmount: 0,
          todayCount: 0,
          monthAmount: 0,
          monthCount: 0
        }
      }
      
      const donations = response.content
      
      const today = new Date()
      const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1)
      const startOfDay = new Date(today.getFullYear(), today.getMonth(), today.getDate())
      
      const stats = {
        totalAmount: 0,
        totalCount: donations.length,
        completedCount: 0,
        pendingCount: 0,
        failedCount: 0,
        cancelledCount: 0,
        averageAmount: 0,
        todayAmount: 0,
        todayCount: 0,
        monthAmount: 0,
        monthCount: 0
      }
      
      donations.forEach(donation => {
        const donationDate = new Date(donation.createdAt)
        const amount = Number(donation.amount)
        
        // 상태별 카운트
        if (donation.paymentStatus === 'COMPLETED') {
          stats.completedCount++
          stats.totalAmount += amount
        } else if (donation.paymentStatus === 'PENDING' || donation.paymentStatus === 'PROCESSING') {
          stats.pendingCount++
        } else if (donation.paymentStatus === 'FAILED') {
          stats.failedCount++
        } else if (donation.paymentStatus === 'CANCELLED') {
          stats.cancelledCount++
        }
        
        // 오늘 기부
        if (donationDate >= startOfDay && donation.paymentStatus === 'COMPLETED') {
          stats.todayAmount += amount
          stats.todayCount++
        }
        
        // 이번달 기부
        if (donationDate >= startOfMonth && donation.paymentStatus === 'COMPLETED') {
          stats.monthAmount += amount
          stats.monthCount++
        }
      })
      
      // 평균 계산
      stats.averageAmount = stats.completedCount > 0 ? stats.totalAmount / stats.completedCount : 0
      
      return stats
    } catch (error) {
      console.error('Failed to fetch donation stats:', error)
      // 에러 시 기본값 반환
      return {
        totalAmount: 0,
        totalCount: 0,
        completedCount: 0,
        pendingCount: 0,
        failedCount: 0,
        cancelledCount: 0,
        averageAmount: 0,
        todayAmount: 0,
        todayCount: 0,
        monthAmount: 0,
        monthCount: 0
      }
    }
  }

  // 특정 기부 상세 조회
  async getDonationById(donationId: number): Promise<AdminDonation> {
    try {
      const response = await apiClient.get<ApiResponse<AdminDonation>>(`${this.basePath}/${donationId}`)
      if (!response.data) {
        throw new Error('No donation data received')
      }
      return response.data
    } catch (error) {
      console.error('Failed to fetch donation detail:', error)
      throw error
    }
  }

  // 기부 환불 처리 (백엔드 기존 API 사용)
  async refundDonation(donationId: number, reason: string): Promise<AdminDonation> {
    try {
      const response = await apiClient.post<ApiResponse<AdminDonation>>(
        `${this.basePath}/${donationId}/refund`,
        { reason }
      )
      if (!response.data) {
        throw new Error('No refund data received')
      }
      return response.data
    } catch (error) {
      console.error('Failed to refund donation:', error)
      throw error
    }
  }

  // 사용자별 기부 내역 (백엔드 기존 API 사용)
  async getUserDonations(userId: number, filters: AdminDonationFilters = {}): Promise<SpringPageResponse<AdminDonation>> {
    try {
      const queryParams = new URLSearchParams()
      queryParams.append('page', String(filters.page || 0))
      queryParams.append('size', String(filters.size || 20))
      queryParams.append('sort', filters.sort || 'createdAt')
      queryParams.append('direction', filters.direction || 'desc')

      const response = await apiClient.get<ApiResponse<SpringPageResponse<AdminDonation>>>(
        `${this.basePath}/users/${userId}?${queryParams}`
      )
      return response.data || { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true, numberOfElements: 0, empty: true, pageable: { pageNumber: 0, pageSize: 20, sort: { sorted: false, unsorted: true, empty: true }, offset: 0, paged: true, unpaged: false }, sort: { sorted: false, unsorted: true, empty: true } }
    } catch (error) {
      console.error('Failed to fetch user donations:', error)
      throw error
    }
  }

  // FDS 상세 결과 조회
  async getFdsDetail(donationId: number): Promise<FdsDetailResult> {
    try {
      const response = await apiClient.get<ApiResponse<FdsDetailResult>>(
        `${this.basePath}/${donationId}/fds`
      )
      if (!response.data) {
        throw new Error('No FDS detail data received')
      }
      return response.data
    } catch (error) {
      console.error('Failed to fetch FDS detail:', error)
      throw error
    }
  }

  // FDS 검증 결과 오버라이드 (관리자 승인/차단)
  async overrideFdsResult(
    donationId: number,
    request: FdsOverrideRequest
  ): Promise<AdminDonation> {
    try {
      const response = await apiClient.post<ApiResponse<AdminDonation>>(
        `${this.basePath}/${donationId}/fds/override`,
        request
      )
      if (!response.data) {
        throw new Error('No override response received')
      }
      return response.data
    } catch (error) {
      console.error('Failed to override FDS result:', error)
      throw error
    }
  }

  // 기부 금액 추이 조회
  async getDonationTrends(period: '7d' | '30d' | '3m' | 'all' = '30d'): Promise<AdminDonationTrendResponse> {
    try {
      // 백엔드 API 경로: GET /api/admin/donations/trends
      const response = await apiClient.get<ApiResponse<AdminDonationTrendResponse>>(
        `/admin/donations/trends?period=${period}`
      )
      if (!response.data) {
        throw new Error('No trend data received')
      }
      return response.data
    } catch (error) {
      console.error('Failed to fetch donation trends:', error)
      throw error
    }
  }

  // 캠페인 배치 작업 실행 (블록체인 기록)
  async executeCampaignBatch(campaignId: number): Promise<CampaignCloseResponse> {
    try {
      const response = await apiClient.post<ApiResponse<CampaignCloseResponse>>(
        `/admin/campaigns/${campaignId}/close`
      )
      if (!response.data) {
        throw new Error('No batch execution response received')
      }
      return response.data
    } catch (error) {
      console.error('Failed to execute campaign batch:', error)
      throw error
    }
  }

  // 에러 핸들러
  handleApiError(error: unknown): string {
    if (error instanceof ApiError) {
      switch (error.status) {
        case 400:
          return error.message || '잘못된 요청입니다.'
        case 401:
          return '인증이 필요합니다. 다시 로그인해주세요.'
        case 403:
          return '권한이 없습니다. 관리자 계정으로 로그인해주세요.'
        case 404:
          return '기부 내역을 찾을 수 없습니다.'
        case 409:
          return '이미 처리된 기부 내역입니다.'
        case 500:
          return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
        default:
          return error.message || '알 수 없는 오류가 발생했습니다.'
      }
    }

    if (error instanceof Error) {
      return error.message
    }

    return '알 수 없는 오류가 발생했습니다.'
  }
}

export const adminDonationApi = new AdminDonationApi()