// 공지사항 목록 아이템
export interface NoticeListItem {
  id: number
  title: string
  isImportant: boolean
  viewCount: number
  createdAt: string
}

// 공지사항 상세
export interface Notice {
  id: number
  title: string
  content: string
  isImportant: boolean
  viewCount: number
  createdAt: string
  updatedAt: string
}

// 공지사항 API 응답
export interface NoticeListResponse {
  data: NoticeListItem[]
}

export interface NoticeDetailResponse {
  data: Notice
}

