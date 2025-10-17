/**
 * Organization API Client
 * 백엔드 단체 관리 API와의 통신을 위한 클라이언트
 */

import { apiClient, ApiResponse } from './client'
import { OrganizationCreateRequest } from '@/types/organization'

// Backend DTO와 일치하는 Organization 인터페이스
export interface Organization {
  id: number
  name: string
  description: string
  imageUrl?: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
  memberCount?: number
  adminCount?: number
  activeCampaignCount?: number
  walletAddress?: string  // 블록체인 지갑 주소
}

// 페이지네이션 응답 타입 (Spring Data의 Page 객체와 일치)
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number // 현재 페이지 번호 (0-based)
  first: boolean
  last: boolean
  numberOfElements: number
}

// 단체 생성 요청 DTO
export interface OrganizationCreateRequest {
  name: string
  description: string
  imageUrl?: string
  status?: 'ACTIVE' | 'INACTIVE'
}

// 단체 수정 요청 DTO
export interface OrganizationUpdateRequest {
  name?: string
  description?: string
  imageUrl?: string
  status?: 'ACTIVE' | 'INACTIVE'
}

// 필터링 옵션
export interface OrganizationFilters {
  status?: 'ACTIVE' | 'INACTIVE' | 'ALL'
  search?: string
}

class OrganizationApiClient {

  /**
   * 모든 단체 목록 조회 (페이지네이션 지원)
   */
  async getAllOrganizations(
    page: number = 0,
    size: number = 10,
    filters?: OrganizationFilters
  ): Promise<PageResponse<Organization>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    // 필터링 파라미터 추가
    if (filters?.status && filters.status !== 'ALL') {
      params.append('status', filters.status)
    }
    
    if (filters?.search) {
      params.append('search', filters.search)
    }

    return apiClient.get<PageResponse<Organization>>(
      `/organizations?${params.toString()}`
    )
  }

  /**
   * 특정 단체 상세 조회
   */
  async getOrganization(id: number): Promise<Organization> {
    return apiClient.get<Organization>(`/organizations/${id}`)
  }

  /**
   * 새 단체 생성 (블록체인 지갑 자동 생성 포함)
   */
  async createOrganization(data: OrganizationCreateRequest): Promise<Organization> {
    return apiClient.post<Organization>('/organizations', data)
  }

  /**
   * 단체 정보 수정
   */
  async updateOrganization(id: number, data: OrganizationUpdateRequest): Promise<Organization> {
    return apiClient.put<Organization>(`/organizations/${id}`, data)
  }

  /**
   * 단체 상태 변경
   */
  async updateOrganizationStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Promise<Organization> {
    return apiClient.put<Organization>(`/organizations/${id}/status`, { status })
  }

  /**
   * 단체 삭제 (소프트 삭제)
   */
  async deleteOrganization(id: number): Promise<void> {
    await apiClient.delete<void>(`/organizations/${id}`)
  }

  /**
   * 단체 검색 (이름으로 필터링)
   * 백엔드의 /organizations/search 엔드포인트 사용
   */
  async searchOrganizations(
    query: string,
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<Organization>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    // 검색어가 있을 경우 name 파라미터 추가
    if (query && query.trim()) {
      params.append('name', query.trim())
    }

    return apiClient.get<PageResponse<Organization>>(
      `/organizations/search?${params.toString()}`
    )
  }

  /**
   * 활성화된 단체만 검색 (자동완성용)
   */
  async searchActiveOrganizations(
    query: string,
    limit: number = 10
  ): Promise<Organization[]> {
    const params = new URLSearchParams({
      page: '0',
      size: limit.toString(),
      status: 'ACTIVE'
    })

    // 검색어가 있을 경우 name 파라미터 추가
    if (query && query.trim()) {
      params.append('name', query.trim())
    }

    const response = await apiClient.get<PageResponse<Organization>>(
      `/organizations/search?${params.toString()}`
    )

    return response.content
  }

  /**
   * 단체 통계 정보 조회
   */
  async getOrganizationStats(): Promise<{
    total: number
    active: number
    inactive: number
  }> {
    try {
      const response = await this.getAllOrganizations(0, 1000) // 모든 데이터 가져오기
      const organizations = response.content

      return {
        total: organizations.length,
        active: organizations.filter(org => org.status === 'ACTIVE').length,
        inactive: organizations.filter(org => org.status === 'INACTIVE').length,
      }
    } catch (error) {
      console.error('Failed to get organization stats:', error)
      return { total: 0, active: 0, inactive: 0 }
    }
  }

  /**
   * 단체의 블록체인 지갑 주소 조회
   */
  async getOrganizationWallet(id: number): Promise<{
    organizationId: string
    organizationName: string
    walletAddress: string
  }> {
    return apiClient.get<{
      organizationId: string
      organizationName: string
      walletAddress: string
    }>(`/organizations/${id}/wallet`)
  }
}

// 싱글톤 인스턴스 생성
const organizationApi = new OrganizationApiClient()

export default organizationApi

// 개별 함수로도 export (편의성을 위해)
export const {
  getAllOrganizations,
  getOrganization,
  createOrganization,
  updateOrganization,
  updateOrganizationStatus,
  deleteOrganization,
  searchOrganizations,
  searchActiveOrganizations,
  getOrganizationStats,
  getOrganizationWallet,
} = organizationApi