package com.hanachain.hanachainbackend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DisplayName("Comment Entity Test")
class CommentEntityTest {

    private Campaign campaign;
    private User user;
    private Comment comment;
    private Comment parentComment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .build();

        campaign = Campaign.builder()
                .title("Test Campaign")
                .description("Test Description")
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(new BigDecimal("500000"))
                .category(Campaign.CampaignCategory.MEDICAL)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(30))
                .user(user)
                .build();

        parentComment = Comment.builder()
                .content("이런 좋은 캠페인을 응원합니다!")
                .user(user)
                .campaign(campaign)
                .commenterName("테스트 사용자")
                .build();

        comment = Comment.builder()
                .content("정말 의미있는 활동이네요.")
                .user(user)
                .campaign(campaign)
                .commenterName("댓글 작성자")
                .build();
    }

    @Test
    @DisplayName("Comment 엔티티 생성 테스트")
    void testCommentCreation() {
        assertThat(comment.getContent()).isEqualTo("정말 의미있는 활동이네요.");
        assertThat(comment.getUser()).isEqualTo(user);
        assertThat(comment.getCampaign()).isEqualTo(campaign);
        assertThat(comment.getStatus()).isEqualTo(Comment.CommentStatus.ACTIVE);
        assertThat(comment.getAnonymous()).isFalse();
        assertThat(comment.getLikeCount()).isZero();
        assertThat(comment.getReplyCount()).isZero();
        assertThat(comment.isActive()).isTrue();
        assertThat(comment.isParentComment()).isTrue();
    }

    @Test
    @DisplayName("댓글 삭제 테스트")
    void testCommentDeletion() {
        // When
        comment.delete();
        
        // Then
        assertThat(comment.getStatus()).isEqualTo(Comment.CommentStatus.DELETED);
        assertThat(comment.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.isActive()).isFalse();
    }

    @Test
    @DisplayName("댓글 숨김 처리 테스트")
    void testCommentHiding() {
        // Given
        String reportReason = "부적절한 내용입니다.";
        
        // When
        comment.hide(reportReason);
        
        // Then
        assertThat(comment.getStatus()).isEqualTo(Comment.CommentStatus.HIDDEN);
        assertThat(comment.getReportReason()).isEqualTo(reportReason);
        assertThat(comment.isHidden()).isTrue();
    }

    @Test
    @DisplayName("좋아요 수 증가/감소 테스트")
    void testLikeCountManagement() {
        // Initially
        assertThat(comment.getLikeCount()).isZero();
        
        // Increment likes
        comment.incrementLikes();
        comment.incrementLikes();
        assertThat(comment.getLikeCount()).isEqualTo(2);
        
        // Decrement likes
        comment.decrementLikes();
        assertThat(comment.getLikeCount()).isEqualTo(1);
        
        // Cannot go below zero
        comment.decrementLikes();
        comment.decrementLikes();
        assertThat(comment.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("대댓글 추가 테스트")
    void testReplyManagement() {
        // Given
        Comment reply = Comment.builder()
                .content("저도 동감합니다!")
                .user(user)
                .campaign(campaign)
                .commenterName("답글 작성자")
                .build();
        
        // When
        parentComment.addReply(reply);
        
        // Then
        assertThat(parentComment.getReplies()).hasSize(1);
        assertThat(parentComment.getReplies().get(0)).isEqualTo(reply);
        assertThat(reply.getParent()).isEqualTo(parentComment);
        assertThat(reply.isReply()).isTrue();
        assertThat(reply.isParentComment()).isFalse();
        assertThat(parentComment.getReplyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("대댓글 수 업데이트 테스트")
    void testReplyCountUpdate() {
        // Given
        Comment activeReply = Comment.builder()
                .content("좋은 의견이네요!")
                .user(user)
                .campaign(campaign)
                .status(Comment.CommentStatus.ACTIVE)
                .build();
        
        Comment deletedReply = Comment.builder()
                .content("삭제된 댓글")
                .user(user)
                .campaign(campaign)
                .status(Comment.CommentStatus.DELETED)
                .build();
        
        parentComment.addReply(activeReply);
        parentComment.addReply(deletedReply);
        
        // When
        parentComment.updateReplyCount();
        
        // Then - Only active replies should be counted
        assertThat(parentComment.getReplyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("익명 댓글 테스트")
    void testAnonymousComment() {
        // Given
        Comment anonymousComment = Comment.builder()
                .content("익명으로 응원합니다.")
                .campaign(campaign)
                .anonymous(true)
                .build();
        
        // Then
        assertThat(anonymousComment.getAnonymous()).isTrue();
        assertThat(anonymousComment.getUser()).isNull(); // 익명 댓글은 user가 null일 수 있음
    }

    @Test
    @DisplayName("Comment Builder 패턴 테스트")
    void testCommentBuilder() {
        // Given & When
        Comment newComment = Comment.builder()
                .content("새로운 댓글입니다.")
                .user(user)
                .campaign(campaign)
                .commenterName("새 사용자")
                .anonymous(false)
                .build();

        // Then
        assertThat(newComment.getContent()).isEqualTo("새로운 댓글입니다.");
        assertThat(newComment.getCommenterName()).isEqualTo("새 사용자");
        assertThat(newComment.getAnonymous()).isFalse();
        assertThat(newComment.getStatus()).isEqualTo(Comment.CommentStatus.ACTIVE);
        assertThat(newComment.getLikeCount()).isZero();
        assertThat(newComment.getReplyCount()).isZero();
    }

    @Test
    @DisplayName("CommentStatus enum 테스트")
    void testCommentStatusEnum() {
        assertThat(Comment.CommentStatus.ACTIVE).isNotNull();
        assertThat(Comment.CommentStatus.DELETED).isNotNull();
        assertThat(Comment.CommentStatus.HIDDEN).isNotNull();
        assertThat(Comment.CommentStatus.BLOCKED).isNotNull();
    }
}