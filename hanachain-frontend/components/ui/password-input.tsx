"use client"

import * as React from "react"
import { Eye, EyeOff } from "lucide-react"
import { cn } from "@/lib/utils"

interface PasswordInputProps extends React.ComponentProps<"input"> {
  label?: string
  error?: string
  required?: boolean
  showStrengthIndicator?: boolean
}

const PasswordInput = React.forwardRef<HTMLInputElement, PasswordInputProps>(
  ({ 
    className, 
    label, 
    error, 
    required, 
    showStrengthIndicator = false,
    ...props 
  }, ref) => {
    const [showPassword, setShowPassword] = React.useState(false)
    const inputId = React.useId()
    const password = props.value as string || ""

    // 비밀번호 강도 체크
    const getPasswordStrength = (password: string) => {
      if (!password) return { score: 0, text: "" }
      
      let score = 0
      const checks = {
        length: password.length >= 8,
        lowercase: /[a-z]/.test(password),
        uppercase: /[A-Z]/.test(password),
        number: /\d/.test(password),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
      }
      
      Object.values(checks).forEach(check => check && score++)
      
      if (score < 3) return { score, text: "약함", color: "text-red-500" }
      if (score < 4) return { score, text: "보통", color: "text-yellow-500" }
      return { score, text: "강함", color: "text-green-500" }
    }

    const strength = showStrengthIndicator ? getPasswordStrength(password) : null

    return (
      <div className="space-y-1">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-medium text-gray-700"
          >
            {label}
            {required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <div className="relative">
          <input
            id={inputId}
            type={showPassword ? "text" : "password"}
            className={cn(
              "w-full px-0 py-2 border-0 border-b-2 bg-transparent pr-10",
              "focus:outline-none focus:ring-0 transition-colors",
              error
                ? "border-red-500 focus:border-red-500"
                : "border-gray-300 focus:border-teal-500",
              "placeholder:text-gray-400",
              className
            )}
            ref={ref}
            aria-invalid={!!error}
            aria-describedby={error ? `${inputId}-error` : undefined}
            {...props}
          />
          <button
            type="button"
            className="absolute right-0 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
            onClick={() => setShowPassword(!showPassword)}
            tabIndex={-1}
          >
            {showPassword ? (
              <EyeOff className="w-5 h-5" />
            ) : (
              <Eye className="w-5 h-5" />
            )}
          </button>
        </div>
        
        {strength && password && (
          <div className="flex items-center gap-2 mt-2">
            <div className="flex gap-1">
              {[1, 2, 3, 4, 5].map((level) => (
                <div
                  key={level}
                  className={cn(
                    "w-6 h-1 rounded-full",
                    level <= strength.score
                      ? strength.score < 3
                        ? "bg-red-500"
                        : strength.score < 4
                        ? "bg-yellow-500"
                        : "bg-green-500"
                      : "bg-gray-200"
                  )}
                />
              ))}
            </div>
            <span className={cn("text-xs", strength.color)}>
              {strength.text}
            </span>
          </div>
        )}
        
        {error && (
          <p id={`${inputId}-error`} className="text-sm text-red-500">
            {error}
          </p>
        )}
      </div>
    )
  }
)

PasswordInput.displayName = "PasswordInput"

export { PasswordInput }
