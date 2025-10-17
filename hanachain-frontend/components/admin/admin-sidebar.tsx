'use client'

import { useState } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import { useAuth } from '@/lib/auth-context'
import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  LayoutDashboard,
  Megaphone,
  Building2,
  Users,
  Settings,
  LogOut,
  ChevronLeft,
  Menu,
  UserCircle2,
  DollarSign,
} from 'lucide-react'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'

interface AdminSidebarProps {
  isCollapsed?: boolean
  onToggle?: () => void
}

const menuItems = [
  {
    title: '대시보드',
    href: '/admin',
    icon: LayoutDashboard,
  },
  {
    title: '캠페인 관리',
    href: '/admin/campaigns',
    icon: Megaphone,
  },
  {
    title: '단체 관리',
    href: '/admin/organizations',
    icon: Building2,
  },
  {
    title: '기부 관리',
    href: '/admin/donations',
    icon: DollarSign,
  },
  {
    title: '사용자 관리',
    href: '/admin/users',
    icon: Users,
  },
  {
    title: '설정',
    href: '/admin/#',
    icon: Settings,
  },
]

export function AdminSidebar({ isCollapsed = false, onToggle }: AdminSidebarProps) {
  const pathname = usePathname()
  const { user, logout } = useAuth()
  const [isHovered, setIsHovered] = useState(false)

  const isExpanded = !isCollapsed || isHovered

  const handleLogout = async () => {
    await logout()
    window.location.href = '/login'
  }

  return (
    <TooltipProvider>
      <div
        className={cn(
          'flex flex-col h-full bg-teal-600 dark:bg-teal-800 border-r border-teal-700 dark:border-teal-900 transition-all duration-300',
          isExpanded ? 'w-64' : 'w-16'
        )}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between p-4 border-b border-teal-700 dark:border-teal-900">
          <div className={cn(
            'flex items-center gap-2 transition-opacity duration-300',
            isExpanded ? 'opacity-100' : 'opacity-0 w-0'
          )}>
            <h2 className="text-lg font-bold text-white">
              HanaChain Admin
            </h2>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggle}
            className="ml-auto"
          >
            {isCollapsed ? <Menu className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </Button>
        </div>

        {/* 사용자 정보 */}
        <div className="p-4 border-b border-teal-700 dark:border-teal-900">
          <div className={cn(
            'flex items-center gap-3 transition-all duration-300',
            !isExpanded && 'justify-center'
          )}>
            <div className="flex-shrink-0">
              <UserCircle2 className="h-8 w-8 text-teal-100" />
            </div>
            {isExpanded && (
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">
                  {user?.nickname || '관리자'}
                </p>
                <p className="text-xs text-teal-100 truncate">
                  {user?.email}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* 메뉴 */}
        <ScrollArea className="flex-1 py-4">
          <nav className="px-3 space-y-1">
            {menuItems.map((item) => {
              const Icon = item.icon
              const isActive = pathname === item.href || 
                (item.href !== '/admin' && pathname.startsWith(item.href))
              
              const linkContent = (
                <Link
                  href={item.href}
                  className={cn(
                    'flex items-center gap-3 px-3 py-2 rounded-lg transition-all duration-200',
                    isActive
                      ? 'bg-teal-700 dark:bg-teal-900 text-white'
                      : 'text-teal-50 hover:bg-teal-700/50 dark:hover:bg-teal-900/50',
                    !isExpanded && 'justify-center'
                  )}
                >
                  <Icon className={cn(
                    'flex-shrink-0 transition-colors',
                    isActive ? 'text-white' : 'text-teal-100',
                    isExpanded ? 'h-5 w-5' : 'h-6 w-6'
                  )} />
                  {isExpanded && (
                    <span className="font-medium">
                      {item.title}
                    </span>
                  )}
                </Link>
              )

              if (!isExpanded) {
                return (
                  <Tooltip key={item.href} delayDuration={0}>
                    <TooltipTrigger asChild>
                      {linkContent}
                    </TooltipTrigger>
                    <TooltipContent side="right">
                      <p>{item.title}</p>
                    </TooltipContent>
                  </Tooltip>
                )
              }

              return <div key={item.href}>{linkContent}</div>
            })}
          </nav>
        </ScrollArea>

        {/* 로그아웃 버튼 */}
        <div className="p-4 border-t border-teal-700 dark:border-teal-900">
          <Tooltip delayDuration={0}>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                onClick={handleLogout}
                className={cn(
                  'w-full justify-start gap-3 text-red-300 hover:text-white hover:bg-red-600/50 dark:hover:bg-red-700/50',
                  !isExpanded && 'justify-center px-2'
                )}
              >
                <LogOut className={cn(
                  'flex-shrink-0',
                  isExpanded ? 'h-5 w-5' : 'h-6 w-6'
                )} />
                {isExpanded && <span className="font-medium">로그아웃</span>}
              </Button>
            </TooltipTrigger>
            {!isExpanded && (
              <TooltipContent side="right">
                <p>로그아웃</p>
              </TooltipContent>
            )}
          </Tooltip>
        </div>
      </div>
    </TooltipProvider>
  )
}