'use client'

import { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useAuth } from '@/lib/auth-context'
import { PermissionProvider } from '@/lib/context/permission-context'
import { OrganizationGuard } from '@/components/permissions/route-guard'
import { CanManageOrganization, CanManageMembers, CanDeleteOrganization } from '@/components/permissions/can-access'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { 
  Building2, 
  Users, 
  Calendar,
  ArrowLeft,
  Edit,
  Trash2,
  Plus,
  MoreHorizontal,
  UserPlus,
  Shield,
  User,
  Mail,
  Clock
} from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import organizationApi, { Organization } from '@/lib/api/organization-api'
import adminOrganizationApi, { OrganizationMember } from '@/lib/api/admin-organization-api'
import { UserManagementModal } from '@/components/admin/user-management-modal'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'

export default function OrganizationDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { isLoggedIn, loading: authLoading } = useAuth()
  
  const organizationId = parseInt(params.id as string)

  // 상태 변수
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [members, setMembers] = useState<OrganizationMember[]>([])
  const [loading, setLoading] = useState(true)
  const [membersLoading, setMembersLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState('info')

  // 모달 상태
  const [userModalOpen, setUserModalOpen] = useState(false)
  const [removeUserDialog, setRemoveUserDialog] = useState<{
    open: boolean
    user: OrganizationMember | null
  }>({ open: false, user: null })

  // 인증 확인
  useEffect(() => {
    if (!authLoading && !isLoggedIn) {
      router.push('/login')
      return
    }
  }, [authLoading, isLoggedIn, router])

  // 조직 데이터 가져오기
  useEffect(() => {
    if (!isLoggedIn || !organizationId) return

    const fetchOrganization = async () => {
      try {
        setLoading(true)
        setError(null)
        const data = await organizationApi.getOrganization(organizationId)
        setOrganization(data)
      } catch (error) {
        console.error('Failed to fetch organization:', error)
        setError('단체 정보를 불러오는데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }

    fetchOrganization()
  }, [organizationId, isLoggedIn])

  // 멤버 데이터 가져오기
  useEffect(() => {
    if (!isLoggedIn || !organizationId) return

    const fetchMembers = async () => {
      try {
        setMembersLoading(true)
        const response = await adminOrganizationApi.getOrganizationMembers(organizationId)
        setMembers(response.content)
      } catch (error) {
        console.error('Failed to fetch members:', error)
        // 권한이 없는 경우 조용히 실패
      } finally {
        setMembersLoading(false)
      }
    }

    fetchMembers()
  }, [organizationId, isLoggedIn])

  const handleBack = () => {
    router.push('/admin/organizations')
  }

  const handleEdit = () => {
    router.push(`/admin/organizations/${organizationId}/edit`)
  }

  const handleAddUser = async (userId: number, role: 'ORG_ADMIN' | 'ORG_MEMBER') => {
    try {
      await adminOrganizationApi.addMemberToOrganization(organizationId, {
        userId,
        role
      })
      // 멤버 목록 새로고침
      const response = await adminOrganizationApi.getOrganizationMembers(organizationId)
      setMembers(response.content)
      setUserModalOpen(false)
    } catch (error) {
      console.error('Failed to add user:', error)
      throw error
    }
  }

  const handleRoleChange = async (userId: number, newRole: 'ORG_ADMIN' | 'ORG_MEMBER') => {
    try {
      await adminOrganizationApi.updateMemberRole(organizationId, userId, { role: newRole })
      // 멤버 목록 새로고침
      const response = await adminOrganizationApi.getOrganizationMembers(organizationId)
      setMembers(response.content)
    } catch (error) {
      console.error('Failed to update role:', error)
    }
  }

  const handleRemoveUser = async (user: OrganizationMember) => {
    try {
      await adminOrganizationApi.removeMemberFromOrganization(organizationId, user.userId)
      // 멤버 목록 새로고침
      const response = await adminOrganizationApi.getOrganizationMembers(organizationId)
      setMembers(response.content)
      setRemoveUserDialog({ open: false, user: null })
    } catch (error) {
      console.error('Failed to remove user:', error)
    }
  }

  const getRoleIcon = (role: string) => {
    return role === 'ORG_ADMIN' ? <Shield className="h-4 w-4" /> : <User className="h-4 w-4" />
  }

  const getRoleBadge = (role: string) => {
    return role === 'ORG_ADMIN' ? (
      <Badge variant="default" className="gap-1">
        <Shield className="h-3 w-3" />
        관리자
      </Badge>
    ) : (
      <Badge variant="secondary" className="gap-1">
        <User className="h-3 w-3" />
        멤버
      </Badge>
    )
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  }

  // 인증 확인 중 로딩 표시
  if (authLoading || loading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        </div>
      </div>
    )
  }

  if (error || !organization) {
    return (
      <div className="container mx-auto py-8 px-4">
        <div className="flex flex-col items-center justify-center min-h-[400px] space-y-4">
          <Building2 className="h-16 w-16 text-muted-foreground" />
          <div className="text-center">
            <h2 className="text-2xl font-semibold">단체를 찾을 수 없습니다</h2>
            <p className="text-muted-foreground mt-2">
              {error || '요청하신 단체가 존재하지 않거나 접근 권한이 없습니다.'}
            </p>
          </div>
          <Button onClick={handleBack} variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            돌아가기
          </Button>
        </div>
      </div>
    )
  }

  return (
    <PermissionProvider>
      <OrganizationGuard organizationId={organizationId}>
        <div className="container mx-auto py-8 px-4 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button variant="ghost" size="sm" onClick={handleBack}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            돌아가기
          </Button>
          <div className="h-8 w-px bg-border" />
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-2">
              <Building2 className="h-6 w-6" />
              {organization.name}
            </h1>
            <p className="text-sm text-muted-foreground">
              단체 상세 정보 및 관리
            </p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <CanManageOrganization organizationId={organizationId}>
            <Button variant="outline" onClick={handleEdit}>
              <Edit className="h-4 w-4 mr-2" />
              수정
            </Button>
          </CanManageOrganization>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <CanManageOrganization organizationId={organizationId}>
                <DropdownMenuItem onClick={handleEdit}>
                  <Edit className="h-4 w-4 mr-2" />
                  정보 수정
                </DropdownMenuItem>
              </CanManageOrganization>
              <CanDeleteOrganization organizationId={organizationId}>
                <DropdownMenuSeparator />
                <DropdownMenuItem className="text-red-600">
                  <Trash2 className="h-4 w-4 mr-2" />
                  단체 삭제
                </DropdownMenuItem>
              </CanDeleteOrganization>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Content Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="info">기본 정보</TabsTrigger>
          <TabsTrigger value="members">소속 유저</TabsTrigger>
          <TabsTrigger value="campaigns">관련 캠페인</TabsTrigger>
        </TabsList>

        {/* 기본 정보 탭 */}
        <TabsContent value="info" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>기본 정보</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* 이미지 및 기본 정보 */}
              <div className="flex flex-col md:flex-row gap-6">
                {organization.imageUrl && (
                  <div className="flex-shrink-0">
                    <Avatar className="h-32 w-32">
                      <AvatarImage src={organization.imageUrl} alt={organization.name} />
                      <AvatarFallback>
                        <Building2 className="h-16 w-16" />
                      </AvatarFallback>
                    </Avatar>
                  </div>
                )}
                <div className="flex-1 space-y-4">
                  <div>
                    <h3 className="text-xl font-semibold">{organization.name}</h3>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge variant={organization.status === 'ACTIVE' ? 'default' : 'secondary'}>
                        {organization.status === 'ACTIVE' ? '활성' : '비활성'}
                      </Badge>
                    </div>
                  </div>
                  <div className="prose prose-sm max-w-none">
                    <p>{organization.description}</p>
                  </div>
                </div>
              </div>

              {/* 통계 정보 */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-6 border-t">
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">{organization.memberCount || 0}</div>
                  <div className="text-sm text-muted-foreground">총 멤버</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-600">{organization.adminCount || 0}</div>
                  <div className="text-sm text-muted-foreground">관리자</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-purple-600">{organization.activeCampaignCount || 0}</div>
                  <div className="text-sm text-muted-foreground">진행 중 캠페인</div>
                </div>
                <div className="text-center">
                  <div className="text-sm text-muted-foreground">등록일</div>
                  <div className="text-sm font-medium">{formatDate(organization.createdAt)}</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* 소속 유저 탭 */}
        <TabsContent value="members" className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <Users className="h-5 w-5" />
                  소속 유저 ({members.length}명)
                </CardTitle>
                <CanManageMembers organizationId={organizationId}>
                  <Button onClick={() => setUserModalOpen(true)} size="sm">
                    <UserPlus className="h-4 w-4 mr-2" />
                    유저 추가
                  </Button>
                </CanManageMembers>
              </div>
            </CardHeader>
            <CardContent>
              {membersLoading ? (
                <div className="flex items-center justify-center py-8">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary" />
                </div>
              ) : members.length === 0 ? (
                <div className="text-center py-8">
                  <Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">소속된 유저가 없습니다.</p>
                  <CanManageMembers organizationId={organizationId}>
                    <Button 
                      onClick={() => setUserModalOpen(true)} 
                      variant="outline" 
                      size="sm" 
                      className="mt-4"
                    >
                      첫 번째 유저 추가하기
                    </Button>
                  </CanManageMembers>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>사용자</TableHead>
                      <TableHead>역할</TableHead>
                      <TableHead>가입일</TableHead>
                      <TableHead>상태</TableHead>
                      <TableHead className="text-right">작업</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {members.map((member) => (
                      <TableRow key={member.id}>
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <Avatar className="h-8 w-8">
                              <AvatarFallback>
                                {member.fullName.charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            <div>
                              <div className="font-medium">{member.fullName}</div>
                              <div className="text-sm text-muted-foreground flex items-center gap-1">
                                <Mail className="h-3 w-3" />
                                {member.email}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          {getRoleBadge(member.role)}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-1 text-sm text-muted-foreground">
                            <Clock className="h-3 w-3" />
                            {formatDate(member.joinedAt)}
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant={member.active ? 'default' : 'secondary'}>
                            {member.active ? '활성' : '비활성'}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="sm">
                                <MoreHorizontal className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <CanManageMembers organizationId={organizationId}>
                                <DropdownMenuItem 
                                  onClick={() => handleRoleChange(
                                    member.userId, 
                                    member.role === 'ORG_ADMIN' ? 'ORG_MEMBER' : 'ORG_ADMIN'
                                  )}
                                >
                                  {getRoleIcon(member.role === 'ORG_ADMIN' ? 'ORG_MEMBER' : 'ORG_ADMIN')}
                                  <span className="ml-2">
                                    {member.role === 'ORG_ADMIN' ? '멤버로 변경' : '관리자로 승격'}
                                  </span>
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem 
                                  onClick={() => setRemoveUserDialog({ open: true, user: member })}
                                  className="text-red-600"
                                >
                                  <Trash2 className="h-4 w-4 mr-2" />
                                  단체에서 제거
                                </DropdownMenuItem>
                              </CanManageMembers>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* 관련 캠페인 탭 */}
        <TabsContent value="campaigns" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>관련 캠페인</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <p className="text-muted-foreground">캠페인 목록 기능은 곧 추가될 예정입니다.</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 유저 추가 모달 */}
      <UserManagementModal
        open={userModalOpen}
        onOpenChange={setUserModalOpen}
        onAddUser={handleAddUser}
        organizationId={organizationId}
      />

      {/* 유저 제거 확인 다이얼로그 */}
      <ConfirmDialog
        open={removeUserDialog.open}
        onOpenChange={(open) => setRemoveUserDialog({ open, user: null })}
        onConfirm={() => removeUserDialog.user && handleRemoveUser(removeUserDialog.user)}
        title="유저 제거 확인"
        description={`정말로 ${removeUserDialog.user?.fullName}님을 이 단체에서 제거하시겠습니까?`}
        confirmText="제거"
        cancelText="취소"
        variant="destructive"
      />
        </div>
      </OrganizationGuard>
    </PermissionProvider>
  )
}