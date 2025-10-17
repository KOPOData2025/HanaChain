package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.notice.NoticeResponse;
import com.hanachain.hanachainbackend.dto.notice.NoticeListResponse;
import com.hanachain.hanachainbackend.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notice", description = "공지사항 관련 API")
public class NoticeController {
    
    private final NoticeService noticeService;
    
    @GetMapping
    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 페이징하여 조회합니다.")
    public ResponseEntity<ApiResponse<Page<NoticeListResponse>>> getNotices(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        log.info("공지사항 목록 조회 요청 - 페이지: {}", pageable.getPageNumber());
        Page<NoticeListResponse> notices = noticeService.getNotices(pageable);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }
    
    @GetMapping("/recent")
    @Operation(summary = "최근 공지사항 조회", description = "최근 공지사항을 지정된 개수만큼 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoticeListResponse>>> getRecentNotices(
            @Parameter(description = "조회할 공지사항 개수", example = "3")
            @RequestParam(defaultValue = "3") int limit
    ) {
        log.info("최근 공지사항 조회 요청 - 개수: {}", limit);
        List<NoticeListResponse> notices = noticeService.getRecentNotices(limit);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }
    
    @GetMapping("/important")
    @Operation(summary = "중요 공지사항 조회", description = "중요 공지사항을 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoticeListResponse>>> getImportantNotices(
            @Parameter(description = "조회할 공지사항 개수", example = "5")
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("중요 공지사항 조회 요청 - 개수: {}", limit);
        List<NoticeListResponse> notices = noticeService.getImportantNotices(limit);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNoticeDetail(
            @Parameter(description = "공지사항 ID", required = true)
            @PathVariable Long id
    ) {
        log.info("공지사항 상세 조회 요청 - ID: {}", id);
        NoticeResponse notice = noticeService.getNoticeDetail(id);
        return ResponseEntity.ok(ApiResponse.success(notice));
    }
}

