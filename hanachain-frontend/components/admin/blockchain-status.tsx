'use client'

import React from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Loader2, CheckCircle2, XCircle, AlertCircle, ExternalLink, RefreshCw } from 'lucide-react'
import { BlockchainStatus } from '@/types/donation'

interface BlockchainStatusDisplayProps {
  status?: BlockchainStatus
  transactionHash?: string
  beneficiaryAddress?: string
  blockchainCampaignId?: string
  errorMessage?: string
  processedAt?: string
  onRetry?: () => void
}

const statusConfig = {
  NONE: {
    label: '미연동',
    variant: 'secondary' as const,
    icon: AlertCircle,
    color: 'text-gray-500'
  },
  BLOCKCHAIN_PENDING: {
    label: '배포 대기',
    variant: 'outline' as const,
    icon: AlertCircle,
    color: 'text-yellow-500'
  },
  BLOCKCHAIN_PROCESSING: {
    label: '배포 진행 중',
    variant: 'default' as const,
    icon: Loader2,
    color: 'text-blue-500',
    animate: true
  },
  ACTIVE: {
    label: '배포 완료',
    variant: 'success' as const,
    icon: CheckCircle2,
    color: 'text-green-500'
  },
  BLOCKCHAIN_FAILED: {
    label: '배포 실패',
    variant: 'destructive' as const,
    icon: XCircle,
    color: 'text-red-500'
  }
}

export function BlockchainStatusDisplay({
  status = 'NONE',
  transactionHash,
  beneficiaryAddress,
  blockchainCampaignId,
  errorMessage,
  processedAt,
  onRetry
}: BlockchainStatusDisplayProps) {
  const config = statusConfig[status] || statusConfig.NONE
  const Icon = config.icon

  return (
    <Card>
      <CardHeader>
        <CardTitle>블록체인 배포 상태</CardTitle>
        <CardDescription>
          캠페인의 스마트 컨트랙트 배포 상태를 확인합니다
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* 상태 배지 */}
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <Icon className={`h-5 w-5 ${config.color} ${config.animate ? 'animate-spin' : ''}`} />
            <Badge variant={config.variant}>
              {config.label}
            </Badge>
          </div>
          {processedAt && (
            <div className="text-sm text-muted-foreground">
              처리 시간: {new Date(processedAt).toLocaleString('ko-KR')}
            </div>
          )}
        </div>

        {/* 진행 상태별 메시지 */}
        {status === 'BLOCKCHAIN_PENDING' && (
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              캠페인이 승인되었습니다. 블록체인 배포가 곧 시작됩니다.
            </AlertDescription>
          </Alert>
        )}

        {status === 'BLOCKCHAIN_PROCESSING' && (
          <Alert>
            <Loader2 className="h-4 w-4 animate-spin" />
            <AlertDescription>
              블록체인 네트워크에 캠페인을 배포하고 있습니다. 
              이 과정은 몇 분 정도 소요될 수 있습니다.
            </AlertDescription>
          </Alert>
        )}

        {/* 성공 시 상세 정보 */}
        {status === 'ACTIVE' && (
          <div className="space-y-3">
            <div className="grid gap-2 text-sm">
              {blockchainCampaignId && (
                <div>
                  <span className="font-medium">캠페인 ID:</span>{' '}
                  <span className="font-mono">{blockchainCampaignId}</span>
                </div>
              )}
              {transactionHash && (
                <div className="flex items-center gap-2">
                  <span className="font-medium">트랜잭션:</span>
                  <a
                    href={`https://sepolia.etherscan.io/tx/${transactionHash}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-mono text-xs text-blue-600 hover:underline flex items-center gap-1"
                  >
                    {transactionHash.substring(0, 10)}...{transactionHash.substring(transactionHash.length - 8)}
                    <ExternalLink className="h-3 w-3" />
                  </a>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 실패 시 오류 정보 */}
        {status === 'BLOCKCHAIN_FAILED' && (
          <div className="space-y-3">
            <Alert variant="destructive">
              <XCircle className="h-4 w-4" />
              <AlertDescription>
                블록체인 배포에 실패했습니다.
                {errorMessage && <div className="mt-2 text-xs">{errorMessage}</div>}
              </AlertDescription>
            </Alert>

            {onRetry && (
              <Button 
                onClick={onRetry} 
                variant="outline" 
                size="sm"
                className="flex items-center gap-2"
              >
                <RefreshCw className="h-4 w-4" />
                재시도
              </Button>
            )}
          </div>
        )}

        {/* 미연동 상태 */}
        {status === 'NONE' && (
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              캠페인이 아직 블록체인에 배포되지 않았습니다.
              캠페인 상태를 '활성'으로 변경하면 자동으로 배포가 시작됩니다.
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  )
}