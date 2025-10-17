package com.hanachain.hanachainbackend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DisplayName("Expense Entity Test")
class ExpenseEntityTest {

    private Campaign campaign;
    private User user;
    private User approver;
    private Expense expense;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .build();

        approver = User.builder()
                .email("admin@example.com")
                .name("Admin User")
                .password("password")
                .role(User.Role.ADMIN)
                .build();

        campaign = Campaign.builder()
                .title("Test Campaign")
                .description("Test Description")
                .targetAmount(new BigDecimal("1000000"))
                .currentAmount(new BigDecimal("500000"))
                .category(Campaign.CampaignCategory.MEDICAL)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(30))
                .user(user)
                .build();

        expense = Expense.builder()
                .amount(new BigDecimal("50000"))
                .title("의료용품 구매")
                .description("산소호흡기 구매")
                .category(Expense.ExpenseCategory.MEDICAL_SUPPLIES)
                .campaign(campaign)
                .vendor("Medical Supply Co.")
                .expenseDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Expense 엔티티 생성 테스트")
    void testExpenseCreation() {
        assertThat(expense.getAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(expense.getTitle()).isEqualTo("의료용품 구매");
        assertThat(expense.getCategory()).isEqualTo(Expense.ExpenseCategory.MEDICAL_SUPPLIES);
        assertThat(expense.getStatus()).isEqualTo(Expense.ExpenseStatus.PENDING);
        assertThat(expense.getCampaign()).isEqualTo(campaign);
        assertThat(expense.isPending()).isTrue();
    }

    @Test
    @DisplayName("지출 승인 테스트")
    void testExpenseApproval() {
        // When
        expense.approve(approver);
        
        // Then
        assertThat(expense.getStatus()).isEqualTo(Expense.ExpenseStatus.APPROVED);
        assertThat(expense.getApprovedBy()).isEqualTo(approver);
        assertThat(expense.getApprovedAt()).isNotNull();
        assertThat(expense.isApproved()).isTrue();
        assertThat(expense.isPending()).isFalse();
    }

    @Test
    @DisplayName("지출 거부 테스트")
    void testExpenseRejection() {
        // Given
        String rejectionReason = "영수증이 불충분합니다.";
        
        // When
        expense.reject(rejectionReason);
        
        // Then
        assertThat(expense.getStatus()).isEqualTo(Expense.ExpenseStatus.REJECTED);
        assertThat(expense.getRejectionReason()).isEqualTo(rejectionReason);
    }

    @Test
    @DisplayName("지출 완료 처리 테스트")
    void testMarkAsCompleted() {
        // Given
        expense.approve(approver);
        
        // When
        expense.markAsCompleted();
        
        // Then
        assertThat(expense.getStatus()).isEqualTo(Expense.ExpenseStatus.COMPLETED);
        assertThat(expense.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Expense Builder 패턴 테스트")
    void testExpenseBuilder() {
        // Given & When
        Expense newExpense = Expense.builder()
                .amount(new BigDecimal("30000"))
                .title("교통비")
                .description("의료진 교통비")
                .category(Expense.ExpenseCategory.TRANSPORTATION)
                .campaign(campaign)
                .vendor("Transportation Service")
                .build();

        // Then
        assertThat(newExpense.getAmount()).isEqualTo(new BigDecimal("30000"));
        assertThat(newExpense.getTitle()).isEqualTo("교통비");
        assertThat(newExpense.getCategory()).isEqualTo(Expense.ExpenseCategory.TRANSPORTATION);
        assertThat(newExpense.getStatus()).isEqualTo(Expense.ExpenseStatus.PENDING);
    }

    @Test
    @DisplayName("ExpenseStatus enum 테스트")
    void testExpenseStatusEnum() {
        assertThat(Expense.ExpenseStatus.PENDING).isNotNull();
        assertThat(Expense.ExpenseStatus.APPROVED).isNotNull();
        assertThat(Expense.ExpenseStatus.REJECTED).isNotNull();
        assertThat(Expense.ExpenseStatus.COMPLETED).isNotNull();
        assertThat(Expense.ExpenseStatus.CANCELLED).isNotNull();
    }

    @Test
    @DisplayName("ExpenseCategory enum 테스트")
    void testExpenseCategoryEnum() {
        assertThat(Expense.ExpenseCategory.MEDICAL_SUPPLIES).isNotNull();
        assertThat(Expense.ExpenseCategory.MEDICAL_TREATMENT).isNotNull();
        assertThat(Expense.ExpenseCategory.EDUCATIONAL_MATERIALS).isNotNull();
        assertThat(Expense.ExpenseCategory.TRANSPORTATION).isNotNull();
        assertThat(Expense.ExpenseCategory.OTHER).isNotNull();
    }
}