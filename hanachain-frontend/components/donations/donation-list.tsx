"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { DonationDetailModal } from "./donation-detail-modal"
import { DonationCertificateModal } from "./donation-certificate-modal"
import {
  Search,
  Filter,
  Calendar,
  CreditCard,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Award
} from "lucide-react"
import { useDonations } from "@/hooks/use-donations"
import { DonationRecord, DonationListParams } from "@/types/donation"
import { formatCurrency, formatDate } from "@/lib/utils"

const STATUS_LABELS = {
  all: '전체',
  completed: '기부완료',
  pending: '결제 대기중',
  failed: '실패',
  cancelled: '취소'
} as const

const STATUS_COLORS = {
  completed: 'bg-green-100 text-green-800 border border-green-200',
  pending: 'bg-yellow-100 text-yellow-800 border border-yellow-200',
  failed: 'bg-red-100 text-red-800 border border-red-200',
  cancelled: 'bg-gray-100 text-gray-800 border border-gray-200'
} as const

const STATUS_ICONS = {
  completed: CheckCircle,
  pending: Clock,
  failed: XCircle,
  cancelled: AlertCircle
} as const

interface DonationListProps {
  className?: string
}

export function DonationList({ className }: DonationListProps) {
  const { donations, total, isLoading, error, fetchDonations } = useDonations()
  const [params, setParams] = useState<DonationListParams>({
    page: 1,
    limit: 10,
    status: 'all',
    sortBy: 'date',
    sortOrder: 'desc'
  })
  const [selectedDonationId, setSelectedDonationId] = useState<string | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [certificateDonationId, setCertificateDonationId] = useState<string | null>(null)
  const [isCertificateModalOpen, setIsCertificateModalOpen] = useState(false)

  useEffect(() => {
    fetchDonations(params)
  }, [fetchDonations, params])

  const handleFilterChange = (key: keyof DonationListParams, value: any) => {
    setParams(prev => ({
      ...prev,
      [key]: value,
      page: 1 // 필터 변경 시 첫 페이지로
    }))
  }

  const handlePageChange = (page: number) => {
    setParams(prev => ({ ...prev, page }))
  }

  const handleDonationClick = (donation: DonationRecord) => {
    setSelectedDonationId(donation.id)
    setIsModalOpen(true)
  }

  const handleViewCertificate = (donation: DonationRecord, e: React.MouseEvent) => {
    e.stopPropagation() // 부모 클릭 이벤트 방지
    setCertificateDonationId(donation.id)
    setIsCertificateModalOpen(true)
  }

  const totalPages = Math.ceil(total / (params.limit || 10))

  return (
    <div className={className}>
      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-col space-y-4 md:flex-row md:items-center md:justify-between md:space-y-0">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">기부 내역</h2>
              <p className="text-sm text-gray-600">총 {total}건의 기부 내역</p>
            </div>

            {/* 필터 및 검색 */}
            <div className="flex flex-col space-y-3 md:flex-row md:items-center md:space-y-0 md:space-x-3">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="캠페인 제목 검색..."
                  value={params.search || ''}
                  onChange={(e) => handleFilterChange('search', e.target.value)}
                  className="pl-10 w-full md:w-64"
                />
              </div>

              <Select
                value={params.status || 'all'}
                onValueChange={(value) => handleFilterChange('status', value)}
              >
                <SelectTrigger className="w-full md:w-32">
                  <Filter className="h-4 w-4 mr-2" />
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(STATUS_LABELS).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select
                value={`${params.sortBy}-${params.sortOrder}`}
                onValueChange={(value) => {
                  const [sortBy, sortOrder] = value.split('-')
                  handleFilterChange('sortBy', sortBy)
                  handleFilterChange('sortOrder', sortOrder)
                }}
              >
                <SelectTrigger className="w-full md:w-36">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="date-desc">최신순</SelectItem>
                  <SelectItem value="date-asc">오래된순</SelectItem>
                  <SelectItem value="amount-desc">금액 높은순</SelectItem>
                  <SelectItem value="amount-asc">금액 낮은순</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {error && (
            <ErrorMessage message={error} className="mb-4" />
          )}

          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <LoadingSpinner className="h-8 w-8" />
              <span className="ml-2 text-gray-600">기부 내역을 불러오는 중...</span>
            </div>
          ) : donations.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-gray-400 mb-4">
                <CreditCard className="h-12 w-12 mx-auto" />
              </div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                기부 내역이 없습니다
              </h3>
              <p className="text-gray-600">
                첫 번째 기부를 시작해보세요!
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {donations.map((donation) => {
                const StatusIcon = STATUS_ICONS[donation.status]
                
                return (
                  <div
                    key={donation.id}
                    onClick={() => handleDonationClick(donation)}
                    className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                  >
                    <div className="flex items-start justify-between space-x-4">
                      {/* 1열: 캠페인 정보 전체 (이미지 + 제목 + 상태 + 날짜 + 메시지 + 금액 + 결제수단) */}
                      <div className="flex items-start space-x-4 flex-1 min-w-0">
                        {/* 캠페인 이미지 */}
                        <div className="flex-shrink-0">
                          <div className="w-16 h-16 bg-gray-200 rounded-lg overflow-hidden">
                            {donation.campaignImage ? (
                              <img
                                src={donation.campaignImage}
                                alt={donation.campaignTitle}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center">
                                <CreditCard className="h-6 w-6 text-gray-400" />
                              </div>
                            )}
                          </div>
                        </div>

                        {/* 캠페인 정보 */}
                        <div className="flex-1 min-w-0 flex items-center justify-between gap-4">
                          {/* 왼쪽: 캠페인 타이틀 + 상태 배지 */}
                          <div className="flex-1 min-w-0">
                            <h4 className="text-sm font-medium text-gray-900 truncate">
                              {donation.campaignTitle}
                            </h4>
                            <div className="flex items-center space-x-2 mt-1">
                              <Badge className={STATUS_COLORS[donation.status]}>
                                <StatusIcon className="h-3 w-3 mr-1" />
                                {STATUS_LABELS[donation.status]}
                              </Badge>
                              <span className="text-sm text-gray-500">
                                <Calendar className="h-3 w-3 inline mr-1" />
                                {formatDate(donation.donatedAt)}
                              </span>
                            </div>
                            {donation.message && (
                              <p className="text-sm text-gray-600 mt-2 truncate">
                                💌 {donation.message}
                              </p>
                            )}
                          </div>

                          {/* 오른쪽: 기부 금액 + 결제 수단 */}
                          <div className="text-right flex-shrink-0">
                            <div className="text-lg font-semibold text-gray-900">
                              {formatCurrency(donation.amount)}
                            </div>
                            {donation.paymentMethod && (
                              <div className="text-sm text-gray-500 mt-1">
                                {donation.paymentMethod === 'card' && '신용카드'}
                                {donation.paymentMethod === 'bank' && '계좌이체'}
                                {donation.paymentMethod === 'naverpay' && '네이버페이'}
                                {donation.paymentMethod === 'kakaopay' && '카카오페이'}
                                {donation.paymentMethod === 'paypal' && '페이팔'}
                                {donation.paymentMethod === 'other' && '기타'}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* 2열: 기부 증서 버튼만 */}
                      {donation.status === 'completed' && (
                        <div className="flex-shrink-0">
                          <Button
                            onClick={(e) => handleViewCertificate(donation, e)}
                            size="sm"
                            className="bg-[#009591]/10 hover:bg-[#009591]/20 text-[#009591] flex items-center justify-center min-h-[60px] min-w-[60px] p-0 rounded-lg"
                            title="기부 증서 보기"
                          >
                            <Award className="w-10 h-10" />
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center space-x-2 mt-6">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(params.page! - 1)}
                disabled={params.page === 1}
              >
                이전
              </Button>
              
              <div className="flex items-center space-x-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  const page = i + 1
                  return (
                    <Button
                      key={page}
                      variant={params.page === page ? "default" : "outline"}
                      size="sm"
                      onClick={() => handlePageChange(page)}
                      className="w-8 h-8 p-0"
                    >
                      {page}
                    </Button>
                  )
                })}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(params.page! + 1)}
                disabled={params.page === totalPages}
              >
                다음
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 기부 상세 모달 */}
      <DonationDetailModal
        donationId={selectedDonationId}
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false)
          setSelectedDonationId(null)
        }}
      />

      {/* 기부 증서 모달 */}
      <DonationCertificateModal
        donationId={certificateDonationId}
        isOpen={isCertificateModalOpen}
        onClose={() => {
          setIsCertificateModalOpen(false)
          setCertificateDonationId(null)
        }}
      />
    </div>
  )
}
