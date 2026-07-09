import './globals.css'

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
      <body>{children}</body>
    </html>
  )
}
