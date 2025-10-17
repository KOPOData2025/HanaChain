package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campaign_stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignStory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "story_seq")
    @SequenceGenerator(name = "story_seq", sequenceName = "story_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "CLOB")
    private String content;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StoryType type = StoryType.UPDATE;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
    
    public enum StoryType {
        INITIAL,    // 초기 캠페인 스토리
        UPDATE,     // 진행 상황 업데이트
        MILESTONE,  // 마일스톤 달성
        COMPLETION, // 완료 보고
        THANK_YOU   // 감사 인사
    }
}
