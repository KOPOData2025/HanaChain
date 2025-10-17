"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { Shield, Eye, BarChart3, CheckCircle } from "lucide-react"
import { PrivacySettings } from "@/types/settings"
import { useSettings } from "@/hooks/use-settings"

interface PrivacySettingsProps {
  className?: string
}

export function PrivacySettingsComponent({ className }: PrivacySettingsProps) {
  const { settings, updateSettings, isLoading, error, clearError } = useSettings()
  const [localSettings, setLocalSettings] = useState<PrivacySettings>(settings.privacy)
  const [hasChanges, setHasChanges] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleToggle = (key: keyof PrivacySettings) => {
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
      await updateSettings({ privacy: localSettings })
      setHasChanges(false)
    } catch (err) {
      // 에러는 useSettings 훅에서 처리됨
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleReset = () => {
    setLocalSettings(settings.privacy)
    setHasChanges(false)
    clearError()
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Shield className="h-5 w-5" />
          <span>개인정보 및 프라이버시</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {error && (
          <ErrorMessage message={error} />
        )}

        {/* 프로필 공개 설정 */}
        <div className="space-y-4">
          <div className="flex items-center space-x-2">
            <Eye className="h-4 w-4 text-gray-600" />
            <h3 className="text-sm font-medium text-gray-900">프로필 공개 설정</h3>
          </div>

          <div className="ml-6 space-y-3">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">프로필 정보 공개</Label>
                <p className="text-xs text-gray-500">
                  다른 사용자가 내 프로필을 볼 수 있습니다
                </p>
              </div>
              <Switch
                checked={localSettings.showProfile}
                onCheckedChange={() => handleToggle('showProfile')}
                disabled={isLoading}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">기부 내역 공개</Label>
                <p className="text-xs text-gray-500">
                  내 기부 내역을 다른 사용자에게 공개합니다
                </p>
              </div>
              <Switch
                checked={localSettings.showDonations}
                onCheckedChange={() => handleToggle('showDonations')}
                disabled={isLoading || !localSettings.showProfile}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">관심 캠페인 공개</Label>
                <p className="text-xs text-gray-500">
                  내가 찜한 캠페인을 다른 사용자에게 공개합니다
                </p>
              </div>
              <Switch
                checked={localSettings.showFavorites}
                onCheckedChange={() => handleToggle('showFavorites')}
                disabled={isLoading || !localSettings.showProfile}
              />
            </div>
          </div>
        </div>

        {/* 데이터 활용 설정 */}
        <div className="space-y-4">
          <div className="flex items-center space-x-2">
            <BarChart3 className="h-4 w-4 text-gray-600" />
            <h3 className="text-sm font-medium text-gray-900">데이터 활용 설정</h3>
          </div>

          <div className="ml-6 space-y-3">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">서비스 개선 분석</Label>
                <p className="text-xs text-gray-500">
                  익명화된 데이터로 서비스 개선에 활용됩니다
                </p>
              </div>
              <Switch
                checked={localSettings.allowAnalytics}
                onCheckedChange={() => handleToggle('allowAnalytics')}
                disabled={isLoading}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">제3자 추적 허용</Label>
                <p className="text-xs text-gray-500">
                  파트너사의 서비스 개선을 위한 데이터 활용을 허용합니다
                </p>
              </div>
              <Switch
                checked={localSettings.allowThirdPartyTracking}
                onCheckedChange={() => handleToggle('allowThirdPartyTracking')}
                disabled={isLoading}
              />
            </div>
          </div>
        </div>

        {/* 중요 알림 */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <Shield className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-medium text-blue-800">개인정보 보호</h4>
              <p className="text-sm text-blue-700 mt-1">
                HanaChain은 개인정보보호법에 따라 사용자의 개인정보를 안전하게 보호합니다. 
                설정 변경은 언제든지 가능하며, 계정 삭제 시 모든 데이터가 안전하게 삭제됩니다.
              </p>
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
