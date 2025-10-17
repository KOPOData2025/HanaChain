'use client'

import { usePathname } from 'next/navigation'
import { Header } from '@/components/ui/header'

interface LayoutClientProps {
  children: React.ReactNode
}

export function LayoutClient({ children }: LayoutClientProps) {
  const pathname = usePathname()
  const isAdminPage = pathname?.startsWith('/admin')

  return (
    <>
      {!isAdminPage && <Header showFullNavigation={true} />}
      <main className={!isAdminPage ? "pt-16" : ""}>
        {children}
      </main>
    </>
  )
}