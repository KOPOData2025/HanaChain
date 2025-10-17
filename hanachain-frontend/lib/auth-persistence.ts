/**
 * 인증 상태 지속성을 위한 유틸리티 함수들
 * 페이지 새로고침 시에도 로그인 상태를 유지하기 위한 헬퍼 함수들
 */

export interface AuthData {
  token: string
  user: {
    id: string
    email: string
    nickname?: string
  }
}

// JWT 디코딩 결과 캐시 인터페이스
interface TokenDecodeCache {
  token: string
  payload: any
  expiration: number | null
  isValid: boolean
  decodedAt: number
}

// 모듈 레벨 캐시 (동일 토큰에 대한 중복 디코딩 방지)
let cachedTokenData: TokenDecodeCache | null = null

/**
 * 인증 데이터를 localStorage에 저장
 */
export function saveAuthData(authData: AuthData): void {
  if (typeof window === 'undefined') return
  
  try {
    localStorage.setItem('authToken', authData.token)
    localStorage.setItem('authUser', JSON.stringify(authData.user))
    console.log('✅ 인증 데이터 저장 완료:', {
      hasToken: !!authData.token,
      userEmail: authData.user.email
    })
  } catch (error) {
    console.error('❌ 인증 데이터 저장 실패:', error)
  }
}

/**
 * localStorage에서 인증 데이터 복원
 */
export function loadAuthData(): AuthData | null {
  if (typeof window === 'undefined') return null
  
  try {
    const token = localStorage.getItem('authToken')
    const userStr = localStorage.getItem('authUser')
    
    // 유효하지 않은 토큰 필터링
    if (!token || token === 'YOUR_JWT_TOKEN' || token === 'null' || token === 'undefined') {
      console.log('❌ 유효하지 않은 토큰:', token)
      return null
    }
    
    if (!userStr) {
      console.log('❌ 저장된 사용자 정보 없음')
      return null
    }
    
    const user = JSON.parse(userStr)
    
    console.log('✅ 인증 데이터 복원 성공:', {
      hasToken: !!token,
      userEmail: user.email
    })
    
    return { token, user }
  } catch (error) {
    console.error('❌ 인증 데이터 복원 실패:', error)
    return null
  }
}

/**
 * 인증 데이터 삭제
 */
export function clearAuthData(): void {
  if (typeof window === 'undefined') return
  
  try {
    localStorage.removeItem('authToken')
    localStorage.removeItem('authUser')
    console.log('✅ 인증 데이터 삭제 완료')
  } catch (error) {
    console.error('❌ 인증 데이터 삭제 실패:', error)
  }
}

/**
 * JWT 토큰 형식 검증 (캐싱 지원)
 */
export function isValidJwtToken(token: string): boolean {
  if (!token || typeof token !== 'string') return false

  // 캐시된 검증 결과가 있으면 재사용
  if (cachedTokenData && cachedTokenData.token === token) {
    return cachedTokenData.isValid
  }

  // JWT는 3개의 부분으로 구성 (header.payload.signature)
  const parts = token.split('.')
  if (parts.length !== 3) {
    // 무효한 토큰을 캐시에 저장
    cachedTokenData = {
      token,
      payload: null,
      expiration: null,
      isValid: false,
      decodedAt: Date.now()
    }
    return false
  }

  // 각 부분이 base64로 인코딩되어 있는지 확인
  try {
    let payload: any = null
    parts.forEach((part, index) => {
      if (part.length === 0) throw new Error('Empty part')
      // Base64 URL-safe 디코딩 시도
      const decoded = atob(part.replace(/-/g, '+').replace(/_/g, '/'))
      // payload 부분(index 1) 파싱
      if (index === 1) {
        payload = JSON.parse(decoded)
      }
    })

    // 검증 성공 - 캐시에 저장
    cachedTokenData = {
      token,
      payload,
      expiration: payload?.exp ? payload.exp * 1000 : null,
      isValid: true,
      decodedAt: Date.now()
    }
    return true
  } catch (error) {
    // 검증 실패 - 캐시에 저장
    cachedTokenData = {
      token,
      payload: null,
      expiration: null,
      isValid: false,
      decodedAt: Date.now()
    }
    return false
  }
}

/**
 * 토큰 만료 시간 확인 (JWT payload에서 exp 추출, 캐싱 지원)
 */
export function getTokenExpiration(token: string): number | null {
  // 캐시된 데이터가 있고 동일 토큰이면 캐시된 만료 시간 반환
  if (cachedTokenData && cachedTokenData.token === token) {
    return cachedTokenData.expiration
  }

  // 캐시가 없으면 디코딩 수행
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null

    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    const expiration = payload.exp ? payload.exp * 1000 : null // Unix timestamp를 밀리초로 변환

    // 디코딩 결과를 캐시에 저장
    cachedTokenData = {
      token,
      payload,
      expiration,
      isValid: true, // 디코딩 성공했으므로 형식적으로는 유효
      decodedAt: Date.now()
    }

    return expiration
  } catch (error) {
    console.warn('토큰 만료 시간 추출 실패:', error)
    return null
  }
}

/**
 * 토큰이 만료되었는지 확인
 */
export function isTokenExpired(token: string): boolean {
  const expiration = getTokenExpiration(token)
  if (!expiration) return true
  
  const now = Date.now()
  const buffer = 5 * 60 * 1000 // 5분 버퍼
  
  return now >= (expiration - buffer)
}

/**
 * 토큰이 곧 만료될 예정인지 확인 (1시간 이내)
 */
export function isTokenExpiringSoon(token: string): boolean {
  const expiration = getTokenExpiration(token)
  if (!expiration) return true
  
  const now = Date.now()
  const oneHour = 60 * 60 * 1000
  
  return (expiration - now) <= oneHour
}

