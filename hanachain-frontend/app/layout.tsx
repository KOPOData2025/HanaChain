import type { Metadata } from 'next'
import { GeistSans } from 'geist/font/sans'
import { GeistMono } from 'geist/font/mono'
import localFont from 'next/font/local'

const hana2 = localFont({
  src: [
    {
      path: '../public/fonts/Hana2-Light.otf',
      weight: '300',
      style: 'normal',
    },
    {
      path: '../public/fonts/Hana2-Regular.otf',
      weight: '400',
      style: 'normal',
    },
    {
      path: '../public/fonts/Hana2-Medium.otf',
      weight: '500',
      style: 'normal',
    },
    {
      path: '../public/fonts/Hana2-Bold.otf',
      weight: '700',
      style: 'normal',
    },
    {
      path: '../public/fonts/Hana2-Heavy.otf',
      weight: '800',
      style: 'normal',
    },
    {
      path: '../public/fonts/Hana2-CM.otf',
      weight: '600',
      style: 'normal',
    },
  ],
  variable: '--font-hana2',
  display: 'swap',
  preload: true,
})
import './globals.css'
import { AuthProvider } from '@/lib/auth-context'
import { LayoutClient } from '@/components/layout/layout-client'

export const metadata: Metadata = {
  title: 'HanaChain - 기부 플랫폼',
  description: 'HanaChain과 함께하는 따뜻한 기부',
  generator: 'HanaChain',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko">
      <head>
        <style>{`
html {
  font-family: ${hana2.style.fontFamily}, system-ui, -apple-system, sans-serif;
  --font-sans: ${GeistSans.variable};
  --font-mono: ${GeistMono.variable};
  --font-hana2: ${hana2.variable};
}
        `}</style>
      </head>
      <body className={`${hana2.variable}`}>
        <AuthProvider>
          <LayoutClient>
            {children}
          </LayoutClient>
        </AuthProvider>
      </body>
    </html>
  )
}
