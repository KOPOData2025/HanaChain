package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.donation.DonationResponse;
import com.hanachain.hanachainbackend.dto.donation.PaymentWebhook;
import com.hanachain.hanachainbackend.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * PortOne 결제 웹훅 처리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook API", description = "결제 웹훅 처리 API")
public class PaymentWebhookController {
    
    private final DonationService donationService;
    
    // PortOne 웹훅 시크릿 키 (환경 변수로 관리)
    @Value("${portone.webhook.secret:default-secret-key}")
    private String webhookSecret;
    
    /**
     * PortOne 결제 완료 웹훅 처리
     * PortOne에서 결제가 완료되면 이 엔드포인트로 웹훅을 전송합니다
     */
    @PostMapping("/payment")
    @Operation(summary = "결제 웹훅 처리", description = "PortOne에서 전송하는 결제 웹훅을 처리합니다")
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @RequestBody PaymentWebhook webhookDto,
            @RequestHeader(value = "X-ImpWebhook-Signature", required = false) String signature) {
        
        log.info("=== Payment Webhook Received ===");
        log.info("MerchantUid: {}", webhookDto.getMerchantUid());
        log.info("ImpUid: {}", webhookDto.getImpUid());
        log.info("Status: {}", webhookDto.getStatus());
        log.info("Amount: {}", webhookDto.getAmount());
        log.info("PayMethod: {}", webhookDto.getPayMethod());
        log.info("PaidAt: {}", webhookDto.getPaidAt());
        log.info("Signature: {}", signature != null ? "Present" : "Not provided");
        log.info("isPaid(): {}", webhookDto.isPaid());
        log.info("isFailed(): {}", webhookDto.isFailed());
        log.info("isCancelled(): {}", webhookDto.isCancelled());
        
        try {
            // 웹훅 서명 검증 (프로덕션 환경에서 필수)
            if (isProductionEnvironment() && !verifyWebhookSignature(webhookDto, signature)) {
                log.error("Invalid webhook signature for merchantUid: {}", webhookDto.getMerchantUid());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Invalid signature"));
            }
            
            // 결제 정보 처리
            String paymentId = webhookDto.getMerchantUid();
            log.info("Processing payment for paymentId: {}", paymentId);
            
            DonationResponse donation = donationService.processDonationPayment(paymentId, webhookDto);
            
            log.info("=== Payment Webhook Processing Complete ===");
            log.info("DonationId: {}", donation.getId());
            log.info("Final PaymentStatus: {}", donation.getPaymentStatus());
            log.info("Campaign Updated: {}", donation.getCampaignTitle());
            
            // PortOne에 성공 응답 전송
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Webhook processed successfully",
                    "donationId", donation.getId(),
                    "paymentStatus", donation.getPaymentStatus()
            ));
            
        } catch (Exception e) {
            log.error("=== Payment Webhook Processing Error ===");
            log.error("MerchantUid: {}", webhookDto.getMerchantUid());
            log.error("Error Type: {}", e.getClass().getSimpleName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Full Stack Trace: ", e);
            
            // 오류가 발생해도 PortOne에 200 OK를 반환해야 재전송을 방지할 수 있음
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "errorType", e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * PortOne 결제 검증 웹훅 (선택적)
     * 결제 검증이 필요한 경우 사용
     */
    @PostMapping("/payment/verify")
    @Operation(summary = "결제 검증 웹훅", description = "PortOne 결제 검증 웹훅을 처리합니다")
    public ResponseEntity<Map<String, Object>> handlePaymentVerification(
            @RequestBody Map<String, Object> verificationData) {
        
        log.info("Received payment verification webhook: {}", verificationData);
        
        // 검증 로직 구현
        // PortOne REST API를 호출하여 실제 결제 정보와 비교
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Verification processed"
        ));
    }
    
    /**
     * 웹훅 테스트 엔드포인트 (개발용)
     */
    @PostMapping("/test")
    @Operation(summary = "웹훅 테스트", description = "웹훅 처리를 테스트하기 위한 엔드포인트")
    public ResponseEntity<Map<String, Object>> testWebhook(
            @RequestBody Map<String, Object> testData) {
        
        log.info("Test webhook received: {}", testData);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test webhook received",
                "data", testData
        ));
    }
    
    /**
     * 웹훅 서명 검증
     * PortOne에서 전송한 웹훅의 서명을 검증하여 위조를 방지
     */
    private boolean verifyWebhookSignature(PaymentWebhook webhookDto, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        
        try {
            // 웹훅 데이터를 문자열로 변환 (PortOne 서명 생성 방식에 따라 조정 필요)
            String dataToSign = String.format("%s%s%s", 
                    webhookDto.getMerchantUid(), 
                    webhookDto.getAmount(), 
                    webhookDto.getStatus());
            
            // HMAC-SHA256으로 서명 생성
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            
            // 서명 비교
            boolean isValid = computedSignature.equals(signature);
            
            if (!isValid) {
                log.warn("Signature mismatch. Expected: {}, Received: {}", 
                        computedSignature, signature);
            }
            
            return isValid;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * 프로덕션 환경 확인
     */
    private boolean isProductionEnvironment() {
        // 환경 변수나 프로파일을 통해 프로덕션 환경인지 확인
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "prod".equals(profile) || "production".equals(profile);
    }
    
    /**
     * 웹훅 상태 확인 (헬스 체크)
     */
    @GetMapping("/status")
    @Operation(summary = "웹훅 상태 확인", description = "웹훅 처리 서비스의 상태를 확인합니다")
    public ResponseEntity<Map<String, Object>> getWebhookStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "message", "Webhook service is running",
                "timestamp", System.currentTimeMillis()
        ));
    }
}