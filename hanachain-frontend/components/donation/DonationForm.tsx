"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogOverlay,
  DialogPortal
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Heart } from "lucide-react"
import { processDonationPayment, type DonationPaymentData } from "@/lib/payment/portone"
import type { PaymentSuccessData } from "@/types/donation"
import { useAuth } from "@/lib/auth-context"
import { cn } from "@/lib/utils"

interface DonationFormProps {
  isOpen: boolean
  onClose: () => void
  campaignId: string
  campaignTitle: string
  onPaymentSuccess: (data: PaymentSuccessData) => void
}

const PRESET_AMOUNTS = [10000, 30000, 50000, 100000, 300000, 500000]

export default function DonationForm({
  isOpen,
  onClose,
  campaignId,
  campaignTitle,
  onPaymentSuccess,
}: DonationFormProps) {
  const router = useRouter()
  const { user } = useAuth()
  const [selectedAmount, setSelectedAmount] = useState<string>("30000")
  const [customAmount, setCustomAmount] = useState("")
  const [donorName, setDonorName] = useState(user?.name || "")
  const [donorEmail, setDonorEmail] = useState(user?.email || "")
  const [donorPhone, setDonorPhone] = useState(user?.phoneNumber || "")
  const [isProcessing, setIsProcessing] = useState(false)
  const [isPaymentInProgress, setIsPaymentInProgress] = useState(false)

  // 사용자 정보가 변경되면 기부자 정보 동기화
  useEffect(() => {
    if (user) {
      setDonorName(user.name || "")
      setDonorEmail(user.email || "")
      setDonorPhone(user.phoneNumber || "")
    }
  }, [user])

  const getDonationAmount = () => {
    if (selectedAmount === "custom") {
      return parseInt(customAmount) || 0
    }
    return parseInt(selectedAmount)
  }

  const handlePayment = async () => {
    const amount = getDonationAmount()

    if (amount < 1000) {
      alert("최소 기부 금액은 1,000원입니다.")
      return
    }

    if (!donorName.trim()) {
      alert("기부자 이름을 입력해주세요.")
      return
    }

    if (!donorEmail.trim()) {
      alert("이메일을 입력해주세요.")
      return
    }

    if (!donorPhone.trim()) {
      alert("전화번호를 입력해주세요.")
      return
    }

    setIsProcessing(true)
    setIsPaymentInProgress(true) // 결제 진행 시작 - 모달 숨김

    try {
      // PortOne V2 SDK 결제 요청
      const paymentData: DonationPaymentData = {
        amount,
        campaignId,
        donorName,
        donorEmail,
        donorPhone,
        campaignTitle,
      }

      // PortOne 결제 요청 (모달은 숨겨진 상태)
      const result = await processDonationPayment(paymentData)

      if (result.success && result.paymentId) {
        // 결제 성공 - 웹훅에 의해 자동으로 승인됨
        console.log('✅ 결제 성공, 기부 완료 페이지로 리다이렉트')

        const successData: PaymentSuccessData = {
          amount,
          paymentId: result.paymentId,
          transactionId: result.transactionId,
          donorName,
        }

        onPaymentSuccess(successData)

        // 결제 성공 시 모달 닫기
        onClose()

        // 기부 완료 페이지로 리다이렉트
        const params = new URLSearchParams({
          campaignTitle,
          amount: amount.toString(),
          campaignId,
        })
        router.push(`/donation/complete?${params.toString()}`)
      } else {
        // 결제 실패 또는 취소 - 모달 다시 표시하여 재시도 가능하도록
        console.error('❌ 결제 실패:', result.error)
        setIsPaymentInProgress(false) // 모달 다시 표시
        alert(result.error || "결제에 실패했습니다. 다시 시도해주세요.")
      }
    } catch (error) {
      console.error("Payment error:", error)
      setIsPaymentInProgress(false) // 에러 발생 시 모달 다시 표시
      alert("결제 중 오류가 발생했습니다. 다시 시도해주세요.")
    } finally {
      setIsProcessing(false)
    }
  }

  const formatCurrency = (amount: number) => {
    return amount.toLocaleString() + "원"
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogPortal>
        <DialogOverlay
          className={cn(
            isPaymentInProgress && "pointer-events-none"
          )}
        />
        <DialogContent
          className={cn(
            "sm:max-w-[500px] max-h-[90vh] overflow-y-auto",
            isPaymentInProgress && "opacity-0 pointer-events-none"
          )}
        >
        <DialogHeader className="space-y-3">
          <DialogTitle className="text-xl font-bold">기부하기</DialogTitle>
          <div className="text-sm text-gray-600">
            <p className="font-medium">{campaignTitle}</p>
          </div>
        </DialogHeader>

        <div className="space-y-6">
          {/* 기부 금액 선택 */}
          <div className="space-y-4">
            <Label className="text-base font-semibold">기부 금액</Label>
            <RadioGroup
              value={selectedAmount}
              onValueChange={setSelectedAmount}
              className="grid grid-cols-2 gap-3"
            >
              {PRESET_AMOUNTS.map((amount) => (
                <div key={amount} className="flex items-center space-x-2">
                  <RadioGroupItem value={amount.toString()} id={amount.toString()} />
                  <Label
                    htmlFor={amount.toString()}
                    className="flex-1 text-center py-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
                  >
                    {formatCurrency(amount)}
                  </Label>
                </div>
              ))}
              {/* 직접 입력 */}
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="custom" id="custom" />
                <div className="flex-1">
                  <Label htmlFor="custom" className="sr-only">
                    직접 입력
                  </Label>
                  <div className="relative">
                    <Input
                      type="number"
                      placeholder="직접 입력 (최소 1,000원)"
                      value={customAmount}
                      onChange={(e) => {
                        setCustomAmount(e.target.value)
                        setSelectedAmount("custom")
                      }}
                      className="pr-8"
                      min="1000"
                    />
                    <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 text-sm">
                      원
                    </span>
                  </div>
                </div>
              </div>
            </RadioGroup>
          </div>

          {/* 기부자 정보 */}
          <div className="space-y-4">
            <Label className="text-base font-semibold">기부자 정보</Label>
            
            <div className="space-y-3">
              <div>
                <Label htmlFor="donorName">이름 *</Label>
                <Input
                  id="donorName"
                  type="text"
                  placeholder="사용자 정보에서 가져옴"
                  value={donorName}
                  readOnly
                  className="bg-gray-50 cursor-not-allowed"
                  required
                />
              </div>

              <div>
                <Label htmlFor="donorEmail">이메일 *</Label>
                <Input
                  id="donorEmail"
                  type="email"
                  placeholder="사용자 정보에서 가져옴"
                  value={donorEmail}
                  readOnly
                  className="bg-gray-50 cursor-not-allowed"
                  required
                />
              </div>

              <div>
                <Label htmlFor="donorPhone">전화번호 *</Label>
                <Input
                  id="donorPhone"
                  type="tel"
                  placeholder="사용자 정보에서 가져옴"
                  value={donorPhone}
                  readOnly
                  className="bg-gray-50 cursor-not-allowed"
                  required
                />
              </div>
            </div>
          </div>

          {/* 기부 요약 */}
          <Card className="bg-[#009591]/5 border-[#009591]/20">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Heart className="h-5 w-5 text-[#009591]" />
                  <span className="font-medium">기부 금액</span>
                </div>
                <span className="text-xl font-bold text-[#009591]">
                  {formatCurrency(getDonationAmount())}
                </span>
              </div>
            </CardContent>
          </Card>

          {/* 결제 버튼 */}
          <Button
            onClick={handlePayment}
            disabled={isProcessing || getDonationAmount() < 1000}
            className="w-full bg-[#009591] hover:bg-[#007A77] text-white text-lg py-6"
          >
            {isProcessing ? (
              <div className="flex items-center space-x-2">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                <span>결제 처리 중...</span>
              </div>
            ) : (
              `${formatCurrency(getDonationAmount())} 기부하기`
            )}
          </Button>

          <p className="text-xs text-gray-500 text-center">
            결제는 안전하게 포트원(구 아임포트)을 통해 처리됩니다.
          </p>
        </div>
        </DialogContent>
      </DialogPortal>
    </Dialog>
  )
}