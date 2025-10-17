'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { 
  Plus, 
  Search, 
  Filter, 
  MoreHorizontal,
  Eye,
  Edit,
  Trash2,
  RefreshCw
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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

import { campaignApi } from '@/lib/api/campaign-api'
import { ApiError } from '@/lib/api/client'
import { 
  CampaignCategory, 
  CampaignStatus,
  CATEGORY_LABELS, 
  STATUS_LABELS,
  AdminCampaignListParams 
} from '@/types/admin'
import { CampaignListItem, SpringPageResponse, BlockchainStatus } from '@/types/donation'

export default function AdminCampaignListPage() {
  const router = useRouter()
  
  // 상태 관리
  const [campaigns, setCampaigns] = useState<CampaignListItem[]>([])
  const [loading, setLoading] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedCampaignId, setSelectedCampaignId] = useState<number | null>(null)

  // 필터 상태
  const [filters, setFilters] = useState<AdminCampaignListParams>({
    page: 0,
    size: 20,
    keyword: '',
    category: undefined,
    status: undefined
  })

  // 블록체인 상태 필터
  const [blockchainFilter, setBlockchainFilter] = useState<BlockchainStatus | 'all'>('all')

  // 페이지네이션 상태
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
    isLast: false,
    isFirst: true
  })

  // 캠페인 목록 조회
  const fetchCampaigns = async (params?: AdminCampaignListParams) => {
    setLoading(true)
    try {
      console.log('📋 캠페인 목록 조회 시작:', params || filters)
      
      const response: SpringPageResponse<CampaignListItem> = await campaignApi.getAdminCampaigns(params || filters)
      
      setCampaigns(response.content)
      setPagination({
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        currentPage: response.number,
        isLast: response.last,
        isFirst: response.first
      })

      console.log('✅ 캠페인 목록 조회 성공:', {
        count: response.content.length,
        totalElements: response.totalElements,
        currentPage: response.number
      })

    } catch (error) {
      console.error('❌ 캠페인 목록 조회 실패:', error)
      
      let errorMessage = '캠페인 목록을 불러오는데 실패했습니다'
      
      if (error instanceof ApiError) {
        errorMessage = campaignApi.handleApiError(error)
        
        // 401 에러의 경우 로그인 페이지로 이동
        if (error.status === 401) {
          router.push('/login')
          return
        }
      }

      toast.error('목록 조회 실패', {
        description: errorMessage
      })
    } finally {
      setLoading(false)
    }
  }

  // 초기 데이터 로드
  useEffect(() => {
    fetchCampaigns()
  }, [])

  // 검색/필터 핸들러
  const handleSearch = (keyword: string) => {
    const newFilters = { ...filters, keyword, page: 0 }
    setFilters(newFilters)
    fetchCampaigns(newFilters)
  }

  const handleCategoryFilter = (category: CampaignCategory | undefined) => {
    const newFilters = { ...filters, category, page: 0 }
    setFilters(newFilters)
    fetchCampaigns(newFilters)
  }

  const handleStatusFilter = (status: CampaignStatus | undefined) => {
    const newFilters = { ...filters, status, page: 0 }
    setFilters(newFilters)
    fetchCampaigns(newFilters)
  }

  // 블록체인 상태 필터링 (클라이언트 사이드)
  const filteredCampaigns = campaigns.filter(campaign => {
    if (blockchainFilter === 'all') return true
    return (campaign as any).blockchainStatus === blockchainFilter
  })

  // 페이지 변경
  const handlePageChange = (newPage: number) => {
    const newFilters = { ...filters, page: newPage }
    setFilters(newFilters)
    fetchCampaigns(newFilters)
  }

  // 캠페인 삭제
  const handleDeleteCampaign = async (id: number) => {
    try {
      await campaignApi.softDeleteAdminCampaign(id)
      toast.success('캠페인이 삭제되었습니다')
      fetchCampaigns() // 목록 새로고침
    } catch (error) {
      console.error('❌ 캠페인 삭제 실패:', error)
      
      let errorMessage = '캠페인 삭제에 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = campaignApi.handleApiError(error)
      }
      
      toast.error('삭제 실패', {
        description: errorMessage
      })
    }
    setDeleteDialogOpen(false)
    setSelectedCampaignId(null)
  }

  // 상태 뱃지 색상
  const getStatusBadgeVariant = (status: CampaignStatus) => {
    switch (status) {
      case 'DRAFT': return 'secondary'
      case 'ACTIVE': return 'default'
      case 'COMPLETED': return 'outline'
      case 'CANCELLED': return 'destructive'
      default: return 'secondary'
    }
  }

  // 금액 포맷팅
  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount)
  }


  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8">
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              캠페인 관리
            </h1>
            <Button
              onClick={() => router.push('/admin/campaigns/create')}
              className="flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              새 캠페인 등록
            </Button>
          </div>
        </div>

        {/* 캠페인 테이블 */}
        <Card>
          <CardHeader>
            {/* 필터 및 검색 */}
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
              {/* 검색 */}
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="캠페인 제목 또는 설명 검색..."
                  value={filters.keyword || ''}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="pl-10 h-9"
                />
              </div>

              {/* 카테고리 필터 */}
              <Select
                value={filters.category || 'all'}
                onValueChange={(value) =>
                  handleCategoryFilter(value === 'all' ? undefined : value as CampaignCategory)
                }
              >
                <SelectTrigger className="h-9 md:w-40">
                  <SelectValue placeholder="카테고리" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">모든 카테고리</SelectItem>
                  {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {/* 상태 필터 */}
              <Select
                value={filters.status || 'all'}
                onValueChange={(value) =>
                  handleStatusFilter(value === 'all' ? undefined : value as CampaignStatus)
                }
              >
                <SelectTrigger className="h-9 md:w-40">
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">모든 상태</SelectItem>
                  {Object.entries(STATUS_LABELS).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {/* 새로고침 버튼 */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchCampaigns()}
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
            ) : campaigns.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                등록된 캠페인이 없습니다
              </div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="text-center text-base font-semibold">제목</TableHead>
                      <TableHead className="text-center text-base font-semibold">카테고리</TableHead>
                      <TableHead className="text-center text-base font-semibold">상태</TableHead>
                      <TableHead className="text-center text-base font-semibold">목표금액</TableHead>
                      <TableHead className="text-center text-base font-semibold">현재금액</TableHead>
                      <TableHead className="text-center text-base font-semibold">진행률</TableHead>
                      <TableHead className="text-center text-base font-semibold">등록일</TableHead>
                      <TableHead className="text-center text-base font-semibold">액션</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredCampaigns.map((campaign, index) => (
                      <TableRow key={campaign.id} className={`animate-fade-in-up stagger-${Math.min(index + 1, 20)}`}>
                        <TableCell className="font-medium">
                          <div className="font-semibold">{campaign.title}</div>
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge variant="outline">
                            {CATEGORY_LABELS[campaign.category]}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex flex-col gap-1 items-center">
                            <Badge variant={getStatusBadgeVariant(campaign.status)}>
                              {STATUS_LABELS[campaign.status]}
                            </Badge>
                            {/* 블록체인 상태 표시 */}
                            {(campaign as any).blockchainStatus && (campaign as any).blockchainStatus !== 'NONE' && (
                              <Badge
                                variant="outline"
                                className={`text-xs ${
                                  (campaign as any).blockchainStatus === 'ACTIVE' ? 'border-green-500 text-green-700' :
                                  (campaign as any).blockchainStatus === 'BLOCKCHAIN_PROCESSING' ? 'border-blue-500 text-blue-700' :
                                  (campaign as any).blockchainStatus === 'BLOCKCHAIN_FAILED' ? 'border-red-500 text-red-700' :
                                  'border-yellow-500 text-yellow-700'
                                }`}
                              >
                                {(campaign as any).blockchainStatus === 'BLOCKCHAIN_PENDING' && '⏳'}
                                {(campaign as any).blockchainStatus === 'BLOCKCHAIN_PROCESSING' && '🔄'}
                                {(campaign as any).blockchainStatus === 'ACTIVE' && '✅'}
                                {(campaign as any).blockchainStatus === 'BLOCKCHAIN_FAILED' && '❌'}
                                {' '}
                                {(campaign as any).blockchainStatus === 'BLOCKCHAIN_PENDING' && '배포 대기'}
                                {(campaign as any).blockchainStatus === 'BLOCKCHAIN_PROCESSING' && '배포 중'}
                                {(campaign as any).blockchainStatus === 'ACTIVE' && '배포 완료'}
                                {(campaign as any).blockchainStatus === 'BLOCKCHAIN_FAILED' && '배포 실패'}
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-right">{formatAmount(campaign.targetAmount)}</TableCell>
                        <TableCell className="text-right">{formatAmount(campaign.currentAmount)}</TableCell>
                        <TableCell className="text-center">
                          <div className="flex items-center gap-2 justify-center">
                            <div className="w-16 bg-gray-200 rounded-r-full h-2">
                              <div
                                className="bg-teal-600 h-2 rounded-r-full"
                                style={{ width: `${Math.min(campaign.progressPercentage, 100)}%` }}
                              />
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          {format(new Date(campaign.createdAt), 'yyyy.MM.dd', { locale: ko })}
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
                                onClick={() => router.push(`/admin/campaigns/${campaign.id}`)}
                                className="flex items-center gap-2"
                              >
                                <Eye className="h-4 w-4" />
                                상세보기
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={() => router.push(`/admin/campaigns/${campaign.id}/edit`)}
                                className="flex items-center gap-2"
                              >
                                <Edit className="h-4 w-4" />
                                수정하기
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                onClick={() => {
                                  setSelectedCampaignId(campaign.id)
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
              <AlertDialogTitle>캠페인 삭제 확인</AlertDialogTitle>
              <AlertDialogDescription>
                선택한 캠페인을 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={() => {
                  if (selectedCampaignId) {
                    handleDeleteCampaign(selectedCampaignId)
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