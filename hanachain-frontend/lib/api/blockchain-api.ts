import { apiClient } from './client'
import { ApiResponse } from '@/types/api'

/**
 * 블록체인 네트워크 정보 타입
 */
export interface NetworkInfo {
  networkName: string
  clientVersion: string
  blockNumber: string  // BigInteger는 문자열로 전달됨
  gasPrice: string     // BigInteger는 문자열로 전달됨
  connected: boolean
}

/**
 * Wei를 Gwei로 변환하는 유틸 함수
 * @param wei Wei 단위 값 (문자열)
 * @returns Gwei 단위 값 (소수점 2자리까지)
 */
export function weiToGwei(wei: string): string {
  try {
    const weiValue = BigInt(wei)
    const gweiValue = Number(weiValue) / 1e9
    return gweiValue.toFixed(2)
  } catch (error) {
    console.error('Wei to Gwei 변환 실패:', error)
    return '0.00'
  }
}

/**
 * 블록 번호 포맷팅 (천 단위 구분)
 * @param blockNumber 블록 번호 (문자열)
 * @returns 포맷팅된 블록 번호
 */
export function formatBlockNumber(blockNumber: string): string {
  try {
    return Number(blockNumber).toLocaleString('ko-KR')
  } catch (error) {
    console.error('블록 번호 포맷팅 실패:', error)
    return '0'
  }
}

/**
 * 블록체인 API 클래스
 */
export class BlockchainApi {
  /**
   * 블록체인 네트워크 정보 조회
   * @returns 네트워크 정보
   */
  static async getNetworkInfo(): Promise<NetworkInfo> {
    try {
      console.log('🔗 블록체인 네트워크 정보 API 요청')

      const response = await apiClient.getPublic<ApiResponse<NetworkInfo>>(
        '/blockchain/test/network-info'
      )

      if (response.success && response.data) {
        console.log('✅ 블록체인 네트워크 정보 조회 성공:', response.data)
        return response.data
      } else {
        console.error('❌ 블록체인 네트워크 정보 조회 실패:', response.message)
        throw new Error(response.message || '네트워크 정보 조회에 실패했습니다')
      }
    } catch (error) {
      console.error('💥 블록체인 네트워크 정보 fetch 에러:', error)
      throw error
    }
  }

  /**
   * 캠페인의 블록체인 트랜잭션 목록 조회
   * @param campaignId 캠페인 ID
   * @param limit 조회할 트랜잭션 수 (기본값: 10)
   * @returns 트랜잭션 목록
   */
  static async getCampaignTransactions(
    campaignId: number,
    limit: number = 10
  ): Promise<import('@/types/donation').BlockchainTransactionListResponse> {
    try {
      console.log(`🔗 캠페인 ${campaignId}의 블록체인 트랜잭션 조회 API 요청`)

      const response = await apiClient.getPublic<ApiResponse<import('@/types/donation').BlockchainTransactionListResponse>>(
        `/blockchain/campaigns/${campaignId}/transactions?limit=${limit}`
      )

      if (response.success && response.data) {
        console.log('✅ 블록체인 트랜잭션 조회 성공:', response.data)
        return response.data
      } else {
        console.error('❌ 블록체인 트랜잭션 조회 실패:', response.message)
        throw new Error(response.message || '트랜잭션 조회에 실패했습니다')
      }
    } catch (error) {
      console.error('💥 블록체인 트랜잭션 fetch 에러:', error)
      throw error
    }
  }
}

export const blockchainApi = new BlockchainApi()

