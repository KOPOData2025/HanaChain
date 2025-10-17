package com.hanachain.hanachainbackend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 작성자 정보
    private CommentAuthor author;

    // 권한 및 상태 정보
    private Boolean isCampaignManager; // 캠페인 담당자 여부
    private Boolean hasDonated;        // 기부 이력 여부 (천사 로고 표시용)
    private Boolean isDeleted;         // 삭제 여부

    // 대댓글 정보
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();

    // 통계 정보
    private Integer likeCount;
    private Integer replyCount;
}
