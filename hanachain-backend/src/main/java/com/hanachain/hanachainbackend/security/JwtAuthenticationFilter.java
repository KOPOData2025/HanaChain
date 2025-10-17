package com.hanachain.hanachainbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.info("🔍 [JWT Filter] 요청 수신: {} {}", method, requestURI);

        // 모든 요청 헤더 로깅 (디버깅용)
        log.info("🔍 [JWT Filter] 요청 헤더 목록:");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            // Authorization 헤더는 특별히 표시
            if ("Authorization".equalsIgnoreCase(headerName)) {
                log.info("🔍 [JWT Filter]   ⭐ {}: {}", headerName,
                    headerValue != null ? headerValue.substring(0, Math.min(30, headerValue.length())) + "..." : "null");
            } else {
                log.info("🔍 [JWT Filter]   {}: {}", headerName, headerValue);
            }
        }

        try {
            // Authorization 헤더 직접 확인
            String rawAuthHeader = request.getHeader("Authorization");
            log.info("🔍 [JWT Filter] Authorization 헤더 원본: {}",
                rawAuthHeader != null ? "있음 (" + rawAuthHeader.length() + "자)" : "❌ 없음");

            if (rawAuthHeader != null) {
                log.info("🔍 [JWT Filter] Authorization 헤더 상세: {}",
                    rawAuthHeader.substring(0, Math.min(50, rawAuthHeader.length())) + "...");
                log.info("🔍 [JWT Filter] Bearer 접두사 확인: {}",
                    rawAuthHeader.startsWith("Bearer ") ? "✅ 있음" : "❌ 없음");
            }

            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                log.info("✅ [JWT Filter] JWT 토큰 추출 성공: {}...", jwt.substring(0, Math.min(20, jwt.length())));

                if (tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    log.info("✅ [JWT Filter] JWT 검증 성공, 사용자명: {}", username);

                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        log.info("✅ [JWT Filter] 사용자 정보 로드 성공: {}, 권한: {}", username, userDetails.getAuthorities());

                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("✅ [JWT Filter] SecurityContext에 인증 정보 설정 완료: {}", authentication.getName());
                    } catch (Exception userLoadEx) {
                        log.error("❌ [JWT Filter] 사용자 정보 로드 실패: {}", username, userLoadEx);
                        throw userLoadEx;
                    }
                } else {
                    log.warn("⚠️ [JWT Filter] 유효하지 않은 JWT 토큰");
                }
            } else {
                log.warn("❌ [JWT Filter] Authorization 헤더에서 JWT 토큰을 추출하지 못함: {}", requestURI);
            }
        } catch (Exception ex) {
            log.error("❌ [JWT Filter] JWT 인증 처리 중 오류 발생: {}", requestURI, ex);
        }

        // 현재 SecurityContext 상태 로깅
        var context = SecurityContextHolder.getContext();
        var auth = context.getAuthentication();
        if (auth != null) {
            log.info("✅ [JWT Filter] 최종 SecurityContext 인증 상태: {}, 권한: {}",
                auth.getName(), auth.getAuthorities());
        } else {
            log.warn("❌ [JWT Filter] 최종 SecurityContext에 인증 정보 없음 - Anonymous 사용자로 처리됨");
        }

        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        log.info("🔍 [getJwtFromRequest] Authorization 헤더 추출:");
        log.info("🔍 [getJwtFromRequest]   - 헤더 존재: {}", bearerToken != null ? "예" : "아니오");

        if (bearerToken != null) {
            log.info("🔍 [getJwtFromRequest]   - 헤더 길이: {}", bearerToken.length());
            log.info("🔍 [getJwtFromRequest]   - 헤더 시작 부분: {}", bearerToken.substring(0, Math.min(20, bearerToken.length())));
            log.info("🔍 [getJwtFromRequest]   - Bearer 접두사: {}", bearerToken.startsWith("Bearer ") ? "있음" : "없음");
        }

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.info("✅ [getJwtFromRequest] 토큰 추출 성공 - 길이: {}, 시작: {}",
                token.length(), token.substring(0, Math.min(20, token.length())) + "...");
            return token;
        }

        log.warn("❌ [getJwtFromRequest] 토큰 추출 실패 - Bearer 접두사가 없거나 헤더가 비어있음");
        return null;
    }
}
