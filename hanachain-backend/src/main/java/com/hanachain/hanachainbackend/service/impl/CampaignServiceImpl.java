package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignFundraisingStats;
import com.hanachain.hanachainbackend.dto.campaign.CampaignImageUploadResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignListResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignUpdateRequest;
import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignManagerRepository;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.repository.OrganizationRepository;
import com.hanachain.hanachainbackend.service.CampaignService;
import com.hanachain.hanachainbackend.service.UserService;
import com.hanachain.hanachainbackend.service.blockchain.BlockchainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CampaignServiceImpl implements CampaignService {
    
    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final CampaignManagerRepository campaignManagerRepository;
    private final UserService userService;
    private final BlockchainService blockchainService;
    private final OrganizationRepository organizationRepository;
    
    @Override
    public Campaign createCampaign(Long userId, CampaignCreateRequest request) {
        User user = userService.findById(userId);
        
        Campaign campaign = Campaign.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .organizer(request.getOrganizer())
                .targetAmount(request.getTargetAmount())
                .imageUrl(request.getImageUrl())
                .status(Campaign.CampaignStatus.ACTIVE)
                .category(request.getCategory())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .user(user)
                .build();
        
        Campaign savedCampaign = campaignRepository.save(campaign);
        log.info("Campaign created: {} by user: {}", savedCampaign.getId(), userId);
        
        // DRAFT 상태에서도 enableBlockchain이 true이면 즉시 블록체인 등록
        if (request.isEnableBlockchain()) {
            log.info("📢 [DRAFT → BLOCKCHAIN] 캠페인 생성 시 즉시 블록체인 등록 시작 - campaignId: {}", savedCampaign.getId());
            
            // 수혜자 주소 설정 (없으면 기본 주소 사용)
            String beneficiaryAddress = StringUtils.hasText(request.getBeneficiaryAddress()) 
                ? request.getBeneficiaryAddress() 
                : "0x0000000000000000000000000000000000000000"; // 기본 주소
            
            savedCampaign.setBeneficiaryAddress(beneficiaryAddress);
            savedCampaign = campaignRepository.saveAndFlush(savedCampaign); // saveAndFlush로 즉시 DB 커밋
            
            // 즉시 블록체인 등록 시작
            initiateBlockchainRegistration(savedCampaign);
        }
        
        return savedCampaign;
    }
    
    @Override
    public Campaign updateCampaign(Long campaignId, Long userId, CampaignUpdateRequest request) {
        Campaign campaign = findCampaignById(campaignId);
        
        // 권한 확인
        if (!campaign.getUser().getId().equals(userId)) {
            throw new RuntimeException("캠페인을 수정할 권한이 없습니다.");
        }
        
        // 활성 상태인 캠페인은 일부 필드만 수정 가능
        if (campaign.getStatus() == Campaign.CampaignStatus.ACTIVE) {
            if (request.getTitle() != null) {
                campaign.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                campaign.setDescription(request.getDescription());
            }
            if (request.getImageUrl() != null) {
                campaign.setImageUrl(request.getImageUrl());
            }
        } else {
            // 초안 상태인 캠페인은 모든 필드 수정 가능
            updateCampaignFields(campaign, request);
        }
        
        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("Campaign updated: {} by user: {}", campaignId, userId);
        
        return updatedCampaign;
    }
    
    private void updateCampaignFields(Campaign campaign, CampaignUpdateRequest request) {
        if (request.getTitle() != null) {
            campaign.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription());
        }
        if (request.getTargetAmount() != null) {
            campaign.setTargetAmount(request.getTargetAmount());
        }
        if (request.getImageUrl() != null) {
            campaign.setImageUrl(request.getImageUrl());
        }
        if (request.getCategory() != null) {
            campaign.setCategory(request.getCategory());
        }
        if (request.getStartDate() != null) {
            campaign.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            campaign.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            campaign.setStatus(request.getStatus());
        }
    }
    
    @Override
    public void deleteCampaign(Long campaignId, Long userId) {
        Campaign campaign = findCampaignById(campaignId);
        
        // 권한 확인
        if (!campaign.getUser().getId().equals(userId)) {
            throw new RuntimeException("캠페인을 삭제할 권한이 없습니다.");
        }
        
        // 활성 캠페인은 삭제 불가
        if (campaign.getStatus() == Campaign.CampaignStatus.ACTIVE) {
            throw new RuntimeException("활성 상태인 캠페인은 삭제할 수 없습니다.");
        }
        
        campaignRepository.delete(campaign);
        log.info("Campaign deleted: {} by user: {}", campaignId, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CampaignDetailResponse getCampaignDetail(Long campaignId) {
        Campaign campaign = findCampaignById(campaignId);
        return CampaignDetailResponse.fromEntity(campaign);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getPublicCampaigns(Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findActiveCampaigns(LocalDateTime.now(), pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getCampaignsWithFilters(
            Campaign.CampaignCategory category, 
            Campaign.CampaignStatus status, 
            String keyword, 
            String sort, 
            Pageable pageable) {
        
        // 키워드가 빈 문자열인 경우 null로 처리
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        
        // 정렬 기준 검증 및 기본값 설정
        if (sort == null || (!sort.equals("popular") && !sort.equals("progress") && !sort.equals("recent"))) {
            sort = "recent";
        }
        
        Page<Campaign> campaigns = campaignRepository.findWithFilters(category, status, keyword, sort, pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getCampaignsByCategory(Campaign.CampaignCategory category, Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findByCategoryAndActive(category, pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getUserCampaigns(Long userId, Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findByUserId(userId, pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> searchCampaigns(String keyword, Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findByKeyword(keyword, pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getPopularCampaigns(Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findTopFundedCampaigns(pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getRecentCampaigns(Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findRecentActiveCampaigns(pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    public Campaign updateCampaignStatus(Long campaignId, Campaign.CampaignStatus status) {
        Campaign campaign = findCampaignById(campaignId);
        Campaign.CampaignStatus previousStatus = campaign.getStatus();
        campaign.setStatus(status);
        
        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("Campaign status updated: {} from {} to {}", campaignId, previousStatus, status);
        
        // 캠페인이 ACTIVE 상태가 되면 블록체인에 등록 시작
        if (status == Campaign.CampaignStatus.ACTIVE && previousStatus != Campaign.CampaignStatus.ACTIVE) {
            initiateBlockchainRegistration(updatedCampaign);
        }
        
        return updatedCampaign;
    }
    
    @Override
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void completeExpiredCampaigns() {
        try {
            List<Campaign> expiredCampaigns = campaignRepository.findExpiredCampaigns(LocalDateTime.now());
            
            for (Campaign campaign : expiredCampaigns) {
                campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
                campaignRepository.save(campaign);
                log.info("Campaign {} marked as completed due to expiration", campaign.getId());
            }
            
            if (!expiredCampaigns.isEmpty()) {
                log.info("Completed {} expired campaigns", expiredCampaigns.size());
            }
        } catch (Exception e) {
            log.error("Failed to complete expired campaigns", e);
        }
    }
    
    @Override
    public CampaignImageUploadResponse uploadCampaignImage(MultipartFile image) {
        log.info("캠페인 이미지 업로드 시작 - 파일명: {}, 크기: {}", image.getOriginalFilename(), image.getSize());
        
        try {
            // 파일 검증
            validateImageFile(image);
            
            // Base64 인코딩
            byte[] imageBytes = image.getBytes();
            String base64EncodedImage = Base64.getEncoder().encodeToString(imageBytes);
            
            // 데이터 URL 형식으로 생성 (data:image/png;base64,...)
            String contentType = image.getContentType();
            String dataUrl = "data:" + contentType + ";base64," + base64EncodedImage;
            
            log.info("캠페인 이미지 Base64 인코딩 완료 - 파일명: {}, 인코딩 크기: {} 문자", 
                    image.getOriginalFilename(), dataUrl.length());
            
            return CampaignImageUploadResponse.success(dataUrl, image.getOriginalFilename(), image.getSize());
            
        } catch (IOException e) {
            log.error("캠페인 이미지 업로드 실패 - 파일명: {}", image.getOriginalFilename(), e);
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("이미지 파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("이미지 파일만 업로드 가능합니다.");
        }

        // 파일 크기 제한 (5MB)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("이미지 파일 크기는 5MB 이하여야 합니다.");
        }
    }
    
    private Campaign findCampaignById(Long campaignId) {
        return campaignRepository.findByIdAndNotDeleted(campaignId)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다: " + campaignId));
    }
    
    private Campaign findCampaignByIdForAdmin(Long campaignId) {
        return campaignRepository.findByIdForAdmin(campaignId)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다: " + campaignId));
    }
    
    // ===== 관리자 전용 메서드 구현 =====
    
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getAdminCampaigns(
            Campaign.CampaignCategory category,
            Campaign.CampaignStatus status,
            String keyword,
            Pageable pageable) {
        
        log.info("관리자 캠페인 목록 조회 - category: {}, status: {}, keyword: {}", category, status, keyword);
        
        Page<Campaign> campaigns = campaignRepository.findAllWithFiltersForAdmin(category, status, keyword, pageable);
        return campaigns.map(CampaignListResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CampaignDetailResponse getAdminCampaignDetail(Long campaignId) {
        log.info("관리자 캠페인 상세 조회 - campaignId: {}", campaignId);
        
        Campaign campaign = findCampaignByIdForAdmin(campaignId);
        return CampaignDetailResponse.fromEntity(campaign);
    }
    
    @Override
    public CampaignDetailResponse createCampaign(CampaignCreateRequest createDto, Long adminUserId) {
        log.info("관리자 캠페인 생성 (블록체인 통합) - adminUserId: {}, title: {}", adminUserId, createDto.getTitle());

        User admin = userService.findById(adminUserId);

        // 조직 ID가 제공된 경우 조직의 블록체인 지갑 주소 자동 매핑
        String beneficiaryAddress = createDto.getBeneficiaryAddress();
        if (createDto.getOrganizationId() != null) {
            log.info("조직 ID {} 로부터 지갑 주소 자동 매핑 시도", createDto.getOrganizationId());

            Organization organization = organizationRepository.findById(createDto.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("조직을 찾을 수 없습니다: " + createDto.getOrganizationId()));

            if (organization.hasWallet()) {
                beneficiaryAddress = organization.getWalletAddress();
                log.info("조직 {} 의 지갑 주소 자동 매핑 완료: {}", organization.getName(), beneficiaryAddress);
            } else {
                log.warn("조직 {} 에 지갑이 없습니다. 수동 주소 사용", organization.getName());
            }
        }

        // 기본 캠페인 생성
        Campaign campaign = Campaign.builder()
                .title(createDto.getTitle())
                .subtitle(createDto.getSubtitle())
                .description(createDto.getDescription())
                .organizer(createDto.getOrganizer())
                .targetAmount(createDto.getTargetAmount())
                .imageUrl(createDto.getImageUrl())
                .status(Campaign.CampaignStatus.ACTIVE)
                .category(createDto.getCategory())
                .startDate(createDto.getStartDate())
                .endDate(createDto.getEndDate())
                .user(admin) // 관리자를 생성자로 설정
                .build();

        Campaign savedCampaign = campaignRepository.save(campaign);
        log.info("관리자 캠페인 DB 저장 완료 - campaignId: {}", savedCampaign.getId());

        // Admin 캠페인은 자동으로 블록체인 등록 시도 (자동 매핑된 주소 또는 수동 주소 사용)
        if (StringUtils.hasText(beneficiaryAddress)) {
            log.info("관리자 캠페인 블록체인 등록 시작 - campaignId: {}, beneficiary: {}",
                savedCampaign.getId(), beneficiaryAddress);

            // 수혜자 주소 설정
            savedCampaign.setBeneficiaryAddress(beneficiaryAddress);
            savedCampaign = campaignRepository.save(savedCampaign);

            // 블록체인 등록 시도 (비동기)
            try {
                initiateBlockchainRegistrationWithRetry(savedCampaign, 3);
                log.info("관리자 캠페인 {} 블록체인 등록 요청 완료", savedCampaign.getId());
            } catch (Exception e) {
                log.error("관리자 캠페인 {} 블록체인 등록 실패", savedCampaign.getId(), e);
                savedCampaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED,
                    "블록체인 등록 실패: " + e.getMessage());
                savedCampaign = campaignRepository.save(savedCampaign);
            }
        } else {
            log.warn("관리자 캠페인 {} - 수혜자 주소가 없어 블록체인 등록 건너뜀", savedCampaign.getId());
            savedCampaign.updateBlockchainStatus(BlockchainStatus.NONE,
                "수혜자 주소가 설정되지 않음");
            savedCampaign = campaignRepository.save(savedCampaign);
        }

        return CampaignDetailResponse.fromEntity(savedCampaign);
    }
    
    @Override
    public CampaignDetailResponse updateCampaignAsAdmin(Long campaignId, CampaignUpdateRequest updateDto, Long adminUserId) {
        log.info("관리자 캠페인 수정 - campaignId: {}, adminUserId: {}", campaignId, adminUserId);
        
        Campaign campaign = findCampaignByIdForAdmin(campaignId);
        
        // 관리자는 모든 필드 수정 가능 (상태 관계없이)
        updateCampaignFields(campaign, updateDto);
        
        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("관리자 캠페인 수정 완료 - campaignId: {}", campaignId);
        
        return CampaignDetailResponse.fromEntity(updatedCampaign);
    }
    
    @Override
    public void softDeleteCampaignAsAdmin(Long campaignId, Long adminUserId) {
        log.info("관리자 캠페인 소프트 삭제 - campaignId: {}, adminUserId: {}", campaignId, adminUserId);
        
        Campaign campaign = findCampaignByIdForAdmin(campaignId);
        
        if (campaign.isDeleted()) {
            throw new RuntimeException("이미 삭제된 캠페인입니다: " + campaignId);
        }
        
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = campaignRepository.softDeleteById(campaignId, now, now);
        
        if (updatedRows == 0) {
            throw new RuntimeException("캠페인 삭제에 실패했습니다: " + campaignId);
        }
        
        log.info("관리자 캠페인 소프트 삭제 완료 - campaignId: {}", campaignId);
    }
    
    @Override
    public CampaignDetailResponse restoreCampaignAsAdmin(Long campaignId, Long adminUserId) {
        log.info("관리자 캠페인 복구 - campaignId: {}, adminUserId: {}", campaignId, adminUserId);
        
        Campaign campaign = findCampaignByIdForAdmin(campaignId);
        
        if (!campaign.isDeleted()) {
            throw new RuntimeException("삭제되지 않은 캠페인입니다: " + campaignId);
        }
        
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = campaignRepository.restoreById(campaignId, now);
        
        if (updatedRows == 0) {
            throw new RuntimeException("캠페인 복구에 실패했습니다: " + campaignId);
        }
        
        // 복구된 캠페인 정보 다시 조회
        Campaign restoredCampaign = findCampaignByIdForAdmin(campaignId);
        log.info("관리자 캠페인 복구 완료 - campaignId: {}", campaignId);
        
        return CampaignDetailResponse.fromEntity(restoredCampaign);
    }
    
    @Override
    public CampaignDetailResponse updateCampaignStatusAsAdmin(Long campaignId, Campaign.CampaignStatus status, Long adminUserId) {
        log.info("관리자 캠페인 상태 변경 - campaignId: {}, newStatus: {}, adminUserId: {}", 
                campaignId, status, adminUserId);
        
        Campaign campaign = findCampaignByIdForAdmin(campaignId);
        campaign.setStatus(status);
        
        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("관리자 캠페인 상태 변경 완료 - campaignId: {}, status: {}", campaignId, status);
        
        return CampaignDetailResponse.fromEntity(updatedCampaign);
    }
    
    @Override
    @Transactional
    public CampaignDetailResponse updateBeneficiaryAddress(Long campaignId, String address, Long adminUserId) {
        log.info("관리자 수혜자 주소 업데이트 - campaignId: {}, address: {}, adminUserId: {}", 
                campaignId, address, adminUserId);
        
        // 주소 형식 검증
        if (address == null || !address.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IllegalArgumentException("올바른 이더리움 주소 형식이 아닙니다.");
        }
        
        Campaign campaign = findCampaignByIdForAdmin(campaignId);
        
        // 블록체인 상태 확인 - 이미 배포된 경우 수정 제한
        if (campaign.getBlockchainStatus() == BlockchainStatus.ACTIVE) {
            throw new IllegalStateException("이미 블록체인에 배포된 캠페인의 수혜자 주소는 수정할 수 없습니다.");
        }
        
        // 수혜자 주소 업데이트
        campaign.setBeneficiaryAddress(address);
        
        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("관리자 수혜자 주소 업데이트 완료 - campaignId: {}, address: {}", campaignId, address);
        
        return CampaignDetailResponse.fromEntity(updatedCampaign);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampaignListResponse> getDeletedCampaigns(Pageable pageable) {
        log.info("삭제된 캠페인 목록 조회");
        
        Page<Campaign> deletedCampaigns = campaignRepository.findDeletedCampaigns(pageable);
        return deletedCampaigns.map(CampaignListResponse::fromEntity);
    }
    
    // ===== 블록체인 통합 메서드 =====
    
    /**
     * 캠페인의 블록체인 등록을 시작합니다
     */
    private void initiateBlockchainRegistration(Campaign campaign) {
        try {
            log.info("블록체인 캠페인 등록 시작 - campaignId: {}", campaign.getId());
            
            // 이미 블록체인 처리가 진행 중이거나 완료된 경우 스킵
            if (campaign.isBlockchainProcessing() || campaign.isBlockchainActive()) {
                log.info("캠페인 {} 블록체인 처리 이미 진행됨 - 상태: {}", 
                        campaign.getId(), campaign.getBlockchainStatus());
                return;
            }
            
            // 필수 정보 검증
            if (!StringUtils.hasText(campaign.getBeneficiaryAddress())) {
                log.warn("캠페인 {} 수혜자 주소 미설정 - 블록체인 등록 건너뜀", campaign.getId());
                campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                    "수혜자 블록체인 주소가 설정되지 않았습니다");
                campaignRepository.save(campaign);
                return;
            }
            
            // 블록체인 상태를 대기 중으로 설정
            campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_PENDING, null);
            campaignRepository.save(campaign);
            
            // 캠페인 기간 계산 (초 단위)
            BigInteger duration = BigInteger.valueOf(
                ChronoUnit.SECONDS.between(campaign.getStartDate(), campaign.getEndDate())
            );
            
            // 목표 금액 (USDC는 6 decimals이므로 변환)
            BigInteger goalAmount = campaign.getTargetAmount()
                .multiply(new BigDecimal("1000000")) // 10^6 for 6 decimals
                .toBigInteger();
            
            // 비동기 블록체인 캠페인 생성 요청
            blockchainService.createCampaignAsync(
                campaign.getId(),
                campaign.getBeneficiaryAddress(),
                goalAmount,
                duration,
                campaign.getTitle(),
                campaign.getDescription()
            ).whenComplete((transactionHash, throwable) -> {
                handleBlockchainCampaignResult(campaign.getId(), transactionHash, throwable);
            });
            
            log.info("블록체인 캠페인 생성 요청 완료 - campaignId: {}", campaign.getId());
            
        } catch (Exception e) {
            log.error("블록체인 캠페인 등록 실패 - campaignId: " + campaign.getId(), e);
            campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                "블록체인 등록 중 예상치 못한 오류 발생: " + e.getMessage());
            campaignRepository.save(campaign);
        }
    }
    
    /**
     * 블록체인 캠페인 생성 결과를 처리합니다 (향상된 오류 처리 포함)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBlockchainCampaignResult(Long campaignId, String transactionHash, Throwable error) {
        log.info("🔄 [BLOCKCHAIN CALLBACK] 캠페인 생성 결과 처리 시작 - campaignId: {}, txHash: {}, hasError: {}", 
                campaignId, transactionHash, error != null);
        
        try {
            // 새로운 트랜잭션에서 캠페인 조회 (REQUIRES_NEW로 격리 문제 해결)
            Campaign campaign = campaignRepository.findById(campaignId)
                .orElseGet(() -> {
                    log.warn("⚠️ [BLOCKCHAIN CALLBACK] 캠페인 조회 실패, 재시도 - campaignId: {}", campaignId);
                    // 약간의 지연 후 재시도
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return campaignRepository.findById(campaignId).orElse(null);
                });
            
            if (campaign == null) {
                log.error("❌ [BLOCKCHAIN CALLBACK] 캠페인을 찾을 수 없습니다 - campaignId: {}", campaignId);
                return;
            }
            
            log.info("📋 [BLOCKCHAIN CALLBACK] 캠페인 조회 완료 - campaignId: {}, 현재상태: {}, 현재해시: {}", 
                    campaignId, campaign.getBlockchainStatus(), campaign.getBlockchainTransactionHash());
            
            if (error != null) {
                log.error("❌ [BLOCKCHAIN CALLBACK] 블록체인 캠페인 생성 실패 - campaignId: " + campaignId, error);
                
                // 오류 유형별 분류 및 처리
                String errorMessage = classifyAndHandleBlockchainError(error);
                campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, errorMessage);
                
                // 특정 오류의 경우 자동 재시도 스케줄링
                scheduleRetryIfApplicable(campaign, error);
                
            } else if (StringUtils.hasText(transactionHash)) {
                log.info("✅ [BLOCKCHAIN CALLBACK] 블록체인 캠페인 생성 성공 - campaignId: {}, txHash: {}", 
                        campaignId, transactionHash);
                
                // 트랜잭션 해시 저장 및 처리 중 상태로 변경
                log.info("🔗 [BLOCKCHAIN CALLBACK] 트랜잭션 해시 저장 시작 - campaignId: {}, txHash: {}", 
                        campaignId, transactionHash);
                campaign.setBlockchainTransactionHash(transactionHash);
                campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_PROCESSING, 
                    "트랜잭션 확인 대기 중");
                
                log.info("✅ [BLOCKCHAIN CALLBACK] 트랜잭션 해시 저장 완료 - campaignId: {}, txHash: {}", 
                        campaignId, transactionHash);
                
                // 트랜잭션 확인을 위한 비동기 모니터링 시작
                scheduleTransactionMonitoring(campaign, transactionHash);
                
            } else {
                log.warn("⚠️ [BLOCKCHAIN CALLBACK] 트랜잭션 해시 누락 - campaignId: {}", campaignId);
                campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                    "트랜잭션 해시를 받지 못했습니다");
            }
            
            campaignRepository.save(campaign);
            log.info("💾 [BLOCKCHAIN CALLBACK] 캠페인 상태 저장 완료 - campaignId: {}", campaignId);
            
        } catch (Exception e) {
            log.error("블록체인 캠페인 결과 처리 중 오류 - campaignId: " + campaignId, e);
            
            // 결과 처리 실패 시에도 캠페인 상태 업데이트 시도
            try {
                Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
                if (campaign != null) {
                    campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                        "결과 처리 중 오류: " + e.getMessage());
                    campaignRepository.save(campaign);
                }
            } catch (Exception saveError) {
                log.error("캠페인 상태 업데이트 중 추가 오류 - campaignId: " + campaignId, saveError);
            }
        }
    }
    
    /**
     * 블록체인 오류를 분류하고 적절한 메시지를 반환합니다
     */
    private String classifyAndHandleBlockchainError(Throwable error) {
        String errorMessage = error.getMessage();
        String lowerCaseMessage = errorMessage != null ? errorMessage.toLowerCase() : "";
        
        // 네트워크 관련 오류
        if (lowerCaseMessage.contains("connection") || 
            lowerCaseMessage.contains("timeout") || 
            lowerCaseMessage.contains("network")) {
            return "네트워크 연결 오류: " + errorMessage;
        }
        
        // 가스 관련 오류
        if (lowerCaseMessage.contains("gas") || 
            lowerCaseMessage.contains("out of gas")) {
            return "가스 부족 오류: " + errorMessage;
        }
        
        // 잔액 부족 오류
        if (lowerCaseMessage.contains("insufficient") || 
            lowerCaseMessage.contains("balance")) {
            return "잔액 부족 오류: " + errorMessage;
        }
        
        // 스마트 컨트랙트 오류
        if (lowerCaseMessage.contains("revert") || 
            lowerCaseMessage.contains("execution failed")) {
            return "스마트 컨트랙트 실행 오류: " + errorMessage;
        }
        
        // 주소 관련 오류
        if (lowerCaseMessage.contains("address") || 
            lowerCaseMessage.contains("invalid")) {
            return "주소 오류: " + errorMessage;
        }
        
        // 기타 오류
        return "블록체인 오류: " + (errorMessage != null ? errorMessage : "알 수 없는 오류");
    }
    
    /**
     * 특정 오류에 대해 자동 재시도 스케줄링
     */
    private void scheduleRetryIfApplicable(Campaign campaign, Throwable error) {
        String errorMessage = error.getMessage();
        if (errorMessage == null) return;
        
        String lowerCaseMessage = errorMessage.toLowerCase();
        
        // 네트워크 또는 일시적 오류의 경우 재시도 스케줄링
        if (lowerCaseMessage.contains("connection") || 
            lowerCaseMessage.contains("timeout") || 
            lowerCaseMessage.contains("network") ||
            lowerCaseMessage.contains("temporary")) {
            
            log.info("캠페인 {} 일시적 오류 감지 - 30초 후 자동 재시도 스케줄링", campaign.getId());
            
            // 30초 후 재시도 (실제 환경에서는 스케줄러 사용)
            CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS)
                .execute(() -> {
                    try {
                        Campaign retryTarget = findCampaignById(campaign.getId());
                        if (retryTarget.isBlockchainFailed()) {
                            log.info("캠페인 {} 자동 재시도 실행", campaign.getId());
                            initiateBlockchainRegistration(retryTarget);
                        }
                    } catch (Exception retryError) {
                        log.error("캠페인 {} 자동 재시도 실패", campaign.getId(), retryError);
                    }
                });
        }
    }
    
    /**
     * 트랜잭션 확인을 위한 모니터링 스케줄링
     */
    private void scheduleTransactionMonitoring(Campaign campaign, String transactionHash) {
        log.info("캠페인 {} 트랜잭션 {} 모니터링 시작", campaign.getId(), transactionHash);
        
        // 트랜잭션 확인을 위한 비동기 작업
        blockchainService.waitForTransactionAsync(transactionHash, 300) // 5분 타임아웃
            .thenAccept(receipt -> {
                try {
                    // 비동기 콜백에서는 기본 findById 사용
                    Campaign updatedCampaign = campaignRepository.findById(campaign.getId())
                        .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다: " + campaign.getId()));
                    
                    if (receipt.isStatusOK()) {
                        log.info("캠페인 {} 트랜잭션 확인 성공", campaign.getId());
                        
                        // TODO: 트랜잭션 로그에서 블록체인 캠페인 ID 추출
                        // 현재는 임시로 ACTIVE 상태로 설정
                        updatedCampaign.updateBlockchainStatus(BlockchainStatus.ACTIVE, null);
                        
                    } else {
                        log.error("캠페인 {} 트랜잭션 실행 실패", campaign.getId());
                        updatedCampaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                            "트랜잭션 실행 실패");
                    }
                    
                    campaignRepository.save(updatedCampaign);
                    
                } catch (Exception e) {
                    log.error("캠페인 {} 트랜잭션 확인 후 처리 오류", campaign.getId(), e);
                }
            })
            .exceptionally(throwable -> {
                log.error("캠페인 {} 트랜잭션 확인 실패", campaign.getId(), throwable);
                
                try {
                    // 비동기 콜백에서는 기본 findById 사용
                    Campaign failedCampaign = campaignRepository.findById(campaign.getId())
                        .orElse(null);
                    if (failedCampaign != null) {
                        failedCampaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                            "트랜잭션 확인 실패: " + throwable.getMessage());
                        campaignRepository.save(failedCampaign);
                    }
                } catch (Exception saveError) {
                    log.error("캠페인 {} 실패 상태 저장 오류", campaign.getId(), saveError);
                }
                
                return null;
            });
    }
    
    /**
     * 블록체인 상태가 처리 중인 캠페인들을 모니터링하고 상태를 업데이트합니다 (향상된 동기화 포함)
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void monitorBlockchainCampaigns() {
        try {
            // 처리 중인 캠페인들 모니터링
            List<Campaign> processingCampaigns = campaignRepository.findByBlockchainStatus(
                BlockchainStatus.BLOCKCHAIN_PROCESSING);
            
            for (Campaign campaign : processingCampaigns) {
                if (StringUtils.hasText(campaign.getBlockchainTransactionHash())) {
                    // 트랜잭션 상태 확인 및 업데이트
                    updateCampaignBlockchainStatus(campaign);
                }
            }
            
            // 활성 상태 캠페인들의 데이터 동기화 (1시간에 한번씩)
            if (shouldRunFullSync()) {
                synchronizeActiveCampaigns();
            }
            
            if (!processingCampaigns.isEmpty()) {
                log.info("블록체인 캠페인 상태 모니터링 완료 - 처리 중인 캠페인: {}", processingCampaigns.size());
            }
            
        } catch (Exception e) {
            log.error("블록체인 캠페인 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * 활성 상태 캠페인들과 블록체인 상태를 동기화합니다
     */
    @Transactional
    public void synchronizeActiveCampaigns() {
        try {
            log.info("활성 캠페인 블록체인 동기화 시작");
            
            // 블록체인에 등록된 모든 활성 캠페인 조회
            List<Campaign> activeCampaigns = campaignRepository.findByBlockchainStatus(BlockchainStatus.ACTIVE);
            
            int syncCount = 0;
            for (Campaign campaign : activeCampaigns) {
                if (campaign.getBlockchainCampaignId() != null) {
                    try {
                        syncCampaignWithBlockchain(campaign);
                        syncCount++;
                    } catch (Exception e) {
                        log.error("캠페인 {} 동기화 실패", campaign.getId(), e);
                    }
                }
            }
            
            log.info("활성 캠페인 블록체인 동기화 완료 - 동기화된 캠페인: {}/{}", 
                    syncCount, activeCampaigns.size());
            
        } catch (Exception e) {
            log.error("활성 캠페인 동기화 중 오류 발생", e);
        }
    }
    
    /**
     * 개별 캠페인을 블록체인과 동기화합니다
     */
    private void syncCampaignWithBlockchain(Campaign campaign) {
        try {
            // 블록체인에서 최신 정보 조회
            BlockchainService.CampaignInfo blockchainInfo = blockchainService.getCampaignFromBlockchain(
                campaign.getBlockchainCampaignId());
            
            if (!blockchainInfo.isExists()) {
                log.warn("블록체인에서 캠페인 {} 정보를 찾을 수 없음", campaign.getId());
                return;
            }
            
            boolean updated = false;
            
            // 1. 모금액 동기화
            BigDecimal blockchainRaised = convertFromBlockchainAmount(blockchainInfo.getTotalRaised());
            if (campaign.getCurrentAmount().compareTo(blockchainRaised) != 0) {
                
                // 차이가 임계값(1 USDC) 이상인 경우에만 업데이트
                BigDecimal difference = blockchainRaised.subtract(campaign.getCurrentAmount()).abs();
                if (difference.compareTo(BigDecimal.ONE) >= 0) {
                    
                    log.info("캠페인 {} 모금액 동기화: {} -> {}", 
                            campaign.getId(), campaign.getCurrentAmount(), blockchainRaised);
                    
                    campaign.setCurrentAmount(blockchainRaised);
                    updated = true;
                    
                    // 기부자 수 재계산 (실제 구현에서는 기부 내역 조회)
                    // TODO: 블록체인에서 기부자 수 조회하여 동기화
                }
            }
            
            // 2. 완료 상태 동기화
            if (blockchainInfo.isFinalized() && 
                campaign.getStatus() != Campaign.CampaignStatus.COMPLETED) {
                log.info("캠페인 {} 상태 동기화: 완료로 변경", campaign.getId());
                campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
                updated = true;
            }
            
            // 3. 목표 금액 검증 (블록체인과 데이터베이스 불일치 확인)
            BigDecimal blockchainGoal = convertFromBlockchainAmount(blockchainInfo.getGoalAmount());
            if (campaign.getTargetAmount().compareTo(blockchainGoal) != 0) {
                log.warn("캠페인 {} 목표 금액 불일치 - DB: {}, 블록체인: {}", 
                        campaign.getId(), campaign.getTargetAmount(), blockchainGoal);
            }
            
            // 4. 마감일 검증
            LocalDateTime blockchainDeadline = LocalDateTime.ofEpochSecond(
                blockchainInfo.getDeadline().longValue(), 0, java.time.ZoneOffset.UTC);
            if (!campaign.getEndDate().isEqual(blockchainDeadline)) {
                log.warn("캠페인 {} 마감일 불일치 - DB: {}, 블록체인: {}", 
                        campaign.getId(), campaign.getEndDate(), blockchainDeadline);
            }
            
            if (updated) {
                campaignRepository.save(campaign);
                log.debug("캠페인 {} 블록체인 동기화 완료", campaign.getId());
            }
            
        } catch (Exception e) {
            log.error("캠페인 {} 블록체인 동기화 실패", campaign.getId(), e);
        }
    }
    
    /**
     * 블록체인 금액(6 decimals)을 일반 decimal로 변환
     */
    private BigDecimal convertFromBlockchainAmount(BigInteger blockchainAmount) {
        return new BigDecimal(blockchainAmount)
            .divide(new BigDecimal("1000000"), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * 일반 decimal을 블록체인 금액(6 decimals)으로 변환
     */
    private BigInteger convertToBlockchainAmount(BigDecimal amount) {
        return amount.multiply(new BigDecimal("1000000")).toBigInteger();
    }
    
    /**
     * 전체 동기화 실행 여부 결정 (마지막 실행으로부터 1시간 경과 여부)
     */
    private static LocalDateTime lastFullSyncTime = LocalDateTime.now().minusHours(2);
    
    private boolean shouldRunFullSync() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(lastFullSyncTime.plusHours(1))) {
            lastFullSyncTime = now;
            return true;
        }
        return false;
    }
    
    /**
     * 개별 캠페인의 블록체인 상태를 업데이트합니다
     */
    private void updateCampaignBlockchainStatus(Campaign campaign) {
        try {
            // 블록체인에서 트랜잭션 상태 조회
            BlockchainService.TransactionStatus txStatus = blockchainService.getTransactionStatus(
                campaign.getBlockchainTransactionHash());
            
            if (txStatus.isConfirmed()) {
                if (txStatus.isSuccessful()) {
                    // 성공: 블록체인 캠페인 정보 조회하여 캠페인 ID 저장
                    // TODO: 실제 구현에서는 트랜잭션 로그에서 캠페인 ID 추출
                    campaign.updateBlockchainStatus(BlockchainStatus.ACTIVE, null);
                    log.info("캠페인 {} 블록체인 등록 완료", campaign.getId());
                } else {
                    // 실패: 실패 상태로 업데이트
                    String errorMsg = StringUtils.hasText(txStatus.getErrorMessage()) 
                        ? txStatus.getErrorMessage() 
                        : "블록체인 트랜잭션 실행 실패";
                    campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, errorMsg);
                    log.warn("캠페인 {} 블록체인 등록 실패: {}", campaign.getId(), errorMsg);
                }
                
                campaignRepository.save(campaign);
            }
            
        } catch (Exception e) {
            log.error("캠페인 {} 블록체인 상태 업데이트 중 오류", campaign.getId(), e);
        }
    }
    
    // ===== 새로운 블록체인 통합 메서드 구현 =====
    
    @Override
    @Transactional
    public Campaign createCampaignWithBlockchain(Long userId, CampaignCreateRequest request, boolean forceBlockchainRegistration) {
        log.info("블록체인 통합 캠페인 생성 시작 - userId: {}, forceBlockchain: {}", userId, forceBlockchainRegistration);
        
        // 기본 캠페인 생성
        Campaign campaign = createCampaign(userId, request);
        
        // 블록체인 등록이 요청되고 수혜자 주소가 있는 경우 즉시 등록 시도
        if (forceBlockchainRegistration && StringUtils.hasText(request.getBeneficiaryAddress())) {
            // 수혜자 주소 설정
            campaign.setBeneficiaryAddress(request.getBeneficiaryAddress());
            campaign = campaignRepository.save(campaign);
            
            // 즉시 블록체인 등록 시도
            try {
                initiateBlockchainRegistrationWithRetry(campaign, 3);
                log.info("캠페인 {} 블록체인 등록 요청 완료", campaign.getId());
            } catch (Exception e) {
                log.error("캠페인 {} 즉시 블록체인 등록 실패", campaign.getId(), e);
                campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                    "즉시 블록체인 등록 실패: " + e.getMessage());
                campaign = campaignRepository.save(campaign);
            }
        }
        
        return campaign;
    }
    
    @Override
    @Transactional(readOnly = true)
    public CampaignDetailResponse getBlockchainStatus(Long campaignId) {
        log.info("캠페인 블록체인 상태 조회 - campaignId: {}", campaignId);
        
        Campaign campaign = findCampaignById(campaignId);
        
        // 블록체인 상태가 ACTIVE이고 블록체인 캠페인 ID가 있는 경우, 최신 정보 조회
        if (campaign.isBlockchainActive() && campaign.getBlockchainCampaignId() != null) {
            try {
                BlockchainService.CampaignInfo blockchainInfo = blockchainService.getCampaignFromBlockchain(
                    campaign.getBlockchainCampaignId());
                
                log.info("블록체인에서 캠페인 정보 조회 성공 - campaignId: {}, blockchainCampaignId: {}", 
                        campaignId, campaign.getBlockchainCampaignId());
                
                // 블록체인 정보를 포함한 상세 응답 생성
                CampaignDetailResponse dto = CampaignDetailResponse.fromEntity(campaign);
                // TODO: 필요시 블록체인에서 조회한 추가 정보를 DTO에 포함
                return dto;
                
            } catch (Exception e) {
                log.error("블록체인에서 캠페인 정보 조회 실패 - campaignId: " + campaignId, e);
                // 블록체인 조회 실패 시에도 데이터베이스 정보 반환
            }
        }
        
        return CampaignDetailResponse.fromEntity(campaign);
    }
    
    @Override
    @Transactional
    public Campaign finalizeCampaignOnBlockchain(Long campaignId, Long userId) {
        log.info("블록체인 캠페인 완료 처리 시작 - campaignId: {}, userId: {}", campaignId, userId);
        
        Campaign campaign = findCampaignById(campaignId);
        
        // 권한 확인 (캠페인 소유자 또는 관리자만)
        if (!campaign.getUser().getId().equals(userId)) {
            throw new RuntimeException("캠페인을 완료할 권한이 없습니다.");
        }
        
        // 캠페인이 블록체인에 등록되어 있는지 확인
        if (!campaign.isBlockchainActive() || campaign.getBlockchainCampaignId() == null) {
            throw new RuntimeException("블록체인에 등록되지 않은 캠페인입니다.");
        }
        
        // 캠페인이 완료 가능한 상태인지 확인
        if (campaign.getStatus() != Campaign.CampaignStatus.ACTIVE) {
            throw new RuntimeException("활성 상태의 캠페인만 완료할 수 있습니다.");
        }
        
        try {
            // 블록체인에서 캠페인 완료 처리 요청
            CompletableFuture<String> finalizeFuture = blockchainService.finalizeCampaignAsync(
                campaign.getBlockchainCampaignId());
            
            // 완료 처리 시작 상태로 변경
            campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_PROCESSING, 
                "캠페인 완료 처리 중");
            campaign = campaignRepository.save(campaign);
            
            // 비동기 완료 처리
            finalizeFuture.whenComplete((transactionHash, throwable) -> {
                handleCampaignFinalizationResult(campaignId, transactionHash, throwable);
            });
            
            log.info("캠페인 {} 블록체인 완료 처리 요청 완료", campaignId);
            return campaign;
            
        } catch (Exception e) {
            log.error("캠페인 {} 블록체인 완료 처리 실패", campaignId, e);
            campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                "캠페인 완료 처리 실패: " + e.getMessage());
            return campaignRepository.save(campaign);
        }
    }
    
    @Override
    @Transactional
    public Campaign retryBlockchainOperation(Long campaignId, Long userId) {
        log.info("블록체인 작업 재시도 - campaignId: {}, userId: {}", campaignId, userId);
        
        Campaign campaign = findCampaignById(campaignId);
        
        // 권한 확인
        if (!campaign.getUser().getId().equals(userId)) {
            throw new RuntimeException("캠페인을 수정할 권한이 없습니다.");
        }
        
        // 실패 상태인 경우만 재시도 가능
        if (!campaign.isBlockchainFailed()) {
            throw new RuntimeException("실패 상태의 캠페인만 재시도할 수 있습니다.");
        }
        
        // 수혜자 주소 확인
        if (!StringUtils.hasText(campaign.getBeneficiaryAddress())) {
            throw new RuntimeException("수혜자 주소가 설정되지 않았습니다.");
        }
        
        try {
            // 재시도 전 상태 초기화
            campaign.setBlockchainTransactionHash(null);
            campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_PENDING, null);
            campaign = campaignRepository.save(campaign);
            
            // 블록체인 등록 재시도 (최대 3회)
            initiateBlockchainRegistrationWithRetry(campaign, 3);
            
            log.info("캠페인 {} 블록체인 작업 재시도 완료", campaignId);
            return campaign;
            
        } catch (Exception e) {
            log.error("캠페인 {} 블록체인 작업 재시도 실패", campaignId, e);
            campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                "재시도 실패: " + e.getMessage());
            return campaignRepository.save(campaign);
        }
    }
    
    @Override
    @Transactional
    public void syncBlockchainStatus(Long campaignId) {
        log.info("블록체인 상태 동기화 시작 - campaignId: {}", campaignId);
        
        Campaign campaign = findCampaignById(campaignId);
        
        // 블록체인에 등록된 캠페인만 동기화
        if (!campaign.isBlockchainActive() || campaign.getBlockchainCampaignId() == null) {
            log.warn("캠페인 {} 블록체인에 등록되지 않음 - 동기화 건너뜀", campaignId);
            return;
        }
        
        try {
            // 블록체인에서 최신 정보 조회
            BlockchainService.CampaignInfo blockchainInfo = blockchainService.getCampaignFromBlockchain(
                campaign.getBlockchainCampaignId());
            
            if (blockchainInfo.isExists()) {
                // 블록체인 정보와 데이터베이스 정보 동기화
                boolean updated = false;
                
                // 모금액 동기화 (USDC 6 decimals를 일반 decimal로 변환)
                BigDecimal blockchainRaised = new BigDecimal(blockchainInfo.getTotalRaised())
                    .divide(new BigDecimal("1000000"), 2, BigDecimal.ROUND_HALF_UP);
                
                if (campaign.getCurrentAmount().compareTo(blockchainRaised) != 0) {
                    campaign.setCurrentAmount(blockchainRaised);
                    updated = true;
                    log.info("캠페인 {} 모금액 동기화: {} -> {}", 
                            campaignId, campaign.getCurrentAmount(), blockchainRaised);
                }
                
                // 완료 상태 동기화
                if (blockchainInfo.isFinalized() && 
                    campaign.getStatus() != Campaign.CampaignStatus.COMPLETED) {
                    campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
                    updated = true;
                    log.info("캠페인 {} 상태 동기화: 완료로 변경", campaignId);
                }
                
                if (updated) {
                    campaignRepository.save(campaign);
                    log.info("캠페인 {} 블록체인 동기화 완료", campaignId);
                }
                
            } else {
                log.warn("블록체인에서 캠페인 {} 정보를 찾을 수 없음", campaignId);
            }
            
        } catch (Exception e) {
            log.error("캠페인 {} 블록체인 동기화 실패", campaignId, e);
        }
    }
    
    /**
     * 재시도 로직을 포함한 블록체인 등록
     */
    private void initiateBlockchainRegistrationWithRetry(Campaign campaign, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("캠페인 {} 블록체인 등록 시도 {}/{}", campaign.getId(), attempt, maxRetries);
                
                // 기존 initiateBlockchainRegistration 메서드 호출
                initiateBlockchainRegistration(campaign);
                
                // 성공 시 리턴
                return;
                
            } catch (Exception e) {
                log.warn("캠페인 {} 블록체인 등록 시도 {}/{} 실패: {}", 
                        campaign.getId(), attempt, maxRetries, e.getMessage());
                
                if (attempt == maxRetries) {
                    // 마지막 시도도 실패한 경우
                    throw new RuntimeException("블록체인 등록 재시도 한계 초과: " + e.getMessage(), e);
                }
                
                // 재시도 전 잠시 대기
                try {
                    TimeUnit.SECONDS.sleep(2 * attempt); // 점진적 대기 시간 증가
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 대기 중 인터럽트", ie);
                }
            }
        }
    }
    
    /**
     * 캠페인 완료 처리 결과를 처리합니다
     */
    private void handleCampaignFinalizationResult(Long campaignId, String transactionHash, Throwable error) {
        try {
            // 비동기 콜백에서는 기본 findById 사용
            Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("캠페인을 찾을 수 없습니다: " + campaignId));
            
            if (error != null) {
                log.error("캠페인 {} 블록체인 완료 처리 실패", campaignId, error);
                campaign.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, 
                    "완료 처리 실패: " + error.getMessage());
            } else {
                log.info("캠페인 {} 블록체인 완료 처리 성공 - txHash: {}", campaignId, transactionHash);
                
                // 완료 처리 성공
                campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
                campaign.updateBlockchainStatus(BlockchainStatus.ACTIVE, null);
                
                // 완료 처리 트랜잭션 해시도 저장 (필요시)
                // campaign.setBlockchainFinalizationTxHash(transactionHash);
            }
            
            campaignRepository.save(campaign);
            
        } catch (Exception e) {
            log.error("캠페인 {} 완료 처리 결과 처리 중 오류", campaignId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignFundraisingStats getCampaignFundraisingStats(Long campaignId, Long userId) {
        // 캠페인 존재 여부 확인
        Campaign campaign = findCampaignById(campaignId);

        // 캠페인 담당자 권한 확인
        boolean isManager = campaignManagerRepository.existsByCampaignIdAndUserIdAndStatus(
            campaignId,
            userId,
            com.hanachain.hanachainbackend.entity.CampaignManager.ManagerStatus.ACTIVE
        );

        if (!isManager) {
            throw new RuntimeException("캠페인 담당자만 모금 통계를 조회할 수 있습니다.");
        }

        // 기본 통계 계산
        BigDecimal currentAmountDecimal = campaign.getCurrentAmount() != null ? campaign.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal targetAmountDecimal = campaign.getTargetAmount();

        Long currentAmount = currentAmountDecimal.longValue();
        Long targetAmount = targetAmountDecimal.longValue();
        Double progressPercentage = targetAmount > 0 ? (currentAmount.doubleValue() / targetAmount.doubleValue() * 100.0) : 0.0;

        // 기부자 수 조회
        Integer donorCount = donationRepository.countDistinctUserIdByCampaignId(campaignId);

        // 평균 기부 금액 계산
        Long averageDonationAmount = donorCount > 0 ? currentAmount / donorCount : 0L;

        // 남은 일수 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = campaign.getEndDate();
        Integer daysLeft = endDate.isAfter(now) ?
            (int) ChronoUnit.DAYS.between(now, endDate) : 0;

        // 최근 7일 일별 기부 추이 조회
        List<CampaignFundraisingStats.DailyDonationTrend> dailyTrend = getDailyDonationTrend(campaignId);

        // 상위 기부 목록 조회 (Top 5)
        List<CampaignFundraisingStats.TopDonation> topDonations = getTopDonations(campaignId, 5);

        return CampaignFundraisingStats.builder()
            .currentAmount(currentAmount)
            .targetAmount(targetAmount)
            .progressPercentage(progressPercentage)
            .donorCount(donorCount)
            .daysLeft(daysLeft)
            .startDate(campaign.getStartDate())
            .endDate(campaign.getEndDate())
            .averageDonationAmount(averageDonationAmount)
            .dailyDonationTrend(dailyTrend)
            .topDonations(topDonations)
            .build();
    }

    /**
     * 최근 7일간 일별 기부 추이 조회
     */
    private List<CampaignFundraisingStats.DailyDonationTrend> getDailyDonationTrend(Long campaignId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 최근 7일

        // Repository에서 일별 통계 조회 (JPQL 또는 Native Query 사용)
        List<Object[]> results = donationRepository.findDailyDonationStats(
            campaignId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        // 날짜별 Map 생성 (0으로 초기화)
        Map<LocalDate, CampaignFundraisingStats.DailyDonationTrend> trendMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendMap.put(date, CampaignFundraisingStats.DailyDonationTrend.builder()
                .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .amount(0L)
                .count(0)
                .build());
        }

        // 실제 데이터로 업데이트
        for (Object[] row : results) {
            // TO_CHAR 함수는 String을 반환하므로 String으로 캐스팅
            String dateStr = (String) row[0];
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            Long amount = ((Number) row[1]).longValue();
            Integer count = ((Number) row[2]).intValue();

            trendMap.put(date, CampaignFundraisingStats.DailyDonationTrend.builder()
                .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .amount(amount)
                .count(count)
                .build());
        }

        return new ArrayList<>(trendMap.values());
    }

    /**
     * 상위 기부자 목록 조회
     */
    private List<CampaignFundraisingStats.TopDonation> getTopDonations(Long campaignId, int limit) {
        // Repository에서 상위 기부 조회
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = donationRepository.findTopDonationsByCampaignId(campaignId, pageable);

        return results.stream()
            .map(row -> {
                String donorName = (String) row[0];
                Long amount = ((Number) row[1]).longValue();
                LocalDateTime donatedAt = (LocalDateTime) row[2];
                Boolean anonymous = (Boolean) row[3];

                // 익명 처리
                String displayName = Boolean.TRUE.equals(anonymous) ? "익명" : maskName(donorName);

                return CampaignFundraisingStats.TopDonation.builder()
                    .donorName(displayName)
                    .amount(amount)
                    .donatedAt(donatedAt)
                    .anonymous(anonymous)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 이름 마스킹 처리 (예: 김철수 → 김**)
     */
    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "**";
    }
}
