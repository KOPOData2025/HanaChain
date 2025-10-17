"use client"

import { useState, useEffect, useCallback } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { Download, Award, RefreshCw } from "lucide-react"
import { DonationCertificate } from "@/types/donation"
import { getDonationCertificate } from "@/lib/api/donation-api"
import { CertificateCarousel } from "./certificate-carousel"
import domtoimage from "dom-to-image-more"
import Confetti from "react-confetti"

interface DonationCertificateModalProps {
  donationId: string | null
  isOpen: boolean
  onClose: () => void
}

/**
 * 기부 증서 모달
 * 증서 표시 및 이미지 다운로드 기능
 */
export function DonationCertificateModal({ donationId, isOpen, onClose }: DonationCertificateModalProps) {
  const [certificate, setCertificate] = useState<DonationCertificate | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isDownloading, setIsDownloading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showConfetti, setShowConfetti] = useState(false)
  const [windowSize, setWindowSize] = useState({ width: 0, height: 0 })
  const [modalCenter, setModalCenter] = useState({ x: 0, y: 0 })
  const [confettiTrigger, setConfettiTrigger] = useState(0)

  // 윈도우 사이즈 추적
  useEffect(() => {
    const updateSize = () => {
      setWindowSize({
        width: window.innerWidth,
        height: window.innerHeight
      })
    }
    
    updateSize()
    window.addEventListener('resize', updateSize)
    return () => window.removeEventListener('resize', updateSize)
  }, [])

  // 증서 데이터 로드
  const loadCertificate = useCallback(async () => {
    if (!donationId) return

    setIsLoading(true)
    setError(null)

    try {
      console.log('🎫 기부 증서 로딩 시작:', { donationId })
      const data = await getDonationCertificate(donationId)
      console.log('✅ 기부 증서 로딩 성공:', data)
      setCertificate(data)
    } catch (err) {
      console.error('❌ 기부 증서 로딩 실패:', err)
      setError(err instanceof Error ? err.message : '기부 증서를 불러올 수 없습니다')
    } finally {
      setIsLoading(false)
    }
  }, [donationId])

  // 모달이 열릴 때 증서 로드 및 confetti 트리거
  useEffect(() => {
    if (donationId && isOpen) {
      loadCertificate()
      // 모달이 열릴 때마다 confetti 트리거 증가
      setConfettiTrigger(prev => prev + 1)
    }
  }, [donationId, isOpen, loadCertificate])

  // confetti 트리거 변경 시 실행
  useEffect(() => {
    if (confettiTrigger > 0 && isOpen) {
      setTimeout(() => {
        setModalCenter({
          x: window.innerWidth / 2,
          y: window.innerHeight / 2
        })
        setShowConfetti(true)
        console.log('🎆 Confetti 시작!', { x: window.innerWidth / 2, y: window.innerHeight / 2 })
        
        // 3초 후에 confetti 효과 종료
        setTimeout(() => {
          setShowConfetti(false)
        }, 6000)
      }, 300)
    }
  }, [confettiTrigger, isOpen])

  // 모달이 닫힐 때 상태 초기화
  useEffect(() => {
    if (!isOpen) {
      setCertificate(null)
      setError(null)
      setShowConfetti(false)
    }
  }, [isOpen])

  // 이미지를 base64로 변환하는 헬퍼 함수
  const convertImageToBase64 = async (imgUrl: string): Promise<string> => {
    try {
      const response = await fetch(imgUrl, {
        mode: 'cors',
        credentials: 'omit'
      })
      const blob = await response.blob()
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onloadend = () => resolve(reader.result as string)
        reader.onerror = reject
        reader.readAsDataURL(blob)
      })
    } catch (error) {
      console.error('이미지 변환 실패:', error)
      throw error
    }
  }

  // 증서 다운로드 (html2canvas 사용)
  const handleDownload = async () => {
    if (!certificate) return

    setIsDownloading(true)

    try {
      console.log('📥 증서 다운로드 시작')

      // 증서 DOM 요소 찾기
      const certificateElement = document.getElementById('donation-certificate')
      if (!certificateElement) {
        throw new Error('증서 요소를 찾을 수 없습니다.')
      }

      // 캠페인 이미지를 base64로 변환
      if (certificate.campaignImage) {
        const imgElement = certificateElement.querySelector('img[alt="' + certificate.campaignTitle + '"]') as HTMLImageElement
        if (imgElement) {
          try {
            const base64Image = await convertImageToBase64(certificate.campaignImage)
            imgElement.src = base64Image
            console.log('✅ 이미지를 base64로 변환 완료')
            // 이미지 로드 대기
            await new Promise(resolve => setTimeout(resolve, 100))
          } catch (error) {
            console.warn('⚠️ 이미지 변환 실패, 원본 URL 사용:', error)
          }
        }
      }

      // dom-to-image-more로 이미지 생성 (oklch 색상 문제 해결)
      // scrollHeight를 사용하여 잘림 없이 전체 영역 캡처
      const scale = 2
      const width = certificateElement.scrollWidth
      const height = certificateElement.scrollHeight

      console.log('📐 증서 크기:', {
        offsetWidth: certificateElement.offsetWidth,
        offsetHeight: certificateElement.offsetHeight,
        scrollWidth: width,
        scrollHeight: height
      })

      // 모든 자식 요소에 border: none 적용
      const allElements = certificateElement.querySelectorAll('*')
      const originalBorders = new Map<Element, string>()

      allElements.forEach((el) => {
        const htmlEl = el as HTMLElement
        originalBorders.set(el, htmlEl.style.border)
        if (!htmlEl.style.border || htmlEl.style.border === '' || !htmlEl.style.border.includes('solid #009591')) {
          htmlEl.style.border = 'none'
        }
      })

      const blob = await domtoimage.toBlob(certificateElement, {
        quality: 1.0,
        width: width * scale,
        height: height * scale,
        cacheBust: true,
        imagePlaceholder: undefined,
        style: {
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
          width: `${width}px`,
          height: `${height}px`,
          margin: '0',
          padding: certificateElement.style.padding
        },
        filter: (node: HTMLElement) => {
          // QR 코드와 모든 요소 포함
          return true
        }
      })

      // 원래 border 복원
      allElements.forEach((el) => {
        const htmlEl = el as HTMLElement
        const originalBorder = originalBorders.get(el)
        if (originalBorder !== undefined) {
          htmlEl.style.border = originalBorder
        }
      })

      if (!blob) {
        throw new Error('이미지 생성에 실패했습니다.')
      }

      // 다운로드 링크 생성
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      const filename = `HanaChain_기부증서_${new Date(certificate.donatedAt).toISOString().split('T')[0]}.png`

      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()

      // 정리
      document.body.removeChild(link)
      URL.revokeObjectURL(url)

      console.log('✅ 증서 다운로드 성공:', filename)
    } catch (err) {
      console.error('❌ 증서 다운로드 실패:', err)
      setError(err instanceof Error ? err.message : '증서 다운로드에 실패했습니다.')
    } finally {
      setIsDownloading(false)
    }
  }

  if (!donationId) return null

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-[600px] max-h-[90vh] overflow-y-auto overflow-x-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <Award className="h-5 w-5 text-[#009591]" />
            <span>기부 증서</span>
          </DialogTitle>
        </DialogHeader>

        {/* 폭죽 Confetti 효과 */}
        {showConfetti && windowSize.width > 0 && modalCenter.x > 0 && (
          <Confetti
            width={windowSize.width}
            height={modalCenter.y + 100}
            recycle={false}
            numberOfPieces={400}
            gravity={0.3}
            initialVelocityX={15}
            initialVelocityY={30}
            drag={0.2}
            confettiSource={{
              x: modalCenter.x,
              y: modalCenter.y,
              w: 8,
              h: 8
            }}
            tweenDuration={2000}
            style={{
              position: 'fixed',
              top: 10,
              left: -30,
              zIndex: 9999,
              pointerEvents: 'none',
              overflow: 'hidden'
            }}
          />
        )}

        {/* 로딩 상태 */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <LoadingSpinner className="h-8 w-8" />
            <span className="ml-2 text-gray-600">기부 증서를 불러오는 중...</span>
          </div>
        )}

        {/* 에러 상태 */}
        {error && !isLoading && (
          <div className="space-y-4">
            <ErrorMessage message={error} />
            <div className="flex justify-center">
              <Button onClick={loadCertificate} variant="outline">
                <RefreshCw className="h-4 w-4 mr-2" />
                다시 시도
              </Button>
            </div>
          </div>
        )}

        {/* 증서 캐러셀 */}
        {certificate && !isLoading && !error && (
          <div className="space-y-6">
            {/* 증서 캐러셀 컴포넌트 */}
            <CertificateCarousel certificate={certificate} />

            {/* 다운로드 버튼 */}
            <div className="flex justify-center pt-4 border-t border-gray-200">
              <Button
                onClick={handleDownload}
                disabled={isDownloading}
                className="bg-[#009591] hover:bg-[#007a77] px-8"
              >
                {isDownloading ? (
                  <>
                    <LoadingSpinner className="h-4 w-4 mr-2" />
                    다운로드 중...
                  </>
                ) : (
                  <>
                    <Download className="h-4 w-4 mr-2" />
                    증서 이미지 다운로드
                  </>
                )}
              </Button>
            </div>

            {/* 안내 메시지 */}
            <div className="text-center text-sm text-gray-600 bg-blue-50 p-4 rounded-lg">
              <p>
                증서 이미지만 다운로드됩니다.
                <br />
                QR 코드 정보는 하단 버튼으로 확인하세요.
              </p>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
