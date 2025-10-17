/**
 * 백엔드 API 관련 타입 정의
 */

// 기본 API 응답 구조
export interface BaseApiResponse {
  success: boolean
  message?: string
  timestamp?: string
}

// 성공 응답 (데이터 포함)
export interface SuccessApiResponse<T> extends BaseApiResponse {
  success: true
  data: T
}

// 에러 응답
export interface ErrorApiResponse extends BaseApiResponse {
  success: false
  error?: {
    code: string
    details?: any
  }
}

// 유니온 타입
export type ApiResponse<T = any> = SuccessApiResponse<T> | ErrorApiResponse

// 페이지네이션 관련
export interface PaginationMeta {
  page: number
  limit: number
  totalCount: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface PaginatedResponse<T> {
  data: T[]
  meta: PaginationMeta
}

// 인증 관련 타입
export interface AuthTokens {
  accessToken: string
  refreshToken?: string
  tokenType?: string
  expiresIn?: number
}

export interface UserProfile {
  id: string
  email: string
  nickname?: string
  name?: string
  phoneNumber?: string
  profileImage?: string
  createdAt: string
  updatedAt: string
  emailVerified: boolean
  status: 'active' | 'inactive' | 'suspended'
}

// 로그인 관련
export interface LoginRequestBody {
  email: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponseData {
  user: UserProfile
  accessToken: string
  refreshToken: string
  tokenType?: string
}

// 회원가입 관련
export interface SignupRequestBody {
  email: string
  password: string
  nickname: string
  termsAccepted: boolean
  privacyAccepted: boolean
  marketingAccepted?: boolean
}

export interface SignupResponseData {
  userId: string
  message: string
}

// 이메일/닉네임 중복 검사
export interface DuplicationCheckRequestBody {
  email?: string
  nickname?: string
}

export interface DuplicationCheckResponseData {
  available: boolean
  suggestions?: string[]
}

// 이메일 인증 관련
export interface SendVerificationRequestBody {
  email: string
  type: 'EMAIL_REGISTRATION' | 'PASSWORD_RESET' | 'EMAIL_CHANGE'
}

export interface SendVerificationResponseData {
  verificationId: string
  expiresAt: string
}

export interface VerifyCodeRequestBody {
  email: string
  code: string
  verificationId?: string
}

export interface VerifyCodeResponseData {
  verified: boolean
  token?: string
}

// HTTP 상태 코드 상수
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
} as const

// API 에러 코드 상수
export const API_ERROR_CODES = {
  // 인증 관련
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  TOKEN_EXPIRED: 'TOKEN_EXPIRED',
  TOKEN_INVALID: 'TOKEN_INVALID',
  ACCOUNT_NOT_VERIFIED: 'ACCOUNT_NOT_VERIFIED',
  ACCOUNT_SUSPENDED: 'ACCOUNT_SUSPENDED',
  
  // 회원가입 관련
  EMAIL_ALREADY_EXISTS: 'EMAIL_ALREADY_EXISTS',
  NICKNAME_ALREADY_EXISTS: 'NICKNAME_ALREADY_EXISTS',
  INVALID_EMAIL_FORMAT: 'INVALID_EMAIL_FORMAT',
  INVALID_PASSWORD_FORMAT: 'INVALID_PASSWORD_FORMAT',
  
  // 인증코드 관련
  VERIFICATION_CODE_EXPIRED: 'VERIFICATION_CODE_EXPIRED',
  VERIFICATION_CODE_INVALID: 'VERIFICATION_CODE_INVALID',
  VERIFICATION_CODE_ATTEMPTS_EXCEEDED: 'VERIFICATION_CODE_ATTEMPTS_EXCEEDED',
  
  // 일반적인 오류
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  NETWORK_ERROR: 'NETWORK_ERROR',
  SERVER_ERROR: 'SERVER_ERROR',
  RATE_LIMIT_EXCEEDED: 'RATE_LIMIT_EXCEEDED',
} as const

// API 엔드포인트 상수
export const API_ENDPOINTS = {
  // 인증
  LOGIN: '/auth/login',
  LOGOUT: '/auth/logout',
  REFRESH: '/auth/refresh',
  PROFILE: '/auth/profile',
  VALIDATE: '/auth/validate',
  
  // 회원가입
  SIGNUP: '/auth/register',
  CHECK_EMAIL: '/auth/check-email',
  CHECK_NICKNAME: '/auth/check-nickname',
  
  // 이메일 인증
  SEND_VERIFICATION: '/auth/verification/send',
  VERIFY_CODE: '/auth/verification/verify',
  
  // 비밀번호 재설정
  FORGOT_PASSWORD: '/auth/forgot-password',
  RESET_PASSWORD: '/auth/reset-password',
} as const

export type ApiEndpoint = typeof API_ENDPOINTS[keyof typeof API_ENDPOINTS]
export type ApiErrorCode = typeof API_ERROR_CODES[keyof typeof API_ERROR_CODES]
export type HttpStatus = typeof HTTP_STATUS[keyof typeof HTTP_STATUS]