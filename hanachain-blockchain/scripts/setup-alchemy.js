require('dotenv').config();
const fs = require('fs');
const path = require('path');

function setupAlchemy() {
    console.log("🔧 Alchemy 설정 가이드\n");
    console.log("=" .repeat(50));
    
    console.log("1️⃣ Alchemy 계정 생성");
    console.log("   • https://www.alchemy.com/ 접속");
    console.log("   • 'Get Started' 클릭");
    console.log("   • 이메일로 무료 계정 생성");
    
    console.log("\n2️⃣ 새 앱 생성");
    console.log("   • 'Create App' 클릭");
    console.log("   • 앱 이름 입력 (예: WeAreHana-Sepolia)");
    console.log("   • 네트워크: Sepolia 선택");
    console.log("   • 'Create App' 클릭");
    
    console.log("\n3️⃣ HTTP URL 복사");
    console.log("   • 앱 대시보드에서 'View Key' 클릭");
    console.log("   • HTTP URL 복사 (예: https://eth-sepolia.g.alchemy.com/v2/YOUR_API_KEY)");
    
    console.log("\n4️⃣ .env 파일 설정");
    console.log("   • 복사한 URL을 .env 파일의 SEPOLIA_URL에 붙여넣기");
    
    // .env 파일 존재 여부 확인
    const envPath = path.join(process.cwd(), '.env');
    if (fs.existsSync(envPath)) {
        console.log("\n✅ .env 파일이 존재합니다.");
        
        const envContent = fs.readFileSync(envPath, 'utf8');
        if (envContent.includes('alchemy.com')) {
            console.log("✅ Alchemy URL이 이미 설정되어 있습니다.");
        } else {
            console.log("⚠️ Alchemy URL이 설정되지 않았습니다.");
            console.log("   • .env 파일을 열어서 SEPOLIA_URL을 Alchemy URL로 변경하세요");
        }
    } else {
        console.log("\n⚠️ .env 파일이 없습니다.");
        console.log("   • cp env.example .env 명령어로 .env 파일을 생성하세요");
    }
    
    console.log("\n5️⃣ 설정 확인");
    console.log("   • npm run check-env 명령어로 환경 변수 확인");
    console.log("   • npm run test-network 명령어로 네트워크 연결 테스트");
    
    console.log("\n" + "=".repeat(50));
    console.log("🔗 유용한 링크:");
    console.log("   • Alchemy: https://www.alchemy.com/");
    console.log("   • Alchemy 문서: https://docs.alchemy.com/");
    console.log("   • Sepolia Faucet: https://sepoliafaucet.com/");
    
    console.log("\n💡 Alchemy 사용의 장점:");
    console.log("   • 99.9% 가동률 보장");
    console.log("   • 월 300M 요청 무료");
    console.log("   • 빠른 응답 시간");
    console.log("   • 개발자 친화적 도구");
}

setupAlchemy(); 