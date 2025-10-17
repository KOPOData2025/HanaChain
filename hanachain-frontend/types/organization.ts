/**
 * Organization 관련 타입 정의
 */

// 단체 생성 요청 DTO
export interface OrganizationCreateRequest {
  name: string
  description?: string
  imageUrl?: string
}

// 단체 상태
export type OrganizationStatus = 'ACTIVE' | 'INACTIVE'
