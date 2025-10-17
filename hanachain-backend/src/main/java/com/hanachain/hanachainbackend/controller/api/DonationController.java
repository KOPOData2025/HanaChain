package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.donation.*;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 기부 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
@Tag(name = "Donation API", description = "기부 관련 API")
public class DonationController {
    
    private final DonationService donationService;
    
    /**
     * 기부 생성 (결제 전 사전 등록) - 익명 기부 지원
     */
    @PostMapping
    @Operation(summary = "기부 생성", description = "결제 전 기부 정보를 사전 등록합니다 (익명 가능)")
    public ResponseEntity<ApiResponse<DonationResponse>> createDonation(
            @Valid @RequestBody DonationCreateRequest requestDto) {
        
        log.info("Creating donation for campaign: {}, amount: {}", 
                requestDto.getCampaignId(), requestDto.getAmount());
        
        DonationResponse donation = donationService.createDonation(requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("기부 정보가 생성되었습니다.", donation));
    }
    
    /**
     * 기부 정보 조회
     */
    @GetMapping("/{donationId}")
    @Operation(summary = "기부 정보 조회", description = "기부 ID로 기부 정보를 조회합니다")
    public ResponseEntity<ApiResponse<DonationResponse>> getDonation(
            @Parameter(description = "기부 ID") @PathVariable Long donationId) {
        
        DonationResponse donation = donationService.getDonation(donationId);
        
        return ResponseEntity.ok(ApiResponse.success(donation));
    }
    
    /**
     * 결제 ID로 기부 조회
     */
    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "결제 ID로 기부 조회", description = "결제 ID로 기부 정보를 조회합니다")
    public ResponseEntity<ApiResponse<DonationResponse>> getDonationByPaymentId(
            @Parameter(description = "결제 ID") @PathVariable String paymentId) {
        
        DonationResponse donation = donationService.getDonationByPaymentId(paymentId);
        
        return ResponseEntity.ok(ApiResponse.success(donation));
    }
    
    /**
     * 내 기부 내역 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 기부 내역 조회", description = "로그인한 사용자의 기부 내역을 조회합니다")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> getMyDonations(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<DonationResponse> donations = donationService.getUserDonations(currentUserId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(donations));
    }
    
    /**
     * 전체 기부 내역 조회 (관리자용)
     */
    @GetMapping("/admin/all")
    @Operation(summary = "전체 기부 내역 조회", description = "모든 기부 내역을 조회합니다 (관리자 전용)")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> getAllDonations(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword) {
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<DonationResponse> donations = donationService.getAllDonations(pageable, keyword);
        
        return ResponseEntity.ok(ApiResponse.success(donations));
    }

    /**
     * 사용자별 기부 내역 조회 (관리자용)
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "사용자별 기부 내역 조회", description = "특정 사용자의 기부 내역을 조회합니다 (관리자 전용)")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> getUserDonations(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<DonationResponse> donations = donationService.getUserDonations(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(donations));
    }
    
    /**
     * 캠페인별 기부 내역 조회
     */
    @GetMapping("/campaigns/{campaignId}")
    @Operation(summary = "캠페인별 기부 내역 조회", description = "특정 캠페인의 기부 내역을 조회합니다")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> getCampaignDonations(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "paidAt") String sort,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<DonationResponse> donations = donationService.getCampaignDonations(campaignId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(donations));
    }
    
    /**
     * 내 기부 통계 조회
     */
    @GetMapping("/my/stats")
    @Operation(summary = "내 기부 통계 조회", description = "로그인한 사용자의 기부 통계를 조회합니다")
    public ResponseEntity<ApiResponse<DonationStats>> getMyDonationStats() {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        DonationStats stats = donationService.getUserDonationStats(currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 캠페인별 기부 통계 조회
     */
    @GetMapping("/campaigns/{campaignId}/stats")
    @Operation(summary = "캠페인별 기부 통계 조회", description = "특정 캠페인의 기부 통계를 조회합니다")
    public ResponseEntity<ApiResponse<DonationStats>> getCampaignDonationStats(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {
        
        DonationStats stats = donationService.getCampaignDonationStats(campaignId);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 기부 취소 (결제 전)
     */
    @PostMapping("/{donationId}/cancel")
    @Operation(summary = "기부 취소", description = "결제 전 기부를 취소합니다")
    public ResponseEntity<ApiResponse<DonationResponse>> cancelDonation(
            @Parameter(description = "기부 ID") @PathVariable Long donationId,
            @RequestParam(required = false) String reason) {
        
        String cancelReason = reason != null ? reason : "사용자 요청에 의한 취소";
        DonationResponse donation = donationService.cancelDonation(donationId, cancelReason);
        
        return ResponseEntity.ok(ApiResponse.success("기부가 취소되었습니다.", donation));
    }
    
    /**
     * 기부 환불 (관리자용)
     */
    @PostMapping("/{donationId}/refund")
    @Operation(summary = "기부 환불", description = "완료된 기부를 환불 처리합니다 (관리자 전용)")
    public ResponseEntity<ApiResponse<DonationResponse>> refundDonation(
            @Parameter(description = "기부 ID") @PathVariable Long donationId,
            @RequestParam String reason) {
        
        DonationResponse donation = donationService.refundDonation(donationId, reason);
        
        return ResponseEntity.ok(ApiResponse.success("기부가 환불되었습니다.", donation));
    }
    
    /**
     * 결제 검증 API (프론트엔드에서 사용)
     */
    @PostMapping("/verify-payment")
    @Operation(summary = "결제 검증", description = "프론트엔드에서 결제 완료 후 검증을 요청합니다")
    public ResponseEntity<ApiResponse<DonationResponse>> verifyPayment(
            @RequestParam String paymentId) {
        
        log.info("Verifying payment: {}", paymentId);
        
        // 결제 정보 조회
        DonationResponse donation = donationService.getDonationByPaymentId(paymentId);
        
        // 결제 상태 확인
        if (donation.getPaymentStatus() == com.hanachain.hanachainbackend.entity.Donation.PaymentStatus.COMPLETED) {
            return ResponseEntity.ok(ApiResponse.success("결제가 완료되었습니다.", donation));
        } else {
            return ResponseEntity.ok(ApiResponse.success("결제 처리 중입니다.", donation));
        }
    }
    
    /**
     * 수동 결제 승인 API (웹훅 실패 시 대체 수단)
     */
    @PostMapping("/{donationId}/approve-payment")
    @Operation(summary = "수동 결제 승인", description = "웹훅 실패 시 수동으로 결제를 승인 처리합니다")
    public ResponseEntity<ApiResponse<DonationResponse>> approvePayment(
            @Parameter(description = "기부 ID") @PathVariable Long donationId,
            @RequestParam(required = false) String impUid) {
        
        log.info("=== 수동 결제 승인 요청 ===");
        log.info("기부 ID: {}", donationId);
        log.info("ImpUid: {}", impUid);

        DonationResponse donation = donationService.manualApprovePayment(donationId, impUid);

        log.info("수동 결제 승인 성공 - 기부 ID: {}", donationId);
        return ResponseEntity.ok(ApiResponse.success("결제가 수동으로 승인되었습니다.", donation));
    }

    /**
     * paymentId로 결제 즉시 승인 (웹훅 우회) - 보안 강화
     */
    @PostMapping("/approve-payment-by-id")
    @Operation(summary = "결제 즉시 승인", description = "paymentId로 결제를 즉시 승인합니다 (웹훅 우회, 인증 필요)")
    public ResponseEntity<ApiResponse<DonationResponse>> approvePaymentByPaymentId(
            @RequestParam String paymentId,
            @RequestParam(required = false) String impUid) {

        log.info("=== 즉시 결제 승인 요청 ===");
        log.info("결제 ID: {}", paymentId);
        log.info("ImpUid: {}", impUid);

        // 현재 인증된 사용자 확인 (익명 기부 허용을 위해 Optional)
        User currentUser = SecurityUtils.getCurrentUser().orElse(null);
        if (currentUser != null) {
            log.info("인증된 사용자: {}", currentUser.getEmail());
        } else {
            log.info("익명 기부 승인 요청");
        }

        // 현재 사용자 정보를 서비스에 전달하여 소유권 검증
        DonationResponse donation = donationService.approvePaymentByPaymentId(paymentId, impUid, currentUser);

        log.info("즉시 결제 승인 성공 - 결제 ID: {}", paymentId);
        return ResponseEntity.ok(ApiResponse.success("결제가 즉시 승인되었습니다.", donation));
    }

    /**
     * FDS 상세 정보 조회 (관리자 전용)
     */
    @GetMapping("/{donationId}/fds")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "FDS 상세 정보 조회", description = "기부의 FDS 검증 상세 정보를 조회합니다 (관리자 전용)")
    public ResponseEntity<ApiResponse<FdsDetailResponse>> getFdsDetail(
            @Parameter(description = "기부 ID") @PathVariable Long donationId) {

        log.info("Fetching FDS detail for donation: {}", donationId);
        FdsDetailResponse fdsDetail = donationService.getFdsDetail(donationId);

        return ResponseEntity.ok(ApiResponse.success(fdsDetail));
    }

    /**
     * FDS 검증 결과 오버라이드 (관리자 승인/차단)
     */
    @PostMapping("/{donationId}/fds/override")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "FDS 결과 오버라이드", description = "관리자가 FDS 검증 결과를 수동으로 변경합니다 (관리자 전용)")
    public ResponseEntity<ApiResponse<DonationResponse>> overrideFdsResult(
            @Parameter(description = "기부 ID") @PathVariable Long donationId,
            @Valid @RequestBody FdsOverrideRequest request) {

        log.info("FDS override request for donation {}: action={}", donationId, request.getAction());
        DonationResponse donation = donationService.overrideFdsResult(donationId, request);

        return ResponseEntity.ok(ApiResponse.success("FDS 검증 결과가 업데이트되었습니다.", donation));
    }
}