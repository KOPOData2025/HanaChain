import * as React from "react"
import { cn } from "@/lib/utils"

interface FormInputProps extends React.ComponentProps<"input"> {
  label?: string
  error?: string
  required?: boolean
  helperText?: string
}

const FormInput = React.forwardRef<HTMLInputElement, FormInputProps>(
  ({ className, label, error, required, helperText, type, ...props }, ref) => {
    const inputId = React.useId()

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
        <input
          id={inputId}
          type={type}
          className={cn(
            "w-full px-0 py-2 border-0 border-b-2 bg-transparent",
            "focus:outline-none focus:ring-0 transition-colors",
            error
              ? "border-red-500 focus:border-red-500"
              : "border-gray-300 focus:border-teal-500",
            "placeholder:text-gray-400",
            className
          )}
          ref={ref}
          aria-invalid={!!error}
          aria-describedby={
            error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined
          }
          {...props}
        />
        {error && (
          <p id={`${inputId}-error`} className="text-sm text-red-500">
            {error}
          </p>
        )}
        {helperText && !error && (
          <p id={`${inputId}-helper`} className="text-sm text-gray-500">
            {helperText}
          </p>
        )}
      </div>
    )
  }
)

FormInput.displayName = "FormInput"

export { FormInput }
