"use client"

import { DonationCertificate } from "@/types/donation"
import { Award } from "lucide-react"

interface DonationCertificateProps {
  certificate: DonationCertificate
}

/**
 * 기부 증서 컴포넌트 - 정사각형 디자인
 * HanaChain 브랜딩, 그라데이션 배경, 캠페인 이미지 포함
 */
export function DonationCertificateComponent({ certificate }: DonationCertificateProps) {
  // 날짜 포맷팅 (yyyy.MM.dd)
  const formattedDate = new Date(certificate.donatedAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).replace(/\. /g, '.').replace(/\.$/, '')

  return (
    <div
      id="donation-certificate"
      style={{
        background: 'linear-gradient(135deg, #e0f2f1 0%, #b2dfdb 50%, #80cbc4 100%)',
        padding: '1.5rem',
        borderRadius: '1rem',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
        width: '450px',
        height: '380px',
        margin: '0 auto',
        fontFamily: 'var(--font-hana2), ui-sans-serif, system-ui, sans-serif',
        position: 'relative',
        border: 'none',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        boxSizing: 'border-box'
      }}
    >

      {/* 상단: 브랜딩 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          marginBottom: '1rem'
        }}
      >
        {/* HanaChain 브랜딩 */}
        <div
          style={{
            width: '3rem',
            height: '3rem',
            backgroundColor: '#ffffff',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
            border: '3px solid #009591'
          }}
        >
          <Award style={{ width: '1.5rem', height: '1.5rem', color: '#009591' }} />
        </div>
        <div>
          <h1
            style={{
              fontSize: '1.5rem',
              fontWeight: 'bold',
              color: '#009591',
              margin: 0,
              lineHeight: 1.2
            }}
          >
            HanaChain
          </h1>
          <h2
            style={{
              fontSize: '1rem',
              fontWeight: '600',
              color: '#00695c',
              margin: 0,
              lineHeight: 1.2
            }}
          >
            기부증서
          </h2>
        </div>
      </div>

      {/* 중단: 기부자 정보(왼쪽) + 캐릭터(오른쪽) */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: '1rem',
          height: '60px',
          overflow: 'visible',
          position: 'relative'
        }}
      >
        {/* 기부자 정보 */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '0.5rem'
          }}
        >
          <p
            style={{
              fontSize: '2.125rem',
              fontWeight: '600',
              color: '#015c59',
              margin: 0
            }}
          >
            기부자 {certificate.donorName} 님
          </p>
          <p
            style={{
              fontSize: '1rem',
              fontWeight: '500',
              color: '#00695c',
              margin: 0
            }}
          >
            기부일 {formattedDate}
          </p>
        </div>

        {/* 캐릭터 이미지 */}
        <img
          src="/byul_baby_heart.png"
          alt="캐릭터"
          style={{
            width: '260px',
            height: '260px',
            objectFit: 'contain',
            position: 'absolute',
            right: '-50px',
            top: '-120px'
          }}
        />
      </div>

      {/* 하단: 캠페인 이미지 + 정보 */}
      <div
        style={{
          display: 'flex',
          gap: '1rem',
          alignItems: 'center'
        }}
      >
        {/* 작은 캠페인 이미지 */}
        <div
          style={{
            width: '80px',
            height: '80px',
            backgroundColor: '#ffffff',
            borderRadius: '0.75rem',
            overflow: 'hidden',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
            flexShrink: 0
          }}
        >
          {certificate.campaignImage ? (
            <img
              src={certificate.campaignImage}
              alt={certificate.campaignTitle}
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'cover'
              }}
            />
          ) : (
            <div
              style={{
                width: '100%',
                height: '100%',
                backgroundColor: '#e0f2f1',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '0.75rem',
                color: '#00695c',
                padding: '0.5rem',
                textAlign: 'center'
              }}
            >
              {certificate.campaignTitle}
            </div>
          )}
        </div>

        {/* 캠페인 정보 */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '0.5rem',
            flex: 1
          }}
        >
          <h3
            style={{
              fontSize: '1rem',
              fontWeight: 'bold',
              color: '#015c59',
              margin: 0,
              lineHeight: '1.4'
            }}
          >
            {certificate.campaignTitle}
          </h3>
          <p
            style={{
              fontSize: '0.875rem',
              fontWeight: '500',
              color: '#00695c',
              margin: 0
            }}
          >
            기부금액: {certificate.amount.toLocaleString()}원
          </p>
        </div>
      </div>

    </div>
  )
}
