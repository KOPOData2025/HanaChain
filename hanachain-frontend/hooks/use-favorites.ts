"use client"

import { useState, useCallback, useEffect } from "react"
import { FavoriteCampaign, CampaignInfo, CampaignFilter, UserCampaignInteraction } from "@/types/favorites"

// Mock 캠페인 데이터
const MOCK_CAMPAIGNS: CampaignInfo[] = [
  {
    id: "camp_001",
    title: "취약계층 아동 교육비 지원",
    description: "교육 기회가 부족한 취약계층 아동들에게 양질의 교육을 제공하기 위한 프로그램입니다.",
    targetAmount: 10000000,
    currentAmount: 7500000,
    progress: 75,
    category: "education",
    categoryLabel: "교육 지원",
    imageUrl: "/happy-children-playing.png",
    organizationName: "희망교육재단",
    endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
    status: "active",
    donorCount: 342,
    isUrgent: false
  },
  {
    id: "camp_002",
    title: "독거노인 돌봄 서비스",
    description: "홀로 지내시는 어르신들의 건강과 안전을 위한 종합 돌봄 서비스를 제공합니다.",
    targetAmount: 5000000,
    currentAmount: 4200000,
    progress: 84,
    category: "elderly",
    categoryLabel: "노인 지원",
    imageUrl: "/elderly-korean-contemplative.png",
    organizationName: "노인복지협회",
    endDate: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000),
    status: "active",
    donorCount: 189,
    isUrgent: true
  },
  {
    id: "camp_003",
    title: "유기동물 보호소 운영비 지원",
    description: "버려진 동물들에게 안전한 보금자리와 의료 서비스를 제공하는 보호소 운영을 돕습니다.",
    targetAmount: 3000000,
    currentAmount: 2800000,
    progress: 93,
    category: "animal",
    categoryLabel: "동물 보호",
    imageUrl: "/feeding-stray-cats.png",
    organizationName: "동물사랑협회",
    endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    status: "active",
    donorCount: 156,
    isUrgent: true
  },
  {
    id: "camp_004",
    title: "아프리카 식수 개발 프로젝트",
    description: "깨끗한 물을 구하기 어려운 아프리카 지역에 우물을 설치하여 식수를 공급합니다.",
    targetAmount: 8000000,
    currentAmount: 8000000,
    progress: 100,
    category: "international",
    categoryLabel: "국제 지원",
    imageUrl: "/african-refugee-family.png",
    organizationName: "글로벌워터",
    endDate: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000),
    status: "completed",
    donorCount: 287,
    isUrgent: false
  },
  {
    id: "camp_005",
    title: "환경보호 캠페인",
    description: "지구 환경 보호를 위한 다양한 활동과 교육 프로그램을 진행합니다.",
    targetAmount: 6000000,
    currentAmount: 3200000,
    progress: 53,
    category: "environment",
    categoryLabel: "환경 보호",
    imageUrl: "/construction-site-workers.png",
    organizationName: "그린어스",
    endDate: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000),
    status: "active",
    donorCount: 98,
    isUrgent: false
  }
]

const MOCK_USER_INTERACTIONS: UserCampaignInteraction[] = [
  {
    campaignId: "camp_001",
    isFavorite: true,
    totalDonated: 150000,
    donationCount: 3,
    lastDonationDate: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
    firstDonationDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  },
  {
    campaignId: "camp_002",
    isFavorite: true,
    totalDonated: 100000,
    donationCount: 2,
    lastDonationDate: new Date(Date.now() - 8 * 24 * 60 * 60 * 1000),
    firstDonationDate: new Date(Date.now() - 20 * 24 * 60 * 60 * 1000)
  },
  {
    campaignId: "camp_003",
    isFavorite: false,
    totalDonated: 50000,
    donationCount: 1,
    lastDonationDate: new Date(Date.now() - 12 * 24 * 60 * 60 * 1000),
    firstDonationDate: new Date(Date.now() - 12 * 24 * 60 * 60 * 1000)
  },
  {
    campaignId: "camp_004",
    isFavorite: true,
    totalDonated: 200000,
    donationCount: 4,
    lastDonationDate: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000),
    firstDonationDate: new Date(Date.now() - 45 * 24 * 60 * 60 * 1000)
  }
]

export function useFavorites() {
  const [campaigns, setCampaigns] = useState<CampaignInfo[]>([])
  const [userInteractions, setUserInteractions] = useState<UserCampaignInteraction[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 캠페인 데이터 로드
  const loadCampaigns = useCallback(async (filter: CampaignFilter = {}) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      let filteredCampaigns = [...MOCK_CAMPAIGNS]

      // 상태 필터링
      if (filter.status && filter.status !== 'all') {
        filteredCampaigns = filteredCampaigns.filter(campaign => campaign.status === filter.status)
      }

      // 카테고리 필터링
      if (filter.category) {
        filteredCampaigns = filteredCampaigns.filter(campaign => campaign.category === filter.category)
      }

      // 검색 필터링
      if (filter.searchQuery) {
        const query = filter.searchQuery.toLowerCase()
        filteredCampaigns = filteredCampaigns.filter(campaign =>
          campaign.title.toLowerCase().includes(query) ||
          campaign.description.toLowerCase().includes(query) ||
          campaign.organizationName.toLowerCase().includes(query)
        )
      }

      // 정렬
      if (filter.sortBy) {
        filteredCampaigns.sort((a, b) => {
          let aValue: any, bValue: any

          switch (filter.sortBy) {
            case 'latest':
              aValue = new Date(a.endDate).getTime()
              bValue = new Date(b.endDate).getTime()
              break
            case 'deadline':
              aValue = new Date(a.endDate).getTime()
              bValue = new Date(b.endDate).getTime()
              break
            case 'amount':
              aValue = a.currentAmount
              bValue = b.currentAmount
              break
            case 'progress':
              aValue = a.progress
              bValue = b.progress
              break
            default:
              aValue = a.title
              bValue = b.title
          }

          if (filter.sortOrder === 'desc') {
            return aValue > bValue ? -1 : aValue < bValue ? 1 : 0
          } else {
            return aValue < bValue ? -1 : aValue > bValue ? 1 : 0
          }
        })
      }

      setCampaigns(filteredCampaigns)
      setUserInteractions(MOCK_USER_INTERACTIONS)
      return filteredCampaigns
    } catch (err) {
      setError('캠페인 데이터를 불러오는데 실패했습니다.')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 즐겨찾기 추가/제거
  const toggleFavorite = useCallback(async (campaignId: string) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      setUserInteractions(prev => {
        const existing = prev.find(interaction => interaction.campaignId === campaignId)
        
        if (existing) {
          return prev.map(interaction =>
            interaction.campaignId === campaignId
              ? { ...interaction, isFavorite: !interaction.isFavorite }
              : interaction
          )
        } else {
          return [...prev, {
            campaignId,
            isFavorite: true,
            totalDonated: 0,
            donationCount: 0
          }]
        }
      })

      return true
    } catch (err) {
      setError('즐겨찾기 설정에 실패했습니다.')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 즐겨찾기 캠페인만 조회
  const getFavoriteCampaigns = useCallback(() => {
    const favoriteIds = userInteractions
      .filter(interaction => interaction.isFavorite)
      .map(interaction => interaction.campaignId)

    return campaigns.filter(campaign => favoriteIds.includes(campaign.id))
  }, [campaigns, userInteractions])

  // 기부한 캠페인만 조회
  const getDonatedCampaigns = useCallback(() => {
    const donatedIds = userInteractions
      .filter(interaction => interaction.donationCount > 0)
      .map(interaction => interaction.campaignId)

    return campaigns.filter(campaign => donatedIds.includes(campaign.id))
  }, [campaigns, userInteractions])

  // 특정 캠페인의 사용자 상호작용 정보 조회
  const getCampaignInteraction = useCallback((campaignId: string): UserCampaignInteraction | null => {
    return userInteractions.find(interaction => interaction.campaignId === campaignId) || null
  }, [userInteractions])

  // 초기 데이터 로드
  useEffect(() => {
    loadCampaigns()
  }, [loadCampaigns])

  return {
    campaigns,
    userInteractions,
    isLoading,
    error,
    loadCampaigns,
    toggleFavorite,
    getFavoriteCampaigns,
    getDonatedCampaigns,
    getCampaignInteraction,
    clearError: () => setError(null)
  }
}
