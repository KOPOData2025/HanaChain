package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.user.FavoriteAddRequest;
import com.hanachain.hanachainbackend.dto.user.FavoriteResponse;
import com.hanachain.hanachainbackend.dto.user.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * 즐겨찾기 관리 서비스 인터페이스
 */
public interface FavoriteService {
    
    /**
     * 즐겨찾기 추가
     * @param userId 사용자 ID
     * @param request 즐겨찾기 추가 요청
     * @return 추가된 즐겨찾기 정보
     */
    FavoriteResponse addFavorite(Long userId, FavoriteAddRequest request);
    
    /**
     * 즐겨찾기 제거
     * @param userId 사용자 ID
     * @param campaignId 캠페인 ID
     */
    void removeFavorite(Long userId, Long campaignId);
    
    /**
     * 사용자 즐겨찾기 목록 조회 (페이징)
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 즐겨찾기 목록
     */
    PagedResponse<FavoriteResponse> getUserFavorites(Long userId, Pageable pageable);
    
    /**
     * 즐겨찾기 목록 검색 (페이징)
     * @param userId 사용자 ID
     * @param search 검색어 (캠페인 제목)
     * @param pageable 페이징 정보
     * @return 페이징된 즐겨찾기 목록
     */
    PagedResponse<FavoriteResponse> searchUserFavorites(Long userId, String search, Pageable pageable);
    
    /**
     * 즐겨찾기 여부 확인
     * @param userId 사용자 ID
     * @param campaignId 캠페인 ID
     * @return 즐겨찾기 여부
     */
    boolean isFavorite(Long userId, Long campaignId);
    
    /**
     * 사용자 즐겨찾기 개수 조회
     * @param userId 사용자 ID
     * @return 즐겨찾기 개수
     */
    long getUserFavoriteCount(Long userId);
}