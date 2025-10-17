"use client"

import { DonationCertificate } from "@/types/donation"
import QRCode from "react-qr-code"

interface CertificateQRInfoProps {
  certificate: DonationCertificate
}

/**
 * 기부 증서 QR 정보 컴포넌트
 * QR 코드, Etherscan 정보, 트랜잭션 해시 표시
 */
export function CertificateQRInfo({ certificate }: CertificateQRInfoProps) {
  const etherscanUrl = `https://sepolia.etherscan.io/tx/${certificate.donationTransactionHash}`

  return (
    <div
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
        gap: '1rem',
        boxSizing: 'border-box'
      }}
    >
      {/* 1행: QR 코드와 안내 문구 */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '170px 1fr',
          gap: '1.5rem',
          alignItems: 'center'
        }}
      >
        {/* 왼쪽 열: QR 코드 */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem'
          }}
        >
          {/* QR 코드 */}
          <div
            style={{
              backgroundColor: '#ffffff',
              padding: '1rem',
              borderRadius: '1rem',
              boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center'
            }}
          >
            <QRCode
              value={etherscanUrl}
              size={150}
              level="H"
              style={{ display: 'block' }}
            />
          </div>
        </div>

        {/* 오른쪽 열: 안내 문구 + Powered by Etherscan */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem',
            justifyContent: 'center',
            alignItems: 'center'
          }}
        >
          {/* 안내 문구 */}
          <div
            style={{
              textAlign: 'center',
              padding: '0.75rem 1rem',
              borderRadius: '0.5rem'
            }}
          >
            <p
              style={{
                fontSize: '0.75rem',
                color: '#015c59',
                margin: 0,
                lineHeight: '1.5',
                fontWeight: '500'
              }}
            >
              QR 코드를 스캔하여 블록체인에서 기부 내역을 확인하세요
            </p>
          </div>

          {/* Powered by Etherscan */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '0.25rem'
            }}
          >
            <p
              style={{
                fontSize: '0.7rem',
                color: '#00695c',
                margin: 0,
                fontWeight: '600'
              }}
            >
              Powered by
            </p>
            <img
              src="/logo-etherscan.svg"
              alt="Etherscan"
              style={{
                width: '100px',
                height: 'auto'
              }}
            />
          </div>
        </div>
      </div>

      {/* 2행: 트랜잭션 해시 (가로 전체) */}
      <div
        style={{
          backgroundColor: 'rgba(255, 255, 255, 0.7)',
          padding: '0.75rem',
          borderRadius: '0.5rem'
        }}
      >
        <p
          style={{
            fontSize: '0.7rem',
            color: '#00695c',
            margin: 0,
            marginBottom: '0.5rem',
            fontWeight: '600',
            textAlign: 'left'
          }}
        >
          트랜잭션 해시
        </p>
        <div
          style={{
            backgroundColor: '#f9fafb',
            padding: '0.5rem',
            borderRadius: '0.375rem',
            border: '1px solid #e5e7eb',
            wordBreak: 'break-all'
          }}
        >
          <p
            style={{
              fontSize: '0.625rem',
              fontFamily: 'monospace',
              color: '#1f2937',
              lineHeight: '1.4',
              margin: 0
            }}
          >
            {certificate.donationTransactionHash}
          </p>
        </div>
      </div>

      {/* 3행: 푸터 문구 */}
      <div
        style={{
          textAlign: 'center',
          marginTop: 'auto'
        }}
      >
        <p
          style={{
            fontSize: '0.7rem',
            color: '#00695c',
            margin: 0,
            fontWeight: '600',
            marginBottom: '0.25rem',
            lineHeight: '1.3'
          }}
        >
          블록체인 기반 투명한 기부 플랫폼
        </p>
        <p
          style={{
            fontSize: '0.7rem',
            color: '#00695c',
            margin: 0,
            lineHeight: '1.3'
          }}
        >
          HanaChain - Making the world a better place
        </p>
      </div>
    </div>
  )
}
