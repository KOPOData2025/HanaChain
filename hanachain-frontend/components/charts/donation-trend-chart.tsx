"use client"

import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts'
import { MonthlyStatistics } from '@/types/statistics'
import { formatCurrency } from '@/lib/utils'

interface DonationTrendChartProps {
  data: MonthlyStatistics[]
  type?: 'line' | 'bar'
  metric?: 'amount' | 'count'
  className?: string
}

const CustomTooltip = ({ active, payload, label, metric }: any) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload
    return (
      <div className="bg-white p-3 border border-gray-200 rounded-lg shadow-lg">
        <div className="font-medium text-gray-900 mb-2">{label}</div>
        <div className="space-y-1 text-sm">
          <div className="flex justify-between space-x-4">
            <span className="text-gray-600">기부 금액:</span>
            <span className="font-medium text-[#009591]">{formatCurrency(data.amount)}</span>
          </div>
          <div className="flex justify-between space-x-4">
            <span className="text-gray-600">기부 횟수:</span>
            <span className="font-medium text-blue-600">{data.count}회</span>
          </div>
        </div>
      </div>
    )
  }
  return null
}

export function DonationTrendChart({ 
  data, 
  type = 'line', 
  metric = 'amount', 
  className 
}: DonationTrendChartProps) {
  if (!data || data.length === 0) {
    return (
      <div className={`flex items-center justify-center h-80 ${className}`}>
        <div className="text-center text-gray-500">
          <div className="text-lg mb-2">📈</div>
          <div>표시할 데이터가 없습니다</div>
        </div>
      </div>
    )
  }

  // 차트 데이터 준비
  const chartData = data.map(item => ({
    ...item,
    name: `${item.year}년 ${item.month}`
  }))

  const metricKey = metric === 'amount' ? 'amount' : 'count'
  const color = metric === 'amount' ? '#009591' : '#3B82F6'

  if (type === 'bar') {
    return (
      <div className={`h-80 ${className}`}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis 
              dataKey="name" 
              stroke="#666"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="#666"
              fontSize={12}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => 
                metric === 'amount' 
                  ? `${(value / 1000).toFixed(0)}k`
                  : value.toString()
              }
            />
            <Tooltip content={(props) => <CustomTooltip {...props} metric={metric} />} />
            <Bar 
              dataKey={metricKey} 
              fill={color}
              radius={[4, 4, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    )
  }

  return (
    <div className={`h-80 ${className}`}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis 
            dataKey="name" 
            stroke="#666"
            fontSize={12}
            tickLine={false}
            axisLine={false}
          />
          <YAxis 
            stroke="#666"
            fontSize={12}
            tickLine={false}
            axisLine={false}
            tickFormatter={(value) => 
              metric === 'amount' 
                ? `${(value / 1000).toFixed(0)}k`
                : value.toString()
            }
          />
          <Tooltip content={(props) => <CustomTooltip {...props} metric={metric} />} />
          <Line 
            type="monotone" 
            dataKey={metricKey} 
            stroke={color}
            strokeWidth={3}
            dot={{ fill: color, strokeWidth: 2, r: 4 }}
            activeDot={{ r: 6, stroke: color, strokeWidth: 2 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
