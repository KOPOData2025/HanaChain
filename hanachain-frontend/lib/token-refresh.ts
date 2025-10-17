/**
 * JWT 토큰 자동 갱신 로직
 * 토큰이 만료되기 전에 자동으로 갱신하는 기능
 */

import { AuthApi } from '@/lib/api/auth-api'
import { loadAuthData, saveAuthData, clearAuthData, isTokenExpiringSoon } from '@/lib/auth-persistence'

class TokenRefreshManager {
  private refreshTimer: NodeJS.Timeout | null = null
  private isRefreshing = false

  /**
   * 토큰 갱신 타이머 시작 (스마트 갱신 로직)
   */
  startTokenRefreshTimer(): void {
    if (typeof window === 'undefined') return
    
    // 기존 타이머 정리
    this.stopTokenRefreshTimer()
    
    // 초기 토큰 상태 확인
    this.scheduleNextRefreshCheck()
    
    console.log('🔄 스마트 토큰 갱신 타이머 시작')
  }

  /**
   * 다음 갱신 확인 스케줄링 (토큰 만료 시간 기반)
   */
  private scheduleNextRefreshCheck(): void {
    const authData = loadAuthData()
    if (!authData) {
      console.log('❌ 갱신 스케줄링: 토큰이 없습니다')
      return
    }

    try {
      // JWT 페이로드에서 만료 시간 추출
      const tokenParts = authData.token.split('.')
      if (tokenParts.length !== 3) {
        console.log('❌ 갱신 스케줄링: 잘못된 JWT 형식')
        return
      }

      const payload = JSON.parse(atob(tokenParts[1]))
      const now = Date.now() / 1000
      const expiresAt = payload.exp

      if (!expiresAt || expiresAt <= now) {
        console.log('❌ 갱신 스케줄링: 토큰이 이미 만료됨')
        this.checkAndRefreshToken()
        return
      }

      // 토큰 만료 30분 전에 갱신 시작
      const refreshTime = expiresAt - (30 * 60) // 30분 전
      const msUntilRefresh = Math.max((refreshTime - now) * 1000, 60000) // 최소 1분 후

      console.log('📅 다음 토큰 갱신 예정:', {
        현재시간: new Date().toISOString(),
        만료시간: new Date(expiresAt * 1000).toISOString(),
        갱신예정: new Date((now + msUntilRefresh / 1000) * 1000).toISOString(),
        대기시간: Math.round(msUntilRefresh / 60000) + '분'
      })

      // 계산된 시간에 갱신 실행
      this.refreshTimer = setTimeout(() => {
        this.checkAndRefreshToken().then(() => {
          // 갱신 후 다음 스케줄 설정
          this.scheduleNextRefreshCheck()
        })
      }, msUntilRefresh)

    } catch (error) {
      console.error('❌ 갱신 스케줄링 중 오류:', error)
      // 오류 시 5분 후 재시도
      this.refreshTimer = setTimeout(() => {
        this.scheduleNextRefreshCheck()
      }, 5 * 60 * 1000)
    }
  }

  /**
   * 토큰 갱신 타이머 중지
   */
  stopTokenRefreshTimer(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
      console.log('⏹️ 토큰 갱신 타이머 중지')
    }
  }

  /**
   * 토큰 상태 확인 및 필요시 갱신 (재시도 로직 포함)
   */
  private async checkAndRefreshToken(): Promise<void> {
    if (this.isRefreshing) {
      console.log('🔄 토큰 갱신이 이미 진행 중입니다')
      return
    }

    try {
      const authData = loadAuthData()
      if (!authData) {
        console.log('❌ 갱신할 토큰이 없습니다')
        return
      }

      console.log('🔄 토큰 갱신 프로세스 시작')
      this.isRefreshing = true

      // 재시도 로직이 포함된 토큰 갱신
      const success = await this.attemptTokenRefreshWithRetry()
      
      if (success) {
        console.log('✅ 토큰 갱신 완료')
      } else {
        console.log('❌ 토큰 갱신 최종 실패 - 로그아웃 처리')
        await this.handleRefreshFailure()
      }
      
    } catch (error) {
      console.error('❌ 토큰 갱신 중 예외 발생:', error)
      await this.handleRefreshFailure()
    } finally {
      this.isRefreshing = false
    }
  }

  /**
   * 재시도 로직이 포함된 토큰 갱신
   */
  private async attemptTokenRefreshWithRetry(maxRetries: number = 3): Promise<boolean> {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        console.log(`🔄 토큰 갱신 시도 ${attempt}/${maxRetries}`)
        
        const refreshResponse = await AuthApi.refreshToken()
        
        if (refreshResponse.success && refreshResponse.data) {
          const { user, token } = refreshResponse.data
          
          // 새로운 토큰과 사용자 정보 저장
          saveAuthData({ token, user })
          
          console.log('✅ 토큰 갱신 성공')
          return true
        } else {
          console.log(`❌ 토큰 갱신 실패 (시도 ${attempt}):`, refreshResponse.message)
        }
        
      } catch (error) {
        console.log(`❌ 토큰 갱신 오류 (시도 ${attempt}):`, error)
        
        // 마지막 시도가 아니면 재시도 전 대기
        if (attempt < maxRetries) {
          const delay = Math.min(1000 * Math.pow(2, attempt - 1), 5000) // 지수 백오프
          console.log(`⏳ ${delay}ms 후 재시도`)
          await new Promise(resolve => setTimeout(resolve, delay))
        }
      }
    }
    
    return false
  }

  /**
   * 토큰 갱신 실패 시 처리
   */
  private async handleRefreshFailure(): Promise<void> {
    try {
      console.log('🧹 토큰 갱신 실패로 인한 로그아웃 처리')
      
      // 인증 데이터 정리
      clearAuthData()
      
      // 사용자에게 알림 (선택적)
      if (typeof window !== 'undefined') {
        // 현재 페이지가 로그인 페이지가 아닌 경우에만 알림
        if (!window.location.pathname.includes('/login')) {
          console.log('🔔 사용자에게 재로그인 필요 알림')
          
          // 3초 후 자동으로 로그인 페이지로 이동
          setTimeout(() => {
            window.location.href = '/login'
          }, 3000)
          
          // 사용자에게 간단한 알림 (옵션)
          if (window.confirm('인증이 만료되었습니다. 로그인 페이지로 이동하시겠습니까?')) {
            window.location.href = '/login'
          }
        }
      }
      
    } catch (error) {
      console.error('❌ 로그아웃 처리 중 오류:', error)
      // 최종 수단으로 새로고침
      if (typeof window !== 'undefined') {
        window.location.reload()
      }
    }
  }

  /**
   * 수동으로 토큰 갱신
   */
  async refreshTokenNow(): Promise<boolean> {
    try {
      this.isRefreshing = true
      await this.checkAndRefreshToken()
      return true
    } catch (error) {
      console.error('수동 토큰 갱신 실패:', error)
      return false
    } finally {
      this.isRefreshing = false
    }
  }
}

// 싱글톤 인스턴스
export const tokenRefreshManager = new TokenRefreshManager()

/**
 * 토큰 갱신 매니저 초기화
 * AuthProvider에서 호출하여 토큰 갱신 타이머 시작
 */
export function initializeTokenRefresh(): void {
  tokenRefreshManager.startTokenRefreshTimer()
}

/**
 * 토큰 갱신 매니저 정리
 * 컴포넌트 언마운트 시 호출하여 타이머 정리
 */
export function cleanupTokenRefresh(): void {
  tokenRefreshManager.stopTokenRefreshTimer()
}

