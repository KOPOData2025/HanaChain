package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.fds.FdsRequest;
import com.hanachain.hanachainbackend.dto.fds.FdsResponse;
import com.hanachain.hanachainbackend.service.FdsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * FDS (사기 탐지 시스템) 검증 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FdsServiceImpl implements FdsService {

    private final WebClient.Builder webClientBuilder;

    @Value("${fds.api.url:http://localhost:8000}")
    private String fdsApiUrl;

    @Value("${fds.api.timeout:3000}")
    private int fdsTimeout;

    /**
     * FDS 검증을 비동기로 수행
     */
    @Override
    public CompletableFuture<FdsResponse> verifyTransactionAsync(FdsRequest request) {
        log.info("=== FDS 비동기 검증 시작 ===");
        log.info("FDS 요청: amount={}, campaign_id={}, user_id={}, payment_method={}",
                request.getAmount(), request.getCampaign_id(), request.getUser_id(), request.getPayment_method());

        return CompletableFuture.supplyAsync(() -> {
            try {
                return verifyTransaction(request);
            } catch (Exception e) {
                log.error("FDS 비동기 검증 실패: {}", e.getMessage());
                throw new RuntimeException("FDS verification failed", e);
            }
        });
    }

    /**
     * FDS 검증을 동기로 수행
     */
    @Override
    public FdsResponse verifyTransaction(FdsRequest request) {
        log.info("=== FDS 동기 검증 시작 ===");

        try {
            WebClient webClient = webClientBuilder.baseUrl(fdsApiUrl).build();

            FdsResponse response = webClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        log.error("FDS API 에러 응답: status={}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("FDS API returned error: " + clientResponse.statusCode()));
                    })
                    .bodyToMono(FdsResponse.class)
                    .timeout(Duration.ofMillis(fdsTimeout))
                    .block();

            if (response != null) {
                log.info("✅ FDS 검증 성공");
                log.info("  - action: {}", response.getAction());
                log.info("  - risk_score: {}", response.getRiskScore());
                log.info("  - confidence: {}", response.getConfidence());
                log.info("  - explanation: {}", response.getExplanation());
                return response;
            } else {
                log.warn("⚠️ FDS API 응답이 null입니다");
                throw new RuntimeException("FDS API returned null response");
            }

        } catch (Exception e) {
            log.error("❌ FDS 검증 실패: {}", e.getMessage(), e);
            throw new RuntimeException("FDS verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * FDS 서비스 상태 확인
     */
    @Override
    public boolean isAvailable() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(fdsApiUrl).build();

            String response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .block();

            boolean available = response != null && response.contains("healthy");
            log.info("FDS 서비스 상태: {}", available ? "가용" : "불가용");
            return available;

        } catch (Exception e) {
            log.warn("FDS 서비스 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}
