/**
 * 캠페인 관련 API 서비스
 * 기존 apiClient를 사용하여 백엔드와 통신
 */

import { apiClient, ApiError, ApiResponse } from './client'
import {
  CampaignCreateDto,
  AdminCampaignListParams,
  AdminCampaignDetail,
  CampaignCloseResponse,
  BatchJobStatusResponse
} from '../../types/admin'
import {
  CampaignListItem,
  CampaignDetailItem,
  CampaignListParams,
  CampaignFundraisingStats,
  SpringPageResponse
} from '../../types/donation'
import { AdminDonation } from './admin-donation-api'

export interface CampaignImageUploadResponse {
  imageUrl: string
  originalFileName: string
  fileSize: number
  message: string
}

export class CampaignApi {
  /**
   * 공개 캠페인 목록 조회 (통합 필터링)
   * @param params 조회 파라미터 (카테고리, 상태, 키워드, 정렬, 페이지네이션)
   * @returns 캠페인 목록과 페이지네이션 정보
   */
  async getCampaigns(params?: CampaignListParams): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())
      if (params?.category) queryParams.append('category', params.category)
      if (params?.status) queryParams.append('status', params.status)
      if (params?.keyword) queryParams.append('keyword', params.keyword)
      if (params?.sort) queryParams.append('sort', params.sort)

      const endpoint = `/campaigns${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('📋 캠페인 목록 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 캠페인 목록 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '캠페인 목록을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 캠페인 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 공개 캠페인 목록 조회
   * @param params 조회 파라미터
   * @returns 활성 상태인 공개 캠페인 목록
   */
  async getPublicCampaigns(params?: Pick<CampaignListParams, 'page' | 'size'>): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/campaigns/public${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('🌐 공개 캠페인 목록 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 공개 캠페인 목록 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '공개 캠페인 목록을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 공개 캠페인 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 캠페인 상세 정보 조회
   * @param id 캠페인 ID
   * @returns 캠페인 상세 정보
   */
  async getCampaignDetail(id: number): Promise<CampaignDetailItem> {
    try {
      console.log('📄 캠페인 상세 조회:', id)
      const response = await apiClient.getPublic<ApiResponse<CampaignDetailItem>>(`/campaigns/${id}`)
      console.log('✅ 캠페인 상세 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '캠페인 정보를 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 캠페인 상세 조회 실패:', error)
      throw error
    }
  }

  /**
   * 카테고리별 캠페인 조회
   * @param category 캠페인 카테고리
   * @param params 조회 파라미터
   * @returns 해당 카테고리의 캠페인 목록
   */
  async getCampaignsByCategory(
    category: CampaignListItem['category'], 
    params?: Pick<CampaignListParams, 'page' | 'size'>
  ): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/campaigns/category/${category}${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('📂 카테고리별 캠페인 조회:', { category, endpoint })
      
      const response = await apiClient.getPublic<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 카테고리별 캠페인 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '카테고리별 캠페인을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 카테고리별 캠페인 조회 실패:', error)
      throw error
    }
  }

  /**
   * 캠페인 검색
   * @param keyword 검색 키워드
   * @param params 조회 파라미터
   * @returns 검색 결과 캠페인 목록
   */
  async searchCampaigns(
    keyword: string,
    params?: Pick<CampaignListParams, 'page' | 'size'>
  ): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      queryParams.append('keyword', keyword)
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/campaigns/search?${queryParams.toString()}`
      console.log('🔍 캠페인 검색:', { keyword, endpoint })
      
      const response = await apiClient.getPublic<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 캠페인 검색 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '캠페인 검색에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 캠페인 검색 실패:', error)
      throw error
    }
  }

  /**
   * 인기 캠페인 조회
   * @param params 조회 파라미터
   * @returns 모금액이 높은 순으로 정렬된 캠페인 목록
   */
  async getPopularCampaigns(params?: Pick<CampaignListParams, 'page' | 'size'>): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/campaigns/popular${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('🔥 인기 캠페인 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 인기 캠페인 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '인기 캠페인을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 인기 캠페인 조회 실패:', error)
      throw error
    }
  }

  /**
   * 최근 캠페인 조회
   * @param params 조회 파라미터
   * @returns 최근 생성된 활성 캠페인 목록
   */
  async getRecentCampaigns(params?: Pick<CampaignListParams, 'page' | 'size'>): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/campaigns/recent${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('🕐 최근 캠페인 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 최근 캠페인 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '최근 캠페인을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 최근 캠페인 조회 실패:', error)
      throw error
    }
  }

  /**
   * 내 캠페인 목록 조회 (인증 필요)
   * @param params 조회 파라미터
   * @returns 현재 로그인한 사용자가 생성한 캠페인 목록
   */
  async getMyCampaigns(params?: Pick<CampaignListParams, 'page' | 'size'>): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/campaigns/my${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('👤 내 캠페인 목록 조회:', endpoint)
      
      const response = await apiClient.get<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 내 캠페인 목록 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '내 캠페인 목록을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 내 캠페인 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 새 캠페인 생성
   * @param data 캠페인 생성 데이터
   * @returns 생성된 캠페인 정보
   */
  async createCampaign(data: CampaignCreateDto) {
    try {
      console.log('🚀 캠페인 생성 API 호출:', data)
      console.log('🔑 현재 토큰 상태:', {
        hasToken: !!localStorage.getItem('authToken'),
        tokenValue: localStorage.getItem('authToken')?.substring(0, 20) + '...'
      })
      
      // 데이터 검증 로그
      console.log('📋 전송할 데이터 검증:', {
        title: data.title,
        description: data.description,
        targetAmount: data.targetAmount,
        category: data.category,
        startDate: data.startDate,
        endDate: data.endDate,
        imageUrl: data.imageUrl
      })
      
      const response = await apiClient.post('/campaigns', data)
      console.log('✅ 캠페인 생성 성공:', response)
      return response
    } catch (error) {
      console.error('❌ 캠페인 생성 실패:', error)
      throw error
    }
  }


  // ===== 관리자 전용 API 메서드들 =====

  /**
   * 관리자 캠페인 목록 조회 (삭제된 항목 포함)
   * @param params 조회 파라미터
   * @returns 관리자용 캠페인 목록
   */
  async getAdminCampaigns(params?: AdminCampaignListParams): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())
      if (params?.category) queryParams.append('category', params.category)
      if (params?.status) queryParams.append('status', params.status)
      if (params?.keyword) queryParams.append('keyword', params.keyword)

      const endpoint = `/admin/campaigns${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('🔍 관리자 캠페인 목록 조회:', endpoint)
      
      const response = await apiClient.get<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 관리자 캠페인 목록 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '관리자 캠페인 목록을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 관리자 캠페인 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 캠페인 상세 조회 (삭제된 항목 포함)
   * @param id 캠페인 ID
   * @returns 관리자용 캠페인 상세 정보
   */
  async getAdminCampaignDetail(id: number): Promise<CampaignDetailItem> {
    try {
      console.log('🔍 관리자 캠페인 상세 조회:', id)
      const response = await apiClient.get<ApiResponse<CampaignDetailItem>>(`/admin/campaigns/${id}`)
      console.log('✅ 관리자 캠페인 상세 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '관리자 캠페인 정보를 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 관리자 캠페인 상세 조회 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 캠페인 생성
   * @param createDto 캠페인 생성 데이터
   * @returns 생성된 캠페인 정보
   */
  async createAdminCampaign(createDto: CampaignCreateDto): Promise<CampaignDetailItem> {
    try {
      console.log('📝 관리자 캠페인 생성:', createDto)
      const response = await apiClient.post<ApiResponse<CampaignDetailItem>>('/admin/campaigns', createDto)
      console.log('✅ 관리자 캠페인 생성 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '관리자 캠페인 생성에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 관리자 캠페인 생성 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 캠페인 수정
   * @param id 캠페인 ID
   * @param updateDto 수정 데이터
   * @returns 수정된 캠페인 정보
   */
  async updateAdminCampaign(id: number, updateDto: Partial<CampaignCreateDto>): Promise<CampaignDetailItem> {
    try {
      console.log('📝 관리자 캠페인 수정:', id, updateDto)
      const response = await apiClient.put<ApiResponse<CampaignDetailItem>>(`/admin/campaigns/${id}`, updateDto)
      console.log('✅ 관리자 캠페인 수정 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '관리자 캠페인 수정에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 관리자 캠페인 수정 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 캠페인 소프트 삭제
   * @param id 캠페인 ID
   * @returns 삭제 결과
   */
  async softDeleteAdminCampaign(id: number) {
    try {
      console.log('🗑️ 관리자 캠페인 소프트 삭제:', id)
      const response = await apiClient.delete(`/admin/campaigns/${id}`)
      console.log('✅ 관리자 캠페인 소프트 삭제 성공:', response)
      return response
    } catch (error) {
      console.error('❌ 관리자 캠페인 소프트 삭제 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 캠페인 복구
   * @param id 캠페인 ID
   * @returns 복구된 캠페인 정보
   */
  async restoreAdminCampaign(id: number): Promise<CampaignDetailItem> {
    try {
      console.log('🔄 관리자 캠페인 복구:', id)
      const response = await apiClient.patch<ApiResponse<CampaignDetailItem>>(`/admin/campaigns/${id}/restore`)
      console.log('✅ 관리자 캠페인 복구 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '관리자 캠페인 복구에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 관리자 캠페인 복구 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 캠페인 상태 변경
   * @param id 캠페인 ID
   * @param status 새로운 상태
   * @returns 업데이트된 캠페인 정보
   */
  async updateAdminCampaignStatus(id: number, status: string): Promise<CampaignDetailItem> {
    try {
      console.log('📝 관리자 캠페인 상태 변경:', id, status)
      const response = await apiClient.patch<ApiResponse<CampaignDetailItem>>(`/admin/campaigns/${id}/status?status=${status}`)
      console.log('✅ 관리자 캠페인 상태 변경 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '관리자 캠페인 상태 변경에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 관리자 캠페인 상태 변경 실패:', error)
      throw error
    }
  }

  /**
   * 관리자 수혜자 주소 업데이트
   * @param id 캠페인 ID
   * @param address 수혜자 이더리움 주소
   * @returns 업데이트된 캠페인 정보
   */
  async updateBeneficiaryAddress(id: number, address: string): Promise<CampaignDetailItem> {
    try {
      console.log('💰 수혜자 주소 업데이트:', id, address)
      const response = await apiClient.patch<ApiResponse<CampaignDetailItem>>(`/admin/campaigns/${id}/beneficiary-address?address=${encodeURIComponent(address)}`)
      console.log('✅ 수혜자 주소 업데이트 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '수혜자 주소 업데이트에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 수혜자 주소 업데이트 실패:', error)
      throw error
    }
  }

  /**
   * 삭제된 캠페인 목록 조회
   * @param params 조회 파라미터
   * @returns 삭제된 캠페인 목록
   */
  async getDeletedCampaigns(params?: Pick<AdminCampaignListParams, 'page' | 'size'>): Promise<SpringPageResponse<CampaignListItem>> {
    try {
      const queryParams = new URLSearchParams()
      
      if (params?.page !== undefined) queryParams.append('page', params.page.toString())
      if (params?.size !== undefined) queryParams.append('size', params.size.toString())

      const endpoint = `/admin/campaigns/deleted${queryParams.toString() ? `?${queryParams.toString()}` : ''}`
      console.log('🗑️ 삭제된 캠페인 목록 조회:', endpoint)
      
      const response = await apiClient.get<ApiResponse<SpringPageResponse<CampaignListItem>>>(endpoint)
      console.log('✅ 삭제된 캠페인 목록 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '삭제된 캠페인 목록을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 삭제된 캠페인 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 캠페인용 이미지 업로드
   * @param file 업로드할 이미지 파일
   * @returns 업로드된 이미지 정보
   */
  async uploadCampaignImage(file: File): Promise<CampaignImageUploadResponse> {
    try {
      console.log('📸 캠페인 이미지 업로드 시작:', {
        fileName: file.name,
        fileSize: file.size,
        fileType: file.type
      })

      // 파일 유효성 검사
      if (!file.type.startsWith('image/')) {
        throw new Error('이미지 파일만 업로드 가능합니다.')
      }

      if (file.size > 10 * 1024 * 1024) { // 10MB 제한
        throw new Error('파일 크기는 10MB 이하여야 합니다.')
      }

      const formData = new FormData()
      formData.append('image', file)
      
      // ApiClient에서 직접 FormData 처리를 위해 fetch를 직접 사용
      const baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
      const token = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null
      
      const response = await fetch(`${baseURL}/admin/campaigns/image`, {
        method: 'POST',
        body: formData,
        headers: {
          ...(token && { Authorization: `Bearer ${token}` }),
          // Note: FormData를 사용할 때는 Content-Type을 설정하지 않음 (브라우저가 자동 설정)
        },
      })
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new ApiError(errorData.message || `이미지 업로드 실패: ${response.status}`, response.status)
      }
      
      const result: ApiResponse<CampaignImageUploadResponse> = await response.json()
      
      if (result.success && result.data) {
        console.log('✅ 캠페인 이미지 업로드 성공:', result.data)
        return result.data
      } else {
        throw new ApiError(result.message || '이미지 업로드에 실패했습니다')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      console.error('❌ 캠페인 이미지 업로드 에러:', error)
      throw new ApiError('캠페인 이미지 업로드에 실패했습니다')
    }
  }

  /**
   * 캠페인의 FDS 검증 미통과 거래 목록 조회
   * @param campaignId 캠페인 ID
   * @returns FDS 검증을 통과하지 못한 거래 목록
   */
  async getUnverifiedFdsDonations(campaignId: number): Promise<AdminDonation[]> {
    try {
      console.log('🔍 FDS 검증 미통과 거래 목록 조회:', campaignId)

      const response = await apiClient.get<ApiResponse<AdminDonation[]>>(
        `/admin/campaigns/${campaignId}/unverified-fds-donations`
      )

      console.log('✅ FDS 검증 미통과 거래 목록 조회 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || 'FDS 검증 미통과 거래 목록 조회에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ FDS 검증 미통과 거래 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 캠페인 마감 및 배치 작업 시작
   * @param campaignId 캠페인 ID
   * @returns 배치 작업 시작 결과
   */
  async closeCampaignAndStartBatch(campaignId: number): Promise<CampaignCloseResponse> {
    try {
      console.log('🔒 캠페인 마감 및 배치 작업 시작:', campaignId)

      const response = await apiClient.post<ApiResponse<CampaignCloseResponse>>(
        `/admin/campaigns/${campaignId}/close`
      )

      console.log('✅ 배치 작업 시작 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '배치 작업 시작에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 배치 작업 시작 실패:', error)
      throw error
    }
  }

  /**
   * 배치 작업 상태 조회
   * @param jobExecutionId 배치 작업 실행 ID
   * @returns 배치 작업 상태 정보
   */
  async getBatchJobStatus(jobExecutionId: number): Promise<BatchJobStatusResponse> {
    try {
      console.log('📊 배치 작업 상태 조회:', jobExecutionId)

      const response = await apiClient.get<ApiResponse<BatchJobStatusResponse>>(
        `/admin/batch/jobs/${jobExecutionId}/status`
      )

      console.log('✅ 배치 작업 상태 조회 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '배치 작업 상태 조회에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 배치 작업 상태 조회 실패:', error)
      throw error
    }
  }

  /**
   * 캠페인 모금 통계 조회 (캠페인 담당자 전용)
   * @param campaignId 캠페인 ID
   * @returns 캠페인 모금 통계 정보
   */
  async getCampaignFundraisingStats(campaignId: number): Promise<CampaignFundraisingStats> {
    try {
      console.log('📊 캠페인 모금 통계 조회:', campaignId)

      const response = await apiClient.get<ApiResponse<CampaignFundraisingStats>>(
        `/campaigns/${campaignId}/fundraising-stats`
      )

      console.log('✅ 캠페인 모금 통계 조회 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '모금 통계 조회에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 캠페인 모금 통계 조회 실패:', error)
      throw error
    }
  }

  /**
   * API 에러 처리 헬퍼
   * @param error ApiError 객체
   * @returns 사용자 친화적 에러 메시지
   */
  handleApiError(error: ApiError): string {
    switch (error.status) {
      case 400:
        return error.details?.message || '입력값을 확인해주세요'
      case 401:
        return '인증이 필요합니다. 다시 로그인해주세요'
      case 403:
        return '권한이 없습니다'
      case 404:
        return '요청한 캠페인을 찾을 수 없습니다'
      case 409:
        return '이미 존재하는 캠페인입니다'
      case 500:
        return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요'
      default:
        return '네트워크 오류가 발생했습니다. 연결을 확인해주세요'
    }
  }
}

// 싱글톤 인스턴스 생성 및 내보내기
export const campaignApi = new CampaignApi()

// 개별 함수들을 직접 내보내기 (편의성을 위해)
export const getCampaigns = (params?: CampaignListParams) => campaignApi.getCampaigns(params)
export const getPublicCampaigns = (params?: Pick<CampaignListParams, 'page' | 'size'>) => campaignApi.getPublicCampaigns(params)
export const getCampaignDetail = (id: number) => campaignApi.getCampaignDetail(id)
export const getCampaignsByCategory = (category: CampaignListItem['category'], params?: Pick<CampaignListParams, 'page' | 'size'>) => campaignApi.getCampaignsByCategory(category, params)
export const searchCampaigns = (keyword: string, params?: Pick<CampaignListParams, 'page' | 'size'>) => campaignApi.searchCampaigns(keyword, params)
export const getPopularCampaigns = (params?: Pick<CampaignListParams, 'page' | 'size'>) => campaignApi.getPopularCampaigns(params)
export const getRecentCampaigns = (params?: Pick<CampaignListParams, 'page' | 'size'>) => campaignApi.getRecentCampaigns(params)
export const getMyCampaigns = (params?: Pick<CampaignListParams, 'page' | 'size'>) => campaignApi.getMyCampaigns(params)
export const createCampaign = (data: CampaignCreateDto) => campaignApi.createCampaign(data)

// 관리자 전용 함수들
export const getAdminCampaigns = (params?: AdminCampaignListParams) => campaignApi.getAdminCampaigns(params)
export const getAdminCampaignDetail = (id: number) => campaignApi.getAdminCampaignDetail(id)
export const createAdminCampaign = (createDto: CampaignCreateDto) => campaignApi.createAdminCampaign(createDto)
export const updateAdminCampaign = (id: number, updateDto: Partial<CampaignCreateDto>) => campaignApi.updateAdminCampaign(id, updateDto)
export const softDeleteAdminCampaign = (id: number) => campaignApi.softDeleteAdminCampaign(id)
export const restoreAdminCampaign = (id: number) => campaignApi.restoreAdminCampaign(id)
export const updateAdminCampaignStatus = (id: number, status: string) => campaignApi.updateAdminCampaignStatus(id, status)
export const updateBeneficiaryAddress = (id: number, address: string) => campaignApi.updateBeneficiaryAddress(id, address)
export const getDeletedCampaigns = (params?: Pick<AdminCampaignListParams, 'page' | 'size'>) => campaignApi.getDeletedCampaigns(params)
export const uploadCampaignImage = (file: File) => campaignApi.uploadCampaignImage(file)

// FDS 검증 관련 함수들
export const getUnverifiedFdsDonations = (campaignId: number) => campaignApi.getUnverifiedFdsDonations(campaignId)

// 배치 작업 관련 함수들
export const closeCampaignAndStartBatch = (campaignId: number) => campaignApi.closeCampaignAndStartBatch(campaignId)
export const getBatchJobStatus = (jobExecutionId: number) => campaignApi.getBatchJobStatus(jobExecutionId)

// 캠페인 모금 통계 조회
export const getCampaignFundraisingStats = (campaignId: number) => campaignApi.getCampaignFundraisingStats(campaignId)