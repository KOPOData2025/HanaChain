"use client"

import { Comment } from "@/types/comment"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip"
import { Heart, MessageSquare } from "lucide-react"
import { formatDistanceToNow } from "date-fns"
import { ko } from "date-fns/locale"
import { useState } from "react"

interface CommentItemProps {
  comment: Comment
  onReply?: (commentId: number) => void
  showReplyButton: boolean
}

export function CommentItem({ comment, onReply, showReplyButton }: CommentItemProps) {
  const [showReplies, setShowReplies] = useState(true)

  // 작성자 이니셜 생성
  const getInitial = (name: string) => {
    return name.charAt(0).toUpperCase()
  }

  // 상대 시간 포맷
  const getRelativeTime = (dateString: string) => {
    try {
      return formatDistanceToNow(new Date(dateString), {
        addSuffix: true,
        locale: ko
      })
    } catch (error) {
      return dateString
    }
  }

  return (
    <div className="space-y-4">
      {/* 메인 댓글 */}
      <div className="flex space-x-3">
        {/* 프로필 사진 */}
        <Avatar className="w-10 h-10">
          {comment.author.profileImageUrl ? (
            <img
              src={comment.author.profileImageUrl}
              alt={comment.author.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <AvatarFallback className="bg-[#009591] text-white font-semibold">
              {getInitial(comment.author.name)}
            </AvatarFallback>
          )}
        </Avatar>

        {/* 댓글 내용 */}
        <div className="flex-1 space-y-2">
          {/* 작성자 정보 및 뱃지 */}
          <div className="flex items-center space-x-2">
            <span className="font-medium text-gray-900">
              {comment.author.nickname || comment.author.name}
            </span>

            {/* 캠페인 담당자 뱃지 */}
            {comment.isCampaignManager && (
              <Badge className="bg-[#009591] text-white text-xs">
                담당자
              </Badge>
            )}

            {/* 천사 로고 (기부자 표시) */}
            {comment.hasDonated && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="flex items-center">
                    <Heart className="h-4 w-4 text-[#009591] fill-[#009591] heart-sparkle" />
                  </div>
                </TooltipTrigger>
                <TooltipContent className="bg-[#009591] text-white border-[#009591]">
                  <p>이 캠페인에 도움을 주셨어요</p>
                </TooltipContent>
              </Tooltip>
            )}

            {/* 작성 시간 */}
            <span className="text-xs text-gray-500">
              {getRelativeTime(comment.createdAt)}
            </span>
          </div>

          {/* 댓글 텍스트 */}
          <div className="text-sm text-gray-700 whitespace-pre-wrap">
            {comment.isDeleted ? (
              <span className="text-gray-400 italic">삭제된 댓글입니다.</span>
            ) : (
              comment.content
            )}
          </div>

          {/* 액션 버튼들 */}
          {!comment.isDeleted && (
            <div className="flex items-center justify-end space-x-4 text-xs">
              {/* 답글 수 표시 */}
              {comment.replyCount > 0 && (
                <button
                  className="text-gray-600 hover:text-[#009591] transition-colors"
                  onClick={() => setShowReplies(!showReplies)}
                >
                  <MessageSquare className="h-3 w-3 inline mr-1" />
                  답글 {comment.replyCount}개
                </button>
              )}

              {/* 답글 버튼 (담당자만 표시) */}
              {showReplyButton && onReply && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-auto p-0 text-gray-600 hover:text-[#009591]"
                  onClick={() => onReply(comment.id)}
                >
                  <MessageSquare className="h-3 w-3 mr-1" />
                  답글 작성
                </Button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* 답글 목록 */}
      {showReplies && comment.replies && comment.replies.length > 0 && (
        <div className="ml-12 space-y-4 border-l-2 border-gray-100 pl-4">
          {comment.replies.map((reply) => (
            <div key={reply.id} className="flex space-x-3">
              {/* 답글 프로필 사진 */}
              <Avatar className="w-8 h-8">
                {reply.author.profileImageUrl ? (
                  <img
                    src={reply.author.profileImageUrl}
                    alt={reply.author.name}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <AvatarFallback className="bg-[#009591] text-white font-semibold text-xs">
                    {getInitial(reply.author.name)}
                  </AvatarFallback>
                )}
              </Avatar>

              {/* 답글 내용 */}
              <div className="flex-1 space-y-1">
                {/* 답글 작성자 정보 */}
                <div className="flex items-center space-x-2">
                  <span className="font-medium text-sm text-gray-900">
                    {reply.author.nickname || reply.author.name}
                  </span>

                  {/* 답글 담당자 뱃지 */}
                  {reply.isCampaignManager && (
                    <Badge className="bg-[#009591] text-white text-xs">
                      담당자
                    </Badge>
                  )}

                  {/* 답글 천사 로고 */}
                  {reply.hasDonated && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="flex items-center">
                          <Heart className="h-3 w-3 text-[#009591] fill-[#009591] heart-sparkle" />
                        </div>
                      </TooltipTrigger>
                      <TooltipContent className="bg-[#009591] text-white border-[#009591]">
                        <p>이 캠페인에 도움을 주셨어요</p>
                      </TooltipContent>
                    </Tooltip>
                  )}

                  {/* 답글 작성 시간 */}
                  <span className="text-xs text-gray-500">
                    {getRelativeTime(reply.createdAt)}
                  </span>
                </div>

                {/* 답글 텍스트 */}
                <div className="text-sm text-gray-700 whitespace-pre-wrap">
                  {reply.isDeleted ? (
                    <span className="text-gray-400 italic">삭제된 답글입니다.</span>
                  ) : (
                    reply.content
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
