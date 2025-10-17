'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  BarChart3,
  TrendingUp,
  Users,
  Building2,
  Megaphone,
  DollarSign,
  ArrowUpRight,
  ArrowDownRight,
  Plus,
  Eye,
  Activity,
  Calendar
} from 'lucide-react'
import { PieChart, Pie, Cell } from 'recharts'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import Link from 'next/link'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { campaignApi } from '@/lib/api/campaign-api'
import organizationApi from '@/lib/api/organization-api'
import { CampaignListItem } from '@/types/donation'
import { Organization } from '@/lib/api/organization-api'
import { cn } from '@/lib/utils'
import { DonationTrendsChart } from '@/components/admin/donation-trends-chart'
import { BatchRequiredCampaigns } from '@/components/admin/batch-required-campaigns'

interface DashboardStats {
  totalCampaigns: number
  activeCampaigns: number
  totalDonations: number
  totalOrganizations: number
  campaignGrowth: number
  donationGrowth: number
  organizationGrowth: number
  userGrowth: number
}

export default function AdminDashboard() {
  const [loading, setLoading] = useState(true)
  const [recentCampaigns, setRecentCampaigns] = useState<CampaignListItem[]>([])
  const [recentOrganizations, setRecentOrganizations] = useState<Organization[]>([])
  const [animatedProgress, setAnimatedProgress] = useState<Record<number, number>>({})
  const [displayStats, setDisplayStats] = useState<DashboardStats>({
    totalCampaigns: 0,
    activeCampaigns: 0,
    totalDonations: 0,
    totalOrganizations: 0,
    campaignGrowth: 12.5,
    donationGrowth: 8.2,
    organizationGrowth: 5.3,
    userGrowth: 15.8,
  })
  const [stats, setStats] = useState<DashboardStats>({
    totalCampaigns: 0,
    activeCampaigns: 0,
    totalDonations: 0,
    totalOrganizations: 0,
    campaignGrowth: 12.5,
    donationGrowth: 8.2,
    organizationGrowth: 5.3,
    userGrowth: 15.8,
  })

  useEffect(() => {
    fetchDashboardData()
  }, [])

  // 파이 차트 애니메이션
  useEffect(() => {
    if (loading || recentCampaigns.length === 0) return

    const duration = 2000 // 2초
    const steps = 60 // 60 프레임
    const stepDuration = duration / steps

    let currentStep = 0
    const interval = setInterval(() => {
      currentStep++
      const progress = currentStep / steps

      // easeOutCubic 이징 함수
      const easeProgress = 1 - Math.pow(1 - progress, 3)

      const newProgress: Record<number, number> = {}
      recentCampaigns.forEach(campaign => {
        newProgress[campaign.id] = campaign.progressPercentage * easeProgress
      })
      setAnimatedProgress(newProgress)

      if (currentStep >= steps) {
        clearInterval(interval)
        // 정확한 최종 값으로 설정
        const finalProgress: Record<number, number> = {}
        recentCampaigns.forEach(campaign => {
          finalProgress[campaign.id] = campaign.progressPercentage
        })
        setAnimatedProgress(finalProgress)
      }
    }, stepDuration)

    return () => clearInterval(interval)
  }, [recentCampaigns, loading])

  // 숫자 롤링 애니메이션
  useEffect(() => {
    if (loading) return

    const duration = 1500 // 1.5초 동안 애니메이션
    const steps = 60 // 60 프레임
    const stepDuration = duration / steps

    let currentStep = 0

    const interval = setInterval(() => {
      currentStep++
      const progress = currentStep / steps

      // easeOutCubic 이징 함수
      const easeProgress = 1 - Math.pow(1 - progress, 3)

      setDisplayStats({
        totalCampaigns: Math.floor(stats.totalCampaigns * easeProgress),
        activeCampaigns: Math.floor(stats.activeCampaigns * easeProgress),
        totalDonations: Math.floor(stats.totalDonations * easeProgress),
        totalOrganizations: Math.floor(stats.totalOrganizations * easeProgress),
        campaignGrowth: stats.campaignGrowth,
        donationGrowth: stats.donationGrowth,
        organizationGrowth: stats.organizationGrowth,
        userGrowth: stats.userGrowth,
      })

      if (currentStep >= steps) {
        clearInterval(interval)
        setDisplayStats(stats) // 정확한 최종 값으로 설정
      }
    }, stepDuration)

    return () => clearInterval(interval)
  }, [stats, loading])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      // 캠페인 데이터 가져오기
      const campaignResponse = await campaignApi.getAdminCampaigns({
        page: 0,
        size: 5,
      })
      setRecentCampaigns(campaignResponse.content)

      // 단체 데이터 가져오기
      const orgResponse = await organizationApi.getAllOrganizations(0, 5)
      setRecentOrganizations(orgResponse.content)

      // 통계 데이터 업데이트
      setStats(prev => ({
        ...prev,
        totalCampaigns: campaignResponse.totalElements,
        activeCampaigns: campaignResponse.content.filter(c => c.status === 'ACTIVE').length,
        totalDonations: campaignResponse.content.reduce((sum, c) => sum + c.currentAmount, 0),
        totalOrganizations: orgResponse.totalElements,
      }))
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(amount)
  }

  const formatFullAmount = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
    }).format(amount)
  }

  const statCards = [
    {
      title: '총 캠페인',
      value: displayStats.totalCampaigns,
      icon: Megaphone,
      growth: displayStats.campaignGrowth,
      gradient: 'from-teal-400 to-teal-600',
      bgColor: 'bg-teal-50',
    },
    {
      title: '활성 캠페인',
      value: displayStats.activeCampaigns,
      icon: Activity,
      growth: displayStats.campaignGrowth,
      gradient: 'from-teal-500 to-cyan-600',
      bgColor: 'bg-teal-50',
    },
    {
      title: '총 모금액',
      value: displayStats.totalDonations,
      icon: DollarSign,
      growth: displayStats.donationGrowth,
      gradient: 'from-cyan-500 to-teal-600',
      bgColor: 'bg-cyan-50',
      isCurrency: true,
    },
    {
      title: '등록 단체',
      value: displayStats.totalOrganizations,
      icon: Building2,
      growth: displayStats.organizationGrowth,
      gradient: 'from-teal-600 to-emerald-600',
      bgColor: 'bg-teal-50',
    },
  ]

  return (
    <div className="p-8">
      {/* 페이지 헤더 */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">대시보드</h1>
        <p className="mt-2 text-gray-600 dark:text-gray-400">
          HanaChain 관리자 대시보드에 오신 것을 환영합니다
        </p>
      </div>

      {/* 통계 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {statCards.map((stat, index) => {
          const Icon = stat.icon
          const isPositive = stat.growth > 0

          return (
            <Card key={index} className="relative overflow-hidden">
              <CardContent className="p-6">
                <div className="flex items-center gap-3 mb-4">
                  <div className={cn('p-2.5 rounded-xl bg-gradient-to-br', stat.gradient)}>
                    <Icon className="h-6 w-6 text-white" />
                  </div>
                  <p className="text-lg text-gray-600 dark:text-gray-400 font-medium">
                    {stat.title}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {typeof stat.value === 'number'
                      ? stat.isCurrency
                        ? `${stat.value.toLocaleString('ko-KR')} 원`
                        : `${stat.value.toLocaleString('ko-KR')}건`
                      : stat.value}
                  </p>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      {/* 기부 금액 추이 & 배치 작업 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <DonationTrendsChart />
        <BatchRequiredCampaigns campaigns={recentCampaigns} onBatchExecuted={fetchDashboardData} />
      </div>

      {/* 최근 캠페인과 단체 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 최근 캠페인 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-xl font-semibold">최근 캠페인</CardTitle>
            <Link href="/admin/campaigns">
              <Button variant="ghost" size="sm">
                전체보기
                <ArrowUpRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="h-16 bg-gray-100 dark:bg-gray-800 animate-pulse rounded-lg" />
                ))}
              </div>
            ) : (
              <div className="divide-y divide-gray-200 dark:divide-gray-700">
                {recentCampaigns.map((campaign, index) => {
                  const currentProgress = animatedProgress[campaign.id] || 0

                  return (
                    <div
                      key={campaign.id}
                      className={`flex items-center justify-between p-3 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors first:pt-0 last:pb-0 animate-fade-in-up stagger-${index + 1}`}
                    >
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        {/* 캠페인 썸네일 이미지 */}
                        <div className="flex-shrink-0 w-12 h-12 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-700">
                          {campaign.imageUrl ? (
                            <img
                              src={campaign.imageUrl}
                              alt={campaign.title}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center">
                              <Megaphone className="w-6 h-6 text-gray-400" />
                            </div>
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <Link
                            href={`/admin/campaigns/${campaign.id}`}
                            className="font-medium text-gray-900 dark:text-white hover:text-teal-600 dark:hover:text-teal-400"
                          >
                            {campaign.title}
                          </Link>
                        </div>
                      </div>
                      <TooltipProvider>
                        <Tooltip delayDuration={0}>
                          <TooltipTrigger asChild>
                            <div className="ml-4 cursor-pointer flex items-center justify-center w-10 h-10">
                              <PieChart width={36} height={36}>
                                <Pie
                                  data={[
                                    { value: Math.min(currentProgress, 100) },
                                    { value: Math.max(100 - currentProgress, 0) }
                                  ]}
                                  cx={17}
                                  cy={17}
                                  innerRadius={7}
                                  outerRadius={14}
                                  startAngle={90}
                                  endAngle={-270}
                                  dataKey="value"
                                  animationDuration={0}
                                >
                                  <Cell fill="#0d9488" />
                                  <Cell fill="#e5e7eb" />
                                </Pie>
                              </PieChart>
                            </div>
                          </TooltipTrigger>
                          <TooltipContent
                            side="left"
                            className="bg-teal-600 text-white border-teal-700 px-3 py-2"
                          >
                            <div className="space-y-1">
                              <p className="text-xs font-semibold">모금 현황</p>
                              <div className="flex justify-between gap-4 text-xs">
                                <span className="text-teal-100">현재 금액:</span>
                                <span className="font-medium">{formatFullAmount(campaign.currentAmount)}</span>
                              </div>
                              <div className="flex justify-between gap-4 text-xs">
                                <span className="text-teal-100">목표 금액:</span>
                                <span className="font-medium">{formatFullAmount(campaign.targetAmount)}</span>
                              </div>
                              <div className="flex justify-between gap-4 text-xs border-t border-teal-500 pt-1 mt-1">
                                <span className="text-teal-100">달성률:</span>
                                <span className="font-semibold">{campaign.progressPercentage.toFixed(1)}%</span>
                              </div>
                            </div>
                          </TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    </div>
                  )
                })}
                {recentCampaigns.length === 0 && (
                  <p className="text-center text-gray-500 py-4">캠페인이 없습니다</p>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 최근 등록 단체 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-xl font-semibold">최근 등록 단체</CardTitle>
            <Link href="/admin/organizations">
              <Button variant="ghost" size="sm">
                전체보기
                <ArrowUpRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="h-16 bg-gray-100 dark:bg-gray-800 animate-pulse rounded-lg" />
                ))}
              </div>
            ) : (
              <div className="divide-y divide-gray-200 dark:divide-gray-700">
                {recentOrganizations.map((org, index) => (
                  <div
                    key={org.id}
                    className={`flex items-center justify-between p-3 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors first:pt-0 last:pb-0 animate-fade-in-up stagger-${index + 1}`}
                  >
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      {/* 단체 로고 이미지 */}
                      <div className="flex-shrink-0 w-12 h-12 rounded-full overflow-hidden bg-gray-100 dark:bg-gray-700">
                        {org.imageUrl ? (
                          <img
                            src={org.imageUrl}
                            alt={org.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center">
                            <Building2 className="w-6 h-6 text-gray-400" />
                          </div>
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <Link
                          href={`/admin/organizations/${org.id}`}
                          className="font-medium text-gray-900 dark:text-white hover:text-blue-600 dark:hover:text-blue-400"
                        >
                          {org.name}
                        </Link>
                      </div>
                    </div>
                    <div className="ml-4">
                      <span className={cn(
                        'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium',
                        org.status === 'ACTIVE'
                          ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
                          : 'bg-gray-100 text-gray-800 dark:bg-gray-900/20 dark:text-gray-400'
                      )}>
                        {org.status === 'ACTIVE' ? '활성' : '비활성'}
                      </span>
                    </div>
                  </div>
                ))}
                {recentOrganizations.length === 0 && (
                  <p className="text-center text-gray-500 py-4">등록된 단체가 없습니다</p>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 빠른 액션 */}
      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-lg font-semibold">빠른 액션</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Link href="/admin/campaigns/create">
              <Button className="w-full" variant="outline">
                <Plus className="mr-2 h-4 w-4" />
                새 캠페인 등록
              </Button>
            </Link>
            <Link href="/admin/campaigns">
              <Button className="w-full" variant="outline">
                <Eye className="mr-2 h-4 w-4" />
                캠페인 관리
              </Button>
            </Link>
            <Link href="/admin/organizations">
              <Button className="w-full" variant="outline">
                <Building2 className="mr-2 h-4 w-4" />
                단체 관리
              </Button>
            </Link>
            <Button className="w-full" variant="outline" disabled>
              <BarChart3 className="mr-2 h-4 w-4" />
              보고서 생성
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}