'use client'

import React from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Wallet, AlertCircle } from 'lucide-react'

interface BeneficiaryAddressFormProps {
  initialAddress?: string
  campaignId: number
  onSubmit: (address: string) => Promise<void>
  disabled?: boolean
}

export function BeneficiaryAddressForm({
  initialAddress = '',
  campaignId,
  onSubmit,
  disabled = false
}: BeneficiaryAddressFormProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Wallet className="h-5 w-5" />
          수혜자 주소
        </CardTitle>
        <CardDescription>
          주최 단체의 이더리움 주소로 자동 설정됩니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* 현재 주소 표시 */}
        {initialAddress ? (
          <div className="p-4 bg-muted rounded-md">
            <p className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">현재 설정된 주소:</p>
            <p className="font-mono text-sm break-all text-gray-900 dark:text-white">{initialAddress}</p>
          </div>
        ) : (
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              수혜자 주소가 아직 설정되지 않았습니다.
            </AlertDescription>
          </Alert>
        )}

        {/* 안내 메시지 */}
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription className="text-xs">
            <strong>안내:</strong> 수혜자 주소는 주최 단체의 이더리움 주소로 자동 설정되며,
            블록체인 배포 시 기부금을 받을 지갑 주소로 사용됩니다.
          </AlertDescription>
        </Alert>
      </CardContent>
    </Card>
  )
}