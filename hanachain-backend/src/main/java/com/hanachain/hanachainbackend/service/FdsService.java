package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.fds.FdsRequest;
import com.hanachain.hanachainbackend.dto.fds.FdsResponse;

import java.util.concurrent.CompletableFuture;

/**
 * FDS (사기 탐지 시스템) 검증 서비스 인터페이스
 */
public interface FdsService {

    /**
     * FDS 검증을 비동기로 수행
     *
     * @param request FDS 요청 데이터
     * @return FDS 응답 CompletableFuture
     */
    CompletableFuture<FdsResponse> verifyTransactionAsync(FdsRequest request);

    /**
     * FDS 검증을 동기로 수행
     *
     * @param request FDS 요청 데이터
     * @return FDS 응답
     */
    FdsResponse verifyTransaction(FdsRequest request);

    /**
     * FDS 서비스 상태 확인
     *
     * @return FDS 서비스 가용 여부
     */
    boolean isAvailable();
}
