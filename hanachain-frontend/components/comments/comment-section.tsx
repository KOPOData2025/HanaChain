"use client"

import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { MessageSquare } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth-context"
import { useRouter } from "next/navigation"
import { commentApi } from "@/lib/api/comment-api"
import type { Comment, CampaignManager } from "@/types/comment"
import { CommentItem } from "./comment-item"
import { CommentForm } from "./comment-form"
import { CommentReplyForm } from "./comment-reply-form"
import { toast } from "sonner"

interface CommentSectionProps {
  campaignId: number
  campaignManagers: CampaignManager[]
}

export function CommentSection({ campaignId, campaignManagers }: CommentSectionProps) {
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [replyingTo, setReplyingTo] = useState<number | null>(null)

  const { isLoggedIn, user } = useAuth()
  const router = useRouter()

  // 현재 사용자가 캠페인 담당자인지 확인
  const isUserCampaignManager = campaignManagers.some(
    (manager) => manager.userId === user?.id && manager.status === 'ACTIVE'
  )

  // 댓글 목록 조회
  useEffect(() => {
    const fetchComments = async () => {
      try {
        setLoading(true)
        setError(null)
        const response = await commentApi.getCampaignComments(campaignId, 0, 20)
        setComments(response.content)
      } catch (err) {
        console.error('댓글 로딩 실패:', err)
        setError('댓글을 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoading(false)
      }
    }

    if (campaignId) {
      fetchComments()
    }
  }, [campaignId])

  // 댓글 작성
  const handleCreateComment = async (content: string) => {
    if (!isLoggedIn) {
      toast.error("로그인이 필요합니다")
      router.push(`/login?redirect=/campaign/${campaignId}`)
      return
    }

    try {
      const newComment = await commentApi.createComment(campaignId, { content })
      setComments([newComment, ...comments])
      toast.success("댓글이 작성되었습니다")
    } catch (err) {
      console.error('댓글 작성 실패:', err)
      toast.error("댓글 작성에 실패했습니다")
      throw err
    }
  }

  // 답글 작성
  const handleCreateReply = async (parentCommentId: number, content: string) => {
    if (!isLoggedIn) {
      toast.error("로그인이 필요합니다")
      router.push(`/login?redirect=/campaign/${campaignId}`)
      return
    }

    if (!isUserCampaignManager) {
      toast.error("답글은 캠페인 담당자만 작성할 수 있습니다")
      return
    }

    try {
      const newReply = await commentApi.createReply(parentCommentId, { content })

      // 댓글 목록 업데이트
      setComments(comments.map(comment => {
        if (comment.id === parentCommentId) {
          return {
            ...comment,
            replies: [...comment.replies, newReply],
            replyCount: comment.replyCount + 1
          }
        }
        return comment
      }))

      toast.success("답글이 작성되었습니다")
      setReplyingTo(null)
    } catch (err) {
      console.error('답글 작성 실패:', err)
      toast.error("답글 작성에 실패했습니다")
      throw err
    }
  }

  // 답글 버튼 클릭
  const handleReplyClick = (commentId: number) => {
    if (!isLoggedIn) {
      toast.error("로그인이 필요합니다")
      router.push(`/login?redirect=/campaign/${campaignId}`)
      return
    }

    if (!isUserCampaignManager) {
      toast.error("답글은 캠페인 담당자만 작성할 수 있습니다")
      return
    }

    setReplyingTo(commentId)
  }

  return (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center mb-6">
          <MessageSquare className="h-5 w-5 text-[#009591] mr-2" />
          <h3 className="text-xl font-semibold">댓글</h3>
          <span className="ml-2 text-sm text-gray-600">({comments.length})</span>
        </div>

        {/* 댓글 작성 폼 */}
        {isLoggedIn ? (
          <div className="mb-8">
            <CommentForm
              onSubmit={handleCreateComment}
              placeholder="댓글을 남겨주세요..."
              submitLabel="댓글 작성"
            />
          </div>
        ) : (
          <div className="mb-8 p-4 bg-gray-50 rounded-lg text-center">
            <p className="text-gray-600 mb-3">댓글을 작성하려면 로그인이 필요합니다</p>
            <Button
              onClick={() => router.push(`/login?redirect=/campaign/${campaignId}`)}
              className="bg-[#009591] hover:bg-[#007A77] text-white"
            >
              로그인하기
            </Button>
          </div>
        )}

        {/* 댓글 목록 */}
        {loading ? (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#009591] mx-auto mb-4"></div>
            <p className="text-gray-600">댓글을 불러오는 중...</p>
          </div>
        ) : error ? (
          <div className="text-center py-8">
            <p className="text-red-500 mb-4">{error}</p>
            <Button
              variant="outline"
              onClick={() => window.location.reload()}
            >
              다시 시도
            </Button>
          </div>
        ) : comments.length === 0 ? (
          <div className="text-center py-12">
            <MessageSquare className="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500 mb-2">아직 댓글이 없습니다</p>
            <p className="text-sm text-gray-400">
              첫 번째 댓글을 작성해보세요!
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {comments.map((comment) => (
              <div key={comment.id}>
                <CommentItem
                  comment={comment}
                  onReply={handleReplyClick}
                  showReplyButton={isUserCampaignManager}
                />

                {/* 답글 작성 폼 */}
                {replyingTo === comment.id && (
                  <CommentReplyForm
                    parentCommentId={comment.id}
                    onSubmit={(content) => handleCreateReply(comment.id, content)}
                    onCancel={() => setReplyingTo(null)}
                  />
                )}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
