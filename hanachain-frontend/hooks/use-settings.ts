"use client"

import { useState, useCallback, useEffect } from "react"
import { UserSettings, SettingsUpdateRequest, PasswordUpdateRequest, EmailUpdateRequest } from "@/types/settings"
import { useAuth } from "@/lib/auth-context"

const DEFAULT_SETTINGS: UserSettings = {
  userId: "",
  notifications: {
    email: true,
    push: false,
    campaignUpdates: true,
    donationConfirmation: true,
    weeklyReport: false,
    marketingEmails: false
  },
  privacy: {
    showProfile: true,
    showDonations: false,
    showFavorites: false,
    allowAnalytics: true,
    allowThirdPartyTracking: false
  },
  account: {
    twoFactorEnabled: false,
    loginNotifications: true,
    sessionTimeout: 60, // 60분
    allowMultipleSessions: false
  },
  updatedAt: new Date()
}

export function useSettings() {
  const { user } = useAuth()
  const [settings, setSettings] = useState<UserSettings>(DEFAULT_SETTINGS)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 설정 로드
  const loadSettings = useCallback(async () => {
    if (!user?.id) return

    setIsLoading(true)
    setError(null)

    try {
      // Mock API 호출 시뮬레이션
      await new Promise(resolve => setTimeout(resolve, 500))

      // Mock 데이터
      const mockSettings: UserSettings = {
        ...DEFAULT_SETTINGS,
        userId: user.id,
        updatedAt: new Date()
      }

      setSettings(mockSettings)
    } catch (err) {
      setError('설정을 불러오는데 실패했습니다.')
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  // 설정 업데이트
  const updateSettings = useCallback(async (updates: SettingsUpdateRequest) => {
    setIsLoading(true)
    setError(null)

    try {
      // Mock API 호출 시뮬레이션
      await new Promise(resolve => setTimeout(resolve, 800))

      const updatedSettings = {
        ...settings,
        ...updates,
        notifications: updates.notifications 
          ? { ...settings.notifications, ...updates.notifications }
          : settings.notifications,
        privacy: updates.privacy
          ? { ...settings.privacy, ...updates.privacy }
          : settings.privacy,
        account: updates.account
          ? { ...settings.account, ...updates.account }
          : settings.account,
        updatedAt: new Date()
      }

      setSettings(updatedSettings)
      return updatedSettings
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '설정 업데이트에 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [settings])

  // 비밀번호 변경
  const updatePassword = useCallback(async (passwordData: PasswordUpdateRequest) => {
    setIsLoading(true)
    setError(null)

    try {
      // 비밀번호 확인
      if (passwordData.newPassword !== passwordData.confirmPassword) {
        throw new Error('새 비밀번호가 일치하지 않습니다.')
      }

      if (passwordData.newPassword.length < 8) {
        throw new Error('비밀번호는 8자 이상이어야 합니다.')
      }

      // Mock API 호출 시뮬레이션
      await new Promise(resolve => setTimeout(resolve, 1000))

      // 실제 구현에서는 서버에서 비밀번호 변경 처리
      return { success: true, message: '비밀번호가 성공적으로 변경되었습니다.' }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '비밀번호 변경에 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 이메일 변경
  const updateEmail = useCallback(async (emailData: EmailUpdateRequest) => {
    setIsLoading(true)
    setError(null)

    try {
      // 이메일 형식 검증
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(emailData.newEmail)) {
        throw new Error('올바른 이메일 형식이 아닙니다.')
      }

      // Mock API 호출 시뮬레이션
      await new Promise(resolve => setTimeout(resolve, 1000))

      // 실제 구현에서는 이메일 변경 및 인증 메일 발송
      return { 
        success: true, 
        message: '이메일 변경 확인 메일을 발송했습니다. 메일을 확인해주세요.' 
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '이메일 변경에 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 계정 삭제
  const deleteAccount = useCallback(async (password: string) => {
    setIsLoading(true)
    setError(null)

    try {
      if (!password) {
        throw new Error('비밀번호를 입력해주세요.')
      }

      // Mock API 호출 시뮬레이션
      await new Promise(resolve => setTimeout(resolve, 1500))

      // 실제 구현에서는 계정 삭제 처리
      return { success: true, message: '계정이 성공적으로 삭제되었습니다.' }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '계정 삭제에 실패했습니다.'
      setError(errorMessage)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 컴포넌트 마운트 시 설정 로드
  useEffect(() => {
    loadSettings()
  }, [loadSettings])

  return {
    settings,
    isLoading,
    error,
    loadSettings,
    updateSettings,
    updatePassword,
    updateEmail,
    deleteAccount,
    clearError: () => setError(null)
  }
}
