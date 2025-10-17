"use client"

import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { useState } from "react"

interface CommentFormProps {
  onSubmit: (content: string) => Promise<void>
  placeholder?: string
  submitLabel?: string
}

export function CommentForm({
  onSubmit,
  placeholder = "댓글을 입력하세요...",
  submitLabel = "댓글 작성"
}: CommentFormProps) {
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
    } catch (error) {
      console.error("댓글 작성 실패:", error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <Textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={placeholder}
        rows={3}
        maxLength={1000}
        className="resize-none"
        disabled={submitting}
      />
      <div className="flex items-center justify-between">
        <span className="text-xs text-gray-500">
          {content.length} / 1000
        </span>
        <Button
          type="submit"
          disabled={!content.trim() || submitting}
          className="bg-[#009591] hover:bg-[#007A77] text-white"
        >
          {submitting ? "작성 중..." : submitLabel}
        </Button>
      </div>
    </form>
  )
}
