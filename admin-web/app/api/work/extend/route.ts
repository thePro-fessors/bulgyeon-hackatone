import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  const { employeeId, extendMinutes } = await request.json();
  const db = await getDb();
  
  await db.run(
    'UPDATE WorkLogs SET durationMinutes = durationMinutes + ? WHERE employeeId = ? AND isFinished = 0',
    [extendMinutes, employeeId]
  );
  
  return NextResponse.json({ success: true, message: 'Work duration extended successfully' });
}
