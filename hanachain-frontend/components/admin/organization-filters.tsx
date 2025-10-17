'use client'

import { useState } from 'react'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Search, X, Filter } from 'lucide-react'
import { Badge } from '@/components/ui/badge'

interface OrganizationFiltersProps {
  searchTerm: string
  statusFilter: string
  onSearchChange: (value: string) => void
  onStatusChange: (value: string) => void
  onClearFilters: () => void
  resultCount: number
  totalCount: number
}

export function OrganizationFilters({
  searchTerm,
  statusFilter,
  onSearchChange,
  onStatusChange,
  onClearFilters,
  resultCount,
  totalCount
}: OrganizationFiltersProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  
  const hasActiveFilters = searchTerm !== '' || statusFilter !== 'all'

  return (
    <Card>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Filter className="h-5 w-5" />
            필터 및 검색
          </CardTitle>
          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="text-xs">
              {resultCount}개 검색됨 / 전체 {totalCount}개
            </Badge>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsExpanded(!isExpanded)}
            >
              {isExpanded ? '축소' : '확장'}
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Main Search Bar */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="단체명 또는 설명으로 검색..."
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            className="pl-10 pr-10"
          />
          {searchTerm && (
            <Button
              variant="ghost"
              size="icon"
              className="absolute right-1 top-1/2 transform -translate-y-1/2 h-6 w-6"
              onClick={() => onSearchChange('')}
            >
              <X className="h-3 w-3" />
            </Button>
          )}
        </div>

        {/* Filter Controls */}
        <div className={`space-y-4 ${isExpanded ? 'block' : 'hidden md:block'}`}>
          <div className="flex flex-col md:flex-row gap-4">
            {/* Status Filter */}
            <div className="flex-1">
              <label className="text-sm font-medium mb-2 block">상태</label>
              <Select value={statusFilter} onValueChange={onStatusChange}>
                <SelectTrigger>
                  <SelectValue placeholder="상태 선택" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">모든 상태</SelectItem>
                  <SelectItem value="ACTIVE">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-green-500" />
                      활성
                    </div>
                  </SelectItem>
                  <SelectItem value="INACTIVE">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-gray-500" />
                      비활성
                    </div>
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Additional Filters (when expanded) */}
            {isExpanded && (
              <>
                <div className="flex-1">
                  <label className="text-sm font-medium mb-2 block">등록일</label>
                  <Select defaultValue="all">
                    <SelectTrigger>
                      <SelectValue placeholder="기간 선택" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체 기간</SelectItem>
                      <SelectItem value="today">오늘</SelectItem>
                      <SelectItem value="week">최근 1주일</SelectItem>
                      <SelectItem value="month">최근 1개월</SelectItem>
                      <SelectItem value="quarter">최근 3개월</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="flex-1">
                  <label className="text-sm font-medium mb-2 block">정렬 방식</label>
                  <Select defaultValue="newest">
                    <SelectTrigger>
                      <SelectValue placeholder="정렬 선택" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="newest">최신 등록순</SelectItem>
                      <SelectItem value="oldest">오래된 순</SelectItem>
                      <SelectItem value="name-asc">이름 오름차순</SelectItem>
                      <SelectItem value="name-desc">이름 내림차순</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </>
            )}
          </div>

          {/* Active Filters & Clear Button */}
          {hasActiveFilters && (
            <div className="flex items-center justify-between pt-2 border-t">
              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">활성 필터:</span>
                {searchTerm && (
                  <Badge variant="outline" className="gap-1">
                    검색: {searchTerm}
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-3 w-3 p-0 hover:bg-transparent"
                      onClick={() => onSearchChange('')}
                    >
                      <X className="h-2 w-2" />
                    </Button>
                  </Badge>
                )}
                {statusFilter !== 'all' && (
                  <Badge variant="outline" className="gap-1">
                    상태: {statusFilter === 'ACTIVE' ? '활성' : '비활성'}
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-3 w-3 p-0 hover:bg-transparent"
                      onClick={() => onStatusChange('all')}
                    >
                      <X className="h-2 w-2" />
                    </Button>
                  </Badge>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={onClearFilters}
                className="gap-1"
              >
                <X className="h-3 w-3" />
                전체 초기화
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}