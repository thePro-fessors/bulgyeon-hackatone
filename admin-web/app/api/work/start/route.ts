import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  const { employeeId, areaId, durationMinutes } = await request.json();
  const db = await getDb();
  
  const startTime = new Date().toISOString();
  await db.run(
    'INSERT INTO WorkLogs (employeeId, areaId, startTime, durationMinutes, isFinished) VALUES (?, ?, ?, ?, 0)',
    [employeeId, areaId, startTime, durationMinutes]
  );
  
  return NextResponse.json({ success: true, message: 'Work started' });
}
