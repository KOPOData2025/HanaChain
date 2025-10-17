'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/lib/auth-context'
import { AdminSidebar } from '@/components/admin/admin-sidebar'
import { cn } from '@/lib/utils'
import { Toaster } from 'sonner'

interface AdminLayoutProps {
  children: React.ReactNode
}

export default function AdminLayout({ children }: AdminLayoutProps) {
  const router = useRouter()
  const { isLoggedIn, loading: authLoading } = useAuth()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [isMobile, setIsMobile] = useState(false)

  // 인증 상태 확인
  useEffect(() => {
    if (!authLoading && !isLoggedIn) {
      router.push('/login')
    }
  }, [isLoggedIn, authLoading, router])

  // 모바일 감지
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768)
      if (window.innerWidth < 768) {
        setSidebarCollapsed(true)
      }
    }

    checkMobile()
    window.addEventListener('resize', checkMobile)
    return () => window.removeEventListener('resize', checkMobile)
  }, [])

  // 로딩 중이거나 인증되지 않은 경우
  if (authLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">로딩 중...</p>
        </div>
      </div>
    )
  }

  if (!isLoggedIn) {
    return null // 리다이렉트가 진행 중
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="flex h-screen">
        {/* 사이드바 */}
        <div className={cn(
          'fixed inset-y-0 left-0 z-50 transition-transform duration-300 md:relative md:translate-x-0',
          isMobile && sidebarCollapsed ? '-translate-x-full' : 'translate-x-0'
        )}>
          <AdminSidebar 
            isCollapsed={sidebarCollapsed}
            onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
          />
        </div>

        {/* 메인 콘텐츠 */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* 모바일 헤더 */}
          {isMobile && (
            <div className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 p-4 md:hidden">
              <button
                onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                className="text-gray-700 dark:text-gray-300"
              >
                <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
            </div>
          )}
          
          {/* 페이지 콘텐츠 */}
          <main className="flex-1 overflow-y-auto">
            {children}
          </main>
        </div>
      </div>

      {/* Toast notifications */}
      <Toaster position="top-right" />

      {/* 모바일 오버레이 */}
      {isMobile && !sidebarCollapsed && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 md:hidden"
          onClick={() => setSidebarCollapsed(true)}
        />
      )}
    </div>
  )
}


