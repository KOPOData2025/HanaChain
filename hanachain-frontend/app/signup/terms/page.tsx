"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { BackButton } from "@/components/ui/back-button"
import { CTAButton } from "@/components/ui/cta-button"
import { TermsCheckbox } from "@/components/ui/terms-checkbox"
import { ErrorMessage } from "@/components/ui/error-message"
import { AuthApi } from "@/lib/api/auth-api"

interface Agreements {
  all: boolean
  serviceTerms: boolean
  privacyPolicy: boolean
  marketing: boolean
}

export default function TermsPage() {
  const router = useRouter()
  const [agreements, setAgreements] = useState<Agreements>({
    all: false,
    serviceTerms: false,
    privacyPolicy: false,
    marketing: false
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState("")

  const handleAllCheck = (checked: boolean) => {
    setAgreements({
      all: checked,
      serviceTerms: checked,
      privacyPolicy: checked,
      marketing: checked
    })
  }

  const handleSingleCheck = (name: keyof Agreements, checked: boolean) => {
    const newAgreements = { ...agreements, [name]: checked }
    
    // 개별 항목 중 하나라도 해제되면 전체 동의도 해제
    if (name !== 'all') {
      newAgreements.all = 
        newAgreements.serviceTerms && 
        newAgreements.privacyPolicy && 
        newAgreements.marketing
    }
    
    setAgreements(newAgreements)
  }

  const handleSubmit = async () => {
    if (!agreements.serviceTerms || !agreements.privacyPolicy) {
      return
    }
    
    setIsSubmitting(true)
    setSubmitError("")
    
    try {
      // 백엔드에서 세션 생성
      const response = await AuthApi.acceptTerms({
        termsAccepted: agreements.serviceTerms,
        privacyAccepted: agreements.privacyPolicy,
        marketingAccepted: agreements.marketing
      })
      
      // 세션 ID만 sessionStorage에 저장 (보안 강화)
      sessionStorage.setItem('signupSessionId', response.sessionId)
      
      router.push('/signup/account')
      
    } catch (error: any) {
      console.error("약관 동의 처리 중 오류:", error)
      setSubmitError(error.message || "약관 동의 처리 중 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const isFormValid = agreements.serviceTerms && agreements.privacyPolicy

  return (
    <div className="min-h-screen bg-white">
      <div className="container max-w-md mx-auto px-4 py-6">
        <BackButton href="/" />
        
        <div className="mt-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">회원가입</h1>
          <p className="text-gray-600 mb-8">
            HanaChain 서비스 이용을 위해 약관 동의가 필요합니다.
          </p>
          
          <div className="space-y-4">
            {/* 전체 동의 */}
            <div className="p-4 bg-gray-50 rounded-lg">
              <TermsCheckbox
                checked={agreements.all}
                onChange={(checked) => handleAllCheck(checked)}
                label="모두 동의합니다"
                className="font-medium"
              />
            </div>
            
            <div className="space-y-3">
              {/* 필수 약관들 */}
              <TermsCheckbox
                checked={agreements.serviceTerms}
                onChange={(checked) => handleSingleCheck('serviceTerms', checked)}
                label="서비스 이용약관 동의"
                required={true}
                description="HanaChain 플랫폼 이용을 위한 기본 약관입니다."
              />
              
              <TermsCheckbox
                checked={agreements.privacyPolicy}
                onChange={(checked) => handleSingleCheck('privacyPolicy', checked)}
                label="개인정보 처리방침 동의"
                required={true}
                description="개인정보 수집 및 이용에 대한 동의입니다."
              />
              
              {/* 선택 약관 */}
              <TermsCheckbox
                checked={agreements.marketing}
                onChange={(checked) => handleSingleCheck('marketing', checked)}
                label="마케팅 정보 수신 동의"
                required={false}
                description="이벤트, 혜택 등 마케팅 정보를 받아보실 수 있습니다."
              />
            </div>
          </div>
          
          {submitError && (
            <ErrorMessage message={submitError} type="error" />
          )}
          
          <div className="mt-12">
            <CTAButton 
              onClick={handleSubmit}
              disabled={!isFormValid}
              loading={isSubmitting}
              fullWidth={true}
            >
              다음
            </CTAButton>
          </div>
          
          {!isFormValid && (
            <p className="text-sm text-gray-500 text-start mt-4">
              필수 약관에 동의해주세요
            </p>
          )}
        </div>
      </div>
    </div>
  )
}
