package com.hanachain.hanachainbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러 설정
     * 특정 경로만 정적 리소스로 처리하도록 제한
     *
     * 중요: 이 설정으로 Spring Boot의 기본 /** 리소스 핸들러를 비활성화하고
     * 명시적으로 지정된 경로만 정적 리소스로 처리합니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 기본 정적 리소스 매핑을 비활성화하기 위해
        // 명시적으로 지정된 경로만 추가
        registry.setOrder(Integer.MIN_VALUE); // 가장 낮은 우선순위로 설정

        // 정적 리소스 경로만 명시적으로 활성화
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false); // 리소스 체인 비활성화

        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/")
                .resourceChain(false);
    }

    /**
     * Path matching 설정
     * 후행 슬래시 무시
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(false); // 후행 슬래시 무시
    }
}
