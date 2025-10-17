'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { TrendingUp, Loader2 } from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { adminDonationApi } from '@/lib/api/admin-donation-api'
import { DonationTrendData } from '@/types/admin'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'

type PeriodType = '7d' | '30d'

const PERIOD_LABELS: Record<PeriodType, string> = {
  '7d': '7일',
  '30d': '30일'
}

export function DonationTrendsChart() {
  const [period, setPeriod] = useState<PeriodType>('7d')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [trendData, setTrendData] = useState<DonationTrendData[]>([])
  const [totalAmount, setTotalAmount] = useState(0)
  const [averageAmount, setAverageAmount] = useState(0)

  useEffect(() => {
    fetchTrendData()
  }, [period])

  const fetchTrendData = async () => {
    setLoading(true)
    setError(null)
    try {
      console.log('📊 [DonationTrendsChart] Fetching trends with period:', period)
      const response = await adminDonationApi.getDonationTrends(period)
      console.log('📊 [DonationTrendsChart] Response received:', response)
      setTrendData(response.data)
      setTotalAmount(response.totalAmount)
      setAverageAmount(response.averageAmount)
    } catch (err) {
      console.error('❌ [DonationTrendsChart] Failed to fetch donation trends:', err)
      setError('기부 금액 추이를 불러오는데 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const formatAmount = (value: number) => {
    if (value >= 100000000) {
      return `${(value / 100000000).toFixed(1)}억`
    }
    if (value >= 10000) {
      return `${(value / 10000).toFixed(0)}만`
    }
    return value.toLocaleString('ko-KR')
  }

  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr)
      if (period === '7d' || period === '30d') {
        return format(date, 'M/d', { locale: ko })
      }
      return format(date, 'M월', { locale: ko })
    } catch {
      return dateStr
    }
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div className="flex items-center gap-2">
          <TrendingUp className="h-5 w-5 text-teal-600" />
          <CardTitle className="text-xl font-semibold">기부 금액 추이</CardTitle>
        </div>
        <div className="flex gap-2">
          {(Object.keys(PERIOD_LABELS) as PeriodType[]).map((p) => (
            <Button
              key={p}
              variant={period === p ? 'default' : 'outline'}
              size="sm"
              onClick={() => setPeriod(p)}
              disabled={loading}
            >
              {PERIOD_LABELS[p]}
            </Button>
          ))}
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="h-[300px] flex items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-teal-600" />
          </div>
        ) : error ? (
          <div className="h-[300px] flex items-center justify-center">
            <p className="text-sm text-red-500">{error}</p>
          </div>
        ) : trendData.length === 0 ? (
          <div className="h-[300px] flex items-center justify-center">
            <p className="text-sm text-gray-500">기부 데이터가 없습니다.</p>
          </div>
        ) : (
          <>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart
                data={trendData}
                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
              >
                <defs>
                  <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0d9488" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="#0d9488" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" className="stroke-gray-200 dark:stroke-gray-700" />
                <XAxis
                  dataKey="date"
                  tickFormatter={formatDate}
                  className="text-xs"
                  tick={{ fill: 'currentColor' }}
                />
                <YAxis
                  tickFormatter={formatAmount}
                  className="text-xs"
                  tick={{ fill: 'currentColor' }}
                />
                <Tooltip
                  content={({ active, payload }) => {
                    if (active && payload && payload.length) {
                      const data = payload[0].payload
                      return (
                        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-3 shadow-lg">
                          <p className="text-xs text-gray-600 dark:text-gray-400 mb-1">
                            {formatDate(data.date)}
                          </p>
                          <p className="text-sm font-semibold text-teal-600 dark:text-teal-400">
                            {data.amount.toLocaleString('ko-KR')} 원
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400">
                            {data.count}건
                          </p>
                        </div>
                      )
                    }
                    return null
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="amount"
                  stroke="#0d9488"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#colorAmount)"
                  animationDuration={1500}
                  animationEasing="ease-in-out"
                />
              </AreaChart>
            </ResponsiveContainer>
            <div className="mt-4 space-y-3">
              <div className="bg-teal-50 dark:bg-teal-900/20 p-3 rounded-lg flex items-center justify-between">
                <p className="text-base font-semibold text-gray-700 dark:text-gray-300">총 기부액</p>
                <p className="text-lg font-bold text-teal-600 dark:text-teal-400">
                  {totalAmount.toLocaleString('ko-KR')} 원
                </p>
              </div>
              <div className="bg-cyan-50 dark:bg-cyan-900/20 p-3 rounded-lg flex items-center justify-between">
                <p className="text-base font-semibold text-gray-700 dark:text-gray-300">평균 기부액</p>
                <p className="text-lg font-bold text-cyan-600 dark:text-cyan-400">
                  {Math.floor(averageAmount).toLocaleString('ko-KR')} 원
                </p>
              </div>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}
