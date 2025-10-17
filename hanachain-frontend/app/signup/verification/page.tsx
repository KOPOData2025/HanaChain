"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { BackButton } from "@/components/ui/back-button"
import { CTAButton } from "@/components/ui/cta-button"
import { ErrorMessage } from "@/components/ui/error-message"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { VerificationCodeInput } from "@/components/ui/verification-code-input"
import { useVerificationCodeSend, useVerificationCodeVerify } from "@/hooks/use-api"
import { useSecureFormSubmission } from "@/hooks/use-form-submission"
import { AuthApi } from "@/lib/api/auth-api"
import { cn } from "@/lib/utils"

export default function VerificationPage() {
  const router = useRouter()
  const [email, setEmail] = useState("")
  const [sessionId, setSessionId] = useState("")
  const [verificationCode, setVerificationCode] = useState("")
  const [timeLeft, setTimeLeft] = useState(600) // 10분 = 600초
  const [success, setSuccess] = useState(false)
  const [cooldownTime, setCooldownTime] = useState(0) // 30초 쿨다운
  const [hasInitialSend, setHasInitialSend] = useState(false)
  
  // API 훅들
  const codeSender = useVerificationCodeSend()
  const codeVerifier = useVerificationCodeVerify()
  
  // 보안 폼 제출 훅 (전역 reCAPTCHA 컨텍스트 사용)
  const { secureSubmit, isSubmitting } = useSecureFormSubmission()
  
  // 세션 검증
  useEffect(() => {
    const savedSessionId = sessionStorage.getItem('signupSessionId')
    const savedEmail = sessionStorage.getItem('signupEmail')
    
    if (!savedSessionId || !savedEmail) {
      router.push('/signup/account')
      return
    }
    
    setSessionId(savedSessionId)
    setEmail(savedEmail)
  }, [router])
  
  useEffect(() => {
    // 타이머 구현
    if (timeLeft > 0 && !success) {
      const timer = setTimeout(() => setTimeLeft(timeLeft - 1), 1000)
      return () => clearTimeout(timer)
    }
  }, [timeLeft, success])
  
  useEffect(() => {
    // 쿨다운 타이머 구현
    if (cooldownTime > 0) {
      const timer = setTimeout(() => setCooldownTime(cooldownTime - 1), 1000)
      return () => clearTimeout(timer)
    }
  }, [cooldownTime])
  
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs < 10 ? "0" : ""}${secs}`
  }
  
  const sendVerificationCode = async (emailToVerify: string) => {
    try {
      await codeSender.sendCode(emailToVerify)
      // 타이머 리셋
      setTimeLeft(600)
      setSuccess(false)
      // 30초 쿨다운 시작
      setCooldownTime(30)
      setHasInitialSend(true)
    } catch (error: any) {
      console.error("인증코드 발송 중 오류 발생:", error)
    }
  }
  
  const verifyCode = async () => {
    if (verificationCode.length !== 6) {
      return
    }
    
    try {
      console.log('📝 [검증페이지] 인증코드 검증 시작')
      
      await secureSubmit(
        async (recaptchaToken: string) => {
          console.log('🔐 [검증페이지] reCAPTCHA 토큰으로 인증코드 검증 실행')
          console.log('토큰 길이:', recaptchaToken.length)
          
          const response = await codeVerifier.verifyCode(email, verificationCode)

          if (response.success) {
            setSuccess(true)
            console.log('✅ [검증페이지] 인증코드 검증 성공')
            
            // 백엔드 세션에 이메일 인증 완료 기록
            await AuthApi.markEmailVerified({
              sessionId,
              email
            })
            
            console.log('✅ [검증페이지] 백엔드 세션에 이메일 인증 완료 기록')
            return response
          }
          
          throw new Error("인증코드 검증 실패")
        },
        {
          action: "signup_verification",
          maxRetries: 3,
          retryDelay: 1000
        }
      )
      
      console.log('🚀 [검증페이지] 다음 페이지로 이동: /signup/nickname')
      // 성공 시 다음 단계로 이동
      setTimeout(() => router.push("/signup/nickname"), 1500)
      
    } catch (error: any) {
      console.error("❌ [검증페이지] 인증코드 확인 중 오류 발생:", error)
    }
  }
  
  const handleCodeChange = (value: string) => {
    // 숫자만 허용, 최대 6자리
    const numericValue = value.replace(/[^0-9]/g, "").slice(0, 6)
    setVerificationCode(numericValue)
    // 입력 시 에러 초기화
    codeVerifier.reset()
  }
  
  const isFormValid = verificationCode.length === 6 && timeLeft > 0 && !success && !isSubmitting && hasInitialSend
  
  return (
    <div className="min-h-screen bg-white">
      <div className="container max-w-md mx-auto px-4 py-4 sm:py-6">
        <BackButton href="/signup/account" />
        
        <div className="mt-6 sm:mt-8">
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 mb-2">이메일 인증</h1>
          <p className="text-sm sm:text-base text-gray-600 mb-6 sm:mb-8 leading-relaxed">
            <span className="font-medium text-gray-900 break-all">{email}</span>로 
            {hasInitialSend ? "발송된 6자리 인증코드를 입력해주세요" : "인증코드를 발송하려면 아래 버튼을 클릭해주세요"}
          </p>
          
          <div className="space-y-6">
            {/* 인증코드 입력 필드 - 항상 표시 */}
            <div>
              <div className="flex items-center justify-between mb-4">
                <label className="block text-sm font-medium text-gray-700">
                  인증코드 입력
                </label>
                {hasInitialSend && (
                  <span className={`text-sm font-medium ${
                    timeLeft <= 60 ? "text-red-500" : "text-gray-500"
                  }`}>
                    {formatTime(timeLeft)}
                  </span>
                )}
              </div>
              
              <VerificationCodeInput
                value={verificationCode}
                onChange={handleCodeChange}
                disabled={!hasInitialSend || success}
                error={codeVerifier.signupErrorMessage}
                success={success}
              />
              
              {success && (
                <p className="text-green-500 text-sm mt-4 flex items-center justify-center gap-2">
                  <span>✅</span>
                  인증이 완료되었습니다! 잠시 후 다음 단계로 이동합니다.
                </p>
              )}
            </div>

            {/* 인증코드 발송 버튼 */}
            {!hasInitialSend && (
              <div>
                <CTAButton 
                  onClick={() => sendVerificationCode(email)} 
                  disabled={codeSender.loading || cooldownTime > 0}
                  loading={codeSender.loading}
                  fullWidth
                >
                  {codeSender.loading ? "발송 중..." : "인증코드 받기"}
                </CTAButton>
                
                {codeSender.signupErrorMessage && (
                  <ErrorMessage message={codeSender.signupErrorMessage} type="error" className="mt-3" />
                )}
              </div>
            )}
            
            {hasInitialSend && (
              <>
                <button
                  type="button"
                  className="text-blue-500 text-sm hover:text-blue-600 underline disabled:text-gray-400 disabled:no-underline flex items-center gap-2"
                  onClick={() => sendVerificationCode(email)}
                  disabled={codeSender.loading || success || cooldownTime > 0}
                >
                  {codeSender.loading && <LoadingSpinner size="sm" />}
                  {codeSender.loading 
                    ? "발송 중..." 
                    : cooldownTime > 0 
                      ? `인증코드 재발송 (${cooldownTime}초 후)`
                      : "인증코드 재발송"
                  }
                </button>
                
                {codeSender.signupErrorMessage && (
                  <ErrorMessage message={codeSender.signupErrorMessage} type="error" />
                )}
              </>
            )}
            
            <div className="mt-6 sm:mt-8">
              <CTAButton 
                onClick={verifyCode} 
                disabled={!isFormValid}
                loading={isSubmitting}
                fullWidth
                className={cn(
                  "transition-all duration-300",
                  success && "bg-green-500 hover:bg-green-600 transform scale-105"
                )}
              >
                {success ? "인증 완료!" : isSubmitting ? "인증 중..." : "인증하기"}
              </CTAButton>
            </div>
            
            {timeLeft === 0 && !success && (
              <div className="text-center text-sm text-gray-500">
                인증 시간이 만료되었습니다. 인증코드를 재발송해주세요.
              </div>
            )}
          </div>
          
        </div>
      </div>
    </div>
  )
}
