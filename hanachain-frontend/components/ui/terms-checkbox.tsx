"use client"

import * as React from "react"
import { Check } from "lucide-react"
import { cn } from "@/lib/utils"

interface TermsCheckboxProps {
  id?: string
  checked?: boolean
  onChange?: (checked: boolean) => void
  label: string
  required?: boolean
  description?: string
  className?: string
}

export function TermsCheckbox({
  id,
  checked = false,
  onChange,
  label,
  required = false,
  description,
  className
}: TermsCheckboxProps) {
  const checkboxId = id || React.useId()

  return (
    <div className={cn("flex items-start space-x-3", className)}>
      <button
        type="button"
        role="checkbox"
        aria-checked={checked}
        aria-required={required}
        onClick={() => onChange?.(!checked)}
        className={cn(
          "flex-shrink-0 w-5 h-5 mt-0.5 rounded border-2 flex items-center justify-center transition-colors",
          "focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 focus-visible:ring-offset-2",
          checked
            ? "bg-teal-500 border-teal-500 text-white"
            : "border-gray-300 hover:border-gray-400"
        )}
      >
        {checked && <Check className="w-3 h-3" />}
      </button>
      <div className="flex-1">
        <label
          htmlFor={checkboxId}
          className="text-sm text-gray-700 cursor-pointer"
        >
          {label}
          {required && <span className="text-teal-500 ml-1">*</span>}
        </label>
        {description && (
          <p className="text-xs text-gray-500 mt-1">{description}</p>
        )}
      </div>
    </div>
  )
}
