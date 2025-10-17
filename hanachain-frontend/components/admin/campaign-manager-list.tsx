'use client'

import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { 
  Plus, 
  MoreHorizontal, 
  Shield, 
  ShieldCheck, 
  ShieldX, 
  User,
  Calendar,
  MessageSquare,
  RotateCcw,
  Trash2
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'

import { 
  campaignManagerApi,
  CampaignManager
} from '@/lib/api/campaign-manager-api'
import { ApiError } from '@/lib/api/client'
import { CampaignManagerDialog } from './campaign-manager-dialog'

interface CampaignManagerListProps {
  campaignId: number
  campaignTitle: string
}

export function CampaignManagerList({ campaignId, campaignTitle }: CampaignManagerListProps) {
  const [managers, setManagers] = useState<CampaignManager[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleteDialog, setDeleteDialog] = useState<{
    open: boolean
    manager: CampaignManager | null
  }>({ open: false, manager: null })

  // 담당자 목록 조회
  const loadManagers = async () => {
    try {
      setLoading(true)
      console.log('🔍 [CampaignManagerList] 담당자 목록 조회 시작:', {
        campaignId,
        campaignTitle,
        timestamp: new Date().toISOString()
      })
      
      const data = await campaignManagerApi.getCampaignManagers(campaignId)
      
      console.log('✅ [CampaignManagerList] API 응답 수신:', {
        data,
        dataType: typeof data,
        isArray: Array.isArray(data),
        length: Array.isArray(data) ? data.length : 'N/A',
        keys: data ? Object.keys(data) : 'N/A'
      })
      
      const managersArray = Array.isArray(data) ? data : []
      setManagers(managersArray)
      
      console.log('📋 [CampaignManagerList] 담당자 목록 설정 완료:', {
        managersCount: managersArray.length,
        managers: managersArray.map(m => ({
          id: m.id,
          userName: m.userName,
          role: m.role,
          status: m.status
        }))
      })
      
    } catch (error) {
      console.error('❌ [CampaignManagerList] 캠페인 담당자 목록 조회 실패:', {
        error,
        errorType: typeof error,
        errorName: error?.constructor?.name,
        errorMessage: error?.message,
        errorStatus: error?.status,
        errorDetails: error?.details,
        campaignId
      })
      
      setManagers([]) // 에러 시 빈 배열로 초기화
      
      if (error instanceof ApiError) {
        console.error('🚨 [CampaignManagerList] API 에러 상세:', {
          status: error.status,
          message: error.message,
          details: error.details
        })
        toast.error(`담당자 목록 조회 실패: ${error.message}`)
      } else {
        toast.error('담당자 목록 조회 중 오류가 발생했습니다')
      }
    } finally {
      setLoading(false)
    }
  }

  // 담당자 권한 해제
  const handleDeleteManager = async (manager: CampaignManager) => {
    try {
      await campaignManagerApi.deleteCampaignManager(manager.id)
      toast.success(`${manager.userName}님의 담당자 권한이 해제되었습니다`)
      loadManagers()
    } catch (error) {
      console.error('담당자 권한 해제 실패:', error)
      if (error instanceof ApiError) {
        toast.error(`권한 해제 실패: ${error.message}`)
      } else {
        toast.error('권한 해제 중 오류가 발생했습니다')
      }
    }
  }

  // 담당자 권한 복원
  const handleRestoreManager = async (manager: CampaignManager) => {
    try {
      await campaignManagerApi.restoreCampaignManager(manager.id)
      toast.success(`${manager.userName}님의 담당자 권한이 복원되었습니다`)
      loadManagers()
    } catch (error) {
      console.error('담당자 권한 복원 실패:', error)
      if (error instanceof ApiError) {
        toast.error(`권한 복원 실패: ${error.message}`)
      } else {
        toast.error('권한 복원 중 오류가 발생했습니다')
      }
    }
  }

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'MANAGER':
        return <ShieldCheck className="h-4 w-4" />
      case 'CO_MANAGER':
        return <Shield className="h-4 w-4" />
      default:
        return <User className="h-4 w-4" />
    }
  }

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'MANAGER':
        return '담당자'
      case 'CO_MANAGER':
        return '부담당자'
      default:
        return role
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge variant="default">활성</Badge>
      case 'REVOKED':
        return <Badge variant="destructive">해제</Badge>
      case 'SUSPENDED':
        return <Badge variant="secondary">일시정지</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  useEffect(() => {
    loadManagers()
  }, [campaignId])

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            캠페인 담당자
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8 text-muted-foreground">
            담당자 목록을 불러오는 중...
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              캠페인 담당자
              <Badge variant="secondary" className="ml-2">
                {(managers || []).filter(m => m.status === 'ACTIVE').length}명
              </Badge>
            </CardTitle>
            <Button onClick={() => setDialogOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              담당자 추가
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {(managers || []).length === 0 ? (
            <div className="text-center py-8">
              <Shield className="h-12 w-12 mx-auto text-muted-foreground/50 mb-4" />
              <h3 className="font-medium mb-2">등록된 담당자가 없습니다</h3>
              <p className="text-sm text-muted-foreground mb-4">
                캠페인을 관리할 담당자를 추가해보세요
              </p>
              <Button onClick={() => setDialogOpen(true)}>
                <Plus className="h-4 w-4 mr-2" />
                첫 번째 담당자 추가
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>담당자</TableHead>
                    <TableHead>역할</TableHead>
                    <TableHead>상태</TableHead>
                    <TableHead>등록일</TableHead>
                    <TableHead className="w-[100px]">관리</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(managers || []).map((manager) => (
                    <TableRow key={manager.id}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback>
                              <User className="h-4 w-4" />
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <div className="font-medium">{manager.userName}</div>
                            <div className="text-sm text-muted-foreground">{manager.userEmail}</div>
                            {manager.userNickname && (
                              <div className="text-xs text-muted-foreground">닉네임: {manager.userNickname}</div>
                            )}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {getRoleIcon(manager.role)}
                          {getRoleLabel(manager.role)}
                        </div>
                      </TableCell>
                      <TableCell>
                        {getStatusBadge(manager.status)}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1 text-sm text-muted-foreground">
                          <Calendar className="h-3 w-3" />
                          {format(new Date(manager.assignedAt), 'yyyy.MM.dd', { locale: ko })}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          등록자: {manager.assignedByUserName}
                        </div>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="sm">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            {manager.status === 'ACTIVE' ? (
                              <DropdownMenuItem
                                onClick={() => setDeleteDialog({ open: true, manager })}
                                className="text-destructive"
                              >
                                <Trash2 className="h-4 w-4 mr-2" />
                                권한 해제
                              </DropdownMenuItem>
                            ) : (
                              <DropdownMenuItem
                                onClick={() => handleRestoreManager(manager)}
                              >
                                <RotateCcw className="h-4 w-4 mr-2" />
                                권한 복원
                              </DropdownMenuItem>
                            )}
                            {manager.notes && (
                              <>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem disabled>
                                  <MessageSquare className="h-4 w-4 mr-2" />
                                  메모 보기
                                </DropdownMenuItem>
                              </>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* 메모가 있는 담당자들의 메모 표시 */}
              {(managers || []).some(m => m.notes) && (
                <div className="space-y-2">
                  <Separator />
                  <h4 className="font-medium text-sm">메모</h4>
                  {(managers || [])
                    .filter(m => m.notes)
                    .map((manager) => (
                      <div key={`note-${manager.id}`} className="text-sm">
                        <span className="font-medium">{manager.userName}</span>: {manager.notes}
                      </div>
                    ))}
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* 담당자 추가 다이얼로그 */}
      <CampaignManagerDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        campaignId={campaignId}
        campaignTitle={campaignTitle}
        onSuccess={loadManagers}
      />

      {/* 권한 해제 확인 다이얼로그 */}
      <AlertDialog 
        open={deleteDialog.open} 
        onOpenChange={(open) => setDeleteDialog({ open, manager: null })}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>담당자 권한 해제</AlertDialogTitle>
            <AlertDialogDescription>
              {deleteDialog.manager?.userName}님의 캠페인 담당자 권한을 해제하시겠습니까?
              <br />
              해제된 권한은 언제든지 복원할 수 있습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (deleteDialog.manager) {
                  handleDeleteManager(deleteDialog.manager)
                }
                setDeleteDialog({ open: false, manager: null })
              }}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              권한 해제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}