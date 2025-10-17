package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.user.DonationCertificateResponse;
import com.hanachain.hanachainbackend.dto.user.DonationFilterRequest;
import com.hanachain.hanachainbackend.dto.user.DonationHistoryResponse;
import com.hanachain.hanachainbackend.dto.user.DonationStatsResponse;
import com.hanachain.hanachainbackend.dto.user.PagedResponse;
import com.hanachain.hanachainbackend.service.DonationHistoryService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 기부 이력 및 통계 REST API 컨트롤러
 */
@Tag(name = "Donation History", description = "기부 이력 및 통계 API")
@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class DonationHistoryController {

    private final DonationHistoryService donationHistoryService;

    @Operation(
        summary = "기부 이력 목록 조회",
        description = "사용자의 기부 이력을 페이징과 필터링을 통해 조회합니다."
    )
    @GetMapping("/donations")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PagedResponse<DonationHistoryResponse>> getDonations(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "기부 상태 필터 (completed, pending, failed, cancelled)") @RequestParam(required = false) String status,
            @Parameter(description = "정렬 기준 (date, amount)") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "정렬 순서 (asc, desc)") @RequestParam(defaultValue = "desc") String sortOrder,
            @Parameter(description = "캠페인 제목 검색어") @RequestParam(required = false) String search) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("기부 이력 조회 요청 - 사용자 ID: {}, 페이지: {}, 크기: {}", currentUserId, page, size);
        
        DonationFilterRequest filterRequest = DonationFilterRequest.builder()
                .page(page)
                .size(size)
                .status(status)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .search(search)
                .build();
        
        PagedResponse<DonationHistoryResponse> donations = donationHistoryService.getUserDonationHistory(
                currentUserId, filterRequest);
        
        return ApiResponse.success("기부 이력을 성공적으로 조회했습니다.", donations);
    }

    @Operation(
        summary = "기부 내역 상세 조회",
        description = "특정 기부 내역의 상세 정보를 조회합니다."
    )
    @GetMapping("/donations/{donationId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<DonationHistoryResponse> getDonation(
            @Parameter(description = "기부 ID") @PathVariable Long donationId) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("기부 상세 조회 요청 - 사용자 ID: {}, 기부 ID: {}", currentUserId, donationId);
        
        DonationHistoryResponse donation = donationHistoryService.getDonationDetail(currentUserId, donationId);
        
        return ApiResponse.success("기부 내역을 성공적으로 조회했습니다.", donation);
    }

    @Operation(
        summary = "기부 통계 조회",
        description = "사용자의 총 기부 통계 정보를 조회합니다."
    )
    @GetMapping("/donations/statistics")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<DonationStatsResponse> getDonationStatistics() {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("기부 통계 조회 요청 - 사용자 ID: {}", currentUserId);
        
        DonationStatsResponse stats = donationHistoryService.getUserDonationStats(currentUserId);
        
        return ApiResponse.success("기부 통계를 성공적으로 조회했습니다.", stats);
    }

    @Operation(
        summary = "최근 기부 내역 조회",
        description = "사용자의 최근 기부 내역을 조회합니다. (대시보드용)"
    )
    @GetMapping("/donations/recent")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<DonationHistoryResponse>> getRecentDonations(
            @Parameter(description = "조회할 기부 개수") @RequestParam(defaultValue = "5") int limit) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("최근 기부 내역 조회 요청 - 사용자 ID: {}, 개수: {}", currentUserId, limit);
        
        List<DonationHistoryResponse> recentDonations = donationHistoryService.getRecentUserDonations(
                currentUserId, limit);
        
        return ApiResponse.success("최근 기부 내역을 성공적으로 조회했습니다.", recentDonations);
    }

    @Operation(
        summary = "기부 통계 캐시 갱신",
        description = "사용자의 기부 통계 캐시를 갱신합니다."
    )
    @PostMapping("/donations/statistics/refresh")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> refreshDonationStatistics() {

        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));

        log.info("기부 통계 캐시 갱신 요청 - 사용자 ID: {}", currentUserId);

        donationHistoryService.refreshUserDonationStats(currentUserId);

        return ApiResponse.success("기부 통계 캐시가 성공적으로 갱신되었습니다.");
    }

    @Operation(
        summary = "기부 증서 조회",
        description = "블록체인 트랜잭션이 완료된 기부의 증서를 조회합니다. 트랜잭션 해시가 있어야 증서 발급이 가능합니다."
    )
    @GetMapping("/donations/{donationId}/certificate")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<DonationCertificateResponse> getDonationCertificate(
            @Parameter(description = "기부 ID") @PathVariable Long donationId) {

        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));

        log.info("기부 증서 조회 요청 - 사용자 ID: {}, 기부 ID: {}", currentUserId, donationId);

        try {
            DonationCertificateResponse certificate = donationHistoryService.getDonationCertificate(
                    currentUserId, donationId);

            return ApiResponse.success("기부 증서를 성공적으로 조회했습니다.", certificate);
        } catch (IllegalArgumentException e) {
            log.warn("기부 증서 조회 실패 - 사용자 ID: {}, 기부 ID: {}, 사유: {}",
                     currentUserId, donationId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
}