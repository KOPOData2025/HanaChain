"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ProfileImageUpload } from "@/components/ui/profile-image-upload"
import { ErrorMessage } from "@/components/ui/error-message"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { DonationList } from "@/components/donations/donation-list"
import { NotificationSettingsComponent } from "@/components/settings/notification-settings"
import { PrivacySettingsComponent } from "@/components/settings/privacy-settings"
import { AccountSettingsComponent } from "@/components/settings/account-settings"
import { Notices } from "@/components/support/notices"
import { FAQComponent } from "@/components/support/faq"
import { InquiryComponent } from "@/components/support/inquiry"
import { DonationStatistics } from "@/components/statistics/donation-statistics"
import { CampaignManagement } from "@/components/campaigns/campaign-management"
import { 
  User, 
  Edit3, 
  Heart, 
  BarChart3,
  Settings,
  HelpCircle,
  CheckCircle,
  Bell,
  MessageSquare,
  FileText,
  Shield
} from "lucide-react"
import { useAuth } from "@/lib/auth-context"
import { useProfile } from "@/hooks/use-profile"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect } from "react"

export default function MyPage() {
  const { user, isLoggedIn, loading } = useAuth()
  const { 
    profile, 
    dashboard, 
    updateProfile, 
    uploadImage,
    deleteProfileImage,
    fetchDashboard,
    isLoading, 
    error, 
    clearError 
  } = useProfile()
  const router = useRouter()
  const [isEditing, setIsEditing] = useState(false)
  const [profileData, setProfileData] = useState({
    nickname: profile?.nickname || user?.nickname || "",
    email: profile?.email || user?.email || "",
  })
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [activeTab, setActiveTab] = useState<'profile' | 'donations' | 'statistics' | 'favorites' | 'settings' | 'support' | 'support-notices' | 'support-faq' | 'support-inquiry'>('profile')

  // 인증되지 않은 사용자는 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!loading && !isLoggedIn) {
      router.push('/login')
    }
  }, [loading, isLoggedIn, router])

  // 프로필 데이터가 변경되면 로컬 상태 업데이트
  useEffect(() => {
    if (profile) {
      setProfileData({
        nickname: profile.nickname || "",
        email: profile.email || "",
      })
    }
  }, [profile])

  // 로그인된 사용자의 대시보드 데이터 가져오기 (마운트 시 1회만 실행)
  useEffect(() => {
    if (isLoggedIn) {
      console.log('🏠 마이페이지 컴포넌트에서 대시보드 데이터 요청')
      fetchDashboard()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoggedIn])

  // 대시보드 데이터가 업데이트될 때마다 로그 출력
  useEffect(() => {
    if (dashboard) {
      console.log('🏆 마이페이지 컴포넌트에서 대시보드 데이터 수신 완료:', {
        profileData: dashboard.profile,
        donationStats: dashboard.donationStats,
        recentDonations: dashboard.recentDonations,
        favoriteCampaignsCount: dashboard.favoriteCampaignsCount,
        totalData: dashboard
      })
      console.log('📊 렌더링될 데이터 상세:', {
        totalAmount: dashboard?.donationStats?.totalAmount,
        totalCount: dashboard?.donationStats?.totalCount,
        favoriteCampaignsCount: dashboard?.favoriteCampaignsCount,
        recentDonationsLength: dashboard?.recentDonations?.length,
        recentDonationsData: dashboard?.recentDonations
      })
    } else {
      console.log('⚠️ 대시보드 데이터가 아직 없습니다 (dashboard is null/undefined)')
    }
  }, [dashboard])

  // 프로필 데이터가 업데이트될 때마다 로그 출력
  useEffect(() => {
    if (profile) {
      console.log('👤 마이페이지 컴포넌트에서 프로필 데이터 수신 완료:', profile)
    }
  }, [profile])

  const handleSaveProfile = async () => {
    setIsSubmitting(true)
    clearError()

    try {
      // 닉네임 업데이트
      if (profileData.nickname !== profile?.nickname) {
        await updateProfile({
          nickname: profileData.nickname,
        })
      }

      // 이미지 업로드 (별도 처리)
      if (selectedImage) {
        await uploadImage(selectedImage)
      }

      setIsEditing(false)
      setSelectedImage(null)
    } catch (err) {
      // 에러는 useProfile 훅에서 처리됨
      console.error('Profile save error:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    setSelectedImage(null)
    setProfileData({
      nickname: profile?.nickname || user?.nickname || "",
      email: profile?.email || user?.email || "",
    })
    clearError()
  }

  // 로딩 중이거나 인증되지 않은 경우 처리
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <LoadingSpinner className="mx-auto mb-4" />
          <p className="text-gray-600">로딩 중...</p>
        </div>
      </div>
    )
  }

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Card className="w-full max-w-md">
          <CardContent className="p-8 text-center">
            <div className="mb-4">
              <User className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h2 className="text-xl font-semibold text-gray-900 mb-2">
                로그인이 필요합니다
              </h2>
              <p className="text-gray-600 mb-6">
                마이페이지를 이용하려면 먼저 로그인해주세요.
              </p>
            </div>
            <div className="space-y-3">
              <Link href="/login">
                <Button className="w-full bg-[#009591] hover:bg-[#007a77]">
                  로그인하러 가기
                </Button>
              </Link>
              <Link href="/signup">
                <Button variant="outline" className="w-full">
                  회원가입
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* 헤더 섹션 */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">마이페이지</h1>
          <p className="text-gray-600">나의 기부 활동을 확인하고 관리하세요</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* 사이드바 */}
          <div className="lg:col-span-1">
            <Card className="sticky top-6">
              <CardContent className="p-6 pt-0">
                <nav className="space-y-1">

                  <h3 className="text-lg font-semibold text-gray-900 mb-4">설정</h3>
                  
                  <Button 
                  variant="ghost" 
                  className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                    activeTab === 'profile' ? 'bg-[#009591]/10 text-[#009591]' : ''
                  }`}
                  onClick={() => setActiveTab('profile')}
                >
                  <Shield className="mr-3 h-4 w-4" />
                  <div className="text-left">
                    <div className="text-sm font-medium">계정 정보</div>
                  </div>
                </Button>
                  <Button 
                  variant="ghost" 
                  className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                    activeTab === 'settings' ? 'bg-[#009591]/10 text-[#009591]' : ''
                  }`}
                  onClick={() => setActiveTab('settings')}
                >
                  <Bell className="mr-3 h-4 w-4" />
                  <div className="text-left">
                    <div className="text-sm font-medium">알림 및 개인정보</div>
                  </div>
                </Button>
                
              <div className="border-t border-gray-200 pt-4 mt-4">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">나의 기부</h3>
                  
                  <Button 
                    variant="ghost" 
                    className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                      activeTab === 'donations' ? 'bg-[#009591]/10 text-[#009591]' : ''
                    }`}
                    onClick={() => setActiveTab('donations')}
                  >
                    <User className="mr-3 h-4 w-4" />
                    <div className="text-left">
                      <div className="text-sm font-medium">기부내역</div>
                    </div>
                  </Button>

                  <Button 
                    variant="ghost" 
                    className="w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50"
                  >
                    <Heart className="mr-3 h-4 w-4" />
                    <div className="text-left">
                      <div className="text-sm font-medium">찜한 기부</div>
                    </div>
                  </Button>
                </div>
                  <Button 
                      variant="ghost" 
                      className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                        activeTab === 'statistics' ? 'bg-[#009591]/10 text-[#009591]' : ''
                      }`}
                      onClick={() => setActiveTab('statistics')}
                    >
                      <BarChart3 className="mr-3 h-4 w-4" />
                      <div className="text-left">
                        <div className="text-sm font-medium">기부통계</div>
                      </div>
                    </Button>

                  <div className="border-t border-gray-200 pt-4 mt-4">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">고객센터</h3>
                    
                    <Button 
                      variant="ghost" 
                      className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                        activeTab === 'support-notices' ? 'bg-[#009591]/10 text-[#009591]' : ''
                      }`}
                      onClick={() => setActiveTab('support-notices')}
                    >
                      <FileText className="mr-3 h-4 w-4" />
                      <div className="text-left">
                        <div className="text-sm font-medium">공지사항</div>
                      </div>
                    </Button>

                    <Button 
                      variant="ghost" 
                      className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                        activeTab === 'support-faq' ? 'bg-[#009591]/10 text-[#009591]' : ''
                      }`}
                      onClick={() => setActiveTab('support-faq')}
                    >
                      <HelpCircle className="mr-3 h-4 w-4" />
                      <div className="text-left">
                        <div className="text-sm font-medium">자주 묻는 질문</div>
                      </div>
                    </Button>

                    <Button 
                      variant="ghost" 
                      className={`w-full justify-start text-left font-normal h-auto p-3 hover:bg-gray-50 ${
                        activeTab === 'support-inquiry' ? 'bg-[#009591]/10 text-[#009591]' : ''
                      }`}
                      onClick={() => setActiveTab('support-inquiry')}
                    >
                      <MessageSquare className="mr-3 h-4 w-4" />
                      <div className="text-left">
                        <div className="text-sm font-medium">1:1 문의하기</div>
                      </div>
                    </Button>
                  </div>

                </nav>
              </CardContent>
            </Card>
          </div>

          {/* 메인 콘텐츠 */}
          <div className="lg:col-span-3 space-y-6">
            {/* 프로필 정보 카드 */}
            {activeTab === 'profile' && (
              <Card>
              <CardHeader className="pb-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">프로필 정보</h2>
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => isEditing ? handleCancelEdit() : setIsEditing(true)}
                    className="flex items-center gap-2"
                    disabled={isSubmitting}
                  >
                    <Edit3 className="h-4 w-4" />
                    {isEditing ? "취소" : "편집"}
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex items-start space-x-6">
                  {/* 프로필 이미지 */}
                  <ProfileImageUpload
                    currentImage={(user as any)?.profileImage}
                    userName={profileData.nickname}
                    onImageChange={setSelectedImage}
                    isEditing={isEditing}
                  />

                  {/* 프로필 정보 */}
                  <div className="flex-1 space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="nickname" className="text-sm font-medium text-gray-700">
                          닉네임
                        </Label>
                        {isEditing ? (
                          <Input
                            id="nickname"
                            value={profileData.nickname}
                            onChange={(e) => setProfileData(prev => ({ ...prev, nickname: e.target.value }))}
                            className="h-9"
                          />
                        ) : (
                          <p className="text-gray-900 py-2 border-b border-transparent">
                            {profileData.nickname}
                          </p>
                        )}
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="email" className="text-sm font-medium text-gray-700">
                          이메일
                        </Label>
                        <p className="text-gray-600 py-2 border-b border-transparent">
                          {profileData.email}
                        </p>
                        <p className="text-xs text-gray-500">
                          이메일은 고객센터에서 변경 가능합니다
                        </p>
                      </div>
                    </div>

                    {error && (
                      <ErrorMessage message={error} className="mt-4" />
                    )}

                    {isEditing && (
                      <div className="flex justify-end space-x-3 pt-4">
                        <Button 
                          variant="outline" 
                          onClick={handleCancelEdit}
                          disabled={isSubmitting}
                        >
                          취소
                        </Button>
                        <Button 
                          className="bg-[#009591] hover:bg-[#007a77]"
                          onClick={handleSaveProfile}
                          disabled={isSubmitting}
                        >
                          {isSubmitting ? (
                            <>
                              <LoadingSpinner className="mr-2 h-4 w-4" />
                              저장 중...
                            </>
                          ) : (
                            <>
                              <CheckCircle className="mr-2 h-4 w-4" />
                              저장
                            </>
                          )}
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
            )}

            {/* 기부 현황 요약 */}
            {activeTab === 'profile' && (
              <Card>
              <CardHeader>
                <h2 className="text-lg font-semibold text-gray-900">나의 기부 현황</h2>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <LoadingSpinner className="mr-2" />
                    <span className="text-gray-600">데이터를 불러오는 중...</span>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-[#009591] mb-1">
                        {(dashboard?.donationStats?.totalAmount || 0).toLocaleString()}원
                      </div>
                      <div className="text-sm text-gray-600">총 기부 금액</div>
                    </div>
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-[#009591] mb-1">
                        {(dashboard?.donationStats?.totalCount || 0).toLocaleString()}회
                      </div>
                      <div className="text-sm text-gray-600">기부 횟수</div>
                    </div>
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-[#009591] mb-1">
                        {(dashboard?.favoriteCampaignsCount || 0).toLocaleString()}개
                      </div>
                      <div className="text-sm text-gray-600">관심 캠페인</div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
            )}

            {/* 최근 기부 내역 */}
            {activeTab === 'profile' && (
              <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">최근 기부 내역</h2>
                  <Button variant="ghost" size="sm" className="text-[#009591]" onClick={() => setActiveTab('donations')}>
                    전체보기
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <LoadingSpinner className="mr-2" />
                    <span className="text-gray-600">데이터를 불러오는 중...</span>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {dashboard?.recentDonations && dashboard.recentDonations.length > 0 ? (
                      dashboard.recentDonations.map((donation, index) => {
                      const statusColor = {
                        completed: 'bg-green-500',
                        pending: 'bg-yellow-500', 
                        failed: 'bg-red-500',
                        cancelled: 'bg-gray-500'
                      }[donation.status] || 'bg-gray-500'
                      
                      const statusText = {
                        completed: '✅ 기부완료',
                        pending: '⏳ 처리중',
                        failed: '❌ 실패',
                        cancelled: '⏹️ 취소'
                      }[donation.status] || '❓ 알 수 없음'

                      return (
                        <div key={donation.id || index} className="flex items-center justify-between p-4 bg-white border rounded-lg shadow-sm hover:shadow-md transition-shadow">
                          <div className="flex items-center space-x-3">
                            <div className={`w-3 h-3 ${statusColor} rounded-full`}></div>
                            <div>
                              <p className="font-medium text-gray-900">{statusText}</p>
                              <p className="text-sm text-gray-600">
                                {new Date(donation.donatedAt).toLocaleDateString('ko-KR')}
                              </p>
                              {donation.status === 'completed' && (
                                <p className="text-xs text-green-600 font-medium mt-1">
                                  기부가 성공적으로 완료되었습니다
                                </p>
                              )}
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="font-semibold text-gray-900">
                              {donation.amount?.toLocaleString() || 0}원
                            </p>
                            {donation.status === 'completed' && (
                              <p className="text-xs text-green-600">승인완료</p>
                            )}
                          </div>
                        </div>
                      )
                      })
                    ) : (
                      <div className="text-center py-8 text-gray-500">
                        <p>최근 기부 내역이 없습니다.</p>
                      </div>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
            )}

            {/* 기부 내역 탭 */}
            {activeTab === 'donations' && (
              <DonationList />
            )}

            {activeTab === 'statistics' && (
              <DonationStatistics />
            )}

            {/* 캠페인 관리 탭 */}
            {activeTab === 'favorites' && (
              <CampaignManagement />
            )}

            {activeTab === 'settings' && (
              <div className="space-y-6">
                <NotificationSettingsComponent />
                <PrivacySettingsComponent />
                <AccountSettingsComponent />
              </div>
            )}

            {activeTab === 'support' && (
              <div className="space-y-6">
                <InquiryComponent />
                <Notices />
                <FAQComponent />
              </div>
            )}

            {activeTab === 'support-notices' && (
              <Notices />
            )}

            {activeTab === 'support-faq' && (
              <FAQComponent />
            )}

            {activeTab === 'support-inquiry' && (
              <InquiryComponent />
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
