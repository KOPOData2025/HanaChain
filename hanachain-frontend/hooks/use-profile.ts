"use client"

import { useState, useCallback, useEffect } from "react"
import { useAuth } from "@/lib/auth-context"
import { MyPageApi, ProfileData, ProfileUpdateData, DashboardData } from "@/lib/api/mypage-api"
import { ApiError } from "@/lib/api/client"

export function useProfile() {
  const { user, setUser, isLoggedIn } = useAuth()
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [profile, setProfile] = useState<ProfileData | null>(null)
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)

  // 프로필 정보 조회
  const fetchProfile = useCallback(async () => {
    if (!isLoggedIn) return

    console.log('👤 마이페이지 프로필 데이터 가져오기 시작')
    setIsLoading(true)
    setError(null)

    try {
      const profileData = await MyPageApi.getProfile()
      console.log('🎯 마이페이지 use-profile 훅에서 받은 프로필 데이터:', profileData)
      setProfile(profileData)
      
      // Auth context 업데이트
      if (setUser && user) {
        setUser({
          ...user,
          nickname: profileData.nickname,
          email: profileData.email,
        })
        console.log('🔄 인증 컨텍스트 업데이트 완료')
      }
      
      return profileData
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : '프로필 정보를 불러오는데 실패했습니다'
      setError(errorMessage)
      console.error('❌ 마이페이지 프로필 fetch 에러 (use-profile):', err)
    } finally {
      setIsLoading(false)
    }
  }, [isLoggedIn, setUser, user])

  // 대시보드 데이터 조회
  const fetchDashboard = useCallback(async () => {
    if (!isLoggedIn) {
      console.log('⚠️ 로그인되지 않았습니다 - 대시보드 데이터를 가져올 수 없습니다')
      return
    }

    console.log('📊 마이페이지 대시보드 데이터 가져오기 시작')
    setIsLoading(true)
    setError(null)

    try {
      const dashboardData = await MyPageApi.getDashboard()
      console.log('🎯 마이페이지 use-profile 훅에서 받은 대시보드 데이터:', dashboardData)
      console.log('📈 대시보드 데이터 상세 내용:', {
        profile: dashboardData.profile,
        donationStats: dashboardData.donationStats,
        recentDonations: dashboardData.recentDonations,
        favoriteCampaignsCount: dashboardData.favoriteCampaignsCount
      })
      setDashboard(dashboardData)
      setProfile(dashboardData.profile)
      console.log('💾 마이페이지 상태 업데이트 완료 - 대시보드 및 프로필 저장됨')
      console.log('✅ setDashboard 호출 완료 - 컴포넌트에서 dashboard 상태가 업데이트될 것입니다')
      return dashboardData
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : '대시보드 정보를 불러오는데 실패했습니다'
      setError(errorMessage)
      console.error('❌ 마이페이지 대시보드 fetch 에러 (use-profile):', err)
    } finally {
      setIsLoading(false)
      console.log('🏁 대시보드 로딩 상태 종료 (isLoading = false)')
    }
  }, [isLoggedIn])

  // 프로필 정보 수정
  const updateProfile = useCallback(async (data: ProfileUpdateData) => {
    setIsLoading(true)
    setError(null)

    try {
      const updatedProfile = await MyPageApi.updateProfile(data)
      setProfile(updatedProfile)
      
      // Auth context 업데이트
      if (setUser && user) {
        setUser({
          ...user,
          nickname: updatedProfile.nickname,
          email: updatedProfile.email,
        })
      }
      
      return updatedProfile
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : '프로필 수정에 실패했습니다'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [setUser, user])

  // 프로필 이미지 업로드
  const uploadImage = useCallback(async (file: File): Promise<string> => {
    setIsLoading(true)
    setError(null)

    try {
      const uploadResponse = await MyPageApi.uploadProfileImage(file)
      
      // 프로필 데이터 업데이트
      if (profile) {
        const updatedProfile = { ...profile, profileImage: uploadResponse.imageUrl }
        setProfile(updatedProfile)
      }
      
      // Auth context 업데이트
      if (setUser && user) {
        setUser({
          ...user,
          profileImage: uploadResponse.imageUrl,
        })
      }
      
      return uploadResponse.imageUrl
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : '이미지 업로드에 실패했습니다'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [profile, setUser, user])

  // 프로필 이미지 삭제
  const deleteProfileImage = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      await MyPageApi.deleteProfileImage()
      
      // 프로필 데이터 업데이트
      if (profile) {
        const updatedProfile = { ...profile, profileImage: null }
        setProfile(updatedProfile)
      }
      
      // Auth context 업데이트
      if (setUser && user) {
        setUser({
          ...user,
          profileImage: null,
        })
      }
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : '프로필 이미지 삭제에 실패했습니다'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [profile, setUser, user])

  // 프로필 완성도 조회
  const getProfileCompleteness = useCallback(async (): Promise<number> => {
    try {
      return await MyPageApi.getProfileCompleteness()
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : '프로필 완성도 조회에 실패했습니다'
      setError(errorMessage)
      return 0
    }
  }, [])

  // 로그인 상태 변화 시 자동으로 프로필 조회
  useEffect(() => {
    if (isLoggedIn && !profile) {
      fetchProfile()
    }
  }, [isLoggedIn, profile, fetchProfile])

  return {
    // 데이터
    profile: profile || user,
    dashboard,
    
    // 상태
    isLoading,
    error,
    
    // 메서드
    fetchProfile,
    fetchDashboard,
    updateProfile,
    uploadImage,
    deleteProfileImage,
    getProfileCompleteness,
    clearError: () => setError(null),
  }
}
