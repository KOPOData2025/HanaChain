"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { DonationOverviewChart } from "@/components/charts/donation-overview-chart"
import { DonationTrendChart } from "@/components/charts/donation-trend-chart"
import { GoalProgressChart } from "@/components/charts/goal-progress-chart"
import { 
  BarChart3, 
  TrendingUp, 
  Target, 
  Calendar,
  DollarSign,
  Heart,
  Award,
  Users
} from "lucide-react"
import { useStatistics } from "@/hooks/use-statistics"
import { StatisticsTimeRange } from "@/types/statistics"
import { formatCurrency, formatDate } from "@/lib/utils"

interface DonationStatisticsProps {
  className?: string
}

const TIME_RANGE_OPTIONS = [
  { value: 'all', label: '전체' },
  { value: '1y', label: '최근 1년' },
  { value: '6m', label: '최근 6개월' },
  { value: '3m', label: '최근 3개월' },
  { value: '1m', label: '최근 1개월' }
] as const

export function DonationStatistics({ className }: DonationStatisticsProps) {
  const { statistics, isLoading, error, timeRange, fetchStatistics, clearError } = useStatistics()
  const [selectedRange, setSelectedRange] = useState<StatisticsTimeRange['range']>('all')

  useEffect(() => {
    fetchStatistics({ range: selectedRange })
  }, [fetchStatistics, selectedRange])

  const handleTimeRangeChange = (range: StatisticsTimeRange['range']) => {
    setSelectedRange(range)
  }

  if (isLoading && !statistics) {
    return (
      <div className={`flex items-center justify-center py-12 ${className}`}>
        <LoadingSpinner className="h-8 w-8" />
        <span className="ml-2 text-gray-600">통계를 불러오는 중...</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className={className}>
        <ErrorMessage message={error} />
      </div>
    )
  }

  if (!statistics) {
    return (
      <div className={`text-center py-12 ${className}`}>
        <BarChart3 className="h-12 w-12 mx-auto text-gray-400 mb-4" />
        <h3 className="text-lg font-medium text-gray-900 mb-2">
          통계 데이터가 없습니다
        </h3>
        <p className="text-gray-600">
          기부를 시작하면 통계를 확인할 수 있습니다.
        </p>
      </div>
    )
  }

  const { overview, categoryBreakdown, monthlyTrends, topCampaigns, personalGoals } = statistics

  return (
    <div className={`space-y-6 ${className}`}>
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">기부 통계</h2>
          <p className="text-gray-600 mt-1">나의 기부 현황을 한눈에 확인하세요</p>
        </div>
        
        <Select value={selectedRange} onValueChange={handleTimeRangeChange}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {TIME_RANGE_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* 주요 지표 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <div className="p-2 bg-green-100 rounded-lg">
                <DollarSign className="h-5 w-5 text-green-600" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-600">총 기부 금액</p>
                <p className="text-2xl font-bold text-gray-900">
                  {formatCurrency(overview.totalAmount)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Heart className="h-5 w-5 text-blue-600" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-600">총 기부 횟수</p>
                <p className="text-2xl font-bold text-gray-900">
                  {overview.totalDonations}회
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Users className="h-5 w-5 text-purple-600" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-600">지원한 캠페인</p>
                <p className="text-2xl font-bold text-gray-900">
                  {overview.totalCampaigns}개
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <div className="p-2 bg-orange-100 rounded-lg">
                <Award className="h-5 w-5 text-orange-600" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-600">평균 기부액</p>
                <p className="text-2xl font-bold text-gray-900">
                  {formatCurrency(overview.averageDonation)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 차트 섹션 */}
      <Tabs defaultValue="overview" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview">카테고리별 분석</TabsTrigger>
          <TabsTrigger value="trends">기부 추이</TabsTrigger>
          <TabsTrigger value="goals">목표 달성</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* 카테고리별 기부 분석 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <BarChart3 className="h-5 w-5" />
                  <span>카테고리별 기부 분석</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <DonationOverviewChart data={categoryBreakdown} />
              </CardContent>
            </Card>

            {/* 상위 기부 캠페인 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Award className="h-5 w-5" />
                  <span>주요 기부 캠페인</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {topCampaigns.map((campaign, index) => (
                    <div key={campaign.campaignId} className="flex items-center space-x-3">
                      <div className="flex-shrink-0 w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                        <span className="text-sm font-medium text-gray-600">{index + 1}</span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <h4 className="text-sm font-medium text-gray-900 truncate">
                          {campaign.campaignTitle}
                        </h4>
                        <div className="flex items-center space-x-4 text-xs text-gray-500">
                          <span>{formatCurrency(campaign.totalDonated)}</span>
                          <span>{campaign.donationCount}회</span>
                          <span>{formatDate(campaign.lastDonationDate)}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="trends" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* 월별 기부 금액 추이 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <TrendingUp className="h-5 w-5" />
                  <span>월별 기부 금액 추이</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <DonationTrendChart 
                  data={monthlyTrends} 
                  type="line" 
                  metric="amount" 
                />
              </CardContent>
            </Card>

            {/* 월별 기부 횟수 추이 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Calendar className="h-5 w-5" />
                  <span>월별 기부 횟수 추이</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <DonationTrendChart 
                  data={monthlyTrends} 
                  type="bar" 
                  metric="count" 
                />
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="goals" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Target className="h-5 w-5" />
                <span>개인 기부 목표</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <GoalProgressChart goals={personalGoals} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* 기부 스트릭 정보 */}
      {overview.streakDays > 0 && (
        <Card className="bg-gradient-to-r from-green-50 to-blue-50 border-green-200">
          <CardContent className="p-6">
            <div className="flex items-center space-x-4">
              <div className="p-3 bg-green-100 rounded-full">
                <Award className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <h3 className="font-medium text-gray-900">연속 기부 스트릭</h3>
                <p className="text-sm text-gray-600">
                  {overview.streakDays}일 연속으로 기부 활동을 하고 있어요! 
                  {overview.lastDonationDate && (
                    <span className="ml-1">
                      마지막 기부: {formatDate(overview.lastDonationDate)}
                    </span>
                  )}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
