'use client';

import React, { createContext, useContext, useEffect, useState, useRef } from 'react';

// 전역 타입 선언
declare global {
  interface Window {
    grecaptcha: any;
  }
}

interface RecaptchaContextType {
  isReady: boolean;
  generateToken: (action?: string) => Promise<string>;
  refreshToken: () => Promise<string>;
}

const RecaptchaContext = createContext<RecaptchaContextType | null>(null);

export function RecaptchaProvider({ children }: { children: React.ReactNode }) {
  const [isReady, setIsReady] = useState(false);
  const initPromise = useRef<Promise<void> | null>(null);
  const siteKey = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY!;

  // reCAPTCHA 초기화 함수
  const initRecaptcha = async (): Promise<void> => {
    if (initPromise.current) {
      console.log('🔄 reCAPTCHA 초기화 이미 진행 중...');
      return initPromise.current;
    }

    console.log('🚀 reCAPTCHA 전역 초기화 시작');

    initPromise.current = new Promise((resolve, reject) => {
      // 이미 로드되고 준비된 경우
      if (window.grecaptcha?.ready) {
        console.log('📦 reCAPTCHA 스크립트 이미 존재, ready() 호출');
        window.grecaptcha.ready(() => {
          console.log('✅ reCAPTCHA 초기화 완료 (기존 스크립트)');
          setIsReady(true);
          resolve();
        });
        return;
      }

      // 스크립트 로드
      const existingScript = document.querySelector('script[src*="recaptcha"]');
      if (!existingScript) {
        console.log('📦 reCAPTCHA 스크립트 새로 로드 시작');
        const script = document.createElement('script');
        script.src = `https://www.google.com/recaptcha/api.js?render=${siteKey}`;
        script.async = true;
        script.defer = true;
        
        script.onload = () => {
          console.log('📦 reCAPTCHA 스크립트 로드 완료');
          
          // 로드 완료 후 ready 대기 (폴링 방식으로 안정성 확보)
          const checkReady = (attempt = 1) => {
            console.log(`🔍 reCAPTCHA ready 상태 확인 (시도 ${attempt}/10)`);
            
            if (window.grecaptcha?.ready) {
              window.grecaptcha.ready(() => {
                console.log('✅ reCAPTCHA 초기화 완료 (신규 스크립트)');
                setIsReady(true);
                resolve();
              });
            } else if (attempt < 10) {
              setTimeout(() => checkReady(attempt + 1), 200);
            } else {
              console.error('❌ reCAPTCHA ready 함수 로드 실패 (시간 초과)');
              reject(new Error('reCAPTCHA ready 함수 로드 실패'));
            }
          };
          
          checkReady();
        };
        
        script.onerror = () => {
          console.error('❌ reCAPTCHA 스크립트 로드 실패');
          reject(new Error('reCAPTCHA 스크립트 로드 실패'));
        };
        
        document.head.appendChild(script);
      } else {
        console.log('📦 reCAPTCHA 스크립트 이미 존재, 상태 확인 중...');
        // 기존 스크립트가 있지만 ready가 없는 경우
        const waitForReady = (attempt = 1) => {
          if (window.grecaptcha?.ready) {
            window.grecaptcha.ready(() => {
              console.log('✅ reCAPTCHA 초기화 완료 (기존 스크립트 대기)');
              setIsReady(true);
              resolve();
            });
          } else if (attempt < 20) {
            setTimeout(() => waitForReady(attempt + 1), 250);
          } else {
            reject(new Error('reCAPTCHA ready 함수 대기 시간 초과'));
          }
        };
        waitForReady();
      }
    });

    return initPromise.current;
  };

  // 토큰 생성 함수
  const generateToken = async (action: string = 'submit'): Promise<string> => {
    console.log(`🎯 토큰 생성 요청 (action: ${action})`);
    
    if (!isReady) {
      console.error('❌ reCAPTCHA가 아직 준비되지 않음');
      throw new Error('reCAPTCHA가 아직 준비되지 않았습니다');
    }

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        console.error('❌ 토큰 생성 시간 초과');
        reject(new Error('reCAPTCHA 토큰 생성 시간 초과'));
      }, 10000);

      window.grecaptcha.ready(async () => {
        try {
          console.log(`🔄 reCAPTCHA execute 호출 (${action})`);
          const token = await window.grecaptcha.execute(siteKey, { action });
          clearTimeout(timeout);
          
          if (!token) {
            throw new Error('토큰 생성 결과가 없습니다');
          }
          
          console.log(`✅ reCAPTCHA 토큰 생성 성공 (${action}):`, token.substring(0, 20) + '...');
          resolve(token);
        } catch (error) {
          clearTimeout(timeout);
          console.error('❌ reCAPTCHA 토큰 생성 실패:', error);
          reject(error);
        }
      });
    });
  };

  const refreshToken = async (): Promise<string> => {
    return generateToken('refresh');
  };

  useEffect(() => {
    if (!siteKey) {
      console.error('❌ NEXT_PUBLIC_RECAPTCHA_SITE_KEY가 설정되지 않음');
      return;
    }
    
    initRecaptcha().catch((error) => {
      console.error('❌ reCAPTCHA 초기화 실패:', error);
    });
  }, [siteKey]);

  return (
    <RecaptchaContext.Provider value={{ isReady, generateToken, refreshToken }}>
      {children}
    </RecaptchaContext.Provider>
  );
}

export const useRecaptcha = () => {
  const context = useContext(RecaptchaContext);
  if (!context) {
    throw new Error('useRecaptcha는 RecaptchaProvider 내부에서 사용해야 합니다');
  }
  return context;
};