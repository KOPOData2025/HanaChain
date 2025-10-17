package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이지네이션된 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
    
    private List<T> content;
    private int page;
    private int limit;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
}