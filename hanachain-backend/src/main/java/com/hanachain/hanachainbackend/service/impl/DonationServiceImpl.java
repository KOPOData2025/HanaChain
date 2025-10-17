package com.hanachain.hanachainbackend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.donation.*;
import com.hanachain.hanachainbackend.dto.fds.FdsRequest;
import com.hanachain.hanachainbackend.dto.fds.FdsResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.exception.BusinessException;
import com.hanachain.hanachainbackend.exception.ForbiddenException;
import com.hanachain.hanachainbackend.exception.InternalServerErrorException;
import com.hanachain.hanachainbackend.exception.NotFoundException;
import com.hanachain.hanachainbackend.exception.UnauthorizedException;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.DonationService;
import com.hanachain.hanachainbackend.service.FdsService;
import com.hanachain.hanachainbackend.service.PortoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 기부 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final FdsService fdsService;
    private final PortoneService portoneService;
    
    /**
     * 기부 생성 (결제 전 사전 등록)
     */
    @Override
    @Transactional
    public DonationResponse createDonation(DonationCreateRequest requestDto) {
        log.info("Creating donation for campaign: {}, amount: {}", 
                requestDto.getCampaignId(), requestDto.getAmount());
        
        // 입력값 검증
        if (!requestDto.isValidDonorInfo()) {
            throw new BusinessException("익명 기부가 아닌 경우 기부자 이름은 필수입니다.");
        }
        
        // 결제 ID 중복 확인
        if (donationRepository.findByPaymentId(requestDto.getPaymentId()).isPresent()) {
            throw new BusinessException("이미 존재하는 결제 ID입니다: " + requestDto.getPaymentId());
        }
        
        // 캠페인 조회
        Campaign campaign = campaignRepository.findById(requestDto.getCampaignId())
                .orElseThrow(() -> new NotFoundException("캠페인을 찾을 수 없습니다: " + requestDto.getCampaignId()));
        
        // 캠페인 기부 가능 상태 확인
        if (!campaign.isActive()) {
            throw new BusinessException("현재 기부가 불가능한 캠페인입니다.");
        }
        
        // 로그인한 사용자 정보 가져오기
        User currentUser = null;
        try {
            currentUser = com.hanachain.hanachainbackend.security.SecurityUtils.getCurrentUser()
                    .orElse(null);
            if (currentUser != null) {
                log.info("Found authenticated user: userId={}, userEmail={}", 
                        currentUser.getId(), currentUser.getEmail());
            }
        } catch (Exception e) {
            log.debug("No authenticated user found, proceeding with anonymous donation");
        }
        
        // 기부 엔티티 생성
        Donation donation = Donation.builder()
                .amount(requestDto.getAmount())
                .message(requestDto.getMessage())
                .paymentId(requestDto.getPaymentId())
                .paymentMethod(requestDto.getPaymentMethod())
                .anonymous(requestDto.getAnonymous())
                .donorName(requestDto.getDonorName())
                .campaign(campaign)
                .user(currentUser) // 사용자 정보 연결 (null일 수 있음)
                .paymentStatus(Donation.PaymentStatus.PENDING)
                .build();
        
        donation = donationRepository.save(donation);

        log.info("Donation created successfully: ID={}, PaymentID={}, UserId={}",
                donation.getId(), donation.getPaymentId(),
                donation.getUser() != null ? donation.getUser().getId() : "anonymous");

        // FDS 검증 비동기 호출
        final Donation finalDonation = donation;
        final Long userId = currentUser != null ? currentUser.getId() : null;

        CompletableFuture<Void> fdsVerification = performFdsVerificationAsync(finalDonation, userId, requestDto);

        // FDS 검증 결과를 기다리지 않고 즉시 응답 반환 (병렬 처리)
        // FDS 결과는 별도 스레드에서 DB에 업데이트됨
        log.info("Returning donation response while FDS verification runs in background");

        return DonationResponse.fromEntity(donation);
    }

    /**
     * FDS 검증을 비동기로 수행하고 결과를 DB에 저장
     */
    private CompletableFuture<Void> performFdsVerificationAsync(Donation donation, Long userId, DonationCreateRequest requestDto) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("=== Starting FDS Verification for Donation ID: {} ===", donation.getId());

                // FDS 요청 데이터 생성
                FdsRequest fdsRequest = FdsRequest.builder()
                        .amount(donation.getAmount().doubleValue())
                        .campaign_id(donation.getCampaign().getId())
                        .user_id(userId != null ? userId : -1L) // 익명 기부는 -1
                        .payment_method(mapPaymentMethod(requestDto.getPaymentMethod()))
                        .build();

                // FDS API 호출 (타임아웃 3초)
                CompletableFuture<FdsResponse> fdsResponseFuture = fdsService.verifyTransactionAsync(fdsRequest);
                FdsResponse fdsResponse = fdsResponseFuture.get(3, TimeUnit.SECONDS);

                // 🔍 실제 features 개수와 내용 로깅 추가
                log.info("📊 FDS API Response Details:");
                log.info("  - Features count: {}", fdsResponse.getFeatures() != null ? fdsResponse.getFeatures().size() : "null");
                if (fdsResponse.getFeatures() != null) {
                    log.info("  - Features values: {}", fdsResponse.getFeatures());
                }
                log.info("  - Q-values: {}", fdsResponse.getQValues());

                // FDS 결과를 Donation에 저장
                donation.updateFdsResult(
                        fdsResponse.getAction(),
                        BigDecimal.valueOf(fdsResponse.getRiskScore()),
                        BigDecimal.valueOf(fdsResponse.getConfidence()),
                        fdsResponse.getExplanation()
                );

                // FDS 상세 정보를 JSON으로 저장
                String fdsDetailJson = createFdsDetailJson(donation.getId(), fdsResponse);
                donation.setFdsDetailJson(fdsDetailJson);

                donationRepository.save(donation);

                log.info("✅ FDS verification completed successfully for Donation ID: {}", donation.getId());
                log.info("  - Action: {}", fdsResponse.getAction());
                log.info("  - Risk Score: {}", fdsResponse.getRiskScore());
                log.info("  - Confidence: {}", fdsResponse.getConfidence());
                log.info("  - FDS Detail JSON saved ({} characters)", fdsDetailJson.length());

                // 고위험 거래인 경우 경고 로그
                if (donation.isFdsHighRisk()) {
                    log.warn("⚠️ HIGH RISK TRANSACTION DETECTED: Donation ID={}, Action={}, RiskScore={}",
                            donation.getId(), fdsResponse.getAction(), fdsResponse.getRiskScore());
                }

            } catch (TimeoutException e) {
                log.warn("⏱️ FDS verification timeout for Donation ID: {}", donation.getId());
                donation.markFdsTimeout();
                donationRepository.save(donation);

            } catch (Exception e) {
                log.error("❌ FDS verification failed for Donation ID: {}: {}",
                        donation.getId(), e.getMessage(), e);
                donation.markFdsFailed(e.getMessage());
                donationRepository.save(donation);
            }
        });
    }

    /**
     * Features 리스트에서 안전하게 값을 가져옴 (인덱스 범위 체크)
     */
    private Double getSafeFeature(List<Double> features, int index, String featureName) {
        if (index < features.size()) {
            return features.get(index);
        }
        log.warn("⚠️ Feature index {} ({}) out of bounds, using default 0.0", index, featureName);
        return 0.0;
    }

    /**
     * Features 리스트에서 안전하게 Integer 값을 가져옴
     */
    private Integer getSafeFeatureInt(List<Double> features, int index, String featureName) {
        return getSafeFeature(features, index, featureName).intValue();
    }

    /**
     * FDS 응답을 FdsDetailResponse JSON으로 변환
     */
    private String createFdsDetailJson(Long donationId, FdsResponse fdsResponse) throws JsonProcessingException {
        log.info("Creating FDS detail JSON for donation: {}", donationId);

        // 17개의 features를 FdsFeatures로 매핑
        List<Double> features = fdsResponse.getFeatures();
        int expectedFeatureCount = 17;

        // Features 개수 검증 및 상세 로깅
        if (features == null) {
            log.error("❌ FDS features is null for donation: {}", donationId);
            throw new BusinessException("FDS features 데이터가 없습니다");
        }

        int actualFeatureCount = features.size();
        if (actualFeatureCount != expectedFeatureCount) {
            log.warn("⚠️ Features count mismatch for donation {}: expected {}, got {}",
                     donationId, expectedFeatureCount, actualFeatureCount);
            log.warn("⚠️ Features values: {}", features);

            // 개수가 부족한 경우 0.0으로 패딩, 많은 경우 경고만 출력
            if (actualFeatureCount < expectedFeatureCount) {
                log.warn("⚠️ Padding features with 0.0 values");
                while (features.size() < expectedFeatureCount) {
                    features.add(0.0);
                }
            } else {
                log.warn("⚠️ Using only first {} features", expectedFeatureCount);
            }
        }

        log.info("✅ Features validation passed - using {} features", features.size());

        FdsFeatures featuresDto = FdsFeatures.builder()
                // 거래 정보 (4개)
                .amountNormalized(getSafeFeature(features, 0, "amountNormalized"))
                .hourOfDay(getSafeFeatureInt(features, 1, "hourOfDay"))
                .dayOfWeek(getSafeFeatureInt(features, 2, "dayOfWeek"))
                .isWeekend(getSafeFeatureInt(features, 3, "isWeekend"))
                // 계정 정보 (5개)
                .accountAge(getSafeFeatureInt(features, 4, "accountAge"))
                .isNewUser(getSafeFeatureInt(features, 5, "isNewUser"))
                .emailVerified(getSafeFeatureInt(features, 6, "emailVerified"))
                .phoneVerified(getSafeFeatureInt(features, 7, "phoneVerified"))
                .hasProfile(getSafeFeatureInt(features, 8, "hasProfile"))
                // 기부 이력 (5개)
                .donationCount(getSafeFeatureInt(features, 9, "donationCount"))
                .avgDonationAmount(getSafeFeature(features, 10, "avgDonationAmount"))
                .daysSinceLastDonation(getSafeFeatureInt(features, 11, "daysSinceLastDonation"))
                .uniqueCampaigns(getSafeFeatureInt(features, 12, "uniqueCampaigns"))
                .suspiciousPatterns(getSafeFeatureInt(features, 13, "suspiciousPatterns"))
                // 결제 수단 (3개)
                .paymentMethodRisk(getSafeFeatureInt(features, 14, "paymentMethodRisk"))
                .isNewPaymentMethod(getSafeFeatureInt(features, 15, "isNewPaymentMethod"))
                .paymentFailures(getSafeFeatureInt(features, 16, "paymentFailures"))
                .build();

        log.info("✅ FdsFeatures created successfully for donation: {}", donationId);

        // Q-values를 FDS 응답에서 가져와서 DTO로 변환
        FdsQValues qValuesDto = null;
        if (fdsResponse.getQValues() != null && !fdsResponse.getQValues().isEmpty()) {
            qValuesDto = FdsQValues.builder()
                    .approve(fdsResponse.getQValues().get("approve"))
                    .manualReview(fdsResponse.getQValues().get("manual_review"))
                    .block(fdsResponse.getQValues().get("block"))
                    .build();
            log.info("Q-values extracted: approve={}, manual_review={}, block={}",
                    qValuesDto.getApprove(), qValuesDto.getManualReview(), qValuesDto.getBlock());
        } else {
            log.warn("Q-values not available in FDS response");
        }

        // FdsDetailResponse 생성
        FdsDetailResponse fdsDetail = FdsDetailResponse.builder()
                .donationId(donationId)
                .action(fdsResponse.getAction())
                .actionId(fdsResponse.getActionId())
                .riskScore(BigDecimal.valueOf(fdsResponse.getRiskScore()))
                .confidence(BigDecimal.valueOf(fdsResponse.getConfidence()))
                .explanation(fdsResponse.getExplanation())
                .checkedAt(LocalDateTime.now())
                .features(featuresDto)
                .qValues(qValuesDto)
                .timestamp(LocalDateTime.parse(fdsResponse.getTimestamp()))
                .build();

        // JSON으로 직렬화
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Java 8 날짜/시간 지원

        String json = objectMapper.writeValueAsString(fdsDetail);
        log.info("FDS detail JSON created successfully ({} characters)", json.length());

        return json;
    }

    /**
     * PaymentMethod enum을 FDS API용 문자열로 변환
     */
    private String mapPaymentMethod(Donation.PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "OTHER";
        }
        return paymentMethod.name();
    }
    
    /**
     * 결제 완료 처리 (웹훅에서 호출)
     */
    @Override
    @Transactional
    public DonationResponse processDonationPayment(String paymentId, PaymentWebhook webhookDto) {
        log.info("=== Processing Donation Payment ===");
        log.info("PaymentId: {}", paymentId);
        log.info("Webhook Status: {}", webhookDto.getStatus());
        log.info("Webhook Amount: {}", webhookDto.getAmount());
        
        // 기부 정보 조회
        Donation donation = donationRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    log.error("Donation not found for paymentId: {}", paymentId);
                    return new NotFoundException("결제 정보를 찾을 수 없습니다: " + paymentId);
                });
        
        log.info("Found donation: ID={}, currentStatus={}, amount={}", 
                donation.getId(), donation.getPaymentStatus(), donation.getAmount());
        
        // 결제 금액 검증
        if (donation.getAmount().compareTo(webhookDto.getAmount()) != 0) {
            log.error("Payment amount mismatch. Expected: {}, Received: {}", 
                    donation.getAmount(), webhookDto.getAmount());
            throw new BusinessException("결제 금액이 일치하지 않습니다.");
        }
        
        log.info("Amount verification passed");
        
        // 이미 처리된 결제인지 확인
        if (donation.getPaymentStatus() == Donation.PaymentStatus.COMPLETED) {
            log.warn("Payment already completed for donation ID: {}", donation.getId());
            return DonationResponse.fromEntity(donation);
        }
        
        // 웹훅 상태에 따른 처리
        log.info("Webhook status check: isPaid={}, isFailed={}, isCancelled={}", 
                webhookDto.isPaid(), webhookDto.isFailed(), webhookDto.isCancelled());
        
        if (webhookDto.isPaid()) {
            log.info("Processing payment completion for donation ID: {}", donation.getId());
            
            // 결제 완료 처리
            Donation.PaymentStatus previousStatus = donation.getPaymentStatus();
            donation.markAsPaid();
            
            log.info("Payment status changed: {} → {}", previousStatus, donation.getPaymentStatus());
            log.info("PaidAt set to: {}", donation.getPaidAt());
            
            // 즉시 DB 반영
            donation = donationRepository.saveAndFlush(donation);
            log.info("Donation status immediately saved to DB");
            
            // 캠페인 통계 업데이트
            Campaign campaign = donation.getCampaign();
            log.info("Updating campaign: ID={}, title={}", campaign.getId(), campaign.getTitle());
            log.info("Campaign before update: currentAmount={}, donorCount={}", 
                    campaign.getCurrentAmount(), campaign.getDonorCount());
            
            campaign.addDonation(donation.getAmount());
            campaignRepository.saveAndFlush(campaign);
            log.info("Campaign statistics immediately saved to DB");
            
            log.info("Campaign after update: currentAmount={}, donorCount={}", 
                    campaign.getCurrentAmount(), campaign.getDonorCount());
            log.info("Payment completed successfully. Donation ID: {}, Amount: {}", 
                    donation.getId(), donation.getAmount());
            
        } else if (webhookDto.isFailed()) {
            // 결제 실패 처리
            log.info("Processing payment failure for donation ID: {}", donation.getId());
            donation.markAsFailed(webhookDto.getFailReason());
            log.warn("Payment failed. Donation ID: {}, Reason: {}", 
                    donation.getId(), webhookDto.getFailReason());
            
        } else if (webhookDto.isCancelled()) {
            // 결제 취소 처리
            log.info("Processing payment cancellation for donation ID: {}", donation.getId());
            donation.markAsCancelled();
            log.info("Payment cancelled. Donation ID: {}", donation.getId());
            
        } else {
            log.warn("Unknown payment status: {} for donation ID: {}", 
                    webhookDto.getStatus(), donation.getId());
            log.warn("Webhook data: isPaid={}, isFailed={}, isCancelled={}", 
                    webhookDto.isPaid(), webhookDto.isFailed(), webhookDto.isCancelled());
        }
        
        donation = donationRepository.save(donation);
        log.info("Donation saved with final status: {}", donation.getPaymentStatus());
        
        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * 기부 정보 조회
     */
    @Override
    public DonationResponse getDonation(Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new NotFoundException("기부 정보를 찾을 수 없습니다: " + donationId));
        
        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * 결제 ID로 기부 조회
     */
    @Override
    public DonationResponse getDonationByPaymentId(String paymentId) {
        Donation donation = donationRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다: " + paymentId));
        
        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * 전체 기부 내역 조회 (관리자용)
     */
    @Override
    public Page<DonationResponse> getAllDonations(Pageable pageable, String keyword) {
        log.info("Getting all donations for admin with keyword: {}", keyword);
        
        Page<Donation> donations = donationRepository.findAllWithKeywordSearch(keyword, pageable);
        return donations.map(DonationResponse::fromEntity);
    }
    
    /**
     * 사용자별 기부 내역 조회
     */
    @Override
    public Page<DonationResponse> getUserDonations(Long userId, Pageable pageable) {
        Page<Donation> donations = donationRepository.findByUserId(userId, pageable);
        return donations.map(DonationResponse::fromEntity);
    }
    
    /**
     * 캠페인별 기부 내역 조회
     */
    @Override
    public Page<DonationResponse> getCampaignDonations(Long campaignId, Pageable pageable) {
        Page<Donation> donations = donationRepository.findByCampaignIdAndCompleted(campaignId, pageable);
        return donations.map(DonationResponse::fromEntitySimple);
    }
    
    /**
     * 캠페인별 기부 통계 조회
     */
    @Override
    public DonationStats getCampaignDonationStats(Long campaignId) {
        BigDecimal totalAmount = donationRepository.getTotalAmountByCampaign(campaignId);
        long totalCount = donationRepository.getTotalDonationCountByCampaign(campaignId);
        long uniqueDonorCount = donationRepository.getUniqueDonorCountByCampaign(campaignId);
        
        DonationStats stats = DonationStats.builder()
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalCount(totalCount)
                .completedCount(totalCount) // 완료된 기부만 조회하므로 동일
                .pendingCount(0L)
                .failedCount(0L)
                .uniqueDonorCount(uniqueDonorCount)
                .build();
        
        stats.calculateAverageAmount();
        return stats;
    }
    
    /**
     * 사용자별 기부 통계 조회
     */
    @Override
    public DonationStats getUserDonationStats(Long userId) {
        Object[] rawStats = donationRepository.getUserDonationStats(userId);
        
        if (rawStats.length == 0) {
            return DonationStats.builder()
                    .totalAmount(BigDecimal.ZERO)
                    .totalCount(0L)
                    .completedCount(0L)
                    .pendingCount(0L)
                    .failedCount(0L)
                    .averageAmount(BigDecimal.ZERO)
                    .build();
        }
        
        BigDecimal totalAmount = (BigDecimal) rawStats[0];
        Long totalCount = ((Number) rawStats[1]).longValue();
        Long completedCount = ((Number) rawStats[2]).longValue();
        Long pendingCount = ((Number) rawStats[3]).longValue();
        Long failedCount = ((Number) rawStats[4]).longValue();
        
        DonationStats stats = DonationStats.builder()
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalCount(totalCount)
                .completedCount(completedCount)
                .pendingCount(pendingCount)
                .failedCount(failedCount)
                .build();
        
        stats.calculateAverageAmount();
        return stats;
    }
    
    /**
     * 기부 취소 처리
     */
    @Override
    @Transactional
    public DonationResponse cancelDonation(Long donationId, String reason) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new NotFoundException("기부 정보를 찾을 수 없습니다: " + donationId));
        
        if (donation.getPaymentStatus() != Donation.PaymentStatus.PENDING) {
            throw new BusinessException("취소 가능한 상태가 아닙니다.");
        }
        
        donation.markAsCancelled();
        donation = donationRepository.save(donation);
        
        log.info("Donation cancelled: ID={}, Reason={}", donationId, reason);
        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * 기부 환불 처리
     */
    @Override
    @Transactional
    public DonationResponse refundDonation(Long donationId, String reason) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new NotFoundException("기부 정보를 찾을 수 없습니다: " + donationId));

        if (donation.getPaymentStatus() != Donation.PaymentStatus.COMPLETED) {
            throw new BusinessException("환불 가능한 상태가 아닙니다.");
        }

        log.info("=== Refund Donation Request ===");
        log.info("DonationId: {}, Amount: {}, Reason: {}", donationId, donation.getAmount(), reason);

        // 1. PortOne API를 통한 실제 결제 취소
        try {
            boolean cancelled = portoneService.cancelPayment(
                    donation.getPaymentId(),
                    reason
            );

            if (!cancelled) {
                log.error("❌ PortOne payment cancellation failed for donation: {}", donationId);
                throw new BusinessException("실제 결제 취소에 실패했습니다. PortOne 관리자 콘솔을 확인해주세요.");
            }

            log.info("✅ PortOne payment cancelled successfully for donation: {}", donationId);

        } catch (BusinessException e) {
            // 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during PortOne cancellation: {}", e.getMessage(), e);
            throw new InternalServerErrorException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 2. 캠페인에서 기부 금액 차감
        Campaign campaign = donation.getCampaign();
        log.info("Campaign before refund: ID={}, currentAmount={}, donorCount={}",
                campaign.getId(), campaign.getCurrentAmount(), campaign.getDonorCount());

        campaign.setCurrentAmount(campaign.getCurrentAmount().subtract(donation.getAmount()));
        campaign.setDonorCount(campaign.getDonorCount() - 1);
        campaignRepository.save(campaign);

        log.info("Campaign after refund: currentAmount={}, donorCount={}",
                campaign.getCurrentAmount(), campaign.getDonorCount());

        // 3. 기부 상태를 환불로 변경
        donation.setPaymentStatus(Donation.PaymentStatus.REFUNDED);
        donation.setFailureReason(reason);
        donation = donationRepository.save(donation);

        log.info("✅ Donation refunded successfully (both PortOne and DB): ID={}, Amount={}, Reason={}",
                donationId, donation.getAmount(), reason);

        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * 미완료 기부 정리 (스케줄러에서 사용)
     */
    @Override
    @Transactional
    public void cleanupPendingDonations() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // 24시간 전
        List<Donation> pendingDonations = donationRepository.findPendingDonationsOlderThan(cutoffTime);
        
        for (Donation donation : pendingDonations) {
            donation.markAsFailed("자동 취소 - 결제 시간 초과");
            donationRepository.save(donation);
        }
        
        log.info("Cleaned up {} pending donations", pendingDonations.size());
    }
    
    /**
     * 수동 결제 승인 처리 (웹훅 실패 시 대체 수단)
     */
    @Override
    @Transactional
    public DonationResponse manualApprovePayment(Long donationId, String impUid) {
        log.info("=== Manual Payment Approval ===");
        log.info("DonationId: {}, ImpUid: {}", donationId, impUid);
        
        // 기부 정보 조회
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new NotFoundException("기부 정보를 찾을 수 없습니다: " + donationId));
        
        log.info("Found donation: PaymentId={}, CurrentStatus={}, Amount={}", 
                donation.getPaymentId(), donation.getPaymentStatus(), donation.getAmount());
        
        // 이미 완료된 기부인지 확인
        if (donation.getPaymentStatus() == Donation.PaymentStatus.COMPLETED) {
            log.info("Payment already completed for donation: {}", donationId);
            return DonationResponse.fromEntity(donation);
        }
        
        // PENDING 상태인지 확인
        if (donation.getPaymentStatus() != Donation.PaymentStatus.PENDING) {
            throw new BusinessException("승인 가능한 상태가 아닙니다. 현재 상태: " + donation.getPaymentStatus());
        }
        
        // TODO: 실제 환경에서는 PortOne REST API를 호출하여 결제 상태를 확인해야 함
        // 현재는 수동 승인으로 처리
        log.info("Manually approving payment for donation: {}", donationId);
        
        // 결제 완료 처리
        Donation.PaymentStatus previousStatus = donation.getPaymentStatus();
        donation.markAsPaid();
        
        log.info("Payment status changed: {} → {}", previousStatus, donation.getPaymentStatus());
        log.info("PaidAt set to: {}", donation.getPaidAt());
        
        // 즉시 DB 반영
        donation = donationRepository.saveAndFlush(donation);
        log.info("Donation status immediately saved to DB (manual approval)");
        
        // 캠페인 통계 업데이트
        Campaign campaign = donation.getCampaign();
        log.info("Updating campaign: ID={}, title={}", campaign.getId(), campaign.getTitle());
        log.info("Campaign before update: currentAmount={}, donorCount={}", 
                campaign.getCurrentAmount(), campaign.getDonorCount());
        
        campaign.addDonation(donation.getAmount());
        campaignRepository.saveAndFlush(campaign);
        log.info("Campaign statistics immediately saved to DB (manual approval)");
        
        log.info("Campaign after update: currentAmount={}, donorCount={}", 
                campaign.getCurrentAmount(), campaign.getDonorCount());
        
        // 기부 정보 저장
        donation = donationRepository.save(donation);
        
        log.info("Manual payment approval completed successfully for donation: {}", donationId);
        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * paymentId로 결제 즉시 승인 (웹훅 우회)
     */
    @Override
    @Transactional
    public DonationResponse approvePaymentByPaymentId(String paymentId, String impUid, User currentUser) {
        log.info("=== Immediate Payment Approval (Security Enhanced) ===");
        log.info("PaymentId: {}, ImpUid: {}", paymentId, impUid);
        log.info("CurrentUser: {}", currentUser != null ? currentUser.getEmail() : "Anonymous");
        
        // paymentId로 기부 정보 조회
        Donation donation = donationRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다: " + paymentId));
        
        log.info("Found donation: ID={}, CurrentStatus={}, Amount={}, UserId={}, DonorName={}", 
                donation.getId(), donation.getPaymentStatus(), donation.getAmount(), 
                donation.getUser() != null ? donation.getUser().getId() : "null", 
                donation.getDonorName());
        
        // 보안 검증: 소유권 확인
        validatePaymentOwnership(donation, currentUser);
        
        // 이미 완료된 기부인지 확인
        if (donation.getPaymentStatus() == Donation.PaymentStatus.COMPLETED) {
            log.info("Payment already completed for paymentId: {}", paymentId);
            return DonationResponse.fromEntity(donation);
        }
        
        // PENDING 상태인지 확인
        if (donation.getPaymentStatus() != Donation.PaymentStatus.PENDING) {
            throw new BusinessException("승인 가능한 상태가 아닙니다. 현재 상태: " + donation.getPaymentStatus());
        }
        
        // 결제 생성 시간 검증 (최근 1시간 내)
        validatePaymentTime(donation);
        
        log.info("Immediately approving payment for paymentId: {}", paymentId);
        
        // 결제 완료 처리
        Donation.PaymentStatus previousStatus = donation.getPaymentStatus();
        donation.markAsPaid();
        
        log.info("Payment status changed: {} → {}", previousStatus, donation.getPaymentStatus());
        log.info("PaidAt set to: {}", donation.getPaidAt());
        
        // 즉시 DB 반영
        donation = donationRepository.saveAndFlush(donation);
        log.info("Donation status immediately saved to DB (immediate approval)");
        
        // 캠페인 통계 업데이트
        Campaign campaign = donation.getCampaign();
        log.info("Updating campaign: ID={}, title={}", campaign.getId(), campaign.getTitle());
        log.info("Campaign before update: currentAmount={}, donorCount={}", 
                campaign.getCurrentAmount(), campaign.getDonorCount());
        
        campaign.addDonation(donation.getAmount());
        campaignRepository.saveAndFlush(campaign);
        log.info("Campaign statistics immediately saved to DB (immediate approval)");
        
        log.info("Campaign after update: currentAmount={}, donorCount={}", 
                campaign.getCurrentAmount(), campaign.getDonorCount());
        log.info("Immediate payment approval completed successfully for paymentId: {}", paymentId);
        
        return DonationResponse.fromEntity(donation);
    }
    
    /**
     * 호환성을 위한 기존 메서드 (Deprecated)
     * @deprecated 보안상 위험하므로 사용하지 마세요. approvePaymentByPaymentId(String, String, User) 사용 권장
     */
    @Deprecated
    public DonationResponse approvePaymentByPaymentId(String paymentId, String impUid) {
        log.warn("DEPRECATED METHOD CALLED: approvePaymentByPaymentId without user validation - SECURITY RISK!");
        return approvePaymentByPaymentId(paymentId, impUid, null);
    }
    
    /**
     * 결제 소유권 검증
     */
    private void validatePaymentOwnership(Donation donation, User currentUser) {
        log.info("=== Payment Ownership Validation ===");
        
        // 익명 기부의 경우
        if (donation.getUser() == null) {
            log.info("Anonymous donation - time-based validation only");
            
            // 익명 기부는 시간 제한으로만 보안 강화 (1시간 이내 생성된 결제만 승인)
            // 추가적으로 세션 정보나 IP 검증도 가능하지만 현재는 시간 제한만 적용
            log.info("Anonymous donation approval allowed with time restriction");
            
        } else {
            // 회원 기부의 경우
            log.info("Member donation - checking user ownership");
            
            if (currentUser == null) {
                throw new SecurityException("회원 기부는 로그인이 필요합니다.");
            }
            
            if (!donation.getUser().getId().equals(currentUser.getId())) {
                log.warn("User ID mismatch: donation.userId={}, currentUserId={}", 
                        donation.getUser().getId(), currentUser.getId());
                throw new SecurityException("해당 결제에 대한 권한이 없습니다.");
            }
            log.info("User ownership validated for member donation");
        }
    }
    
    /**
     * 결제 시간 검증 (최근 1시간 내 생성된 결제만 승인 가능)
     */
    private void validatePaymentTime(Donation donation) {
        log.info("=== Payment Time Validation ===");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = donation.getCreatedAt();
        Duration timeDiff = Duration.between(createdAt, now);

        log.info("Payment created at: {}, Current time: {}, Diff: {} minutes",
                createdAt, now, timeDiff.toMinutes());

        // 1시간(60분) 제한
        if (timeDiff.toMinutes() > 60) {
            throw new BusinessException("결제 승인 시간이 만료되었습니다. 생성 후 1시간 내에만 승인 가능합니다.");
        }

        log.info("Payment time validation passed");
    }

    /**
     * FDS 상세 정보 조회
     */
    @Override
    public FdsDetailResponse getFdsDetail(Long donationId) {
        log.info("=== Fetching FDS Detail ===");
        log.info("DonationId: {}", donationId);

        // 기부 정보 조회
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new NotFoundException("기부를 찾을 수 없습니다: " + donationId));

        // FDS 검증 상태 확인 (PENDING, TIMEOUT은 차단, SUCCESS/FAILED는 허용)
        String fdsStatus = donation.getFdsStatus();
        if (fdsStatus == null || fdsStatus.equals("PENDING") || fdsStatus.equals("TIMEOUT")) {
            log.warn("FDS verification not available for donation: {} (status: {})", donationId, fdsStatus);
            throw new BusinessException("FDS 검증 결과가 아직 없습니다. 상태: " + fdsStatus);
        }

        // FDS 액션 확인
        if (donation.getFdsAction() == null || donation.getFdsAction().trim().isEmpty()) {
            log.warn("FDS action is empty for donation: {} despite status: {}", donationId, fdsStatus);
            throw new BusinessException("FDS 검증 액션 정보가 없습니다.");
        }

        log.info("FDS status check passed - Status: {}, Action: {}", fdsStatus, donation.getFdsAction());

        // JSON 데이터 조회
        String fdsDetailJson = donation.getFdsDetailJson();
        if (fdsDetailJson == null || fdsDetailJson.trim().isEmpty()) {
            log.warn("FDS detail JSON is empty for donation: {}", donationId);
            throw new NotFoundException("FDS 상세 정보가 없습니다. 기부 ID: " + donationId);
        }

        // JSON을 DTO로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Java 8 날짜/시간 지원

        try {
            FdsDetailResponse fdsDetail = objectMapper.readValue(fdsDetailJson, FdsDetailResponse.class);
            log.info("FDS detail fetched successfully for donation: {}", donationId);
            return fdsDetail;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse FDS detail JSON for donation {}: {}", donationId, e.getMessage(), e);
            throw new InternalServerErrorException("FDS 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * FDS 검증 결과 오버라이드 (관리자 승인/차단)
     */
    @Override
    @Transactional
    public DonationResponse overrideFdsResult(Long donationId, FdsOverrideRequest request) {
        log.info("=== FDS Override Request ===");
        log.info("DonationId: {}, Action: {}, Reason: {}", donationId, request.getAction(), request.getReason());

        // 관리자 권한 확인
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("인증이 필요합니다"));

        if (!currentUser.getRole().isSystemLevelAdmin()) {
            log.warn("Non-admin user {} attempted FDS override for donation {}",
                    currentUser.getEmail(), donationId);
            throw new ForbiddenException("관리자 권한이 필요합니다");
        }

        // 기부 조회
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new NotFoundException("기부를 찾을 수 없습니다: " + donationId));

        log.info("Found donation - Current FDS Action: {}, Payment Status: {}",
                donation.getFdsAction(), donation.getPaymentStatus());

        // 이미 APPROVE인 경우 변경 불가
        if ("APPROVE".equals(donation.getFdsAction())) {
            throw new BusinessException("이미 승인된 기부는 변경할 수 없습니다");
        }

        // 이미 환불/취소된 경우 변경 불가
        if (donation.getPaymentStatus() == Donation.PaymentStatus.REFUNDED ||
            donation.getPaymentStatus() == Donation.PaymentStatus.CANCELLED) {
            throw new BusinessException("이미 환불 또는 취소된 기부는 변경할 수 없습니다");
        }

        // 이전 액션 저장 (로깅용)
        String previousAction = donation.getFdsAction();

        // 액션 업데이트
        String newAction = request.getAction().equalsIgnoreCase("approve") ? "APPROVE" : "BLOCK";
        donation.setFdsAction(newAction);

        // 설명에 관리자 오버라이드 정보 추가
        String overrideExplanation = String.format("관리자 오버라이드 (이전: %s → 현재: %s) - %s",
                previousAction, newAction, request.getReason());
        donation.setFdsExplanation(overrideExplanation);
        donation.setFdsCheckedAt(LocalDateTime.now());

        // BLOCK인 경우 자동 환불/취소 처리
        if ("BLOCK".equals(newAction)) {
            log.info("Processing payment cancellation/refund for blocked donation");

            if (donation.getPaymentStatus() == Donation.PaymentStatus.COMPLETED) {
                // 완료된 결제는 환불 처리
                log.info("Refunding completed payment - Donation ID: {}, Amount: {}",
                        donationId, donation.getAmount());

                // 1. PortOne API를 통한 실제 결제 취소
                try {
                    String cancellationReason = "FDS 차단: " + request.getReason();
                    boolean cancelled = portoneService.cancelPayment(
                            donation.getPaymentId(),
                            cancellationReason
                    );

                    if (!cancelled) {
                        log.error("❌ PortOne payment cancellation failed for donation: {}", donationId);
                        throw new BusinessException("실제 결제 취소에 실패했습니다. PortOne 관리자 콘솔을 확인해주세요.");
                    }

                    log.info("✅ PortOne payment cancelled successfully for donation: {}", donationId);

                } catch (BusinessException e) {
                    // 비즈니스 예외는 그대로 전파
                    throw e;
                } catch (Exception e) {
                    log.error("❌ Unexpected error during PortOne cancellation: {}", e.getMessage(), e);
                    throw new InternalServerErrorException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
                }

                // 2. 캠페인에서 기부 금액 차감
                Campaign campaign = donation.getCampaign();
                log.info("Campaign before refund: ID={}, currentAmount={}, donorCount={}",
                        campaign.getId(), campaign.getCurrentAmount(), campaign.getDonorCount());

                campaign.setCurrentAmount(campaign.getCurrentAmount().subtract(donation.getAmount()));
                campaign.setDonorCount(campaign.getDonorCount() - 1);
                campaignRepository.save(campaign);

                log.info("Campaign after refund: currentAmount={}, donorCount={}",
                        campaign.getCurrentAmount(), campaign.getDonorCount());

                // 3. 기부 상태를 환불로 변경
                donation.setPaymentStatus(Donation.PaymentStatus.REFUNDED);
                donation.setFailureReason("FDS 차단: " + request.getReason());

                log.info("✅ Payment refunded successfully (both PortOne and DB) for donation: {}", donationId);

            } else if (donation.getPaymentStatus() == Donation.PaymentStatus.PENDING ||
                       donation.getPaymentStatus() == Donation.PaymentStatus.PROCESSING) {
                // 대기 중인 결제는 취소 처리
                log.info("Cancelling pending/processing payment - Donation ID: {}", donationId);

                donation.setPaymentStatus(Donation.PaymentStatus.CANCELLED);
                donation.setFailureReason("FDS 차단: " + request.getReason());

                log.info("✅ Payment cancelled successfully for donation: {}", donationId);

            } else if (donation.getPaymentStatus() == Donation.PaymentStatus.FAILED) {
                // 실패한 결제는 상태 변경 없이 사유만 업데이트
                log.info("Payment already failed - Donation ID: {}", donationId);
                donation.setFailureReason("FDS 차단: " + request.getReason());
            }
        }

        // 저장
        Donation savedDonation = donationRepository.save(donation);

        // 로그 기록
        log.info("✅ FDS override completed successfully");
        log.info("  - Admin: {} ({})", currentUser.getNickname(), currentUser.getEmail());
        log.info("  - Donation: {}", donationId);
        log.info("  - Action: {} → {}", previousAction, newAction);
        log.info("  - Payment Status: {}", savedDonation.getPaymentStatus());
        log.info("  - Reason: {}", request.getReason());

        return DonationResponse.fromEntity(savedDonation);
    }

    /**
     * 기부 금액 추이 조회 (관리자용)
     */
    @Override
    public AdminDonationTrendResponse getDonationTrends(String period) {
        log.info("=== Fetching Donation Trends ===");
        log.info("Period: {}", period);

        // 기간 계산
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period) {
            case "7d":
                startDate = endDate.minusDays(7);
                break;
            case "30d":
                startDate = endDate.minusDays(30);
                break;
            case "3m":
                startDate = endDate.minusMonths(3);
                break;
            case "all":
                // 전체 기간 조회 (시스템 시작일부터)
                startDate = LocalDateTime.of(2020, 1, 1, 0, 0);
                break;
            default:
                log.warn("Invalid period: {}, using default 30d", period);
                startDate = endDate.minusDays(30);
                period = "30d";
        }

        log.info("Date range: {} to {}", startDate, endDate);

        // 기간 내 완료된 기부 데이터 조회
        List<Donation> donations = donationRepository.findCompletedDonationsBetweenDates(startDate, endDate);
        log.info("Found {} completed donations in period", donations.size());

        // 일별로 그룹화
        java.util.Map<String, DonationTrendData> dailyDataMap = new java.util.TreeMap<>();

        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalCount = 0;

        for (Donation donation : donations) {
            if (donation.getPaidAt() == null) {
                continue;
            }

            String date = donation.getPaidAt().toLocalDate().toString(); // YYYY-MM-DD
            BigDecimal amount = donation.getAmount();

            DonationTrendData trendData = dailyDataMap.computeIfAbsent(date, k ->
                    DonationTrendData.builder()
                            .date(date)
                            .amount(BigDecimal.ZERO)
                            .count(0L)
                            .build()
            );

            trendData.setAmount(trendData.getAmount().add(amount));
            trendData.setCount(trendData.getCount() + 1);

            totalAmount = totalAmount.add(amount);
            totalCount++;
        }

        // TreeMap을 리스트로 변환 (날짜 순서대로 정렬됨)
        List<DonationTrendData> trendDataList = new java.util.ArrayList<>(dailyDataMap.values());

        // 평균 계산
        BigDecimal averageAmount = totalCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("Trend summary: totalAmount={}, totalCount={}, avgAmount={}, days={}",
                totalAmount, totalCount, averageAmount, trendDataList.size());

        return AdminDonationTrendResponse.builder()
                .period(period)
                .data(trendDataList)
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .averageAmount(averageAmount)
                .build();
    }
}