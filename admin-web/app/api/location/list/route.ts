import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function GET() {
  try {
    const db = await getDb();

    // 테이블이 없는 경우 예외 방지용 안전 장치
    await db.exec(`
      CREATE TABLE IF NOT EXISTS RealtimeLocations (
        employeeId TEXT PRIMARY KEY,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        timestamp TEXT NOT NULL
      )
    `);

    // 모든 작업자의 최종 수집된 실시간 위치 조회
    const locations = await db.all(`
      SELECT 
        rl.employeeId, 
        COALESCE(u.name, '알 수 없는 사용자') as name, 
        rl.latitude, 
        rl.longitude, 
        rl.timestamp
      FROM RealtimeLocations rl
      LEFT JOIN Users u ON rl.employeeId = u.employeeId
    `);

    return NextResponse.json({ success: true, locations });
  } catch (err: any) {
    return NextResponse.json({ success: false, message: err.message }, { status: 500 });
  }
}
