"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import Confetti from "react-confetti"
import { Card, CardContent } from "@/components/ui/card"
import { Heart } from "lucide-react"

export default function DonationCompletePage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [windowSize, setWindowSize] = useState({ width: 0, height: 0 })
  const [showConfetti, setShowConfetti] = useState(false)

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

    // 컨페티 즉시 시작
    setShowConfetti(true)

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
    return parseInt(value).toLocaleString()
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
      <div className="w-full max-w-xl animate-in fade-in slide-in-from-bottom-4 duration-700">
        <Card className="border-2 border-[#009591]/20 shadow-2xl">
          <CardContent className="p-8 md:p-10 relative">
            {/* 카드 왼쪽 상단 브랜딩 */}
            <div className="absolute top-6 left-6 space-y-0.5">
              <div className="flex items-center space-x-1.5 text-[#009591]">
                <Heart className="h-4 w-4 fill-current" />
                <span className="text-[10px] font-medium tracking-wide">블록체인 기반 기부 플랫폼</span>
              </div>
              <h1 className="text-lg font-bold text-gray-900 tracking-tight">
                HanaChain
              </h1>
            </div>

            {/* 상단 섹션 */}
            <div className="text-center space-y-3 mb-8 mt-14">
              <p className="text-2xl md:text-3xl font-semibold text-[#009591]">
                기부해주셔서 감사합니다
              </p>
            </div>

            {/* 구분선 */}
            <div className="w-full h-px bg-gradient-to-r from-transparent via-[#009591]/30 to-transparent my-8" />

            {/* 중간 섹션 */}
            <div className="flex flex-col items-center space-y-6 my-8">
              {/* 캐릭터 애니메이션 */}
              <div className="relative w-48 h-48 md:w-64 md:h-64">
                <video
                  src="/throwing_logo_1.MOV"
                  autoPlay
                  loop
                  muted
                  playsInline
                  preload="auto"
                  className="w-full h-full object-contain"
                  style={{ background: 'transparent' }}
                />
              </div>

              {/* 캠페인 정보 */}
              <div className="text-center space-y-4 w-full">
                <div className="space-y-2">
                  <h2 className="text-base md:text-lg font-bold text-gray-900 break-keep">
                    {campaignTitle}
                  </h2>
                </div>

                {/* 기부 금액 현판 디자인 */}
                <div className="relative mt-6 px-4">
                  {/* 현판 배경 - 연한 teal */}
                  <div className="relative bg-gradient-to-br from-[#2a7773] via-[#3a8f8a] to-[#2a7773] rounded-xl p-4 shadow-2xl overflow-hidden">
                    {/* 빛 반사 효과 - 상단 */}
                    <div className="absolute top-0 left-0 right-0 h-8 bg-gradient-to-b from-white/30 via-white/10 to-transparent rounded-t-xl" />

                    {/* 빛 반사 효과 - 대각선 애니메이션 (좁은 너비) */}
                    <div className="absolute top-0 left-0 w-full h-full overflow-hidden">
                      <div className="absolute top-0 left-1/100 w-1/4 h-full bg-gradient-to-br from-white/25 via-white/15 to-transparent transform -skew-x-12 blur-sm animate-shine-slide" />
                    </div>

                    {/* 빛나는 테두리 효과 */}
                    <div className="absolute inset-0 rounded-xl border-2 border-white/20" />

                    {/* 금액 텍스트 */}
                    <div className="relative z-10">
                      <p className="text-3xl md:text-4xl lg:text-5xl font-bold text-white drop-shadow-[0_2px_8px_rgba(0,0,0,0.3)] tracking-tight">
                        {formatCurrency(amount)}
                      </p>
                    </div>
                  </div>

                  {/* 외부 그림자 */}
                  <div className="absolute inset-0 -z-10 bg-gradient-to-br from-[#009591]/20 to-[#007773]/20 rounded-xl blur-xl transform translate-y-2" />
                </div>
              </div>
            </div>

            {/* 구분선 */}
            <div className="w-full h-px bg-gradient-to-r from-transparent via-[#009591]/30 to-transparent my-8" />

            {/* 하단 섹션 */}
            <div className="text-center space-y-3">
              <p className="text-gray-600 text-xs md:text-sm leading-relaxed">
                기부 내역은 마이페이지에서 확인 가능합니다.
              </p>

              <p className="text-[10px] md:text-xs text-gray-500">
                모든 기부 내역은 블록체인에 투명하게 기록됩니다.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* 추가 안내 문구 */}
        <p className="text-center text-xs md:text-sm text-gray-500 mt-5">
          여러분의 따뜻한 마음이 세상을 변화시킵니다. 💚
        </p>
      </div>
    </div>
  )
}
