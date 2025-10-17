package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.dto.user.DonationCertificateResponse;
import com.hanachain.hanachainbackend.dto.user.DonationFilterRequest;
import com.hanachain.hanachainbackend.dto.user.DonationHistoryResponse;
import com.hanachain.hanachainbackend.dto.user.DonationStatsResponse;
import com.hanachain.hanachainbackend.dto.user.PagedResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 기부 이력 관리 서비스 인터페이스
 */
public interface DonationHistoryService {
    
    /**
     * 사용자별 기부 이력 조회 (페이징 및 필터링)
     * @param userId 사용자 ID
     * @param filterRequest 필터링 및 페이징 요청
     * @return 페이징된 기부 이력
     */
    PagedResponse<DonationHistoryResponse> getUserDonationHistory(Long userId, DonationFilterRequest filterRequest);
    
    /**
     * 사용자별 기부 통계 조회
     * @param userId 사용자 ID
     * @return 기부 통계
     */
    DonationStatsResponse getUserDonationStats(Long userId);
    
    /**
     * 사용자의 최근 기부 내역 조회 (대시보드용)
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @return 최근 기부 내역 리스트
     */
    List<DonationHistoryResponse> getRecentUserDonations(Long userId, int limit);
    
    /**
     * 특정 기부 내역 상세 조회
     * @param userId 사용자 ID
     * @param donationId 기부 ID
     * @return 기부 내역 상세 정보
     */
    DonationHistoryResponse getDonationDetail(Long userId, Long donationId);
    
    /**
     * 사용자의 기부 통계 업데이트 (캐시 무효화)
     * @param userId 사용자 ID
     */
    void refreshUserDonationStats(Long userId);

    /**
     * 기부 증서 조회
     * 블록체인 트랜잭션 해시가 있는 완료된 기부에 대해서만 증서 발급 가능
     * @param userId 사용자 ID
     * @param donationId 기부 ID
     * @return 기부 증서 정보
     * @throws IllegalArgumentException 트랜잭션 해시가 없거나 권한이 없는 경우
     */
    DonationCertificateResponse getDonationCertificate(Long userId, Long donationId);
}