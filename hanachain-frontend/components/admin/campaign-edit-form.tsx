"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { format } from "date-fns";
import { CalendarIcon, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
  FormDescription,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { HtmlContent } from "@/components/ui/html-content";
import { updateAdminCampaign } from "@/lib/api/campaign-api";
import { handleApiError } from "@/lib/error-handler";
import { toast } from "sonner";
import type { CampaignDetailItem } from "@/types/donation";
import type { CampaignCategory, CampaignStatus } from "@/types/admin";

// 폼 검증 스키마
const campaignUpdateSchema = z.object({
  title: z.string().min(1, "제목을 입력해주세요").max(100, "제목은 100자 이하로 입력해주세요"),
  description: z.string().min(1, "설명을 입력해주세요"),
  targetAmount: z.number().min(1000, "목표 금액은 최소 1,000원 이상이어야 합니다"),
  imageUrl: z.string().url("올바른 이미지 URL을 입력해주세요").optional().or(z.literal("")),
  category: z.enum(["MEDICAL", "EDUCATION", "DISASTER_RELIEF", "ENVIRONMENT", "ANIMAL_WELFARE", "COMMUNITY", "EMERGENCY", "OTHER"]),
  status: z.enum(["DRAFT", "ACTIVE", "COMPLETED", "CANCELLED"]),
  startDate: z.date({ required_error: "시작일을 선택해주세요" }),
  endDate: z.date({ required_error: "종료일을 선택해주세요" }),
}).refine((data) => data.endDate > data.startDate, {
  message: "종료일은 시작일보다 늦어야 합니다",
  path: ["endDate"],
});

type FormData = z.infer<typeof campaignUpdateSchema>;

interface AdminCampaignEditFormProps {
  campaign: CampaignDetailItem;
  onSuccess: () => void;
}

// 카테고리 옵션
const categoryOptions: { value: CampaignCategory; label: string }[] = [
  { value: "MEDICAL", label: "의료" },
  { value: "EDUCATION", label: "교육" },
  { value: "DISASTER_RELIEF", label: "재해구호" },
  { value: "ENVIRONMENT", label: "환경" },
  { value: "ANIMAL_WELFARE", label: "동물복지" },
  { value: "COMMUNITY", label: "지역사회" },
  { value: "EMERGENCY", label: "응급상황" },
  { value: "OTHER", label: "기타" },
];

// 상태 옵션
const statusOptions: { value: CampaignStatus; label: string; variant: "default" | "secondary" | "destructive" | "outline" }[] = [
  { value: "DRAFT", label: "초안", variant: "secondary" },
  { value: "ACTIVE", label: "진행중", variant: "default" },
  { value: "COMPLETED", label: "마감됨", variant: "outline" },
  { value: "CANCELLED", label: "취소됨", variant: "destructive" },
];

export function AdminCampaignEditForm({ campaign, onSuccess }: AdminCampaignEditFormProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPreview, setShowPreview] = useState(false);

  const form = useForm<FormData>({
    resolver: zodResolver(campaignUpdateSchema),
    defaultValues: {
      title: campaign.title,
      description: campaign.description,
      targetAmount: campaign.targetAmount,
      imageUrl: campaign.imageUrl || "",
      category: campaign.category,
      status: campaign.status,
      startDate: new Date(campaign.startDate),
      endDate: new Date(campaign.endDate),
    },
  });

  const formatAmount = (value: string) => {
    const number = value.replace(/[^\d]/g, "");
    return number.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  };

  const parseAmount = (value: string) => {
    return parseInt(value.replace(/[^\d]/g, "")) || 0;
  };

  const onSubmit = async (data: FormData) => {
    try {
      setIsSubmitting(true);

      const updateData = {
        title: data.title,
        description: data.description,
        targetAmount: data.targetAmount,
        imageUrl: data.imageUrl || undefined,
        category: data.category,
        status: data.status,
        startDate: data.startDate.toISOString(),
        endDate: data.endDate.toISOString(),
      };

      await updateAdminCampaign(campaign.id, updateData);
      toast.success("캠페인이 성공적으로 수정되었습니다.");
      onSuccess();
    } catch (error) {
      const apiError = handleApiError(error);
      toast.error(apiError.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Basic Information */}
        <div className="space-y-4">
          <h3 className="text-lg font-semibold">기본 정보</h3>
          
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
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

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
                    onClick={() => setShowPreview(!showPreview)}
                  >
                    {showPreview ? '편집' : '미리보기'}
                  </Button>
                </div>
                <FormControl>
                  {showPreview ? (
                    <div className="min-h-[120px] p-3 border rounded-md bg-gray-50">
                      <HtmlContent 
                        html={field.value || ''} 
                        className="text-gray-900"
                      />
                    </div>
                  ) : (
                    <Textarea
                      placeholder="캠페인에 대한 자세한 설명을 입력하세요 (HTML 태그 사용 가능)"
                      className="min-h-[120px]"
                      {...field}
                    />
                  )}
                </FormControl>
                <FormDescription>
                  캠페인의 목적, 필요성, 기대효과 등을 구체적으로 작성해주세요. HTML 태그를 사용하여 서식을 지정할 수 있습니다.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="targetAmount"
            render={({ field }) => (
              <FormItem>
                <FormLabel>목표 금액 *</FormLabel>
                <FormControl>
                  <div className="relative">
                    <Input
                      placeholder="0"
                      value={formatAmount(field.value?.toString() || "")}
                      onChange={(e) => {
                        const amount = parseAmount(e.target.value);
                        field.onChange(amount);
                      }}
                      className="text-right pr-8"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                      원
                    </span>
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="imageUrl"
            render={({ field }) => (
              <FormItem>
                <FormLabel>이미지 URL</FormLabel>
                <FormControl>
                  <Input 
                    placeholder="https://example.com/image.jpg" 
                    {...field} 
                  />
                </FormControl>
                <FormDescription>
                  캠페인을 대표하는 이미지 URL을 입력하세요. (선택사항)
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* Category and Status */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
                    {categoryOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="status"
            render={({ field }) => (
              <FormItem>
                <FormLabel>상태 *</FormLabel>
                <Select onValueChange={field.onChange} defaultValue={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="상태를 선택하세요" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {statusOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        <div className="flex items-center space-x-2">
                          <Badge variant={option.variant} className="text-xs">
                            {option.label}
                          </Badge>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* Date Range */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
                        variant="outline"
                        className={cn(
                          "w-full pl-3 text-left font-normal",
                          !field.value && "text-muted-foreground"
                        )}
                      >
                        {field.value ? (
                          format(field.value, "yyyy년 MM월 dd일")
                        ) : (
                          <span>날짜를 선택하세요</span>
                        )}
                        <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                      </Button>
                    </FormControl>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                      mode="single"
                      selected={field.value}
                      onSelect={field.onChange}
                      disabled={(date) =>
                        date < new Date("1900-01-01")
                      }
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
                <FormMessage />
              </FormItem>
            )}
          />

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
                        variant="outline"
                        className={cn(
                          "w-full pl-3 text-left font-normal",
                          !field.value && "text-muted-foreground"
                        )}
                      >
                        {field.value ? (
                          format(field.value, "yyyy년 MM월 dd일")
                        ) : (
                          <span>날짜를 선택하세요</span>
                        )}
                        <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                      </Button>
                    </FormControl>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                      mode="single"
                      selected={field.value}
                      onSelect={field.onChange}
                      disabled={(date) =>
                        date < new Date("1900-01-01")
                      }
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* Form Actions */}
        <div className="flex justify-end space-x-4 pt-6 border-t">
          <Button 
            type="button" 
            variant="outline"
            onClick={() => window.history.back()}
            disabled={isSubmitting}
          >
            취소
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                저장 중...
              </>
            ) : (
              "저장"
            )}
          </Button>
        </div>
      </form>
    </Form>
  );
}