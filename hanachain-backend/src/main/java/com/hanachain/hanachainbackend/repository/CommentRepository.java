package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Comment;
import com.hanachain.hanachainbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 특정 캠페인의 활성 댓글 조회 (최상위 댓글만, 페이징)
    Page<Comment> findByCampaignAndStatusAndParentIsNullOrderByCreatedAtDesc(
        Campaign campaign, 
        Comment.CommentStatus status, 
        Pageable pageable
    );
    
    // 특정 댓글의 답글 조회 (활성 답글만)
    List<Comment> findByParentAndStatusOrderByCreatedAtAsc(Comment parent, Comment.CommentStatus status);
    
    // 특정 캠페인의 전체 활성 댓글 수 (답글 포함)
    long countByCampaignAndStatus(Campaign campaign, Comment.CommentStatus status);
    
    // 특정 사용자가 작성한 댓글 목록 (페이징)
    Page<Comment> findByUserAndStatusOrderByCreatedAtDesc(User user, Comment.CommentStatus status, Pageable pageable);
    
    // 특정 캠페인의 최신 댓글 조회 (제한된 개수)
    @Query("SELECT c FROM Comment c WHERE c.campaign = :campaign AND c.status = 'ACTIVE' ORDER BY c.createdAt DESC")
    List<Comment> findRecentCommentsByCampaign(@Param("campaign") Campaign campaign, Pageable pageable);
    
    // 댓글과 답글을 함께 조회 (N+1 문제 해결)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.replies WHERE c.campaign = :campaign AND c.status = 'ACTIVE' AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findParentCommentsWithReplies(@Param("campaign") Campaign campaign);
    
    // 특정 기간 내 댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.campaign = :campaign AND c.status = 'ACTIVE' AND c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    List<Comment> findByCampaignAndCreatedAtBetween(
        @Param("campaign") Campaign campaign,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // 인기 댓글 조회 (좋아요 수 기준)
    @Query("SELECT c FROM Comment c WHERE c.campaign = :campaign AND c.status = 'ACTIVE' AND c.parent IS NULL ORDER BY c.likeCount DESC, c.createdAt DESC")
    List<Comment> findPopularCommentsByCampaign(@Param("campaign") Campaign campaign, Pageable pageable);
    
    // 특정 사용자가 작성한 특정 캠페인의 댓글
    List<Comment> findByUserAndCampaignAndStatusOrderByCreatedAtDesc(User user, Campaign campaign, Comment.CommentStatus status);
    
    // 신고된 댓글 목록 (관리자용)
    @Query("SELECT c FROM Comment c WHERE c.status = 'HIDDEN' AND c.reportReason IS NOT NULL ORDER BY c.updatedAt DESC")
    List<Comment> findReportedComments();
    
    // 캠페인별 댓글 통계
    @Query("SELECT c.status, COUNT(c) FROM Comment c WHERE c.campaign = :campaign GROUP BY c.status")
    List<Object[]> getCommentStatsByCampaign(@Param("campaign") Campaign campaign);
    
    // 사용자별 댓글 통계 (전체 활성 댓글 수)
    long countByUserAndStatus(User user, Comment.CommentStatus status);
    
    // 특정 댓글의 모든 답글 (재귀적으로 모든 레벨의 답글)
    @Query(value = "WITH comment_tree AS (" +
                   "  SELECT id, content, parent_id, user_id, created_at, like_count, 0 as level " +
                   "  FROM comments WHERE id = :commentId AND status = 'ACTIVE' " +
                   "  UNION ALL " +
                   "  SELECT c.id, c.content, c.parent_id, c.user_id, c.created_at, c.like_count, ct.level + 1 " +
                   "  FROM comments c " +
                   "  INNER JOIN comment_tree ct ON c.parent_id = ct.id " +
                   "  WHERE c.status = 'ACTIVE' " +
                   ") " +
                   "SELECT * FROM comment_tree ORDER BY level, created_at",
           nativeQuery = true)
    List<Object[]> findCommentTreeByCommentId(@Param("commentId") Long commentId);
    
    // 캠페인의 일별 댓글 수 통계 (최근 30일)
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM-DD') as date, COUNT(*) as count " +
                   "FROM comments " +
                   "WHERE campaign_id = :campaignId " +
                   "AND status = 'ACTIVE' " +
                   "AND created_at >= :startDate " +
                   "GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD') " +
                   "ORDER BY date",
           nativeQuery = true)
    List<Object[]> getDailyCommentStats(@Param("campaignId") Long campaignId, @Param("startDate") LocalDateTime startDate);
    
    // 댓글 ID로 캠페인과 사용자 정보 포함 조회 (N+1 문제 해결)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.campaign LEFT JOIN FETCH c.user WHERE c.id = :id")
    Optional<Comment> findByIdWithCampaignAndUser(@Param("id") Long id);
}