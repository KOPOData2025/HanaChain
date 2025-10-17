'use client'

import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
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
import {
  MoreHorizontal,
  Eye,
  Edit,
  Trash2,
  Clock,
  CheckCircle,
  XCircle,
  Calendar
} from 'lucide-react'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'

interface Organization {
  id: number
  name: string
  description: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
  imageUrl?: string
  memberCount?: number
  adminCount?: number
  activeCampaignCount?: number
}

interface OrganizationCardViewProps {
  organizations: Organization[]
  selectedOrgs: number[]
  loading?: boolean
  onView: (id: number) => void
  onEdit: (id: number) => void
  onDelete: (id: number) => void
  onStatusChange?: (id: number, status: Organization['status']) => void
  onSelectOrg: (orgId: number, checked: boolean) => void
}

const statusConfig = {
  ACTIVE: {
    label: '활성',
    variant: 'default' as const,
    icon: CheckCircle,
    color: 'text-green-600'
  },
  INACTIVE: {
    label: '비활성',
    variant: 'secondary' as const,
    icon: XCircle,
    color: 'text-gray-600'
  },
}

export function OrganizationCardView({
  organizations,
  selectedOrgs,
  loading = false,
  onView,
  onEdit,
  onDelete,
  onStatusChange,
  onSelectOrg
}: OrganizationCardViewProps) {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [orgToDelete, setOrgToDelete] = useState<Organization | null>(null)

  const handleDeleteClick = (org: Organization) => {
    setOrgToDelete(org)
    setDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = () => {
    if (orgToDelete) {
      onDelete(orgToDelete.id)
      setDeleteDialogOpen(false)
      setOrgToDelete(null)
    }
  }

  if (loading) {
    return (
      <div className="grid grid-cols-1 gap-4">
        {Array.from({ length: 3 }).map((_, index) => (
          <Card key={index} className="animate-pulse">
            <CardContent className="p-4">
              <div className="flex items-start space-x-4">
                <div className="h-16 w-16 rounded-full bg-muted" />
                <div className="flex-1 space-y-2">
                  <div className="h-5 bg-muted rounded w-3/4" />
                  <div className="h-4 bg-muted rounded w-full" />
                  <div className="h-4 bg-muted rounded w-1/2" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (organizations.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="flex flex-col items-center gap-4">
          <div className="text-muted-foreground">
            검색 조건에 맞는 단체가 없습니다.
          </div>
          <Button variant="outline" size="sm">
            필터 초기화
          </Button>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className="grid grid-cols-1 gap-4">
        {organizations.map((org) => {
          const StatusIcon = statusConfig[org.status].icon
          const isSelected = selectedOrgs.includes(org.id)

          return (
            <Card key={org.id} className={`transition-colors ${isSelected ? 'bg-muted/50' : ''}`}>
              <CardContent className="p-4">
                <div className="flex items-start gap-4">
                  {/* Selection checkbox */}
                  <div className="pt-1">
                    <Checkbox
                      checked={isSelected}
                      onCheckedChange={(checked) => onSelectOrg(org.id, checked as boolean)}
                    />
                  </div>

                  {/* Organization avatar */}
                  <Avatar className="h-16 w-16 flex-shrink-0">
                    <AvatarImage src={org.imageUrl} alt={org.name} />
                    <AvatarFallback className="text-lg">
                      {org.name.charAt(0)}
                    </AvatarFallback>
                  </Avatar>

                  {/* Organization info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <h3 className="font-semibold text-lg leading-tight">
                          {org.name}
                        </h3>
                        <div className="text-xs text-muted-foreground mt-1">
                          ID: {org.id}
                        </div>
                      </div>

                      {/* Actions dropdown */}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8 flex-shrink-0">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => onView(org.id)}>
                            <Eye className="mr-2 h-4 w-4" />
                            상세보기
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => onEdit(org.id)}>
                            <Edit className="mr-2 h-4 w-4" />
                            수정
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            onClick={() => handleDeleteClick(org)}
                            className="text-destructive focus:text-destructive"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            삭제
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>

                    {/* Description */}
                    <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                      {org.description}
                    </p>

                    {/* Status and date info */}
                    <div className="flex items-center justify-between mt-3">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4 text-muted-foreground" />
                        <span className="text-sm text-muted-foreground">
                          {format(new Date(org.createdAt), 'yyyy.MM.dd', { locale: ko })}
                        </span>
                      </div>

                      {/* Status badge with dropdown */}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm" className="gap-1 h-7">
                            <StatusIcon className={`h-3 w-3 ${statusConfig[org.status].color}`} />
                            <Badge variant={statusConfig[org.status].variant} className="text-xs">
                              {statusConfig[org.status].label}
                            </Badge>
                          </Button>
                        </DropdownMenuTrigger>
                        {onStatusChange && (
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => onStatusChange(org.id, 'ACTIVE')}>
                              <CheckCircle className="mr-2 h-4 w-4 text-green-600" />
                              활성
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => onStatusChange(org.id, 'INACTIVE')}>
                              <XCircle className="mr-2 h-4 w-4 text-gray-600" />
                              비활성
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        )}
                      </DropdownMenu>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>단체 삭제 확인</AlertDialogTitle>
            <AlertDialogDescription>
              <span className="font-medium">{orgToDelete?.name}</span> 단체를 삭제하시겠습니까?
              <br />
              이 작업은 되돌릴 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}