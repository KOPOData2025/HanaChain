package com.hanachain.hanachainbackend.config;

import com.hanachain.hanachainbackend.security.CustomUserDetailsService;
import com.hanachain.hanachainbackend.security.JwtAuthenticationEntryPoint;
import com.hanachain.hanachainbackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.security.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) // H2 Console을 위한 설정
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 공개 인증 엔드포인트 - context path가 제거되므로 /api/auth/가 아닌 /auth/ 사용
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/check-email").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/check-email").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/check-nickname").permitAll()
                .requestMatchers("/auth/verification/**").permitAll()
                .requestMatchers("/auth/signup/**").permitAll()

                // 기타 공개 엔드포인트
                .requestMatchers("/campaigns/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/comments/public/**").permitAll() // 공개 댓글 조회
                .requestMatchers("/health").permitAll()
                
                // 공개 캠페인 엔드포인트들 - 인증 없이 허용
                .requestMatchers(HttpMethod.GET, "/campaigns").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaigns/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaigns/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaigns/popular").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaigns/recent").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaigns/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaigns/category/**").permitAll()
                
                // 공지사항 엔드포인트들 - 인증 없이 허용
                .requestMatchers(HttpMethod.GET, "/notices").permitAll()
                .requestMatchers(HttpMethod.GET, "/notices/**").permitAll()
                
                // 캠페인 담당자 조회 엔드포인트들 - 인증 없이 허용
                .requestMatchers(HttpMethod.GET, "/campaign-managers/campaigns/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/campaign-managers/users/**").permitAll()

                // 댓글 조회 엔드포인트들 - 인증 없이 허용
                .requestMatchers(HttpMethod.GET, "/campaigns/*/comments").permitAll()
                .requestMatchers(HttpMethod.GET, "/comments/*").permitAll()
                
                // 블록체인 엔드포인트들 - 인증 없이 허용
                .requestMatchers(HttpMethod.GET, "/blockchain/**").permitAll()

                // 우수 단체 조회 엔드포인트들 - 인증 없이 허용 (메인 페이지)
                .requestMatchers(HttpMethod.GET, "/organizations").permitAll()
                .requestMatchers(HttpMethod.GET, "/organizations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/organizations/search").permitAll()

                // 개발 환경 엔드포인트 (개발 프로파일에서만)
                .requestMatchers("/dev/**").permitAll()

                // H2 Console (개발 환경에서만)
                .requestMatchers("/h2-console/**").permitAll()

                // Swagger/OpenAPI 엔드포인트
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()

                // Actuator 엔드포인트
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // 관리자 엔드포인트
                // .requestMatchers("/admin/**").hasRole("ADMIN") // 디버깅을 위해 임시 비활성화
                .requestMatchers(HttpMethod.PUT, "/expenses/*/approve").hasRole("ADMIN") // 지출 승인
                .requestMatchers(HttpMethod.PUT, "/expenses/*/reject").hasRole("ADMIN")  // 지출 거부
                .requestMatchers(HttpMethod.GET, "/expenses/pending").hasRole("ADMIN")   // 승인 대기 지출 조회
                .requestMatchers(HttpMethod.PUT, "/comments/*/hide").hasRole("ADMIN")    // 댓글 숨김
                .requestMatchers(HttpMethod.GET, "/comments/reported").hasRole("ADMIN")  // 신고된 댓글 조회
                
                // 기부 엔드포인트 - 익명 기부 허용
                .requestMatchers(HttpMethod.POST, "/donations").permitAll()
                .requestMatchers(HttpMethod.POST, "/donations/verify-payment").permitAll()

                // 관리자 기부 엔드포인트 - 일반 기부 규칙 앞에 배치
                .requestMatchers(HttpMethod.GET, "/donations/admin/all").hasRole("ADMIN")  // 전체 기부 내역 조회
                .requestMatchers(HttpMethod.GET, "/donations/users/**").hasRole("ADMIN")  // 사용자별 기부 내역 조회
                .requestMatchers(HttpMethod.POST, "/donations/*/refund").hasRole("ADMIN")  // 기부 환불
                .requestMatchers(HttpMethod.GET, "/donations/*/fds").hasRole("ADMIN")  // FDS 상세 조회
                .requestMatchers(HttpMethod.POST, "/donations/*/fds/override").hasRole("ADMIN")  // FDS 오버라이드

                // 웹훅 엔드포인트 - 외부 서비스(PortOne) 호출 허용
                .requestMatchers("/webhooks/**").permitAll()

                // 관리자 사용자 엔드포인트 - 일반 사용자 규칙 앞에 배치
                .requestMatchers(HttpMethod.GET, "/users/list").hasRole("ADMIN")  // 관리자 사용자 목록 조회

                // 사용자 엔드포인트 - ADMIN도 USER의 모든 권한 포함
                .requestMatchers("/users/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/campaigns/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/donations/**").hasAnyRole("USER", "ADMIN")  // 기타 기부 엔드포인트는 인증 필요
                .requestMatchers("/expenses/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/comments/**").hasAnyRole("USER", "ADMIN")

                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
