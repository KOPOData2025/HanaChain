package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 즐겨찾기 추가 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteAddRequest {
    
    @NotNull(message = "캠페인 ID는 필수입니다")
    @Positive(message = "캠페인 ID는 양수여야 합니다")
    private Long campaignId;
    
    @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
    private String memo;
}