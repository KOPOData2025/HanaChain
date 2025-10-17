"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
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
import { processDonationPayment, type DonationPaymentData } from "@/lib/payment/portone"
import type { PaymentSuccessData } from "@/types/donation"
import { useAuth } from "@/lib/auth-context"
import { cn } from "@/lib/utils"
import Image from "next/image"
import CountUp from "react-countup"

interface DonationFormProps {
  isOpen: boolean
  onClose: () => void
  campaignId: string
  campaignTitle: string
  onPaymentSuccess: (data: PaymentSuccessData) => void
}

const INCREMENT_AMOUNTS = [5000, 10000, 50000, 100000]

export default function DonationForm({
  isOpen,
  onClose,
  campaignId,
  campaignTitle,
  onPaymentSuccess,
}: DonationFormProps) {
  const router = useRouter()
  const { user } = useAuth()
  const [amount, setAmount] = useState<number>(0)
  const [prevAmount, setPrevAmount] = useState<number>(0)
  const [isAnimating, setIsAnimating] = useState(false)
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

  const handleIncrementAmount = (increment: number) => {
    setPrevAmount(amount)
    setAmount(prev => prev + increment)
    setIsAnimating(true)
  }

  const handleAmountChange = (value: string) => {
    // 숫자만 추출 (쉼표 제거)
    const numValue = parseInt(value.replace(/,/g, '')) || 0
    setIsAnimating(false)
    setAmount(numValue)
  }

  const formatAmountDisplay = (num: number) => {
    if (num === 0) return ''
    return num.toLocaleString('ko-KR')
  }

  const getDonationAmount = () => {
    return amount
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
          <div className="text-xl text-gray-600">
            <p className="font-medium">{campaignTitle}</p>
          </div>
        </DialogHeader>

        <div className="space-y-6">
          {/* 기부 금액 입력 */}
          <div className="space-y-4">
            {/* 큰 숫자 입력 필드 */}
            <div className="relative">
              {isAnimating ? (
                // 애니메이션 중: CountUp 표시
                <div className="w-full text-3xl font-bold text-center h-20 border-2 border-[#009591]/30 rounded-xl px-4 flex items-center justify-center bg-white">
                  <CountUp
                    start={prevAmount}
                    end={amount}
                    duration={0.8}
                    separator=","
                    onEnd={() => setIsAnimating(false)}
                    formattingFn={(value) => value.toLocaleString('ko-KR')}
                  />
                </div>
              ) : (
                // 일반 상태: input 표시
                <input
                  type="text"
                  value={formatAmountDisplay(amount)}
                  onChange={(e) => handleAmountChange(e.target.value)}
                  className="w-full text-3xl font-bold text-center h-20 border-2 border-[#009591]/30 rounded-xl px-4 focus:outline-none focus:ring-2 focus:ring-[#009591] focus:border-transparent"
                  placeholder="0"
                  inputMode="numeric"
                />
              )}
              <span className="absolute right-6 top-1/2 transform -translate-y-1/2 text-2xl text-gray-500 pointer-events-none">
                원
              </span>
            </div>

            {/* 증액 버튼 */}
            <div className="grid grid-cols-4 gap-2">
              {INCREMENT_AMOUNTS.map((increment) => (
                <Button
                  key={increment}
                  type="button"
                  variant="outline"
                  onClick={() => handleIncrementAmount(increment)}
                  className="text-sm font-medium py-6 transition-transform active:scale-95 hover:bg-gray-100"
                >
                  +{increment >= 10000 ? `${increment / 10000}만` : `${increment / 1000}천`}원
                </Button>
              ))}
            </div>
          </div>

          {/* 기부자 정보 */}
          <div className="space-y-4">
            <Label className="text-base font-semibold">기부자 정보</Label>

            <div className="grid grid-cols-1 md:grid-cols-[auto_1fr] gap-6">
              {/* 왼쪽: 이미지 */}
              <div className="flex items-center justify-center md:justify-start">
                <Image
                  src="/byul_baby_heart.png"
                  alt="별이 캐릭터"
                  width={50}
                  height={50}
                  className="w-full max-w-[100px] h-auto"
                />
              </div>

              {/* 오른쪽: 입력 필드 */}
              <div className="space-y-3">
                <div>
                  <Label htmlFor="donorName" className="mb-2 block">이름</Label>
                  <Input
                    id="donorName"
                    type="text"
                    placeholder="사용자 정보에서 가져옴"
                    value={donorName}
                    readOnly
                    className="bg-gray-50 cursor-not-allowed border-[#009591]/30"
                    required
                  />
                </div>

                <div>
                  <Label htmlFor="donorEmail" className="mb-2 block">이메일</Label>
                  <Input
                    id="donorEmail"
                    type="email"
                    placeholder="사용자 정보에서 가져옴"
                    value={donorEmail}
                    readOnly
                    className="bg-gray-50 cursor-not-allowed border-[#009591]/30"
                    required
                  />
                </div>

                <div>
                  <Label htmlFor="donorPhone" className="mb-2 block">전화번호</Label>
                  <Input
                    id="donorPhone"
                    type="tel"
                    placeholder="사용자 정보에서 가져옴"
                    value={donorPhone}
                    readOnly
                    className="bg-gray-50 cursor-not-allowed border-[#009591]/30"
                    required
                  />
                </div>
              </div>
            </div>
          </div>

          {/* 결제 버튼 */}
          <Button
            onClick={handlePayment}
            disabled={isProcessing || getDonationAmount() < 1000}
            className={cn(
              "w-full text-white text-lg py-6 relative overflow-hidden bg-[#009591] hover:bg-[#007A77]"
            )}
          >
            {/* 빛나는 효과 */}
            {!isProcessing && getDonationAmount() >= 1000 && (
              <div className="absolute inset-y-0 left-0 w-[30%] bg-gradient-to-r from-transparent via-white/20 to-transparent animate-shine-slide pointer-events-none" />
            )}

            {/* 버튼 텍스트 */}
            <span className="relative z-10">
              {isProcessing ? (
                <div className="flex items-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  <span>결제 처리 중...</span>
                </div>
              ) : (
                `${formatCurrency(getDonationAmount())} 기부하기`
              )}
            </span>
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