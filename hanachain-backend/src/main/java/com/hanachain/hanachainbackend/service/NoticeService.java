package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.notice.NoticeResponse;
import com.hanachain.hanachainbackend.dto.notice.NoticeListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoticeService {
    
    /**
     * 공지사항 목록을 조회합니다. (페이징)
     */
    Page<NoticeListResponse> getNotices(Pageable pageable);
    
    /**
     * 최근 공지사항을 조회합니다.
     */
    List<NoticeListResponse> getRecentNotices(int limit);
    
    /**
     * 중요 공지사항을 조회합니다.
     */
    List<NoticeListResponse> getImportantNotices(int limit);
    
    /**
     * 공지사항 상세 정보를 조회합니다.
     */
    NoticeResponse getNoticeDetail(Long id);
}

