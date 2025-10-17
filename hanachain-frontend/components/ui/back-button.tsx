"use client"

import { ArrowLeft } from "lucide-react"
import { useRouter } from "next/navigation"
import { cn } from "@/lib/utils"

interface BackButtonProps {
  onBack?: () => void
  href?: string
  className?: string
  children?: React.ReactNode
}

export function BackButton({ onBack, href, className, children }: BackButtonProps) {
  const router = useRouter()

  const handleClick = () => {
    if (onBack) {
      onBack()
    } else if (href) {
      router.push(href)
    } else {
      router.back()
    }
  }

  return (
    <button
      onClick={handleClick}
      className={cn(
        "inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors",
        "focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2",
        "rounded-md p-2 -ml-2",
        className
      )}
      aria-label="이전 단계로 돌아가기"
    >
      <ArrowLeft className="h-5 w-5" />
      {children || "이전"}
    </button>
  )
}
