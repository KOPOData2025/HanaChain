"use client"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Search, User, LogOut } from "lucide-react"
import Link from "next/link"
import Image from "next/image"
import { useAuth } from "@/lib/auth-context"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

interface HeaderProps {
  title?: string
  showLogo?: boolean
  showFullNavigation?: boolean
  className?: string
}

export function Header({ 
  title, 
  showLogo = true, 
  showFullNavigation = false,
  className 
}: HeaderProps) {
  const { isLoggedIn, user, logout } = useAuth()

  if (showFullNavigation) {
    return (
      <header className={cn("bg-white border-b fixed top-0 left-0 right-0 z-50", className)}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-8">
              <Link href="/" className="flex items-center space-x-2 hover:opacity-80 transition-opacity">
                <Image 
                  src="/hana_logo.PNG" 
                  alt="HanaChain 로고" 
                  width={50} 
                  height={50} 
                  className="object-contain"
                />
                <span className="text-xl font-bold text-gray-900">HanaChain</span>
              </Link>
              <nav className="hidden md:flex space-x-8">
                <a href="#" className="text-gray-700 hover:text-[#009591]">
                  기부하기
                </a>
                <a href="#" className="text-gray-700 hover:text-[#009591]">
                  소식
                </a>
                {isLoggedIn && (
                  <Link href="/mypage" className="text-gray-700 hover:text-[#009591]">
                    마이페이지
                  </Link>
                )}
              </nav>
            </div>
            <div className="flex items-center space-x-4">
              <div className="relative">
                <Search className="h-5 w-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="검색"
                  className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#009591]"
                />
              </div>
              
              {isLoggedIn ? (
                <>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="sm" className="flex items-center space-x-2">
                        <User className="h-4 w-4" />
                        <span className="hidden sm:inline text-sm">{user?.nickname || user?.email}</span>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-48">
                      <div className="px-2 py-1.5 text-sm text-gray-600">
                        {user?.email}
                      </div>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem asChild>
                        <Link href="/mypage" className="flex items-center">
                          <User className="mr-2 h-4 w-4" />
                          <span>마이페이지</span>
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem 
                        onClick={logout}
                        className="flex items-center text-red-600 focus:text-red-600"
                      >
                        <LogOut className="mr-2 h-4 w-4" />
                        <span>로그아웃</span>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              ) : (
                <>
                  <Link href="/login">
                    <Button variant="ghost" size="sm" className="text-gray-700 hover:text-[#009591]">
                      로그인
                    </Button>
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>
    )
  }

  return (
    <header className={cn("flex items-center justify-between py-4", className)}>
      {showLogo && (
        <div className="flex items-center">
          <Link href="/" className="flex items-center space-x-2 hover:opacity-80 transition-opacity">
            <Image 
              src="/hana_logo.PNG" 
              alt="HanaChain 로고" 
              width={50} 
              height={50} 
              className="object-contain"
            />
            <h1 className="text-xl font-bold text-gray-900" aria-label="HanaChain 로고">
              HanaChain
            </h1>
          </Link>
        </div>
      )}
      {title && (
        <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      )}
    </header>
  )
}
