package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_managers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignManager extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "campaign_manager_seq")
    @SequenceGenerator(name = "campaign_manager_seq", sequenceName = "campaign_manager_sequence", allocationSize = 1)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ManagerRole role = ManagerRole.MANAGER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ManagerStatus status = ManagerStatus.ACTIVE;
    
    @Column(nullable = false)
    private LocalDateTime assignedAt;
    
    @Column
    private LocalDateTime revokedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedBy;
    
    @Column(length = 500)
    private String notes;
    
    /**
     * 담당자 권한이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == ManagerStatus.ACTIVE && revokedAt == null;
    }
    
    /**
     * 담당자 권한 비활성화
     */
    public void revoke() {
        this.status = ManagerStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }
    
    /**
     * 담당자 권한 복원
     */
    public void restore() {
        this.status = ManagerStatus.ACTIVE;
        this.revokedAt = null;
    }
    
    public enum ManagerRole {
        MANAGER("담당자"),
        CO_MANAGER("부담당자");
        
        private final String displayName;
        
        ManagerRole(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum ManagerStatus {
        ACTIVE("활성"),
        REVOKED("해제"),
        SUSPENDED("일시정지");
        
        private final String displayName;
        
        ManagerStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}