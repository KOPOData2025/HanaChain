"use client"

import { useState, useEffect } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import Link from "next/link"
import { BackButton } from "@/components/ui/back-button"
import { CTAButton } from "@/components/ui/cta-button"
import { FormInput } from "@/components/ui/form-input"
import { PasswordInput } from "@/components/ui/password-input"
import { ErrorMessage } from "@/components/ui/error-message"
import { Checkbox } from "@/components/ui/checkbox"
import { useAuth } from "@/lib/auth-context"

const loginSchema = z.object({
  email: z.string().email("유효한 이메일 주소를 입력해주세요"),
  password: z.string().min(1, "비밀번호를 입력해주세요"),
  rememberMe: z.boolean().optional()
})

type LoginFormData = z.infer<typeof loginSchema>

export default function LoginPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { login, loading, isLoggedIn } = useAuth()
  const [submitError, setSubmitError] = useState("")

  // 리다이렉트 URL 가져오기
  const redirectUrl = searchParams.get('redirect') || '/'

  // 이미 로그인된 사용자는 자동으로 리다이렉트
  useEffect(() => {
    if (isLoggedIn && !loading) {
      console.log('✅ 이미 로그인된 상태 - 홈으로 리다이렉트')
      router.push(redirectUrl)
    }
  }, [isLoggedIn, loading, router, redirectUrl])
  
  const { 
    register, 
    handleSubmit, 
    formState: { errors },
    watch
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      rememberMe: false
    }
  })
  
  const watchedPassword = watch("password")
  
  const onSubmit = async (data: LoginFormData) => {
    setSubmitError("")
    
    try {
      console.log('🔐 로그인 시도:', data.email)
      
      const success = await login(data.email, data.password)
      
      if (success) {
        console.log('✅ 로그인 성공')
        // 성공 시 리다이렉트 URL로 이동 (기본값: 홈페이지)
        router.push(redirectUrl)
      } else {
        setSubmitError("이메일 또는 비밀번호가 올바르지 않습니다.")
      }
      
    } catch (error) {
      console.error("❌ 로그인 중 오류:", error)
      setSubmitError("로그인 중 오류가 발생했습니다. 다시 시도해주세요.")
    }
  }
  
  return (
    <div className="min-h-screen bg-white">
      <div className="container max-w-md mx-auto px-4 py-6 pt-22">
        <BackButton href="/" />
        
        <div className="mt-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">이메일로 계속하기</h1>
          <p className="text-gray-600 mb-8">
            HanaChain 계정에 로그인하세요.
          </p>
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div>
              <FormInput
                label="이메일 주소"
                type="email"
                placeholder="예) hana@hanachain.co.kr"
                required
                error={errors.email?.message}
                {...register("email")}
              />
            </div>
            
            <div>
              <PasswordInput
                label="비밀번호"
                placeholder="비밀번호 (8자 이상)"
                required
                error={errors.password?.message}
                value={watchedPassword || ""}
                {...register("password")}
              />
            </div>
            
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Checkbox 
                  id="rememberMe"
                  {...register("rememberMe")}
                />
                <label 
                  htmlFor="rememberMe" 
                  className="text-sm text-gray-600 cursor-pointer"
                >
                  로그인 유지
                </label>
              </div>
              
              <Link 
                href="/forgot-password" 
                className="text-sm text-gray-600 hover:text-teal-600 underline"
              >
                비밀번호 재설정
              </Link>
            </div>
            
            {submitError && (
              <ErrorMessage message={submitError} type="error" />
            )}
            
            <div className="mt-8">
              <CTAButton 
                type="submit" 
                disabled={loading}
                loading={loading}
                fullWidth
              >
                로그인
              </CTAButton>
            </div>
          </form>
          
          <div className="mt-6 text-center">
            <Link 
              href="/signup/terms"
              className="inline-flex items-center justify-center w-full p-4 text-gray-600 hover:text-teal-600 border border-gray-300 rounded-lg hover:bg-teal-50 transition-colors"
            >
              이메일로 HanaChain 가입
              <span className="ml-1">→</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}