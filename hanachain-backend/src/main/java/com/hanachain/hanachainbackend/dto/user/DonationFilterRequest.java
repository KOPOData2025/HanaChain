package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 기부 이력 필터링 및 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationFilterRequest {
    
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    @Builder.Default
    private Integer page = 0;
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Builder.Default
    private Integer size = 20;
    
    // 기부 상태 필터링
    @Pattern(regexp = "^(completed|pending|failed|cancelled)?$", 
             message = "유효하지 않은 상태입니다")
    private String status;
    
    // 캠페인 제목 검색
    private String search;
    
    // 정렬 기준
    @Pattern(regexp = "^(date|amount)$", 
             message = "정렬 기준은 date 또는 amount만 가능합니다")
    @Builder.Default
    private String sortBy = "date";
    
    // 정렬 순서
    @Pattern(regexp = "^(asc|desc)$", 
             message = "정렬 순서는 asc 또는 desc만 가능합니다")
    @Builder.Default
    private String sortOrder = "desc";
}