"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import Image from "next/image"
import Confetti from "react-confetti"
import { Card, CardContent } from "@/components/ui/card"
import { Heart } from "lucide-react"

export default function DonationCompletePage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [windowSize, setWindowSize] = useState({ width: 0, height: 0 })
  const [showConfetti, setShowConfetti] = useState(true)

  // URL에서 기부 정보 가져오기
  const campaignTitle = searchParams.get("campaignTitle") || ""
  const amount = searchParams.get("amount") || "0"
  const campaignId = searchParams.get("campaignId") || ""

  // 윈도우 크기 감지 (Confetti용)
  useEffect(() => {
    const handleResize = () => {
      setWindowSize({
        width: window.innerWidth,
        height: window.innerHeight,
      })
    }

    // 초기 크기 설정
    handleResize()

    // 리사이즈 이벤트 리스너
    window.addEventListener("resize", handleResize)

    return () => {
      window.removeEventListener("resize", handleResize)
    }
  }, [])

  // Confetti 완료 핸들러
  const handleConfettiComplete = () => {
    setShowConfetti(false)
  }

  // 잘못된 접근 처리 (필수 파라미터 누락 시)
  useEffect(() => {
    if (!campaignTitle || !amount || amount === "0") {
      console.error("기부 완료 페이지 접근 오류: 필수 파라미터 누락")
      router.push("/")
    }
  }, [campaignTitle, amount, router])

  const formatCurrency = (value: string) => {
    return parseInt(value).toLocaleString() + "원"
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#009591]/5 via-white to-[#009591]/10 flex items-center justify-center p-4">
      {/* Confetti 효과 */}
      {showConfetti && (
        <Confetti
          width={windowSize.width}
          height={windowSize.height}
          recycle={false}
          numberOfPieces={300}
          gravity={0.1}
          colors={["#009591", "#00B8B3", "#FFD700", "#FF6B9D", "#C3B1E1"]}
          onConfettiComplete={handleConfettiComplete}
        />
      )}

      {/* 메인 컨텐츠 */}
      <div className="w-full max-w-2xl animate-in fade-in slide-in-from-bottom-4 duration-700">
        <Card className="border-2 border-[#009591]/20 shadow-2xl">
          <CardContent className="p-8 md:p-12">
            {/* 상단 섹션 */}
            <div className="text-center space-y-4 mb-8">
              <div className="inline-flex items-center justify-center space-x-2 text-[#009591] mb-2">
                <Heart className="h-6 w-6 fill-current" />
                <span className="text-sm font-medium tracking-wide">블록체인 기반 기부 플랫폼</span>
              </div>

              <h1 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
                하나체인
              </h1>

              <p className="text-xl md:text-2xl font-semibold text-[#009591] mt-4">
                믿고 기부해주셔서 감사합니다.
              </p>
            </div>

            {/* 구분선 */}
            <div className="w-full h-px bg-gradient-to-r from-transparent via-[#009591]/30 to-transparent my-8" />

            {/* 중간 섹션 */}
            <div className="flex flex-col items-center space-y-6 my-10">
              {/* 하트 이미지 */}
              <div className="relative w-32 h-32 md:w-40 md:h-40 animate-in zoom-in duration-500 delay-300">
                <Image
                  src="/byul_baby_heart.png"
                  alt="기부 완료"
                  fill
                  className="object-contain drop-shadow-lg"
                  priority
                />
              </div>

              {/* 캠페인 정보 */}
              <div className="text-center space-y-4 w-full">
                <div className="space-y-2">
                  <p className="text-sm text-gray-500 font-medium">기부한 캠페인</p>
                  <h2 className="text-lg md:text-xl font-bold text-gray-900 break-keep">
                    {campaignTitle}
                  </h2>
                </div>

                {/* 기부 금액 강조 */}
                <Card className="bg-[#009591]/5 border-[#009591]/20 mt-6">
                  <CardContent className="p-6">
                    <div className="flex items-center justify-center space-x-3">
                      <Heart className="h-6 w-6 text-[#009591] fill-current animate-pulse" />
                      <div className="text-center">
                        <p className="text-sm text-gray-600 mb-1">기부 금액</p>
                        <p className="text-3xl md:text-4xl font-bold text-[#009591]">
                          {formatCurrency(amount)}
                        </p>
                      </div>
                      <Heart className="h-6 w-6 text-[#009591] fill-current animate-pulse" />
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>

            {/* 구분선 */}
            <div className="w-full h-px bg-gradient-to-r from-transparent via-[#009591]/30 to-transparent my-8" />

            {/* 하단 섹션 */}
            <div className="text-center space-y-4">
              <p className="text-gray-600 text-sm md:text-base leading-relaxed">
                기부 내역은 마이페이지에서 확인 가능합니다.
              </p>

              <p className="text-xs text-gray-500 mt-4">
                모든 기부 내역은 블록체인에 투명하게 기록됩니다.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* 추가 안내 문구 */}
        <p className="text-center text-sm text-gray-500 mt-6">
          여러분의 따뜻한 마음이 세상을 변화시킵니다. 💚
        </p>
      </div>
    </div>
  )
}
