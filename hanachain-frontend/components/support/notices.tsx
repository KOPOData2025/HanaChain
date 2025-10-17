"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { 
  Search, 
  Megaphone, 
  Eye, 
  Calendar,
  AlertCircle,
  Settings,
  Gift,
  FileText
} from "lucide-react"
import { useSupport } from "@/hooks/use-support"
import { Notice } from "@/types/support"
import { formatDate } from "@/lib/utils"

const CATEGORY_LABELS = {
  general: '일반',
  maintenance: '점검',
  update: '업데이트',
  event: '이벤트'
} as const

const CATEGORY_ICONS = {
  general: FileText,
  maintenance: Settings,
  update: AlertCircle,
  event: Gift
} as const

const CATEGORY_COLORS = {
  general: 'bg-gray-100 text-gray-800',
  maintenance: 'bg-orange-100 text-orange-800',
  update: 'bg-blue-100 text-blue-800',
  event: 'bg-green-100 text-green-800'
} as const

interface NoticesProps {
  className?: string
}

export function Notices({ className }: NoticesProps) {
  const { notices, fetchNotices, isLoading, error, clearError } = useSupport()
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [selectedNotice, setSelectedNotice] = useState<Notice | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)

  useEffect(() => {
    fetchNotices({
      query: searchQuery || undefined,
      category: selectedCategory === 'all' ? undefined : selectedCategory
    })
  }, [fetchNotices, searchQuery, selectedCategory])

  const handleNoticeClick = (notice: Notice) => {
    setSelectedNotice(notice)
    setIsDetailOpen(true)
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Megaphone className="h-5 w-5" />
          <span>공지사항</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* 검색 및 필터 */}
        <div className="flex flex-col space-y-3 md:flex-row md:items-center md:space-y-0 md:space-x-3 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="공지사항 검색..."
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
              <SelectItem value="all">전체</SelectItem>
              {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {error && (
          <ErrorMessage message={error} className="mb-4" />
        )}

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <LoadingSpinner className="h-8 w-8" />
            <span className="ml-2 text-gray-600">공지사항을 불러오는 중...</span>
          </div>
        ) : notices.length === 0 ? (
          <div className="text-center py-12">
            <Megaphone className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              공지사항이 없습니다
            </h3>
            <p className="text-gray-600">
              새로운 공지사항이 등록되면 알려드리겠습니다.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {notices.map((notice) => {
              const CategoryIcon = CATEGORY_ICONS[notice.category]
              
              return (
                <div
                  key={notice.id}
                  onClick={() => handleNoticeClick(notice)}
                  className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                >
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0">
                      <CategoryIcon className="h-5 w-5 text-gray-600 mt-1" />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-2 mb-1">
                            {notice.isImportant && (
                              <Badge className="bg-red-100 text-red-800 text-xs">
                                중요
                              </Badge>
                            )}
                            <Badge className={`${CATEGORY_COLORS[notice.category]} text-xs`}>
                              {CATEGORY_LABELS[notice.category]}
                            </Badge>
                          </div>
                          
                          <h4 className="text-sm font-medium text-gray-900 truncate">
                            {notice.title}
                          </h4>
                          
                          <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                            <div className="flex items-center space-x-1">
                              <Calendar className="h-3 w-3" />
                              <span>{formatDate(notice.createdAt)}</span>
                            </div>
                            <div className="flex items-center space-x-1">
                              <Eye className="h-3 w-3" />
                              <span>{notice.views.toLocaleString()}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* 공지사항 상세 모달 */}
        <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
          <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
            {selectedNotice && (
              <>
                <DialogHeader>
                  <div className="space-y-2">
                    <div className="flex items-center space-x-2">
                      {selectedNotice.isImportant && (
                        <Badge className="bg-red-100 text-red-800">
                          중요
                        </Badge>
                      )}
                      <Badge className={CATEGORY_COLORS[selectedNotice.category]}>
                        {CATEGORY_LABELS[selectedNotice.category]}
                      </Badge>
                    </div>
                    <DialogTitle className="text-left">
                      {selectedNotice.title}
                    </DialogTitle>
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                        <span>{formatDate(selectedNotice.createdAt)}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <Eye className="h-4 w-4" />
                        <span>조회 {selectedNotice.views.toLocaleString()}</span>
                      </div>
                    </div>
                  </div>
                </DialogHeader>

                <div className="mt-6">
                  <div className="prose prose-sm max-w-none">
                    <div className="whitespace-pre-wrap text-gray-700 leading-relaxed">
                      {selectedNotice.content}
                    </div>
                  </div>
                </div>
              </>
            )}
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  )
}
