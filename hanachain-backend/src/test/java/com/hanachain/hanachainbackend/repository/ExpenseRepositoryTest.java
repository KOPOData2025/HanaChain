package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Expense;
import com.hanachain.hanachainbackend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Expense Repository Test")
class ExpenseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExpenseRepository expenseRepository;

    private User user;
    private Campaign campaign;
    private Expense expense1;
    private Expense expense2;
    private Expense expense3;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password")
                .build();
        entityManager.persist(user);

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
        entityManager.persist(campaign);

        expense1 = Expense.builder()
                .amount(new BigDecimal("50000"))
                .title("의료용품 구매")
                .description("산소호흡기 구매")
                .category(Expense.ExpenseCategory.MEDICAL_SUPPLIES)
                .status(Expense.ExpenseStatus.COMPLETED)
                .campaign(campaign)
                .vendor("Medical Supply Co.")
                .expenseDate(LocalDateTime.now().minusDays(2))
                .build();

        expense2 = Expense.builder()
                .amount(new BigDecimal("30000"))
                .title("교통비")
                .description("의료진 교통비")
                .category(Expense.ExpenseCategory.TRANSPORTATION)
                .status(Expense.ExpenseStatus.APPROVED)
                .campaign(campaign)
                .vendor("Transportation Service")
                .expenseDate(LocalDateTime.now().minusDays(1))
                .build();

        expense3 = Expense.builder()
                .amount(new BigDecimal("20000"))
                .title("관리비")
                .description("사무용품 구매")
                .category(Expense.ExpenseCategory.ADMINISTRATIVE)
                .status(Expense.ExpenseStatus.PENDING)
                .campaign(campaign)
                .vendor("Office Supply Store")
                .expenseDate(LocalDateTime.now())
                .build();

        entityManager.persist(expense1);
        entityManager.persist(expense2);
        entityManager.persist(expense3);
        entityManager.flush();
    }

    @Test
    @DisplayName("캠페인별 지출 목록 조회 테스트")
    void testFindByCampaignOrderByCreatedAtDesc() {
        // When
        Page<Expense> expenses = expenseRepository.findByCampaignOrderByCreatedAtDesc(
                campaign, PageRequest.of(0, 10));

        // Then
        assertThat(expenses.getContent()).hasSize(3);
        assertThat(expenses.getContent().get(0)).isEqualTo(expense3); // 가장 최근 생성
    }

    @Test
    @DisplayName("캠페인별 상태별 지출 조회 테스트")
    void testFindByCampaignAndStatusOrderByCreatedAtDesc() {
        // When
        List<Expense> completedExpenses = expenseRepository
                .findByCampaignAndStatusOrderByCreatedAtDesc(campaign, Expense.ExpenseStatus.COMPLETED);

        // Then
        assertThat(completedExpenses).hasSize(1);
        assertThat(completedExpenses.get(0)).isEqualTo(expense1);
    }

    @Test
    @DisplayName("완료된 지출 총액 계산 테스트")
    void testGetTotalCompletedExpenseAmount() {
        // When
        BigDecimal totalAmount = expenseRepository.getTotalCompletedExpenseAmount(campaign);

        // Then
        assertThat(totalAmount).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("승인된 지출 총액 계산 테스트")
    void testGetTotalApprovedExpenseAmount() {
        // When
        BigDecimal totalAmount = expenseRepository.getTotalApprovedExpenseAmount(campaign);

        // Then
        assertThat(totalAmount).isEqualTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("캠페인별 지출 통계 조회 테스트")
    void testGetExpenseStatsByCampaign() {
        // When
        List<Object[]> stats = expenseRepository.getExpenseStatsByCampaign(campaign);

        // Then
        assertThat(stats).isNotEmpty();
        // 각 상태별로 카운트와 총액이 올바르게 집계되는지 확인
        // [status, count, sum] 형태로 반환됨
    }

    @Test
    @DisplayName("카테고리별 지출 조회 테스트")
    void testFindByCampaignAndCategoryOrderByCreatedAtDesc() {
        // When
        List<Expense> medicalExpenses = expenseRepository
                .findByCampaignAndCategoryOrderByCreatedAtDesc(campaign, Expense.ExpenseCategory.MEDICAL_SUPPLIES);

        // Then
        assertThat(medicalExpenses).hasSize(1);
        assertThat(medicalExpenses.get(0)).isEqualTo(expense1);
    }

    @Test
    @DisplayName("상태별 지출 조회 테스트")
    void testFindByStatusOrderByCreatedAtAsc() {
        // When
        List<Expense> pendingExpenses = expenseRepository
                .findByStatusOrderByCreatedAtAsc(Expense.ExpenseStatus.PENDING);

        // Then
        assertThat(pendingExpenses).hasSize(1);
        assertThat(pendingExpenses.get(0)).isEqualTo(expense3);
    }

    @Test
    @DisplayName("기간별 지출 조회 테스트")
    void testFindByCampaignAndExpenseDateBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        LocalDateTime endDate = LocalDateTime.now().minusDays(1);

        // When
        List<Expense> expenses = expenseRepository.findByCampaignAndExpenseDateBetween(
                campaign, startDate, endDate);

        // Then
        assertThat(expenses).hasSize(2); // expense1과 expense2
    }

    @Test
    @DisplayName("벤더별 지출 조회 테스트")
    void testFindByCampaignAndVendorContainingIgnoreCaseOrderByCreatedAtDesc() {
        // When
        List<Expense> expenses = expenseRepository
                .findByCampaignAndVendorContainingIgnoreCaseOrderByCreatedAtDesc(campaign, "Medical");

        // Then
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0)).isEqualTo(expense1);
    }

    @Test
    @DisplayName("영수증이 있는 지출만 조회 테스트")
    void testFindByCampaignAndReceiptUrlIsNotNullOrderByCreatedAtDesc() {
        // Given - 영수증 URL 추가
        expense1.setReceiptUrl("https://example.com/receipt1.jpg");
        entityManager.merge(expense1);
        entityManager.flush();

        // When
        List<Expense> expensesWithReceipts = expenseRepository
                .findByCampaignAndReceiptUrlIsNotNullOrderByCreatedAtDesc(campaign);

        // Then
        assertThat(expensesWithReceipts).hasSize(1);
        assertThat(expensesWithReceipts.get(0)).isEqualTo(expense1);
    }

    @Test
    @DisplayName("지출 ID로 캠페인 포함 조회 테스트")
    void testFindByIdWithCampaign() {
        // When
        var expenseOpt = expenseRepository.findByIdWithCampaign(expense1.getId());

        // Then
        assertThat(expenseOpt).isPresent();
        assertThat(expenseOpt.get().getCampaign()).isEqualTo(campaign);
    }
}