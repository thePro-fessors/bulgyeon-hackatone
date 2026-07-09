import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  const { employeeId, type } = await request.json();
  const db = await getDb();
  
  const timestamp = new Date().toISOString();
  await db.run(
    'INSERT INTO Accidents (employeeId, type, timestamp) VALUES (?, ?, ?)',
    [employeeId, type, timestamp]
  );
  
  return NextResponse.json({ success: true, message: 'Emergency recorded' });
}
