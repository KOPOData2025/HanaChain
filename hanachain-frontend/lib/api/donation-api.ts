import { SpringPageResponse, DonationRecord, DonationCertificate } from '@/types/donation'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

// 백엔드 API 응답 타입 (DonationHistoryResponse 구조에 맞춤)
export interface DonationApiResponse {
  id: string
  campaignId: string
  campaignTitle: string
  campaignImage?: string
  amount: number
  status: 'completed' | 'pending' | 'failed' | 'cancelled'
  donatedAt: string
  message?: string
  paymentMethod?: 'card' | 'bank' | 'naverpay' | 'kakaopay' | 'paypal' | 'other'
  receiptNumber?: string
  donationTransactionHash?: string
}

export interface DonationStatsResponse {
  totalAmount: number
  totalCount: number
  completedCount: number
  pendingCount: number
  failedCount: number
  averageAmount?: number
  uniqueDonorCount?: number
}

// 내 기부 내역 조회
export async function getMyDonations(params?: {
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}): Promise<SpringPageResponse<DonationApiResponse>> {
  const token = localStorage.getItem('authToken')
  if (!token) {
    throw new Error('로그인이 필요합니다.')
  }

  const queryParams = new URLSearchParams({
    page: String(params?.page || 0),
    size: String(params?.size || 10),
    sort: params?.sort || 'createdAt',
    direction: params?.direction || 'desc'
  })

  const response = await fetch(`${API_BASE_URL}/mypage/donations?${queryParams}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.')
    }
    throw new Error('기부 내역을 불러오는데 실패했습니다.')
  }

  const result = await response.json()
  console.log('🔍 Raw API Response:', result)
  console.log('🔍 Response keys:', Object.keys(result))
  console.log('🔍 Result.data exists?', !!result.data)
  
  if (result.data) {
    console.log('🔍 Result.data keys:', Object.keys(result.data))
    console.log('🔍 Result.data.content exists?', !!result.data.content)
    console.log('🔍 Result.data.content type:', typeof result.data.content)
    console.log('🔍 Result.data.content is array?', Array.isArray(result.data.content))
    
    if (result.data.content && Array.isArray(result.data.content)) {
      console.log('🔍 Content length:', result.data.content.length)
      if (result.data.content.length > 0) {
        console.log('🔍 First item keys:', Object.keys(result.data.content[0] || {}))
        console.log('🔍 First item id:', result.data.content[0]?.id)
        console.log('🔍 First item:', result.data.content[0])
      }
    }
  }
  
  return result.data // ApiResponse<Page<DonationResponseDto>>의 data 부분
}

// 내 기부 통계 조회
export async function getMyDonationStats(): Promise<DonationStatsResponse> {
  const token = localStorage.getItem('authToken')
  if (!token) {
    throw new Error('로그인이 필요합니다.')
  }

  const response = await fetch(`${API_BASE_URL}/mypage/donations/statistics`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.')
    }
    throw new Error('기부 통계를 불러오는데 실패했습니다.')
  }

  const result = await response.json()
  return result.data
}

// 특정 기부 상세 조회
export async function getDonationById(donationId: string): Promise<DonationApiResponse> {
  const token = localStorage.getItem('authToken')
  if (!token) {
    throw new Error('로그인이 필요합니다.')
  }

  console.log('🔍 기부 상세 조회 시작:', { donationId, token: token ? '있음' : '없음' })

  const response = await fetch(`${API_BASE_URL}/mypage/donations/${donationId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  console.log('🔍 기부 상세 조회 응답:', { 
    status: response.status, 
    statusText: response.statusText,
    ok: response.ok 
  })

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('기부 내역을 찾을 수 없습니다.')
    }
    if (response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.')
    }
    if (response.status === 403) {
      throw new Error('접근 권한이 없습니다.')
    }
    
    const errorText = await response.text()
    console.error('❌ 기부 상세 조회 실패:', { status: response.status, errorText })
    throw new Error('기부 내역을 불러오는데 실패했습니다.')
  }

  const result = await response.json()
  console.log('✅ 기부 상세 조회 성공:', result)
  return result.data
}

// 결제 ID로 기부 조회
export async function getDonationByPaymentId(paymentId: string): Promise<DonationApiResponse> {
  const token = localStorage.getItem('authToken')
  
  console.log('🔍 결제 ID로 기부 조회 시작:', { paymentId, token: token ? '있음' : '없음' })

  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}/donations/payment/${paymentId}`, {
    headers
  })

  console.log('🔍 결제 ID로 기부 조회 응답:', { 
    status: response.status, 
    statusText: response.statusText,
    ok: response.ok 
  })

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('해당 결제 ID에 대한 기부 내역을 찾을 수 없습니다.')
    }
    
    const errorText = await response.text()
    console.error('❌ 결제 ID로 기부 조회 실패:', { status: response.status, errorText })
    throw new Error('기부 내역을 불러오는데 실패했습니다.')
  }

  const result = await response.json()
  console.log('✅ 결제 ID로 기부 조회 성공:', result)
  return result.data
}

// 캠페인별 기부 내역 조회
export async function getCampaignDonations(
  campaignId: number,
  params?: {
    page?: number
    size?: number
    sort?: string
    direction?: 'asc' | 'desc'
  }
): Promise<SpringPageResponse<DonationApiResponse>> {
  const queryParams = new URLSearchParams({
    page: String(params?.page || 0),
    size: String(params?.size || 10),
    sort: params?.sort || 'paidAt',
    direction: params?.direction || 'desc'
  })

  const response = await fetch(`${API_BASE_URL}/donations/campaigns/${campaignId}?${queryParams}`, {
    headers: {
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    throw new Error('캠페인 기부 내역을 불러오는데 실패했습니다.')
  }

  const result = await response.json()
  return result.data
}

// 캠페인별 기부 통계 조회
export async function getCampaignDonationStats(campaignId: number): Promise<DonationStatsResponse> {
  const response = await fetch(`${API_BASE_URL}/donations/campaigns/${campaignId}/stats`, {
    headers: {
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    throw new Error('캠페인 기부 통계를 불러오는데 실패했습니다.')
  }

  const result = await response.json()
  return result.data
}

// 백엔드 응답을 프론트엔드 타입으로 변환하는 함수
export function transformDonationResponse(apiResponse: DonationApiResponse): DonationRecord {
  return {
    id: apiResponse.id,
    campaignId: apiResponse.campaignId,
    campaignTitle: apiResponse.campaignTitle,
    campaignImage: apiResponse.campaignImage,
    amount: apiResponse.amount,
    status: apiResponse.status,
    donatedAt: new Date(apiResponse.donatedAt),
    message: apiResponse.message,
    paymentMethod: apiResponse.paymentMethod,
    receiptNumber: apiResponse.receiptNumber,
    donationTransactionHash: apiResponse.donationTransactionHash
  }
}

// 기부 취소 (결제 전)
export async function cancelDonation(donationId: string, reason?: string): Promise<DonationApiResponse> {
  const token = localStorage.getItem('authToken')
  if (!token) {
    throw new Error('로그인이 필요합니다.')
  }

  const params = reason ? `?reason=${encodeURIComponent(reason)}` : ''

  const response = await fetch(`${API_BASE_URL}/donations/${donationId}/cancel${params}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  if (!response.ok) {
    throw new Error('기부 취소에 실패했습니다.')
  }

  const result = await response.json()
  return result.data
}

// 백엔드 API 응답 타입 (기부 증서)
export interface DonationCertificateApiResponse {
  donorName: string
  amount: number
  donatedAt: string
  campaignTitle: string
  campaignOrganization: string
  donationTransactionHash: string
  donationId: string
  campaignImage?: string
}

// 기부 증서 조회
export async function getDonationCertificate(donationId: string): Promise<DonationCertificate> {
  const token = localStorage.getItem('authToken')
  if (!token) {
    throw new Error('로그인이 필요합니다.')
  }

  console.log('🎫 기부 증서 조회 시작:', { donationId, token: token ? '있음' : '없음' })

  const response = await fetch(`${API_BASE_URL}/mypage/donations/${donationId}/certificate`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  })

  console.log('🎫 기부 증서 조회 응답:', {
    status: response.status,
    statusText: response.statusText,
    ok: response.ok
  })

  if (!response.ok) {
    const errorText = await response.text()
    console.error('❌ 기부 증서 조회 실패:', { status: response.status, errorText })

    if (response.status === 404) {
      throw new Error('기부 내역을 찾을 수 없습니다.')
    }
    if (response.status === 401) {
      throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.')
    }
    if (response.status === 403) {
      throw new Error('접근 권한이 없습니다.')
    }

    // API 응답 에러 메시지 파싱
    try {
      const errorJson = JSON.parse(errorText)
      throw new Error(errorJson.message || '기부 증서를 불러오는데 실패했습니다.')
    } catch (e) {
      throw new Error('기부 증서를 불러오는데 실패했습니다.')
    }
  }

  const result = await response.json()
  console.log('✅ 기부 증서 조회 성공:', result)

  // API 응답을 프론트엔드 타입으로 변환
  const apiData: DonationCertificateApiResponse = result.data

  return {
    donorName: apiData.donorName,
    amount: apiData.amount,
    donatedAt: new Date(apiData.donatedAt),
    campaignTitle: apiData.campaignTitle,
    campaignOrganization: apiData.campaignOrganization,
    donationTransactionHash: apiData.donationTransactionHash,
    donationId: apiData.donationId,
    campaignImage: apiData.campaignImage
  }
}