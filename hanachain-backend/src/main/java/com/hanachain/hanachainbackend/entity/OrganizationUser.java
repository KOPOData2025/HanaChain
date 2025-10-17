package com.hanachain.hanachainbackend.entity;

import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 조직과 사용자 간의 다대다 관계를 나타내는 OrganizationUser 엔티티
 * 추가 역할 정보를 포함합니다
 */
@Entity
@Table(name = "organization_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE organization_users SET deleted_at = CURRENT_TIMESTAMP WHERE id_organization_user = ?")
@Where(clause = "deleted_at IS NULL")
public class OrganizationUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "org_user_seq")
    @SequenceGenerator(
        name = "org_user_seq", 
        sequenceName = "organization_user_sequence", 
        allocationSize = 1,
        initialValue = 1
    )
    @Column(name = "id_organization_user")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organization", nullable = false)
    private Organization organization;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OrganizationRole role;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생명주기 콜백
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드

    /**
     * 이 조직 멤버십이 활성 상태인지 확인합니다
     */
    public boolean isActive() {
        return deletedAt == null &&
               user.getEnabled() &&
               organization.isActive();
    }

    /**
     * 이 멤버십이 소프트 삭제되었는지 확인합니다
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 사용자가 이 조직에서 관리자 역할을 가지고 있는지 확인합니다
     */
    public boolean isAdmin() {
        return role.isAdmin();
    }

    /**
     * 사용자가 이 조직을 관리할 수 있는지 확인합니다
     */
    public boolean canManageOrganization() {
        return role.canManageOrganization() && isActive();
    }

    /**
     * 사용자가 이 조직의 멤버를 관리할 수 있는지 확인합니다
     */
    public boolean canManageMembers() {
        return role.canManageMembers() && isActive();
    }

    /**
     * 사용자가 이 조직을 위한 캠페인을 생성할 수 있는지 확인합니다
     */
    public boolean canCreateCampaigns() {
        return role.canCreateCampaigns() && isActive();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationUser)) return false;
        OrganizationUser that = (OrganizationUser) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "OrganizationUser{" +
                "id=" + id +
                ", role=" + role +
                ", createdAt=" + createdAt +
                ", organizationId=" + (organization != null ? organization.getId() : null) +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }
}