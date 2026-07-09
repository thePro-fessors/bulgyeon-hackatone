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
      const { id, name, isDanger } = item;
      if (!id || !name) continue;
      await db.run(
        'INSERT OR REPLACE INTO Areas (id, name, isDanger) VALUES (?, ?, ?)',
        [id, name, isDanger ? 1 : 0]
      );
    }
    await db.run('COMMIT');
    return NextResponse.json({ success: true, message: `${items.length} areas processed` });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
