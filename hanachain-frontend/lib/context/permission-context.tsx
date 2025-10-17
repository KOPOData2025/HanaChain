'use client'

import React, { createContext, useContext, useCallback } from 'react'
import { useAuth } from '../auth-context'

// 백엔드 권한과 일치하는 권한 타입
export enum Permission {
  // 조직 관리 권한
  CREATE_ORGANIZATION = 'CREATE_ORGANIZATION',
  UPDATE_ANY_ORGANIZATION = 'UPDATE_ANY_ORGANIZATION',
  UPDATE_OWN_ORGANIZATION = 'UPDATE_OWN_ORGANIZATION',
  DELETE_ANY_ORGANIZATION = 'DELETE_ANY_ORGANIZATION',
  DELETE_OWN_ORGANIZATION = 'DELETE_OWN_ORGANIZATION',
  VIEW_ANY_ORGANIZATION = 'VIEW_ANY_ORGANIZATION',
  VIEW_OWN_ORGANIZATION = 'VIEW_OWN_ORGANIZATION',

  // 멤버 관리 권한
  MANAGE_ANY_ORGANIZATION_MEMBERS = 'MANAGE_ANY_ORGANIZATION_MEMBERS',
  MANAGE_OWN_ORGANIZATION_MEMBERS = 'MANAGE_OWN_ORGANIZATION_MEMBERS',
  VIEW_ANY_ORGANIZATION_MEMBERS = 'VIEW_ANY_ORGANIZATION_MEMBERS',
  VIEW_OWN_ORGANIZATION_MEMBERS = 'VIEW_OWN_ORGANIZATION_MEMBERS',

  // 캠페인 관리 권한
  CREATE_CAMPAIGN = 'CREATE_CAMPAIGN',
  UPDATE_ANY_CAMPAIGN = 'UPDATE_ANY_CAMPAIGN',
  UPDATE_OWN_CAMPAIGN = 'UPDATE_OWN_CAMPAIGN',
  DELETE_ANY_CAMPAIGN = 'DELETE_ANY_CAMPAIGN',
  DELETE_OWN_CAMPAIGN = 'DELETE_OWN_CAMPAIGN',
  VIEW_ANY_CAMPAIGN = 'VIEW_ANY_CAMPAIGN',
  VIEW_PUBLIC_CAMPAIGN = 'VIEW_PUBLIC_CAMPAIGN',

  // 사용자 관리 권한
  MANAGE_USERS = 'MANAGE_USERS',
  VIEW_USER_DETAILS = 'VIEW_USER_DETAILS',

  // 시스템 권한
  SYSTEM_ADMIN = 'SYSTEM_ADMIN',
  AUDIT_LOG_ACCESS = 'AUDIT_LOG_ACCESS',

  // 역할 관리 권한
  ASSIGN_ORGANIZATION_ROLES = 'ASSIGN_ORGANIZATION_ROLES',
  ASSIGN_SYSTEM_ROLES = 'ASSIGN_SYSTEM_ROLES'
}

// 백엔드 역할과 일치하는 사용자 역할
export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  SUPER_ADMIN = 'SUPER_ADMIN',
  CAMPAIGN_ADMIN = 'CAMPAIGN_ADMIN'
}

// 백엔드 역할과 일치하는 조직 역할
export enum OrganizationRole {
  ORG_ADMIN = 'ORG_ADMIN',
  ORG_MEMBER = 'ORG_MEMBER'
}

interface PermissionContextType {
  hasPermission: (permission: Permission) => boolean
  hasPermissionInOrganization: (organizationId: number, permission: Permission) => boolean
  canAccessOrganization: (organizationId: number) => boolean
  canManageOrganization: (organizationId: number) => boolean
  canManageOrganizationMembers: (organizationId: number) => boolean
  isSuperAdmin: () => boolean
  isCampaignAdmin: () => boolean
  isSystemLevelAdmin: () => boolean
  getUserRole: () => UserRole | null
  getOrganizationRole: (organizationId: number) => OrganizationRole | null
}

const PermissionContext = createContext<PermissionContextType | undefined>(undefined)

// 역할 권한 매핑 (프론트엔드용 간소화 버전)
const ROLE_PERMISSIONS: Record<UserRole, Permission[]> = {
  [UserRole.SUPER_ADMIN]: Object.values(Permission), // 슈퍼 관리자는 모든 권한 보유
  [UserRole.ADMIN]: [
    Permission.VIEW_ANY_ORGANIZATION,
    Permission.VIEW_ANY_ORGANIZATION_MEMBERS,
    Permission.VIEW_ANY_CAMPAIGN,
    Permission.UPDATE_ANY_CAMPAIGN,
    Permission.DELETE_ANY_CAMPAIGN,
    Permission.MANAGE_USERS,
    Permission.VIEW_USER_DETAILS,
    Permission.SYSTEM_ADMIN,
    Permission.AUDIT_LOG_ACCESS,
    Permission.VIEW_PUBLIC_CAMPAIGN,
    Permission.CREATE_CAMPAIGN,
    Permission.UPDATE_OWN_CAMPAIGN,
    Permission.DELETE_OWN_CAMPAIGN
  ],
  [UserRole.CAMPAIGN_ADMIN]: [
    Permission.CREATE_ORGANIZATION,
    Permission.UPDATE_ANY_ORGANIZATION,
    Permission.DELETE_ANY_ORGANIZATION,
    Permission.VIEW_ANY_ORGANIZATION,
    Permission.MANAGE_ANY_ORGANIZATION_MEMBERS,
    Permission.VIEW_ANY_ORGANIZATION_MEMBERS,
    Permission.CREATE_CAMPAIGN,
    Permission.UPDATE_ANY_CAMPAIGN,
    Permission.DELETE_ANY_CAMPAIGN,
    Permission.VIEW_ANY_CAMPAIGN,
    Permission.VIEW_PUBLIC_CAMPAIGN,
    Permission.VIEW_USER_DETAILS,
    Permission.ASSIGN_ORGANIZATION_ROLES,
    Permission.UPDATE_OWN_CAMPAIGN,
    Permission.DELETE_OWN_CAMPAIGN
  ],
  [UserRole.USER]: [
    Permission.CREATE_CAMPAIGN,
    Permission.UPDATE_OWN_CAMPAIGN,
    Permission.DELETE_OWN_CAMPAIGN,
    Permission.VIEW_PUBLIC_CAMPAIGN
  ]
}

const ORG_ROLE_PERMISSIONS: Record<OrganizationRole, Permission[]> = {
  [OrganizationRole.ORG_ADMIN]: [
    Permission.UPDATE_OWN_ORGANIZATION,
    Permission.DELETE_OWN_ORGANIZATION,
    Permission.VIEW_OWN_ORGANIZATION,
    Permission.MANAGE_OWN_ORGANIZATION_MEMBERS,
    Permission.VIEW_OWN_ORGANIZATION_MEMBERS,
    Permission.CREATE_CAMPAIGN,
    Permission.UPDATE_OWN_CAMPAIGN,
    Permission.DELETE_OWN_CAMPAIGN,
    Permission.VIEW_PUBLIC_CAMPAIGN,
    Permission.ASSIGN_ORGANIZATION_ROLES
  ],
  [OrganizationRole.ORG_MEMBER]: [
    Permission.VIEW_OWN_ORGANIZATION,
    Permission.VIEW_OWN_ORGANIZATION_MEMBERS,
    Permission.CREATE_CAMPAIGN,
    Permission.UPDATE_OWN_CAMPAIGN,
    Permission.DELETE_OWN_CAMPAIGN,
    Permission.VIEW_PUBLIC_CAMPAIGN
  ]
}

interface PermissionProviderProps {
  children: React.ReactNode
  userOrganizations?: { organizationId: number; role: OrganizationRole }[]
}

export function PermissionProvider({ children, userOrganizations = [] }: PermissionProviderProps) {
  const { user } = useAuth()

  const getUserRole = useCallback((): UserRole | null => {
    if (!user) return null
    return user.role as UserRole
  }, [user])

  const hasPermission = useCallback((permission: Permission): boolean => {
    const userRole = getUserRole()
    if (!userRole) return false
    
    return ROLE_PERMISSIONS[userRole]?.includes(permission) || false
  }, [getUserRole])

  const getOrganizationRole = useCallback((organizationId: number): OrganizationRole | null => {
    const orgMembership = userOrganizations.find(org => org.organizationId === organizationId)
    return orgMembership?.role || null
  }, [userOrganizations])

  const hasPermissionInOrganization = useCallback((organizationId: number, permission: Permission): boolean => {
    // 먼저 시스템 수준 권한 확인
    if (hasPermission(permission)) {
      return true
    }

    // 조직 수준 권한 확인
    const orgRole = getOrganizationRole(organizationId)
    if (!orgRole) return false

    return ORG_ROLE_PERMISSIONS[orgRole]?.includes(permission) || false
  }, [hasPermission, getOrganizationRole])

  const canAccessOrganization = useCallback((organizationId: number): boolean => {
    const userRole = getUserRole()
    if (!userRole) return false

    // 슈퍼 관리자와 캠페인 관리자는 모든 조직에 접근 가능
    if (userRole === UserRole.SUPER_ADMIN || userRole === UserRole.CAMPAIGN_ADMIN) {
      return true
    }

    // 사용자가 조직의 멤버인지 확인
    return userOrganizations.some(org => org.organizationId === organizationId)
  }, [getUserRole, userOrganizations])

  const canManageOrganization = useCallback((organizationId: number): boolean => {
    return hasPermissionInOrganization(organizationId, Permission.UPDATE_OWN_ORGANIZATION) ||
           hasPermission(Permission.UPDATE_ANY_ORGANIZATION)
  }, [hasPermissionInOrganization, hasPermission])

  const canManageOrganizationMembers = useCallback((organizationId: number): boolean => {
    return hasPermissionInOrganization(organizationId, Permission.MANAGE_OWN_ORGANIZATION_MEMBERS) ||
           hasPermission(Permission.MANAGE_ANY_ORGANIZATION_MEMBERS)
  }, [hasPermissionInOrganization, hasPermission])

  const isSuperAdmin = useCallback((): boolean => {
    return getUserRole() === UserRole.SUPER_ADMIN
  }, [getUserRole])

  const isCampaignAdmin = useCallback((): boolean => {
    return getUserRole() === UserRole.CAMPAIGN_ADMIN
  }, [getUserRole])

  const isSystemLevelAdmin = useCallback((): boolean => {
    const role = getUserRole()
    return role === UserRole.SUPER_ADMIN || role === UserRole.ADMIN || role === UserRole.CAMPAIGN_ADMIN
  }, [getUserRole])

  const value: PermissionContextType = {
    hasPermission,
    hasPermissionInOrganization,
    canAccessOrganization,
    canManageOrganization,
    canManageOrganizationMembers,
    isSuperAdmin,
    isCampaignAdmin,
    isSystemLevelAdmin,
    getUserRole,
    getOrganizationRole
  }

  return (
    <PermissionContext.Provider value={value}>
      {children}
    </PermissionContext.Provider>
  )
}

export function usePermissions() {
  const context = useContext(PermissionContext)
  if (context === undefined) {
    throw new Error('usePermissions must be used within a PermissionProvider')
  }
  return context
}