/**
 * 사용자 관리 API
 */

import { apiClient, ApiError } from './client'

// 사용자 프로필 인터페이스 (백엔드 UserProfileDto와 매핑)
export interface UserProfile {
  id: number
  email: string
  nickname?: string
  name?: string
  phoneNumber?: string
  profileImage?: string
  createdAt: string
  updatedAt: string
  emailVerified: boolean
  status: 'active' | 'inactive' | 'suspended'
}

// 페이지 응답 인터페이스 (Spring Data Page 구조)
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

// 사용자 API 클래스
class UserApi {
  /**
   * 사용자 목록 조회 (관리자 전용)
   * @param keyword 검색 키워드
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   * @returns 페이지네이션된 사용자 목록
   */
  async getUserList(
    keyword?: string,
    page: number = 0,
    size: number = 20
  ): Promise<PageResponse<UserProfile>> {
    try {
      console.log('📋 사용자 목록 조회 시작:', { keyword, page, size })

      const params = new URLSearchParams()
      if (keyword) params.append('keyword', keyword)
      params.append('page', page.toString())
      params.append('size', size.toString())

      const response = await apiClient.get<{
        success: boolean
        data: PageResponse<UserProfile>
        message?: string
      }>(`/users/list?${params.toString()}`)

      if (response.success && response.data) {
        console.log('✅ 사용자 목록 조회 성공:', {
          count: response.data.content.length,
          totalElements: response.data.totalElements,
          currentPage: response.data.number
        })
        return response.data
      }

      throw new Error(response.message || '사용자 목록 조회 실패')
    } catch (error) {
      console.error('❌ 사용자 목록 조회 실패:', error)

      if (error instanceof ApiError) {
        throw error
      }

      throw new Error('사용자 목록을 불러오는데 실패했습니다')
    }
  }

  /**
   * 특정 사용자 프로필 조회
   * @param userId 사용자 ID
   * @returns 사용자 프로필
   */
  async getUserProfile(userId: number): Promise<UserProfile> {
    try {
      console.log('🔍 사용자 프로필 조회:', userId)

      const response = await apiClient.get<{
        success: boolean
        data: UserProfile
        message?: string
      }>(`/users/${userId}`)

      if (response.success && response.data) {
        console.log('✅ 사용자 프로필 조회 성공:', response.data)
        return response.data
      }

      throw new Error(response.message || '사용자 프로필 조회 실패')
    } catch (error) {
      console.error('❌ 사용자 프로필 조회 실패:', error)

      if (error instanceof ApiError) {
        throw error
      }

      throw new Error('사용자 정보를 불러오는데 실패했습니다')
    }
  }

  /**
   * 사용자 상태 변경 (관리자 전용)
   * @param userId 사용자 ID
   * @param status 새로운 상태
   */
  async updateUserStatus(
    userId: number,
    status: 'active' | 'inactive' | 'suspended'
  ): Promise<void> {
    try {
      console.log('🔄 사용자 상태 변경:', { userId, status })

      const response = await apiClient.put<{
        success: boolean
        message?: string
      }>(`/users/${userId}/status`, { status })

      if (response.success) {
        console.log('✅ 사용자 상태 변경 성공')
        return
      }

      throw new Error(response.message || '사용자 상태 변경 실패')
    } catch (error) {
      console.error('❌ 사용자 상태 변경 실패:', error)

      if (error instanceof ApiError) {
        throw error
      }

      throw new Error('사용자 상태를 변경하는데 실패했습니다')
    }
  }
}

const userApi = new UserApi()
export default userApi
