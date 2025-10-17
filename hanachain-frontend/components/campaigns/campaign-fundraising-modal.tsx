"use client"

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import {
  TrendingUp,
  Users,
  Calendar,
  Target,
  DollarSign,
  BarChart3,
  Clock
} from "lucide-react"
import { formatCurrency } from "@/lib/utils"
import { CampaignFundraisingStats } from "@/types/donation"
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend
} from "recharts"

interface CampaignFundraisingModalProps {
  isOpen: boolean
  onClose: () => void
  campaignTitle: string
  stats: CampaignFundraisingStats | null
  loading?: boolean
}

export function CampaignFundraisingModal({
  isOpen,
  onClose,
  campaignTitle,
  stats,
  loading = false
}: CampaignFundraisingModalProps) {

  // 로딩 중이거나 데이터가 없을 때
  if (loading || !stats) {
    return (
      <Dialog open={isOpen} onOpenChange={onClose}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>모금 정보</DialogTitle>
            <DialogDescription>
              {campaignTitle}의 상세 모금 정보를 확인하세요.
            </DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#009591]"></div>
          </div>
        </DialogContent>
      </Dialog>
    )
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold">모금 정보</DialogTitle>
          <DialogDescription className="text-base">
            {campaignTitle}의 상세 모금 정보를 확인하세요.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 mt-4">
          {/* 주요 지표 카드 */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* 현재 모금액 */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-muted-foreground">현재 모금액</span>
                  <DollarSign className="h-4 w-4 text-[#009591]" />
                </div>
                <p className="text-2xl font-bold text-[#009591]">
                  {formatCurrency(stats.currentAmount)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  목표: {formatCurrency(stats.targetAmount)}
                </p>
              </CardContent>
            </Card>

            {/* 달성률 */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-muted-foreground">달성률</span>
                  <Target className="h-4 w-4 text-[#009591]" />
                </div>
                <p className="text-2xl font-bold">
                  {stats.progressPercentage.toFixed(1)}%
                </p>
                <Progress value={stats.progressPercentage} className="mt-2 h-2" />
              </CardContent>
            </Card>

            {/* 총 기부자 수 */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-muted-foreground">총 기부자</span>
                  <Users className="h-4 w-4 text-[#009591]" />
                </div>
                <p className="text-2xl font-bold">{stats.donorCount}명</p>
                <p className="text-xs text-muted-foreground mt-1">
                  평균: {formatCurrency(stats.averageDonationAmount)}
                </p>
              </CardContent>
            </Card>

            {/* 남은 기간 */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-muted-foreground">남은 기간</span>
                  <Calendar className="h-4 w-4 text-[#009591]" />
                </div>
                <p className="text-2xl font-bold">
                  {stats.daysLeft > 0 ? `D-${stats.daysLeft}` : '종료됨'}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {new Date(stats.endDate).toLocaleDateString()}까지
                </p>
              </CardContent>
            </Card>
          </div>

          {/* 일별 기부 추이 차트 */}
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center mb-4">
                <BarChart3 className="h-5 w-5 text-[#009591] mr-2" />
                <h3 className="text-lg font-semibold">일별 기부 추이 (최근 7일)</h3>
              </div>

              {stats.dailyDonationTrend && stats.dailyDonationTrend.length > 0 ? (
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={stats.dailyDonationTrend}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="date"
                      tickFormatter={(value) => {
                        const date = new Date(value)
                        return `${date.getMonth() + 1}/${date.getDate()}`
                      }}
                    />
                    <YAxis />
                    <Tooltip
                      formatter={(value: number) => formatCurrency(value)}
                      labelFormatter={(label) => {
                        const date = new Date(label)
                        return date.toLocaleDateString('ko-KR')
                      }}
                    />
                    <Legend />
                    <Bar dataKey="amount" fill="#009591" name="기부액" />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  아직 기부 내역이 없습니다.
                </div>
              )}
            </CardContent>
          </Card>

          {/* 기부 건수 추이 */}
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center mb-4">
                <TrendingUp className="h-5 w-5 text-[#009591] mr-2" />
                <h3 className="text-lg font-semibold">기부 건수 추이</h3>
              </div>

              {stats.dailyDonationTrend && stats.dailyDonationTrend.length > 0 ? (
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={stats.dailyDonationTrend}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="date"
                      tickFormatter={(value) => {
                        const date = new Date(value)
                        return `${date.getMonth() + 1}/${date.getDate()}`
                      }}
                    />
                    <YAxis />
                    <Tooltip
                      labelFormatter={(label) => {
                        const date = new Date(label)
                        return date.toLocaleDateString('ko-KR')
                      }}
                    />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="count"
                      stroke="#009591"
                      strokeWidth={2}
                      name="기부 건수"
                    />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  아직 기부 내역이 없습니다.
                </div>
              )}
            </CardContent>
          </Card>

          {/* 상위 기부자 */}
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center mb-4">
                <Users className="h-5 w-5 text-[#009591] mr-2" />
                <h3 className="text-lg font-semibold">상위 기부자 (Top 5)</h3>
              </div>

              {stats.topDonations && stats.topDonations.length > 0 ? (
                <div className="space-y-3">
                  {stats.topDonations.map((donation, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between p-3 bg-muted/50 rounded-lg"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-[#009591] text-white font-semibold">
                          {index + 1}
                        </div>
                        <div>
                          <p className="font-medium">
                            {donation.anonymous ? '익명' : donation.donorName}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {new Date(donation.donatedAt).toLocaleDateString()}
                          </p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-[#009591]">
                          {formatCurrency(donation.amount)}
                        </p>
                        {donation.anonymous && (
                          <Badge variant="secondary" className="text-xs mt-1">
                            익명
                          </Badge>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  아직 기부 내역이 없습니다.
                </div>
              )}
            </CardContent>
          </Card>

          {/* 기간 정보 */}
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center mb-4">
                <Clock className="h-5 w-5 text-[#009591] mr-2" />
                <h3 className="text-lg font-semibold">캠페인 기간</h3>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">시작일</p>
                  <p className="font-medium">
                    {new Date(stats.startDate).toLocaleDateString('ko-KR', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric'
                    })}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground mb-1">종료일</p>
                  <p className="font-medium">
                    {new Date(stats.endDate).toLocaleDateString('ko-KR', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric'
                    })}
                  </p>
                </div>
              </div>
              {stats.daysLeft > 0 && (
                <div className="mt-4 p-3 bg-[#009591]/10 border border-[#009591]/20 rounded-lg">
                  <p className="text-sm text-[#009591] font-medium">
                    ⏰ 캠페인 종료까지 {stats.daysLeft}일 남았습니다.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </DialogContent>
    </Dialog>
  )
}
