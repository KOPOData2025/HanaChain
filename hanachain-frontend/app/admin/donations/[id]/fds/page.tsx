'use client'

import { useState, useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import {
  ArrowLeft,
  Shield,
  ShieldCheck,
  ShieldAlert,
  ShieldQuestion,
  AlertCircle,
  CheckCircle2,
  XCircle,
  TrendingUp,
  Activity,
  Clock,
  DollarSign,
  User,
  CreditCard,
  Bot
} from 'lucide-react'
import { BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie } from 'recharts'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Progress } from '@/components/ui/progress'

import { adminDonationApi, AdminDonation } from '@/lib/api/admin-donation-api'
import { FdsDetailResult, FdsFeatures, FDS_FEATURE_LABELS, FDS_FEATURE_GROUPS } from '@/types/admin'
import { ApiError } from '@/lib/api/client'
import { cn } from '@/lib/utils'

export default function FdsDetailPage() {
  const router = useRouter()
  const params = useParams()
  const donationId = Number(params.id)

  // 상태 관리
  const [loading, setLoading] = useState(true)
  const [donation, setDonation] = useState<AdminDonation | null>(null)
  const [fdsDetail, setFdsDetail] = useState<FdsDetailResult | null>(null)
  const [actionDialogOpen, setActionDialogOpen] = useState(false)
  const [actionType, setActionType] = useState<'approve' | 'block'>('approve')
  const [actionReason, setActionReason] = useState('')
  const [processing, setProcessing] = useState(false)
  const [displayedText, setDisplayedText] = useState('')
  const [isTyping, setIsTyping] = useState(false)

  // 데이터 로드
  useEffect(() => {
    fetchData()
  }, [donationId])

  // 타이핑 효과
  useEffect(() => {
    if (fdsDetail?.explanation && !isTyping) {
      setIsTyping(true)
      setDisplayedText('')

      let currentIndex = 0
      const text = fdsDetail.explanation

      const typingInterval = setInterval(() => {
        if (currentIndex < text.length) {
          setDisplayedText(text.substring(0, currentIndex + 1))
          currentIndex++
        } else {
          clearInterval(typingInterval)
          setIsTyping(false)
        }
      }, 30) // 30ms마다 한 글자씩

      return () => clearInterval(typingInterval)
    }
  }, [fdsDetail?.explanation])

  const fetchData = async () => {
    setLoading(true)
    try {
      // 기부 정보와 FDS 상세 정보를 동시에 가져오기
      const [donationData, fdsData] = await Promise.all([
        adminDonationApi.getDonationById(donationId),
        adminDonationApi.getFdsDetail(donationId)
      ])

      setDonation(donationData)
      setFdsDetail(fdsData)
    } catch (error) {
      console.error('Failed to fetch FDS detail:', error)

      let errorMessage = '데이터를 불러오는데 실패했습니다'
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

  // 관리자 액션 처리
  const handleAction = async () => {
    if (!actionReason.trim()) {
      toast.error('처리 사유를 입력해주세요')
      return
    }

    setProcessing(true)
    try {
      await adminDonationApi.overrideFdsResult(donationId, {
        action: actionType,
        reason: actionReason
      })

      toast.success(`기부가 ${actionType === 'approve' ? '승인' : '차단'}되었습니다`)
      setActionDialogOpen(false)
      setActionReason('')

      // 데이터 새로고침
      await fetchData()
    } catch (error) {
      console.error('Failed to process action:', error)

      let errorMessage = '처리에 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = adminDonationApi.handleApiError(error)
      }

      toast.error('처리 실패', {
        description: errorMessage
      })
    } finally {
      setProcessing(false)
    }
  }

  // FDS 액션 뱃지 색상
  const getFdsActionBadgeVariant = (action: string) => {
    switch (action) {
      case 'APPROVE': return 'success'
      case 'MANUAL_REVIEW': return 'warning'
      case 'BLOCK': return 'default' // 커스텀 스타일 적용을 위해 default로 변경
      default: return 'secondary'
    }
  }

  // FDS 액션 뱃지 커스텀 스타일
  const getFdsActionBadgeStyle = (action: string) => {
    if (action === 'BLOCK') {
      return 'bg-red-400 text-white hover:bg-red-500 border-red-400'
    }
    if (action === 'APPROVE') {
      return 'bg-green-600 text-white hover:bg-green-700 border-green-600'
    }
    return ''
  }

  // FDS 액션 아이콘
  const getFdsActionIcon = (action: string) => {
    switch (action) {
      case 'APPROVE': return <ShieldCheck className="h-4 w-4" />
      case 'MANUAL_REVIEW': return <ShieldQuestion className="h-4 w-4" />
      case 'BLOCK': return <ShieldAlert className="h-4 w-4" />
      default: return <Shield className="h-4 w-4" />
    }
  }

  // 위험도 색상
  const getRiskScoreColor = (score: number) => {
    if (score >= 0.7) return 'text-red-600'
    if (score >= 0.4) return 'text-orange-600'
    return 'text-green-600'
  }

  const getRiskScoreBg = (score: number) => {
    if (score >= 0.7) return 'bg-red-100'
    if (score >= 0.4) return 'bg-orange-100'
    return 'bg-green-100'
  }

  // 금액 포맷팅
  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount)
  }

  // Q-values 차트 데이터
  const getQValuesChartData = () => {
    if (!fdsDetail || !fdsDetail.qValues) return []

    return [
      {
        name: 'APPROVE',
        label: '승인',
        value: fdsDetail.qValues.approve || 0,
        selected: fdsDetail.actionId === 0
      },
      {
        name: 'MANUAL_REVIEW',
        label: '수동 검토',
        value: fdsDetail.qValues.manualReview || 0,
        selected: fdsDetail.actionId === 1
      },
      {
        name: 'BLOCK',
        label: '차단',
        value: fdsDetail.qValues.block || 0,
        selected: fdsDetail.actionId === 2
      }
    ]
  }

  // 특징 값 포맷팅
  const formatFeatureValue = (key: keyof FdsFeatures, value: number | undefined | null): string => {
    // null/undefined 체크
    if (value === null || value === undefined || isNaN(value)) {
      return '-'
    }

    if (key === 'hourOfDay') {
      return `${Math.floor(value)}시`
    }
    if (key === 'dayOfWeek') {
      const days = ['월', '화', '수', '목', '금', '토', '일']
      return days[Math.floor(value)] || value.toString()
    }
    if (key === 'isWeekend' || key === 'isCreditCard' || key === 'isBankTransfer') {
      return value === 1 ? '예' : '아니오'
    }
    if (value >= 0 && value <= 1) {
      return `${(value * 100).toFixed(1)}%`
    }
    return value.toFixed(2)
  }

  // 데이터 로딩 중
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <Activity className="h-8 w-8 animate-spin mx-auto mb-4" />
          <p className="text-gray-600 dark:text-gray-400">데이터를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (!donation || !fdsDetail) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-red-600">
              <XCircle className="h-5 w-5" />
              데이터를 찾을 수 없습니다
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              요청한 FDS 검증 결과를 찾을 수 없습니다.
            </p>
            <Button onClick={() => router.back()} variant="outline" className="w-full">
              <ArrowLeft className="h-4 w-4 mr-2" />
              돌아가기
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  // FDS 상세 정보가 없는 경우 (기존 데이터)
  const hasDetailedInfo = fdsDetail.qValues || fdsDetail.features

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8">
        {/* 헤더 */}
        <div className="mb-6">
          <Button
            variant="ghost"
            onClick={() => router.back()}
            className="mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            돌아가기
          </Button>

          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                FDS 검증 결과 상세
              </h1>
              <p className="text-gray-600 dark:text-gray-400">
                기부 ID: {donation.id} | {format(new Date(fdsDetail.checkedAt), 'yyyy년 MM월 dd일 HH:mm', { locale: ko })}
              </p>
            </div>

            <Badge
              variant={getFdsActionBadgeVariant(fdsDetail.action)}
              className={cn(
                "flex items-center gap-1 w-fit cursor-pointer hover:opacity-80 transition-opacity",
                getFdsActionBadgeStyle(fdsDetail.action)
              )}
            >
              {getFdsActionIcon(fdsDetail.action)}
              {fdsDetail.action}
            </Badge>
          </div>
        </div>

        {/* 기부 정보 요약 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-2xl font-bold">
              기부 정보
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">기부자</div>
                <div className="font-medium">{donation.anonymous ? '익명' : donation.donorName || '미제공'}</div>
              </div>
              <div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">금액</div>
                <div className="font-bold text-lg">{formatAmount(donation.amount)}</div>
              </div>
              <div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">캠페인</div>
                <div className="font-medium">{donation.campaignTitle}</div>
              </div>
              <div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">결제 방법</div>
                <div className="font-medium flex items-center gap-1">
                  <CreditCard className="h-4 w-4" />
                  {donation.paymentMethod === 'CREDIT_CARD' ? '신용카드' : donation.paymentMethod}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 위험도 및 신뢰도 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertCircle className="h-5 w-5" />
                위험도 점수
              </CardTitle>
              <CardDescription>AI 모델이 분석한 거래 위험도</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex justify-center items-center">
                <ResponsiveContainer width="100%" height={250}>
                  <PieChart>
                    <Pie
                      data={[
                        { name: '위험도', value: fdsDetail.riskScore * 100 },
                        { name: '안전도', value: (1 - fdsDetail.riskScore) * 100 }
                      ]}
                      cx="50%"
                      cy="50%"
                      innerRadius={65}
                      outerRadius={100}
                      paddingAngle={2}
                      dataKey="value"
                    >
                      <Cell fill={
                        fdsDetail.riskScore >= 0.7 ? '#dc2626' :
                        fdsDetail.riskScore >= 0.4 ? '#ea580c' : '#16a34a'
                      } />
                      <Cell fill="#e5e7eb" />
                    </Pie>
                    <text
                      x="50%"
                      y="45%"
                      textAnchor="middle"
                      dominantBaseline="middle"
                      className="text-3xl font-bold"
                      fill={
                        fdsDetail.riskScore >= 0.7 ? '#dc2626' :
                        fdsDetail.riskScore >= 0.4 ? '#ea580c' : '#16a34a'
                      }
                    >
                      {(fdsDetail.riskScore * 100).toFixed(1)}%
                    </text>
                    <text
                      x="50%"
                      y="58%"
                      textAnchor="middle"
                      dominantBaseline="middle"
                      className="text-sm font-medium"
                      fill={
                        fdsDetail.riskScore >= 0.7 ? '#dc2626' :
                        fdsDetail.riskScore >= 0.4 ? '#ea580c' : '#16a34a'
                      }
                    >
                      {fdsDetail.riskScore >= 0.7 ? '높은 위험' :
                       fdsDetail.riskScore >= 0.4 ? '중간 위험' : '낮은 위험'}
                    </text>
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="h-5 w-5" />
                모델 신뢰도
              </CardTitle>
              <CardDescription>예측 결과에 대한 모델의 확신도</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex justify-center items-center">
                <ResponsiveContainer width="100%" height={250}>
                  <PieChart>
                    <Pie
                      data={[
                        { name: '신뢰도', value: fdsDetail.confidence * 100 },
                        { name: '불확실도', value: (1 - fdsDetail.confidence) * 100 }
                      ]}
                      cx="50%"
                      cy="50%"
                      innerRadius={65}
                      outerRadius={100}
                      paddingAngle={2}
                      dataKey="value"
                    >
                      <Cell fill="#00857D" />
                      <Cell fill="#e5e7eb" />
                    </Pie>
                    <text
                      x="50%"
                      y="45%"
                      textAnchor="middle"
                      dominantBaseline="middle"
                      className="text-3xl font-bold"
                      fill="#00857D"
                    >
                      {(fdsDetail.confidence * 100).toFixed(1)}%
                    </text>
                    <text
                      x="50%"
                      y="58%"
                      textAnchor="middle"
                      dominantBaseline="middle"
                      className="text-sm font-medium"
                      fill="#00857D"
                    >
                      {fdsDetail.confidence >= 0.8 ? '매우 높은 신뢰도' :
                       fdsDetail.confidence >= 0.6 ? '높은 신뢰도' :
                       fdsDetail.confidence >= 0.4 ? '중간 신뢰도' : '낮은 신뢰도'}
                    </text>
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* FDS 설명 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-xl font-bold">FDS 분석 결과</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-3">
              <Bot className="h-6 w-6 text-gray-600 dark:text-gray-400 flex-shrink-0" />
              <p className="text-base text-gray-700 dark:text-gray-300 leading-relaxed">
                {displayedText}
                {isTyping && <span className="animate-pulse">|</span>}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* 상세 정보 없음 경고 */}
        {!hasDetailedInfo && (
          <Alert className="mb-6 border-yellow-500 bg-yellow-50 dark:bg-yellow-950">
            <AlertCircle className="h-4 w-4 text-yellow-600" />
            <AlertDescription className="text-yellow-800 dark:text-yellow-200">
              <strong>주의:</strong> 이 기부는 상세한 FDS 분석 정보가 저장되지 않았습니다. 
              기본적인 위험도 점수와 액션 정보만 표시됩니다.
            </AlertDescription>
          </Alert>
        )}

        {/* DQN Q-Values 차트 */}
        {fdsDetail.qValues && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Activity className="h-5 w-5" />
                DQN 모델 Q-Values
              </CardTitle>
              <CardDescription>
                강화학습 모델이 각 액션에 대해 평가한 가치 점수
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={getQValuesChartData()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="label" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="value" name="Q-Value">
                    {getQValuesChartData().map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={entry.selected ? '#22c55e' : '#94a3b8'}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
              <div className="mt-4 text-sm text-gray-600 dark:text-gray-400">
                <p>✓ 녹색 막대는 모델이 선택한 액션입니다</p>
                <p>✓ Q-Value가 높을수록 모델이 해당 액션이 더 적절하다고 판단합니다</p>
              </div>
            </CardContent>
          </Card>
        )}

        {/* 입력 특징 시각화 */}
        {fdsDetail.features && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User className="h-5 w-5" />
                FDS 입력 특징
              </CardTitle>
              <CardDescription>
                AI 모델이 분석한 거래 및 사용자 행동 특징
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Tabs defaultValue="transaction" className="w-full">
                <TabsList className="grid w-full grid-cols-4">
                  <TabsTrigger value="transaction">거래 정보</TabsTrigger>
                  <TabsTrigger value="account">계정 정보</TabsTrigger>
                  <TabsTrigger value="history">기부 이력</TabsTrigger>
                  <TabsTrigger value="payment">결제 수단</TabsTrigger>
                </TabsList>

                {/* 거래 정보 탭 */}
                <TabsContent value="transaction" className="space-y-4 mt-4">
                  {Object.entries(FDS_FEATURE_GROUPS.transaction.features).map((_, index) => {
                    const key = FDS_FEATURE_GROUPS.transaction.features[index] as keyof FdsFeatures
                    const value = fdsDetail.features[key] ?? 0
                  return (
                    <div key={key} className="space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="text-sm font-medium">{FDS_FEATURE_LABELS[key]}</span>
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                          {formatFeatureValue(key, value)}
                        </span>
                      </div>
                      <Progress
                        value={key === 'hourOfDay' ? ((value ?? 0) / 24) * 100 :
                               key === 'dayOfWeek' ? ((value ?? 0) / 7) * 100 :
                               (value ?? 0) * 100}
                        className="h-2"
                      />
                    </div>
                  )
                })}
              </TabsContent>

              {/* 계정 정보 탭 */}
              <TabsContent value="account" className="space-y-4 mt-4">
                {Object.entries(FDS_FEATURE_GROUPS.account.features).map((_, index) => {
                  const key = FDS_FEATURE_GROUPS.account.features[index] as keyof FdsFeatures
                  const value = fdsDetail.features[key] ?? 0
                  return (
                    <div key={key} className="space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="text-sm font-medium">{FDS_FEATURE_LABELS[key]}</span>
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                          {formatFeatureValue(key, value)}
                        </span>
                      </div>
                      <Progress value={(value ?? 0) * 10} className="h-2" />
                      <p className="text-xs text-gray-500">
                        {(value ?? 0) < 0.3 ? '신규 계정' :
                         (value ?? 0) < 0.6 ? '일반 계정' : '오래된 계정'}
                      </p>
                    </div>
                  )
                })}
              </TabsContent>

              {/* 기부 이력 탭 */}
              <TabsContent value="history" className="space-y-4 mt-4">
                {Object.entries(FDS_FEATURE_GROUPS.history.features).map((_, index) => {
                  const key = FDS_FEATURE_GROUPS.history.features[index] as keyof FdsFeatures
                  const value = fdsDetail.features[key] ?? 0
                  return (
                    <div key={key} className="space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="text-sm font-medium">{FDS_FEATURE_LABELS[key]}</span>
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                          {formatFeatureValue(key, value)}
                        </span>
                      </div>
                      <Progress value={(value ?? 0) * 100} className="h-2" />
                    </div>
                  )
                })}
              </TabsContent>

              {/* 결제 수단 탭 */}
              <TabsContent value="payment" className="space-y-4 mt-4">
                {Object.entries(FDS_FEATURE_GROUPS.payment.features).map((_, index) => {
                  const key = FDS_FEATURE_GROUPS.payment.features[index] as keyof FdsFeatures
                  const value = fdsDetail.features[key] ?? 0
                  return (
                    <div key={key} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                      <div className="flex items-center gap-2">
                        <CreditCard className="h-4 w-4" />
                        <span className="text-sm font-medium">{FDS_FEATURE_LABELS[key]}</span>
                      </div>
                      {(value ?? 0) === 1 ? (
                        <CheckCircle2 className="h-5 w-5 text-green-600" />
                      ) : (
                        <XCircle className="h-5 w-5 text-gray-400" />
                      )}
                    </div>
                  )
                })}
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
        )}

        {/* 관리자 액션 (APPROVE가 아닌 경우) */}
        {fdsDetail.action !== 'APPROVE' && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-orange-600">
                <Clock className="h-5 w-5" />
                관리자 처리 필요
              </CardTitle>
              <CardDescription>
                FDS 시스템이 {fdsDetail.action === 'MANUAL_REVIEW' ? '수동 검토' : '차단'}를 권장했습니다.
                관리자가 직접 검토하여 최종 결정을 내려주세요.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {fdsDetail.action === 'MANUAL_REVIEW' ? (
                  <div className="grid grid-cols-2 gap-4">
                    <Button
                      onClick={() => {
                        setActionType('approve')
                        setActionDialogOpen(true)
                      }}
                      className="bg-green-600 hover:bg-green-700"
                    >
                      <CheckCircle2 className="h-4 w-4 mr-2" />
                      승인 처리
                    </Button>
                    <Button
                      onClick={() => {
                        setActionType('block')
                        setActionDialogOpen(true)
                      }}
                      variant="destructive"
                    >
                      <XCircle className="h-4 w-4 mr-2" />
                      거부 처리
                    </Button>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    <Button
                      onClick={() => {
                        setActionType('approve')
                        setActionDialogOpen(true)
                      }}
                      variant="outline"
                      className="border-orange-500 text-orange-600 hover:bg-orange-50"
                    >
                      <CheckCircle2 className="h-4 w-4 mr-2" />
                      강제 승인 (주의)
                    </Button>
                    <Button
                      onClick={() => {
                        setActionType('block')
                        setActionDialogOpen(true)
                      }}
                      variant="destructive"
                    >
                      <XCircle className="h-4 w-4 mr-2" />
                      차단 확정
                    </Button>
                  </div>
                )}

                <Alert>
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>
                    <strong>참고:</strong> 처리 후에는 되돌릴 수 없습니다.
                    신중하게 검토한 후 결정해주세요.
                  </AlertDescription>
                </Alert>
              </div>
            </CardContent>
          </Card>
        )}

        {/* 액션 확인 다이얼로그 */}
        <AlertDialog open={actionDialogOpen} onOpenChange={setActionDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>
                {actionType === 'approve' ? '기부 승인 확인' : '기부 차단 확인'}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {actionType === 'approve'
                  ? 'FDS 시스템의 권장과 다르게 이 기부를 승인하시겠습니까?'
                  : '이 기부를 차단하고 환불 처리하시겠습니까?'}
              </AlertDialogDescription>
            </AlertDialogHeader>

            <div className="space-y-4 py-4">
              <div>
                <label className="text-sm font-medium mb-2 block">
                  처리 사유 <span className="text-red-500">*</span>
                </label>
                <Textarea
                  value={actionReason}
                  onChange={(e) => setActionReason(e.target.value)}
                  placeholder="처리 사유를 상세히 입력해주세요..."
                  rows={4}
                  className="resize-none"
                />
              </div>

              <Alert variant={actionType === 'approve' ? 'default' : 'destructive'}>
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  {actionType === 'approve'
                    ? '승인 후에는 되돌릴 수 없습니다. 신중하게 검토해주세요.'
                    : '차단 및 환불 처리 후에는 되돌릴 수 없습니다.'}
                </AlertDescription>
              </Alert>
            </div>

            <AlertDialogFooter>
              <AlertDialogCancel disabled={processing}>
                취소
              </AlertDialogCancel>
              <AlertDialogAction
                onClick={handleAction}
                disabled={processing || !actionReason.trim()}
                className={actionType === 'approve' ? 'bg-green-600 hover:bg-green-700' : ''}
              >
                {processing ? '처리 중...' : actionType === 'approve' ? '승인 처리' : '차단 처리'}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}
