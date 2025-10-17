package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.comment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {

    /**
     * 캠페인의 댓글 목록 조회 (공개)
     */
    Page<CommentResponse> getCampaignComments(Long campaignId, Pageable pageable);

    /**
     * 댓글 상세 조회
     */
    CommentResponse getCommentById(Long commentId);

    /**
     * 댓글 작성
     */
    CommentResponse createComment(Long campaignId, String username, CommentCreateRequest requestDto);

    /**
     * 답글 작성 (캠페인 담당자만 가능)
     */
    CommentResponse createReply(Long parentCommentId, String username, CommentReplyRequest requestDto);

    /**
     * 댓글 수정 (작성자만 가능)
     */
    CommentResponse updateComment(Long commentId, String username, CommentUpdateRequest requestDto);

    /**
     * 댓글 삭제 (작성자만 가능)
     */
    void deleteComment(Long commentId, String username);
}
