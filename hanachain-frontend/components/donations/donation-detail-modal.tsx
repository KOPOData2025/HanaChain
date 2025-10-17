"use client"

import { useState, useEffect } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import {
  Calendar,
  CreditCard,
  Receipt,
  Heart,
  ExternalLink,
  Download,
  CheckCircle,
  Clock,
  XCircle,
  AlertCircle,
  RefreshCw
} from "lucide-react"
import { DonationRecord } from "@/types/donation"
import { formatCurrency, formatDate } from "@/lib/utils"
import { getDonationById, transformDonationResponse } from "@/lib/api/donation-api"

const STATUS_LABELS = {
  completed: '기부 완료',
  pending: '결제 대기중',
  failed: '결제 실패',
  cancelled: '기부 취소'
} as const

const STATUS_COLORS = {
  completed: 'bg-green-100 text-green-800',
  pending: 'bg-yellow-100 text-yellow-800',
  failed: 'bg-red-100 text-red-800',
  cancelled: 'bg-gray-100 text-gray-800'
} as const

const STATUS_ICONS = {
  completed: CheckCircle,
  pending: Clock,
  failed: XCircle,
  cancelled: AlertCircle
} as const

interface DonationDetailModalProps {
  donationId: string | null
  isOpen: boolean
  onClose: () => void
}

export function DonationDetailModal({ donationId, isOpen, onClose }: DonationDetailModalProps) {
  const [donation, setDonation] = useState<DonationRecord | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 기부 상세 정보 로드
  useEffect(() => {
    if (donationId && isOpen) {
      loadDonationDetail()
    }
  }, [donationId, isOpen])

  const loadDonationDetail = async () => {
    if (!donationId) return

    setIsLoading(true)
    setError(null)

    try {
      console.log('🔍 기부 상세 정보 로딩 시작:', { donationId })
      const response = await getDonationById(donationId)
      const transformedDonation = transformDonationResponse(response)
      
      console.log('✅ 기부 상세 정보 로딩 성공:', transformedDonation)
      setDonation(transformedDonation)
    } catch (err) {
      console.error('❌ 기부 상세 정보 로딩 실패:', err)
      setError(err instanceof Error ? err.message : '기부 정보를 불러올 수 없습니다')
    } finally {
      setIsLoading(false)
    }
  }

  // 모달이 닫힐 때 상태 초기화
  useEffect(() => {
    if (!isOpen) {
      setDonation(null)
      setError(null)
    }
  }, [isOpen])

  if (!donationId) return null

  const StatusIcon = donation ? STATUS_ICONS[donation.status] : Clock

  const getPaymentMethodLabel = (method?: string) => {
    switch (method) {
      case 'card': return '신용카드'
      case 'bank': return '계좌이체'
      case 'naverpay': return '네이버페이'
      case 'kakaopay': return '카카오페이'
      case 'paypal': return '페이팔'
      case 'other': return '기타'
      default: return method || '알 수 없음'
    }
  }

  const handleDownloadReceipt = () => {
    // TODO: 영수증 다운로드 구현
    alert('영수증 다운로드 기능은 준비중입니다.')
  }

  const handleViewCampaign = () => {
    if (!donation) return
    // TODO: 캠페인 상세 페이지로 이동
    window.open(`/campaign/${donation.campaignId}`, '_blank')
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <Heart className="h-5 w-5 text-[#009591]" />
            <span>기부 상세 정보</span>
          </DialogTitle>
        </DialogHeader>

        {/* 로딩 상태 */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <LoadingSpinner className="h-8 w-8" />
            <span className="ml-2 text-gray-600">기부 정보를 불러오는 중...</span>
          </div>
        )}

        {/* 에러 상태 */}
        {error && !isLoading && (
          <div className="space-y-4">
            <ErrorMessage message={error} />
            <div className="flex justify-center">
              <Button onClick={loadDonationDetail} variant="outline">
                <RefreshCw className="h-4 w-4 mr-2" />
                다시 시도
              </Button>
            </div>
          </div>
        )}

        {/* 데이터가 있을 때만 표시 */}
        {donation && !isLoading && !error && (
          <div className="space-y-6">
          {/* 상태 및 기본 정보 */}
          <div className="text-center space-y-3">
            <Badge className={`${STATUS_COLORS[donation.status]} text-base px-4 py-2`}>
              <StatusIcon className="h-4 w-4 mr-2" />
              {STATUS_LABELS[donation.status]}
            </Badge>
            
            <div className="text-3xl font-bold text-gray-900">
              {formatCurrency(donation.amount)}
            </div>
            
            <div className="text-gray-600">
              <Calendar className="h-4 w-4 inline mr-2" />
              {formatDate(donation.donatedAt)}에 기부
            </div>
          </div>

          <Separator />

          {/* 캠페인 정보 */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-gray-900">캠페인 정보</h3>
            
            <div className="flex space-x-4">
              {donation.campaignImage && (
                <div className="flex-shrink-0">
                  <img
                    src={donation.campaignImage}
                    alt={donation.campaignTitle}
                    className="w-24 h-24 object-cover rounded-lg"
                  />
                </div>
              )}
              
              <div className="flex-1">
                <h4 className="font-medium text-gray-900 mb-2">
                  {donation.campaignTitle}
                </h4>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleViewCampaign}
                  className="flex items-center space-x-2"
                >
                  <ExternalLink className="h-4 w-4" />
                  <span>캠페인 보기</span>
                </Button>
              </div>
            </div>
          </div>

          <Separator />

          {/* 기부 메시지 */}
          {donation.message && (
            <>
              <div className="space-y-3">
                <h3 className="text-lg font-semibold text-gray-900">응원 메시지</h3>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <p className="text-gray-700 leading-relaxed">
                    "{donation.message}"
                  </p>
                </div>
              </div>
              <Separator />
            </>
          )}

          {/* 결제 정보 */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-gray-900">결제 정보</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <div className="text-sm text-gray-600">결제 방법</div>
                <div className="flex items-center space-x-2">
                  <CreditCard className="h-4 w-4 text-gray-400" />
                  <span className="font-medium">
                    {getPaymentMethodLabel(donation.paymentMethod || '')}
                  </span>
                </div>
              </div>

              <div className="space-y-2">
                <div className="text-sm text-gray-600">기부 ID</div>
                <div className="font-mono text-sm text-gray-900">
                  {donation.id}
                </div>
              </div>

              {donation.receiptNumber && (
                <div className="space-y-2">
                  <div className="text-sm text-gray-600">영수증 번호</div>
                  <div className="font-mono text-sm text-gray-900">
                    {donation.receiptNumber}
                  </div>
                </div>
              )}

              <div className="space-y-2">
                <div className="text-sm text-gray-600">기부 일시</div>
                <div className="text-sm text-gray-900">
                  {new Date(donation.donatedAt).toLocaleString('ko-KR')}
                </div>
              </div>

              {donation.donationTransactionHash && (
                <div className="space-y-2 md:col-span-2">
                  <div className="text-sm text-gray-600">블록체인 트랜잭션</div>
                  <div className="flex items-center space-x-2">
                    <code className="text-sm bg-gray-100 px-2 py-1 rounded font-mono text-gray-900">
                      {donation.donationTransactionHash.substring(0, 10)}...
                      {donation.donationTransactionHash.substring(donation.donationTransactionHash.length - 8)}
                    </code>
                    <a
                      href={`https://sepolia.etherscan.io/tx/${donation.donationTransactionHash}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center text-[#009591] hover:text-[#007a77] transition-colors"
                    >
                      <ExternalLink className="h-4 w-4" />
                      <span className="ml-1 text-sm">Etherscan에서 확인</span>
                    </a>
                  </div>
                </div>
              )}
            </div>
          </div>

          <Separator />

          {/* 액션 버튼들 */}
          <div className="flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-3">
            {donation.status === 'completed' && donation.receiptNumber && (
              <Button
                onClick={handleDownloadReceipt}
                variant="outline"
                className="flex items-center space-x-2"
              >
                <Download className="h-4 w-4" />
                <span>영수증 다운로드</span>
              </Button>
            )}

            <Button
              variant="outline"
              onClick={handleViewCampaign}
              className="flex items-center space-x-2"
            >
              <Heart className="h-4 w-4" />
              <span>다시 기부하기</span>
            </Button>

            {donation.status === 'failed' && (
              <Button
                variant="outline"
                className="flex items-center space-x-2 text-[#009591] border-[#009591] hover:bg-[#009591] hover:text-white"
              >
                <Receipt className="h-4 w-4" />
                <span>다시 시도</span>
              </Button>
            )}
          </div>

          {/* 상태별 추가 정보 */}
          {donation.status === 'pending' && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <Clock className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-medium text-yellow-800">결제 대기중</h4>
                  <p className="text-sm text-yellow-700 mt-1">
                    결제가 진행중입니다. 잠시만 기다려주세요.
                  </p>
                </div>
              </div>
            </div>
          )}

          {donation.status === 'failed' && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <XCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-medium text-red-800">결제 실패</h4>
                  <p className="text-sm text-red-700 mt-1">
                    {donation.failureReason || '결제에 실패했습니다. 다시 시도해보거나 다른 결제 방법을 이용해주세요.'}
                  </p>
                </div>
              </div>
            </div>
          )}

          {donation.status === 'completed' && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <CheckCircle className="h-5 w-5 text-green-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-medium text-green-800">기부 완료</h4>
                  <p className="text-sm text-green-700 mt-1">
                    소중한 기부에 감사드립니다. 여러분의 따뜻한 마음이 세상을 변화시킵니다.
                  </p>
                </div>
              </div>
            </div>
          )}

          {donation.status === 'cancelled' && (
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <div className="flex items-start space-x-3">
                <AlertCircle className="h-5 w-5 text-gray-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-medium text-gray-800">기부 취소</h4>
                  <p className="text-sm text-gray-700 mt-1">
                    기부가 취소되었습니다.
                  </p>
                </div>
              </div>
            </div>
          )}

          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
