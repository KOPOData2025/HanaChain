package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_seq")
    @SequenceGenerator(name = "notice_seq", sequenceName = "notice_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "CLOB")
    private String content;
    
    @Column(name = "is_important")
    @Builder.Default
    private Boolean isImportant = false;
    
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;
    
    // 조회수 증가 메서드
    public void incrementViewCount() {
        this.viewCount++;
    }
}

