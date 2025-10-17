import PortOne, { PaymentRequest } from "@portone/browser-sdk/v2"

/**
 * 모바일 디바이스 감지 함수
 */
function isMobileDevice(): boolean {
  if (typeof window === 'undefined') return false
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

export interface DonationPaymentData {
  amount: number
  campaignId: string
  donorName: string
  donorEmail: string
  donorPhone: string
  campaignTitle: string
}

export interface PaymentResult {
  success: boolean
  paymentId?: string
  transactionId?: string
  error?: string
}

// 포트원 설정 (실제 운영시에는 환경변수로 관리)
const PORTONE_CONFIG = {
  storeId: process.env.NEXT_PUBLIC_PORTONE_STORE_ID || "store-test", // 실제 storeId로 변경 필요
  channelKey: process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY || "channel-key-test", // 실제 channelKey로 변경 필요
}

/**
 * 포트원 V2 SDK를 사용한 기부 결제 처리
 */
export async function processDonationPayment(
  paymentData: DonationPaymentData
): Promise<PaymentResult> {
  try {
    // 먼저 백엔드에 기부 정보를 사전 등록
    const paymentId = await createDonation(paymentData)
    if (!paymentId) {
      return {
        success: false,
        error: "기부 정보 생성에 실패했습니다.",
      }
    }

    // 포트원 결제 요청 파라미터 구성
    const paymentRequest: PaymentRequest = {
      storeId: PORTONE_CONFIG.storeId,
      channelKey: PORTONE_CONFIG.channelKey,
      paymentId: paymentId,
      orderName: `[기부] ${paymentData.campaignTitle}`,
      totalAmount: paymentData.amount,
      currency: "CURRENCY_KRW",
      payMethod: "CARD", // 카드 결제로 고정 (추후 다양한 결제수단 지원 가능)
      customer: {
        fullName: paymentData.donorName,
        email: paymentData.donorEmail,
        phoneNumber: paymentData.donorPhone,
      },
      // 추가 메타데이터
      customData: {
        campaignId: paymentData.campaignId,
        donationType: "campaign_donation",
      },
      // 결제창 표시 설정
      windowType: {
        pc: "IFRAME", // PC에서는 iframe으로 표시
        mobile: "POPUP", // 모바일에서는 팝업으로 표시
      },
      // 모바일에서만 redirectUrl 설정 (PC에서는 프로미스로 결과 반환)
      ...(isMobileDevice() ? { redirectUrl: `${window.location.origin}/donation/complete` } : {}),
    }

    console.log("Starting PortOne payment with data:", paymentRequest)

    // 포트원 결제 요청 실행
    const response = await PortOne.requestPayment(paymentRequest)

    console.log("PortOne payment response:", response)

    // 응답이 없는 경우 처리
    if (!response) {
      return {
        success: false,
        error: "결제 응답을 받을 수 없습니다.",
      }
    }

    // 결제 결과 처리
    if (response.code !== undefined) {
      // 결제 실패 또는 취소
      return {
        success: false,
        error: response.message || "결제가 취소되었거나 실패했습니다.",
      }
    }

    // 결제 성공 응답 처리
    if (response.paymentId) {
      // 서버에서 결제 검증 필요 (실제 운영시 구현)
      const isValid = await verifyPayment(response.paymentId)
      
      if (isValid) {
        return {
          success: true,
          paymentId: response.paymentId,
          transactionId: response.txId,
        }
      } else {
        return {
          success: false,
          error: "결제 검증에 실패했습니다.",
        }
      }
    }

    return {
      success: false,
      error: "알 수 없는 오류가 발생했습니다.",
    }
  } catch (error) {
    console.error("PortOne payment error:", error)
    return {
      success: false,
      error: error instanceof Error ? error.message : "결제 처리 중 오류가 발생했습니다.",
    }
  }
}

/**
 * 백엔드에 기부 정보 사전 등록
 */
async function createDonation(paymentData: DonationPaymentData): Promise<string | null> {
  try {
    const paymentId = `donation_${paymentData.campaignId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    
    // 디버깅: 사용자 상태 및 토큰 존재 확인
    const authToken = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null
    const authUser = typeof window !== 'undefined' ? localStorage.getItem('authUser') : null
    console.log('🔍 기부 생성 요청 시작')
    console.log('🔍 Auth token exists:', !!authToken)
    console.log('🔍 Auth user exists:', !!authUser)
    console.log('🔍 Auth token preview:', authToken ? `${authToken.substring(0, 20)}...` : 'null')
    console.log('🔍 사용자 정보:', authUser ? JSON.parse(authUser).email : 'No user')
    console.log('🔍 PaymentData campaignId:', paymentData.campaignId, 'Type:', typeof paymentData.campaignId)
    console.log('🔍 ParseInt 결과:', parseInt(paymentData.campaignId))
    
    const requestBody = {
      campaignId: parseInt(paymentData.campaignId),
      amount: paymentData.amount,
      paymentId: paymentId,
      paymentMethod: 'CREDIT_CARD',
      anonymous: false,
      donorName: paymentData.donorName,
      donorEmail: paymentData.donorEmail,
      donorPhone: paymentData.donorPhone
    }
    
    const headers = {
      'Content-Type': 'application/json',
      // JWT 토큰이 있으면 추가 (익명 기부도 가능하도록 설정했으므로 선택사항)
      ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {})
    }
    
    console.log('🔍 Request URL:', `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/donations`)
    console.log('🔍 Request headers:', headers)
    console.log('🔍 Request body:', requestBody)
    
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/donations`, {
      method: 'POST',
      headers,
      body: JSON.stringify(requestBody)
    })

    console.log('🔍 Response status:', response.status)
    console.log('🔍 Response headers:', Object.fromEntries(response.headers.entries()))

    if (!response.ok) {
      const errorText = await response.text()
      console.error('❌ Failed to create donation:', response.status, errorText)
      console.error('❌ Response headers:', Object.fromEntries(response.headers.entries()))
      return null
    }

    const data = await response.json()
    console.log('✅ Donation created successfully:', data)
    return paymentId
  } catch (error) {
    console.error('❌ Error creating donation:', error)
    return null
  }
}

/**
 * 웹훅 시뮬레이션 - 개발 환경에서 PortOne 웹훅을 시뮬레이션
 */
async function simulateWebhook(paymentId: string, amount: number): Promise<boolean> {
  try {
    console.log('🔗 웹훅 시뮬레이션 시작:', { paymentId, amount })

    // PortOne 웹훅 형식에 맞는 데이터 생성
    const webhookData = {
      merchantUid: paymentId,
      impUid: `imp_${Date.now()}`, // PortOne 거래 고유 ID
      amount: amount,
      status: 'paid', // PortOne 결제 완료 상태
      buyerName: 'Test Buyer',
      buyerEmail: 'test@example.com',
      buyerTel: '010-1234-5678',
      paidAt: Math.floor(Date.now() / 1000), // Unix timestamp
      receiptUrl: `https://portone.io/receipts/${paymentId}`,
      failReason: null,
      cancelledAt: null,
      failedAt: null
    }

    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/webhooks/payment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // PortOne 웹훅 시그니처 헤더 (개발용)
        'X-ImpWebhook-Signature': 'dev-signature'
      },
      body: JSON.stringify(webhookData)
    })

    if (!response.ok) {
      console.error('❌ 웹훅 시뮬레이션 실패:', response.status)
      return false
    }

    const result = await response.json()
    console.log('✅ 웹훅 시뮬레이션 성공:', result)
    
    return result.status === 'success'
  } catch (error) {
    console.error('❌ 웹훅 시뮬레이션 오류:', error)
    return false
  }
}

/**
 * 서버에서 결제 즉시 승인 (웹훅 우회)
 * 웹훅을 기다리지 않고 즉시 승인 API를 호출하여 COMPLETED 상태로 변경
 */
async function verifyPayment(paymentId: string): Promise<boolean> {
  try {
    console.log('🔍 결제 즉시 승인 시작:', paymentId)

    // 즉시 승인 API 호출 (웹훅 우회)
    const approveResponse = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/donations/approve-payment-by-id`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        // JWT 토큰이 있으면 추가
        ...(typeof window !== 'undefined' && localStorage.getItem('authToken')
          ? { 'Authorization': `Bearer ${localStorage.getItem('authToken')}` }
          : {})
      },
      body: new URLSearchParams({
        paymentId: paymentId,
        impUid: '' // PortOne 거래 ID (현재는 선택적)
      })
    })

    if (!approveResponse.ok) {
      const errorText = await approveResponse.text()
      console.error(`❌ 결제 즉시 승인 실패: ${approveResponse.status}`, errorText)
      return false
    }

    const approveData = await approveResponse.json()
    console.log('🔍 결제 즉시 승인 결과:', approveData)

    // 승인 성공 여부 확인
    const isApproved = approveData.success === true && approveData.data?.paymentStatus === 'COMPLETED'

    if (isApproved) {
      console.log('✅ 결제 즉시 승인 완료 (웹훅 우회)')
    } else {
      console.log('❌ 결제 즉시 승인 실패')
    }

    return isApproved
  } catch (error) {
    console.error('❌ 결제 즉시 승인 오류:', error)
    return false
  }
}

/**
 * 환경 변수 검증
 */
export function validatePortOneConfig(): boolean {
  if (!PORTONE_CONFIG.storeId || PORTONE_CONFIG.storeId === "store-test") {
    console.warn("PortOne storeId is not configured properly")
    return false
  }
  
  if (!PORTONE_CONFIG.channelKey || PORTONE_CONFIG.channelKey === "channel-key-test") {
    console.warn("PortOne channelKey is not configured properly")
    return false
  }
  
  return true
}