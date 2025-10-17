import { Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"

interface LoadingSpinnerProps {
  size?: "sm" | "md" | "lg"
  overlay?: boolean
  text?: string
  className?: string
}

export function LoadingSpinner({ 
  size = "md", 
  overlay = false, 
  text,
  className 
}: LoadingSpinnerProps) {
  const sizes = {
    sm: "w-4 h-4",
    md: "w-6 h-6", 
    lg: "w-8 h-8"
  }

  const spinner = (
    <div className={cn("flex flex-col items-center gap-2", className)}>
      <Loader2 className={cn("animate-spin text-red-500", sizes[size])} />
      {text && (
        <p className="text-sm text-gray-600" aria-busy="true">
          {text}
        </p>
      )}
    </div>
  )

  if (overlay) {
    return (
      <div className="fixed inset-0 bg-black/20 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg p-6 shadow-lg">
          {spinner}
        </div>
      </div>
    )
  }

  return spinner
}
