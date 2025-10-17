'use client'

import { useEffect, useRef, useState, forwardRef, useImperativeHandle, useCallback } from 'react'
import Script from 'next/script'

// 전역 타입 선언
declare global {
  interface Window {
    grecaptcha: any
    onRecaptchaLoaded?: () => void
  }
}

interface RecaptchaProps {
  onVerify: (token: string) => void
  onError?: (error: Error) => void
  action?: string
  className?: string
}

export interface RecaptchaRef {
  refreshToken: () => Promise<string | null>
  isReady: () => boolean
  getStatus: () => {
    isLoaded: boolean
    isExecuting: boolean
    hasError: boolean
  }
}

/**
 * Google reCAPTCHA v3 컴포넌트
 * 사용자의 상호작용 없이 백그라운드에서 동작하여 봇을 감지합니다.
 */
export const Recaptcha = forwardRef<RecaptchaRef, RecaptchaProps>(({ 
  onVerify, 
  onError,
  action = 'submit',
  className 
}, ref) => {
  const [isLoaded, setIsLoaded] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isExecuting, setIsExecuting] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const siteKey = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY

  // reCAPTCHA 실행 함수 (메모이제이션으로 안정성 강화)
  const executeRecaptcha = useCallback(async (): Promise<string | null> => {
    console.log('🔄 reCAPTCHA 실행 시도', {
      isLoaded,
      isExecuting,
      hasGrecaptcha: !!window.grecaptcha,
      hasSiteKey: !!siteKey
    })

    if (!window.grecaptcha || !isLoaded || !siteKey || isExecuting) {
      const reason = !window.grecaptcha ? 'grecaptcha not loaded' : 
                   !isLoaded ? 'component not loaded' :
                   !siteKey ? 'site key missing' : 'already executing'
      console.warn('❌ reCAPTCHA 실행 불가:', reason)
      return null
    }

    setIsExecuting(true)
    setError(null)

    try {
      const token = await new Promise<string>((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('reCAPTCHA 토큰 생성 시간 초과 (10초)'))
        }, 10000)

        window.grecaptcha.ready(() => {
          window.grecaptcha
            .execute(siteKey, { action })
            .then((token: string) => {
              clearTimeout(timeout)
              console.log('✅ reCAPTCHA 토큰 생성 성공:', token.slice(0, 20) + '...')
              resolve(token)
            })
            .catch((err: Error) => {
              clearTimeout(timeout)
              console.error('❌ reCAPTCHA 실행 중 오류:', err)
              reject(err)
            })
        })
      })
      
      return token
    } catch (err) {
      const error = err instanceof Error ? err : new Error('reCAPTCHA 실행 중 알 수 없는 오류가 발생했습니다.')
      console.error('❌ reCAPTCHA 실행 중 오류:', error)
      setError(error.message)
      onError?.(error)
      return null
    } finally {
      setIsExecuting(false)
    }
  }, [isLoaded, isExecuting, siteKey, action])

  // ref를 통해 외부에서 호출할 수 있는 함수들 (의존성 최적화)
  useImperativeHandle(ref, () => ({
    refreshToken: async () => {
      console.log('🔄 reCAPTCHA 토큰 갱신 요청')
      const token = await executeRecaptcha()
      if (token) {
        onVerify(token)
      }
      return token
    },
    isReady: () => {
      const ready = isLoaded && !!window.grecaptcha && !isExecuting
      console.log('🔍 reCAPTCHA 준비 상태:', { ready, isLoaded, hasGrecaptcha: !!window.grecaptcha, isExecuting })
      return ready
    },
    getStatus: () => ({
      isLoaded,
      isExecuting,
      hasError: !!error
    })
  }), [executeRecaptcha, onVerify, isLoaded, isExecuting, error])

  // 초기 토큰 생성 (안정성 강화)
  useEffect(() => {
    if (!siteKey) {
      const err = new Error('NEXT_PUBLIC_RECAPTCHA_SITE_KEY가 설정되지 않았습니다.')
      setError(err.message)
      onError?.(err)
      return
    }

    if (isLoaded && !isExecuting) {
      console.log('🚀 초기 reCAPTCHA 토큰 생성 시작')
      // 약간의 지연을 두어 스크립트 안정화 대기
      const timer = setTimeout(() => {
        executeRecaptcha().then(token => {
          if (token) {
            console.log('✅ 초기 토큰 생성 완료')
            onVerify(token)
          } else {
            console.warn('⚠️ 초기 토큰 생성 실패')
          }
        })
      }, 100)
      
      return () => clearTimeout(timer)
    }
  }, [isLoaded, siteKey, executeRecaptcha, onVerify])

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      console.log('🧹 reCAPTCHA 컴포넌트 정리')
      setIsExecuting(false)
      setError(null)
    }
  }, [])

  const handleScriptLoad = () => {
    setIsLoaded(true)
  }

  const handleScriptError = () => {
    const err = new Error('reCAPTCHA 스크립트 로드에 실패했습니다.')
    setError(err.message)
    onError?.(err)
  }

  if (!siteKey) {
    return (
      <div className="text-red-500 text-sm">
        reCAPTCHA 설정 오류: 사이트 키가 필요합니다.
      </div>
    )
  }

  return (
    <>
      <Script
        src={`https://www.google.com/recaptcha/api.js?render=${siteKey}`}
        strategy="lazyOnload"
        onLoad={handleScriptLoad}
        onError={handleScriptError}
      />
      <div 
        ref={containerRef} 
        className={className}
        style={{ display: 'none' }}
      >
        {error && (
          <div className="text-red-500 text-sm">
            {error}
          </div>
        )}
        {isExecuting && (
          <div className="text-gray-500 text-sm">
            reCAPTCHA 검증 중...
          </div>
        )}
      </div>
    </>
  )
})

/**
 * 서버에서 reCAPTCHA 토큰을 검증하는 함수
 * 실제 구현에서는 서버 API에서 사용됩니다.
 * 
 * @param token reCAPTCHA 토큰
 * @param secretKey reCAPTCHA 시크릿 키
 * @returns 검증 결과
 */
export async function verifyRecaptchaToken(
  token: string,
  secretKey: string
): Promise<{
  success: boolean
  score?: number
  action?: string
  challenge_ts?: string
  hostname?: string
  'error-codes'?: string[]
}> {
  const response = await fetch('https://www.google.com/recaptcha/api/siteverify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      secret: secretKey,
      response: token,
    }),
  })

  return response.json()
}
