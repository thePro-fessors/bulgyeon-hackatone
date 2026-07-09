import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const areaId = searchParams.get('areaId');
  const db = await getDb();
  
  if (areaId) {
    const lotos = await db.all('SELECT * FROM Lotos WHERE areaId = ?', [areaId]);
    return NextResponse.json({ success: true, lotos });
  }
  
  const lotos = await db.all('SELECT l.*, a.name as areaName FROM Lotos l JOIN Areas a ON l.areaId = a.id');
  return NextResponse.json({ success: true, lotos });
}
