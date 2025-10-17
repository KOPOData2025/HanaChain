'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/lib/auth-context'
import { usePermissions, Permission, UserRole, OrganizationRole } from '@/lib/context/permission-context'

interface RouteProtectionOptions {
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
  onAccessDenied?: () => void
}

/**
 * Hook for protecting routes based on authentication and permissions
 */
export function useProtectedRoute(options: RouteProtectionOptions = {}) {
  const {
    requireAuth = true,
    permission,
    permissions,
    requireAll = false,
    userRole,
    userRoles,
    organizationId,
    organizationRole,
    organizationRoles,
    redirectTo = '/login',
    onAccessDenied
  } = options

  const router = useRouter()
  const { isLoggedIn, loading } = useAuth()
  const {
    hasPermission,
    hasPermissionInOrganization,
    getUserRole,
    getOrganizationRole
  } = usePermissions()

  const checkAccess = (): boolean => {
    // 로딩 중이면 확인 건너뛰기
    if (loading) return true

    // 인증 요구사항 확인
    if (requireAuth && !isLoggedIn) {
      return false
    }

    // 로그인하지 않았고 인증이 필요하지 않으면 접근 허용
    if (!isLoggedIn && !requireAuth) {
      return true
    }

    // 사용자 역할 요구사항 확인
    if (userRole || userRoles) {
      const currentUserRole = getUserRole()
      if (!currentUserRole) return false

      if (userRole && currentUserRole !== userRole) return false
      if (userRoles && !userRoles.includes(currentUserRole)) return false
    }

    // 조직 역할 요구사항 확인
    if ((organizationRole || organizationRoles) && organizationId) {
      const currentOrgRole = getOrganizationRole(organizationId)
      if (!currentOrgRole) return false

      if (organizationRole && currentOrgRole !== organizationRole) return false
      if (organizationRoles && !organizationRoles.includes(currentOrgRole)) return false
    }

    // 권한 요구사항 확인
    if (permission || permissions) {
      const permissionsToCheck = permissions || (permission ? [permission] : [])

      if (organizationId) {
        // 조직별 권한 확인
        if (requireAll) {
          return permissionsToCheck.every(p => hasPermissionInOrganization(organizationId, p))
        } else {
          return permissionsToCheck.some(p => hasPermissionInOrganization(organizationId, p))
        }
      } else {
        // 시스템 수준 권한 확인
        if (requireAll) {
          return permissionsToCheck.every(p => hasPermission(p))
        } else {
          return permissionsToCheck.some(p => hasPermission(p))
        }
      }
    }

    return true
  }

  useEffect(() => {
    if (loading) return

    const hasAccess = checkAccess()
    
    if (!hasAccess) {
      if (onAccessDenied) {
        onAccessDenied()
      } else {
        router.replace(redirectTo)
      }
    }
  }, [
    loading,
    isLoggedIn,
    permission,
    permissions,
    requireAll,
    userRole,
    userRoles,
    organizationId,
    organizationRole,
    organizationRoles,
    redirectTo,
    onAccessDenied,
    router,
    hasPermission,
    hasPermissionInOrganization,
    getUserRole,
    getOrganizationRole
  ])

  return {
    hasAccess: !loading && checkAccess(),
    loading,
    isLoggedIn
  }
}

/**
 * Hook for organization-specific route protection
 */
export function useOrganizationRoute(
  organizationId: number,
  options: Omit<RouteProtectionOptions, 'organizationId'> = {}
) {
  return useProtectedRoute({
    ...options,
    organizationId,
    redirectTo: options.redirectTo || '/dashboard'
  })
}

/**
 * Hook for admin-only routes
 */
export function useAdminRoute(options: Omit<RouteProtectionOptions, 'userRoles'> = {}) {
  return useProtectedRoute({
    ...options,
    userRoles: [UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.CAMPAIGN_ADMIN],
    redirectTo: options.redirectTo || '/dashboard'
  })
}

/**
 * Hook for organization admin routes
 */
export function useOrganizationAdminRoute(
  organizationId: number,
  options: Omit<RouteProtectionOptions, 'organizationId' | 'organizationRole'> = {}
) {
  return useProtectedRoute({
    ...options,
    organizationId,
    organizationRole: OrganizationRole.ORG_ADMIN,
    redirectTo: options.redirectTo || `/organizations/${organizationId}`
  })
}