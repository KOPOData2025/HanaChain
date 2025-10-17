'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { Loader2, Plus, Shield, Building2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'

import organizationApi from '@/lib/api/organization-api'
import { ApiError } from '@/lib/api/client'
import { OrganizationCreateRequest } from '@/types/organization'

// 폼 검증 스키마
const organizationCreateSchema = z.object({
  name: z.string()
    .min(2, '단체명은 최소 2자 이상이어야 합니다')
    .max(255, '단체명은 255자를 초과할 수 없습니다'),

  description: z.string()
    .max(2000, '단체 설명은 2000자를 초과할 수 없습니다')
    .optional(),

  imageUrl: z.string()
    .max(512, 'URL은 512자를 초과할 수 없습니다')
    .optional()
    .refine((val) => {
      if (!val || val.trim() === '') return true
      try {
        new URL(val)
        return true
      } catch {
        return false
      }
    }, '올바른 URL 형식이 아닙니다'),
})

type OrganizationCreateFormData = z.infer<typeof organizationCreateSchema>

export function OrganizationCreateForm() {
  const router = useRouter()
  const [isSubmitting, setIsSubmitting] = useState(false)

  const form = useForm<OrganizationCreateFormData>({
    resolver: zodResolver(organizationCreateSchema),
    mode: 'onSubmit',
    defaultValues: {
      name: '',
      description: '',
      imageUrl: '',
    }
  })

  // 폼 제출 핸들러
  const onSubmit = async (data: OrganizationCreateFormData) => {
    setIsSubmitting(true)

    try {
      // API 요청 데이터 구성
      const apiData: OrganizationCreateRequest = {
        name: data.name.trim(),
        description: data.description?.trim() || undefined,
        imageUrl: data.imageUrl?.trim() || undefined,
      }

      console.log('📝 단체 등록 요청:', apiData)

      // API 호출
      const response = await organizationApi.createOrganization(apiData)

      toast.success('단체가 성공적으로 등록되었습니다!', {
        description: `"${data.name}" 단체가 생성되었습니다. 블록체인 지갑이 자동으로 생성되었습니다.`
      })

      console.log('✅ 단체 등록 성공:', response)

      // 단체 상세 페이지로 이동
      router.push(`/admin/organizations/${response.id}`)

    } catch (error) {
      console.error('❌ 단체 등록 실패:', error)

      let errorMessage = '단체 등록에 실패했습니다'

      if (error instanceof ApiError) {
        console.error('❌ API 에러 상세:', {
          status: error.status,
          message: error.message,
          details: error.details
        })

        // 400 에러의 경우 상세한 검증 에러 메시지 표시
        if (error.status === 400 && error.details) {
          if (error.details.errors && Array.isArray(error.details.errors)) {
            errorMessage = error.details.errors.map((err: any) => err.message || err.defaultMessage).join(', ')
          } else if (error.details.message) {
            errorMessage = error.details.message
          }
        } else if (error.message) {
          errorMessage = error.message
        }

        // 401 에러의 경우 로그인 페이지로 이동
        if (error.status === 401) {
          router.push('/login')
          return
        }
      }

      toast.error('단체 등록 실패', {
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
          새 단체 등록
        </CardTitle>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
            {/* 기본 정보 섹션 */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                <Building2 className="h-5 w-5" />
                기본 정보
              </h3>

              {/* 단체명 */}
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>단체명 *</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="단체명을 입력하세요"
                        {...field}
                        maxLength={255}
                      />
                    </FormControl>
                    <FormDescription>
                      {field.value.length}/255자
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 단체 설명 */}
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>단체 설명 (선택)</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="단체에 대한 설명을 입력하세요"
                        {...field}
                        rows={6}
                        maxLength={2000}
                        className="resize-none"
                      />
                    </FormControl>
                    <FormDescription>
                      {field.value?.length || 0}/2000자
                    </FormDescription>
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
                        placeholder="https://example.com/logo.jpg"
                        {...field}
                        maxLength={512}
                      />
                    </FormControl>
                    <FormDescription>
                      단체 로고 또는 대표 이미지 URL을 입력하세요
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* 블록체인 통합 안내 섹션 */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                <Shield className="h-5 w-5" />
                블록체인 통합
              </h3>

              {/* 블록체인 지갑 자동 생성 안내 */}
              <div className="rounded-lg bg-blue-50 dark:bg-blue-950 p-4 border border-blue-200 dark:border-blue-800">
                <h4 className="text-sm font-medium text-blue-900 dark:text-blue-100 mb-2 flex items-center gap-2">
                  <Shield className="h-4 w-4" />
                  🔐 블록체인 지갑 자동 생성
                </h4>
                <ul className="text-sm text-blue-800 dark:text-blue-200 space-y-1">
                  <li>• 단체 등록 시 블록체인 지갑이 자동으로 생성됩니다</li>
                  <li>• 지갑 주소 및 개인키는 암호화되어 안전하게 저장됩니다</li>
                  <li>• 캠페인 생성 시 자동으로 연결되어 투명한 기부금 관리가 가능합니다</li>
                  <li>• 별도의 설정 없이 블록체인 기능을 바로 사용할 수 있습니다</li>
                </ul>
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
                  '단체 등록'
                )}
              </Button>
            </div>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}
