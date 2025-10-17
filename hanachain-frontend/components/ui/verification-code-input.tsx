"use client"

import { useState, useRef, useEffect, KeyboardEvent, ClipboardEvent } from "react"
import { cn } from "@/lib/utils"

interface VerificationCodeInputProps {
  value: string
  onChange: (value: string) => void
  onFocus?: () => void
  onBlur?: () => void
  disabled?: boolean
  error?: string
  className?: string
  length?: number
  success?: boolean
}

export function VerificationCodeInput({
  value,
  onChange,
  onFocus,
  onBlur,
  disabled = false,
  error,
  className,
  length = 6,
  success = false
}: VerificationCodeInputProps) {
  const [focusedIndex, setFocusedIndex] = useState<number | null>(null)
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])
  const [isFocused, setIsFocused] = useState(false)

  // 입력값을 배열로 변환
  const digits = value.padEnd(length, '').split('').slice(0, length)

  useEffect(() => {
    // 값이 변경될 때 다음 빈 칸으로 포커스 이동
    if (value.length < length) {
      const nextIndex = value.length
      inputRefs.current[nextIndex]?.focus()
      setFocusedIndex(nextIndex)
    }
  }, [value, length])

  const handleInputChange = (index: number, inputValue: string) => {
    if (disabled) return

    // 숫자만 허용
    const numericValue = inputValue.replace(/[^0-9]/g, '')
    
    if (numericValue.length > 1) {
      // 여러 자리 입력 시 (붙여넣기 등)
      const newValue = numericValue.slice(0, length)
      onChange(newValue)
      return
    }

    // 새로운 값 생성
    const newDigits = [...digits]
    newDigits[index] = numericValue
    const newValue = newDigits.join('').replace(/\s/g, '')
    
    onChange(newValue)

    // 값이 입력되고 마지막 칸이 아니면 다음 칸으로 이동
    if (numericValue && index < length - 1) {
      inputRefs.current[index + 1]?.focus()
      setFocusedIndex(index + 1)
    }
  }

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (disabled) return

    if (e.key === 'Backspace') {
      e.preventDefault()
      
      if (digits[index]) {
        // 현재 칸에 값이 있으면 삭제
        const newDigits = [...digits]
        newDigits[index] = ''
        onChange(newDigits.join('').replace(/\s/g, ''))
      } else if (index > 0) {
        // 현재 칸이 비어있으면 이전 칸으로 이동하고 삭제
        const newDigits = [...digits]
        newDigits[index - 1] = ''
        onChange(newDigits.join('').replace(/\s/g, ''))
        inputRefs.current[index - 1]?.focus()
        setFocusedIndex(index - 1)
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus()
      setFocusedIndex(index - 1)
    } else if (e.key === 'ArrowRight' && index < length - 1) {
      inputRefs.current[index + 1]?.focus()
      setFocusedIndex(index + 1)
    }
  }

  const handleFocus = (index: number) => {
    setFocusedIndex(index)
    setIsFocused(true)
    onFocus?.()
  }

  const handleBlur = () => {
    setFocusedIndex(null)
    setIsFocused(false)
    onBlur?.()
  }

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault()
    const pastedText = e.clipboardData.getData('text')
    const numericText = pastedText.replace(/[^0-9]/g, '').slice(0, length)
    onChange(numericText)
  }

  const handleBoxClick = (index: number) => {
    if (disabled) return
    inputRefs.current[index]?.focus()
    setFocusedIndex(index)
  }

  return (
    <div className={cn("space-y-2", className)} role="group" aria-label="인증코드 입력">
      {/* 입력 박스들 */}
      <div className="flex justify-center gap-2 sm:gap-3">
        {Array.from({ length }, (_, index) => (
          <div
            key={index}
            onClick={() => handleBoxClick(index)}
            className={cn(
              "relative w-10 h-12 sm:w-12 sm:h-14 rounded-lg border-2 transition-all duration-200 cursor-text",
              "flex items-center justify-center bg-white touch-manipulation",
              success && digits[index]
                ? "border-green-500 bg-green-50 animate-pulse"
                : focusedIndex === index || (isFocused && index === value.length)
                ? "border-teal-500 ring-2 ring-teal-200 shadow-md"
                : digits[index]
                ? "border-teal-300 bg-teal-50"
                : "border-gray-300 hover:border-gray-400",
              error && !success && "border-red-300 ring-2 ring-red-200",
              disabled && "bg-gray-50 border-gray-200 cursor-not-allowed"
            )}
          >
            {/* 숨겨진 실제 입력 필드 */}
            <input
              ref={(el) => (inputRefs.current[index] = el)}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={1}
              value={digits[index] || ''}
              onChange={(e) => handleInputChange(index, e.target.value)}
              onKeyDown={(e) => handleKeyDown(index, e)}
              onFocus={() => handleFocus(index)}
              onBlur={handleBlur}
              onPaste={handlePaste}
              disabled={disabled}
              className="absolute inset-0 w-full h-full opacity-0 cursor-text"
              aria-label={`인증코드 ${index + 1}번째 자리`}
              aria-describedby={error ? "verification-error" : undefined}
            />
            
            {/* 표시되는 숫자 */}
            <span
              className={cn(
                "text-lg sm:text-xl font-semibold transition-colors",
                success && digits[index]
                  ? "text-green-700"
                  : digits[index] 
                  ? "text-teal-700" 
                  : "text-gray-400",
                focusedIndex === index && !success && "text-teal-600"
              )}
              aria-hidden="true"
            >
              {digits[index] || ''}
            </span>

            {/* 포커스 커서 */}
            {focusedIndex === index && !digits[index] && (
              <div className="absolute inset-0 flex items-center justify-center" aria-hidden="true">
                <div className="w-0.5 h-5 sm:h-6 bg-teal-500 animate-pulse" />
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 에러 메시지 */}
      {error && (
        <p id="verification-error" className="text-sm text-red-500 text-center mt-2" role="alert">
          {error}
        </p>
      )}

      {/* 진행 표시 */}
      <div className="flex justify-center mt-3" aria-hidden="true">
        <div className="flex gap-1">
          {Array.from({ length }, (_, index) => (
            <div
              key={index}
              className={cn(
                "w-2 h-2 rounded-full transition-colors duration-200",
                index < value.length ? "bg-teal-500" : "bg-gray-300"
              )}
            />
          ))}
        </div>
      </div>

      {/* 스크린 리더용 진행 상황 안내 */}
      <div className="sr-only" aria-live="polite">
        {value.length}자리 중 {value.length}자리 입력 완료
      </div>
    </div>
  )
}