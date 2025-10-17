"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { CheckCircle, Heart } from "lucide-react"
import { CTAButton } from "@/components/ui/cta-button"

export default function CompletePage() {
  const router = useRouter()
  const [nickname, setNickname] = useState("")
  
  useEffect(() => {
    // 저장된 닉네임 읽기
    const savedNickname = sessionStorage.getItem("signupNickname")
    if (savedNickname) {
      setNickname(savedNickname)
    }
    
    // 회원가입 완료 후 세션 데이터 정리
    const cleanupSessionStorage = () => {
      // 2초 후에 정리 (사용자가 페이지를 볼 수 있도록)
      setTimeout(() => {
        sessionStorage.removeItem("signupSessionId")
        sessionStorage.removeItem("signupEmail")
        sessionStorage.removeItem("signupNickname")
      }, 2000)
    }
    
    cleanupSessionStorage()
  }, [])
  
  return (
    <div className="min-h-screen bg-white">
      <div className="container max-w-md mx-auto px-4 py-6">
        
        <div className="mt-16 text-center">
          {/* 완료 아이콘 */}
          <div className="flex justify-center mb-6">
            <div className="relative">
              <CheckCircle className="text-green-500 w-20 h-20" />
              <Heart className="absolute -top-2 -right-2 text-red-500 w-8 h-8 fill-current" />
            </div>
          </div>
          
          {/* 완료 메시지 */}
          <h1 className="text-3xl font-bold text-gray-900 mb-3">
            회원가입 완료!
          </h1>
          <div className="text-gray-600 mb-8">
            <p className="mb-1">
              <span className="font-medium text-teal-500">{nickname || "새로운 기부자"}</span>님,
            </p>
            <p>HanaChain의 회원이 되신 것을 환영합니다!</p>
          </div>
          
          {/* 안내 카드 */}
          <div className="bg-gradient-to-br from-red-50 to-pink-50 border border-red-100 rounded-xl p-6 mb-8 text-left">
            <div className="flex items-start gap-3">
              <div className="flex-shrink-0 w-8 h-8 bg-red-100 rounded-full flex items-center justify-center mt-1">
                <span className="text-red-600 text-sm font-bold">!</span>
              </div>
              <div>
                <h2 className="font-semibold text-gray-900 mb-2">
                  기부 참여를 위한 다음 단계
                </h2>
                <p className="text-gray-600 text-sm leading-relaxed mb-3">
                  실제 기부에 참여하시려면 <span className="font-medium">본인인증</span>이 필요합니다.
                  마이페이지에서 본인인증을 완료하시면 모든 기부 서비스를 이용하실 수 있습니다.
                </p>
                <ul className="text-xs text-gray-500 space-y-1">
                  <li>• 휴대폰 본인인증 또는 아이핀 인증</li>
                  <li>• 기부금 영수증 발급을 위한 필수 절차</li>
                  <li>• 안전한 기부 환경 조성</li>
                </ul>
              </div>
            </div>
          </div>
          
          {/* 액션 버튼들 */}
          <div className="space-y-3">
            <CTAButton 
              onClick={() => router.push("/mypage")}
              fullWidth
            >
              본인인증하러 가기
            </CTAButton>
            
            <button
              className="w-full py-3 text-gray-600 text-sm hover:text-gray-800 transition-colors"
              onClick={() => router.push("/")}
            >
              나중에 하기 (홈으로 이동)
            </button>
          </div>
          
          {/* 추가 정보 */}
          <div className="mt-8 text-xs text-gray-400 leading-relaxed">
            <p>기부 플랫폼 HanaChain과 함께</p>
            <p>더 나은 세상을 만들어가요 ❤️</p>
          </div>
        </div>
      </div>
    </div>
  )
}
