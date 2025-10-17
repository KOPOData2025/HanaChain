'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import {
  Search,
  Filter,
  Download,
  Eye,
  RefreshCw,
  CreditCard,
  Calendar,
  DollarSign,
  TrendingUp,
  AlertCircle,
  CheckCircle,
  XCircle,
  Clock,
  RotateCcw,
  Shield,
  ShieldAlert,
  ShieldCheck,
  ShieldQuestion,
  MoreHorizontal
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
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select'
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

import { adminDonationApi, AdminDonation, AdminDonationStats, AdminDonationFilters } from '@/lib/api/admin-donation-api'
import { ApiError } from '@/lib/api/client'
import { cn } from '@/lib/utils'

const PAYMENT_STATUS_LABELS = {
  PENDING: '대기중',
  PROCESSING: '처리중',
  COMPLETED: '완료',
  FAILED: '실패',
  CANCELLED: '취소',
  REFUNDED: '환불'
}

const PAYMENT_METHOD_LABELS = {
  CREDIT_CARD: '신용카드',
  BANK_TRANSFER: '계좌이체',
  VIRTUAL_ACCOUNT: '가상계좌',
  MOBILE_PAYMENT: '모바일결제',
  PAYPAL: '페이팔',
  OTHER: '기타'
}

export default function AdminDonationsPage() {
  const router = useRouter()
  
  // 상태 관리
  const [donations, setDonations] = useState<AdminDonation[]>([])
  const [stats, setStats] = useState<AdminDonationStats | null>(null)
  const [loading, setLoading] = useState(false)
  const [selectedDonation, setSelectedDonation] = useState<AdminDonation | null>(null)
  const [refundDialogOpen, setRefundDialogOpen] = useState(false)
  const [refundReason, setRefundReason] = useState('')

  // 필터 상태
  const [filters, setFilters] = useState<AdminDonationFilters>({
    page: 0,
    size: 20,
    keyword: ''
  })

  // 페이지네이션 상태
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
    isLast: false,
    isFirst: true
  })

  // 기부 내역 조회
  const fetchDonations = async (params?: AdminDonationFilters) => {
    setLoading(true)
    try {
      const currentFilters = params || filters

      const response = await adminDonationApi.getAllDonations(currentFilters)
      
      setDonations(response.content)
      setPagination({
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        currentPage: response.number,
        isLast: response.last,
        isFirst: response.first
      })

    } catch (error) {
      console.error('Failed to fetch donations:', error)
      
      let errorMessage = '기부 내역을 불러오는데 실패했습니다'
      
      if (error instanceof ApiError) {
        errorMessage = adminDonationApi.handleApiError(error)
        
        if (error.status === 401) {
          router.push('/login')
          return
        }
      }

      toast.error('조회 실패', {
        description: errorMessage
      })
    } finally {
      setLoading(false)
    }
  }

  // 통계 조회
  const fetchStats = async () => {
    try {
      const response = await adminDonationApi.getDonationStats()
      setStats(response)
    } catch (error) {
      console.error('Failed to fetch donation stats:', error)
    }
  }

  // 초기 데이터 로드
  useEffect(() => {
    fetchDonations()
    fetchStats()
  }, [])

  // 검색 핸들러
  const handleSearch = (keyword: string) => {
    const newFilters = { ...filters, keyword, page: 0 }
    setFilters(newFilters)
    fetchDonations(newFilters)
  }

  // 페이지 변경
  const handlePageChange = (newPage: number) => {
    const newFilters = { ...filters, page: newPage }
    setFilters(newFilters)
    fetchDonations(newFilters)
  }

  // 환불 처리
  const handleRefund = async (donationId: number) => {
    if (!refundReason.trim()) {
      toast.error('환불 사유를 입력해주세요')
      return
    }

    try {
      await adminDonationApi.refundDonation(donationId, refundReason)
      toast.success('환불 처리가 완료되었습니다')
      fetchDonations() // 목록 새로고침
      fetchStats() // 통계 새로고침
      setRefundDialogOpen(false)
      setRefundReason('')
      setSelectedDonation(null)
    } catch (error) {
      console.error('Failed to refund donation:', error)
      
      let errorMessage = '환불 처리에 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = adminDonationApi.handleApiError(error)
      }
      
      toast.error('환불 실패', {
        description: errorMessage
      })
    }
  }


  // 상태 뱃지 색상
  const getStatusBadgeVariant = (status: AdminDonation['paymentStatus']) => {
    switch (status) {
      case 'PENDING': return 'secondary'
      case 'PROCESSING': return 'default'
      case 'COMPLETED': return 'success'
      case 'FAILED': return 'destructive'
      case 'CANCELLED': return 'outline'
      case 'REFUNDED': return 'warning'
      default: return 'secondary'
    }
  }

  // 상태 아이콘
  const getStatusIcon = (status: AdminDonation['paymentStatus']) => {
    switch (status) {
      case 'PENDING': return <Clock className="h-3 w-3" />
      case 'PROCESSING': return <RefreshCw className="h-3 w-3 animate-spin" />
      case 'COMPLETED': return <CheckCircle className="h-3 w-3" />
      case 'FAILED': return <XCircle className="h-3 w-3" />
      case 'CANCELLED': return <AlertCircle className="h-3 w-3" />
      case 'REFUNDED': return <RotateCcw className="h-3 w-3" />
      default: return null
    }
  }

  // 금액 포맷팅
  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount)
  }

  // FDS 액션 뱃지 색상
  const getFdsActionBadgeVariant = (action?: string) => {
    switch (action) {
      case 'APPROVE': return 'success'
      case 'MANUAL_REVIEW': return 'warning'
      case 'BLOCK': return 'default' // 커스텀 스타일 적용을 위해 default로 변경
      default: return 'secondary'
    }
  }

  // FDS 액션 뱃지 커스텀 스타일
  const getFdsActionBadgeStyle = (action?: string) => {
    if (action === 'BLOCK') {
      return 'bg-red-400 text-white hover:bg-red-500 border-red-400'
    }
    if (action === 'APPROVE') {
      return 'bg-green-600 text-white hover:bg-green-700 border-green-600'
    }
    return ''
  }

  // FDS 액션 아이콘
  const getFdsActionIcon = (action?: string) => {
    switch (action) {
      case 'APPROVE': return <ShieldCheck className="h-3 w-3" />
      case 'MANUAL_REVIEW': return <ShieldQuestion className="h-3 w-3" />
      case 'BLOCK': return <ShieldAlert className="h-3 w-3" />
      default: return <Shield className="h-3 w-3" />
    }
  }

  // FDS 상태 뱃지 색상
  const getFdsStatusBadgeVariant = (status?: string) => {
    switch (status) {
      case 'SUCCESS': return 'success'
      case 'PENDING': return 'secondary'
      case 'FAILED': return 'destructive'
      case 'TIMEOUT': return 'warning'
      default: return 'outline'
    }
  }

  // FDS Risk Score 색상 클래스
  const getRiskScoreColor = (score?: number) => {
    if (!score) return 'text-gray-500'
    if (score >= 0.7) return 'text-red-600 font-bold'
    if (score >= 0.4) return 'text-orange-600 font-semibold'
    return 'text-green-600'
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8">
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                기부 관리
              </h1>
              <p className="text-gray-600 dark:text-gray-400">
                전체 기부 내역을 확인하고 관리하세요
              </p>
            </div>
            <Button 
              onClick={() => toast.info('엑셀 다운로드 기능은 준비 중입니다')}
              className="flex items-center gap-2"
              variant="outline"
              disabled
            >
              <Download className="h-4 w-4" />
              엑셀 다운로드 (준비중)
            </Button>
          </div>
        </div>

        {/* 통계 카드 */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">총 기부금액</CardTitle>
                <DollarSign className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatAmount(stats.totalAmount)}</div>
                <p className="text-xs text-muted-foreground">
                  총 {stats.totalCount}건
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">오늘 기부</CardTitle>
                <TrendingUp className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatAmount(stats.todayAmount)}</div>
                <p className="text-xs text-muted-foreground">
                  {stats.todayCount}건
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">이번달 기부</CardTitle>
                <Calendar className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatAmount(stats.monthAmount)}</div>
                <p className="text-xs text-muted-foreground">
                  {stats.monthCount}건
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">평균 기부액</CardTitle>
                <CreditCard className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatAmount(stats.averageAmount)}</div>
                <p className="text-xs text-muted-foreground">
                  완료 {stats.completedCount}건
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 기부 내역 테이블 */}
        <Card>
          <CardHeader>
            {/* 필터 및 검색 */}
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
              {/* 검색 */}
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="기부자명, 캠페인명 검색..."
                  value={filters.keyword || ''}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="pl-10 h-9"
                />
              </div>

              {/* 새로고침 버튼 */}
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  fetchDonations()
                  fetchStats()
                }}
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
            ) : donations.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                기부 내역이 없습니다
              </div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="text-center text-base font-semibold">결제ID</TableHead>
                      <TableHead className="text-center text-base font-semibold">기부자</TableHead>
                      <TableHead className="text-center text-base font-semibold">캠페인</TableHead>
                      <TableHead className="text-center text-base font-semibold">금액</TableHead>
                      <TableHead className="text-center text-base font-semibold">결제방법</TableHead>
                      <TableHead className="text-center text-base font-semibold">상태</TableHead>
                      <TableHead className="text-center text-base font-semibold">FDS 검증</TableHead>
                      <TableHead className="text-center text-base font-semibold">위험도</TableHead>
                      <TableHead className="text-center text-base font-semibold">기부일시</TableHead>
                      <TableHead className="text-center text-base font-semibold">액션</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {donations.map((donation, index) => (
                      <TableRow key={donation.id} className={`animate-fade-in-up stagger-${Math.min(index + 1, 20)}`}>
                        <TableCell className="font-mono text-xs max-w-[120px]">
                          <div className="overflow-x-auto whitespace-nowrap scrollbar-hide">
                            {donation.paymentId}
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="font-medium">
                            {donation.anonymous ? '익명' : donation.donorName || '미제공'}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="font-medium">{donation.campaignTitle}</div>
                        </TableCell>
                        <TableCell className="text-right font-medium">
                          {formatAmount(donation.amount)}
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge variant="outline">
                            {PAYMENT_METHOD_LABELS[donation.paymentMethod]}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex justify-center">
                            <Badge
                              variant={getStatusBadgeVariant(donation.paymentStatus)}
                              className="flex items-center gap-1 w-fit"
                            >
                              {getStatusIcon(donation.paymentStatus)}
                              {PAYMENT_STATUS_LABELS[donation.paymentStatus]}
                            </Badge>
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex justify-center">
                            {donation.fdsAction ? (
                              <Badge
                                variant={getFdsActionBadgeVariant(donation.fdsAction)}
                                className={cn(
                                  "flex items-center gap-1 w-fit cursor-pointer hover:opacity-80 transition-opacity",
                                  getFdsActionBadgeStyle(donation.fdsAction)
                                )}
                                onClick={() => router.push(`/admin/donations/${donation.id}/fds`)}
                                title="FDS 검증 결과 상세 보기"
                              >
                                {getFdsActionIcon(donation.fdsAction)}
                                {donation.fdsAction}
                              </Badge>
                            ) : (
                              <Badge variant="outline" className="flex items-center gap-1 w-fit">
                                <Shield className="h-3 w-3" />
                                {donation.fdsStatus || 'PENDING'}
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          {donation.fdsRiskScore !== undefined && donation.fdsRiskScore !== null ? (
                            <div className="flex flex-col items-center">
                              <span className={getRiskScoreColor(donation.fdsRiskScore)}>
                                {(donation.fdsRiskScore * 100).toFixed(1)}%
                              </span>
                              {donation.fdsConfidence !== undefined && (
                                <span className="text-xs text-gray-500">
                                  신뢰도: {(donation.fdsConfidence * 100).toFixed(0)}%
                                </span>
                              )}
                            </div>
                          ) : (
                            <span className="text-gray-400 text-sm">-</span>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          {donation.paidAt ?
                            format(new Date(donation.paidAt), 'yyyy.MM.dd HH:mm', { locale: ko }) :
                            format(new Date(donation.createdAt), 'yyyy.MM.dd HH:mm', { locale: ko })
                          }
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
                                onClick={() => router.push(`/admin/donations/${donation.id}`)}
                                className="flex items-center gap-2"
                              >
                                <Eye className="h-4 w-4" />
                                상세보기
                              </DropdownMenuItem>
                              {donation.paymentStatus === 'COMPLETED' && (
                                <>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem
                                    onClick={() => {
                                      setSelectedDonation(donation)
                                      setRefundDialogOpen(true)
                                    }}
                                    className="flex items-center gap-2 text-red-600"
                                  >
                                    <RotateCcw className="h-4 w-4" />
                                    환불 처리
                                  </DropdownMenuItem>
                                </>
                              )}
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

        {/* 환불 확인 다이얼로그 */}
        <AlertDialog open={refundDialogOpen} onOpenChange={setRefundDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>환불 처리 확인</AlertDialogTitle>
              <AlertDialogDescription>
                선택한 기부 내역을 환불 처리하시겠습니까?
                <br />
                기부자: {selectedDonation?.donorName || '익명'}
                <br />
                금액: {selectedDonation && formatAmount(selectedDonation.amount)}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <div className="py-4">
              <label className="text-sm font-medium">환불 사유</label>
              <Input
                value={refundReason}
                onChange={(e) => setRefundReason(e.target.value)}
                placeholder="환불 사유를 입력하세요"
                className="mt-2"
              />
            </div>
            <AlertDialogFooter>
              <AlertDialogCancel onClick={() => setRefundReason('')}>
                취소
              </AlertDialogCancel>
              <AlertDialogAction
                onClick={() => {
                  if (selectedDonation) {
                    handleRefund(selectedDonation.id)
                  }
                }}
                className="bg-red-600 hover:bg-red-700"
              >
                환불 처리
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}