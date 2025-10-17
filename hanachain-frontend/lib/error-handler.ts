/**
 * 에러 처리 유틸리티
 * 회원가입 프로세스에서 발생하는 다양한 에러를 일관되게 처리합니다.
 */

export type ApiError = {
  message: string
  code?: string
  field?: string // 특정 폼 필드와 관련된 에러의 경우
}

export type ApiState<T> = {
  data: T | null
  loading: boolean
  error: ApiError | null
}

export const initialApiState = <T>(): ApiState<T> => ({
  data: null,
  loading: false,
  error: null
})

/**
 * 다양한 에러 타입을 ApiError 형태로 변환
 */
export const handleApiError = (error: unknown): ApiError => {
  // 이미 ApiError 형태인 경우
  if (typeof error === 'object' && error !== null && 'message' in error) {
    const apiError = error as ApiError
    return {
      message: apiError.message,
      code: apiError.code,
      field: apiError.field
    }
  }
  
  // Error 객체인 경우
  if (error instanceof Error) {
    return { 
      message: error.message || '알 수 없는 오류가 발생했습니다',
      code: 'UNKNOWN_ERROR'
    }
  }
  
  // 문자열 에러인 경우
  if (typeof error === 'string') {
    return { 
      message: error,
      code: 'STRING_ERROR'
    }
  }
  
  // 기타 모든 경우
  return { 
    message: '알 수 없는 오류가 발생했습니다',
    code: 'UNKNOWN_ERROR'
  }
}

/**
 * 네트워크 에러를 사용자 친화적인 메시지로 변환
 */
export const getNetworkErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    // 타임아웃 에러
    if (error.message.includes('timeout') || error.message.includes('시간이 초과')) {
      return '요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.'
    }
    
    // 네트워크 연결 에러
    if (error.message.includes('fetch') || error.message.includes('network')) {
      return '인터넷 연결을 확인하고 다시 시도해주세요.'
    }
    
    // 서버 에러 (500번대)
    if (error.message.includes('500') || error.message.includes('Server Error')) {
      return '서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.'
    }
    
    // 인증 에러 (401, 403)
    if (error.message.includes('401') || error.message.includes('403')) {
      return '인증에 실패했습니다. 다시 로그인해주세요.'
    }
  }
  
  return '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
}

/**
 * 회원가입 특화 에러 메시지 변환
 */
export const getSignupErrorMessage = (error: ApiError): string => {
  const { code, field, message } = error
  
  // 이메일 관련 에러
  if (field === 'email' || code?.includes('EMAIL')) {
    if (code === 'EMAIL_ALREADY_EXISTS') {
      return '이미 사용 중인 이메일입니다. 다른 이메일을 입력해주세요.'
    }
    if (code === 'EMAIL_INVALID') {
      return '올바른 이메일 형식이 아닙니다.'
    }
  }
  
  // 비밀번호 관련 에러
  if (field === 'password' || code?.includes('PASSWORD')) {
    if (code === 'PASSWORD_TOO_WEAK') {
      return '비밀번호가 너무 약합니다. 영문, 숫자, 특수문자를 포함해 8자 이상 입력해주세요.'
    }
    if (code === 'PASSWORD_MISMATCH') {
      return '비밀번호가 일치하지 않습니다.'
    }
  }
  
  // 닉네임 관련 에러
  if (field === 'nickname' || code?.includes('NICKNAME')) {
    if (code === 'NICKNAME_ALREADY_EXISTS') {
      return '이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해주세요.'
    }
    if (code === 'NICKNAME_INVALID') {
      return '닉네임은 2-20자의 한글, 영문, 숫자만 사용 가능합니다.'
    }
  }
  
  // 인증코드 관련 에러
  if (field === 'verificationCode' || code?.includes('VERIFICATION')) {
    if (code === 'VERIFICATION_CODE_INVALID') {
      return '인증코드가 올바르지 않습니다. 다시 확인해주세요.'
    }
    if (code === 'VERIFICATION_CODE_EXPIRED') {
      return '인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.'
    }
  }
  
  // 기본 메시지 반환
  return message || '오류가 발생했습니다. 다시 시도해주세요.'
}

/**
 * 타임아웃 프로미스 생성
 */
export const createTimeoutPromise = (ms: number) => {
  return new Promise<never>((_, reject) => {
    setTimeout(() => reject(new Error('요청 시간이 초과되었습니다')), ms)
  })
}

/**
 * 타임아웃이 적용된 fetch 함수
 */
export const fetchWithTimeout = async <T>(
  promise: Promise<T>, 
  timeoutMs = 10000
): Promise<T> => {
  return Promise.race([promise, createTimeoutPromise(timeoutMs)])
}

/**
 * 재시도 로직이 적용된 fetch 함수
 */
export const fetchWithRetry = async <T>(
  fetchFn: () => Promise<T>,
  maxRetries = 3,
  delayMs = 1000
): Promise<T> => {
  let lastError: unknown
  
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await fetchFn()
    } catch (error) {
      lastError = error
      
      // 마지막 시도가 아니면 지연 후 재시도
      if (attempt < maxRetries - 1) {
        // 지수 백오프: 시도할 때마다 지연 시간 증가
        const delay = delayMs * Math.pow(2, attempt)
        await new Promise(resolve => setTimeout(resolve, delay))
      }
    }
  }
  
  throw lastError
}

/**
 * API 호출을 위한 고차 함수
 * 타임아웃, 재시도, 에러 처리를 모두 포함
 */
export const apiCall = async <T>(
  fetchFn: () => Promise<T>,
  options: {
    timeout?: number
    maxRetries?: number
    retryDelay?: number
  } = {}
): Promise<T> => {
  const { timeout = 10000, maxRetries = 3, retryDelay = 1000 } = options
  
  const wrappedFetch = () => fetchWithTimeout(fetchFn(), timeout)
  
  try {
    return await fetchWithRetry(wrappedFetch, maxRetries, retryDelay)
  } catch (error) {
    throw handleApiError(error)
  }
}

/**
 * React 상태 관리를 위한 API 상태 업데이트 헬퍼
 */
export const createApiStateHandlers = <T>() => {
  const setLoading = (setState: React.Dispatch<React.SetStateAction<ApiState<T>>>) => {
    setState(prev => ({ ...prev, loading: true, error: null }))
  }
  
  const setSuccess = (
    setState: React.Dispatch<React.SetStateAction<ApiState<T>>>,
    data: T
  ) => {
    setState({ data, loading: false, error: null })
  }
  
  const setError = (
    setState: React.Dispatch<React.SetStateAction<ApiState<T>>>,
    error: unknown
  ) => {
    setState(prev => ({ 
      ...prev, 
      loading: false, 
      error: handleApiError(error) 
    }))
  }
  
  return { setLoading, setSuccess, setError }
}
