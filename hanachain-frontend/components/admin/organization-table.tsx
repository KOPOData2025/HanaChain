'use client'

import { useState } from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
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
import { Pagination } from '@/components/admin/pagination'
import { OrganizationCardView } from '@/components/admin/organization-card-view'
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
  XCircle
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

interface OrganizationTableProps {
  organizations: Organization[]
  loading?: boolean
  onView: (id: number) => void
  onEdit: (id: number) => void
  onDelete: (id: number) => void
  onStatusChange?: (id: number, status: Organization['status']) => void
  // 페이지네이션 속성
  currentPage: number
  totalPages: number
  totalItems: number
  itemsPerPage: number
  onPageChange: (page: number) => void
  onItemsPerPageChange: (itemsPerPage: number) => void
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

export function OrganizationTable({
  organizations,
  loading = false,
  onView,
  onEdit,
  onDelete,
  onStatusChange,
  currentPage,
  totalPages,
  totalItems,
  itemsPerPage,
  onPageChange,
  onItemsPerPageChange
}: OrganizationTableProps) {
  const [selectedOrgs, setSelectedOrgs] = useState<number[]>([])
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [orgToDelete, setOrgToDelete] = useState<Organization | null>(null)

  const handleSelectAll = (checked: boolean) => {
    setSelectedOrgs(checked ? organizations.map(org => org.id) : [])
  }

  const handleSelectOrg = (orgId: number, checked: boolean) => {
    setSelectedOrgs(prev =>
      checked
        ? [...prev, orgId]
        : prev.filter(id => id !== orgId)
    )
  }

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
      <Card>
        <CardContent className="p-8 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4" />
          <p className="text-muted-foreground">단체 목록을 불러오는 중...</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <>
      <Card>
        <CardContent className="p-0">
          {selectedOrgs.length > 0 && (
            <div className="border-b p-4 bg-muted/50">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">
                  {selectedOrgs.length}개 단체 선택됨
                </span>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm">
                    일괄 상태 변경
                  </Button>
                  <Button variant="outline" size="sm" className="text-destructive">
                    일괄 삭제
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Desktop Table View */}
          <div className="hidden md:block">
            <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-12">
                  <Checkbox
                    checked={organizations.length > 0 && selectedOrgs.length === organizations.length}
                    onCheckedChange={handleSelectAll}
                  />
                </TableHead>
                <TableHead className="w-16">썸네일</TableHead>
                <TableHead>단체명</TableHead>
                <TableHead>설명</TableHead>
                <TableHead className="w-32">등록일</TableHead>
                <TableHead className="w-28">상태</TableHead>
                <TableHead className="w-16">액션</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {organizations.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8">
                    <div className="flex flex-col items-center gap-2">
                      <div className="text-muted-foreground">
                        검색 조건에 맞는 단체가 없습니다.
                      </div>
                      <Button variant="outline" size="sm">
                        필터 초기화
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                organizations.map((org, index) => {
                  const StatusIcon = statusConfig[org.status].icon
                  const isSelected = selectedOrgs.includes(org.id)

                  return (
                    <TableRow
                      key={org.id}
                      className={`${isSelected ? 'bg-muted/50' : ''} animate-fade-in-up stagger-${Math.min(index + 1, 20)}`}
                    >
                      <TableCell>
                        <Checkbox
                          checked={isSelected}
                          onCheckedChange={(checked) => handleSelectOrg(org.id, checked as boolean)}
                        />
                      </TableCell>
                      <TableCell>
                        <Avatar className="h-10 w-10">
                          <AvatarImage src={org.imageUrl} alt={org.name} />
                          <AvatarFallback className="text-xs">
                            {org.name.charAt(0)}
                          </AvatarFallback>
                        </Avatar>
                      </TableCell>
                      <TableCell>
                        <div className="font-medium">{org.name}</div>
                        <div className="text-xs text-muted-foreground">
                          ID: {org.id}
                        </div>
                      </TableCell>
                      <TableCell className="max-w-md">
                        <p className="truncate text-sm text-muted-foreground">
                          {org.description}
                        </p>
                      </TableCell>
                      <TableCell className="text-sm">
                        <div>
                          {format(new Date(org.createdAt), 'yyyy.MM.dd', { locale: ko })}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {format(new Date(org.createdAt), 'HH:mm')}
                        </div>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="sm" className="gap-1">
                              <StatusIcon className={`h-3 w-3 ${statusConfig[org.status].color}`} />
                              <Badge variant={statusConfig[org.status].variant}>
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
                      </TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-8 w-8">
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
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
            </Table>
          </div>

          {/* Mobile Card View */}
          <div className="md:hidden p-4">
            <OrganizationCardView
              organizations={organizations}
              selectedOrgs={selectedOrgs}
              loading={loading}
              onView={onView}
              onEdit={onEdit}
              onDelete={onDelete}
              onStatusChange={onStatusChange}
              onSelectOrg={handleSelectOrg}
            />
          </div>
          
          {/* Pagination */}
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            itemsPerPage={itemsPerPage}
            onPageChange={onPageChange}
            onItemsPerPageChange={onItemsPerPageChange}
          />
        </CardContent>
      </Card>

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