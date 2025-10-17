"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { BackButton } from "@/components/ui/back-button"
import { CTAButton } from "@/components/ui/cta-button"
import { FormInput } from "@/components/ui/form-input"
import { ErrorMessage } from "@/components/ui/error-message"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { useNicknameCheck, useSignupComplete } from "@/hooks/use-api"
import { useDebounce } from "@/hooks/use-debounce"
import { useSecureFormSubmission } from "@/hooks/use-form-submission"
import { AuthApi } from "@/lib/api/auth-api"

const nicknameSchema = z.object({
  nickname: z.string()
    .min(2, "닉네임은 2자 이상이어야 합니다")
    .max(15, "닉네임은 15자 이하여야 합니다")
    .regex(/^[a-zA-Z0-9가-힣_]+$/, "영문, 숫자, 한글, 언더바(_)만 사용 가능합니다")
})

type NicknameFormData = z.infer<typeof nicknameSchema>

export default function NicknamePage() {
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
  } = useForm<NicknameFormData>({
    resolver: zodResolver(nicknameSchema),
    mode: "onChange"
  })
  
  const nickname = watch("nickname")
  
  // 닉네임 디바운싱 (500ms 지연)
  const debouncedNickname = useDebounce(nickname, 500)
  
  // API 훅들
  const nicknameCheck = useNicknameCheck()
  const signupComplete = useSignupComplete()
  
  // 보안 폼 제출 훅 (전역 reCAPTCHA 컨텍스트 사용)
  const { secureSubmit, isSubmitting } = useSecureFormSubmission()
  
  // 세션 검증
  useEffect(() => {
    const savedSessionId = sessionStorage.getItem('signupSessionId')
    
    if (!savedSessionId) {
      router.push('/signup/verification')
      return
    }
    
    setSessionId(savedSessionId)
  }, [router])
  
  // 디바운싱된 닉네임으로 자동 중복 검사
  useEffect(() => {
    if (debouncedNickname && debouncedNickname.length >= 2 && !errors.nickname) {
      checkNicknameDuplicate(debouncedNickname)
    }
  }, [debouncedNickname])
  
  const checkNicknameDuplicate = async (nickname: string) => {
    try {
      const result = await nicknameCheck.checkNickname(nickname)
      if (!result.available) {
        setError("nickname", { 
          type: "manual", 
          message: result.message || "이미 사용 중인 닉네임입니다" 
        })
        return false
      }
      clearErrors("nickname")
      return true
    } catch (error: any) {
      const errorMessage = nicknameCheck.signupErrorMessage || "닉네임 확인 중 오류가 발생했습니다"
      setError("nickname", { 
        type: "manual", 
        message: errorMessage
      })
      return false
    }
  }
  
  const onSubmit = async (data: NicknameFormData) => {
    setSubmitError("")
    
    try {
      console.log('📝 [닉네임페이지] 회원가입 완료 요청 시작')
      
      await secureSubmit(
        async (recaptchaToken: string) => {
          console.log('🔐 [닉네임페이지] reCAPTCHA 토큰으로 회원가입 완료 실행')
          
          // 닉네임 중복 검사
          const isNicknameAvailable = await checkNicknameDuplicate(data.nickname)
          
          if (!isNicknameAvailable) {
            throw new Error("닉네임 사용 불가")
          }
          
          // 백엔드 세션을 통한 회원가입 완료
          const response = await AuthApi.completeSignup({
            sessionId,
            nickname: data.nickname
          })
          
          console.log('✅ [닉네임페이지] 회원가입 완료 성공')
          return response
        },
        {
          action: "signup_nickname",
          maxRetries: 3,
          retryDelay: 1000
        }
      )
      
      console.log('🚀 [닉네임페이지] 완료 페이지로 이동: /signup/complete')
      
      // 완료 페이지에서 사용할 닉네임 저장
      sessionStorage.setItem('signupNickname', data.nickname)
      
      // 성공 시 완료 페이지로 이동
      router.push("/signup/complete")
      
    } catch (error: any) {
      console.error("❌ [닉네임페이지] 회원가입 중 오류 발생:", error)
      if (error instanceof Error) {
        setSubmitError(error.message)
      } else {
        setSubmitError(signupComplete.signupErrorMessage || "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.")
      }
    }
  }
  
  const isFormValid = nickname && !errors.nickname

  return (
    <div className="min-h-screen bg-white">
      <div className="container max-w-md mx-auto px-4 py-6">
        <BackButton href="/signup/verification" />

        <div className="mt-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">닉네임 설정</h1>
          <p className="text-gray-600 mb-8">
            HanaChain에서 사용할 고유한 기부자 명칭을 설정해주세요.
          </p>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="relative">
              <FormInput
                label="닉네임"
                placeholder="2-15자 영문, 숫자, 한글, 언더바(_)"
                required
                error={errors.nickname?.message}
                {...register("nickname")}
              />
              {nicknameCheck.loading && (
                <div className="absolute right-3 top-11">
                  <LoadingSpinner size="sm" />
                </div>
              )}
            </div>

            {nickname && !errors.nickname && (
              <p className="text-green-500 text-sm flex items-center gap-1">
                <span>✅</span>
                사용 가능한 닉네임 형식입니다
              </p>
            )}

            <div className="space-y-2 bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-start gap-2">
                <span className="text-blue-600 text-xl">ℹ️</span>
                <div className="text-sm text-blue-900">
                  <p className="font-medium mb-1">블록체인 지갑 자동 생성</p>
                  <p className="text-blue-700">회원가입 완료 시 블록체인 지갑이 자동으로 생성됩니다. 별도의 지갑 비밀번호 설정이 필요하지 않습니다.</p>
                </div>
              </div>
            </div>

            {submitError && (
              <ErrorMessage message={submitError} type="error" />
            )}

            <div className="mt-8">
              <CTAButton
                type="submit"
                disabled={!isFormValid || isSubmitting}
                loading={isSubmitting}
                fullWidth
              >
                완료
              </CTAButton>
            </div>
          </form>


          <div className="mt-6 text-xs text-gray-500 leading-relaxed">
            <p>• 닉네임은 다른 기부자들에게 공개됩니다</p>
            <p>• 나중에 프로필에서 변경할 수 있습니다</p>
            <p>• 지갑은 시스템에서 안전하게 관리되며, 필요 시 복구 가능합니다</p>
          </div>
        </div>
      </div>
    </div>
  )
}
