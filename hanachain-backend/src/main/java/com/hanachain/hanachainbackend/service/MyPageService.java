package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.user.DashboardResponse;

/**
 * 마이페이지 대시보드 서비스 인터페이스
 */
public interface MyPageService {
    
    /**
     * 사용자 대시보드 정보 조회
     * @param userId 사용자 ID
     * @return 대시보드 응답 DTO
     */
    DashboardResponse getDashboard(Long userId);
    
    /**
     * 대시보드 요약 정보 조회
     * @param userId 사용자 ID
     * @return 대시보드 응답 DTO
     */
    DashboardResponse getDashboardSummary(Long userId);
}