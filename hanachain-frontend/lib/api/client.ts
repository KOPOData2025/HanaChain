/**
 * HTTP API 클라이언트 설정
 * 백엔드 서버와의 통신을 위한 axios 기반 클라이언트
 */

import type { ApiResponse } from '@/types/api'

// Re-export ApiResponse for backward compatibility
export type { ApiResponse } from '@/types/api'

// 토큰 캐시 인터페이스
interface TokenCache {
  token: string | null
  lastUpdate: number
}

class ApiClient {
  private baseURL: string

  // 토큰 메모리 캐시 (localStorage 접근 최소화)
  private tokenCache: TokenCache = {
    token: null,
    lastUpdate: 0
  }
  private readonly CACHE_DURATION = 60000 // 1분 캐시 유효 기간

  // 인증이 필요하지 않은 공개 엔드포인트 목록
  private publicEndpoints = [
    '/campaigns',
    '/campaigns/public',
    '/campaigns/popular',
    '/campaigns/recent',
    '/campaigns/search',
    '/campaigns/category/',
    '/campaign-managers/campaigns/',
    '/organizations',
    '/organizations/search',
    // 인증 관련 엔드포인트 (기존 토큰 없이 호출되어야 함)
    '/auth/login',
    '/auth/register',  // API_ENDPOINTS.SIGNUP과 일치
    '/auth/refresh',
    '/auth/logout',
    '/auth/verification/verify',  // API_ENDPOINTS.VERIFY_CODE와 일치
    '/auth/verification/send',  // API_ENDPOINTS.SEND_VERIFICATION과 일치
    '/auth/check-email',
    '/auth/check-nickname',
  ]

  constructor() {
    // 환경변수에서 백엔드 URL을 가져오거나 기본값 사용
    this.baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
    
    // 디버깅을 위한 로그 (개발 환경에서만)
    if (process.env.NODE_ENV === 'development') {
      console.log('🌐 [ApiClient] 초기화:', {
        baseURL: this.baseURL,
        env_NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
        NODE_ENV: process.env.NODE_ENV
      })
    }
  }

  // 공개 API 엔드포인트인지 확인 (HTTP 메서드 고려)
  private isPublicEndpoint(endpoint: string, method?: string): boolean {
    // 모든 /auth/* 경로는 기본적으로 공개 엔드포인트로 처리
    // (로그인, 회원가입, 토큰 갱신 등은 기존 토큰 없이 호출되어야 함)
    if (endpoint.startsWith('/auth/')) {
      // 단, /auth/validate 와 /auth/profile 은 인증 필요
      const protectedAuthEndpoints = ['/auth/validate', '/auth/profile']
      return !protectedAuthEndpoints.some(protectedEndpoint => endpoint.startsWith(protectedEndpoint))
    }

    // /organizations 엔드포인트: GET만 공개, POST/PUT/DELETE는 인증 필요
    if (endpoint === '/organizations' || endpoint.startsWith('/organizations?')) {
      const httpMethod = (method || 'GET').toUpperCase()
      return httpMethod === 'GET'
    }

    return this.publicEndpoints.some(publicPath => {
      // 정확한 매치 또는 시작 매치 (예: /campaigns/category/로 시작하는 모든 경로)
      return endpoint === publicPath ||
             (publicPath.endsWith('/') && endpoint.startsWith(publicPath)) ||
             endpoint.startsWith(publicPath + '?') || // 쿼리 파라미터가 있는 경우
             (endpoint.startsWith('/campaigns/') && endpoint.match(/^\/campaigns\/\d+$/)) // 캠페인 상세 조회
    })
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`
    
    // 기본 헤더 설정
    const defaultHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    // 공개 API가 아닌 경우에만 토큰 포함 (HTTP 메서드 고려)
    const httpMethod = options.method || 'GET'
    const isPublic = this.isPublicEndpoint(endpoint, httpMethod)
    const token = this.getAuthToken()

    if (!isPublic && token) {
      defaultHeaders.Authorization = `Bearer ${token}`
    }

    const config: RequestInit = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers,
      },
    }

    // 인증 관련 엔드포인트가 아닌 경우에만 상세 로그 출력
    if (!endpoint.includes('/auth/')) {
      // Headers 객체를 일반 객체로 변환하여 로깅
      const headersForLog: Record<string, string> = {}
      if (config.headers) {
        if (config.headers instanceof Headers) {
          config.headers.forEach((value, key) => {
            headersForLog[key] = value
          })
        } else {
          Object.assign(headersForLog, config.headers)
        }
      }

      console.log('🔗 [HTTP 요청] 준비 완료:', {
        url,
        method: options.method || 'GET',
        isPublicEndpoint: isPublic,
        token: token ? '있음 (' + token.substring(0, 20) + '...)' : '❌ 없음',
        tokenIncluded: !isPublic && !!token,
        defaultHeadersAuth: defaultHeaders.Authorization || '❌ 없음',
        configHeadersType: config.headers?.constructor?.name || 'unknown',
        configHeadersAuth: headersForLog.Authorization || headersForLog.authorization || '❌ 없음',
        allConfigHeaders: headersForLog,
        hasBody: !!config.body,
        tokenDebug: this.getTokenDebugInfo()
      })
    }

    try {
      const response = await fetch(url, config)
      
      // 응답 데이터 파싱
      let responseData: any
      const contentType = response.headers.get('content-type')
      
      if (contentType && contentType.includes('application/json')) {
        responseData = await response.json()
      } else {
        responseData = await response.text()
      }

      if (!response.ok) {
        // 401 Unauthorized 응답 시 토큰 관련 처리
        if (response.status === 401) {
          const isAuthEndpoint = endpoint.includes('/auth/')
          
          if (isAuthEndpoint) {
            console.warn('🔐 인증 엔드포인트 실패:', {
              endpoint,
              status: response.status,
              message: responseData?.message || '인증이 필요합니다'
            })
          } else {
            // 인증이 필요한 일반 API에서 401 발생 시 단계적 처리
            console.warn('⚠️ 401 Unauthorized 발생 - 단계적 복구 시작:', {
              endpoint,
              status: response.status,
              message: responseData?.message || '토큰이 만료되었거나 유효하지 않습니다'
            })
            
            // 단계적 복구 처리
            await this.handle401Recovery(endpoint, options)
          }
        } else {
          console.error('❌ API 요청 실패:', {
            status: response.status,
            statusText: response.statusText,
            responseData: responseData || 'No response data',
            responseType: typeof responseData,
            url,
            method: options.method || 'GET',
            responseHeaders: Object.fromEntries(response.headers.entries()),
            requestHeaders: config.headers,
            hasToken: !!this.getAuthToken(),
            tokenInfo: this.getTokenDebugInfo()
          })
        }
        
        const error = new ApiError(
          responseData?.message || `HTTP error! status: ${response.status}`
        )
        error.status = response.status
        error.details = responseData
        throw error
      }

      return responseData
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      
      // 네트워크 에러 등
      const apiError = new ApiError(
        error instanceof Error ? error.message : '네트워크 오류가 발생했습니다'
      )
      apiError.status = 0
      apiError.details = error
      throw apiError
    }
  }

  // GET 요청
  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' })
  }

  // 공개 GET 요청 (명시적으로 인증 없이 호출)
  async getPublic<T>(endpoint: string): Promise<T> {
    const url = `${this.baseURL}${endpoint}`
    
    const config: RequestInit = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    }

    console.log('🌐 공개 API GET 요청:', {
      url,
      method: 'GET',
      headers: config.headers,
      authIncluded: false
    })

    try {
      const response = await fetch(url, config)
      
      let responseData: any
      const contentType = response.headers.get('content-type')
      
      if (contentType && contentType.includes('application/json')) {
        responseData = await response.json()
      } else {
        responseData = await response.text()
      }

      if (!response.ok) {
        const error = new ApiError(
          responseData?.message || `HTTP error! status: ${response.status}`
        )
        error.status = response.status
        error.details = responseData
        throw error
      }

      return responseData
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      
      const apiError = new ApiError(
        error instanceof Error ? error.message : '네트워크 오류가 발생했습니다'
      )
      apiError.status = 0
      apiError.details = error
      throw apiError
    }
  }

  // POST 요청
  async post<T>(endpoint: string, data?: any): Promise<T> {
    console.log('🌐 API POST 요청:', { endpoint, data })
    const result = await this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    })
    console.log('🌐 API POST 응답:', { endpoint, result })
    return result
  }

  // PUT 요청
  async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  // PATCH 요청
  async patch<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  // DELETE 요청
  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' })
  }

  // 인증 토큰 가져오기 (메모리 캐싱으로 localStorage 접근 최소화)
  private getAuthToken(): string | null {
    if (typeof window === 'undefined') {
      console.log('🔍 [getAuthToken] 서버 환경 - 토큰 없음')
      return null
    }

    const now = Date.now()

    // 캐시가 유효하면 즉시 반환 (localStorage 접근 불필요)
    // 단, 유효한 토큰(문자열)만 캐싱하고 null은 캐싱하지 않음
    if (this.tokenCache.token && (now - this.tokenCache.lastUpdate) < this.CACHE_DURATION) {
      console.log('🔍 [getAuthToken] 캐시에서 토큰 반환:', {
        tokenPreview: this.tokenCache.token.substring(0, 20) + '...',
        cacheAge: now - this.tokenCache.lastUpdate + 'ms'
      })
      return this.tokenCache.token
    }

    console.log('🔍 [getAuthToken] localStorage에서 토큰 읽기 시도')

    // 캐시가 무효화되었거나 없으면 localStorage에서 읽기
    const token = localStorage.getItem('authToken')

    console.log('🔍 [getAuthToken] localStorage 결과:', {
      hasToken: !!token,
      tokenValue: token ? token.substring(0, 30) + '...' : 'null',
      tokenLength: token?.length || 0,
      isInvalidValue: token === 'YOUR_JWT_TOKEN' || token === 'null' || token === 'undefined'
    })

    // 잘못된 토큰 값들 필터링
    if (!token || token === 'YOUR_JWT_TOKEN' || token === 'null' || token === 'undefined') {
      console.warn('⚠️ [getAuthToken] 무효한 토큰 - null 반환')
      // 무효한 토큰은 캐싱하지 않음 (매번 localStorage 확인)
      this.tokenCache = { token: null, lastUpdate: 0 }
      return null
    }

    console.log('✅ [getAuthToken] 유효한 토큰 발견 - 캐시에 저장')
    // 유효한 토큰만 캐시에 저장
    this.tokenCache = { token, lastUpdate: now }
    return token
  }

  // 토큰 디버그 정보 가져오기
  private getTokenDebugInfo(): object {
    if (typeof window === 'undefined') return { info: 'Server-side rendering' }
    
    const token = localStorage.getItem('authToken')
    if (!token || token === 'YOUR_JWT_TOKEN' || token === 'null' || token === 'undefined') {
      return { 
        status: 'No valid token',
        tokenValue: token,
        localStorage: {
          authToken: localStorage.getItem('authToken'),
          authUser: localStorage.getItem('authUser')
        }
      }
    }
    
    try {
      // JWT 토큰 디코드
      const parts = token.split('.')
      if (parts.length !== 3) {
        return { status: 'Invalid JWT format', tokenParts: parts.length }
      }
      
      const payload = JSON.parse(atob(parts[1]))
      const now = Date.now() / 1000
      
      return {
        status: 'Valid JWT',
        tokenLength: token.length,
        tokenPreview: token.substring(0, 20) + '...',
        payload: {
          exp: payload.exp,
          iat: payload.iat,
          sub: payload.sub,
          email: payload.email,
          roles: payload.roles || payload.authorities
        },
        expiresAt: payload.exp ? new Date(payload.exp * 1000).toISOString() : 'Unknown',
        isExpired: payload.exp ? payload.exp < now : 'Unknown',
        timeUntilExpiry: payload.exp ? Math.round(payload.exp - now) + 's' : 'Unknown'
      }
    } catch (error) {
      return {
        status: 'Token decode error',
        error: error instanceof Error ? error.message : 'Unknown error',
        tokenPreview: token.substring(0, 20) + '...'
      }
    }
  }

  // 인증 토큰 저장 (캐시 동기화)
  setAuthToken(token: string): void {
    console.log('💾 토큰 저장 시도:', { token: token.substring(0, 20) + '...', windowDefined: typeof window !== 'undefined' })
    if (typeof window !== 'undefined') {
      localStorage.setItem('authToken', token)
      // 캐시 즉시 갱신
      this.tokenCache = { token, lastUpdate: Date.now() }
      console.log('✅ 토큰 저장 및 캐시 갱신 완료')
    } else {
      console.log('❌ window 객체 없음 - 서버사이드 렌더링 중')
    }
  }

  // 인증 토큰 제거 (캐시 초기화)
  removeAuthToken(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('authToken')
      // 캐시 초기화
      this.tokenCache = { token: null, lastUpdate: 0 }
      console.log('✅ 토큰 제거 및 캐시 초기화 완료')
    }
  }

  /**
   * 401 에러 발생 시 단계적 복구 처리
   */
  private async handle401Recovery(endpoint: string, originalOptions: RequestInit): Promise<void> {
    try {
      console.log('🔄 401 복구 처리 시작')

      // 브라우저 환경이 아니면 즉시 종료
      if (typeof window === 'undefined') {
        console.log('❌ 서버 환경에서는 복구 불가')
        return
      }

      // 로그인 상태 확인
      const token = localStorage.getItem('authToken')
      const isLoggedIn = token && token !== 'YOUR_JWT_TOKEN' && token !== 'null' && token !== 'undefined'

      // 비로그인 상태에서는 조용히 종료 (redirect 없음)
      if (!isLoggedIn) {
        console.log('🔕 비로그인 상태 401 에러 - 복구 처리 중단')
        return
      }

      // 1단계: 토큰 갱신 시도 (로그인된 사용자만)
      console.log('🔄 1단계: 토큰 갱신 시도')
      const refreshSuccess = await this.attemptTokenRefresh()

      if (refreshSuccess) {
        console.log('✅ 토큰 갱신 성공 - 복구 완료')
        return
      }

      // 2단계: 사용자에게 알림 후 재로그인 유도
      console.log('🔄 2단계: 사용자 알림 및 재로그인 유도')
      this.notifyAuthExpired()

      // notifyAuthExpired()에서 사용자 선택에 따라 이미 처리되므로
      // 여기서는 추가 로그아웃 처리를 하지 않음

    } catch (error) {
      console.error('❌ 401 복구 처리 중 오류:', error)

      // 로그인된 상태에서만 강제 로그아웃
      const token = localStorage.getItem('authToken')
      const isLoggedIn = token && token !== 'YOUR_JWT_TOKEN' && token !== 'null' && token !== 'undefined'

      if (isLoggedIn) {
        await this.performFinalLogout()
      }
    }
  }

  /**
   * 토큰 갱신 시도
   */
  private async attemptTokenRefresh(): Promise<boolean> {
    try {
      // refresh 엔드포인트 호출 (AuthApi를 직접 import하면 순환 참조 위험)
      const refreshResponse = await fetch(`${this.baseURL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          refreshToken: localStorage.getItem('refreshToken') || ''
        })
      })

      if (refreshResponse.ok) {
        const data = await refreshResponse.json()
        if (data.success && data.data?.accessToken) {
          this.setAuthToken(data.data.accessToken)
          console.log('✅ 토큰 자동 갱신 성공')
          return true
        }
      }
      
      console.log('❌ 토큰 갱신 실패')
      return false
      
    } catch (error) {
      console.log('❌ 토큰 갱신 중 오류:', error)
      return false
    }
  }

  /**
   * 사용자에게 인증 만료 알림
   */
  private notifyAuthExpired(): void {
    // 로그인된 상태인지 확인 (localStorage에 유효한 토큰이 있는지 체크)
    const token = localStorage.getItem('authToken')
    const isLoggedIn = token && token !== 'YOUR_JWT_TOKEN' && token !== 'null' && token !== 'undefined'

    // 로그인되지 않은 상태에서는 알림 없이 조용히 처리
    if (!isLoggedIn) {
      console.log('🔕 비로그인 상태 401 에러 - 알림 없이 조용히 처리')
      return
    }

    // 로그인된 사용자의 토큰이 만료된 경우에만 알림 표시
    console.log('⚠️ 로그인된 사용자의 토큰 만료 - 사용자에게 알림')
    if (window.confirm('인증이 만료되었습니다. 다시 로그인하시겠습니까?')) {
      // 사용자가 확인 시 로그인 페이지로 이동
      window.location.href = '/login'
    } else {
      // 사용자가 취소 시에도 3초 후 자동 처리
      setTimeout(() => {
        this.performFinalLogout()
      }, 3000)
    }
  }

  /**
   * 최종 로그아웃 처리
   */
  private async performFinalLogout(): Promise<void> {
    try {
      // localStorage 정리
      localStorage.removeItem('authToken')
      localStorage.removeItem('authUser')
      localStorage.removeItem('refreshToken')
      
      console.log('🧹 인증 정보 정리 완료')
      
      // 현재 페이지가 로그인 페이지가 아니면 로그인 페이지로 이동
      if (!window.location.pathname.includes('/login')) {
        console.log('🔄 로그인 페이지로 이동')
        window.location.href = '/login'
      } else {
        // 이미 로그인 페이지라면 새로고침
        window.location.reload()
      }
      
    } catch (error) {
      console.error('❌ 최종 로그아웃 처리 중 오류:', error)
      // 오류 발생 시 강제 새로고침
      window.location.reload()
    }
  }
}

// API 에러 클래스 정의
export class ApiError extends Error {
  status?: number
  details?: any

  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

// 싱글톤 인스턴스 생성 및 내보내기
export const apiClient = new ApiClient()