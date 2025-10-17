package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.user.FavoriteAddRequest;
import com.hanachain.hanachainbackend.dto.user.FavoriteResponse;
import com.hanachain.hanachainbackend.dto.user.PagedResponse;
import com.hanachain.hanachainbackend.service.FavoriteService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 즐겨찾기 관리 REST API 컨트롤러
 */
@Tag(name = "Favorites", description = "즐겨찾기 관리 API")
@Slf4j
@RestController
@RequestMapping("/mypage/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(
        summary = "즐겨찾기 목록 조회",
        description = "사용자의 즐겨찾기 캠페인 목록을 페이징하여 조회합니다."
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PagedResponse<FavoriteResponse>> getFavorites(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "캠페인 제목 검색어") @RequestParam(required = false) String search) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("즐겨찾기 목록 조회 요청 - 사용자 ID: {}, 페이지: {}, 크기: {}", currentUserId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<FavoriteResponse> favorites;
        
        if (search != null && !search.trim().isEmpty()) {
            favorites = favoriteService.searchUserFavorites(currentUserId, search.trim(), pageable);
        } else {
            favorites = favoriteService.getUserFavorites(currentUserId, pageable);
        }
        
        return ApiResponse.success("즐겨찾기 목록을 성공적으로 조회했습니다.", favorites);
    }

    @Operation(
        summary = "즐겨찾기 추가",
        description = "캠페인을 즐겨찾기에 추가합니다."
    )
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<FavoriteResponse> addFavorite(
            @Valid @RequestBody FavoriteAddRequest request) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.info("즐겨찾기 추가 요청 - 사용자 ID: {}, 캠페인 ID: {}", currentUserId, request.getCampaignId());
        
        FavoriteResponse favorite = favoriteService.addFavorite(currentUserId, request);
        
        return ApiResponse.success("즐겨찾기에 성공적으로 추가했습니다.", favorite);
    }

    @Operation(
        summary = "즐겨찾기 제거",
        description = "캠페인을 즐겨찾기에서 제거합니다."
    )
    @DeleteMapping("/{campaignId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> removeFavorite(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.info("즐겨찾기 제거 요청 - 사용자 ID: {}, 캠페인 ID: {}", currentUserId, campaignId);
        
        favoriteService.removeFavorite(currentUserId, campaignId);
        
        return ApiResponse.success("즐겨찾기에서 성공적으로 제거했습니다.");
    }

    @Operation(
        summary = "즐겨찾기 여부 확인",
        description = "특정 캠페인이 즐겨찾기에 추가되어 있는지 확인합니다."
    )
    @GetMapping("/check/{campaignId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Boolean> checkFavorite(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("즐겨찾기 여부 확인 요청 - 사용자 ID: {}, 캠페인 ID: {}", currentUserId, campaignId);
        
        boolean isFavorite = favoriteService.isFavorite(currentUserId, campaignId);
        
        return ApiResponse.success("즐겨찾기 여부를 성공적으로 확인했습니다.", isFavorite);
    }

    @Operation(
        summary = "즐겨찾기 개수 조회",
        description = "사용자의 총 즐겨찾기 개수를 조회합니다."
    )
    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Long> getFavoriteCount() {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("즐겨찾기 개수 조회 요청 - 사용자 ID: {}", currentUserId);
        
        long count = favoriteService.getUserFavoriteCount(currentUserId);
        
        return ApiResponse.success("즐겨찾기 개수를 성공적으로 조회했습니다.", count);
    }
}