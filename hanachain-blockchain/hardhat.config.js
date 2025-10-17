// 1. 필수 플러그인 및 환경변수 로드
require('dotenv').config();
require("@nomicfoundation/hardhat-toolbox");
/** @type import('hardhat/config').HardhatUserConfig */

module.exports = {
  // 2. Solidity 컴파일러 설정
  solidity: {
    version: "0.8.20", // OpenZeppelin 5.x 호환성을 위한 버전 (통합)
    settings: {
      optimizer: {
        enabled: true, // 가스비 절약을 위한 최적화 활성화
        runs: 200      // 최적화 횟수(일반적으로 200이면 충분)
      },
      viaIR: true // Stack too deep 문제 해결
    }
  },

  // 3. 네트워크 설정
  networks: {
    localhost: {
      url: "http://127.0.0.1:8545" // 로컬 하드햇 노드 주소
    },
    sepolia: {
      url: process.env.SEPOLIA_URL || "", // Sepolia 테스트넷 RPC URL (.env에서 불러옴)
      accounts: process.env.PRIVATE_KEY ? [process.env.PRIVATE_KEY] : []
      // 배포에 사용할 지갑의 프라이빗키 (.env에서 불러옴)
    }
  },

  // 4. Etherscan API 키 설정 (컨트랙트 소스코드 검증용)
  etherscan: {
    apiKey: {
      sepolia: process.env.ETHERSCAN_API_KEY || "dummy" // .env에서 불러옴
    }
  },

  // 5. Sourcify 검증 설정 (API 키 불필요한 자동 검증)
  sourcify: {
    enabled: true
  },

  // 6. 프로젝트 폴더 구조 설정
  paths: {
    sources: "./contracts",   // 스마트컨트랙트 소스코드 폴더
    tests: "./test",          // 테스트 코드 폴더
    cache: "./cache",         // 컴파일 캐시 폴더
    artifacts: "./artifacts"  // 컴파일 결과물(ABI 등) 폴더
  }
};
