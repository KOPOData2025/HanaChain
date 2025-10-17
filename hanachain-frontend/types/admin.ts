/**
 * 관리자 기능 관련 타입 정의
 */

// 캠페인 생성 DTO
export interface CampaignCreateDto {
  title: string
  subtitle?: string
  description: string
  organizer: string
  targetAmount: number
  imageUrl?: string
  category: CampaignCategory
  startDate: string  // ISO 8601 format
  endDate: string    // ISO 8601 format
  beneficiaryAddress?: string  // 블록체인 수혜자 주소 (이더리움 주소 형식)
  organizationId?: number  // 조직 ID (제공 시 자동으로 조직 지갑 주소를 beneficiaryAddress로 매핑)
}

// 캠페인 카테고리
export type CampaignCategory = 
  | 'MEDICAL' 
  | 'EDUCATION' 
  | 'DISASTER_RELIEF'
  | 'ENVIRONMENT'
  | 'ANIMAL_WELFARE'
  | 'COMMUNITY'
  | 'EMERGENCY'
  | 'OTHER'

// 캠페인 상태
export type CampaignStatus = 
  | 'DRAFT'
  | 'ACTIVE' 
  | 'COMPLETED'
  | 'CANCELLED'

// 카테고리 한국어 매핑
export const CATEGORY_LABELS: Record<CampaignCategory, string> = {
  MEDICAL: '의료',
  EDUCATION: '교육', 
  DISASTER_RELIEF: '재해구호',
  ENVIRONMENT: '환경',
  ANIMAL_WELFARE: '동물복지',
  COMMUNITY: '지역사회',
  EMERGENCY: '응급상황',
  OTHER: '기타'
}

// 상태 한국어 매핑
export const STATUS_LABELS: Record<CampaignStatus, string> = {
  DRAFT: '초안',
  ACTIVE: '진행중',
  COMPLETED: '마감됨',
  CANCELLED: '취소됨'
}

// 관리자 캠페인 목록 조회 파라미터
export interface AdminCampaignListParams {
  page?: number
  size?: number
  category?: CampaignCategory
  status?: CampaignStatus
  keyword?: string
}

// 관리자 캠페인 상세 정보
export interface AdminCampaignDetail {
  id: number
  title: string
  description: string
  targetAmount: number
  currentAmount: number
  donorCount: number
  imageUrl?: string
  status: CampaignStatus
  category: CampaignCategory
  startDate: string
  endDate: string
  createdAt: string
  updatedAt: string
  user: {
    id: number
    name: string
    email: string
  }
  progressPercentage: number
  isActive: boolean
}

// 배치 작업 상태
export type BatchJobStatus =
  | 'STARTING'
  | 'STARTED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'STOPPED'
  | 'UNKNOWN'

// 캠페인 마감 응답
export interface CampaignCloseResponse {
  campaignId: number
  campaignTitle: string
  jobExecutionId: number
  totalDonations: number
  batchStatus: string
  batchStartedAt: string
  message: string
}

// 배치 작업 상태 응답
export interface BatchJobStatusResponse {
  jobExecutionId: number
  campaignId: number
  jobName: string
  status: BatchJobStatus
  startTime: string
  endTime?: string
  totalProcessed: number
  successfulTransfers: number
  failedTransfers: number
  skippedCount: number
  progressPercentage: number
  exitCode: string
  running: boolean
}

// 배치 상태 한국어 매핑
export const BATCH_STATUS_LABELS: Record<BatchJobStatus, string> = {
  STARTING: '시작중',
  STARTED: '시작됨',
  RUNNING: '실행중',
  COMPLETED: '완료',
  FAILED: '실패',
  STOPPED: '중지됨',
  UNKNOWN: '알 수 없음'
}

// FDS (사기 탐지 시스템) 상세 결과
export interface FdsDetailResult {
  // 기본 정보
  donationId: number
  action: 'APPROVE' | 'MANUAL_REVIEW' | 'BLOCK'
  actionId: number
  riskScore: number
  confidence: number
  explanation: string
  checkedAt: string

  // 17개 입력 특징
  features: FdsFeatures

  // DQN Q-values (3개 액션별)
  qValues: {
    approve: number
    manualReview: number
    block: number
  }

  // 추가 정보
  timestamp: string
}

// FDS 입력 특징 (17개)
export interface FdsFeatures {
  // 거래 정보 (4개)
  amountNormalized: number        // 0: 정규화된 금액 (0-1)
  hourOfDay: number               // 1: 시간대 (0-23)
  dayOfWeek: number               // 2: 요일 (0-6, 월요일=0)
  isWeekend: number               // 3: 주말 여부 (0 or 1)

  // 사용자 계정 정보 (1개)
  accountAgeDays: number          // 4: 계정 생성 경과일 (정규화 0-10)

  // 기부 이력 (10개)
  totalPreviousDonations: number  // 5: 총 이전 기부액 (정규화)
  donationCount: number           // 6: 기부 횟수 (정규화)
  avgDonationAmount: number       // 7: 평균 기부액 (정규화)
  lastDonationDaysAgo: number     // 8: 마지막 기부 후 경과일 (정규화)
  donationsLast24h: number        // 9: 최근 24시간 기부 횟수 (정규화)
  donationsLast7d: number         // 10: 최근 7일 기부 횟수 (정규화)
  donationsLast30d: number        // 11: 최근 30일 기부 횟수 (정규화)
  uniqueCampaigns24h: number      // 12: 최근 24시간 고유 캠페인 수 (정규화)
  uniqueCampaigns7d: number       // 13: 최근 7일 고유 캠페인 수 (정규화)
  uniqueCampaigns30d: number      // 14: 최근 30일 고유 캠페인 수 (정규화)

  // 결제 수단 (2개)
  isCreditCard: number            // 15: 신용카드 여부 (0 or 1)
  isBankTransfer: number          // 16: 계좌이체 여부 (0 or 1)
}

// FDS 특징 카테고리별 그룹
export const FDS_FEATURE_GROUPS = {
  transaction: {
    label: '거래 정보',
    features: ['amountNormalized', 'hourOfDay', 'dayOfWeek', 'isWeekend']
  },
  account: {
    label: '계정 정보',
    features: ['accountAgeDays']
  },
  history: {
    label: '기부 이력',
    features: [
      'totalPreviousDonations', 'donationCount', 'avgDonationAmount',
      'lastDonationDaysAgo', 'donationsLast24h', 'donationsLast7d',
      'donationsLast30d', 'uniqueCampaigns24h', 'uniqueCampaigns7d',
      'uniqueCampaigns30d'
    ]
  },
  payment: {
    label: '결제 수단',
    features: ['isCreditCard', 'isBankTransfer']
  }
} as const

// FDS 특징 한국어 레이블
export const FDS_FEATURE_LABELS: Record<keyof FdsFeatures, string> = {
  amountNormalized: '기부 금액',
  hourOfDay: '시간대',
  dayOfWeek: '요일',
  isWeekend: '주말 여부',
  accountAgeDays: '계정 연령',
  totalPreviousDonations: '총 기부액',
  donationCount: '기부 횟수',
  avgDonationAmount: '평균 기부액',
  lastDonationDaysAgo: '마지막 기부 후 경과',
  donationsLast24h: '24시간 내 기부',
  donationsLast7d: '7일 내 기부',
  donationsLast30d: '30일 내 기부',
  uniqueCampaigns24h: '24시간 내 고유 캠페인',
  uniqueCampaigns7d: '7일 내 고유 캠페인',
  uniqueCampaigns30d: '30일 내 고유 캠페인',
  isCreditCard: '신용카드 사용',
  isBankTransfer: '계좌이체 사용'
}

// FDS 처리 액션 요청
export interface FdsOverrideRequest {
  action: 'approve' | 'block'
  reason: string
}

// 기부 금액 추이 데이터 포인트
export interface DonationTrendData {
  date: string              // 날짜 (YYYY-MM-DD)
  amount: number            // 해당일 총 기부액
  count: number             // 해당일 기부 건수
}

// 관리자 기부 금액 추이 응답
export interface AdminDonationTrendResponse {
  period: '7d' | '30d' | '3m' | 'all'
  data: DonationTrendData[]
  totalAmount: number
  totalCount: number
  averageAmount: number
}