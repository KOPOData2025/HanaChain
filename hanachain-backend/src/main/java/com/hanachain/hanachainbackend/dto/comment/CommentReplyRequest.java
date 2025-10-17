package com.hanachain.hanachainbackend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyRequest {

    @NotBlank(message = "답글 내용은 필수입니다")
    @Size(min = 1, max = 1000, message = "답글 내용은 1자 이상 1000자 이하여야 합니다")
    private String content;
}
