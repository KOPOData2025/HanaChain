"use client"

import { useState, useCallback } from "react"
import { StatisticsSummary, StatisticsTimeRange, DonationStatistics, CategoryStatistics, MonthlyStatistics } from "@/types/statistics"

// Mock 데이터 생성 함수
const generateMockStatistics = (): StatisticsSummary => {
  const now = new Date()
  const lastYear = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate())

  // 기본 통계
  const overview: DonationStatistics = {
    totalAmount: 2580000,
    totalDonations: 47,
    totalCampaigns: 12,
    averageDonation: 54894,
    lastDonationDate: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000), // 3일 전
    streakDays: 15
  }

  // 카테고리별 통계
  const categoryBreakdown: CategoryStatistics[] = [
    {
      category: 'children',
      categoryLabel: '아동 지원',
      amount: 980000,
      count: 18,
      percentage: 38.0,
      color: '#FF6B6B'
    },
    {
      category: 'elderly',
      categoryLabel: '노인 지원',
      amount: 720000,
      count: 12,
      percentage: 27.9,
      color: '#4ECDC4'
    },
    {
      category: 'education',
      categoryLabel: '교육 지원',
      amount: 450000,
      count: 8,
      percentage: 17.4,
      color: '#45B7D1'
    },
    {
      category: 'environment',
      categoryLabel: '환경 보호',
      amount: 280000,
      count: 6,
      percentage: 10.9,
      color: '#96CEB4'
    },
    {
      category: 'animal',
      categoryLabel: '동물 보호',
      amount: 150000,
      count: 3,
      percentage: 5.8,
      color: '#FFEAA7'
    }
  ]

  // 월별 통계 (최근 12개월)
  const monthlyTrends: MonthlyStatistics[] = []
  for (let i = 11; i >= 0; i--) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const baseAmount = 150000 + Math.random() * 200000
    const seasonalMultiplier = (date.getMonth() === 11 || date.getMonth() === 0) ? 1.5 : 1 // 연말연시 증가
    
    monthlyTrends.push({
      month: date.toLocaleDateString('ko-KR', { month: 'short' }),
      year: date.getFullYear(),
      amount: Math.round(baseAmount * seasonalMultiplier),
      count: Math.round((baseAmount * seasonalMultiplier) / 50000),
      date
    })
  }

  // 연도별 트렌드 (최근 3년)
  const yearlyTrends = [
    {
      period: '2022',
      amount: 1890000,
      count: 32,
      growth: 0
    },
    {
      period: '2023',
      amount: 2150000,
      count: 38,
      growth: 13.8
    },
    {
      period: '2024',
      amount: 2580000,
      count: 47,
      growth: 20.0
    }
  ]

  // 상위 기부 캠페인
  const topCampaigns = [
    {
      campaignId: 'camp_001',
      campaignTitle: '취약계층 아동 교육비 지원',
      totalDonated: 450000,
      donationCount: 8,
      lastDonationDate: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000)
    },
    {
      campaignId: 'camp_002',
      campaignTitle: '독거노인 돌봄 서비스',
      totalDonated: 320000,
      donationCount: 6,
      lastDonationDate: new Date(now.getTime() - 8 * 24 * 60 * 60 * 1000)
    },
    {
      campaignId: 'camp_003',
      campaignTitle: '유기동물 보호소 운영',
      totalDonated: 280000,
      donationCount: 5,
      lastDonationDate: new Date(now.getTime() - 12 * 24 * 60 * 60 * 1000)
    }
  ]

  // 개인 목표
  const personalGoals = [
    {
      id: 'goal_001',
      title: '연간 기부 목표',
      targetAmount: 3000000,
      currentAmount: 2580000,
      targetDate: new Date(now.getFullYear(), 11, 31),
      isAchieved: false,
      progress: 86.0
    },
    {
      id: 'goal_002',
      title: '월간 기부 목표',
      targetAmount: 300000,
      currentAmount: 280000,
      targetDate: new Date(now.getFullYear(), now.getMonth(), 31),
      isAchieved: false,
      progress: 93.3
    }
  ]

  return {
    overview,
    categoryBreakdown,
    monthlyTrends,
    yearlyTrends,
    topCampaigns,
    personalGoals
  }
}

export function useStatistics() {
  const [statistics, setStatistics] = useState<StatisticsSummary | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [timeRange, setTimeRange] = useState<StatisticsTimeRange>({ range: 'all' })

  // 통계 데이터 조회
  const fetchStatistics = useCallback(async (range: StatisticsTimeRange = { range: 'all' }) => {
    setIsLoading(true)
    setError(null)
    setTimeRange(range)

    try {
      // Mock API 호출 시뮬레이션
      await new Promise(resolve => setTimeout(resolve, 800))

      const mockData = generateMockStatistics()
      
      // 시간 범위에 따른 데이터 필터링
      let filteredData = { ...mockData }
      
      if (range.range !== 'all') {
        const now = new Date()
        let startDate: Date
        
        switch (range.range) {
          case '1m':
            startDate = new Date(now.getFullYear(), now.getMonth(), 1)
            break
          case '3m':
            startDate = new Date(now.getFullYear(), now.getMonth() - 2, 1)
            break
          case '6m':
            startDate = new Date(now.getFullYear(), now.getMonth() - 5, 1)
            break
          case '1y':
            startDate = new Date(now.getFullYear() - 1, now.getMonth(), 1)
            break
          default:
            startDate = new Date(0)
        }

        // 월별 트렌드 필터링
        filteredData.monthlyTrends = mockData.monthlyTrends.filter(
          trend => trend.date >= startDate
        )

        // 범위에 따른 전체 통계 조정 (실제로는 서버에서 계산됨)
        const rangeMultiplier = range.range === '1m' ? 0.1 : 
                              range.range === '3m' ? 0.3 : 
                              range.range === '6m' ? 0.6 : 
                              range.range === '1y' ? 0.8 : 1

        filteredData.overview = {
          ...mockData.overview,
          totalAmount: Math.round(mockData.overview.totalAmount * rangeMultiplier),
          totalDonations: Math.round(mockData.overview.totalDonations * rangeMultiplier),
          totalCampaigns: Math.round(mockData.overview.totalCampaigns * rangeMultiplier)
        }
      }

      setStatistics(filteredData)
      return filteredData
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '통계 데이터를 불러오는데 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 목표 업데이트
  const updateGoal = useCallback(async (goalId: string, updates: Partial<any>) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      if (statistics) {
        const updatedGoals = statistics.personalGoals.map(goal =>
          goal.id === goalId ? { ...goal, ...updates } : goal
        )

        setStatistics(prev => prev ? {
          ...prev,
          personalGoals: updatedGoals
        } : null)
      }

      return { success: true }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '목표 업데이트에 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [statistics])

  // 새 목표 추가
  const addGoal = useCallback(async (goalData: Omit<any, 'id' | 'progress'>) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      const newGoal = {
        ...goalData,
        id: `goal_${Date.now()}`,
        progress: (goalData.currentAmount / goalData.targetAmount) * 100
      }

      if (statistics) {
        setStatistics(prev => prev ? {
          ...prev,
          personalGoals: [...prev.personalGoals, newGoal]
        } : null)
      }

      return newGoal
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '목표 추가에 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [statistics])

  return {
    statistics,
    isLoading,
    error,
    timeRange,
    fetchStatistics,
    updateGoal,
    addGoal,
    clearError: () => setError(null)
  }
}
