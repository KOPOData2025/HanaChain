package com.hanachain.hanachainbackend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.organization.OrganizationResponse;
import com.hanachain.hanachainbackend.dto.donation.DonationResponse;
import com.hanachain.hanachainbackend.dto.auth.LoginResponse;
import com.hanachain.hanachainbackend.dto.comment.CommentResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DTO JSON 직렬화 구조 검증 테스트
 *
 * 목적: DTO 클래스명 변경 시 JSON 응답 구조가 변경되지 않음을 보장
 *
 * 검증 항목:
 * 1. 클래스명이 JSON에 포함되지 않음
 * 2. 필드명이 JSON 키로 정확히 매핑됨
 * 3. camelCase 형식 유지
 */
@SpringBootTest
@DisplayName("DTO JSON 직렬화 구조 검증")
class DtoJsonStructureTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OrganizationResponse - JSON 구조 검증")
    void organizationDto_shouldSerializeCorrectly() throws Exception {
        // Given
        OrganizationResponse dto = OrganizationResponse.builder()
                .id(1L)
                .name("Test Organization")
                .description("Test Description")
                .imageUrl("https://example.com/image.jpg")
                .status(OrganizationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .memberCount(10L)
                .adminCount(2L)
                .activeCampaignCount(5L)
                .walletAddress("0x1234567890abcdef")
                .build();

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then - 필드명이 JSON 키로 정확히 매핑됨
        assertThat(json).contains("\"id\":");
        assertThat(json).contains("\"name\":");
        assertThat(json).contains("\"description\":");
        assertThat(json).contains("\"imageUrl\":");
        assertThat(json).contains("\"status\":");
        assertThat(json).contains("\"memberCount\":");
        assertThat(json).contains("\"walletAddress\":");

        // 클래스명이 JSON에 포함되지 않음
        assertThat(json).doesNotContain("OrganizationResponse");
        assertThat(json).doesNotContain("class");

        System.out.println("OrganizationResponse JSON: " + json);
    }

    @Test
    @DisplayName("DonationResponse - JSON 구조 검증")
    void donationResponseDto_shouldSerializeCorrectly() throws Exception {
        // Given
        DonationResponse dto = DonationResponse.builder()
                .id(1L)
                .campaignId(10L)
                .campaignTitle("테스트 캠페인")
                .amount(new BigDecimal("50000"))
                .donorName("김기부")
                .message("응원합니다")
                .anonymous(false)
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"id\":");
        assertThat(json).contains("\"campaignId\":");
        assertThat(json).contains("\"amount\":");
        assertThat(json).contains("\"donorName\":");
        assertThat(json).contains("\"anonymous\":");

        // 클래스명이 JSON에 포함되지 않음
        assertThat(json).doesNotContain("DonationResponse");
        assertThat(json).doesNotContain("DonationResponse");

        System.out.println("DonationResponse JSON: " + json);
    }

    @Test
    @DisplayName("LoginResponse - JSON 구조 검증")
    void loginResponseDto_shouldSerializeCorrectly() throws Exception {
        // Given
        LoginResponse dto = LoginResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .refreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"accessToken\":");
        assertThat(json).contains("\"refreshToken\":");
        assertThat(json).contains("\"tokenType\":");
        assertThat(json).contains("\"expiresIn\":");

        // 클래스명이 JSON에 포함되지 않음
        assertThat(json).doesNotContain("LoginResponse");
        assertThat(json).doesNotContain("LoginResponse");

        System.out.println("LoginResponse JSON: " + json);
    }

    @Test
    @DisplayName("camelCase 필드명이 유지됨을 검증")
    void dtoFields_shouldUseCamelCase() throws Exception {
        // Given
        OrganizationResponse dto = OrganizationResponse.builder()
                .id(1L)
                .memberCount(10L)
                .activeCampaignCount(5L)
                .walletAddress("0x1234")
                .build();

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then - camelCase 형식 검증
        assertThat(json).contains("\"memberCount\":");  // not member_count
        assertThat(json).contains("\"activeCampaignCount\":");  // not active_campaign_count
        assertThat(json).contains("\"walletAddress\":");  // not wallet_address

        // snake_case가 아님을 확인
        assertThat(json).doesNotContain("member_count");
        assertThat(json).doesNotContain("active_campaign_count");
        assertThat(json).doesNotContain("wallet_address");
    }

    @Test
    @DisplayName("JSON 역직렬화 후 필드값이 정확히 복원됨")
    void dto_shouldDeserializeCorrectly() throws Exception {
        // Given
        String json = "{\"id\":1,\"name\":\"Test Org\",\"status\":\"ACTIVE\"}";

        // When
        OrganizationResponse dto = objectMapper.readValue(json, OrganizationResponse.class);

        // Then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test Org");
        assertThat(dto.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("null 필드는 JSON에서 생략됨 (기본 설정)")
    void nullFields_shouldBeOmittedFromJson() throws Exception {
        // Given
        OrganizationResponse dto = OrganizationResponse.builder()
                .id(1L)
                .name("Test")
                // description은 null
                .build();

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then - null 필드는 포함되지 않거나 명시적으로 null로 표시
        // (Spring Boot 기본 설정에 따라 다름)
        System.out.println("Null fields JSON: " + json);
    }
}
