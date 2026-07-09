import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { employeeId, latitude, longitude } = await request.json();
    if (!employeeId || latitude == null || longitude == null) {
      return NextResponse.json({ success: false, message: 'Invalid arguments' }, { status: 400 });
    }

    const db = await getDb();

    // 테이블 생성 (안전장치)
    await db.exec(`
      CREATE TABLE IF NOT EXISTS RealtimeLocations (
        employeeId TEXT PRIMARY KEY,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        timestamp TEXT NOT NULL
      )
    `);

    const timestamp = new Date().toISOString();
    await db.run(
      'INSERT OR REPLACE INTO RealtimeLocations (employeeId, latitude, longitude, timestamp) VALUES (?, ?, ?, ?)',
      [employeeId, latitude, longitude, timestamp]
    );

    return NextResponse.json({ success: true, message: 'Location reported successfully' });
  } catch (err: any) {
    return NextResponse.json({ success: false, message: err.message }, { status: 500 });
  }
}
