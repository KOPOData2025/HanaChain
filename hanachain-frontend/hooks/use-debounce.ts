import { useState, useEffect } from 'react'

/**
 * 입력값에 디바운싱을 적용하는 커스텀 훅
 * API 호출 등의 비용이 큰 작업에서 불필요한 호출을 줄이는 데 사용됩니다.
 * 
 * @param value 디바운싱할 값
 * @param delay 지연 시간 (밀리초)
 * @returns 디바운싱된 값
 */
export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value)
    }, delay)

    return () => {
      clearTimeout(timer)
    }
  }, [value, delay])

  return debouncedValue
}

/**
 * 콜백 함수에 디바운싱을 적용하는 커스텀 훅
 * 
 * @param callback 디바운싱할 콜백 함수
 * @param delay 지연 시간 (밀리초)
 * @returns 디바운싱된 콜백 함수
 */
export function useDebounceCallback<T extends (...args: any[]) => any>(
  callback: T,
  delay: number
): T {
  const [debounceTimer, setDebounceTimer] = useState<NodeJS.Timeout | null>(null)

  const debouncedCallback = ((...args: Parameters<T>) => {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    const timer = setTimeout(() => {
      callback(...args)
    }, delay)

    setDebounceTimer(timer)
  }) as T

  useEffect(() => {
    return () => {
      if (debounceTimer) {
        clearTimeout(debounceTimer)
      }
    }
  }, [debounceTimer])

  return debouncedCallback
}
