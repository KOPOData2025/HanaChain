import { useState, useCallback } from 'react'
import { useRecaptcha } from '@/lib/recaptcha-context'

/**
 * 폼 제출 중복 방지를 위한 커스텀 훅
 * 폼이 제출 중일 때 중복 제출을 방지하고 로딩 상태를 관리합니다.
 */
export function useFormSubmission() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitCount, setSubmitCount] = useState(0)

  /**
   * 안전한 폼 제출 함수
   * @param submitFunction 실제 제출 로직을 담은 함수
   * @param options 제출 옵션
   */
  const safeSubmit = useCallback(async <T>(
    submitFunction: () => Promise<T>,
    options?: {
      maxRetries?: number
      retryDelay?: number
      onRetry?: (attempt: number) => void
    }
  ): Promise<T> => {
    // 이미 제출 중이면 중복 제출 방지
    if (isSubmitting) {
      throw new Error('이미 처리 중입니다. 잠시만 기다려주세요.')
    }

    const { maxRetries = 1, retryDelay = 1000, onRetry } = options || {}
    
    setIsSubmitting(true)
    setSubmitCount(prev => prev + 1)

    let lastError: unknown
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        const result = await submitFunction()
        return result
      } catch (error) {
        lastError = error
        
        // 마지막 시도가 아니고 재시도 가능한 에러인 경우
        if (attempt < maxRetries && isRetryableError(error)) {
          onRetry?.(attempt)
          await new Promise(resolve => setTimeout(resolve, retryDelay))
          continue
        }
        
        // 재시도 불가능하거나 마지막 시도인 경우
        break
      }
    }
    
    throw lastError
  }, [isSubmitting])

  /**
   * 제출 상태를 초기화합니다.
   */
  const resetSubmission = useCallback(() => {
    setIsSubmitting(false)
  }, [])

  /**
   * 제출 완료 후 상태를 정리합니다.
   */
  const completeSubmission = useCallback(() => {
    setIsSubmitting(false)
  }, [])

  return {
    isSubmitting,
    submitCount,
    safeSubmit,
    resetSubmission,
    completeSubmission
  }
}

/**
 * 재시도 가능한 에러인지 판단하는 함수
 */
function isRetryableError(error: unknown): boolean {
  if (error instanceof Error) {
    // 네트워크 오류나 일시적인 서버 오류는 재시도 가능
    if (error.message.includes('fetch') ||
        error.message.includes('network') ||
        error.message.includes('timeout') ||
        error.message.includes('500')) {
      return true
    }
  }
  
  return false
}

/**
 * 전역 reCAPTCHA 컨텍스트를 사용한 보안 폼 제출 훅
 */
export function useSecureFormSubmission() {
  const { isReady, generateToken } = useRecaptcha()
  const [attempts, setAttempts] = useState(0)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const secureSubmit = useCallback(async <T>(
    submitFunction: (token: string) => Promise<T>,
    options?: {
      action?: string
      maxRetries?: number
      retryDelay?: number
    }
  ): Promise<T> => {
    const { action = 'submit', maxRetries = 3, retryDelay = 1000 } = options || {}
    
    console.log(`🚀 폼 제출 시도 ${attempts + 1}/${maxRetries} (action: ${action})`)
    
    if (!isReady) {
      console.error('❌ reCAPTCHA가 아직 준비되지 않음')
      throw new Error('reCAPTCHA가 준비되지 않았습니다. 페이지를 새로고침하고 잠시 기다린 후 다시 시도해주세요.')
    }

    setIsSubmitting(true)

    try {
      // 전역 컨텍스트에서 토큰 생성
      console.log(`🔄 reCAPTCHA 토큰 생성 시작 (action: ${action})`)
      const token = await generateToken(action)
      
      console.log('🎯 폼 제출 함수 실행')
      // 비즈니스 로직 실행
      const result = await submitFunction(token)
      
      console.log('✅ 폼 제출 성공')
      setAttempts(0) // 성공 시 재시도 카운터 초기화
      return result
      
    } catch (error) {
      const newAttempts = attempts + 1
      setAttempts(newAttempts)
      
      console.error(`❌ 폼 제출 실패 (시도 ${newAttempts}/${maxRetries}):`, error)
      
      // reCAPTCHA 관련 에러인지 확인
      const isRecaptchaError = error instanceof Error && (
        error.message.includes('reCAPTCHA') ||
        error.message.includes('CAPTCHA') ||
        error.message.includes('토큰')
      )
      
      // 최대 재시도 횟수 도달
      if (newAttempts >= maxRetries) {
        setAttempts(0)
        if (isRecaptchaError) {
          throw new Error('reCAPTCHA 검증에 여러 번 실패했습니다. 페이지를 새로고침하고 다시 시도해주세요.')
        }
        throw new Error('최대 재시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.')
      }
      
      // 재시도 가능한 경우 에러를 다시 throw하여 재시도 유발
      if (isRecaptchaError || isRetryableError(error)) {
        const waitTime = retryDelay * newAttempts // 선형 백오프
        console.log(`⏳ ${waitTime}ms 후 재시도 예정`)
        await new Promise(resolve => setTimeout(resolve, waitTime))
        
        // 재귀적으로 다시 시도
        return secureSubmit(submitFunction, options)
      }
      
      // 재시도 불가능한 에러는 즉시 throw
      throw error
      
    } finally {
      setIsSubmitting(false)
    }
  }, [isReady, generateToken, attempts])

  return {
    secureSubmit,
    isSubmitting,
    isReady,
    attempts,
  }
}