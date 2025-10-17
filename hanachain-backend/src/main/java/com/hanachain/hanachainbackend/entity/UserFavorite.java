package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;

/**
 * 사용자 즐겨찾기 캠페인 엔티티 (M:N 관계 매핑)
 */
@Entity
@Table(name = "user_favorites", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_campaign_favorite", columnNames = {"user_id", "campaign_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavorite extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_favorite_seq")
    @SequenceGenerator(name = "user_favorite_seq", sequenceName = "user_favorite_sequence", allocationSize = 1)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @Column(length = 500)
    private String memo; // 개인 메모
    
    // 중복 즐겨찾기 방지를 위한 유니크 제약조건은 @Table 어노테이션으로 클래스 레벨에서 정의
}