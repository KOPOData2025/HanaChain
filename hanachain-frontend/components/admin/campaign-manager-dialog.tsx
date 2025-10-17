'use client'

import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { Search, Plus, X, User } from 'lucide-react'

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

import { 
  campaignManagerApi,
  CampaignManagerCreateRequest,
  UserProfile,
  PageableResponse
} from '@/lib/api/campaign-manager-api'
import { ApiError } from '@/lib/api/client'

interface CampaignManagerDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  campaignId: number
  campaignTitle: string
  onSuccess?: () => void
}

export function CampaignManagerDialog({ 
  open, 
  onOpenChange, 
  campaignId, 
  campaignTitle, 
  onSuccess 
}: CampaignManagerDialogProps) {
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [users, setUsers] = useState<PageableResponse<UserProfile> | null>(null)
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null)
  const [role, setRole] = useState<'MANAGER' | 'CO_MANAGER'>('MANAGER')
  const [notes, setNotes] = useState('')
  const [showUserModal, setShowUserModal] = useState(false)

  // 유저 목록 조회
  const searchUsers = async (keyword = '', page = 0) => {
    console.log('🔄 searchUsers 실행됨, keyword:', keyword)
    try {
      console.log('⏰ 유저 목록 조회 시작, keyword:', keyword)
      setLoading(true)
      const response = await campaignManagerApi.getUserList(keyword, page, 20)
      console.log('✅ 유저 목록 조회 성공, response:', response)
      console.log('📊 유저 목록 데이터 상세:', {
        totalElements: response.totalElements,
        contentLength: response.content?.length,
        keyword: keyword,
        page: page
      })
      setUsers(response)
    } catch (error) {
      console.log('❌ 유저 목록 조회 실패, error:', error)
      console.error('유저 목록 조회 실패:', error)
      if (error instanceof ApiError) {
        console.log('🔴 API 에러 발생, message:', error.message)
        toast.error(`유저 목록 조회 실패: ${error.message}`)
      } else {
        console.log('🔴 비 API 에러 발생')
        toast.error('유저 목록 조회 중 오류가 발생했습니다')
      }
    } finally {
      console.log('🏁 유저 목록 조회 완료')
      setLoading(false)
    }
  }

  // 캠페인 담당자 등록
  const handleSubmit = async () => {
    if (!selectedUser) {
      toast.error('담당자로 등록할 유저를 선택해주세요')
      return
    }

    try {
      setLoading(true)
      
      const request: CampaignManagerCreateRequest = {
        campaignId,
        userId: selectedUser.id,
        role,
        notes: notes.trim() || undefined
      }

      await campaignManagerApi.createCampaignManager(request)
      
      toast.success(`${selectedUser.name}님이 캠페인 담당자로 등록되었습니다`)
      onSuccess?.()
      handleClose()
    } catch (error) {
      console.error('캠페인 담당자 등록 실패:', error)
      if (error instanceof ApiError) {
        toast.error(`담당자 등록 실패: ${error.message}`)
      } else {
        toast.error('담당자 등록 중 오류가 발생했습니다')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setSelectedUser(null)
    setRole('MANAGER')
    setNotes('')
    setSearchKeyword('')
    setUsers(null)
    onOpenChange(false)
  }

  const handleSearch = () => {
    console.log('🔍 검색 버튼 클릭됨, keyword:', searchKeyword)
    searchUsers(searchKeyword, 0)
    setShowUserModal(true)
  }

  const handleUserSelect = (user: UserProfile) => {
    console.log('👤 유저 선택됨:', user.name)
    setSelectedUser(user)
    setShowUserModal(false)
  }

  // 다이얼로그가 열릴 때 초기화
  useEffect(() => {
    if (open) {
      console.log('🚪 다이얼로그 열림')
      // 초기 유저 목록은 자동으로 로드하지 않고 검색 버튼 클릭 시에만 로드
    }
  }, [open])

  // 상태 변화 디버깅
  useEffect(() => {
    console.log('🔄 상태 변화 감지:', {
      users: users ? `${users.content.length}명 (총 ${users.totalElements}명)` : 'null',
      selectedUser: selectedUser ? `${selectedUser.name} (${selectedUser.email})` : 'null',
      searchKeyword: searchKeyword,
      loading: loading
    })
  }, [users, selectedUser, searchKeyword, loading])

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>캠페인 담당자 등록</DialogTitle>
          <DialogDescription>
            &quot;{campaignTitle}&quot; 캠페인의 담당자를 등록합니다
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 flex-1 min-h-0">
          {/* 유저 검색 */}
          <div className="space-y-3">
            <Label>담당자 검색</Label>
            <div className="flex gap-2">
              <Input
                placeholder="이름 또는 이메일로 검색..."
                value={searchKeyword}
                onChange={(e) => {
                  console.log('⌨️ 검색어 입력 변경됨:', e.target.value)
                  setSearchKeyword(e.target.value)
                }}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
              <Button 
                variant="outline" 
                size="icon"
                onClick={handleSearch}
                disabled={loading}
              >
                <Search className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* 선택된 유저 표시 */}
          {selectedUser && (
            <div className="p-4 border rounded-lg bg-muted/50">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarImage src={selectedUser.profileImage} />
                    <AvatarFallback>
                      <User className="h-5 w-5" />
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="font-medium">{selectedUser.name}</div>
                    <div className="text-sm text-muted-foreground">{selectedUser.email}</div>
                    {selectedUser.nickname && (
                      <div className="text-sm text-muted-foreground">닉네임: {selectedUser.nickname}</div>
                    )}
                    {selectedUser.phoneNumber && (
                      <div className="text-sm text-muted-foreground">전화번호: {selectedUser.phoneNumber}</div>
                    )}
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setSelectedUser(null)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}


          {/* 역할 선택 */}
          <div className="space-y-3">
            <Label htmlFor="role">담당자 역할</Label>
            <Select value={role} onValueChange={(value: 'MANAGER' | 'CO_MANAGER') => setRole(value)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="MANAGER">담당자</SelectItem>
                <SelectItem value="CO_MANAGER">부담당자</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* 메모 */}
          <div className="space-y-3">
            <Label htmlFor="notes">메모 (선택사항)</Label>
            <Textarea
              id="notes"
              placeholder="담당자 등록에 대한 메모를 입력하세요..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              maxLength={500}
            />
            <div className="text-xs text-muted-foreground text-right">
              {notes.length}/500자
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={loading}>
            취소
          </Button>
          <Button 
            onClick={handleSubmit} 
            disabled={!selectedUser || loading}
          >
            {loading ? '등록 중...' : '담당자 등록'}
          </Button>
        </DialogFooter>
      </DialogContent>
      </Dialog>

      {/* 유저 목록 모달 */}
      <Dialog open={showUserModal} onOpenChange={setShowUserModal}>
        <DialogContent className="max-w-2xl max-h-[80vh] flex flex-col">
          <DialogHeader>
            <DialogTitle>담당자 선택</DialogTitle>
            <p className="text-sm text-muted-foreground">
              캠페인 담당자로 등록할 사용자를 선택해주세요
            </p>
          </DialogHeader>

          <div className="flex-1 min-h-0">
            {/* 검색 결과 통계 */}
            {users && (
              <div className="flex items-center justify-between mb-4">
                <div className="text-sm text-muted-foreground">
                  검색어: "{searchKeyword}"
                </div>
                <Badge variant="secondary">
                  총 {users.totalElements}명
                </Badge>
              </div>
            )}

            {/* 유저 목록 */}
            <ScrollArea className="h-[400px] border rounded-lg">
              <div className="p-4">
                {!users ? (
                  <div className="text-center py-8 text-muted-foreground">
                    {loading ? '검색 중...' : '검색을 실행해주세요'}
                  </div>
                ) : users.content.length === 0 ? (
                  <div className="text-center py-8 text-muted-foreground">
                    검색 결과가 없습니다
                  </div>
                ) : (
                  <div className="space-y-2">
                    {users.content.map((user, index) => (
                      <div key={user.id}>
                        <div
                          className="flex items-center gap-3 p-3 rounded-lg hover:bg-muted cursor-pointer transition-colors"
                          onClick={() => handleUserSelect(user)}
                        >
                          <Avatar className="h-10 w-10">
                            <AvatarImage src={user.profileImage} />
                            <AvatarFallback>
                              <User className="h-5 w-5" />
                            </AvatarFallback>
                          </Avatar>
                          <div className="flex-1">
                            <div className="font-medium">{user.name}</div>
                            <div className="text-sm text-muted-foreground">{user.email}</div>
                            {user.nickname && (
                              <div className="text-sm text-muted-foreground">닉네임: {user.nickname}</div>
                            )}
                            {user.phoneNumber && (
                              <div className="text-sm text-muted-foreground">전화번호: {user.phoneNumber}</div>
                            )}
                          </div>
                        </div>
                        {index < users.content.length - 1 && <Separator />}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </ScrollArea>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setShowUserModal(false)}>
              취소
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}