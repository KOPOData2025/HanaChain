'use client'

import { useRouter } from 'next/navigation'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AlertCircle, CheckCircle2 } from 'lucide-react'
import { CampaignListItem } from '@/types/donation'

interface BatchRequiredCampaignsProps {
  campaigns: CampaignListItem[]
}

export function BatchRequiredCampaigns({ campaigns }: BatchRequiredCampaignsProps) {
  const router = useRouter()

  // 배치 작업이 필요한 캠페인 필터링
  const batchRequiredCampaigns = campaigns.filter(campaign => {
    // COMPLETED 상태이고 배치 작업이 완료되지 않은 캠페인
    return campaign.status === 'COMPLETED' &&
           (!campaign.batchJobExecutionId || campaign.batchJobStatus === 'PENDING')
  })

  const handleCampaignClick = (campaignId: number) => {
    router.push(`/admin/campaigns/${campaignId}`)
  }

  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
    }).format(amount)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold flex items-center gap-2">
          {batchRequiredCampaigns.length === 0 ? (
            <>
              <CheckCircle2 className="h-5 w-5 text-green-600" />
              블록체인 기록 상태
            </>
          ) : (
            <>
              <AlertCircle className="h-5 w-5 text-amber-600" />
              블록체인 기록 작업 필요 캠페인
            </>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {batchRequiredCampaigns.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="mb-4 p-3 bg-green-50 dark:bg-green-900/20 rounded-full">
              <CheckCircle2 className="h-12 w-12 text-green-600 dark:text-green-400" />
            </div>
            <p className="text-lg font-medium text-gray-900 dark:text-white mb-1">
              모든 캠페인이 On-Chain 상에 기록되어 있습니다
            </p>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              블록체인에 기록되지 않은 캠페인이 없습니다
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {batchRequiredCampaigns.map((campaign) => (
                <div
                  key={campaign.id}
                  onClick={() => handleCampaignClick(campaign.id)}
                  className="p-4 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors cursor-pointer"
                >
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-gray-900 dark:text-white truncate mb-2">
                      {campaign.title}
                    </h4>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">목표 금액:</span>
                        <span className="ml-2 font-medium text-gray-900 dark:text-white">
                          {formatAmount(campaign.targetAmount)}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">현재 금액:</span>
                        <span className="ml-2 font-medium text-teal-600 dark:text-teal-400">
                          {formatAmount(campaign.currentAmount)}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">달성률:</span>
                        <span className="ml-2 font-medium text-gray-900 dark:text-white">
                          {campaign.progressPercentage.toFixed(1)}%
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">기부자:</span>
                        <span className="ml-2 font-medium text-gray-900 dark:text-white">
                          {campaign.donorCount}명
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
