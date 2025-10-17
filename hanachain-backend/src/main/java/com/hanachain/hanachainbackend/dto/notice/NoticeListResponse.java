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
public class NoticeListResponse {
    
    private Long id;
    private String title;
    private Boolean isImportant;
    private Integer viewCount;
    private LocalDateTime createdAt;
    
    public static NoticeListResponse fromEntity(Notice notice) {
        return NoticeListResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .isImportant(notice.getIsImportant())
                .viewCount(notice.getViewCount())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}

