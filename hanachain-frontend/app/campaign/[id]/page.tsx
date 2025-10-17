"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { CircularProgress } from "@/components/ui/circular-progress"
import { Badge } from "@/components/ui/badge"
import { ArrowLeft, Share2, Flag, Users, BarChart3 } from "lucide-react"
import Link from "next/link"
import Footer from "@/components/footer"
import DonationForm from "@/components/donation/DonationForm"
import type { PaymentSuccessData } from "@/types/donation"
import { useState, use, useEffect } from "react"
import CountUp from "react-countup"
import { campaignApi } from "@/lib/api/campaign-api"
import { campaignManagerApi, CampaignManager } from "@/lib/api/campaign-manager-api"
import { CampaignDetailItem, CampaignFundraisingStats } from "@/types/donation"
import { formatCurrency } from "@/lib/utils"
import { HtmlContent } from "@/components/ui/html-content"
import { useAuth } from "@/lib/auth-context"
import { useRouter } from "next/navigation"
import { CommentSection } from "@/components/comments/comment-section"
import { CampaignFundraisingModal } from "@/components/campaigns/campaign-fundraising-modal"
import { BlockchainTransactionList } from "@/components/blockchain/blockchain-transaction-list"

export default function CampaignDetail({ params }: { params: Promise<{ id: string }> }) {
  const [isDonationFormOpen, setIsDonationFormOpen] = useState(false)
  const [campaign, setCampaign] = useState<CampaignDetailItem | null>(null)
  const [relatedCampaigns, setRelatedCampaigns] = useState<CampaignListItem[]>([])
  const [campaignManagers, setCampaignManagers] = useState<CampaignManager[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 모금 정보 모달 상태
  const [isFundraisingModalOpen, setIsFundraisingModalOpen] = useState(false)
  const [isCampaignManager, setIsCampaignManager] = useState(false)
  const [fundraisingStats, setFundraisingStats] = useState<CampaignFundraisingStats | null>(null)
  const [statsLoading, setStatsLoading] = useState(false)
  const [isAnimated, setIsAnimated] = useState(false)

  // 인증 상태와 라우터
  const { isLoggedIn, user, loading: authLoading } = useAuth()
  const router = useRouter()

  // React.use()를 사용하여 params 추출
  const { id } = use(params)
  
  // 백엔드에서 캠페인 상세 정보 및 관련 캠페인 가져오기
  useEffect(() => {
    const fetchCampaignDetail = async () => {
      try {
        setLoading(true)
        setError(null)
        const campaignData = await campaignApi.getCampaignDetail(parseInt(id))
        setCampaign(campaignData)
        
        // 애니메이션을 위한 지연
        setTimeout(() => {
          setIsAnimated(true)
        }, 300)
      } catch (err) {
        console.error('캠페인 상세 정보 로딩 실패:', err)
        setError('캠페인 정보를 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoading(false)
      }
    }

    const fetchRelatedCampaigns = async () => {
      try {
        // 관련 캠페인으로 최근 캠페인 일부를 가져옴 (현재 캠페인 제외)
        const response = await campaignApi.getRecentCampaigns({ page: 0, size: 4 })
        // 현재 캠페인을 제외한 다른 캠페인들만 필터링
        const filtered = response.content.filter(c => c.id !== parseInt(id))
        setRelatedCampaigns(filtered.slice(0, 2)) // 최대 2개만 표시
      } catch (err) {
        console.error('관련 캠페인 로딩 실패:', err)
        // 관련 캠페인 로딩 실패는 치명적이지 않으므로 빈 배열로 설정
        setRelatedCampaigns([])
      }
    }

    const fetchCampaignManagers = async () => {
      try {
        // 활성 캠페인 담당자 목록 가져오기
        const managers = await campaignManagerApi.getActiveCampaignManagers(parseInt(id))
        setCampaignManagers(managers)
      } catch (err) {
        console.error('캠페인 담당자 로딩 실패:', err)
        // 캠페인 담당자 로딩 실패는 치명적이지 않으므로 빈 배열로 설정
        setCampaignManagers([])
      }
    }

    if (id) {
      fetchCampaignDetail()
      fetchRelatedCampaigns()
      fetchCampaignManagers()
    }
  }, [id])

  // 캠페인 담당자 여부 확인
  useEffect(() => {
    const checkManagerStatus = async () => {
      if (!isLoggedIn || !user?.id || !id) {
        setIsCampaignManager(false)
        return
      }

      try {
        const isManager = await campaignManagerApi.checkCampaignManager(
          parseInt(id),
          user.id,
          true // activeOnly
        )
        setIsCampaignManager(isManager)

        if (process.env.NODE_ENV === 'development') {
          console.log('캠페인 담당자 여부:', {
            campaignId: id,
            userId: user.id,
            isManager
          })
        }
      } catch (err) {
        console.error('캠페인 담당자 확인 실패:', err)
        setIsCampaignManager(false)
      }
    }

    checkManagerStatus()
  }, [isLoggedIn, user, id])


  const handlePaymentSuccess = async (data: PaymentSuccessData) => {
    // 서버에서 최신 캠페인 데이터 다시 가져오기
    try {
      const updatedCampaign = await campaignApi.getCampaignDetail(parseInt(id))
      setCampaign(updatedCampaign)
      console.log("Campaign updated after donation:", updatedCampaign)
    } catch (error) {
      console.error("Failed to refresh campaign data:", error)
    }
    
    console.log("Payment success:", data)
  }

  const openDonationForm = () => {
    if (!isLoggedIn) {
      // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
      router.push(`/login?redirect=/campaign/${id}`)
      return
    }
    setIsDonationFormOpen(true)
  }

  const closeDonationForm = () => {
    setIsDonationFormOpen(false)
  }

  // 모금 정보 모달 열기
  const openFundraisingModal = async () => {
    if (!campaign) return

    setIsFundraisingModalOpen(true)
    setStatsLoading(true)

    try {
      // 백엔드 API로부터 실제 모금 통계 조회
      const stats = await campaignApi.getCampaignFundraisingStats(campaign.id)
      setFundraisingStats(stats)
    } catch (error) {
      console.error('모금 통계 조회 실패:', error)
      // 에러 발생 시 현재 캠페인 데이터를 기반으로 기본 통계 생성
      const fallbackStats: CampaignFundraisingStats = {
        currentAmount: campaign.currentAmount,
        targetAmount: campaign.targetAmount,
        progressPercentage: Math.min(100, (campaign.currentAmount / campaign.targetAmount) * 100),
        donorCount: campaign.donorCount,
        daysLeft: Math.max(0, Math.ceil((new Date(campaign.endDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))),
        startDate: campaign.startDate,
        endDate: campaign.endDate,
        averageDonationAmount: campaign.donorCount > 0 ? campaign.currentAmount / campaign.donorCount : 0,
        dailyDonationTrend: generateMockDailyTrend(campaign.currentAmount, campaign.donorCount),
        topDonations: generateMockTopDonations()
      }

      setFundraisingStats(fallbackStats)
    } finally {
      setStatsLoading(false)
    }
  }

  // Mock 데이터 생성 함수들
  const generateMockDailyTrend = (totalAmount: number, totalDonors: number) => {
    const trends = []
    const today = new Date()

    for (let i = 6; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)

      // 간단한 랜덤 분배 (실제로는 백엔드에서 가져와야 함)
      const dailyAmount = Math.floor((totalAmount / 7) * (0.5 + Math.random()))
      const dailyCount = Math.floor((totalDonors / 7) * (0.5 + Math.random()))

      trends.push({
        date: date.toISOString().split('T')[0],
        amount: dailyAmount,
        count: dailyCount
      })
    }

    return trends
  }

  const generateMockTopDonations = () => {
    // 실제로는 백엔드에서 가져와야 함
    return [
      { donorName: '김**', amount: 500000, donatedAt: new Date().toISOString(), anonymous: false },
      { donorName: '익명', amount: 300000, donatedAt: new Date().toISOString(), anonymous: true },
      { donorName: '이**', amount: 200000, donatedAt: new Date().toISOString(), anonymous: false },
      { donorName: '박**', amount: 150000, donatedAt: new Date().toISOString(), anonymous: false },
      { donorName: '익명', amount: 100000, donatedAt: new Date().toISOString(), anonymous: true },
    ]
  }

  // 로딩 중일 때
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-[#009591] mx-auto mb-4"></div>
          <p className="text-gray-600">캠페인 정보를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  // 에러가 발생했을 때
  if (error || !campaign) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error || '캠페인을 찾을 수 없습니다.'}</p>
          <Link href="/">
            <Button variant="outline">
              <ArrowLeft className="h-4 w-4 mr-2" />
              메인으로 돌아가기
            </Button>
          </Link>
        </div>
      </div>
    )
  }

  // 서버에서 받은 실제 현재 금액 사용
  const currentAmount = campaign.currentAmount
  const progressPercentage = Math.min(100, (currentAmount / campaign.targetAmount) * 100)
  
  // 종료일까지 남은 날짜 계산
  const daysLeft = Math.max(0, Math.ceil((new Date(campaign.endDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24)))
  
  // 캠페인 기부 가능 상태 확인
  const now = new Date()
  const startDate = new Date(campaign.startDate)
  const endDate = new Date(campaign.endDate)
  const isInPeriod = now >= startDate && now <= endDate
  const isStatusActive = campaign.status === 'ACTIVE'
  const isCampaignActive = campaign.isActive || (isInPeriod && isStatusActive)
  
  // 최종 기부 가능 여부 (캠페인 활성 + 로그인 상태)
  const canDonate = isCampaignActive && isLoggedIn
  
  // 기부 불가능한 이유 파악
  const getDisabledReason = () => {
    if (!isLoggedIn) return '로그인이 필요합니다'
    if (!isStatusActive) return '진행 중이 아닌 캠페인'
    if (now < startDate) return '아직 시작되지 않은 캠페인'
    if (now > endDate) return '종료된 캠페인'
    if (!campaign.isActive) return '비활성화된 캠페인'
    return ''
  }
  
  // 버튼 텍스트 결정
  const getButtonText = () => {
    if (!isLoggedIn) return '로그인하고 기부하기'
    if (canDonate) return '지금 기부하기'
    return getDisabledReason()
  }
  
  // 디버그 정보 (개발 환경에서만 출력)
  if (process.env.NODE_ENV === 'development') {
    console.log('Campaign Debug Info:', {
      id: campaign.id,
      title: campaign.title,
      status: campaign.status,
      isActive: campaign.isActive,
      isCampaignActive,
      startDate: campaign.startDate,
      endDate: campaign.endDate,
      now: now.toISOString(),
      isInPeriod,
      isStatusActive,
      isLoggedIn,
      user: user?.email || 'not logged in',
      canDonate,
      daysLeft,
      disabledReason: getDisabledReason(),
      buttonText: getButtonText()
    })
  }

  // 카테고리 번역
  const categoryMap = {
    'MEDICAL': '의료',
    'EDUCATION': '교육',
    'DISASTER_RELIEF': '재해구호',
    'ENVIRONMENT': '환경',
    'ANIMAL_WELFARE': '동물보호',
    'COMMUNITY': '지역사회',
    'EMERGENCY': '긴급',
    'OTHER': '기타'
  } as const

  const translatedCategory = categoryMap[campaign.category as keyof typeof categoryMap] || campaign.category

  return (
    <div className="min-h-screen bg-gray-50">
      <div>
        {/* Hero Section */}
        <div className="relative">
          <img
            src={campaign.imageUrl || "/placeholder.svg"}
            alt={campaign.title}
            className="w-full h-96 object-cover"
          />
          <div className="absolute inset-0 bg-black/40" />
          
          {/* 컨텐츠 컨테이너를 max-w-7xl로 중앙 정렬 */}
          <div className="absolute inset-0 flex items-end">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full pb-8">
              <Badge className="bg-[#009591] text-white mb-4">{translatedCategory}</Badge>
              <h1 className="text-3xl font-bold mb-2 text-white">{campaign.title}</h1>
            </div>
          </div>
          
          {/* 돌아가기 버튼도 max-w-7xl 컨테이너 안에 */}
          <div className="absolute top-0 left-0 right-0">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
              <Link href="/">
                <Button variant="ghost" size="sm" className="text-white hover:bg-white/20">
                  <ArrowLeft className="h-5 w-5 mr-2" />
                  돌아가기
                </Button>
              </Link>
            </div>
          </div>
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Main Content */}
            <div className="lg:col-span-2 space-y-8">
              {/* Campaign Story - 백엔드에서 스토리 데이터가 없으므로 기본 설명 표시 */}
              <Card>
                <CardContent className="p-6">
                  <h3 className="text-xl font-semibold mb-4">캠페인 소개</h3>
                  <img
                    src={campaign.imageUrl || "/placeholder.svg"}
                    alt={campaign.title}
                    className="w-full h-64 object-cover rounded-lg mb-4"
                  />
                  <HtmlContent
                    html={campaign.htmlDescription || campaign.description}
                    className="text-gray-700"
                  />
                </CardContent>
              </Card>

              {/* 블록체인 트랜잭션 섹션 */}
              {campaign.blockchainStatus === 'ACTIVE' && (
                <BlockchainTransactionList
                  campaignId={campaign.id}
                  blockchainCampaignId={campaign.blockchainCampaignId}
                  contractAddress={campaign.beneficiaryAddress}
                  autoRefresh={true}
                  refreshInterval={30000}
                />
              )}

              {/* 댓글 섹션 */}
              <CommentSection
                campaignId={campaign.id}
                campaignManagers={campaignManagers}
              />
            </div>

            {/* Sidebar */}
            <div className="space-y-6">
              {/* Donation Card */}
              <Card className="sticky top-24">
                <CardContent className="px-6 py-0">
                  <div className="relative flex flex-col items-center mb-6">
                    {/* D-N 표시 (오른쪽 상단) */}
                    <div className="absolute top-0 right-0 bg-teal-500 text-white text-sm font-bold px-3 py-1 rounded-full z-10">
                      D-{daysLeft}
                    </div>
                    
                    <CircularProgress value={isAnimated ? progressPercentage : 0} size={200}>
                      <div className="text-center">
                        <div className="text-sm text-gray-600 mb-1">달성률</div>
                        <div className="text-3xl font-bold text-[#009591] font-sans">
                          {Math.round(progressPercentage)}%
                        </div>
                      </div>
                    </CircularProgress>
                    
                    {/* 금액 정보 (이미지와 유사한 스타일) */}
                    <div className="mt-6 w-full">
                      <div className="text-center">
                        <div className="text-4xl font-sans font-bold">
                          <span className="inline-block bg-[linear-gradient(135deg,_#41aea5_0%,_#328780_25%,_#1a6b63_50%,_#2a8981_75%,_#1a6b63_100%)] bg-clip-text text-transparent animate-shine bg-[length:300%_300%]">
                            <CountUp
                              end={currentAmount}
                              duration={5}
                              separator=","
                              suffix="원"
                              start={0}
                            />
                          </span>
                        </div>
                        <div className="text-lg text-gray-500 mt-1 font-sans font-semibold">
                          {campaign.targetAmount.toLocaleString()}원 <span className="font-sans">목표</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <Button 
                    onClick={openDonationForm}
                    className={`w-full text-white text-lg py-3 mb-4 ${
                      !isLoggedIn 
                        ? 'bg-blue-600 hover:bg-blue-700' 
                        : canDonate 
                          ? 'bg-[#009591] hover:bg-[#007A77]' 
                          : 'bg-gray-400 cursor-not-allowed'
                    }`}
                    disabled={!isCampaignActive}
                  >
                    {getButtonText()}
                  </Button>

                  {/* 캠페인 담당자 전용 모금 정보 버튼 */}
                  {isCampaignManager && (
                    <Button
                      onClick={openFundraisingModal}
                      variant="outline"
                      className="w-full border-[#009591] text-[#009591] hover:bg-[#009591] hover:text-white"
                    >
                      <BarChart3 className="h-4 w-4 mr-2" />
                      캠페인 대시보드
                    </Button>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>

          {/* Recommended Campaigns Section */}
          <div className="mt-12">
            <Card>
              <CardContent className="p-6">
                <h3 className="text-xl font-semibold mb-6">추천하는 기부캠페인</h3>
                {relatedCampaigns.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {relatedCampaigns.map((related) => {
                      const relatedProgressPercentage = Math.min(100, (related.currentAmount / related.targetAmount) * 100)
                      const relatedCategoryTranslated = categoryMap[related.category as keyof typeof categoryMap] || related.category
                      
                      return (
                        <Link key={related.id} href={`/campaign/${related.id}`}>
                          <div className="flex space-x-4 p-4 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer border border-gray-100">
                            <img
                              src={related.imageUrl || "/placeholder.svg"}
                              alt={related.title}
                              className="w-24 h-24 object-cover rounded"
                            />
                            <div className="flex-1">
                              <p className="font-medium line-clamp-2 mb-2">{related.title}</p>
                              <p className="text-sm text-gray-600 mb-2">{related.organizer || related.creatorName}</p>
                              <Badge variant="outline" className="mb-3 text-xs">
                                {relatedCategoryTranslated}
                              </Badge>
                              <div className="flex items-center gap-2">
                                <Progress
                                  value={relatedProgressPercentage}
                                  className="h-2 w-[80%] [&>div]:bg-teal-600"
                                />
                                <span className="text-xs text-gray-500 font-medium whitespace-nowrap">
                                  {relatedProgressPercentage.toFixed(1)}%
                                </span>
                              </div>
                            </div>
                          </div>
                        </Link>
                      )
                    })}
                  </div>
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    <p>관련 캠페인을 불러오는 중...</p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* Footer */}
      <Footer />

      {/* Donation Form Modal */}
      <DonationForm
        isOpen={isDonationFormOpen}
        onClose={closeDonationForm}
        campaignId={campaign.id.toString()}
        campaignTitle={campaign.title}
        onPaymentSuccess={handlePaymentSuccess}
      />

      {/* Fundraising Info Modal (담당자 전용) */}
      <CampaignFundraisingModal
        isOpen={isFundraisingModalOpen}
        onClose={() => setIsFundraisingModalOpen(false)}
        campaignTitle={campaign.title}
        stats={fundraisingStats}
        loading={statsLoading}
      />
    </div>
  )
}
