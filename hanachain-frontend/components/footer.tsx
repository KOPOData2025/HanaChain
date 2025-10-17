export default function Footer() {
  return (
    <footer className="bg-white border-t mt-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Footer Links */}
        <div className="flex flex-wrap items-center justify-between mb-6">
          <div className="flex flex-wrap items-center space-x-4 text-sm text-gray-600">
            <a href="#" className="hover:text-[#009591]">
              HanaChain 소개
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591]">
              회사 소개
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591]">
              HanaChain 이용약관
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591] text-[#009591] font-medium">
              개인정보처리방침
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591]">
              고정형 영상정보처리기기 운영관리방침
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591]">
              실시간 불특제어 기준 정보
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591]">
              명예의 전당
            </a>
            <span className="text-gray-300">|</span>
            <a href="#" className="hover:text-[#009591]">
              온라인 봉사학교 HanaChain스쿨
            </a>
          </div>
        </div>

        {/* Company Information */}
        <div className="space-y-2 text-sm text-gray-600">
          <div className="flex flex-wrap items-center space-x-4">
            <span>(주)HanaChain</span>
            <span className="text-gray-300">|</span>
            <span>대표자 이수정</span>
            <span className="text-gray-300">|</span>
            <span>사업자등록번호 787-86-02813</span>
            <span className="text-gray-300">|</span>
            <span>대표번호 02 565 8606</span>
            <span className="text-gray-300">|</span>
            <span>통신판매업신고 제 2023-대구동구-0975 호</span>
            <a href="#" className="text-[#009591] hover:underline">
              사업자정보확인
            </a>
          </div>

          <div>(41260) 대구광역시 동구 동대구로 465, 701호 (신천동, 대구스케일업허브)</div>

          <div>기술연구소: (08504) 서울특별시 금천구 서부샛길 648, 308호 (가산동, 대륭테크노타운6차)</div>

          <div className="flex items-center space-x-4">
            <a href="mailto:support@hanachain.charity" className="text-[#009591] hover:underline">
              support@hanachain.charity
            </a>
            <div className="flex items-center space-x-4 ml-auto">
              <a href="#" className="hover:text-[#009591]">
                나눔단체 개설 신청
              </a>
              <span className="text-gray-300">|</span>
              <a href="#" className="hover:text-[#009591]">
                고객의 소리
              </a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  )
}
