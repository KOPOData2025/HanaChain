'use client'

import { useEffect, useState, useRef, useCallback } from 'react'
import { toast } from 'sonner'
import {
  RefreshCw,
  CheckCircle,
  XCircle,
  Clock,
  AlertCircle,
  PlayCircle,
  StopCircle
} from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { getBatchJobStatus } from '@/lib/api/campaign-api'
import { BatchJobStatusResponse, BATCH_STATUS_LABELS, BatchJobStatus } from '@/types/admin'
import { ApiError } from '@/lib/api/client'
import { cn } from '@/lib/utils'

interface BatchStatusCardProps {
  jobExecutionId: number
  campaignId: number
  onComplete?: () => void
  onError?: (error: string) => void
}

export function BatchStatusCard({
  jobExecutionId,
  campaignId,
  onComplete,
  onError
}: BatchStatusCardProps) {
  const [status, setStatus] = useState<BatchJobStatusResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const completedCallbackFiredRef = useRef(false)
  const initialLoadDoneRef = useRef(false) // 초기 로드 완료 여부 추적

  console.log('🎯 BatchStatusCard 렌더링:', { jobExecutionId, campaignId, status, loading })

  // 배치 작업 상태 조회 (useCallback으로 안정적인 참조 유지)
  const fetchBatchStatus = useCallback(async () => {
    if (!jobExecutionId) return

    setLoading(true)
    try {
      const response = await getBatchJobStatus(jobExecutionId)
      setStatus(response)

      // 작업 완료 시 콜백 호출 (한 번만)
      if (response.status === 'COMPLETED') {
        if (!completedCallbackFiredRef.current) {
          completedCallbackFiredRef.current = true
          onComplete?.()
        }
      }

      // 작업 실패 시 콜백 호출
      if (response.status === 'FAILED') {
        onError?.('배치 작업이 실패했습니다.')
      }
    } catch (error) {
      console.error('❌ 배치 작업 상태 조회 실패:', error)

      let errorMessage = '배치 작업 상태를 조회하는데 실패했습니다'
      if (error instanceof ApiError) {
        errorMessage = error.message
      }

      toast.error('상태 조회 실패', {
        description: errorMessage
      })

      onError?.(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [jobExecutionId, onComplete, onError]) // 의존성 명시

  // 초기 로드 (정말 한 번만 실행, 절대 반복 안 함)
  useEffect(() => {
    if (!initialLoadDoneRef.current) {
      initialLoadDoneRef.current = true
      fetchBatchStatus()
    }
  }, [fetchBatchStatus])

  // 상태 뱃지 색상
  const getStatusBadgeVariant = (status: BatchJobStatus) => {
    switch (status) {
      case 'STARTING':
      case 'STARTED':
        return 'secondary'
      case 'RUNNING':
        return 'default'
      case 'COMPLETED':
        return 'success'
      case 'FAILED':
        return 'destructive'
      case 'STOPPED':
        return 'outline'
      default:
        return 'secondary'
    }
  }

  // 상태 아이콘
  const getStatusIcon = (status: BatchJobStatus) => {
    switch (status) {
      case 'STARTING':
      case 'STARTED':
        return <PlayCircle className="h-3 w-3" />
      case 'RUNNING':
        return <RefreshCw className="h-3 w-3 animate-spin" />
      case 'COMPLETED':
        return <CheckCircle className="h-3 w-3" />
      case 'FAILED':
        return <XCircle className="h-3 w-3" />
      case 'STOPPED':
        return <StopCircle className="h-3 w-3" />
      default:
        return <Clock className="h-3 w-3" />
    }
  }

  if (!status) {
    return (
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="text-lg">📊 배치 작업 상태</CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={fetchBatchStatus}
              disabled={loading}
            >
              <RefreshCw className={cn('h-4 w-4', loading && 'animate-spin')} />
            </Button>
          </div>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="flex flex-col justify-center items-center py-8 text-center">
            <RefreshCw className="h-8 w-8 text-gray-400 mb-3" />
            <p className="text-gray-600 dark:text-gray-400 mb-2">
              새로고침 버튼을 눌러 배치 작업 상태를 확인하세요
            </p>
            <p className="text-sm text-gray-500">
              자동 polling이 비활성화되었습니다
            </p>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="text-lg">📊 배치 작업 상태</CardTitle>
          <div className="flex items-center gap-2">
            <Badge
              variant={getStatusBadgeVariant(status.status)}
              className="flex items-center gap-1"
            >
              {getStatusIcon(status.status)}
              {BATCH_STATUS_LABELS[status.status]}
            </Badge>
            <Button
              variant="outline"
              size="sm"
              onClick={fetchBatchStatus}
              disabled={loading}
            >
              <RefreshCw className={cn('h-4 w-4', loading && 'animate-spin')} />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* 진행률 */}
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600 dark:text-gray-400">진행률</span>
            <span className="font-medium">{status.progressPercentage.toFixed(1)}%</span>
          </div>
          <Progress value={status.progressPercentage} className="h-2" />
        </div>

        {/* 통계 */}
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <div className="text-xs text-gray-500">처리 건수</div>
            <div className="text-2xl font-bold">
              {status.totalProcessed}
              <span className="text-sm text-gray-500 ml-1">건</span>
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-xs text-gray-500">성공</div>
            <div className="text-2xl font-bold text-green-600">
              {status.successfulTransfers}
              <span className="text-sm text-gray-500 ml-1">건</span>
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-xs text-gray-500">실패</div>
            <div className="text-2xl font-bold text-red-600">
              {status.failedTransfers}
              <span className="text-sm text-gray-500 ml-1">건</span>
            </div>
          </div>

          <div className="space-y-1">
            <div className="text-xs text-gray-500">스킵</div>
            <div className="text-2xl font-bold text-yellow-600">
              {status.skippedCount}
              <span className="text-sm text-gray-500 ml-1">건</span>
            </div>
          </div>
        </div>

        {/* 작업 정보 */}
        <div className="border-t pt-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">작업 ID</span>
            <span className="font-mono">{status.jobExecutionId}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">작업명</span>
            <span>{status.jobName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">시작 시간</span>
            <span>{new Date(status.startTime).toLocaleString('ko-KR')}</span>
          </div>
          {status.endTime && (
            <div className="flex justify-between">
              <span className="text-gray-600 dark:text-gray-400">종료 시간</span>
              <span>{new Date(status.endTime).toLocaleString('ko-KR')}</span>
            </div>
          )}
          <div className="flex justify-between">
            <span className="text-gray-600 dark:text-gray-400">종료 코드</span>
            <span>{status.exitCode}</span>
          </div>
        </div>

        {/* 상태 메시지 */}
        {status.status === 'RUNNING' && (
          <div className="flex items-center gap-2 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
            <RefreshCw className="h-4 w-4 text-blue-600 animate-spin" />
            <span className="text-sm text-blue-700 dark:text-blue-300">
              블록체인 트랜잭션 처리 중입니다...
            </span>
          </div>
        )}

        {status.status === 'COMPLETED' && (
          <div className="flex items-center gap-2 p-3 bg-green-50 dark:bg-green-900/20 rounded-lg">
            <CheckCircle className="h-4 w-4 text-green-600" />
            <span className="text-sm text-green-700 dark:text-green-300">
              배치 작업이 성공적으로 완료되었습니다.
            </span>
          </div>
        )}

        {status.status === 'FAILED' && (
          <div className="flex items-center gap-2 p-3 bg-red-50 dark:bg-red-900/20 rounded-lg">
            <AlertCircle className="h-4 w-4 text-red-600" />
            <span className="text-sm text-red-700 dark:text-red-300">
              배치 작업이 실패했습니다. 로그를 확인해주세요.
            </span>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
