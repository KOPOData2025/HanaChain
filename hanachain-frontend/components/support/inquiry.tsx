"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { 
  MessageSquare, 
  Plus, 
  Send, 
  Clock, 
  CheckCircle, 
  Calendar,
  User,
  UserCheck
} from "lucide-react"
import { useSupport } from "@/hooks/use-support"
import { Inquiry, InquiryCreateRequest } from "@/types/support"
import { formatDate } from "@/lib/utils"

const CATEGORY_LABELS = {
  donation: '기부 관련',
  account: '계정 관련',
  payment: '결제 관련',
  technical: '기술 문의',
  general: '일반 문의',
  other: '기타'
} as const

const STATUS_LABELS = {
  pending: '접수됨',
  'in-progress': '처리중',
  resolved: '해결됨',
  closed: '종료됨'
} as const

const STATUS_COLORS = {
  pending: 'bg-yellow-100 text-yellow-800',
  'in-progress': 'bg-blue-100 text-blue-800',
  resolved: 'bg-green-100 text-green-800',
  closed: 'bg-gray-100 text-gray-800'
} as const

const STATUS_ICONS = {
  pending: Clock,
  'in-progress': Clock,
  resolved: CheckCircle,
  closed: CheckCircle
} as const

interface InquiryProps {
  className?: string
}

export function InquiryComponent({ className }: InquiryProps) {
  const { inquiries, fetchInquiries, createInquiry, isLoading, error, clearError } = useSupport()
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [selectedInquiry, setSelectedInquiry] = useState<Inquiry | null>(null)
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false)
  const [formData, setFormData] = useState<InquiryCreateRequest>({
    title: '',
    content: '',
    category: 'general'
  })

  useEffect(() => {
    fetchInquiries()
  }, [fetchInquiries])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    if (!formData.title.trim() || !formData.content.trim()) {
      return
    }

    try {
      await createInquiry(formData)
      setFormData({ title: '', content: '', category: 'general' })
      setIsCreateDialogOpen(false)
      alert('문의가 성공적으로 등록되었습니다.')
    } catch (err) {
      // 에러는 useSupport 훅에서 처리됨
    }
  }

  const handleInquiryClick = (inquiry: Inquiry) => {
    setSelectedInquiry(inquiry)
    setIsDetailDialogOpen(true)
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center space-x-2">
            <MessageSquare className="h-5 w-5" />
            <span>1:1 문의</span>
          </CardTitle>
          
          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button className="bg-[#009591] hover:bg-[#007a77]">
                <Plus className="h-4 w-4 mr-2" />
                문의하기
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-2xl">
              <DialogHeader>
                <DialogTitle>새 문의 등록</DialogTitle>
              </DialogHeader>
              
              <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                  <ErrorMessage message={error} />
                )}

                <div className="space-y-2">
                  <Label htmlFor="category">문의 유형</Label>
                  <Select
                    value={formData.category}
                    onValueChange={(value) => setFormData(prev => ({ 
                      ...prev, 
                      category: value as InquiryCreateRequest['category'] 
                    }))}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                        <SelectItem key={value} value={value}>
                          {label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="title">제목</Label>
                  <Input
                    id="title"
                    value={formData.title}
                    onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                    placeholder="문의 제목을 입력하세요"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="content">문의 내용</Label>
                  <Textarea
                    id="content"
                    value={formData.content}
                    onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
                    placeholder="문의 내용을 자세히 입력해주세요..."
                    rows={8}
                    required
                  />
                </div>

                <div className="flex justify-end space-x-3">
                  <Button 
                    type="button" 
                    variant="outline" 
                    onClick={() => setIsCreateDialogOpen(false)}
                  >
                    취소
                  </Button>
                  <Button 
                    type="submit" 
                    disabled={isLoading || !formData.title.trim() || !formData.content.trim()}
                  >
                    {isLoading ? (
                      <>
                        <LoadingSpinner className="mr-2 h-4 w-4" />
                        등록 중...
                      </>
                    ) : (
                      <>
                        <Send className="mr-2 h-4 w-4" />
                        문의 등록
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </CardHeader>
      
      <CardContent>
        {error && !isCreateDialogOpen && (
          <ErrorMessage message={error} className="mb-4" />
        )}

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <LoadingSpinner className="h-8 w-8" />
            <span className="ml-2 text-gray-600">문의 내역을 불러오는 중...</span>
          </div>
        ) : inquiries.length === 0 ? (
          <div className="text-center py-12">
            <MessageSquare className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              문의 내역이 없습니다
            </h3>
            <p className="text-gray-600 mb-4">
              궁금한 점이 있으시면 언제든 문의해주세요.
            </p>
            <Button 
              onClick={() => setIsCreateDialogOpen(true)}
              className="bg-[#009591] hover:bg-[#007a77]"
            >
              <Plus className="h-4 w-4 mr-2" />
              첫 문의하기
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {inquiries.map((inquiry) => {
              const StatusIcon = STATUS_ICONS[inquiry.status]
              
              return (
                <div
                  key={inquiry.id}
                  onClick={() => handleInquiryClick(inquiry)}
                  className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                >
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0">
                      <StatusIcon className="h-5 w-5 text-gray-600 mt-1" />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-2 mb-1">
                            <Badge className={STATUS_COLORS[inquiry.status]}>
                              {STATUS_LABELS[inquiry.status]}
                            </Badge>
                            <Badge variant="outline" className="text-xs">
                              {CATEGORY_LABELS[inquiry.category]}
                            </Badge>
                          </div>
                          
                          <h4 className="text-sm font-medium text-gray-900 truncate">
                            {inquiry.title}
                          </h4>
                          
                          <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                            {inquiry.content}
                          </p>
                          
                          <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                            <div className="flex items-center space-x-1">
                              <Calendar className="h-3 w-3" />
                              <span>{formatDate(inquiry.createdAt)}</span>
                            </div>
                            {inquiry.responses.length > 0 && (
                              <div className="flex items-center space-x-1">
                                <MessageSquare className="h-3 w-3" />
                                <span>답변 {inquiry.responses.length}개</span>
                              </div>
                            )}
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

        {/* 문의 상세 모달 */}
        <Dialog open={isDetailDialogOpen} onOpenChange={setIsDetailDialogOpen}>
          <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
            {selectedInquiry && (
              <>
                <DialogHeader>
                  <div className="space-y-2">
                    <div className="flex items-center space-x-2">
                      <Badge className={STATUS_COLORS[selectedInquiry.status]}>
                        {STATUS_LABELS[selectedInquiry.status]}
                      </Badge>
                      <Badge variant="outline">
                        {CATEGORY_LABELS[selectedInquiry.category]}
                      </Badge>
                    </div>
                    <DialogTitle className="text-left">
                      {selectedInquiry.title}
                    </DialogTitle>
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                        <span>{formatDate(selectedInquiry.createdAt)}</span>
                      </div>
                    </div>
                  </div>
                </DialogHeader>

                <div className="space-y-6">
                  {/* 문의 내용 */}
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-center space-x-2 mb-3">
                      <User className="h-4 w-4 text-gray-600" />
                      <span className="text-sm font-medium text-gray-900">나의 문의</span>
                    </div>
                    <div className="whitespace-pre-wrap text-gray-700 leading-relaxed">
                      {selectedInquiry.content}
                    </div>
                  </div>

                  {/* 답변 */}
                  {selectedInquiry.responses.length > 0 && (
                    <div className="space-y-4">
                      <h4 className="font-medium text-gray-900">답변</h4>
                      {selectedInquiry.responses.map((response) => (
                        <div key={response.id} className="bg-blue-50 p-4 rounded-lg">
                          <div className="flex items-center space-x-2 mb-3">
                            <UserCheck className="h-4 w-4 text-blue-600" />
                            <span className="text-sm font-medium text-blue-900">
                              {response.authorName}
                            </span>
                            <span className="text-sm text-blue-700">
                              {formatDate(response.createdAt)}
                            </span>
                          </div>
                          <div className="whitespace-pre-wrap text-blue-800 leading-relaxed">
                            {response.content}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* 답변 대기 중 메시지 */}
                  {selectedInquiry.responses.length === 0 && selectedInquiry.status === 'pending' && (
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                      <div className="flex items-start space-x-3">
                        <Clock className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                        <div>
                          <h4 className="font-medium text-yellow-800">답변 대기중</h4>
                          <p className="text-sm text-yellow-700 mt-1">
                            문의를 접수했습니다. 빠른 시일 내에 답변드리겠습니다.
                          </p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  )
}
