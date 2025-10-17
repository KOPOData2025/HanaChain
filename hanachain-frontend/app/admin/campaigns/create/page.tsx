'use client'

import { AdminCampaignCreateForm } from '@/components/admin/campaign-create-form'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import Link from 'next/link'

export default function AdminCampaignCreatePage() {


  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        {/* 헤더 */}
        <div className="mb-8">
          {/* 뒤로가기 버튼 */}
          <div className="mb-4">
            <Link href="/admin/campaigns">
              <Button variant="ghost" size="sm" className="flex items-center gap-2">
                <ArrowLeft className="h-4 w-4" />
                캠페인 목록으로 돌아가기
              </Button>
            </Link>
          </div>

          {/* 페이지 제목 */}
          <div className="space-y-2">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              새 캠페인 등록
            </h1>
            <p className="text-gray-600 dark:text-gray-400">
              새로운 기부 캠페인을 등록하고 관리하세요. 모든 필수 정보를 정확히 입력해주세요.
            </p>
          </div>
        </div>

        {/* 캠페인 등록 폼 */}
        <AdminCampaignCreateForm />

        {/* 도움말 섹션 */}
        <div className="mt-12 p-6 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
          <h3 className="text-lg font-semibold text-blue-900 dark:text-blue-100 mb-4">
            💡 캠페인 등록 가이드
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm text-blue-800 dark:text-blue-200">
            <div>
              <h4 className="font-semibold mb-2">기본 정보 작성 시 주의사항</h4>
              <ul className="space-y-1 list-disc list-inside">
                <li>캠페인 제목은 명확하고 이해하기 쉽게 작성하세요</li>
                <li>설명은 캠페인의 목적과 배경을 상세히 포함하세요</li>
                <li>목표 금액은 실제 필요한 금액을 현실적으로 설정하세요</li>
                <li>카테고리는 캠페인 성격에 가장 적합한 것을 선택하세요</li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold mb-2">일정 설정 가이드</h4>
              <ul className="space-y-1 list-disc list-inside">
                <li>시작일은 충분한 준비 기간을 고려하여 설정하세요</li>
                <li>캠페인 기간은 너무 짧지도 길지도 않게 적절히 설정하세요</li>
                <li>종료일은 목표 달성을 위한 충분한 시간을 확보하세요</li>
                <li>공휴일이나 특별한 날짜를 고려하여 일정을 계획하세요</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}