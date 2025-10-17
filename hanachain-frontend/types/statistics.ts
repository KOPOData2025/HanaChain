export interface DonationStatistics {
  totalAmount: number
  totalDonations: number
  totalCampaigns: number
  averageDonation: number
  lastDonationDate?: Date
  streakDays: number
}

export interface CategoryStatistics {
  category: string
  categoryLabel: string
  amount: number
  count: number
  percentage: number
  color: string
}

export interface MonthlyStatistics {
  month: string
  year: number
  amount: number
  count: number
  date: Date
}

export interface DonationTrend {
  period: string // "2024-01", "2024-02", etc.
  amount: number
  count: number
  growth: number // percentage growth from previous period
}

export interface TopCampaign {
  campaignId: string
  campaignTitle: string
  totalDonated: number
  donationCount: number
  lastDonationDate: Date
}

export interface DonationGoal {
  id: string
  title: string
  targetAmount: number
  currentAmount: number
  targetDate: Date
  isAchieved: boolean
  progress: number // percentage
}

export interface StatisticsSummary {
  overview: DonationStatistics
  categoryBreakdown: CategoryStatistics[]
  monthlyTrends: MonthlyStatistics[]
  yearlyTrends: DonationTrend[]
  topCampaigns: TopCampaign[]
  personalGoals: DonationGoal[]
}

export interface StatisticsTimeRange {
  range: 'all' | '1y' | '6m' | '3m' | '1m'
  startDate?: Date
  endDate?: Date
}

export interface ChartDataPoint {
  name: string
  value: number
  label?: string
  color?: string
}

export interface TimeSeriesDataPoint {
  date: string
  amount: number
  count: number
  label?: string
}
