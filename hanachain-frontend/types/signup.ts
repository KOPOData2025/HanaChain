export interface SignupData {
  email: string
  password: string
  nickname: string
  terms: {
    service: boolean
    privacy: boolean
    marketing: boolean
  }
}

export interface VerificationSession {
  email: string
  code: string
  expiresAt: Date
  attempts: number
}

export interface User {
  email: string
  nickname: string
}
