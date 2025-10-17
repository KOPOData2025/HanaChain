'use client'

import React from 'react'
import { usePermissions, Permission, UserRole, OrganizationRole } from '@/lib/context/permission-context'

interface CanAccessProps {
  children: React.ReactNode
  permission?: Permission
  permissions?: Permission[]
  requireAll?: boolean
  userRole?: UserRole
  userRoles?: UserRole[]
  organizationId?: number
  organizationRole?: OrganizationRole
  organizationRoles?: OrganizationRole[]
  fallback?: React.ReactNode
  inverse?: boolean
}

/**
 * Conditional rendering component that shows content based on user permissions
 */
export function CanAccess({
  children,
  permission,
  permissions,
  requireAll = false,
  userRole,
  userRoles,
  organizationId,
  organizationRole,
  organizationRoles,
  fallback = null,
  inverse = false
}: CanAccessProps) {
  const {
    hasPermission,
    hasPermissionInOrganization,
    getUserRole,
    getOrganizationRole
  } = usePermissions()

  const checkAccess = (): boolean => {
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

    // 특정 요구사항이 없으면 기본적으로 true
    return true
  }

  const hasAccess = checkAccess()
  const shouldShow = inverse ? !hasAccess : hasAccess

  return shouldShow ? <>{children}</> : <>{fallback}</>
}

// 일반적인 사용 사례를 위한 편의 컴포넌트

interface CanViewProps {
  children: React.ReactNode
  organizationId?: number
  fallback?: React.ReactNode
}

export function CanViewOrganization({ children, organizationId, fallback }: CanViewProps) {
  return (
    <CanAccess
      permissions={[Permission.VIEW_OWN_ORGANIZATION, Permission.VIEW_ANY_ORGANIZATION]}
      organizationId={organizationId}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}

export function CanManageOrganization({ children, organizationId, fallback }: CanViewProps) {
  return (
    <CanAccess
      permissions={[Permission.UPDATE_OWN_ORGANIZATION, Permission.UPDATE_ANY_ORGANIZATION]}
      organizationId={organizationId}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}

export function CanManageMembers({ children, organizationId, fallback }: CanViewProps) {
  return (
    <CanAccess
      permissions={[Permission.MANAGE_OWN_ORGANIZATION_MEMBERS, Permission.MANAGE_ANY_ORGANIZATION_MEMBERS]}
      organizationId={organizationId}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}

export function CanDeleteOrganization({ children, organizationId, fallback }: CanViewProps) {
  return (
    <CanAccess
      permissions={[Permission.DELETE_OWN_ORGANIZATION, Permission.DELETE_ANY_ORGANIZATION]}
      organizationId={organizationId}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}

export function CanCreateOrganization({ children, fallback }: Omit<CanViewProps, 'organizationId'>) {
  return (
    <CanAccess
      permission={Permission.CREATE_ORGANIZATION}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}

export function IsSystemAdmin({ children, fallback }: Omit<CanViewProps, 'organizationId'>) {
  return (
    <CanAccess
      userRoles={[UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.CAMPAIGN_ADMIN]}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}

export function IsOrganizationAdmin({ children, organizationId, fallback }: CanViewProps) {
  return (
    <CanAccess
      organizationRole={OrganizationRole.ORG_ADMIN}
      organizationId={organizationId}
      fallback={fallback}
    >
      {children}
    </CanAccess>
  )
}