"use client"

import { Progress } from "@/components/ui/progress"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { DonationGoal } from "@/types/statistics"
import { formatCurrency } from "@/lib/utils"
import { Target, TrendingUp, Calendar, CheckCircle } from "lucide-react"

interface GoalProgressChartProps {
  goals: DonationGoal[]
  className?: string
}

export function GoalProgressChart({ goals, className }: GoalProgressChartProps) {
  if (!goals || goals.length === 0) {
    return (
      <div className={`flex items-center justify-center p-8 ${className}`}>
        <div className="text-center text-gray-500">
          <Target className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <div className="text-lg font-medium mb-2">설정된 목표가 없습니다</div>
          <div className="text-sm">새로운 기부 목표를 설정해보세요</div>
        </div>
      </div>
    )
  }

  return (
    <div className={`space-y-4 ${className}`}>
      {goals.map((goal) => {
        const isCloseToTarget = goal.progress >= 80 && !goal.isAchieved
        const isOverdue = new Date() > goal.targetDate && !goal.isAchieved
        const daysLeft = Math.ceil((goal.targetDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))
        
        return (
          <Card key={goal.id} className="relative overflow-hidden">
            <CardContent className="p-6">
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <div className="flex items-center space-x-2 mb-2">
                    <h3 className="font-medium text-gray-900">{goal.title}</h3>
                    {goal.isAchieved && (
                      <Badge className="bg-green-100 text-green-800">
                        <CheckCircle className="h-3 w-3 mr-1" />
                        달성
                      </Badge>
                    )}
                    {isCloseToTarget && (
                      <Badge className="bg-orange-100 text-orange-800">
                        <TrendingUp className="h-3 w-3 mr-1" />
                        거의 달성
                      </Badge>
                    )}
                    {isOverdue && (
                      <Badge variant="destructive">
                        <Calendar className="h-3 w-3 mr-1" />
                        기한 초과
                      </Badge>
                    )}
                  </div>
                  
                  <div className="text-sm text-gray-600 mb-3">
                    목표일: {goal.targetDate.toLocaleDateString('ko-KR')}
                    {!goal.isAchieved && daysLeft > 0 && (
                      <span className="ml-2">({daysLeft}일 남음)</span>
                    )}
                  </div>
                </div>
                
                <div className="text-right">
                  <div className="text-2xl font-bold text-gray-900">
                    {goal.progress.toFixed(1)}%
                  </div>
                  <div className="text-sm text-gray-500">
                    {formatCurrency(goal.currentAmount)} / {formatCurrency(goal.targetAmount)}
                  </div>
                </div>
              </div>

              {/* 프로그레스 바 */}
              <div className="space-y-2">
                <Progress 
                  value={Math.min(goal.progress, 100)} 
                  className="h-3"
                  style={{
                    background: goal.isAchieved ? '#dcfce7' : isOverdue ? '#fee2e2' : '#f3f4f6'
                  }}
                />
                
                <div className="flex justify-between text-xs text-gray-500">
                  <span>0원</span>
                  <span>
                    남은 금액: {formatCurrency(Math.max(0, goal.targetAmount - goal.currentAmount))}
                  </span>
                  <span>{formatCurrency(goal.targetAmount)}</span>
                </div>
              </div>

              {/* 달성률에 따른 메시지 */}
              {goal.isAchieved && (
                <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                  <div className="flex items-center space-x-2 text-green-800">
                    <CheckCircle className="h-4 w-4" />
                    <span className="text-sm font-medium">목표를 달성했습니다! 🎉</span>
                  </div>
                </div>
              )}
              
              {isCloseToTarget && (
                <div className="mt-4 p-3 bg-orange-50 border border-orange-200 rounded-lg">
                  <div className="flex items-center space-x-2 text-orange-800">
                    <TrendingUp className="h-4 w-4" />
                    <span className="text-sm font-medium">
                      목표 달성까지 {formatCurrency(goal.targetAmount - goal.currentAmount)}만 남았어요!
                    </span>
                  </div>
                </div>
              )}
              
              {isOverdue && (
                <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                  <div className="flex items-center space-x-2 text-red-800">
                    <Calendar className="h-4 w-4" />
                    <span className="text-sm font-medium">
                      목표 기한이 지났습니다. 새로운 목표를 설정해보세요.
                    </span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
