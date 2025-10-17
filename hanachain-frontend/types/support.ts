export interface Notice {
  id: string
  title: string
  content: string
  category: 'general' | 'maintenance' | 'update' | 'event'
  isImportant: boolean
  createdAt: Date
  updatedAt: Date
  views: number
}

export interface FAQ {
  id: string
  question: string
  answer: string
  category: 'donation' | 'account' | 'payment' | 'technical' | 'general'
  tags: string[]
  isPopular: boolean
  helpfulCount: number
  createdAt: Date
  updatedAt: Date
}

export interface Inquiry {
  id: string
  userId: string
  title: string
  content: string
  category: 'donation' | 'account' | 'payment' | 'technical' | 'general' | 'other'
  status: 'pending' | 'in-progress' | 'resolved' | 'closed'
  priority: 'low' | 'normal' | 'high' | 'urgent'
  attachments?: string[]
  createdAt: Date
  updatedAt: Date
  responses: InquiryResponse[]
}

export interface InquiryResponse {
  id: string
  inquiryId: string
  content: string
  isStaff: boolean
  authorName: string
  createdAt: Date
  attachments?: string[]
}

export interface InquiryCreateRequest {
  title: string
  content: string
  category: Inquiry['category']
  attachments?: File[]
}

export interface SupportStats {
  totalInquiries: number
  pendingInquiries: number
  resolvedInquiries: number
  averageResponseTime: number // hours
}

export interface SupportSearchParams {
  query?: string
  category?: string
  page?: number
  limit?: number
}
