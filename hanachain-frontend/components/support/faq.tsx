"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { 
  Search, 
  HelpCircle, 
  ChevronDown, 
  ChevronUp,
  ThumbsUp,
  Star,
  Tag
} from "lucide-react"
import { useSupport } from "@/hooks/use-support"
import { FAQ } from "@/types/support"

const CATEGORY_LABELS = {
  donation: '기부',
  account: '계정',
  payment: '결제',
  technical: '기술',
  general: '일반'
} as const

const CATEGORY_COLORS = {
  donation: 'bg-green-100 text-green-800',
  account: 'bg-blue-100 text-blue-800',
  payment: 'bg-yellow-100 text-yellow-800',
  technical: 'bg-purple-100 text-purple-800',
  general: 'bg-gray-100 text-gray-800'
} as const

interface FAQProps {
  className?: string
}

export function FAQComponent({ className }: FAQProps) {
  const { faqs, fetchFAQs, markFAQHelpful, isLoading, error, clearError } = useSupport()
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set())

  useEffect(() => {
    fetchFAQs({
      query: searchQuery || undefined,
      category: selectedCategory === 'all' ? undefined : selectedCategory
    })
  }, [fetchFAQs, searchQuery, selectedCategory])

  const toggleExpanded = (faqId: string) => {
    setExpandedItems(prev => {
      const newSet = new Set(prev)
      if (newSet.has(faqId)) {
        newSet.delete(faqId)
      } else {
        newSet.add(faqId)
      }
      return newSet
    })
  }

  const handleHelpful = async (faqId: string) => {
    try {
      await markFAQHelpful(faqId)
    } catch (err) {
      // 에러는 useSupport 훅에서 처리됨
    }
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <HelpCircle className="h-5 w-5" />
          <span>자주 묻는 질문</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* 검색 및 필터 */}
        <div className="flex flex-col space-y-3 md:flex-row md:items-center md:space-y-0 md:space-x-3 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="FAQ 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>

          <Select value={selectedCategory} onValueChange={setSelectedCategory}>
            <SelectTrigger className="w-full md:w-32">
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
            <span className="ml-2 text-gray-600">FAQ를 불러오는 중...</span>
          </div>
        ) : faqs.length === 0 ? (
          <div className="text-center py-12">
            <HelpCircle className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              검색 결과가 없습니다
            </h3>
            <p className="text-gray-600">
              다른 검색어를 시도해보세요.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {faqs.map((faq) => {
              const isExpanded = expandedItems.has(faq.id)
              
              return (
                <Collapsible
                  key={faq.id}
                  open={isExpanded}
                  onOpenChange={() => toggleExpanded(faq.id)}
                >
                  <div className="border border-gray-200 rounded-lg">
                    <CollapsibleTrigger className="w-full p-4 text-left hover:bg-gray-50 transition-colors">
                      <div className="flex items-start justify-between">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-2 mb-2">
                            {faq.isPopular && (
                              <Badge className="bg-orange-100 text-orange-800 text-xs">
                                <Star className="h-3 w-3 mr-1" />
                                인기
                              </Badge>
                            )}
                            <Badge className={`${CATEGORY_COLORS[faq.category]} text-xs`}>
                              {CATEGORY_LABELS[faq.category]}
                            </Badge>
                          </div>
                          
                          <h4 className="text-sm font-medium text-gray-900 text-left">
                            {faq.question}
                          </h4>
                          
                          <div className="flex items-center space-x-3 mt-2">
                            <div className="flex items-center space-x-1 text-xs text-gray-500">
                              <ThumbsUp className="h-3 w-3" />
                              <span>{faq.helpfulCount}</span>
                            </div>
                            
                            {faq.tags.length > 0 && (
                              <div className="flex items-center space-x-1">
                                <Tag className="h-3 w-3 text-gray-400" />
                                <div className="flex flex-wrap gap-1">
                                  {faq.tags.slice(0, 2).map((tag, index) => (
                                    <span key={index} className="text-xs text-gray-500 bg-gray-100 px-1 rounded">
                                      {tag}
                                    </span>
                                  ))}
                                  {faq.tags.length > 2 && (
                                    <span className="text-xs text-gray-500">
                                      +{faq.tags.length - 2}
                                    </span>
                                  )}
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                        
                        <div className="flex-shrink-0 ml-4">
                          {isExpanded ? (
                            <ChevronUp className="h-5 w-5 text-gray-400" />
                          ) : (
                            <ChevronDown className="h-5 w-5 text-gray-400" />
                          )}
                        </div>
                      </div>
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                      <div className="px-4 pb-4 border-t border-gray-100">
                        <div className="pt-4">
                          <div className="prose prose-sm max-w-none">
                            <div className="whitespace-pre-wrap text-gray-700 leading-relaxed">
                              {faq.answer}
                            </div>
                          </div>
                          
                          <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
                            <div className="text-xs text-gray-500">
                              마지막 업데이트: {new Date(faq.updatedAt).toLocaleDateString('ko-KR')}
                            </div>
                            
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleHelpful(faq.id)}
                              className="text-xs"
                            >
                              <ThumbsUp className="h-3 w-3 mr-1" />
                              도움이 돼요 ({faq.helpfulCount})
                            </Button>
                          </div>
                        </div>
                      </div>
                    </CollapsibleContent>
                  </div>
                </Collapsible>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
