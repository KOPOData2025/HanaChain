'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import {
  Plus,
  Search,
  MoreHorizontal,
  Eye,
  Edit,
  Trash2,
  RefreshCw
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

import organizationApi, {
  Organization,
  OrganizationFilters as ApiFilters
} from '@/lib/api/organization-api'

export default function OrganizationsPage() {
  const router = useRouter()

  // 상태 관리
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [loading, setLoading] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null)

  // 필터 상태
  const [filters, setFilters] = useState({
    page: 0,
    size: 20,
    keyword: '',
    status: undefined as 'ACTIVE' | 'INACTIVE' | undefined
  })

  // 페이지네이션 상태
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
    isLast: false,
    isFirst: true
  })

  // 단체 목록 조회
  const fetchOrganizations = async (params?: typeof filters) => {
    setLoading(true)
    try {
      console.log('📋 단체 목록 조회 시작:', params || filters)

      const apiFilters: ApiFilters = {
        status: (params || filters).status,
        search: (params || filters).keyword || undefined
      }

      const response = await organizationApi.getAllOrganizations(
        (params || filters).page,
        (params || filters).size,
        apiFilters
      )

      setOrganizations(response.content)
      setPagination({
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        currentPage: response.number,
        isLast: response.last,
        isFirst: response.first
      })

      console.log('✅ 단체 목록 조회 성공:', {
        count: response.content.length,
        totalElements: response.totalElements,
        currentPage: response.number
      })

    } catch (error) {
      console.error('❌ 단체 목록 조회 실패:', error)

      toast.error('목록 조회 실패', {
        description: '단체 목록을 불러오는데 실패했습니다'
      })
    } finally {
      setLoading(false)
    }
  }

  // 초기 데이터 로드
  useEffect(() => {
    fetchOrganizations()
  }, [])

  // 검색/필터 핸들러
  const handleSearch = (keyword: string) => {
    const newFilters = { ...filters, keyword, page: 0 }
    setFilters(newFilters)
    fetchOrganizations(newFilters)
  }

  const handleStatusFilter = (status: string) => {
    const newFilters = {
      ...filters,
      status: status === 'all' ? undefined : status as 'ACTIVE' | 'INACTIVE',
      page: 0
    }
    setFilters(newFilters)
    fetchOrganizations(newFilters)
  }

  // 페이지 변경
  const handlePageChange = (newPage: number) => {
    const newFilters = { ...filters, page: newPage }
    setFilters(newFilters)
    fetchOrganizations(newFilters)
  }

  // 단체 삭제
  const handleDeleteOrganization = async (id: number) => {
    try {
      await organizationApi.deleteOrganization(id)
      toast.success('단체가 삭제되었습니다')
      fetchOrganizations() // 목록 새로고침
    } catch (error) {
      console.error('❌ 단체 삭제 실패:', error)

      toast.error('삭제 실패', {
        description: '단체 삭제에 실패했습니다'
      })
    }
    setDeleteDialogOpen(false)
    setSelectedOrgId(null)
  }

  // 상태 뱃지 색상
  const getStatusBadgeVariant = (status: Organization['status']) => {
    switch (status) {
      case 'ACTIVE': return 'default'
      case 'INACTIVE': return 'secondary'
      case 'PENDING': return 'outline'
      default: return 'secondary'
    }
  }

  // 상태 한국어 레이블
  const STATUS_LABELS: Record<Organization['status'], string> = {
    ACTIVE: '활성',
    INACTIVE: '비활성',
    PENDING: '대기'
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8">
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              단체 관리
            </h1>
            <Button
              onClick={() => router.push('/admin/organizations/new')}
              className="flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              새 단체 등록
            </Button>
          </div>
        </div>

        {/* 단체 테이블 */}
        <Card>
          <CardHeader>
            {/* 필터 및 검색 */}
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
              {/* 검색 */}
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="단체명 또는 설명 검색..."
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
                  <SelectItem value="ACTIVE">활성</SelectItem>
                  <SelectItem value="INACTIVE">비활성</SelectItem>
                  <SelectItem value="PENDING">대기</SelectItem>
                </SelectContent>
              </Select>

              {/* 새로고침 버튼 */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchOrganizations()}
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
            ) : organizations.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                등록된 단체가 없습니다
              </div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="text-center text-base font-semibold w-16">로고</TableHead>
                      <TableHead className="text-center text-base font-semibold">단체명</TableHead>
                      <TableHead className="text-center text-base font-semibold">설명</TableHead>
                      <TableHead className="text-center text-base font-semibold">상태</TableHead>
                      <TableHead className="text-center text-base font-semibold">등록일</TableHead>
                      <TableHead className="text-center text-base font-semibold">액션</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {organizations.map((org, index) => (
                      <TableRow key={org.id} className={`animate-fade-in-up stagger-${Math.min(index + 1, 20)}`}>
                        <TableCell className="text-center">
                          <div className="flex justify-center">
                            <div className="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center overflow-hidden">
                              {org.imageUrl ? (
                                <img src={org.imageUrl} alt={org.name} className="w-full h-full object-cover" />
                              ) : (
                                <span className="text-sm font-semibold text-gray-600 dark:text-gray-300">
                                  {org.name.charAt(0)}
                                </span>
                              )}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="text-center font-medium">
                          <div className="font-semibold">{org.name}</div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm text-gray-500 truncate max-w-[800px]">
                            {org.description}
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex justify-center">
                            <Badge variant={getStatusBadgeVariant(org.status)}>
                              {STATUS_LABELS[org.status]}
                            </Badge>
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          {format(new Date(org.createdAt), 'yyyy.MM.dd', { locale: ko })}
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
                                onClick={() => router.push(`/admin/organizations/${org.id}`)}
                                className="flex items-center gap-2"
                              >
                                <Eye className="h-4 w-4" />
                                상세보기
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={() => router.push(`/admin/organizations/${org.id}/edit`)}
                                className="flex items-center gap-2"
                              >
                                <Edit className="h-4 w-4" />
                                수정하기
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                onClick={() => {
                                  setSelectedOrgId(org.id)
                                  setDeleteDialogOpen(true)
                                }}
                                className="flex items-center gap-2 text-red-600"
                              >
                                <Trash2 className="h-4 w-4" />
                                삭제하기
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
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

        {/* 삭제 확인 다이얼로그 */}
        <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>단체 삭제 확인</AlertDialogTitle>
              <AlertDialogDescription>
                선택한 단체를 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={() => {
                  if (selectedOrgId) {
                    handleDeleteOrganization(selectedOrgId)
                  }
                }}
                className="bg-red-600 hover:bg-red-700"
              >
                삭제
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}
