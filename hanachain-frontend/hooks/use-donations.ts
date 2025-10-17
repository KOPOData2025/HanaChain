"use client"

import { useState, useCallback, useEffect } from "react"
import { DonationRecord, DonationListParams, DonationListResponse, DonationStats } from "@/types/donation"
import { 
  getMyDonations, 
  getMyDonationStats, 
  getDonationById as getDonationByIdApi, 
  DonationApiResponse, 
  DonationStatsResponse 
} from "@/lib/api/donation-api"

// API 응답을 프론트엔드 타입으로 변환
function mapApiResponseToDonationRecord(apiResponse: DonationApiResponse): DonationRecord {
  console.log('🔍 Mapping API response object:', apiResponse)
  console.log('🔍 API response type:', typeof apiResponse)
  
  if (!apiResponse) {
    console.error('❌ API response is null or undefined')
    throw new Error('API response is null or undefined')
  }
  
  console.log('🔍 API response keys:', Object.keys(apiResponse))
  console.log('🔍 ID field check - apiResponse.id:', apiResponse.id)
  console.log('🔍 Campaign ID:', apiResponse.campaignId)
  console.log('🔍 Campaign Title:', apiResponse.campaignTitle)
  
  if (apiResponse.id === undefined || apiResponse.id === null) {
    console.error('❌ API response missing id field. Full object:', apiResponse)
    console.error('❌ Available fields:', Object.keys(apiResponse))
    throw new Error('API response missing id field')
  }
  
  if (!apiResponse.campaignId || !apiResponse.campaignTitle) {
    console.error('❌ API response missing campaign fields. campaignId:', apiResponse.campaignId, 'campaignTitle:', apiResponse.campaignTitle)
    throw new Error('API response missing campaign fields')
  }
  // 결제 상태 매핑 (백엔드에서 이미 변환된 값 사용)
  const status = apiResponse.status

  // 결제 방법 매핑 (백엔드에서 이미 변환된 값 사용)
  const paymentMethod = apiResponse.paymentMethod

  return {
    id: apiResponse.id,
    campaignId: apiResponse.campaignId,
    campaignTitle: apiResponse.campaignTitle,
    campaignImage: apiResponse.campaignImage,
    amount: apiResponse.amount,
    status,
    donatedAt: new Date(apiResponse.donatedAt),
    message: apiResponse.message,
    paymentMethod,
    receiptNumber: apiResponse.receiptNumber
  }
}

export function useDonations() {
  const [donations, setDonations] = useState<DonationRecord[]>([])
  const [total, setTotal] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchDonations = useCallback(async (params: DonationListParams = {}) => {
    setIsLoading(true)
    setError(null)

    try {
      // 백엔드 API 호출
      const response = await getMyDonations({
        page: (params.page || 1) - 1, // 백엔드는 0부터 시작
        size: params.limit || 10,
        sort: params.sortBy === 'amount' ? 'amount' : 'createdAt',
        direction: params.sortOrder || 'desc'
      })

      console.log('🔍 Full API response received:', response)
      console.log('🔍 Response keys:', Object.keys(response))
      console.log('🔍 Response content exists?', !!response.content)
      console.log('🔍 Response content type:', typeof response.content)
      console.log('🔍 Response content is array?', Array.isArray(response.content))
      
      if (!response.content) {
        console.error('❌ No content in response. Full response:', response)
        setDonations([])
        setTotal(0)
        return {
          donations: [],
          total: 0,
          page: params.page || 1,
          limit: params.limit || 10,
          hasNext: false
        } as DonationListResponse
      }
      
      console.log('🔍 Content array length:', response.content.length)
      if (response.content.length > 0) {
        console.log('🔍 First content item:', response.content[0])
        console.log('🔍 First content item keys:', Object.keys(response.content[0] || {}))
      }
      
      // API 응답을 프론트엔드 타입으로 변환
      const mappedDonations = response.content
        .filter((item: any) => {
          if (!item) {
            console.warn('⚠️ Skipping null/undefined item in content array')
            return false
          }
          return true
        })
        .map((item: any, index: number) => {
          console.log(`🔍 Mapping item ${index}:`, item)
          try {
            return mapApiResponseToDonationRecord(item)
          } catch (error) {
            console.error(`❌ Failed to map item ${index}:`, error, item)
            return null
          }
        })
        .filter((item: any) => item !== null)
      
      // 프론트엔드 필터링 (상태, 검색어)
      let filteredDonations = mappedDonations
      
      // 상태별 필터링
      if (params.status && params.status !== 'all') {
        filteredDonations = filteredDonations.filter(d => d.status === params.status)
      }

      // 검색 필터링
      if (params.search) {
        const searchTerm = params.search.toLowerCase()
        filteredDonations = filteredDonations.filter(d => 
          d.campaignTitle.toLowerCase().includes(searchTerm) ||
          d.message?.toLowerCase().includes(searchTerm)
        )
      }

      setDonations(filteredDonations)
      setTotal(response.totalElements)

      return {
        donations: filteredDonations,
        total: response.totalElements,
        page: params.page || 1,
        limit: params.limit || 10,
        hasNext: !response.last
      } as DonationListResponse

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '기부 내역을 불러오는데 실패했습니다.'
      setError(errorMessage)
      
      // 인증 오류 시 로그인 페이지로 리디렉션 필요한 경우
      if (errorMessage.includes('로그인') || errorMessage.includes('인증')) {
        // window.location.href = '/login'
      }
      
      // 개발 환경에서 임시로 빈 배열 반환
      setDonations([])
      setTotal(0)
      
      return {
        donations: [],
        total: 0,
        page: params.page || 1,
        limit: params.limit || 10,
        hasNext: false
      } as DonationListResponse
    } finally {
      setIsLoading(false)
    }
  }, [])

  const getDonationStats = useCallback(async (): Promise<DonationStats> => {
    try {
      const stats = await getMyDonationStats()
      return {
        totalAmount: stats.totalAmount,
        totalCount: stats.totalCount,
        completedCount: stats.completedCount,
        pendingCount: stats.pendingCount,
        failedCount: stats.failedCount
      }
    } catch (err) {
      console.error('Failed to fetch donation stats:', err)
      // 에러 시 기본값 반환
      return {
        totalAmount: 0,
        totalCount: 0,
        completedCount: 0,
        pendingCount: 0,
        failedCount: 0
      }
    }
  }, [])

  const getDonationById = useCallback(async (id: string): Promise<DonationRecord | undefined> => {
    try {
      const apiResponse = await getDonationByIdApi(id)
      return mapApiResponseToDonationRecord(apiResponse)
    } catch (err) {
      console.error('Failed to fetch donation by id:', err)
      return undefined
    }
  }, [])

  return {
    donations,
    total,
    isLoading,
    error,
    fetchDonations,
    getDonationStats,
    getDonationById,
    clearError: () => setError(null)
  }
}