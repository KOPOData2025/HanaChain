/**
 * 인증 상태 관련 유틸리티 함수들
 * 토큰 상태에 따른 사용자 피드백 및 UI 표시용
 */

type TokenStatus = 'valid' | 'pending' | 'expired' | 'unknown'

export interface AuthStatusInfo {
  message: string
  severity: 'info' | 'warning' | 'error' | 'success'
  showToast: boolean
  icon: string
  color: string
}

/**
 * 토큰 상태에 따른 사용자 피드백 정보 생성
 */
export function getAuthStatusInfo(tokenStatus: TokenStatus, isLoggedIn: boolean): AuthStatusInfo {
  if (!isLoggedIn) {
    return {
      message: '로그인이 필요합니다',
      severity: 'info',
      showToast: false,
      icon: '🔐',
      color: 'text-gray-500'
    }
  }

  switch (tokenStatus) {
    case 'valid':
      return {
        message: '인증이 완료되었습니다',
        severity: 'success',
        showToast: false,
        icon: '✅',
        color: 'text-green-600'
      }
    
    case 'pending':
      return {
        message: '인증 상태 확인 중...',
        severity: 'info',
        showToast: false,
        icon: '⏳',
        color: 'text-blue-500'
      }
    
    case 'expired':
      return {
        message: '인증이 만료되었습니다. 다시 로그인해주세요.',
        severity: 'error',
        showToast: true,
        icon: '⛔',
        color: 'text-red-600'
      }
    
    case 'unknown':
      return {
        message: '인증 상태를 확인할 수 없습니다. 네트워크 연결을 확인해주세요.',
        severity: 'warning',
        showToast: false,
        icon: '⚠️',
        color: 'text-yellow-600'
      }
    
    default:
      return {
        message: '인증 상태를 알 수 없습니다',
        severity: 'warning',
        showToast: false,
        icon: '❓',
        color: 'text-gray-500'
      }
  }
}

/**
 * 토큰 상태가 API 호출에 안전한지 확인
 */
export function isTokenSafeForApiCall(tokenStatus: TokenStatus): boolean {
  // 'expired'인 경우에만 API 호출을 차단
  // 'unknown'이나 'pending'인 경우에는 호출 허용 (API 클라이언트에서 401 처리)
  return tokenStatus !== 'expired'
}

/**
 * 관리자 페이지 접근 가능 여부 확인
 */
export function canAccessAdminPage(isLoggedIn: boolean, tokenStatus: TokenStatus, user: any): boolean {
  if (!isLoggedIn || !user) {
    return false
  }
  
  // 토큰이 만료된 경우 접근 불가
  if (tokenStatus === 'expired') {
    return false
  }
  
  // 다른 상태에서는 접근 허용 (백엔드에서 권한 재확인)
  return true
}

/**
 * 네트워크 오류 시 재시도 가능 여부 확인
 */
export function canRetryAuth(tokenStatus: TokenStatus): boolean {
  return tokenStatus === 'unknown' || tokenStatus === 'pending'
}

/**
 * 토큰 상태 기반 로딩 상태 표시 여부
 */
export function shouldShowAuthLoading(loading: boolean, tokenStatus: TokenStatus): boolean {
  return loading || tokenStatus === 'pending'
}