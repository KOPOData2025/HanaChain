"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { LoadingSpinner } from "@/components/ui/loading-spinner"
import { ErrorMessage } from "@/components/ui/error-message"
import { 
  Key, 
  Mail, 
  Shield, 
  Trash2, 
  CheckCircle, 
  AlertTriangle,
  Eye,
  EyeOff
} from "lucide-react"
import { useSettings } from "@/hooks/use-settings"
import { useAuth } from "@/lib/auth-context"

interface AccountSettingsProps {
  className?: string
}

export function AccountSettingsComponent({ className }: AccountSettingsProps) {
  const { user } = useAuth()
  const { settings, updateSettings, updatePassword, updateEmail, deleteAccount, isLoading, error, clearError } = useSettings()
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  const [emailData, setEmailData] = useState({
    newEmail: '',
    password: ''
  })
  const [deletePassword, setDeletePassword] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)
  const [isPasswordDialogOpen, setIsPasswordDialogOpen] = useState(false)
  const [isEmailDialogOpen, setIsEmailDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)

  const handleAccountSettingsUpdate = async (field: string, value: any) => {
    try {
      await updateSettings({ account: { [field]: value } })
    } catch (err) {
      // 에러는 useSettings 훅에서 처리됨
    }
  }

  const handlePasswordUpdate = async () => {
    try {
      await updatePassword(passwordData)
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setIsPasswordDialogOpen(false)
      alert('비밀번호가 성공적으로 변경되었습니다.')
    } catch (err) {
      // 에러는 useSettings 훅에서 처리됨
    }
  }

  const handleEmailUpdate = async () => {
    try {
      const result = await updateEmail(emailData)
      setEmailData({ newEmail: '', password: '' })
      setIsEmailDialogOpen(false)
      alert(result.message)
    } catch (err) {
      // 에러는 useSettings 훅에서 처리됨
    }
  }

  const handleAccountDelete = async () => {
    try {
      await deleteAccount(deletePassword)
      setDeletePassword('')
      setIsDeleteDialogOpen(false)
      // 계정 삭제 후 로그아웃 처리
      alert('계정이 성공적으로 삭제되었습니다.')
    } catch (err) {
      // 에러는 useSettings 훅에서 처리됨
    }
  }

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Shield className="h-5 w-5" />
          <span>계정 설정</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {error && (
          <ErrorMessage message={error} />
        )}

        {/* 계정 정보 */}
        <div className="space-y-4">
          <h3 className="text-sm font-medium text-gray-900">계정 정보</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label className="text-sm">이메일</Label>
              <div className="flex items-center space-x-2">
                <div className="flex-1 py-2 px-3 bg-gray-50 border rounded-md text-sm text-gray-700">
                  {user?.email}
                </div>
                <Dialog open={isEmailDialogOpen} onOpenChange={setIsEmailDialogOpen}>
                  <DialogTrigger asChild>
                    <Button variant="outline" size="sm">
                      변경
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>이메일 변경</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4">
                      <div className="space-y-2">
                        <Label>새 이메일</Label>
                        <Input
                          type="email"
                          value={emailData.newEmail}
                          onChange={(e) => setEmailData(prev => ({ ...prev, newEmail: e.target.value }))}
                          placeholder="새 이메일을 입력하세요"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>현재 비밀번호</Label>
                        <Input
                          type="password"
                          value={emailData.password}
                          onChange={(e) => setEmailData(prev => ({ ...prev, password: e.target.value }))}
                          placeholder="현재 비밀번호를 입력하세요"
                        />
                      </div>
                      <div className="flex justify-end space-x-2">
                        <Button variant="outline" onClick={() => setIsEmailDialogOpen(false)}>
                          취소
                        </Button>
                        <Button 
                          onClick={handleEmailUpdate}
                          disabled={isLoading || !emailData.newEmail || !emailData.password}
                        >
                          {isLoading ? <LoadingSpinner className="mr-2 h-4 w-4" /> : <Mail className="mr-2 h-4 w-4" />}
                          변경
                        </Button>
                      </div>
                    </div>
                  </DialogContent>
                </Dialog>
              </div>
            </div>

            <div className="space-y-2">
              <Label className="text-sm">비밀번호</Label>
              <Dialog open={isPasswordDialogOpen} onOpenChange={setIsPasswordDialogOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline" className="w-full">
                    <Key className="mr-2 h-4 w-4" />
                    비밀번호 변경
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>비밀번호 변경</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label>현재 비밀번호</Label>
                      <div className="relative">
                        <Input
                          type={showPasswords ? "text" : "password"}
                          value={passwordData.currentPassword}
                          onChange={(e) => setPasswordData(prev => ({ ...prev, currentPassword: e.target.value }))}
                          placeholder="현재 비밀번호"
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="absolute right-2 top-1/2 transform -translate-y-1/2 h-7 w-7 p-0"
                          onClick={() => setShowPasswords(!showPasswords)}
                        >
                          {showPasswords ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </Button>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label>새 비밀번호</Label>
                      <Input
                        type={showPasswords ? "text" : "password"}
                        value={passwordData.newPassword}
                        onChange={(e) => setPasswordData(prev => ({ ...prev, newPassword: e.target.value }))}
                        placeholder="새 비밀번호 (8자 이상)"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>비밀번호 확인</Label>
                      <Input
                        type={showPasswords ? "text" : "password"}
                        value={passwordData.confirmPassword}
                        onChange={(e) => setPasswordData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                        placeholder="새 비밀번호 확인"
                      />
                    </div>
                    <div className="flex justify-end space-x-2">
                      <Button variant="outline" onClick={() => setIsPasswordDialogOpen(false)}>
                        취소
                      </Button>
                      <Button 
                        onClick={handlePasswordUpdate}
                        disabled={isLoading || !passwordData.currentPassword || !passwordData.newPassword || !passwordData.confirmPassword}
                      >
                        {isLoading ? <LoadingSpinner className="mr-2 h-4 w-4" /> : <CheckCircle className="mr-2 h-4 w-4" />}
                        변경
                      </Button>
                    </div>
                  </div>
                </DialogContent>
              </Dialog>
            </div>
          </div>
        </div>

        {/* 보안 설정 */}
        <div className="space-y-4">
          <h3 className="text-sm font-medium text-gray-900">보안 설정</h3>
          
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">2단계 인증</Label>
                <p className="text-xs text-gray-500">
                  로그인 시 추가 인증이 필요합니다
                </p>
              </div>
              <Switch
                checked={settings.account.twoFactorEnabled}
                onCheckedChange={(checked) => handleAccountSettingsUpdate('twoFactorEnabled', checked)}
                disabled={isLoading}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">로그인 알림</Label>
                <p className="text-xs text-gray-500">
                  새로운 기기에서 로그인 시 알림을 받습니다
                </p>
              </div>
              <Switch
                checked={settings.account.loginNotifications}
                onCheckedChange={(checked) => handleAccountSettingsUpdate('loginNotifications', checked)}
                disabled={isLoading}
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">세션 타임아웃</Label>
                <p className="text-xs text-gray-500">
                  비활성 상태 시 자동 로그아웃 시간
                </p>
              </div>
              <Select
                value={settings.account.sessionTimeout.toString()}
                onValueChange={(value) => handleAccountSettingsUpdate('sessionTimeout', parseInt(value))}
                disabled={isLoading}
              >
                <SelectTrigger className="w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="30">30분</SelectItem>
                  <SelectItem value="60">1시간</SelectItem>
                  <SelectItem value="120">2시간</SelectItem>
                  <SelectItem value="240">4시간</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-sm font-normal">다중 세션 허용</Label>
                <p className="text-xs text-gray-500">
                  여러 기기에서 동시 로그인을 허용합니다
                </p>
              </div>
              <Switch
                checked={settings.account.allowMultipleSessions}
                onCheckedChange={(checked) => handleAccountSettingsUpdate('allowMultipleSessions', checked)}
                disabled={isLoading}
              />
            </div>
          </div>
        </div>

        {/* 계정 삭제 */}
        <div className="space-y-4 border-t pt-6">
          <h3 className="text-sm font-medium text-red-600">위험 구역</h3>
          
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-start space-x-3">
              <AlertTriangle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <h4 className="font-medium text-red-800">계정 삭제</h4>
                <p className="text-sm text-red-700 mt-1">
                  계정을 삭제하면 모든 데이터가 영구적으로 삭제되며 복구할 수 없습니다.
                </p>
                <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
                  <DialogTrigger asChild>
                    <Button variant="destructive" size="sm" className="mt-3">
                      <Trash2 className="mr-2 h-4 w-4" />
                      계정 삭제
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle className="text-red-600">계정 삭제 확인</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4">
                      <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                        <p className="text-sm text-red-800">
                          정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
                        </p>
                      </div>
                      <div className="space-y-2">
                        <Label>비밀번호 확인</Label>
                        <Input
                          type="password"
                          value={deletePassword}
                          onChange={(e) => setDeletePassword(e.target.value)}
                          placeholder="비밀번호를 입력하세요"
                        />
                      </div>
                      <div className="flex justify-end space-x-2">
                        <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
                          취소
                        </Button>
                        <Button 
                          variant="destructive"
                          onClick={handleAccountDelete}
                          disabled={isLoading || !deletePassword}
                        >
                          {isLoading ? <LoadingSpinner className="mr-2 h-4 w-4" /> : <Trash2 className="mr-2 h-4 w-4" />}
                          계정 삭제
                        </Button>
                      </div>
                    </div>
                  </DialogContent>
                </Dialog>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
