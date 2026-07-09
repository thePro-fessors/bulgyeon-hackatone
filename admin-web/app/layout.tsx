import './globals.css'
import Link from 'next/link'

export const metadata = {
  title: 'Safety Site - 중앙 관제 대시보드',
  description: 'Safety Tracking Admin Dashboard',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ko">
      <body>
        <div className="layout-wrapper">
          <nav className="global-nav">
            <div className="nav-container">
              <span className="logo-text">⚡ Safety Site Control</span>
              <div className="navbar">
                <Link href="/" className="nav-link">관제 대시보드</Link>
                <Link href="/manage" className="nav-link">데이터 관리 센터</Link>
              </div>
            </div>
          </nav>
          {children}
        </div>
      </body>
    </html>
  )
}
