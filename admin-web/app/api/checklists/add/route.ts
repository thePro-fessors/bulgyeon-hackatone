import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { id, text } = await request.json();
    if (!id || !text) {
      return NextResponse.json({ success: false, message: 'ID and Text are required' }, { status: 400 });
    }
    const db = await getDb();
    await db.run('INSERT INTO Checklists (id, text) VALUES (?, ?)', [id, text]);
    return NextResponse.json({ success: true, message: 'Checklist added successfully' });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
