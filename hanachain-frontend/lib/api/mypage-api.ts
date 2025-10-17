/**
 * 마이페이지 관련 API 함수들
 * 백엔드 mypage 컨트롤러와 연동하는 실제 API 호출 함수들
 */

import { apiClient, ApiResponse, ApiError } from './client'

// 마이페이지 API 인터페이스 타입 정의

export interface ProfileData {
  nickname: string
  email: string
  profileImage?: string | null
  profileCompleted?: boolean
}

export interface DonationStats {
  totalAmount: number
  totalCount: number
  completedCount: number
  pendingCount: number
  failedCount: number
}

export interface DonationRecord {
  id: string
  campaignId: string
  campaignTitle: string
  campaignImage?: string
  amount: number
  status: 'completed' | 'pending' | 'failed' | 'cancelled'
  donatedAt: string
  message?: string
  paymentMethod?: 'card' | 'bank' | 'naverpay' | 'kakaopay' | 'paypal' | 'other'
  receiptNumber?: string
  donationTransactionHash?: string
}

export interface DashboardData {
  profile: ProfileData
  donationStats: DonationStats
  recentDonations: DonationRecord[]
  favoriteCampaignsCount: number
}

export interface ProfileUpdateData {
  nickname?: string
}

export interface ProfileImageUploadResponse {
  imageUrl: string
  originalFileName: string
  fileSize: number
  message: string
}

// 마이페이지 API 클래스
export class MyPageApi {
  /**
   * 대시보드 종합 정보 조회
   */
  static async getDashboard(): Promise<DashboardData> {
    try {
      console.log('🚀 마이페이지 대시보드 API 요청 시작:', '/mypage/dashboard')
      
      // 디버그: API 클라이언트 설정 확인
      const token = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null
      console.log('🔑 저장된 토큰 상태:', {
        hasToken: !!token,
        tokenLength: token?.length || 0,
        tokenPreview: token ? token.substring(0, 20) + '...' : 'No token',
        baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
      })
      
      const response = await apiClient.get<ApiResponse<DashboardData>>('/mypage/dashboard')
      
      console.log('📥 마이페이지 대시보드 API 응답 전체:', response)
      
      if (response.success && response.data) {
        console.log('✅ 마이페이지 대시보드 데이터 수신 성공:', {
          profile: response.data.profile,
          donationStats: response.data.donationStats,
          recentDonations: response.data.recentDonations,
          favoriteCampaignsCount: response.data.favoriteCampaignsCount
        })
        return response.data
      } else {
        console.error('❌ 마이페이지 대시보드 API 응답 실패:', response.message)
        throw new ApiError(response.message || '대시보드 정보 조회에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('💥 마이페이지 대시보드 fetch 에러:', error)
      throw new ApiError('대시보드 정보를 불러오는데 실패했습니다')
    }
  }

  /**
   * 대시보드 요약 정보 조회
   */
  static async getDashboardSummary(): Promise<DashboardData> {
    try {
      const response = await apiClient.get<ApiResponse<DashboardData>>('/mypage/dashboard/summary')
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new ApiError(response.message || '대시보드 요약 정보 조회에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('Dashboard summary fetch error:', error)
      throw new ApiError('대시보드 요약 정보를 불러오는데 실패했습니다')
    }
  }

  /**
   * 프로필 정보 조회
   */
  static async getProfile(): Promise<ProfileData> {
    try {
      console.log('🚀 마이페이지 프로필 API 요청 시작:', '/mypage/profile')
      const response = await apiClient.get<ApiResponse<ProfileData>>('/mypage/profile')
      
      console.log('📥 마이페이지 프로필 API 응답 전체:', response)
      
      if (response.success && response.data) {
        console.log('✅ 마이페이지 프로필 데이터 수신 성공:', {
          nickname: response.data.nickname,
          email: response.data.email,
          profileImage: response.data.profileImage,
          profileCompleted: response.data.profileCompleted
        })
        return response.data
      } else {
        console.error('❌ 마이페이지 프로필 API 응답 실패:', response.message)
        throw new ApiError(response.message || '프로필 정보 조회에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('💥 마이페이지 프로필 fetch 에러:', error)
      throw new ApiError('프로필 정보를 불러오는데 실패했습니다')
    }
  }

  /**
   * 프로필 정보 수정
   */
  static async updateProfile(data: ProfileUpdateData): Promise<ProfileData> {
    try {
      const response = await apiClient.put<ApiResponse<ProfileData>>('/mypage/profile', data)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new ApiError(response.message || '프로필 수정에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('Profile update error:', error)
      throw new ApiError('프로필 수정에 실패했습니다')
    }
  }

  /**
   * 프로필 이미지 업로드
   */
  static async uploadProfileImage(file: File): Promise<ProfileImageUploadResponse> {
    try {
      const formData = new FormData()
      formData.append('image', file)
      
      // ApiClient에서 직접 FormData 처리를 위해 fetch를 직접 사용
      const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
      const token = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null
      
      const response = await fetch(`${baseURL}/mypage/profile/image`, {
        method: 'POST',
        body: formData,
        headers: {
          ...(token && { Authorization: `Bearer ${token}` }),
          // Note: FormData를 사용할 때는 Content-Type을 설정하지 않음 (브라우저가 자동 설정)
        },
      })
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new ApiError(errorData.message || `이미지 업로드 실패: ${response.status}`)
      }
      
      const result: ApiResponse<ProfileImageUploadResponse> = await response.json()
      
      if (result.success && result.data) {
        return result.data
      } else {
        throw new ApiError(result.message || '이미지 업로드에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('Profile image upload error:', error)
      throw new ApiError('프로필 이미지 업로드에 실패했습니다')
    }
  }

  /**
   * 프로필 이미지 삭제
   */
  static async deleteProfileImage(): Promise<void> {
    try {
      const response = await apiClient.delete<ApiResponse<void>>('/mypage/profile/image')
      
      if (!response.success) {
        throw new ApiError(response.message || '프로필 이미지 삭제에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('Profile image delete error:', error)
      throw new ApiError('프로필 이미지 삭제에 실패했습니다')
    }
  }

  /**
   * 프로필 완성도 조회
   */
  static async getProfileCompleteness(): Promise<number> {
    try {
      const response = await apiClient.get<ApiResponse<number>>('/mypage/profile/completeness')
      
      if (response.success && response.data !== undefined) {
        return response.data
      } else {
        throw new ApiError(response.message || '프로필 완성도 조회에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('Profile completeness fetch error:', error)
      throw new ApiError('프로필 완성도 조회에 실패했습니다')
    }
  }

  /**
   * 프로필 완성도 업데이트
   */
  static async refreshProfileCompleteness(): Promise<void> {
    try {
      const response = await apiClient.post<ApiResponse<void>>('/mypage/profile/completeness/refresh')
      
      if (!response.success) {
        throw new ApiError(response.message || '프로필 완성도 업데이트에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('Profile completeness refresh error:', error)
      throw new ApiError('프로필 완성도 업데이트에 실패했습니다')
    }
  }
}

// 편의를 위한 개별 함수들 (기존 코드와의 호환성)
export const getDashboard = MyPageApi.getDashboard
export const getDashboardSummary = MyPageApi.getDashboardSummary
export const getProfile = MyPageApi.getProfile
export const updateProfile = MyPageApi.updateProfile
export const uploadProfileImage = MyPageApi.uploadProfileImage
export const deleteProfileImage = MyPageApi.deleteProfileImage
export const getProfileCompleteness = MyPageApi.getProfileCompleteness
export const refreshProfileCompleteness = MyPageApi.refreshProfileCompleteness