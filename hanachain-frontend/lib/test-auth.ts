/**
 * 테스트용 JWT 토큰 생성 유틸리티
 * 개발/테스트 중에만 사용
 */

// 간단한 테스트용 JWT 토큰 생성 (실제 서명은 하지 않음)
export function generateTestJwtToken(): string {
  const header = {
    alg: "HS512",
    typ: "JWT"
  }
  
  const payload = {
    sub: "test@example.com",
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + (60 * 60), // 1시간 후 만료
    role: "USER"
  }
  
  // Base64 인코딩 (실제 JWT 형식을 모방)
  const encodedHeader = btoa(JSON.stringify(header))
  const encodedPayload = btoa(JSON.stringify(payload))
  const fakeSignature = btoa("test-signature-for-development")
  
  return `${encodedHeader}.${encodedPayload}.${fakeSignature}`
}

// 실제 백엔드 토큰을 받아오는 함수 (백엔드가 준비되면 사용)
export async function getValidJwtToken(): Promise<string> {
  try {
    const response = await fetch('http://localhost:8080/api/dev/generate-test-token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const result = await response.json()
    if (result.success && result.data && result.data.accessToken) {
      console.log('✅ 백엔드에서 유효한 JWT 토큰을 성공적으로 가져왔습니다.')
      console.log('🔍 받은 토큰:', result.data.accessToken.substring(0, 50) + '...')
      return result.data.accessToken
    } else {
      throw new Error('백엔드에서 유효한 토큰을 받지 못했습니다.')
    }
  } catch (error) {
    console.log('⚠️ 백엔드에서 토큰을 가져올 수 없어 테스트 토큰을 생성합니다:', error)
    return generateTestJwtToken()
  }
}

// localStorage에 테스트 토큰 설정
export function setTestAuthToken(): void {
  const testToken = generateTestJwtToken()
  localStorage.setItem('authToken', testToken)
  console.log('🔧 테스트 JWT 토큰이 설정되었습니다:', testToken.substring(0, 50) + '...')
}
