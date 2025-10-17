import { AlertCircle, AlertTriangle, Info } from "lucide-react"
import { cn } from "@/lib/utils"

interface ErrorMessageProps {
  message: string
  type?: "error" | "warning" | "info"
  className?: string
}

export function ErrorMessage({ message, type = "error", className }: ErrorMessageProps) {
  if (!message) return null

  const icons = {
    error: AlertCircle,
    warning: AlertTriangle,
    info: Info
  }

  const styles = {
    error: "text-red-500",
    warning: "text-yellow-500", 
    info: "text-blue-500"
  }

  const Icon = icons[type]

  return (
    <div className={cn(
      "flex items-center gap-2 text-sm mt-1 animate-in fade-in duration-200",
      styles[type],
      className
    )}>
      <Icon className="w-4 h-4 flex-shrink-0" />
      <span>{message}</span>
    </div>
  )
}
