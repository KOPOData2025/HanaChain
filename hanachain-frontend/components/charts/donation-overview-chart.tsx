"use client"

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts'
import { CategoryStatistics } from '@/types/statistics'
import { formatCurrency } from '@/lib/utils'

interface DonationOverviewChartProps {
  data: CategoryStatistics[]
  className?: string
}

const RADIAN = Math.PI / 180
const renderCustomizedLabel = ({
  cx, cy, midAngle, innerRadius, outerRadius, percent
}: any) => {
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5
  const x = cx + radius * Math.cos(-midAngle * RADIAN)
  const y = cy + radius * Math.sin(-midAngle * RADIAN)

  if (percent < 0.05) return null // 5% 미만은 라벨 숨김

  return (
    <text 
      x={x} 
      y={y} 
      fill="white" 
      textAnchor={x > cx ? 'start' : 'end'} 
      dominantBaseline="central"
      fontSize={12}
      fontWeight={500}
    >
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  )
}

const CustomTooltip = ({ active, payload }: any) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload
    return (
      <div className="bg-white p-3 border border-gray-200 rounded-lg shadow-lg">
        <div className="flex items-center space-x-2 mb-2">
          <div 
            className="w-3 h-3 rounded-full" 
            style={{ backgroundColor: data.color }}
          />
          <span className="font-medium text-gray-900">{data.categoryLabel}</span>
        </div>
        <div className="space-y-1 text-sm">
          <div className="flex justify-between space-x-4">
            <span className="text-gray-600">기부 금액:</span>
            <span className="font-medium">{formatCurrency(data.amount)}</span>
          </div>
          <div className="flex justify-between space-x-4">
            <span className="text-gray-600">기부 횟수:</span>
            <span className="font-medium">{data.count}회</span>
          </div>
          <div className="flex justify-between space-x-4">
            <span className="text-gray-600">비율:</span>
            <span className="font-medium">{data.percentage.toFixed(1)}%</span>
          </div>
        </div>
      </div>
    )
  }
  return null
}

export function DonationOverviewChart({ data, className }: DonationOverviewChartProps) {
  if (!data || data.length === 0) {
    return (
      <div className={`flex items-center justify-center h-80 ${className}`}>
        <div className="text-center text-gray-500">
          <div className="text-lg mb-2">📊</div>
          <div>표시할 데이터가 없습니다</div>
        </div>
      </div>
    )
  }

  return (
    <div className={`h-80 ${className}`}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            labelLine={false}
            label={renderCustomizedLabel}
            outerRadius={80}
            fill="#8884d8"
            dataKey="amount"
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} />
          <Legend 
            verticalAlign="bottom" 
            height={36}
            formatter={(value, entry: any) => (
              <span style={{ color: entry.color, fontSize: '14px' }}>
                {entry.payload.categoryLabel}
              </span>
            )}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
