package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.VerificationSession;
import com.hanachain.hanachainbackend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from}")
    private String fromAddress;
    
    /**
     * 이메일 서비스 설정을 검증합니다.
     */
    private void validateEmailConfiguration() {
        if (fromAddress == null || fromAddress.isEmpty()) {
            throw new RuntimeException("Email from address is not configured");
        }
        if (mailSender == null) {
            throw new RuntimeException("JavaMailSender is not configured");
        }
        log.debug("Email configuration validated - From: {}", fromAddress);
    }
    
    @Override
    public void sendVerificationEmail(String to, String verificationCode, VerificationSession.VerificationType type) {
        log.info("Attempting to send verification email to: {} for type: {}", to, type);
        
        validateEmailConfiguration();
        
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address cannot be null or empty");
        }
        
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        
        String subject = getSubjectByType(type);
        String content = createVerificationEmailContent(verificationCode, type);
        
        log.debug("Sending verification email - To: {}, Subject: {}, Code: {}", to, subject, verificationCode);
        
        sendHtmlEmail(to, subject, content);
        
        log.info("Verification email sent successfully to: {}", to);
    }
    
    @Override
    public void sendPasswordResetEmail(String to, String verificationCode) {
        sendVerificationEmail(to, verificationCode, VerificationSession.VerificationType.PASSWORD_RESET);
    }
    
    @Override
    public void sendEmailChangeConfirmation(String to, String verificationCode) {
        sendVerificationEmail(to, verificationCode, VerificationSession.VerificationType.EMAIL_CHANGE);
    }
    
    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            log.debug("Creating HTML email message - To: {}, Subject: {}", to, subject);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            log.debug("Sending HTML email via JavaMailSender...");
            mailSender.send(message);
            
            log.info("HTML email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("MessagingException while sending HTML email to: {} - Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email due to messaging error: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("Unexpected error while sending HTML email to: {} - Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email due to unexpected error: " + e.getMessage(), e);
        }
    }
    
    private String getSubjectByType(VerificationSession.VerificationType type) {
        return switch (type) {
            case EMAIL_REGISTRATION -> "[위아하나] 이메일 인증 코드";
            case PASSWORD_RESET -> "[위아하나] 비밀번호 재설정 인증 코드";
            case EMAIL_CHANGE -> "[위아하나] 이메일 변경 인증 코드";
        };
    }
    
    private String createVerificationEmailContent(String verificationCode, VerificationSession.VerificationType type) {
        String action = getActionByType(type);
        
        return String.format("""
            <!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>위아하나 인증 코드</title>
    <style>
      @import url("https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;700;800&display=swap");
      @import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css");
    </style>
  </head>
  <body
    style="
      font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Noto Sans KR',
        -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      max-width: 600px;
      margin: 0 auto;
      padding: 0;
      background-color: #f8f9fa;
    "
  >
    <!-- Header with Logo -->
    <div
      style="
        background-color: white;
        padding: 24px 32px;
        border-bottom: 1px solid #e9ecef;
      "
    >
      <div style="display: flex; align-items: center">
        <img
          src="https://www.hanafn.com/assets/img/ko/info/img-hana-symbol.png"
          alt="HanaChain Logo"
          style="
            width: 40px;
            height: 40px;
            margin-right: 12px;
            object-fit: contain;
          "
        />
        <h1
          style="
            color: #009591;
            margin: 0;
            font-size: 24px;
            font-weight: 700;
            letter-spacing: -0.5px;
          "
        >
          HanaChain
        </h1>
      </div>
    </div>

    <!-- Main Content -->
    <div style="background-color: white; padding: 40px 32px">
      <h2
        style="
          color: #212529;
          margin: 0 0 24px 0;
          font-size: 20px;
          font-weight: 500;
          line-height: 1.4;
        "
      >
        안녕하세요.
      </h2>

      <p
        style="
          color: #495057;
          margin: 0 0 16px 0;
          font-size: 16px;
          line-height: 1.6;
          font-weight: 500;
        "
      >
        HanaChain 계정 %s 확인을 위한<br />
        인증코드 안내드립니다.
      </p>

      <div style="margin: 32px 0">
        <p
          style="
            color: #6c757d;
            margin: 0 0 24px 0;
            font-size: 14px;
            line-height: 1.5;
          "
        >
          HanaChain 계정 %s 확인을 위한 인증코드를 아래와 같이<br />
          안내해 드립니다.
        </p>

        <p
          style="
            color: #009591;
            margin: 0 0 16px 0;
            font-size: 14px;
            font-weight: 500;
          "
        >
          본 안내 메일 발송 후 10분 이내에 HanaChain 인증코드 확인<br />
          화면에 입력해 주세요.
        </p>
      </div>

      <!-- Verification Code Box -->
      <div
        style="
          background-color: #f0fffe;
          border-radius: 8px;
          padding: 24px;
          text-align: center;
          margin: 32px 0;
        "
      >
        <div
          style="
            color: #009591;
            font-size: 36px;
            font-weight: 700;
            letter-spacing: 8px;
            font-family: 'Courier New', monospace;
          "
        >
          %s
        </div>
      </div>

      <p
        style="
          color: #6c757d;
          margin: 24px 0 0 0;
          font-size: 14px;
          line-height: 1.5;
        "
      >
        만약 본인이 이메일 주소 확인 신청을 한 것이 아니라면, 본<br />
        메일은 무시해 주세요.
      </p>

      <p
        style="
          color: #6c757d;
          margin: 16px 0 0 0;
          font-size: 14px;
          line-height: 1.5;
        "
      >
        기타 문의 사항이 있으시면,
        <span style="color: #009591; text-decoration: underline"
          >support@wearehana.org</span
        >로<br />
        보내주세요.
      </p>
    </div>

    <!-- Footer -->
    <div
      style="
        background-color: #f8f9fa;
        padding: 24px 32px;
        text-align: center;
        border-top: 1px solid #e9ecef;
      "
    >
      <p style="color: #6c757d; margin: 0; font-size: 14px; font-weight: 500">
        HanaChain 팀
      </p>
    </div>
  </body>
</html>

            """, action, action, verificationCode);
    }
    
    private String getActionByType(VerificationSession.VerificationType type) {
        return switch (type) {
            case EMAIL_REGISTRATION -> "회원가입";
            case PASSWORD_RESET -> "비밀번호 재설정";
            case EMAIL_CHANGE -> "이메일 변경";
        };
    }
}
