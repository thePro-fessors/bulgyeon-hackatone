import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { items } = await request.json();
    if (!Array.isArray(items) || items.length === 0) {
      return NextResponse.json({ success: false, message: 'Invalid or empty items array' }, { status: 400 });
    }
    const db = await getDb();
    await db.run('BEGIN TRANSACTION');
    for (const item of items) {
      const { id, text } = item;
      if (!id || !text) continue;
      await db.run(
        'INSERT OR REPLACE INTO Checklists (id, text) VALUES (?, ?)',
        [id, text]
      );
    }
    await db.run('COMMIT');
    return NextResponse.json({ success: true, message: `${items.length} checklists processed` });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
