# HanaChain

### 블록체인 기반 기부 플랫폼

기부는 카드 결제로 쉽게, 기록은 블록체인으로 투명하게, <br />
**HanaChain이 기부부터 기록까지 책임집니다.**

## 목차

### About Project
- [프로젝트 개요](#프로젝트-개요)
- [프로젝트 주요 기능 이해하기](#프로젝트-주요-기능-이해하기)
  - [기부 프로세스](#기부-프로세스)
  - [화면 스크린샷](#화면-스크린샷)
- [개발자 프로필](#개발자-프로필)

### About Code
- [코드 개요](#코드-개요)
- [개발 환경 설정](#개발-환경-설정)
  - [환경 변수 설정](#환경-변수-설정)
  - [상세 설정 가이드](#상세-설정-가이드)
- [레포지토리 구조](#레포지토리-구조)
  - [Frontend (hanachain-frontend)](#frontend)
  - [Backend (hanachain-backend)](#backend)
  - [FDS (hanachain-fds)](#fds-system)
  - [Blockchain (hanachain-blockchain)](#blockchain)
   

---

# About Project

## 프로젝트 개요

**HanaChain**은 기존 기부 플랫폼의 문제를 블록체인 기술을 통해 해결한 기부 플랫폼입니다.

_아래 이미지는 클릭 시 고화질로 볼 수 있습니다._

| | |
|:---------:|:------:|
| <img width="1200" height="675" alt="3" src="https://github.com/user-attachments/assets/9544ec68-e13f-4f15-8a5a-8f6a9cb497f8" /> | <img width="1200" height="675" alt="4" src="https://github.com/user-attachments/assets/c9b766ef-21c0-41fe-8137-4e48464af701" /> |
| <img width="1200" height="675" alt="6" src="https://github.com/user-attachments/assets/bccbed5d-1e2c-4c65-abaf-1e3846955e4c" /> | <img width="1200" height="675" alt="10" src="https://github.com/user-attachments/assets/d098681b-1cf1-4f0b-b4b3-ae8e033a6098" /> |
| <img width="1200" height="675" alt="11" src="https://github.com/user-attachments/assets/525d20a3-e55b-4908-b663-e22ec108d117" /> | <img width="1200" height="675" alt="13" src="https://github.com/user-attachments/assets/a7808b63-1563-41b1-a608-e6e35c0dfacd" /> |
| <img width="1200" height="675" alt="14" src="https://github.com/user-attachments/assets/5ccc8487-11a2-413b-ad2e-d2ab0eee460f" /> | <img width="1200" height="675" alt="18" src="https://github.com/user-attachments/assets/f26d152c-50f9-4360-96b7-ff7d7737ec81" /> |

---

## 프로젝트 주요 기능 이해하기

### 기부 프로세스

1. **캠페인 생성**
   - 플랫폼 관리자가 캠페인 생성
   - 스마트 컨트랙트에 캠페인 등록(자동)

2. **기부 진행**
   - 사용자가 Frontend에서 기부 결제 시도
   - PortOne 외부 서비스에서 결제 처리
   - Backend에서 FDS API 호출하여 이상 기부 탐지
   - FDS가 승인 / 보류 판단

3. **거래 기록**
   - 플랫폼 관리자가 배치 작업 시 Web3j를 통해 블록체인에 기록
   - 캠페인 정보 업데이트

4. **거래 내역 확인**
   - 모든 기부 내역이 Sepolia 네트워크에 영구 저장
   - [Etherscan](https://etherscan.io)에서 트랜잭션 해시 값으로 누구나 거래 확인 가능

### 화면 스크린샷

| | |
|:---------:|:------:|
| <img width="1958" height="1167" alt="image" src="https://github.com/user-attachments/assets/f5721a32-4a1d-40f9-a141-ad6f56507b44" /> <br /> 메인 페이지 `USER` | <img width="1958" height="1167" alt="image" src="https://github.com/user-attachments/assets/be488212-779b-4c45-8a27-f48276e68a92" /> <br /> 로그인 `USER` |
| <img width="1670" height="1083" alt="image" src="https://github.com/user-attachments/assets/78256539-c6dd-4a2a-9cd3-116e9449f3e3" /> <br /> 캠페인 리스트 `USER` | <img width="1670" height="1083" alt="image" src="https://github.com/user-attachments/assets/c82d6cf9-adfb-443f-856f-e00dd5123e4d" /> <br /> 캠페인 상세 조회 `USER` |
| <img width="1668" height="1082" alt="image" src="https://github.com/user-attachments/assets/8c591c73-d765-4a41-a2ad-bb35adb6673a" /> <br /> 기부 결제 요청 `USER` | <img width="1668" height="1082" alt="image" src="https://github.com/user-attachments/assets/a5fb6cf4-4eba-415a-a320-15e87cbf277f" /> <br /> 기부 결제 프로세스 `USER` |
| <img width="1668" height="1082" alt="image" src="https://github.com/user-attachments/assets/fd3edef5-dbec-4b96-9517-0bb1414c8635" /> <br /> 기부 결제 완료 `USER` | <div width="1958" height="1167" col=2> <img width="44.3%" alt="image" src="https://github.com/user-attachments/assets/87cb8ef7-9486-4005-97c7-5d9c55a895c2" /> <img width="44%" alt="image" src="https://github.com/user-attachments/assets/0dc7a49d-3693-4b32-bd2d-64dead352553" /> </div> 기부 증서 `USER` |
| <img width="1958" height="1167" alt="image" src="https://github.com/user-attachments/assets/c36162bd-46f2-4c3a-be3d-545466befebb" /> <br /> 기부 내역 조회 `USER` | <img width="1333" height="807" alt="image" src="https://github.com/user-attachments/assets/4b467a76-5cf9-43a1-94f7-320e62a0b4fa" /> <br /> 관리자 대시보드 `admin` |
| <img width="1333" height="807" alt="image" src="https://github.com/user-attachments/assets/cd225ece-be5c-4065-b50a-cc2a020ecf28" /> <br /> 캠페인 등록 `admin`| <img width="1333" height="807" alt="image" src="https://github.com/user-attachments/assets/26d554ac-acf9-4f6c-9e6b-87e5545db501" /> <br /> FDS 탐지 결과 `admin` |

---

## 개발자 프로필

| 구분 | 내용 | 비고 |
|:---:|:---:|:---:|
| 이름 | 조승우 | <img width="120" height="608" alt="image" src="https://github.com/user-attachments/assets/7ab2c3a2-78aa-4efb-a3f5-59a20fe8ee8e" /> |
| 학력 | 경희대학교 산업경영공학과 | 졸업 (GPA 4.01 / 4.5) |
| 연락처 | 이메일 | choseung97@gmail.com |
| Skill set | Language | Java, C, SQL, Python, Javascript, Typescript, Solidity |
| | Framework & Library | Spring Boot, Express |
| | Database | Oracle, MySQL |
| | Etc | Docker, Scikit-Learn, TensorFlow, Git |
| 자격증 | 정보처리기사 | 2025.08.13 |
| | 빅데이터분석기사 | 2025.07.11 |
| | SQL개발자(SQLD) | 2025.06.27 |
| | TOEIC Speaking(영어) - AL | 2025.08.24 |
| 수상 | 2025학년도 폴리텍 벤처창업경진대회 본선진출(동상 확보) | 한국폴리텍대학 (2025.10.22) |
| | 2025 GreenTech Globalthon | GDG on Campus INHA (2025.01.18) |
| | 2024년 오픈소스 컨트리뷰션 | 과학기술정보통신부 (2024.12.06) |
| | 제1회 CJ대한통운 미래기술 챌린지 | CJ대한통운 (2021.11.26) |
| | 2021 FIELD Camp Competition | 대한산업공학회 (2021.08.14) |
| 인턴 | CJ대한통운(e-commerce IT) NLP 모델 개발 | 2022.01 ~ 2022.02(1.5개월) |
| 교육 | KAIST SW사관학교 Jungle(비학위 과정) | 2024.03 ~ 2024.07(6개월) |

---

# About Code

## 코드 개요

HanaChain 프로젝트는 4개의 독립적인 레포지토리로 구성되어 있습니다.

### 기술 스택

| 영역 | 기술 |
|------|------|
| **Frontend** | Next.js 15, React 19, TypeScript, Tailwind CSS v4 |
| **Backend** | Spring Boot 3.2.5, Java 17, Spring Security, JPA |
| **Database** | Oracle 23c Free |
| **Blockchain** | Hardhat, Solidity 0.8.19, Web3j, Sepolia Testnet |
| **FDS** | Python 3.9+, TensorFlow 2.13+, Flask, DQN |
| **Payment** | PortOne Browser SDK v2 |
| **Email** | Spring Mail (Gmail SMTP) |

---

## 개발 환경 설정

### 환경 변수 설정

각 레포지토리의 `.env.example` 파일을 참고하여 `.env` 파일 생성:

- `hanachain-frontend/.env.local`
- `hanachain-backend/.env`
- `hanachain-fds/deploy/.env`
- `hanachain-blockchain/.env`

### 상세 설정 가이드

자세한 시스템 구동 가이드는 `산출물/09 HanaChain_시스템_구동_가이드.pdf` 파일을 참고하세요.

---

## 레포지토리 구조

### Frontend

#### 폴더 구조

```
hanachain-frontend/
├── app/                      # Next.js App Router 페이지
│   ├── admin/                # 관리자 패널
│   ├── campaign/             # 캠페인 상세
│   ├── campaigns/            # 캠페인 목록
│   ├── donation/             # 기부 완료
│   ├── login/                # 로그인
│   ├── signup/               # 회원가입
│   ├── mypage/               # 마이페이지
│   └── payment/              # 결제 처리
│
├── components/               # React 컴포넌트
│   ├── ui/                   # UI 컴포넌트 라이브러리
│   ├── admin/                # 관리자 컴포넌트
│   ├── donation/             # 기부 컴포넌트
│   ├── blockchain/           # 블록체인 컴포넌트
│   ├── campaigns/            # 캠페인 컴포넌트
│   └── ...                   # 기타 컴포넌트
│
├── lib/                      # 유틸리티 및 통합
│   ├── api/                  # Backend API 연동
│   ├── payment/              # PortOne 결제
│   └── auth-context.tsx      # 인증 상태 관리
│
├── types/                    # TypeScript 타입 정의
│   ├── admin.ts
│   ├── api.ts
│   └── donation.ts
│
├── public/                   # 정적 에셋
│   ├── fonts/
│   └── images/
│
├── Dockerfile                # Docker 이미지 빌드 설정
├── docker-compose.yml        # Docker Compose 설정
├── .dockerignore             # Docker 빌드 제외 파일
└── .env.example              # 환경 변수 템플릿
```

#### 주요 특징

- **인증 시스템**: JWT 기반 인증, 다단계 회원가입
  - `lib/auth-context.tsx`
      - 인증 상태 관리
  - `app/signup/`
      - 다단계 회원가입 플로우
  - `lib/api/auth-api.ts`
      - 인증 API 호출

- **결제 연동**: PortOne v2 SDK 통합
  - `lib/payment/`
      - PortOne 결제 처리 로직
  - `components/donation/`
      - 기부 폼 컴포넌트
  - `app/payment/`
      - 결제 처리 페이지

- **데이터 시각화**: Recharts 차트
  - `components/charts/`
      - 기부 통계 차트 컴포넌트
  - `components/statistics/`
      - 기부 통계 컴포넌트

---

### Backend

#### 폴더 구조

```
hanachain-backend/
├── src/main/java/com/hanachain/hanachainbackend/
│   ├── config/                   # 설정 클래스
│   ├── controller/               # REST API 엔드포인트
│   ├── dto/                      # Data Transfer Objects
│   ├── entity/                   # JPA 엔티티
│   ├── repository/               # Spring Data JPA
│   ├── service/                  # 비즈니스 로직
│   ├── security/                 # JWT 및 보안
│   ├── batch/                    # Spring Batch 작업
│   ├── exception/                # 예외 처리
│   └── util/                     # 유틸리티
│
├── src/main/resources/
│   ├── application.yml           # 기본 설정
│   ├── application-dev.yml       # 개발 환경
│   ├── application-prod.yml      # 프로덕션 환경
│   └── db/migration/             # Flyway 마이그레이션
│
├── src/test/java/                # 테스트 코드
│   ├── controller/
│   ├── service/
│   └── repository/
│
├── Dockerfile                    # Docker 이미지 빌드 설정
├── docker-compose.yml            # Docker Compose 설정
├── .dockerignore                 # Docker 빌드 제외 파일
└── .env.example                  # 환경 변수 템플릿
```

#### 주요 특징

- **인증/인가**: JWT 토큰 기반 stateless 인증, Role-based 접근 제어
  - `security/JwtTokenProvider.java`
      - JWT 토큰 생성/검증
  - `security/JwtAuthenticationFilter.java`
      - 인증 필터
  - `controller/AuthController.java`
      - 인증 API 엔드포인트

- **이메일 인증**: JavaMail + Gmail SMTP 연동
  - `service/EmailService.java`
      - 이메일 발송 서비스
  - `controller/VerificationController.java`
      - 이메일 인증 API

- **데이터베이스**: Oracle 23c Free, Flyway 마이그레이션
  - `entity/`
      - JPA 엔티티 클래스
  - `repository/`
      - Spring Data JPA 레포지토리
  - `src/main/resources/db/migration/`
      - DB 마이그레이션 스크립트

- **블록체인 연동**: Web3j를 통한 스마트 컨트랙트 상호작용
  - `service/DonationService.java`
      - 블록체인 기부 처리

- **배치 작업**: Spring Batch를 통한 캠페인 상태 업데이트 및 블록체인 기록
  - `batch/CampaignStatusBatchJob.java`
      - 배치 작업 정의

---

### FDS System

#### 폴더 구조

```
hanachain-fds/
├── src/                            # 소스 코드
│   ├── dqn_model.py                # DQN 에이전트
│   ├── predictor.py                # 사기 예측
│   ├── trainer.py                  # 모델 학습
│   ├── feature_engineering.py      # 특징 엔지니어링
│   └── evaluator.py                # 모델 평가
│
├── tests/                          # 테스트 코드
│   ├── test_dqn_model.py
│   ├── test_predictor.py
│   └── test_scenarios.py
│
├── deploy/                         # 배포
│   ├── api_server.py               # Flask API 서버
│   ├── requirements.txt
│   └── Dockerfile                  # 배포용 Docker 이미지
│
├── data/                           # 데이터
│   ├── models/                     # 학습된 모델
│   ├── processed/                  # 전처리 데이터
│   └── raw/                        # 원본 데이터
│
├── Dockerfile                      # 개발용 Docker 이미지 빌드 설정
├── docker-compose.yml              # Docker Compose 설정
├── .dockerignore                   # Docker 빌드 제외 파일
└── .env.example                    # 환경 변수 템플릿
```

#### 주요 특징

- **강화학습**: Deep Q-Network (DQN) 알고리즘
  - `src/dqn_model.py`
      - DQN 에이전트 구현
  - `src/trainer.py`
      - 모델 학습 로직
  - `src/reward_function.py`
      - 보상 함수 정의

- **3단계 판단**: APPROVE / MANUAL_REVIEW / BLOCK
  - `src/predictor.py`
      - 사기 예측 시스템
  - `deploy/api_server.py`
      - Flask REST API 서버

- **17개 특징**: 거래 금액, 시간, 사용자 히스토리
  - `src/feature_engineering.py`
      - 특징 추출 및 정규화

- **높은 정확도**: 89.4% 정확도, 76.8% 정밀도
  - `src/evaluator.py`
      - 모델 평가 및 성능 지표

- **다양한 사기 패턴 감지**: 자금세탁, 계정탈취, 버스트 사기
  - `tests/test_scenarios.py`
      - 사기 시나리오 테스트

---

### Blockchain

#### 폴더 구조

```
hanachain-blockchain/
├── contracts/                         # Solidity 스마트 컨트랙트
│   ├── core/                          # 프로덕션 컨트랙트
│   │   ├── MockUSDC.sol               # ERC20 USDC 토큰 (6 decimals, faucet)
│   │   ├── HanaChainCampaign.sol      # 단일 캠페인 관리 (Solidity 0.8.0)
│   │   └── HanaChainFactory.sol       # 캠페인 팩토리 패턴 (Solidity 0.8.19)
│   └── examples/
│       └── Lock.sol                   # Hardhat 템플릿 예시
│
├── scripts/                           # 배포 및 유틸리티 스크립트
│   ├── core/                          # 코어 컨트랙트 배포
│   │   ├── deploy-mock-usdc.js        # MockUSDC 배포
│   │   ├── deploy-campaign.js         # HanaChainCampaign 배포
│   │   └── deploy-hana.js             # 전체 시스템 배포 (Factory + USDC)
│   ├── check-environment.js           # 환경 변수 검증
│   ├── test-network.js                # Sepolia 네트워크 연결 테스트
│   ├── check-wallet.js                # 지갑 잔액 및 상태 확인
│   ├── verify-setup.js                # 5단계 종합 검증 시스템
│   └── check-campaigns.js             # 배포된 캠페인 상태 조회
│
├── test/                              # Hardhat 테스트 스위트
│   ├── core/                          # 프로덕션 컨트랙트 테스트
│   │   ├── MockUSDC.test.js           # USDC 토큰 테스트
│   │   ├── HanaChainCampaign.test.js  # 캠페인 라이프사이클 테스트
│   │   └── HanaChainFactory.test.js   # 팩토리 패턴 테스트
│   └── examples/
│       └── Lock.test.js               # Hardhat 템플릿 테스트
│
├── artifacts/                         # 컴파일된 컨트랙트 (자동 생성)
├── hardhat.config.js                  # Hardhat 설정 (Sepolia testnet)
├── package.json                       # npm 의존성 및 스크립트
├── .env.example                       # 환경 변수 템플릿
└── README.md                          # 프로젝트 문서
```

#### 주요 특징

- **USDC 기반 경제**: MockUSDC ERC20 토큰으로 투명한 기부 처리
  - `contracts/core/MockUSDC.sol`
      - ERC20 USDC 토큰 컨트랙트 (6 decimals)
  - `scripts/core/deploy-mock-usdc.js`
      - USDC 배포 스크립트

- **스마트 컨트랙트**: HanaChainCampaign으로 캠페인 생성, 기부, 완료 관리
  - `contracts/core/HanaChainCampaign.sol`
      - 단일 캠페인 관리 컨트랙트
  - `contracts/core/HanaChainFactory.sol`
      - 캠페인 팩토리 패턴
  - `scripts/core/deploy-hana.js`
      - 전체 시스템 배포 (Factory + USDC)

- **Sepolia 테스트넷**: Alchemy RPC를 통한 테스트넷 배포
  - `hardhat.config.js`
      - Sepolia 네트워크 설정
  - `scripts/test-network.js`
      - 네트워크 연결 테스트

- **검증 시스템**: 5단계 환경 검증
  - `scripts/verify-setup.js`
      - 환경변수 → 네트워크 → 지갑 → 컴파일 → 배포
  - `scripts/check-environment.js`
      - 환경 변수 검증
  - `scripts/check-wallet.js`
      - 지갑 상태 확인
