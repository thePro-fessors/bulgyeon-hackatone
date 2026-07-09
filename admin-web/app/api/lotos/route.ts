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
  
  return NextResponse.json({ success: false, message: 'Missing areaId' }, { status: 400 });
}
