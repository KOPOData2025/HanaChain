// 새로운 이더리움 지갑 생성 스크립트
const { ethers } = require('ethers');

// 새로운 랜덤 지갑 생성
const wallet = ethers.Wallet.createRandom();

console.log('=== 새로운 Ethereum 지갑 생성 ===');
console.log('주소:', wallet.address);
console.log('Private Key:', wallet.privateKey);
console.log('Mnemonic:', wallet.mnemonic.phrase);
console.log('=====================================');
console.log('');
console.log('사용 방법:');
console.log('1. 위 주소로 Sepolia ETH를 받으세요');
console.log('2. Private Key를 BLOCKCHAIN_PRIVATE_KEY 환경변수로 설정하세요');
console.log('3. export BLOCKCHAIN_PRIVATE_KEY="' + wallet.privateKey + '"');