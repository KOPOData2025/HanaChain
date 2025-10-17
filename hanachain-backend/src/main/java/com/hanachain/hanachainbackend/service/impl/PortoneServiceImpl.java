package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.exception.BusinessException;
import com.hanachain.hanachainbackend.exception.InternalServerErrorException;
import com.hanachain.hanachainbackend.service.PortoneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * PortOne V2 결제 서비스 구현체
 * PortOne REST API v2를 사용하여 결제 취소 및 조회 기능 제공
 */
@Slf4j
@Service
public class PortoneServiceImpl implements PortoneService {

    private static final String PORTONE_API_BASE_URL = "https://api.portone.io";
    private static final String CANCEL_ENDPOINT = "/payments/{paymentId}/cancel";
    private static final String PAYMENT_INFO_ENDPOINT = "/payments/{paymentId}";

    @Value("${portone.api.secret}")
    private String apiSecret;

    private final RestTemplate restTemplate;

    public PortoneServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 결제 취소 (전액 환불)
     *
     * @param paymentId PortOne 결제 ID
     * @param reason 취소 사유
     * @return 취소 성공 여부
     */
    @Override
    public boolean cancelPayment(String paymentId, String reason) {
        log.info("=== PortOne Payment Cancellation Request ===");
        log.info("PaymentId: {}", paymentId);
        log.info("Reason: {}", reason);

        try {
            // 요청 헤더 설정
            HttpHeaders headers = createHeaders();

            // 요청 바디 설정 (전액 취소)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // API 호출
            String url = PORTONE_API_BASE_URL + CANCEL_ENDPOINT;
            log.info("Calling PortOne API: POST {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class,
                    paymentId
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ PortOne payment cancelled successfully");
                log.info("Response: {}", response.getBody());
                return true;
            } else {
                log.warn("⚠️ Unexpected response status: {}", response.getStatusCode());
                return false;
            }

        } catch (HttpClientErrorException e) {
            log.error("❌ PortOne API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            handlePortoneError(e.getStatusCode(), e.getResponseBodyAsString());
            return false;

        } catch (HttpServerErrorException e) {
            log.error("❌ PortOne API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InternalServerErrorException("PortOne 서버 오류: " + e.getMessage());

        } catch (Exception e) {
            log.error("❌ Unexpected error during payment cancellation: {}", e.getMessage(), e);
            throw new InternalServerErrorException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 부분 취소 (부분 환불)
     *
     * @param paymentId PortOne 결제 ID
     * @param amount 취소 금액
     * @param reason 취소 사유
     * @return 취소 성공 여부
     */
    @Override
    public boolean cancelPaymentPartial(String paymentId, BigDecimal amount, String reason) {
        log.info("=== PortOne Partial Payment Cancellation Request ===");
        log.info("PaymentId: {}", paymentId);
        log.info("Amount: {}", amount);
        log.info("Reason: {}", reason);

        try {
            // 요청 헤더 설정
            HttpHeaders headers = createHeaders();

            // 요청 바디 설정 (부분 취소)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amount.longValue()); // PortOne은 원 단위 정수
            requestBody.put("reason", reason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // API 호출
            String url = PORTONE_API_BASE_URL + CANCEL_ENDPOINT;
            log.info("Calling PortOne API: POST {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class,
                    paymentId
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ PortOne partial payment cancelled successfully");
                log.info("Response: {}", response.getBody());
                return true;
            } else {
                log.warn("⚠️ Unexpected response status: {}", response.getStatusCode());
                return false;
            }

        } catch (HttpClientErrorException e) {
            log.error("❌ PortOne API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            handlePortoneError(e.getStatusCode(), e.getResponseBodyAsString());
            return false;

        } catch (HttpServerErrorException e) {
            log.error("❌ PortOne API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InternalServerErrorException("PortOne 서버 오류: " + e.getMessage());

        } catch (Exception e) {
            log.error("❌ Unexpected error during partial payment cancellation: {}", e.getMessage(), e);
            throw new InternalServerErrorException("부분 결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 정보 조회
     *
     * @param paymentId PortOne 결제 ID
     * @return 결제 정보 (JSON)
     */
    @Override
    public String getPaymentInfo(String paymentId) {
        log.info("=== PortOne Payment Info Request ===");
        log.info("PaymentId: {}", paymentId);

        try {
            // 요청 헤더 설정
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // API 호출
            String url = PORTONE_API_BASE_URL + PAYMENT_INFO_ENDPOINT;
            log.info("Calling PortOne API: GET {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class,
                    paymentId
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ PortOne payment info retrieved successfully");
                return response.getBody();
            } else {
                log.warn("⚠️ Unexpected response status: {}", response.getStatusCode());
                return null;
            }

        } catch (HttpClientErrorException e) {
            log.error("❌ PortOne API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            handlePortoneError(e.getStatusCode(), e.getResponseBodyAsString());
            return null;

        } catch (Exception e) {
            log.error("❌ Unexpected error during payment info retrieval: {}", e.getMessage(), e);
            throw new InternalServerErrorException("결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * HTTP 헤더 생성
     * PortOne API 인증을 위한 Authorization 헤더 포함
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "PortOne " + apiSecret);
        return headers;
    }

    /**
     * PortOne API 오류 처리
     */
    private void handlePortoneError(HttpStatusCode statusCode, String responseBody) {
        log.error("PortOne API Error - Status: {}, Body: {}", statusCode, responseBody);

        // HttpStatusCode를 HttpStatus로 변환 (Spring Framework 6 호환)
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            throw new BusinessException("PortOne API 오류: " + statusCode);
        }

        switch (status) {
            case UNAUTHORIZED:
                throw new BusinessException("PortOne API 인증 실패: API Secret을 확인해주세요");

            case FORBIDDEN:
                throw new BusinessException("PortOne API 권한 없음: 해당 결제에 대한 권한이 없습니다");

            case NOT_FOUND:
                throw new BusinessException("결제 정보를 찾을 수 없습니다");

            case BAD_REQUEST:
                // 응답 바디에서 상세 오류 메시지 추출 가능
                if (responseBody.contains("CANCEL_AMOUNT_EXCEEDS_CANCELLABLE_AMOUNT")) {
                    throw new BusinessException("취소 가능한 금액을 초과했습니다");
                } else if (responseBody.contains("PAYMENT_ALREADY_CANCELLED")) {
                    throw new BusinessException("이미 취소된 결제입니다");
                } else {
                    throw new BusinessException("잘못된 요청: " + responseBody);
                }

            default:
                throw new BusinessException("PortOne API 오류: " + status);
        }
    }
}
