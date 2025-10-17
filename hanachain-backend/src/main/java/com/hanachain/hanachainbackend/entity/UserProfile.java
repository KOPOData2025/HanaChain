package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 프로필 확장 정보 엔티티
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {
    
    @Id
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;
    
    @Column(length = 1000)
    private String bio; // 자기소개
    
    @Column(length = 100)
    private String location; // 위치
    
    @Column(length = 100)
    private String website; // 웹사이트
    
    @Column(length = 50)
    private String occupation; // 직업
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;
    
    public enum ProfileVisibility {
        PUBLIC,    // 공개
        PRIVATE,   // 비공개
        FRIENDS    // 친구만
    }
}