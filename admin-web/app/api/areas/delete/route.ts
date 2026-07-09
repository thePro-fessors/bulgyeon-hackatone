import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { id } = await request.json();
    if (!id) return NextResponse.json({ success: false, message: 'ID is required' }, { status: 400 });
    const db = await getDb();
    await db.run('DELETE FROM Areas WHERE id = ?', [id]);
    return NextResponse.json({ success: true, message: 'Area deleted successfully' });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
