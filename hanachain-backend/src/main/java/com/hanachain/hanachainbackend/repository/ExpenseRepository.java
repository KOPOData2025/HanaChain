package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    // 특정 캠페인의 지출 목록 조회 (페이징)
    Page<Expense> findByCampaignOrderByCreatedAtDesc(Campaign campaign, Pageable pageable);
    
    // 특정 캠페인의 지출 목록 조회 (상태별)
    List<Expense> findByCampaignAndStatusOrderByCreatedAtDesc(Campaign campaign, Expense.ExpenseStatus status);
    
    // 특정 캠페인의 완료된 지출 총액 계산
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.campaign = :campaign AND e.status = 'COMPLETED'")
    BigDecimal getTotalCompletedExpenseAmount(@Param("campaign") Campaign campaign);
    
    // 특정 캠페인의 승인된 지출 총액 계산
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.campaign = :campaign AND e.status = 'APPROVED'")
    BigDecimal getTotalApprovedExpenseAmount(@Param("campaign") Campaign campaign);
    
    // 특정 캠페인의 지출 현황 통계
    @Query("SELECT e.status, COUNT(e), COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.campaign = :campaign GROUP BY e.status")
    List<Object[]> getExpenseStatsByCampaign(@Param("campaign") Campaign campaign);
    
    // 특정 카테고리별 지출 현황
    List<Expense> findByCampaignAndCategoryOrderByCreatedAtDesc(Campaign campaign, Expense.ExpenseCategory category);
    
    // 승인 대기 중인 지출 목록
    List<Expense> findByStatusOrderByCreatedAtAsc(Expense.ExpenseStatus status);
    
    // 특정 기간 내 지출 목록
    @Query("SELECT e FROM Expense e WHERE e.campaign = :campaign AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByCampaignAndExpenseDateBetween(
        @Param("campaign") Campaign campaign,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // 특정 승인자가 승인한 지출 목록
    @Query("SELECT e FROM Expense e WHERE e.approvedBy.id = :approverId ORDER BY e.approvedAt DESC")
    List<Expense> findByApprovedByIdOrderByApprovedAtDesc(@Param("approverId") Long approverId);
    
    // 캠페인별 월별 지출 통계
    @Query(value = "SELECT TO_CHAR(expense_date, 'YYYY-MM') as month, " +
                   "COUNT(*) as count, " +
                   "SUM(amount) as total_amount " +
                   "FROM expenses " +
                   "WHERE campaign_id = :campaignId " +
                   "AND status = 'COMPLETED' " +
                   "AND expense_date >= :startDate " +
                   "GROUP BY TO_CHAR(expense_date, 'YYYY-MM') " +
                   "ORDER BY month", 
           nativeQuery = true)
    List<Object[]> getMonthlyExpenseStats(@Param("campaignId") Long campaignId, @Param("startDate") LocalDateTime startDate);
    
    // 지출 영수증이 있는 지출만 조회
    List<Expense> findByCampaignAndReceiptUrlIsNotNullOrderByCreatedAtDesc(Campaign campaign);
    
    // 특정 벤더의 지출 목록
    List<Expense> findByCampaignAndVendorContainingIgnoreCaseOrderByCreatedAtDesc(Campaign campaign, String vendor);
    
    // 지출 ID로 캠페인 포함 조회 (N+1 문제 해결)
    @Query("SELECT e FROM Expense e JOIN FETCH e.campaign WHERE e.id = :id")
    Optional<Expense> findByIdWithCampaign(@Param("id") Long id);
}