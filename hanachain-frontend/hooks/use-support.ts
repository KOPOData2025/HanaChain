"use client"

import { useState, useCallback } from "react"
import { Notice, FAQ, Inquiry, InquiryCreateRequest, SupportSearchParams } from "@/types/support"

// Mock 데이터
const MOCK_NOTICES: Notice[] = [
  {
    id: "notice_001",
    title: "서비스 정기 점검 안내",
    content: "안녕하세요, HanaChain입니다.\n\n서비스 품질 향상을 위해 정기 점검을 실시할 예정입니다.\n\n• 점검 일시: 2024년 12월 15일(일) 오전 2시 ~ 오전 6시\n• 점검 내용: 서버 안정성 개선 및 신규 기능 업데이트\n\n점검 중에는 서비스 이용이 일시적으로 제한될 수 있습니다.\n이용에 불편을 드려 죄송합니다.",
    category: "maintenance",
    isImportant: true,
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    views: 1542
  },
  {
    id: "notice_002",
    title: "연말 기부 캠페인 이벤트 오픈!",
    content: "따뜻한 연말을 맞아 특별한 기부 캠페인 이벤트를 준비했습니다.\n\n• 이벤트 기간: 2024년 12월 1일 ~ 12월 31일\n• 참여 방법: 캠페인 기부 시 자동 참여\n• 혜택: 기부 금액의 5% 적립금 지급\n\n많은 참여 부탁드립니다!",
    category: "event",
    isImportant: false,
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
    views: 2156
  },
  {
    id: "notice_003",
    title: "개인정보 처리방침 변경 안내",
    content: "개인정보보호법 개정에 따라 개인정보 처리방침이 일부 변경됩니다.\n\n• 시행일: 2024년 12월 10일\n• 주요 변경사항: 개인정보 보유기간 명시, 제3자 제공 내용 구체화\n\n자세한 내용은 홈페이지를 참조해주세요.",
    category: "general",
    isImportant: false,
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
    views: 856
  }
]

const MOCK_FAQS: FAQ[] = [
  {
    id: "faq_001",
    question: "기부는 어떻게 하나요?",
    answer: "기부는 다음과 같은 방법으로 할 수 있습니다:\n\n1. 홈페이지에서 원하는 캠페인을 선택합니다\n2. '기부하기' 버튼을 클릭합니다\n3. 기부 금액과 결제 방법을 선택합니다\n4. 결제를 완료하면 기부가 완료됩니다\n\n신용카드, 계좌이체, 네이버페이, 카카오페이 등 다양한 결제 방법을 지원합니다.",
    category: "donation",
    tags: ["기부방법", "결제", "캠페인"],
    isPopular: true,
    helpfulCount: 245,
    createdAt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000)
  },
  {
    id: "faq_002",
    question: "기부 내역은 어디서 확인할 수 있나요?",
    answer: "기부 내역은 마이페이지에서 확인하실 수 있습니다:\n\n1. 로그인 후 마이페이지로 이동합니다\n2. '기부내역' 탭을 클릭합니다\n3. 모든 기부 내역을 상태별로 확인할 수 있습니다\n\n기부 완료, 진행중, 실패 등 상태별로 필터링도 가능합니다.",
    category: "account",
    tags: ["기부내역", "마이페이지", "확인"],
    isPopular: true,
    helpfulCount: 189,
    createdAt: new Date(Date.now() - 25 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 8 * 24 * 60 * 60 * 1000)
  },
  {
    id: "faq_003",
    question: "기부 영수증은 어떻게 받나요?",
    answer: "기부 영수증은 기부 완료 후 자동으로 발급됩니다:\n\n1. 기부 완료 시 등록된 이메일로 영수증이 발송됩니다\n2. 마이페이지 > 기부내역에서 '영수증 다운로드' 버튼으로 다운로드 가능합니다\n3. 연말정산용 기부금 영수증도 별도로 발급 가능합니다\n\n기부금 영수증 관련 문의사항은 고객센터로 연락주세요.",
    category: "donation",
    tags: ["영수증", "다운로드", "연말정산"],
    isPopular: true,
    helpfulCount: 167,
    createdAt: new Date(Date.now() - 20 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000)
  },
  {
    id: "faq_004",
    question: "비밀번호를 잊어버렸어요",
    answer: "비밀번호를 잊어버리신 경우 다음과 같이 재설정하실 수 있습니다:\n\n1. 로그인 페이지에서 '비밀번호 찾기'를 클릭합니다\n2. 가입 시 등록한 이메일을 입력합니다\n3. 이메일로 발송된 링크를 클릭하여 새 비밀번호를 설정합니다\n\n이메일이 오지 않는 경우 스팸함도 확인해보세요.",
    category: "account",
    tags: ["비밀번호", "찾기", "재설정"],
    isPopular: false,
    helpfulCount: 89,
    createdAt: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000)
  }
]

const MOCK_INQUIRIES: Inquiry[] = [
  {
    id: "inquiry_001",
    userId: "user_001",
    title: "기부 취소 문의",
    content: "어제 실수로 잘못된 캠페인에 기부했습니다. 취소 가능한가요?",
    category: "donation",
    status: "resolved",
    priority: "normal",
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000),
    responses: [
      {
        id: "response_001",
        inquiryId: "inquiry_001",
        content: "안녕하세요. 기부 취소 관련 문의를 주셔서 감사합니다.\n\n기부 후 24시간 이내에는 취소가 가능합니다. 해당 기부 건에 대해 취소 처리해드리겠습니다.\n\n추가 문의사항이 있으시면 언제든 연락주세요.",
        isStaff: true,
        authorName: "고객지원팀",
        createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000)
      }
    ]
  }
]

export function useSupport() {
  const [notices, setNotices] = useState<Notice[]>([])
  const [faqs, setFaqs] = useState<FAQ[]>([])
  const [inquiries, setInquiries] = useState<Inquiry[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 공지사항 조회
  const fetchNotices = useCallback(async (params: SupportSearchParams = {}) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      let filtered = [...MOCK_NOTICES]
      
      if (params.query) {
        const query = params.query.toLowerCase()
        filtered = filtered.filter(notice => 
          notice.title.toLowerCase().includes(query) ||
          notice.content.toLowerCase().includes(query)
        )
      }

      if (params.category) {
        filtered = filtered.filter(notice => notice.category === params.category)
      }

      // 중요 공지사항 우선, 그 다음 최신순
      filtered.sort((a, b) => {
        if (a.isImportant && !b.isImportant) return -1
        if (!a.isImportant && b.isImportant) return 1
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })

      setNotices(filtered)
      return filtered
    } catch (err) {
      setError('공지사항을 불러오는데 실패했습니다.')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // FAQ 조회
  const fetchFAQs = useCallback(async (params: SupportSearchParams = {}) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      let filtered = [...MOCK_FAQS]

      if (params.query) {
        const query = params.query.toLowerCase()
        filtered = filtered.filter(faq => 
          faq.question.toLowerCase().includes(query) ||
          faq.answer.toLowerCase().includes(query) ||
          faq.tags.some(tag => tag.toLowerCase().includes(query))
        )
      }

      if (params.category) {
        filtered = filtered.filter(faq => faq.category === params.category)
      }

      // 인기 FAQ 우선, 그 다음 도움이 된 순
      filtered.sort((a, b) => {
        if (a.isPopular && !b.isPopular) return -1
        if (!a.isPopular && b.isPopular) return 1
        return b.helpfulCount - a.helpfulCount
      })

      setFaqs(filtered)
      return filtered
    } catch (err) {
      setError('FAQ를 불러오는데 실패했습니다.')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 문의 내역 조회
  const fetchInquiries = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 500))

      // 최신순 정렬
      const sorted = [...MOCK_INQUIRIES].sort((a, b) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      )

      setInquiries(sorted)
      return sorted
    } catch (err) {
      setError('문의 내역을 불러오는데 실패했습니다.')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // 문의 등록
  const createInquiry = useCallback(async (inquiryData: InquiryCreateRequest) => {
    setIsLoading(true)
    setError(null)

    try {
      await new Promise(resolve => setTimeout(resolve, 1000))

      const newInquiry: Inquiry = {
        id: `inquiry_${Date.now()}`,
        userId: "user_001",
        title: inquiryData.title,
        content: inquiryData.content,
        category: inquiryData.category,
        status: "pending",
        priority: "normal",
        createdAt: new Date(),
        updatedAt: new Date(),
        responses: []
      }

      setInquiries(prev => [newInquiry, ...prev])
      return newInquiry
    } catch (err) {
      setError('문의 등록에 실패했습니다.')
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  // FAQ 도움됨 표시
  const markFAQHelpful = useCallback(async (faqId: string) => {
    try {
      await new Promise(resolve => setTimeout(resolve, 300))

      setFaqs(prev => prev.map(faq => 
        faq.id === faqId 
          ? { ...faq, helpfulCount: faq.helpfulCount + 1 }
          : faq
      ))
    } catch (err) {
      setError('처리 중 오류가 발생했습니다.')
    }
  }, [])

  return {
    notices,
    faqs,
    inquiries,
    isLoading,
    error,
    fetchNotices,
    fetchFAQs,
    fetchInquiries,
    createInquiry,
    markFAQHelpful,
    clearError: () => setError(null)
  }
}
