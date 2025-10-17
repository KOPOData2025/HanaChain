"use client"

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { AuthApi } from '@/lib/api/auth-api'
import { loadAuthData, clearAuthData, isValidJwtToken, isTokenExpired } from '@/lib/auth-persistence'
import { initializeTokenRefresh, cleanupTokenRefresh } from '@/lib/token-refresh'

interface User {
  id: string
  email: string
  nickname?: string
  name?: string
  phoneNumber?: string
}

type TokenStatus = 'valid' | 'pending' | 'expired' | 'unknown'

interface AuthContextType {
  isLoggedIn: boolean
  user: User | null
  setUser: (user: User | null) => void
  login: (email: string, password: string) => Promise<boolean>
  logout: () => void
  loading: boolean
  tokenStatus: TokenStatus
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [tokenStatus, setTokenStatus] = useState<TokenStatus>('unknown')

  // 컴포넌트 마운트 시 토큰 기반 인증 상태 확인
  useEffect(() => {
    checkAuthStatus()
    
    // 토큰 갱신 타이머 시작
    initializeTokenRefresh()
    
    // 컴포넌트 언마운트 시 정리
    return () => {
      cleanupTokenRefresh()
    }
  }, [])

  // 인증 상태 확인 함수
  const checkAuthStatus = async () => {
    try {
      setLoading(true)

      // 서버사이드 렌더링 환경에서는 localStorage 접근 불가
      if (typeof window === 'undefined') {
        console.log('🔍 [checkAuthStatus] 서버 환경 - 인증 확인 건너뛰기')
        setLoading(false)
        return
      }

      console.log('🔍 [checkAuthStatus] 페이지 로드 시 인증 상태 확인 시작')

      // localStorage 직접 확인
      const rawToken = localStorage.getItem('authToken')
      const rawUser = localStorage.getItem('authUser')
      console.log('🔍 [checkAuthStatus] localStorage 직접 조회:', {
        hasRawToken: !!rawToken,
        rawTokenPreview: rawToken ? rawToken.substring(0, 30) + '...' : 'null',
        rawTokenLength: rawToken?.length || 0,
        hasRawUser: !!rawUser,
        rawUserPreview: rawUser ? rawUser.substring(0, 50) + '...' : 'null'
      })

      // 저장된 인증 데이터 복원
      const authData = loadAuthData()

      if (!authData) {
        console.log('❌ [checkAuthStatus] loadAuthData() 결과가 없습니다')
        // 잘못된 토큰이 남아있을 수 있으므로 정리
        const token = localStorage.getItem('authToken')
        if (token && (token === 'YOUR_JWT_TOKEN' || token === 'null' || token === 'undefined')) {
          console.log('🧹 [checkAuthStatus] 잘못된 토큰 정리:', token)
          localStorage.removeItem('authToken')
        }
        setLoading(false)
        return
      }

      console.log('✅ [checkAuthStatus] 저장된 인증 데이터 발견:', {
        hasToken: !!authData.token,
        tokenPreview: authData.token ? authData.token.substring(0, 30) + '...' : 'null',
        tokenLength: authData.token?.length || 0,
        userEmail: authData.user.email,
        tokenValid: isValidJwtToken(authData.token)
      })
      
      // JWT 토큰 형식 검증
      if (!isValidJwtToken(authData.token)) {
        console.log('❌ JWT 토큰 형식이 올바르지 않습니다')
        clearAuthData()
        setLoading(false)
        return
      }
      
      // 토큰 만료 확인
      if (isTokenExpired(authData.token)) {
        console.log('❌ 토큰이 만료되었습니다')
        clearAuthData()
        setUser(null)
        setIsLoggedIn(false)
        setTokenStatus('expired')
        setLoading(false)
        return
      }
      
      // 저장된 사용자 정보로 즉시 UI 업데이트 (빠른 응답)
      setUser(authData.user)
      setIsLoggedIn(true)
      setTokenStatus('pending') // 서버 검증 대기 중
      console.log('✅ 저장된 사용자 정보로 UI 복원 완료')
      
      // 백그라운드에서 토큰 유효성 검증 및 프로필 업데이트 (비차단적)
      try {
        console.log('🔍 백그라운드 서버 토큰 검증 시작...')
        const isTokenValid = await AuthApi.validateToken()
        if (!isTokenValid) {
          console.warn('⚠️ 서버 토큰 검증 실패 - 저장된 인증 정보 유지 (네트워크 오류 가능성)')
          console.warn('⚠️ 다음 API 호출 시 재검증됩니다')
          setTokenStatus('unknown') // 검증 실패, 상태 불명
          // 토큰 검증 실패해도 저장된 정보는 유지
          // 실제 API 호출 시 401 응답으로 재검증될 예정
          return
        }
        console.log('✅ 백그라운드 서버 토큰 검증 성공')
        setTokenStatus('valid') // 검증 성공
        
        // 최신 사용자 프로필 정보 조회
        const userProfile = await AuthApi.getProfile()
        if (userProfile) {
          setUser(userProfile)
          setIsLoggedIn(true)
          localStorage.setItem('authUser', JSON.stringify(userProfile))
          console.log('✅ 사용자 프로필 정보 업데이트 완료')
        }
      } catch (validationError) {
        console.warn('⚠️ 백그라운드 토큰 검증 중 오류 - 저장된 정보 유지:', validationError)
        console.warn('⚠️ 일시적 네트워크 오류일 수 있으므로 인증 상태는 유지됩니다')
        setTokenStatus('unknown') // 검증 오류, 상태 불명
        // 검증 실패해도 저장된 정보가 있으면 계속 진행
        // 사용자가 실제 API를 호출할 때 재검증될 예정
      }
      
    } catch (error) {
      console.error('인증 상태 확인 중 오류:', error)
      // 오류 발생 시 저장된 정보가 있으면 유지, 없으면 로그아웃
      const authData = loadAuthData()
      if (!authData) {
        clearAuthData()
        setUser(null)
        setIsLoggedIn(false)
        setTokenStatus('unknown')
      } else {
        // 저장된 정보가 있으면 유지하되 상태를 불명으로 설정
        setTokenStatus('unknown')
        console.warn('⚠️ 인증 상태 확인 중 오류 발생 - 저장된 정보 유지')
      }
    } finally {
      setLoading(false)
    }
  }

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      setLoading(true)
      
      console.log('🚀 AuthContext login 시작:', { email, password: '***' })
      
      // 실제 백엔드 API 로그인 호출
      const response = await AuthApi.login({ email, password })
      
      console.log('🔍 AuthContext - AuthApi.login 응답:', response)
      
      if (response.success && response.data) {
        const { user: userData, token } = response.data
        
        console.log('✅ AuthContext - 로그인 성공:', { userData, token: token ? '토큰있음' : '토큰없음' })
        
        // 토큰이 있으면 ApiClient에 설정 (이미 AuthApi.login에서 처리됨)
        if (token) {
          console.log('🔧 AuthContext - 토큰 재설정 확인')
        }
        
        // 사용자 정보 상태 업데이트
        setUser(userData)
        setIsLoggedIn(true)
        setTokenStatus('valid') // 로그인 성공 시 토큰 상태를 유효로 설정
        
        // 로컬스토리지에 사용자 정보 저장 (토큰은 ApiClient에서 자동 관리)
        localStorage.setItem('authUser', JSON.stringify(userData))
        
        // 토큰 저장 확인
        const savedToken = localStorage.getItem('authToken')
        console.log('✅ AuthContext - localStorage 업데이트 완료')
        console.log('🔍 현재 localStorage authToken:', savedToken ? '저장됨' : '저장실패')
        console.log('🔍 현재 localStorage authUser:', localStorage.getItem('authUser'))
        
        // 토큰이 제대로 저장되었는지 확인
        if (!savedToken || savedToken === 'YOUR_JWT_TOKEN') {
          console.error('❌ 토큰 저장 실패! 수동으로 설정합니다.')
          if (token) {
            localStorage.setItem('authToken', token)
            console.log('✅ 수동 토큰 저장 완료')
          }
        }
        
        // 토큰 갱신 타이머 재시작
        initializeTokenRefresh()
        
        return true
      } else {
        console.log('❌ AuthContext - 로그인 실패:', response)
      }
      
      return false
    } catch (error) {
      console.error('❌ AuthContext - 로그인 중 오류:', error)
      return false
    } finally {
      setLoading(false)
    }
  }

  const logout = async () => {
    try {
      // 토큰 갱신 타이머 중지
      cleanupTokenRefresh()
      
      // 백엔드에 로그아웃 요청
      await AuthApi.logout()
    } catch (error) {
      console.error('로그아웃 API 호출 실패:', error)
    } finally {
      // 로컬 상태 초기화
      setUser(null)
      setIsLoggedIn(false)
      setTokenStatus('unknown')
      
      // 로컬스토리지에서 인증 정보 삭제
      clearAuthData()
      console.log('✅ 로그아웃 완료 - 모든 인증 데이터 삭제')
    }
  }

  const value: AuthContextType = {
    isLoggedIn,
    user,
    setUser,
    login,
    logout,
    loading,
    tokenStatus
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth는 AuthProvider 내에서 사용되어야 합니다')
  }
  return context
}