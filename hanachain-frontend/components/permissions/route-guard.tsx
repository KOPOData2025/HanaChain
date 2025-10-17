'use client'

import React from 'react'
import { useProtectedRoute } from '@/hooks/use-protected-route'
import { Permission, UserRole, OrganizationRole } from '@/lib/context/permission-context'
import { Loader2 } from 'lucide-react'

interface RouteGuardProps {
  children: React.ReactNode
  requireAuth?: boolean
  permission?: Permission
  permissions?: Permission[]
  requireAll?: boolean
  userRole?: UserRole
  userRoles?: UserRole[]
  organizationId?: number
  organizationRole?: OrganizationRole
  organizationRoles?: OrganizationRole[]
  redirectTo?: string
  loadingComponent?: React.ReactNode
  fallbackComponent?: React.ReactNode
  onAccessDenied?: () => void
}

/**
 * Route guard component that protects pages based on permissions
 */
export function RouteGuard({
  children,
  loadingComponent,
  fallbackComponent,
  ...protectionOptions
}: RouteGuardProps) {
  const { hasAccess, loading } = useProtectedRoute(protectionOptions)

  // 로딩 상태 표시
  if (loading) {
    return (
      loadingComponent || (
        <div className="min-h-screen flex items-center justify-center">
          <div className="flex flex-col items-center gap-4">
            <Loader2 className="h-8 w-8 animate-spin" />
            <p className="text-muted-foreground">로딩 중...</p>
          </div>
        </div>
      )
    )
  }

  // 접근 권한이 없을 때 대체 컴포넌트 표시
  if (!hasAccess) {
    return (
      fallbackComponent || (
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold text-destructive mb-4">
              접근 권한이 없습니다
            </h1>
            <p className="text-muted-foreground">
              이 페이지에 접근할 권한이 없습니다.
            </p>
          </div>
        </div>
      )
    )
  }

  return <>{children}</>
}

// 일반적인 보호 패턴을 위한 편의 컴포넌트

interface OrganizationGuardProps {
  children: React.ReactNode
  organizationId: number
  requireAdmin?: boolean
  loadingComponent?: React.ReactNode
  fallbackComponent?: React.ReactNode
}

export function OrganizationGuard({
  children,
  organizationId,
  requireAdmin = false,
  loadingComponent,
  fallbackComponent
}: OrganizationGuardProps) {
  const protectionOptions = requireAdmin
    ? {
        organizationId,
        organizationRole: OrganizationRole.ORG_ADMIN,
        redirectTo: `/organizations/${organizationId}`
      }
    : {
        organizationId,
        permissions: [Permission.VIEW_OWN_ORGANIZATION, Permission.VIEW_ANY_ORGANIZATION],
        redirectTo: '/dashboard'
      }

  return (
    <RouteGuard
      {...protectionOptions}
      loadingComponent={loadingComponent}
      fallbackComponent={fallbackComponent}
    >
      {children}
    </RouteGuard>
  )
}

export function AdminGuard({
  children,
  loadingComponent,
  fallbackComponent
}: Omit<OrganizationGuardProps, 'organizationId' | 'requireAdmin'>) {
  return (
    <RouteGuard
      userRoles={[UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.CAMPAIGN_ADMIN]}
      redirectTo="/dashboard"
      loadingComponent={loadingComponent}
      fallbackComponent={fallbackComponent}
    >
      {children}
    </RouteGuard>
  )
}

export function SuperAdminGuard({
  children,
  loadingComponent,
  fallbackComponent
}: Omit<OrganizationGuardProps, 'organizationId' | 'requireAdmin'>) {
  return (
    <RouteGuard
      userRole={UserRole.SUPER_ADMIN}
      redirectTo="/dashboard"
      loadingComponent={loadingComponent}
      fallbackComponent={fallbackComponent}
    >
      {children}
    </RouteGuard>
  )
}