'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import {
  Search,
  MoreHorizontal,
  Eye,
  Edit,
  Ban,
  RefreshCw,
  CheckCircle,
  XCircle
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

import userApi, { UserProfile } from '@/lib/api/user-api'

export default function UsersPage() {
  const router = useRouter()

  // 상태 관리
  const [users, setUsers] = useState<UserProfile[]>([])
  const [loading, setLoading] = useState(false)
  const [statusDialogOpen, setStatusDialogOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null)
  const [newStatus, setNewStatus] = useState<'active' | 'inactive' | 'suspended'>('active')

  // 필터 상태
  const [filters, setFilters] = useState({
    page: 0,
    size: 20,
    keyword: '',
    status: undefined as 'active' | 'inactive' | 'suspended' | undefined
  })

  // 페이지네이션 상태
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
    isLast: false,
    isFirst: true
  })

  // 사용자 목록 조회
  const fetchUsers = async (params?: typeof filters) => {
    setLoading(true)
    try {
      console.log('📋 사용자 목록 조회 시작:', params || filters)

      const response = await userApi.getUserList(
        (params || filters).keyword || undefined,
        (params || filters).page,
        (params || filters).size
      )

      setUsers(response.content)
      setPagination({
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        currentPage: response.number,
        isLast: response.last,
        isFirst: response.first
      })

      console.log('✅ 사용자 목록 조회 성공:', {
        count: response.content.length,
        totalElements: response.totalElements,
        currentPage: response.number
      })

    } catch (error) {
      console.error('❌ 사용자 목록 조회 실패:', error)

      toast.error('목록 조회 실패', {
        description: '사용자 목록을 불러오는데 실패했습니다'
      })
    } finally {
      setLoading(false)
    }
  }

  // 초기 데이터 로드
  useEffect(() => {
    fetchUsers()
  }, [])

  // 검색/필터 핸들러
  const handleSearch = (keyword: string) => {
    const newFilters = { ...filters, keyword, page: 0 }
    setFilters(newFilters)
    fetchUsers(newFilters)
  }

  const handleStatusFilter = (status: string) => {
    const newFilters = {
      ...filters,
      status: status === 'all' ? undefined : status as 'active' | 'inactive' | 'suspended',
      page: 0
    }
    setFilters(newFilters)
    fetchUsers(newFilters)
  }

  // 페이지 변경
  const handlePageChange = (newPage: number) => {
    const newFilters = { ...filters, page: newPage }
    setFilters(newFilters)
    fetchUsers(newFilters)
  }

  // 사용자 상태 변경
  const handleStatusChange = async (user: UserProfile, status: 'active' | 'inactive' | 'suspended') => {
    try {
      await userApi.updateUserStatus(user.id, status)
      toast.success('상태가 변경되었습니다')
      fetchUsers() // 목록 새로고침
    } catch (error) {
      console.error('❌ 사용자 상태 변경 실패:', error)

      toast.error('상태 변경 실패', {
        description: '사용자 상태를 변경하는데 실패했습니다'
      })
    }
    setStatusDialogOpen(false)
    setSelectedUser(null)
  }

  // 상태 뱃지 색상
  const getStatusBadgeVariant = (status: UserProfile['status']) => {
    switch (status) {
      case 'active': return 'default'
      case 'inactive': return 'secondary'
      case 'suspended': return 'destructive'
      default: return 'secondary'
    }
  }

  // 상태 한국어 레이블
  const STATUS_LABELS: Record<UserProfile['status'], string> = {
    active: '활성',
    inactive: '비활성',
    suspended: '정지'
  }

  // 상태 아이콘
  const STATUS_ICONS = {
    active: CheckCircle,
    inactive: XCircle,
    suspended: Ban
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8">
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              사용자 관리
            </h1>
          </div>
        </div>

        {/* 사용자 테이블 */}
        <Card>
          <CardHeader>
            {/* 필터 및 검색 */}
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
              {/* 검색 */}
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="이메일 또는 닉네임 검색..."
                  value={filters.keyword || ''}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="pl-10 h-9"
                />
              </div>

              {/* 상태 필터 */}
              <Select
                value={filters.status || 'all'}
                onValueChange={handleStatusFilter}
              >
                <SelectTrigger className="h-9 md:w-40">
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">모든 상태</SelectItem>
                  <SelectItem value="active">활성</SelectItem>
                  <SelectItem value="inactive">비활성</SelectItem>
                  <SelectItem value="suspended">정지</SelectItem>
                </SelectContent>
              </Select>

              {/* 새로고침 버튼 */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchUsers()}
                disabled={loading}
                className="flex items-center gap-2 h-9"
              >
                <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                새로고침
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex justify-center items-center py-8">
                <RefreshCw className="h-6 w-6 animate-spin mr-2" />
                불러오는 중...
              </div>
            ) : users.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                등록된 사용자가 없습니다
              </div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="text-center text-base font-semibold w-16">프로필</TableHead>
                      <TableHead className="text-center text-base font-semibold">이메일</TableHead>
                      <TableHead className="text-center text-base font-semibold">닉네임</TableHead>
                      <TableHead className="text-center text-base font-semibold">이름</TableHead>
                      <TableHead className="text-center text-base font-semibold">가입일</TableHead>
                      <TableHead className="text-center text-base font-semibold">상태</TableHead>
                      <TableHead className="text-center text-base font-semibold">액션</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {users.map((user, index) => {
                      const StatusIcon = STATUS_ICONS[user.status]

                      return (
                        <TableRow key={user.id} className={`animate-fade-in-up stagger-${Math.min(index + 1, 20)}`}>
                          <TableCell className="text-center">
                            <div className="flex justify-center">
                              <div className="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center overflow-hidden">
                                {user.profileImage ? (
                                  <img src={user.profileImage} alt={user.nickname || user.email} className="w-full h-full object-cover" />
                                ) : (
                                  <span className="text-sm font-semibold text-gray-600 dark:text-gray-300">
                                    {(user.nickname || user.email).charAt(0).toUpperCase()}
                                  </span>
                                )}
                              </div>
                            </div>
                          </TableCell>
                          <TableCell className="text-center">
                            <div className="font-medium">{user.email}</div>
                            {user.emailVerified && (
                              <Badge variant="outline" className="text-xs mt-1">
                                인증됨
                              </Badge>
                            )}
                          </TableCell>
                          <TableCell className="text-center">
                            {user.nickname || '-'}
                          </TableCell>
                          <TableCell className="text-center">
                            {user.name || '-'}
                          </TableCell>
                          <TableCell className="text-center">
                            {format(new Date(user.createdAt), 'yyyy.MM.dd', { locale: ko })}
                          </TableCell>
                          <TableCell className="text-center">
                            <div className="flex justify-center">
                              <Button
                                variant="ghost"
                                size="sm"
                                className="gap-1"
                                onClick={() => {
                                  setSelectedUser(user)
                                  setNewStatus(user.status)
                                  setStatusDialogOpen(true)
                                }}
                              >
                                <StatusIcon className={`h-3 w-3 ${
                                  user.status === 'active' ? 'text-green-600' :
                                  user.status === 'suspended' ? 'text-red-600' :
                                  'text-gray-600'
                                }`} />
                                <Badge variant={getStatusBadgeVariant(user.status)}>
                                  {STATUS_LABELS[user.status]}
                                </Badge>
                              </Button>
                            </div>
                          </TableCell>
                          <TableCell className="text-right">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" className="h-8 w-8 p-0">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                  onClick={() => router.push(`/admin/users/${user.id}`)}
                                  className="flex items-center gap-2"
                                >
                                  <Eye className="h-4 w-4" />
                                  상세보기
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  onClick={() => router.push(`/admin/users/${user.id}/edit`)}
                                  className="flex items-center gap-2"
                                >
                                  <Edit className="h-4 w-4" />
                                  수정하기
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  onClick={() => {
                                    setSelectedUser(user)
                                    setNewStatus('suspended')
                                    setStatusDialogOpen(true)
                                  }}
                                  className="flex items-center gap-2 text-red-600"
                                  disabled={user.status === 'suspended'}
                                >
                                  <Ban className="h-4 w-4" />
                                  계정 정지
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>

                {/* 페이지네이션 */}
                {pagination.totalPages > 1 && (
                  <div className="flex justify-center items-center gap-2 mt-6">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={pagination.isFirst}
                      onClick={() => handlePageChange(0)}
                    >
                      처음
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={pagination.isFirst}
                      onClick={() => handlePageChange(pagination.currentPage - 1)}
                    >
                      이전
                    </Button>

                    <span className="px-4 py-2 text-sm">
                      {pagination.currentPage + 1} / {pagination.totalPages}
                    </span>

                    <Button
                      variant="outline"
                      size="sm"
                      disabled={pagination.isLast}
                      onClick={() => handlePageChange(pagination.currentPage + 1)}
                    >
                      다음
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={pagination.isLast}
                      onClick={() => handlePageChange(pagination.totalPages - 1)}
                    >
                      마지막
                    </Button>
                  </div>
                )}
              </>
            )}
          </CardContent>
        </Card>

        {/* 상태 변경 확인 다이얼로그 */}
        <AlertDialog open={statusDialogOpen} onOpenChange={setStatusDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>사용자 상태 변경</AlertDialogTitle>
              <AlertDialogDescription>
                <span className="font-medium">{selectedUser?.email}</span>의 상태를{' '}
                <span className="font-medium">{STATUS_LABELS[newStatus]}</span>(으)로 변경하시겠습니까?
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={() => {
                  if (selectedUser) {
                    handleStatusChange(selectedUser, newStatus)
                  }
                }}
                className={newStatus === 'suspended' ? 'bg-red-600 hover:bg-red-700' : ''}
              >
                변경
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}
