export interface NotificationSettings {
  email: boolean
  push: boolean
  campaignUpdates: boolean
  donationConfirmation: boolean
  weeklyReport: boolean
  marketingEmails: boolean
}

export interface PrivacySettings {
  showProfile: boolean
  showDonations: boolean
  showFavorites: boolean
  allowAnalytics: boolean
  allowThirdPartyTracking: boolean
}

export interface AccountSettings {
  twoFactorEnabled: boolean
  loginNotifications: boolean
  sessionTimeout: number // minutes
  allowMultipleSessions: boolean
}

export interface UserSettings {
  userId: string
  notifications: NotificationSettings
  privacy: PrivacySettings
  account: AccountSettings
  updatedAt: Date
}

export interface SettingsUpdateRequest {
  notifications?: Partial<NotificationSettings>
  privacy?: Partial<PrivacySettings>
  account?: Partial<AccountSettings>
}

export interface PasswordUpdateRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface EmailUpdateRequest {
  newEmail: string
  password: string
}
