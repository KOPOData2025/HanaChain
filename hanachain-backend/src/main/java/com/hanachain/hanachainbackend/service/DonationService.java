package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.donation.*;
import com.hanachain.hanachainbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 기부 서비스 인터페이스
 */
public interface DonationService {
    
    /**
     * 기부 생성 (결제 전 사전 등록)
     * @param requestDto 기부 생성 요청 DTO
     * @return 생성된 기부 정보
     */
    DonationResponse createDonation(DonationCreateRequest requestDto);
    
    /**
     * 결제 완료 처리 (웹훅에서 호출)
     * @param paymentId 결제 ID
     * @param webhookDto 웹훅 데이터
     * @return 처리 결과
     */
    DonationResponse processDonationPayment(String paymentId, PaymentWebhook webhookDto);
    
    /**
     * 기부 정보 조회
     * @param donationId 기부 ID
     * @return 기부 정보
     */
    DonationResponse getDonation(Long donationId);
    
    /**
     * 결제 ID로 기부 조회
     * @param paymentId 결제 ID
     * @return 기부 정보
     */
    DonationResponse getDonationByPaymentId(String paymentId);
    
    /**
     * 전체 기부 내역 조회 (관리자용)
     * @param pageable 페이징 정보
     * @param keyword 검색 키워드 (기부자명, 캠페인명)
     * @return 기부 내역 목록
     */
    Page<DonationResponse> getAllDonations(Pageable pageable, String keyword);
    
    /**
     * 사용자별 기부 내역 조회
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 기부 내역 목록
     */
    Page<DonationResponse> getUserDonations(Long userId, Pageable pageable);
    
    /**
     * 캠페인별 기부 내역 조회
     * @param campaignId 캠페인 ID
     * @param pageable 페이징 정보
     * @return 기부 내역 목록
     */
    Page<DonationResponse> getCampaignDonations(Long campaignId, Pageable pageable);
    
    /**
     * 캠페인별 기부 통계 조회
     * @param campaignId 캠페인 ID
     * @return 기부 통계
     */
    DonationStats getCampaignDonationStats(Long campaignId);
    
    /**
     * 사용자별 기부 통계 조회
     * @param userId 사용자 ID
     * @return 기부 통계
     */
    DonationStats getUserDonationStats(Long userId);
    
    /**
     * 기부 취소 처리
     * @param donationId 기부 ID
     * @param reason 취소 사유
     * @return 처리 결과
     */
    DonationResponse cancelDonation(Long donationId, String reason);
    
    /**
     * 기부 환불 처리
     * @param donationId 기부 ID
     * @param reason 환불 사유
     * @return 처리 결과
     */
    DonationResponse refundDonation(Long donationId, String reason);
    
    /**
     * 미완료 기부 정리 (스케줄러에서 사용)
     * 일정 시간이 지난 PENDING 상태의 기부를 자동으로 취소 처리
     */
    void cleanupPendingDonations();
    
    /**
     * 수동 결제 승인 처리 (웹훅 실패 시 대체 수단)
     * @param donationId 기부 ID
     * @param impUid PortOne 거래 고유번호 (선택적)
     * @return 승인 처리 결과
     */
    DonationResponse manualApprovePayment(Long donationId, String impUid);
    
    /**
     * paymentId로 결제 즉시 승인 (웹훅 우회) - 보안 강화
     * @param paymentId 결제 ID
     * @param impUid PortOne 거래 고유번호 (선택적)
     * @param currentUser 현재 인증된 사용자 (소유권 검증용, null 가능)
     * @return 승인 처리 결과
     */
    DonationResponse approvePaymentByPaymentId(String paymentId, String impUid, User currentUser);

    /**
     * FDS 상세 정보 조회
     * @param donationId 기부 ID
     * @return FDS 상세 정보
     */
    FdsDetailResponse getFdsDetail(Long donationId);

    /**
     * FDS 검증 결과 오버라이드 (관리자 승인/차단)
     * @param donationId 기부 ID
     * @param request 오버라이드 요청 DTO
     * @return 업데이트된 기부 정보
     */
    DonationResponse overrideFdsResult(Long donationId, FdsOverrideRequest request);

    /**
     * 기부 금액 추이 조회 (관리자용)
     * @param period 조회 기간 (7d, 30d, 3m, all)
     * @return 기부 금액 추이 데이터
     */
    AdminDonationTrendResponse getDonationTrends(String period);
}