package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "expense_seq")
    @SequenceGenerator(name = "expense_seq", sequenceName = "expense_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "CLOB")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.PENDING;
    
    @Column(length = 1000)
    private String receiptUrl; // 영수증 이미지 URL
    
    @Column(length = 100)
    private String vendor; // 지출 업체명
    
    @Column
    private LocalDateTime expenseDate; // 실제 지출 발생 날짜
    
    @Column
    private LocalDateTime approvedAt;
    
    @Column(length = 1000)
    private String rejectionReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy; // 승인한 관리자
    
    // 승인 처리
    public void approve(User approver) {
        this.status = ExpenseStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
    }
    
    // 거부 처리
    public void reject(String reason) {
        this.status = ExpenseStatus.REJECTED;
        this.rejectionReason = reason;
    }
    
    // 지출 완료 처리
    public void markAsCompleted() {
        this.status = ExpenseStatus.COMPLETED;
    }
    
    public boolean isPending() {
        return status == ExpenseStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == ExpenseStatus.APPROVED;
    }
    
    public boolean isCompleted() {
        return status == ExpenseStatus.COMPLETED;
    }
    
    public enum ExpenseStatus {
        PENDING,    // 승인 대기
        APPROVED,   // 승인됨
        REJECTED,   // 거부됨
        COMPLETED,  // 지출 완료
        CANCELLED   // 취소됨
    }
    
    public enum ExpenseCategory {
        MEDICAL_SUPPLIES,    // 의료용품
        MEDICAL_TREATMENT,   // 의료비
        EDUCATIONAL_MATERIALS, // 교육자료
        TUITION_FEES,       // 학비
        EMERGENCY_RELIEF,   // 긴급구호
        FOOD_SUPPLIES,      // 식료품
        SHELTER_MATERIALS,  // 주거용품
        TRANSPORTATION,     // 교통비
        ADMINISTRATIVE,     // 관리비용
        EQUIPMENT,          // 장비구입
        MARKETING,          // 홍보비
        OTHER              // 기타
    }
}