"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import {
  Heart,
  Users,
  PawPrintIcon as Paw,
  TreePine,
  Globe,
  Home,
  Stethoscope,
  ChevronLeft,
  ChevronRight,
  Baby,
} from "lucide-react"
import Link from "next/link"
import React, { useState, useEffect, useRef } from "react"
import CountUp from "react-countup"
import Footer from "@/components/footer"
import { CampaignList } from "@/components/campaigns/campaign-list"
import { campaignApi } from "@/lib/api/campaign-api"
import { CampaignListItem } from "@/types/donation"
import { formatCurrency } from "@/lib/utils"
import organizationApi, { Organization } from "@/lib/api/organization-api"
import { noticeApi } from "@/lib/api/notice-api"
import { NoticeListItem } from "@/types/notice"
import { BlockchainApi, NetworkInfo, formatBlockNumber } from "@/lib/api/blockchain-api"

export default function HanaChainPlatform() {
  const [currentSlide, setCurrentSlide] = useState(0)
  const [featuredCampaigns, setFeaturedCampaigns] = useState<CampaignListItem[]>([])
  const [campaignList, setCampaignList] = useState<CampaignListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [notices, setNotices] = useState<NoticeListItem[]>([])
  
  // CountUp 애니메이션을 위한 상태
  const [startCount, setStartCount] = useState(false)
  const statsRef = useRef<HTMLDivElement>(null)
  
  // 현재 시각 표시
  const [currentTime, setCurrentTime] = useState('')

  // 블록체인 네트워크 정보
  const [blockchainInfo, setBlockchainInfo] = useState<NetworkInfo | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  // 한국 시간 포맷팅 함수
  const formatKoreanTime = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hour = now.getHours()
    return `${year}.${month}.${day} ${hour}시 기준`
  }

  // 컴포넌트 마운트 시 현재 시각 설정
  useEffect(() => {
    setCurrentTime(formatKoreanTime())
  }, [])

  // 백엔드에서 캠페인 데이터 가져오기
  useEffect(() => {
    const fetchCampaigns = async () => {
      try {
        setLoading(true)
        // 인기 캠페인을 피처드로 사용 (4개)
        const popularResponse = await campaignApi.getPopularCampaigns({ page: 0, size: 4 })
        setFeaturedCampaigns(popularResponse.content)

        // 일반 캠페인 목록 (5개)
        const campaignsResponse = await campaignApi.getRecentCampaigns({ page: 0, size: 5 })
        setCampaignList(campaignsResponse.content)

        // 우수 단체 목록 (4개)
        try {
          const orgsResponse = await organizationApi.getAllOrganizations(0, 4, { status: 'ACTIVE' })
          setOrganizations(orgsResponse.content)
        } catch (orgErr) {
          console.error('단체 데이터 로딩 실패:', orgErr)
          // 단체 로딩 실패해도 캠페인은 보여줌
        }

        // 최근 공지사항 목록 (3개)
        try {
          const noticesResponse = await noticeApi.getRecentNotices(3)
          setNotices(noticesResponse)
        } catch (noticeErr) {
          console.error('공지사항 데이터 로딩 실패:', noticeErr)
          // 공지사항 로딩 실패해도 다른 데이터는 보여줌
        }
      } catch (err) {
        console.error('캠페인 데이터 로딩 실패:', err)
        setError('캠페인 정보를 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoading(false)
      }
    }

    fetchCampaigns()
  }, [])

  useEffect(() => {
    if (featuredCampaigns.length > 0) {
      const timer = setInterval(() => {
        setCurrentSlide((prev) => (prev + 1) % featuredCampaigns.length)
      }, 5000)
      return () => clearInterval(timer)
    }
  }, [featuredCampaigns.length])

  // Intersection Observer로 스크롤 감지
  useEffect(() => {
    // blockchainInfo가 로드되지 않았으면 아직 실제 UI가 없으므로 대기
    if (!blockchainInfo) return

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !startCount) {
            setStartCount(true)
          }
        })
      },
      {
        threshold: 0.5, // 50% 보일 때 트리거
      }
    )

    if (statsRef.current) {
      observer.observe(statsRef.current)
    }

    return () => {
      if (statsRef.current) {
        observer.unobserve(statsRef.current)
      }
    }
  }, [startCount, blockchainInfo])

  // 블록체인 네트워크 정보 가져오기 및 1분마다 갱신
  useEffect(() => {
    const fetchBlockchainInfo = async () => {
      try {
        const info = await BlockchainApi.getNetworkInfo()
        setBlockchainInfo(info)
        setLastUpdated(new Date())
      } catch (err) {
        console.warn('블록체인 정보 로딩 실패:', err)
        // 에러 발생 시 이전 데이터 유지
      }
    }

    // 초기 로드
    fetchBlockchainInfo()

    // 1분(60초)마다 갱신
    const interval = setInterval(fetchBlockchainInfo, 60000)

    return () => clearInterval(interval)
  }, [])

  // 상대 시간 표시 함수
  const getRelativeTime = (date: Date | null): string => {
    if (!date) return '-'
    
    const now = new Date()
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)
    
    if (diffInSeconds < 10) return '방금 전'
    if (diffInSeconds < 60) return `${diffInSeconds}초 전`
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}분 전`
    return `${Math.floor(diffInSeconds / 3600)}시간 전`
  }

  // 블록체인 익스플로러 URL 가져오기
  const getBlockchainExplorerUrl = (networkName: string): string => {
    const lowerName = networkName.toLowerCase()
    
    if (lowerName.includes('sepolia')) {
      return 'https://sepolia.etherscan.io/'
    } else if (lowerName.includes('mainnet') || lowerName.includes('ethereum')) {
      return 'https://etherscan.io/'
    } else if (lowerName.includes('polygon')) {
      return 'https://polygonscan.com/'
    } else if (lowerName.includes('mumbai')) {
      return 'https://mumbai.polygonscan.com/'
    }
    
    // 기본값: Sepolia
    return 'https://sepolia.etherscan.io/'
  }

  // 블록체인 상태 클릭 핸들러
  const handleBlockchainClick = () => {
    if (blockchainInfo) {
      const explorerUrl = getBlockchainExplorerUrl(blockchainInfo.networkName)
      window.open(explorerUrl, '_blank', 'noopener,noreferrer')
    }
  }

  const nextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % featuredCampaigns.length)
  }

  const prevSlide = () => {
    setCurrentSlide((prev) => (prev - 1 + featuredCampaigns.length) % featuredCampaigns.length)
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Grouped Hero Banners and Category Section */}
        <div className="flex flex-col lg:flex-row lg:items-stretch gap-6 mb-6">
          <div className="lg:flex-1">
            <Card className="bg-gradient-to-r from-[#28B2A5] to-[#009591] text-white overflow-hidden h-full relative py-[10px]">
              {/* 데코레이션 이미지 */}
              <img 
                src="/heart_hana_logo.png" 
                alt="decoration" 
                className="absolute top-[50%] right-4 w-28 h-28 object-contain z-10 pointer-events-none animate-float"
              />
              <CardContent className="px-6 h-full flex flex-col justify-between gap-0.5">
                <div className="flex items-start justify-start mb-2">
                  <div className="flex items-center gap-1 text-xs opacity-80">
                    <div className="w-4 h-4 rounded-full border border-white flex items-center justify-center">
                      <span className="text-[10px] font-semibold">i</span>
                    </div>
                    <span>광고</span>
                  </div>
                </div>
                <div className="flex flex-col items-center justify-center">
                  <p className="text-3xl font-semibold mr-40 text-center">쓸수록 기부되는</p>
                  <h2 className="text-4xl font-bold text-center">
                    <span className="text-6xl inline-block bg-[linear-gradient(135deg,_#7be5d5_0%,_#7be5d5_35%,_#2a8981_50%,_#5eead4_65%,_#7be5d5_100%)] bg-clip-text text-transparent animate-shine bg-[length:300%_300%]">
                      Hero
                    </span>
                    {" "}체크카드 출시
                  </h2>
                </div>
                <div className="flex flex-col items-center justify-center gap-3">
                  <img 
                    src="/hero_card.png" 
                    alt="Hero 체크카드" 
                    className="w-full max-w-md object-contain"
                  />
                  <a 
                    href="https://www.hanacard.co.kr/OPI41000000D.web?schID=pcd&mID=PI41006884P&CD_PD_SEQ=18643&"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-medium font-medium underline hover:opacity-80 transition-opacity"
                  >
                    하나카드 이벤트 바로가기
                  </a>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 우리가 함께 나눈 사랑의 기부 Card */}
          <div className="lg:flex-1">
            <Card className="bg-gradient-to-br from-[#4DC4B7] to-[#28B2A5] text-white overflow-hidden h-full">
              <CardContent className="p-6 py-1 h-full flex flex-col">
                <div className="flex items-start justify-between mb-0 flex-1">
                  <div className="flex-1 mt-8 ml-4">
                    <h3 className="text-4xl font-semibold mb-1">모두가 <br></br>하나되는</h3>
                    <h3 className="text-3xl font-semibold mb-1">사랑의 기부</h3>
                  </div>
                  <div className="flex items-center justify-center py-0 my-0">
                    <img
                      src="/byul_holding_lock.png"
                      alt="별돌이 캐릭터"
                      className="w-52 h-52 object-contain"
                    />
                  </div>
                </div>

                {blockchainInfo ? (
                  <div ref={statsRef} className="bg-white rounded-lg p-3 text-gray-900 mt-2 mb-2">
                    <div className="mb-2">
                      <span className="text-md font-medium text-gray-600">기부 현황</span>
                    </div>
                    
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      <div className="bg-gray-50 rounded-md p-2 flex items-center justify-center">
                        <p className="font-medium text-lg text-gray-800">
                          {startCount ? (
                            <CountUp end={91452603} duration={5} separator="," />
                          ) : (
                            "0"
                          )}
                          원
                        </p>
                      </div>
                      <div className="bg-gray-50 rounded-md p-2 flex items-center justify-center">
                        <p className="font-medium text-lg text-gray-800">
                          {startCount ? (
                            <CountUp end={68195} duration={3} separator="," />
                          ) : (
                            "0"
                          )}
                          건
                        </p>
                      </div>
                    </div>
                  </div>
                ) : (
                  /* 스켈레톤 로딩 UI */
                  <div className="bg-white rounded-lg p-3 text-gray-900 mt-2 mb-2 animate-pulse">
                    <div className="mb-2">
                      <span className="text-md font-medium text-transparent bg-gray-200 rounded">기부 현황</span>
                    </div>
                    
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      <div className="bg-gray-50 rounded-md p-2 flex items-center justify-center">
                        <p className="font-medium text-lg text-gray-800 text-transparent bg-gray-200 rounded">91,452,603원</p>
                      </div>
                      <div className="bg-gray-50 rounded-md p-2 flex items-center justify-center">
                        <p className="font-medium text-lg text-gray-800 text-transparent bg-gray-200 rounded">68,195건</p>
                      </div>
                    </div>
                  </div>
                )}

                {/* 블록체인 상태 정보 */}
                {blockchainInfo ? (
                  <div 
                    onClick={handleBlockchainClick}
                    className="bg-white rounded-lg p-3 text-gray-900 mt-2 cursor-pointer"
                    title="블록 익스플로러에서 확인하기"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-md font-medium text-gray-600">블록체인 상태</span>
                      <div className="flex items-center gap-2">
                        {/* 블록 체인 연결 애니메이션 */}
                        {blockchainInfo.connected ? (
                          <div className="flex items-center gap-0.5">
                            <div 
                              className="w-1.5 h-1.5 rounded-full bg-green-500 animate-bounce" 
                              style={{ animationDelay: '0ms', animationDuration: '1s' }}
                            />
                            <div className="w-1 h-px bg-green-500 animate-pulse" />
                            <div 
                              className="w-1.5 h-1.5 rounded-full bg-green-500 animate-bounce" 
                              style={{ animationDelay: '150ms', animationDuration: '1s' }}
                            />
                            <div className="w-1 h-px bg-green-500 animate-pulse" />
                            <div 
                              className="w-1.5 h-1.5 rounded-full bg-green-500 animate-bounce" 
                              style={{ animationDelay: '300ms', animationDuration: '1s' }}
                            />
                          </div>
                        ) : (
                          <div className="w-2 h-2 rounded-full bg-gray-400" />
                        )}
                        <span className={`text-xs font-medium ${blockchainInfo.connected ? 'text-green-600' : 'text-gray-500'}`}>
                          {blockchainInfo.connected ? '연결됨' : '오프라인'}
                        </span>
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      <div className="bg-gray-50 rounded-md p-2">
                        <span className="text-gray-500 block mb-0.5">네트워크</span>
                        <p className="font-medium text-gray-800 truncate">{blockchainInfo.networkName || '-'}</p>
                      </div>
                      <div className="bg-gray-50 rounded-md p-2">
                        <span className="text-gray-500 block mb-0.5">최신 블록</span>
                        <p className="font-medium text-gray-800">#{formatBlockNumber(blockchainInfo.blockNumber)}</p>
                      </div>
                      <div className="bg-gray-50 rounded-md p-2 col-span-2 flex items-center justify-between">
                        <div className="flex-1">
                          <span className="text-gray-500 block mb-0.5">업데이트</span>
                          <p className="font-medium text-gray-800">{getRelativeTime(lastUpdated)}</p>
                        </div>
                        <svg 
                          className="w-4 h-4 text-gray-400 ml-2" 
                          fill="none" 
                          stroke="currentColor" 
                          viewBox="0 0 24 24"
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                        </svg>
                      </div>
                    </div>
                  </div>
                ) : (
                  /* 스켈레톤 로딩 UI */
                  <div className="bg-white rounded-lg p-3 text-gray-900 mt-2 animate-pulse">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-md font-medium text-transparent bg-gray-200 rounded">블록체인 상태</span>
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-0.5">
                          <div className="w-1.5 h-1.5 rounded-full bg-gray-200" />
                          <div className="w-1 h-px bg-gray-200" />
                          <div className="w-1.5 h-1.5 rounded-full bg-gray-200" />
                          <div className="w-1 h-px bg-gray-200" />
                          <div className="w-1.5 h-1.5 rounded-full bg-gray-200" />
                        </div>
                        <span className="text-xs font-medium text-transparent bg-gray-200 rounded">연결됨</span>
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      <div className="bg-gray-50 rounded-md p-2">
                        <span className="text-gray-500 block mb-0.5 text-transparent bg-gray-200 rounded">네트워크</span>
                        <p className="font-medium text-gray-800 text-transparent bg-gray-200 rounded">Sepolia</p>
                      </div>
                      <div className="bg-gray-50 rounded-md p-2">
                        <span className="text-gray-500 block mb-0.5 text-transparent bg-gray-200 rounded">최신 블록</span>
                        <p className="font-medium text-gray-800 text-transparent bg-gray-200 rounded">#1,234,567</p>
                      </div>
                      <div className="bg-gray-50 rounded-md p-2 col-span-2 flex items-center justify-between">
                        <div className="flex-1">
                          <span className="text-gray-500 block mb-0.5 text-transparent bg-gray-200 rounded">업데이트</span>
                          <p className="font-medium text-gray-800 text-transparent bg-gray-200 rounded">방금 전</p>
                        </div>
                        <svg 
                          className="w-4 h-4 text-gray-200 ml-2" 
                          fill="none" 
                          stroke="currentColor" 
                          viewBox="0 0 24 24"
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                        </svg>
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Category Icons Section (moved from sidebar) */}
          <div className="lg:w-80 flex flex-col justify-between">
            <Card>
              <CardContent className="pl-6 pr-6 mb-4 flex flex-col">
                <h3 className="font-semibold text-xl">카테고리</h3>
                <div className="grid grid-cols-3 gap-6 pt-6">
                  <div className="text-center">
                    <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-2">
                      <Users className="h-6 w-6 text-blue-600" />
                    </div>
                    <span className="text-sm text-gray-600">취약계층</span>
                  </div>
                  <div className="text-center">
                    <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-2">
                      <Baby className="h-6 w-6 text-green-600" />
                    </div>
                    <span className="text-sm text-gray-600">아동청소년</span>
                  </div>
                  <div className="text-center">
                    <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-2">
                      <Heart className="h-6 w-6 text-purple-600" />
                    </div>
                    <span className="text-sm text-gray-600">시니어</span>
                  </div>
                  <div className="text-center">
                    <div className="w-12 h-12 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-2">
                      <Paw className="h-6 w-6 text-yellow-600" />
                    </div>
                    <span className="text-sm text-gray-600">동물보호</span>
                  </div>
                  <div className="text-center">
                    <div className="w-12 h-12 bg-teal-100 rounded-full flex items-center justify-center mx-auto mb-2">
                      <TreePine className="h-6 w-6 text-teal-600" />
                    </div>
                    <span className="text-sm text-gray-600">환경</span>
                  </div>
                  <div className="text-center">
                    <div className="w-12 h-12 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-2">
                      <Globe className="h-6 w-6 text-indigo-600" />
                    </div>
                    <span className="text-sm text-gray-600">해외</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Featured Organizations */}
            <Card>
              <CardContent className="pl-0 pr-0 mb-1">
                <h3 className="font-semibold text-xl pl-6 pr-6 pb-2">우수 단체</h3>
                
                {loading ? (
                  /* 스켈레톤 로딩 UI - 실제 카드 높이와 동일하게 */
                  <div className="relative overflow-hidden pt-4 pb-4 px-6">
                    <div className="flex gap-6 animate-pulse">
                      {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="flex-shrink-0 w-20">
                          <div className="flex flex-col items-center space-y-2">
                            {/* 원형 로고 스켈레톤 */}
                            <div className="w-20 h-20 rounded-full bg-gray-200" />
                            {/* 단체 이름 스켈레톤 - 2줄 */}
                            <div className="w-full space-y-1">
                              <div className="h-3 bg-gray-200 rounded w-full" />
                              <div className="h-3 bg-gray-200 rounded w-3/4 mx-auto" />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : organizations.length === 0 ? (
                  <div className="flex items-center justify-center py-8">
                    <p className="text-sm text-gray-500">등록된 단체가 없습니다</p>
                  </div>
                ) : (
                  <div className="relative overflow-hidden pt-4 pb-4 fade-edges">
                    {/* 무한 스크롤 컨테이너 */}
                    <div className="flex gap-2 animate-scroll-infinite">
                      {/* 원본 단체들 */}
                      {organizations.map((org) => (
                        <Link 
                          key={`org-${org.id}`}
                          href={`/campaigns?keyword=${encodeURIComponent(org.name)}`}
                          className="flex-shrink-0 w-23"
                        >
                          <div className="flex flex-col items-center space-y-2 cursor-pointer hover:opacity-80 transition-opacity">
                            {/* 원형 로고 */}
                            <div className="w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center overflow-hidden border-2 border-gray-200">
                              {org.imageUrl ? (
                                <img 
                                  src={org.imageUrl} 
                                  alt={org.name}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <span className="text-2xl font-bold text-[#009591]">
                                  {org.name.charAt(0)}
                                </span>
                              )}
                            </div>
                            {/* 단체 이름 */}
                            <p className="text-xs text-center font-medium text-gray-700 line-clamp-2 w-full">
                              {org.name}
                            </p>
                          </div>
                        </Link>
                      ))}
                      {/* 복제된 단체들 (끊김없는 효과) */}
                      {organizations.map((org) => (
                        <Link 
                          key={`org-duplicate-${org.id}`}
                          href={`/campaigns?keyword=${encodeURIComponent(org.name)}`}
                          className="flex-shrink-0 w-20"
                          aria-hidden="true"
                        >
                          <div className="flex flex-col items-center space-y-2 cursor-pointer hover:opacity-80 transition-opacity">
                            {/* 원형 로고 */}
                            <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center overflow-hidden border-2 border-gray-200">
                              {org.imageUrl ? (
                                <img 
                                  src={org.imageUrl} 
                                  alt={org.name}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <span className="text-2xl font-bold text-[#009591]">
                                  {org.name.charAt(0)}
                                </span>
                              )}
                            </div>
                            {/* 단체 이름 */}
                            <p className="text-xs text-center font-medium text-gray-700 line-clamp-2 w-full">
                              {org.name}
                            </p>
                          </div>
                        </Link>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>

        <div className="flex flex-col lg:flex-row gap-6">
          {/* Main Content */}
          <div className="lg:flex-1 space-y-6">
          {/* Featured Campaign Carousel */}
          <div className="relative overflow-hidden rounded-lg">
            {loading ? (
              /* 캐러셀 스켈레톤 UI - 실제 카드 구조 반영 */
              <Card className="overflow-hidden w-full h-80 border-0 rounded-lg p-0 animate-pulse">
                <div className="relative w-full h-full bg-gray-200">
                  {/* 슬라이드 번호 스켈레톤 (우측 상단) */}
                  <div className="absolute top-4 right-4 bg-gray-300 rounded-full w-16 h-8" />
                  
                  {/* 하단 정보 스켈레톤 */}
                  <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-gray-300/90 via-gray-300/60 to-transparent">
                    {/* 제목 스켈레톤 */}
                    <div className="space-y-2 mb-3">
                      <div className="h-6 bg-gray-400 rounded w-3/4" />
                      <div className="h-6 bg-gray-400 rounded w-1/2" />
                    </div>
                    
                    {/* 진행률 바와 모금액 스켈레톤 */}
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="h-4 bg-gray-400 rounded w-12" />
                        <div className="h-5 bg-gray-400 rounded w-24" />
                      </div>
                      <div className="h-2 bg-gray-400 rounded w-full" />
                    </div>
                  </div>
                </div>
              </Card>
            ) : error ? (
              <div className="flex items-center justify-center h-80 bg-red-50 rounded-lg">
                <p className="text-red-500">{error}</p>
              </div>
            ) : featuredCampaigns.length > 0 ? (
              <>
                <div
                  className="flex transition-transform duration-500 ease-in-out h-80"
                  style={{ transform: `translateX(-${currentSlide * 100}%)` }}
                >
                  {featuredCampaigns.map((campaign, index) => (
                    <div key={campaign.id} className="w-full h-full flex-shrink-0">
                      <Link href={`/campaign/${campaign.id}`} className="block w-full h-full">
                        <Card className="overflow-hidden cursor-pointer hover:shadow-lg transition-shadow w-full h-full border-0 rounded-lg p-0">
                          <div className="relative w-full h-full">
                            <img
                              src={campaign.imageUrl || "/placeholder.svg"}
                              alt={campaign.title}
                              className="w-full h-full object-cover object-center"
                            />
                            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent"></div>
                            
                            {/* 슬라이드 번호 표시 (우측 상단) */}
                            <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-sm rounded-full px-3 py-1.5">
                              <span className="text-white text-sm font-medium">
                                {currentSlide + 1}/{featuredCampaigns.length}
                              </span>
                            </div>
                            
                            <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/90 via-black/60 to-transparent">
                              <h2 className="text-xl font-bold text-white mb-3 drop-shadow-lg">{campaign.title}</h2>
                              
                              {/* 진행률 바와 모금액 */}
                              <div className="space-y-2">
                                <div className="flex items-center justify-between text-sm">
                                  <span className="text-white/90 font-medium">{campaign.progressPercentage.toFixed(1)}%</span>
                                  <span className="text-white font-semibold text-lg">{formatCurrency(campaign.currentAmount)}</span>
                                </div>
                                <Progress value={campaign.progressPercentage} className="h-2 bg-white/20" />
                              </div>
                            </div>
                          </div>
                        </Card>
                      </Link>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className="flex items-center justify-center h-80 bg-gray-100 rounded-lg">
                <p className="text-gray-500">표시할 캠페인이 없습니다</p>
              </div>
            )}
            
            {featuredCampaigns.length > 1 && (
              <>
                <button
                  onClick={prevSlide}
                  className="absolute left-4 top-1/2 -translate-y-1/2 bg-white/40 hover:bg-white/60 rounded-full p-2 shadow-lg transition-all z-10 backdrop-blur-sm"
                >
                  <ChevronLeft className="h-5 w-5 text-gray-700" />
                </button>
                <button
                  onClick={nextSlide}
                  className="absolute right-4 top-1/2 -translate-y-1/2 bg-white/40 hover:bg-white/60 rounded-full p-2 shadow-lg transition-all z-10 backdrop-blur-sm"
                >
                  <ChevronRight className="h-5 w-5 text-gray-700" />
                </button>
              </>
            )}
          </div>
          {/* Recent Campaigns */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">최신 캠페인</h3>
              <Link href="/campaigns" className="text-[#009591] hover:underline text-sm">
                전체 보기 →
              </Link>
            </div>
            
            <CampaignList 
              title=""
              showFilters={false}
              defaultFilters={{ sort: 'recent', size: 3 }}
              pageSize={3}
              gridClassName="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
              className="mb-0"
            />
          </div>
          </div>

          {/* Sidebar - 공지사항 */}
          <div className="lg:w-80 space-y-6">
            <Card>
              <CardContent className="pl-6 pr-6 pb-6">
                <h3 className="font-semibold text-xl mb-4">공지사항</h3>
                
                {loading ? (
                  /* 공지사항 스켈레톤 UI - 실제 아이템 구조 반영 */
                  <div className="space-y-3 animate-pulse">
                    {[1, 2, 3].map((i) => (
                      <div 
                        key={i}
                        className="border-b last:border-0 pb-3 last:pb-0 p-2 rounded"
                      >
                        <div className="flex items-start justify-between gap-2">
                          {/* 제목 스켈레톤 */}
                          <div className="h-4 bg-gray-200 rounded flex-1" />
                        </div>
                        {/* 날짜 스켈레톤 */}
                        <div className="h-3 bg-gray-200 rounded w-24 mt-1" />
                      </div>
                    ))}
                  </div>
                ) : notices.length === 0 ? (
                  <div className="flex items-center justify-center py-8">
                    <p className="text-sm text-gray-500">등록된 공지사항이 없습니다</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {notices.map((notice) => (
                      <div 
                        key={notice.id}
                        className="border-b last:border-0 pb-3 last:pb-0 cursor-pointer hover:bg-gray-50 p-2 rounded transition-colors"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <h4 className="text-sm font-medium text-gray-900 line-clamp-1 flex-1">
                            {notice.isImportant && (
                              <span className="text-red-500 mr-1">●</span>
                            )}
                            {notice.title}
                          </h4>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          {new Date(notice.createdAt).toLocaleDateString('ko-KR', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit'
                          })}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
        
        <Footer />
      </div>
    </div>
  )
}
