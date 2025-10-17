"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { BackButton } from "@/components/ui/back-button"
import { CTAButton } from "@/components/ui/cta-button"
import { FormInput } from "@/components/ui/form-input"
import { PasswordInput } from "@/components/ui/password-input"
import { ErrorMessage } from "@/components/ui/error-message"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { useEmailCheck } from "@/hooks/use-api"
import { useDebounce } from "@/hooks/use-debounce"
import { useSecureFormSubmission } from "@/hooks/use-form-submission"
import { AuthApi } from "@/lib/api/auth-api"

const accountSchema = z.object({
  email: z.string().email("유효한 이메일 주소를 입력해주세요"),
  password: z.string()
    .min(8, "비밀번호는 최소 8자 이상이어야 합니다")
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[a-zA-Z\d!@#$%^&*]{8,}$/, 
      "영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다"),
  confirmPassword: z.string()
}).refine(data => data.password === data.confirmPassword, {
  message: "비밀번호가 일치하지 않습니다",
  path: ["confirmPassword"]
})

type AccountFormData = z.infer<typeof accountSchema>

export default function AccountPage() {
  const router = useRouter()
  const [submitError, setSubmitError] = useState("")
  const [sessionId, setSessionId] = useState("")
  
  const { 
    register, 
    handleSubmit, 
    formState: { errors }, 
    watch,
    setError,
    clearErrors
  } = useForm<AccountFormData>({
    resolver: zodResolver(accountSchema),
    mode: "onChange"
  })
  
  const watchedPassword = watch("password")
  const watchedEmail = watch("email")
  
  // 이메일 디바운싱 (500ms 지연)
  const debouncedEmail = useDebounce(watchedEmail, 500)
  
  // 이메일 중복 검사 API 훅
  const emailCheck = useEmailCheck()
  
  // 보안 폼 제출 훅 (전역 reCAPTCHA 컨텍스트 사용)
  const { secureSubmit, isSubmitting } = useSecureFormSubmission()
  
  // 세션 검증
  useEffect(() => {
    const savedSessionId = sessionStorage.getItem('signupSessionId')
    
    if (!savedSessionId) {
      router.push('/signup/terms')
      return
    }
    
    setSessionId(savedSessionId)
  }, [router])
  
  // 디바운싱된 이메일로 자동 중복 검사
  useEffect(() => {
    if (debouncedEmail && debouncedEmail.length > 0 && !errors.email) {
      checkEmailDuplicate(debouncedEmail)
    }
  }, [debouncedEmail])
  
  // 이메일 중복 검사
  const checkEmailDuplicate = async (email: string) => {
    try {
      const result = await emailCheck.checkEmail(email)
      if (!result.available) {
        setError("email", { 
          type: "manual", 
          message: result.message || "이미 사용 중인 이메일입니다" 
        })
        return false
      }
      clearErrors("email")
      return true
    } catch (error) {
      const errorMessage = emailCheck.signupErrorMessage || "이메일 확인 중 오류가 발생했습니다"
      setError("email", { 
        type: "manual", 
        message: errorMessage
      })
      return false
    }
  }
  
  const onSubmit = async (data: AccountFormData) => {
    setSubmitError("")
    
    if (!sessionId) {
      setSubmitError("세션이 만료되었습니다. 처음부터 다시 시작해주세요.")
      router.push('/signup/terms')
      return
    }
    
    try {
      console.log('📝 계정 정보 폼 제출 시작')
      
      await secureSubmit(
        async (recaptchaToken: string) => {
          console.log('🔐 reCAPTCHA 토큰으로 폼 제출 실행')
          
          // 이메일 중복 검사
          const isEmailAvailable = await checkEmailDuplicate(data.email)
          
          if (!isEmailAvailable) {
            throw new Error("이메일 사용 불가")
          }
          
          // 백엔드 세션에 계정 정보 저장 (보안 강화)
          await AuthApi.saveAccount({
            sessionId,
            email: data.email,
            password: data.password
          })
          
          console.log('✅ 계정 정보 백엔드 저장 완료')
          
          // sessionStorage에 이메일 저장 (Verification 페이지에서 사용)
          sessionStorage.setItem('signupEmail', data.email)
          
          return { success: true }
        },
        {
          action: "signup_account",
          maxRetries: 3,
          retryDelay: 1000
        }
      )
      
      console.log('🚀 다음 페이지로 이동: /signup/verification')
      // 성공 시 다음 페이지로 이동
      router.push("/signup/verification")
      
    } catch (error) {
      console.error("❌ 폼 제출 중 오류:", error)
      if (error instanceof Error) {
        setSubmitError(error.message)
      } else {
        setSubmitError("계정 생성 중 오류가 발생했습니다. 다시 시도해주세요.")
      }
    }
  }
  
  return (
    <div className="min-h-screen bg-white">
      <div className="container max-w-md mx-auto px-4 py-6">
        <BackButton href="/signup/terms" />
        
        <div className="mt-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">계정 정보 입력</h1>
          <p className="text-gray-600 mb-8">
            HanaChain에서 사용할 계정 정보를 입력해주세요.
          </p>
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="relative">
              <FormInput
                label="이메일"
                type="email"
                placeholder="example@email.com"
                required
                error={errors.email?.message}
                {...register("email")}
              />
              {emailCheck.loading && (
                <div className="absolute right-3 top-11">
                  <LoadingSpinner size="sm" />
                </div>
              )}
            </div>
            
            <PasswordInput
              label="비밀번호"
              placeholder="영문, 숫자, 특수문자 포함 8자 이상"
              required
              showStrengthIndicator
              error={errors.password?.message}
              value={watchedPassword}
              {...register("password")}
            />
            
            <PasswordInput
              label="비밀번호 확인"
              placeholder="비밀번호를 다시 입력해주세요"
              required
              error={errors.confirmPassword?.message}
              {...register("confirmPassword")}
            />
            
            {submitError && (
              <ErrorMessage message={submitError} type="error" />
            )}
            
            <div className="mt-8">
              <CTAButton 
                type="submit" 
                disabled={isSubmitting}
                loading={isSubmitting}
                fullWidth
              >
                다음
              </CTAButton>
            </div>
          </form>
          
        </div>
      </div>
    </div>
  )
}
