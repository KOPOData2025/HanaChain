package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.dto.user.DashboardResponse;
import com.hanachain.hanachainbackend.dto.user.DonationHistoryResponse;
import com.hanachain.hanachainbackend.dto.user.DonationStatsResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileResponse;
import com.hanachain.hanachainbackend.service.MyPageService;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.repository.UserFavoriteRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 마이페이지 대시보드 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {
    
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    
    @Override
    public DashboardResponse getDashboard(Long userId) {
        log.debug("사용자 대시보드 정보 조회 시작: userId={}", userId);
        
        try {
            // 사용자 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            // 프로필 정보 생성
            ProfileResponse profile = ProfileResponse.from(user);
            
            // 기부 통계 조회
            DonationStatsResponse donationStats = getDonationStats(userId);
            
            // 최근 기부 내역 조회 (최대 5건)
            List<DonationHistoryResponse> recentDonations = donationRepository
                    .findRecentUserDonations(userId, PageRequest.of(0, 5))
                    .stream()
                    .map(DonationHistoryResponse::from)
                    .collect(Collectors.toList());
            
            // 즐겨찾기 캠페인 수 조회
            Long favoriteCampaignsCount = userFavoriteRepository.countByUserId(userId);
            
            log.debug("사용자 대시보드 정보 조회 완료: userId={}, donationCount={}, favoriteCount={}", 
                    userId, donationStats.getTotalCount(), favoriteCampaignsCount);
            
            return DashboardResponse.builder()
                    .profile(profile)
                    .donationStats(donationStats)
                    .recentDonations(recentDonations)
                    .favoriteCampaignsCount(favoriteCampaignsCount)
                    .build();
            
        } catch (Exception e) {
            log.error("사용자 대시보드 정보 조회 중 오류 발생: userId={}", userId, e);
            throw e;
        }
    }
    
    @Override
    public DashboardResponse getDashboardSummary(Long userId) {
        log.debug("사용자 대시보드 요약 정보 조회 시작: userId={}", userId);
        
        try {
            // 사용자 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            // 프로필 정보 생성 (간소화)
            ProfileResponse profile = ProfileResponse.from(user);
            
            // 기부 통계 조회 (캐시된 User 엔티티 값 사용 - 빠른 응답)
            DonationStatsResponse donationStats = DonationStatsResponse.fromUser(user);
            
            // 즐겨찾기 캠페인 수 조회
            Long favoriteCampaignsCount = userFavoriteRepository.countByUserId(userId);
            
            log.debug("사용자 대시보드 요약 정보 조회 완료: userId={}", userId);
            
            return DashboardResponse.builder()
                    .profile(profile)
                    .donationStats(donationStats)
                    .recentDonations(List.of()) // 요약에서는 최근 기부 내역 제외
                    .favoriteCampaignsCount(favoriteCampaignsCount)
                    .build();
                    
        } catch (Exception e) {
            log.error("사용자 대시보드 요약 정보 조회 중 오류 발생: userId={}", userId, e);
            throw e;
        }
    }
    
    /**
     * 사용자의 상세 기부 통계 조회
     */
    private DonationStatsResponse getDonationStats(Long userId) {
        try {
            log.debug("🔍 getDonationStats 시작: userId={}", userId);
            Object[] stats = donationRepository.getUserDonationStats(userId);

            // 쿼리 결과 로깅
            if (stats != null && stats.length > 0) {
                log.info("📊 getUserDonationStats 쿼리 결과: totalAmount={}, totalCount={}, completedCount={}, pendingCount={}, failedCount={}",
                        stats[0], stats[1], stats[2], stats[3], stats[4]);
            } else {
                log.warn("⚠️ getUserDonationStats 쿼리 결과가 null이거나 비어있습니다: userId={}", userId);
            }

            DonationStatsResponse response = DonationStatsResponse.from(stats);
            log.info("✅ DonationStatsResponse 생성 완료: {}", response);
            return response;
        } catch (Exception e) {
            log.warn("❌ 기부 통계 조회 중 오류 발생, 기본값 반환: userId={}", userId, e);
            // 오류 발생 시 User 엔티티의 캐시된 값 사용
            User user = userRepository.findById(userId).orElse(null);
            return user != null ? DonationStatsResponse.fromUser(user) : DonationStatsResponse.builder().build();
        }
    }
}