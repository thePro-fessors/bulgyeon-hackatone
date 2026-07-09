import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { id, name, isDanger } = await request.json();
    if (!id || !name) {
      return NextResponse.json({ success: false, message: 'ID and Name are required' }, { status: 400 });
    }
    const db = await getDb();
    await db.run('INSERT INTO Areas (id, name, isDanger) VALUES (?, ?, ?)', [id, name, isDanger ? 1 : 0]);
    return NextResponse.json({ success: true, message: 'Area added successfully' });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
