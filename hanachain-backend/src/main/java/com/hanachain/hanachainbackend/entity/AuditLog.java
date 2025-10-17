package com.hanachain.hanachainbackend.entity;

import com.hanachain.hanachainbackend.entity.enums.AuditAction;
import com.hanachain.hanachainbackend.entity.enums.AuditCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for audit logging
 * Tracks all security-related actions and admin operations
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_category", columnList = "category"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_ip_address", columnList = "ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "audit_log_sequence", allocationSize = 1)
    private Long id;
    
    @Column(name = "user_id", nullable = true)
    private Long userId;
    
    @Column(name = "user_email", length = 100)
    private String userEmail;
    
    @Column(name = "user_role", length = 50)
    private String userRole;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditCategory category;
    
    @Column(nullable = false, length = 500)
    private String description;
    
    @Column(name = "target_entity_type", length = 100)
    private String targetEntityType;
    
    @Column(name = "target_entity_id")
    private Long targetEntityId;
    
    @Column(name = "organization_id")
    private Long organizationId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "request_method", length = 10)
    private String requestMethod;
    
    @Column(name = "request_url", length = 1000)
    private String requestUrl;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Lob
    @Column(name = "request_body")
    private String requestBody;
    
    @Lob
    @Column(name = "additional_data")
    private String additionalData;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "risk_level", length = 20)
    @Builder.Default
    private String riskLevel = "LOW";
    
    /**
     * Create audit log for successful action
     */
    public static AuditLog success(AuditAction action, AuditCategory category, String description) {
        return AuditLog.builder()
                .action(action)
                .category(category)
                .description(description)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create audit log for failed action
     */
    public static AuditLog failure(AuditAction action, AuditCategory category, String description, String errorMessage) {
        return AuditLog.builder()
                .action(action)
                .category(category)
                .description(description)
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .riskLevel("HIGH")
                .build();
    }
    
    /**
     * Add user context to audit log
     */
    public AuditLog withUser(User user) {
        if (user != null) {
            this.userId = user.getId();
            this.userEmail = user.getEmail();
            this.userRole = user.getRole().name();
        }
        return this;
    }
    
    /**
     * Add request context to audit log
     */
    public AuditLog withRequest(String method, String url, String ipAddress, String userAgent) {
        this.requestMethod = method;
        this.requestUrl = url;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        return this;
    }
    
    /**
     * Add target entity context to audit log
     */
    public AuditLog withTarget(String entityType, Long entityId) {
        this.targetEntityType = entityType;
        this.targetEntityId = entityId;
        return this;
    }
    
    /**
     * Add organization context to audit log
     */
    public AuditLog withOrganization(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }
    
    /**
     * Set risk level for the audit log
     */
    public AuditLog withRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        return this;
    }
    
    /**
     * Add additional data as JSON string
     */
    public AuditLog withAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        return this;
    }
}