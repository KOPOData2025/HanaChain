"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { Bell, Mail, Smartphone, CheckCircle } from "lucide-react"
import { NotificationSettings } from "@/types/settings"
import { useSettings } from "@/hooks/use-settings"

interface NotificationSettingsProps {
  className?: string
}

export function NotificationSettingsComponent({ className }: NotificationSettingsProps) {
  const { settings, updateSettings, isLoading, error, clearError } = useSettings()
  const [localSettings, setLocalSettings] = useState<NotificationSettings>(settings.notifications)
  const [hasChanges, setHasChanges] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleToggle = (key: keyof NotificationSettings) => {
    const newSettings = {
      ...localSettings,
      [key]: !localSettings[key]
    }
    setLocalSettings(newSettings)
    setHasChanges(true)
  }

  const handleSave = async () => {
    setIsSubmitting(true)
    clearError()

    try {
      await updateSettings({ notifications: localSettings })
      setHasChanges(false)
    } catch (err) {
      // 에러는 useSettings 훅에서 처리됨
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleReset = () => {
    setLocalSettings(settings.notifications)
    setHasChanges(false)
    clearError()
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Bell className="h-5 w-5" />
          <span>알림 설정</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {error && (
          <ErrorMessage message={error} />
        )}

        {/* 이메일 알림 */}
        <div className="space-y-4">
          <div className="flex items-center space-x-2">
            <Mail className="h-4 w-4 text-gray-600" />
            <h3 className="text-sm font-medium text-gray-900">이메일 알림</h3>
          </div>

          <div className="ml-6 space-y-3">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">이메일 알림 수신</Label>
                <p className="text-xs text-gray-500">
                  모든 이메일 알림을 받으시려면 활성화해주세요
                </p>
              </div>
              <Switch
                checked={localSettings.email}
                onCheckedChange={() => handleToggle('email')}
                disabled={isLoading}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">기부 확인 알림</Label>
                <p className="text-xs text-gray-500">
                  기부 완료 시 확인 메일을 받습니다
                </p>
              </div>
              <Switch
                checked={localSettings.donationConfirmation}
                onCheckedChange={() => handleToggle('donationConfirmation')}
                disabled={isLoading || !localSettings.email}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">캠페인 업데이트</Label>
                <p className="text-xs text-gray-500">
                  기부한 캠페인의 진행 상황을 알려드립니다
                </p>
              </div>
              <Switch
                checked={localSettings.campaignUpdates}
                onCheckedChange={() => handleToggle('campaignUpdates')}
                disabled={isLoading || !localSettings.email}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">주간 리포트</Label>
                <p className="text-xs text-gray-500">
                  매주 기부 현황과 추천 캠페인을 받습니다
                </p>
              </div>
              <Switch
                checked={localSettings.weeklyReport}
                onCheckedChange={() => handleToggle('weeklyReport')}
                disabled={isLoading || !localSettings.email}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">마케팅 이메일</Label>
                <p className="text-xs text-gray-500">
                  새로운 캠페인과 이벤트 소식을 받습니다
                </p>
              </div>
              <Switch
                checked={localSettings.marketingEmails}
                onCheckedChange={() => handleToggle('marketingEmails')}
                disabled={isLoading || !localSettings.email}
              />
            </div>
          </div>
        </div>

        {/* 푸시 알림 */}
        <div className="space-y-4">
          <div className="flex items-center space-x-2">
            <Smartphone className="h-4 w-4 text-gray-600" />
            <h3 className="text-sm font-medium text-gray-900">푸시 알림</h3>
          </div>

          <div className="ml-6 space-y-3">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">웹 푸시 알림</Label>
                <p className="text-xs text-gray-500">
                  브라우저에서 실시간 알림을 받습니다
                </p>
              </div>
              <Switch
                checked={localSettings.push}
                onCheckedChange={() => handleToggle('push')}
                disabled={isLoading}
              />
            </div>
          </div>
        </div>

        {/* 저장 버튼 */}
        {hasChanges && (
          <div className="flex items-center justify-end space-x-3 pt-4 border-t">
            <Button
              variant="outline"
              onClick={handleReset}
              disabled={isSubmitting}
            >
              취소
            </Button>
            <Button
              onClick={handleSave}
              disabled={isSubmitting}
              className="bg-[#009591] hover:bg-[#007a77]"
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
      </CardContent>
    </Card>
  )
}
