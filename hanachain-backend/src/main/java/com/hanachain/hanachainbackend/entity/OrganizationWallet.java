package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 조직의 블록체인 지갑을 나타내는 엔티티
 * 각 조직은 하나의 이더리움 지갑을 가지며, 캠페인 수혜자 주소로 자동 매핑됨
 */
@Entity
@Table(name = "organization_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_wallet_seq")
    @SequenceGenerator(
        name = "organization_wallet_seq",
        sequenceName = "organization_wallet_sequence",
        allocationSize = 1,
        initialValue = 1
    )
    @Column(name = "id_org_wallet")
    private Long id;

    /**
     * 소속 조직 (1:1 관계)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organization", nullable = false, unique = true)
    private Organization organization;

    /**
     * 이더리움 지갑 주소 (0x로 시작하는 40자리 16진수)
     */
    @Column(name = "wallet_address", nullable = false, unique = true, length = 50)
    private String walletAddress;

    /**
     * 암호화된 개인키 (AES-256 암호화)
     * 보안상 이유로 API 응답에 절대 포함되어서는 안 됨
     */
    @Column(name = "private_key_encrypted", nullable = false, length = 500)
    private String privateKeyEncrypted;

    /**
     * 지갑 타입 (ETHEREUM, POLYGON, BSC 등)
     */
    @Column(name = "wallet_type", nullable = false, length = 30)
    @Builder.Default
    private String walletType = "ETHEREUM";

    /**
     * 지갑 활성 상태 (1: 활성, 0: 비활성)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 지갑 생성 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 지갑 정보 수정 일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
     * 지갑 주소가 유효한 이더리움 형식인지 검증
     */
    public boolean isValidEthereumAddress() {
        if (walletAddress == null || walletAddress.isEmpty()) {
            return false;
        }
        // 0x로 시작하고 40자리 16진수인지 확인
        return walletAddress.matches("^0x[a-fA-F0-9]{40}$");
    }

    /**
     * 지갑이 활성 상태인지 확인
     */
    public boolean isActive() {
        return isActive != null && isActive;
    }

    /**
     * 지갑 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 지갑 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 보안을 위해 개인키는 절대 로그나 toString에 포함하지 않음
     */
    @Override
    public String toString() {
        return "OrganizationWallet{" +
                "id=" + id +
                ", organizationId=" + (organization != null ? organization.getId() : null) +
                ", walletAddress='" + walletAddress + '\'' +
                ", walletType='" + walletType + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationWallet)) return false;
        OrganizationWallet that = (OrganizationWallet) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
