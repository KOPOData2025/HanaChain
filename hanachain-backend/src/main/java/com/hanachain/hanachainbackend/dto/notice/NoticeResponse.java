package com.hanachain.hanachainbackend.dto.notice;

import com.hanachain.hanachainbackend.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResponse {
    
    private Long id;
    private String title;
    private String content;
    private Boolean isImportant;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static NoticeResponse fromEntity(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .isImportant(notice.getIsImportant())
                .viewCount(notice.getViewCount())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}

