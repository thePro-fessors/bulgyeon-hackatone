import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export const dynamic = 'force-dynamic';

export async function GET() {
  const db = await getDb();
  
  const activeLogs = await db.all(`
    SELECT w.*, u.name as userName, a.name as areaName 
    FROM WorkLogs w 
    JOIN Users u ON w.employeeId = u.employeeId
    JOIN Areas a ON w.areaId = a.id
    WHERE w.isFinished = 0
  `);
  
  const accidents = await db.all(`
    SELECT a.*, u.name as userName
    FROM Accidents a
    JOIN Users u ON a.employeeId = u.employeeId
    ORDER BY a.id DESC LIMIT 50
  `);
  
  return NextResponse.json({ success: true, activeLogs, accidents });
}
