import { apiClient } from './client'
import { ApiResponse } from '@/types/api'

export interface CampaignManager {
  id: number
  campaignId: number
  campaignTitle: string
  userId: number
  userName: string
  userEmail: string
  userNickname?: string
  role: 'MANAGER' | 'CO_MANAGER'
  status: 'ACTIVE' | 'REVOKED' | 'SUSPENDED'
  assignedAt: string
  revokedAt?: string
  assignedByUserId: number
  assignedByUserName: string
  notes?: string
}

export interface CampaignManagerCreateRequest {
  campaignId: number
  userId: number
  role?: 'MANAGER' | 'CO_MANAGER'
  notes?: string
}

export interface CampaignManagerUpdateRequest {
  role?: 'MANAGER' | 'CO_MANAGER'
  status?: 'ACTIVE' | 'REVOKED' | 'SUSPENDED'
  notes?: string
}

export interface UserProfile {
  id: number
  email: string
  name: string
  nickname?: string
  phoneNumber?: string
  profileImage?: string
  totalDonatedAmount: number
  totalDonationCount: number
}

export interface PageableResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const campaignManagerApi = {
  // 캠페인 담당자 등록
  async createCampaignManager(data: CampaignManagerCreateRequest): Promise<CampaignManager> {
    const response = await apiClient.post<ApiResponse<CampaignManager>>('/campaign-managers', data) as ApiResponse<CampaignManager>
    return response.data
  },

  // 캠페인 담당자 정보 수정
  async updateCampaignManager(id: number, data: CampaignManagerUpdateRequest): Promise<CampaignManager> {
    const response = await apiClient.put<ApiResponse<CampaignManager>>(`/campaign-managers/${id}`, data) as ApiResponse<CampaignManager>
    return response.data
  },

  // 캠페인 담당자 권한 해제
  async deleteCampaignManager(id: number): Promise<void> {
    await apiClient.delete(`/campaign-managers/${id}`)
  },

  // 캠페인 담당자 권한 복원
  async restoreCampaignManager(id: number): Promise<CampaignManager> {
    const response = await apiClient.post<ApiResponse<CampaignManager>>(`/campaign-managers/${id}/restore`) as ApiResponse<CampaignManager>
    return response.data
  },

  // 특정 캠페인의 모든 담당자 조회
  async getCampaignManagers(campaignId: number): Promise<CampaignManager[]> {
    const response = await apiClient.get<ApiResponse<CampaignManager[]>>(`/campaign-managers/campaigns/${campaignId}`) as ApiResponse<CampaignManager[]>
    return response.data || []
  },

  // 특정 캠페인의 활성 담당자만 조회
  async getActiveCampaignManagers(campaignId: number): Promise<CampaignManager[]> {
    const response = await apiClient.get<ApiResponse<CampaignManager[]>>(`/campaign-managers/campaigns/${campaignId}/active`) as ApiResponse<CampaignManager[]>
    return response.data || []
  },

  // 페이징된 캠페인 담당자 목록 조회
  async getCampaignManagersPaginated(campaignId: number, page = 0, size = 10): Promise<PageableResponse<CampaignManager>> {
    const response = await apiClient.get<ApiResponse<PageableResponse<CampaignManager>>>(
      `/campaign-managers/campaigns/${campaignId}/paginated?page=${page}&size=${size}&sort=assignedAt,desc`
    ) as ApiResponse<PageableResponse<CampaignManager>>
    return response.data
  },

  // 특정 캠페인과 유저의 담당자 관계 조회
  async getCampaignManager(campaignId: number, userId: number): Promise<CampaignManager> {
    const response = await apiClient.get<ApiResponse<CampaignManager>>(`/campaign-managers/campaigns/${campaignId}/users/${userId}`) as ApiResponse<CampaignManager>
    return response.data
  },

  // 캠페인 담당자 여부 확인
  async checkCampaignManager(campaignId: number, userId: number, activeOnly = true): Promise<boolean> {
    const response = await apiClient.get<ApiResponse<boolean>>(
      `/campaign-managers/campaigns/${campaignId}/users/${userId}/check?activeOnly=${activeOnly}`
    ) as ApiResponse<boolean>
    return response.data
  },

  // 캠페인 담당자 수 조회
  async countCampaignManagers(campaignId: number): Promise<number> {
    const response = await apiClient.get<ApiResponse<number>>(`/campaign-managers/campaigns/${campaignId}/count`) as ApiResponse<number>
    return response.data
  },

  // 일반 유저 목록 조회 (담당자 등록을 위한)
  async getUserList(keyword?: string, page = 0, size = 20): Promise<PageableResponse<UserProfile>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort: 'name,asc'
    })
    
    if (keyword && keyword.trim()) {
      params.append('keyword', keyword.trim())
    }

    const response = await apiClient.get<ApiResponse<PageableResponse<UserProfile>>>(`/users/list?${params}`)
    return response.data
  }
}