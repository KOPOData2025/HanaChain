import * as React from "react"
import { Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"

interface CTAButtonProps extends React.ComponentProps<"button"> {
  loading?: boolean
  fullWidth?: boolean
  size?: "sm" | "md" | "lg"
  variant?: "primary" | "secondary"
}

const CTAButton = React.forwardRef<HTMLButtonElement, CTAButtonProps>(
  ({ 
    className, 
    loading = false, 
    fullWidth = true, 
    size = "md", 
    variant = "primary",
    children, 
    disabled,
    ...props 
  }, ref) => {
    const sizeClasses = {
      sm: "py-2 px-4 text-sm",
      md: "py-3 px-6 text-base",
      lg: "py-4 px-8 text-lg"
    }

    const variantClasses = {
      primary: "bg-teal-500 text-white hover:bg-teal-600 focus-visible:ring-teal-500",
      secondary: "bg-gray-200 text-gray-900 hover:bg-gray-300 focus-visible:ring-gray-400"
    }

    return (
      <button
        className={cn(
          "inline-flex items-center justify-center gap-2 rounded-md font-medium transition-all",
          "focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2",
          "disabled:opacity-50 disabled:pointer-events-none",
          fullWidth ? "w-full" : "",
          sizeClasses[size],
          variantClasses[variant],
          className
        )}
        disabled={disabled || loading}
        aria-busy={loading}
        ref={ref}
        {...props}
      >
        {loading && <Loader2 className="w-4 h-4 animate-spin" />}
        {children}
      </button>
    )
  }
)

CTAButton.displayName = "CTAButton"

export { CTAButton }
