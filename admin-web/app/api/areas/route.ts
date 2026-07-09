import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function GET() {
  const db = await getDb();
  const areas = await db.all('SELECT * FROM Areas');
  return NextResponse.json({ success: true, areas });
}
