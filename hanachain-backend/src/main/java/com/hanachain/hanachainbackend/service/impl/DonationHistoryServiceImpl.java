package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.dto.user.DonationCertificateResponse;
import com.hanachain.hanachainbackend.dto.user.DonationFilterRequest;
import com.hanachain.hanachainbackend.dto.user.DonationHistoryResponse;
import com.hanachain.hanachainbackend.dto.user.DonationStatsResponse;
import com.hanachain.hanachainbackend.dto.user.PagedResponse;
import com.hanachain.hanachainbackend.exception.ProfileNotFoundException;
import com.hanachain.hanachainbackend.service.DonationHistoryService;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 기부 이력 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationHistoryServiceImpl implements DonationHistoryService {

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;

    @Override
    public PagedResponse<DonationHistoryResponse> getUserDonationHistory(Long userId, DonationFilterRequest filterRequest) {
        log.debug("사용자 기부 이력 조회 - 사용자 ID: {}, 필터: {}", userId, filterRequest);
        
        // 사용자 존재 확인
        validateUserExists(userId);
        
        // Pageable 생성
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize());
        
        // 상태 매핑 (프론트엔드 형식 → DB enum)
        Donation.PaymentStatus status = mapStatusFromFrontend(filterRequest.getStatus());
        
        // 기부 이력 조회
        Page<Donation> donationPage = donationRepository.findUserDonationsWithFilters(
                userId, status, filterRequest.getSearch(), 
                filterRequest.getSortBy(), filterRequest.getSortOrder(), pageable);
        
        // DTO 변환
        List<DonationHistoryResponse> donationHistory = donationPage.getContent()
                .stream()
                .map(DonationHistoryResponse::from)
                .collect(Collectors.toList());
        
        return PagedResponse.<DonationHistoryResponse>builder()
                .content(donationHistory)
                .page(donationPage.getNumber())
                .limit(donationPage.getSize())
                .totalElements(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .first(donationPage.isFirst())
                .last(donationPage.isLast())
                .hasNext(donationPage.hasNext())
                .hasPrevious(donationPage.hasPrevious())
                .build();
    }

    @Override
    @Cacheable(value = "userDonationStats", key = "#userId")
    public DonationStatsResponse getUserDonationStats(Long userId) {
        log.debug("사용자 기부 통계 조회 - 사용자 ID: {}", userId);
        
        validateUserExists(userId);
        
        // Repository에서 통계 조회
        Object[] stats = donationRepository.getUserDonationStats(userId);
        
        return DonationStatsResponse.from(stats);
    }

    @Override
    public List<DonationHistoryResponse> getRecentUserDonations(Long userId, int limit) {
        log.debug("사용자 최근 기부 내역 조회 - 사용자 ID: {}, 개수: {}", userId, limit);
        
        validateUserExists(userId);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Donation> recentDonations = donationRepository.findRecentUserDonations(userId, pageable);
        
        return recentDonations.stream()
                .map(DonationHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public DonationHistoryResponse getDonationDetail(Long userId, Long donationId) {
        log.debug("기부 내역 상세 조회 - 사용자 ID: {}, 기부 ID: {}", userId, donationId);
        
        validateUserExists(userId);
        
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("기부 내역을 찾을 수 없습니다: " + donationId));
        
        // 자신의 기부 내역인지 확인
        if (!donation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 기부 내역에 대한 권한이 없습니다.");
        }
        
        return DonationHistoryResponse.from(donation);
    }

    @Override
    @CacheEvict(value = "userDonationStats", key = "#userId")
    public void refreshUserDonationStats(Long userId) {
        log.debug("사용자 기부 통계 캐시 무효화 - 사용자 ID: {}", userId);
        // 캐시 무효화만 수행 (어노테이션으로 처리됨)
    }

    @Override
    public DonationCertificateResponse getDonationCertificate(Long userId, Long donationId) {
        log.debug("기부 증서 조회 - 사용자 ID: {}, 기부 ID: {}", userId, donationId);

        // 사용자 존재 확인
        validateUserExists(userId);

        // 기부 내역 조회
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("기부 내역을 찾을 수 없습니다: " + donationId));

        // 자신의 기부 내역인지 확인
        if (!donation.getUser().getId().equals(userId)) {
            log.warn("기부 증서 조회 권한 없음 - 요청 사용자: {}, 기부 소유자: {}", userId, donation.getUser().getId());
            throw new IllegalArgumentException("본인의 기부 내역만 조회할 수 있습니다.");
        }

        // 블록체인 트랜잭션 해시 존재 여부 확인
        if (donation.getDonationTransactionHash() == null || donation.getDonationTransactionHash().isEmpty()) {
            log.warn("블록체인 트랜잭션 해시 없음 - 기부 ID: {}", donationId);
            throw new IllegalArgumentException("블록체인 트랜잭션이 아직 처리되지 않았습니다. 잠시 후 다시 시도해주세요.");
        }

        // 결제 완료 상태 확인 (선택적 검증)
        if (donation.getPaymentStatus() != Donation.PaymentStatus.COMPLETED) {
            log.warn("결제 미완료 상태 - 기부 ID: {}, 상태: {}", donationId, donation.getPaymentStatus());
            throw new IllegalArgumentException("결제가 완료된 기부만 증서를 발급할 수 있습니다.");
        }

        // 익명 기부 여부 확인
        boolean isAnonymous = donation.getAnonymous() != null && donation.getAnonymous();

        log.info("기부 증서 발급 성공 - 기부 ID: {}, 익명 여부: {}, 트랜잭션 해시: {}",
                 donationId, isAnonymous, donation.getDonationTransactionHash());

        return DonationCertificateResponse.from(donation, isAnonymous);
    }

    /**
     * 사용자 존재 여부 확인
     */
    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ProfileNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
    }

    /**
     * 프론트엔드 상태 값을 DB enum으로 매핑
     */
    private Donation.PaymentStatus mapStatusFromFrontend(String status) {
        if (status == null) {
            return null;
        }
        
        return switch (status.toLowerCase()) {
            case "completed" -> Donation.PaymentStatus.COMPLETED;
            case "pending" -> Donation.PaymentStatus.PENDING;
            case "failed" -> Donation.PaymentStatus.FAILED;
            case "cancelled" -> Donation.PaymentStatus.CANCELLED;
            default -> null;
        };
    }
}