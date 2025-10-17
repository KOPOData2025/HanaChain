package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.notice.NoticeResponse;
import com.hanachain.hanachainbackend.dto.notice.NoticeListResponse;
import com.hanachain.hanachainbackend.entity.Notice;
import com.hanachain.hanachainbackend.exception.NotFoundException;
import com.hanachain.hanachainbackend.repository.NoticeRepository;
import com.hanachain.hanachainbackend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {
    
    private final NoticeRepository noticeRepository;
    
    @Override
    public Page<NoticeListResponse> getNotices(Pageable pageable) {
        log.debug("공지사항 목록 조회 - 페이지: {}", pageable.getPageNumber());
        Page<Notice> notices = noticeRepository.findAllActive(pageable);
        return notices.map(NoticeListResponse::fromEntity);
    }
    
    @Override
    public List<NoticeListResponse> getRecentNotices(int limit) {
        log.debug("최근 공지사항 조회 - 개수: {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<Notice> notices = noticeRepository.findRecentNotices(pageable);
        return notices.stream()
                .map(NoticeListResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NoticeListResponse> getImportantNotices(int limit) {
        log.debug("중요 공지사항 조회 - 개수: {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<Notice> notices = noticeRepository.findImportantNotices(pageable);
        return notices.stream()
                .map(NoticeListResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public NoticeResponse getNoticeDetail(Long id) {
        log.debug("공지사항 상세 조회 - ID: {}", id);
        Notice notice = noticeRepository.findByIdActive(id)
                .orElseThrow(() -> new NotFoundException("공지사항을 찾을 수 없습니다. ID: " + id));
        
        // 조회수 증가
        notice.incrementViewCount();
        noticeRepository.save(notice);
        
        return NoticeResponse.fromEntity(notice);
    }
}

