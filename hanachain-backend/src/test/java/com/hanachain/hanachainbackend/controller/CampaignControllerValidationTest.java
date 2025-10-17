package com.hanachain.hanachainbackend.controller;

import com.hanachain.hanachainbackend.controller.api.CampaignController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CampaignController의 정렬 필드 검증 로직을 테스트합니다.
 */
class CampaignControllerValidationTest {

    private CampaignController controller;
    private Method validateMethod;

    @BeforeEach
    void setUp() throws Exception {
        controller = new CampaignController(null); // 서비스는 null이어도 검증 테스트에는 문제없음
        
        // private 메서드에 접근하기 위해 리플렉션 사용
        validateMethod = CampaignController.class.getDeclaredMethod("validateAndSanitizePageable", Pageable.class);
        validateMethod.setAccessible(true);
    }

    @Test
    void testValidSortField() throws Exception {
        // Given: 유효한 정렬 필드
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // When: 검증 메서드 호출
        Pageable result = (Pageable) validateMethod.invoke(controller, pageable);
        
        // Then: 원본 그대로 반환
        assertEquals(pageable, result);
        assertEquals("createdAt", result.getSort().iterator().next().getProperty());
    }

    @Test
    void testInvalidSortField_recent() throws Exception {
        // Given: 유효하지 않은 정렬 필드 "recent"
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "recent"));
        
        // When: 검증 메서드 호출
        Pageable result = (Pageable) validateMethod.invoke(controller, pageable);
        
        // Then: 기본 정렬로 대체됨
        assertNotEquals(pageable, result);
        assertEquals("createdAt", result.getSort().iterator().next().getProperty());
        assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
    }

    @Test
    void testMixedValidInvalidSortFields() throws Exception {
        // Given: 유효한 필드와 유효하지 않은 필드가 섞인 경우
        Pageable pageable = PageRequest.of(0, 10, 
            Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.ASC, "recent"))
                .and(Sort.by(Sort.Direction.DESC, "title")));
        
        // When: 검증 메서드 호출
        Pageable result = (Pageable) validateMethod.invoke(controller, pageable);
        
        // Then: 유효한 필드만 유지됨
        assertEquals(2, result.getSort().stream().count()); // createdAt, title만 유지
        assertTrue(result.getSort().stream()
            .anyMatch(order -> "createdAt".equals(order.getProperty())));
        assertTrue(result.getSort().stream()
            .anyMatch(order -> "title".equals(order.getProperty())));
        assertFalse(result.getSort().stream()
            .anyMatch(order -> "recent".equals(order.getProperty())));
    }

    @Test
    void testUnsortedPageable() throws Exception {
        // Given: 정렬이 없는 Pageable
        Pageable pageable = PageRequest.of(0, 10);
        
        // When: 검증 메서드 호출
        Pageable result = (Pageable) validateMethod.invoke(controller, pageable);
        
        // Then: 원본 그대로 반환
        assertEquals(pageable, result);
        assertTrue(result.getSort().isUnsorted());
    }

    @Test
    void testAllInvalidSortFields() throws Exception {
        // Given: 모든 정렬 필드가 유효하지 않은 경우
        Pageable pageable = PageRequest.of(1, 20, 
            Sort.by(Sort.Direction.ASC, "invalid1")
                .and(Sort.by(Sort.Direction.DESC, "invalid2")));
        
        // When: 검증 메서드 호출
        Pageable result = (Pageable) validateMethod.invoke(controller, pageable);
        
        // Then: 기본 정렬로 대체되지만 페이지 정보는 유지됨
        assertEquals(1, result.getPageNumber());
        assertEquals(20, result.getPageSize());
        assertEquals("createdAt", result.getSort().iterator().next().getProperty());
        assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
    }
}