package com.hanachain.hanachainbackend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAuthor {

    private Long id;
    private String name;
    private String email;
    private String profileImageUrl;
    private String nickname;
}
