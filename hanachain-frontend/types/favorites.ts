export interface FavoriteCampaign {
  id: string
  campaignId: string
  userId: string
  createdAt: Date
  campaign: CampaignInfo
}

export interface CampaignInfo {
  id: string
  title: string
  description: string
  targetAmount: number
  currentAmount: number
  progress: number
  category: string
  categoryLabel: string
  imageUrl: string
  organizationName: string
  endDate: Date
  status: 'active' | 'completed' | 'cancelled'
  donorCount: number
  isUrgent: boolean
}

export interface CampaignFilter {
  status?: 'all' | 'active' | 'completed' | 'cancelled'
  category?: string
  searchQuery?: string
  sortBy?: 'latest' | 'deadline' | 'amount' | 'progress'
  sortOrder?: 'asc' | 'desc'
}

export interface CampaignSort {
  field: 'createdAt' | 'endDate' | 'currentAmount' | 'progress' | 'title'
  order: 'asc' | 'desc'
  label: string
}

export interface UserCampaignInteraction {
  campaignId: string
  isFavorite: boolean
  totalDonated: number
  donationCount: number
  lastDonationDate?: Date
  firstDonationDate?: Date
}
