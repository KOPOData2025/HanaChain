/**
 * 댓글 작성자 정보 인터페이스
 */
export interface CommentAuthor {
  id: number
  name: string
  email: string
  profileImageUrl?: string
  nickname?: string
}

/**
 * 댓글 인터페이스
 */
export interface Comment {
  id: number
  content: string
  createdAt: string
  updatedAt?: string

  // 작성자 정보
  author: CommentAuthor

  // 권한 및 상태 정보
  isCampaignManager: boolean  // 캠페인 담당자 여부
  hasDonated: boolean          // 기부 이력 여부 (천사 로고 표시용)
  isDeleted: boolean           // 삭제 여부

  // 대댓글 정보
  replies: Comment[]

  // 통계 정보
  likeCount: number
  replyCount: number
}

/**
 * 댓글 작성 요청 DTO
 */
export interface CommentCreateRequest {
  content: string
}

/**
 * 답글 작성 요청 DTO
 */
export interface CommentReplyRequest {
  content: string
}

/**
 * 댓글 수정 요청 DTO
 */
export interface CommentUpdateRequest {
  content: string
}

/**
 * 댓글 목록 응답 (페이지네이션)
 */
export interface CommentsPageResponse {
  content: Comment[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

/**
 * 캠페인 담당자 인터페이스 (댓글 권한 확인용)
 */
export interface CampaignManager {
  id: number
  userId: number
  userName: string
  userEmail: string
  userNickname?: string
  role: string
  status: string
  assignedAt: string
}
