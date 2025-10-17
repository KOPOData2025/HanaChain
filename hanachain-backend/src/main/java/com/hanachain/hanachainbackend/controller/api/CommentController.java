package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.comment.*;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.security.JwtTokenProvider;
import com.hanachain.hanachainbackend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 캠페인의 댓글 목록 조회 (공개)
     */
    @GetMapping("/campaigns/{campaignId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCampaignComments(
            @PathVariable Long campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("캠페인 댓글 목록 조회: 캠페인 ID {}, 페이지 {}, 크기 {}", campaignId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentResponse> comments = commentService.getCampaignComments(campaignId, pageable);
            log.info("댓글 목록 조회 성공: {} 개", comments.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회 성공", comments));
        } catch (Exception e) {
            log.error("댓글 목록 조회 실패: 캠페인 ID {}", campaignId, e);
            throw e;
        }
    }

    /**
     * 댓글 상세 조회
     */
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> getComment(@PathVariable Long commentId) {
        log.info("댓글 상세 조회: 댓글 ID {}", commentId);

        CommentResponse comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comment));
    }

    /**
     * 댓글 작성 (로그인 필요)
     */
    @PostMapping("/campaigns/{campaignId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long campaignId,
            @Valid @RequestBody CommentCreateRequest requestDto,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("댓글 작성: 사용자 {}, 캠페인 ID {}", username, campaignId);

        CommentResponse comment = commentService.createComment(campaignId, username, requestDto);
        return ResponseEntity.ok(ApiResponse.success("댓글 작성 성공", comment));
    }

    /**
     * 답글 작성 (캠페인 담당자만 가능)
     */
    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<ApiResponse<CommentResponse>> createReply(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReplyRequest requestDto,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("답글 작성: 사용자 {}, 부모 댓글 ID {}", username, commentId);

        CommentResponse reply = commentService.createReply(commentId, username, requestDto);
        return ResponseEntity.ok(ApiResponse.success("답글 작성 성공", reply));
    }

    /**
     * 댓글 수정 (작성자만 가능)
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest requestDto,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("댓글 수정: 사용자 {}, 댓글 ID {}", username, commentId);

        CommentResponse comment = commentService.updateComment(commentId, username, requestDto);
        return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공", comment));
    }

    /**
     * 댓글 삭제 (작성자만 가능)
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("댓글 삭제: 사용자 {}, 댓글 ID {}", username, commentId);

        commentService.deleteComment(commentId, username);
        return ResponseEntity.ok(ApiResponse.success("댓글 삭제 성공"));
    }
}
