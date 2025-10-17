export interface DonationRecord {
  id: string
  campaignId: string
  campaignTitle: string
  campaignImage?: string
  amount: number
  status: 'completed' | 'pending' | 'failed' | 'cancelled'
  donatedAt: Date
  message?: string
  paymentMethod?: 'card' | 'bank' | 'naverpay' | 'kakaopay' | 'paypal' | 'other'
  receiptNumber?: string
  donationTransactionHash?: string
}

// 백엔드 API 응답과 매칭되는 기부 응답 타입
export interface DonationResponseDto {
  id: number
  amount: number
  message?: string
  paymentId?: string
  paymentStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REFUNDED'
  paymentMethod?: 'CREDIT_CARD' | 'BANK_TRANSFER' | 'NAVER_PAY' | 'KAKAO_PAY'
  anonymous: boolean
  donorName?: string
  campaignId: number
  campaignTitle: string
  creatorName?: string
  createdAt: string
  paidAt?: string
  cancelledAt?: string
  failureReason?: string
}

export interface Campaign {
  id: string
  title: string
  description: string
  targetAmount: number
  currentAmount: number
  category: string
  status: 'active' | 'completed' | 'suspended'
  organizationName: string
  mainImage?: string
  endDate?: Date
}

// 백엔드 API와 매칭되는 캠페인 타입
export interface CampaignListItem {
  id: number
  title: string
  description: string
  targetAmount: number
  currentAmount: number
  donorCount: number
  imageUrl?: string
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
  category: 'MEDICAL' | 'EDUCATION' | 'DISASTER_RELIEF' | 'ENVIRONMENT' | 'ANIMAL_WELFARE' | 'COMMUNITY' | 'EMERGENCY' | 'OTHER'
  startDate: string
  endDate: string
  createdAt: string
  creatorName: string
  organizer?: string // 단체명 필드 추가
  progressPercentage: number
  isActive: boolean
  // 배치 작업 관련 필드
  batchJobExecutionId?: number
  batchJobStatus?: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'STOPPED'
}

// 블록체인 상태 enum
export type BlockchainStatus = 
  | 'NONE'
  | 'BLOCKCHAIN_PENDING'
  | 'BLOCKCHAIN_PROCESSING'
  | 'ACTIVE'
  | 'BLOCKCHAIN_FAILED'

export interface CampaignDetailItem extends CampaignListItem {
  user: {
    id: number
    name: string
    email: string
  }
  updatedAt: string
  deletedAt?: string
  // HTML 형태의 상세 설명 (article 태그 포함)
  htmlDescription?: string

  // 블록체인 관련 필드
  blockchainStatus?: BlockchainStatus
  blockchainCampaignId?: string
  blockchainTransactionHash?: string
  beneficiaryAddress?: string
  blockchainErrorMessage?: string
  blockchainProcessedAt?: string

  // 배치 작업 관련 필드
  batchJobExecutionId?: number
  batchJobStatus?: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'STOPPED'
}

export interface DonationStats {
  totalAmount: number
  totalCount: number
  completedCount: number
  pendingCount: number
  failedCount: number
}

export interface DonationListParams {
  page?: number
  limit?: number
  status?: DonationRecord['status'] | 'all'
  sortBy?: 'date' | 'amount'
  sortOrder?: 'asc' | 'desc'
  search?: string
}

export interface DonationListResponse {
  donations: DonationRecord[]
  total: number
  page: number
  limit: number
  hasNext: boolean
}

// 캠페인 필터링 및 페이지네이션 타입
export interface CampaignFilters {
  category?: CampaignListItem['category']
  status?: CampaignListItem['status']
  keyword?: string
  sort?: 'recent' | 'popular' | 'progress'
}

export interface CampaignListParams extends CampaignFilters {
  page?: number
  size?: number
}

export interface SpringPageResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
  totalPages: number
  totalElements: number
  last: boolean
  first: boolean
  numberOfElements: number
  size: number
  number: number
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
  empty: boolean
}

// 기부 증서 타입
export interface DonationCertificate {
  donorName: string                   // 기부자명 (익명 처리 포함)
  amount: number                      // 기부 금액
  donatedAt: Date                     // 기부 날짜
  campaignTitle: string               // 캠페인 제목
  campaignOrganization: string        // 캠페인 주최 단체명
  donationTransactionHash: string     // 블록체인 트랜잭션 해시 (전체)
  donationId: string                  // 기부 ID
  campaignImage?: string              // 캠페인 대표 이미지 URL
}

// 캠페인 담당자용 모금 통계 타입
export interface CampaignFundraisingStats {
  // 기본 모금 정보
  currentAmount: number               // 현재 모금액
  targetAmount: number                // 목표 금액
  progressPercentage: number          // 달성률 (%)
  donorCount: number                  // 총 기부자 수

  // 기간 정보
  daysLeft: number                    // 남은 일수
  startDate: string                   // 시작일
  endDate: string                     // 종료일

  // 통계 정보
  averageDonationAmount: number       // 평균 기부 금액
  dailyDonationTrend: DailyDonationTrend[]  // 일별 기부 추이 (최근 7일)
  topDonations: TopDonation[]         // 상위 기부 목록 (Top 5)
}

// 일별 기부 추이
export interface DailyDonationTrend {
  date: string                        // 날짜 (YYYY-MM-DD)
  amount: number                      // 해당일 총 기부액
  count: number                       // 해당일 기부 건수
}

// 상위 기부 정보
export interface TopDonation {
  donorName: string                   // 기부자명 (익명 처리 포함)
  amount: number                      // 기부 금액
  donatedAt: string                   // 기부 날짜
  anonymous: boolean                  // 익명 여부
}

// 블록체인 트랜잭션 타입
export interface BlockchainTransaction {
  transactionHash: string             // 트랜잭션 해시
  blockNumber: string                 // 블록 번호
  timestamp: string                   // 타임스탬프 (ISO 형식)
  from: string                        // 발신자 주소
  to: string                          // 수신자 주소
  value: string                       // 전송 금액 (USDC)
  eventType: 'DonationMade' | 'CampaignCreated' | 'CampaignFinalized' | 'CampaignCancelled'  // 이벤트 타입
  donorName?: string                  // 기부자명 (DonationMade인 경우)
  anonymous?: boolean                 // 익명 여부
}

// 블록체인 트랜잭션 목록 응답
export interface BlockchainTransactionListResponse {
  transactions: BlockchainTransaction[]
  totalCount: number
  lastUpdated: string                 // 마지막 업데이트 시간
}

// 기부 완료 페이지 데이터
export interface DonationCompleteData {
  campaignTitle: string               // 캠페인 제목
  amount: number                      // 기부 금액
  campaignId: string                  // 캠페인 ID
  donorName: string                   // 기부자 이름
  paymentId: string                   // 결제 ID
}

// 결제 성공 데이터
export interface PaymentSuccessData {
  amount: number                      // 기부 금액
  paymentId: string                   // 결제 ID
  transactionId?: string              // 거래 ID (선택적)
  donorName: string                   // 기부자 이름
}