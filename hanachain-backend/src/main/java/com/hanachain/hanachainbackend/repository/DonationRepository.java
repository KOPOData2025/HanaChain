package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Donation;
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
public interface DonationRepository extends JpaRepository<Donation, Long> {
    
    Optional<Donation> findByPaymentId(String paymentId);
    
    @Query("SELECT d FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED' ORDER BY d.paidAt DESC")
    Page<Donation> findByCampaignIdAndCompleted(@Param("campaignId") Long campaignId, Pageable pageable);
    
    @Query("SELECT d FROM Donation d WHERE d.user.id = :userId ORDER BY d.createdAt DESC")
    Page<Donation> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT d FROM Donation d WHERE d.paymentStatus = :status ORDER BY d.createdAt DESC")
    Page<Donation> findByPaymentStatus(@Param("status") Donation.PaymentStatus status, Pageable pageable);
    
    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalAmountByCampaign(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(DISTINCT d.user.id) FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED' AND d.user IS NOT NULL")
    long getUniqueDonorCountByCampaign(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(d) FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED'")
    long getTotalDonationCountByCampaign(@Param("campaignId") Long campaignId);
    
    @Query("SELECT d FROM Donation d WHERE d.paidAt BETWEEN :startDate AND :endDate AND d.paymentStatus = 'COMPLETED'")
    List<Donation> findCompletedDonationsBetweenDates(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT d FROM Donation d WHERE d.paymentStatus = 'PENDING' AND d.createdAt < :before")
    List<Donation> findPendingDonationsOlderThan(@Param("before") LocalDateTime before);
    
    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalCompletedAmount();
    
    @Query("SELECT COUNT(d) FROM Donation d WHERE d.paymentStatus = 'COMPLETED'")
    long getTotalCompletedCount();
    
    @Query("SELECT d FROM Donation d WHERE d.paymentStatus = 'COMPLETED' ORDER BY d.amount DESC")
    Page<Donation> findTopDonations(Pageable pageable);
    
    /**
     * 관리자용 전체 기부 내역 조회 (검색 지원)
     */
    @Query("SELECT d FROM Donation d LEFT JOIN d.user u LEFT JOIN d.campaign c " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(COALESCE(d.donorName, u.name, '익명')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.paymentId) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Donation> findAllWithKeywordSearch(@Param("keyword") String keyword, Pageable pageable);
    
    // === 마이페이지 전용 쿼리 ===
    
    /**
     * 사용자별 기부 이력 조회 (상태 필터링 및 정렬 지원)
     */
    @Query("SELECT d FROM Donation d WHERE d.user.id = :userId " +
           "AND (:status IS NULL OR d.paymentStatus = :status) " +
           "AND (:search IS NULL OR LOWER(d.campaign.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'amount' AND :sortOrder = 'desc' THEN d.amount END DESC, " +
           "CASE WHEN :sortBy = 'amount' AND :sortOrder = 'asc' THEN d.amount END ASC, " +
           "CASE WHEN :sortBy = 'date' AND :sortOrder = 'desc' THEN d.createdAt END DESC, " +
           "CASE WHEN :sortBy = 'date' AND :sortOrder = 'asc' THEN d.createdAt END ASC")
    Page<Donation> findUserDonationsWithFilters(@Param("userId") Long userId,
                                               @Param("status") Donation.PaymentStatus status,
                                               @Param("search") String search,
                                               @Param("sortBy") String sortBy,
                                               @Param("sortOrder") String sortOrder,
                                               Pageable pageable);
    
    /**
     * 사용자의 기부 통계 조회
     * TODO: 프로덕션에서는 COMPLETED만 집계하도록 수정 필요
     */
    @Query("SELECT " +
           "SUM(d.amount) as totalAmount, " + // 모든 기부 금액 합산 (임시)
           "COUNT(d) as totalCount, " +
           "SUM(CASE WHEN d.paymentStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
           "SUM(CASE WHEN d.paymentStatus IN ('PENDING', 'PROCESSING') THEN 1 ELSE 0 END) as pendingCount, " +
           "SUM(CASE WHEN d.paymentStatus = 'FAILED' THEN 1 ELSE 0 END) as failedCount " +
           "FROM Donation d WHERE d.user.id = :userId")
    Object[] getUserDonationStats(@Param("userId") Long userId);
    
    /**
     * 사용자의 최근 기부 내역 조회
     */
    @Query("SELECT d FROM Donation d WHERE d.user.id = :userId " +
           "ORDER BY d.createdAt DESC")
    List<Donation> findRecentUserDonations(@Param("userId") Long userId, Pageable pageable);

    // === 배치 처리 전용 쿼리 ===

    /**
     * 배치 처리 대상 기부 내역 조회
     * (완료된 결제 중 블록체인에 기록되지 않은 항목)
     */
    @Query("SELECT d FROM Donation d " +
           "WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND (d.blockchainRecorded = false OR d.blockchainRecorded IS NULL) " +
           "ORDER BY d.paidAt ASC")
    Page<Donation> findPendingBlockchainRecords(@Param("campaignId") Long campaignId, Pageable pageable);

    /**
     * 배치 처리 대상 기부 건수 조회
     */
    @Query("SELECT COUNT(d) FROM Donation d " +
           "WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND (d.blockchainRecorded = false OR d.blockchainRecorded IS NULL)")
    long countPendingBlockchainRecords(@Param("campaignId") Long campaignId);

    // === 댓글 시스템 전용 쿼리 ===

    /**
     * 특정 사용자가 특정 캠페인에 완료된 기부 이력이 있는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END " +
           "FROM Donation d " +
           "WHERE d.user.id = :userId " +
           "AND d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED'")
    boolean hasUserDonatedToCampaign(@Param("userId") Long userId,
                                     @Param("campaignId") Long campaignId);

    // === 캠페인 모금 통계 전용 쿼리 ===

    /**
     * 캠페인별 고유 기부자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT d.user.id) FROM Donation d " +
           "WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND d.user IS NOT NULL")
    Integer countDistinctUserIdByCampaignId(@Param("campaignId") Long campaignId);

    /**
     * 캠페인별 일별 기부 통계 조회 (날짜, 총액, 건수)
     */
    @Query("SELECT FUNCTION('TO_CHAR', d.paidAt, 'YYYY-MM-DD') as date, " +
           "SUM(d.amount) as amount, " +
           "COUNT(d) as count " +
           "FROM Donation d " +
           "WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND d.paidAt BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('TO_CHAR', d.paidAt, 'YYYY-MM-DD') " +
           "ORDER BY date ASC")
    List<Object[]> findDailyDonationStats(@Param("campaignId") Long campaignId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 캠페인별 상위 기부 목록 조회
     */
    @Query("SELECT " +
           "COALESCE(d.donorName, u.name, '익명') as donorName, " +
           "d.amount, " +
           "d.paidAt, " +
           "d.anonymous " +
           "FROM Donation d " +
           "LEFT JOIN d.user u " +
           "WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "ORDER BY d.amount DESC")
    List<Object[]> findTopDonationsByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

    // === FDS (사기 탐지 시스템) 검증 전용 쿼리 ===

    /**
     * 캠페인의 FDS 검증 미통과 거래 존재 여부 확인
     * - fdsAction이 'BLOCK' 또는 'MANUAL_REVIEW'인 경우
     * - fdsStatus가 'SUCCESS'가 아닌 경우 (PENDING, FAILED, TIMEOUT)
     * - paymentStatus가 'COMPLETED'인 거래만 체크 (완료된 결제만 검증 대상)
     *
     * @param campaignId 캠페인 ID
     * @return FDS 검증 미통과 거래 존재 여부
     */
    @Query("SELECT COUNT(d) > 0 FROM Donation d WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND (d.fdsAction IN ('BLOCK', 'MANUAL_REVIEW') OR d.fdsStatus != 'SUCCESS')")
    boolean existsUnverifiedFdsDonations(@Param("campaignId") Long campaignId);

    /**
     * 캠페인의 FDS 검증 미통과 거래 수 조회
     *
     * @param campaignId 캠페인 ID
     * @return FDS 검증 미통과 거래 건수
     */
    @Query("SELECT COUNT(d) FROM Donation d WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND (d.fdsAction IN ('BLOCK', 'MANUAL_REVIEW') OR d.fdsStatus != 'SUCCESS')")
    long countUnverifiedFdsDonations(@Param("campaignId") Long campaignId);

    /**
     * 캠페인의 FDS 검증 미통과 거래 목록 조회
     * 최신 거래부터 정렬하여 반환
     *
     * @param campaignId 캠페인 ID
     * @return FDS 검증 미통과 거래 목록
     */
    @Query("SELECT d FROM Donation d " +
           "JOIN FETCH d.campaign c " +
           "JOIN FETCH c.user cu " +
           "LEFT JOIN FETCH d.user u " +
           "WHERE d.campaign.id = :campaignId " +
           "AND d.paymentStatus = 'COMPLETED' " +
           "AND (d.fdsAction IN ('BLOCK', 'MANUAL_REVIEW') OR d.fdsStatus != 'SUCCESS') " +
           "ORDER BY d.createdAt DESC")
    List<Donation> findUnverifiedFdsDonationsByCampaignId(@Param("campaignId") Long campaignId);

    // === 블록체인 트랜잭션 조회 전용 쿼리 ===

    /**
     * 트랜잭션 해시로 기부 정보 조회
     * 블록체인 이벤트에서 기부자 이름을 표시하기 위해 사용
     *
     * @param txHash 블록체인 트랜잭션 해시
     * @return 기부 정보 (Optional)
     */
    @Query("SELECT d FROM Donation d LEFT JOIN FETCH d.user u " +
           "WHERE d.donationTransactionHash = :txHash")
    Optional<Donation> findByDonationTransactionHash(@Param("txHash") String txHash);
}
