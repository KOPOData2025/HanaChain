'use client'

import React, { useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Wallet, Save, AlertCircle, CheckCircle2 } from 'lucide-react'

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
  const [address, setAddress] = useState(initialAddress)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // 이더리움 주소 유효성 검사
  const validateAddress = (addr: string): boolean => {
    // 이더리움 주소: 0x로 시작하고 40개의 16진수 문자
    const ethAddressRegex = /^0x[a-fA-F0-9]{40}$/
    return ethAddressRegex.test(addr)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)

    // 유효성 검사
    if (!address) {
      setError('수혜자 주소를 입력해주세요.')
      return
    }

    if (!validateAddress(address)) {
      setError('올바른 이더리움 주소 형식이 아닙니다. (0x로 시작하는 42자)')
      return
    }

    // 제출
    setIsSubmitting(true)
    try {
      await onSubmit(address)
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : '주소 저장에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleAddressChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newAddress = e.target.value
    setAddress(newAddress)
    
    // 실시간 유효성 검사 (입력 중에는 에러 표시 안 함)
    if (error && newAddress) {
      if (validateAddress(newAddress)) {
        setError(null)
      }
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Wallet className="h-5 w-5" />
          수혜자 주소 설정
        </CardTitle>
        <CardDescription>
          기부금을 받을 이더리움 지갑 주소를 설정합니다.
          블록체인 배포 전에 반드시 설정해야 합니다.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="beneficiary-address">
              이더리움 주소 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="beneficiary-address"
              type="text"
              placeholder="0x..."
              value={address}
              onChange={handleAddressChange}
              disabled={disabled || isSubmitting}
              className="font-mono text-sm"
            />
            <p className="text-xs text-muted-foreground">
              Sepolia 테스트넷에서 사용할 수 있는 이더리움 지갑 주소를 입력하세요.
            </p>
          </div>

          {/* 에러 메시지 */}
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* 성공 메시지 */}
          {success && (
            <Alert className="border-green-200 bg-green-50">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <AlertDescription className="text-green-800">
                수혜자 주소가 성공적으로 저장되었습니다.
              </AlertDescription>
            </Alert>
          )}

          {/* 현재 주소 표시 */}
          {initialAddress && !address && (
            <div className="p-3 bg-muted rounded-md">
              <p className="text-sm font-medium mb-1">현재 설정된 주소:</p>
              <p className="font-mono text-xs break-all">{initialAddress}</p>
            </div>
          )}

          {/* 제출 버튼 */}
          <Button
            type="submit"
            disabled={disabled || isSubmitting || !address || address === initialAddress}
            className="w-full"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                저장 중...
              </>
            ) : (
              <>
                <Save className="mr-2 h-4 w-4" />
                주소 저장
              </>
            )}
          </Button>

          {/* 주의사항 */}
          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription className="text-xs">
              <strong>주의:</strong> 잘못된 주소를 입력하면 기부금을 받을 수 없습니다. 
              주소를 정확히 확인한 후 저장해주세요. 
              블록체인 배포 후에는 수정이 제한될 수 있습니다.
            </AlertDescription>
          </Alert>
        </form>
      </CardContent>
    </Card>
  )
}

// Loader2 import 추가
import { Loader2 } from 'lucide-react'