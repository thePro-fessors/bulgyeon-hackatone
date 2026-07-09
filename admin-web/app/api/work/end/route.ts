import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  const { employeeId } = await request.json();
  const db = await getDb();
  
  await db.run(
    'UPDATE WorkLogs SET isFinished = 1 WHERE employeeId = ? AND isFinished = 0',
    [employeeId]
  );
  
  return NextResponse.json({ success: true, message: 'Work ended successfully' });
}
