'use client'

import { useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AlertTriangle, RefreshCw, Home } from 'lucide-react'

interface ErrorPageProps {
  error: Error & { digest?: string }
  reset: () => void
}

export default function OrganizationsError({ error, reset }: ErrorPageProps) {
  useEffect(() => {
    console.error('Organizations page error:', error)
  }, [error])

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-center justify-center min-h-[400px]">
        <Card className="w-full max-w-md">
          <CardHeader className="text-center">
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/20">
              <AlertTriangle className="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <CardTitle className="text-xl">오류가 발생했습니다</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-center text-sm text-muted-foreground">
              단체 목록을 불러오는 중에 문제가 발생했습니다.
              잠시 후 다시 시도해주세요.
            </p>
            
            {process.env.NODE_ENV === 'development' && (
              <div className="rounded-md bg-red-50 dark:bg-red-900/20 p-3">
                <p className="text-xs text-red-800 dark:text-red-200 font-mono">
                  {error.message}
                </p>
              </div>
            )}

            <div className="flex flex-col gap-2">
              <Button onClick={reset} className="w-full">
                <RefreshCw className="mr-2 h-4 w-4" />
                다시 시도
              </Button>
              <Button 
                variant="outline" 
                onClick={() => window.location.href = '/admin'}
                className="w-full"
              >
                <Home className="mr-2 h-4 w-4" />
                관리자 홈으로
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}