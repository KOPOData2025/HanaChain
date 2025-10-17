/**
 * Admin Organization API Client
 * 관리자용 단체 관리 API와의 통신을 위한 클라이언트
 */

import { apiClient } from './client'

// 단체 멤버 정보
export interface OrganizationMember {
  id: number // OrganizationUser ID
  userId: number
  username: string
  email: string
  fullName: string
  role: 'ORG_ADMIN' | 'ORG_MEMBER'
  joinedAt: string // createdAt from OrganizationUser
  active: boolean // user.enabled && deletedAt == null
}

// 유저 검색 결과
export interface UserSearchResult {
  id: number
  name: string
  email: string
  profileImage?: string
}

// 유저 추가 요청
export interface AddMemberRequest {
  userId: number
  role: 'ORG_ADMIN' | 'ORG_MEMBER'
}

// 역할 변경 요청
export interface UpdateMemberRoleRequest {
  role: 'ORG_ADMIN' | 'ORG_MEMBER'
}

// API 응답 타입
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

// 페이지네이션 응답 타입
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
}

class AdminOrganizationApiClient {

  /**
   * 단체 멤버 목록 조회
   */
  async getOrganizationMembers(
    organizationId: number,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<OrganizationMember>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })

    const response = await apiClient.get<ApiResponse<PageResponse<OrganizationMember>>>(
      `/admin/organizations/${organizationId}/users?${params.toString()}`
    )
    
    return response.data
  }

  /**
   * 단체에 유저 추가
   */
  async addMemberToOrganization(
    organizationId: number,
    request: AddMemberRequest
  ): Promise<OrganizationMember> {
    const response = await apiClient.post<ApiResponse<OrganizationMember>>(
      `/admin/organizations/${organizationId}/users`,
      request
    )
    
    return response.data
  }

  /**
   * 멤버 역할 변경
   */
  async updateMemberRole(
    organizationId: number,
    userId: number,
    request: UpdateMemberRoleRequest
  ): Promise<OrganizationMember> {
    const response = await apiClient.put<ApiResponse<OrganizationMember>>(
      `/admin/organizations/${organizationId}/users/${userId}`,
      request
    )
    
    return response.data
  }

  /**
   * 단체에서 멤버 제거
   */
  async removeMemberFromOrganization(
    organizationId: number,
    userId: number
  ): Promise<void> {
    const response = await apiClient.delete<ApiResponse<null>>(
      `/admin/organizations/${organizationId}/users/${userId}`
    )
    
    if (!response.success) {
      throw new Error(response.message)
    }
  }

  /**
   * 유저 검색
   */
  async searchUsers(
    keyword: string,
    limit: number = 10
  ): Promise<UserSearchResult[]> {
    const params = new URLSearchParams({
      keyword,
      size: limit.toString(),
    })

    const response = await apiClient.get<ApiResponse<PageResponse<UserSearchResult>>>(
      `/users/list?${params.toString()}`
    )
    
    return response.data.content
  }
}

// 싱글톤 인스턴스 생성
const adminOrganizationApi = new AdminOrganizationApiClient()

export default adminOrganizationApi

// 개별 함수로도 export (편의성을 위해)
export const {
  getOrganizationMembers,
  addMemberToOrganization,
  updateMemberRole,
  removeMemberFromOrganization,
  searchUsers,
} = adminOrganizationApi