import { SignupData, VerificationSession, User } from '@/types/signup'
import { ApiResponse } from '@/types/api'

// 임시 데이터 저장소
const mockDatabase = {
  users: [
    { email: 'test@example.com', nickname: 'tester' },
    { email: 'existing@hanachain.com', nickname: 'existing_user' }
  ] as User[],
  verificationSessions: [] as VerificationSession[]
}

// Mock API 설정
const MOCK_DELAY = 800 // 지연 시간 (ms)
const RANDOM_FAILURE_RATE = 0.05 // 랜덤 실패 확률 (5%)

// 유틸리티 함수들
const simulateRandomFailure = (): boolean => {
  return Math.random() < RANDOM_FAILURE_RATE
}

const simulateDelay = (): Promise<void> => {
  return new Promise(resolve => setTimeout(resolve, MOCK_DELAY))
}

const generateVerificationCode = (): string => {
  return Math.floor(100000 + Math.random() * 900000).toString()
}

const logMockApi = (action: string, data?: any) => {
  console.log(`🔧 Mock API: ${action}`, data ? data : '')
}

// Mock API 함수들
export const mockApi = {
  // 이메일 중복 검사 (기존 함수)
  checkEmailDuplicate: async (email: string): Promise<ApiResponse<{ isDuplicate: boolean }>> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('네트워크 오류가 발생했습니다')
    }
    
    const isDuplicate = mockDatabase.users.some(user => user.email === email)
    
    logMockApi('이메일 중복 검사', { email, isDuplicate })
    
    return {
      success: true,
      data: { isDuplicate }
    }
  },

  // 이메일 사용 가능 여부 확인 (새로운 API 훅용)
  checkEmailAvailability: async (email: string): Promise<{ available: boolean; message?: string }> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('네트워크 오류가 발생했습니다')
    }
    
    const isDuplicate = mockDatabase.users.some(user => user.email === email)
    
    logMockApi('이메일 사용 가능 여부 확인', { email, available: !isDuplicate })
    
    return {
      available: !isDuplicate,
      message: isDuplicate ? '이미 사용 중인 이메일입니다' : undefined
    }
  },

  // 인증코드 발송
  sendVerificationCode: async (email: string): Promise<ApiResponse> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('인증코드 발송에 실패했습니다')
    }
    
    // 기존 세션 삭제
    mockDatabase.verificationSessions = mockDatabase.verificationSessions.filter(
      session => session.email !== email
    )
    
    // 새 인증코드 생성 및 저장
    const code = generateVerificationCode()
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000) // 10분 후 만료
    
    mockDatabase.verificationSessions.push({
      email,
      code,
      expiresAt,
      attempts: 0
    })
    
    logMockApi('인증코드 발송', { email, code: `${code.slice(0, 2)}****` })
    
    // 개발 중에는 콘솔에 인증코드를 표시
    console.log(`📧 [개발용] ${email}의 인증코드: ${code}`)
    
    return {
      success: true,
      data: undefined as any,
      message: '인증코드가 발송되었습니다'
    }
  },

  // 인증코드 확인
  verifyCode: async (email: string, code: string): Promise<ApiResponse> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('인증코드 확인에 실패했습니다')
    }
    
    const session = mockDatabase.verificationSessions.find(
      session => session.email === email
    )
    
    if (!session) {
      return {
        success: false,
        message: '인증 세션이 만료되었습니다. 다시 인증코드를 요청해주세요.'
      }
    }
    
    if (new Date() > session.expiresAt) {
      return {
        success: false,
        message: '인증코드가 만료되었습니다. 다시 인증코드를 요청해주세요.'
      }
    }
    
    session.attempts += 1
    
    if (session.attempts > 5) {
      return {
        success: false,
        message: '시도 횟수를 초과했습니다. 다시 인증코드를 요청해주세요.'
      }
    }
    
    if (session.code !== code) {
      return {
        success: false,
        message: '인증코드가 일치하지 않습니다. 다시 입력해주세요.'
      }
    }
    
    logMockApi('인증코드 확인 성공', { email })

    return {
      success: true,
      data: undefined as any,
      message: '이메일 인증이 완료되었습니다'
    }
  },

  // 닉네임 중복 검사 (기존 함수)
  checkNicknameDuplicate: async (nickname: string): Promise<ApiResponse<{ isDuplicate: boolean }>> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('네트워크 오류가 발생했습니다')
    }
    
    const isDuplicate = mockDatabase.users.some(user => user.nickname === nickname)
    
    logMockApi('닉네임 중복 검사', { nickname, isDuplicate })
    
    return {
      success: true,
      data: { isDuplicate }
    }
  },

  // 닉네임 사용 가능 여부 확인 (새로운 API 훅용)
  checkNicknameAvailability: async (nickname: string): Promise<{ available: boolean; message?: string }> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('네트워크 오류가 발생했습니다')
    }
    
    const isDuplicate = mockDatabase.users.some(user => user.nickname === nickname)
    
    logMockApi('닉네임 사용 가능 여부 확인', { nickname, available: !isDuplicate })
    
    return {
      available: !isDuplicate,
      message: isDuplicate ? '이미 사용 중인 닉네임입니다' : undefined
    }
  },

  // 회원가입 (기존 함수)
  register: async (signupData: SignupData): Promise<ApiResponse> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('회원가입에 실패했습니다')
    }
    
    // 이메일 중복 검사
    const emailExists = mockDatabase.users.some(user => user.email === signupData.email)
    if (emailExists) {
      return {
        success: false,
        message: '이미 사용 중인 이메일입니다'
      }
    }
    
    // 닉네임 중복 검사
    const nicknameExists = mockDatabase.users.some(user => user.nickname === signupData.nickname)
    if (nicknameExists) {
      return {
        success: false,
        message: '이미 사용 중인 닉네임입니다'
      }
    }
    
    // 새 사용자 추가
    const newUser: User = {
      email: signupData.email,
      nickname: signupData.nickname
    }
    
    mockDatabase.users.push(newUser)
    
    logMockApi('회원가입 성공', { email: signupData.email, nickname: signupData.nickname })

    return {
      success: true,
      data: undefined as any,
      message: '회원가입이 완료되었습니다'
    }
  },

  // 회원가입 완료 (새로운 API 훅용)
  completeSignup: async (signupData: {
    email: string
    password: string
    nickname: string
  }): Promise<{ success: boolean; userId?: string; message?: string }> => {
    await simulateDelay()
    
    if (simulateRandomFailure()) {
      throw new Error('회원가입에 실패했습니다')
    }
    
    // 이메일 중복 검사
    const emailExists = mockDatabase.users.some(user => user.email === signupData.email)
    if (emailExists) {
      throw new Error('이미 사용 중인 이메일입니다')
    }
    
    // 닉네임 중복 검사
    const nicknameExists = mockDatabase.users.some(user => user.nickname === signupData.nickname)
    if (nicknameExists) {
      throw new Error('이미 사용 중인 닉네임입니다')
    }
    
    // 새 사용자 추가
    const newUser: User = {
      email: signupData.email,
      nickname: signupData.nickname
    }
    
    mockDatabase.users.push(newUser)
    
    const userId = `user_${Date.now()}`
    
    logMockApi('회원가입 완료', { email: signupData.email, nickname: signupData.nickname, userId })
    
    return {
      success: true,
      userId,
      message: '회원가입이 완료되었습니다'
    }
  }
}

// 개발용 디버그 함수
export const debugMockDatabase = () => {
  console.log('📊 Mock Database 상태:', {
    users: mockDatabase.users,
    verificationSessions: mockDatabase.verificationSessions.map(session => ({
      ...session,
      code: `${session.code.slice(0, 2)}****`,
      expiresAt: session.expiresAt.toISOString()
    }))
  })
}
