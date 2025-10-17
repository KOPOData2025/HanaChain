package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserFavorite;
import com.hanachain.hanachainbackend.dto.user.FavoriteAddRequest;
import com.hanachain.hanachainbackend.dto.user.FavoriteResponse;
import com.hanachain.hanachainbackend.dto.user.PagedResponse;
import com.hanachain.hanachainbackend.exception.ProfileNotFoundException;
import com.hanachain.hanachainbackend.service.FavoriteService;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.UserFavoriteRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 즐겨찾기 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final UserFavoriteRepository userFavoriteRepository;

    @Override
    @Transactional
    public FavoriteResponse addFavorite(Long userId, FavoriteAddRequest request) {
        log.info("즐겨찾기 추가 시작 - 사용자 ID: {}, 캠페인 ID: {}", userId, request.getCampaignId());
        
        // 사용자와 캠페인 조회
        User user = findUserById(userId);
        Campaign campaign = findCampaignById(request.getCampaignId());
        
        // 중복 확인
        if (userFavoriteRepository.existsByUserIdAndCampaignId(userId, request.getCampaignId())) {
            throw new IllegalArgumentException("이미 즐겨찾기에 추가된 캠페인입니다.");
        }
        
        try {
            // 즐겨찾기 생성 및 저장
            UserFavorite favorite = UserFavorite.builder()
                    .user(user)
                    .campaign(campaign)
                    .memo(request.getMemo())
                    .build();
            
            UserFavorite savedFavorite = userFavoriteRepository.save(favorite);
            
            log.info("즐겨찾기 추가 완료 - 사용자 ID: {}, 캠페인 ID: {}", userId, request.getCampaignId());
            
            return buildFavoriteResponse(savedFavorite);
            
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 즐겨찾기 추가 시도 - 사용자 ID: {}, 캠페인 ID: {}", userId, request.getCampaignId());
            throw new IllegalArgumentException("이미 즐겨찾기에 추가된 캠페인입니다.");
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long campaignId) {
        log.info("즐겨찾기 제거 시작 - 사용자 ID: {}, 캠페인 ID: {}", userId, campaignId);
        
        UserFavorite favorite = userFavoriteRepository.findByUserIdAndCampaignId(userId, campaignId)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기에서 찾을 수 없는 캠페인입니다."));
        
        userFavoriteRepository.delete(favorite);
        
        log.info("즐겨찾기 제거 완료 - 사용자 ID: {}, 캠페인 ID: {}", userId, campaignId);
    }

    @Override
    public PagedResponse<FavoriteResponse> getUserFavorites(Long userId, Pageable pageable) {
        log.debug("사용자 즐겨찾기 목록 조회 - 사용자 ID: {}, 페이지: {}", userId, pageable.getPageNumber());
        
        validateUserExists(userId);
        
        Page<UserFavorite> favoritePage = userFavoriteRepository.findByUserId(userId, pageable);
        
        List<FavoriteResponse> favorites = favoritePage.getContent()
                .stream()
                .map(this::buildFavoriteResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.<FavoriteResponse>builder()
                .content(favorites)
                .page(favoritePage.getNumber())
                .limit(favoritePage.getSize())
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .first(favoritePage.isFirst())
                .last(favoritePage.isLast())
                .hasNext(favoritePage.hasNext())
                .hasPrevious(favoritePage.hasPrevious())
                .build();
    }

    @Override
    public PagedResponse<FavoriteResponse> searchUserFavorites(Long userId, String search, Pageable pageable) {
        log.debug("사용자 즐겨찾기 검색 - 사용자 ID: {}, 검색어: {}, 페이지: {}", userId, search, pageable.getPageNumber());
        
        validateUserExists(userId);
        
        Page<UserFavorite> favoritePage = userFavoriteRepository.findByUserIdWithSearch(userId, search, pageable);
        
        List<FavoriteResponse> favorites = favoritePage.getContent()
                .stream()
                .map(this::buildFavoriteResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.<FavoriteResponse>builder()
                .content(favorites)
                .page(favoritePage.getNumber())
                .limit(favoritePage.getSize())
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .first(favoritePage.isFirst())
                .last(favoritePage.isLast())
                .hasNext(favoritePage.hasNext())
                .hasPrevious(favoritePage.hasPrevious())
                .build();
    }

    @Override
    public boolean isFavorite(Long userId, Long campaignId) {
        return userFavoriteRepository.existsByUserIdAndCampaignId(userId, campaignId);
    }

    @Override
    public long getUserFavoriteCount(Long userId) {
        validateUserExists(userId);
        return userFavoriteRepository.countByUserId(userId);
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 캠페인 ID로 캠페인 조회
     */
    private Campaign findCampaignById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));
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
     * UserFavorite 엔티티로부터 FavoriteResponse 생성
     */
    private FavoriteResponse buildFavoriteResponse(UserFavorite favorite) {
        Campaign campaign = favorite.getCampaign();
        
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .campaignId(campaign.getId())
                .campaignTitle(campaign.getTitle())
                .campaignImage(campaign.getImageUrl())
                .campaignDescription(campaign.getDescription())
                .memo(favorite.getMemo())
                .createdAt(favorite.getCreatedAt())
                .campaignStatus(campaign.getStatus().toString())
                .campaignEndDate(campaign.getEndDate())
                .build();
    }
}