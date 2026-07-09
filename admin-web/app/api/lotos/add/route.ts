import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { id, areaId, text } = await request.json();
    if (!id || !areaId || !text) {
      return NextResponse.json({ success: false, message: 'ID, AreaID, and Text are required' }, { status: 400 });
    }
    const db = await getDb();
    await db.run('INSERT INTO Lotos (id, areaId, text) VALUES (?, ?, ?)', [id, areaId, text]);
    return NextResponse.json({ success: true, message: 'Loto added successfully' });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
