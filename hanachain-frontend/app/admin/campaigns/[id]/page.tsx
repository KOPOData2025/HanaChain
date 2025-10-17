'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { 
  ArrowLeft, 
  Edit, 
  Trash2, 
  RefreshCw, 
  Calendar,
  Target,
  TrendingUp,
  Users,
  Eye,
  Archive,
  RotateCcw
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

import { campaignApi, closeCampaignAndStartBatch, getUnverifiedFdsDonations } from '@/lib/api/campaign-api'
import { ApiError } from '@/lib/api/client'
import { CampaignDetailItem } from '@/types/donation'
import { CampaignStatus, STATUS_LABELS } from '@/types/admin'
import { HtmlContent } from '@/components/ui/html-content'
import { CampaignManagerList } from '@/components/admin/campaign-manager-list'
import { BlockchainStatusDisplay } from '@/components/admin/blockchain-status'
import { BeneficiaryAddressForm } from '@/components/admin/beneficiary-address-form'
import { BatchStatusCard } from '@/components/admin/batch-status-card'
import { CampaignCloseResponse } from '@/types/admin'
import { AdminDonation } from '@/lib/api/admin-donation-api'
import Link from 'next/link'

export default function AdminCampaignDetailPage() {
  const router = useRouter()
  const params = useParams()
  const campaignId = Number(params.id)
  
  // 상태 관리
  const [campaign, setCampaign] = useState<CampaignDetailItem | null>(null)
  const [loading, setLoading] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [restoreDialogOpen, setRestoreDialogOpen] = useState(false)
  const [statusUpdateLoading, setStatusUpdateLoading] = useState(false)

  // 배치 작업 관련 상태
  const [closeCampaignDialogOpen, setCloseCampaignDialogOpen] = useState(false)
  const [batchJobInfo, setBatchJobInfo] = useState<CampaignCloseResponse | null>(null)
  const [batchLoading, setBatchLoading] = useState(false)

  // FDS 미통과 거래 관련 상태
  const [fdsUnverifiedDonations, setFdsUnverifiedDonations] = useState<AdminDonation[]>([])
  const [fdsDialogOpen, setFdsDialogOpen] = useState(false)

  // 디버깅: batchJobInfo 변경 추적
  useEffect(() => {
    console.log('🔄 batchJobInfo 상태 변경:', batchJobInfo)
  }, [batchJobInfo])

  // 캠페인 상세 조회
  const fetchCampaignDetail = useCallback(async () => {
    if (!campaignId || isNaN(campaignId)) {
      toast.error('잘못된 캠페인 ID입니다')
      router.push('/admin/campaigns')
      return
    }

    setLoading(true)
    try {
      console.log('🔍 관리자 캠페인 상세 조회 시작:', campaignId)

      const response = await campaignApi.getAdminCampaignDetail(campaignId)
      setCampaign(response)

      console.log('✅ 관리자 캠페인 상세 조회 성공:', response)
      console.log('🔍 배치 작업 필드 확인:', {
        batchJobExecutionId: response.batchJobExecutionId,
        batchJobStatus: response.batchJobStatus,
        hasJobId: !!response.batchJobExecutionId,
        hasStatus: !!response.batchJobStatus,
        allKeys: Object.keys(response)
      })

      // 배치 작업 정보가 있으면 자동으로 설정
      if (response.batchJobExecutionId && response.batchJobStatus) {
        console.log('📦 배치 작업 정보 자동 로드:', {
          jobExecutionId: response.batchJobExecutionId,
          status: response.batchJobStatus
        })
        const batchInfo = {
          campaignId: response.id,
          campaignTitle: response.title,
          jobExecutionId: response.batchJobExecutionId,
          totalDonations: response.donorCount || 0,
          batchStatus: response.batchJobStatus,
          batchStartedAt: new Date().toISOString(),
          message: `배치 작업 실행 중 (상태: ${response.batchJobStatus})`
        }
        console.log('📦 setBatchJobInfo 호출:', batchInfo)
        setBatchJobInfo(batchInfo)
      } else {
        console.log('⚠️ 배치 작업 정보 없음 - BatchStatusCard 표시 안 됨')
      }

    } catch (error) {
      console.error('❌ 캠페인 상세 조회 실패:', error)

      let errorMessage = '캠페인 정보를 불러오는데 실패했습니다'

      if (error instanceof ApiError) {
        errorMessage = campaignApi.handleApiError(error)

        if (error.status === 401) {
          router.push('/login')
          return
        }

        if (error.status === 404) {
          toast.error('캠페인을 찾을 수 없습니다')
          router.push('/admin/campaigns')
          return
        }
      }

      toast.error('상세 조회 실패', {
        description: errorMessage
      })
    } finally {
      setLoading(false)
    }
  }, [campaignId, router])

  // 초기 데이터 로드
  useEffect(() => {
    fetchCampaignDetail()
  }, [fetchCampaignDetail])

  // 캠페인 소프트 삭제
  const handleSoftDelete = async () => {
    if (!campaign) return

    try {
      await campaignApi.softDeleteAdminCampaign(campaign.id)
      toast.success('캠페인이 삭제되었습니다')
      
      // 상세 정보 새로고침
      await fetchCampaignDetail()
      
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
  }

  // 캠페인 복구
  const handleRestore = async () => {
    if (!campaign) return

    try {
      const restored = await campaignApi.restoreAdminCampaign(campaign.id)
      setCampaign(restored)
      toast.success('캠페인이 복구되었습니다')
      
    } catch (error) {
      console.error('❌ 캠페인 복구 실패:', error)
      
      let errorMessage = '캠페인 복구에 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = campaignApi.handleApiError(error)
      }
      
      toast.error('복구 실패', {
        description: errorMessage
      })
    }
    setRestoreDialogOpen(false)
  }

  // 상태 변경
  const handleStatusChange = async (newStatus: CampaignStatus) => {
    if (!campaign || campaign.status === newStatus) return

    setStatusUpdateLoading(true)
    try {
      const updated = await campaignApi.updateAdminCampaignStatus(campaign.id, newStatus)
      setCampaign(updated)
      toast.success(`캠페인 상태가 '${STATUS_LABELS[newStatus]}'(으)로 변경되었습니다`)

    } catch (error) {
      console.error('❌ 캠페인 상태 변경 실패:', error)

      let errorMessage = '상태 변경에 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = campaignApi.handleApiError(error)
      }

      toast.error('상태 변경 실패', {
        description: errorMessage
      })
    } finally {
      setStatusUpdateLoading(false)
    }
  }

  // 캠페인 마감 및 배치 작업 시작
  const handleCloseCampaign = async () => {
    if (!campaign) return

    setBatchLoading(true)
    try {
      console.log('🔒 캠페인 마감 및 배치 작업 시작:', campaign.id)

      const response = await closeCampaignAndStartBatch(campaign.id)
      setBatchJobInfo(response)

      toast.success('배치 작업 시작', {
        description: response.message
      })

      setCloseCampaignDialogOpen(false)

    } catch (error) {
      console.error('❌ 배치 작업 시작 실패:', error)

      let errorMessage = '배치 작업 시작에 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = campaignApi.handleApiError(error)
      }

      // FDS 검증 미통과 거래가 있는지 확인
      if (error instanceof Error && error.message.includes('FDS 검증을 통과하지 못한 거래')) {
        // FDS 미통과 거래 목록 조회
        try {
          const unverifiedDonations = await getUnverifiedFdsDonations(campaign.id)
          setFdsUnverifiedDonations(unverifiedDonations)
          setCloseCampaignDialogOpen(false)
          setFdsDialogOpen(true)

          toast.error('캠페인 마감 불가', {
            description: errorMessage
          })
          return
        } catch (fetchError) {
          console.error('❌ FDS 미통과 거래 조회 실패:', fetchError)
        }
      }

      toast.error('배치 작업 실패', {
        description: errorMessage
      })
    } finally {
      setBatchLoading(false)
    }
  }

  // 배치 작업 완료 콜백 (재렌더링 방지를 위해 fetchCampaignDetail 호출 제거)
  const handleBatchComplete = useCallback(() => {
    toast.success('배치 작업 완료', {
      description: '모든 기부 내역이 블록체인에 기록되었습니다.'
    })
    // 캠페인 정보 새로고침 제거 - 무한 재렌더링 방지
    // fetchCampaignDetail()
  }, [])

  // 배치 작업 에러 콜백
  const handleBatchError = useCallback((error: string) => {
    toast.error('배치 작업 오류', {
      description: error
    })
  }, [])

  // 캠페인 마감 가능 여부 체크
  const canCloseCampaign = campaign &&
    campaign.status === 'ACTIVE' &&
    new Date(campaign.endDate) < new Date() &&
    campaign.donorCount > 0

  // 디버깅
  if (campaign) {
    console.log('🔍 Campaign Debug:', {
      id: campaign.id,
      status: campaign.status,
      endDate: campaign.endDate,
      endDateParsed: new Date(campaign.endDate),
      now: new Date(),
      isEnded: new Date(campaign.endDate) < new Date(),
      donorCount: campaign.donorCount,
      deletedAt: campaign.deletedAt,
      isDeleted: campaign.deletedAt !== null,
      canCloseCampaign
    })
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

  // FDS 사유 메시지 포맷팅 (기술 메시지를 사용자 친화적으로 변환)
  const formatFdsExplanation = (explanation: string | undefined): string => {
    if (!explanation) return 'FDS 검증을 통과하지 못했습니다'

    // Java exception 패턴 감지
    if (explanation.includes('java.lang.') || explanation.includes('Exception:')) {
      return 'FDS 검증을 통과하지 못했습니다'
    }

    // 기타 기술적인 메시지 감지
    if (explanation.includes('RuntimeException') ||
        explanation.includes('verification failed') ||
        explanation.includes('failed')) {
      return 'FDS 검증을 통과하지 못했습니다'
    }

    // 일반 메시지는 그대로 반환
    return explanation
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-600" />
          <p className="text-gray-600 dark:text-gray-400">캠페인 정보를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (!campaign) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-600 dark:text-gray-400">캠페인을 찾을 수 없습니다</p>
          <Button 
            onClick={() => router.push('/admin/campaigns')}
            className="mt-4"
          >
            목록으로 돌아가기
          </Button>
        </div>
      </div>
    )
  }

  const isDeleted = campaign.deletedAt !== null
  const progressPercentage = campaign.targetAmount > 0 
    ? (campaign.currentAmount / campaign.targetAmount * 100)
    : 0

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        {/* 헤더 */}
        <div className="mb-8">
          <div className="mb-4">
            <Link href="/admin/campaigns">
              <Button variant="ghost" size="sm" className="flex items-center gap-2">
                <ArrowLeft className="h-4 w-4" />
                캠페인 목록으로 돌아가기
              </Button>
            </Link>
          </div>

          <div className="flex justify-between items-start">
            <div className="space-y-2">
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                  {campaign.title}
                </h1>
                {isDeleted && (
                  <Badge variant="destructive" className="flex items-center gap-1">
                    <Archive className="h-3 w-3" />
                    삭제됨
                  </Badge>
                )}
              </div>
              <div className="flex items-center gap-4">
                <Badge variant={getStatusBadgeVariant(campaign.status)}>
                  {STATUS_LABELS[campaign.status]}
                </Badge>
                <span className="text-sm text-gray-500">
                  ID: {campaign.id}
                </span>
                <span className="text-sm text-gray-500">
                  등록일: {format(new Date(campaign.createdAt), 'yyyy.MM.dd HH:mm', { locale: ko })}
                </span>
              </div>
            </div>

            <div className="flex gap-2">
              {!isDeleted ? (
                <>
                  <Button
                    variant="outline"
                    onClick={() => router.push(`/admin/campaigns/${campaign.id}/edit`)}
                    className="flex items-center gap-2"
                  >
                    <Edit className="h-4 w-4" />
                    수정
                  </Button>
                  <Button
                    variant="destructive"
                    onClick={() => setDeleteDialogOpen(true)}
                    className="flex items-center gap-2"
                  >
                    <Trash2 className="h-4 w-4" />
                    삭제
                  </Button>
                </>
              ) : (
                <Button
                  variant="default"
                  onClick={() => setRestoreDialogOpen(true)}
                  className="flex items-center gap-2"
                >
                  <RotateCcw className="h-4 w-4" />
                  복구
                </Button>
              )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 메인 정보 */}
          <div className="lg:col-span-2 space-y-6">
            {/* 기본 정보 */}
            <Card>
              <CardHeader>
                <CardTitle>기본 정보</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">설명</h3>
                  <div className="max-h-none overflow-visible">
                    <HtmlContent 
                      html={campaign.htmlDescription || campaign.description} 
                      className="text-gray-900 dark:text-white max-w-none max-h-none"
                    />
                  </div>
                </div>
                
                <Separator />
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">주최자</h3>
                    <p className="text-gray-900 dark:text-white">{campaign.organizer}</p>
                  </div>
                  <div>
                    <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">카테고리</h3>
                    <Badge variant="outline">{campaign.category}</Badge>
                  </div>
                </div>

                {campaign.imageUrl && (
                  <>
                    <Separator />
                    <div>
                      <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">대표 이미지</h3>
                      <img 
                        src={campaign.imageUrl} 
                        alt={campaign.title}
                        className="max-w-full h-auto rounded-lg border"
                        onError={(e) => {
                          e.currentTarget.style.display = 'none'
                        }}
                      />
                    </div>
                  </>
                )}
              </CardContent>
            </Card>

            {/* 상태 관리 */}
            {!isDeleted && (
              <Card>
                <CardHeader>
                  <CardTitle>상태 관리</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div>
                      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        캠페인 상태 변경
                      </label>
                      <Select
                        value={campaign.status}
                        onValueChange={(value) => handleStatusChange(value as CampaignStatus)}
                        disabled={statusUpdateLoading}
                      >
                        <SelectTrigger className="mt-1">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {Object.entries(STATUS_LABELS).map(([key, label]) => (
                            <SelectItem key={key} value={key}>
                              {label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    {statusUpdateLoading && (
                      <p className="text-sm text-blue-600 flex items-center gap-2">
                        <RefreshCw className="h-3 w-3 animate-spin" />
                        상태 변경 중...
                      </p>
                    )}
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 캠페인 마감 및 배치 작업 */}
            {!isDeleted && canCloseCampaign && (
              <Card>
                <CardHeader>
                  <CardTitle>캠페인 마감 및 블록체인 전송</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      캠페인을 마감하고 모든 기부 내역을 블록체인에 기록합니다.
                    </p>
                    <div className="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span>총 기부 건수:</span>
                        <span className="font-medium">{campaign.donorCount}건</span>
                      </div>
                      <div className="flex justify-between">
                        <span>총 기부 금액:</span>
                        <span className="font-medium">{formatAmount(campaign.currentAmount)}</span>
                      </div>
                    </div>
                    <Button
                      onClick={() => setCloseCampaignDialogOpen(true)}
                      disabled={batchLoading || !!batchJobInfo}
                      className="w-full"
                    >
                      {batchLoading ? (
                        <>
                          <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                          배치 작업 시작 중...
                        </>
                      ) : batchJobInfo ? (
                        '배치 작업 실행 중'
                      ) : (
                        '캠페인 마감 및 블록체인 전송'
                      )}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 배치 작업 상태 */}
            {batchJobInfo && (
              <BatchStatusCard
                jobExecutionId={batchJobInfo.jobExecutionId}
                campaignId={batchJobInfo.campaignId}
                onComplete={handleBatchComplete}
                onError={handleBatchError}
              />
            )}
          </div>

          {/* 사이드바 */}
          <div className="space-y-6">
            {/* 수혜자 주소 설정 */}
            <BeneficiaryAddressForm
              initialAddress={campaign.beneficiaryAddress || ''}
              campaignId={campaign.id}
              onSubmit={async (address) => {
                try {
                  const updatedCampaign = await campaignApi.updateBeneficiaryAddress(campaign.id, address)
                  setCampaign(updatedCampaign)
                  toast.success('수혜자 주소가 성공적으로 업데이트되었습니다')
                } catch (error) {
                  console.error('수혜자 주소 업데이트 실패:', error)
                  let errorMessage = '수혜자 주소 업데이트에 실패했습니다'
                  if (error instanceof Error) {
                    errorMessage = error.message
                  }
                  toast.error('수혜자 주소 업데이트 실패', {
                    description: errorMessage
                  })
                  throw error
                }
              }}
              disabled={campaign.blockchainStatus === 'BLOCKCHAIN_PROCESSING' || campaign.blockchainStatus === 'ACTIVE'}
            />

            {/* 통계 정보 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="h-5 w-5" />
                  진행 현황
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600 dark:text-gray-400">진행률</span>
                    <span className="font-semibold">{progressPercentage.toFixed(1)}%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-teal-600 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${Math.min(progressPercentage, 100)}%` }}
                    />
                  </div>
                </div>

                <Separator />

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Target className="h-4 w-4 text-gray-500" />
                      <span className="text-sm text-gray-600 dark:text-gray-400">목표 금액</span>
                    </div>
                    <span className="font-semibold">{formatAmount(campaign.targetAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <TrendingUp className="h-4 w-4 text-green-500" />
                      <span className="text-sm text-gray-600 dark:text-gray-400">현재 금액</span>
                    </div>
                    <span className="font-semibold text-green-600">{formatAmount(campaign.currentAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Users className="h-4 w-4 text-blue-500" />
                      <span className="text-sm text-gray-600 dark:text-gray-400">기부자 수</span>
                    </div>
                    <span className="font-semibold">{campaign.donorCount}명</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 일정 정보 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Calendar className="h-5 w-5" />
                  캠페인 일정
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div>
                  <span className="text-sm text-gray-600 dark:text-gray-400">시작일</span>
                  <p className="font-medium">
                    {format(new Date(campaign.startDate), 'yyyy년 MM월 dd일 (E)', { locale: ko })}
                  </p>
                </div>
                <div>
                  <span className="text-sm text-gray-600 dark:text-gray-400">종료일</span>
                  <p className="font-medium">
                    {format(new Date(campaign.endDate), 'yyyy년 MM월 dd일 (E)', { locale: ko })}
                  </p>
                </div>
              </CardContent>
            </Card>

            {/* 블록체인 배포 상태 */}
            <BlockchainStatusDisplay
              status={campaign.blockchainStatus}
              transactionHash={campaign.blockchainTransactionHash}
              beneficiaryAddress={campaign.beneficiaryAddress}
              blockchainCampaignId={campaign.blockchainCampaignId}
              errorMessage={campaign.blockchainErrorMessage}
              processedAt={campaign.blockchainProcessedAt}
              onRetry={() => {
                // 실패 시 재시도 로직 (상태를 다시 ACTIVE로 변경)
                if (campaign.blockchainStatus === 'BLOCKCHAIN_FAILED') {
                  handleStatusChange('ACTIVE')
                }
              }}
            />

            {/* 캠페인 담당자 관리 */}
            <CampaignManagerList 
              campaignId={campaignId} 
              campaignTitle={campaign.title} 
            />

            {/* 시스템 정보 */}
            <Card>
              <CardHeader>
                <CardTitle>시스템 정보</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div>
                  <span className="text-gray-600 dark:text-gray-400">생성일시:</span>
                  <p>{format(new Date(campaign.createdAt), 'yyyy.MM.dd HH:mm:ss', { locale: ko })}</p>
                </div>
                <div>
                  <span className="text-gray-600 dark:text-gray-400">수정일시:</span>
                  <p>{format(new Date(campaign.updatedAt), 'yyyy.MM.dd HH:mm:ss', { locale: ko })}</p>
                </div>
                {isDeleted && campaign.deletedAt && (
                  <div>
                    <span className="text-red-600">삭제일시:</span>
                    <p className="text-red-600">{format(new Date(campaign.deletedAt), 'yyyy.MM.dd HH:mm:ss', { locale: ko })}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>

        {/* 삭제 확인 다이얼로그 */}
        <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>캠페인 삭제 확인</AlertDialogTitle>
              <AlertDialogDescription>
                "{campaign.title}" 캠페인을 정말 삭제하시겠습니까? 
                이 작업은 소프트 삭제로 처리되며, 나중에 복구할 수 있습니다.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleSoftDelete}
                className="bg-red-600 hover:bg-red-700"
              >
                삭제
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        {/* 복구 확인 다이얼로그 */}
        <AlertDialog open={restoreDialogOpen} onOpenChange={setRestoreDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>캠페인 복구 확인</AlertDialogTitle>
              <AlertDialogDescription>
                "{campaign.title}" 캠페인을 복구하시겠습니까?
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleRestore}
                className="bg-green-600 hover:bg-green-700"
              >
                복구
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        {/* 캠페인 마감 확인 다이얼로그 */}
        <AlertDialog open={closeCampaignDialogOpen} onOpenChange={setCloseCampaignDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>캠페인 마감 및 배치 작업 시작</AlertDialogTitle>
              <AlertDialogDescription className="space-y-3">
                <p>
                  "{campaign.title}" 캠페인을 마감하고 모든 기부 내역을 블록체인에 기록합니다.
                </p>
                <div className="bg-yellow-50 dark:bg-yellow-900/20 p-3 rounded-lg space-y-2 text-sm">
                  <p className="font-medium text-yellow-800 dark:text-yellow-200">⚠️ 주의사항</p>
                  <ul className="list-disc list-inside space-y-1 text-yellow-700 dark:text-yellow-300">
                    <li>배치 작업이 시작되면 취소할 수 없습니다</li>
                    <li>모든 기부 내역이 블록체인에 영구적으로 기록됩니다</li>
                    <li>작업은 수 분이 소요될 수 있습니다</li>
                  </ul>
                </div>
                <div className="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg space-y-1 text-sm">
                  <div className="flex justify-between">
                    <span>처리 대상 기부 건수:</span>
                    <span className="font-medium">{campaign.donorCount}건</span>
                  </div>
                  <div className="flex justify-between">
                    <span>총 기부 금액:</span>
                    <span className="font-medium">{formatAmount(campaign.currentAmount)}</span>
                  </div>
                  {campaign.beneficiaryAddress && (
                    <div className="flex justify-between">
                      <span>수혜자 주소:</span>
                      <span className="font-mono text-xs">{campaign.beneficiaryAddress.substring(0, 10)}...</span>
                    </div>
                  )}
                </div>
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleCloseCampaign}
                disabled={batchLoading}
                className="bg-blue-600 hover:bg-blue-700"
              >
                {batchLoading ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    시작 중...
                  </>
                ) : (
                  '배치 작업 시작'
                )}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        {/* FDS 미통과 거래 경고 다이얼로그 */}
        <AlertDialog open={fdsDialogOpen} onOpenChange={setFdsDialogOpen}>
          <AlertDialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
            <AlertDialogHeader>
              <AlertDialogTitle className="text-red-600">⚠️ FDS 검증 미통과 거래 발견</AlertDialogTitle>
              <AlertDialogDescription className="space-y-4">
                <p>
                  캠페인 마감이 불가능합니다. 다음 거래들이 FDS(사기 탐지 시스템) 검증을 통과하지 못했습니다.
                </p>
                <div className="bg-red-50 dark:bg-red-900/20 p-4 rounded-lg space-y-3">
                  <div className="flex items-center gap-2 text-red-800 dark:text-red-200 font-medium">
                    <span>미통과 거래 건수: {fdsUnverifiedDonations.length}건</span>
                  </div>
                  <p className="text-sm text-red-700 dark:text-red-300">
                    아래 거래들을 환불 처리한 후 다시 캠페인 마감을 시도해주세요.
                  </p>
                </div>

                {/* 미통과 거래 목록 */}
                <div className="space-y-3 max-h-96 overflow-y-auto">
                  {fdsUnverifiedDonations.map((donation) => (
                    <Card key={donation.id} className="border-red-200 dark:border-red-800">
                      <CardContent className="p-4 space-y-2">
                        <div className="flex justify-between items-start">
                          <div className="space-y-1">
                            <div className="flex items-center gap-2">
                              <span className="font-medium">기부 ID: {donation.id}</span>
                              <Badge variant="destructive" className="text-xs">
                                {donation.fdsAction === 'BLOCK' ? '차단' : donation.fdsAction === 'MANUAL_REVIEW' ? '수동검토' : 'FDS 미통과'}
                              </Badge>
                            </div>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                              결제 ID: {donation.paymentId}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                              기부자: {donation.donorName || '익명'}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                              금액: {formatAmount(donation.amount)}
                            </p>
                            {donation.fdsExplanation && (
                              <p className="text-sm text-red-600 dark:text-red-400">
                                사유: {formatFdsExplanation(donation.fdsExplanation)}
                              </p>
                            )}
                          </div>
                          <div className="flex flex-col gap-2">
                            <Link href={`/admin/donations?search=${donation.paymentId}`}>
                              <Button size="sm" variant="outline" className="w-full">
                                <Eye className="h-3 w-3 mr-1" />
                                상세보기
                              </Button>
                            </Link>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>

                <div className="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg text-sm">
                  <p className="font-medium text-blue-800 dark:text-blue-200 mb-2">💡 처리 방법</p>
                  <ol className="list-decimal list-inside space-y-1 text-blue-700 dark:text-blue-300">
                    <li>각 거래의 "상세보기" 버튼을 클릭하여 기부 관리 페이지로 이동</li>
                    <li>해당 거래를 환불 처리</li>
                    <li>모든 문제 거래를 환불한 후 다시 캠페인 마감 시도</li>
                  </ol>
                </div>
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogAction
                onClick={() => setFdsDialogOpen(false)}
                className="bg-blue-600 hover:bg-blue-700"
              >
                확인
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}