import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function GET() {
  const db = await getDb();
  const checklists = await db.all('SELECT * FROM Checklists');
  return NextResponse.json({ success: true, checklists });
}
