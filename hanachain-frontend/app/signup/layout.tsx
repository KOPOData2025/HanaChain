'use client'

import { RecaptchaProvider } from '@/lib/recaptcha-context'

/**
 * 회원가입 플로우 전용 레이아웃
 * reCAPTCHA는 회원가입 페이지에서만 필요하므로 여기서만 로드
 */
export default function SignupLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <RecaptchaProvider>
      {children}
    </RecaptchaProvider>
  )
}
