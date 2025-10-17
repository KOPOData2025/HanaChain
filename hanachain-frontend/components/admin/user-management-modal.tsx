'use client'

import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Search, Users, Mail, UserPlus, Loader2 } from 'lucide-react'
import adminOrganizationApi, { UserSearchResult } from '@/lib/api/admin-organization-api'

interface UserManagementModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onAddUser: (userId: number, role: 'ORG_ADMIN' | 'ORG_MEMBER') => Promise<void>
  organizationId: number
}

export function UserManagementModal({
  open,
  onOpenChange,
  onAddUser,
  organizationId
}: UserManagementModalProps) {
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([])
  const [selectedUser, setSelectedUser] = useState<UserSearchResult | null>(null)
  const [selectedRole, setSelectedRole] = useState<'ORG_ADMIN' | 'ORG_MEMBER'>('ORG_MEMBER')
  const [searching, setSearching] = useState(false)
  const [adding, setAdding] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 검색어가 변경될 때마다 검색 실행
  useEffect(() => {
    console.log('🔄 useEffect 실행됨, searchKeyword:', searchKeyword)
    const delayedSearch = setTimeout(() => {
      console.log('⏰ 디바운스 타이머 실행, 검색어 길이:', searchKeyword.trim().length)
      if (searchKeyword.trim().length >= 2) {
        console.log('✅ 검색 조건 충족, handleSearch 호출')
        handleSearch()
      } else {
        console.log('❌ 검색 조건 미충족, 결과 초기화')
        setSearchResults([])
      }
    }, 300) // 300ms 디바운스

    return () => clearTimeout(delayedSearch)
  }, [searchKeyword])

  // 모달이 열릴 때 초기화
  useEffect(() => {
    if (open) {
      setSearchKeyword('')
      setSearchResults([])
      setSelectedUser(null)
      setSelectedRole('ORG_MEMBER')
      setError(null)
    }
  }, [open])

  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      setSearchResults([])
      return
    }

    try {
      setSearching(true)
      setError(null)
      console.log('🔍 검색 시작:', searchKeyword.trim())
      const results = await adminOrganizationApi.searchUsers(searchKeyword.trim(), 10)
      console.log('✅ 검색 결과:', results)
      setSearchResults(results)
    } catch (error) {
      console.error('❌ 검색 실패:', error)
      setError('사용자 검색 중 오류가 발생했습니다.')
      setSearchResults([])
    } finally {
      setSearching(false)
      console.log('🏁 검색 완료')
    }
  }

  const handleSelectUser = (user: UserSearchResult) => {
    setSelectedUser(user)
    setError(null)
  }

  const handleAddToOrganization = async () => {
    if (!selectedUser) return

    try {
      setAdding(true)
      setError(null)
      await onAddUser(selectedUser.id, selectedRole)
      onOpenChange(false)
    } catch (error) {
      console.error('Failed to add user to organization:', error)
      setError('사용자 추가 중 오류가 발생했습니다.')
    } finally {
      setAdding(false)
    }
  }

  const getUserInitials = (name: string) => {
    return name.charAt(0).toUpperCase()
  }

  const getRoleBadge = (role: 'ORG_ADMIN' | 'ORG_MEMBER') => {
    return role === 'ORG_ADMIN' ? (
      <Badge variant="default">관리자</Badge>
    ) : (
      <Badge variant="secondary">멤버</Badge>
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UserPlus className="h-5 w-5" />
            단체에 유저 추가
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* 검색 섹션 */}
          <div className="space-y-4">
            <div>
              <Label htmlFor="search">사용자 검색</Label>
              <div className="relative mt-1">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input
                  id="search"
                  placeholder="이름 또는 이메일로 검색..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  className="pl-10"
                />
                {searching && (
                  <div className="absolute right-3 top-3">
                    <Loader2 className="h-4 w-4 animate-spin" />
                  </div>
                )}
              </div>
              <p className="text-sm text-muted-foreground mt-1">
                최소 2글자 이상 입력하세요
              </p>
            </div>

            {/* 검색 결과 */}
            {searchResults.length > 0 && (
              <div className="border rounded-lg max-h-64 overflow-y-auto">
                <div className="p-2">
                  <p className="text-sm font-medium text-muted-foreground mb-2">
                    검색 결과 ({searchResults.length}명)
                  </p>
                  <div className="space-y-1">
                    {searchResults.map((user) => (
                      <div
                        key={user.id}
                        className={`flex items-center space-x-3 p-2 rounded-md cursor-pointer transition-colors ${
                          selectedUser?.id === user.id
                            ? 'bg-primary/10 border border-primary/20'
                            : 'hover:bg-muted'
                        }`}
                        onClick={() => handleSelectUser(user)}
                      >
                        <Avatar className="h-8 w-8">
                          <AvatarFallback>
                            {getUserInitials(user.name)}
                          </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <p className="text-sm font-medium truncate">{user.name}</p>
                            {selectedUser?.id === user.id && (
                              <Badge variant="outline" className="ml-2">선택됨</Badge>
                            )}
                          </div>
                          <p className="text-xs text-muted-foreground flex items-center gap-1">
                            <Mail className="h-3 w-3" />
                            {user.email}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* 검색 결과가 없을 때 */}
            {searchKeyword.length >= 2 && !searching && searchResults.length === 0 && (
              <div className="text-center py-8 text-muted-foreground">
                <Users className="h-12 w-12 mx-auto mb-2 opacity-50" />
                <p>검색 결과가 없습니다</p>
                <p className="text-sm">다른 키워드로 다시 검색해보세요</p>
              </div>
            )}
          </div>

          {/* 선택된 사용자 및 역할 설정 */}
          {selectedUser && (
            <div className="space-y-4 p-4 border rounded-lg bg-muted/30">
              <div>
                <Label>선택된 사용자</Label>
                <div className="mt-2 flex items-center space-x-3">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback>
                      {getUserInitials(selectedUser.name)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium">{selectedUser.name}</p>
                    <p className="text-sm text-muted-foreground">{selectedUser.email}</p>
                  </div>
                </div>
              </div>

              <div>
                <Label htmlFor="role">역할 선택</Label>
                <Select value={selectedRole} onValueChange={(value: 'ORG_ADMIN' | 'ORG_MEMBER') => setSelectedRole(value)}>
                  <SelectTrigger className="mt-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ORG_MEMBER">
                      <div className="flex items-center gap-2">
                        <Badge variant="secondary">멤버</Badge>
                        <span>일반 멤버</span>
                      </div>
                    </SelectItem>
                    <SelectItem value="ORG_ADMIN">
                      <div className="flex items-center gap-2">
                        <Badge variant="default">관리자</Badge>
                        <span>단체 관리자</span>
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-sm text-muted-foreground mt-1">
                  {selectedRole === 'ORG_ADMIN' 
                    ? '단체 정보 수정 및 멤버 관리 권한이 부여됩니다.' 
                    : '단체 정보 조회 권한만 부여됩니다.'}
                </p>
              </div>
            </div>
          )}

          {/* 오류 메시지 */}
          {error && (
            <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md">
              {error}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={adding}>
            취소
          </Button>
          <Button 
            onClick={handleAddToOrganization} 
            disabled={!selectedUser || adding}
            className="gap-2"
          >
            {adding ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                추가 중...
              </>
            ) : (
              <>
                <UserPlus className="h-4 w-4" />
                {selectedRole === 'ORG_ADMIN' ? '관리자로 추가' : '멤버로 추가'}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}