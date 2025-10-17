package com.hanachain.hanachainbackend.batch.reader;

import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 기부 내역 ItemReader
 *
 * 완료된 결제 중 블록체인에 기록되지 않은 기부 내역을 조회합니다.
 * RepositoryItemReader를 사용하여 페이징 처리를 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonationItemReader {

    private final DonationRepository donationRepository;

    /**
     * 캠페인 ID에 해당하는 미처리 기부 내역 Reader 생성
     *
     * @param campaignId 캠페인 ID
     * @param pageSize 페이지 크기 (Chunk 크기와 동일)
     * @return RepositoryItemReader
     */
    public RepositoryItemReader<Donation> createReader(Long campaignId, int pageSize) {
        log.info("Creating DonationItemReader for campaign: {}, pageSize: {}", campaignId, pageSize);

        RepositoryItemReader<Donation> reader = new RepositoryItemReader<>();
        reader.setRepository(donationRepository);
        reader.setMethodName("findPendingBlockchainRecords");
        reader.setPageSize(pageSize);

        // 쿼리 파라미터 설정 (List 형태로 전달)
        reader.setArguments(java.util.Arrays.asList(campaignId));

        // 정렬 설정 (기부 날짜 오름차순)
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("paidAt", Sort.Direction.ASC);
        reader.setSort(sorts);

        reader.setName("donationItemReader");
        reader.setSaveState(true); // Job 실패 시 재시작 지원

        return reader;
    }

    /**
     * 처리 대상 기부 건수 조회
     *
     * @param campaignId 캠페인 ID
     * @return 미처리 기부 건수
     */
    public long countPendingDonations(Long campaignId) {
        long count = donationRepository.countPendingBlockchainRecords(campaignId);
        log.info("Pending blockchain records for campaign {}: {}", campaignId, count);
        return count;
    }
}
