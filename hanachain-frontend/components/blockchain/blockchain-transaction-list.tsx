"use client"

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ExternalLink, RefreshCw, Clock, User, CheckCircle2 } from 'lucide-react'
import { BlockchainTransaction } from '@/types/donation'
import { BlockchainApi } from '@/lib/api/blockchain-api'

interface BlockchainTransactionListProps {
  campaignId: number
  blockchainCampaignId?: string
  contractAddress?: string
  autoRefresh?: boolean
  refreshInterval?: number // 밀리초 단위
}

export function BlockchainTransactionList({
  campaignId,
  blockchainCampaignId,
  contractAddress,
  autoRefresh = false,
  refreshInterval = 30000 // 기본 30초
}: BlockchainTransactionListProps) {
  const [transactions, setTransactions] = useState<BlockchainTransaction[]>([])
  const [totalCount, setTotalCount] = useState(0)
  const [lastUpdated, setLastUpdated] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isRefreshing, setIsRefreshing] = useState(false)

  // 트랜잭션 로드 함수
  const loadTransactions = async (showRefreshAnimation = false) => {
    try {
      if (showRefreshAnimation) {
        setIsRefreshing(true)
      } else {
        setLoading(true)
      }
      setError(null)

      const response = await BlockchainApi.getCampaignTransactions(campaignId, 10)
      setTransactions(response.transactions)
      setTotalCount(response.totalCount)
      setLastUpdated(response.lastUpdated)
    } catch (err) {
      console.error('트랜잭션 로드 실패:', err)
      setError('트랜잭션 정보를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
      setIsRefreshing(false)
    }
  }

  // 초기 로드
  useEffect(() => {
    loadTransactions()
  }, [campaignId])

  // 자동 새로고침
  useEffect(() => {
    if (!autoRefresh) return

    const interval = setInterval(() => {
      loadTransactions(true)
    }, refreshInterval)

    return () => clearInterval(interval)
  }, [autoRefresh, refreshInterval, campaignId])

  // 수동 새로고침
  const handleRefresh = () => {
    loadTransactions(true)
  }

  // 이벤트 타입에 따른 뱃지 색상
  const getEventBadgeColor = (eventType: BlockchainTransaction['eventType']) => {
    switch (eventType) {
      case 'DonationMade':
        return 'bg-green-100 text-green-800'
      case 'CampaignCreated':
        return 'bg-blue-100 text-blue-800'
      case 'CampaignFinalized':
        return 'bg-purple-100 text-purple-800'
      case 'CampaignCancelled':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  // 이벤트 타입 한글 변환
  const getEventTypeLabel = (eventType: BlockchainTransaction['eventType']) => {
    switch (eventType) {
      case 'DonationMade':
        return '기부'
      case 'CampaignCreated':
        return '캠페인 생성'
      case 'CampaignFinalized':
        return '캠페인 종료'
      case 'CampaignCancelled':
        return '캠페인 취소'
      default:
        return eventType
    }
  }

  // 주소 축약 표시
  const shortenAddress = (address: string) => {
    if (!address || address.length < 10) return address
    return `${address.substring(0, 6)}...${address.substring(address.length - 4)}`
  }

  // 트랜잭션 해시 축약 표시
  const shortenHash = (hash: string) => {
    if (!hash || hash.length < 10) return hash
    return `${hash.substring(0, 10)}...${hash.substring(hash.length - 8)}`
  }

  // 블록체인 탐색기 URL 생성 (예: Etherscan)
  const getExplorerUrl = (hash: string) => {
    // 실제 환경에 맞게 수정 필요
    return `https://sepolia.etherscan.io/tx/${hash}`
  }

  // 시간 포맷팅
  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now.getTime() - date.getTime()

    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)

    if (minutes < 1) return '방금 전'
    if (minutes < 60) return `${minutes}분 전`
    if (hours < 24) return `${hours}시간 전`
    return `${days}일 전`
  }

  if (loading && !isRefreshing) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#009591]"></div>
            <p className="ml-3 text-gray-600">블록체인 트랜잭션을 불러오는 중...</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <p className="text-red-500 text-center">{error}</p>
          <div className="mt-4 text-center">
            <Button onClick={handleRefresh} variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" />
              다시 시도
            </Button>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-[#009591]" />
            <CardTitle className="text-lg">블록체인 트랜잭션</CardTitle>
            <Badge variant="secondary" className="ml-2">
              총 {totalCount}건
            </Badge>
          </div>
          <Button
            onClick={handleRefresh}
            variant="ghost"
            size="sm"
            disabled={isRefreshing}
            className="text-gray-600"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </Button>
        </div>

        {/* 블록체인 정보 */}
        <div className="mt-3 p-3 bg-gray-50 rounded-lg text-sm">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {blockchainCampaignId && (
              <div>
                <span className="text-gray-600">캠페인 ID:</span>
                <span className="ml-2 font-mono text-[#009591]">#{blockchainCampaignId}</span>
              </div>
            )}
            {contractAddress && (
              <div>
                <span className="text-gray-600">컨트랙트:</span>
                <span className="ml-2 font-mono text-xs">{shortenAddress(contractAddress)}</span>
              </div>
            )}
            {lastUpdated && (
              <div>
                <span className="text-gray-600">마지막 업데이트:</span>
                <span className="ml-2">{formatTime(lastUpdated)}</span>
              </div>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-6 pt-0">
        {transactions.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <p>아직 블록체인에 기록된 트랜잭션이 없습니다.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {transactions.map((tx, index) => (
              <div
                key={tx.transactionHash || index}
                className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                {/* 트랜잭션 헤더 */}
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Badge className={getEventBadgeColor(tx.eventType)}>
                      {getEventTypeLabel(tx.eventType)}
                    </Badge>
                    {tx.eventType === 'DonationMade' && tx.donorName && (
                      <span className="text-sm text-gray-700">
                        <User className="h-3 w-3 inline mr-1" />
                        {tx.donorName}
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-gray-500 flex items-center">
                    <Clock className="h-3 w-3 mr-1" />
                    {formatTime(tx.timestamp)}
                  </span>
                </div>

                {/* 트랜잭션 상세 정보 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
                  {tx.value && parseFloat(tx.value) > 0 && (
                    <div>
                      <span className="text-gray-600">금액:</span>
                      <span className="ml-2 font-semibold text-[#009591]">
                        {parseFloat(tx.value).toLocaleString()} USDC
                      </span>
                    </div>
                  )}
                  <div>
                    <span className="text-gray-600">블록:</span>
                    <span className="ml-2 font-mono">#{tx.blockNumber}</span>
                  </div>
                  <div className="col-span-1 md:col-span-2">
                    <span className="text-gray-600">TX Hash:</span>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="font-mono text-xs bg-gray-100 px-2 py-1 rounded">
                        {shortenHash(tx.transactionHash)}
                      </span>
                      <a
                        href={getExplorerUrl(tx.transactionHash)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-[#009591] hover:underline flex items-center text-xs"
                      >
                        <ExternalLink className="h-3 w-3 mr-1" />
                        Etherscan에서 보기
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 더 보기 버튼 (필요시) */}
        {transactions.length > 0 && totalCount > transactions.length && (
          <div className="mt-4 text-center">
            <Button variant="outline" size="sm">
              더 보기 ({totalCount - transactions.length}개 더)
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
