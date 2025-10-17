/**
 * 공지사항 관련 API 서비스
 */

import { apiClient, ApiResponse } from './client'
import { NoticeListItem, Notice } from '@/types/notice'

export class NoticeApi {
  /**
   * 최근 공지사항 조회
   * @param limit 조회할 공지사항 개수 (기본값: 3)
   * @returns 공지사항 목록
   */
  async getRecentNotices(limit: number = 3): Promise<NoticeListItem[]> {
    try {
      const endpoint = `/notices/recent?limit=${limit}`
      console.log('📋 최근 공지사항 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<NoticeListItem[]>>(endpoint)
      console.log('✅ 최근 공지사항 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '공지사항을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 최근 공지사항 조회 실패:', error)
      throw error
    }
  }

  /**
   * 중요 공지사항 조회
   * @param limit 조회할 공지사항 개수 (기본값: 5)
   * @returns 공지사항 목록
   */
  async getImportantNotices(limit: number = 5): Promise<NoticeListItem[]> {
    try {
      const endpoint = `/notices/important?limit=${limit}`
      console.log('📋 중요 공지사항 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<NoticeListItem[]>>(endpoint)
      console.log('✅ 중요 공지사항 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '중요 공지사항을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 중요 공지사항 조회 실패:', error)
      throw error
    }
  }

  /**
   * 공지사항 상세 조회
   * @param id 공지사항 ID
   * @returns 공지사항 상세 정보
   */
  async getNoticeDetail(id: number): Promise<Notice> {
    try {
      const endpoint = `/notices/${id}`
      console.log('📋 공지사항 상세 조회:', endpoint)
      
      const response = await apiClient.getPublic<ApiResponse<Notice>>(endpoint)
      console.log('✅ 공지사항 상세 조회 성공:', response)
      
      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '공지사항을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 공지사항 상세 조회 실패:', error)
      throw error
    }
  }
}

// 싱글톤 인스턴스 생성 및 export
export const noticeApi = new NoticeApi()

