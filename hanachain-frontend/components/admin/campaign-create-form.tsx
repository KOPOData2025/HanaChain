'use client'

import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { CalendarIcon, Loader2, Plus, Shield, Link2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'

import { campaignApi } from '@/lib/api/campaign-api'
import { ApiError } from '@/lib/api/client'
import {
  CampaignCreateDto,
  CampaignCategory,
  CATEGORY_LABELS
} from '@/types/admin'
import { RichTextEditor, RichTextViewer } from '@/components/ui/rich-text-editor'
import { cn } from '@/lib/utils'
import organizationApi, { Organization } from '@/lib/api/organization-api'
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { Check, ChevronsUpDown } from 'lucide-react'

// 폼 검증 스키마
const campaignCreateSchema = z.object({
  title: z.string()
    .min(1, '제목은 필수입니다')
    .max(200, '제목은 200자를 초과할 수 없습니다'),
  
  subtitle: z.string()
    .max(500, '부제목은 500자를 초과할 수 없습니다')
    .optional(),
  
  description: z.string()
    .min(1, '설명은 필수입니다')
    .min(10, '설명은 최소 10자 이상 입력해주세요'),
  
  organizer: z.string()
    .min(1, '주최자는 필수입니다')
    .max(100, '주최자는 100자를 초과할 수 없습니다'),
  
  targetAmount: z.string()
    .min(1, '목표금액은 필수입니다')
    .refine((val) => {
      const num = parseInt(val.replace(/,/g, ''))
      return num >= 1000 && num <= 1000000000
    }, '목표금액은 1,000원 이상 10억원 이하여야 합니다'),
  
  imageUrl: z.string()
    .max(500, 'URL은 500자를 초과할 수 없습니다')
    .optional(),
  
  category: z.enum(['MEDICAL', 'EDUCATION', 'DISASTER_RELIEF', 'ENVIRONMENT', 'ANIMAL_WELFARE', 'COMMUNITY', 'EMERGENCY', 'OTHER'] as const, {
    required_error: '카테고리를 선택해주세요'
  }),
  
  startDate: z.date({
    required_error: '시작일을 선택해주세요'
  }),
  
  endDate: z.date({
    required_error: '종료일을 선택해주세요'
  }),
  
  beneficiaryAddress: z.string()
    .optional()
    .refine((val) => {
      if (!val || val.trim() === '') return true // 선택사항이므로 빈 값 허용
      return /^0x[a-fA-F0-9]{40}$/.test(val)
    }, '올바른 이더리움 주소 형식이 아닙니다 (0x로 시작하는 40자리 16진수)'),
  
  enableBlockchain: z.boolean().default(false) // 블록체인 통합 활성화 여부
}).refine(
  (data) => data.endDate > data.startDate,
  {
    message: '종료일은 시작일보다 늦어야 합니다',
    path: ['endDate']
  }
).refine(
  (data) => {
    if (data.enableBlockchain && (!data.beneficiaryAddress || data.beneficiaryAddress.trim() === '')) {
      return false
    }
    return true
  },
  {
    message: '블록체인 통합을 활성화하려면 수혜자 주소를 입력해야 합니다',
    path: ['beneficiaryAddress']
  }
)

type CampaignCreateFormData = z.infer<typeof campaignCreateSchema>

export function AdminCampaignCreateForm() {
  const router = useRouter()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showPreview, setShowPreview] = useState(false)
  const [organizationSearch, setOrganizationSearch] = useState('')
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [isLoadingOrganizations, setIsLoadingOrganizations] = useState(false)
  const [showOrganizationDropdown, setShowOrganizationDropdown] = useState(false)
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<number | null>(null)
  const [isLoadingWallet, setIsLoadingWallet] = useState(false)
  const [walletAutoPopulated, setWalletAutoPopulated] = useState(false)

  const form = useForm<CampaignCreateFormData>({
    resolver: zodResolver(campaignCreateSchema),
    mode: 'onSubmit', // Only validate on submit to prevent premature warnings
    reValidateMode: 'onSubmit', // Disable revalidation after first submit
    shouldFocusError: false, // Prevent auto-focus that triggers validation
    defaultValues: {
      title: '',
      subtitle: '',
      description: '',
      organizer: '',
      targetAmount: '',
      imageUrl: '',
      category: undefined,
      startDate: undefined,
      endDate: undefined,
      beneficiaryAddress: '',
      enableBlockchain: false,
    }
  })

  // 단체 검색 디바운싱 (300ms)
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (organizationSearch.trim().length >= 1) {
        setIsLoadingOrganizations(true)
        try {
          const results = await organizationApi.searchActiveOrganizations(
            organizationSearch,
            10
          )
          setOrganizations(results)
        } catch (error) {
          console.error('단체 검색 실패:', error)
          setOrganizations([])
        } finally {
          setIsLoadingOrganizations(false)
        }
      } else {
        setOrganizations([])
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [organizationSearch])

  // 금액 포맷팅 헬퍼 함수
  const formatAmount = (value: string): string => {
    const numericValue = value.replace(/[^0-9]/g, '')
    if (!numericValue) return ''
    
    return new Intl.NumberFormat('ko-KR').format(parseInt(numericValue))
  }

  // 날짜를 API 형식으로 변환
  const formatDateForAPI = (date: Date): string => {
    return date.toISOString().slice(0, 19)
  }

  // 폼 제출 핸들러
  const onSubmit = async (data: CampaignCreateFormData) => {
    setIsSubmitting(true)
    
    try {
      // 금액을 숫자로 변환
      const targetAmount = parseInt(data.targetAmount.replace(/,/g, ''))
      
      // API 요청 데이터 구성
      const apiData: CampaignCreateDto = {
        title: data.title.trim(),
        subtitle: data.subtitle?.trim() || undefined,
        description: data.description.trim(),
        organizer: data.organizer.trim(),
        targetAmount,
        imageUrl: data.imageUrl?.trim() || undefined,
        category: data.category,
        startDate: formatDateForAPI(data.startDate),
        endDate: formatDateForAPI(data.endDate),
        beneficiaryAddress: data.beneficiaryAddress?.trim() || undefined,
        organizationId: selectedOrganizationId || undefined
      }

      console.log('📝 캠페인 등록 요청:', apiData)

      // API 호출
      const response = await campaignApi.createAdminCampaign(apiData)

      toast.success('캠페인이 성공적으로 등록되었습니다!', {
        description: `"${data.title}" 캠페인이 생성되었습니다.`
      })

      console.log('✅ 캠페인 등록 성공:', response)

      // 캠페인 목록 페이지로 이동
      router.push('/admin/campaigns')

    } catch (error) {
      console.error('❌ 캠페인 등록 실패:', error)
      
      let errorMessage = '캠페인 등록에 실패했습니다'
      
      if (error instanceof ApiError) {
        console.error('❌ API 에러 상세:', {
          status: error.status,
          message: error.message,
          details: error.details,
          fullError: JSON.stringify(error.details, null, 2)
        })

        // 400 에러의 경우 상세한 검증 에러 메시지 표시
        if (error.status === 400 && error.details) {
          console.error('🔍 검증 에러 상세:', error.details)

          if (error.details.errors && Array.isArray(error.details.errors)) {
            const errorMessages = error.details.errors.map((err: any) => {
              const field = err.field || err.path || '알 수 없음'
              const message = err.message || err.defaultMessage || '검증 실패'
              return `${field}: ${message}`
            })
            errorMessage = errorMessages.join('\n')
            console.error('📋 필드별 에러:', errorMessages)
          } else if (error.details.message) {
            errorMessage = error.details.message
          } else if (typeof error.details === 'string') {
            errorMessage = error.details
          }
        } else {
          errorMessage = campaignApi.handleApiError(error)
        }
        
        // 401 에러의 경우 로그인 페이지로 이동
        if (error.status === 401) {
          router.push('/login')
          return
        }
      }

      toast.error('캠페인 등록 실패', {
        description: errorMessage
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card className="w-full max-w-4xl mx-auto">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Plus className="h-5 w-5" />
          새 캠페인 등록
        </CardTitle>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
            {/* 기본 정보 섹션 */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                기본 정보
              </h3>
              
              {/* 캠페인 제목 */}
              <FormField
                control={form.control}
                name="title"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>캠페인 제목 *</FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="캠페인 제목을 입력하세요"
                        {...field}
                        maxLength={200}
                      />
                    </FormControl>
                    <FormDescription>
                      {field.value.length}/200자
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 캠페인 부제목 */}
              <FormField
                control={form.control}
                name="subtitle"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>캠페인 부제목</FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="캠페인 부제목을 입력하세요 (선택사항)"
                        {...field}
                        maxLength={500}
                      />
                    </FormControl>
                    <FormDescription>
                      {field.value?.length || 0}/500자
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 주최자 (단체 검색 자동완성) */}
              <FormField
                control={form.control}
                name="organizer"
                render={({ field }) => (
                  <FormItem className="flex flex-col">
                    <FormLabel>주최자 또는 단체명 *</FormLabel>
                    <Popover
                      open={showOrganizationDropdown}
                      onOpenChange={setShowOrganizationDropdown}
                    >
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant="outline"
                            role="combobox"
                            aria-expanded={showOrganizationDropdown}
                            className={cn(
                              "w-full justify-between",
                              !field.value && "text-muted-foreground"
                            )}
                          >
                            {field.value || "등록된 단체를 검색하세요..."}
                            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-full p-0" align="start">
                        <Command shouldFilter={false}>
                          <CommandInput
                            placeholder="단체명 검색..."
                            value={organizationSearch}
                            onValueChange={setOrganizationSearch}
                          />
                          <CommandList>
                            <CommandEmpty>
                              {isLoadingOrganizations
                                ? "검색 중..."
                                : organizationSearch.trim().length >= 1
                                ? "검색 결과가 없습니다."
                                : "단체명을 입력하세요."}
                            </CommandEmpty>
                            {organizations.length > 0 && (
                              <CommandGroup>
                                {organizations.map((org) => (
                                  <CommandItem
                                    key={org.id}
                                    value={org.name}
                                    onSelect={async () => {
                                      form.setValue('organizer', org.name)
                                      setShowOrganizationDropdown(false)
                                      setOrganizationSearch('')
                                      setSelectedOrganizationId(org.id)

                                      // 단체의 지갑 주소 자동 가져오기
                                      if (org.walletAddress) {
                                        // Organization 객체에 이미 walletAddress가 있는 경우
                                        form.setValue('beneficiaryAddress', org.walletAddress)
                                        setWalletAutoPopulated(true)
                                        toast.success('단체 지갑 주소가 자동으로 설정되었습니다', {
                                          description: `${org.name}의 지갑 주소: ${org.walletAddress.slice(0, 10)}...`
                                        })
                                      } else {
                                        // API를 통해 지갑 주소 가져오기
                                        setIsLoadingWallet(true)
                                        try {
                                          const walletData = await organizationApi.getOrganizationWallet(org.id)
                                          form.setValue('beneficiaryAddress', walletData.walletAddress)
                                          setWalletAutoPopulated(true)
                                          toast.success('단체 지갑 주소가 자동으로 설정되었습니다', {
                                            description: `${walletData.organizationName}의 지갑 주소: ${walletData.walletAddress.slice(0, 10)}...`
                                          })
                                        } catch (error) {
                                          console.error('지갑 주소 조회 실패:', error)
                                          toast.warning('지갑 주소를 가져올 수 없습니다', {
                                            description: '수동으로 입력해주세요.'
                                          })
                                          setWalletAutoPopulated(false)
                                        } finally {
                                          setIsLoadingWallet(false)
                                        }
                                      }
                                    }}
                                    className="justify-start"
                                  >
                                    <div className="flex items-center gap-3 flex-1 min-w-0">
                                      {/* 단체 로고 (원형) */}
                                      <div className="shrink-0 w-10 h-10 rounded-full overflow-hidden bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
                                        {org.imageUrl ? (
                                          <img
                                            src={org.imageUrl}
                                            alt={org.name}
                                            className="w-full h-full object-cover"
                                          />
                                        ) : (
                                          <span className="text-xs font-semibold text-gray-400">
                                            {org.name.charAt(0)}
                                          </span>
                                        )}
                                      </div>
                                      {/* 단체 이름 */}
                                      <span className="font-medium truncate">{org.name}</span>
                                      {/* 선택 체크마크 (오른쪽 끝) */}
                                      <Check
                                        className={cn(
                                          "ml-auto h-4 w-4 shrink-0",
                                          field.value === org.name
                                            ? "opacity-100"
                                            : "opacity-0"
                                        )}
                                      />
                                    </div>
                                  </CommandItem>
                                ))}
                              </CommandGroup>
                            )}
                          </CommandList>
                        </Command>
                      </PopoverContent>
                    </Popover>
                    <FormDescription>
                      데이터베이스에 등록된 단체만 선택할 수 있습니다. 단체명을 입력하면 자동으로 필터링됩니다.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 캠페인 설명 */}
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex items-center justify-between">
                      <FormLabel>캠페인 설명 *</FormLabel>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={(e) => {
                          e.preventDefault()
                          e.stopPropagation()
                          // 검증 경고를 방지하기 위해 기존 폼 에러 제거
                          form.clearErrors()
                          setShowPreview(!showPreview)
                        }}
                      >
                        {showPreview ? '편집' : '미리보기'}
                      </Button>
                    </div>
                    <FormControl>
                      <div className="relative">
                        {/* Rich Text Editor - Always mounted but hidden during preview */}
                        <div className={cn("w-full", showPreview && "absolute opacity-0 pointer-events-none")}>
                          <RichTextEditor
                            value={field.value || ''}
                            onChange={field.onChange}
                            placeholder="캠페인에 대한 상세한 설명을 입력하세요. 툴바를 사용하여 텍스트를 서식을 설정할 수 있습니다."
                            minHeight="300px"
                          />
                        </div>
                        
                        {/* Preview Mode - Only visible when preview is active */}
                        {showPreview && (
                          <div className="min-h-[300px] p-4 border rounded-md bg-gray-50 dark:bg-gray-800">
                            <RichTextViewer 
                              html={field.value || ''} 
                              className="text-gray-900 dark:text-gray-100"
                            />
                          </div>
                        )}
                      </div>
                    </FormControl>
                    <FormDescription>
                      캠페인의 목적, 배경, 기대효과 등을 자세히 작성해주세요. 텍스트 서식, 이미지 삽입 등을 자유롭게 사용할 수 있습니다.
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 목표 금액 */}
              <FormField
                control={form.control}
                name="targetAmount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>목표 금액 *</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <Input 
                          placeholder="1,000,000"
                          {...field}
                          onChange={(e) => {
                            const formatted = formatAmount(e.target.value)
                            field.onChange(formatted)
                          }}
                        />
                        <div className="absolute inset-y-0 right-3 flex items-center pointer-events-none">
                          <span className="text-gray-500">원</span>
                        </div>
                      </div>
                    </FormControl>
                    <FormDescription>
                      최소 1,000원 이상, 최대 10억원 이하
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 카테고리 */}
              <FormField
                control={form.control}
                name="category"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>카테고리 *</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="카테고리를 선택하세요" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                          <SelectItem key={key} value={key}>
                            {label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 이미지 URL */}
              <FormField
                control={form.control}
                name="imageUrl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>이미지 URL (선택)</FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="https://example.com/image.jpg"
                        {...field}
                        maxLength={500}
                      />
                    </FormControl>
                    <FormDescription>
                      캠페인 대표 이미지 URL을 입력하세요
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* 블록체인 통합 섹션 */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                <Shield className="h-5 w-5" />
                블록체인 통합 설정
              </h3>
              
              {/* 블록체인 통합 활성화 */}
              <FormField
                control={form.control}
                name="enableBlockchain"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                    <div className="space-y-0.5">
                      <FormLabel className="text-base">
                        블록체인 투명성 기능 활성화
                      </FormLabel>
                      <FormDescription>
                        캠페인을 블록체인에 등록하여 투명한 기부 관리가 가능합니다. 
                        모든 거래 내역이 공개적으로 검증 가능하며, 자동 정산 기능을 사용할 수 있습니다.
                      </FormDescription>
                    </div>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />

              {/* 수혜자 주소 입력 (블록체인 활성화 시에만 표시) */}
              {form.watch('enableBlockchain') && (
                <FormField
                  control={form.control}
                  name="beneficiaryAddress"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="flex items-center gap-2">
                        <Link2 className="h-4 w-4" />
                        수혜자 이더리움 주소 *
                      </FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            placeholder="0x742d35Cc6634C0532925a3b8D8d3c8b6e15c2693"
                            {...field}
                            className={cn(
                              "font-mono",
                              walletAutoPopulated && "bg-gray-50 dark:bg-gray-900"
                            )}
                            readOnly={walletAutoPopulated}
                            style={walletAutoPopulated ? { pointerEvents: 'none' } : undefined}
                            disabled={isLoadingWallet}
                          />
                          {isLoadingWallet && (
                            <div className="absolute inset-y-0 right-3 flex items-center">
                              <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
                            </div>
                          )}
                        </div>
                      </FormControl>
                      {walletAutoPopulated ? (
                        <FormDescription className="flex items-center gap-2 text-blue-600 dark:text-blue-400">
                          <Shield className="h-4 w-4" />
                          선택한 단체의 지갑 주소가 자동으로 설정되었습니다. 변경이 필요한 경우 단체를 다시 선택하세요.
                        </FormDescription>
                      ) : (
                        <FormDescription>
                          캠페인 목표 달성 시 자동으로 기부금이 전송될 이더리움 주소를 입력하세요.
                          <br />
                          형식: 0x로 시작하는 40자리 16진수 (예: 0x742d35Cc6634C0532925a3b8D8d3c8b6e15c2693)
                        </FormDescription>
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              {/* 블록체인 기능 안내 */}
              {form.watch('enableBlockchain') && (
                <div className="rounded-lg bg-blue-50 dark:bg-blue-950 p-4">
                  <h4 className="text-sm font-medium text-blue-900 dark:text-blue-100 mb-2">
                    🔗 블록체인 통합 시 제공되는 기능:
                  </h4>
                  <ul className="text-sm text-blue-800 dark:text-blue-200 space-y-1">
                    <li>• 모든 기부 거래의 투명한 공개 검증</li>
                    <li>• 실시간 기부금 추적 및 현황 확인</li>
                    <li>• 목표 달성 시 자동 정산 (스마트 컨트랙트)</li>
                    <li>• Etherscan에서 모든 거래 내역 확인 가능</li>
                    <li>• 변조 불가능한 블록체인 기록 보관</li>
                  </ul>
                </div>
              )}
            </div>

            {/* 일정 섹션 */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                캠페인 일정
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* 시작일 */}
                <FormField
                  control={form.control}
                  name="startDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>시작일 *</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button
                              variant={"outline"}
                              className={cn(
                                "pl-3 text-left font-normal",
                                !field.value && "text-muted-foreground"
                              )}
                            >
                              {field.value ? (
                                format(field.value, "yyyy년 MM월 dd일 (E)", { locale: ko })
                              ) : (
                                <span>시작일을 선택하세요</span>
                              )}
                              <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            onSelect={(date) => {
                              if (date) {
                                // 선택된 날짜의 다음 날 00:00으로 설정
                                const nextDay = new Date(date)
                                nextDay.setDate(nextDay.getDate() + 1)
                                nextDay.setHours(0, 0, 0, 0)
                                field.onChange(nextDay)
                              } else {
                                field.onChange(date)
                              }
                            }}
                            initialFocus
                            locale={ko}
                          />
                        </PopoverContent>
                      </Popover>
                      <FormDescription>
                        캠페인 시작일 (과거 날짜 선택 가능)
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* 종료일 */}
                <FormField
                  control={form.control}
                  name="endDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>종료일 *</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button
                              variant={"outline"}
                              className={cn(
                                "pl-3 text-left font-normal",
                                !field.value && "text-muted-foreground"
                              )}
                            >
                              {field.value ? (
                                format(field.value, "yyyy년 MM월 dd일 (E)", { locale: ko })
                              ) : (
                                <span>종료일을 선택하세요</span>
                              )}
                              <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            onSelect={(date) => {
                              if (date) {
                                // 선택된 날짜의 다음 날 00:00으로 설정하여 검증 통과 보장
                                const nextDay = new Date(date)
                                nextDay.setDate(nextDay.getDate() + 1)
                                nextDay.setHours(0, 0, 0, 0)
                                field.onChange(nextDay)
                              } else {
                                field.onChange(date)
                              }
                            }}
                            disabled={(date) => {
                              const startDate = form.getValues('startDate')
                              return startDate && date <= startDate
                            }}
                            initialFocus
                            locale={ko}
                          />
                        </PopoverContent>
                      </Popover>
                      <FormDescription>
                        캠페인 종료일 (선택한 날짜의 다음 날에 종료)
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>

            {/* 제출 버튼 */}
            <div className="flex justify-end space-x-4">
              <Button 
                type="button" 
                variant="outline"
                onClick={() => router.back()}
                disabled={isSubmitting}
              >
                취소
              </Button>
              <Button 
                type="submit"
                disabled={isSubmitting}
                className="min-w-[120px]"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    등록 중...
                  </>
                ) : (
                  '캠페인 등록'
                )}
              </Button>
            </div>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}