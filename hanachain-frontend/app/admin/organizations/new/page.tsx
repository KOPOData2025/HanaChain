'use client'

import { useEffect } from 'react'
import { useAuth } from '@/lib/auth-context'
import { useRouter } from 'next/navigation'
import { OrganizationCreateForm } from '@/components/admin/organization-create-form'

export default function NewOrganizationPage() {
  const router = useRouter()
  const { isLoggedIn, loading: authLoading } = useAuth()

  // 인증 확인
  useEffect(() => {
    if (!authLoading && !isLoggedIn) {
      router.push('/login')
      return
    }
  }, [authLoading, isLoggedIn, router])

  // 인증 확인 중 로딩 표시
  if (authLoading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        </div>
      </div>
    )
  }

  // 인증되지 않은 경우 빈 화면 (리다이렉트 진행 중)
  if (!isLoggedIn) {
    return null
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <OrganizationCreateForm />
    </div>
  )
}
