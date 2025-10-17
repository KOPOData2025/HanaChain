package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_seq")
    @SequenceGenerator(name = "comment_seq", sequenceName = "comment_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean anonymous = false;
    
    @Column(length = 50)
    private String commenterName; // 익명이 아닌 경우 표시할 이름
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.ACTIVE;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer replyCount = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null 가능 (비회원 댓글)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    // 대댓글 기능을 위한 자기 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();
    
    @Column(length = 1000)
    private String reportReason; // 신고 사유
    
    // 댓글 삭제 (소프트 삭제)
    public void delete() {
        this.status = CommentStatus.DELETED;
        this.content = "삭제된 댓글입니다.";
    }
    
    // 댓글 숨김 처리 (신고 등으로 인한)
    public void hide(String reason) {
        this.status = CommentStatus.HIDDEN;
        this.reportReason = reason;
    }
    
    // 좋아요 수 증가
    public void incrementLikes() {
        this.likeCount++;
    }
    
    // 좋아요 수 감소
    public void decrementLikes() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
    
    // 대댓글 수 업데이트
    public void updateReplyCount() {
        this.replyCount = (int) replies.stream()
                .filter(reply -> reply.getStatus() == CommentStatus.ACTIVE)
                .count();
    }
    
    // 대댓글 추가
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParent(this);
        updateReplyCount();
    }
    
    public boolean isActive() {
        return status == CommentStatus.ACTIVE;
    }
    
    public boolean isDeleted() {
        return status == CommentStatus.DELETED;
    }
    
    public boolean isHidden() {
        return status == CommentStatus.HIDDEN;
    }
    
    public boolean isParentComment() {
        return parent == null;
    }
    
    public boolean isReply() {
        return parent != null;
    }
    
    public enum CommentStatus {
        ACTIVE,   // 활성
        DELETED,  // 삭제됨
        HIDDEN,   // 숨김 처리 (신고 등)
        BLOCKED   // 차단됨
    }
}