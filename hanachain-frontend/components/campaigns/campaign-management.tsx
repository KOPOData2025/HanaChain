"use client"

import { useState, useEffect, useMemo } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { 
  Search, 
  Star, 
  Heart, 
  Calendar, 
  Users, 
  Target,
  TrendingUp,
  Filter,
  SortAsc,
  SortDesc,
  Clock
} from "lucide-react"
import { useFavorites } from "@/hooks/use-favorites"
import { campaignApi } from "@/lib/api/campaign-api"
import { CampaignListItem, CampaignListParams } from "@/types/donation"
import { formatCurrency, formatDate } from "@/lib/utils"
import { useDebounce } from "@/hooks/use-debounce"
import Image from "next/image"
import Link from "next/link"

const CATEGORY_OPTIONS = [
  { value: '', label: '전체 카테고리' },
  { value: 'MEDICAL', label: '의료 지원' },
  { value: 'EDUCATION', label: '교육 지원' },
  { value: 'DISASTER_RELIEF', label: '재해 구호' },
  { value: 'ENVIRONMENT', label: '환경 보호' },
  { value: 'ANIMAL_WELFARE', label: '동물 보호' },
  { value: 'COMMUNITY', label: '지역사회' },
  { value: 'EMERGENCY', label: '응급 상황' },
  { value: 'OTHER', label: '기타' }
]

const SORT_OPTIONS = [
  { value: 'recent', label: '최신순' },
  { value: 'popular', label: '인기순' },
  { value: 'progress', label: '달성률순' }
]

const STATUS_OPTIONS = [
  { value: '', label: '전체 상태' },
  { value: 'ACTIVE', label: '진행중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'DRAFT', label: '준비중' },
  { value: 'CANCELLED', label: '취소' }
]

const STATUS_LABELS = {
  DRAFT: '준비중',
  ACTIVE: '진행중',
  COMPLETED: '완료',
  CANCELLED: '취소'
} as const

const STATUS_COLORS = {
  DRAFT: 'secondary',
  ACTIVE: 'default', 
  COMPLETED: 'success',
  CANCELLED: 'destructive'
} as const

interface CampaignManagementProps {
  className?: string
}

export function CampaignManagement({ className }: CampaignManagementProps) {
  const { toggleFavorite, getFavoriteCampaigns, getDonatedCampaigns, getCampaignInteraction } = useFavorites()

  const [campaigns, setCampaigns] = useState<CampaignListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  const [activeTab, setActiveTab] = useState<'all' | 'favorites' | 'donated'>('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [selectedStatus, setSelectedStatus] = useState('')
  const [sortBy, setSortBy] = useState<'recent' | 'popular' | 'progress'>('recent')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')

  const debouncedSearchQuery = useDebounce(searchQuery, 300)

  // 캠페인 목록 조회
  const fetchCampaigns = async (params: CampaignListParams) => {
    try {
      setLoading(true)
      setError(null)
      
      const response = await campaignApi.getCampaigns(params)
      
      // getCampaigns는 이미 SpringPageResponse<CampaignListItem>을 직접 반환
      setCampaigns(response.content)
    } catch (err) {
      console.error('❌ 캠페인 목록 조회 실패:', err)
      
      // 401 에러인 경우 더 명확한 메시지 표시
      if (err instanceof Error && err.message.includes('401')) {
        console.log('ℹ️ 401 에러 감지 - 공개 API로 처리되어야 함')
        setError('인증 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
      } else {
        setError(err instanceof Error ? err.message : '캠페인 목록을 불러오는데 실패했습니다')
      }
    } finally {
      setLoading(false)
    }
  }

  // 현재 탭에 따른 캠페인 데이터
  const currentCampaigns = useMemo(() => {
    switch (activeTab) {
      case 'favorites':
        return getFavoriteCampaigns()
      case 'donated':
        return getDonatedCampaigns()
      default:
        return campaigns
    }
  }, [activeTab, campaigns, getFavoriteCampaigns, getDonatedCampaigns])

  // 필터 변경시 데이터 다시 로드 (전체 탭일 때만)
  useEffect(() => {
    if (activeTab === 'all') {
      const params: CampaignListParams = {
        keyword: debouncedSearchQuery || undefined,
        category: selectedCategory || undefined,
        status: selectedStatus || undefined,
        sort: sortBy,
        page: 0,
        size: 20
      }

      fetchCampaigns(params)
    }
  }, [activeTab, debouncedSearchQuery, selectedCategory, selectedStatus, sortBy, sortOrder])

  const handleFavoriteToggle = async (campaignId: string) => {
    try {
      await toggleFavorite(campaignId)
    } catch (err) {
      // 에러는 useFavorites 훅에서 처리됨
    }
  }

  const toggleSortOrder = () => {
    setSortOrder(prev => prev === 'asc' ? 'desc' : 'asc')
  }

  const getDaysLeft = (endDate: Date) => {
    const now = new Date()
    const diffTime = endDate.getTime() - now.getTime()
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    return diffDays
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Target className="h-5 w-5" />
          <span>내 기부 관리</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* 탭 네비게이션 */}
        <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as typeof activeTab)} className="space-y-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="all">전체 캠페인</TabsTrigger>
            <TabsTrigger value="favorites">즐겨찾기</TabsTrigger>
            <TabsTrigger value="donated">기부한 캠페인</TabsTrigger>
          </TabsList>

          {/* 검색 및 필터 */}
          <div className="space-y-4">
            <div className="flex flex-col space-y-3 md:flex-row md:items-center md:space-y-0 md:space-x-3">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="캠페인 검색..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>

              <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                <SelectTrigger className="w-full md:w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORY_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={selectedStatus} onValueChange={setSelectedStatus}>
                <SelectTrigger className="w-full md:w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center space-x-3">
              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger className="w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SORT_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Button
                variant="outline"
                size="sm"
                onClick={toggleSortOrder}
                className="flex items-center space-x-1"
              >
                {sortOrder === 'asc' ? (
                  <SortAsc className="h-4 w-4" />
                ) : (
                  <SortDesc className="h-4 w-4" />
                )}
              </Button>
            </div>
          </div>

          {error && (
            <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-4">
              <p className="text-destructive text-sm">{error}</p>
            </div>
          )}

          {/* 탭 콘텐츠 */}
          <TabsContent value={activeTab} className="space-y-4">
            {(loading && activeTab === 'all') ? (
              <div className="flex items-center justify-center py-12">
                <LoadingSpinner className="h-8 w-8" />
                <span className="ml-2 text-gray-600">캠페인을 불러오는 중...</span>
              </div>
            ) : currentCampaigns.length === 0 ? (
              <div className="text-center py-12">
                <Target className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  {activeTab === 'favorites' ? '즐겨찾기한 캠페인이 없습니다' :
                   activeTab === 'donated' ? '기부한 캠페인이 없습니다' :
                   '검색 결과가 없습니다'}
                </h3>
                <p className="text-gray-600">
                  {activeTab === 'favorites' ? '관심있는 캠페인을 즐겨찾기에 추가해보세요.' :
                   activeTab === 'donated' ? '새로운 캠페인에 기부해보세요.' :
                   '다른 검색어를 시도해보세요.'}
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {currentCampaigns.map((campaign) => {
                  const interaction = getCampaignInteraction(campaign.id.toString())
                  const daysLeft = getDaysLeft(new Date(campaign.endDate))
                  const isExpired = daysLeft < 0

                  return (
                    <Link key={campaign.id} href={`/campaign/${campaign.id}`}>
                      <Card className="group hover:shadow-lg transition-shadow cursor-pointer h-full">
                        <div className="relative">
                          <div className="aspect-video relative overflow-hidden rounded-t-lg">
                            {campaign.imageUrl ? (
                              <Image
                                src={campaign.imageUrl}
                                alt={campaign.title}
                                fill
                                className="object-cover group-hover:scale-105 transition-transform duration-300"
                              />
                            ) : (
                              <div className="w-full h-full bg-gray-200 flex items-center justify-center">
                                <Heart className="h-12 w-12 text-gray-400" />
                              </div>
                            )}
                            <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent" />
                          </div>
                          
                          <Button
                            variant="ghost"
                            size="sm"
                            className="absolute top-3 right-3 bg-white/90 hover:bg-white"
                            onClick={(e) => {
                              e.preventDefault()
                              e.stopPropagation()
                              handleFavoriteToggle(campaign.id.toString())
                            }}
                          >
                            <Star
                              className={`h-4 w-4 ${
                                interaction?.isFavorite 
                                  ? 'fill-yellow-400 text-yellow-400' 
                                  : 'text-gray-600'
                              }`}
                            />
                          </Button>

                          <div className="absolute top-3 left-3 flex space-x-2">
                            <Badge variant={STATUS_COLORS[campaign.status] as any}>
                              {STATUS_LABELS[campaign.status]}
                            </Badge>
                          </div>
                        </div>

                        <CardContent className="p-4">
                          <div className="space-y-3">
                            <div>
                              <h3 className="font-medium text-gray-900 line-clamp-2 min-h-[2.5rem]">
                                {campaign.title}
                              </h3>
                              <p className="text-sm text-gray-600 mt-1">
                                {campaign.creatorName}
                              </p>
                            </div>

                            <div className="space-y-2">
                              <Progress value={campaign.progressPercentage} className="h-2" />
                              <div className="flex items-center justify-between text-sm">
                                <span className="font-medium text-gray-900">
                                  {campaign.progressPercentage.toFixed(1)}% 달성
                                </span>
                                <span className="text-gray-600">
                                  {formatCurrency(campaign.currentAmount)}
                                </span>
                              </div>
                              <div className="text-sm text-gray-500">
                                목표: {formatCurrency(campaign.targetAmount)}
                              </div>
                            </div>

                            <div className="flex items-center justify-between text-sm text-gray-600">
                              <div className="flex items-center space-x-1">
                                <Users className="h-4 w-4" />
                                <span>{campaign.donorCount}명 참여</span>
                              </div>
                              <div className="flex items-center space-x-1">
                                <Calendar className="h-4 w-4" />
                                <span>
                                  {isExpired ? '마감됨' : `${daysLeft}일 남음`}
                                </span>
                              </div>
                            </div>

                            {interaction && interaction.donationCount > 0 && (
                              <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                                <div className="flex items-center space-x-2 text-green-800">
                                  <Heart className="h-4 w-4 fill-current" />
                                  <span className="text-sm font-medium">
                                    내가 기부한 캠페인
                                  </span>
                                </div>
                                <div className="text-sm text-green-700 mt-1">
                                  {formatCurrency(interaction.totalDonated)} · {interaction.donationCount}회
                                  {interaction.lastDonationDate && (
                                    <span className="ml-2">
                                      (최근 {formatDate(interaction.lastDonationDate)})
                                    </span>
                                  )}
                                </div>
                              </div>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                    </Link>
                  )
                })}
              </div>
            )}
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  )
}
