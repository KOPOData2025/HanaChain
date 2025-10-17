/**
 * 인증 관련 API 함수들
 * 백엔드 서버와 연동하는 실제 API 호출 함수들
 */

import { apiClient, ApiError } from './client'
import { 
  ApiResponse,
  LoginRequestBody,
  LoginResponseData,
  SignupRequestBody,
  SignupResponseData,
  DuplicationCheckResponseData,
  SendVerificationResponseData,
  VerifyCodeResponseData,
  UserProfile,
  API_ENDPOINTS
} from '@/types/api'

// 커스텀 응답 타입 (백엔드 응답을 프론트엔드 형식으로 변환)
export interface LoginResponse {
  success: boolean
  data?: {
    token: string
    user: {
      id: string
      email: string
      nickname?: string
    }
  }
  message?: string
}

export interface SignupResponse {
  success: boolean
  data?: {
    userId: string
    message?: string
  }
  message?: string
}

export interface EmailCheckRequest {
  email: string
}

export interface EmailCheckResponse {
  available: boolean
  message?: string
}

export interface NicknameCheckRequest {
  nickname: string
}

export interface NicknameCheckResponse {
  available: boolean
  message?: string
}

export interface VerificationCodeRequest {
  email: string
  type: 'EMAIL_REGISTRATION' | 'PASSWORD_RESET' | 'EMAIL_CHANGE'
}

export interface VerificationCodeResponse {
  success: boolean
  message?: string
}

export interface VerifyCodeRequest {
  email: string
  code: string
  type: 'EMAIL_REGISTRATION' | 'PASSWORD_RESET' | 'EMAIL_CHANGE'
}

// 단계별 회원가입 인터페이스들
export interface TermsRequest {
  termsAccepted: boolean
  privacyAccepted: boolean
  marketingAccepted: boolean
}

export interface TermsResponse {
  sessionId: string
}

export interface AccountRequest {
  sessionId: string
  email: string
  password: string
}

export interface VerifyEmailRequest {
  sessionId: string
  email: string
}

export interface CompleteSignupRequest {
  sessionId: string
  nickname: string
  phoneNumber?: string
}

export interface CompleteSignupResponse {
  userId: number
  success: boolean
}

export interface VerifyCodeResponse {
  success: boolean
  message?: string
}

/**
 * 인증 API 클래스
 */
export class AuthApi {
  /**
   * 로그인
   */
  static async login(credentials: LoginRequestBody): Promise<LoginResponse> {
    try {
      const response = await apiClient.post<ApiResponse<LoginResponseData>>(API_ENDPOINTS.LOGIN, credentials)

      // 성공 응답 타입 가드
      if (response.success) {
        const successResponse = response as { success: true; data: LoginResponseData; message?: string }

        // 디버그 로그 추가
        console.log('🔍 Login API Response:', {
          response: successResponse,
          hasData: !!successResponse.data,
          hasAccessToken: !!successResponse.data.accessToken,
          tokenLength: successResponse.data.accessToken.length || 0,
          user: successResponse.data.user
        })

        // 성공 시 토큰을 클라이언트에 저장
        if (successResponse.data.accessToken) {
          console.log('✅ 토큰 저장 중:', successResponse.data.accessToken)
          apiClient.setAuthToken(successResponse.data.accessToken)
          console.log('✅ localStorage 확인:', localStorage.getItem('authToken'))
        }

        return {
          success: true,
          data: {
            token: successResponse.data.accessToken,
            user: successResponse.data.user
          },
          message: successResponse.message
        }
      }

      return {
        success: false,
        message: response.message || '로그인에 실패했습니다'
      }
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          success: false,
          message: error.message || '로그인에 실패했습니다'
        }
      }
      throw error
    }
  }

  /**
   * 로그아웃
   */
  static async logout(): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.LOGOUT)
    } catch (error) {
      // 로그아웃 실패해도 로컬 토큰은 제거
      console.warn('로그아웃 API 호출 실패:', error)
    } finally {
      apiClient.removeAuthToken()
    }
  }

  /**
   * 사용자 정보 조회 (토큰 기반)
   */
  static async getProfile(): Promise<{ id: string; email: string; nickname?: string; name?: string; phoneNumber?: string } | null> {
    try {
      const response = await apiClient.get<{
        success: boolean
        data: { id: string; email: string; nickname?: string; name?: string; phoneNumber?: string }
      }>(API_ENDPOINTS.PROFILE)

      return response.success ? response.data : null
    } catch (error) {
      console.warn('프로필 조회 실패:', error)
      return null
    }
  }

  /**
   * 이메일 중복 검사
   */
  static async checkEmailAvailability(email: string): Promise<EmailCheckResponse> {
    try {
      const response = await apiClient.post<EmailCheckResponse>(API_ENDPOINTS.CHECK_EMAIL, { email })
      return response
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          available: false,
          message: error.message || '이메일 확인 중 오류가 발생했습니다'
        }
      }
      throw error
    }
  }

  /**
   * 닉네임 중복 검사
   */
  static async checkNicknameAvailability(nickname: string): Promise<NicknameCheckResponse> {
    try {
      const response = await apiClient.post<NicknameCheckResponse>(API_ENDPOINTS.CHECK_NICKNAME, { nickname })
      return response
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          available: false,
          message: error.message || '닉네임 확인 중 오류가 발생했습니다'
        }
      }
      throw error
    }
  }

  /**
   * 이메일 인증코드 발송
   */
  static async sendVerificationCode(email: string, type: 'EMAIL_REGISTRATION' | 'PASSWORD_RESET' | 'EMAIL_CHANGE' = 'EMAIL_REGISTRATION'): Promise<VerificationCodeResponse> {
    try {
      const response = await apiClient.post<VerificationCodeResponse>(API_ENDPOINTS.SEND_VERIFICATION, { email, type })
      return response
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          success: false,
          message: error.message || '인증코드 발송에 실패했습니다'
        }
      }
      throw error
    }
  }

  /**
   * 이메일 인증코드 확인
   */
  static async verifyCode(email: string, code: string): Promise<VerifyCodeResponse> {
    try {
      const response = await apiClient.post<VerifyCodeResponse>(API_ENDPOINTS.VERIFY_CODE, { 
        email, 
        code, 
        type: 'EMAIL_REGISTRATION' 
      })
      return response
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          success: false,
          message: error.message || '인증코드 확인에 실패했습니다'
        }
      }
      throw error
    }
  }

  /**
   * 회원가입 완료
   */
  static async signup(signupData: { email: string; password: string; nickname: string }): Promise<SignupResponse> {
    try {
      const response = await apiClient.post<SignupResponse>(API_ENDPOINTS.SIGNUP, signupData)
      return response
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          success: false,
          message: error.message || '회원가입에 실패했습니다'
        }
      }
      throw error
    }
  }

  // validate 결과 캐시 (5분간 유지)
  private static validateCache = {
    result: null as boolean | null,
    timestamp: 0,
    CACHE_DURATION: 5 * 60 * 1000 // 5분
  }

  /**
   * 토큰 유효성 검증 (재시도 및 캐싱 포함)
   */
  static async validateToken(): Promise<boolean> {
    try {
      console.log('🔍 토큰 유효성 검증 시작')
      
      // 브라우저 환경이 아니면 false 반환
      if (typeof window === 'undefined') {
        console.log('❌ 서버사이드 렌더링 환경에서는 토큰 검증 불가')
        return false
      }

      // 토큰이 없으면 false 반환
      const token = localStorage.getItem('authToken')
      console.log('🔍 localStorage에서 토큰 조회:', {
        hasToken: !!token,
        tokenLength: token?.length || 0
      })
      
      if (!token || token === 'YOUR_JWT_TOKEN' || token === 'null' || token === 'undefined') {
        console.log('❌ 유효한 토큰이 없습니다')
        return false
      }

      // 캐시된 결과 확인 (성공한 경우만 캐시 사용)
      const now = Date.now()
      if (this.validateCache.result === true && 
          (now - this.validateCache.timestamp) < this.validateCache.CACHE_DURATION) {
        console.log('✅ 캐시된 검증 결과 사용 (유효)')
        return true
      }

      // JWT 토큰 형식 검증
      const tokenParts = token.split('.')
      if (tokenParts.length !== 3) {
        console.log('❌ JWT 토큰 형식이 올바르지 않습니다')
        return false
      }

      // JWT 페이로드 디코드해서 만료 시간 확인
      try {
        const payload = JSON.parse(atob(tokenParts[1]))
        const currentTime = Date.now() / 1000
        
        console.log('🔍 JWT 페이로드 분석:', {
          expiresAt: payload.exp ? new Date(payload.exp * 1000).toISOString() : 'Unknown',
          isExpired: payload.exp ? payload.exp < currentTime : false,
          timeUntilExpiry: payload.exp ? Math.round(payload.exp - currentTime) + 's' : 'Unknown'
        })
        
        // 로컬에서 만료 확인 (서버 호출 전 사전 검증)
        if (payload.exp && payload.exp < currentTime) {
          console.log('❌ 토큰이 로컬에서 만료 확인됨')
          this.validateCache.result = false
          this.validateCache.timestamp = now
          return false
        }
      } catch (decodeError) {
        console.warn('⚠️ JWT 페이로드 디코드 실패:', decodeError)
        // 디코드 실패해도 서버 검증은 진행
      }

      // 백엔드 토큰 유효성 검증 (재시도 포함)
      const result = await this.validateTokenWithRetry(token)
      
      // 결과 캐싱
      this.validateCache.result = result
      this.validateCache.timestamp = now
      
      return result
      
    } catch (error) {
      console.warn('❌ 토큰 유효성 검증 중 전체 오류:', {
        error: error instanceof Error ? error.message : '알 수 없는 오류',
        errorType: typeof error
      })
      return false
    }
  }

  /**
   * 재시도 로직이 포함된 토큰 검증
   */
  private static async validateTokenWithRetry(token: string, maxRetries: number = 3): Promise<boolean> {
    let lastError: any = null
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        console.log(`🔍 백엔드 토큰 검증 시도 ${attempt}/${maxRetries}`)
        
        const response = await apiClient.get<{ success: boolean }>(API_ENDPOINTS.VALIDATE)
        
        console.log('✅ 토큰 유효성 검증 성공:', {
          attempt,
          success: response.success
        })
        
        return response.success || false
        
      } catch (apiError) {
        lastError = apiError
        console.warn(`⚠️ 토큰 검증 실패 (시도 ${attempt}/${maxRetries}):`, {
          error: apiError instanceof Error ? apiError.message : '알 수 없는 오류',
          willRetry: attempt < maxRetries
        })
        
        // 마지막 시도가 아니면 재시도 전 대기
        if (attempt < maxRetries) {
          const delay = Math.min(1000 * Math.pow(2, attempt - 1), 5000) // 지수 백오프 (최대 5초)
          console.log(`⏳ ${delay}ms 후 재시도`)
          await new Promise(resolve => setTimeout(resolve, delay))
        }
      }
    }
    
    // 모든 재시도 실패 시
    console.error('❌ 모든 토큰 검증 시도 실패:', {
      maxRetries,
      lastError: lastError instanceof Error ? lastError.message : '알 수 없는 오류'
    })
    
    // 네트워크 오류인 경우 로컬 토큰 형식만으로 판단 (임시 허용)
    if (this.isNetworkError(lastError)) {
      console.warn('🌐 네트워크 오류로 인한 검증 실패 - 로컬 토큰 형식으로 임시 허용')
      return this.validateTokenLocally(token)
    }
    
    return false
  }

  /**
   * 네트워크 오류 여부 확인
   */
  private static isNetworkError(error: any): boolean {
    if (!error) return false
    
    const errorMessage = error.message?.toLowerCase() || ''
    const isNetworkError = errorMessage.includes('network') || 
                          errorMessage.includes('fetch') ||
                          errorMessage.includes('connection') ||
                          error.name === 'TypeError' ||
                          error.status === 0
    
    console.log('🔍 네트워크 오류 확인:', { 
      isNetworkError, 
      errorMessage, 
      errorName: error.name,
      errorStatus: error.status 
    })
    
    return isNetworkError
  }

  /**
   * 로컬에서만 토큰 유효성 검증 (네트워크 오류 시 임시 사용)
   */
  private static validateTokenLocally(token: string): boolean {
    try {
      const tokenParts = token.split('.')
      if (tokenParts.length !== 3) return false
      
      const payload = JSON.parse(atob(tokenParts[1]))
      const now = Date.now() / 1000
      
      // 만료 시간만 체크
      const isValid = !payload.exp || payload.exp > now
      
      console.log('🏠 로컬 토큰 검증 결과:', {
        isValid,
        expiresAt: payload.exp ? new Date(payload.exp * 1000).toISOString() : 'Unknown'
      })
      
      return isValid
      
    } catch (error) {
      console.warn('❌ 로컬 토큰 검증 실패:', error)
      return false
    }
  }

  /**
   * 토큰 갱신
   */
  static async refreshToken(): Promise<LoginResponse> {
    try {
      const response = await apiClient.post<ApiResponse<LoginResponseData>>(API_ENDPOINTS.REFRESH)

      console.log('🔄 토큰 갱신 응답:', response)

      // 성공 응답 타입 가드
      if (response.success) {
        const successResponse = response as { success: true; data: LoginResponseData; message?: string }

        // 성공 시 새로운 토큰을 클라이언트에 저장
        if (successResponse.data.accessToken) {
          console.log('✅ 새로운 토큰 저장 중:', successResponse.data.accessToken)
          apiClient.setAuthToken(successResponse.data.accessToken)
        }

        return {
          success: true,
          data: {
            token: successResponse.data.accessToken,
            user: successResponse.data.user
          },
          message: successResponse.message
        }
      }

      return {
        success: false,
        message: response.message || '토큰 갱신에 실패했습니다'
      }
    } catch (error) {
      if (error instanceof ApiError) {
        return {
          success: false,
          message: error.message || '토큰 갱신에 실패했습니다'
        }
      }
      throw error
    }
  }

  // 단계별 회원가입 API 메서드들
  
  /**
   * 약관 동의 단계
   */
  static async acceptTerms(termsData: TermsRequest): Promise<TermsResponse> {
    try {
      const response = await apiClient.post<{ success: boolean; data: TermsResponse }>('/auth/signup/terms', termsData)
      if (response.success && response.data) {
        return response.data
      }
      throw new Error("약관 동의 처리 실패")
    } catch (error) {
      if (error instanceof ApiError) {
        throw new Error(error.message || '약관 동의 처리에 실패했습니다')
      }
      throw error
    }
  }

  /**
   * 계정 정보 저장 단계
   */
  static async saveAccount(accountData: AccountRequest): Promise<void> {
    try {
      const response = await apiClient.post<{ success: boolean }>('/auth/signup/account', accountData)
      if (!response.success) {
        throw new Error("계정 정보 저장 실패")
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw new Error(error.message || '계정 정보 저장에 실패했습니다')
      }
      throw error
    }
  }

  /**
   * 이메일 인증 완료 표시
   */
  static async markEmailVerified(verifyData: VerifyEmailRequest): Promise<void> {
    try {
      const response = await apiClient.post<{ success: boolean }>('/auth/signup/verify-email', verifyData)
      if (!response.success) {
        throw new Error("이메일 인증 처리 실패")
      }
    } catch (error) {
      if (error instanceof ApiError) {
        throw new Error(error.message || '이메일 인증 처리에 실패했습니다')
      }
      throw error
    }
  }

  /**
   * 회원가입 완료
   */
  static async completeSignup(completeData: CompleteSignupRequest): Promise<CompleteSignupResponse> {
    try {
      const response = await apiClient.post<{ success: boolean; data: CompleteSignupResponse }>('/auth/signup/complete', completeData)
      if (response.success && response.data) {
        return response.data
      }
      throw new Error("회원가입 완료 실패")
    } catch (error) {
      if (error instanceof ApiError) {
        throw new Error(error.message || '회원가입 완료에 실패했습니다')
      }
      throw error
    }
  }
}