package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.comment.*;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Comment;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.CommentRepository;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.security.CampaignManagerPermissionService;
import com.hanachain.hanachainbackend.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final CampaignManagerPermissionService permissionService;

    @Override
    public Page<CommentResponse> getCampaignComments(Long campaignId, Pageable pageable) {
        log.debug("댓글 조회 시작: 캠페인 ID {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> {
                    log.error("캠페인을 찾을 수 없음: ID {}", campaignId);
                    return new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId);
                });

        log.debug("캠페인 조회 성공: ID {}, 제목 {}", campaign.getId(), campaign.getTitle());

        Page<Comment> comments = commentRepository.findByCampaignAndStatusAndParentIsNullOrderByCreatedAtDesc(
                campaign, Comment.CommentStatus.ACTIVE, pageable);

        log.debug("댓글 엔티티 조회 완료: {} 개", comments.getTotalElements());

        try {
            Page<CommentResponse> result = comments.map(this::convertToDto);
            log.debug("DTO 변환 완료: {} 개", result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("DTO 변환 중 오류 발생", e);
            throw new RuntimeException("댓글 목록 변환 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findByIdWithCampaignAndUser(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        return convertToDto(comment);
    }

    @Override
    @Transactional
    public CommentResponse createComment(Long campaignId, String username, CommentCreateRequest requestDto) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .campaign(campaign)
                .user(user)
                .commenterName(user.getName())
                .status(Comment.CommentStatus.ACTIVE)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 작성 완료: 사용자 {}, 캠페인 ID {}, 댓글 ID {}", username, campaignId, savedComment.getId());

        return convertToDto(savedComment);
    }

    @Override
    @Transactional
    public CommentResponse createReply(Long parentCommentId, String username, CommentReplyRequest requestDto) {
        Comment parentComment = commentRepository.findByIdWithCampaignAndUser(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다: " + parentCommentId));

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        Long campaignId = parentComment.getCampaign().getId();
        Long userId = user.getId();

        // 캠페인 담당자 권한 확인
        if (!permissionService.isActiveCampaignManager(userId, campaignId)) {
            throw new IllegalStateException("답글은 캠페인 담당자만 작성할 수 있습니다");
        }

        Comment reply = Comment.builder()
                .content(requestDto.getContent())
                .campaign(parentComment.getCampaign())
                .user(user)
                .parent(parentComment)
                .commenterName(user.getName())
                .status(Comment.CommentStatus.ACTIVE)
                .build();

        Comment savedReply = commentRepository.save(reply);

        // 부모 댓글의 답글 수 업데이트
        parentComment.updateReplyCount();
        commentRepository.save(parentComment);

        log.info("답글 작성 완료: 사용자 {}, 부모 댓글 ID {}, 답글 ID {}", username, parentCommentId, savedReply.getId());

        return convertToDto(savedReply);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, String username, CommentUpdateRequest requestDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // 작성자 확인
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인이 작성한 댓글만 수정할 수 있습니다");
        }

        comment.setContent(requestDto.getContent());
        Comment updatedComment = commentRepository.save(comment);

        log.info("댓글 수정 완료: 사용자 {}, 댓글 ID {}", username, commentId);

        return convertToDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // 작성자 확인
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인이 작성한 댓글만 삭제할 수 있습니다");
        }

        comment.delete(); // 소프트 삭제
        commentRepository.save(comment);

        // 부모 댓글이 있다면 답글 수 업데이트
        if (comment.getParent() != null) {
            Comment parent = comment.getParent();
            parent.updateReplyCount();
            commentRepository.save(parent);
        }

        log.info("댓글 삭제 완료: 사용자 {}, 댓글 ID {}", username, commentId);
    }

    /**
     * Comment 엔티티를 CommentResponse로 변환
     */
    private CommentResponse convertToDto(Comment comment) {
        // Campaign이 null인 경우 처리
        if (comment.getCampaign() == null) {
            throw new IllegalStateException("댓글의 캠페인 정보가 없습니다: " + comment.getId());
        }

        Long campaignId = comment.getCampaign().getId();
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;

        // 작성자 정보 생성
        CommentAuthor authorDto = null;
        if (comment.getUser() != null) {
            User user = comment.getUser();
            authorDto = CommentAuthor.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .profileImageUrl(user.getProfileImage())
                    .nickname(user.getNickname())
                    .build();
        }

        // 캠페인 담당자 여부 확인
        boolean isCampaignManager = userId != null &&
                permissionService.isActiveCampaignManager(userId, campaignId);

        // 기부 이력 여부 확인 (천사 로고 표시용)
        boolean hasDonated = userId != null &&
                donationRepository.hasUserDonatedToCampaign(userId, campaignId);

        // 답글 목록 변환 (replies가 초기화되지 않았을 수 있음)
        List<CommentResponse> replyDtos = new ArrayList<>();
        try {
            if (comment.getReplies() != null) {
                replyDtos = comment.getReplies().stream()
                        .filter(Comment::isActive)
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // LazyInitializationException 등의 경우 빈 리스트 반환
            log.warn("답글 목록 로딩 실패: {}", e.getMessage());
            replyDtos = new ArrayList<>();
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .author(authorDto)
                .isCampaignManager(isCampaignManager)
                .hasDonated(hasDonated)
                .isDeleted(comment.isDeleted())
                .replies(replyDtos)
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .build();
    }
}
