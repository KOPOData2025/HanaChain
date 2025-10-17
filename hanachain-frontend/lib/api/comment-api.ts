/**
 * 댓글 관련 API 서비스
 */

import { apiClient, ApiError, ApiResponse } from './client'
import type {
  Comment,
  CommentCreateRequest,
  CommentReplyRequest,
  CommentUpdateRequest,
  CommentsPageResponse,
} from '@/types/comment'

export class CommentApi {
  /**
   * 캠페인의 댓글 목록 조회 (공개)
   */
  async getCampaignComments(
    campaignId: number,
    page: number = 0,
    size: number = 10
  ): Promise<CommentsPageResponse> {
    try {
      console.log('📋 캠페인 댓글 목록 조회:', { campaignId, page, size })

      const response = await apiClient.getPublic<ApiResponse<CommentsPageResponse>>(
        `/campaigns/${campaignId}/comments?page=${page}&size=${size}`
      )

      console.log('✅ 캠페인 댓글 목록 조회 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '댓글 목록을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 캠페인 댓글 목록 조회 실패:', error)
      throw error
    }
  }

  /**
   * 댓글 상세 조회
   */
  async getComment(commentId: number): Promise<Comment> {
    try {
      console.log('📄 댓글 상세 조회:', commentId)

      const response = await apiClient.getPublic<ApiResponse<Comment>>(
        `/comments/${commentId}`
      )

      console.log('✅ 댓글 상세 조회 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '댓글을 불러오는데 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 댓글 상세 조회 실패:', error)
      throw error
    }
  }

  /**
   * 댓글 작성 (로그인 필요)
   */
  async createComment(
    campaignId: number,
    data: CommentCreateRequest
  ): Promise<Comment> {
    try {
      console.log('💬 댓글 작성:', { campaignId, data })

      const response = await apiClient.post<ApiResponse<Comment>>(
        `/campaigns/${campaignId}/comments`,
        data
      )

      console.log('✅ 댓글 작성 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '댓글 작성에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 댓글 작성 실패:', error)
      throw error
    }
  }

  /**
   * 답글 작성 (캠페인 담당자만 가능)
   */
  async createReply(
    commentId: number,
    data: CommentReplyRequest
  ): Promise<Comment> {
    try {
      console.log('💬 답글 작성:', { commentId, data })

      const response = await apiClient.post<ApiResponse<Comment>>(
        `/comments/${commentId}/reply`,
        data
      )

      console.log('✅ 답글 작성 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '답글 작성에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 답글 작성 실패:', error)
      throw error
    }
  }

  /**
   * 댓글 수정 (작성자만 가능)
   */
  async updateComment(
    commentId: number,
    data: CommentUpdateRequest
  ): Promise<Comment> {
    try {
      console.log('✏️ 댓글 수정:', { commentId, data })

      const response = await apiClient.put<ApiResponse<Comment>>(
        `/comments/${commentId}`,
        data
      )

      console.log('✅ 댓글 수정 성공:', response)

      if (response.success && response.data) {
        return response.data
      } else {
        throw new Error(response.message || '댓글 수정에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 댓글 수정 실패:', error)
      throw error
    }
  }

  /**
   * 댓글 삭제 (작성자만 가능)
   */
  async deleteComment(commentId: number): Promise<void> {
    try {
      console.log('🗑️ 댓글 삭제:', commentId)

      const response = await apiClient.delete<ApiResponse<void>>(
        `/comments/${commentId}`
      )

      console.log('✅ 댓글 삭제 성공:', response)

      if (!response.success) {
        throw new Error(response.message || '댓글 삭제에 실패했습니다')
      }
    } catch (error) {
      console.error('❌ 댓글 삭제 실패:', error)
      throw error
    }
  }

  /**
   * API 에러 처리 헬퍼
   */
  handleApiError(error: ApiError): string {
    switch (error.status) {
      case 400:
        return error.details?.message || '입력값을 확인해주세요'
      case 401:
        return '로그인이 필요합니다'
      case 403:
        return '권한이 없습니다'
      case 404:
        return '댓글을 찾을 수 없습니다'
      case 500:
        return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요'
      default:
        return '네트워크 오류가 발생했습니다. 연결을 확인해주세요'
    }
  }
}

// 싱글톤 인스턴스 생성 및 내보내기
export const commentApi = new CommentApi()

// 개별 함수들을 직접 내보내기 (편의성을 위해)
export const getCampaignComments = (campaignId: number, page?: number, size?: number) =>
  commentApi.getCampaignComments(campaignId, page, size)
export const getComment = (commentId: number) => commentApi.getComment(commentId)
export const createComment = (campaignId: number, data: CommentCreateRequest) =>
  commentApi.createComment(campaignId, data)
export const createReply = (commentId: number, data: CommentReplyRequest) =>
  commentApi.createReply(commentId, data)
export const updateComment = (commentId: number, data: CommentUpdateRequest) =>
  commentApi.updateComment(commentId, data)
export const deleteComment = (commentId: number) => commentApi.deleteComment(commentId)
