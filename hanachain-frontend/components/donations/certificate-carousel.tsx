"use client"

import { DonationCertificate } from "@/types/donation"
import { DonationCertificateComponent } from "./donation-certificate"
import { CertificateQRInfo } from "./certificate-qr-info"
import {
  Carousel,
  CarouselContent,
  CarouselItem,
} from "@/components/ui/carousel"
import { useState, useEffect } from "react"
import type { CarouselApi } from "@/components/ui/carousel"

interface CertificateCarouselProps {
  certificate: DonationCertificate
}

/**
 * 기부 증서 캐러셀 컴포넌트
 * 슬라이드 1: 기부 증서
 * 슬라이드 2: QR 코드 정보
 */
export function CertificateCarousel({ certificate }: CertificateCarouselProps) {
  const [api, setApi] = useState<CarouselApi>()
  const [current, setCurrent] = useState(0)

  useEffect(() => {
    if (!api) return

    setCurrent(api.selectedScrollSnap())

    api.on("select", () => {
      setCurrent(api.selectedScrollSnap())
    })
  }, [api])

  return (
    <div className="relative w-full mx-auto" style={{ maxWidth: '450px' }}>
      <Carousel
        setApi={setApi}
        opts={{
          align: "center",
          loop: false,
        }}
        className="w-full"
      >
        <CarouselContent>
          {/* 슬라이드 1: 기부 증서 */}
          <CarouselItem>
            <DonationCertificateComponent certificate={certificate} />
          </CarouselItem>

          {/* 슬라이드 2: QR 코드 정보 */}
          <CarouselItem>
            <CertificateQRInfo certificate={certificate} />
          </CarouselItem>
        </CarouselContent>
      </Carousel>

      {/* 점 표시기 */}
      <div className="flex justify-center gap-2 mt-4">
        <button
          onClick={() => api?.scrollTo(0)}
          className={`w-2 h-2 rounded-full transition-all ${
            current === 0 ? "bg-[#009591] w-6" : "bg-gray-300"
          }`}
          aria-label="증서 보기"
        />
        <button
          onClick={() => api?.scrollTo(1)}
          className={`w-2 h-2 rounded-full transition-all ${
            current === 1 ? "bg-[#009591] w-6" : "bg-gray-300"
          }`}
          aria-label="QR 코드 보기"
        />
      </div>

      {/* 슬라이드 인디케이터 안내 */}
      <div className="text-center mt-2 text-sm text-gray-600">
        <p>하단 버튼을 눌러 QR 코드 정보를 확인하세요</p>
      </div>
    </div>
  )
}
