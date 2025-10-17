package com.hanachain.hanachainbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 작업을 위한 설정 클래스
 * 블록체인 작업을 위한 전용 스레드 풀을 제공합니다.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
    
    /**
     * 블록체인 작업을 위한 전용 스레드 풀 설정
     * 
     * 블록체인 트랜잭션은 네트워크 지연과 확인 대기 시간으로 인해
     * 상당한 시간이 소요될 수 있어 전용 스레드 풀을 사용합니다.
     */
    @Bean("blockchainTaskExecutor")
    public Executor blockchainTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 개수: 블록체인 작업의 특성상 I/O 대기가 많으므로 적절한 수준으로 설정
        executor.setCorePoolSize(5);
        
        // 최대 스레드 개수: 트래픽 증가 시 확장 가능하도록 설정
        executor.setMaxPoolSize(20);
        
        // 큐 용량: 대기 중인 작업을 버퍼링
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사: 로깅과 모니터링을 위한 식별
        executor.setThreadNamePrefix("Blockchain-");
        
        // 스레드 유지 시간: 비활성 스레드의 대기 시간
        executor.setKeepAliveSeconds(60);
        
        // 큐 포화 시 정책: 호출 스레드에서 직접 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 어플리케이션 종료 시 스레드 정리
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 스레드 풀 초기화
        executor.initialize();
        
        log.info("Blockchain task executor configured - Core: {}, Max: {}, Queue: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * 일반적인 비동기 작업을 위한 기본 스레드 풀 설정
     * 이메일 전송, 알림 등의 일반적인 비동기 작업에 사용됩니다.
     */
    @Bean("generalTaskExecutor")
    public Executor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 일반적인 비동기 작업을 위한 설정
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("General-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        
        executor.initialize();
        
        log.info("General task executor configured - Core: {}, Max: {}, Queue: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}