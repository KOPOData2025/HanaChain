import { useState, useCallback } from 'react'
import { 
  ApiState, 
  initialApiState, 
  createApiStateHandlers,
  apiCall,
  getSignupErrorMessage
} from '@/lib/error-handler'

/**
 * API 호출과 상태 관리를 위한 커스텀 훅
 */
export function useApi<T>() {
  const [state, setState] = useState<ApiState<T>>(initialApiState<T>())
  const { setLoading, setSuccess, setError } = createApiStateHandlers<T>()
  
  const execute = useCallback(async (
    fetchFn: () => Promise<T>,
    options?: {
      timeout?: number
      maxRetries?: number
      retryDelay?: number
    }
  ) => {
    try {
      setLoading(setState)
      const result = await apiCall(fetchFn, options)
      setSuccess(setState, result)
      return result
    } catch (error) {
      setError(setState, error)
      throw error
    }
  }, [setLoading, setSuccess, setError])
  
  const reset = useCallback(() => {
    setState(initialApiState<T>())
  }, [])
  
  return {
    ...state,
    execute,
    reset,
    // 회원가입에 특화된 에러 메시지
    signupErrorMessage: state.error ? getSignupErrorMessage(state.error) : null
  }
}

/**
 * 특정 API 엔드포인트별 커스텀 훅들
 */

// 이메일 중복 검사
export function useEmailCheck() {
  const api = useApi<{ available: boolean; message?: string }>()
  
  const checkEmail = useCallback(async (email: string) => {
    const { AuthApi } = await import('@/lib/api/auth-api')
    return api.execute(() => AuthApi.checkEmailAvailability(email))
  }, [api])
  
  return {
    ...api,
    checkEmail
  }
}

// 닉네임 중복 검사
export function useNicknameCheck() {
  const api = useApi<{ available: boolean; message?: string }>()
  
  const checkNickname = useCallback(async (nickname: string) => {
    const { AuthApi } = await import('@/lib/api/auth-api')
    return api.execute(() => AuthApi.checkNicknameAvailability(nickname))
  }, [api])
  
  return {
    ...api,
    checkNickname
  }
}

// 인증코드 발송
export function useVerificationCodeSend() {
  const api = useApi<{ success: boolean; message?: string }>()
  
  const sendCode = useCallback(async (email: string) => {
    const { AuthApi } = await import('@/lib/api/auth-api')
    return api.execute(() => AuthApi.sendVerificationCode(email))
  }, [api])
  
  return {
    ...api,
    sendCode
  }
}

// 인증코드 확인
export function useVerificationCodeVerify() {
  const api = useApi<{ success: boolean; message?: string }>()
  
  const verifyCode = useCallback(async (email: string, code: string) => {
    const { AuthApi } = await import('@/lib/api/auth-api')
    return api.execute(() => AuthApi.verifyCode(email, code))
  }, [api])
  
  return {
    ...api,
    verifyCode
  }
}

// 회원가입 완료
export function useSignupComplete() {
  const api = useApi<{ success: boolean; userId?: string; message?: string }>()
  
  const completeSignup = useCallback(async (signupData: {
    email: string
    password: string
    nickname: string
  }) => {
    const { AuthApi } = await import('@/lib/api/auth-api')
    return api.execute(() => AuthApi.signup(signupData))
  }, [api])
  
  return {
    ...api,
    completeSignup
  }
}
