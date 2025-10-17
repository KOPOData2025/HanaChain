package com.hanachain.hanachainbackend.entity;

import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 하나체인 플랫폼의 조직을 나타내는 엔티티
 * deleted_at 컬럼을 통한 소프트 삭제 기능 지원
 * BaseEntity를 상속받아 표준 감사 필드(createdAt, updatedAt, deletedAt) 사용
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE organizations SET deleted_at = CURRENT_TIMESTAMP WHERE id_organization = ?")
@Where(clause = "deleted_at IS NULL")
public class Organization extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_seq")
    @SequenceGenerator(
        name = "organization_seq", 
        sequenceName = "organization_sequence", 
        allocationSize = 1,
        initialValue = 1
    )
    @Column(name = "id_organization")
    private Long id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Lob
    @Column(name = "description")
    private String description;
    
    @Column(name = "image_url", length = 512)
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    // 관계 정의
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrganizationUser> members = new ArrayList<>();

    /**
     * 조직의 블록체인 지갑 (1:1 관계)
     * 캠페인 생성 시 수혜자 주소로 자동 매핑됨
     */
    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private OrganizationWallet wallet;

    // 비즈니스 메서드

    /**
     * 조직이 활성 상태이고 소프트 삭제되지 않았는지 확인합니다
     */
    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE && !isDeleted();
    }

    /**
     * 활성(enabled) 멤버 수를 가져옵니다
     */
    public long getActiveMemberCount() {
        return members.stream()
            .filter(member -> member.getUser().getEnabled())
            .count();
    }

    /**
     * 조직 관리자 수를 가져옵니다
     */
    public long getAdminCount() {
        return members.stream()
            .filter(member -> member.getRole().isAdmin())
            .filter(member -> member.getUser().getEnabled())
            .count();
    }
    
    /**
     * 조직 캠페인으로 모금된 총액을 가져옵니다
     * 참고: 조직 멤버의 캠페인을 쿼리하는 서비스를 통해 계산되어야 합니다
     */
    public BigDecimal getTotalRaised() {
        // 이 메서드는 서비스 레이어에서 구현되어야 합니다
        // 캠페인 사용자가 이 조직의 멤버인 캠페인을 쿼리하여 계산
        return BigDecimal.ZERO;
    }

    /**
     * 활성 캠페인 수를 가져옵니다
     * 참고: 조직 멤버의 캠페인을 쿼리하는 서비스를 통해 계산되어야 합니다
     */
    public long getActiveCampaignCount() {
        // 이 메서드는 서비스 레이어에서 구현되어야 합니다
        // 캠페인 사용자가 이 조직의 멤버인 캠페인을 쿼리하여 계산
        return 0L;
    }

    // 멤버 관리를 위한 헬퍼 메서드

    /**
     * 조직에 멤버를 추가합니다
     */
    public void addMember(User user, com.hanachain.hanachainbackend.entity.enums.OrganizationRole role) {
        OrganizationUser orgUser = OrganizationUser.builder()
            .organization(this)
            .user(user)
            .role(role)
            .createdAt(LocalDateTime.now())
            .build();
        members.add(orgUser);
    }

    /**
     * 조직에서 멤버를 제거합니다
     */
    public void removeMember(User user) {
        members.removeIf(member -> member.getUser().equals(user));
    }

    /**
     * 사용자가 이 조직의 멤버인지 확인합니다
     */
    public boolean hasMember(User user) {
        return members.stream()
            .anyMatch(member -> member.getUser().equals(user));
    }

    /**
     * 이 조직에서 사용자의 역할을 가져옵니다
     */
    public com.hanachain.hanachainbackend.entity.enums.OrganizationRole getUserRole(User user) {
        return members.stream()
            .filter(member -> member.getUser().equals(user))
            .map(OrganizationUser::getRole)
            .findFirst()
            .orElse(null);
    }

    /**
     * 사용자가 이 조직의 관리자인지 확인합니다
     */
    public boolean isUserAdmin(User user) {
        return members.stream()
            .anyMatch(member ->
                member.getUser().equals(user) &&
                member.getRole().isAdmin()
            );
    }

    /**
     * 관리자 사용자 목록을 가져옵니다
     */
    public List<User> getAdminUsers() {
        return members.stream()
            .filter(member -> member.getRole().isAdmin())
            .filter(member -> member.getUser().getEnabled())
            .map(OrganizationUser::getUser)
            .toList();
    }

    /**
     * 조직을 안전하게 삭제할 수 있는지 확인합니다
     * (활성 캠페인이 없고, 최소 한 명의 관리자가 남아있어야 함)
     */
    public boolean canBeDeleted() {
        // 현재는 삭제 허용 - 활성 캠페인 확인은 서비스 레이어에서 구현되어야 합니다
        return true;
    }

    /**
     * 목록 뷰를 위한 축약된 설명을 가져옵니다
     */
    public String getTruncatedDescription(int maxLength) {
        if (description == null) return null;
        if (description.length() <= maxLength) return description;
        return description.substring(0, maxLength) + "...";
    }

    /**
     * 조직의 블록체인 지갑 주소를 가져옵니다
     * 지갑이 없으면 null 반환
     */
    public String getWalletAddress() {
        return wallet != null ? wallet.getWalletAddress() : null;
    }

    /**
     * 조직이 블록체인 지갑을 가지고 있는지 확인합니다
     */
    public boolean hasWallet() {
        return wallet != null && wallet.isActive();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization)) return false;
        Organization that = (Organization) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", memberCount=" + (members != null ? members.size() : 0) +
                '}';
    }
}