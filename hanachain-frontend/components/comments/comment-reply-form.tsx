"use client"

import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { useState } from "react"
import { X } from "lucide-react"

interface CommentReplyFormProps {
  onSubmit: (content: string) => Promise<void>
  onCancel: () => void
  parentCommentId: number
}

export function CommentReplyForm({
  onSubmit,
  onCancel,
  parentCommentId
}: CommentReplyFormProps) {
  const [content, setContent] = useState("")
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!content.trim()) {
      return
    }

    try {
      setSubmitting(true)
      await onSubmit(content)
      setContent("") // 성공 시 입력 필드 초기화
      onCancel() // 답글 폼 닫기
    } catch (error) {
      console.error("답글 작성 실패:", error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="ml-12 mt-2 p-4 bg-gray-50 rounded-lg border border-gray-200">
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-medium text-gray-700">답글 작성</span>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onCancel}
          className="h-auto p-1"
        >
          <X className="h-4 w-4" />
        </Button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        <Textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="답글을 입력하세요..."
          rows={2}
          maxLength={1000}
          className="resize-none"
          disabled={submitting}
          autoFocus
        />
        <div className="flex items-center justify-between">
          <span className="text-xs text-gray-500">
            {content.length} / 1000
          </span>
          <div className="space-x-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onCancel}
              disabled={submitting}
            >
              취소
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={!content.trim() || submitting}
              className="bg-[#009591] hover:bg-[#007A77] text-white"
            >
              {submitting ? "작성 중..." : "답글 작성"}
            </Button>
          </div>
        </div>
      </form>
    </div>
  )
}
