/**
 * 캠페인 목록 컴포넌트 (새로운 백엔드 API 연동)
 */
"use client"

import { useState, useEffect, useCallback } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { 
  Search, 
  Heart,
  Calendar,
  Target,
  Users,
  ChevronLeft,
  ChevronRight,
  Star
} from 'lucide-react'
import { campaignApi } from '@/lib/api/campaign-api'
import { CampaignListItem, CampaignListParams } from '@/types/donation'
import { formatCurrency } from '@/lib/utils'
import { useDebounce } from '@/hooks/use-debounce'
import { useFavorites } from '@/hooks/use-favorites'
import Link from 'next/link'
import Image from 'next/image'
import { cn } from '@/lib/utils'

// 카테고리 옵션 (백엔드 API에 맞춰 수정)
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
] as const

// 상태 옵션
const STATUS_OPTIONS = [
  { value: '', label: '전체 상태' },
  { value: 'ACTIVE', label: '진행 중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'DRAFT', label: '준비 중' }
] as const

// 정렬 옵션 (백엔드 API에 맞춰 수정)
const SORT_OPTIONS = [
  { value: 'recent', label: '최신순' },
  { value: 'popular', label: '인기순' },
  { value: 'progress', label: '달성률순' }
] as const

// 상태별 라벨
const STATUS_LABELS = {
  DRAFT: '준비중',
  ACTIVE: '진행중',
  COMPLETED: '완료',
  CANCELLED: '취소'
} as const

// 상태별 색상
const STATUS_COLORS = {
  DRAFT: 'secondary',
  ACTIVE: 'default',
  COMPLETED: 'success',
  CANCELLED: 'destructive'
} as const

interface CampaignListProps {
  className?: string
  title?: string
  showFilters?: boolean
  defaultFilters?: Partial<CampaignListParams>
  pageSize?: number
  gridClassName?: string
}

export function CampaignList({ 
  className, 
  title = "캠페인 목록",
  showFilters = true,
  defaultFilters = {},
  pageSize = 12,
  gridClassName
}: CampaignListProps) {
  const [campaigns, setCampaigns] = useState<CampaignListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  // 즐겨찾기 훅
  const { toggleFavorite, getCampaignInteraction } = useFavorites()
  
  // 필터 상태
  const [filters, setFilters] = useState<CampaignListParams>({
    category: undefined,
    status: undefined,
    keyword: '',
    sort: 'recent',
    page: 0,
    size: pageSize,
    ...defaultFilters
  })
  
  // 페이지네이션 상태
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalElements: 0,
    currentPage: 0,
    hasNext: false,
    hasPrevious: false
  })

  // 검색어 디바운스
  const debouncedKeyword = useDebounce(filters.keyword || '', 500)

  // 캠페인 목록 조회
  const fetchCampaigns = useCallback(async (params: CampaignListParams) => {
    try {
      setLoading(true)
      setError(null)
      
      console.log('🔍 캠페인 목록 조회:', params)
      
      const response = await campaignApi.getCampaigns(params)
      
      // getCampaigns는 이미 SpringPageResponse<CampaignListItem>을 직접 반환
      setCampaigns(response.content)
      setPagination({
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        currentPage: response.number,
        hasNext: !response.last,
        hasPrevious: !response.first
      })
      
      console.log('✅ 캠페인 목록 조회 성공:', {
        campaigns: response.content.length,
        totalElements: response.totalElements,
        currentPage: response.number
      })
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
  }, [])

  // 초기 로딩
  useEffect(() => {
    fetchCampaigns(filters)
  }, [])

  // 검색어 변경 시
  useEffect(() => {
    if (debouncedKeyword !== (filters.keyword || '')) {
      const newFilters = { ...filters, keyword: debouncedKeyword, page: 0 }
      setFilters(newFilters)
      fetchCampaigns(newFilters)
    }
  }, [debouncedKeyword, filters, fetchCampaigns])

  // 필터 변경 처리
  const handleFilterChange = (key: keyof CampaignListParams, value: any) => {
    const newFilters = { ...filters, [key]: value, page: 0 }
    setFilters(newFilters)
    fetchCampaigns(newFilters)
  }

  // 페이지 변경 처리
  const handlePageChange = (newPage: number) => {
    const newFilters = { ...filters, page: newPage }
    setFilters(newFilters)
    fetchCampaigns(newFilters)
  }

  // 즐겨찾기 토글
  const handleToggleFavorite = async (campaignId: number, e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    
    try {
      await toggleFavorite(campaignId.toString())
      // 필요시 캠페인 목록 새로고침
      // fetchCampaigns(filters)
    } catch (err) {
      console.error('즐겨찾기 토글 실패:', err)
    }
  }

  // 캠페인 카드 렌더링
  const renderCampaignCard = (campaign: CampaignListItem) => {
    const interaction = getCampaignInteraction(campaign.id.toString())
    const isFavorite = interaction?.isFavorite || false

    return (
      <Link key={campaign.id} href={`/campaign/${campaign.id}`}>
        <Card className="pt-0 h-full hover:shadow-lg transition-shadow cursor-pointer">
          {/* 캠페인 이미지 */}
          <div className="relative h-48 w-full">
            {campaign.imageUrl ? (
              <Image
                src={campaign.imageUrl}
                alt={campaign.title}
                fill
                className="object-cover rounded-t-lg"
              />
            ) : (
              <div className="h-full bg-muted flex items-center justify-center rounded-t-lg">
                <Heart className="h-12 w-12 text-muted-foreground" />
              </div>
            )}
            
            {/* 상태 뱃지 */}
            <Badge 
              variant={STATUS_COLORS[campaign.status] as any}
              className="absolute top-3 right-3 bg-white/90 hover:bg-white shadow-sm px-2 py-1.5"
            >
              {STATUS_LABELS[campaign.status]}
            </Badge>

            {/* 즐겨찾기 버튼 */}
            <Button
              size="sm"
              variant="ghost"
              className="absolute top-3 left-3 p-1.5 bg-white/80 hover:bg-white"
              onClick={(e) => handleToggleFavorite(campaign.id, e)}
            >
              <Star 
                className={cn(
                  "h-4 w-4",
                  isFavorite ? "fill-yellow-400 text-yellow-400" : "text-gray-600"
                )} 
              />
            </Button>
          </div>

          <CardContent className="px-4 pb-4">
            {/* 제목 */}
            <h3 className="font-semibold line-clamp-2">
              {campaign.title}
            </h3>
            
            {/* 단체명 */}
            <p className="text-sm text-muted-foreground mb-2">
              {campaign.organizer}
            </p>

            {/* 진행률 */}
            <div className="mb-3">
              <div className="flex justify-between items-center mb-1">
                <span className="text-sm text-muted-foreground">달성률</span>
                <span className="text-sm font-medium">
                  {campaign.progressPercentage.toFixed(1)}%
                </span>
              </div>
              <Progress value={campaign.progressPercentage} className="h-2" />
            </div>

            {/* 금액 정보 */}
            <div className="space-y-1 mb-3">
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">모금액</span>
                <span className="text-sm font-semibold">
                  {formatCurrency(campaign.currentAmount)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">목표금액</span>
                <span className="text-sm">
                  {formatCurrency(campaign.targetAmount)}
                </span>
              </div>
            </div>

            {/* 기타 정보 */}
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <div className="flex items-center gap-1">
                <Users className="h-3 w-3" />
                <span>{campaign.donorCount}명 후원</span>
              </div>
              <div className="flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                <span>~{new Date(campaign.endDate).toLocaleDateString()}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </Link>
    )
  }

  return (
    <div className={cn("space-y-6", className)}>
      {/* 필터 섹션 */}
      {showFilters && (
        <Card>
          <CardContent className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {/* 검색 */}
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="캠페인 검색..."
                  value={filters.keyword || ''}
                  onChange={(e) => handleFilterChange('keyword', e.target.value)}
                  className="pl-10"
                />
              </div>

              {/* 카테고리 */}
              <Select 
                value={filters.category || ''} 
                onValueChange={(value) => handleFilterChange('category', value || undefined)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="카테고리 선택" />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORY_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {/* 상태 */}
              <Select 
                value={filters.status || ''} 
                onValueChange={(value) => handleFilterChange('status', value || undefined)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="상태 선택" />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {/* 정렬 */}
              <Select 
                value={filters.sort || 'recent'} 
                onValueChange={(value) => handleFilterChange('sort', value)}
              >
                <SelectTrigger>
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
            </div>
          </CardContent>
        </Card>
      )}

      {/* 결과 섹션 */}
      {loading ? (
        <div className="flex justify-center items-center py-12">
          <LoadingSpinner className="w-8 h-8" />
        </div>
      ) : error ? (
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-destructive mb-4">오류가 발생했습니다</p>
            <p className="text-sm text-muted-foreground mb-4">{error}</p>
            <Button onClick={() => fetchCampaigns(filters)}>
              다시 시도
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* 캠페인 그리드 */}
          {campaigns.length > 0 ? (
            <div className={gridClassName || "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"}>
              {campaigns.map(renderCampaignCard)}
            </div>
          ) : (
            <Card>
              <CardContent className="py-12 text-center">
                <Heart className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                <p className="text-muted-foreground mb-2">조건에 맞는 캠페인이 없습니다</p>
                <p className="text-sm text-muted-foreground">다른 검색 조건을 시도해보세요</p>
              </CardContent>
            </Card>
          )}

          {/* 페이지네이션 */}
          {pagination.totalPages > 1 && (
            <div className="flex justify-center items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(pagination.currentPage - 1)}
                disabled={!pagination.hasPrevious}
              >
                <ChevronLeft className="h-4 w-4" />
                이전
              </Button>

              <div className="flex items-center gap-1">
                {Array.from({ length: Math.min(5, pagination.totalPages) }, (_, i) => {
                  const pageNum = Math.max(0, pagination.currentPage - 2) + i
                  if (pageNum >= pagination.totalPages) return null
                  
                  return (
                    <Button
                      key={pageNum}
                      variant={pageNum === pagination.currentPage ? "default" : "outline"}
                      size="sm"
                      onClick={() => handlePageChange(pageNum)}
                      className="w-10"
                    >
                      {pageNum + 1}
                    </Button>
                  )
                })}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => handlePageChange(pagination.currentPage + 1)}
                disabled={!pagination.hasNext}
              >
                다음
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}